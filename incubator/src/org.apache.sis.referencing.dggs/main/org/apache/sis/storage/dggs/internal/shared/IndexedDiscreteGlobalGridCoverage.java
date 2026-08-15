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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Polygon;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.wrapper.jts.JTS;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.coverage.CoverageUtilities;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;
import org.apache.sis.storage.image.internal.ImageBuilder;
import org.apache.sis.storage.rs.CodeIterator;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class IndexedDiscreteGlobalGridCoverage extends AbstractDiscreteGlobalGridCoverage{

    protected final List<Object> zones;
    protected final Map<Object,Integer> index = new HashMap();

    public IndexedDiscreteGlobalGridCoverage(DiscreteGlobalGridGeometry gridGeometry) {
        super(gridGeometry);
        this.zones = this.gridGeometry.getZoneIds();

        //build an index
        //todo to remove, need something better then a List<Zone>
        for (int i = 0, n = zones.size(); i < n; i++) {
            index.put(zones.get(i), i);
        }
    }

    @Override
    public GridCoverage sample(GridGeometry fullArea, GridGeometry tileArea) throws CannotEvaluateException {

        try {
            final Quantity samplingResolution = GridAsDiscreteGlobalGridResource.computeAverageResolution(tileArea);
            final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();
            coder.setPrecisionLevel(coder.decode(zones.get(0)).getLocationType().getRefinementLevel());
            final Quantity coverageResolution = coder.getPrecision(null);
            final double coverageResM = coverageResolution.to(Units.METRE).getValue().doubleValue();
            final double samplingResM = samplingResolution.to(Units.METRE).getValue().doubleValue();

            //choose the most efficiant resampling strategy
            if (coverageResM > (4 * samplingResM)) {
                return sampleByMask(fullArea, tileArea);
            } else {
                return sampleByEvaluator(fullArea, tileArea);
            }
        } catch (TransformException | IncommensurableException ex) {
            return sampleByMask(fullArea, tileArea);
        }
    }

    private GridCoverage sampleByMask(GridGeometry fullArea, GridGeometry tileArea) throws CannotEvaluateException {
        final List<SampleDimension> sampleDimensions = getSampleDimensions();

        try {
            tileArea = CoverageUtilities.forceLowerToZero(tileArea);

            //start by creating a mask
            final GridExtent extent = tileArea.getExtent();
            final int width = Math.toIntExact(extent.getSize(0));
            final int height = Math.toIntExact(extent.getSize(1));
            final MathTransform gridToCRS = tileArea.getGridToCRS(PixelInCell.CELL_CENTER);
            final BufferedImage maskImage = new ImageBuilder()
                    .setSize(width, height)
                    .setNumBands(1)
                    .setDataType(DataBuffer.TYPE_BYTE)
                    .createBufferedImage();
            final Graphics2D g = maskImage.createGraphics();
            g.setColor(Color.WHITE);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            final WritableRaster maskRaster = maskImage.getRaster();
            final Rectangle imgRect = new Rectangle(width, height);
            final Area bbox = new Area(imgRect);

            final MathTransform zoneToCrs = CRS.findOperation(dggrs.getGridSystem().getCrs(), tileArea.getCoordinateReferenceSystem(), null).getMathTransform();
            final MathTransform zoneToGrid = MathTransforms.concatenate(zoneToCrs, gridToCRS.inverse());

            //prepare the coverage
            final double[] fillValue = new double[sampleDimensions.size()];
            Arrays.fill(fillValue, Double.NaN);
            final BufferedImage image = new ImageBuilder()
                    .setSize(width, height)
                    .setNumBands(sampleDimensions.size())
                    .setDataType(DataBuffer.TYPE_DOUBLE)
                    .setFillValue(fillValue)
                    .createBufferedImage();
            final WritableRaster raster = image.getRaster();

            final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();
            final CodeIterator zoneIterator = createIterator();
            double[] cell = null;
            final long nanLong = Double.doubleToRawLongBits(Double.NaN);
            while (zoneIterator.next()) {

                //check if we have some data
                cell = zoneIterator.getCell(cell);
                boolean allNaN = true;
                for (double d : cell) {
                    if (Double.doubleToRawLongBits(d) != nanLong) {
                        //need exact NaN test, multiple NaN may be used, we skip only this one
                        allNaN = false;
                    }
                }
                if (allNaN) continue;


                final int[] position = zoneIterator.getPosition();
                final Object zid = zones.get(position[0]);
                final Zone zone = coder.decode(zid);
                final Polygon polygon = DiscreteGlobalGridSystems.toJTSPolygon(zone.getGeographicExtent());
                final Shape shape = JTS.asShape(JTS.transform(polygon, zoneToGrid));
                final Area area = new Area(shape);
                area.intersect(bbox);
                if (!area.isEmpty()) {
                    g.fill(area);
                    Rectangle bounds = area.getBounds();

                    //fill the corresponding pixel in the target
                    final PixelIterator rite = new PixelIterator.Builder().setRegionOfInterest(bounds).create(maskImage);
                    while (rite.next()) {
                        if (rite.getSample(0) != 0) {
                            Point pt = rite.getPosition();
                            raster.setPixel(pt.x, pt.y, cell);
                            maskRaster.setSample(pt.x, pt.y, 0, 0);
                        }
                    }
                }
            }

            g.dispose();

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
        return new Eval();
    }

    private final class Eval implements Evaluator {

        private final DiscreteGlobalGridReferenceSystem.Coder coder;
        private boolean nullIfOutside = false;
        private boolean wraparoundEnabled = false;
        private final CodeIterator iterator;

        public Eval() {
            coder = dggrs.createCoder();
            iterator = createIterator();
            try {
                coder.setPrecisionLevel(IndexedDiscreteGlobalGridCoverage.this.getGeometry().getRefinementLevel());
            } catch (IncommensurableException ex) {
                //should not happen since the geometry has been created
                throw new RuntimeException(ex);
            }
        }

        @Override
        public IndexedDiscreteGlobalGridCoverage getCoverage() {
            return IndexedDiscreteGlobalGridCoverage.this;
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
                try {
                    final Object zoneId = coder.encodeIdentifier(dp);
                    final Integer idx = index.get(zoneId);
                    if (idx != null) {
                        iterator.moveTo(new int[]{idx});
                        return iterator.getCell((double[])null);
                    }
                } catch (IllegalArgumentException ex) {
                    //coordinate outside dggrs supported area
                }
            } catch (TransformException ex) {
                throw new CannotEvaluateException(ex.getMessage(), ex);
            }

            if (nullIfOutside) {
                return null;
            } else {
                throw new PointOutsideCoverageException();
            }
        }

    }
}
