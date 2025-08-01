package org.skyve.impl.util;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.skyve.CORE;
import org.skyve.domain.Bean;
import org.skyve.domain.messages.Message;
import org.skyve.domain.messages.UniqueConstraintViolationException;
import org.skyve.domain.messages.ValidationException;
import org.skyve.domain.types.Decimal;
import org.skyve.domain.types.converters.Converter;
import org.skyve.domain.types.converters.Format;
import org.skyve.domain.types.converters.Validator;
import org.skyve.impl.bind.BindUtil;
import org.skyve.impl.metadata.customer.CustomerImpl;
import org.skyve.impl.metadata.model.document.field.ConvertibleField;
import org.skyve.impl.metadata.model.document.field.Date;
import org.skyve.impl.metadata.model.document.field.DateTime;
import org.skyve.impl.metadata.model.document.field.Decimal10;
import org.skyve.impl.metadata.model.document.field.Decimal2;
import org.skyve.impl.metadata.model.document.field.Decimal5;
import org.skyve.impl.metadata.model.document.field.LongInteger;
import org.skyve.impl.metadata.model.document.field.Text;
import org.skyve.impl.metadata.model.document.field.TextFormat;
import org.skyve.impl.metadata.model.document.field.Time;
import org.skyve.impl.metadata.model.document.field.Timestamp;
import org.skyve.impl.metadata.model.document.field.validator.DateValidator;
import org.skyve.impl.metadata.model.document.field.validator.DecimalValidator;
import org.skyve.impl.metadata.model.document.field.validator.IntegerValidator;
import org.skyve.impl.metadata.model.document.field.validator.LongValidator;
import org.skyve.impl.metadata.model.document.field.validator.TextValidator;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.Attribute;
import org.skyve.metadata.model.Attribute.AttributeType;
import org.skyve.metadata.model.Extends;
import org.skyve.metadata.model.document.Bizlet;
import org.skyve.metadata.model.document.Collection;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.model.document.Relation;
import org.skyve.metadata.model.document.UniqueConstraint;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.user.User;
import org.skyve.util.BeanValidator;
import org.skyve.util.BeanVisitor;
import org.skyve.util.Util;
import org.skyve.util.logging.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtil.class);
    private static final Logger BIZLET_LOGGER = Category.BIZLET.logger();

	private ValidationUtil() {
		// no implementation
	}

	/**
	 * Validate a document instance against its metadata.
	 * 
	 * @param document The document to validate.
	 * @param bean The bean to validate.
	 * @param e The exception to populate.
	 */
	public static void validateBeanAgainstDocument(Document document, Bean bean) {
		validateBeanAgainstDocument(CORE.getUser(), document, bean);
	}

	private static void validateBeanAgainstDocument(User user, Document document, Bean bean) {
		Customer customer = user.getCustomer();
		ValidationException e = new ValidationException();
		for (Attribute attribute : document.getAttributes()) {
			validateBeanPropertyAgainstAttribute(user, attribute, bean, e);
		}

		Extends inherits = document.getExtends();
		Document baseDocument = document;
		while (inherits != null) {
			Module baseModule = customer.getModule(baseDocument.getOwningModuleName());
			baseDocument = baseModule.getDocument(customer, inherits.getDocumentName());
			for (Attribute attribute : baseDocument.getAttributes()) {
				validateBeanPropertyAgainstAttribute(user, attribute, bean, e);
			}
			inherits = baseDocument.getExtends();
		}

		if (! e.getMessages().isEmpty()) {
			LOGGER.warn("Validation Failed for bean {}", bean);
			throw e;
		}
	}

	/**
	 * Validate a document attribute against its metadata.
	 * 
	 * @param customer	The current customer for this thread
	 * @param attribute The attribute to validate.
	 * @param bean The bean to validate
	 * @param e The exception to populate
	 */
	@SuppressWarnings("unchecked")
	public static void validateBeanPropertyAgainstAttribute(User user, Attribute attribute, Bean bean, ValidationException e) {
		String binding = attribute.getName();
		AttributeType type = attribute.getAttributeType();
		if (AttributeType.inverseOne.equals(type) || 
				AttributeType.inverseMany.equals(type) ||
				binding.equals(Bean.BIZ_KEY)) {
			return;
		}

		String localisedDisplayName = attribute.getLocalisedDisplayName();
		Converter<?> converter = (attribute instanceof ConvertibleField convertible) ? 
									convertible.getConverterForCustomer(user.getCustomer()) : 
									null;
		Object attributeValue = getAttributeValue(bean, binding);
		if (attribute.isRequired() && (attributeValue == null)) {
			String requiredMessage = attribute.getLocalisedRequiredMessage();
			if (requiredMessage == null) {
				requiredMessage = Util.nullSafeI18n(BeanValidator.VALIDATION_REQUIRED_KEY, localisedDisplayName);
			}
			e.getMessages().add(new Message(binding, requiredMessage));
		}

		if ((converter != null) && (attributeValue != null)) {
			validateFormat(converter.getFormat(), attributeValue, binding, bean, localisedDisplayName, e);
			@SuppressWarnings("rawtypes")
			Validator validator = converter.getValidator();
			if (validator != null) {
				validator.validate(user, attributeValue, binding, localisedDisplayName, converter, e);
			}
		}
		
		if (attribute instanceof Collection collection) {
			int minCardinality = collection.getMinCardinality();
			if (minCardinality > 0) {
				List<Bean> collectionValue = (List<Bean>) attributeValue;
				if ((collectionValue == null) || (collectionValue.size() < minCardinality)) {
					e.getMessages().add(new Message(binding, 
														(minCardinality == 1) ?
															Util.nullSafeI18n(BeanValidator.VALIDATION_COLLECTION_MIN_CARDINALITY_SINGULAR_KEY, localisedDisplayName) :
															Util.nullSafeI18n(BeanValidator.VALIDATION_COLLECTION_MIN_CARDINALITY_PLURAL_KEY, String.valueOf(minCardinality), localisedDisplayName)));
				}
			}
			Integer maxCardinality = collection.getMaxCardinality();
			if (maxCardinality != null) {
				int max = maxCardinality.intValue();
				List<Bean> collectionValue = (List<Bean>) attributeValue;
				if ((collectionValue != null) && (collectionValue.size() > max)) {
					e.getMessages().add(new Message(binding, 
														(max == 1) ?
															Util.nullSafeI18n(BeanValidator.VALIDATION_COLLECTION_MAX_CARDINALITY_SINGULAR_KEY, localisedDisplayName) :
															Util.nullSafeI18n(BeanValidator.VALIDATION_COLLECTION_MAX_CARDINALITY_PLURAL_KEY, String.valueOf(max), localisedDisplayName)));
				}
			}
		}
		else if (attribute instanceof Text text) {
			int fieldLength = text.getLength();
			if (attributeValue instanceof String stringValue) {
				if (stringValue.length() > fieldLength) {
					e.getMessages().add(new Message(binding,
														Util.nullSafeI18n(BeanValidator.VALIDATION_LENGTH_KEY, localisedDisplayName, String.valueOf(fieldLength))));
				}
				TextFormat format = text.getFormat();
				if (format != null) {
					validateFormat(format.getFormat(), stringValue, binding, bean, localisedDisplayName, e);
				}
				TextValidator validator = text.getValidator();
				if (validator != null) {
					validator.validate(user, stringValue, binding, localisedDisplayName, (Converter<String>) converter, e);
				}
			}
		}
		else if (attribute instanceof org.skyve.impl.metadata.model.document.field.Integer integer) {
			IntegerValidator validator = integer.getValidator();
			if ((validator != null) && (attributeValue instanceof Integer integerValue)) {
				validator.validate(user, integerValue, binding, localisedDisplayName, (Converter<Integer>) converter, e);
			}
		}
		else if (attribute instanceof LongInteger integer) {
			LongValidator validator = integer.getValidator();
			if ((validator != null) && (attributeValue instanceof Long longValue)) {
				validator.validate(user, longValue, binding, localisedDisplayName, (Converter<Long>) converter, e);
			}
		}
		else if (attribute instanceof Date date) {
			DateValidator validator = date.getValidator();
			if ((validator != null) && (attributeValue instanceof java.util.Date dateValue)) {
				validator.validate(user,
									dateValue,
									binding,
									localisedDisplayName,
									(Converter<java.util.Date>) converter,
									e);
			}
		}
		else if (attribute instanceof DateTime dateTime) {
			DateValidator validator = dateTime.getValidator();
			if ((validator != null) && (attributeValue instanceof java.util.Date dateValue)) {
				validator.validate(user,
									dateValue,
									binding,
									localisedDisplayName,
									(Converter<java.util.Date>) converter,
									e);
			}
		}
		else if (attribute instanceof Time time) {
			DateValidator validator = time.getValidator();
			if ((validator != null) && (attributeValue instanceof java.util.Date dateValue)) {
				validator.validate(user,
									dateValue,
									binding,
									localisedDisplayName,
									(Converter<java.util.Date>) converter,
									e);
			}
		}
		else if (attribute instanceof Timestamp timestamp) {
			DateValidator validator = timestamp.getValidator();
			if ((validator != null) && (attributeValue instanceof java.util.Date dateValue)) {
				validator.validate(user,
									dateValue,
									binding,
									localisedDisplayName,
									(Converter<java.util.Date>) converter,
									e);
			}
		}
		else if (attribute instanceof Decimal2 decimal) {
			DecimalValidator validator = decimal.getValidator();
			if ((validator != null) && (attributeValue instanceof org.skyve.domain.types.Decimal2 decimalValue)) {
				validator.validate(user,
									decimalValue,
									binding,
									localisedDisplayName,
									(Converter<Decimal>) converter,
									e);
			}
		}
		else if (attribute instanceof Decimal5 decimal) {
			DecimalValidator validator = decimal.getValidator();
			if ((validator != null) && (attributeValue instanceof org.skyve.domain.types.Decimal5 decimalValue)) {
				validator.validate(user,
									decimalValue,
									binding,
									localisedDisplayName,
									(Converter<Decimal>) converter,
									e);
			}
		}
		else if (attribute instanceof Decimal10 decimal) {
			DecimalValidator validator = decimal.getValidator();
			if ((validator != null) && (attributeValue instanceof org.skyve.domain.types.Decimal10 decimalValue)) {
				validator.validate(user,
									decimalValue,
									binding,
									localisedDisplayName,
									(Converter<Decimal>) converter,
									e);
			}
		}
	}

	private static void validateFormat(Format<?> format,
											Object value,
											String binding,
											Bean bean,
											String localisedDisplayName,
											ValidationException e) {
		if (format != null) {
			try {
				@SuppressWarnings({"rawtypes", "unchecked"})
				String display = ((Format) format).toDisplayValue(value);
				Object newValue = format.fromDisplayValue(display);

				// ok, we've passed validation from the 2 calls above...
				// now, only set the newValue back on the bean if it was a reformatted string,
				// otherwise its another type that is just displayed a certain way
				if ((value instanceof String) && (newValue instanceof String)) {
					BindUtil.set(bean, binding, newValue);
				}
			}
			catch (@SuppressWarnings("unused") Exception e1) {
				e.getMessages().add(new Message(binding, 
													Util.nullSafeI18n(BeanValidator.VALIDATION_FORMAT_KEY,
																		localisedDisplayName, (value == null) ? "" : value.toString(),
																		format.getMask())));
			}
		}
	}
	
	private static Object getAttributeValue(Bean bean, String binding) {
		Object result = null;
		try {
			result = BindUtil.get(bean, binding);
		}
		catch (Exception e) {
			e.printStackTrace();
			LOGGER.warn("Validation Failed for bean " + bean);
			throw new ValidationException(new Message(binding, Util.nullSafeI18n(BeanValidator.VALIDATION_ACCESS_KEY)));
		}

		return result;
	}

	/**
	 * Validate a bean against its bizlet .validate().
	 * NB This validation method does NOT recursively validate using bizlets through
	 * the base document hierarchy as the bizlet class should be arranged in such a way as to extend the
	 * bizlet methods required of the base bizlet classes through the standard java extension mechanism.
	 * 
	 * @param bizlet	To validate against
	 * @param bean	To validate
	 */
	public static <T extends Bean> void validateBeanAgainstBizlet(Bizlet<T> bizlet, T bean) {
		ValidationException e = new ValidationException();

		try {
			CustomerImpl internalCustomer = (CustomerImpl) CORE.getUser().getCustomer();
			boolean vetoed = internalCustomer.interceptBeforeValidate(bean, e);
			if (! vetoed) {
				if (UtilImpl.BIZLET_TRACE)
                    BIZLET_LOGGER.info("Entering {}.validate: {}", bizlet.getClass().getName(), bean);
				bizlet.validate(bean, e);
				if (UtilImpl.BIZLET_TRACE) 
                    BIZLET_LOGGER.info("Exiting {}.validate: {}", bizlet.getClass().getName(), bean);
				internalCustomer.interceptAfterValidate(bean, e);
			}
		}
		catch (ValidationException ve) {
			// validation method has thrown a validation exception that is not the e parameter passed in
			if (ve != e) {
				List<Message> messages = ve.getMessages();
				if (! messages.isEmpty()) { // add ve's messages
					e.getMessages().addAll(messages);
				}
			}
			else {
				throw e;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			e.getMessages().add(new Message("An error occurred processing " + 
												bizlet.getClass().getName() +
												".validate() - See stack trace in log"));
		}

		if (! e.getMessages().isEmpty()) {
			LOGGER.warn("Validation Failed for bean {}", bean);
			throw e;
		}
	}

	/**
	 * Updates the bindings in the message 
	 * based on the binding of the validatedBean in relation to the masterBean.
	 * 
	 * @param customer
	 * @param e
	 * @param masterBean
	 * @param validatedBean
	 */
	public static void processMessageBindings(Customer customer,
												final Message e,
												Bean masterBean,
												final Bean validatedBean) {
		Document masterDocument = customer.getModule(masterBean.getBizModule()).getDocument(customer, 
																								masterBean.getBizDocument());

		// find this object within the beanBeingSaved
		new BeanVisitor(true, false) {
			@Override
			protected boolean accept(String binding,
							Document document,
							Document owningDocument,
							Relation owningRelation,
							Bean bean) {
				if (bean == validatedBean) {
					if (! binding.isEmpty()) {
						e.setBindingPrefix(binding + '.');
					}

					return false;
				}

				return true;
			}
		}.visit(masterDocument, masterBean, customer);
	}

	public static void checkCollectionUniqueConstraints(Customer customer, Document document, Bean bean) {
		try {
			for (Attribute attribute : document.getAllAttributes(customer)) {
				if (attribute instanceof Collection collection) {
					String referenceName = attribute.getName();
					for (UniqueConstraint constraint : collection.getUniqueConstraints()) {
						Set<String> uniqueValues = new TreeSet<>();

						@SuppressWarnings("unchecked")
						List<Bean> elements = (List<Bean>) BindUtil.get(bean, referenceName);
						if (elements != null) { // should always be
							for (int i = 0, l = elements.size(); i < l; i++) {
								Bean element = elements.get(i);
								StringBuilder sb = new StringBuilder(128);
	
								for (String fieldName : constraint.getFieldNames()) {
									sb.append(BindUtil.getDisplay(customer, element, fieldName)).append(';');
								}
	
								if (! uniqueValues.add(sb.toString())) { // already exists
									String message = null;
									try {
										message = BindUtil.formatMessage(constraint.getMessage(), element);
									}
									catch (Exception ex) {
										ex.printStackTrace();
										message = "Unique Constraint Violation occurred on collection " + referenceName +
													" but could not display the unique constraint message for constraint " +
													constraint.getName();
									}
	
									throw new UniqueConstraintViolationException(document,
																					constraint.getName(),
																					referenceName + '[' + i + ']',
																					message);
								}
							}
						}
					}
				}
			}
		}
		catch (UniqueConstraintViolationException ve) {
			LOGGER.warn("Validation Failed for bean {}", bean);
			throw ve;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			LOGGER.warn("Validation Failed for bean {}", bean);
			throw new ValidationException(new Message("An error occurred checking collection unique constraints. - See stack trace in log"));
		}
	}
}
