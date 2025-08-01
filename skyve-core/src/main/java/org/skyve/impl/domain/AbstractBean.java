package org.skyve.impl.domain;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.beanutils.LazyDynaMap;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.hibernate.collection.spi.PersistentCollection;
import org.skyve.CORE;
import org.skyve.domain.Bean;
import org.skyve.domain.HierarchicalBean;
import org.skyve.impl.bind.BindUtil;
import org.skyve.impl.persistence.AbstractPersistence;
import org.skyve.impl.util.UtilImpl;
import org.skyve.metadata.MetaDataException;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.Attribute;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.util.Binder.TargetMetaData;
import org.skyve.util.logging.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBean implements Bean {
	private static final long serialVersionUID = -5241897716950549433L;

    private static final Logger DIRTY_LOGGER = Category.DIRTY.logger();

    // LOGGER to use in sub-classes - this is instance scoped for polymorphism and re-instated after deserialzation in readResolve() 
    protected transient Logger LOGGER = LoggerFactory.getLogger(getClass());

	// Holds the old (replaced) values when a setter is called.
	private Map<String, Serializable> originalValues = new TreeMap<>();
	
	// Holds any dynamic attributes - lazily instantiated
	private LazyDynaMap dynamic = null;
	
	/**
	 * Take a copy of the old value before setting a new value.
	 * This method ensures that the value is actually going to change.
	 * 
	 * @param propertyName
	 * @param propertyValue
	 */
	protected final void preset(String propertyName, Serializable propertyValue) {
		try {
			if (! originalValues.containsKey(propertyName)) {
				Serializable oldValue = (Serializable) BindUtil.get(this, propertyName);
				if (oldValue == null) {
					if (propertyValue != null) {
						originalValues.put(propertyName, oldValue);
                        if (UtilImpl.DIRTY_TRACE) {
                            DIRTY_LOGGER.info("AbstractBean.preset(): Bean {} is DIRTY : property {} is now {} from {}", 
                                    			toString(),
                                    			propertyName,
                                    			propertyValue,
                                    			oldValue);
                        }
					}
				}
				else {
					if ((propertyValue == null) || (! oldValue.equals(propertyValue))) {
						originalValues.put(propertyName, oldValue);
						if (UtilImpl.DIRTY_TRACE)  {
                            DIRTY_LOGGER.info("AbstractBean.preset(): Bean {} is DIRTY : property {} is now {} from {}",
                                    			toString(),
                                    			propertyName,
                                    			propertyValue,
                                    			oldValue);
						}
					}
				}
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Could not preset for bean " + this + " property " + propertyName, e);
		}
	}
	
	@Override
	public Map<String, Serializable> originalValues() {
		return originalValues;
	}
	
	@Override
	public boolean isChanged() {
		Customer customer = null;
		Module module = null;
		Document document = null;
		try {
			customer = CORE.getUser().getCustomer();
			if (customer != null) {
				module = customer.getModule(getBizModule());
				if (module != null) {
					document = module.getDocument(customer, getBizDocument());
				}
			}
		} 
		catch (@SuppressWarnings("unused") MetaDataException e) {
			// do nothing - we can continue
		}

		// if this bean is unchanged, check the persistent collections to see if they're dirty
		if (originalValues.isEmpty()) {
			Class<?> type = getClass();
			// Drive off of the bean as it could be an extension class or a domain object that was hand coded.
			for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(type)) {
				Class<?> propertyType = descriptor.getPropertyType();
				// malformed bean property in the code somehow (maybe in the extension class)
				if (propertyType == null) {
					continue;
				}
				if (Collection.class.isAssignableFrom(propertyType)) {
					try {
						String propertyName = descriptor.getName();

						if (HierarchicalBean.class.isAssignableFrom(type) && 
								propertyName.equals("children")) {
							continue;
						}
						
						// Determine if we are tracking changes for this collection
						boolean trackChanges = true;
						if ((customer != null) && (module != null) && (document != null)) {
							try {
								// If this collection is an attribute (could be on an extension object)
								// then check the trackChanges switch, but if it isn't a metadata attribute,
								// treat it as if it's not dirty
								Attribute attribute = null;
								try {
									// NB Check for base documents also
									TargetMetaData target = BindUtil.getMetaDataForBinding(customer, module, document, propertyName);
									attribute = target.getAttribute();
								}
								catch (@SuppressWarnings("unused") MetaDataException e) {
									// nothing to really do here
								}
								if (attribute == null) {
									trackChanges = false; // its an extension attribute, so its not to be tracked
								}
								else {
									trackChanges = attribute.isTrackChanges(); // leave it up to the metadata
								}
							}
							catch (@SuppressWarnings("unused") Exception e) {
								// if we get here, leave trackChanges on
							}
						}

						if (trackChanges) {
							// Note transient collections place their original state in their 
							// owning bean's originalValues which is tested up above first. 
							Object collection = BindUtil.get(this, propertyName);
							if (collection instanceof PersistentCollection persistentCollection) { // persistent
								if (persistentCollection.isDirty()) {
									if (UtilImpl.DIRTY_TRACE) {
                                        DIRTY_LOGGER.info("AbstractBean.isChanged(): Bean {} is DIRTY : persistent collection {} is dirty",
                                                			toString(),
                                                			propertyName);
									}
									return true;
								}
							}
						}
					}
					catch (Exception e) {
						throw new IllegalStateException("Could not determine if a collection is dirty", e);
					}
				}
			}
		}
		else {
            if (UtilImpl.DIRTY_TRACE) {
                DIRTY_LOGGER.info("AbstractBean.isChanged(): Bean {} is DIRTY : originalValues is not empty", toString());
            }
			return true;
		}

		return false;
	}
	
	@Override
	public boolean isNotChanged() {
		return ! isChanged();
	}
	
	@Override
	public boolean hasChanged() {
		Document document = getDocumentMetaData();
		ChangedBeanVisitor cbv = new ChangedBeanVisitor();
		cbv.visit(document, this, CORE.getCustomer());
		return cbv.isChanged();
	}
	
	@SuppressWarnings("static-method")
	protected final <T extends Bean> T getElementById(List<T> list, String elementBizId) {
		return BindUtil.getElementInCollection(list, elementBizId);
	}
	
	@SuppressWarnings("static-method")
	protected final <T extends Bean> void setElementById(List<T> list, T element) {
		BindUtil.setElementInCollection(list, element);
	}

	@Override
	public boolean evaluateCondition(String condition) {
		return BindUtil.evaluateCondition(this, condition);
	}

	public boolean isUserInOwningModuleRole(String roleName) {
		return isUserInRole(getBizModule(), roleName);
	}

	@SuppressWarnings("static-method") // not static because it is used in the generated beans generating warnings
	public boolean isUserInRole(String moduleName, String roleName) {
		return AbstractPersistence.get().getUser().isInRole(moduleName, roleName);
	}

	/**
	 * Determine if the user is in a certain data group
	 * 
	 * @param dataGroupId
	 *            <code>null</code> for admin group or a specific group name.
	 * @return
	 */
	@SuppressWarnings("static-method") // not static because it is used in the generated beans generating warnings
	public boolean isUserInDataGroup(String dataGroupId) {
		String myDataGroupId = AbstractPersistence.get().getUser().getDataGroupId();
		if (dataGroupId == null) { // no data group to check
			return (myDataGroupId == null); // check that user has no data group
		}

		return dataGroupId.equals(myDataGroupId); // check whether in same data group
	}

	@Override
	public boolean isPersisted() {
		return AbstractPersistence.get().isPersisted(this);
	}

	@Override
	public boolean isNotPersisted() {
		return (! AbstractPersistence.get().isPersisted(this));
	}

	/**
	 * Indicates if the bean has been created fully or not. Defaults to true -
	 * that is - fully created upon construction.
	 */
	@Override
	public boolean isCreated() {
		return true;
	}

	/**
	 * Indicates if the bean has been created fully or not. Defaults to false -
	 * that is - fully created upon construction.
	 */
	@Override
	public boolean isNotCreated() {
		return ! isCreated();
	}

	@Override
	public boolean isDynamic(String attributeName) {
		return (dynamic != null) && dynamic.getMap().containsKey(attributeName);
	}

	@Override
	public Object getDynamic(String simpleBinding) {
		if (! isDynamic(simpleBinding)) {
			throw new IllegalArgumentException("Binding does not exist - " + simpleBinding);
		}
		return dynamic.get(simpleBinding);
	}
	
	@Override
	public void setDynamic(String simpleBinding, Object value) {
		if (! isDynamic(simpleBinding)) {
			throw new IllegalArgumentException("Binding does not exist - " + simpleBinding);
		}
		dynamic.set(simpleBinding, value);
	}

	@Override
	public void putDynamic(String simpleBinding, Object value) {
		if (dynamic == null) {
			dynamic = new LazyDynaMap();
		}
		dynamic.set(simpleBinding, value);
	}

	@Override
	public void putAllDynamic(@SuppressWarnings("hiding") Map<String, Object> dynamic) {
		if (dynamic == null) {
			this.dynamic = null;
		}
		else if (this.dynamic == null) {
			this.dynamic = new LazyDynaMap(dynamic);
		}
		else {
			this.dynamic.getMap().putAll(dynamic);
		}
	}
	
	@Override
	public final boolean equals(Object o) {
		return ((o instanceof Bean bean) && 
				this.getBizId().equals(bean.getBizId()));
	}
	
	@Override
	public final int hashCode() {
		return getBizId().hashCode();
	}
	
	/**
	 * Compare this bean to another by bizId.
	 */
	@Override
	public final int compareTo(Bean other) {
		return compareTo(this, other);
	}

	@Override
	public String toString() {
		return super.toString() + '#' + getBizId();
	}

	/**
	 * Compare 2 beans by bizId.
	 */
	public static int compareTo(Bean thisOne, Bean otherOne) {
		if ((thisOne == null) && (otherOne == null)) {
			return 0;
		}
		else if (thisOne == null) {
			return -1;
		}
		else if (otherOne == null) {
			return 1;
		}
		else {
			return thisOne.getBizId().compareTo(otherOne.getBizId());
		}
	}

	/**
	 * Injects any {@link Injected} fields and reinstates the LOGGER after de-serialisation.
	 * @return this
	 * @see BeanProvider#injectFields(Object)
	 */
	protected Object readResolve() {
	    LOGGER = LoggerFactory.getLogger(getClass());
	    UtilImpl.inject(this);
	    return this;
	}
}
