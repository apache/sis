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
 * Matrices can be of arbitrary size, but the most common ones in the context of spatio-temporal referencing are
 * not greater than 5×5 (because the matrix size in affine transforms is the number of dimensions + 1).
 * This package differs from other matrix packages by the special treatment done for such small matrices.
 *
 * <p><b>Example:</b> In the two dimensional case, an affine transform from a map projection (units in metres)
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
