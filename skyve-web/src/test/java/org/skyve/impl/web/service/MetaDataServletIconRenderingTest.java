package org.skyve.impl.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.skyve.impl.metadata.MetadataIconResolver.ResolvedIcon;
import org.skyve.impl.metadata.model.document.DocumentImpl;

@SuppressWarnings("static-method")
class MetaDataServletIconRenderingTest {
	@Test
	void dataSourceRendersFontIcon() {
		StringBuilder target = new StringBuilder();

		MetaDataServlet.appendDataSourceIcon(target, new ResolvedIcon("fa-solid fa-user", null));

		assertEquals("\",\"fontIcon\":\"fa-solid fa-user", target.toString());
	}

	@Test
	void dataSourceRendersImageIcon() {
		StringBuilder target = new StringBuilder();

		MetaDataServlet.appendDataSourceIcon(target, new ResolvedIcon(null, "icons/document.png"));

		assertEquals("\",\"icon\":\"icons/document.png", target.toString());
	}

	@Test
	void dataSourceRendersNothingWithoutIcon() {
		StringBuilder target = new StringBuilder();

		MetaDataServlet.appendDataSourceIcon(target, new ResolvedIcon(null, null));

		assertEquals("", target.toString());
	}

	@Test
	void dataSourceEscapesIconMetadataForJson() {
		StringBuilder target = new StringBuilder();

		MetaDataServlet.appendDataSourceIcon(target, new ResolvedIcon("font\"\\icon", null));

		assertEquals("\",\"fontIcon\":\"font\\\"\\\\icon", target.toString());
	}

	@Test
	void viewRendersFontIconOnly() {
		StringBuilder target = new StringBuilder();

		MetaDataServlet.appendViewIcon(target,
				new ResolvedIcon("fa-solid fa-user", null),
				document());

		assertEquals(",\"iconStyleClass\":\"fa-solid fa-user\"", target.toString());
	}

	@Test
	void viewRendersResolvedImageUrl() {
		StringBuilder target = new StringBuilder();

		MetaDataServlet.appendViewIcon(target,
				new ResolvedIcon(null, "view.png"),
				document());

		assertEquals(",\"icon32x32Url\":\"resources?_doc=admin.Contact&_n=view.png\"", target.toString());
	}

	@Test
	void viewRendersNothingWithoutIconUrls() {
		StringBuilder target = new StringBuilder();

		MetaDataServlet.appendViewIcon(target, new ResolvedIcon(null, null), document());

		assertEquals("", target.toString());
	}

	@Test
	void viewEscapesFontIconForJson() {
		StringBuilder target = new StringBuilder();

		MetaDataServlet.appendViewIcon(target,
				new ResolvedIcon("font\"\\icon", null),
				document());

		assertEquals(",\"iconStyleClass\":\"font\\\"\\\\icon\"", target.toString());
	}

	@Test
	void viewEscapesImageUrlForJson() {
		StringBuilder target = new StringBuilder();

		MetaDataServlet.appendViewIcon(target,
				new ResolvedIcon(null, "view\"\\icon.png"),
				document());

		assertEquals(",\"icon32x32Url\":\"resources?_doc=admin.Contact&_n=view\\\"\\\\icon.png\"",
				target.toString());
	}

	private static DocumentImpl document() {
		DocumentImpl result = new DocumentImpl();
		result.setOwningModuleName("admin");
		result.setName("Contact");
		return result;
	}
}
