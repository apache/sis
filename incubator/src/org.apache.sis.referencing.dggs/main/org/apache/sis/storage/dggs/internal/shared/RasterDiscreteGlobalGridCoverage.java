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

import java.awt.Point;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.function.Function;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.image.WritablePixelIterator;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.WritableCodeIterator;


/**
 * Discrete global grid coverage with data stored in a raster image.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class RasterDiscreteGlobalGridCoverage extends IndexedDiscreteGlobalGridCoverage{

    private final GenericName name;
    private final List<SampleDimension> sampleDimensions;
    private final RenderedImage samples;
    private final Function<Object,Point> zoneToGrid;
    /**
     * @todo not used yet, but will be.
     */
    private final Function<Point,Object> gridToZone;

    public RasterDiscreteGlobalGridCoverage(
            GenericName name,
            DiscreteGlobalGridGeometry geometry,
            List<SampleDimension> sampleDimensions,
            RenderedImage samples,
            Function<Object,Point> zoneToGrid,
            Function<Point,Object> gridToZone) {
        super(geometry);
        this.name = name;
        this.sampleDimensions = sampleDimensions;
        this.samples = samples;
        this.zoneToGrid = zoneToGrid;
        this.gridToZone = gridToZone;

        if (samples.getWidth() * samples.getHeight() != zones.size()) {
            throw new IllegalArgumentException("Number of image pixel do not match number of cells");
        }

    }

    @Override
    public CodeIterator createIterator() {
        return new Iterator(false);
    }

    @Override
    public WritableCodeIterator createWritableIterator() {
        return new Iterator(true);
    }

    @Override
    public List<SampleDimension> getSampleDimensions() {
        return sampleDimensions;
    }

    private final class Iterator implements WritableCodeIterator {

        private int position = -1;

        private final PixelIterator cursor;
        private final DiscreteGlobalGridReferenceSystem.Coder coder;

        public Iterator(boolean writable) {
            coder = dggrs.createCoder();
            if (writable) {
                cursor = WritablePixelIterator.create(samples);
            } else {
                cursor = PixelIterator.create(samples);
            }
        }

        @Override
        public void setSample(int band, double value) {
            final Point pt = zoneToGrid.apply(zones.get(position));
            cursor.moveTo(pt.x, pt.y);
            ((WritablePixelIterator)cursor).setSample(band, value);
        }

        @Override
        public int getNumBands() {
            return cursor.getNumBands();
        }

        @Override
        public int[] getPosition() {
            return new int[]{position};
        }

        public Object getZoneId() {
            return zones.get(position);
        }

        public Zone getZone() throws TransformException {
            return coder.decode(zones.get(position));
        }

        public void moveTo(Object zid) {
            Integer idx = index.get(zid);
            if (idx == null) {
                throw new IllegalArgumentException("Zone " + zid +" is not part of this coverage");
            }
            position = idx;
        }

        @Override
        public void moveTo(int[] zid) {
            if (zid[0] >= 0 && zid[0] < zones.size()) {
                position = zid[0];
            } else {
                throw new IllegalArgumentException("Zone " + zid[0] +" is not part of this coverage");
            }
        }

        @Override
        public boolean next() {
            if (position < zones.size()-1) {
                position ++;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public double getSampleDouble(int band) {
            final Point pt = zoneToGrid.apply(zones.get(position));
            cursor.moveTo(pt.x, pt.y);
            return cursor.getSampleDouble(band);
        }

        @Override
        public void rewind() {
            position = -1;
        }

        @Override
        public void close() throws DataStoreException {
        }

    }

}
