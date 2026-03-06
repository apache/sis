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
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractSimilarity<T extends AbstractSimilarity<T>> extends SimplifiedTransform implements Similarity<T>{

    protected static final String PROPERTY_MATRIX = "matrix";

    //keep track of the matrix state
    protected final Object lock = new Object();
    protected PropertyChangeSupport eventManager;
    protected final Matrix<?> oldMatrix;

    protected boolean dirty = true;
    protected boolean inverseDirty = true;
    protected boolean affineDirty = true;

    //store a view of the global matrix of size dimension+1
    //which group rotation,scale and translation
    private final Matrix<?> matrix;
    private final Matrix<?> inverseMatrix;
    private final Matrix<?> inverseRotation;
    private final Affine<?> affine;
    private final Affine<?> inverseAffine;

    public AbstractSimilarity(int dimension) {
        super(dimension);

        this.inverseRotation = MatrixND.create(dimension, dimension).setToIdentity();
        this.dirty = false;
        final int msize = dimension + 1;
        this.matrix = MatrixND.create(msize, msize).setToIdentity();
        this.oldMatrix = this.matrix.copy();
        this.inverseMatrix = this.matrix.copy();
        this.affine = AffineND.create(dimension);
        this.inverseAffine = AffineND.create(dimension);
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
    public ReadOnly.Matrix<?> viewMatrix(){
        if (dirty){
            dirty = false;
            //update matrix
            final Vector<?> translation = getTranslation();
            final Matrix<?> rotation = getRotation();
            final Vector<?> scale = getScale();
            matrix.setToIdentity();
            matrix.set(rotation);
            matrix.scale(scale.extend(1).toArrayDouble());
            for (int i = 0, dimension = getDimension(); i < dimension; i++) {
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
    public ReadOnly.Matrix<?> viewMatrixInverse(){
        if (dirty){
            viewMatrix();
        }
        if (inverseDirty){
            inverseDirty = false;
            inverseRotation.set(getRotation()).invert();
            inverseMatrix.set(matrix).invert();
        }
        return inverseMatrix;
    }

    @Override
    public ReadOnly.Affine<?> viewAffine() {
        if (dirty || affineDirty) {
            affine.setFromMatrix(viewMatrix());
            inverseAffine.setFromMatrix(viewMatrixInverse());
            affineDirty = false;
        }
        return affine;
    }

    @Override
    public ReadOnly.Affine<?> viewAffineInverse() {
        if (dirty || affineDirty) {
            affine.setFromMatrix(viewMatrix());
            inverseAffine.setFromMatrix(viewMatrixInverse());
            affineDirty = false;
        }
        return inverseAffine;
    }

    @Override
    public T setFromAffine(ReadOnly.Affine<?> trs){
        setFromMatrix(trs.toMatrix());
        return (T) this;
    }

    @Override
    public T setFromMatrix(ReadOnly.Matrix<?> matrix) {
        Matrices.decomposeMatrix(matrix, getRotation(), getScale(), getTranslation());
        notifyChanged();
        return (T) this;
    }

    @Override
    public T setToTranslation(double[] trs){
        boolean change = false;
        final Matrix<?> rotation = getRotation();
        if (!rotation.isIdentity()){
            change = true;
            rotation.setToIdentity();
        }
        final Vector<?> scale = getScale();
        if (!scale.isAll(1.0)){
            change = true;
            scale.setAll(1.0);
        }
        final Vector<?> translation = getTranslation();
        if (!Arrays.equals(trs, translation.toArrayDouble())){
            change = true;
            translation.set(trs);
        }

        if (change) notifyChanged();

        return (T) this;
    }

    @Override
    public T setToIdentity(){
        boolean change = false;
        final Matrix<?> rotation = getRotation();
        if (!rotation.isIdentity()){
            change = true;
            rotation.setToIdentity();
        }
        final Vector<?> scale = getScale();
        if (!scale.isAll(1.0)){
            change = true;
            scale.setAll(1.0);
        }
        final Vector<?> translation = getTranslation();
        if (!translation.isAll(0.0)){
            change = true;
            translation.setAll(0.0);
        }

        if (change) notifyChanged();
        return (T) this;
    }

    @Override
    public T invert() {
        return setFromAffine(toAffine().invert());
    }

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
        return getScale().isAll(1) && getTranslation().isAll(0) && getRotation().isIdentity();
    }

    @Override
    public Transform createInverse() {
        return toAffine().invert().copy();
    }

    @Override
    public Tuple<?> transform(ReadOnly.Tuple<?> source, Tuple<?> out) {
        return toAffine().transform(source, out);
    }

    @Override
    public void transform(double[] in, int sourceOffset, double[] out, int destOffset, int nbTuple) {
        toAffine().transform(in, sourceOffset, out, destOffset, nbTuple);
    }

    @Override
    public void transform(float[] in, int sourceOffset, float[] out, int destOffset, int nbTuple) {
        toAffine().transform(in, sourceOffset, out, destOffset, nbTuple);
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        toAffine().transform(source, sourceOffset, dest, destOffset, 1);
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        toAffine().transform(source, sourceOffset, dest, destOffset, 1);
    }

    @Override
    public Tuple<?> inverseTransform(ReadOnly.Tuple<?> source, Tuple<?> dest) {
        return toAffine().invert().transform(source, dest);
    }

    @Override
    public Matrix<?> toMatrix() {
        return viewMatrix().copy();
    }

    @Override
    public Matrix<?> toMatrix(Matrix<?> buffer) {
        if (buffer == null) return toMatrix();
        buffer.set(viewMatrix());
        return buffer;
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
     * Flag to indicate the transform parameters has changed.
     * This is used to recalculate the general matrix when needed.
     */
    @Override
    public void notifyChanged(){
        dirty = true;
        inverseDirty = true;
        affineDirty = true;

        if (eventManager != null && eventManager.hasListeners(PROPERTY_MATRIX)) {
            //we have listeners, we need to recalculate the transform now
            eventManager.firePropertyChange(PROPERTY_MATRIX, oldMatrix.copy(), viewMatrix().copy());
        }
    }
}
