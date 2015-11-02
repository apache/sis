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
import org.apache.sis.util.resources.Messages;


/**
 * The provider for <cite>"Lambert Conic Conformal (West Orientated)"</cite> projection (EPSG:9826).
 * In this projection method, the <var>x</var> values increase toward West. However the projection
 * is defined in such a way that the sign of <var>x</var> values are reversed before to apply the
 * <cite>"false easting"</cite> translation. As a consequence of this operation order, despite its
 * name the <cite>"false easting"</cite> is effectively a <cite>"false westing"</cite> (FW) parameter.
 * See §1.3.1.3 in <i>Geomatics Guidance Note number 7, part 2 – April 2015</i>.
 *
 * <p>In Apache SIS implementation, this operation method does <strong>not</strong> reverse the sign of
 * <var>x</var> values because all our map projection "kernels" go from (longitude, latitude) in degrees
 * to (easting, northing) in metres by definition. The sign reversal is applied later, by examination of
 * axis directions in {@link org.apache.sis.referencing.cs.CoordinateSystems}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlTransient
public final class LambertConformalWest extends AbstractLambert {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6226753337274190088L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9826";

    /**
     * The operation parameter descriptor for the <cite>False easting</cite> (FE) parameter value.
     * In the case of West Orientated projection, despite its EPSG name this parameter is actually
     * <cite>False westing</cite> (FW)
     */
    static final ParameterDescriptor<Double> FALSE_WESTING;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        FALSE_WESTING = createShift(builder
                .addNamesAndIdentifiers(LambertConformal1SP.FALSE_EASTING)
                .setRemarks(Messages.formatInternational(Messages.Keys.MisnamedParameter_1, "False westing")));

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER)
                .addName("Lambert Conic Conformal (West Orientated)")
                .createGroupForMapProjection(
                        LambertConformal1SP.LATITUDE_OF_ORIGIN,
                        LambertConformal1SP.LONGITUDE_OF_ORIGIN,
                        LambertConformal1SP.SCALE_FACTOR,
                                            FALSE_WESTING,
                        LambertConformal1SP.FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public LambertConformalWest() {
        super(PARAMETERS);
    }
}
