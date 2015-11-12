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
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.util.resources.Messages;


/**
 * The provider for <cite>"Transverse Mercator (South Orientated)"</cite> projection (EPSG:9808).
 * The terms <cite>false easting</cite> (FE) and <cite>false northing</cite> (FN) increase
 * the Westing and Southing value at the natural origin. In other words they are effectively
 * <cite>false westing</cite> (FW) and <cite>false southing</cite> (FS) respectively.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/transverse_mercator_south_oriented.html">Transverse Mercator (South Oriented) on RemoteSensing.org</a>
 */
@XmlTransient
public final class TransverseMercatorSouth extends AbstractMercator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5938929136350638347L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9808";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        final ParameterDescriptor<Double> falseSouthing = createShift(builder
                .addNamesAndIdentifiers(FALSE_NORTHING)
                .setRemarks(Messages.formatInternational(Messages.Keys.MisnamedParameter_1, "False southing")));

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER)
                .addName(                    "Transverse Mercator (South Orientated)")
                .addName(Citations.OGC,      "Transverse_Mercator_South_Orientated")
                .addName(Citations.GEOTIFF,  "CT_TransvMercator_SouthOriented")
                .addIdentifier(Citations.GEOTIFF,  "27")
                .createGroupForMapProjection(
                        TransverseMercator.LATITUDE_OF_ORIGIN,
                        TransverseMercator.LONGITUDE_OF_ORIGIN,
                        TransverseMercator.SCALE_FACTOR,
                        LambertConformalWest.FALSE_WESTING,
                        falseSouthing);
    }

    /**
     * Constructs a new provider.
     */
    public TransverseMercatorSouth() {
        super(PARAMETERS);
    }

    /**
     * {@inheritDoc}
     *
     * @return The map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new org.apache.sis.referencing.operation.projection.TransverseMercator(this, parameters);
    }
}
