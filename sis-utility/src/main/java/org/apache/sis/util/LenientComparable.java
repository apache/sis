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
 * Indicates that this object can be compared for equality using different levels of strictness.
 * For example {@link org.opengis.referencing.operation.MathTransform} implementations can be
 * compared ignoring some properties (remarks, <i>etc.</i>) that are not relevant to the
 * coordinates calculation.
 *
 * {@section Conditions for equality}
 *
 * <table class="sis">
 * <tr><th colspan="2">{@link org.apache.sis.metadata.iso.MetadataEntity} subclasses</th></tr>
 * <tr><td>{@link ComparisonMode#STRICT STRICT}</td>
 * <td class="sep">Objects must be of the same class and all attributes must be equal, including {@code xlink}
 * and others {@linkplain org.apache.sis.metadata.iso.MetadataEntity#getIdentifiers() identifiers}.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}</td>
 * <td class="sep">The same attributes than the above {@code STRICT} mode must be equal,
 * but the metadata object don't need to be implemented by the same class
 * provided that they implement the same GeoAPI interface.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}</td>
 * <td class="sep">Only the attributes defined in the GeoAPI interfaces are compared.
 * The above-cited identifiers and {@code xlinks} attributes are ignored.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}</td>
 * <td class="sep">The same attributes than the above {@code IGNORE_METADATA} mode are compared,
 * but a slight (implementation dependant) difference is tolerated in floating point numbers.</td></tr>
 *
 *
 *
 * <tr><th class="hsep" colspan="2">{@link org.apache.sis.referencing.AbstractIdentifiedObject} subclasses</th></tr>
 * <tr><td>{@link ComparisonMode#STRICT STRICT}</td>
 * <td class="sep">Objects must be of the same class and all attributes must be equal.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}</td>
 * <td class="sep">The same attributes than the above {@code STRICT} mode must be equal,
 * but the referencing object don't need to be implemented by the same class
 * provided that they implement the same GeoAPI interface.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}</td>
 * <td class="sep">The
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getIdentifiers() identifiers},
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getAlias() aliases},
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getScope() scope},
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getDomainOfValidity() domain of validity} and
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getRemarks() remarks} are ignored because
 * they have no incidence on the coordinate values to be computed by
 * {@linkplain org.opengis.referencing.operation.ConcatenatedOperation coordinate operations}.
 * All other attributes that are relevant to coordinate calculations, must be equal.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}</td>
 * <td class="sep">The same attributes than the above {@code IGNORE_METADATA} mode are compared,
 * but a slight (implementation dependant) difference is tolerated in floating point numbers.</td></tr>
 *
 *
 *
 * <tr><th class="hsep" colspan="2">{@link org.apache.sis.referencing.operation.transform.AbstractMathTransform} subclasses
 * except {@link org.apache.sis.referencing.operation.transform.LinearTransform}</th></tr>
 * <tr><td>{@link ComparisonMode#STRICT STRICT}</td>
 * <td class="sep">Objects must be of the same class and all attributes must be equal, including the
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#getParameterValues()
 * parameter values}.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}</td>
 * <td class="sep">Synonymous to the {@code STRICT} mode, because there is no GeoAPI interfaces for the various
 * kind of math transforms.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}</td>
 * <td class="sep">Objects must be of the same class, but the parameter values can be different
 * if they are different way to formulate the same transform.
 * For example a {@code "Mercator (2SP)"} projection with a
 * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#standardParallels
 * standard parallel} value of 60° produces the same results than a {@code "Mercator (1SP)"} projection with a
 * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#scaleFactor scale factor}
 * value of 0.5</td></tr>
 *
 * <tr><td>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}</td>
 * <td class="sep">The same attributes than the above {@code IGNORE_METADATA} mode are compared,
 * but a slight (implementation dependant) difference is tolerated in floating point numbers.</td></tr>
 *
 *
 *
 * <tr><th class="hsep" colspan="2">{@link org.apache.sis.referencing.operation.matrix.XMatrix} and
 * {@link org.apache.sis.referencing.operation.transform.LinearTransform} implementations</th></tr>
 * <tr><td>{@link ComparisonMode#STRICT STRICT}</td>
 * <td class="sep">Objects must be of the same class, matrixes must have the same size and all matrix
 * elements must be equal.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}</td>
 * <td class="sep">Matrixes must have the same size and all matrix elements must be equal,
 * but the matrixes are not required to be the same implementation class
 * (any {@link org.opengis.referencing.operation.Matrix} is okay).</td></tr>
 *
 * <tr><td>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}</td>
 * <td class="sep">Synonymous to the {@code BY_CONTRACT} mode, because matrixes don't have metadata.</td></tr>
 *
 * <tr><td>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}</td>
 * <td class="sep">The same attributes than the above {@code BY_CONTRACT} mode are compared,
 * but a slight (implementation dependant) difference is tolerated in floating point numbers.</td></tr>
 * </table>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 */
public interface LenientComparable {
    /**
     * Compares this object with the given object for equality.
     * The strictness level is controlled by the second argument:
     * <p>
     * <table class="sis">
     *   <tr><td>{@link ComparisonMode#STRICT STRICT}</td>
     *     <td class="sep">All attributes of the compared objects shall be strictly equal.</td></tr>
     *   <tr><td>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}</td>
     *     <td class="sep">Only the attributes published in the interface contract need to be compared.</td></tr>
     *   <tr><td>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}</td>
     *     <td class="sep">Only the attributes relevant to the object functionality are compared.</td></tr>
     *   <tr><td>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}</td>
     *     <td class="sep">Only the attributes relevant to the object functionality are compared,
     *                     with some tolerance threshold on numerical values.</td></tr>
     *   <tr><td>{@link ComparisonMode#DEBUG DEBUG}</td>
     *     <td class="sep">Special mode for figuring out why two objects expected to be equal are not.</td></tr>
     * </table>
     *
     * @param  other The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     *
     * @see Utilities#deepEquals(Object, Object, ComparisonMode)
     */
    boolean equals(Object other, ComparisonMode mode);

    /**
     * Returns {@code true} if this object is strictly equals to the given object.
     * This method is typically implemented as below:
     *
     * {@preformat java
     *     return equals(other, ComparisonMode.STRICT);
     * }
     *
     * In SIS implementations, this method is typically {@code final} in order to ensure that
     * subclasses override the above {@link #equals(Object, ComparisonMode)} method instead.
     *
     * @param  other The object to compare to {@code this}.
     * @return {@code true} if both objects are strictly equal.
     *
     * @see ComparisonMode#STRICT
     */
    @Override
    boolean equals(Object other);
}
