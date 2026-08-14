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
package org.apache.sis.referencing.dggs;

import java.util.Collection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * Factory to create DGGRS instances.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface DiscreteGlobalGridReferenceSystemFactory {

    /**
     * List supported DGGH names.
     * Example : Healpix, H3, IVEA3H, ...
     * @return never null
     */
    Collection<String> listDggh();

    /**
     * List the possible zonal referencing identifier for a DGGH.
     * The first entry is the default, which should be the most common case.
     *
     * @param dggh Dggh name, not null
     * @return never null, must contain at least one item
     */
    Collection<String> listZonalRefId(String dggh);

    /**
     * Create a DGGRS.
     *
     * @param dgghId not null
     * @param zonalRefId can be null, in which case the default one from listZonalRefId will be used
     * @param base the base CRS attached, null for default (should be GeographicCRS for earth or InertialCRS for star fixed)
     * @throws FactoryException if creation failed
     */
    DiscreteGlobalGridReferenceSystem createDggrs(String dgghId, String zonalRefId, CoordinateReferenceSystem base) throws FactoryException;

}
