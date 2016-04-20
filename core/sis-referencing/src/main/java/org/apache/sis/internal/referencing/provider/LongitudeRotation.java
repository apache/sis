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
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.parameter.Parameters;


/**
 * The provider for <cite>"Longitude rotation"</cite> (EPSG:9601).
 * The "Longitude rotation" is created as an affine transform containing only a translation term in degrees.
 * Advantage of using an affine transform for such simple operation is that this {@code AffineTransform} can
 * be efficiently concatenated with other affine transform instances.
 *
 * <blockquote><p><b>Operation name:</b> {@code Longitude rotation}</p>
 * <table class="sis">
 *   <caption>Operation parameters</caption>
 *   <tr><th>Parameter name</th> <th>Default value</th></tr>
 *   <tr><td>{@code Longitude offset}</td> <td></td></tr>
 * </table></blockquote>
 *
 * The Apache SIS implementation of this operation method always perform the longitude rotation in degrees.
 * The longitude axis of source and target CRS shall be converted to degrees before this operation is applied.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@XmlTransient
public final class LongitudeRotation extends GeographicOffsets {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2104496465933824935L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder().addIdentifier("9601").addName("Longitude rotation").createGroup(TX);
    }

    /**
     * Constructs a provider with default parameters.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public LongitudeRotation() {
        this(2, 2, new LongitudeRotation[4]);
        redimensioned[0] = this;
        redimensioned[1] = new LongitudeRotation(2, 3, redimensioned);
        redimensioned[2] = new LongitudeRotation(3, 2, redimensioned);
        redimensioned[3] = new LongitudeRotation(3, 3, redimensioned);
    }

    /**
     * Constructs a provider for the given dimensions.
     *
     * @param sourceDimensions  number of dimensions in the source CRS of this operation method.
     * @param targetDimensions  number of dimensions in the target CRS of this operation method.
     * @param redimensioned     providers for all combinations between 2D and 3D cases.
     */
    private LongitudeRotation(int sourceDimensions, int targetDimensions, GeodeticOperation[] redimensioned) {
        super(sourceDimensions, targetDimensions, PARAMETERS, redimensioned);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     * The parameter value is unconditionally converted to degrees.
     *
     * <p>The operation is created as an affine transform between two two-dimensional CRS. We do not override the
     * {@link AffineTransform2D#getParameterDescriptors()} and {@link AffineTransform2D#getParameterValues()} methods
     * in order to make that fact clearer, in the hope to reduce ambiguity about the nature of the transform.
     * Note also that the "Longitude rotation" operation has unit of measurement while the "Affine" operation
     * does not, so maybe our unconditional conversion to degrees would be more surprising for the user if the
     * operation was shown as a "Longitude rotation".</p>
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
        return new AffineTransform2D(1, 0, 0, 1, pv.doubleValue(TX), 0);
    }
}
