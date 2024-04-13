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
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The provider for <q>Geocentric translations (geog2D domain)</q> (EPSG:9603).
 * This is a special case of {@link PositionVector7Param2D} where only the translation
 * terms can be set to a non-null value.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@XmlTransient
public final class GeocentricTranslation2D extends GeocentricAffineBetweenGeographic {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7160250630666911608L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier("9603")
                .addName("Geocentric translations (geog2D domain)")
                .addName(Citations.ESRI, "Geocentric_Translation")
                .createGroup(SRC_SEMI_MAJOR,
                             SRC_SEMI_MINOR,
                             TGT_SEMI_MAJOR,
                             TGT_SEMI_MINOR,
                             TX, TY, TZ);
    }

    /**
     * The canonical instance of this operation method.
     *
     * @see #provider()
     */
    private static final GeocentricTranslation2D INSTANCE = new GeocentricTranslation2D();

    /**
     * Returns the canonical instance of this operation method.
     * This method is invoked by {@link java.util.ServiceLoader} using reflection.
     *
     * @return the canonical instance of this operation method.
     */
    public static GeocentricTranslation2D provider() {
        return INSTANCE;
    }

    /**
     * Creates a new provider.
     *
     * @todo Make this constructor private after we stop class-path support.
     */
    public GeocentricTranslation2D() {
        super(Type.TRANSLATION, PARAMETERS, (byte) 2);
    }

    /**
     * Returns the operation method which is the closest match for the given transform.
     * This is an adjustment based on the number of dimensions only, on the assumption
     * that the given transform has been created by this provider or a compatible one.
     */
    @Override
    public AbstractProvider variantFor(final MathTransform transform) {
        return maxDimension(transform) >= 3 ? GeocentricTranslation3D.provider() : this;
    }
}
