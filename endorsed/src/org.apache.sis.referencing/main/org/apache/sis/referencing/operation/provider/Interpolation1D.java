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

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.internal.shared.Constants;


/**
 * The provider for interpolation of one-dimensional coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Interpolation1D extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2571645687075958970L;

    /**
     * The operation parameter descriptor for the <q>preimage</q> parameter value.
     * This parameter is optional and often omitted.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Image_%28mathematics%29#Inverse_image">Preimage on Wikipedia</a>
     * @see <a href="https://mathworld.wolfram.com/Preimage.html">Preimage on MathWord</a>
     */
    private static final ParameterDescriptor<double[]> PREIMAGE;

    /**
     * The operation parameter descriptor for the <q>values</q> parameter value.
     */
    private static final ParameterDescriptor<double[]> VALUES;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = new ParameterBuilder().setCodeSpace(Citations.SIS, Constants.SIS);
        PREIMAGE   = builder.addName("preimage").create(double[].class, null);         // Optional and often omitted.
        VALUES     = builder.addName("values")  .create(double[].class, null);         // Optional but usually provided.
        PARAMETERS = builder.addName("Interpolation 1D").createGroup(PREIMAGE, VALUES);
    }

    /**
     * Constructs a provider for the 1-dimensional case.
     */
    public Interpolation1D() {
        super(Conversion.class, PARAMETERS,
              CoordinateSystem.class, false,
              CoordinateSystem.class, false,
              (byte) 1);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     *
     * @param  context  the parameter values together with its context.
     * @return the created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws InvalidGeodeticParameterException if a preimage is specified and its sequence of values is not monotonic.
     */
    @Override
    public MathTransform createMathTransform(final Context context) throws InvalidGeodeticParameterException {
        final Parameters pv = Parameters.castOrWrap(context.getCompletedParameters());
        try {
            return MathTransforms.interpolate(pv.getValue(PREIMAGE), pv.getValue(VALUES));
        } catch (IllegalArgumentException e) {
            throw new InvalidGeodeticParameterException(e.getMessage(), e);
        }
    }
}
