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
package org.apache.sis.util;

import java.io.Serializable;
import java.util.StringTokenizer;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.internal.system.Modules.MAJOR_VERSION;
import static org.apache.sis.internal.system.Modules.MINOR_VERSION;


/**
 * Holds a version number as a sequence of strings separated by either a dot or a dash.
 * The first three strings, usually numbers, are called respectively {@linkplain #getMajor() major},
 * {@linkplain #getMinor() minor} and {@linkplain #getRevision() revision}.
 * For example a version code such as {@code "6.11.2"} will have major number 6, minor
 * number 11 and revision number 2. Alternatively a version code such as {@code "3.18-SNAPSHOT"}
 * will have major version number 3, minor version number 18 and revision string "SNAPSHOT".
 *
 * <p>This class provides methods for performing comparisons of {@code Version} objects where major,
 * minor and revision parts are compared as numbers when possible, or as strings otherwise.</p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus inherently thread-safe.
 * Subclasses may or may not be immutable, at implementation choice. But implementors are
 * encouraged to make sure that subclasses remain immutable for more predictable behavior.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public class Version implements CharSequence, Comparable<Version>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8402041502662929792L;

    /**
     * The separator characters between {@linkplain #getMajor() major}, {@linkplain #getMinor() minor}
     * and {@linkplain #getRevision() revision} components. Any character in this string fits.
     */
    private static final String SEPARATORS = ".-";

    /**
     * The version of this Apache SIS distribution.
     */
    public static final Version SIS = new Version(MAJOR_VERSION + "." + MINOR_VERSION + "-SNAPSHOT");

    /**
     * A few commonly used version numbers. This list is based on SIS needs, e.g. in {@code DataStore} implementations.
     * New constants are likely to be added in any future SIS versions.
     *
     * @see #valueOf(int[])
     */
    private static final Version[] CONSTANTS = {
        new Version("1"),
        new Version("2")
    };

    /**
     * The version in string form, with leading and trailing spaces removed.
     */
    private final String version;

    /**
     * The components of the version string. Will be created when first needed.
     */
    private transient String[] components;

    /**
     * The parsed components of the version string. Will be created when first needed.
     */
    private transient Comparable<?>[] parsed;

    /**
     * The hash code value. Will be computed when first needed.
     */
    private transient int hashCode;

    /**
     * Creates a new version object from the supplied string.
     *
     * @param version The version as a string.
     */
    public Version(final String version) {
        ArgumentChecks.ensureNonNull("version", version);
        this.version = version;
    }

    /**
     * Returns an instance for the given integer values.
     * The {@code components} array must contain at least 1 element, where:
     *
     * <ul>
     *   <li>The first element is the {@linkplain #getMajor() major} number.</li>
     *   <li>The second element (if any) is the {@linkplain #getMinor() minor} number.</li>
     *   <li>The third element (if any) is the {@linkplain #getRevision() revision} number.</li>
     *   <li>Other elements (if any) will be appended to the {@link #toString() string value}.</li>
     * </ul>
     *
     * @param  components The major number, optionally followed by minor, revision or other numbers.
     * @return A new or existing instance of {@code Version} for the given numbers.
     *
     * @since 0.4
     */
    public static Version valueOf(final int... components) {
        if (components.length == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "components"));
        }
        final Version version;
        final int major = components[0];
        if (components.length == 1) {
            if (major >= 1 && major <= CONSTANTS.length) {
                return CONSTANTS[major - 1];
            } else {
                version = new Version(Integer.toString(major));
            }
        } else {
            final StringBuilder buffer = new StringBuilder().append(major);
            for (int i=1; i<components.length; i++) {
                buffer.append('.').append(components[i]);
            }
            version = new Version(buffer.toString());
        }
        /*
         * Pre-compute the 'parsed' array since we already have the integer values. It will avoid the need to
         * create the 'this.components' array and to parse the String values if a 'getFoo()' method is invoked.
         * Note that the cost is typically only the 'parsed' array creation, not Integer objects creation, since
         * version numbers are usually small enough for allowing 'Integer.valueOf(int)' to cache them.
         */
        final Integer[] parsed = new Integer[components.length];
        for (int i=0; i<components.length; i++) {
            parsed[i] = components[i];
        }
        version.parsed = parsed;
        return version;
    }

    /**
     * Returns the major version number. This method returns an {@link Integer} if possible,
     * or a {@link String} otherwise.
     *
     * @return The major version number.
     */
    public Comparable<?> getMajor() {
        return getComponent(0);
    }

    /**
     * Returns the minor version number. This method returns an {@link Integer} if possible,
     * or a {@link String} otherwise. If there is no minor version number, then this method
     * returns {@code null}.
     *
     * @return The minor version number, or {@code null} if none.
     */
    public Comparable<?> getMinor() {
        return getComponent(1);
    }

    /**
     * Returns the revision number. This method returns an {@link Integer} if possible,
     * or a {@link String} otherwise. If there is no revision number, then this method
     * returns {@code null}.
     *
     * @return The revision number, or {@code null} if none.
     */
    public Comparable<?> getRevision() {
        return getComponent(2);
    }

    /**
     * Returns the specified components of this version string. For a version of the
     * {@code major.minor.revision} form, index 0 stands for the major version number,
     * 1 stands for the minor version number and 2 stands for the revision number.
     *
     * <p>The return value is an {@link Integer} if the component is parsable as an integer,
     * or a {@link String} otherwise. If there is no component at the specified index,
     * then this method returns {@code null}.</p>
     *
     * @param  index The index of the component to fetch.
     * @return The value at the specified index, or {@code null} if none.
     * @throws IndexOutOfBoundsException if {@code index} is negative.
     */
    final synchronized Comparable<?> getComponent(final int index) {
        if (parsed == null) {
            if (components == null) {
                final StringTokenizer tokens = new StringTokenizer(version, SEPARATORS);
                components = new String[tokens.countTokens()];
                for (int i=0; tokens.hasMoreTokens(); i++) {
                    components[i] = tokens.nextToken();
                }
            }
            parsed = new Comparable<?>[components.length];
        }
        if (index >= parsed.length) {
            return null;
        }
        Comparable<?> candidate = parsed[index];
        if (candidate == null) {
            final String value = CharSequences.trimWhitespaces(components[index]);
            try {
                candidate = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                candidate = value;
            }
            parsed[index] = candidate;
        }
        return candidate;
    }

    /**
     * Get the rank of the specified object according this type.
     * This is for {@link #compareTo(Version, int)} internal only.
     */
    private static int getTypeRank(final Object value) {
        if (value instanceof CharSequence) {
            return 0;
        }
        if (value instanceof Number) {
            return 1;
        }
        throw new IllegalArgumentException(String.valueOf(value));
    }

    /**
     * Compares this version with an other version object, up to the specified limit. A limit
     * of 1 compares only the {@linkplain #getMajor() major} version number. A limit of 2 compares
     * the major and {@linkplain #getMinor() minor} version numbers, <i>etc</i>.
     * The comparisons are performed as {@link Integer} object if possible, or as {@link String}
     * otherwise.
     *
     * @param  other The other version object to compare with.
     * @param  limit The maximum number of components to compare.
     * @return A negative value if this version is lower than the supplied version, a positive
     *         value if it is higher, or 0 if they are equal.
     */
    public int compareTo(final Version other, final int limit) {
        ArgumentChecks.ensureNonNull ("other", other);
        ArgumentChecks.ensurePositive("limit", limit);
        for (int i=0; i<limit; i++) {
            final Comparable<?> v1 =  this.getComponent(i);
            final Comparable<?> v2 = other.getComponent(i);
            if (v1 == null) {
                return (v2 == null) ? 0 : -1;
            } else if (v2 == null) {
                return +1;
            }
            final int dr = getTypeRank(v1) - getTypeRank(v2);
            if (dr != 0) {
                /*
                 * One value is a text while the other value is a number.  We could be tempted to
                 * force a comparison by converting the number to a String and then invoking the
                 * String.compareTo(String) method, but this strategy would violate the following
                 * contract from Comparable.compareTo(Object):  "The implementor must also ensure
                 * that the relation is transitive". Use case:
                 *
                 *    A is the integer 10
                 *    B is the string "8Z"
                 *    C is the integer 5.
                 *
                 * If mismatched types are converted to String before being compared, then we
                 * would have A < B < C. Transitivity implies that A < C, but if we compare A
                 * and C directly we get A > C because they are compared as numbers.  An easy
                 * way to fix this inconsistency is to define all String as lexicographically
                 * preceding Integer, no matter their content. This is what we do here.
                 */
                return dr;
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            final int c = ((Comparable) v1).compareTo(v2);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /**
     * Compares this version with an other version object. This method performs the same
     * comparison than {@link #compareTo(Version, int)} with no limit.
     *
     * @param  other The other version object to compare with.
     * @return A negative value if this version is lower than the supplied version,
     *         a positive value if it is higher, or 0 if they are equal.
     */
    @Override
    public int compareTo(final Version other) {
        return compareTo(other, Integer.MAX_VALUE);
    }

    /**
     * Compare this version string with the specified object for equality. Two version are
     * considered equal if <code>{@linkplain #compareTo(Object) compareTo}(other) == 0</code>.
     *
     * @param other The object to compare with this version for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other != null && getClass() == other.getClass()) {
            return compareTo((Version) other) == 0;
        }
        return false;
    }

    /**
     * Returns the length of the version string.
     */
    @Override
    public int length() {
        return version.length();
    }

    /**
     * Returns the {@code char} value at the specified index.
     */
    @Override
    public char charAt(final int index) {
        return version.charAt(index);
    }

    /**
     * Returns a new version string that is a subsequence of this sequence.
     */
    @Override
    public CharSequence subSequence(final int start, final int end) {
        return version.subSequence(start, end);
    }

    /**
     * Returns the version string. This is the string specified at construction time.
     */
    @Override
    public String toString() {
        return version;
    }

    /**
     * Returns a hash code value for this version.
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int code = (int) serialVersionUID;
            int index = 0;
            Comparable<?> component;
            while ((component = getComponent(index)) != null) {
                code = code * 31 + component.hashCode();
                index++;
            }
            if (code == 0) {
                code = -1;
            }
            hashCode = code;
        }
        return hashCode;
    }
}
