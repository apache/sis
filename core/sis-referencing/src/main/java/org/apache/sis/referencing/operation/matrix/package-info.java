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
 * {@linkplain org.opengis.referencing.operation.Matrix} implementations tuned for spatio-temporal referencing.
 * Matrices can be of arbitrary size, but the most common ones in the context of geospatial coordinate operations
 * are not greater than 5×5 (number of spatio-temporal dimensions + 1).
 * This package differs from other matrix packages by the special treatment done for such small matrices.
 *
 * <p>This package provides public implementations of small square matrices, with size ranging from 1×1 to 4×4.
 * Those implementations are made public because in many cases, the user know that (s)he is working with (for
 * example) three-dimensional Coordinate Reference Systems (CRS). If the number of CRS dimensions is fixed to 3,
 * then <cite>affine transforms</cite> between those CRS can be represented by 4×4 matrices,
 * and the <cite>derivatives</cite> of those transforms can be represented by 3×3 matrices.
 * Since the user know the matrices size, (s)he can use the specific implementation and read or write
 * directly the <var>m</var><sub><var>row</var> <var>column</var></sub> field.</p>
 *
 * <p><b>Example:</b> in the two dimensional case, an affine transform from a map projection (units in metres)
 * to the screen (units in pixels) can be performed by the following matrix multiplication:</p>
 *
 * <p><center><img src="doc-files/AffineTransform.png"></center></p>
 *
 * {@section Related projects}
 * This package is <strong>not</strong> designed for large matrices, and is rooted in
 * {@code org.apache.sis.referencing} for making clearer that this is not a general-purpose library.
 * For computational intensive calculations, better guarantees on numerical stability, sparse matrices support
 * and more, consider using an dedicated library like <a href="http://mikiobraun.github.io/jblas">jblas</a> instead.
 *
 * <p>The <a href="http://java.net/projects/vecmath">Vecmath</a> library shares similar goals than {@code MatrixSIS}.
 * Like SIS, Vecmath is optimized for small matrices of interest for 2D and 3D graphics.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
package org.apache.sis.referencing.operation.matrix;
