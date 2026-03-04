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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

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
public class SimilarityND extends AbstractAffine<SimilarityND> implements Similarity<SimilarityND> {

    private static final String PROPERTY_MATRIX = "matrix";

    //keep track of the matrix state
    private final Object lock = new Object();
    private PropertyChangeSupport eventManager;
    private final Matrix<?> oldMatrix;

    private final int dimension;
    private final Matrix<?> rotation;
    private final Vector<?> scale;
    private final Vector<?> translation;

    //store a view of the global matrix of size dimension+1
    //which group rotation,scale and translation
    private final Inverse inverseAff = new Inverse();
    private boolean dirty = true;
    private boolean inverseDirty = true;
    private boolean affineDirty = true;
    private final Matrix<?> matrix;
    private final Matrix<?> inverseMatrix;
    private final Matrix<?> inverseRotation;
    private final Affine<?> affine;

    public static Similarity<?> create(int dimension) {
        return new SimilarityND(dimension);
    }

    public static Similarity<?> create(Matrix rotation, Vector<?> scale, Vector<?> translation) {
        return new SimilarityND(rotation, scale, translation);
    }

    public static Similarity<?> create(Quaternion rotation, Vector<?> scale, Vector<?> translation) {
        return new SimilarityND(rotation, scale, translation);
    }

    protected SimilarityND(int dimension){
        super(dimension);
        this.dimension = dimension;
        this.rotation = MatrixND.create(dimension, dimension).setToIdentity();
        this.inverseRotation = rotation.copy();
        this.scale = Vectors.create(dimension, DataType.DOUBLE);
        //set scale to 1 by default
        this.scale.setAll(1d);
        this.translation = Vectors.create(dimension, DataType.DOUBLE);
        this.dirty = false;
        final int msize = dimension+1;
        this.matrix = MatrixND.create(msize, msize).setToIdentity();
        this.oldMatrix = this.matrix.copy();
        this.inverseMatrix = MatrixND.create(msize, msize);
        this.affine = AffineND.create(dimension);
    }

    protected SimilarityND(Matrix rotation, Vector<?> scale, Vector<?> translation) {
        super(scale.getDimension());
        this.dimension = scale.getDimension();
        this.rotation = rotation;
        this.inverseRotation = this.rotation.copy();
        this.scale = scale;
        this.translation = translation;
        final int msize = dimension+1;
        this.matrix = MatrixND.create(msize, msize).setToIdentity();
        this.oldMatrix = this.matrix.copy();
        this.inverseMatrix = MatrixND.create(msize, msize);
        this.affine = AffineND.create(dimension);
    }

    protected SimilarityND(Quaternion rotation, Vector<?> scale, Vector<?> translation) {
        super(scale.getDimension());
        this.dimension = scale.getDimension();
        this.rotation = rotation.toMatrix3();
        this.inverseRotation = this.rotation.copy();
        this.scale = scale;
        this.translation = translation;
        final int msize = dimension+1;
        this.matrix = MatrixND.create(msize, msize).setToIdentity();
        this.oldMatrix = this.matrix.copy();
        this.inverseMatrix = MatrixND.create(msize, msize);
        this.affine = AffineND.create(dimension);
    }

    /**
     * Copy values from given transform.
     * @param trs
     */
    @Override
    public SimilarityND set(Similarity trs){
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
     * Set transform from given matrix.
     * Matrix must be orthogonal of size dimension+1.
     * @param trs
     */
    @Override
    public SimilarityND setFromMatrix(Matrix trs){
        Matrices.decomposeMatrix(trs, rotation, scale, translation);
        notifyChanged();
        return this;
    }

    /**
     * Set transform from given matrix.
     * Matrix must be orthogonal of size dimension+1.
     * @param trs
     */
    @Override
    public SimilarityND set(Affine trs){
        setFromMatrix(trs.toMatrix());
        return this;
    }

    @Override
    public SimilarityND set(int row, int col, double value) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Set to identity.
     * This method will send a change event if values have changed.
     */
    @Override
    public SimilarityND setToIdentity(){
        boolean change = false;
        if (!rotation.isIdentity()){
            change = true;
            rotation.setToIdentity();
        }
        if (!scale.isAll(1.0)){
            change = true;
            scale.setAll(1.0);
        }
        if (!translation.isAll(0.0)){
            change = true;
            translation.setAll(0.0);
        }

        if (change) notifyChanged();
        return this;
    }

    /**
     * Set this transform to given translation.
     * This will reset rotation and scale values.
     *
     * This method will send a change event if values have changed.
     */
    @Override
    public SimilarityND setToTranslation(double[] trs){
        boolean change = false;
        if (!rotation.isIdentity()){
            change = true;
            rotation.setToIdentity();
        }
        if (!scale.isAll(1.0)){
            change = true;
            scale.setAll(1.0);
        }
        if (!Arrays.equals(trs, translation.toArrayDouble())){
            change = true;
            translation.set(trs);
        }

        if (change) notifyChanged();

        return this;
    }

    @Override
    public double get(int row, int col) {
        return viewMatrix().get(row, col);
    }

    /**
     * Dimension of the transform.
     * @return int
     */
    @Override
    public int getDimension() {
        return dimension;
    }

    /**
     * Get transform rotation.
     * Call notifyChanged after if you modified the values.
     *
     * @return Matrix
     */
    @Override
    public Matrix getRotation() {
        return rotation;
    }

    /**
     * Get transform scale.
     * Call notifyChanged after if you modified the values.
     *
     * @return Vector<?>
     */
    @Override
    public Vector<?> getScale() {
        return scale;
    }

    /**
     * Get transform translation.
     * Call notifyChanged after if you modified the values.
     *
     * @return Vector<?>
     */
    @Override
    public Vector<?> getTranslation() {
        return translation;
    }

    /**
     * Flag to indicate the transform parameters has changed.
     * This is used to recalculate the general matrix when needed.
     */
    @Override
    public void notifyChanged(){
        dirty=true;
        inverseDirty=true;
        affineDirty=true;

        if (eventManager!=null && eventManager.hasListeners(PROPERTY_MATRIX)){
            //we have listeners, we need to recalculate the transform now
            eventManager.firePropertyChange(PROPERTY_MATRIX, oldMatrix.copy(), viewMatrix().copy());
        }
    }

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
    @Override
    public Matrix<?> viewMatrix(){
        if (dirty){
            dirty = false;
            //update matrix
            matrix.setToIdentity();
            matrix.set(rotation);
            matrix.scale(scale.extend(1).toArrayDouble());
            for (int i=0;i<dimension;i++){
                matrix.set(i, dimension, translation.get(i));
            }
            oldMatrix.set(matrix);
            if (!matrix.isFinite()){
                throw new RuntimeException("Matrix is not finite :\nRotation\n"+rotation+"Scale "+scale+"\nTranslate "+translation);
            }
        }
        return matrix;
    }

    /**
     * Get a general inverse matrix view of size : dimension+1
     * DO NOT MODIFY THIS MATRIX.
     *
     * @return Matrix, never null
     */
    @Override
    public Matrix<?> viewMatrixInverse(){
        if (dirty){
            viewMatrix();
        }
        if (inverseDirty){
            inverseDirty = false;
            inverseRotation.set(rotation).invert();
            inverseMatrix.set(matrix).invert();
        }
        return inverseMatrix;
    }

    private Affine<?> asAffine() {
        if (dirty || affineDirty) {
            affine.setFromMatrix(viewMatrix());
            affineDirty = false;
        }
        return affine;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getInputDimensions() {
        return dimension;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getOutputDimensions() {
        return dimension;
    }

    @Override
    public Tuple<?> transform(Tuple<?> source, Tuple<?> out) {
        return asAffine().transform(source, out);

        //under code uses less memory but is much slower
//        if (out == null) out = DefaultVector<?>.create(dimension);
//
//        if (out instanceof Vector<?>) {
//            //scale
//            out.set(source);
//            ((Vector<?>) out).localMultiply(scale);
//            //rotate
//            rotation.transform(out, out);
//            //translate
//            ((Vector<?>) out).localAdd(translation);
//        } else {
//            final Vector<?> array = DefaultVector<?>.create(out);
//            //scale
//            array.set(source);
//            array.localMultiply(scale);
//            //rotate
//            rotation.transform(array, array);
//            //translate
//            array.localAdd(translation);
//            out.set(array);
//        }
//
//        return out;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void transform(double[] in, int sourceOffset, double[] out, int destOffset, int nbTuple) {
        asAffine().transform(in, sourceOffset, out, destOffset, nbTuple);

        //we could use this, but it is slower.
        //asMatrix().transform(in, sourceOffset, out, destOffset, nbTuple);

        //this is also slower
//        //scale
//        Vector<?>s.multiplyRegular(in, out, sourceOffset, destOffset, scale.toDouble(), nbTuple);
//        //rotate
//        rotation.transform(out, destOffset, out, destOffset, nbTuple);
//        //translate
//        Vector<?>s.addRegular(out, out, destOffset, destOffset, translation.toDouble(), nbTuple);
//
//        return out;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void transform(float[] in, int sourceOffset, float[] out, int destOffset, int nbTuple) {
        asAffine().transform(in, sourceOffset, out, destOffset, nbTuple);

        //we could use this, but it is slower.
        //asMatrix().transform(source, sourceOffset, dest, destOffset, nbTuple);

        //this is also slower
//        //scale
//        Vector<?>s.multiplyRegular(in, out, sourceOffset, destOffset, scale.toFloat(), nbTuple);
//        //rotate
//        rotation.transform(out, destOffset, out, destOffset, nbTuple);
//        //translate
//        Vector<?>s.addRegular(out, out, destOffset, destOffset, translation.toFloat(), nbTuple);
//
//        return out;
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        asAffine().transform(source, sourceOffset, dest, destOffset, 1);
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        asAffine().transform(source, sourceOffset, dest, destOffset, 1);
    }

    /**
     * Inverse transform a single tuple.
     *
     * @param source tuple, can not be null.
     * @param dest tuple, can be null.
     * @return destination tuple.
     */
    public Tuple<?> inverseTransform(Tuple<?> source, Tuple<?> dest){
        return inverseAff.transform(source, dest);
    }

    protected PropertyChangeSupport getEventManager() {
        return getEventManager(true);
    }

    protected PropertyChangeSupport getEventManager(boolean create) {
        synchronized (lock){
            if (eventManager==null && create) eventManager = new PropertyChangeSupport(this);
        }
        return eventManager;
    }

    public void addEventListener(PropertyChangeListener listener) {
        getEventManager().addPropertyChangeListener(listener);
    }

    public void removeEventListener(PropertyChangeListener listener) {
        getEventManager().removePropertyChangeListener(listener);
    }

    /**
     * Inverse view of this transform.
     * The returned affine is no modifiable.
     * The returned affine reflects any change made to this NodeTransform
     *
     * @return inverse transform view
     */
    @Override
    public Affine inverse() {
        return inverseAff;
    }

    @Override
    public SimilarityND invert() {
        final Affine<?> affine = AffineND.create(this);
        affine.invert();
        set(affine);
        return this;
    }


    @Override
    public SimilarityND multiply(Affine<?> other) {
        if (other instanceof Similarity<?> s) {
            return multiply(s);
        } else {
            return multiply(new SimilarityND(dimension).set(other));
        }
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
    public Matrix toMatrix() {
        return viewMatrix().copy();
    }

    @Override
    public Matrix toMatrix(Matrix buffer) {
        if (buffer==null) return toMatrix();
        buffer.set(viewMatrix());
        return buffer;
    }

    @Override
    public SimilarityND copy() {
        final SimilarityND aff = new SimilarityND(dimension);
        aff.set(this);
        return aff;
    }

    private class Inverse extends AbstractAffine<SimilarityND> {

        public Inverse() {
            super(dimension);
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
        public Tuple<?> transform(Tuple<?> source, Tuple<?> dest) {
            if (dest == null) dest = Vectors.create(dimension, DataType.DOUBLE);
            viewMatrixInverse();
            Vector<?> vd = Vectors.castOrWrap(dest);
            //inverse translate
            vd.set(source).subtract(translation);
            //inverse rotate
            inverseRotation.transform(vd, vd);
            //invert scale
            vd.divide(scale);
            return vd;
        }

        @Override
        public void transform(double[] source, int sourceOffset, double[] dest, int destOffset, int nbTuple) {
            viewMatrixInverse();
            //inverse translate
            Vectors.subtractRegular(source, dest, sourceOffset, destOffset, translation.toArrayDouble(), nbTuple);
            //inverse rotate
            inverseRotation.transform(dest, destOffset, dest, destOffset, nbTuple);
            //invert scale
            Vectors.divideRegular(dest, dest, destOffset, destOffset, scale.toArrayDouble(), nbTuple);
        }

        @Override
        public void transform(float[] source, int sourceOffset, float[] dest, int destOffset, int nbTuple) {
            viewMatrixInverse();
            //inverse translate
            Vectors.subtractRegular(source, dest, sourceOffset, destOffset, translation.toArrayFloat(), nbTuple);
            //inverse rotate
            inverseRotation.transform(dest, destOffset, dest, destOffset, nbTuple);
            //invert scale
            Vectors.divideRegular(dest, dest, destOffset, destOffset, scale.toArrayFloat(), nbTuple);
        }

        @Override
        protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
            viewMatrixInverse();
            //inverse translate
            Vectors.subtractRegular(source, dest, sourceOffset, destOffset, translation.toArrayDouble(), 1);
            //inverse rotate
            inverseRotation.transform(dest, destOffset, dest, destOffset, 1);
            //invert scale
            Vectors.divideRegular(dest, dest, destOffset, destOffset, scale.toArrayDouble(), 1);
        }

        @Override
        protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
            viewMatrixInverse();
            //inverse translate
            Vectors.subtractRegular(source, dest, sourceOffset, destOffset, translation.toArrayFloat(), 1);
            //inverse rotate
            inverseRotation.transform(dest, destOffset, dest, destOffset, 1);
            //invert scale
            Vectors.divideRegular(dest, dest, destOffset, destOffset, scale.toArrayFloat(), 1);
        }

        @Override
        public double get(int row, int col) {
            return viewMatrixInverse().get(row, col);
        }

        @Override
        public Matrix toMatrix() {
            return viewMatrixInverse().copy();
        }

        @Override
        public Matrix toMatrix(Matrix buffer) {
            if (buffer==null) {
                return viewMatrixInverse().copy();
            } else {
                buffer.set(viewMatrixInverse());
                return buffer;
            }
        }

        @Override
        public SimilarityND set(int row, int col, double value) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public SimilarityND setFromMatrix(Matrix m) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public SimilarityND createInverse() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public SimilarityND copy() {
            final SimilarityND aff = new SimilarityND(dimension);
            aff.set(Inverse.this);
            return aff;
        }
    }
}
