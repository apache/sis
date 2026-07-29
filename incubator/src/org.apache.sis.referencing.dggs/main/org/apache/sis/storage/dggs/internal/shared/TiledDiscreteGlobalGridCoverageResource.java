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
package org.apache.sis.storage.dggs.internal.shared;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.measure.IncommensurableException;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.dggs.DiscreteGlobalGridResource;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class TiledDiscreteGlobalGridCoverageResource extends AbstractResource implements DiscreteGlobalGridResource{

    public TiledDiscreteGlobalGridCoverageResource() {
        super(null);
    }

    @Override
    public final NumberRange<Integer> getAvailableDepths() {
        final NumberRange<Integer> tileDepthRange = getTileAvailableDepths();
        final int minDepth = (int) (tileDepthRange.getMinDouble(true) + getTileRelativeDepth());
        final int maxDepth = (int) (tileDepthRange.getMaxDouble(true) + getTileRelativeDepth());
        return NumberRange.create(minDepth, true, maxDepth, true);
    }

    @Override
    public final int getDefaultDepth() {
        return (int) (getTileAvailableDepths().getMinDouble(true) + getTileRelativeDepth());
    }

    @Override
    public final int getMaxRelativeDepth() {
        return getTileRelativeDepth();
    }

    public abstract NumberRange<Integer> getTileAvailableDepths();

    public abstract int getTileRelativeDepth();

    @Override
    public CodedCoverage read(CodedGeometry query, int... range) throws DataStoreException {
        final DiscreteGlobalGridGeometry localGridGeometry = getGridGeometry();
        final DiscreteGlobalGridReferenceSystem localDggrs = localGridGeometry.getReferenceSystem();
        final NumberRange<Integer> availableDepths = getAvailableDepths();

        //convert the query to this DGGRS and depth
        DiscreteGlobalGridGeometry dggrsQuery = DiscreteGlobalGridResource.toDiscreteGlobalGridGeometry(query);
        try {
            dggrsQuery = dggrsQuery.transformTo(localDggrs, availableDepths, getTileRelativeDepth());
        } catch (TransformException | IncommensurableException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }

        //at this point the query contains a list of data zones we have
        //convert it to tile zones
        final Object[] parentZoneIds;
        if (dggrsQuery.getRelativeDepth() != null && dggrsQuery.getRelativeDepth() == getTileRelativeDepth()) {
            //we can reuse the query parent zones ids directly
            parentZoneIds = dggrsQuery.getBaseZoneIds();
        } else {
            //we must loop on all data zones to find all the parents we need
            final DiscreteGlobalGridHierarchy dggh = localDggrs.getGridSystem().getHierarchy();
            final HashSet<Object> pzoneIds = new HashSet<>();
            final List<Object> zoneIds = dggrsQuery.getZoneIds();
            final int parentLevel = dggh.getZone(zoneIds.get(0)).getLocationType().getRefinementLevel() - getTileRelativeDepth();
            for (Object zid : zoneIds) {
                pzoneIds.add(dggh.getZone(zid).getFirstParent(parentLevel).getIdentifier());
            }
            parentZoneIds = pzoneIds.toArray();
        }

        final List<CodedCoverage> tiles = new ArrayList<>();
        for (Object pid : parentZoneIds) {
            CodedCoverage coverage = getZoneTile(pid);
            if (coverage != null) tiles.add(coverage);
        }

        try {
            return new TiledDiscreteGlobalGridCoverage(tiles.toArray(CodedCoverage[]::new));
        } catch (TransformException ex) {
            throw new DataStoreException(ex);
        }
    }

    /**
     * Get a tile.
     *
     * @param identifierOrZone must be a valid tile zone identifieri n tile level range
     * @return tile or null if the tile do not exist
     * @throws DataStoreException
     */
    public abstract CodedCoverage getZoneTile(Object identifierOrZone) throws DataStoreException;

}
