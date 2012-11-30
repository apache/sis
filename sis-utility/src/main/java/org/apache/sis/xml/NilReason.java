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

import java.net.URI;
import java.net.URISyntaxException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.collection.WeakHashSet;


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
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 *
 * @see NilObject
 */
@Immutable
public final class NilReason implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1302619137838086028L;

    /**
     * There is no value.
     *
     * <p>The string representation is {@code "inapplicable"}.</p>
     */
    public static final NilReason INAPPLICABLE = new NilReason("inapplicable");

    /**
     * The correct value is not readily available to the sender of this data.
     * Furthermore, a correct value may not exist.
     *
     * <p>The string representation is {@code "missing"}.</p>
     */
    public static final NilReason MISSING = new NilReason("missing");

    /**
     * The value will be available later.
     *
     * <p>The string representation is {@code "template"}.</p>
     */
    public static final NilReason TEMPLATE = new NilReason("template");

    /**
     * The correct value is not known to, and not computable by, the sender of this data.
     * However, a correct value probably exists.
     *
     * <p>The string representation is {@code "unknown"}.</p>
     */
    public static final NilReason UNKNOWN = new NilReason("unknown");

    /**
     * The value is not divulged.
     *
     * <p>The string representation is {@code "withheld"}.</p>
     */
    public static final NilReason WITHHELD = new NilReason("withheld");

    /**
     * Other reason without explanation. Users are encouraged to use the {@link #valueOf(String)}
     * method instead than this constant, in order to provide a brief explanation.
     *
     * <p>When testing if a {@code NilReason} is {@code "other"}, users should test if
     * <code>{@linkplain #getExplanation()} != null</code> instead than comparing
     * against this constant.</p>
     *
     * <p>The string representation of this constant is {@code "other"}.
     * The string representation of other values created by {@code valueOf(String)} is
     * {@code "other:text"} where {@code text} is a string of two or more characters
     * with no included spaces.</p>
     */
    public static final NilReason OTHER = new NilReason("other");

    /**
     * List of predefined constants.
     */
    private static final NilReason[] PREDEFINED = {
        INAPPLICABLE, MISSING, TEMPLATE, UNKNOWN, WITHHELD, OTHER
    };

    /**
     * The pool of other nil reasons created up to date.
     */
    private static final WeakHashSet<NilReason> POOL = new WeakHashSet<>(NilReason.class);

    /**
     * Either the XML value as a {@code String} (including the explanation if the prefix
     * is "{@code other}", or an {@link URI}.
     */
    private final Object reason;

    /**
     * The invocation handler for empty objects, created when first needed.
     * The same handler can be shared for all objects.
     */
    private transient InvocationHandler handler;

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
     *       text, then a new instance is created and returned for that explanation.
     *       Note that if the given explanation contains any characters that are not
     *       {@linkplain Character#isUnicodeIdentifierPart(char) unicode identifier}
     *       (for example white spaces), then those characters are omitted.</li>
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
            if (reason.regionMatches(true, lower, "other", 0, upper - lower)) {
                final int length = reason.length();
                final StringBuilder buffer = new StringBuilder(length).append("other:");
                i++; // Skip the ':' character.
                while (i < length) {
                    final int c = reason.codePointAt(i);
                    if (!Character.isSpaceChar(c) && !Character.isISOControl(c)) {
                        buffer.appendCodePoint(c);
                    }
                    i += Character.charCount(c);
                }
                if (buffer.length() == 6) { // 6 is the length of "other:"
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
     * instance itself, then this method returns an empty string.</p>
     *
     * @return The explanation, or {@code null} if this {@code NilReason} is not of kind {@link #OTHER}.
     */
    public String getExplanation() {
        if (reason instanceof String) {
            final String text = (String) reason;
            final int s = text.indexOf(':');
            if (s >= 0) {
                return text.substring(s + 1);
            }
            if (text.equals("other")) {
                return "";
            }
        }
        return null;
    }

    /**
     * If the explanation of this {@code NilReason} is referenced by a URI, returns that URI.
     * Otherwise returns {@code null}.
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
     * is one of the predefined constants, or a string of the form {@code "other:text"}, or
     * a URI.
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
     * This method returns an object which implement the given interface together with the
     * {@link NilObject} interface. The {@link NilObject#getNilReason()} method will return
     * this {@code NilReason} instance, and all other methods (except the ones inherited from
     * the {@code Object} class) will return an empty collection, empty array, {@code null},
     * {@link Double#NaN NaN}, {@code 0} or {@code false}, in this preference order,
     * depending on the method return type.
     *
     * @param  <T> The compile-time type of the {@code type} argument.
     * @param  type The object type as an <strong>interface</strong>.
     *         This is usually a <a href="http://www.geoapi.org">GeoAPI</a> interface.
     * @return An {@link NilObject} of the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> T createNilObject(final Class<T> type) {
        ArgumentChecks.ensureNonNull("type", type);
        if (NilObjectHandler.isIgnoredInterface(type)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
        }
        InvocationHandler h;
        synchronized (this) {
            if ((h = handler) == null) {
                handler = h = new NilObjectHandler(this);
            }
        }
        return (T) Proxy.newProxyInstance(NilReason.class.getClassLoader(),
                new Class<?>[] {type, NilObject.class, LenientComparable.class}, h);
    }
}
