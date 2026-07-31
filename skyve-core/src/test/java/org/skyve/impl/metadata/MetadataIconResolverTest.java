package org.skyve.impl.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.skyve.impl.metadata.MetadataIconResolver.ResolvedIcon;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.view.View;

@SuppressWarnings("static-method")
class MetadataIconResolverTest {
	@Test
	void viewFontIconHasFirstPrecedence() {
		Document document = document("document-font", "document.png");
		View view = view("view-font", "view.png");

		ResolvedIcon result = MetadataIconResolver.resolve(document, view);

		assertEquals("view-font", result.iconStyleClass());
		assertNull(result.iconFileName());
	}

	@Test
	void viewImageHasPrecedenceOverDocumentFontIcon() {
		Document document = document("document-font", "document.png");
		View view = view(null, "view.png");

		ResolvedIcon result = MetadataIconResolver.resolve(document, view);

		assertNull(result.iconStyleClass());
		assertEquals("view.png", result.iconFileName());
	}

	@Test
	void documentFontIconHasPrecedenceOverDocumentImage() {
		Document document = document("document-font", "document.png");

		ResolvedIcon result = MetadataIconResolver.resolve(document, view(null, null));

		assertEquals("document-font", result.iconStyleClass());
		assertNull(result.iconFileName());
	}

	@Test
	void documentImageIsUsedWhenNoFontIconExists() {
		Document document = document(null, "document.png");

		ResolvedIcon result = MetadataIconResolver.resolve(document, view(null, null));

		assertNull(result.iconStyleClass());
		assertEquals("document.png", result.iconFileName());
	}

	@Test
	void listViewResolvesFromDocumentOnly() {
		Document document = document(null, "document.png");

		ResolvedIcon result = MetadataIconResolver.resolve(document, null);

		assertNull(result.iconStyleClass());
		assertEquals("document.png", result.iconFileName());
	}

	@Test
	void noIconIsReturnedWhenMetadataHasNone() {
		ResolvedIcon result = MetadataIconResolver.resolve(document(null, null), view(null, null));

		assertNull(result.iconStyleClass());
		assertNull(result.iconFileName());
	}

	private static Document document(String iconStyleClass, String iconFileName) {
		Document result = mock(Document.class);
		when(result.getIconStyleClass()).thenReturn(iconStyleClass);
		when(result.getIcon32x32RelativeFileName()).thenReturn(iconFileName);
		return result;
	}

	private static View view(String iconStyleClass, String iconFileName) {
		View result = mock(View.class);
		when(result.getIconStyleClass()).thenReturn(iconStyleClass);
		when(result.getIcon32x32RelativeFileName()).thenReturn(iconFileName);
		return result;
	}
}
