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
package org.apache.sis.storage.dggs;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.dggs.DiscreteGlobalGrid;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.dggs.internal.shared.GridAsDiscreteGlobalGridResource;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;


/**
 * A Resource which offer acces to a coverage structured in DGGS cells.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface DiscreteGlobalGridResource extends CodedResource {

    /**
     * @return DiscreteGlobalGridGeometry, never null
     */
    @Override
    DiscreteGlobalGridGeometry getGridGeometry();

    /**
     * @return available dggrs refinement levels available in the resource
     */
    NumberRange<Integer> getAvailableDepths();

    /**
     * @return default depth of the data
     */
    int getDefaultDepth();

    /**
     * @todo : should not be here, on a query ?
     * @return maximum sub zone relative depth possible to query.
     */
    int getMaxRelativeDepth();

    /**
     * {@inheritDoc }
     */
    @Override
    public default CodedCoverage read(GridGeometry domain, int... range) throws DataStoreException {

        final Quantity<?> coverageResolution;
        try {
            coverageResolution = GridAsDiscreteGlobalGridResource.computeAverageResolution(domain);
        } catch (TransformException ex) {
            throw new DataStoreException(ex);
        }

        //extract zones in the wanted area
        final DiscreteGlobalGridReferenceSystem dggrs = getGridGeometry().getReferenceSystem();
        final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();
        final Stream<Zone> zones;
        try {
            coder.setPrecision(coverageResolution, null);
            final DiscreteGlobalGrid grid = dggrs.getGridSystem().getHierarchy().getGrids().get(coder.getPrecisionLevel());
            zones = grid.getZones(domain.getEnvelope(dggrs.getGridSystem().getCrs()));
        } catch (IncommensurableException | TransformException ex) {
            throw new DataStoreException(ex);
        }

        //todo check intersection with additional dimensions

        final List<Object> zoneIds = zones.map(Zone::getIdentifier).toList();
        final DiscreteGlobalGridGeometry geometry = DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds, null);
        return read(geometry, range);
    }

    /**
     * Retrieve a set of DGGRS zone data.
     *
     * @param geometry zones to read
     * @param range bands to select
     * @return DiscreteGlobalGridCoverage, never null
     * @throws DataStoreException
     */
    @Override
    public CodedCoverage read(CodedGeometry geometry, int ... range) throws DataStoreException;

    public static DiscreteGlobalGridGeometry toDiscreteGlobalGridGeometry(CodedGeometry geom) {
        if (geom instanceof DiscreteGlobalGridGeometry dgg) {
            return dgg;
        }

        final ReferenceSystem rs = geom.getReferenceSystem();
        for (ReferenceSystem r : ReferenceSystems.getSingleComponents(rs, false)) {
            if (r instanceof DiscreteGlobalGridReferenceSystem dggrs) {
                Optional<CodedGeometry> opt = geom.slice(r);
                if (opt.isPresent()) {
                    return (DiscreteGlobalGridGeometry) opt.get();
                }
            }
        }

        throw new UnsupportedOperationException("Not available yet");
    }

}
