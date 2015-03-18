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

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of linear transforms. For efficiency reasons, this transform implements itself the matrix
 * to be returned by {@link #getMatrix()}.
 *
 * <p>Subclasses need to implement the following methods:</p>
 * <ul>
 *   <li>{@link #isAffine()}</li>
 *   <li>{@link #getElement(int, int)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
abstract class AbstractLinearTransform extends AbstractMathTransform
        implements LinearTransform, Matrix // Not Cloneable, despite the clone() method.
{
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
    public abstract boolean isAffine();

    /**
     * Returns a copy of the matrix that user can modify.
     */
    @Override
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
     * @return A copy of the parameter values for this math transform.
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
}
