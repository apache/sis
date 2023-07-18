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
import java.io.ObjectStreamException;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.internal.jaxb.FinalClassExtensions;
import org.apache.sis.math.MathFunctions;


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
 * <h2>Immutability and thread safety</h2>
 * This final class is immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see NilObject
 *
 * @since 0.3
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
     * <h4>Providing an explanation</h4>
     * Users are encouraged to use the {@link #valueOf(String)} method instead of this constant,
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
     *
     * @see #unique()
     */
    private static final WeakHashSet<NilReason> POOL = new WeakHashSet<>(NilReason.class);

    /**
     * The last {@linkplain #ordinal} value used in a {@code NilReason} instance.
     * This is used for choosing the next value to assign to {@link #ordinal}.
     * This value is usually equal to {@code PREDEFINED.length + POOL.size()}
     * but may be different if some pool entries have been garbage collected.
     * Accesses to this field shall be synchronized on {@link #POOL}.
     */
    private static int lastOrdinal;

    /**
     * A numerical identifier of this {@code NilReason}, or 0 if not yet assigned.
     * This field is used for choosing a NaN value for floating point numbers.
     * We want the same NaN to be used for {@code float} and {@code double} types.
     * This value shall not be modified after {@code NilReason} publication.
     *
     * @see #ordinal()
     * @see #forNumber(Number)
     */
    private transient int ordinal;
    static {
        int i = 0;
        while (i < PREDEFINED.length) {
            PREDEFINED[i].ordinal = ++i;
        }
        lastOrdinal = i;
    }

    /**
     * Either the XML value as a {@code String} (including the explanation if the prefix
     * is "{@code other}", or a {@link URI}. Those types are serializable.
     */
    @SuppressWarnings("serial")
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
     * Returns an array containing every instances of this type that have not yet been garbage collected.
     * The first elements of the returned array are the constants defined in this class, in declaration order.
     * All other elements are the instances created by the {@link #valueOf(String)} method, in no particular order.
     *
     * @return an array containing the instances of {@code NilReason}.
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
     *       predefined constant is returned.</li>
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
     * @param  reason  the reason why an element is not present.
     * @return the reason as a {@code NilReason} object.
     * @throws URISyntaxException if the given string is not one of the predefined enumeration
     *         values and cannot be parsed as a URI.
     */
    public static NilReason valueOf(String reason) throws URISyntaxException {
        reason = reason.strip();
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
                    result = reason;                            // Use the existing instance.
                }
                return new NilReason(result).unique();
            }
        }
        return new NilReason(new URI(reason)).unique();
    }

    /**
     * Returns a unique instance of this nil reason.
     * If another instance has been created previously for the same reason,
     * that other instance is returned. Otherwise {@code this} is returned.
     *
     * @return a unique instance of this nil reason.
     */
    private NilReason unique() {
        synchronized (POOL) {
            final NilReason instance = POOL.unique(this);
            if (instance == this) {
                if ((instance.ordinal = ++lastOrdinal) < 0) {
                    /*
                     * Overflow, but still accept to create this nil reason.
                     * Maybe it will not be used for floating point numbers.
                     */
                    instance.ordinal = lastOrdinal = Integer.MIN_VALUE;     // Really MIN, not MAX.
                }
            }
            return instance;
        }
    }

    /**
     * Invoked after deserialization in order to return a unique instance if possible.
     *
     * @return a unique instance of the deserialized {@code NilReason}.
     * @throws ObjectStreamException required by specification but should never be thrown.
     */
    private Object readResolve() throws ObjectStreamException {
        if (reason instanceof String) {
            for (final NilReason candidate : PREDEFINED) {
                if (reason.equals(candidate.reason)) {
                    return candidate;
                }
            }
        }
        return unique();
    }

    /**
     * Returns an ordinal value for this nil reason.
     * Numbers start at 1.
     *
     * @return an ordinal value for this nil reason, numbered from 1.
     * @throws ArithmeticException if there is too many {@code NilReason} instances.
     *
     * @see #forNumber(Number)
     */
    final int ordinal() {
        if (ordinal > 0) return ordinal;
        throw new ArithmeticException(Errors.format(Errors.Keys.IntegerOverflow_1, Integer.SIZE));
    }

    /**
     * If this {@code NilReason} is an enumeration of kind {@link #OTHER}, returns the explanation text.
     * Otherwise returns {@code null}. If non-null, then the explanation is a string without whitespace.
     *
     * <p>Note that in the special case where {@code this} nil reason is the {@link #OTHER} instance itself,
     * then this method returns an empty string. For all other cases, the string contains at least two characters.</p>
     *
     * @return the explanation, or {@code null} if this {@code NilReason} is not of kind {@link #OTHER}.
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
     * @return the URI, or {@code null} if the explanation of this {@code NilReason} is not referenced by a URI.
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
     * @return the GML string representation of this {@code NilReason}.
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
     * @param  other  the object to compare with this {@code NilReason}.
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
     *   <li>One of {@link Float}, {@link Double} or {@link String} types.
     *       In such case, this method returns an instance which will be recognized as "nil" by the XML marshaller.</li>
     * </ul>
     *
     * <h4>Historical note</h4>
     * In previous Apache SIS releases, this method recognized also {@code Boolean}, {@link Byte}, {@link Short},
     * {@code Integer}, {@link Long}, {@link Float} and {@code Double} types in the same way as {@code String}.
     * The support for those types has been removed in Apache SIS 1.4 (except for types supporting NaN values)
     * because it depends on {@code java.lang} constructors now marked as deprecated for removal.
     * See <a href="https://issues.apache.org/jira/browse/SIS-586">SIS-586</a> on JIRA issue tracker.
     *
     * @param  <T>   the compile-time type of the {@code type} argument.
     * @param  type  the object type as an <strong>interface</strong>
     *         (usually a <a href="http://www.geoapi.org">GeoAPI</a> one) or one of the special types.
     * @throws IllegalArgumentException if the given type is not a supported type.
     * @return an {@link NilObject} of the given type.
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T createNilObject(final Class<T> type) {
        ArgumentChecks.ensureNonNull("type", type);
        /*
         * Check for existing instance in the cache before to create a new object. Returning a unique
         * instance is mandatory for the types handled by `createNilInstance(Class)`. Since we have
         * to cache those values anyway, we opportunistically extend the caching to other types too.
         *
         * Implementation note: we have two synchronizations here: one lock on `this` because of the
         * `synchronized` statement in this method signature, and another lock in `WeakValueHashMap`.
         * The second lock may seem useless since we already hold a lock on `this`. But it is actually
         * needed because the garbage-collected entries are removed from the map in a background thread
         * (see ReferenceQueueConsumer), which is synchronized on the map itself. It is better to keep
         * the synchronization on the map shorter than the snychronization on `this` because a single
         * ReferenceQueueConsumer thread is shared by all the SIS library.
         */
        if (nilObjects == null) {
            nilObjects = new WeakValueHashMap<>((Class) Class.class);
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
                 * If the requested type is not an interface, this is usually an error except for some special cases:
                 * Float, Double and String (Apache SIS 1.3 supported also: Boolean, Byte, Short, Integer and Long).
                 */
                object = createNilInstance(type);
            }
            if (nilObjects.put(type, object) != null) {
                throw new AssertionError(type);                                 // Should never happen.
            }
        }
        return type.cast(object);
    }

    /**
     * Returns a new {@code Float}, {@code Double} or {@code String} instance to be considered as a nil value.
     *
     * <p><b>Reminder:</b> If more special cases are added, do not forget to update the {@link #forObject(Object)}
     * method and to update the {@link #createNilObject(Class)} and {@link #forObject(Object)} javadoc.</p>
     *
     * <h4>Implementation note</h4>
     * There is no special case for {@link Character} because Java {@code char}s are not really full Unicode characters.
     * They are parts of UTF-16 encoding instead. If there is a need to represent a single Unicode character, we should
     * probably still use a {@link String} where the string contain 1 or 2 Java characters. This may also facilitate the
     * encoding in the XML files, since many files use another encoding than UTF-16 anyway.
     *
     * @throws IllegalArgumentException if the given type is not a supported type.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-586">SIS-586</a>
     */
    @SuppressWarnings({"RedundantStringConstructorCall", "UnnecessaryBoxing"})
    private Object createNilInstance(final Class<?> type) {
        if (type == Double .class) return Double.valueOf(MathFunctions.toNanFloat(ordinal()));
        if (type == Float  .class) return Float .valueOf(MathFunctions.toNanFloat(ordinal()));
        final Object object;
        if (type == String .class) {
            object = new String("");         // REALLY need a new instance.
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
        }
        FinalClassExtensions.associate(object, this);
        return object;
    }

    /**
     * If the given object is nil, returns the reason why it does not contain information.
     * This method performs the following choices:
     *
     * <ul>
     *   <li>If the given object implements the {@link NilObject} interface, then this method delegates
     *       to the {@link NilObject#getNilReason()} method.</li>
     *   <li>Otherwise if the given object is one of the {@link Float}, {@link Double} or {@link String} instances
     *       returned by {@link #createNilObject(Class)}, then this method returns the associated reason.</li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * @param  object  the object for which to get the {@code NilReason}, or {@code null}.
     * @return the reason why the given object contains no information, or {@code null} if the given object is not nil.
     *
     * @see NilObject#getNilReason()
     *
     * @since 0.4
     */
    public static NilReason forObject(final Object object) {
        if (object != null) {
            if (object instanceof NilObject) {
                return ((NilObject) object).getNilReason();
            } else if (object instanceof Double) {
                final Double value = (Double) object;
                if (value.isNaN()) {
                    return forNumber(value);
                }
            } else if (object instanceof Float) {
                final Float value = (Float) object;
                if (value.isNaN()) {
                    return forNumber(value);
                }
            } else if (object instanceof String) {
                return (NilReason) FinalClassExtensions.property(object);
            }
        }
        return null;
    }

    /**
     * Returns the nil reason for the ordinal value extracted from the given floating point value.
     * Caller should have verified that the value is NaN before to invoke this method.
     *
     * @param  value  the floating point value for which to search the nil reason.
     * @return the nil reason, or {@code null} if none has been found for the given ordinal.
     *
     * @see #ordinal()
     */
    private static NilReason forNumber(final Number value) {
        try {
            final int ordinal = MathFunctions.toNanOrdinal(value.floatValue());
            if (ordinal >= 1) {
                if (ordinal <= PREDEFINED.length) {
                    return PREDEFINED[ordinal - 1];
                }
                synchronized (POOL) {
                    for (final NilReason reason : POOL) {       // Should be a very small set, usually empty.
                        if (reason.ordinal == ordinal) {
                            return reason;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            // Unsupported bit pattern. Ignore.
        }
        return null;
    }
}
