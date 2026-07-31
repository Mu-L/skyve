package org.skyve.impl.web.faces.views;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class EditorialThemeResourcesTest {
	private static final Path SOURCE_CSS = Path.of("src/css/editorial/main.css");
	private static final Path PACKAGED_CSS = Path.of(
			"src/main/resources/META-INF/resources/skyve/editorial/assets/css/main-min.css");

	@Test
	void selectedButtonGroupsUseTheSolidEditorialButtonColours() throws Exception {
		String css = Files.readString(SOURCE_CSS);
		String packagedCss = Files.readString(PACKAGED_CSS);

		assertTrue(css.contains("body .ui-selectonebutton > .ui-button.ui-state-active"), css);
		assertTrue(css.contains("body .ui-selectmanybutton > .ui-button.ui-state-active"), css);
		assertTrue(css.contains("background: var(--skyve-editorial-button-hover-bg) !important"), css);
		assertTrue(css.contains("color: var(--skyve-editorial-button-hover-fg) !important"), css);
		assertTrue(packagedCss.contains("body .ui-selectonebutton>.ui-button.ui-state-active"), packagedCss);
		assertTrue(packagedCss.contains("body .ui-selectmanybutton>.ui-button.ui-state-active"), packagedCss);
	}
}
