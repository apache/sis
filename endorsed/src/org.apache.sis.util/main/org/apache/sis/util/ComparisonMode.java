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

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;


/**
 * Specifies the level of strictness when comparing two {@link LenientComparable} objects for equality.
 * This enumeration allows users to specify which kind of differences can be tolerated between two objects:
 * differences in implementation class, differences in some kinds of property,
 * or slight difference in numerical values.
 *
 * <p>This enumeration is <em>ordered</em> from stricter to more lenient levels:</p>
 *
 * <ol>
 *   <li>{@link #STRICT}          – All attributes of the compared objects shall be strictly equal.</li>
 *   <li>{@link #BY_CONTRACT}     – Only the attributes published in the interface contract need to be compared.</li>
 *   <li>{@link #IGNORE_METADATA} – Only the attributes relevant to the object functionality are compared.</li>
 *   <li>{@link #COMPATIBILITY}      – Like {@code IGNORE_METADATA}, but ignore also some structural changes for historical reasons.</li>
 *   <li>{@link #APPROXIMATE}     – Like {@code COMPATIBILITY}, with some tolerance threshold on numerical values.</li>
 *   <li>{@link #ALLOW_VARIANT}   – Objects not really equal but related (e.g., <abbr>CRS</abbr> using different axis order).</li>
 *   <li>{@link #DEBUG}           – Special mode for figuring out why two objects expected to be equal are not.</li>
 * </ol>
 *
 * If two objects are equal at some level of strictness <var>E</var>,
 * then they shall also be equal at all levels listed after <var>E</var> in the above list.
 * For example, if two objects are equal at the {@link #BY_CONTRACT} level,
 * then they shall also be equal at the {@link #IGNORE_METADATA} level
 * but not necessarily at the {@link #STRICT} level.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see LenientComparable#equals(Object, ComparisonMode)
 * @see Utilities#deepEquals(Object, Object, ComparisonMode)
 *
 * @since 0.3
 */
public enum ComparisonMode {
    /**
     * All attributes of the compared objects shall be strictly equal. This comparison mode
     * is equivalent to the {@link Object#equals(Object)} method, and must be compliant with
     * the contract documented in that method. In particular, this comparison mode shall be
     * consistent with {@link Object#hashCode()} and be symmetric ({@code A.equals(B)} implies
     * {@code B.equals(A)}).
     *
     * <h4>Implementation note</h4>
     * In the <abbr>SIS</abbr> implementations, this comparison mode usually have the following
     * characteristics (not always, this is only typical):
     *
     * <ul>
     *   <li>The objects being compared need to be the same implementation class.</li>
     *   <li>Private fields are compared directly instead of invoking public getter methods.</li>
     * </ul>
     *
     * @see Object#equals(Object)
     */
    STRICT,

    /**
     * Only the attributes published in some contract (typically a GeoAPI interface) need to be compared.
     * The implementation classes do not need to be the same and some private attributes may be ignored.
     *
     * <p>Note that this comparison mode does <strong>not</strong> guarantee {@link Object#hashCode()}
     * consistency, neither comparison symmetry (i.e. {@code A.equals(B)} and {@code B.equals(A)} may
     * return different results if the {@code equals} methods are implemented differently).</p>
     *
     * <h4>Implementation note</h4>
     * In the <abbr>SIS</abbr> implementations, this comparison mode usually have the following
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
     * <h4>Application to coordinate reference systems</h4>
     * If the objects being compared are {@link CoordinateReferenceSystem} instances,
     * then only the properties impacting coordinate values shall be compared.
     * Metadata like the {@linkplain CoordinateReferenceSystem#getIdentifiers() identifiers}
     * or the {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getDomains() domain of validity},
     * which have no impact on the coordinates being calculated, shall be ignored.
     *
     * <h4>Application to coordinate operations</h4>
     * If the objects being compared are {@link MathTransform} instances,
     * then two transforms defined in a different way may be considered equivalent.
     * For example, it is possible to define a Mercator projection in different ways,
     * named "variant A", "variant B" and "variant C" in EPSG dataset, each having their own set of parameters.
     * The {@link #STRICT} or {@link #BY_CONTRACT} modes shall consider two projections as equal only if their
     * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#getParameterValues()
     * parameter values} are strictly identical, while the {@code IGNORE_METADATA} mode can consider
     * those objects as equivalent despite difference in the set of parameters,
     * as long as coordinate operations still produce the same results.
     *
     * <h5>Example</h5>
     * A <q>Mercator (variant B)</q> projection with a <i>standard parallel</i> value of 60° produces the
     * same results as a <q>Mercator (variant A)</q> projection with a <i>scale factor</i> value of 0.5.
     *
     * @see #isIgnoringMetadata()
     * @see org.apache.sis.util.Utilities#equalsIgnoreMetadata(Object, Object)
     */
    IGNORE_METADATA,

    /**
     * Only the attributes relevant to the object functionality are compared, with a tolerance for some structural changes.
     * The changes may exist for historical reasons, or for compatibility with common practice in other software.
     * However, the changes should not have a practical impact on the numerical results of coordinate operations.
     * For example, changes of input or output axis order is not accepted by this comparison mode.
     *
     * <h4>Application to datum ensembles</h4>
     * Two Coordinate Reference Systems (<abbr>CRS</abbr>) may be considered compatible when
     * one <abbr>CRS</abbr> is associated to a datum and the other <abbr>CRS</abbr> is associated to a datum ensemble,
     * but the former can be considered as the {@linkplain org.apache.sis.referencing.datum.DatumOrEnsemble#isLegacyDatum
     * legacy definition} of the latter. This comparison mode can check, among other criteria,
     * whether the datum and the datum ensemble share a common authority code.
     *
     * <p><b>Example:</b> {@code EPSG:9:4326} and {@code EPSG:10:4326}, which are both the same (at least conceptually)
     * geographic <abbr>CRS</abbr> associated to the authority code 4326 in the <abbr>EPSG</abbr> geodetic dataset,
     * but as defined in version 9 and 10 respectively of the <abbr>EPSG</abbr> database.
     * They are the <abbr>CRS</abbr> definitions before and after the introduction of datum ensemble in the schema.</p>
     *
     * <h4>Application to dynamic reference frames</h4>
     * Two Reference Frames may be considered compatible despite one frame being static and the other frame being dynamic.
     * This comparison mode can check, among other criteria, whether the two reference frames share a common authority code
     * or have an {@linkplain org.apache.sis.referencing.IdentifiedObjects#isHeuristicMatchForName equivalent name}.
     *
     * <p><b>Example:</b> the "WGS 1972" reference frame as defined in versions 9 and 10 of <abbr>EPSG</abbr> database.</p>
     *
     * @see #isCompatibility()
     *
     * @since 1.5
     */
    COMPATIBILITY,

    /**
     * Only the attributes relevant to compatibility are compared, with some tolerance threshold on numerical values.
     * The threshold is implementation dependent, but the current <abbr>SIS</abbr> implementation generally aims for
     * a precision of 1 centimeter in the linear and angular parameter values for a planet of the size of Earth.
     *
     * <h4>Application to coordinate operations</h4>
     * If two {@link MathTransform} objects are considered equal according this mode, then for any given identical source position,
     * the two compared transforms shall compute at least approximately the same target position.
     * A small difference is tolerated between the target coordinates calculated by the two math transforms.
     * How small is “small” is implementation dependent — the threshold cannot be specified in the current
     * implementation, because of the non-linear nature of map projections.
     *
     * @see #isApproximate()
     *
     * @since 1.0
     */
    APPROXIMATE,

    /**
     * Most but not all attributes relevant to the object functionality are compared.
     * This comparison mode is equivalent to {@link #APPROXIMATE}, except that it
     * ignores some aspects that may differ between objects not equal but related.
     * Below is a list of examples where the objects being compared would be
     * considered different according the other modes such as {@link #APPROXIMATE},
     * but are considered equivalent according this {@code ALLOW_VARIANT} mode.
     *
     * <ul class="verbose">
     *   <li>Two Coordinate Reference Systems (<abbr>CRS</abbr>) with the same axes but in different order.
     *       <b>Example:</b> two geographic <abbr>CRS</abbr>s with the same attributes but with
     *       (<var>latitude</var>, <var>longitude</var>) axes in one case and
     *       (<var>longitude</var>, <var>latitude</var>) axes in the other case.</li>
     * </ul>
     *
     * @since 0.7
     */
    ALLOW_VARIANT,

    /**
     * Asserts that two objects shall be approximately equal.
     * The same comparison as {@link #APPROXIMATE} is performed,
     * except that an {@link AssertionError} is thrown if the two objects are not equal and assertions are enabled.
     * The exception message and stack trace help to locate which attributes are not equal.
     * This mode is typically used in assertions like below:
     *
     * {@snippet lang="java" :
     *     assert Utilities.deepEquals(object1, object2, ComparisonMode.DEBUG);
     *     }
     *
     * Note that a comparison in {@code DEBUG} mode may still return {@code false} without
     * throwing an exception, since not all corner cases are tested. The exception is only
     * intended to provide more details for some common cases.
     */
    @Debug
    DEBUG;

    /**
     * Returns {@code true} if this comparison ignores metadata.
     * This method returns {@code true} for {@link #IGNORE_METADATA}, {@link #COMPATIBILITY}, {@link #APPROXIMATE},
     * {@link #ALLOW_VARIANT} and {@link #DEBUG}.
     *
     * @return whether this comparison ignores metadata.
     *
     * @since 0.6
     */
    public boolean isIgnoringMetadata() {
        return ordinal() >= IGNORE_METADATA.ordinal();
    }

    /**
     * Returns {@code true} if this comparison accepts structural changes for compatibility reasons.
     * This method returns {@code true} for {@link #COMPATIBILITY}, {@link #APPROXIMATE}, {@link #ALLOW_VARIANT}
     * and {@link #DEBUG}.
     *
     * @return whether this comparison accepts structural changes for compatibility reasons.
     *
     * @since 1.5
     */
    public boolean isCompatibility() {
        return ordinal() >= COMPATIBILITY.ordinal();
    }

    /**
     * Returns {@code true} if this comparison uses a tolerance threshold.
     * This method returns {@code true} for {@link #APPROXIMATE}, {@link #ALLOW_VARIANT} and {@link #DEBUG}.
     *
     * @return whether this comparison uses a tolerance threshold.
     *
     * @since 1.0
     */
    public boolean isApproximate() {
        return ordinal() >= APPROXIMATE.ordinal();
    }

    /**
     * If the two given objects are equal according one of the modes enumerated in this class,
     * then returns that mode. Otherwise returns {@code null}.
     *
     * <p><b>Note:</b> this method never return the {@link #DEBUG} mode.</p>
     *
     * @param  o1  the first object to compare, or {@code null}.
     * @param  o2  the second object to compare, or {@code null}.
     * @return the most suitable comparison mode, or {@code null} if the two given objects
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
            if (cp.equals(o2, APPROXIMATE))     return APPROXIMATE;
            if (cp.equals(o2, ALLOW_VARIANT))   return ALLOW_VARIANT;
        }
        return null;
    }
}
