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

import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.opengis.referencing.operation.MathTransform;

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
public interface Similarity<T extends Similarity<T>> extends Transform {

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
    Matrix<?> getRotation();

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
     *ç&é ,
     * +
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
    Matrix<?> viewMatrix();

    /**
     * Get a general inverse matrix view of size : dimension+1
     *
     * @return Matrix, never null
     */
    Matrix<?> viewMatrixInverse();

    Affine<?> viewAffine();

    /**
     * Inverse view of this transform.
     * The returned affine is no modifiable.
     * The returned affine reflects any change made to this transform
     *
     * @return inverse transform view
     */
    Affine<?> viewAffineInverse();

    /**
     * Inverse transform a single tuple.
     *
     * @param source tuple, can not be null.
     * @param dest tuple, can be null.
     * @return destination tuple.
     */
    Tuple<?> inverseTransform(Tuple<?> source, Tuple<?> dest);

    /**
     * Multiply this similarity by given similarity and store the result in this similarity.
     *
     * @param other multiplying similarity
     * @return this similarity
     */
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
     * @throws IllegalArgumentException if matrix is not affine
     */
    T setFromMatrix(Matrix<?> trs) throws IllegalArgumentException;

    /**
     * Set transform from given affine.
     * Affine must be of same size.
     *
     * @param trs
     */
    T setFromAffine(Affine<?> trs);

    /**
     * Set to identity.
     * This method will send a change event if values have changed.
     */
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
     * Inverse this affine transform.
     *
     * @return this affine instance
     */
    T invert();

    /**
     * Create a square matrix of size dimensions+1
     * The last matrix line will be [0,...,1]
     *
     * @return matrix
     */
    Matrix<?> toMatrix();

    /**
     * Create a square matrix of size dimensions+1
     * The last matrix line will be [0,...,1]
     *
     * @param buffer to store matrix values in
     * @return matrix
     */
    Matrix<?> toMatrix(Matrix<?> buffer);

    /**
     * Create and affine transform.
     *
     * @return 
     */
    Affine<?> toAffine();

    /**
     * Create a copy of this Affine.
     *
     * @return copy
     */
    T copy();

    /**
     * Flag to indicate the transform parameters has changed.
     * This is used to recalculate the general matrix when needed.
     */
    void notifyChanged();

    /**
     * Combine the different elements to obtain a linear transform of dimension 3.
     */
    default MathTransform toMathTransform() {
        return MathTransforms.linear(toMatrix().toMatrixSIS());
    }
}
