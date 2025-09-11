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
package org.apache.sis.referencing.operation.provider;

import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 * The provider for <q>Axis Order Reversal (2D)</q> (EPSG:9843).
 * This is a trivial operation that just swap the two first axes.
 * The inverse operation is this operation itself.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
public class AxisOrderReversal extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5657908757386024307L;

    /**
     * The group of all parameters expected by this coordinate operation (in this case, none).
     */
    private static final ParameterDescriptorGroup PARAMETERS = builder()
            .addIdentifier("9843").addName("Axis Order Reversal (2D)").createGroup();

    /**
     * The canonical instance of this operation method.
     *
     * @see #provider()
     */
    private static final AxisOrderReversal INSTANCE = new AxisOrderReversal();

    /**
     * Returns the canonical instance of this operation method.
     * This method is invoked by {@link java.util.ServiceLoader} using reflection.
     *
     * @return the canonical instance of this operation method.
     */
    public static AxisOrderReversal provider() {
        return INSTANCE;
    }

    /**
     * Constructs a provider with default parameters.
     *
     * @todo Delete this constructor after we stop class-path support.
     *       Implementation will be moved to {@link #INSTANCE}.
     */
    public AxisOrderReversal() {
        this(PARAMETERS, (byte) 2);
    }

    /**
     * For {@link AxisOrderReversal3D} subclass only.
     *
     * @param parameters  description of parameters expected by this operation.
     * @param dimension   the number of dimensions (2 or 3).
     */
    AxisOrderReversal(final ParameterDescriptorGroup parameters, final byte dimension) {
        super(Conversion.class, parameters,
              CoordinateSystem.class, false,
              CoordinateSystem.class, false,
              dimension);
    }

    /**
     * Returns the operation method which is the closest match for the given transform.
     * This is an adjustment based on the number of dimensions only, on the assumption
     * that the given transform has been created by this provider or a compatible one.
     */
    @Override
    public AbstractProvider variantFor(final MathTransform transform) {
        final int dimension = maxDimension(transform);
        if (dimension != minSourceDimension) {
            return (dimension >= 3) ? AxisOrderReversal3D.INSTANCE : INSTANCE;
        }
        return this;
    }

    /**
     * Returns the transform.
     *
     * @param  context  the parameter values together with its context.
     * @return the created affine transform.
     * @throws FactoryException if a transform cannot be created.
     */
    @Override
    public MathTransform createMathTransform(final Context context) throws FactoryException {
        final int sourceDimensions = context.getSourceDimensions().orElse(minSourceDimension);
        final int targetDimensions = context.getTargetDimensions().orElse(minSourceDimension);
        final MatrixSIS m = Matrices.createZero(targetDimensions + 1, sourceDimensions + 1);
        m.setElement(0, 1, 1);
        m.setElement(1, 0, 1);
        m.setElement(targetDimensions, sourceDimensions, 1);
        for (int i = Math.min(targetDimensions, sourceDimensions); --i >= 2;) {
            m.setElement(i, i, 1);
        }
        return context.getFactory().createAffineTransform(m);
    }

    /**
     * The inverse of this operation is itself.
     *
     * @return {@code this}.
     */
    @Override
    public AbstractProvider inverse() {
        return this;
    }
}
