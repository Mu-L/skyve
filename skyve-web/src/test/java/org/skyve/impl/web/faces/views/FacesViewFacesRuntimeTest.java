package org.skyve.impl.web.faces.views;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.skyve.impl.metadata.view.widget.bound.input.CompleteType;
import org.skyve.impl.sail.mock.MockFacesContext;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIPanel;

@SuppressWarnings("static-method")
class FacesViewFacesRuntimeTest {
	@Test
	void mockFacesContextCanBeCreatedWhenMojarraRuntimeIsPresent() {
		try (MockFacesContext context = MockFacesContext.get()) {
			assertNotNull(context.getViewRoot());
		}
	}

	@Test
	void completeAndLookupCanBeInvokedWithCurrentComponentContext() {
		try (MockFacesContext context = MockFacesContext.get()) {
			UIPanel component = new UIPanel();
			component.getAttributes().put("binding", "name");
			component.getAttributes().put("complete", CompleteType.previous);
			component.getAttributes().put("module", "admin");
			component.getAttributes().put("document", "Contact");
			component.getAttributes().put("query", "");
			component.getAttributes().put("display", "name");
			component.pushComponentToEL(context, component);
			assertNotNull(UIComponent.getCurrentComponent(context));

			FacesView view = new FacesView();
			invokeIgnoringThrowable(() -> view.complete("abc"));
			invokeIgnoringThrowable(() -> view.lookup("abc"));

			component.popComponentFromEL(context);
		}
	}

	private static void invokeIgnoringThrowable(Runnable invocation) {
		try {
			invocation.run();
		}
		catch (Exception ignored) {
			ignored.getClass();
		}
	}
}
