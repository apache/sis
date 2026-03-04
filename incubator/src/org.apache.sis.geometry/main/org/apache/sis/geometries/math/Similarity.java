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

/**
 * A similarity is the equivalent of a affine transform but preserving angles by avoiding
 * shearing value not rotations.
 * 3 different elements are stored.
 * - rotation matrix
 * - translation vector
 * - scale vector
 * <p>
 * The purpose of similary is to store elements separately, avoiding innacuracy and progressive
 * distortion when opearations are accumulated.
 * <p>
 * A good description of the problem can be found here :
 * https://www.gamedeveloper.com/programming/in-depth-matrices-rotation-scale-and-drifting
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Similarity<T extends Similarity<T>> extends Affine<T> {

    /**
     * Dimension of the transform.
     * @return int
     */
    int getDimension();

    /**
     * Get transform rotation.
     * Call notifyChanged after if you modified the values.
     *
     * @return Matrix
     */
    Matrix getRotation();

    /**
     * Get transform scale.
     * Call notifyChanged after if you modified the values.
     *
     * @return Vector
     */
    Vector<?> getScale();

    /**
     * Get transform translation.
     * Call notifyChanged after if you modified the values.
     *
     * @return Vector
     */
    Vector<?> getTranslation();

    /**
     * Get a general matrix view of size : dimension+1
     * This matrix combine rotation, scale and translation
     *
     * [R*S, R*S, R*S, T]
     * [R*S, R*S, R*S, T]
     * [R*S, R*S, R*S, T]
     * [  0,   0,   0, 1]
     *
     * @return Matrix, never null
     */
    Matrix viewMatrix();

    /**
     * Get a general inverse matrix view of size : dimension+1
     *
     * @return Matrix, never null
     */
    Matrix viewMatrixInverse();

    /**
     * {@inheritDoc }
     */
    @Override
    void transform(double[] in, int sourceOffset, double[] out, int destOffset, int nbTuple);

    /**
     * {@inheritDoc }
     */
    @Override
    void transform(float[] in, int sourceOffset, float[] out, int destOffset, int nbTuple);

    /**
     * Inverse transform a single tuple.
     *
     * @param source tuple, can not be null.
     * @param dest tuple, can be null.
     * @return destination tuple.
     */
    Tuple<?> inverseTransform(Tuple<?> source, Tuple<?> dest);

    /**
     * Inverse view of this transform.
     * The returned affine is no modifiable.
     * The returned affine reflects any change made to this transform
     *
     * @return inverse transform view
     */
    Affine<?> inverse();

    T multiply(Similarity<?> other);

    /**
     * Copy values from given transform.
     * @param trs
     * @return this instance
     */
    T set(Similarity<?> trs);

    /**
     * Set transform from given matrix.
     * Matrix must be orthogonal of size dimension+1.
     *
     * @param trs
     */
    @Override
    T setFromMatrix(Matrix trs);

    /**
     * Set transform from given matrix.
     * Affine must be of same size.
     *
     * @param trs
     */
    @Override
    T set(Affine<?> trs);

    /**
     * Set to identity.
     * This method will send a change event if values have changed.
     */
    @Override
    T setToIdentity();

    /**
     * Set this transform to given translation.
     * This will reset rotation and scale values.
     *
     * This method will send a change event if values have changed.
     * @param trs
     */
    T setToTranslation(double[] trs);

    /**
     * Flag to indicate the transform parameters has changed.
     * This is used to recalculate the general matrix when needed.
     */
    void notifyChanged();

}
