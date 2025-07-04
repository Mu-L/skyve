package org.skyve.impl.metadata.user;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.skyve.CORE;
import org.skyve.impl.metadata.customer.CustomerImpl;
import org.skyve.impl.metadata.module.menu.AbstractDocumentOrQueryOrModelMenuItem;
import org.skyve.impl.metadata.module.menu.EditItem;
import org.skyve.impl.metadata.repository.router.Router;
import org.skyve.impl.metadata.repository.router.UxUiMetadata;
import org.skyve.impl.metadata.view.ViewImpl;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.Module.DocumentRef;
import org.skyve.metadata.module.menu.Menu;
import org.skyve.metadata.module.menu.MenuGroup;
import org.skyve.metadata.module.menu.MenuItem;
import org.skyve.metadata.module.query.MetaDataQueryDefinition;
import org.skyve.metadata.user.Role;
import org.skyve.metadata.user.User;
import org.skyve.metadata.user.UserAccess;
import org.skyve.metadata.view.View.ViewType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

class AccessProcessor {
	private User user;
	private CustomerImpl customer;
	private Map<String, Menu> moduleMenuMap;
	private Map<String, Set<String>> accesses;
	private Router router;

	AccessProcessor(User user, Map<String, Menu> moduleMenuMap, Map<String, Set<String>> accesses) {
		this.user = user;
		this.customer = (CustomerImpl) user.getCustomer(); 
		this.moduleMenuMap = moduleMenuMap;
		this.accesses = accesses;
		router = CORE.getRepository().getRouter();
	}
	
	/**
	 * This method determines the accesses allowed.
	 */
	void process() {
		for (Entry<String, Menu> entry : moduleMenuMap.entrySet()) {
			final Menu menu = entry.getValue();
			final List<MenuItem> menuItems = menu.getItems();

			final String moduleName = entry.getKey();
			final Module module = customer.getModule(moduleName);
			
			// Determine the home URL and fan out from the menu items only if the user has menu access
			if (! menuItems.isEmpty()) {
				processModuleHome(module, moduleName);
				processMenuItems(menuItems, module, moduleName);
			}

			// Process the roles in each module so that any role based accesses are added 
			processRoles(module, moduleName);
		}
		processedUxUiViews.clear();
//for (String a : accesses.keySet()) {
//	System.out.println(a);
//	for (String u : accesses.get(a)) {
//		System.out.println(" u=" + u);
//	}
//}
	}

	private Set<String> processedUxUiViews = new TreeSet<>();

	private void processModuleHome(final Module module, final String moduleName) {
		String homeDocumentName = module.getHomeDocumentName();
		if (homeDocumentName != null) {
			ViewType homeRef = module.getHomeRef();
			if (homeRef == ViewType.list) {
				DocumentRef ref = module.getDocumentRefs().get(homeDocumentName);
				String queryName = ref.getDefaultQueryName();
				if (queryName != null) {
					addAccessForUxUis(UserAccess.queryAggregate(moduleName, queryName), Collections.emptySet());
				}
				addAccessForUxUis(UserAccess.documentAggregate(moduleName, homeDocumentName), Collections.emptySet());
			}
			else if (homeRef == ViewType.edit) {
				Document document = module.getDocument(customer, homeDocumentName);
				addAccessForUxUis(UserAccess.singular(document.getOwningModuleName(), homeDocumentName), Collections.emptySet());
				processViews(document);
			}
		}
	}
	
	private void processMenuItems(final List<MenuItem> items, final Module module, final String moduleName) {
		for (MenuItem item : items) {
			// NB Disregard LinkItem as it is outside of accesses
			if (item instanceof MenuGroup group) {
				processMenuItems(group.getItems(), module, moduleName);
			}
			else if (item instanceof EditItem edit) {
				String documentName = edit.getDocumentName();
				Document document = module.getDocument(customer, documentName);
				addAccessForUxUis(UserAccess.singular(document.getOwningModuleName(), documentName), edit.getUxUis());
				processViews(document);
			}
			else if (item instanceof AbstractDocumentOrQueryOrModelMenuItem aggregate) {
				String documentName = null;
				String queryName = aggregate.getQueryName();
				Set<String> uxuis = aggregate.getUxUis();
				if (queryName != null) {
					addAccessForUxUis(UserAccess.queryAggregate(moduleName, queryName), uxuis);
					MetaDataQueryDefinition query = module.getNullSafeMetaDataQuery(queryName);
					documentName = query.getDocumentName();
				}
				else {
					documentName = aggregate.getDocumentName();
					DocumentRef ref = module.getDocumentRefs().get(documentName);
					queryName = ref.getDefaultQueryName();
					if (queryName != null) {
						addAccessForUxUis(UserAccess.queryAggregate(moduleName, queryName), uxuis);
					}
					else {
						String modelName = aggregate.getModelName();
						if (modelName != null) {
							addAccessForUxUis(UserAccess.modelAggregate(moduleName, documentName, modelName), uxuis);
						}
						else {
							addAccessForUxUis(UserAccess.documentAggregate(moduleName, documentName), uxuis);
						}
					}
				}
				
				Document document = module.getDocument(customer, documentName);
				addAccessForUxUis(UserAccess.singular(document.getOwningModuleName(), documentName), uxuis);
				processViews(document);
			}
		}
	}
	
	private void processViews(Document document) {
		for (UxUiMetadata uxui : router.getUxUis()) {
			String uxuiName = uxui.getName();
			ViewImpl createView = (ViewImpl) document.getView(uxuiName, customer, ViewType.create.toString());
			String uxuiViewKey = null;

			// create and edit view are the same - use edit view
			if (ViewType.edit.toString().equals(createView.getName())) {
				uxuiViewKey = document.getOwningModuleName() + '.' + document.getName() + ':' + createView.getOverriddenUxUiName();
				if (! processedUxUiViews.contains(uxuiViewKey)) {
					processedUxUiViews.add(uxuiViewKey);
					processView(document, createView, uxuiName);
				}
			}
			else {
				uxuiViewKey = document.getOwningModuleName() + '.' + document.getName() + ':' + ViewType.create.toString() + createView.getOverriddenUxUiName();
				if (! processedUxUiViews.contains(uxuiViewKey)) {
					processedUxUiViews.add(uxuiViewKey);
					processView(document, createView, uxuiName);
				}

				ViewImpl editView = (ViewImpl) document.getView(uxuiName, customer, ViewType.edit.toString());
				uxuiViewKey = document.getOwningModuleName() + '.' + document.getName() + ':' + editView.getOverriddenUxUiName();
				if (! processedUxUiViews.contains(uxuiViewKey)) {
					processedUxUiViews.add(uxuiViewKey);
					processView(document, editView, uxuiName);
				}
			}
		}
	}
	
	private void processRoles(final Module module, final String moduleName) {
		for (Role role : module.getRoles()) {
			if (user.isInRole(moduleName, role.getName())) {
				Map<UserAccess, Set<String>> roleAccesses = ((RoleImpl) role).getAccesses();
				for (Entry<UserAccess, Set<String>> entry : roleAccesses.entrySet()) {
					addAccessForUxUis(entry.getKey(), entry.getValue());
				}
			}
		}
	}
	
	private void processView(Document document, ViewImpl view, String uxui) {
		final String overriddenUxUi = view.getOverriddenUxUiName();
		final Module module = customer.getModule(document.getOwningModuleName());
		
		Set<UserAccess> viewAccesses = view.getAccesses(customer, document, uxui);
		if (viewAccesses != null) { // can be null when access control is turned off
			for (UserAccess viewAccess : viewAccesses) {
				if (viewAccess.isSingular()) {
					boolean has = hasViewAccess(viewAccess);
					addAccessForUxUi(viewAccess, overriddenUxUi);
					if (! has) {
						processViews(module.getDocument(customer, viewAccess.getDocumentName()));
					}
				}
				else {
					addAccessForUxUi(viewAccess, overriddenUxUi);
				}
			}
		}
	}
	
	private void addAccessForUxUis(@Nonnull UserAccess access, @Nonnull Set<String> uxuis) {
		String accessString = access.toString();
		Set<String> accessUxUis = accesses.get(accessString);
		if (accessUxUis == null) { // DNE
			if (! uxuis.isEmpty()) {
				accessUxUis = new HashSet<>(uxuis);
				accesses.put(accessString, accessUxUis);
			}
			else {
				accesses.putIfAbsent(accessString, UserAccess.ALL_UX_UIS);
			}
		}
		else if (accessUxUis != UserAccess.ALL_UX_UIS) {
			if (! uxuis.isEmpty()) {
				accessUxUis.addAll(uxuis);
			}
			else {
				accesses.put(accessString, UserAccess.ALL_UX_UIS);
			}
		}
	}

	private void addAccessForUxUi(@Nonnull UserAccess access, @Nullable String uxui) {
		String accessString = access.toString();
		Set<String> accessUxUis = accesses.get(accessString);
		if (accessUxUis == null) {
			if (uxui != null) {
				accessUxUis = new HashSet<>();
				accessUxUis.add(uxui);
				accesses.put(accessString, accessUxUis);
			}
			else {
				accesses.putIfAbsent(accessString, UserAccess.ALL_UX_UIS);
			}
		}
		else if (accessUxUis != UserAccess.ALL_UX_UIS){
			if (uxui != null) {
				accessUxUis.add(uxui);
			}
			else {
				accesses.put(accessString, UserAccess.ALL_UX_UIS);
			}
		}
	}
	
	private boolean hasViewAccess(@Nonnull UserAccess singularUserAccess) {
		return accesses.containsKey(singularUserAccess.toString());
	}
}
