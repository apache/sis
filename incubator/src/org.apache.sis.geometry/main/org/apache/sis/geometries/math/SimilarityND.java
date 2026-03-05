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
 *
 * History :
 * At first the project used Matrix for 3d scenes, but this approach starts to raise problems
 * when node are moved around very often (like billboards), the mathematic inaccuracy makes the
 * objects start to distord over time.
 *
 * A good description of the problem can be found here :
 * http://www.altdevblogaday.com/2012/07/03/matrices-rotation-scale-and-drifting/
 *
 *
 * @author Johann Sorel
 */
public class SimilarityND extends AbstractSimilarity<SimilarityND> {

    private final int dimension;
    public final Matrix<?> rotation;
    public final Vector<?> scale;
    public final Vector<?> translation;

    public static Similarity<?> create(int dimension) {
        return new SimilarityND(dimension);
    }

    public static Similarity<?> create(Matrix<?> rotation, Vector<?> scale, Vector<?> translation) {
        return new SimilarityND(rotation, scale, translation);
    }

    public static Similarity<?> create(Quaternion rotation, Vector<?> scale, Vector<?> translation) {
        return new SimilarityND(rotation, scale, translation);
    }

    protected SimilarityND(int dimension){
        super(dimension);
        this.dimension = dimension;
        this.rotation = MatrixND.create(dimension, dimension).setToIdentity();
        this.scale = Vectors.create(dimension, DataType.DOUBLE);
        //set scale to 1 by default
        this.scale.setAll(1d);
        this.translation = Vectors.create(dimension, DataType.DOUBLE);
    }

    protected SimilarityND(Matrix<?> rotation, Vector<?> scale, Vector<?> translation) {
        super(scale.getDimension());
        this.dimension = scale.getDimension();
        this.rotation = rotation;
        this.scale = scale;
        this.translation = translation;
        dirty = true;
    }

    protected SimilarityND(Quaternion rotation, Vector<?> scale, Vector<?> translation) {
        super(scale.getDimension());
        this.dimension = scale.getDimension();
        this.rotation = rotation.toMatrix3();
        this.scale = scale;
        this.translation = translation;
        dirty = true;
    }

    /**
     * Copy values from given transform.
     * @param trs
     */
    @Override
    public SimilarityND set(Similarity<?> trs){
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

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public int getInputDimensions() {
        return dimension;
    }

    @Override
    public int getOutputDimensions() {
        return dimension;
    }

    @Override
    public Matrix<?> getRotation() {
        return rotation;
    }

    @Override
    public Vector<?> getScale() {
        return scale;
    }

    @Override
    public Vector<?> getTranslation() {
        return translation;
    }

    @Override
    public SimilarityND multiply(Similarity<?> other) {
        /*
        b00 = A.r00*A.s0 * B.r00*B.s0 + A.r01*A.s0 * B.r10*B.s1 + A.r02*A.s0 * B.r20*B.s2;
        b01 = A.r00*A.s0 * B.r01*B.s0 + A.r01*A.s0 * B.r11*B.s1 + A.r02*A.s0 * B.r21*B.s2;
        b02 = A.r00*A.s0 * B.r02*B.s0 + A.r01*A.s0 * B.r12*B.s1 + A.r02*A.s0 * B.r22*B.s2;
        b10 = A.r10*A.s1 * B.r00*B.s0 + A.r11*A.s1 * B.r10*B.s1 + A.r12*A.s1 * B.r20*B.s2;
        b11 = A.r10*A.s1 * B.r01*B.s0 + A.r11*A.s1 * B.r11*B.s1 + A.r12*A.s1 * B.r21*B.s2;
        b12 = A.r10*A.s1 * B.r02*B.s0 + A.r11*A.s1 * B.r12*B.s1 + A.r12*A.s1 * B.r22*B.s2;
        b20 = A.r20*A.s2 * B.r00*B.s0 + A.r21*A.s2 * B.r10*B.s1 + A.r22*A.s2 * B.r20*B.s2;
        b21 = A.r20*A.s2 * B.r01*B.s0 + A.r21*A.s2 * B.r11*B.s1 + A.r22*A.s2 * B.r21*B.s2;
        b22 = A.r20*A.s2 * B.r02*B.s0 + A.r21*A.s2 * B.r12*B.s1 + A.r22*A.s2 * B.r22*B.s2;

        b03 = A.r00*A.s0 * B.t0       + A.r01*A.s0 * B.t1       + A.r02*A.s0 * B.t2         + A.t0;
        b13 = A.r10*A.s1 * B.t0       + A.r11*A.s1 * B.t1       + A.r12*A.s1 * B.t2         + A.t1;
        b23 = A.r20*A.s2 * B.t0       + A.r21*A.s2 * B.t1       + A.r22*A.s2 * B.t2         + A.t2;
        */

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
    public Matrix<?> toMatrix() {
        final Matrix<?> matrix = MatrixND.create(dimension+1, dimension+1);
        final Vector<?> translation = getTranslation();
        final Matrix<?> rotation = getRotation();
        final Vector<?> scale = getScale();
        matrix.setToIdentity();
        matrix.set(rotation);
        matrix.scale(scale.extend(1).toArrayDouble());
        for (int i = 0, dimension = getDimension(); i < dimension; i++) {
            matrix.set(i, dimension, translation.get(i));
        }
        return matrix;
    }

    @Override
    public Matrix<?> toMatrix(Matrix<?> matrix) {
        if (matrix == null) matrix = MatrixND.create(dimension+1, dimension+1);
        final Vector<?> translation = getTranslation();
        final Matrix<?> rotation = getRotation();
        final Vector<?> scale = getScale();
        matrix.setToIdentity();
        matrix.set(rotation);
        matrix.scale(scale.extend(1).toArrayDouble());
        for (int i = 0, dimension = getDimension(); i < dimension; i++) {
            matrix.set(i, dimension, translation.get(i));
        }
        return matrix;
    }

    @Override
    public Affine<?> toAffine() {
        return AffineND.create(dimension).setFromMatrix(toMatrix());
    }

    @Override
    public SimilarityND copy() {
        return new SimilarityND(dimension).set(this);
    }

}
