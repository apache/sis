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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.measure.Units;


/**
 * The provider for <cite>"Geographic2D with Height Offsets"</cite> (EPSG:9618).
 * This is not the same than a <cite>"Geographic3D offsets"</cite> because this
 * operation also performs a simplified transformation from ellipsoidal height
 * to geoidal height, as can been seen from the difference in parameter name.
 * For a "Geographic3D offsets" with ellipsoidal heights, see the parent class.
 *
 * <p>Examples of coordinate transformations using this method:</p>
 * <ul>
 *   <li>EPSG:1335  from 2D to 2D geographic CRS.</li>
 *   <li>EPSG:1336  from 3D to 2D geographic CRS.</li>
 *   <li>EPSG:15596 from 3D to 3D geographic CRS.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@XmlTransient
public final class GeographicAndVerticalOffsets extends GeographicOffsets {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7822664525013018023L;

    /**
     * The operation parameter descriptor for the <cite>"Geoid undulation"</cite> parameter value.
     *
     * @see #TZ
     */
    static final ParameterDescriptor<Double> TH;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        TH = builder.addIdentifier("8604").addName("Geoid undulation").create(0, Units.METRE);
        PARAMETERS = builder().addIdentifier("9618").addName("Geographic2D with Height Offsets").createGroup(TY, TX, TH);
    }

    /**
     * Constructs a provider with default parameters.
     */
    public GeographicAndVerticalOffsets() {
        this(3, 3, new GeographicAndVerticalOffsets[4]);
        redimensioned[0] = new GeographicAndVerticalOffsets(2, 2, redimensioned);
        redimensioned[1] = new GeographicAndVerticalOffsets(2, 3, redimensioned);
        redimensioned[2] = new GeographicAndVerticalOffsets(3, 2, redimensioned);
        redimensioned[3] = this;
    }

    /**
     * For default constructor only.
     */
    private GeographicAndVerticalOffsets(int sourceDimensions, int targetDimensions, GeodeticOperation[] redimensioned) {
        super(sourceDimensions, targetDimensions, PARAMETERS, redimensioned);
    }

    /**
     * Returns the parameter descriptor for the vertical axis.
     */
    @Override
    ParameterDescriptor<Double> vertical() {
        return TH;
    }
}
