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

import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;


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
public interface Similarity<T extends Similarity<T>> extends ReadOnly.Similarity<T> {

    public static final int ROTATION_UPDATED = 1;
    public static final int SCALE_UPDATED = 1 << 1;
    public static final int TRANSLATION_UPDATED = 1 << 2;
    public static final int ALL_UPDATED = ROTATION_UPDATED | SCALE_UPDATED | TRANSLATION_UPDATED;

    /**
     * Update this similarity.
     * This method will send a change event if values have changed.
     *
     * @param updater, gets rotation,scale,translation as input and returns the modified elements.
     * @return this similarity
     */
    T update(TriFunction<Matrix<?>,Vector<?>,Vector<?>,Integer> updater);

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
    ReadOnly.Matrix<?> viewMatrix();

    /**
     * Get a general inverse matrix view of size : dimension+1
     *
     * @return Matrix, never null
     */
    ReadOnly.Matrix<?> viewMatrixInverse();

    /**
     * Get an affine view of size : dimension+1
     * This affine combine rotation, scale and translation
     *
     * [R*S, R*S, R*S, T]
     * [R*S, R*S, R*S, T]
     * [R*S, R*S, R*S, T]
     *
     * @return Affine, never null
     */
    ReadOnly.Affine<?> viewAffine();

    /**
     * Inverse view of this transform.
     * The returned affine reflects any change made to this transform
     *
     * @return inverse transform view
     */
    ReadOnly.Affine<?> viewAffineInverse();

    /**
     * Multiply this similarity by given similarity and store the result in this similarity.
     * This method will send a change event if values have changed.
     *
     * @param other multiplying similarity
     * @return this similarity
     */
    T multiply(ReadOnly.Similarity<?> other);

    /**
     * Copy values from given transform.
     * This method will send a change event if values have changed.
     *
     * @param trs
     * @return this instance
     */
    T set(ReadOnly.Similarity<?> trs);

    /**
     * Update the rotation.
     * This method will send a change event if values have changed.
     *
     * @param rotation new rotation
     * @return this instance
     */
    T setRotation(ReadOnly.Matrix<?> rotation);

    /**
     * Update the scale.
     * This method will send a change event if values have changed.
     *
     * @param scale new scale
     * @return this instance
     */
    T setScale(ReadOnly.Tuple<?> scale);

    /**
     * Update the translation.
     * This method will send a change event if values have changed.
     *
     * @param translation new translation
     * @return this instance
     */
    T setTranslation(ReadOnly.Tuple<?> translation);

    /**
     * Set transform from given matrix.
     * Matrix must be orthogonal of size dimension+1.
     * This method will send a change event if values have changed.
     *
     * @param trs
     * @throws IllegalArgumentException if matrix is not affine
     */
    T setFromMatrix(ReadOnly.Matrix<?> trs) throws IllegalArgumentException;

    /**
     * Set transform from given affine.
     * Affine must be of same size.
     * This method will send a change event if values have changed.
     *
     * @param trs
     */
    T setFromAffine(ReadOnly.Affine<?> trs);

    /**
     * Set to identity.
     * This method will send a change event if values have changed.
     */
    T setToIdentity();

    /**
     * Set this transform to given translation.
     * This will reset rotation and scale values.
     * This method will send a change event if values have changed.
     *
     * @param trs
     */
    T setToTranslation(double[] trs);

    /**
     * Inverse this affine transform.
     * This method will send a change event if values have changed.
     *
     * @return this affine instance
     */
    T invert();

    /**
     * Create a copy of this Affine.
     *
     * @return copy
     */
    @Override
    T copy();

    /**
     * Combine the different elements to obtain a linear transform of dimension 3.
     */
    default MathTransform toMathTransform() {
        return MathTransforms.linear(toMatrix().toMatrixSIS());
    }

    @FunctionalInterface
    public static interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
