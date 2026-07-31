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

import org.apache.sis.storage.dggs.internal.shared.GridAsDiscreteGlobalGridResource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.rs.Code;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.storage.rs.CodeTransform;
import org.apache.sis.storage.rs.WritableCodeIterator;

/**
 * View a grid coverage resource as a dggrs coverage resource.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GridAsCodedResource extends AbstractResource implements CodedResource {

    private final CodedGeometry gridGeometry;
    private final GridCoverageResource source;
    private final GenericName name;

    public GridAsCodedResource(GenericName name, DiscreteGlobalGridReferenceSystem dggrs, GridCoverageResource resource)
            throws DataStoreException, IncommensurableException, TransformException, FactoryException {
        super(null);
        ArgumentChecks.ensureNonNull("name", name);
        ArgumentChecks.ensureNonNull("resource", resource);
        this.name = name;
        this.source = resource;

        //create matching reference grid geometry replacing the horizontal crs
        final GridGeometry sourceGridGeometry = resource.getGridGeometry();
        final List<SingleCRS> singles = (List) ReferenceSystems.getSingleComponents(sourceGridGeometry.getCoordinateReferenceSystem(), true);
        final List<CodedGeometry> parts = new ArrayList<>();

        for (int i = 0, n = singles.size(); i < n ; i++) {
            final SingleCRS s = singles.get(i);
            if (CRS.isHorizontalCRS(s)) {
                CodedGeometry rrg = new CodedGeometry(dggrs, new GridExtent(null, 0, 0, true), CodeTransforms.toTransform(dggrs), null);
                parts.add(rrg);
            } else {
                final GridGeometry crsSlice = CodeTransforms.slice(sourceGridGeometry, s);
                parts.add(new CodedGeometry(crsSlice));
            }
        }
        gridGeometry = CodedGeometry.compound(parts.toArray(CodedGeometry[]::new));

        Quantity<?> res = GridAsDiscreteGlobalGridResource.computeAverageResolution(resource.getGridGeometry());
        DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();
        coder.setPrecision(res, null);
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(name);
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return source.getSampleDimensions();
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return source.getEnvelope();
    }

    @Override
    public CodedGeometry getGridGeometry() {
        return gridGeometry;
    }

    @Override
    public CodedCoverage read(CodedGeometry userQuery, int... range) throws DataStoreException {

        List<SampleDimension> sampleDimensions = source.getSampleDimensions();
        if (range != null && range.length != 0) {
            final List<SampleDimension> selected = new ArrayList<>();
            for (int i = 0; i < range.length; i++) {
                selected.add(sampleDimensions.get(range[i]));
            }
            sampleDimensions = selected;
        }

        /*
        Convert the geometry to source grid geometry.
        add additional dimensions if some are missing.
        */
        GridGeometry coverageQuery;
        try {
            coverageQuery = toGridGeometry(userQuery);
        } catch (FactoryException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
        if (source.getGridGeometry().isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)) {
            //append dimensions we did not have in the query and remove those we don't have
            //bug coverageQuery = source.getGridGeometry().derive().rounding(GridRoundingMode.ENCLOSING).subgrid(coverageQuery).build();

            try {
                coverageQuery = smartIntersect(source.getGridGeometry(), coverageQuery);
            } catch (FactoryException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            } catch (DisjointExtentException ex) {
                //no intersection
                throw new NoSuchDataException("Data do not intersect requested area", ex);
            }
        }
        //convert it back to a referenced grid geometry query
        final CodedGeometry resultGeometry;
        try {
            resultGeometry = expend(userQuery, coverageQuery);
        } catch (FactoryException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }

        final GridExtent extent = resultGeometry.getExtent();
        final CodeTransform transform = resultGeometry.getGridToRS();
        final int nbZone = Math.toIntExact(extent.getLatticePointCount());

        final List<Array> samples = new ArrayList<>();
        final double[] nans = new double[sampleDimensions.size()];
        for (int i = 0; i < nans.length; i++) {
            final SampleSystem ss = new SampleSystem(DataType.DOUBLE, sampleDimensions.get(i));
            final double[] datas = new double[nbZone];
            Arrays.fill(datas, Double.NaN);
            samples.add(NDArrays.of(ss, datas));
            nans[i] = Double.NaN;
        }

        final ArrayCodedCoverage target;
        try {
            target = new ArrayCodedCoverage(getIdentifier().get(), resultGeometry, samples);
        } catch (FactoryException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }

        try (final WritableCodeIterator iterator = target.createWritableIterator()) {
            try {
                source.setLoadingStrategy(RasterLoadingStrategy.AT_GET_TILE_TIME);
                final GridCoverage coverage = source.read(coverageQuery, range).forConvertedValues(true);
                final GridCoverage.Evaluator evaluator = coverage.evaluator();
                evaluator.setNullIfOutside(true);
                evaluator.setWraparoundEnabled(true);

                while (iterator.next()) {
                    final int[] gridPosition = iterator.getPosition();
                    final Code location = transform.toCode(gridPosition);
                    DirectPosition dp = location.toDirectPosition();
                    //todo convert to grid coverage coordinate
                    double[] values = evaluator.apply(dp);
                    if (values == null) {
                        values = nans;
                    }
                    iterator.setCell(values);
                }
            } catch (NoSuchDataException | FactoryException ex) {
                // do nothing
            }
        } catch (TransformException ex) {
            throw new DataStoreException(ex);
        }

        return target;
    }

    private static GridGeometry toGridGeometry(CodedGeometry geometry) throws FactoryException {

        final List<ReferenceSystem> all = ReferenceSystems.getSingleComponents(geometry.getReferenceSystem(), true);

        GridGeometry gg = null;
        for (int k = 0, n = all.size(); k < n; k++) {
            final CodedGeometry rgg = geometry.slice(all.get(k)).get();
            GridGeometry slice = rgg.isRegularGrid().orElse(null);
            if (slice == null) {
                final Envelope env = rgg.getEnvelope();
                final double[] resolution = rgg.getResolutionProjected(true);

                final long[] low = new long[env.getDimension()];
                final long[] high = new long[low.length];
                for (int i = 0 ; i < low.length; i++) {
                    high[i] = (long)Math.ceil((env.getSpan(i) / resolution[i])*2);
                }
                final GridExtent extent = new GridExtent(null, low, high, false);
                slice = new GridGeometry(extent, env, GridOrientation.HOMOTHETY);
            }
            gg = (gg == null) ? slice : new GridGeometry(gg, slice);
        }

        return gg;
    }

    /**
     * Create a new ReferencedGridGeometry containing the base ReferencedGridGeometry
     * and adding any dimension missing from the second GridGeometry.
     */
    private static CodedGeometry expend(CodedGeometry base, GridGeometry toAppend) throws FactoryException {

        final Set<ReferenceSystem> all = new LinkedHashSet();
        all.addAll(ReferenceSystems.getSingleComponents(toAppend.getCoordinateReferenceSystem(), true));

        final List<CodedGeometry> parts = new ArrayList<>();
        for (ReferenceSystem rs : all) {
            if (rs instanceof CoordinateReferenceSystem crs) {
                if (CRS.isHorizontalCRS(crs)) {
                    //find the dggrs in the base
                    for (ReferenceSystem brs : ReferenceSystems.getSingleComponents(base.getReferenceSystem(), true)) {
                        if (brs instanceof DiscreteGlobalGridReferenceSystem dggrs) {
                            //copy it from base
                            parts.add(base.slice(brs).get());
                        }
                    }
                } else {
                    Optional<CodedGeometry> slice = base.slice(rs);
                    if (!slice.isPresent()) {
                        //copy it from the missing geometry
                        parts.add(new CodedGeometry(CodeTransforms.slice(toAppend, crs)));
                    } else {
                        parts.add(slice.get());
                    }
                }
            }
        }

        return CodedGeometry.compound(parts.toArray(CodedGeometry[]::new));
    }

    private static GridGeometry smartIntersect(GridGeometry base, GridGeometry query) throws FactoryException {

        final List<ReferenceSystem> queryCrs = ReferenceSystems.getSingleComponents(query.getCoordinateReferenceSystem(), true);
        final CodedGeometry basergg = new CodedGeometry(base);
        final CodedGeometry queryrgg = new CodedGeometry(query);

        GridGeometry result = null;

        for (ReferenceSystem rs : ReferenceSystems.getSingleComponents(base.getCoordinateReferenceSystem(), true)) {
            final CoordinateReferenceSystem baseSliceCrs = (CoordinateReferenceSystem) rs;
            GridGeometry slice = basergg.slice(baseSliceCrs).get().isRegularGrid().get();

            for (ReferenceSystem target : queryCrs) {
                try {
                    CRS.findOperation(baseSliceCrs, (CoordinateReferenceSystem) target, null);
                    final GridGeometry targetSlice = queryrgg.slice(target).get().isRegularGrid().get();
                    slice = slice.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(targetSlice).build();
                    break;
                } catch (FactoryException ex) {
                    //not found
                }
            }
            result = result == null ? slice : new GridGeometry(result, slice);
        }

        return result;
    }

}
