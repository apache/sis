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
package org.apache.sis.internal.referencing.provider;

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.Constants;


/**
 * Base class of operations working on the number of dimensions of a geographic CRS.
 * The default implementation does nothing; this is used as a placeholder for the result of a call
 * to {@link Geographic3Dto2D#redimension(int, int)} when the given number of dimensions are equal.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@XmlTransient
class GeographicRedimension extends GeodeticOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3021902514274756742L;

    /**
     * Constructs a math transform provider from a set of parameters.
     * This is for sub-class constructors only.
     */
    GeographicRedimension(final int sourceDimensions,
                          final int targetDimensions,
                          final ParameterDescriptorGroup parameters,
                          final GeodeticOperation[] redimensioned)
    {
        super(sourceDimensions, targetDimensions, parameters, redimensioned);
    }

    /**
     * Creates an identity operation of the given number of dimensions.
     */
    GeographicRedimension(final int dimension, final GeodeticOperation[] redimensioned) {
        super(dimension, dimension, builder().setCodeSpace(Citations.SIS, Constants.SIS)
                .addName("Identity " + dimension + 'D').createGroup(), redimensioned);
    }

    /**
     * Returns the interface implemented by all coordinate operations that extends this class.
     *
     * @return default to {@link Conversion}.
     */
    @Override
    public final Class<Conversion> getOperationType() {
        return Conversion.class;
    }

    /**
     * Returns the transform.
     *
     * @param  factory  the factory for creating affine transforms.
     * @param  values   the parameter values.
     * @return the math transform for the given parameter values.
     * @throws FactoryException if an error occurred while creating the transform.
     */
    @Override
    public MathTransform createMathTransform(MathTransformFactory factory, ParameterValueGroup values)
            throws FactoryException
    {
        return factory.createAffineTransform(Matrices.createDiagonal(getTargetDimensions() + 1, getSourceDimensions() + 1));
    }
}
