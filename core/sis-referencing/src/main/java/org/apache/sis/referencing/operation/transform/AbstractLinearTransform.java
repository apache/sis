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
package org.apache.sis.referencing.operation.transform;

import java.util.Arrays;
import java.io.Serializable;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of linear transforms. For efficiency reasons, this transform implements itself the matrix
 * to be returned by {@link #getMatrix()}.
 *
 * <p>Subclasses need to implement the following methods:</p>
 * <ul>
 *   <li>{@link #getElement(int, int)}</li>
 *   <li>{@link #equalsSameClass(Object)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@SuppressWarnings("CloneInNonCloneableClass")   // Intentionally not Cloneable despite the clone() method.
abstract class AbstractLinearTransform extends AbstractMathTransform implements LinearTransform, Matrix, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4649708313541868599L;

    /**
     * The inverse transform, or {@code null} if not yet created.
     * This field is part of the serialization form in order to avoid rounding errors if a user
     * asks for the inverse of the inverse (i.e. the original transform) after deserialization.
     */
    MathTransform inverse;

    /**
     * Constructs a transform.
     */
    AbstractLinearTransform() {
    }

    /**
     * Returns {@code true} if this transform is affine.
     *
     * @return {@code true} if this transform is affine, or {@code false} otherwise.
     */
    @Override
    public boolean isAffine() {
        return Matrices.isAffine(this);
    }

    /**
     * Returns a copy of the matrix that user can modify.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public final Matrix clone() {
        return Matrices.copy(this);
    }

    /**
     * Returns an immutable view of the matrix for this transform.
     */
    @Override
    public final Matrix getMatrix() {
        return this;
    }

    /**
     * Gets the number of rows in the matrix.
     */
    @Override
    public int getNumRow() {
        return getTargetDimensions() + 1;
    }

    /**
     * Gets the number of columns in the matrix.
     */
    @Override
    public int getNumCol() {
        return getSourceDimensions() + 1;
    }

    /**
     * Creates the inverse transform of this object.
     */
    @Override
    public synchronized MathTransform inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            /*
             * Should never be the identity transform at this point (except during tests) because
             * MathTransforms.linear(…) should never instantiate this class in the identity case.
             * But we check anyway as a paranoiac safety.
             */
            if (isIdentity()) {
                inverse = this;
            } else {
                inverse = MathTransforms.linear(Matrices.inverse(this));
                if (inverse instanceof AbstractLinearTransform) {
                    ((AbstractLinearTransform) inverse).inverse = this;
                }
            }
        }
        return inverse;
    }

    /**
     * Returns the parameter descriptors for this math transform.
     *
     * @return {@inheritDoc}
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Affine.getProvider(getSourceDimensions(), getTargetDimensions(), isAffine()).getParameters();
    }

    /**
     * Returns the matrix elements as a group of parameters values. The number of parameters depends on the
     * matrix size. Only matrix elements different from their default value will be included in this group.
     *
     * @return The parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return Affine.parameters(this);
    }

    /**
     * Unsupported operation, since this matrix is unmodifiable.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        throw new UnsupportedOperationException(isAffine()
                ? Errors.format(Errors.Keys.UnmodifiableAffineTransform)
                : Errors.format(Errors.Keys.UnmodifiableObject_1, AbstractLinearTransform.class));
    }

    /**
     * Transforms an array of relative distance vectors. Distance vectors are transformed without applying
     * the translation components. The default implementation is not very efficient, but it should not be
     * an issue since this method is not invoked often.
     *
     * @since 0.7
     */
    @Override
    public void deltaTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        int offFinal = 0;
        double[] dstFinal = null;
        final int srcDim, dstDim;
        int srcInc = srcDim = getSourceDimensions();
        int dstInc = dstDim = getTargetDimensions();
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, srcDim, dstOff, dstDim, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * srcDim;  srcInc = -srcInc;
                    dstOff += (numPts - 1) * dstDim;  dstInc = -dstInc;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*srcDim);
                    srcOff = 0;
                    break;
                }
                case BUFFER_TARGET: {
                    dstFinal = dstPts; dstPts = new double[numPts * dstInc];
                    offFinal = dstOff; dstOff = 0;
                    break;
                }
            }
        }
        final double[] buffer = new double[dstDim];
        while (--numPts >= 0) {
            for (int j=0; j<dstDim; j++) {
                double sum = 0;
                for (int i=0; i<srcDim; i++) {
                    final double e = getElement(j, i);
                    if (e != 0) {   // See the comment in ProjectiveTransform for the purpose of this test.
                        sum += srcPts[srcOff + i] * e;
                    }
                }
                buffer[j] = sum;
            }
            System.arraycopy(buffer, 0, dstPts, dstOff, dstDim);
            srcOff += srcInc;
            dstOff += dstInc;
        }
        if (dstFinal != null) {
            System.arraycopy(dstPts, 0, dstFinal, offFinal, dstPts.length);
        }
    }

    /**
     * Compares this math transform with an object which is known to be of the same class.
     * Implementors can safely cast the {@code object} argument to their subclass.
     *
     * @param  object The object to compare with this transform.
     * @return {@code true} if the given object is considered equals to this math transform.
     */
    protected abstract boolean equalsSameClass(final Object object);

    /**
     * Compares the specified object with this linear transform for equality.
     * This implementation returns {@code true} if the following conditions are meet:
     * <ul>
     *   <li>In {@code STRICT} mode, the objects are of the same class and {@link #equalsSameClass(Object)}
     *       returns {@code true}.</li>
     *   <li>In other modes, the matrix are equals or approximatively equals (depending on the mode).</li>
     * </ul>
     *
     * @param  object The object to compare with this transform.
     * @param  mode The strictness level of the comparison. Default to {@link ComparisonMode#STRICT STRICT}.
     * @return {@code true} if the given object is considered equals to this math transform.
     */
    @Override
    public final boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) { // Slight optimization
            return true;
        }
        if (object != null) {
            if (getClass() == object.getClass() && !mode.isApproximative()) {
                return equalsSameClass(object);
            }
            if (mode != ComparisonMode.STRICT) {
                if (object instanceof LinearTransform) {
                    return Matrices.equals(this, ((LinearTransform) object).getMatrix(), mode);
                } else if (object instanceof Matrix) {
                    return Matrices.equals(this, (Matrix) object, mode);
                }
            }
        }
        return false;
    }

    /**
     * Returns a string representation of the matrix.
     */
    @Override
    public String toString() {
        return Matrices.toString(this);
    }
}
