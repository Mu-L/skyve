package org.skyve.impl.sail.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

@SuppressWarnings({"static-method", "resource"})
class MockFacesContextTest {
	@Test
	void defaultStateAndAccessorsBehaveAsDocumented() {
		MockFacesContext context = new MockFacesContext();

		assertNotNull(context.getApplication());
		assertNotNull(context.getELContext());
		assertNotNull(context.getViewRoot());
		assertFalse(context.getClientIdsWithMessages().hasNext());
		assertNotNull(context.getExternalContext());
		assertNull(context.getLifecycle());
		assertNull(context.getMaximumSeverity());
		assertFalse(context.getMessages().hasNext());
		assertFalse(context.getMessages("client").hasNext());
		assertNull(context.getRenderKit());
		assertTrue(context.getRenderResponse());
		assertFalse(context.getResponseComplete());
		assertFalse(context.isPostback());
		assertNull(context.getResponseStream());
	}

	@Test
	void viewRootAndWriterCanBeReplacedAndReleasedFlagTracksLifecycle() {
		MockFacesContext context = new MockFacesContext();
		UIViewRoot root = new UIViewRoot();
		ResponseWriter writer = mock(ResponseWriter.class);

		context.setViewRoot(root);
		context.setResponseStream(null);
		context.setResponseWriter(writer);
		context.setPostback(true);
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "summary", "detail"));
		context.renderResponse();
		context.responseComplete();

		assertSame(root, context.getViewRoot());
		assertSame(writer, context.getResponseWriter());
		assertSame(FacesMessage.SEVERITY_WARN, context.getMaximumSeverity());
		assertEquals("summary", context.getMessages().next().getSummary());
		assertEquals("detail", context.getMessages(null).next().getDetail());
		assertTrue(context.isPostback());
		assertFalse(context.isReleased());
		context.release();
		assertTrue(context.isReleased());
	}

	@Test
	void servletBackedInstallExposesTheSuppliedObjects() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		try (MockFacesContext installed = MockFacesContext.get(request, response)) {
			assertSame(installed, FacesContext.getCurrentInstance());
			assertSame(request, installed.getExternalContext().getRequest());
			assertSame(response, installed.getExternalContext().getResponse());
		}
	}

	@Test
	void installAndCloseManageTheCurrentThreadContext() {
		MockFacesContext context;

		try (MockFacesContext installed = MockFacesContext.get()) {
			context = installed;
			assertSame(installed, FacesContext.getCurrentInstance());
			assertFalse(installed.isReleased());
		}

		assertNull(FacesContext.getCurrentInstance());
		assertTrue(context.isReleased());
	}

	@Test
	void installRefusesToReplaceAnExistingContext() {
		try (MockFacesContext installed = MockFacesContext.get()) {
			IllegalStateException exception = assertThrows(IllegalStateException.class, MockFacesContext::get);

			assertEquals("A FacesContext is already installed on the current thread", exception.getMessage());
			assertSame(installed, FacesContext.getCurrentInstance());
		}
	}
}
