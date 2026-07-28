package org.skyve.impl.web.faces.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.skyve.impl.sail.mock.MockFacesContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings("static-method")
class SwaggerViewTest {
	@Test
	@SuppressWarnings("java:S3011") // Reflection invokes the private lifecycle callback without a JSF container.
	void postConstructInitialisesInheritedLocalisationState() throws Exception {
		SwaggerView view = new SwaggerView();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setPreferredLocales(java.util.List.of(Locale.forLanguageTag("he")));

		try (MockFacesContext ignored = MockFacesContext.get(request, new MockHttpServletResponse())) {
			Method postConstruct = SwaggerView.class.getDeclaredMethod("postConstruct");
			postConstruct.setAccessible(true);
			postConstruct.invoke(view);
		}

		assertEquals("rtl", view.getDir());
		assertEquals("he", view.getLanguageTag());
	}
}
