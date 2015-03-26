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
 * Interfaces of classes for which instances can be compared for equality using different levels of strictness.
 * For example {@link org.opengis.referencing.operation.MathTransform} implementations can be
 * compared ignoring some properties (remarks, <i>etc.</i>) that are not relevant to the
 * coordinates calculation.
 *
 * <div class="section">Conditions for equality</div>
 * <ul>
 *   <li>{@link org.apache.sis.metadata.iso.ISOMetadata} subclasses
 *     <ol>
 *       <li>{@link ComparisonMode#STRICT STRICT} – Objects must be of the same class
 *           and all attributes must be equal, including {@code xlink} and others
 *           {@linkplain org.apache.sis.metadata.iso.ISOMetadata#getIdentifiers() identifiers}.</li>
 *       <li>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT} – The same attributes than the above
 *           {@code STRICT} mode must be equal, but the metadata object don't need to be implemented
 *           by the same class provided that they implement the same GeoAPI interface.</li>
 *       <li>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} – Only the attributes defined
 *           in the GeoAPI interfaces are compared. The above-cited identifiers and {@code xlinks}
 *           attributes are ignored.</li>
 *       <li>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE} – The same attributes than the above
 *           {@code IGNORE_METADATA} mode are compared, but a slight (implementation dependant)
 *           difference is tolerated in floating point numbers.</li>
 *     </ol>
 *   </li>
 *   <li>{@link org.apache.sis.referencing.AbstractIdentifiedObject} subclasses
 *     <ol>
 *       <li>{@link ComparisonMode#STRICT STRICT} – Objects must be of the same class
 *           and all attributes must be equal.</li>
 *       <li>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT} – The same attributes than the above
 *           {@code STRICT} mode must be equal, but the referencing object don't need to be
 *           implemented by the same class provided that they implement the same GeoAPI interface.</li>
 *       <li>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} – The
 *           {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getIdentifiers() identifiers},
 *           {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getAlias() aliases},
 *           {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getScope() scope},
 *           {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getDomainOfValidity() domain of validity} and
 *           {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getRemarks() remarks}
 *           are ignored because they have no incidence on the coordinate values to be computed by
 *           {@linkplain org.opengis.referencing.operation.ConcatenatedOperation coordinate operations}.
 *           All other attributes that are relevant to coordinate calculations, must be equal.</li>
 *       <li>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE} – The same attributes than the above
 *           {@code IGNORE_METADATA} mode are compared, but a slight (implementation dependant)
 *           difference is tolerated in floating point numbers.</li>
 *     </ol>
 *   </li>
 *   <li>{@link org.apache.sis.referencing.operation.transform.AbstractMathTransform} subclasses
 *       except {@link org.apache.sis.referencing.operation.transform.LinearTransform}
 *     <ol>
 *       <li>{@link ComparisonMode#STRICT STRICT} – Objects must be of the same class and all
 *           attributes must be equal, including the
 *           {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#getParameterValues() parameter values}.</li>
 *       <li>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT} – Synonymous to the {@code STRICT} mode,
 *            because there is no GeoAPI interfaces for the various kind of math transforms.</li>
 *       <li>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} – Objects must be of the same class,
 *           but the parameter values can be different if they are different way to formulate the same transform.
 *           For example a <cite>"Mercator (2SP)"</cite> projection on a sphere with a <cite>standard parallel</cite>
 *           value of 60° produces the same results than a <cite>"Mercator (1SP)"</cite> projection on the same sphere
 *           with a <cite>scale factor</cite> value of 0.5.</li>
 *       <li>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE} – The same attributes than the above
 *           {@code IGNORE_METADATA} mode are compared, but a slight (implementation dependant)
 *           difference is tolerated in floating point numbers.</li>
 *     </ol>
 *   </li>
 *   <li>{@link org.apache.sis.referencing.operation.matrix.MatrixSIS} and
 *       {@link org.apache.sis.referencing.operation.transform.LinearTransform} implementations
 *     <ol>
 *       <li>{@link ComparisonMode#STRICT STRICT} – Objects must be of the same class, matrixes
 *           must have the same size and all matrix elements must be equal.</li>
 *       <li>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT} – Matrixes must have the same size
 *           and all matrix elements must be equal, but the matrixes are not required to be the
 *           same implementation class (any {@link org.opengis.referencing.operation.Matrix} is okay).</li>
 *       <li>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} – Synonymous to the
 *           {@code BY_CONTRACT} mode, because matrixes don't have metadata.</li>
 *       <li>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE} – The same attributes than the above
 *           {@code BY_CONTRACT} mode are compared, but a slight (implementation dependant)
 *           difference is tolerated in floating point numbers.</li>
 *     </ol>
 *   </li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public interface LenientComparable {
    /**
     * Compares this object with the given object for equality.
     * The strictness level is controlled by the second argument,
     * from stricter to more permissive values:
     *
     * <table class="compact" summary="Description of comparison modes.">
     *   <tr><td>{@link ComparisonMode#STRICT STRICT}:</td>
     *        <td>All attributes of the compared objects shall be strictly equal.</td></tr>
     *   <tr><td>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}:</td>
     *       <td>Only the attributes published in the interface contract need to be compared.</td></tr>
     *   <tr><td>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}:</td>
     *       <td>Only the attributes relevant to the object functionality are compared.</td></tr>
     *   <tr><td>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}:</td>
     *       <td>Only the attributes relevant to the object functionality are compared,
     *           with some tolerance threshold on numerical values.</td></tr>
     *   <tr><td>{@link ComparisonMode#DEBUG DEBUG}:</td>
     *        <td>Special mode for figuring out why two objects expected to be equal are not.</td></tr>
     * </table>
     *
     * <div class="section">Conformance to the {@code equals(Object)} method contract</div>
     * {@link ComparisonMode#STRICT} is the only mode compliant with the {@link Object#equals(Object)} contract.
     * For all other modes <var>m</var>, the comparison is not guaranteed to be <cite>symmetric</cite> neither
     * <cite>transitive</cite>:
     *
     * <ul>
     *   <li>{@code x.equals(y,m)} is <strong>not</strong> guaranteed to be equal to {@code y.equals(x,m)}.
     *       In particular, the {@code BY_CONTRACT} mode and all modes below it will typically compare only the
     *       properties known to {@code this} instance, ignoring any properties that may be known only by the other
     *       instance.</li>
     *   <li>{@code x.equals(y,m)} and {@code y.equals(z,m)} does <strong>not</strong> implies
     *       {@code x.equals(z,m)}. In particular, the use of a comparison threshold for the
     *       {@code APPROXIMATIVE} mode is incompatible with the transitivity contract.</li>
     * </ul>
     *
     * @param  other The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal according the given comparison mode.
     *
     * @see Utilities#deepEquals(Object, Object, ComparisonMode)
     */
    boolean equals(Object other, ComparisonMode mode);

    /**
     * Returns {@code true} if this object is strictly equals to the given object.
     * This method is usually implemented as below:
     *
     * {@preformat java
     *     public boolean equals(Object other) {
     *         return equals(other, ComparisonMode.STRICT);
     *     }
     * }
     *
     * Implementors shall ensure that the following conditions hold. Unless the {@code equals}
     * behavior is clearly documented in the interface javadoc (as for example in the Java
     * collection framework), {@link ComparisonMode#STRICT} is the only reliable mode for
     * this method implementation.
     *
     * <ul>
     *   <li>{@code A.equals(B)} implies {@code B.equals(A)};</li>
     *   <li>{@code A.equals(B)} and {@code B.equals(C)} implies {@code A.equals(C)};</li>
     *   <li>{@code A.equals(B)} implies {@code A.hashCode() == B.hashCode()};</li>
     * </ul>
     *
     * This method is declared {@code final} in most SIS implementations for ensuring that
     * subclasses override the above {@link #equals(Object, ComparisonMode)} method instead
     * than this one.
     *
     * @param  other The object to compare to {@code this}.
     * @return {@code true} if both objects are strictly equal.
     *
     * @see ComparisonMode#STRICT
     */
    @Override
    boolean equals(Object other);
}
