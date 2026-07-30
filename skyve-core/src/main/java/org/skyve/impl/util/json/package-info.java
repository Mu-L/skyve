/**
 * Low-level JSON reading, writing, and minification utilities.
 *
 * <p>{@code JSONReader} parses a JSON string into Java objects (maps, lists, and
 * primitives) without requiring a full object-mapping framework. {@code JSONWriter}
 * serialises Java objects, including self-describing Java records, to a JSON string.
 * Typed reads can also reconstruct class-free records through their canonical
 * constructors. When supplied with a user context, readers reconstruct Skyve
 * document instances nested within records, Java objects, maps, and collections.
 * {@code Minifier} strips whitespace and comments from JSON strings to reduce payload
 * size.
 *
 * <p>These utilities are intentionally lightweight and independent of Jackson or Gson
 * so that they can be used in contexts (such as Skyve metadata loading) that run before
 * third-party libraries are fully initialised.
 *
 * @see org.skyve.impl.util
 */
package org.skyve.impl.util.json;
