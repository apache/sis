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
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The provider for <cite>"Miller Cylindrical"</cite> projection.
 * This is a {@link Mercator1SP} projection with the following modifications:
 *
 * <ol>
 *   <li>The latitude of parallels are scaled by a factor of 0.8 before the projection (actually 2×0.4 where
 *       the factor 2 is required for canceling the scaling performed by the classical Mercator formula).</li>
 *   <li>The northing is multiplied by 1.25 after the projection.</li>
 * </ol>
 *
 * Note that the Miller projection is typically used with spherical formulas. However the Apache SIS implementation
 * supports also the ellipsoidal formulas. If spherical formulas are desired, then the parameters shall contains
 * semi-major and semi-minor axis lengths of equal length.
 *
 * <div class="section">Additional identifiers:</div>
 * This projection has the following identifiers from the French mapping agency (IGNF),
 * which have not yet been declared in this class:
 *
 * <ul>
 *   <li>Name {@code "Miller_Cylindrical_Sphere"}</li>
 *   <li>Identifier {@code "PRC9901"}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/miller_cylindrical.html">Miller Cylindrical on RemoteSensing.org</a>
 */
@XmlTransient
public final class MillerCylindrical extends AbstractMercator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7682370461334391883L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder().setCodeSpace(Citations.OGC, Constants.OGC)
                .addName      ("Miller_Cylindrical")
                .addName      (Citations.GEOTIFF,  "CT_MillerCylindrical")
                .addIdentifier(Citations.GEOTIFF,  "20")
                .addName      (Citations.PROJ4,    "mill")
                .addIdentifier(Citations.MAP_INFO, "11")
                .createGroupForMapProjection(toArray(MercatorSpherical.PARAMETERS.descriptors()));
    }

    /**
     * Constructs a new provider.
     */
    public MillerCylindrical() {
        super(PARAMETERS);
    }
}
