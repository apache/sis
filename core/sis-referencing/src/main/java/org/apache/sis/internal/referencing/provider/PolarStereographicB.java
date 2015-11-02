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
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;


/**
 * The provider for <cite>"Polar Stereographic (Variant B)"</cite> projection (EPSG:9829).
 * This provider includes a <cite>"Latitude of standard parallel"</cite> parameter and
 * determines the hemisphere of the projection from that parameter value.
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlTransient
public final class PolarStereographicB extends AbstractStereographic {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5188231050523249971L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9829";

    /**
     * The operation parameter descriptor for the <cite>Longitude of origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Latitude of standard parallel</cite> (φ₁) parameter value.
     * Valid values are -90° or 90°.
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL;

    /**
     * The operation parameter descriptor for the <cite>Scale factor</cite> (not necessarily at natural origin)
     * parameter value. Valid values range is (0 … ∞) and default value is 1.
     *
     * <p>This parameter is used by {@link PolarStereographicA} and is not formally a parameter of
     * {@code PolarStereographicB} projection. Nevertheless we declare it is as an optional parameter
     * because it is sometime used in Well Known Text (WKT). However it shall be interpreted as a
     * <cite>Scale factor at the standard parallel</cite> rather than at the natural origin.</p>
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        LONGITUDE_OF_ORIGIN = createLongitude(
                exceptEPSG(PolarStereographicA.LONGITUDE_OF_ORIGIN,
                builder.addIdentifier("8833").addName("Longitude of origin")));

        STANDARD_PARALLEL = createMandatoryLatitude(builder
                .addIdentifier("8832").addName("Latitude of standard parallel")
                .addName(sameNameAs(Citations.OGC,  Mercator2SP.STANDARD_PARALLEL))
                .addName(sameNameAs(Citations.ESRI, Mercator2SP.STANDARD_PARALLEL)));

        SCALE_FACTOR = createScale(builder
                .addNamesAndIdentifiers(Mercator2SP.SCALE_FACTOR)
                .setRemarks(notFormalParameter("Polar Stereographic (variant A)")).setDeprecated(true));

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER)
                .addName("Polar Stereographic (variant B)")
                .addName(Citations.S57,  "Polar stereographic")
                .addName(Citations.S57,  "PST")
                .addIdentifier(Citations.S57, "11")
                .createGroupForMapProjection(
                        STANDARD_PARALLEL,
                        LONGITUDE_OF_ORIGIN,
                        SCALE_FACTOR,       // Not formally a parameter of this projection.
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public PolarStereographicB() {
        super(PARAMETERS);
    }
}
