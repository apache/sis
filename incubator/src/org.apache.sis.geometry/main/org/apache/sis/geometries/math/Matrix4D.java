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

import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class Matrix4D extends Matrix{

    private final Matrix4 matrix;

    public Matrix4D() {
        this.matrix = new Matrix4();
    }

    public Matrix4D(Matrix4 matrix) {
        this.matrix = matrix;
    }

    public Matrix4D(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33
            ) {
        this.matrix = new Matrix4(
                m00, m01, m02, m03,
                m10, m11, m12, m13,
                m20, m21, m22, m23,
                m30, m31, m32, m33
        );
    }

    @Override
    public void transform(Tuple tuple, Tuple result) {
        //TODO not efficient
        result.set(matrix.multiply(tuple.toArrayDouble()));
    }

    @Override
    public Matrix4D inverse() throws NoninvertibleMatrixException {
        return new Matrix4D(Matrix4.castOrCopy(matrix.inverse()));
    }
}
