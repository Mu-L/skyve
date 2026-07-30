package org.skyve.impl.util.json;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Converts dynamically parsed JSON values to explicitly requested Java types.
 * Record values are reconstructed by matching JSON properties to record components
 * and invoking the canonical constructor.
 *
 * <p>Supported targets include records, primitive and reference arrays, parameterised
 * collections and maps, enums, characters, primitive wrappers, {@link BigInteger},
 * and {@link BigDecimal}. Unknown properties are ignored when constructing a record;
 * every declared record component must be present.
 *
 * <p>Thread-safe: this class holds no mutable state.
 */
final class JSONTypeMapper {
	/**
	 * Prevents instantiation of this stateless mapper.
	 */
	private JSONTypeMapper() {
		// Utility class.
	}

	/**
	 * Converts a dynamically parsed value to an explicitly supplied Java class.
	 *
	 * @param <T> target Java type
	 * @param value parsed value, or {@code null}
	 * @param targetType requested class; must not be {@code null}
	 * @return converted value, or {@code null} when the source value is {@code null}
	 * @throws ReflectiveOperationException if a target constructor cannot be located or invoked
	 * @throws IllegalArgumentException if the value is incompatible with the requested type
	 */
	static @Nullable <T> T convert(@Nullable Object value, @Nonnull Class<T> targetType)
	throws ReflectiveOperationException {
		Object result = convertValue(value, targetType);
		return result == null ? null : targetType.cast(result);
	}

	/**
	 * Converts a parsed value using complete reflective type information.
	 *
	 * @param value parsed source value, or {@code null}
	 * @param targetType target class or parameterised type
	 * @return converted value, preserving {@code null} for non-primitive targets
	 * @throws ReflectiveOperationException if reflective construction fails
	 */
	private static @Nullable Object convertValue(@Nullable Object value, @Nonnull Type targetType)
	throws ReflectiveOperationException {
		if (value == null) {
			if ((targetType instanceof Class<?> targetClass) && targetClass.isPrimitive()) {
				throw new IllegalArgumentException("Cannot assign null to " + targetClass.getTypeName());
			}
			return null;
		}
		if (targetType instanceof WildcardType) {
			return value;
		}
		if (targetType instanceof ParameterizedType parameterizedType) {
			return convertParameterizedValue(value, parameterizedType);
		}
		if (! (targetType instanceof Class<?> targetClass)) {
			throw new IllegalArgumentException("Unsupported target type " + targetType.getTypeName());
		}
		
		return convertClassValue(value, targetClass);
	}

	/**
	 * Converts a value to a parameterised collection, map, or its raw class.
	 *
	 * @param value non-null parsed value
	 * @param targetType parameterised destination type
	 * @return converted non-null value
	 * @throws ReflectiveOperationException if nested conversion or construction fails
	 */
	private static @Nonnull Object convertParameterizedValue(@Nonnull Object value,
																@Nonnull ParameterizedType targetType)
	throws ReflectiveOperationException {
		Class<?> rawClass = (Class<?>) targetType.getRawType();
		if (Collection.class.isAssignableFrom(rawClass)) {
			return convertCollection(value, rawClass, targetType.getActualTypeArguments()[0]);
		}
		if (Map.class.isAssignableFrom(rawClass)) {
			Type[] arguments = targetType.getActualTypeArguments();
			return convertMap(value, rawClass, arguments[0], arguments[1]);
		}
		
		throw new IllegalArgumentException("Unsupported target type " + targetType.getTypeName());
	}

	/**
	 * Converts a value to a concrete class, including records and scalar Java types.
	 *
	 * @param value non-null parsed value
	 * @param targetClass requested class
	 * @return converted non-null value
	 * @throws ReflectiveOperationException if reflective construction fails
	 * @throws IllegalArgumentException if no supported conversion exists
	 */
	private static @Nonnull Object convertClassValue(@Nonnull Object value, @Nonnull Class<?> targetClass)
	throws ReflectiveOperationException {
		Class<?> boxedTargetClass = boxed(targetClass);
		if (boxedTargetClass.isInstance(value)) {
			return value;
		}
		if (targetClass.isRecord()) {
			return convertRecord(value, targetClass);
		}
		if (targetClass.isArray()) {
			return convertArray(value, targetClass.getComponentType());
		}
		if (Number.class.isAssignableFrom(boxedTargetClass) && (value instanceof Number number)) {
			return convertNumber(number, boxedTargetClass);
		}
		if (targetClass.isEnum() && (value instanceof String string)) {
			return enumValue(targetClass, string);
		}
		if (Character.class.equals(boxedTargetClass) && 
				(value instanceof String string) && 
				(string.length() == 1)) {
			return Character.valueOf(string.charAt(0));
		}
		
		throw new IllegalArgumentException("Cannot convert " + value.getClass().getTypeName() + " to " + targetClass.getTypeName());
	}

	/**
	 * Constructs a record by matching map entries to components in declaration order.
	 *
	 * <p>Side effects: invokes the record's canonical constructor, including any compact
	 * constructor validation or application logic.
	 *
	 * @param value parsed map containing all record components
	 * @param targetClass record class to construct
	 * @return constructed record instance
	 * @throws ReflectiveOperationException if the canonical constructor cannot be accessed or invoked
	 * @throws IllegalArgumentException if the value is not a map or a component is missing
	 */
	private static @Nonnull Object convertRecord(@Nonnull Object value, @Nonnull Class<?> targetClass)
	throws ReflectiveOperationException {
		if (! (value instanceof Map<?, ?> values)) {
			throw new IllegalArgumentException("Cannot convert " + value.getClass().getTypeName() + " to record " + targetClass.getTypeName());
		}
		
		RecordComponent[] components = targetClass.getRecordComponents();
		Class<?>[] parameterTypes = new Class<?>[components.length];
		Object[] arguments = new Object[components.length];
		for (int i = 0; i < components.length; i++) {
			RecordComponent component = components[i];
			String name = component.getName();
			if (! values.containsKey(name)) {
				throw new IllegalArgumentException("JSON object has no value for record component " + targetClass.getTypeName() + '.' + name);
			}
			parameterTypes[i] = component.getType();
			arguments[i] = convertValue(values.get(name), component.getGenericType());
		}
		
		Constructor<?> constructor = targetClass.getDeclaredConstructor(parameterTypes);
		if (! constructor.trySetAccessible()) {
			throw new IllegalAccessException("Cannot access constructor for " + targetClass.getTypeName());
		}
		return constructor.newInstance(arguments);
	}

	/**
	 * Copies parsed list elements into an array of the requested component type.
	 *
	 * @param value parsed list value
	 * @param componentType destination component type
	 * @return newly allocated array
	 * @throws ReflectiveOperationException if an element conversion fails
	 */
	private static @Nonnull Object convertArray(@Nonnull Object value, @Nonnull Class<?> componentType)
	throws ReflectiveOperationException {
		if (! (value instanceof List<?> values)) {
			throw new IllegalArgumentException("Cannot convert " + value.getClass().getTypeName() + " to an array");
		}
		
		Object result = Array.newInstance(componentType, values.size());
		for (int i = 0; i < values.size(); i++) {
			Array.set(result, i, convertValue(values.get(i), componentType));
		}
		
		return result;
	}

	/**
	 * Copies parsed elements into a collection compatible with the requested raw type.
	 *
	 * <p>Interfaces and abstract collection types use an insertion-ordered set or list;
	 * concrete types are created through a no-argument constructor.
	 *
	 * @param value parsed collection value
	 * @param targetClass requested raw collection class
	 * @param elementType requested element type
	 * @return newly allocated mutable collection
	 * @throws ReflectiveOperationException if construction or element conversion fails
	 */
	private static @Nonnull Collection<Object> convertCollection(@Nullable Object value,
																	@Nonnull Class<?> targetClass,
																	@Nonnull Type elementType) throws ReflectiveOperationException {
		if (! (value instanceof Collection<?> values)) {
			throw new IllegalArgumentException("Cannot convert " + 
												((value == null) ? "null" : value.getClass().getTypeName()) + 
												" to " + targetClass.getTypeName());
		}
		Collection<Object> result = newCollection(targetClass);
		for (Object element : values) {
			result.add(convertValue(element, elementType));
		}
		return result;
	}

	/**
	 * Copies parsed entries into a map compatible with the requested raw type.
	 *
	 * <p>Interfaces and abstract map types use an insertion-ordered map; concrete types
	 * are created through a no-argument constructor.
	 *
	 * @param value parsed map value
	 * @param targetClass requested raw map class
	 * @param keyType requested key type
	 * @param valueType requested value type
	 * @return newly allocated mutable map
	 * @throws ReflectiveOperationException if construction or entry conversion fails
	 */
	private static @Nonnull Map<Object, Object> convertMap(@Nullable Object value, @Nonnull Class<?> targetClass,
			@Nonnull Type keyType, @Nonnull Type valueType) throws ReflectiveOperationException {
		if (! (value instanceof Map<?, ?> values)) {
				throw new IllegalArgumentException("Cannot convert " + 
														((value == null) ? "null" : value.getClass().getTypeName()) + 
														" to " + targetClass.getTypeName());
		}
		Map<Object, Object> result = newMap(targetClass);
		for (Map.Entry<?, ?> entry : values.entrySet()) {
			result.put(convertValue(entry.getKey(), keyType), convertValue(entry.getValue(), valueType));
		}
		return result;
	}

	/**
	 * Creates an empty mutable collection compatible with a requested raw type.
	 *
	 * @param targetClass requested collection class
	 * @return empty mutable collection
	 * @throws ReflectiveOperationException if a concrete collection cannot be constructed
	 */
	@SuppressWarnings("unchecked")
	private static @Nonnull Collection<Object> newCollection(@Nonnull Class<?> targetClass)
	throws ReflectiveOperationException {
		if (targetClass.isInterface() || Modifier.isAbstract(targetClass.getModifiers())) {
			return Set.class.isAssignableFrom(targetClass) ? new LinkedHashSet<>() : new ArrayList<>();
		}
		return (Collection<Object>) targetClass.getDeclaredConstructor().newInstance();
	}

	/**
	 * Creates an empty mutable map compatible with a requested raw type.
	 *
	 * @param targetClass requested map class
	 * @return empty mutable map
	 * @throws ReflectiveOperationException if a concrete map cannot be constructed
	 */
	@SuppressWarnings("unchecked")
	private static @Nonnull Map<Object, Object> newMap(@Nonnull Class<?> targetClass)
	throws ReflectiveOperationException {
		if (targetClass.isInterface() || Modifier.isAbstract(targetClass.getModifiers())) {
			return new LinkedHashMap<>();
		}
		return (Map<Object, Object>) targetClass.getDeclaredConstructor().newInstance();
	}

	/**
	 * Converts a parsed number to a supported numeric target class.
	 *
	 * <p>Integral narrowing follows the corresponding {@link Number} conversion and may
	 * truncate or overflow. Converting {@link BigDecimal} to {@link BigInteger} discards
	 * the fractional part.
	 *
	 * @param value source number
	 * @param targetClass boxed numeric target class
	 * @return converted numeric value
	 * @throws IllegalArgumentException if the numeric target is unsupported
	 */
	private static @Nonnull Object convertNumber(@Nonnull Number value, @Nonnull Class<?> targetClass) {
		if (Byte.class.equals(targetClass)) {
			return Byte.valueOf(value.byteValue());
		}
		if (Short.class.equals(targetClass)) {
			return Short.valueOf(value.shortValue());
		}
		if (Integer.class.equals(targetClass)) {
			return Integer.valueOf(value.intValue());
		}
		if (Long.class.equals(targetClass)) {
			return Long.valueOf(value.longValue());
		}
		if (Float.class.equals(targetClass)) {
			return Float.valueOf(value.floatValue());
		}
		if (Double.class.equals(targetClass)) {
			return Double.valueOf(value.doubleValue());
		}
		if (BigInteger.class.equals(targetClass)) {
			return value instanceof BigDecimal decimal ? decimal.toBigInteger() : BigInteger.valueOf(value.longValue());
		}
		if (BigDecimal.class.equals(targetClass)) {
			return new BigDecimal(value.toString());
		}
		throw new IllegalArgumentException("Unsupported numeric target type " + targetClass.getTypeName());
	}

	/**
	 * Resolves an enum constant by its exact declared name.
	 *
	 * @param targetClass enum class
	 * @param value case-sensitive constant name
	 * @return matching enum constant
	 * @throws IllegalArgumentException if the name is not declared by the enum
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static @Nonnull Object enumValue(@Nonnull Class<?> targetClass, @Nonnull String value) {
		return Enum.valueOf((Class<? extends Enum>) targetClass, value);
	}

	/**
	 * Returns the wrapper class corresponding to a primitive type.
	 *
	 * @param type primitive or reference class
	 * @return wrapper class for a primitive, otherwise {@code type}
	 */
	private static @Nonnull Class<?> boxed(@Nonnull Class<?> type) {
		if (! type.isPrimitive()) {
			return type;
		}
		if (boolean.class.equals(type)) {
			return Boolean.class;
		}
		if (byte.class.equals(type)) {
			return Byte.class;
		}
		if (short.class.equals(type)) {
			return Short.class;
		}
		if (int.class.equals(type)) {
			return Integer.class;
		}
		if (long.class.equals(type)) {
			return Long.class;
		}
		if (float.class.equals(type)) {
			return Float.class;
		}
		if (double.class.equals(type)) {
			return Double.class;
		}
		if (char.class.equals(type)) {
			return Character.class;
		}
		return type; // void has no wrapper type
	}
}
