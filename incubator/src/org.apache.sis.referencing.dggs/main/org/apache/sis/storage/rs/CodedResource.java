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
package org.apache.sis.storage.rs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.referencing.dggs.DiscreteGlobalGrid;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.coverage.BandedCoverageResource;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.dggs.internal.shared.GridAsDiscreteGlobalGridResource;
import org.apache.sis.storage.rs.internal.shared.CodeTransforms;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A Resource which offer acces to a coverage structured in located cells.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface CodedResource extends BandedCoverageResource {

    /**
     * Get default resource geometry.
     * @return resource geometry
     */
    CodedGeometry getGridGeometry() throws DataStoreException;

    /**
     * List alternative geometry available.
     * First entry is the default ReferencedGridGeometry from getGridGeometry().
     *
     * @return list of alternative geometry available.
     */
    default List<CodedGeometry> getAlternateGridGeometry() throws DataStoreException{
        return List.of(getGridGeometry());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public default CodedCoverage read(GridGeometry domain, int... range) throws DataStoreException {
        final CodedGeometry resourceGeometry = getGridGeometry();


        //cut domain in single crs grid geometries
        final CoordinateReferenceSystem queryCrs = domain.getCoordinateReferenceSystem();
        final List<SingleCRS> queryCrss = CRS.getSingleComponents(queryCrs);
        final List<CodedGeometry> domainSlices = new ArrayList<>();
        for (SingleCRS scrs : queryCrss) {
            GridGeometry slice;
            try {
                slice = CodeTransforms.slice(domain, scrs);
            } catch (FactoryException ex) {
                throw new DataStoreException(ex);
            }
            CodedGeometry d = new CodedGeometry(slice);
            domainSlices.add(d);
        }



        //ensure we extract a single slice on axes where no range has been defined
        final List<CodedGeometry> querySlices = new ArrayList<>();
        final List<ReferenceSystem> singleComponents = ReferenceSystems.getSingleComponents(resourceGeometry.getReferenceSystem(), true);
        loop:
        for (ReferenceSystem rs : singleComponents) {
            if (rs instanceof DiscreteGlobalGridReferenceSystem dggrs) {
                try {
                    final DiscreteGlobalGridGeometry dgggeom = toDiscreteGlobalGridGeometry(domain, dggrs);
                    querySlices.add(dgggeom);
                } catch (TransformException | IncommensurableException ex) {
                    throw new DataStoreException(ex);
                }
            } else {
                for (CodedGeometry slicegeom : domainSlices) {
                    if (slicegeom.getReferenceSystem().equals(rs)) {
                        continue loop;
                    }
                }
                //not found
                final Optional<CodedGeometry> slice = resourceGeometry.slice(rs);
                if (!slice.isPresent()) continue;
                final CodedGeometry slicegeom = slice.get();
                final Optional<GridGeometry> regularGrid = slicegeom.isRegularGrid();
                if (!regularGrid.isPresent()) continue;
                double ratio = 1.0;
                if (rs instanceof VerticalCRS) {
                    ratio = 0.0; //prefer the lowest level
                }
                GridGeometry build = regularGrid.get().derive().sliceByRatio(ratio).build();
                querySlices.add(new CodedGeometry(build));
            }
        }


        //place RS in order : horiontal > vertical > others
        querySlices.sort(new Comparator<CodedGeometry>() {
            @Override
            public int compare(CodedGeometry o1, CodedGeometry o2) {
                final ReferenceSystem rs1 = o1.getReferenceSystem();
                final ReferenceSystem rs2 = o2.getReferenceSystem();
                if (rs1 instanceof DiscreteGlobalGridReferenceSystem) return -1;
                if (rs2 instanceof DiscreteGlobalGridReferenceSystem) return +1;

                final CoordinateReferenceSystem crs1 = (CoordinateReferenceSystem) rs1;
                final CoordinateReferenceSystem crs2 = (CoordinateReferenceSystem) rs2;
                if (CRS.isHorizontalCRS(crs1)) return -1;
                if (CRS.isHorizontalCRS(crs2)) return +1;
                if (crs1 instanceof VerticalCRS) return -1;
                if (crs2 instanceof VerticalCRS) return +1;
                return 0;
            }
        });
        final CodedGeometry query = CodedGeometry.compound(querySlices.toArray(CodedGeometry[]::new));

        return read(query, range);
    }

    private static DiscreteGlobalGridGeometry toDiscreteGlobalGridGeometry(GridGeometry domain, DiscreteGlobalGridReferenceSystem dggrs) throws TransformException, IncommensurableException {
        Quantity<?> coverageResolution = GridAsDiscreteGlobalGridResource.computeAverageResolution(domain);

        coverageResolution = Quantities.create(coverageResolution.getValue().doubleValue() * 10, Units.METRE);

        //extract zones in the wanted area and resolution
        final DiscreteGlobalGridHierarchy hierarchy = dggrs.getGridSystem().getHierarchy();
        final DiscreteGlobalGrid grid = hierarchy.getGrid(coverageResolution);

        final List<Object> zoneIds;
        try (Stream<Zone> zones = grid.getZones(domain.getEnvelope(dggrs.getGridSystem().getCrs()))) {
            zoneIds = zones.map(Zone::getIdentifier).toList();
        }
        return DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds, null);
    }

    /**
     * Retrieve a set of DGGRS zone data.
     *
     * @param geometry to read
     * @param range bands to select
     * @return DiscreteGlobalGridCoverage, never null
     * @throws DataStoreException
     */
    public CodedCoverage read(CodedGeometry geometry, int ... range) throws DataStoreException;
}
