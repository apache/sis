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

import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * Base class for all transformations that perform a translation in the geographic domain.
 * This base class defines a provider for <cite>"Geographic3D offsets"</cite> (EPSG:9660),
 * but subclasses will provide different operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public class GeographicOffsets extends GeodeticOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6246011184175753328L;

    /**
     * The operation parameter descriptor for the <cite>"Longitude offset"</cite> parameter value.
     */
    static final ParameterDescriptor<Double> TX;

    /**
     * The operation parameter descriptor for the <cite>"Latitude offset"</cite> parameter value.
     */
    static final ParameterDescriptor<Double> TY;

    /**
     * The operation parameter descriptor for the <cite>"Vertical Offset"</cite> parameter value.
     */
    static final ParameterDescriptor<Double> TZ;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        TY = builder.addIdentifier("8601").addName("Latitude offset") .create(0, NonSI.DEGREE_ANGLE);
        TX = builder.addIdentifier("8602").addName("Longitude offset").create(0, NonSI.DEGREE_ANGLE);
        TZ = builder.addIdentifier("8603").addName("Vertical Offset") .create(0, SI.METRE);
        PARAMETERS = builder.addIdentifier("9660").addName("Geographic3D offsets").createGroup(TY, TX, TZ);
    }

    /**
     * Constructs a provider with default parameters.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public GeographicOffsets() {
        super(3, 3, PARAMETERS, new GeographicOffsets[4]);
        redimensioned[0] = new GeographicOffsets2D(redimensioned);
        redimensioned[1] = new GeographicOffsets(2, 3, PARAMETERS, redimensioned);
        redimensioned[2] = new GeographicOffsets(3, 2, PARAMETERS, redimensioned);
        redimensioned[3] = this;
    }

    /**
     * For subclasses constructor only.
     */
    GeographicOffsets(int sourceDimensions, int targetDimensions,
            ParameterDescriptorGroup parameters, GeodeticOperation[] redimensioned)
    {
        super(sourceDimensions, targetDimensions, parameters, redimensioned);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     * The parameter values are unconditionally converted to degrees and metres.
     *
     * @param  factory Ignored (can be null).
     * @param  values The group of parameter values.
     * @return The created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    @Override
    public MathTransform createMathTransform(MathTransformFactory factory, ParameterValueGroup values)
            throws ParameterNotFoundException
    {
        final Parameters pv = Parameters.castOrWrap(values);
        final Matrix4 t = new Matrix4();
        t.m03 = pv.doubleValue(TX);
        t.m13 = pv.doubleValue(TY);
        t.m23 = pv.doubleValue(TZ);
        return MathTransforms.linear(t);
    }
}
