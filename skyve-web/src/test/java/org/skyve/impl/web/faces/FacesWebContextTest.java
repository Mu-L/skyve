package org.skyve.impl.web.faces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.skyve.domain.messages.MessageSeverity;
import org.skyve.impl.sail.mock.MockFacesContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.faces.application.FacesMessage;

@SuppressWarnings("static-method")
class FacesWebContextTest {
	@Test
	void messageMapsSeveritiesToFacesMessages() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession();

		try (MockFacesContext facesContext = MockFacesContext.get(request, new MockHttpServletResponse())) {
			FacesWebContext context = new FacesWebContext();
			context.message(MessageSeverity.info, "info");
			context.message(MessageSeverity.warn, "warn");
			context.message(MessageSeverity.error, "error");
			context.message(MessageSeverity.fatal, "fatal");
			context.growl(MessageSeverity.info, "growl");

			List<FacesMessage> messages = new ArrayList<>();
			facesContext.getMessages().forEachRemaining(messages::add);
			assertEquals(5, messages.size());
			org.junit.jupiter.api.Assertions.assertSame(FacesMessage.SEVERITY_INFO, messages.get(0).getSeverity());
			org.junit.jupiter.api.Assertions.assertSame(FacesMessage.SEVERITY_WARN, messages.get(1).getSeverity());
			org.junit.jupiter.api.Assertions.assertSame(FacesMessage.SEVERITY_ERROR, messages.get(2).getSeverity());
			org.junit.jupiter.api.Assertions.assertSame(FacesMessage.SEVERITY_FATAL, messages.get(3).getSeverity());
		}
	}

	@Test
	void growlsAndMessagesListsAreNotStoredLocally() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession();

		try (MockFacesContext ignored = MockFacesContext.get(request, new MockHttpServletResponse())) {
			FacesWebContext context = new FacesWebContext();

			assertNull(context.getGrowls());
			assertNull(context.getMessages());
		}
	}
}
