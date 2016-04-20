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
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider for <cite>"Coordinate Frame Rotation (geog3D domain)"</cite> (EPSG:1038).
 * This is the same transformation than "{@link PositionVector7Param}"
 * except that the rotation angles have the opposite sign.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class CoordinateFrameRotation3D extends GeocentricAffineBetweenGeographic {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5605297074740440504L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier("1038")
                .addName("Coordinate Frame Rotation (geog3D domain)")
                .createGroupWithSameParameters(PositionVector7Param3D.PARAMETERS);
        /*
         * NOTE: we omit the "Bursa-Wolf" alias because it is ambiguous, since it can apply
         * to both "Coordinate Frame Rotation" and "Position Vector 7-param. transformation"
         * We also omit "Coordinate Frame Rotation" alias for similar reason.
         */
    }

    /**
     * Constructs the provider.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public CoordinateFrameRotation3D() {
        this(3, 3, new GeocentricAffineBetweenGeographic[4]);
        redimensioned[0] = new CoordinateFrameRotation2D(      redimensioned);
        redimensioned[1] = new CoordinateFrameRotation3D(2, 3, redimensioned);
        redimensioned[2] = new CoordinateFrameRotation3D(3, 2, redimensioned);
        redimensioned[3] = this;
    }

    /**
     * Constructs a provider for the given dimensions.
     *
     * @param sourceDimensions  number of dimensions in the source CRS of this operation method.
     * @param targetDimensions  number of dimensions in the target CRS of this operation method.
     * @param redimensioned     providers for all combinations between 2D and 3D cases, or {@code null}.
     */
    private CoordinateFrameRotation3D(int sourceDimensions, int targetDimensions, GeodeticOperation[] redimensioned) {
        super(sourceDimensions, targetDimensions, PARAMETERS, redimensioned);
    }

    /**
     * Returns the type of this operation.
     */
    @Override
    int getType() {
        return FRAME_ROTATION;
    }
}
