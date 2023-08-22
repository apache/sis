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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.internal.Constants;


/**
 * Base class of operations working on the number of dimensions of a geographic CRS.
 * The default implementation does nothing; this is used as a placeholder for the result of a call
 * to {@link Geographic3Dto2D#redimension(int, int)} when the given number of dimensions are equal.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.8
 */
@XmlTransient
class GeographicRedimension extends GeodeticOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3021902514274756742L;

    /**
     * Returns the provider for the specified combination of source and target dimensions.
     */
    @Override
    final GeodeticOperation redimensioned(int indexOfDim) {
        return Geographic3Dto2D.REDIMENSIONED[indexOfDim];
    }

    /**
     * Creates a copy of this provider.
     *
     * @deprecated This is a temporary constructor before replacement by a {@code provider()} method with JDK9.
     */
    @Deprecated
    GeographicRedimension(final GeographicRedimension copy) {
        super(copy);
    }

    /**
     * Constructs a math transform provider from a set of parameters.
     * This is for sub-class constructors only.
     */
    GeographicRedimension(final ParameterDescriptorGroup parameters, final int indexOfDim) {
        super(Conversion.class, parameters, indexOfDim,
              CoordinateSystem.class, false,
              CoordinateSystem.class, false);
    }

    /**
     * Creates an identity operation of the given number of dimensions.
     */
    GeographicRedimension(final int indexOfDim, final String name) {
        this(builder().setCodeSpace(Citations.SIS, Constants.SIS).addName(name).createGroup(), indexOfDim);
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
