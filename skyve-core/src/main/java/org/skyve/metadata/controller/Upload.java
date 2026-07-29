package org.skyve.metadata.controller;

import org.skyve.content.MimeType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Carries an uploaded file from the browser to an {@link UploadAction}.
 *
 * <p>The file name, MIME type, and content stream are provided by the HTTP multipart
 * form submission. The stream is lifecycle-managed by Skyve and must not be closed
 * by the action implementation.
 *
 * @see UploadAction
 * @see WebFileInputStream
 */
public class Upload {
	private @Nonnull String fileName;
	@SuppressWarnings("resource")
	private @Nonnull WebFileInputStream stream;
	private @Nullable MimeType mimeType;
	
	/**
	 * Constructor
	 * @param fileName
	 * @param stream
	 * @param mimeType
	 */
	public Upload(@Nonnull String fileName, @Nonnull WebFileInputStream stream, @Nullable MimeType mimeType) {
		this.fileName = fileName;
		this.stream = stream;
		this.mimeType = mimeType;
	}

	/**
	 * Returns the original file name as reported by the browser.
	 *
	 * @return file name; never {@code null}
	 */
	public @Nonnull String getFileName() {
		return fileName;
	}

	/**
	 * Returns the lifecycle-managed stream wrapping the uploaded file content.
	 *
	 * <p>The stream remains open for the duration of the action invocation.
	 * Implementations must not close it.
	 *
	 * @return the upload stream; never {@code null}
	 */
	public @Nonnull WebFileInputStream getInputStream() {
		return stream;
	}

	/**
	 * Returns the MIME type reported by the browser for the uploaded file.
	 *
	 * @return the MIME type; may be {@code null} if the browser did not report one
	 */
	public @Nullable MimeType getMimeType() {
		return mimeType;
	}
}
