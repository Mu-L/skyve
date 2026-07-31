package org.skyve.impl.generate;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.junit.jupiter.api.Test;
import org.skyve.impl.metadata.MetadataIconResolver.ResolvedIcon;
import org.skyve.impl.metadata.customer.CustomerImpl;
import org.skyve.impl.metadata.model.document.DocumentImpl;
import org.skyve.impl.metadata.module.ModuleImpl;
import org.skyve.impl.metadata.view.ViewImpl;
import org.skyve.metadata.user.User;

import jakarta.annotation.Nonnull;

@SuppressWarnings("static-method")
class ViewRendererIconTest {
	@Test
	void viewRendererUsesResolvedFontIconWithoutImageFallback() {
		DocumentImpl document = document();
		document.setIconStyleClass("fa-solid fa-file");
		document.setIcon32x32RelativeFileName("document.png");
		ViewImpl view = new ViewImpl();
		view.setIconStyleClass("fa-solid fa-pen");
		view.setIcon32x32RelativeFileName("view.png");
		ViewRenderer renderer = renderer(document, view);

		renderer.visitView();

		ResolvedIcon icon = new ResolvedIcon("fa-solid fa-pen", null);
		verify(renderer).renderView(icon);
	}

	@Test
	void viewRendererPassesResolvedViewImage() {
		DocumentImpl document = document();
		document.setIconStyleClass("fa-solid fa-file");
		ViewImpl view = new ViewImpl();
		view.setIcon32x32RelativeFileName("view.png");
		ViewRenderer renderer = renderer(document, view);

		renderer.visitView();

		ResolvedIcon icon = new ResolvedIcon(null, "view.png");
		verify(renderer).renderView(icon);
	}

	@Test
	void viewRendererPassesResolvedDocumentImageFallback() {
		DocumentImpl document = document();
		document.setIcon32x32RelativeFileName("document.png");
		ViewRenderer renderer = renderer(document, new ViewImpl());

		renderer.visitView();

		ResolvedIcon icon = new ResolvedIcon(null, "document.png");
		verify(renderer).renderView(icon);
	}

	@Test
	void viewRendererPassesNoIconToBothLifecycleCallbacks() {
		ViewRenderer renderer = renderer(document(), new ViewImpl());

		renderer.visitView();
		ResolvedIcon icon = new ResolvedIcon(null, null);
		renderer.visitedView();

		verify(renderer).renderView(icon);
		verify(renderer).renderedView(icon);
	}

	private static DocumentImpl document() {
		DocumentImpl result = new DocumentImpl();
		result.setName("Contact");
		result.setOwningModuleName("admin");
		return result;
	}

	private static ViewRenderer renderer(@Nonnull DocumentImpl document, @Nonnull ViewImpl view) {
		CustomerImpl customer = new CustomerImpl();
		User user = mock(User.class);
		when(user.getCustomer()).thenReturn(customer);
		ModuleImpl module = new ModuleImpl();
		module.setName("admin");
		return mock(ViewRenderer.class,
				withSettings().useConstructor(user, module, document, view, "desktop")
						.defaultAnswer(CALLS_REAL_METHODS));
	}
}
