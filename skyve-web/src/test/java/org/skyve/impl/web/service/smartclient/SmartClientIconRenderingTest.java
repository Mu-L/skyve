package org.skyve.impl.web.service.smartclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.skyve.impl.metadata.MetadataIconResolver.ResolvedIcon;

@SuppressWarnings("static-method")
class SmartClientIconRenderingTest {
	@Test
	void createEditViewRendersFontIcon() {
		StringWriter target = new StringWriter();

		SmartClientGeneratorServlet.appendIcon(new PrintWriter(target),
				"edit",
				new ResolvedIcon("fa-solid fa-user", null));

		assertEquals("',_editFontIcon:'fa-solid fa-user", target.toString());
	}

	@Test
	void createEditViewRendersImageIcon() {
		StringWriter target = new StringWriter();

		SmartClientGeneratorServlet.appendIcon(new PrintWriter(target),
				"create",
				new ResolvedIcon(null, "icons/document.png"));

		assertEquals("',_createIcon:'icons/document.png", target.toString());
	}

	@Test
	void createEditViewRendersNothingWithoutIcon() {
		StringWriter target = new StringWriter();

		SmartClientGeneratorServlet.appendIcon(new PrintWriter(target),
				"edit",
				new ResolvedIcon(null, null));

		assertEquals("", target.toString());
	}

	@Test
	void createEditViewEscapesIconMetadataForJavaScript() {
		StringWriter target = new StringWriter();

		SmartClientGeneratorServlet.appendIcon(new PrintWriter(target),
				"edit",
				new ResolvedIcon("font'\\icon", null));

		assertEquals("',_editFontIcon:'font\\'\\\\icon", target.toString());
	}

	@Test
	void listViewRendersFontIcon() {
		StringBuilder target = new StringBuilder();

		SmartClientViewRenderer.appendDataSourceIcon(target, new ResolvedIcon("fa-solid fa-user", null));

		assertEquals("',fontIcon:'fa-solid fa-user", target.toString());
	}

	@Test
	void listViewRendersImageIcon() {
		StringBuilder target = new StringBuilder();

		SmartClientViewRenderer.appendDataSourceIcon(target, new ResolvedIcon(null, "icons/document.png"));

		assertEquals("',icon:'icons/document.png", target.toString());
	}

	@Test
	void listViewRendersNothingWithoutIcon() {
		StringBuilder target = new StringBuilder();

		SmartClientViewRenderer.appendDataSourceIcon(target, new ResolvedIcon(null, null));

		assertEquals("", target.toString());
	}

	@Test
	void listViewEscapesIconMetadataForJavaScript() {
		StringBuilder target = new StringBuilder();

		SmartClientViewRenderer.appendDataSourceIcon(target, new ResolvedIcon(null, "icon'\\file.png"));

		assertEquals("',icon:'icon\\'\\\\file.png", target.toString());
	}
}
