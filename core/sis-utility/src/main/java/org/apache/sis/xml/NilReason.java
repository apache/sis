/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.xml;

import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.internal.jaxb.PrimitiveTypeProperties;


/**
 * Explanation for a missing XML element. The nil reason can be parsed and formatted as a
 * string using the {@link #valueOf(String)} and {@link #toString()} methods respectively.
 * The string can be either a {@link URI} or an enumeration value described below.
 * More specifically, {@code NilReason} can be:
 *
 * <ul>
 *   <li>One of the predefined {@link #INAPPLICABLE}, {@link #MISSING}, {@link #TEMPLATE},
 *       {@link #UNKNOWN} or {@link #WITHHELD} enumeration values.</li>
 *   <li>The {@link #OTHER} enumeration value, or a new enumeration value formatted as
 *       {@code "other:"} concatenated with a brief textual explanation.</li>
 *   <li>A URI which should refer to a resource which describes the reason for the exception.</li>
 * </ul>
 *
 * {@code NilReason} is used in a number of XML elements where it is necessary to permit
 * one of the above values as an alternative to the primary element.
 *
 * <div class="section">Immutability and thread safety</div>
 * This final class is immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see NilObject
 */
public final class NilReason implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5553785821187789895L;

    /**
     * There is no value.
     *
     * <p>The string representation is {@code "inapplicable"}.
     * Other properties (explanation and URI) are {@code null}.</p>
     */
    public static final NilReason INAPPLICABLE = new NilReason("inapplicable");

    /**
     * The correct value is not readily available to the sender of this data.
     * Furthermore, a correct value may not exist.
     *
     * <p>The string representation is {@code "missing"}.
     * Other properties (explanation and URI) are {@code null}.</p>
     */
    public static final NilReason MISSING = new NilReason("missing");

    /**
     * The value will be available later.
     *
     * <p>The string representation is {@code "template"}.
     * Other properties (explanation and URI) are {@code null}.</p>
     */
    public static final NilReason TEMPLATE = new NilReason("template");

    /**
     * The correct value is not known to, and not computable by, the sender of this data.
     * However, a correct value probably exists.
     *
     * <p>The string representation is {@code "unknown"}.
     * Other properties (explanation and URI) are {@code null}.</p>
     */
    public static final NilReason UNKNOWN = new NilReason("unknown");

    /**
     * The value is not divulged.
     *
     * <p>The string representation is {@code "withheld"}.
     * Other properties (explanation and URI) are {@code null}.</p>
     */
    public static final NilReason WITHHELD = new NilReason("withheld");

    /**
     * The {@value} string.
     */
    private static final String other = "other";

    /**
     * Other reason without explanation.
     * The string representation of this constant is {@code "other"}.
     * The explanation property is an empty string, and the URI is {@code null}.
     *
     * <div class="section">Providing an explanation</div>
     * Users are encouraged to use the {@link #valueOf(String)} method instead than this constant,
     * in order to provide a brief explanation. The string representation for {@code valueOf(…)}
     * is <code>"other:<var>explanation</var>"</code> where <var>explanation</var> is a string of
     * two or more characters with no included spaces.
     *
     * <p>When testing if a {@code NilReason} instance is any kind of {@code "other"} reason,
     * users should test if <code>{@linkplain #getOtherExplanation()} != null</code> instead
     * than comparing the reference against this constant.</p>
     */
    public static final NilReason OTHER = new NilReason(other);

    /**
     * List of predefined constants.
     */
    private static final NilReason[] PREDEFINED = {
        INAPPLICABLE, MISSING, TEMPLATE, UNKNOWN, WITHHELD, OTHER
    };

    /**
     * The pool of other nil reasons created up to date.
     */
    private static final WeakHashSet<NilReason> POOL = new WeakHashSet<NilReason>(NilReason.class);

    /**
     * Either the XML value as a {@code String} (including the explanation if the prefix
     * is "{@code other}", or an {@link URI}.
     */
    private final Object reason;

    /**
     * The invocation handler for {@link NilObject} instances, created when first needed.
     * The same handler can be shared for all objects having the same {@code NilReason},
     * no matter the interface they implement.
     */
    private transient InvocationHandler handler;

    /**
     * The values created by {@link #createNilObject(Class)}. They are often instances of {@link NilObject},
     * except for some JDK types like {@link String}, {@link Boolean} or {@link Integer} which are handled
     * in a special way.
     */
    private transient Map<Class<?>, Object> nilObjects;

    /**
     * Creates a new reason for the given XML value or the given URI.
     */
    private NilReason(final Object reason) {
        this.reason = reason;
    }

    /**
     * Returns an array containing every instances of this type that have not yet been
     * garbage collected. The first elements of the returned array are the constants
     * defined in this class, in declaration order. All other elements are the instances
     * created by the {@link #valueOf(String)} method, in no particular order.
     *
     * @return An array containing the instances of {@code NilReason}.
     */
    public static NilReason[] values() {
        final int predefinedCount = PREDEFINED.length;
        NilReason[] reasons;
        synchronized (POOL) {
            reasons = POOL.toArray(new NilReason[predefinedCount + POOL.size()]);
        }
        int count = reasons.length;
        while (count != 0 && reasons[count-1] == null) {
            count--;
        }
        count += predefinedCount;
        final NilReason[] source = reasons;
        if (count != reasons.length) {
            reasons = new NilReason[count];
        }
        System.arraycopy(source, 0, reasons, predefinedCount, count - predefinedCount);
        System.arraycopy(PREDEFINED, 0, reasons, 0, predefinedCount);
        return reasons;
    }

    /**
     * Parses the given nil reason. This method accepts the following cases:
     *
     * <ul>
     *   <li>If the given argument is one of the {@code "inapplicable"}, {@code "missing"},
     *       {@code "template"}, {@code "unknown"}, {@code "withheld"} or {@code "other"}
     *       strings (ignoring cases and leading/trailing spaces), then the corresponding
     *       pre-defined constant is returned.</li>
     *   <li>Otherwise if the given argument is {@code "other:"} followed by an explanation
     *       text, then an instance for that explanation is returned. More specifically:
     *       <ul>
     *         <li>{@linkplain Character#isSpaceChar(int) Space characters} and
     *             {@linkplain Character#isISOControl(int) ISO controls} are silently omitted.</li>
     *         <li>If the remaining string has less than two characters, then the {@link #OTHER}
     *             constant is returned.</li>
     *       </ul>
     *   </li>
     *   <li>Otherwise this method attempts to parse the given argument as a {@link URI}.
     *       Such URI should refer to a resource which describes the reason for the exception.</li>
     * </ul>
     *
     * This method returns existing instances when possible.
     *
     * @param  reason The reason why an element is not present.
     * @return The reason as a {@code NilReason} object.
     * @throws URISyntaxException If the given string is not one of the predefined enumeration
     *         values and can not be parsed as a URI.
     */
    public static NilReason valueOf(String reason) throws URISyntaxException {
        reason = CharSequences.trimWhitespaces(reason);
        int i = reason.indexOf(':');
        if (i < 0) {
            for (final NilReason candidate : PREDEFINED) {
                if (reason.equalsIgnoreCase((String) candidate.reason)) {
                    return candidate;
                }
            }
        } else {
            final int lower = CharSequences.skipLeadingWhitespaces(reason, 0,  i);
            final int upper = CharSequences.skipTrailingWhitespaces(reason, lower, i);
            if (reason.regionMatches(true, lower, other, 0, upper - lower)) {
                final int length = reason.length();
                final StringBuilder buffer = new StringBuilder(length).append(other);
                while (i < length) {
                    final int c = reason.codePointAt(i);
                    if (!Character.isSpaceChar(c) && !Character.isISOControl(c)) {
                        buffer.appendCodePoint(c);
                    }
                    i += Character.charCount(c);
                }
                if (buffer.length() < other.length() + 2) {
                    return OTHER;
                }
                String result = buffer.toString();
                if (result.equals(reason)) {
                    result = reason; // Use the existing instance.
                }
                return POOL.unique(new NilReason(result));
            }
        }
        return POOL.unique(new NilReason(new URI(reason)));
    }

    /**
     * Invoked after deserialization in order to return a unique instance if possible.
     *
     * @return A unique instance of the deserialized {@code NilReason}.
     */
    private Object readResolve() {
        if (reason instanceof String) {
            for (final NilReason candidate : PREDEFINED) {
                if (reason.equals(candidate.reason)) {
                    return candidate;
                }
            }
        }
        return POOL.unique(this);
    }

    /**
     * If this {@code NilReason} is an enumeration of kind {@link #OTHER}, returns the explanation
     * text. Otherwise returns {@code null}. If non-null, then the explanation is a string without
     * whitespace.
     *
     * <p>Note that in the special case where {@code this} nil reason is the {@link #OTHER}
     * instance itself, then this method returns an empty string. For all other cases, the
     * string contains at least two characters.</p>
     *
     * @return The explanation, or {@code null} if this {@code NilReason} is not of kind {@link #OTHER}.
     */
    public String getOtherExplanation() {
        if (reason instanceof String) {
            final String text = (String) reason;
            final int s = text.indexOf(':');
            if (s >= 0) {
                return text.substring(s + 1);
            }
            if (text.equals(other)) {
                return "";
            }
        }
        return null;
    }

    /**
     * If the explanation of this {@code NilReason} is referenced by a URI, returns that URI.
     * Otherwise returns {@code null}. The URI and the {@linkplain #getOtherExplanation() other
     * explanation} attributes are mutually exclusive.
     *
     * @return The URI, or {@code null} if the explanation of this {@code NilReason}
     *         is not referenced by a URI.
     */
    public URI getURI() {
        return (reason instanceof URI) ? (URI) reason : null;
    }

    /**
     * Returns the GML string representation of this {@code NilReason}. The returned string
     * is a simple enumeration value (e.g. {@code "inapplicable"}) if this {@code NilReason}
     * is one of the predefined constants, or a string of the form {@code "other:explanation"},
     * or a URI.
     *
     * @return The GML string representation of this {@code NilReason}.
     */
    @Override
    public String toString() {
        return reason.toString();
    }

    /**
     * Returns a hash code value for this {@code NilReason}.
     */
    @Override
    public int hashCode() {
        return reason.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Compares this {@code NilReason} with the specified object for equality.
     *
     * @param other The object to compare with this {@code NilReason}.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof NilReason) {
            return reason.equals(((NilReason) other).reason);
        }
        return false;
    }

    /**
     * Returns an object of the given type which is nil for the reason represented by this instance.
     * The {@code type} argument can be one of the following cases:
     *
     * <ul class="verbose">
     *   <li>An <strong>interface</strong>: in such case, this method returns an object which implement the given
     *       interface together with the {@link NilObject} and {@link LenientComparable} interfaces:
     *       <ul>
     *         <li>The {@link NilObject#getNilReason()} method will return this {@code NilReason} instance.</li>
     *         <li>The {@code equals(…)} and {@code hashCode()} methods behave as documented in {@link LenientComparable}.</li>
     *         <li>The {@code toString()} method is unspecified (may contain debugging information).</li>
     *         <li>All other methods return an empty collections, empty arrays, {@code null}, {@link Double#NaN NaN},
     *             {@code 0} or {@code false}, in this preference order, depending on the method return type.</li>
     *       </ul>
     *   </li>
     *   <li>One of {@code Boolean}, {@link Byte}, {@link Short}, {@code Integer}, {@link Long}, {@link Float},
     *       {@code Double} or {@code String} types: in such case, this method returns a specific instance which
     *       will be recognized as "nil" by the XML marshaller.</li>
     * </ul>
     *
     * @param  <T> The compile-time type of the {@code type} argument.
     * @param  type The object type as an <strong>interface</strong>
     *         (usually a <a href="http://www.geoapi.org">GeoAPI</a> one) or one of the special types.
     * @throws IllegalArgumentException If the given type is not a supported type.
     * @return An {@link NilObject} of the given type.
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T createNilObject(final Class<T> type) {
        ArgumentChecks.ensureNonNull("type", type);
        /*
         * Check for existing instance in the cache before to create a new object. Returning a unique
         * instance is mandatory for the types handled by 'createNilPrimitive(Class)'. Since we have
         * to cache those values anyway, we opportunistically extend the caching to other types too.
         *
         * Implementation note: we have two synchronizations here: one lock on 'this' because of the
         * 'synchronized' statement in this method signature, and an other lock in WeakValueHashMap.
         * The second lock may seem useless since we already hold a lock on 'this'. But it is actually
         * needed because the garbage-collected entries are removed from the map in a background thread
         * (see ReferenceQueueConsumer), which is synchronized on the map itself. It is better to keep
         * the synchronization on the map shorter than the snychronization on 'this' because a single
         * ReferenceQueueConsumer thread is shared by all the SIS library.
         */
        if (nilObjects == null) {
            nilObjects = new WeakValueHashMap<Class<?>, Object>((Class) Class.class);
        }
        Object object = nilObjects.get(type);
        if (object == null) {
            /*
             * If no object has been previously created, check for the usual case where the requested type
             * is an interface. We still have a special case for InternationalString. For all other cases,
             * we will rely on the proxy.
             */
            if (type.isInterface()) {
                if (NilObjectHandler.isIgnoredInterface(type)) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
                }
                if (type == InternationalString.class) {
                    object = new NilInternationalString(this);
                } else {
                    if (handler == null) {
                        handler = new NilObjectHandler(this);
                    }
                    object = Proxy.newProxyInstance(NilReason.class.getClassLoader(),
                            new Class<?>[] {type, NilObject.class, LenientComparable.class}, handler);
                }
            } else {
                /*
                 * If the requested type is not an interface, this is usually an error except for some
                 * special cases: Boolean, Byte, Short, Integer, Long, Float, Double or String.
                 */
                object = createNilPrimitive(type);
                PrimitiveTypeProperties.associate(object, this);
            }
            if (nilObjects.put(type, object) != null) {
                throw new AssertionError(type); // Should never happen.
            }
        }
        return type.cast(object);
    }

    /**
     * Returns a new {@code Boolean}, {@link Byte}, {@link Short}, {@code Integer}, {@link Long},
     * {@link Float}, {@code Double} or {@code String} instance to be considered as a nil value.
     * The caller is responsible for registering the value in {@link PrimitiveTypeProperties}.
     *
     * <p><b>REMINDER:</b> If more special cases are added, do not forget to update the {@link #mayBeNil(Object)}
     * method and to update the {@link #createNilObject(Class)} and {@link #forObject(Object)} javadoc.</p>
     *
     * <div class="note"><b>Implementation note:</b>
     * There is no special case for {@link Character} because Java {@code char}s are not really full Unicode characters.
     * They are parts of UTF-16 encoding instead. If there is a need to represent a single Unicode character, we should
     * probably still use a {@link String} where the string contain 1 or 2 Java characters. This may also facilitate the
     * encoding in the XML files, since many files use an other encoding than UTF-16 anyway.</div>
     *
     * @throws IllegalArgumentException If the given type is not a supported type.
     */
    @SuppressWarnings({"RedundantStringConstructorCall", "BooleanConstructorCall"})
    private static Object createNilPrimitive(final Class<?> type) {
        if (type == String .class) return new String("");         // REALLY need a new instance.
        if (type == Boolean.class) return new Boolean(false);     // REALLY need a new instance, not Boolean.FALSE.
        if (type == Byte   .class) return new Byte((byte) 0);     // REALLY need a new instance, not Byte.valueOf(0).
        if (type == Short  .class) return new Short((byte) 0);    // REALLY need a new instance, not Short.valueOf(0).
        if (type == Integer.class) return new Integer(0);         // REALLY need a new instance, not Integer.valueOf(0).
        if (type == Long   .class) return new Long(0);            // REALLY need a new instance, not Long.valueOf(0).
        if (type == Float  .class) return new Float(Float.NaN);   // REALLY need a new instance, not Float.valueOf(…).
        if (type == Double .class) return new Double(Double.NaN); // REALLY need a new instance, not Double.valueOf(…).
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
    }

    /**
     * Returns {@code true} if the given object may be one of the sentinel values
     * created by {@link #createNilPrimitive(Class)}. This method only checks the value.
     * If this method returns {@code true}, then the caller still need to check the actual instance using the
     * {@link PrimitiveTypeProperties} class. The purpose of this method is to filter the values that can not
     * be sentinel values, in order to avoid the synchronization done by {@code PrimitiveTypeProperties}.
     */
    private static boolean mayBeNil(final Object object) {
        // 'instanceof' checks on instances of final classes are expected to be very fast.
        if (object instanceof String)  return ((String) object).isEmpty();
        if (object instanceof Boolean) return !((Boolean) object) && (object != Boolean.FALSE);
        if (object instanceof Number) {
            /*
             * Following test may return false positives for Long, Float and Double types, but this is okay
             * since the real check will be done by PrimitiveTypeProperties.  The purpose of this method is
             * only to perform a cheap filtering. Note that this method relies on the fact that casting NaN
             * to 'int' produces 0.
             */
            return ((Number) object).intValue() == 0;
        }
        return false;
    }

    /**
     * If the given object is nil, returns the reason why it does not contain information.
     * This method performs the following choices:
     *
     * <ul>
     *   <li>If the given object implements the {@link NilObject} interface, then this method delegates
     *       to the {@link NilObject#getNilReason()} method.</li>
     *   <li>Otherwise if the given object is one of the {@code Boolean}, {@link Byte}, {@link Short}, {@code Integer},
     *       {@link Long}, {@link Float}, {@code Double} or {@code String} instances returned by
     *       {@link #createNilObject(Class)}, then this method returns the associated reason.</li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * @param  object The object for which to get the {@code NilReason}, or {@code null}.
     * @return The reason why the given object contains no information,
     *         or {@code null} if the given object is not nil.
     *
     * @see NilObject#getNilReason()
     *
     * @since 0.4
     */
    public static NilReason forObject(final Object object) {
        if (object != null) {
            if (object instanceof NilObject) {
                return ((NilObject) object).getNilReason();
            }
            if (mayBeNil(object)) {
                return (NilReason) PrimitiveTypeProperties.property(object);
            }
        }
        return null;
    }
}
