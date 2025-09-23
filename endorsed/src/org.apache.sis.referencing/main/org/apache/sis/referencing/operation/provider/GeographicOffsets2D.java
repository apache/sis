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
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.parameter.Parameters;


/**
 * The provider for <q>Geographic2D offsets</q> (EPSG:9619).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
public final class GeographicOffsets2D extends GeographicOffsets {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1611236201346560796L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder().addIdentifier("9619").addName("Geographic2D offsets").createGroup(TY, TX);
    }

    /**
     * The canonical instance of this operation method.
     *
     * @see #provider()
     */
    private static final GeographicOffsets INSTANCE = new GeographicOffsets();

    /**
     * Returns the canonical instance of this operation method.
     * This method is invoked by {@link java.util.ServiceLoader} using reflection.
     *
     * @return the canonical instance of this operation method.
     */
    public static GeographicOffsets provider() {
        return INSTANCE;
    }

    /**
     * Creates a new provider.
     *
     * @todo Make this constructor private after we stop class-path support.
     */
    public GeographicOffsets2D() {
        super(PARAMETERS, (byte) 2);
    }

    /**
     * Returns the operation method which is the closest match for the given transform.
     * This is an adjustment based on the number of dimensions only, on the assumption
     * that the given transform has been created by this provider or a compatible one.
     */
    @Override
    public AbstractProvider variantFor(final MathTransform transform) {
        return maxDimension(transform) >= 3 ? GeographicOffsets.provider() : this;
    }

    /**
     * Creates a transform from the specified group of parameter values.
     * The parameter values are unconditionally converted to degrees.
     *
     * @param  context  the parameter values together with its context.
     * @return the created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    @Override
    public MathTransform createMathTransform(final Context context) {
        final Parameters pv = Parameters.castOrWrap(context.getCompletedParameters());
        return new AffineTransform2D(1, 0, 0, 1, pv.doubleValue(TX), pv.doubleValue(TY));
    }
}
