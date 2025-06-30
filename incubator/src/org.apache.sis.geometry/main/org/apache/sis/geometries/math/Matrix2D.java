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
package org.apache.sis.geometries.math;

import org.apache.sis.referencing.operation.matrix.Matrix2;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Matrix2D extends Matrix2{

    public Matrix2D() {
    }

    public Matrix2D(double m00, double m01, double m10, double m11) {
        super(m00, m01, m10, m11);
    }

    public Matrix2D(double[] elements) {
        super(elements);
    }


    public void transform(Tuple tuple, Tuple result) {
        //TODO not efficient
        result.set(multiply(tuple.toArrayDouble()));
    }

    /**
     * Casts or copies the given matrix to a {@code Matrix2D} implementation. If the given {@code matrix}
     * is already an instance of {@code Matrix2D}, then it is returned unchanged. Otherwise this method
     * verifies the matrix size, then copies all elements in a new {@code Matrix2D} object.
     *
     * @param  matrix  the matrix to cast or copy, or {@code null}.
     * @return the matrix argument if it can be safely casted (including {@code null} argument),
     *         or a copy of the given matrix otherwise.
     * @throws IllegalArgumentException if the size of the given matrix is not {@value #SIZE}×{@value #SIZE}.
     */
    public static Matrix2D castOrCopy(final org.opengis.referencing.operation.Matrix matrix) throws IllegalArgumentException {
        if (matrix == null || matrix instanceof Matrix2D) {
            return (Matrix2D) matrix;
        }
        if (matrix.getNumCol() != 2 || matrix.getNumRow() != 2) {
            throw new IllegalArgumentException("Matrix is not of size 2x2");
        }
        return new Matrix2D(Matrix2.castOrCopy(matrix).getElements());
    }
}
