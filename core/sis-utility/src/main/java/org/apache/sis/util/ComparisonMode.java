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


/**
 * Specifies the level of strictness when comparing two {@link LenientComparable} objects
 * for equality. This enumeration allows users to specify which kind of differences can be
 * tolerated between two objects: differences in implementation class, differences in
 * some kinds of property, or slight difference in numerical values.
 *
 * <p>This enumeration is <em>ordered</em> from stricter to more lenient levels:</p>
 *
 * <ol>
 *   <li>{@link #STRICT}          – All attributes of the compared objects shall be strictly equal.</li>
 *   <li>{@link #BY_CONTRACT}     – Only the attributes published in the interface contract need to be compared.</li>
 *   <li>{@link #IGNORE_METADATA} – Only the attributes relevant to the object functionality are compared.</li>
 *   <li>{@link #APPROXIMATIVE}   – Only the attributes relevant to the object functionality are compared,
 *                                  with some tolerance threshold on numerical values.</li>
 *   <li>{@link #ALLOW_VARIANT}   – For objects not really equal but related (e.g. CRS using different axis order).</li>
 *   <li>{@link #DEBUG}           – Special mode for figuring out why two objects expected to be equal are not.</li>
 * </ol>
 *
 * If two objects are equal at some level of strictness <var>E</var>, then they should also
 * be equal at all levels listed below <var>E</var> in the above list. For example if two objects
 * are equal at the {@link #BY_CONTRACT} level, then they should also be equal at the
 * {@link #IGNORE_METADATA} level but not necessarily at the {@link #STRICT} level.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see LenientComparable#equals(Object, ComparisonMode)
 * @see Utilities#deepEquals(Object, Object, ComparisonMode)
 */
public enum ComparisonMode {
    /**
     * All attributes of the compared objects shall be strictly equal. This comparison mode
     * is equivalent to the {@link Object#equals(Object)} method, and must be compliant with
     * the contract documented in that method. In particular, this comparison mode shall be
     * consistent with {@link Object#hashCode()} and be symmetric ({@code A.equals(B)} implies
     * {@code B.equals(A)}).
     *
     * <div class="section">Implementation note</div>
     * In the SIS implementation, this comparison mode usually have the following
     * characteristics (not always, this is only typical):
     *
     * <ul>
     *   <li>The objects being compared need to be the same implementation class.</li>
     *   <li>Private fields are compared directly instead than invoking public getter methods.</li>
     * </ul>
     *
     * @see Object#equals(Object)
     */
    STRICT,

    /**
     * Only the attributes published in some contract (typically a GeoAPI interface) need to be compared.
     * The implementation classes do not need to be the same and some private attributes may be ignored.
     *
     * <p>Note that this comparison mode does <strong>not</strong> guaranteed {@link Object#hashCode()}
     * consistency, neither comparison symmetry (i.e. {@code A.equals(B)} and {@code B.equals(A)} may
     * return different results if the {@code equals} methods are implemented differently).</p>
     *
     * <div class="section">Implementation note</div>
     * In the SIS implementation, this comparison mode usually have the following
     * characteristics (not always, this is only typical):
     *
     * <ul>
     *   <li>The objects being compared need to implement the same GeoAPI interfaces.</li>
     *   <li>Public getter methods are used (no direct access to private fields).</li>
     * </ul>
     */
    BY_CONTRACT,

    /**
     * Only the attributes relevant to the object functionality are compared. Attributes that
     * are only informative can be ignored. This comparison mode is typically less strict than
     * {@link #BY_CONTRACT}.
     *
     * <div class="section">Application to coordinate reference systems</div>
     * If the objects being compared are {@link org.opengis.referencing.crs.CoordinateReferenceSystem} instances,
     * then only the properties relevant to the coordinate localization shall be compared.
     * Metadata like the {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getIdentifiers() identifiers}
     * or the {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getDomainOfValidity() domain of validity},
     * which have no impact on the coordinates being calculated, shall be ignored.
     *
     * <div class="section">Application to coordinate operations</div>
     * If the objects being compared are {@link org.opengis.referencing.operation.MathTransform} instances,
     * then two transforms defined in a different way may be considered equivalent. For example it is possible
     * to define a {@linkplain org.apache.sis.referencing.operation.projection.Mercator Mercator} projection in
     * two different ways, as a <cite>"Mercator (1SP)"</cite> or as a <cite>"Mercator (2SP)"</cite> projection,
     * each having their own set of parameters.
     * The {@link #STRICT} or {@link #BY_CONTRACT} modes shall consider two projections as equal only if their
     * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#getParameterValues()
     * parameter values} are strictly identical, while the {@code IGNORE_METADATA} mode can consider
     * those objects as equivalent despite difference in the set of parameters, as long as coordinate
     * transformations still produce the same results.
     *
     * <div class="note"><b>Example:</b> A <cite>"Mercator (2SP)"</cite> projection with a <cite>standard parallel</cite>
     * value of 60° produces the same results than a <cite>"Mercator (1SP)"</cite> projection with a <cite>scale factor</cite>
     * value of 0.5.</div>
     *
     * @see org.apache.sis.util.Utilities#equalsIgnoreMetadata(Object, Object)
     */
    IGNORE_METADATA,

    /**
     * Only the attributes relevant to the object functionality are compared, with some tolerance
     * threshold on numerical values.
     *
     * <div class="section">Application to coordinate operations</div>
     * If two {@link org.opengis.referencing.operation.MathTransform} objects are considered equal according this mode,
     * then for any given identical source position, the two compared transforms shall compute at least approximatively
     * the same target position.
     * A small difference is tolerated between the target coordinates calculated by the two math transforms.
     * How small is “small” is implementation dependent — the threshold can not be specified in the current
     * implementation, because of the non-linear nature of map projections.
     */
    APPROXIMATIVE,

    /**
     * Most but not all attributes relevant to the object functionality are compared.
     * This comparison mode is equivalent to {@link #APPROXIMATIVE}, except that it
     * ignores some attributes that may differ between objects not equal but related.
     *
     * <p>The main purpose of this method is to verify if two Coordinate Reference Systems (CRS)
     * are approximatively equal ignoring axis order.</p>
     *
     * <div class="note"><b>Example:</b>
     * consider two geographic coordinate reference systems with the same attributes except axis order,
     * where one CRS uses (<var>latitude</var>, <var>longitude</var>) axes
     * and the other CRS uses (<var>longitude</var>, <var>latitude</var>) axes.
     * All comparison modes (even {@code APPROXIMATIVE}) will consider those two CRS as different,
     * except this {@code ALLOW_VARIANT} mode which will consider one CRS to be a variant of the other.
     * </div>
     *
     * @since 0.7
     */
    ALLOW_VARIANT,

    /**
     * Same as {@link #APPROXIMATIVE}, except that an {@link AssertionError} is thrown if the two
     * objects are not equal and assertions are enabled. The exception message and stack trace help
     * to locate which attributes are not equal. This mode is typically used in assertions like below:
     *
     * {@preformat java
     *     assert Utilities.deepEquals(object1, object2, ComparisonMode.DEBUG);
     * }
     *
     * Note that a comparison in {@code DEBUG} mode may still return {@code false} without
     * throwing an exception, since not all corner cases are tested. The exception is only
     * intended to provide more details for some common cases.
     */
    @Debug
    DEBUG;

    /**
     * Returns {@code true} if this comparison ignores metadata.
     * This method currently returns {@code true} for {@code IGNORE_METADATA}, {@code APPROXIMATIVE}
     * or {@code DEBUG} only, but this list may be extended in future SIS versions.
     *
     * @return Whether this comparison ignore metadata.
     *
     * @since 0.6
     */
    public boolean isIgnoringMetadata() {
        return ordinal() >= IGNORE_METADATA.ordinal();
    }

    /**
     * Returns {@code true} if this comparison uses a tolerance threshold.
     * This method currently returns {@code true} for {@code APPROXIMATIVE} or {@code DEBUG} only,
     * but this list may be extended in future SIS versions.
     *
     * @return Whether this comparison uses a tolerance threshold.
     *
     * @since 0.6
     */
    public boolean isApproximative() {
        return ordinal() >= APPROXIMATIVE.ordinal();
    }

    /**
     * If the two given objects are equal according one of the modes enumerated in this class,
     * then returns that mode. Otherwise returns {@code null}.
     *
     * <p><b>Note:</b> this method never return the {@link #DEBUG} mode.</p>
     *
     * @param  o1 The first object to compare, or {@code null}.
     * @param  o2 The second object to compare, or {@code null}.
     * @return The most suitable comparison mode, or {@code null} if the two given objects
     *         are not equal according any mode in this enumeration.
     */
    public static ComparisonMode equalityLevel(final Object o1, Object o2) {
        if (o1 == o2) {
            return STRICT;
        }
        if (o1 != null && o2 != null) {
            if (o1.equals(o2)) {
                return STRICT;
            }
            final LenientComparable cp;
            if (o1 instanceof LenientComparable) {
                cp = (LenientComparable) o1;
            } else if (o2 instanceof LenientComparable) {
                cp = (LenientComparable) o2;
                o2 = o1;
            } else {
                return null;
            }
            if (cp.equals(o2, BY_CONTRACT))     return BY_CONTRACT;
            if (cp.equals(o2, IGNORE_METADATA)) return IGNORE_METADATA;
            if (cp.equals(o2, APPROXIMATIVE))   return APPROXIMATIVE;
            if (cp.equals(o2, ALLOW_VARIANT))   return ALLOW_VARIANT;
        }
        return null;
    }
}
