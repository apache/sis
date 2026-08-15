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

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.FilterFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.feature.Features;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.Vector1D;
import org.apache.sis.geometry.DirectPosition1D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.jts.JTS;
import org.apache.sis.math.Statistics;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGrid;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.rs.Code;
import org.apache.sis.referencing.rs.CodeOperation;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.storage.coverage.FeatureExt;
import org.apache.sis.storage.dggs.internal.shared.ArrayDiscreteGlobalGridCoverage;
import org.apache.sis.storage.rs.CodeTransform;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.storage.rs.internal.shared.AddDimCodedCoverage;
import org.apache.sis.storage.rs.internal.shared.ArrayCodedCoverage;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.iso.Names;


/**
 * Provide operations related to DiscreteGlobalGridReferenceSystem.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DiscreteGlobalGridCoverageProcessor {

    /**
     * QuantizationValue has no effect if QuantizationSelection is CENTER_PIXEL.
     */
    public static enum QuantizationValue {
        /**
         * Pick the minimal value from pixels intersecting the dggrs zone.
         */
        MINIMUM,
        /**
         * Pick the maximal value from pixels intersecting the dggrs zone.
         */
        MAXIMUM,
        /**
         * Pick the mean value from pixels intersecting the dggrs zone.
         */
        MEAN,
        /**
         * Pick the root mean square value from pixels intersecting the dggrs zone.
         */
        ROOT_MEAN_SQUARE,
        /**
         * Pick the value range from pixels intersecting the dggrs zone.
         */
        SPAN,
        /**
         * Pick the sum value from pixels intersecting the dggrs zone.
         */
        SUM,
        /**
         * Pick the standard deviation value from pixels intersecting the dggrs zone.
         */
        STANDARD_DEVIATION
    };

    public static enum QuantizationSelection {
        /**
         * Use a single pixel at zone center position.
         */
        CENTER_PIXEL,
        /**
         * Use all pixels intersecting zone polygon.
         * All pixels have the same weight.
         */
        PIXELS_WEIGHT_FIXED,
        /**
         *
         * Use all pixels intersecting zone polygon.
         * Pixels have a weight which vary based on the intersection area.
         * The sum of all weights will be one.
         * If pixels do not cover the full zone, weights will be adjusted to be one.
         */
        PIXELS_WEIGHT_BY_AREA
    };

    private static final String[] CELLS_RESOURCES_NAMES = {"cell", "cells", "dggs_id", "cells_id", "cell_id", "dggs_cell", "dggs_cells"};

    private QuantizationValue[] quantizationValues = new QuantizationValue[]{QuantizationValue.MEAN};
    private QuantizationSelection quantizationSelection = QuantizationSelection.CENTER_PIXEL;

    public DiscreteGlobalGridCoverageProcessor() {
    }

    /**
     * Set quantization selection method.
     * Default is CENTER_PIXEL
     *
     * @param quantizationSelection not null
     */
    public void setQuantizationSelection(QuantizationSelection quantizationSelection) {
        ArgumentChecks.ensureNonNull("selection", quantizationSelection);
        this.quantizationSelection = quantizationSelection;
    }

    /**
     * @return QuantizationSelection never null
     */
    public QuantizationSelection getQuantizationSelection() {
        return quantizationSelection;
    }

    /**
     *
     * @param quantizationValues
     */
    public void setQuantizationValues(QuantizationValue ... quantizationValues) {
        ArgumentChecks.ensureNonNull("values", quantizationValues);
        ArgumentChecks.ensureStrictlyPositive("values", quantizationValues.length);
        this.quantizationValues = quantizationValues;
    }

    /**
     * @return quantization values, never null or empty
     */
    public QuantizationValue[] getQuantizationValues() {
        return quantizationValues.clone();
    }

    /**
     * Resample a GridCoverage to a DiscreteGlobalGridCoverage.
     * The current sampling method use nearest neighbor.
     *
     * @param source to sample from, not null
     * @param gridGeometry geometry of the desired dggs coverage.
     * @param range bands to read
     * @return dggs coverage, not null
     *
     * @throws DataStoreException if extracting datas for the source failed or dggs coverage failed at writing
     * @throws TransformException if a dggrs zone computation failed
     */
    public CodedCoverage resample(GridCoverageResource source, DiscreteGlobalGridGeometry gridGeometry, int ... range) throws DataStoreException, TransformException {
        final List<Object> zones = gridGeometry.getZoneIds();
        List<SampleDimension> sampleDimensions = source.getSampleDimensions();
        if (range != null && range.length != 0) {
            final List<SampleDimension> selected = new ArrayList<>();
            for (int i = 0; i < range.length; i++) {
                selected.add(sampleDimensions.get(range[i]));
            }
            sampleDimensions = selected;
        }

        final List<Array> samples = new ArrayList<>();
        final double[] nans = new double[sampleDimensions.size()];
        for (int i = 0; i < nans.length; i++) {
            final SampleSystem ss = new SampleSystem(DataType.DOUBLE, sampleDimensions.get(i));
            final double[] datas = new double[zones.size()];
            Arrays.fill(datas, Double.NaN);
            samples.add(NDArrays.of(ss, datas));
            nans[i] = Double.NaN;
        }

        final ArrayDiscreteGlobalGridCoverage target = new ArrayDiscreteGlobalGridCoverage(source.getIdentifier().get(), gridGeometry, samples);
        final CodeTransform gridToRS = gridGeometry.getGridToRS();
        final DiscreteGlobalGridReferenceSystem dggrs = gridGeometry.getReferenceSystem();
        final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();
        try (final WritableCodeIterator iterator = target.createWritableIterator()) {

            final Envelope env = target.getEnvelope().get();
            final double[] resolution = target.getResolution(true);

            final GridExtent extent = new GridExtent(
                    (long) Math.ceil((env.getSpan(0) / resolution[0])*2),
                    (long) Math.ceil((env.getSpan(1) / resolution[0])*2));
            final GridGeometry grid = new GridGeometry(extent, env, GridOrientation.REFLECTION_Y);
            try {
                source.setLoadingStrategy(RasterLoadingStrategy.AT_GET_TILE_TIME);
                GridCoverage coverage = source.read(grid, range).forConvertedValues(true);

                //if (coverage.getClass().getName().endsWith("TileMatrixCoverage")) {
                    //todo : ugly hack to enforce coverage cache
                    final GridGeometry domain = coverage.getGridGeometry();
                    RenderedImage image = coverage.render(domain.getExtent());
                    coverage = new GridCoverageBuilder()
                            .setDomain(domain)
                            .setRanges(coverage.getSampleDimensions())
                            .setValues(image)
                            .build();
                //}

                final GridCoverage.Evaluator evaluator = coverage.evaluator();
                evaluator.setNullIfOutside(true);

                while (iterator.next()) {
                    final int[] position = iterator.getPosition();
                    final Code code = gridToRS.toCode(position);
                    final Object zid = code.getOrdinate(0);
                    final Zone zone = coder.decode(zid);
                    final DirectPosition dp = zone.getPosition();
                    double[] values = evaluator.apply(dp);
                    if (values == null) {
                        values = nans;
                    }
                    iterator.setCell(values);
                }
            } catch (NoSuchDataException ex) {
                // do nothing
            }
        } catch (TransformException ex) {
            throw new DataStoreException(ex);
        }

        return target;
    }

    /**
     * Resample a FeatureSet to a DiscreteGlobalGridCoverage.
     * The current sampling method use nearest neighbor.
     *
     * @param featureSet to sample from, not null
     * @param gridGeometry geometry of the desired dggs coverage.
     * @param sampleDimensions bands to read, must match a feature type property
     * @return dggs coverage, not null
     *
     * @throws DataStoreException if extracting datas for the source failed or dggs coverage failed at writing
     * @throws TransformException if a dggrs zone computation failed
     */
    public CodedCoverage resample(FeatureSet featureSet, DiscreteGlobalGridGeometry gridGeometry, List<SampleDimension> sampleDimensions) throws DataStoreException, TransformException {
        final FeatureType baseType = featureSet.getType();
        final List<Object> zones = gridGeometry.getZoneIds();

        final List<Array> samples = new ArrayList<>();
        final double[] nans = new double[sampleDimensions.size()];
        final String[] propertyNames = new String[nans.length];
        final Statistics[][] stats = new Statistics[sampleDimensions.size()][zones.size()];
        //counters for categorized values
        final Map<Object,AtomicLong>[][] counters = new Map[sampleDimensions.size()][zones.size()];
        for (int i = 0; i < nans.length; i++) {
            final SampleDimension sd = sampleDimensions.get(i);
            final SampleSystem ss = new SampleSystem(DataType.DOUBLE, sd);
            final double[] datas = new double[zones.size()];
            Arrays.fill(datas, Double.NaN);
            samples.add(NDArrays.of(ss, datas));
            nans[i] = Double.NaN;

            propertyNames[i] = sd.getName().tip().toString();

            //check if value is categorized
            final PropertyType prop = baseType.getProperty(propertyNames[i]);
            if (prop instanceof AttributeType at) {
                final Object characteristic = at.characteristics().get(AttributeConvention.VALID_VALUES_CHARACTERISTIC.toString());
                if (characteristic instanceof AttributeType cat) {
                    final Object defaultValue = cat.getDefaultValue();
                    if (defaultValue instanceof Collection) {
                        counters[i][0] = new HashMap<>();
                    }
                }
            }
        }


        final ArrayDiscreteGlobalGridCoverage target = new ArrayDiscreteGlobalGridCoverage(featureSet.getIdentifier().get(), gridGeometry, samples);
        final Envelope env = target.getEnvelope().get();

        //read only the features needed
        final List<FeatureQuery.NamedExpression> projections = new ArrayList<>(1 + sampleDimensions.size());
        final FilterFactory<Feature, Object, Object> ff = DefaultFilterFactory.forFeatures();
        // todo FIX SIS !!!!!!
        projections.add(new FeatureQuery.NamedExpression(ff.property(AttributeConvention.GEOMETRY), AttributeConvention.GEOMETRY));
        //projections.add(new FeatureQuery.NamedExpression(ff.function("ST_Transform", ff.property(AttributeConvention.GEOMETRY), ff.literal(dggrs.getGridSystem().getCrs())), AttributeConvention.GEOMETRY_PROPERTY));
        sampleDimensions.forEach((n) -> projections.add(new FeatureQuery.NamedExpression(ff.property(n.getName().toString()), n.getName())));
        final FeatureQuery query = new FeatureQuery();
        query.setSelection(env);
        query.setProjection(projections.toArray(FeatureQuery.NamedExpression[]::new));
        final FeatureSet subset = featureSet.subset(query);

        FeatureType type = subset.getType();
        final Class geomClass = Features.getValueClass(type.getProperty(AttributeConvention.GEOMETRY));


        final DiscreteGlobalGridReferenceSystem dggrs = gridGeometry.getReferenceSystem();
        final DiscreteGlobalGridHierarchy dggh = dggrs.getGridSystem().getHierarchy();
        final DiscreteGlobalGrid grid = dggh.getGrids().get(gridGeometry.getRefinementLevel());

        MathTransform trs;
        try { //todo remove when SIS bug is fixed
            final CoordinateReferenceSystem srcCrs = FeatureExt.getCRS(featureSet.getType());
            final CoordinateReferenceSystem targetCrs = dggrs.getGridSystem().getCrs();
            if (!Utilities.equalsIgnoreMetadata(srcCrs, targetCrs)) {
                trs = CRS.findOperation(srcCrs, targetCrs, null).getMathTransform();
            } else {
                trs = null;
            }

        } catch (OperationNotFoundException ex){
            //todo FIX SIS !
            trs = null;
        } catch (FactoryException ex) {
            //SIS bug, use identity for now
            throw new DataStoreException(ex.getMessage(), ex);
        }

        if (geomClass == Point.class) {
            //use a more efficient approach

            final Map<Long,Integer> index = new HashMap();
            for (int i = 0, n = zones.size(); i < n; i++) {
                index.put((Long) zones.get(i), i);
            }

            final double[] coord = new double[2];
            final DirectPosition dp = new GeneralDirectPosition(coord);
            try (Stream<Feature> stream = subset.features(false)) {
                final Iterator<Feature> fite = stream.iterator();
                zoneLoop:
                while (fite.hasNext()) {
                    final Feature feature = fite.next();
                    org.locationtech.jts.geom.Point pt = (org.locationtech.jts.geom.Point) feature.getPropertyValue(AttributeConvention.GEOMETRY);
                    coord[0] = pt.getX();
                    coord[1] = pt.getY();
                    if(trs != null) trs.transform(coord, 0, coord, 0, 1);
                    Zone z = grid.getZone(dp);

                    Integer zoneIndex = index.get(z.getLongIdentifier());
                    if (zoneIndex != null) {
                        for (int i = 0; i < propertyNames.length; i++) {
                            Object value = feature.getPropertyValue(propertyNames[i]);
                            if (value instanceof Number n) {
                                double v = n.doubleValue();
                                if (!Double.isNaN(v)) {
                                    if (stats[i][zoneIndex] == null) {
                                        stats[i][zoneIndex] = new Statistics("");
                                    }
                                    stats[i][zoneIndex].accept(v);
                                }
                            }
                            if (counters[i][0] != null) {
                                if (counters[i][zoneIndex] == null) counters[i][zoneIndex] = new HashMap<>();
                                AtomicLong cnt = counters[i][zoneIndex].get(value);
                                if (cnt == null) {
                                    cnt = new AtomicLong();
                                    counters[i][zoneIndex].put(value, cnt);
                                }
                                cnt.incrementAndGet();
                            }
                        }
                    }
                }
            }

        } else {
            //todo : this approach is very basic, just as a proof of concept

            //prepare all zones as geometries
            //todo create a quadtree
            final Quadtree tree = new Quadtree();
            final int nbZones = zones.size();
            IntStream.range(0, nbZones).parallel().forEach(new IntConsumer() {
                @Override
                public void accept(int i) {
                    final Zone zone = dggh.getZone(zones.get(i));
                    final Polygon zoneGeom = DiscreteGlobalGridSystems.toJTSPolygon(zone.getGeographicExtent());
                    final org.locationtech.jts.geom.Envelope zenv = zoneGeom.getEnvelopeInternal();
                    synchronized(tree) {
                        tree.insert(zenv, new Object[]{zenv, zoneGeom, i});
                    }
                }
            });

            final Vector1D.Double buffer = new Vector1D.Double();
            try (Stream<Feature> stream = subset.features(false)) {
                final Iterator<Feature> fite = stream.iterator();
                zoneLoop:
                while (fite.hasNext()) {
                    final Feature feature = fite.next();
                    org.locationtech.jts.geom.Geometry geom = (org.locationtech.jts.geom.Geometry) feature.getPropertyValue(AttributeConvention.GEOMETRY);
                    org.locationtech.jts.geom.Envelope geomEnv = geom.getEnvelopeInternal();

                    if (true && trs != null) { //waiting for fix in SIS
                        geom = JTS.transform(geom, trs);
                        geomEnv = geom.getEnvelopeInternal();
                    }

                    final List<Object[]> candidates = tree.query(geomEnv);
                    for (Object[] entry : candidates) {
                        if (((Geometry)entry[1]).intersects(geom)) {
                            final int zoneIndex = (Integer) entry[2];

                            for (int i = 0; i < propertyNames.length; i++) {
                                Object value = feature.getPropertyValue(propertyNames[i]);
                                if (value instanceof Number n) {
                                    buffer.x = n.doubleValue();
                                    if (!Double.isNaN(buffer.x)) {
                                        if (stats[i][zoneIndex] == null) {
                                            stats[i][zoneIndex] = new Statistics("");
                                        }
                                        stats[i][zoneIndex].accept(buffer.x);
                                    }
                                    //fast method without stats
                                    //samples.get(i).set(zoneIndex, buffer);
                                }
                                if (counters[i][0] != null) {
                                    if (counters[i][zoneIndex] == null) counters[i][zoneIndex] = new HashMap<>();
                                    AtomicLong cnt = counters[i][zoneIndex].get(value);
                                    if (cnt == null) {
                                        cnt = new AtomicLong();
                                        counters[i][zoneIndex].put(value, cnt);
                                    }
                                    cnt.incrementAndGet();
                                }
                            }

                            //TODO we should make a 'fast mode case'
                            //remove this zone from futur searchs
                            //tree.remove((org.locationtech.jts.geom.Envelope) entry[0], entry[1]);
                            //continue zoneLoop; // a feature may intersect multiple zones
                        }
                    }
                }
            } catch (TransformException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }

        }

        //copy stats to buffers
        final Vector1D.Double buffer = new Vector1D.Double();
        for(int i = 0; i < stats.length; i++) {
            final Array arr = samples.get(i);
            for(int k = 0; k < stats[0].length; k++) {
                if (stats[i][k] != null) {
                    buffer.x = stats[i][k].mean();
                    arr.set(k, buffer);
                }
                //replace stat value by a counted value if it is one
                if (counters[i][k] != null) {
                    Entry<Object,AtomicLong> mostNumerous = null;
                    for (Entry<Object,AtomicLong> entry : counters[i][k].entrySet()) {
                        if (mostNumerous == null || mostNumerous.getValue().get() > entry.getValue().get()) {
                            mostNumerous = entry;
                        }
                    }
                    if (mostNumerous != null && mostNumerous.getKey() instanceof Number n) {
                        buffer.x = n.doubleValue();
                        arr.set(k, buffer);
                    }
                }
            }
        }

        return target;
    }

    /**
     * Resample a GridCoverage to a DiscreteGlobalGridCoverage.
     * The current sampling method use nearest neighbor.
     * This method assume the source coverage has a dimension representing the dggrs zones (ids).
     *
     * @param source
     * @param gridGeometry
     * @param range
     * @return
     * @throws DataStoreException
     */
    public CodedCoverage resampleZonalCoverage(GridCoverageResource source, DiscreteGlobalGridGeometry gridGeometry, int... range) throws DataStoreException, TransformException {
        final List<Object> zones = gridGeometry.getZoneIds();

        List<SampleDimension> sampleDimensions = source.getSampleDimensions();
        if (range != null && range.length != 0) {
            final List<SampleDimension> selected = new ArrayList<>();
            for (int i = 0; i < range.length; i++) {
                selected.add(sampleDimensions.get(range[i]));
            }
            sampleDimensions = selected;
        }

        final List<Array> samples = new ArrayList<>();
        final double[] nans = new double[sampleDimensions.size()];
        for (int i = 0; i < nans.length; i++) {
            final SampleSystem ss = new SampleSystem(DataType.DOUBLE, sampleDimensions.get(i));
            final double[] datas = new double[zones.size()];
            Arrays.fill(datas, Double.NaN);
            samples.add(NDArrays.of(ss, datas));
            nans[i] = Double.NaN;
        }

        final ArrayDiscreteGlobalGridCoverage target = new ArrayDiscreteGlobalGridCoverage(source.getIdentifier().get(), gridGeometry, samples);
        final CodeTransform gridToRS = gridGeometry.getGridToRS();
        final DiscreteGlobalGridReferenceSystem dggrs = gridGeometry.getReferenceSystem();
        final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();

        try (final WritableCodeIterator iterator = target.createWritableIterator()) {

            CoordinateReferenceSystem crs = source.getGridGeometry().getCoordinateReferenceSystem();
            CoordinateSystem cs = crs.getCoordinateSystem();
            int dimension = cs.getDimension();

            int cellDimensionId = -1;
            for (int dimIdx = 0; dimIdx < dimension; dimIdx++) {
                CoordinateSystemAxis csa = cs.getAxis(dimIdx);
                AxisDirection axisDirection = csa.getDirection();
                String abbreviation = csa.getAbbreviation().toLowerCase();
                DimensionNameType axisType = source.getGridGeometry().getExtent().getAxisType(dimIdx).orElse(null);

                if (cellDimensionId == -1 && (axisDirection == AxisDirection.COLUMN_POSITIVE || Arrays.asList(CELLS_RESOURCES_NAMES).contains(abbreviation))) {
                    cellDimensionId = dimIdx;
                }
            }

            if (cellDimensionId == -1) {
                throw new DataStoreException("No cell dimension found in the source raster resource.");
            }

            final Envelope env = source.getGridGeometry().getEnvelope();
            GeneralEnvelope env2 = new GeneralEnvelope(env);
            env2.setRange(0, env.getMinimum(0), env.getMinimum(0));
            final GridExtent extent = new GridExtent(source.getGridGeometry().getExtent().getHigh(0), 1L);
            final GridGeometry grid = new GridGeometry(extent, env2, GridOrientation.REFLECTION_Y);

            try {
                source.setLoadingStrategy(RasterLoadingStrategy.AT_GET_TILE_TIME);
                final GridCoverage coverage = source.read(grid);
                final GridCoverage.Evaluator evaluator = coverage.evaluator();
                evaluator.setNullIfOutside(true);

                while(iterator.next()) {
                    final int[] position = iterator.getPosition();
                    final Code code = gridToRS.toCode(position);
                    final Object zid = code.getOrdinate(0);
                    final Zone zone = coder.decode(zid);
                    final long cellId = zone.getLongIdentifier();

                    DirectPosition dp = new DirectPosition1D(cellId);

//                    DirectPosition dp;
//                    if (timeDimensionId != -1) {
//                        dp = new DirectPosition2D(coverage.getCoordinateReferenceSystem(), source.getEnvelope().orElseThrow().getMinimum(0), cellId);
//                    } else {
//                        dp = new DirectPosition1D(cellId);
//                    }

                    double[] values = evaluator.apply(dp);
                    if (values == null) {
                        values = nans;
                    }
                    iterator.setCell(values);
                }
            } catch (NoSuchDataException ex) {
                // do nothing
            }
        } catch (TransformException ex) {
            throw new DataStoreException(ex);
        }

        return target;
    }

    /**
     * Resample a DGGRS coverage to a different grid.
     *
     * @param coverage source coverage
     * @param gridGeometry target geometry
     * @param range bands to select
     * @return resamples coverage
     * @throws FactoryException
     * @throws DataStoreException
     * @throws TransformException
     */
    public CodedCoverage resample(CodedCoverage coverage, CodedGeometry query, int ... range) throws FactoryException, DataStoreException, TransformException {

        if (query instanceof DiscreteGlobalGridGeometry) {
            final DiscreteGlobalGridGeometry dggGrid = (DiscreteGlobalGridGeometry) query;
            if (coverage instanceof AddDimCodedCoverage ad) {
                //remove any extra slice information
                coverage = ad.getSource();
            }

            final DiscreteGlobalGridReferenceSystem dggrs = dggGrid.getReferenceSystem();
            final DiscreteGlobalGridHierarchy dggh = dggrs.getGridSystem().getHierarchy();
            final List<SampleDimension> sampleDimensions = coverage.getSampleDimensions();

            final CodeOperation op = ReferenceSystems.findOperation(dggrs, coverage.getGeometry().getReferenceSystem(), null);
            final CodeTransform toCode = dggGrid.getGridToRS();
            final CodedCoverage.CodeEvaluator eva = coverage.codeEvaluator();

            final ArrayDiscreteGlobalGridCoverage target = new ArrayDiscreteGlobalGridCoverage(Names.createLocalName(null, null, "Resampled"), dggGrid, sampleDimensions.toArray(SampleDimension[]::new));
            Code tcode = null;
            try (final WritableCodeIterator iterator = target.createWritableIterator()) {
                while (iterator.next()) {
                    final int[] gridPosition = iterator.getPosition();
                    final Code code = toCode.toCode(gridPosition);
                    if (!(code.getOrdinate(0) instanceof CharSequence)) {
                        code.setOrdinate(0, dggh.toTextIdentifier(code.getOrdinate(0)));
                    }
                    tcode = op.transform(code, tcode);
                    double[] values = eva.apply(tcode);
                    if (values != null) {
                        iterator.setCell(values);
                    }
                }
            }

            return target;
        }

        //general case
        final ReferenceSystem dggrs = query.getReferenceSystem();
        final List<SampleDimension> sampleDimensions = coverage.getSampleDimensions();
        final CodedCoverage target = new ArrayCodedCoverage(Names.createLocalName(null, null, "Resampled"), query, sampleDimensions.toArray(SampleDimension[]::new));

        final CodeOperation op = ReferenceSystems.findOperation(dggrs, coverage.getGeometry().getReferenceSystem(), null);
        final CodeTransform toCode = query.getGridToRS();
        final CodedCoverage.CodeEvaluator eva = coverage.codeEvaluator();

        Code tcode = null;
        try (final WritableCodeIterator iterator = target.createWritableIterator()) {
            while (iterator.next()) {
                final int[] gridPosition = iterator.getPosition();
                final Code code = toCode.toCode(gridPosition);
                tcode = op.transform(code, tcode);
                double[] values = eva.apply(tcode);
                if (values != null) {
                    iterator.setCell(values);
                }
            }
        }

        return target;
    }
}
