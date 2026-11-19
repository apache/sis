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
package org.apache.sis.referencing.dggs.smc;

import java.util.Collection;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystemFactory;


/**
 * Creates SMC reference systems on the global lattice.
 * Other lattices are created with the {@link SmcDggrs} constructors.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SmcFactory implements DiscreteGlobalGridReferenceSystemFactory {

    private static final String ZONAL_REFERENCE = "default";

    /**
     * Creates a factory. Invoked by {@link java.util.ServiceLoader}.
     */
    public SmcFactory() {
    }

    @Override
    public Collection<String> listDggh() {
        return List.of(SmcDggrs.IDENTIFIER);
    }

    @Override
    public Collection<String> listZonalRefId(final String dggh) {
        return List.of(ZONAL_REFERENCE);
    }

    /**
     * Creates the SMC reference system on the global lattice.
     *
     * @param  dgghId      must be {@code "SMC"}.
     * @param  zonalRefId  {@code "default"} or {@code null}.
     * @param  base        geographic CRS, or {@code null} for CRS:84.
     * @throws FactoryException if an argument is not supported.
     */
    @Override
    public DiscreteGlobalGridReferenceSystem createDggrs(final String dgghId, final String zonalRefId,
            final CoordinateReferenceSystem base) throws FactoryException
    {
        if (!SmcDggrs.IDENTIFIER.equals(dgghId)) {
            throw new NoSuchIdentifierException("Unknown identifier " + dgghId, dgghId);
        }
        if (zonalRefId != null && !ZONAL_REFERENCE.equals(zonalRefId)) {
            throw new NoSuchIdentifierException("Unknown zonal reference " + zonalRefId, zonalRefId);
        }
        if (base == null || base instanceof GeographicCRS) {
            return new SmcDggrs((GeographicCRS) base);
        }
        throw new FactoryException("SMC grids require a geographic base CRS.");
    }
}
