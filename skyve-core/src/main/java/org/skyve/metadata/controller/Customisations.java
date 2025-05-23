package org.skyve.metadata.controller;

import org.skyve.impl.metadata.view.HorizontalAlignment;
import org.skyve.metadata.model.Attribute.AttributeType;
import org.skyve.report.ReportFormat;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Enables the customisation of certain Skyve functions.
 * A class implementing this interface can be set in factories.customisationsClass of the JSON configuration.
 * The NoCustomisations class represents the Skyve defaults and can be extended for your own implementations.
 */
public interface Customisations {
	/**
	 * Determine the default text alignment to use within textual widgets given an attribute type.
	 */
	@Nonnull HorizontalAlignment determineDefaultWidgetTextAlignment(@Nonnull String uxui, @Nullable AttributeType attributeType);

	/**
	 * Determine the default text alignment to use in columns given an attribute type.
	 */
	@Nonnull HorizontalAlignment determineDefaultColumnTextAlignment(@Nonnull String uxui, @Nullable AttributeType attributeType);

	/**
	 * Determine the default column width in pixels given an attribute type.
	 * <code>null</code> may be returned if there is no default.
	 */
	@Nullable Integer determineDefaultColumnWidth(@Nonnull String uxui, @Nullable AttributeType attributeType);

	/**
	 * Register custom ExpressionEvaluators for use in this Skyve deployment using ExpressionEvaluator.register().
	 */
	void registerCustomExpressions();
	
	/**
	 * Register custom Formatters for use in this Skyve deployment using Formatters.register().
	 */
	void registerCustomFormatters();

	/**
	 * Determine the list grid export formats allowed.
	 * @return	The array of allowed formats.
	 */
	@Nonnull ReportFormat[] listGridExportFormats();
	
// TODO add the view generator bit in	
//	@Nonnull ViewGenerator viewGenerator();
}
