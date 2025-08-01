package org.skyve.util;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.skyve.CORE;
import org.skyve.domain.Bean;
import org.skyve.impl.persistence.AbstractPersistence;
import org.skyve.impl.util.UtilImpl;
import org.skyve.impl.util.UtilImpl.ArchiveConfig;
import org.skyve.impl.web.AbstractWebContext;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.user.User;
import org.skyve.util.test.TestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Skyve utility methods
 */
public class Util {

    /**
     * Skyve's framework logger
     * <p>
     * Replace with someting like this:
     * <p>
     * <code>
     * private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MyClass.class);
     * </code>
     * 
     * @deprecated This logger will be removed; please switch to using
     *             a logger named appropriately for the class doing the logging.
     *             For example <a href="https://www.slf4j.org/manual.html#typical_usage">
     *             see the Typical usage pattern suggested by slf4j</a>.
     * 
     */
    @Deprecated(since = "9.3.0", forRemoval = true)
    public static final java.util.logging.Logger LOGGER = UtilImpl.LOGGER;

    private static final Logger utilLogger = LoggerFactory.getLogger(Util.class);

	/**
	 * UTF-8 charset identifier
	 */
	public static final String UTF8 = "UTF-8";

	/**
	 * Disallow instantiation
	 */
	private Util() {
		// nothing to see here
	}

	/**
	 * Clone the object given by serializing and deserializing it.
	 * @param object	The object to clone.
	 * @return	The cloned object.
	 */
	public static final @Nonnull <T extends Serializable> T cloneBySerialization(@Nonnull T object) {
		return UtilImpl.cloneBySerialization(object);
	}

	/**
	 * Clone the object given by serializing and deserializing it, setting any persistent Skyve beans transient (not persisted).
	 * @param object	The object to clone.
	 * @return	The cloned non-persisted object.
	 */
	public static final @Nonnull <T extends Serializable> T cloneToTransientBySerialization(@Nonnull T object) {
		return UtilImpl.cloneToTransientBySerialization(object);
	}

	/**
	 * Recurse the bean ensuring that everything is touched and loaded from the database.
	 * 
	 * @param bean The bean to load.
	 */
	public static void populateFully(@Nonnull Bean bean) {
		UtilImpl.populateFully(bean);
	}

	/**
	 * Utility method that tries to properly initialise the persistence layer proxies used by lazy loading.
	 * 
	 * @param <T>
	 * @param possibleProxy The possible proxy
	 * @return the resolved proxy or possibleProxy
	 */
	public static @Nonnull <T> T deproxy(@Nonnull T possibleProxy) throws ClassCastException {
		return UtilImpl.deproxy(possibleProxy);
	}

	/**
	 * Trims and sets "" to null.
	 * 
	 * @param value
	 * @return
	 */
	public static @Nullable String processStringValue(@Nullable String value) {
		return UtilImpl.processStringValue(value);
	}

	/**
	 * Internationalises a string for the user's locale and performs message formatting on tokens like {0}, {1} etc.
	 */
	public static @Nonnull String nullSafeI18n(@Nonnull String key, String... values) {
		// NB Don't attempt to get a user unless persistence has been initialised
		User u = (AbstractPersistence.IMPLEMENTATION_CLASS == null) ? null : CORE.getUser();
		return nullSafeI18n(key, (u == null) ? null : u.getLocale(), values);
	}
	
	/**
	 * Internationalises a string for the user's locale and performs message formatting on tokens like {0}, {1} etc.
	 * Only returns null if the key is null.
	 */
	public static @Nullable String i18n(@Nullable String key, String... values) {
		if (key == null) {
			return null;
		}
		return nullSafeI18n(key, values);
	}
	
	// language code -> (key -> string)
	// Use of this map is WAY faster than using ResourceBundle which sux arse.
	// This map is synchronized on during the put but reads are left free.
	// The usage of this map is almost always read.
	// The map is keyed on language code because there are less language codes than locales.
	// NB Make the map volatile to ensure it is readable by multiple threads.
	private static volatile Map<String, Map<String, String>> I18N_PROPERTIES = new TreeMap<>();
	
	/**
	 * Internationalises a string for a particular locale and performs message formatting on tokens like {0}, {1} etc.
	 */
	public static @Nonnull String nullSafeI18n(@Nonnull String key, @Nullable Locale locale, String... values) {
		String result = null;

		try {
			Locale l = (locale == null) ? Locale.ENGLISH : locale;
			String lang = l.getLanguage();
			Map<String, String> properties = I18N_PROPERTIES.get(lang);
			if (properties == null) {
				synchronized (I18N_PROPERTIES) {
					properties = I18N_PROPERTIES.get(lang);
					if (properties == null) {
						ResourceBundle bundle = ResourceBundle.getBundle("resources.i18n", l, Thread.currentThread().getContextClassLoader());
						properties = new TreeMap<>();
						for (String bundleKey : bundle.keySet()) {
							properties.put(bundleKey, bundle.getString(bundleKey));
						}
						ResourceBundle.clearCache(Thread.currentThread().getContextClassLoader());
						I18N_PROPERTIES.put(lang, properties);
					}
				}
			}
			result = properties.get(key);
			if (result == null) {
				if ((lang != null) && (! lang.equals(Locale.ENGLISH.getLanguage()))) {
					result = nullSafeI18n(key, Locale.ENGLISH, values);
				}
				if (result == null) {
					result = key;
				}
			}

			if ((values != null) && (values.length > 0)) {
				result = MessageFormat.format(result, (Object[]) values);
			}
		}
		catch (@SuppressWarnings("unused") MissingResourceException e) {
		    utilLogger.warn("Could not find bundle \"resources.i18n\"");
		}

		return (result == null) ? key : result;
	}
	
	/**
	 * Internationalises a string for a particular locale and performs message formatting on tokens like {0}, {1} etc.
	 * Only returns null if the key is null.
	 */
	public static @Nullable String i18n(@Nullable String key, @Nullable Locale locale, String... values) {
		if (key == null) {
			return null;
		}
		return nullSafeI18n(key, locale, values);
	}

	public static boolean isRTL() {
		// NB Don't attempt to get a user unless persistence has been initialised
		User u = (AbstractPersistence.IMPLEMENTATION_CLASS == null) ? null : CORE.getUser();
		return isRTL((u == null) ? null : u.getLocale());
	}

	public static boolean isRTL(@Nullable Locale locale) {
		return (locale != null) && (! ComponentOrientation.getOrientation(locale).isLeftToRight());
	}

	/**
	 * Get the Country Name for a 2 letter country code in the current user's locale.
	 * @param twoLetterCountryCode	To convert.
	 * @return	The country name in the current user's locale, or if no user, the default system locale, or null if unknown. 
	 */
	public static @Nullable String countryNameFromCode(@Nonnull String twoLetterCountryCode) {
		User user = CORE.getUser();
		Locale userLocale = user.getLocale();
		Locale countryLocale = new Locale("", twoLetterCountryCode);
		return UtilImpl.processStringValue((userLocale == null) ?
												countryLocale.getDisplayCountry() :
												countryLocale.getDisplayCountry(userLocale));
	}
	
	/**
	 * Determine the length in bytes of a UTF-8 CharSequence.
	 * @param sequence	To determine the byte length of.
	 * @return	The byte length.
	 */
	public static int UTF8Length(@Nonnull CharSequence sequence) {
		int count = 0;
		for (int i = 0, len = sequence.length(); i < len; i++) {
			char ch = sequence.charAt(i);
			if (ch <= 0x7F) {
				count++;
			}
			else if (ch <= 0x7FF) {
				count += 2;
			}
			else if (Character.isHighSurrogate(ch)) {
				count += 4;
				++i;
			}
			else {
				count += 3;
			}
		}

		return count;
	}

	/**
	 * Get the lastIndexOf() for a regular expression.
	 * @param string	The String to search
	 * @param regex	The regex to search on.
	 * @return	The index of the last occurrence of the regex match.
	 */
	public static int lastIndexOfRegEx(@Nonnull String string, @Nonnull String regex) {
		int result = -1;

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(string);
		while (m.find()) {
			result = m.start();
		}

		return result;
	}

	/**
	 * Set any Skyve Persistent Beans found in the object to transient (not persisted).
	 * @param object	The object to set transient
	 */
	public static void setTransient(@Nullable Object object) {
		UtilImpl.setTransient(object);
	}

	/**
	 * Set the data group of any Skyve beans found in the object
	 * @param object	The object to set the data group for
	 * @param bizDataGroupId	The data group to set (or clear if null)
	 */
	public static void setDataGroup(@Nullable Object object, @Nullable String bizDataGroupId) {
		UtilImpl.setDataGroup(object, bizDataGroupId);
	}

	/**
	 * Make an instance of a document bean with random values for its properties.
	 * 
	 * @param <T> The type of Document bean to produce.
	 * @param user
	 * @param module
	 * @param document The document (corresponds to type T)
	 * @param depth How far to traverse the object graph - through associations and collections.
	 *        There are relationships that are never ending - ie Contact has Interactions which has User which has Contact
	 * @return The randomly constructed bean.
	 * @throws Exception
	 */
	public static @Nonnull <T extends Bean> T constructRandomInstance(@Nonnull User user,
																		@Nonnull Module module,
																		@Nonnull Document document,
																		int depth)
	throws Exception {
		return TestUtil.constructRandomInstance(user, module, document, depth);
	}

	public static @Nonnull String getContentDirectory() {
		return UtilImpl.CONTENT_DIRECTORY;
	}

	public static @Nonnull String getAddinsDirectory() {
		return (UtilImpl.ADDINS_DIRECTORY == null) ? (UtilImpl.CONTENT_DIRECTORY + "addins/") : UtilImpl.ADDINS_DIRECTORY;
	}
	
	public static @Nonnull String getBackupDirectory() {
		return (UtilImpl.BACKUP_DIRECTORY == null) ? UtilImpl.CONTENT_DIRECTORY : UtilImpl.BACKUP_DIRECTORY;
	}

	public static @Nonnull String getCacheDirectory() {
		return (UtilImpl.CACHE_DIRECTORY == null) ? (UtilImpl.CONTENT_DIRECTORY + "SKYVE_CACHE/") : UtilImpl.CACHE_DIRECTORY;
	}

	public static @Nonnull String getThumbnnailDirectory() {
		return (UtilImpl.THUMBNAIL_DIRECTORY == null) ? (UtilImpl.CONTENT_DIRECTORY + "SKYVE_THUMBNAILS/") : UtilImpl.THUMBNAIL_DIRECTORY;
	}

    public static ArchiveConfig getArchiveConfig() {
        return UtilImpl.ARCHIVE_CONFIG;
    }

	public static String getModuleDirectory() {
		return UtilImpl.MODULE_DIRECTORY;
	}

	public static String getPasswordHashingAlgorithm() {
		return UtilImpl.PASSWORD_HASHING_ALGORITHM;
	}
	
	public static String getSupportEmailAddress() {
		return ((UtilImpl.SUPPORT_EMAIL_ADDRESS == null) ? "" : UtilImpl.SUPPORT_EMAIL_ADDRESS);
	}

	private static volatile Boolean secureUrl = null;
	
	public static boolean isSecureUrl() {
		if (secureUrl == null) {
			synchronized (Util.class) {
				if (secureUrl == null) {
					secureUrl = Boolean.valueOf((UtilImpl.SERVER_URL == null) ? false : UtilImpl.SERVER_URL.startsWith("https://"));
				}
			}
		}
		return secureUrl.booleanValue();
	}
	
	public static String getServerUrl() {
		return UtilImpl.SERVER_URL;
	}

	public static String getSkyveContext() {
		return UtilImpl.SKYVE_CONTEXT;
	}

	public static String getSkyveContextRealPath() {
		return UtilImpl.SKYVE_CONTEXT_REAL_PATH;
	}

	public static String getHomeUri() {
		return UtilImpl.HOME_URI;
	}

	public static String getSkyveContextUrl() {
		return UtilImpl.SERVER_URL + UtilImpl.SKYVE_CONTEXT;
	}

	/**
	 * This is the base URL for the Skyve app - set via the "url" object in the JSON config.
	 * This is the url.server + url.context + '/'.
	 * @return	The base URL (Base HREF)
	 */
	public static @Nonnull String getBaseUrl() {
		StringBuilder result = new StringBuilder(128);
		result.append(UtilImpl.SERVER_URL).append(UtilImpl.SKYVE_CONTEXT).append('/');
		return result.toString();
	}

	/**
	 * The home page URL for the Skyve app - set via the "url" object in the JSON config.
	 * This is used as a default redirect after login.
	 * @return	The home URL.
	 */
	public static @Nonnull String getHomeUrl() {
		StringBuilder result = new StringBuilder(128);
		result.append(UtilImpl.SERVER_URL).append(UtilImpl.SKYVE_CONTEXT).append(UtilImpl.HOME_URI);
		return result.toString();
	}

	public static @Nonnull String getLoginUrl() {
		StringBuilder result = new StringBuilder(128);
		result.append(UtilImpl.SERVER_URL).append(UtilImpl.SKYVE_CONTEXT).append(UtilImpl.AUTHENTICATION_LOGIN_URI);
		return result.toString();
	}

	public static @Nonnull String getLoggedOutUrl() {
		StringBuilder result = new StringBuilder(128);
		result.append(UtilImpl.SERVER_URL).append(UtilImpl.SKYVE_CONTEXT).append(UtilImpl.AUTHENTICATION_LOGGED_OUT_URI);
		return result.toString();
	}

	public static @Nonnull String getDocumentUrl(@Nonnull String bizModule, @Nonnull String bizDocument) {
		return getDocumentUrl(bizModule, bizDocument, null);
	}

	public static @Nonnull String getDocumentUrl(@Nonnull String bizModule,
													@Nonnull String bizDocument,
													@Nullable String bizId) {
		StringBuilder result = new StringBuilder(128);

		result.append(UtilImpl.SERVER_URL).append(UtilImpl.SKYVE_CONTEXT).append(UtilImpl.HOME_URI);
		result.append("?a=e&m=").append(bizModule).append("&d=").append(bizDocument);
		if (bizId != null) {
			result.append("&i=").append(bizId);
		}

		return result.toString();
	}

	public static @Nonnull String getDocumentUrl(@Nonnull Bean bean) {
		return getDocumentUrl(bean.getBizModule(), bean.getBizDocument(), bean.getBizId());
	}
	
	/**
	 * Constructs a HTML anchor for the document instance
	 *  
	 * @param bizModule - the module of the document instance
	 * @param bizDocument - the document
	 * @param bizId - the bizId of the instance
	 * @param targetNewWindow - whether the target value for the anchor is a new window
	 * @param anchorMarkup - the html markup value (usually text) of the anchor 
	 * @return - the constructed URL as a String
	 */
	public static @Nonnull String getDocumentAnchorUrl(@Nonnull String bizModule,
														@Nonnull String bizDocument,
														@Nullable String bizId,
														boolean targetNewWindow,
														@Nonnull String anchorMarkup) {
		StringBuilder result = new StringBuilder(128);

		result.append("<a href=\"").append(getDocumentUrl(bizModule, bizDocument, bizId));
		if (targetNewWindow) {
			result.append("\" target=\"_blank\">");
		}
		else {
			result.append("\">");
		}
		result.append(anchorMarkup).append("</a>");

		return result.toString();
	}

	public static @Nonnull String getListUrl(@Nonnull String bizModule, @Nonnull String queryName) {
		StringBuilder result = new StringBuilder(128);

		result.append(UtilImpl.SERVER_URL).append(UtilImpl.SKYVE_CONTEXT).append(UtilImpl.HOME_URI);
		result.append("?a=l&m=").append(bizModule).append("&q=").append(queryName);

		return result.toString();
	}
	
	public static @Nonnull String getContentUrl(@Nonnull String bizModule,
													@Nonnull String bizDocument,
													@Nonnull String binding,
													@Nonnull String contentId) {
		StringBuilder result = new StringBuilder(128);

		result.append(UtilImpl.SERVER_URL).append(UtilImpl.SKYVE_CONTEXT).append(UtilImpl.HOME_URI);
		result.append("content?_n=").append(contentId);
		result.append("&_doc=").append(bizModule).append('.').append(bizDocument);
		result.append("&_b=").append(binding);

		return result.toString();
	}
	
	public static @Nonnull String getResourceUrl(@Nullable String bizModule,
													@Nullable String bizDocument,
													@Nonnull String resourceFileName) {
		StringBuilder result = new StringBuilder(128);

		result.append(UtilImpl.SERVER_URL).append(UtilImpl.SKYVE_CONTEXT).append(UtilImpl.HOME_URI);
		result.append("resources?_n=").append(resourceFileName);
		if ((bizModule != null) && (bizDocument != null)) {
			result.append("&_doc=").append(bizModule).append('.').append(bizDocument);
		}

		return result.toString();
	}

	public static @Nonnull String getResourceUrl(@Nonnull String resourceFileName) {
		return getResourceUrl(null, null, resourceFileName);
	}
	
	public static @Nonnull String getResetPasswordUrl() {
		StringBuilder result = new StringBuilder(128);
		result.append(Util.getSkyveContextUrl())
			  .append("/pages/resetPassword.jsp").append("?")
			  .append("t={passwordResetToken}").append("&")
			  .append(AbstractWebContext.CUSTOMER_COOKIE_NAME).append("={bizCustomer}");
		
		return result.toString();
	}
	
    /**
     * Constructs a HTML image URL for the given content item
     * (If the content item is not an image type, the content servlet returns a file type thumbnail)
     * 
	 * @param bizModule - the module of the document containing the content
	 * @param bizDocument - the document containing the content
	 * @param binding - the binding name of the content attribute
	 * @param contentId - the value of the content attribute (a String id of the item in the content store) 
	 * @param width - the width of the image in pixels
	 * @param height - the height of the image in pixels
	 * @return - the constructed URL as a String
     */
    public static @Nonnull String getContentImageUrl(@Nonnull String bizModule,
    													@Nonnull String bizDocument,
    													@Nonnull String binding,
    													@Nonnull String contentId,
    													int width,
    													int height) {
    	StringBuilder result = new StringBuilder(128);
    	
    	result.append("<img src=\"");
    	result.append(getContentUrl(bizModule, bizDocument, binding, contentId));
    	result.append("&_w=").append(width);
    	result.append("&_h=").append(height);
    	result.append("\"/>");
        
    	return result.toString();
    }

	/**
	 * Constructs an HTML anchor URL for the given content item
	 *  
	 * @param bizModule - the module of the document containing the content
	 * @param bizDocument - the document containing the content
	 * @param binding - the binding name of the content attribute
	 * @param contentId - the value of the content attribute (a String id of the item in the content store) 
	 * @param targetNewWindow - whether the target value for the anchor is a new window
	 * @param anchorMarkup - the html markup value (usually text) of the anchor 
	 * @return - the constructed URL as a String
	 */	
    public static @Nonnull String getContentAnchorUrl(@Nonnull String bizModule,
    													@Nonnull String bizDocument,
    													@Nonnull String binding,
    													@Nonnull String contentId,
    													boolean targetNewWindow,
    													@Nonnull String anchorMarkup) {
        String contentUrl = getContentUrl(bizModule, bizDocument, binding, contentId);
        StringBuilder result = new StringBuilder(128);
        
        result.append("<a href=\"").append(contentUrl);
        if (targetNewWindow) {
        	result.append("\" target=\"_blank\">");
        }
        else {
            result.append("\">");
        }
        result.append(anchorMarkup).append("</a>");
        
        return result.toString();
    }
	
	/**
	 * Constructs an HTML anchor with image URL for the given content item
	 *  
	 * @param bizModule - the module of the document containing the content
	 * @param bizDocument - the document containing the content
	 * @param binding - the binding name of the content attribute
	 * @param contentId - the value of the content attribute (a String id of the item in the content store) 
	 * @param targetNewWindow - whether the target value for the anchor is a new window
	 * @param width - the width of the image in pixels
	 * @param height - the height of the image in pixels
	 * @return - the constructed URL as a String
	 */
    public static @Nonnull String getContentAnchorWithImageUrl(@Nonnull String bizModule,
    															@Nonnull String bizDocument,
    															@Nonnull String binding,
    															@Nonnull String contentId,
    															boolean targetNewWindow,
    															int width,
    															int height) {
    	return getContentAnchorUrl(bizModule,
    								bizDocument,
    								binding,
    								contentId,
    								targetNewWindow,
    								getContentImageUrl(bizModule, bizDocument, binding, contentId, width, height));
    }

	/**
	 * Yields the HTML hex code for a given colour.
	 * @param colour	The colour
	 * @return	The colour code.
	 */
	public static @Nonnull String htmlColourCode(Color colour) {
		return '#' + StringUtils.leftPad(Integer.toHexString(colour.getRGB() & 0x00ffffff), 6, "0");
	}
	
	/**
	 * Yields the Color for a given HTML hex code.
	 * @param activity	The scope
	 * @return	The colour
	 */
	public static @Nonnull Color htmlColour(String colourCode) {
		return Color.decode(colourCode);
	}
	
	/**
	 * Appending or printing or writing to a Writer doesn't guarantee that the entire String will be written.
	 * This method writes 1K at a time.
	 * (Note that there was nothing quite the same in commons-io)
	 * 
	 * @param chars The CharSequence to copy.
	 * @param writer	The writer to copy to
	 * @throws IOException
	 */
	public static void chunkCharsToWriter(CharSequence chars, Writer writer)
	throws IOException {
		for (int i = 0, l = chars.length(); i < l; i += 1024) {
			int end = Math.min(i + 1024, l);
			writer.write(chars.subSequence(i, end).toString());
		}
	}
	
	/**
	 * Writing to an OutputStream doesn't guarantee that the entire byte array will be written.
	 * This method writes 1K at a time.
	 * 
	 * @param bytes	The bytes to copy.
	 * @param stream	The stream to copy to
	 * @throws IOException
	 */
	public static void chunkBytesToOutputStream(byte[] bytes, OutputStream stream)
	throws IOException {
		int length = bytes.length;
		int offset = 0;
		while (offset < length) {
			int bytesToWrite = Math.min(1024, length - offset);
			stream.write(bytes, offset, bytesToWrite);
			offset += bytesToWrite;
		}
	}
}
