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
package org.apache.sis.referencing.operation.transform;

import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;


/**
 * A {@link MathTransform} which convert coordinates using only linear equations.
 * Such transform can be represented by a {@linkplain #getMatrix() matrix}.
 * Those transforms are often affine, but not necessarily.
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>The {@linkplain Matrix#getNumCol() number of columns} in the matrix is equal to
 *       the number of {@linkplain #getSourceDimensions() source dimensions} plus 1</li>
 *   <li>The {@linkplain Matrix#getNumRow() number of rows} in the matrix is equal to
 *       the number of {@linkplain #getTargetDimensions() target dimensions} plus 1.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 *
 * @see MathTransforms#getMatrix(MathTransform)
 */
public interface LinearTransform extends MathTransform {
    /**
     * Returns the coefficients of this linear transform as a matrix.
     * Converting a coordinate with this {@code MathTransform} is equivalent to multiplying the
     * returned matrix by a vector containing the ordinate values with an additional 1 in the last row.
     * For example if this transform converts projected coordinates (metres) to display coordinates (pixel),
     * then the same conversions can be done by the returned matrix as below:
     *
     * <center><p>
     * <img src="../matrix/doc-files/AffineTransform.png" alt="Matrix representation of an affine transform">
     * </p></center>
     *
     * @return The coefficients of this linear transform as a matrix.
     */
    Matrix getMatrix();
}
