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

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.measure.IncommensurableException;
import org.apache.sis.coverage.BandedCoverage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.image.internal.ImageBuilder;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 * Referenced Coverage backed by a list of samples stored in TupleArrays.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractCodedCoverage extends CodedCoverage{

    protected final GenericName name;
    protected final CodedGeometry gridGeometry;
    protected final GridExtent extent;
    protected final int dimension;
    //todo allow other kind of base reference system in the futur
    protected final DiscreteGlobalGridReferenceSystem horizontalRs;
    protected final DiscreteGlobalGridGeometry horizontalGrid;
    protected final Map<Object,Integer> index = new HashMap();
    protected final ReferenceSystem rs;
    protected final List<ReferenceSystem> singleRS;
    protected final CoordinateReferenceSystem compoundCrs;
    protected final int horizontalRsIndex;
    /**
     * Store the step between each cell for each dimension.
     * At the last dimension, the step size is 1.
     */
    protected final long[] dimStep;
    /**
     * Store the size of each dimension
     */
    protected final long[] dimSize;
    /**
     * Store the offset of each dimension
     */
    protected final long[] dimOffsets;

    //samples
    protected final List<SampleDimension> sampleDimensions;

    public AbstractCodedCoverage(final GenericName name, CodedGeometry gridGeometry, List<SampleDimension> sampleDimensions) throws FactoryException {
        this.name = name;
        this.gridGeometry = gridGeometry;
        this.extent = gridGeometry.getExtent();
        this.dimension = extent.getDimension();
        this.rs = gridGeometry.getReferenceSystem();
        this.horizontalRs = ReferenceSystems.getHorizontalComponent(rs).map(DiscreteGlobalGridReferenceSystem.class::cast).orElseThrow();
        this.horizontalGrid = (DiscreteGlobalGridGeometry) gridGeometry.slice(horizontalRs).get();
        this.singleRS = ReferenceSystems.getSingleComponents(rs, true);
        this.horizontalRsIndex = singleRS.indexOf(horizontalRs);
        this.sampleDimensions = sampleDimensions;

        //compute equivalent CRS
        final List<CoordinateReferenceSystem> ccrs = new ArrayList<>();
        for (int i = 0, n = singleRS.size(); i < n ;i++) {
            if (i == horizontalRsIndex) {
                ccrs.add(horizontalRs.getGridSystem().getCrs());
            } else {
                ccrs.add((CoordinateReferenceSystem) singleRS.get(i));
            }
        }
        compoundCrs = CRS.compound(ccrs.toArray(CoordinateReferenceSystem[]::new));

        //compute size of each dimension
        dimSize = new long[dimension];
        dimOffsets = new long[dimension];
        for (int i = 0; i < dimSize.length; i++) {
            dimSize[i] = extent.getSize(i);
            dimOffsets[i] = extent.getLow(i);
        }
        dimStep = new long[dimension];
        dimStep[dimension-1] = 1;
        for (int i = dimension - 2; i >= 0; i--) {
            dimStep[i] = dimStep[i+1] * dimSize[i+1];
        }

        //build an index
        //todo to remove, need something better then a List<Zone>
        final List<Object> zones = this.horizontalGrid.getZoneIds();
        for (int i = 0, n = zones.size(); i < n; i++) {
            index.put(zones.get(i), i);
        }
    }

    @Override
    public List<SampleDimension> getSampleDimensions() {
        return Collections.unmodifiableList(sampleDimensions);
    }

    @Override
    public CodedGeometry getGeometry() {
        return gridGeometry;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return compoundCrs;
    }

    @Override
    public Optional<Envelope> getEnvelope() {
        return Optional.ofNullable(gridGeometry.getEnvelope());
    }

    @Override
    public double[] getResolution(boolean allowEstimate) {
        return gridGeometry.getResolution(allowEstimate);
    }

    @Override
    public GridCoverage sample(GridGeometry fullArea, GridGeometry tileArea) throws CannotEvaluateException {
        final List<SampleDimension> sampleDimensions = getSampleDimensions();

        try {
            final GridExtent extent = tileArea.getExtent();
            final int width = Math.toIntExact(extent.getSize(0));
            final int height = Math.toIntExact(extent.getSize(1));
            final long lowX = extent.getLow(0);
            final long lowY = extent.getLow(1);
            final MathTransform gridToCRS = tileArea.getGridToCRS(PixelInCell.CELL_CENTER);
            final BufferedImage image = new ImageBuilder()
                    .setSize(width, height)
                    .setNumBands(sampleDimensions.size())
                    .setDataType(DataBuffer.TYPE_DOUBLE)
                    .createBufferedImage();
            final WritableRaster raster = image.getRaster();

            // Verify no overflow is possible before allocating any array
            final int nbPts = Math.multiplyExact(width, height);
            final int xyLength = Math.multiplyExact(nbPts, 2);
            final double[] xyGrid = new double[xyLength];
            for (int y=0;y<height;y++) {
                for (int x=0;x<width;x++) {
                    int idx = (y * width + x) * 2;
                    xyGrid[idx] = lowX + x;
                    xyGrid[idx+1] = lowY + y;
                }
            }

            //convert to crs
            final double[] xyTin;
            final CoordinateReferenceSystem crs2d = CRS.getHorizontalComponent(getCoordinateReferenceSystem());
            final CoordinateReferenceSystem gridCrs2d = CRS.getHorizontalComponent(tileArea.getCoordinateReferenceSystem());
            if (!CRS.equivalent(gridCrs2d, crs2d)) {
                MathTransform trs = CRS.findOperation(gridCrs2d, crs2d, null).getMathTransform();
                trs = MathTransforms.concatenate(gridToCRS, trs);
                xyTin = xyGrid;
                trs.transform(xyTin, 0, xyTin, 0, xyTin.length/2);
            } else {
                gridToCRS.transform(xyGrid, 0, xyGrid, 0, xyGrid.length/2);
                xyTin = xyGrid;
            }

            final Evaluator evaluator = evaluator();
            evaluator.setNullIfOutside(true);
            final double[] none = new double[raster.getNumBands()];
            Arrays.fill(none, Double.NaN);

            final ThreadLocal<Evaluator> evaluators = ThreadLocal.withInitial(() -> {
                Evaluator eval = evaluator();
                eval.setNullIfOutside(true);
                return eval;
            });

            IntStream.range(0, xyTin.length/2).parallel().forEach((int i) -> {
                int imgx = i % width;
                int imgy = i / width;
                final DirectPosition2D dp = new DirectPosition2D();
                dp.x = xyTin[i*2];
                dp.y = xyTin[i*2+1];
                final double[] sample = evaluators.get().apply(dp);
                if (sample != null) {
                    raster.setPixel(imgx, imgy, sample);
                } else {
                    raster.setPixel(imgx, imgy, none);
                }
            });
            return new GridCoverageBuilder()
                    .setDomain(tileArea)
                    .setRanges(getSampleDimensions())
                    .setValues(raster)
                    .build();
        } catch (TransformException | FactoryException ex) {
            throw new CannotEvaluateException(ex.getMessage(), ex);
        }
    }

    @Override
    public Evaluator evaluator() {
        return new CoverageEvaluator();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.name);
        hash = 59 * hash + Objects.hashCode(this.gridGeometry);
        hash = 59 * hash + Objects.hashCode(this.sampleDimensions);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractCodedCoverage other = (AbstractCodedCoverage) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.gridGeometry, other.gridGeometry)) {
            return false;
        }
        return Objects.equals(this.sampleDimensions, other.sampleDimensions);
    }

    private final class CoverageEvaluator implements Evaluator {

        private final DiscreteGlobalGridReferenceSystem.Coder coder;
        private boolean nullIfOutside = false;
        private boolean wraparoundEnabled = false;
        private final CodeIterator iterator;

        //cache last transforms
        private CoordinateReferenceSystem lastPosCrs;
        private MathTransform[] lastPosTransform;

        public CoverageEvaluator() {
            coder = horizontalRs.createCoder();
            iterator = createIterator();
            lastPosTransform = new MathTransform[singleRS.size()];
            try {
                coder.setPrecisionLevel(AbstractCodedCoverage.this.horizontalGrid.getRefinementLevel());
            } catch (IncommensurableException ex) {
                //should not happen since the geometry has been created
                throw new RuntimeException(ex);
            }
        }

        @Override
        public BandedCoverage getCoverage() {
            return AbstractCodedCoverage.this;
        }

        @Override
        public boolean isNullIfOutside() {
            return nullIfOutside;
        }

        @Override
        public void setNullIfOutside(boolean flag) {
            this.nullIfOutside = flag;
        }

        @Override
        public boolean isWraparoundEnabled() {
            return wraparoundEnabled;
        }

        @Override
        public void setWraparoundEnabled(boolean allow) {
            this.wraparoundEnabled = allow;
        }

        @Override
        public double[] apply(DirectPosition dp) throws CannotEvaluateException {
            try {
                double[] cell = apply2(dp);
                if (cell != null) return cell;
            } catch (TransformException ex) {
                throw new CannotEvaluateException(ex.getMessage(), ex);
            }

            if (nullIfOutside) {
                return null;
            } else {
                throw new PointOutsideCoverageException();
            }
        }

        private double[] apply2(DirectPosition dp) throws CannotEvaluateException, TransformException {
            updateSourceTransform(dp.getCoordinateReferenceSystem());
            final double[] coordinates = dp.getCoordinates();

            final Object zoneId;
            try {
                zoneId = coder.encodeIdentifier(dp);
            } catch (IllegalArgumentException ex) {
                //coordinate outside dggrs supported area
                return null;
            }
            final Integer horIdx = index.get(zoneId);
            if (horIdx != null) {
                //compute the other dimensions
                final int[] gridPosition = new int[singleRS.size()];
                for (int i = 0; i < singleRS.size(); i++) {
                    if (i == horizontalRsIndex) {
                        gridPosition[i] = horIdx;
                    } else if (lastPosTransform[i] != null) {
                        final double[] target = new double[1];
                        lastPosTransform[i].transform(coordinates, 0, target, 0, 1);
                        gridPosition[i] = (int) Math.round(target[0]);
                        if (gridPosition[i] < 0 || gridPosition[i] > extent.getHigh(i)) {
                            //outside grid
                            return null;
                        }
                    }
                }

                //get cell value
                iterator.moveTo(gridPosition);
                final double[] cell = iterator.getCell((double[])null);
                return cell;
            }
            return null;
        }

        private void updateSourceTransform(CoordinateReferenceSystem source) {
            if (lastPosCrs == source) return;

            lastPosTransform = new MathTransform[singleRS.size()];
            for (int i = 0; i < singleRS.size(); i++) {
                if (i != horizontalRsIndex) {
                    try {
                        MathTransform trs = CRS.findOperation(source, (CoordinateReferenceSystem) singleRS.get(i), null).getMathTransform();
                        lastPosTransform[i] = trs;
                    } catch (FactoryException ex) {
                    }
                }
            }
            lastPosCrs = source;
        }

    }

}
