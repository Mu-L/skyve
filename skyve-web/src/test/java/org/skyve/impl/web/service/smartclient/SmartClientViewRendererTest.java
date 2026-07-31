package org.skyve.impl.web.service.smartclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.skyve.util.OWASP;

@SuppressWarnings("static-method")
class SmartClientViewRendererTest {
	private static final String UNSAFE_TEXT = "<img src=x onerror=alert(1)> & \"quoted\" 'single'";

	@Test
	void escapeSmartClientTextReturnsNullForNullValue() {
		assertNull(SmartClientViewRenderer.escapeSmartClientText(null, true));
		assertNull(SmartClientViewRenderer.escapeSmartClientText(null, true));
		assertNull(SmartClientViewRenderer.escapeSmartClientText(null, false));
	}

	@Test
	void escapeSmartClientTextEscapesHtmlByDefaultBeforeJavaScriptStringEscaping() {
		assertEquals(OWASP.escapeJsString(OWASP.escapeHtml(UNSAFE_TEXT)),
						SmartClientViewRenderer.escapeSmartClientText(UNSAFE_TEXT, true));
	}

	@Test
	void escapeSmartClientTextEscapesHtmlWhenExplicitlyTrueBeforeJavaScriptStringEscaping() {
		assertEquals(OWASP.escapeJsString(OWASP.escapeHtml(UNSAFE_TEXT)),
						SmartClientViewRenderer.escapeSmartClientText(UNSAFE_TEXT, true));
	}

	@Test
	void escapeSmartClientTextLeavesTrustedHtmlRawWhenFalseButEscapesJavaScriptString() {
		assertEquals(OWASP.escapeJsString(UNSAFE_TEXT),
						SmartClientViewRenderer.escapeSmartClientText(UNSAFE_TEXT, false));
	}

	@Test
	void escapeSmartClientTextEscapesApostropheExactlyOnce() {
		assertEquals("O\\'Brien", SmartClientViewRenderer.escapeSmartClientText("O'Brien", false));
	}
}
