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

import org.locationtech.jts.geom.Geometry;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.WIND_EVEN_ODD;
import static java.awt.geom.PathIterator.WIND_NON_ZERO;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.util.logging.Logging;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Abstract Java2D path iterator for JTS Geometry.
 *
 * @author Johann Sorel (Puzzle-GIS + Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
abstract class JTSPathIterator<T extends Geometry> implements PathIterator {

    private static final Logger LOGGER = Logging.getLogger("org.apache.sis.internal.feature.jts");
    static final AffineTransform2D IDENTITY = new AffineTransform2D(1, 0, 0, 1, 0, 0);

    protected MathTransform transform;
    protected T geometry;

    protected JTSPathIterator(final MathTransform trs) {
        this(null, trs);
    }

    protected JTSPathIterator(final T geometry, final MathTransform trs) {
        this.transform = (trs == null) ? IDENTITY : trs;
        this.geometry = geometry;
    }

    public void setGeometry(final T geom) {
        this.geometry = geom;
    }

    public void setTransform(final MathTransform trs) {
        this.transform = (trs == null) ? IDENTITY : trs;
        reset();
    }

    public MathTransform getTransform() {
        return transform;
    }

    public T getGeometry() {
        return geometry;
    }

    public abstract void reset();

    protected void safeTransform(float[] in, int offset, float[] out, int outOffset, int nb) {
        try {
            transform.transform(in, offset, out, outOffset, nb);
        } catch (TransformException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            Arrays.fill(out, outOffset, outOffset + nb * 2, Float.NaN);
        }
    }

    protected void safeTransform(double[] in, int offset, float[] out, int outOffset, int nb) {
        try {
            transform.transform(in, offset, out, outOffset, nb);
        } catch (TransformException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            Arrays.fill(out, outOffset, outOffset + nb * 2, Float.NaN);
        }
    }

    protected void safeTransform(double[] in, int offset, double[] out, int outOffset, int nb) {
        try {
            transform.transform(in, offset, out, outOffset, nb);
        } catch (TransformException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            Arrays.fill(out, outOffset, outOffset + nb * 2, Double.NaN);
        }
    }

    static final class Empty extends JTSPathIterator<Geometry> {

        public static final Empty INSTANCE = new Empty();

        private Empty() {
            super(null, null);
        }

        @Override
        public int getWindingRule() {
            return WIND_NON_ZERO;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public void next() {
            throw new IllegalStateException();
        }

        @Override
        public int currentSegment(final double[] coords) {
            return 0;
        }

        @Override
        public int currentSegment(final float[] coords) {
            return 0;
        }

        @Override
        public void reset() {
        }
    }

    static final class Point extends JTSPathIterator<org.locationtech.jts.geom.Point> {

        private boolean done;

        /**
         * Create a new Point path iterator.
         */
        public Point(final org.locationtech.jts.geom.Point point, final MathTransform trs) {
            super(point, trs);
        }

        @Override
        public int getWindingRule() {
            return WIND_EVEN_ODD;
        }

        @Override
        public void next() {
            done = true;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public int currentSegment(final double[] coords) {
            coords[0] = geometry.getX();
            coords[1] = geometry.getY();
            safeTransform(coords, 0, coords, 0, 1);
            return SEG_MOVETO;
        }

        @Override
        public int currentSegment(final float[] coords) {
            coords[0] = (float) geometry.getX();
            coords[1] = (float) geometry.getY();
            safeTransform(coords, 0, coords, 0, 1);
            return SEG_MOVETO;
        }

        @Override
        public void reset() {
            done = false;
        }
    }

    static final class LineString extends JTSPathIterator<org.locationtech.jts.geom.LineString> {

        private CoordinateSequence coordinates;
        private int coordinateCount;
        /**
         * True if the line is a ring
         */
        private boolean isClosed;
        private int currentCoord;
        private boolean done;

        /**
         * Create a new LineString path iterator.
         */
        public LineString(final org.locationtech.jts.geom.LineString ls, final MathTransform trs) {
            super(ls, trs);
            setGeometry(ls);
        }

        @Override
        public void setGeometry(final org.locationtech.jts.geom.LineString geom) {
            super.setGeometry(geom);
            if (geom != null) {
                coordinates = geom.getCoordinateSequence();
                coordinateCount = coordinates.size();
                isClosed = geom instanceof LinearRing;
            }
            reset();
        }

        @Override
        public void reset() {
            done = false;
            currentCoord = 0;
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
            if (((currentCoord == (coordinateCount - 1)) && !isClosed)
                    || ((currentCoord == coordinateCount) && isClosed)) {
                done = true;
            } else {
                currentCoord++;
            }
        }

        @Override
        public int currentSegment(final double[] coords) {
            if (currentCoord == 0) {
                coords[0] = coordinates.getX(0);
                coords[1] = coordinates.getY(0);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_MOVETO;
            } else if ((currentCoord == coordinateCount) && isClosed) {
                return SEG_CLOSE;
            } else {
                coords[0] = coordinates.getX(currentCoord);
                coords[1] = coordinates.getY(currentCoord);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_LINETO;
            }
        }

        @Override
        public int currentSegment(final float[] coords) {
            if (currentCoord == 0) {
                coords[0] = (float) coordinates.getX(0);
                coords[1] = (float) coordinates.getY(0);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_MOVETO;
            } else if ((currentCoord == coordinateCount) && isClosed) {
                return SEG_CLOSE;
            } else {
                coords[0] = (float) coordinates.getX(currentCoord);
                coords[1] = (float) coordinates.getY(currentCoord);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_LINETO;
            }
        }
    }

    static final class Polygon extends JTSPathIterator<org.locationtech.jts.geom.Polygon> {

        /**
         * The rings describing the polygon geometry
         */
        private org.locationtech.jts.geom.LineString[] rings;
        /**
         * The current ring during iteration
         */
        private int currentRing;
        /**
         * Current line coordinate
         */
        private int currentCoord;
        /**
         * The array of coordinates that represents the line geometry
         */
        private CoordinateSequence coords;
        private int csSize;
        /**
         * True when the iteration is terminated
         */
        private boolean done;

        /**
         * Create a new Polygon path iterator.
         */
        public Polygon(final org.locationtech.jts.geom.Polygon p, final MathTransform trs) {
            super(p, trs);
            setGeometry(p);
        }

        @Override
        public void setGeometry(final org.locationtech.jts.geom.Polygon geom) {
            this.geometry = geom;
            if (geom != null) {
                int numInteriorRings = geom.getNumInteriorRing();
                rings = new org.locationtech.jts.geom.LineString[numInteriorRings + 1];
                rings[0] = geom.getExteriorRing();

                for (int i = 0; i < numInteriorRings; i++) {
                    rings[i + 1] = geom.getInteriorRingN(i);
                }
            }
            reset();
        }

        @Override
        public void reset() {
            currentRing = 0;
            currentCoord = 0;
            coords = rings[0].getCoordinateSequence();
            csSize = coords.size() - 1;
            done = false;
        }

        @Override
        public int currentSegment(final double[] coords) {
            // first make sure we're not at the last element, this prevents us from exceptions
            // in the case where coords.size() == 0
            if (currentCoord == csSize) {
                return SEG_CLOSE;
            } else if (currentCoord == 0) {
                coords[0] = this.coords.getX(0);
                coords[1] = this.coords.getY(0);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_MOVETO;
            } else {
                coords[0] = this.coords.getX(currentCoord);
                coords[1] = this.coords.getY(currentCoord);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_LINETO;
            }
        }

        @Override
        public int currentSegment(final float[] coords) {
            // first make sure we're not at the last element, this prevents us from exceptions
            // in the case where coords.size() == 0
            if (currentCoord == csSize) {
                return SEG_CLOSE;
            } else if (currentCoord == 0) {
                coords[0] = (float) this.coords.getX(0);
                coords[1] = (float) this.coords.getY(0);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_MOVETO;
            } else {
                coords[0] = (float) this.coords.getX(currentCoord);
                coords[1] = (float) this.coords.getY(currentCoord);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_LINETO;
            }
        }

        @Override
        public int getWindingRule() {
            return WIND_EVEN_ODD;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public void next() {
            if (currentCoord == csSize) {
                if (currentRing < (rings.length - 1)) {
                    currentCoord = 0;
                    currentRing++;
                    coords = rings[currentRing].getCoordinateSequence();
                    csSize = coords.size() - 1;
                } else {
                    done = true;
                }
            } else {
                currentCoord++;
            }
        }
    }

    static final class MultiLineString extends JTSPathIterator<org.locationtech.jts.geom.MultiLineString> {

        private int coordinateCount;
        //global geometry state
        private int nbGeom;
        private int currentGeom = -1;
        private boolean done;
        //sub geometry state
        private CoordinateSequence currentSequence;
        private int currentCoord = -1;

        /**
         * Create a new MultiLineString path iterator.
         */
        public MultiLineString(final org.locationtech.jts.geom.MultiLineString ls, final MathTransform trs) {
            super(ls, trs);
            setGeometry(ls);
        }

        @Override
        public void setGeometry(final org.locationtech.jts.geom.MultiLineString geom) {
            super.setGeometry(geom);
            if (geom != null) {
                nbGeom = geom.getNumGeometries();
                nextSubGeom();
            }
            reset();
        }

        private void nextSubGeom() {
            if (++currentGeom >= nbGeom) {
                //nothing left, we are done
                currentSequence = null;
                currentCoord = -1;
                done = true;
            } else {
                final org.locationtech.jts.geom.LineString subGeom = ((org.locationtech.jts.geom.LineString) geometry.getGeometryN(currentGeom));
                currentSequence = subGeom.getCoordinateSequence();
                coordinateCount = currentSequence.size();

                if (coordinateCount == 0) {
                    //no point in this line, skip it
                    nextSubGeom();
                } else {
                    currentCoord = 0;
                    done = false;
                }
            }
        }

        @Override
        public void reset() {
            currentGeom = -1;
            nextSubGeom();
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
            if (++currentCoord >= coordinateCount) {
                //we go to the size, even if we don't have a coordinate at this index,
                //to indicate we close the path
                //no more points in this segment
                nextSubGeom();
            }
        }

        @Override
        public int currentSegment(final double[] coords) {
            if (currentCoord == 0) {
                coords[0] = currentSequence.getX(currentCoord);
                coords[1] = currentSequence.getY(currentCoord);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_MOVETO;
            } else if (currentCoord == coordinateCount) {
                return SEG_CLOSE;
            } else {
                coords[0] = currentSequence.getX(currentCoord);
                coords[1] = currentSequence.getY(currentCoord);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_LINETO;
            }
        }

        @Override
        public int currentSegment(final float[] coords) {
            if (currentCoord == 0) {
                coords[0] = (float) currentSequence.getX(currentCoord);
                coords[1] = (float) currentSequence.getY(currentCoord);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_MOVETO;
            } else if (currentCoord == coordinateCount) {
                return SEG_CLOSE;
            } else {
                coords[0] = (float) currentSequence.getX(currentCoord);
                coords[1] = (float) currentSequence.getY(currentCoord);
                safeTransform(coords, 0, coords, 0, 1);
                return SEG_LINETO;
            }
        }
    }

    static class GeometryCollection extends JTSPathIterator<org.locationtech.jts.geom.GeometryCollection> {

        protected int nbGeom = 1;
        protected int currentGeom;
        protected JTSPathIterator currentIterator;
        protected boolean done;

        public GeometryCollection(final org.locationtech.jts.geom.GeometryCollection gc, final MathTransform trs) {
            super(gc, trs);
            reset();
        }

        @Override
        public void reset() {
            currentGeom = 0;
            done = false;
            nbGeom = geometry.getNumGeometries();
            if (geometry != null && nbGeom > 0) {
                prepareIterator(geometry.getGeometryN(0));
            } else {
                done = true;
            }
        }

        @Override
        public void setGeometry(final org.locationtech.jts.geom.GeometryCollection geom) {
            super.setGeometry(geom);
            if (geom == null) {
                nbGeom = 0;
            } else {
                nbGeom = geom.getNumGeometries();
            }
        }

        /**
         * Returns the specific iterator for the geometry passed.
         *
         * @param candidate The geometry whole iterator is requested
         */
        protected void prepareIterator(final Geometry candidate) {

            //try to reuse the previous iterator.
            if (candidate.isEmpty()) {
                if (currentIterator instanceof JTSPathIterator.Empty) {
                    //nothing to do
                } else {
                    currentIterator = JTSPathIterator.Empty.INSTANCE;
                }
            } else if (candidate instanceof org.locationtech.jts.geom.Point) {
                if (currentIterator instanceof JTSPathIterator.Point) {
                    currentIterator.setGeometry(candidate);
                } else {
                    currentIterator = new JTSPathIterator.Point((org.locationtech.jts.geom.Point) candidate, transform);
                }
            } else if (candidate instanceof org.locationtech.jts.geom.Polygon) {
                if (currentIterator instanceof JTSPathIterator.Polygon) {
                    currentIterator.setGeometry(candidate);
                } else {
                    currentIterator = new JTSPathIterator.Polygon((org.locationtech.jts.geom.Polygon) candidate, transform);
                }
            } else if (candidate instanceof org.locationtech.jts.geom.LineString) {
                if (currentIterator instanceof JTSPathIterator.LineString) {
                    currentIterator.setGeometry(candidate);
                } else {
                    currentIterator = new JTSPathIterator.LineString((org.locationtech.jts.geom.LineString) candidate, transform);
                }
            } else if (candidate instanceof org.locationtech.jts.geom.GeometryCollection) {
                if (currentIterator instanceof JTSPathIterator.GeometryCollection) {
                    currentIterator.setGeometry(candidate);
                } else {
                    currentIterator = new JTSPathIterator.GeometryCollection((org.locationtech.jts.geom.GeometryCollection) candidate, transform);
                }
            } else {
                currentIterator = JTSPathIterator.Empty.INSTANCE;
            }

        }

        @Override
        public void setTransform(final MathTransform trs) {
            if (currentIterator != null) {
                currentIterator.setTransform(trs);
            }
            super.setTransform(trs);
        }

        @Override
        public int currentSegment(final double[] coords) {
            return currentIterator.currentSegment(coords);
        }

        @Override
        public int currentSegment(final float[] coords) {
            return currentIterator.currentSegment(coords);
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
            currentIterator.next();

            if (currentIterator.isDone()) {
                if (currentGeom < (nbGeom - 1)) {
                    currentGeom++;
                    prepareIterator(geometry.getGeometryN(currentGeom));
                } else {
                    done = true;
                }
            }
        }
    }
}
