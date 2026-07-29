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
package org.apache.sis.storage.rs.internal.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CompoundCodedResource extends AbstractResource implements CodedResource {

    private final GenericName name;
    private final List<CodedResource> resources;
    private List<SampleDimension> sampleDimensions;
    private final CodedGeometry geometry;


    public CompoundCodedResource(GenericName name, List<CodedResource> resources) throws DataStoreException {
        super(null);
        this.name = name;
        this.resources = resources;

        sampleDimensions = new ArrayList<>();

        //create the sum of all dimensions in the grid geometry


        ReferenceSystem rs = resources.get(0).getGridGeometry().getReferenceSystem();
        for (CodedResource cr : resources) {
            final String crName = cr.getIdentifier().get().tip().toString();
            final CodedGeometry gridGeom = cr.getGridGeometry();

            if (!gridGeom.getReferenceSystem().equals(rs)) {
                rs = null;
            }

            for (SampleDimension sd : cr.getSampleDimensions()) {
                final String attName = sd.getName().toString();
                final String finalName = crName+":"+attName;
                sampleDimensions.add(new SampleDimension.Builder().setName(finalName).build());
            }
        }

        geometry = new CodedGeometry(rs, null, null, null);
        sampleDimensions = Collections.unmodifiableList(sampleDimensions);
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(name);
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return sampleDimensions;
    }

    @Override
    public CodedGeometry getGridGeometry() throws DataStoreException {
        return geometry;
    }

    @Override
    public CodedCoverage read(CodedGeometry geometry, int... range) throws DataStoreException {
        try {
            final CodedCoverage[] resampled = new CodedCoverage[resources.size()];
            for (int i = 0; i < resampled.length; i++) {
                final CodedResource resource = resources.get(i);
                CodedCoverage coverage = resources.get(i).read(geometry);
                if (!geometry.equals(coverage.getGeometry())) {
                    coverage = new ResampledCodedCoverage(resource.getIdentifier().get(), coverage, geometry);
                }
                resampled[i] = coverage;
            }
            return new CompoundCodedCoverage(name, resampled, sampleDimensions);
        } catch (FactoryException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    private CodedGeometry compound(List<CodedGeometry> geometries) {

        final Map<ReferenceSystem,CodedGeometry> map = new LinkedHashMap();

        //cut each geometry in dimensions slice and try to merge them
        for (CodedGeometry geom : geometries) {
            final ReferenceSystem referenceSystem = geom.getReferenceSystem();
            final List<ReferenceSystem> singleComponents = ReferenceSystems.getSingleComponents(referenceSystem, true);
            sliceLoop:
            for (ReferenceSystem rs : singleComponents) {
                final CodedGeometry slice = geom.slice(rs).get();

                //make a more accurate test to find a match
                for (Entry<ReferenceSystem, CodedGeometry> entry : map.entrySet()) {
                    final ReferenceSystem krs = entry.getKey();
                    if (rs instanceof DiscreteGlobalGridReferenceSystem && krs instanceof DiscreteGlobalGridReferenceSystem) {
                        final CodedGeometry result = mergeDggrs(entry.getValue(), slice);
                        map.remove(entry.getKey());
                        map.put(result.getReferenceSystem(), result);
                        continue sliceLoop;
                    } else if (rs instanceof CoordinateReferenceSystem crs1 && krs instanceof CoordinateReferenceSystem crs2) {
                        try {
                            CRS.findOperation(crs2, crs1, null);
                            final CodedGeometry result = mergeGrids(entry.getValue(), slice);
                            map.remove(entry.getKey());
                            map.put(result.getReferenceSystem(), result);
                            continue sliceLoop;
                        } catch (FactoryException | TransformException ex) {
                            //do nothing, continue
                        }
                    }
                }

                //no equivalent dimension, we can add it
                map.put(rs, slice);
            }
        }

        final List<CodedGeometry> sliceQuery = new ArrayList<>(map.values());

        //place RS in order : horiontal > vertical > others
        sliceQuery.sort(new Comparator<CodedGeometry>() {
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
        return CodedGeometry.compound(sliceQuery.toArray(CodedGeometry[]::new));
    }

    private CodedGeometry mergeDggrs(CodedGeometry cg1, CodedGeometry cg2) {
        final DiscreteGlobalGridReferenceSystem dggrs1 = (DiscreteGlobalGridReferenceSystem) cg1.getReferenceSystem();
        final DiscreteGlobalGridReferenceSystem dggrs2 = (DiscreteGlobalGridReferenceSystem) cg2.getReferenceSystem();
        if (!dggrs1.equals(dggrs2)) {
            throw new IllegalArgumentException("Two code geometries use different DGGRS");
        }
        return DiscreteGlobalGridGeometry.unstructured(dggrs1, null, null);
    }

    private CodedGeometry mergeGrids(CodedGeometry cg1, CodedGeometry cg2) throws FactoryException, TransformException {
        final GridGeometry grid1 = cg1.isRegularGrid().get();
        GridGeometry grid2 = cg2.isRegularGrid().get();
        final CoordinateReferenceSystem crs1 = grid1.getCoordinateReferenceSystem();
        final CoordinateReferenceSystem crs2 = grid2.getCoordinateReferenceSystem();

        if ( grid1.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)
           && grid2.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)) {

            grid2 = toOther(grid2, crs1);
            final double[] array1 = allTransformedValues(grid1);
            final double[] array2 = allTransformedValues(grid2);
            final double[] all = merge(array1, array2);

            final MathTransform trs = MathTransforms.interpolate(null, all);
            final GridGeometry result = new GridGeometry(new GridExtent(null, 0, all.length, false), PixelInCell.CELL_CENTER, trs, crs1);
            return new CodedGeometry(result);

        } else {
            //we can not merge the axis values
            final GridGeometry result = new GridGeometry(null, null, null, crs1);
            return new CodedGeometry(result);
        }
    }

    private GridGeometry toOther(GridGeometry grid, CoordinateReferenceSystem target) throws FactoryException {
        final CoordinateReferenceSystem source = grid.getCoordinateReferenceSystem();
        final GridExtent sourceExtent = grid.getExtent();
        final MathTransform sourceGridToCRS = grid.getGridToCRS(PixelInCell.CELL_CENTER);
        final MathTransform trs = CRS.findOperation(source, target, null).getMathTransform();
        final MathTransform gridToCrs = MathTransforms.concatenate(sourceGridToCRS, trs);
        return new GridGeometry(sourceExtent, PixelInCell.CELL_CENTER, gridToCrs, target);
    }

    private static double[] allTransformedValues(GridGeometry grid) throws TransformException {
        final GridExtent extent = grid.getExtent();
        final MathTransform gridToCRS = grid.getGridToCRS(PixelInCell.CELL_CENTER);

        final long[] low = extent.getLow().getCoordinateValues();
        final long[] high = extent.getHigh().getCoordinateValues();
        final long min = low[0];
        final long max = high[0];

        final double[] array = new double[(int)(max-min+1)];
        for (int i = 0; i < array.length; i++) {
            array[i] = min + i;
        }
        gridToCRS.transform(array, 0, array, 0, array.length);
        return array;
    }

    private static double[] merge(double[] array1, double[] array2) {
        final Set<Double> all = new TreeSet();
        for (double d : array1) all.add(d);
        for (double d : array2) all.add(d);
        return all.stream().mapToDouble(Double::doubleValue).toArray();
    }

}
