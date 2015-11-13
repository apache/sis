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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The provider for <cite>"Vertical Offset"</cite> (EPSG:9616).
 * The Apache SIS implementation of this operation method always perform the vertical offset in metres.
 * The vertical axis of source and target CRS shall be converted to metres before this operation is applied.
 *
 * <p><b>IMPORTANT:</b> if the source and target axis directions are opposite, then the input coordinates
 * need to be multiplied by -1 <strong>before</strong> the operation is applied. This order is required
 * for consistency with the sign of <cite>"Vertical Offset"</cite> parameter value.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class VerticalOffset extends GeographicOffsets {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8309224700931038020L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder().addIdentifier("9616").addName("Vertical Offset").createGroup(TZ);
    }

    /**
     * Constructs a provider with default parameters.
     */
    public VerticalOffset() {
        super(1, PARAMETERS);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     * The parameter value is unconditionally converted to metres.
     *
     * @param  factory Ignored (can be null).
     * @param  values The group of parameter values.
     * @return The created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws ParameterNotFoundException
    {
        final Parameters pv = Parameters.castOrWrap(values);
        final Matrix2 t = new Matrix2();
        t.m01 = pv.doubleValue(TZ);
        return MathTransforms.linear(t);
    }
}
