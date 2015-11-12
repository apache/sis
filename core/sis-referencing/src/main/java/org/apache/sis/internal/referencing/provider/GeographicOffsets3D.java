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
import javax.measure.unit.SI;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The provider for <cite>"Geographic3D offsets"</cite> (EPSG:9660).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class GeographicOffsets3D extends GeographicOffsets {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1611236201346560796L;

    /**
     * The operation parameter descriptor for the <cite>"Vertical Offset"</cite> parameter value.
     */
    private static final ParameterDescriptor<Double> TZ;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        TZ = builder.addIdentifier("8603").addName("Vertical Offset").create(0, SI.METRE);
        PARAMETERS = builder.addIdentifier("9660").addName("Geographic3D offsets").createGroup(TY, TX, TZ);
    }

    /**
     * Constructs a provider with default parameters.
     */
    public GeographicOffsets3D() {
        super(PARAMETERS);
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
