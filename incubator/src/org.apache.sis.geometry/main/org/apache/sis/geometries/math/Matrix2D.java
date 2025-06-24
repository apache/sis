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
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Matrix2D extends Matrix{

    private final Matrix2 matrix;

    public Matrix2D() {
        this.matrix = new Matrix2();
    }

    public Matrix2D(Matrix2 matrix) {
        this.matrix = matrix;
    }

    public Matrix2D(double m00, double m01, double m10, double m11) {
        this.matrix = new Matrix2(m00, m01, m10, m11);
    }

    @Override
    public void transform(Tuple tuple, Tuple result) {
        //TODO not efficient
        result.set(matrix.multiply(tuple.toArrayDouble()));
    }

    @Override
    public Matrix2D inverse() throws NoninvertibleMatrixException {
        return new Matrix2D(Matrix2.castOrCopy(matrix.inverse()));
    }

    public Matrix2D fromRotation(double angleRad) {
        final double sin = Math.sin(angleRad);
        final double cos = Math.cos(angleRad);
        matrix.m00 = cos;
        matrix.m01 = sin;
        matrix.m10 = -sin;
        matrix.m11 = cos;
        return this;
    }

}
