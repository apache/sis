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
 * Matrix implementations for spatio-temporal referencing.
 * Matrices can be of arbitrary size, but the most common ones in the context of geospatial coordinate operations
 * are not greater than 5×5 (number of spatio-temporal dimensions + 1).
 * This package differs from other matrix packages by:
 *
 * <ul>
 *   <li>The class specializations for such small matrices.</li>
 *   <li>Methods specific to coordinate systems support like
 *       {@link org.apache.sis.referencing.operation.matrix.Matrices#createTransform(org.opengis.geometry.Envelope,
 *       org.opengis.referencing.cs.AxisDirection[], org.opengis.geometry.Envelope,
 *       org.opengis.referencing.cs.AxisDirection[]) Matrices.createTransform(…)}.</li>
 *   <li>Matrix inversions tolerant to {@link java.lang.Double#NaN NaN} values and non-square matrix in some situations.</li>
 * </ul>
 *
 * <p>This package provides public implementations of small square matrices, with size ranging from 1×1 to 4×4.
 * Those implementations are made public because in many cases, the user know that (s)he is working with (for
 * example) three-dimensional Coordinate Reference Systems (CRS). If the number of CRS dimensions is fixed to 3,
 * then <cite>affine transforms</cite> between those CRS can be represented by 4×4 matrices,
 * and the <cite>derivatives</cite> of those transforms can be represented by 3×3 matrices.
 * Since the user know the matrices size, (s)he can use the specific implementation and read or write
 * directly the <var>m</var><sub><var>row</var> <var>column</var></sub> field.</p>
 *
 * <p><b>Example:</b> in the two dimensional case, an affine transform from a map projection (units in metres)
 * to the screen (units in pixels) can be performed by the following matrix multiplication:</p>
 *
 * <center>
 * <img src="doc-files/AffineTransform.png" alt="Matrix representation of an affine transform">
 * </center>
 *
 * <div class="section">Extended floating point precision</div>
 * This package uses extended floating point precision for most arithmetic operations like matrix multiplications and
 * inversions. SIS needs extended precision because <cite>affine transforms</cite> concatenations like conversion from
 * degrees to radians, followed by some operations, followed by conversion back from radians to degrees, are very frequent.
 * Without extended precision, we often obtain values like 0.99999… where we would expect an identity transform.
 * The usual workaround - namely comparing the floating point values with a small <var>epsilon</var> tolerance value -
 * is dangerous in this particular case because <cite>datum shifts</cite>, when expressed as a matrix from their
 * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters}, are very close to the
 * identity transform.
 *
 * <p>The current implementation uses
 * <a href="http://en.wikipedia.org/wiki/Double-double_%28arithmetic%29#Double-double_arithmetic">double-double
 * arithmetic</a>. However this may change in any future SIS version.</p>
 *
 * <div class="section">Related projects</div>
 * This package is <strong>not</strong> designed for large matrices, and is rooted in
 * {@code org.apache.sis.referencing} for making clearer that this is not a general-purpose library.
 * For computational intensive calculations, better guarantees on numerical stability, sparse matrices support
 * and more, consider using an dedicated library like <a href="http://mikiobraun.github.io/jblas">jblas</a> instead.
 *
 * <p>The <a href="http://java.net/projects/vecmath">Vecmath</a> library shares similar goals than {@code MatrixSIS}.
 * Like SIS, Vecmath is optimized for small matrices of interest for 2D and 3D graphics.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
package org.apache.sis.referencing.operation.matrix;
