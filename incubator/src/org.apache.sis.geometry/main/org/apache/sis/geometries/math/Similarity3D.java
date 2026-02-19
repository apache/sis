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

import java.util.Objects;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.opengis.referencing.operation.MathTransform;

/**
 * 3D similarity.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Similarity3D implements Similarity {

    public final Vector3D.Double scale = new Vector3D.Double(1, 1, 1);
    public final Vector3D.Double translate = new Vector3D.Double(0, 0, 0);
    public final Matrix3 rotation = new Matrix3(1,0,0, 0,1,0, 0,0,1);

    public Similarity3D(){}

    /**
     * Test if this transform is identity.
     * <ul>
     *  <li>Scale must be all at 1</li>
     *  <li>Translation must be all at 0</li>
     *  <li>Rotation must be an identity matrix</li>
     * </ul>
     *
     * @return true if transform is identity.
     */
    @Override
    public boolean isIdentity() {
        return scale.isAll(1) && translate.isAll(0) && rotation.isIdentity();
    }

    /**
     * Set this transformation to identity.
     */
    @Override
    public void toIdentity() {
        scale.setAll(1.0);
        translate.setAll(0.0);
        rotation.m00 = 1.0; rotation.m01 = 0.0; rotation.m02 = 0.0;
        rotation.m10 = 0.0; rotation.m11 = 1.0; rotation.m12 = 0.0;
        rotation.m20 = 0.0; rotation.m21 = 0.0; rotation.m22 = 1.0;
    }

    /**
     * Combine the different elements to obtain a 4x4 matrix.
     * [R*S, R*S, R*S, T]
     * [R*S, R*S, R*S, T]
     * [R*S, R*S, R*S, T]
     * [  0,   0,   0, 1]
     */
    public Matrix4 toMatrix() {

        Matrix4 matrix = new Matrix4();
        //set rotation
        matrix.m00 = rotation.m00; matrix.m10 = rotation.m10; matrix.m20 = rotation.m20;
        matrix.m01 = rotation.m01; matrix.m11 = rotation.m11; matrix.m21 = rotation.m21;
        matrix.m02 = rotation.m02; matrix.m12 = rotation.m12; matrix.m22 = rotation.m22;
        //scale matrix
        matrix.m00 *= scale.x; matrix.m01 *= scale.y; matrix.m02 *= scale.z;
        matrix.m10 *= scale.x; matrix.m11 *= scale.y; matrix.m12 *= scale.z;
        matrix.m20 *= scale.x; matrix.m21 *= scale.y; matrix.m22 *= scale.z;
        //add translation
        matrix.m03 = translate.x;
        matrix.m13 = translate.y;
        matrix.m23 = translate.z;

        return matrix;
    }

    /**
     * Extract scale, translation and rotation from 4x4 matrix.
     */
    public void fromMatrix(Matrix4 matrix) {
        Matrices.decomposeMatrix(matrix, rotation, scale, translate);
    }

    /**
     * Copy scale, translation and rotation from transform.
     */
    public void fromTransform(Similarity3D trs) {
        this.rotation.setMatrix(trs.rotation);
        this.translate.set(trs.translate);
        this.scale.set(trs.scale);
    }

    /**
     * Combine the different elements to obtain a linear transform of dimension 3.
     */
    public MathTransform toMathTransform() {
        return MathTransforms.linear(toMatrix());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.scale,
                this.translate,
                this.rotation);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Similarity3D other = (Similarity3D) obj;
        return Objects.equals(this.scale, other.scale)
            && Objects.equals(this.translate, other.translate)
            && Objects.equals(this.rotation, other.rotation);
    }
}
