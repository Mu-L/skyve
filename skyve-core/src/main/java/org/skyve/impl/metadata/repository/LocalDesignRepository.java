package org.skyve.impl.metadata.repository;

import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.skyve.domain.Bean;
import org.skyve.domain.types.formatters.Formatter;
import org.skyve.domain.types.formatters.Formatters;
import org.skyve.impl.bind.BindUtil;
import org.skyve.impl.metadata.customer.CustomerImpl;
import org.skyve.impl.metadata.model.document.AbstractInverse;
import org.skyve.impl.metadata.model.document.AbstractInverse.InverseRelationship;
import org.skyve.impl.metadata.model.document.DocumentImpl;
import org.skyve.impl.metadata.model.document.field.Enumeration;
import org.skyve.impl.metadata.model.document.field.Field;
import org.skyve.impl.metadata.module.menu.AbstractDocumentMenuItem;
import org.skyve.impl.metadata.module.menu.AbstractDocumentOrQueryOrModelMenuItem;
import org.skyve.impl.metadata.module.menu.EditItem;
import org.skyve.impl.metadata.module.menu.ListItem;
import org.skyve.impl.metadata.module.menu.MapItem;
import org.skyve.impl.metadata.module.menu.TreeItem;
import org.skyve.impl.metadata.repository.customer.CustomerModuleRoleMetaData;
import org.skyve.impl.metadata.repository.customer.CustomerRoleMetaData;
import org.skyve.impl.metadata.user.ActionPrivilege;
import org.skyve.impl.metadata.user.Privilege;
import org.skyve.impl.metadata.user.RoleImpl;
import org.skyve.impl.metadata.user.UserImpl;
import org.skyve.impl.metadata.view.ViewImpl;
import org.skyve.impl.metadata.view.container.form.FormLabelLayout;
import org.skyve.impl.util.UtilImpl;
import org.skyve.metadata.FormatterName;
import org.skyve.metadata.MetaDataException;
import org.skyve.metadata.Ordering;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.customer.CustomerRole;
import org.skyve.metadata.model.Attribute;
import org.skyve.metadata.model.Attribute.AttributeType;
import org.skyve.metadata.model.Persistent;
import org.skyve.metadata.model.document.Association.AssociationType;
import org.skyve.metadata.model.document.Collection;
import org.skyve.metadata.model.document.Collection.CollectionType;
import org.skyve.metadata.model.document.Condition;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.model.document.Inverse;
import org.skyve.metadata.model.document.Inverse.InverseCardinality;
import org.skyve.metadata.model.document.Reference;
import org.skyve.metadata.model.document.Relation;
import org.skyve.metadata.model.document.UniqueConstraint;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.Module.DocumentRef;
import org.skyve.metadata.module.menu.Menu;
import org.skyve.metadata.module.menu.MenuGroup;
import org.skyve.metadata.module.menu.MenuItem;
import org.skyve.metadata.module.query.MetaDataQueryColumn;
import org.skyve.metadata.module.query.MetaDataQueryDefinition;
import org.skyve.metadata.module.query.MetaDataQueryProjectedColumn;
import org.skyve.metadata.module.query.QueryDefinition;
import org.skyve.metadata.user.Role;
import org.skyve.metadata.user.User;
import org.skyve.metadata.user.UserAccess;
import org.skyve.metadata.view.View;
import org.skyve.metadata.view.View.ViewType;
import org.skyve.metadata.view.model.list.ListModel;
import org.skyve.util.Binder.TargetMetaData;
import org.skyve.util.ExpressionEvaluator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Do not instantiate directly, use CORE.getRepository().
 * 
 * @author Mike
 */
public class LocalDesignRepository extends FileSystemRepository {
	public LocalDesignRepository() {
		super();
	}

	public LocalDesignRepository(@Nonnull String absolutePath) {
		super(absolutePath);
	}

	public LocalDesignRepository(@Nonnull String absolutePath, boolean loadClasses) {
		super(absolutePath, loadClasses);
	}

	@Override
	public boolean getUseScaffoldedViews() {
		return true;
	}
	
	@Override
	public UserImpl retrieveUser(String userPrincipal) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean populatePermissions(User user) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resetUserPermissions(User user) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean populateUser(User user, Connection connection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Bean> retrieveAllJobSchedulesForAllCustomers() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Bean> retrieveAllReportSchedulesForAllCustomers() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String retrievePublicUserName(String customerName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void resetMenus(User user) {
		UserImpl internalUser = (UserImpl) user;
		for (Module module : user.getCustomer().getModules()) {
			Menu menu = UtilImpl.cloneBySerialization(module.getMenu());
			removeInaccessibleItems(module.getName(), menu, user);
			internalUser.putModuleMenu(module, menu);
		}
	}

	private static void removeInaccessibleItems(@Nonnull String moduleName, @Nonnull Menu menu, @Nonnull User user) {
		// Check all the child items to see if we have access
		Iterator<MenuItem> i = menu.getItems().iterator();
		while (i.hasNext()) {
			MenuItem menuItem = i.next();

			// If we are dealing with a menu group, recurse the check to its menu items
			if (menuItem instanceof Menu menuGroup) {
				removeInaccessibleItems(moduleName, menuGroup, user);

				// If there are no menu items left, remove the menu group as well.
				if (menuGroup.getItems().isEmpty()) {
					i.remove();
				}
			}
			else { // we are dealing with menu items (not a group)
				// item is secured by at least 1 role
				boolean secureMenuItem = (! menuItem.getRoleNames().isEmpty());

				// if not a secured item then it is automatically accessible
				boolean accessibleMenuItem = (! secureMenuItem);

				if (! accessibleMenuItem) {
					// check for a role name in the menu item that the user has permissions to.
					for (String roleName : menuItem.getRoleNames()) {
						if (user.isInRole(moduleName, roleName)) {
							accessibleMenuItem = true;
							break;
						}
					}
				}

				// Remove the menu item if it is not accessible,
				if (! accessibleMenuItem) {
					i.remove();
				}
			}
		}
	}

	@Override
	public void validateCustomerForGenerateDomain(Customer customer) {
		try {
			Module homeModule = customer.getHomeModule();
			if (homeModule == null) {
				throw new MetaDataException("Repository returned null for [homeModule] in customer " + customer.getName());
			}
		}
		catch (MetaDataException e) {
			throw new MetaDataException("Home Module reference does not reference a module in customer " + customer.getName(), e);
		}

		CustomerImpl customerImpl = (CustomerImpl) customer;
		
		// NB Module entry names (keys) are in defined order
		for (Entry<String, FormLabelLayout> moduleEntry : customerImpl.getModuleEntries().entrySet()) {
			String moduleName = moduleEntry.getKey();
			FormLabelLayout formLabelLayout = moduleEntry.getValue();

			Module module = null;
			try {
				module = getModule(customer, moduleName);
				if (module == null) {
					throw new MetaDataException("Repository returned null for " + moduleName + 
													" for customer " + customer.getName());
				}
			}
			catch (MetaDataException e) {
				throw new MetaDataException("Module reference " + moduleName + 
												" does not reference a module in customer " + customer.getName(), e);
			}

			// Catch where we can't convert from top defined to side rendered
			// NB null = side (the default)
			if ((formLabelLayout != FormLabelLayout.top) && (module.getFormLabelLayout() == FormLabelLayout.top)) {
				throw new MetaDataException("Module reference " + moduleName + " for customer " + customer.getName() +
												" has a layout of side but the module is defined with a layout of top and cannot be converted");
			}
		}
		
		// Validate the role metadata module roles point to valid module roles
		// NB We don't need to check the module name of the role as this is checked when the metadata
		// is converted and we know all module names are correct from the validation performed above.
		for (CustomerRole role : customer.getRoles()) {
			for (CustomerModuleRoleMetaData moduleRole : ((CustomerRoleMetaData) role).getRoles()) {
				String moduleName = moduleRole.getModuleName();
				Module module = getModule(customer, moduleName);
				if (module.getRole(moduleRole.getName()) == null) {
					throw new MetaDataException("Module role " + moduleRole.getName() + 
													" for module " + moduleName +
													" for customer role " + role.getName() +
													" in customer " + customer.getName() +
													" does not reference a valid module role");
				}
			}
		}
		
		// Validate text search roles point to valid module roles
		validateFeatureRoles(customer, customerImpl.getTextSearchRoles());
		
		// Validate flag roles point to valid module roles
		validateFeatureRoles(customer, customerImpl.getFlagRoles());
		
		// Validate switch mode roles point to valid module roles
		validateFeatureRoles(customer, customerImpl.getSwitchModeRoles());
		
		// TODO check the converter type corresponds to the type required.
	}

	@Override
	public void validateModuleForGenerateDomain(Customer customer, Module module) {
		// if home document is transient then home ref had better be edit
		String homeDocumentName = module.getHomeDocumentName();
		if (homeDocumentName != null) {
			Document homeDocument = module.getDocument(customer, homeDocumentName);
			if ((homeDocument.getPersistent() == null) && (! ViewType.edit.equals(module.getHomeRef()))) { // is transient but not edit
				throw new MetaDataException("Home document " + homeDocumentName + 
												" for customer " + customer.getName() + 
												" in module " + module.getName() +
												" is transient and therefore the module requires a homeRef of 'edit'.");
			}
		}

		// check query columns
		for (QueryDefinition query : module.getMetadataQueries()) {
			if (query instanceof MetaDataQueryDefinition documentQuery) {
				Module queryDocumentModule = documentQuery.getDocumentModule(customer);
				Document queryDocument = queryDocumentModule.getDocument(customer, documentQuery.getDocumentName());
				for (MetaDataQueryColumn column : documentQuery.getColumns()) {
					String binding = column.getBinding();
					if (binding != null) {
						// Check that the column binding is valid/exists
						TargetMetaData target = null;
						try {
							target = BindUtil.getMetaDataForBinding(customer, 
																		queryDocumentModule,
																		queryDocument,
																		binding);
						}
						catch (MetaDataException e) {
							throw new MetaDataException("Query " + query.getName() + 
															" in module " + query.getOwningModule().getName() +
															" with column binding " + binding +
															" is not a valid binding.", e);
						}

						final Document targetDocument = target.getDocument();
						final Attribute targetAttribute = target.getAttribute();

						MetaDataQueryProjectedColumn projectedColumn = (column instanceof MetaDataQueryProjectedColumn pc) ? pc : null;

						// Check that non-persistent column bindings are not sortable, filterable or editable
						if ((projectedColumn != null) && 
								(projectedColumn.isSortable() || projectedColumn.isFilterable() || projectedColumn.isEditable())) {
							boolean nonPersistent = false;
							// target document is non-persistent and not a mapped document
							if (! targetDocument.isPersistable()) {
								nonPersistent = true;
							}
							// target attribute is a non-persistent non-implicit attribute
							else if ((targetAttribute != null) && 
										(! BindUtil.isImplicit(targetAttribute.getName())) &&
										(! targetAttribute.isPersistent())) {
								nonPersistent = true;
							}
							else {
								// Test for a compound binding and any of the interim relations or documents along the way are not persistent
								int lastDotIndex = binding.lastIndexOf('.');
								if (lastDotIndex > 0) { // compound
									Module owningModule = queryDocumentModule;
									Document owningDocument = queryDocument;
									
									// Tokenise the relation bindings up to the ultimate binding (which we know is OK from the above tests)
									StringTokenizer tokenizer = new StringTokenizer(binding.substring(0, lastDotIndex), ".");
									while (tokenizer.hasMoreTokens()) {
										// Test if document is not persistent or mapped
										// Note that mapped is allowed through an interim binding
										if (owningDocument.getPersistent() == null) {
											nonPersistent = true;
											break;
										}

										// Check if relation is not persistent
										String relationBindingPart = tokenizer.nextToken();
										TargetMetaData relationTarget = BindUtil.getMetaDataForBinding(customer, owningModule, owningDocument, relationBindingPart);
										Relation relationAttribute = (Relation) relationTarget.getAttribute();
										// Attribute could be null if 'parent' binding part is used in query
										if (relationAttribute == null) { // "parent" binding
											owningDocument = relationTarget.getDocument().getParentDocument(customer);
											if (owningDocument == null) {
												throw new MetaDataException("Query " + query.getName() + 
																				" in module " + query.getOwningModule().getName() +
																				" with column binding " + binding +
																				" is not a valid binding.");
											}
										}
										else { // relation binding
											if (! relationAttribute.isPersistent()) {
												nonPersistent = true;
												break;
											}
											owningDocument = owningModule.getDocument(customer, relationAttribute.getDocumentName());
										}
										owningModule = customer.getModule(owningDocument.getOwningModuleName());
									}
								}
							}

							if (nonPersistent) {
								throw new MetaDataException("Query " + query.getName() + 
																" in module " + query.getOwningModule().getName() +
																" with column binding " + binding +
																" references a transient (or mapped) attribute and should not be sortable, filterable or editable.");
							}
						}
						
						// Check that the formatter or customerFormatter are compatible if defined
						if ((projectedColumn != null) && (targetAttribute != null)) {
							AttributeType targetAttributeType = targetAttribute.getAttributeType();
							FormatterName formatterName = projectedColumn.getFormatterName();
							if (formatterName != null) {
								// Check any implicit formatter is compatible with the column attribute type
								Class<?> targetAttributeImplementingType = targetAttribute.getImplementingType();
								if (! formatterName.getFormatter().getValueType().isAssignableFrom(targetAttributeImplementingType)) {
									throw new MetaDataException("Query " + query.getName() + 
																" in module " + query.getOwningModule().getName() +
																" with column binding " + binding +
																" has formatter " + formatterName.name() + 
																" for type " + formatterName.getFormatter().getValueType() + 
																" but the column binding is to attribute type " + targetAttributeType + 
																" of incompatible type " + targetAttributeImplementingType);
								}
							}
							String customFormatterName = projectedColumn.getCustomFormatterName();
							if (customFormatterName != null) {
								// Check any custom formatter is compatible with the column attribute type
								// NB Formatter existence checked in ModuleMetaData.convert()
								Formatter<?> formatter = Formatters.get(customFormatterName);
								Class<?> targetAttributeImplementingType = targetAttribute.getImplementingType();
								if ((formatter != null) && 
										(! formatter.getValueType().isAssignableFrom(targetAttributeImplementingType))) {
									throw new MetaDataException("Query " + query.getName() + 
																" in module " + query.getOwningModule().getName() +
																" with column binding " + binding +
																" has formatter " + customFormatterName + 
																" for type " + formatter.getValueType() + 
																" but the column binding is to attribute type " + targetAttributeType + 
																" of incompatible type " + targetAttributeImplementingType);
								}
							}
						}

						// Customer overridden documents that are used in metadata queries cause an error unless 
						// <association>.bizId is used as the binding.
						if ((targetAttribute != null) && 
								AttributeType.association.equals(targetAttribute.getAttributeType()) &&
								(column.getFilterOperator() != null)) {
							throw new MetaDataException("Query " + query.getName() + 
															" in module " + query.getOwningModule().getName() +
															" with column binding " + binding +
															" references an association which has a column filter defined.  Use [" + 
															binding + ".bizId] as the binding for the column.");
						}
					}
				}
			}
		}
		
		// check menu items
		checkMenu(module.getMenu().getItems(), customer, module);

		// check action privilege references an action in the given document view
		for (Role role : module.getRoles()) {
			RoleImpl roleImpl = (RoleImpl) role;
			for (Privilege privilege : roleImpl.getPrivileges()) {
				if (privilege instanceof ActionPrivilege actionPrivilege) {
					String actionPrivilegeName = actionPrivilege.getName();
					Document actionDocument = module.getDocument(customer, actionPrivilege.getDocumentName());
					if (getClassAction(customer, actionDocument, actionPrivilegeName, false, false) == null) {
						if (getMetaDataAction(customer, actionDocument, actionPrivilegeName) == null) {
							throw new MetaDataException("Action privilege " + actionPrivilege.getName() + 
															" for customer " + customer.getName() + 
															" in module " + module.getName() +
															" for document " + actionDocument.getName() + 
															" for role " + role.getName() +
															" does not reference a valid action");
						}
					}
				}
			}
			
			// Check modelAggregate and previousComplete UserAccesses
			for (UserAccess access : roleImpl.getAccesses().keySet()) {
				if (access.isModelAggregate()) {
					Document accessDocument = module.getDocument(customer, access.getDocumentName());
					try {
						getModel(customer, accessDocument, access.getComponent(), false);
					}
					catch (Exception e) {
						throw new MetaDataException("User Access [" + access.toString() + 
														"] in module " + module.getName() +
														" is for model " + access.getComponent() +
														" which does not exist.",
														e);
					}
				}
				else if (access.isDynamicImage()) {
					Document accessDocument = module.getDocument(customer, access.getDocumentName());
					try {
						getDynamicImage(customer, accessDocument, access.getComponent(), false);
					}
					catch (Exception e) {
						throw new MetaDataException("User Access [" + access.toString() + 
														"] in module " + module.getName() +
														" is for dynamic image " + access.getComponent() +
														" which does not exist.",
														e);
					}
				}
				else if (access.isPreviousComplete() || access.isContent()) {
					Document accessDocument = module.getDocument(customer, access.getDocumentName());
					String binding = access.getComponent();
					try {
						BindUtil.getMetaDataForBinding(customer, module, accessDocument, binding);
					}
					catch (MetaDataException e) {
						throw new MetaDataException("User Access [" + access.toString() + 
														"] in module " + module.getName() +
														" with binding " + binding +
														" is not a valid binding.", e);
					}
				}
				else if (access.isReport()) {
					String reportModuleName = access.getModuleName();
					String reportDocumentName = access.getDocumentName();
					String reportName = access.getComponent();
					Document reportDocument = customer.getModule(reportModuleName).getDocument(customer, reportDocumentName);
					if (getReportFileName(customer, reportDocument, reportName) == null) { // not found
						throw new MetaDataException("User Access [" + access.toString() + 
														"] in module " + module.getName() +
														" for module/document/report " + reportModuleName + "/" +
														reportDocumentName + "/" + reportName +
														" does not exist.");
					}
				}
			}
		}
	}

	private void checkMenu(@Nonnull List<MenuItem> items, @Nullable Customer customer, @Nonnull Module module) {
		for (MenuItem item : items) {
			if (item instanceof MenuGroup group) {
				checkMenu(group.getItems(), customer, module);
			}
			else {
				if (item instanceof AbstractDocumentMenuItem documentItem) {
					String documentName = documentItem.getDocumentName();
					Document document = null;
					if (documentName != null) {
						try {
							document = module.getDocument(customer, documentName);
						}
						catch (Exception e) {
							throw new MetaDataException("Menu [" + item.getName() + 
															"] in module " + module.getName() +
															" is for document " + documentName +
															" which does not exist.", e);
						}
						// NB Only EditItems or ListModel ListItems can be to a transient document
						if (document.getPersistent() == null) { // non-persistent document
							boolean listModelItem = false;
							if (item instanceof AbstractDocumentOrQueryOrModelMenuItem dataItem) {
								listModelItem = (dataItem.getQueryName() == null) && (dataItem.getModelName() != null);
							}
							boolean editItem = (item instanceof EditItem);
							if (! (listModelItem || editItem)) {
								throw new MetaDataException("Menu [" + item.getName() + 
																"] in module " + module.getName() +
																" is for document " + documentName +
																" which is not persistent.");
							}
						}
					}

					if (item instanceof AbstractDocumentOrQueryOrModelMenuItem dataItem) {
						String queryName = dataItem.getQueryName();
						MetaDataQueryDefinition query = null;
						if (queryName != null) {
							query = module.getMetaDataQuery(queryName);
							if (query == null) {
								throw new MetaDataException("Menu [" + item.getName() + 
																"] in module " + module.getName() +
																" is for query " + queryName +
																" which does not exist.");
							}
							documentName = query.getDocumentName();
							document = module.getDocument(customer, documentName);
						}
						
						String modelName = dataItem.getModelName();
						if (modelName != null) {
							try {
								if (item instanceof ListItem) { // includes TreeItem
									ListModel<Bean> model = getListModel(customer, document, modelName, false);
									// Check driving document can be obtained to ensure bindings and accesses can be calculated
									model.getDrivingDocument();
								}
								else if (item instanceof MapItem) {
									getMapModel(customer, document, modelName, false);
								}
							}
							catch (Exception e) {
								throw new MetaDataException("Menu [" + item.getName() + 
																"] in module " + module.getName() +
																" is for model " + documentName + '.' + modelName +
																" which does not exist or cannot be instantiated.",
																e);
							}
						}
						
						if (item instanceof TreeItem) {
							// Not a model, then its a query or document so check the document is hierarchical
							if ((modelName == null) && (documentName != null) && (document != null)) {
								if (! documentName.equals(document.getParentDocumentName())) {
									throw new MetaDataException("Tree Menu [" + item.getName() + 
																	"] in module " + module.getName() + 
																	" is for document " + document.getName() + 
																	" which is not hierarchical.");
								}
							}
						}
						else if (item instanceof MapItem mapItem) {
							if (document != null) {
								// Check binding is valid
								String binding = mapItem.getGeometryBinding();
								try {
									TargetMetaData target = BindUtil.getMetaDataForBinding(customer, module, document, binding);
									Attribute attribute = target.getAttribute();
									if ((attribute == null) || 
											(! AttributeType.geometry.equals(attribute.getAttributeType()))) {
										throw new MetaDataException("Map Menu [" + item.getName() + 
																		"] in module " + module.getName() + 
																		" has a geometryBinding of " + binding + 
																		" which is not a geometry.");
									}
								}
								catch (Exception e) {
									throw new MetaDataException("Map Menu [" + item.getName() + 
																	"] in module " + module.getName() + 
																	" has a geometryBinding of " + binding + 
																	" which does not exist.",
																	e);
								}
								
								// If the query is defined and not polymorphic, check that the binding is in the query.
								if ((queryName == null) && (documentName != null)) { // default document query
									query = module.getDocumentDefaultQuery(customer, documentName);
								}
								if (query != null) {
									if (! Boolean.TRUE.equals(query.getPolymorphic())) {
										if (query.getColumns().stream().noneMatch(c -> ((c instanceof MetaDataQueryProjectedColumn projected) && binding.equals(projected.getBinding())))) {
											throw new MetaDataException("Map Menu [" + item.getName() + 
																			"] in module " + module.getName() + 
																			" has a geometryBinding of " + binding + 
																			" which is not a column in the " + 
																			((queryName == null) ? "default query" : "query " + queryName) + 
																			". Either add the column (preferable) to the query or set the query to be polymorphic.");
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void validateDocumentForGenerateDomain(Customer customer, Document document) {
		String documentIdentifier = document.getOwningModuleName() + '.' + document.getName();
		Module module = getModule(customer, document.getOwningModuleName());

		// Check conditions
		for (String conditionName : document.getConditionNames()) {
			// Check expression conditions
			Condition condition = document.getCondition(conditionName);
			String expression = condition.getExpression();
			if (BindUtil.isSkyveExpression(expression)) {
				String error = ExpressionEvaluator.validate(expression,
																Boolean.class,
																customer,
																module,
																document);
				if (error != null) {
					throw new MetaDataException("Condition " + conditionName + " in document " + documentIdentifier + " with expression " + expression + " has an error : " + error);
				}
			}
		}
		
		// Check the bizKey expression, if defined
		String bizKeyExpression = document.getBizKeyExpression();
		if (bizKeyExpression != null) {
			String error = BindUtil.validateMessageExpressions(bizKeyExpression, customer, document);
			if (error != null) {
				throw new MetaDataException("The biz key [expression] defined contains malformed binding expressions in document " + documentIdentifier + ": " + error);
			}
		}
		
		// If document has a parentDocument defined, ensure that it exists in the document's module.
		try {
			document.getParentDocument(customer);
		}
		catch (@SuppressWarnings("unused") MetaDataException e) {
			throw new MetaDataException("The document " + documentIdentifier + 
											" has a parent document of " +
											document.getParentDocumentName() + " that does not exist in this module.");
		}
		
		// Check Persistent name and Strategy="mapped" do not coexist in a Document as they conflict
		Persistent persistent = document.getPersistent();
		if ((persistent != null) &&
				Persistent.ExtensionStrategy.mapped.equals(persistent.getStrategy()) &&
				(persistent.getName() != null)) {
			throw new MetaDataException(documentIdentifier + " can not have a persistent name and a mapped strategy" +
											" - all inherited attributes will be persisted as columns in tables for each subtype document.");
		}

		// NOTE - Persistent etc is checked when generating documents as it is dependent on the hierarchy and persistence strategy etc

		// Check attributes
		for (Attribute attribute : document.getAttributes()) {
			// TODO for all fields that hasDomain is true, ensure that a bizlet exists and it returns domain values (collection length not zero)
			// TODO for all composition collections (ie reference a document that has a parentDocument = to this one) - no queryName is defined on the collection.
			// TODO for all aggregation collections (ie reference a document that has does not have a parentDocument = to this one {or parentDocument is not defined}) - a queryName must be defined on the collection.

			if (attribute instanceof Field field) {
				// Check the default value expressions, if defined
				String defaultValue = field.getDefaultValue();
				if (defaultValue != null) {
					// We can get a MetaDataException out of this when an enumeration that have not been generated previously is asked for its type
					// In this case, we can only validate so far as we don;t have the Java type
					Class<?> type = null;
					try {
						type = attribute.getImplementingType();
					}
					catch (MetaDataException e) { // thrown when enumerations haven't been generated yet
						if (! (attribute instanceof Enumeration)) {
							throw e;
						}
					}

					if (String.class.equals(type)) {
						if (BindUtil.containsSkyveExpressions(defaultValue)) {
							String error = BindUtil.validateMessageExpressions(defaultValue, customer, document);
							if (error != null) {
								throw new MetaDataException("The default value " + defaultValue + " is not a valid expression for attribute " + 
																module.getName() + '.' + document.getName() + '.' + attribute.getName() + ": " + error);
							}
						}
						// NB nothing to do here as its a string already
					}
					else {
						if (BindUtil.isSkyveExpression(defaultValue)) {
							String error = ExpressionEvaluator.validate(defaultValue, type, customer, module, document);
							if (error != null) {
								throw new MetaDataException("The default value " + defaultValue + " is not a valid expression for attribute " + 
																module.getName() + '.' + document.getName() + '.' + attribute.getName() + ": " + error);
							}
						}
						else {
							// Don't test enumerations that have not been generated previously - their default value should be the enumeration name constant.
							// If it is wrong then its a compile time error, and we don't wanna depend on the generated class to validate - chicken and egg.
							if (type != null) {
								try {
									BindUtil.fromSerialised(type, defaultValue);
								} 
								catch (Exception e) {
									throw new MetaDataException("The default value " + defaultValue + " for attribute " + 
																	module.getName() + '.' + document.getName() + '.' + attribute.getName() + " is not coercible to type " + type + 
																	".  Date based types should be expressed as a standard XML date format - YYYY-MM-DD or YYYY-MM-DDTHH24:MM:SS", e);
								}
							}
						}
					}
				}
			}
			else if (attribute instanceof Reference reference) {
				// Check the document points to a document that exists within this module
				String targetDocumentName = reference.getDocumentName();
				DocumentRef targetDocumentRef = module.getDocumentRefs().get(targetDocumentName);
				if (targetDocumentRef == null) {
					throw new MetaDataException("The target [documentName] of " + 
													targetDocumentName + " in Reference " +
													reference.getName() + " in document " + 
													documentIdentifier + " is not a valid document reference in this module.");
				}
				Document targetDocument = module.getDocument(customer, targetDocumentName);
				if (targetDocument == null) {
					throw new MetaDataException("The target [documentName] of " + 
													targetDocumentName + " in Reference " +
													reference.getName() + " in document " + 
													documentIdentifier + " cannot be found.");
				}
				
				// Check the query (if defined) points to a query of the required document type
				String queryName = reference.getQueryName();
				if (queryName != null) {
					MetaDataQueryDefinition query = module.getMetaDataQuery(queryName);
					if (query == null) {
						throw new MetaDataException("The target [queryName] of " + 
														queryName + " in Reference " +
														reference.getName() + " in document " + 
														documentIdentifier + " is not a valid document query in this module.");
					}
					
					String queryDocumentName = query.getDocumentName();
					if (! targetDocumentName.equals(queryDocumentName)) {
						throw new MetaDataException("The target [queryName] of " + 
														queryName + " in Reference " +
														reference.getName() + " in document " + 
														documentIdentifier + " references a document query for document " + 
														queryDocumentName + ", not document " + targetDocumentName);
					}
				}
				
				// Check collection order by bindings are valid
				if (reference instanceof Collection collection) {
					Module targetModule = getModule(customer, targetDocument.getOwningModuleName());
					for (Ordering ordering : collection.getOrdering()) {
						String by = ordering.getBy();
						TargetMetaData target = null; 
						try {
							target = BindUtil.validateBinding(customer, targetModule, targetDocument, by);
						}
						catch (MetaDataException e) {
							throw new MetaDataException("The order by binding of " + by + " in collection " + collection.getName() +
															" in document " +  documentIdentifier + " is invalid", e);
						}
						if (! BindUtil.isAScalarType(target.getType())) {
							throw new MetaDataException("The order by binding of " + by + " in collection " + collection.getName() +
															" in document " +  documentIdentifier + " is not scalar.");
						}
					}
				}
				
				boolean dynamicDocument = document.isDynamic();
				boolean dynamicTargetDocument = targetDocument.isDynamic();
				
				// Disallow a dynamic embedded association to a static document (can't save it in hibernate without a static owner)
				if (dynamicDocument && (! dynamicTargetDocument) && (reference.getType() == AssociationType.embedded)) {
					throw new MetaDataException("The dynamic embedded association " + reference.getName() + 
													" in document " + documentIdentifier + " references document " +
													targetDocumentName + " which is not a dynamic document. Dynamic embedded associations to static documents are not permitted.");
				}

				// Disallow a dynamic child collection to a static document (can't save it in hibernate without a static owner)
				if (dynamicDocument && (! dynamicTargetDocument) && (reference.getType() == CollectionType.child)) {
					throw new MetaDataException("The dynamic child collection " + reference.getName() + 
													" in document " + documentIdentifier + " references document " +
													targetDocumentName + " which is not a dynamic document. Dynamic child collections to static documents are not permitted.");
				}
			}
			else if (attribute instanceof Inverse) {
				// Check that the document name and reference name point to a reference
				AbstractInverse inverse = (AbstractInverse) attribute;
				String targetDocumentName = inverse.getDocumentName();
				DocumentRef inverseDocumentRef = module.getDocumentRefs().get(targetDocumentName);
				if (inverseDocumentRef == null) {
					throw new MetaDataException("The target [documentName] of " + 
													targetDocumentName + " in Inverse " +
													inverse.getName() + " in document " + 
													documentIdentifier + " is not a valid document reference in this module.");
				}
				Module targetModule = module;
				String targetModuleName = inverseDocumentRef.getReferencedModuleName();
				if (targetModuleName != null) {
					targetModule = getModule(customer, targetModuleName);
				}
				Document targetDocument = getDocument(customer, targetModule, targetDocumentName);

				String targetReferenceName = inverse.getReferenceName();
				Reference targetReference = targetDocument.getReferenceByName(targetReferenceName);
				if (targetReference == null) {
					throw new MetaDataException("The target [referenceName] of " + 
													targetReferenceName + " in Inverse " +
													inverse.getName() + " in document " + 
													documentIdentifier + " is not a valid reference within the document " + 
													targetModule.getName() + '.' + targetDocumentName);
				}
				boolean one = InverseCardinality.one.equals(inverse.getCardinality());
				if (targetReference instanceof Collection collection) {
					if (one) {
						throw new MetaDataException("The target [referenceName] of " + 
														targetReferenceName + " in Inverse " +
														inverse.getName() + " in document " + 
														documentIdentifier + " points to a valid collection within the document " + 
														targetModule.getName() + '.' + targetDocumentName + 
														" but the cardinality of the inverse is set to one.");
					}
					if (CollectionType.child.equals(collection.getType())) {
						throw new MetaDataException("The target [referenceName] of " + 
														targetReferenceName + " in Inverse " +
														inverse.getName() + " in document " + 
														documentIdentifier + " points to a valid collection within the document " + 
														targetModule.getName() + '.' + targetDocumentName + 
														" but the collection is a child collection.  The [parent] attribute should be used instead of an inverse reference.");
					}
				}
				inverse.setRelationship((targetReference instanceof Collection) ?
											InverseRelationship.manyToMany :
											(one ? InverseRelationship.oneToOne : InverseRelationship.oneToMany));
			}
		}
		
		// Check the message binding expressions, if present
		List<UniqueConstraint> constraints = document.getUniqueConstraints();
		for (UniqueConstraint constraint : constraints) {
			String message = constraint.getMessage();
			String error = BindUtil.validateMessageExpressions(message, customer, document);
			if (error != null) {
				throw new MetaDataException("The unique constraint [message] contains malformed binding expressions in constraint " +
												constraint.getName() + " in document " + documentIdentifier + ": " + error);
			}
		}
		// TODO check binding in uniqueConstraint.fieldReference.ref as above and ensure the binding is persistent
	}

	@Override
	public void validateViewForGenerateDomain(Customer customer, Document document, View view, String uxui) {
		CustomerImpl customerImpl = (CustomerImpl) customer;
		ViewImpl viewImpl = (ViewImpl) view;
		new ViewValidator(viewImpl, this, customerImpl, (DocumentImpl) document, uxui).visit();
		
		// Check modelAggregate and previousComplete UserAccesses
		Set<UserAccess> accesses = viewImpl.getAccesses(customerImpl, document, uxui);
		if (accesses != null) { // can be null if access control is turned off
			for (UserAccess access : accesses) {
				if (access.isModelAggregate()) {
					try {
						getModel(customer, document, access.getComponent(), false);
					}
					catch (Exception e) {
						throw new MetaDataException("User Access [" + access.toString() + 
														"] in module.document " + document.getOwningModuleName() + '.' + document.getName() +
														" in view " + view.getName() +
														" is for model " + access.getComponent() +
														" which does not exist.",
														e);
					}
				}
				else if (access.isDynamicImage()) {
					try {
						getDynamicImage(customer, document, access.getComponent(), false);
					}
					catch (Exception e) {
						throw new MetaDataException("User Access [" + access.toString() + 
														"] in module.document " + document.getOwningModuleName() + '.' + document.getName() +
														" in view " + view.getName() +
														" is for dynamic image " + access.getComponent() +
														" which does not exist.",
														e);
					}
				}
				else if (access.isPreviousComplete() || access.isContent()) {
					final Module module = getModule(customer, document.getOwningModuleName());
					final String binding = access.getComponent();
					try {
						BindUtil.getMetaDataForBinding(customer, module, document, binding);
					}
					catch (MetaDataException e) {
						throw new MetaDataException("User Access [" + access.toString() + 
														"] in module.document " + module.getName() + '.' + document.getName() +
														" in view " + view.getName() +
														" with binding " + binding +
														" is not a valid binding.", e);
					}
				}
				else if (access.isReport()) {
					String reportModuleName = access.getModuleName();
					String reportDocumentName = access.getDocumentName();
					String reportName = access.getComponent();
					Document reportDocument = customer.getModule(reportModuleName).getDocument(customer, reportDocumentName);
					if (getReportFileName(customer, reportDocument, reportName) == null) { // not found
						final Module module = getModule(customer, document.getOwningModuleName());
						throw new MetaDataException("User Access [" + access.toString() + 
														"] in module.document " + module.getName() + '.' + document.getName() +
														" in view " + view.getName() +
														" for module/document/report " + reportModuleName + "/" +
														reportDocumentName + "/" + reportName +
														" does not exist.");
					}
				}
			}
		}
	}
	
	/**
	 * Validates feature roles point to valid module roles.
	 * 
	 * We don't need to check the module name of the role as this is checked when the metadata 
	 * is converted and we know all module names are correct from the validation performed prior.
	 * 
	 * @param customer
	 * @param featureRoles
	 */
	private void validateFeatureRoles(Customer customer, Set<String> featureRoles) {
		for (String textSearchModuleRole : featureRoles) {
			String[] moduleAndRoleName = textSearchModuleRole.split("\\.");
			String moduleName = moduleAndRoleName[0];
			Module module = getModule(customer, moduleName);
			String roleName = moduleAndRoleName[1];
			if (module.getRole(roleName) == null) {
				throw new MetaDataException("Module role " + roleName + 
						" for module " + moduleName +
						" does not reference a valid module role");
			}
		}
	}
}
