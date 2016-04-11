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
 * The provider for <cite>"Position Vector transformation (geog2D domain)"</cite> (EPSG:9606).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class PositionVector7Param2D extends GeocentricAffineBetweenGeographic {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6398226638364450229L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier("9606")
                .addName("Position Vector transformation (geog2D domain)")
                .createGroup(SRC_SEMI_MAJOR,
                             SRC_SEMI_MINOR,
                             TGT_SEMI_MAJOR,
                             TGT_SEMI_MINOR,
                             TX, TY, TZ, RX, RY, RZ, DS);
        /*
         * NOTE: we omit the "Bursa-Wolf" alias because it is ambiguous, since it can apply
         * to both "Coordinate Frame Rotation" and "Position Vector 7-param. transformation"
         * We also omit "Position Vector 7-param. transformation" alias for similar reason.
         */
    }

    /**
     * Constructs the provider.
     */
    public PositionVector7Param2D() {
        this(null);
    }

    /**
     * Constructs a provider that can be resized.
     */
    PositionVector7Param2D(GeodeticOperation[] redimensioned) {
        super(2, 2, PARAMETERS, redimensioned);
    }

    /**
     * Returns the three-dimensional variant of this operation method.
     */
    @Override
    Class<PositionVector7Param3D> variant3D() {
        return PositionVector7Param3D.class;
    }

    /**
     * Returns the type of this operation.
     */
    @Override
    int getType() {
        return SEVEN_PARAM;
    }
}
