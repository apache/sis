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
package org.apache.sis.internal.feature.jts;

import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.WIND_NON_ZERO;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.referencing.operation.MathTransform;

/**
 * Decimating Java2D path iterators for JTS geometries.
 *
 * @author Johann Sorel (Puzzle-GIS + Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
final class DecimateJTSPathIterator {

    static final class LineString extends JTSPathIterator<org.locationtech.jts.geom.LineString> {

        private final CoordinateSequence coordinates;
        private final int coordinateCount;
        /**
         * True if the line is a ring
         */
        private final boolean isClosed;
        private int lastCoord;
        private int currentIndex;
        private boolean done;
        private final double[] resolution;
        private final double[] currentCoord = new double[2];

        /**
         * Create a new LineString path iterator.
         */
        public LineString(final org.locationtech.jts.geom.LineString ls, final MathTransform trs, final double[] resolution) {
            super(ls, trs);
            coordinates = ls.getCoordinateSequence();
            coordinateCount = coordinates.size();
            isClosed = ls instanceof LinearRing;
            this.resolution = resolution;
            currentCoord[0] = coordinates.getX(0);
            currentCoord[1] = coordinates.getY(0);
        }

        @Override
        public void reset() {
            done = false;
            currentIndex = 0;
            currentCoord[0] = coordinates.getX(0);
            currentCoord[1] = coordinates.getY(0);
        }

        @Override
        public int getWindingRule() {
            return WIND_NON_ZERO;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public void next() {

            while (true) {
                if (((currentIndex == (coordinateCount - 1)) && !isClosed)
                 || ((currentIndex == coordinateCount) && isClosed)) {
                    done = true;
                    break;
                }

                currentIndex++;
                double candidateX = coordinates.getX(currentIndex);
                double candidateY = coordinates.getY(currentIndex);

                if (Math.abs(candidateX - currentCoord[0]) >= resolution[0] || Math.abs(candidateY - currentCoord[1]) >= resolution[1]) {
                    currentCoord[0] = candidateX;
                    currentCoord[1] = candidateY;
                    break;
                }

            }
        }

        @Override
        public int currentSegment(final double[] coords) {
            if (currentIndex == 0) {
                safeTransform(currentCoord, 0, coords, 0, 1);
                return SEG_MOVETO;
            } else if ((currentIndex == coordinateCount) && isClosed) {
                return SEG_CLOSE;
            } else {
                safeTransform(currentCoord, 0, coords, 0, 1);
                return SEG_LINETO;
            }
        }

        @Override
        public int currentSegment(final float[] coords) {
            if (currentIndex == 0) {
                safeTransform(currentCoord, 0, coords, 0, 1);
                return SEG_MOVETO;
            } else if ((currentIndex == coordinateCount) && isClosed) {
                return SEG_CLOSE;
            } else {
                safeTransform(currentCoord, 0, coords, 0, 1);
                return SEG_LINETO;
            }
        }
    }

    static final class GeometryCollection extends JTSPathIterator.GeometryCollection {

        private final double[] resolution;

        public GeometryCollection(final org.locationtech.jts.geom.GeometryCollection gc, final MathTransform trs, final double[] resolution) {
            super(gc, trs);
            this.resolution = resolution;
            reset();
        }

        /**
         * Returns the specific iterator for the geometry passed.
         *
         * @param candidate The geometry whole iterator is requested
         *
         */
        @Override
        protected void prepareIterator(final Geometry candidate) {
            if (candidate.isEmpty()) {
                currentIterator = JTSPathIterator.Empty.INSTANCE;
            } else if (candidate instanceof org.locationtech.jts.geom.Point) {
                currentIterator = new JTSPathIterator.Point((org.locationtech.jts.geom.Point) candidate, transform);
            } else if (candidate instanceof org.locationtech.jts.geom.Polygon) {
                currentIterator = new JTSPathIterator.Polygon((org.locationtech.jts.geom.Polygon) candidate, transform);
            } else if (candidate instanceof org.locationtech.jts.geom.LineString) {
                currentIterator = new DecimateJTSPathIterator.LineString((org.locationtech.jts.geom.LineString) candidate, transform, resolution);
            } else if (candidate instanceof org.locationtech.jts.geom.GeometryCollection) {
                currentIterator = new DecimateJTSPathIterator.GeometryCollection((org.locationtech.jts.geom.GeometryCollection) candidate, transform, resolution);
            } else {
                currentIterator = JTSPathIterator.Empty.INSTANCE;
            }
        }
    }
}
