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
import javax.measure.unit.NonSI;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Transformation;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;


/**
 * The provider for <cite>"Geographic2D offsets"</cite> (EPSG:9619).
 * The default implementation handles the 2D case.
 * The {@link GeographicOffsets3D} subclass adds a third dimension.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public class GeographicOffsets extends AbstractProvider {
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
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        TY = builder.addIdentifier("8601").addName("Latitude offset").create(0, NonSI.DEGREE_ANGLE);
        TX = builder.addIdentifier("8602").addName("Longitude offset").create(0, NonSI.DEGREE_ANGLE);
        PARAMETERS = builder.addIdentifier("9619").addName("Geographic2D offsets").createGroup(TY, TX);
    }

    /**
     * Constructs a provider with default parameters.
     */
    public GeographicOffsets() {
        super(2, 2, PARAMETERS);
    }

    /**
     * For subclass constructor only.
     */
    GeographicOffsets(ParameterDescriptorGroup parameters) {
        super(3, 3, parameters);
    }

    /**
     * Returns the operation type.
     *
     * @return Interface implemented by all coordinate operations that use this method.
     */
    @Override
    public final Class<Transformation> getOperationType() {
        return Transformation.class;
    }

    /**
     * Creates a transform from the specified group of parameter values.
     * The parameter values are unconditionally converted to degrees.
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
        return new AffineTransform2D(1, 0, 0, 1, pv.doubleValue(TX), pv.doubleValue(TY));
    }
}
