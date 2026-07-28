package org.skyve.impl.web.faces.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.skyve.impl.sail.mock.MockFacesContext;

@SuppressWarnings("static-method")
class PublicFacesViewTest {
	@Test
	void customerParameterGetterAndSetterRoundTrip() {
		PublicFacesView view = new PublicFacesView();

		view.setBizCustomerParameter("acme");

		assertEquals("acme", view.getBizCustomerParameter());
	}

	@Test
	void preRenderThrowsWhenCustomerParameterMissing() {
		PublicFacesView view = new PublicFacesView();

		try (MockFacesContext ignored = MockFacesContext.get()) {
			IllegalStateException ex = assertThrows(IllegalStateException.class, view::preRender);
			assertEquals("Malformed URL - this URL must have a 'c' parameter", ex.getMessage());
		}
	}
}
