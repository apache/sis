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

import java.util.Arrays;
import java.util.List;
import org.apache.sis.coverage.BandedCoverage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TiledDiscreteGlobalGridCoverage extends AbstractDiscreteGlobalGridCoverage {

    private final CodedCoverage[] tiles;

    public TiledDiscreteGlobalGridCoverage(CodedCoverage[] tiles) throws TransformException {
        super(aggregateGeometries(tiles));
        this.tiles = tiles;
    }

    @Override
    public CodeIterator createIterator() {
        return new TiledIterator();
    }

    @Override
    public WritableCodeIterator createWritableIterator() {
        return new WritableTiledIterator();
    }

    @Override
    public List<SampleDimension> getSampleDimensions() {
        return tiles[0].getSampleDimensions();
    }

    @Override
    public Evaluator evaluator() {
        return new TiledEvaluator();
    }

    private static DiscreteGlobalGridGeometry aggregateGeometries(CodedCoverage[] tiles) throws TransformException {
        DiscreteGlobalGridReferenceSystem dggrs = null;
        final Object[] parentZoneIds = new Object[tiles.length];
        Integer relativeDepth = null;
        Integer baseDepth = null;
        for (int i = 0; i < tiles.length; i++) {
            final DiscreteGlobalGridGeometry geometry = (DiscreteGlobalGridGeometry) tiles[i].getGeometry();
            Object[] baseZoneIds = geometry.getBaseZoneIds();
            if (baseZoneIds == null || baseZoneIds.length != 1) {
                throw new IllegalArgumentException("Tile must have a single parent zone identifier");
            }
            parentZoneIds[i] = baseZoneIds[0];

            if (dggrs == null) {
                dggrs = geometry.getReferenceSystem();
            } else if (dggrs != geometry.getReferenceSystem()){
                throw new IllegalArgumentException("Tiles must have the same DGGRS");
            }

            if (baseDepth == null) {
                baseDepth = dggrs.getGridSystem().getHierarchy().getZone(baseZoneIds[0]).getLocationType().getRefinementLevel();
            } else if (baseDepth != dggrs.getGridSystem().getHierarchy().getZone(baseZoneIds[0]).getLocationType().getRefinementLevel()){
                throw new IllegalArgumentException("Tiles must have the same depth");
            }

            if (relativeDepth == null) {
                relativeDepth = geometry.getRelativeDepth();
            } else if (relativeDepth != geometry.getRelativeDepth()){
                throw new IllegalArgumentException("Tiles must have the same relative depth");
            }
        }

        return DiscreteGlobalGridGeometry.subZones(dggrs, parentZoneIds, relativeDepth);
    }

    private final class TiledIterator implements CodeIterator {

        private final CodeIterator[] iterators;
        private final int[] offsets;
        private int size;

        private CodeIterator iterator;
        private int position = -1;

        private TiledIterator() {
            this.iterators = new CodeIterator[tiles.length];
            this.offsets = new int[tiles.length];
            int count = 0;
            for (int i = 0; i < iterators.length; i++) {
                iterators[i] = tiles[i].createIterator();
                offsets[i] = count;
                count += ((DiscreteGlobalGridGeometry)tiles[i].getGeometry()).getZoneIds().size();
            }
            this.size = count;
        }

        private void updateIterator() {
            if (position < 0 || position >= size) {
                iterator = null;
            } else {
                int idx = Arrays.binarySearch(offsets, position);
                if (idx < 0) {
                    idx = -(idx +1);
                    // use the previous iterator
                    idx--;
                }
                iterator = iterators[idx];
                iterator.moveTo(new int[]{position - offsets[idx]});
            }
        }

        @Override
        public int getNumBands() {
            return getSampleDimensions().size();
        }

        @Override
        public double getSampleDouble(int band) {
            updateIterator();
            return iterator.getSampleDouble(band);
        }

        @Override
        public int[] getPosition() {
            return new int[]{position};
        }

        @Override
        public void moveTo(int[] zid) {
            this.position = zid[0];
            iterator = null;
        }

        @Override
        public boolean next() {
            if (position+1 >= size) return false;
            position++;
            iterator = null;
            return true;
        }

        @Override
        public void rewind() {
            position = -1;
            iterator = null;
        }

    }

    private final class WritableTiledIterator implements WritableCodeIterator {

        private final WritableCodeIterator[] iterators;
        private final int[] offsets;
        private int size;

        private WritableCodeIterator iterator;
        private int position = -1;

        private WritableTiledIterator() {
            this.iterators = new WritableCodeIterator[tiles.length];
            this.offsets = new int[tiles.length];
            int count = 0;
            for (int i = 0; i < iterators.length; i++) {
                iterators[i] = tiles[i].createWritableIterator();
                offsets[i] = count;
                count += ((DiscreteGlobalGridGeometry)tiles[i].getGeometry()).getZoneIds().size();
            }
            this.size = count;
        }

        private void updateIterator() {
            if (position < 0 || position >= size) {
                iterator = null;
            } else {
                int idx = Arrays.binarySearch(offsets, position);
                if (idx < 0) {
                    idx = -(idx +1);
                    // use the previous iterator
                    idx--;
                }
                iterator = iterators[idx];
                iterator.moveTo(new int[]{position - offsets[idx]});
            }
        }

        @Override
        public int getNumBands() {
            return getSampleDimensions().size();
        }

        @Override
        public double getSampleDouble(int band) {
            updateIterator();
            return iterator.getSampleDouble(band);
        }

        @Override
        public int[] getPosition() {
            return new int[]{position};
        }

        @Override
        public void moveTo(int[] zid) {
            this.position = zid[0];
            iterator = null;
        }

        @Override
        public boolean next() {
            if (position+1 >= size) return false;
            position++;
            iterator = null;
            return true;
        }

        @Override
        public void rewind() {
            position = -1;
            iterator = null;
        }

        @Override
        public void setSample(int band, double value) {
            updateIterator();
            iterator.setSample(band, value);
        }

        @Override
        public void close() throws DataStoreException {
            for (WritableCodeIterator i : iterators) {
                i.close();
            }
        }

    }

    private final class TiledEvaluator implements Evaluator {

        private boolean nullIfOutside = false;
        private boolean wraparoundEnabled = false;

        private final Evaluator[] evaluators;

        public TiledEvaluator() {
            this.evaluators = new Evaluator[tiles.length];
            for (int i = 0; i < evaluators.length; i++) {
                evaluators[i] = tiles[i].evaluator();
                evaluators[i].setNullIfOutside(true);
            }
        }

        @Override
        public BandedCoverage getCoverage() {
            return TiledDiscreteGlobalGridCoverage.this;
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
        public double[] apply(DirectPosition point) throws CannotEvaluateException {
            for (Evaluator e : evaluators) {
                double[] r = e.apply(point);
                if (r != null) return r;
            }
            if (nullIfOutside) return null;
            throw new PointOutsideCoverageException();
        }

    }

}
