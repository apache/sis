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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.iso.Names;
import static org.apache.sis.storage.rs.CodedGeometry.compound;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CompoundCodedResource extends AbstractResource implements CodedResource {

    private final GenericName name;
    private final List<CodedResource> resources;
    private final List<SampleDimension> sampleDimensions;

    /**
     * For each band of this resource, the index of the resource providing it.
     */
    private final int[] bandToResource;

    /**
     * For each band of this resource, the band index in the resource providing it.
     */
    private final int[] bandToResourceBand;

    private final CodedGeometry geometry;


    public CompoundCodedResource(GenericName name, List<CodedResource> resources) throws DataStoreException {
        super(null);
        this.name = name;
        this.resources = resources;

        final List<SampleDimension> bands = new ArrayList<>();
        final List<Integer> resourceOfBands = new ArrayList<>();
        final List<Integer> bandsInResources = new ArrayList<>();

        //create the sum of all dimensions in the grid geometry
        ReferenceSystem rs = resources.get(0).getGridGeometry().getReferenceSystem();
        for (int r = 0; r < resources.size(); r++) {
            final CodedResource cr = resources.get(r);
            final String crName = cr.getIdentifier().get().tip().toString();
            final CodedGeometry gridGeom = cr.getGridGeometry();

            if (!gridGeom.getReferenceSystem().equals(rs)) {
                rs = null;
            }

            final List<SampleDimension> sds = cr.getSampleDimensions();
            for (int b = 0; b < sds.size(); b++) {
                final SampleDimension sd = sds.get(b);
                bands.add(ReorderCodedCoverage.rename(sd, Names.createLocalName(null, null, crName + ":" + sd.getName())));
                resourceOfBands.add(r);
                bandsInResources.add(b);
            }
        }

        geometry = new CodedGeometry(rs, null, null, null);
        sampleDimensions = Collections.unmodifiableList(bands);
        bandToResource = resourceOfBands.stream().mapToInt(Integer::intValue).toArray();
        bandToResourceBand = bandsInResources.stream().mapToInt(Integer::intValue).toArray();
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
        /*
         * Only the resources providing at least one requested band are read,
         * each one with only the bands it provides, in the order they are requested.
         * The coverages are stacked, then reordered in the requested order
         * and renamed with the band names of this resource.
         */
        final int[] bands = (range == null || range.length == 0) ? ArraysExt.range(0, sampleDimensions.size()) : range;
        final int[] coverageOfResource = new int[resources.size()];
        Arrays.fill(coverageOfResource, -1);
        final List<CodedResource> used = new ArrayList<>();
        final List<List<Integer>> usedBands = new ArrayList<>();       // Indices in this resource.
        for (final int band : bands) {
            final int r = bandToResource[band];
            if (coverageOfResource[r] < 0) {
                coverageOfResource[r] = used.size();
                used.add(resources.get(r));
                usedBands.add(new ArrayList<>());
            }
            final List<Integer> resourceBands = usedBands.get(coverageOfResource[r]);
            if (!resourceBands.contains(band)) {
                resourceBands.add(band);
            }
        }
        final List<Integer> stacked = usedBands.stream().flatMap(List::stream).toList();
        final int[] reorder = new int[bands.length];
        final GenericName[] bandNames = new GenericName[bands.length];
        for (int i = 0; i < bands.length; i++) {
            reorder[i] = stacked.indexOf(bands[i]);
            bandNames[i] = sampleDimensions.get(bands[i]).getName();
        }
        /*
         * Do not align the query on this resource geometry : it has no extent and no transform,
         * aligning would only add undefined components, resulting in a geometry without extent.
         * Each resource aligns the query on its own geometry.
         */
        try {
            final CodedCoverage[] coverages = new CodedCoverage[used.size()];
            final CodedGeometry[] geometries = new CodedGeometry[coverages.length + 1];
            geometries[0] = geometry;
            for (int i = 0; i < coverages.length; i++) {
                final int[] resourceRange = usedBands.get(i).stream().mapToInt((band) -> bandToResourceBand[band]).toArray();
                coverages[i] = used.get(i).read(geometry, resourceRange);
                geometries[i+1] = coverages[i].getGeometry();
            }
            //sources may have added dimensions which are not in the query, aggregate them
            final CodedGeometry target;
            try {
                target = aggregate(geometries);
            } catch (IllegalArgumentException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }
            for (int i = 0; i < coverages.length; i++) {
                if (!target.equals(coverages[i].getGeometry())) {
                    coverages[i] = new ResampledCodedCoverage(used.get(i).getIdentifier().get(), coverages[i], target);
                }
            }
            return new ReorderCodedCoverage(name, new CompoundCodedCoverage(name, coverages), reorder, bandNames);
        } catch (FactoryException | TransformException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * Aggregates the given geometries in a single geometry.
     * The first geometry is the reference, it is copied unchanged at the beginning of the result.
     * The components of the other geometries which can not be mapped from any component of the
     * reference are appended after it. Such components are typically temporal or vertical axes.
     * Equivalent components found in several geometries are merged in a single component
     * containing all their values.
     *
     * @param  geometries  geometries to aggregate, the first one is the reference.
     * @return the aggregated geometry, or the reference if no other geometry has extra components.
     * @throws IllegalArgumentException if an extra component can not be aggregated.
     * @throws FactoryException if an operation between equivalent components can not be created.
     * @throws TransformException if the values of equivalent components can not be converted.
     */
    private static CodedGeometry aggregate(CodedGeometry ... geometries) throws FactoryException, TransformException {
        if (geometries.length == 0) return null;
        final CodedGeometry reference = geometries[0];
        final ReferenceSystem referenceRs = reference.getReferenceSystem();
        if (referenceRs == null) return reference;
        final List<ReferenceSystem> referenceComponents = ReferenceSystems.getSingleComponents(referenceRs, true);

        final List<CodedGeometry> extras = new ArrayList<>();
        for (int g = 1; g < geometries.length; g++) {
            final CodedGeometry geometry = geometries[g];
            if (geometry.getReferenceSystem() == null) continue;
            componentLoop:
            for (ReferenceSystem component : ReferenceSystems.getSingleComponents(geometry.getReferenceSystem(), true)) {
                if (isMappable(referenceComponents, component)) continue;
                if (!(component instanceof CoordinateReferenceSystem crs) || ReferenceSystems.getDimension(crs) != 1) {
                    throw new IllegalArgumentException("Can not aggregate component " + component.getName()
                            + ", only one-dimensional coordinate reference systems are supported.");
                }
                final CodedGeometry slice = geometry.slice(component).get();

                for (int i = 0, n = extras.size(); i < n; i++) {
                    final CodedGeometry extra = extras.get(i);
                    try {
                        CRS.findOperation(crs, (CoordinateReferenceSystem) extra.getReferenceSystem(), null);
                    } catch (FactoryException ex) {
                        //not an equivalent axis, continue
                        continue;
                    }
                    if (extra.isRegularGrid().isEmpty() || slice.isRegularGrid().isEmpty()) {
                        throw new IllegalArgumentException("Can not aggregate component " + component.getName()
                                + ", only regular grids can be merged.");
                    }
                    extras.set(i, mergeGrids(extra, slice));
                    continue componentLoop;
                }

                //no equivalent component, we can add it
                extras.add(slice);
            }
        }
        if (extras.isEmpty()) return reference;
        extras.add(0, reference);
        return compound(extras.toArray(CodedGeometry[]::new));
    }

    /**
     * Returns true if an operation exists from one of the reference components to the given component.
     */
    private static boolean isMappable(List<ReferenceSystem> referenceComponents, ReferenceSystem component) {
        for (ReferenceSystem candidate : referenceComponents) {
            try {
                ReferenceSystems.findOperation(candidate, component, null);
                return true;
            } catch (FactoryException ex) {
                //not mappable from this component, try next
            }
        }
        return false;
    }

    /**
     * Merges two one-dimensional regular grids in a grid containing the values of both.
     */
    private static CodedGeometry mergeGrids(CodedGeometry cg1, CodedGeometry cg2) throws FactoryException, TransformException {
        final GridGeometry grid1 = cg1.isRegularGrid().get();
        GridGeometry grid2 = cg2.isRegularGrid().get();
        final CoordinateReferenceSystem crs1 = grid1.getCoordinateReferenceSystem();

        if ( grid1.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)
           && grid2.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)) {

            grid2 = toOther(grid2, crs1);
            final double[] array1 = allTransformedValues(grid1);
            final double[] array2 = allTransformedValues(grid2);
            final double[] all = merge(array1, array2);
            if (all.length == array1.length) {
                //second grid does not add any value, preserve the original transform
                return cg1;
            }

            final MathTransform trs = MathTransforms.interpolate(null, all);
            final GridGeometry result = new GridGeometry(new GridExtent(null, 0, all.length, false), PixelInCell.CELL_CENTER, trs, crs1);
            return new CodedGeometry(result);

        } else {
            //we can not merge the axis values
            final GridGeometry result = new GridGeometry(null, null, null, crs1);
            return new CodedGeometry(result);
        }
    }

    private static GridGeometry toOther(GridGeometry grid, CoordinateReferenceSystem target) throws FactoryException {
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
        final Set<Double> all = new TreeSet<>();
        for (double d : array1) all.add(d);
        for (double d : array2) all.add(d);
        return all.stream().mapToDouble(Double::doubleValue).toArray();
    }
}
