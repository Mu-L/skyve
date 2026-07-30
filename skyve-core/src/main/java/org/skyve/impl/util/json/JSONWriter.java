package org.skyve.impl.util.json;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.skyve.domain.Bean;
import org.skyve.domain.ChildBean;
import org.skyve.domain.HierarchicalBean;
import org.skyve.domain.PersistentBean;
import org.skyve.domain.types.Enumeration;
import org.skyve.domain.types.OptimisticLock;
import org.skyve.impl.bind.BindUtil;
import org.skyve.impl.domain.AbstractPersistentBean;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.Attribute;
import org.skyve.metadata.model.Attribute.AttributeType;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.model.document.Reference;
import org.skyve.metadata.module.Module;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.slf4j.Logger;
import org.skyve.util.logging.SkyveLoggerFactory;

/**
 * Serialises Java objects — domain beans, maps, collections, and primitives — to
 * a JSON string, respecting Skyve attribute converters and bean reference semantics.
 *
 * <p>Java records are written as self-describing JSON objects in component declaration
 * order, using the same {@code class} attribute as ordinary Java beans. The writer
 * also supports circular-reference detection via a visited-object stack. Geometry
 * values are written as Well-Known Text (WKT). Dates and Skyve temporal types are
 * written as ISO-8601 strings.
 *
 * <p>Threading: not thread-safe. Create a new instance per serialisation.
 */
public class JSONWriter {
	private static final Logger LOGGER = SkyveLoggerFactory.getLogger(JSONWriter.class);
	private StringBuilder buf = new StringBuilder();
	private Deque<Object> calls = new ArrayDeque<>(16); // non-null elements
	private Customer customer;

	/**
	 * Creates a writer with the customer metadata needed to serialise Skyve documents.
	 *
	 * @param customer customer context, or {@code null} when no Skyve document will be written
	 */
	public JSONWriter(@Nullable Customer customer) {
		this.customer = customer;
	}

	/**
	 * Serialises an object graph to JSON, optionally projecting named bean properties.
	 *
	 * <p>Records always include a leading {@code class} attribute. Ordinary Java beans
	 * include it only when {@code propertyNames} is {@code null}; projected beans omit it.
	 * Repeated references on the active recursion path are written as JSON {@code null}.
	 *
	 * @param object root value, or {@code null}
	 * @param propertyNames projected property names, or {@code null} for a complete value
	 * @return JSON text; never {@code null}
	 * @throws IllegalStateException if a Skyve document is supplied without customer metadata
	 */
	public @Nonnull String write(@Nullable Object object, @Nullable Set<String> propertyNames) {
		buf.setLength(0);
		value(object, propertyNames, true);
		return buf.toString();
	}

	/**
	 * Returns the JSON number representation of a long value.
	 */
	public static @Nonnull String write(long n) {
		return String.valueOf(n);
	}

	/**
	 * Returns the JSON number representation of a double value.
	 */
	public static @Nonnull String write(double d) {
		return String.valueOf(d);
	}

	/**
	 * Returns a quoted JSON representation of a character.
	 */
	public static @Nonnull String write(char c) {
		return "\"" + c + "\"";
	}

	/**
	 * Returns the JSON literal representing a boolean value.
	 */
	public static @Nonnull String write(boolean b) {
		return String.valueOf(b);
	}

	/**
	 * Writes one value while detecting references already active in the current graph path.
	 *
	 * @param object value to write, or {@code null}
	 * @param propertyNames optional bean projection propagated to nested values
	 * @param topLevel whether the value is the root projection value
	 */
	private void value(@Nullable Object object, @Nullable Set<String> propertyNames, boolean topLevel) {
		if (object == null || cyclic(object)) {
			add("null");
		}
		else {
			calls.push(object);
			writeValue(object, propertyNames, topLevel);
			calls.pop();
		}
	}

	/**
	 * Dispatches a non-null value to its type-specific writer.
	 *
	 * @param object non-null value to write
	 * @param propertyNames optional bean projection propagated to nested values
	 * @param topLevel whether the value is the root projection value
	 */
	@SuppressWarnings("java:S3776") // Type dispatch is intentionally explicit.
	private void writeValue(@Nonnull Object object, @Nullable Set<String> propertyNames, boolean topLevel) {
		if (object instanceof Class<?> type) {
			string(type.getName());
		}
		else if (object instanceof Boolean bool) {
			bool(bool.booleanValue());
		}
		else if (object instanceof Number) {
			add(object);
		}
		else if (object instanceof Date) {
			string(object.toString());
		}
		else if (object instanceof String || object instanceof Character) {
			string(object);
		}
		else if (object instanceof Enumeration enumeration) {
			string(enumeration.toCode());
		}
		else if (object instanceof Enum<?>) {
			string(object);
		}
		else if (object instanceof Map<?, ?> map) {
			map(map, propertyNames, false);
		}
		else if (object.getClass().isArray()) {
			array(object, propertyNames, topLevel);
		}
		else if (object instanceof Iterator<?> iterator) {
			array(iterator, propertyNames, topLevel);
		}
		else if (object instanceof Iterable<?> iterable) {
			array(iterable.iterator(), propertyNames, topLevel);
		}
		// if we have properties (we are doing a list projection),
		// then use the bizId as the bean and don't embed the JSON object
		// (Bean first, then record in case a record implements Bean.
		else if (object instanceof Bean bean) {
			writeBeanValue(bean, propertyNames, topLevel);
		}
		// (Bean first, then record in case a record implements Bean.
		else if (object.getClass().isRecord()) {
			recordValue(object, propertyNames);
		}
		else if (object instanceof OptimisticLock optimisticLock) {
			string(optimisticLock.toString());
		}
		else if (object instanceof Geometry geometry) {
			string(new WKTWriter().write(geometry));
		}
		else {
			bean(object, propertyNames, false);
		}
	}

	/**
	 * Writes a Skyve document inline or as a nested bizId reference for a projection.
	 *
	 * @param bean document value to write
	 * @param propertyNames optional document projection
	 * @param topLevel whether the document is the root projection value
	 */
	private void writeBeanValue(@Nonnull Bean bean, @Nullable Set<String> propertyNames, boolean topLevel) {
		// List projections refer to nested beans by bizId instead of embedding them.
		if ((propertyNames != null) && (! topLevel)) {
			string(bean.getBizId());
		}
		else {
			document(bean, propertyNames, false);
		}
	}

	/**
	 * Serialises a Java record as a self-describing JSON object whose properties
	 * follow record component declaration order.
	 *
	 * @param object the record instance to serialise
	 * @param propertyNames optional projection propagated to nested values
	 * @throws IllegalStateException if a component accessor cannot be invoked
	 */
	private void recordValue(@Nonnull Object object, @Nullable Set<String> propertyNames) {
		RecordComponent[] components = object.getClass().getRecordComponents();
		add("{");
		add("class", object.getClass(), propertyNames, false);
		if (components.length > 0) {
			add(',');
		}
		for (int i = 0; i < components.length; i++) {
			RecordComponent component = components[i];
			Method accessor = component.getAccessor();
			try {
				accessor.trySetAccessible();
				add(component.getName(), accessor.invoke(object), propertyNames, false);
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				Throwable cause = Objects.requireNonNullElse(e.getCause(), e);
				throw new IllegalStateException("Could not read record component " + component.getName(), cause);
			}
			if (i < components.length - 1) {
				add(',');
			}
		}
		add("}");
	}

	/**
	 * Reports whether an object is already active on the current recursion path.
	 *
	 * @param object non-null object being considered for writing
	 * @return {@code true} when writing it again would create a cycle
	 */
	private boolean cyclic(@Nonnull Object object) {
		return calls.contains(object);
	}

	/**
	 * Writes an ordinary JavaBean using readable properties accepted by the writer contract.
	 *
	 * <p>A leading {@code class} property is included for complete values and omitted
	 * when a projection is supplied. Reflective accessor failures are logged and the
	 * JSON object is closed with the properties written successfully up to that point.
	 *
	 * @param object non-null JavaBean to inspect
	 * @param propertyNames projection propagated to property values, or {@code null}
	 * @param topLevel whether property values are part of the root projection
	 */
	@SuppressWarnings("java:S3776") // Complexity OK
	private void bean(@Nonnull Object object, @Nullable Set<String> propertyNames, boolean topLevel) {
		boolean firstProperty = true;

		add("{");
		BeanInfo info;
		if (propertyNames == null) {
			add("class", object.getClass(), propertyNames, topLevel);
			firstProperty = false;
		}

		try {
			Class<?> type = object.getClass();
			info = Introspector.getBeanInfo(type);
			PropertyDescriptor[] props = info.getPropertyDescriptors();
			for (int i = 0; i < props.length; ++i) {
				PropertyDescriptor prop = props[i];
				String name = prop.getName();
				Method accessor = prop.getReadMethod();
				Method mutator = prop.getWriteMethod();
				if ((accessor != null) && // has read access
						((mutator != null) || // has write access
							// errorMessage property in ErrorMessage
							"errorMessage".equals(name) ||
							// or is a collection, iterator or iterable
							Collection.class.isAssignableFrom(prop.getPropertyType()) ||
							Iterator.class.equals(prop.getPropertyType()) || 
							Iterable.class.equals(prop.getPropertyType()))) {
					accessor.trySetAccessible();
					Object value = accessor.invoke(object, (Object[]) null);
					if (! firstProperty) {
						add(',');
					}
					add(name, value, propertyNames, topLevel);
					firstProperty = false;
				}
			}
		}
		catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
			Throwable cause = Objects.requireNonNullElse(e.getCause(), e);
			LOGGER.error(cause.getMessage(), cause);
		}
		add("}");
	}

	/**
	 * Writes a Skyve document using document metadata or a projected binding set.
	 *
	 * <p>Complete documents contain module/document discriminators, declared attributes,
	 * identity and ownership properties, plus applicable child, hierarchy, and persistence
	 * state. Projected documents contain the discriminators and requested sanitised bindings.
	 * Metadata and binding failures after writing begins are logged and the object is closed.
	 *
	 * @param bean non-null Skyve document instance
	 * @param propertyNames requested bindings, or {@code null} for a complete document
	 * @param topLevel whether projected nested beans should be embedded
	 * @throws IllegalStateException if this writer has no customer metadata
	 */
	@SuppressWarnings("java:S3776") // Complexity OK
	private void document(@Nonnull Bean bean, @Nullable Set<String> propertyNames, boolean topLevel) {
		if (customer == null) {
			throw new IllegalStateException("Marshalling a Skyve Bean requires a customer");
		}
		add("{");
		try {
			add(Bean.MODULE_KEY, bean.getBizModule(), propertyNames, topLevel);
			add(',');
			add(Bean.DOCUMENT_KEY, bean.getBizDocument(), propertyNames, topLevel);

			if (propertyNames == null) { // This is a real bean we are marshalling - (for edit view)
				Module module = customer.getModule(bean.getBizModule());
				Document document = module.getDocument(customer, bean.getBizDocument());

				for (Attribute attribute : document.getAllAttributes(customer)) {
					String name = attribute.getName();
					add(',');
					if (attribute instanceof Reference) {
						Object value = BindUtil.get(bean, name);
						add(name, value, propertyNames, topLevel);
					}
					else {
						// Ensure that the code is sent back for attributes with domain values
						if (attribute.getDomainType() != null) {
							Object value = BindUtil.get(bean, name);
							add(name, value, propertyNames, topLevel);
						}
						// ensure Booleans output true or false, not yes or no
						else if (AttributeType.bool.equals(attribute.getAttributeType())) {
							Object value = BindUtil.get(bean, name);
							add(name, value, propertyNames, topLevel);
						}
						else {
							String value = BindUtil.getDisplay(customer, bean, name);
							add(name, value, propertyNames, topLevel);
						}
					}
				}

				add(',');
				add(Bean.DOCUMENT_ID, bean.getBizId(), propertyNames, topLevel);
				add(',');
				add(Bean.CUSTOMER_NAME, bean.getBizCustomer(), propertyNames, topLevel);
				add(',');
				add(Bean.DATA_GROUP_ID, bean.getBizDataGroupId(), propertyNames, topLevel);
				add(',');
				add(Bean.USER_ID, bean.getBizUserId(), propertyNames, topLevel);

				if (bean instanceof ChildBean<?> childBean) {
					add(',');
					add(ChildBean.PARENT_NAME, childBean.getParent(), propertyNames, topLevel);
					add(',');
					add(Bean.ORDINAL_NAME, childBean.getBizOrdinal(), propertyNames, topLevel);
				}
				
				if (bean instanceof HierarchicalBean<?> hierarchicalBean) {
					add(',');
					add(HierarchicalBean.PARENT_ID, hierarchicalBean.getBizParentId(), propertyNames, topLevel);
				}

				if (bean instanceof AbstractPersistentBean) {
					PersistentBean persistentBean = (PersistentBean) bean;
					add(',');
					add(PersistentBean.VERSION_NAME, persistentBean.getBizVersion(), propertyNames, topLevel);
					add(',');
					add(PersistentBean.LOCK_NAME, persistentBean.getBizLock(), propertyNames, topLevel);
				}
			}
			else { // we are marshalling a DynamicBean (for List View)
				for (String name : propertyNames) {
					Object value = null;
					try {
						value = BindUtil.get(bean, name);
					}
					catch (@SuppressWarnings("unused") Exception e) {
						// do nothing - we try and get bogus properties from map beans in the list views - summary rows for instance
					}
					add(',');
					name = Objects.requireNonNull(BindUtil.sanitiseBinding(name), "sanitised binding");
					add(name, value, propertyNames, topLevel);
				}
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		add("}");
	}

	/**
	 * Writes one named JSON property and recursively serialises its value.
	 *
	 * @param name property name; must not be {@code null}
	 * @param value property value, or {@code null}
	 * @param propertyNames projection propagated to the value, or {@code null}
	 * @param topLevel whether the value is part of the root projection
	 */
	private void add(@Nonnull String name,
						@Nullable Object value,
						@Nullable Set<String> propertyNames,
						boolean topLevel) {
		add('"');
		add(name);
		add("\":");
		value(value, propertyNames, topLevel);
	}

	/**
	 * Writes map entries in iteration order as a JSON object.
	 *
	 * @param map non-null source map
	 * @param propertyNames projection propagated to keys and values, or {@code null}
	 * @param topLevel whether entry values are part of the root projection
	 */
	private void map(@Nonnull Map<?, ?> map, @Nullable Set<String> propertyNames, boolean topLevel) {
		add("{");
		Iterator<?> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
			value(e.getKey(), propertyNames, topLevel);
			add(":");
			value(e.getValue(), propertyNames, topLevel);
			if (it.hasNext())
				add(',');
		}
		add("}");
	}

	/**
	 * Writes the remaining iterator elements as a JSON array.
	 *
	 * <p>Side effects: consumes {@code it} completely.
	 *
	 * @param it non-null iterator positioned before its next element
	 * @param propertyNames projection propagated to elements, or {@code null}
	 * @param topLevel whether elements are part of the root projection
	 */
	private void array(@Nonnull Iterator<?> it, @Nullable Set<String> propertyNames, boolean topLevel) {
		add("[");
		while (it.hasNext()) {
			value(it.next(), propertyNames, topLevel);
			if (it.hasNext())
				add(",");
		}
		add("]");
	}

	/**
	 * Writes a primitive or reference Java array as a JSON array.
	 *
	 * @param object non-null Java array
	 * @param propertyNames projection propagated to elements, or {@code null}
	 * @param topLevel whether elements are part of the root projection
	 * @throws IllegalArgumentException if {@code object} is not an array
	 */
	private void array(@Nonnull Object object, @Nullable Set<String> propertyNames, boolean topLevel) {
		add("[");
		int length = Array.getLength(object);
		for (int i = 0; i < length; ++i) {
			value(Array.get(object, i), propertyNames, topLevel);
			if (i < length - 1)
				add(',');
		}
		add("]");
	}

	/**
	 * Writes the JSON literal corresponding to a primitive boolean value.
	 */
	private void bool(boolean b) {
		add(Boolean.toString(b));
	}

	/**
	 * Writes an object's string form as a quoted, escaped JSON string.
	 *
	 * @param obj non-null value whose {@link Object#toString()} result is written
	 */
	private void string(@Nonnull Object obj) {
		add('"');
		CharacterIterator it = new StringCharacterIterator(obj.toString());
		for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
			if (c == '"')
				add("\\\"");
			else if (c == '\\')
				add("\\\\");
			else if (c == '/')
				add("\\/");
			else if (c == '\b')
				add("\\b");
			else if (c == '\f')
				add("\\f");
			else if (c == '\n')
				add("\\n");
			else if (c == '\r')
				add("\\r");
			else if (c == '\t')
				add("\\t");
			else if (Character.isISOControl(c)) {
				unicode(c);
			}
			else {
				// we need to remove all characters with a high order byte that is not zero
				// so that ServletOutputStream does not barf
//				if ((c & 0xff00) == 0) { // high order byte must be zero
//					add(c);
//				}
				// There is no text output from skyve servlets generated with ServletOutputStream any more.
				// They all use PrintWriter which translates character encodings to UTF-8 for us.
				// So there is no need to do any of the above stuff - just add the character!!
				add(c);
			}
		}
		add('"');
	}

	/**
	 * Appends an object's string form directly to the output buffer.
	 *
	 * @param obj value to append; {@code null} appends the text {@code "null"}
	 */
	private void add(@Nullable Object obj) {
		buf.append(obj);
	}

	/**
	 * Appends one character directly to the output buffer.
	 */
	private void add(char c) {
		buf.append(c);
	}

	static char[] hex = "0123456789ABCDEF".toCharArray();

	/**
	 * Writes a UTF-16 code unit using a four-digit JSON Unicode escape.
	 *
	 * @param c code unit to escape
	 */
	private void unicode(char c) {
		add("\\u");
		int n = c;
		for (int i = 0; i < 4; ++i) {
			int digit = (n & 0xf000) >> 12;
			add(hex[digit]);
			n <<= 4;
		}
	}
}
