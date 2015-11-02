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
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The provider for <cite>"Lambert Conic Conformal (2SP Belgium)"</cite> projection (EPSG:9803).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/lambert_conic_conformal_2sp_belgium.html">Lambert Conic Conformal 2SP (Belgium) on RemoteSensing.org</a>
 */
@XmlTransient
public final class LambertConformalBelgium extends AbstractLambert {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6388030784088639876L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9803";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier(IDENTIFIER)
                .addName(                    "Lambert Conic Conformal (2SP Belgium)")
                .addName(Citations.OGC,      "Lambert_Conformal_Conic_2SP_Belgium")
                .addName(Citations.ESRI,     "Lambert_Conformal_Conic_2SP_Belgium")
                .addIdentifier(Citations.MAP_INFO, "19")
                .addIdentifier(Citations.S57,       "6")
                .createGroupForMapProjection(
                        LambertConformal2SP.LATITUDE_OF_FALSE_ORIGIN,
                        LambertConformal2SP.LONGITUDE_OF_FALSE_ORIGIN,
                        LambertConformal2SP.STANDARD_PARALLEL_1,
                        LambertConformal2SP.STANDARD_PARALLEL_2,
                        LambertConformal2SP.EASTING_AT_FALSE_ORIGIN,
                        LambertConformal2SP.NORTHING_AT_FALSE_ORIGIN);
    }

    /**
     * Constructs a new provider.
     */
    public LambertConformalBelgium() {
        super(PARAMETERS);
    }
}
