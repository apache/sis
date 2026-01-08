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

/**
 * Matrix implementations for spatiotemporal referencing.
 * Matrices can be of arbitrary size, but the most common ones in the context of geospatial coordinate operations
 * are not greater than 5×5 (number of spatiotemporal dimensions + 1).
 * This package differs from other matrix packages by:
 *
 * <ul>
 *   <li>The class specializations for such small matrices.</li>
 *   <li>Methods specific to coordinate systems support like
 *       {@link org.apache.sis.referencing.operation.matrix.Matrices#createTransform(org.opengis.geometry.Envelope,
 *       org.opengis.referencing.cs.AxisDirection[], org.opengis.geometry.Envelope,
 *       org.opengis.referencing.cs.AxisDirection[]) Matrices.createTransform(…)}.</li>
 *   <li>Matrix inversions tolerant to {@link java.lang.Double#NaN NaN} values and non-square matrix in some situations.</li>
 *   <li>Capability to store {@link java.lang.Number} instances for greater precision (depending on the number subtype).</li>
 * </ul>
 *
 * <p>This package provides public implementations of small square matrices, with size ranging from 1×1 to 4×4.
 * Those implementations are convenient for working with Coordinate Reference Systems (CRS) of fixed dimensions.
 * If the number of CRS dimensions is fixed to 3,
 * then <i>affine transforms</i> between those CRS can be represented by 4×4 matrices,
 * and the <i>derivatives</i> of those transforms can be represented by 3×3 matrices.
 * When the number of dimensions is fixed at compile-time, matrix elements can be accessed
 * directly with the <var>m</var><sub><var>row</var> <var>column</var></sub> fields.</p>
 *
 * <p><b>Example:</b> in the two dimensional case, an affine transform from a map projection (units in metres)
 * to the screen (units in pixels) can be performed by the following matrix multiplication:</p>
 *
 * <div style="text-align:center">
 * <img src="doc-files/AffineTransform.png" alt="Matrix representation of an affine transform">
 * </div>
 *
 * <h2>Extended floating point precision</h2>
 * This package uses extended floating point precision for most arithmetic operations like matrix multiplications and
 * inversions. SIS needs extended precision because <i>affine transforms</i> concatenations like conversion from
 * degrees to radians, followed by some operations, followed by conversion back from radians to degrees, are very frequent.
 * Without extended precision, we often obtain values like 0.99999… where we would expect an identity transform.
 * The usual workaround - namely comparing the floating point values with a small <var>epsilon</var> tolerance value -
 * is dangerous in this particular case because <i>datum shifts</i>, when expressed as a matrix from their
 * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters}, are very close to the
 * identity transform.
 *
 * <p>The current implementation uses
 * <a href="https://en.wikipedia.org/wiki/Quadruple-precision_floating-point_format#Double-double_arithmetic">double-double
 * arithmetic</a>. However, this may change in any future SIS version.</p>
 *
 * <h2>Related projects</h2>
 * This package is <strong>not</strong> designed for large matrices, and is rooted in
 * {@code org.apache.sis.referencing} for making clearer that this is not a general-purpose library.
 * For computational intensive calculations, better guarantees on numerical stability, sparse matrices support
 * and more, consider using an dedicated library like <a href="http://mikiobraun.github.io/jblas">jblas</a> instead.
 *
 * <p>The <a href="https://download.java.net/media/java3d/javadoc/1.5.2/javax/vecmath/package-summary.html">Vecmath</a>
 * library shares similar goals than {@code MatrixSIS}.
 * Like SIS, Vecmath is optimized for small matrices of interest for 2D and 3D graphics.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 * @since   0.4
 */
package org.apache.sis.referencing.operation.matrix;
