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
package org.apache.sis.referencing.operation.matrix;

import java.awt.geom.AffineTransform;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;


/**
 * Bridge between {@link Matrix} and Java2D {@link AffineTransform} instances.
 * Those {@code AffineTransform} instances can be viewed as 3×3 matrices.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
public final class AffineTransforms2D extends Static {
    /**
     * Do not allows instantiation of this class.
     */
    private AffineTransforms2D() {
    }

    /**
     * Returns the given matrix as a Java2D affine transform.
     * If the given matrix is already an instance of {@link AffineTransform}, then it is returned directly.
     * Otherwise the values are copied in a new {@code AffineTransform} instance.
     *
     * @param  matrix The matrix to returns as an affine transform, or {@code null}.
     * @return The matrix argument if it can be safely casted (including {@code null} argument),
     *         or a copy of the given matrix otherwise.
     * @throws IllegalArgumentException if the given matrix size is not 3×3 or if the matrix is not affine.
     *
     * @see Matrices#isAffine(Matrix)
     */
    public static AffineTransform castOrCopy(final Matrix matrix) throws IllegalArgumentException {
        if (matrix == null || matrix instanceof AffineTransform) {
            return (AffineTransform) matrix;
        }
        MatrixSIS.ensureSizeMatch(3, matrix);
        if (Matrices.isAffine(matrix)) {
            return new AffineTransform(matrix.getElement(0,0), matrix.getElement(1,0),
                                       matrix.getElement(0,1), matrix.getElement(1,1),
                                       matrix.getElement(0,2), matrix.getElement(1,2));
        }
        throw new IllegalStateException(Errors.format(Errors.Keys.NotAnAffineTransform));
    }
}
