package org.skyve.metadata.module.fluent;

import org.skyve.impl.metadata.repository.module.CalendarItemMetaData;

public class FluentCalendarItem extends FluentMenuItem<FluentCalendarItem> {
	private CalendarItemMetaData item = null;
	
	public FluentCalendarItem() {
		item = new CalendarItemMetaData();
	}

	public FluentCalendarItem(CalendarItemMetaData item) {
		this.item = item;
	}

	public FluentCalendarItem from(@SuppressWarnings("hiding") org.skyve.impl.metadata.module.menu.CalendarItem item) {
		super.from(item);
		documentName(item.getDocumentName());
		queryName(item.getQueryName());
		modelName(item.getModelName());
		startBinding(item.getStartBinding());
		endBinding(item.getEndBinding());
		return this;
	}
	
	public FluentCalendarItem documentName(String documentName) {
		item.setDocumentName(documentName);
		return this;
	}
	
	public FluentCalendarItem queryName(String queryName) {
		item.setQueryName(queryName);
		return this;
	}

	public FluentCalendarItem modelName(String modelName) {
		item.setModelName(modelName);
		return this;
	}
	
	public FluentCalendarItem startBinding(String startBinding) {
		item.setStartBinding(startBinding);
		return this;
	}
	
	public FluentCalendarItem endBinding(String endBinding) {
		item.setEndBinding(endBinding);
		return this;
	}

	@Override
	public CalendarItemMetaData get() {
		return item;
	}
}
