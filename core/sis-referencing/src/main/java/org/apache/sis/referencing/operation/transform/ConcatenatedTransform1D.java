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

import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.geometry.DirectPosition1D;


/**
 * Concatenated transform in which the resulting transform is one-dimensional.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class ConcatenatedTransform1D extends ConcatenatedTransform implements MathTransform1D {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8150427971141078395L;

    /**
     * Constructs a concatenated transform.
     */
    ConcatenatedTransform1D(final MathTransform transform1,
                            final MathTransform transform2)
    {
        super(transform1, transform2);
    }

    /**
     * Checks if transforms are compatibles with this implementation.
     */
    @Override
    boolean isValid() {
        return super.isValid() && getSourceDimensions() == 1 && getTargetDimensions() == 1;
    }

    /**
     * Transforms the specified value.
     */
    @Override
    public double transform(final double value) throws TransformException {
        final double[] values = new double[] {value};
        final double[] buffer = new double[] {transform1.getTargetDimensions()};
        transform1.transform(values, 0, buffer, 0, 1);
        transform2.transform(buffer, 0, values, 0, 1);
        return values[0];
    }

    /**
     * Gets the derivative of this function at a value.
     */
    @Override
    public double derivative(final double value) throws TransformException {
        final DirectPosition1D p = new DirectPosition1D(value);
        final Matrix m = super.derivative(p);
        assert (m.getNumRow() == 1) && (m.getNumCol() == 1);
        return m.getElement(0,0);
    }

    /**
     * Creates the inverse transform of this object.
     */
    @Override
    public MathTransform1D inverse() throws NoninvertibleTransformException {
        return (MathTransform1D) super.inverse();
    }
}
