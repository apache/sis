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

/**
 * 3D similarity.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Similarity3D extends AbstractSimilarity<Similarity3D> {

    public final Vector3D.Double scale = new Vector3D.Double(1, 1, 1);
    public final Vector3D.Double translation = new Vector3D.Double(0, 0, 0);
    public final Matrix3D rotation = new Matrix3D(1,0,0, 0,1,0, 0,0,1);

    public Similarity3D(){
        super(3);
    }

    @Override
    public int getInputDimensions() {
        return 3;
    }

    @Override
    public int getOutputDimensions() {
        return 3;
    }

    @Override
    public int getDimension() {
        return 3;
    }

    @Override
    public Matrix3D getRotation() {
        return rotation;
    }

    @Override
    public Vector3D.Double getScale() {
        return scale;
    }

    @Override
    public Vector3D.Double getTranslation() {
        return translation;
    }

    @Override
    public Similarity3D multiply(Similarity<?> other) {
        final Vector<?> resTrans = other.getTranslation().copy()
                .transform(rotation)
                .multiply(scale)
                .add(translation);

        rotation.multiply(other.getRotation());
        scale.multiply(other.getScale());
        translation.set(resTrans);
        notifyChanged();
        return this;
    }

    @Override
    public Similarity3D set(Similarity<?> trs) {
        if (rotation.equals(trs.getRotation()) && scale.equals(trs.getScale()) && translation.equals(trs.getTranslation())){
            //nothing changes
            return this;
        }
        rotation.set(trs.getRotation());
        scale.set(trs.getScale());
        translation.set(trs.getTranslation());
        notifyChanged();
        return this;
    }

    /**
     * Combine the different elements to obtain a 4x4 matrix.
     * [R*S, R*S, R*S, T]
     * [R*S, R*S, R*S, T]
     * [R*S, R*S, R*S, T]
     * [  0,   0,   0, 1]
     */
    @Override
    public Matrix4D toMatrix() {

        final Matrix4D matrix = new Matrix4D();
        //set rotation
        matrix.m00 = rotation.m00; matrix.m10 = rotation.m10; matrix.m20 = rotation.m20;
        matrix.m01 = rotation.m01; matrix.m11 = rotation.m11; matrix.m21 = rotation.m21;
        matrix.m02 = rotation.m02; matrix.m12 = rotation.m12; matrix.m22 = rotation.m22;
        //scale matrix
        matrix.m00 *= scale.x; matrix.m01 *= scale.y; matrix.m02 *= scale.z;
        matrix.m10 *= scale.x; matrix.m11 *= scale.y; matrix.m12 *= scale.z;
        matrix.m20 *= scale.x; matrix.m21 *= scale.y; matrix.m22 *= scale.z;
        //add translation
        matrix.m03 = translation.x;
        matrix.m13 = translation.y;
        matrix.m23 = translation.z;

        return matrix;
    }

    @Override
    public Affine3D toAffine() {
        final Affine3D affine = new Affine3D();
        //set rotation
        affine.m00 = rotation.m00; affine.m10 = rotation.m10; affine.m20 = rotation.m20;
        affine.m01 = rotation.m01; affine.m11 = rotation.m11; affine.m21 = rotation.m21;
        affine.m02 = rotation.m02; affine.m12 = rotation.m12; affine.m22 = rotation.m22;
        //scale matrix
        affine.m00 *= scale.x; affine.m01 *= scale.y; affine.m02 *= scale.z;
        affine.m10 *= scale.x; affine.m11 *= scale.y; affine.m12 *= scale.z;
        affine.m20 *= scale.x; affine.m21 *= scale.y; affine.m22 *= scale.z;
        //add translation
        affine.m03 = translation.x;
        affine.m13 = translation.y;
        affine.m23 = translation.z;

        return affine;
    }

    @Override
    public Matrix<?> toMatrix(Matrix<?> buffer) {
        if (buffer == null) return toMatrix();
        buffer.set(toMatrix());
        return buffer;
    }

    @Override
    public Similarity3D copy() {
        return new Similarity3D().set(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.scale,
                this.translation,
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
            && Objects.equals(this.translation, other.translation)
            && Objects.equals(this.rotation, other.rotation);
    }

}
