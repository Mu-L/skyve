package org.skyve.impl.metadata;

import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.view.View;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Resolves renderer-neutral icon metadata shared by all presentation targets.
 *
 * <p>The create/edit precedence contract is view font icon, view image, document font icon,
 * document image, then no icon. Passing no view resolves list-view icons from the driving
 * document only.
 */
public final class MetadataIconResolver {
	private static final ResolvedIcon NONE = new ResolvedIcon(null, null);

	private MetadataIconResolver() {
		// Utility class
	}

	/**
	 * Resolves icon metadata for a document and optional create/edit view.
	 *
	 * @param document driving document supplying fallback metadata; must not be {@code null}
	 * @param view optional create/edit view; pass {@code null} for a list view
	 * @return resolved icon; never {@code null}
	 */
	public static @Nonnull ResolvedIcon resolve(@Nonnull Document document, @Nullable View view) {
		if (view != null) {
			String iconStyleClass = view.getIconStyleClass();
			if (iconStyleClass != null) {
				return new ResolvedIcon(iconStyleClass, null);
			}

			String iconFileName = view.getIcon32x32RelativeFileName();
			if (iconFileName != null) {
				return new ResolvedIcon(null, iconFileName);
			}
		}

		String iconStyleClass = document.getIconStyleClass();
		if (iconStyleClass != null) {
			return new ResolvedIcon(iconStyleClass, null);
		}

		String iconFileName = document.getIcon32x32RelativeFileName();
		if (iconFileName != null) {
			return new ResolvedIcon(null, iconFileName);
		}

		return NONE;
	}

	/**
	 * Holds one resolved font icon, one resolved image, or neither.
	 *
	 * @param iconStyleClass font-icon CSS classes, or {@code null}
	 * @param iconFileName relative image filename, or {@code null}
	 */
	public record ResolvedIcon(@Nullable String iconStyleClass, @Nullable String iconFileName) {
		// Immutable metadata value.
	}
}
