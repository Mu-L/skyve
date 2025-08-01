package org.skyve.metadata.view.model.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.skyve.CORE;
import org.skyve.domain.Bean;
import org.skyve.domain.DynamicBean;
import org.skyve.domain.PersistentBean;
import org.skyve.impl.metadata.OrderingImpl;
import org.skyve.metadata.SortDirection;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Association;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.query.MetaDataQueryColumn;
import org.skyve.metadata.module.query.MetaDataQueryProjectedColumn;
import org.skyve.persistence.AutoClosingIterable;
import org.skyve.persistence.AutoClosingIterableAdpater;
import org.skyve.persistence.BizQL;
import org.skyve.persistence.DocumentQuery.AggregateFunction;
import org.skyve.util.Binder;
import org.skyve.util.Binder.TargetMetaData;
import org.skyve.web.SortParameter;

public abstract class InMemoryListModel<T extends Bean> extends ListModel<T> {
	private Module module;
	private Document drivingDocument;
	private Map<String, Object> parameters;
	private List<Bean> rows;
	
	protected InMemoryListModel(Module module, Document drivingDocument) {
		setDrivingDocument(module, drivingDocument);
	}
	
	/**
	 * Use this constructor when the driving document is explicitly set in postConstruct call.
	 */
	protected InMemoryListModel() {
		// nothing to see here
	}
	
	/**
	 * Used to set the driving document in postConstruct() of subclasses.
	 * @param module
	 * @param drivingDocument
	 */
	protected void setDrivingDocument(Module module, Document drivingDocument) {
		this.module = module;
		this.drivingDocument = drivingDocument;
	}
	
	@Override
	public void postConstruct(Customer customer, boolean runtime) {
		projections.add(Bean.DOCUMENT_ID);
		projections.add(PersistentBean.LOCK_NAME);
		projections.add(PersistentBean.TAGGED_NAME);
		projections.add(PersistentBean.FLAG_COMMENT_NAME);
		projections.add(Bean.BIZ_KEY);

		for (MetaDataQueryColumn column : getColumns()) {
			if ((column instanceof MetaDataQueryProjectedColumn projected) && (! projected.isProjected())) {
				continue;
			}
			String binding = column.getBinding();
			if (binding != null) {
				// if this binding is to an association, 
				// add the bizId as the column value and bizKey as the column displayValue
				TargetMetaData target = Binder.getMetaDataForBinding(customer,
																		module,
																		drivingDocument,
																		binding);
				if (target.getAttribute() instanceof Association) {
					StringBuilder sb = new StringBuilder(64);
					sb.append(binding).append('.').append(Bean.BIZ_KEY);
					projections.add(sb.toString());
				}
				projections.add(binding);
			}
			else {
				projections.add(column.getName());
			}
		}
	}
	
	/**
	 * The model is destructive to the collection of rows so ensure you return a copy if required.
	 * 
	 * @param rows
	 */
	public abstract List<Bean> getRows() throws Exception;
	
	@Override
	public Document getDrivingDocument() {
		return drivingDocument;
	}

	private Set<String> projections = new TreeSet<>();
	
	@Override
	public Set<String> getProjections() {
		return projections;
	}

	private InMemoryFilter filter;
	
	@Override
	public final Filter getFilter() {
		if (filter == null) {
			filter = (InMemoryFilter) newFilter();
		}
		return filter;
	}

	@Override
	public Filter newFilter() {
		return new InMemoryFilter();
	}

	@Override
	public void putParameter(String name, Object value) {
		if (parameters == null) {
			parameters = new TreeMap<>();
		}
		parameters.put(name, value);
	}
	
	private void filterAndSort() {
		if (filter != null) {
			filter.filter(rows);
		}
		
		SortParameter[] sorts = getSortParameters();
		if (sorts != null && sorts.length > 0) {
			OrderingImpl[] order = new OrderingImpl[sorts.length];
			int i = 0;
			for (SortParameter sort : sorts) {
				String by = sort.getBy();
				SortDirection direction = sort.getDirection();
				order[i++] = new OrderingImpl(by, direction);
			}
			Binder.order(rows, order);
		}
	}
	
	/**
	 * Determine whether each row is tagged or not given the selected tagId.
	 * This is done in selects of batches of 100 rows against admin.Tagged document.
	 */
	private void tagged() {
		String tagId = getSelectedTagId();
		if (tagId != null) {
			int i = 0; // row counter
			int l = rows.size(); // row length
			Map<String, Bean> stringBeans = new TreeMap<>(); // bizId -> row
			for (Bean row : rows) {
				String bizId = row.getBizId();
				stringBeans.put(bizId, row);
				i++;
				if ((i == l) || // last element reached
						((i % 100) == 0)) { // multiple of 100
					// Select from adminTagged where tagId and bizUserId match and taggedBizId in (bizIds)
					StringBuilder q = new StringBuilder(64);
					q.append("select bean.taggedBizId from {admin.Tagged} as bean ");
					q.append("where bean.tag.bizId = :tagId ");
					q.append("and bean.bizUserId = :bizUserId ");
					q.append("and bean.taggedBizId in (:bizIds)");
					BizQL bizQL = CORE.getPersistence().newBizQL(q.toString());
					bizQL.putParameter("tagId", tagId);
					bizQL.putParameter(Bean.USER_ID, CORE.getUser().getId());
					bizQL.putParameter("bizIds", stringBeans.keySet());
					for (String taggedBizId : bizQL.scalarResults(String.class)) {
						stringBeans.get(taggedBizId).putDynamic(PersistentBean.TAGGED_NAME, Boolean.TRUE);
					}
					stringBeans.clear();
				}
			}
		}
	}
	
	private Bean summarize() throws Exception {
		Map<String, Object> summaryData = new TreeMap<>();

		AggregateFunction summary = getSummary();
		// This needs to be the ID to satisfy the client data source definitions
		summaryData.put(Bean.DOCUMENT_ID, Long.valueOf(rows.size()));

		if (AggregateFunction.Count.equals(summary)) {
			// Set all the columns to zero, in case there are no rows
			for (MetaDataQueryColumn column : getColumns()) {
				String binding = column.getBinding();
				summaryData.put(binding, Long.valueOf(0));
			}
			summaryData.put(PersistentBean.FLAG_COMMENT_NAME, Long.valueOf(0));
			
			for (Bean row : rows) {
				for (MetaDataQueryColumn column : getColumns()) {
					String binding = column.getBinding();
					count(binding, row, summaryData);
				}
				count(PersistentBean.FLAG_COMMENT_NAME, row, summaryData);
			}
		}
		else if (AggregateFunction.Min.equals(summary)) {
			minOrMax(summaryData, false);
		}
		else if (AggregateFunction.Max.equals(summary)) {
			minOrMax(summaryData, true);
		}
		else if (AggregateFunction.Sum.equals(summary)) {
			sum(summaryData);
		}
		else if (AggregateFunction.Avg.equals(summary)) {
			sum(summaryData);

			// Now compute the average
			for (MetaDataQueryColumn column : getColumns()) {
				String binding = column.getBinding();
				Number sum = (Number) summaryData.get(binding);
				if (sum != null) {
					// round to 5dp
					summaryData.put(binding, Double.valueOf(Math.round(sum.doubleValue() / rows.size() * 100000d) / 100000d));
				}
			}
		}
		
		return new DynamicBean(module.getName(), drivingDocument.getName(), summaryData);
	}
	
	private static void count(String binding, Bean row, Map<String, Object> summaryData) {
		Object value = Binder.get(row, binding);
		if (value != null) {
			Long count = (Long) summaryData.get(binding);
			count = Long.valueOf(count.longValue() + 1);
			summaryData.put(binding, count);
		}
	}
	
	private void minOrMax(Map<String, Object> summaryData, boolean max) throws Exception {
		for (Bean row : rows) {
			for (MetaDataQueryColumn column : getColumns()) {
				String binding = column.getBinding();
				minOrMax(binding, row, summaryData, max);
			}
			minOrMax(PersistentBean.FLAG_COMMENT_NAME, row, summaryData, max);
		}
	}
	
	private static void minOrMax(String binding, Bean row, Map<String, Object> summaryData, boolean max) {
		@SuppressWarnings("unchecked")
		Comparable<Object> value = (Comparable<Object>) Binder.get(row, binding);
		if (value != null) {
			@SuppressWarnings("unchecked")
			Comparable<Object> minOrMax = (Comparable<Object>) summaryData.get(binding);
			if (minOrMax == null) {
				summaryData.put(binding, value);
			}
			else if (max && (value.compareTo(minOrMax) > 0)) {
				summaryData.put(binding, value);
			}
			else if ((! max) && (value.compareTo(minOrMax) < 0)) {
				summaryData.put(binding, value);
			}
		}
	}
	
	private void sum(Map<String, Object> summaryData) throws Exception {
		for (Bean row : rows) {
			for (MetaDataQueryColumn column : getColumns()) {
				String binding = column.getBinding();
				Object value = Binder.get(row, binding);
				if (value instanceof Number number) {
					Number sum = (Number) summaryData.get(binding);
					sum = (sum == null) ? Double.valueOf(number.doubleValue()) : Double.valueOf(sum.doubleValue() + number.doubleValue());
					summaryData.put(binding, sum);
				}
			}
		}
		// Round to 5dp
		for (MetaDataQueryColumn column : getColumns()) {
			String binding = column.getBinding();
			Object value = Binder.get(summaryData, binding);
			if (value instanceof Number number) {
				summaryData.put(binding, Double.valueOf(Math.round(number.doubleValue() * 100000d) / 100000d));
			}
		}
	}
	
	@Override
	public Page fetch() throws Exception {
		rows = getRows();
		if (rows == null) {
			rows = new ArrayList<>(0);
		}
		
		filterAndSort();
		
		int startRow = getStartRow();
		int endRow = getEndRow();
		
		Page result = new Page();
		int totalRows = rows.size();
		result.setTotalRows(totalRows);
		result.setSummary(summarize());

		// If something requests a start row > what we have in the set
		// (maybe a criteria has constrained the set such that a page we were at doesn't exist any more)
		// then just send back an empty result set.
		if (startRow < totalRows) {
			// NB This next bit gets destructive on the list
			rows.retainAll(rows.subList(startRow, Math.min(endRow + 1, totalRows)));
			tagged(); // set the tag attribute in the rows
			result.setRows(rows);
		}
		else {
			result.setRows(new ArrayList<>(0));
		}
		
		return result;
	}

	@Override
	public AutoClosingIterable<Bean> iterate() throws Exception {
		rows = getRows();
		if (rows == null) {
			rows = new ArrayList<>(0);
		}

		filterAndSort();
		// Note that tagged() call here is not required as the tagged column can't be exported in the UI
		
		return new AutoClosingIterableAdpater<>(rows);
	}
}
