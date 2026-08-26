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
package org.apache.sis.geometries.adapter;

import org.apache.sis.geometries.*;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.geometries.math.Cursor;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.locationtech.jts.geom.CoordinateXY;


/**
 * Mesh geometry utilities.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class JTSAdapter {

    /**
     * Convert given JTS geometry to SIS Geometry.
     * @param copy if true create a copy of the coordinate sequence, otherwise create a view
     */
    public static Geometry fromJTS(org.locationtech.jts.geom.Geometry jts, boolean copy) {
        if (jts == null) {
            return null;
        }
        CoordinateReferenceSystem crs = org.apache.sis.geometry.wrapper.Geometries.wrap(jts).get().getCoordinateReferenceSystem();
        if (crs == null) crs = Geometries.getUndefinedCRS(2);
        return fromJTS(jts, crs, copy);
    }

    /**
     * Convert given JTS geometry to SIS Geometry.
     * @param copy if true create a copy of the coordinate sequence, otherwise create a view
     */
    private static Geometry fromJTS(org.locationtech.jts.geom.Geometry jts, CoordinateReferenceSystem crs, boolean copy) {
        if (jts == null) {
            return null;
        } else if (jts instanceof org.locationtech.jts.geom.Point cdt) {
            return GeometryFactory.createPoint(toPointSequence(cdt.getCoordinateSequence(), crs, copy));

        } else if (jts instanceof org.locationtech.jts.geom.MultiPoint cdt) {
            return GeometryFactory.createMultiPoint(toPointSequence(jts.getFactory().getCoordinateSequenceFactory().create(cdt.getCoordinates()), crs, copy));

        } else if (jts instanceof org.locationtech.jts.geom.LinearRing cdt) {
            return GeometryFactory.createLinearRing(toPointSequence(cdt.getCoordinateSequence(), crs, copy));

        } else if (jts instanceof org.locationtech.jts.geom.LineString cdt) {
            return GeometryFactory.createLineString(toPointSequence(cdt.getCoordinateSequence(), crs, copy));

        } else if (jts instanceof org.locationtech.jts.geom.MultiLineString cdt) {
            final LineString[] strings = new LineString[cdt.getNumGeometries()];
            for (int i = 0; i < strings.length; i++) {
                strings[i] = (LineString) fromJTS(cdt.getGeometryN(i), crs, copy);
            }
            return GeometryFactory.createMultiLineString(strings);
        } else if (jts instanceof org.locationtech.jts.geom.Polygon cdt) {
            final LinearRing exterior = (LinearRing) fromJTS(cdt.getExteriorRing(), crs, copy);
            final List<LinearRing> interiors = new ArrayList<>(cdt.getNumInteriorRing());
            for (int i = 0, n = cdt.getNumInteriorRing(); i < n; i++) {
                interiors.add((LinearRing) fromJTS(cdt.getInteriorRingN(i), crs, copy));
            }
            return GeometryFactory.createPolygon(exterior, interiors);

        } else if (jts instanceof org.locationtech.jts.geom.MultiPolygon cdt) {
            final Polygon[] geoms = new Polygon[cdt.getNumGeometries()];
            for (int i = 0; i < geoms.length; i++) {
                geoms[i] = (Polygon) fromJTS(cdt.getGeometryN(i), crs, copy);
            }
            return GeometryFactory.createMultiPolygon(geoms);

        } else if (jts instanceof org.locationtech.jts.geom.GeometryCollection cdt) {
            final Geometry[] geoms = new Geometry[cdt.getNumGeometries()];
            for (int i = 0; i < geoms.length; i++) {
                geoms[i] = fromJTS(cdt.getGeometryN(i), crs, copy);
            }
            return GeometryFactory.createGeometryCollection(geoms);

        } else {
            throw new IllegalArgumentException("Unknown JTS geometry type");
        }
    }

    /**
     * View a geometry as a JTS geometry.
     *
     * @param copy if true create a copy of the point sequence, otherwise create a view
     * @return JTS equivalent
     */
    public static org.locationtech.jts.geom.Geometry asJTS(Geometry geom, boolean copy, org.locationtech.jts.geom.GeometryFactory gf) {

        final org.locationtech.jts.geom.Geometry jts;

        if (geom == null) {
            return null;
        } else if (geom instanceof Point cdt) {
            final CoordinateSequence cs = toCoordinateSequence(cdt.asPointSequence(), copy, gf);
            jts = new org.locationtech.jts.geom.Point(cs, gf);
        } else if (geom instanceof MultiPoint cdt) {
            final CoordinateSequence cs = toCoordinateSequence(cdt.asPointSequence(), copy, gf);
            jts = gf.createMultiPoint(cs);
        } else if (geom instanceof LinearRing cdt) {
            final CoordinateSequence cs = toCoordinateSequence(cdt.getPoints(), copy, gf);
            jts = new org.locationtech.jts.geom.LinearRing(cs, gf);
        } else if (geom instanceof LineString cdt) {
            final CoordinateSequence cs = toCoordinateSequence(cdt.getPoints(), copy, gf);
            jts = new org.locationtech.jts.geom.LineString(cs, gf);
        } else if (geom instanceof MultiLineString cdt) {
            final org.locationtech.jts.geom.LineString[] children = new org.locationtech.jts.geom.LineString[cdt.getNumGeometries()];
            for (int i = 0; i < children.length; i++) {
                children[i] = (org.locationtech.jts.geom.LineString) asJTS(cdt.getGeometryN(i), copy, gf);
            }
            jts = gf.createMultiLineString(children);
        } else if (geom instanceof Polygon cdt) {

            final org.locationtech.jts.geom.LinearRing exterior = (org.locationtech.jts.geom.LinearRing) asJTS(cdt.getExteriorRing(), copy, gf);
            final org.locationtech.jts.geom.LinearRing[] children = new org.locationtech.jts.geom.LinearRing[cdt.getNumInteriorRing()];
            for (int i = 0; i < children.length; i++) {
                children[i] = (org.locationtech.jts.geom.LinearRing) asJTS(cdt.getInteriorRingN(i), copy, gf);
            }
            jts = gf.createPolygon(exterior, children);
        } else if (geom instanceof MultiPolygon cdt) {
            final org.locationtech.jts.geom.Polygon[] children = new org.locationtech.jts.geom.Polygon[cdt.getNumGeometries()];
            for (int i = 0; i < children.length; i++) {
                children[i] = (org.locationtech.jts.geom.Polygon) asJTS(cdt.getGeometryN(i), copy, gf);
            }
            jts = gf.createMultiPolygon(children);
        } else if (geom instanceof GeometryCollection cdt) {
            final org.locationtech.jts.geom.Geometry[] children = new org.locationtech.jts.geom.Geometry[cdt.getNumGeometries()];
            for (int i = 0; i < children.length; i++) {
                children[i] = asJTS(cdt.getGeometryN(i), copy, gf);
            }
            jts = gf.createGeometryCollection(children);
        } else {
            throw new IllegalArgumentException("Unsupported geometry " + geom.getClass().getName());
        }

        jts.setUserData(geom.getCoordinateReferenceSystem());
        return jts;
    }

    /**
     * Convert JTS coordinate sequence to SIS PointSequence.
     * @param copy if true create a copy of the coordinate sequence, otherwise create a view
     */
    private static PointSequence toPointSequence(CoordinateSequence cs, CoordinateReferenceSystem crs, boolean copy) {
        final int size = cs.size();
        final int dimension = crs.getCoordinateSystem().getDimension();

        if (copy) {
            final Array positions = NDArrays.of(SampleSystem.of(crs), DataType.DOUBLE, size);
            final Cursor cursor = positions.cursor();
            int i = 0;
            while (cursor.next()) {
                final Tuple samples = cursor.samples();
                samples.set(0, cs.getOrdinate(i, 0));
                samples.set(1, cs.getOrdinate(i, 1));
                if (dimension > 2) {
                    //JTS only goes up to 3 dimensions
                    samples.set(2, cs.getOrdinate(i, 2));
                }
                i++;
            }
            return new ArraySequence(positions);
        } else {
            return new JTSSequence(cs, crs);
        }
    }

    private static CoordinateSequence toCoordinateSequence(PointSequence ps, boolean copy, org.locationtech.jts.geom.GeometryFactory gf) {
        final CoordinateReferenceSystem crs = ps.getCoordinateReferenceSystem();
        final int dimension = ps.getDimension();
        if (copy) {
            final int size = ps.size();
            final CoordinateSequence cs = gf.getCoordinateSequenceFactory().create(size, dimension);

            for (int i = 0; i < size; i++) {
                Tuple position = ps.getPosition(i);
                for (int d = 0; d < dimension; d++) {
                    cs.setOrdinate(i, d, position.get(d));
                }
            }
            return cs;
        } else {
            return new SISSequence(ps);
        }
    }

    private static class JTSSequence implements PointSequence {

        private final CoordinateSequence jts;
        private final CoordinateReferenceSystem crs;
        private final int dim;
        private final AttributesType type;

        private JTSSequence(CoordinateSequence jts, CoordinateReferenceSystem crs) {
            this.jts = jts;
            this.crs = crs;
            this.dim = jts.getDimension();

            final AttributesType.Template at = new AttributesType.Template();
            at.addOrReplaceAttribute(AttributesType.ATT_POSITION, SampleSystem.of(crs), DataType.DOUBLE);
            type = at;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return crs;
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public AttributesType getAttributesType() {
            return type;
        }

        @Override
        public int size() {
            return jts.size();
        }

        @Override
        public Point getPoint(int index) {
            return new JTSPoint(this, index);
        }

        @Override
        public Tuple getPosition(int index) {
            final Vector v = Vectors.create(crs, DataType.DOUBLE);
            for (int i = 0; i < dim; i++) {
                v.set(i, jts.getOrdinate(index, i));
            }
            return v;
        }

        @Override
        public void setPosition(int index, Tuple value) {
            for (int i = 0; i < dim; i++) {
                jts.setOrdinate(index, i, value.get(i));
            }
        }

        @Override
        public Tuple getAttribute(int index, String name) {
            if (AttributesType.ATT_POSITION.equals(name)) {
                return getPosition(index);
            }
            throw new IllegalArgumentException("No attribute for name " + name);
        }

        @Override
        public void setAttribute(int index, String name, Tuple value) {
            if (AttributesType.ATT_POSITION.equals(name)) {
                setPosition(index, value);
            } else {
                throw new IllegalArgumentException("No attribute for name " + name);
            }
        }

    }

    /**
     * An indexed point in the jts sequence
     */
    private static class JTSPoint implements Point {

        private final JTSSequence parent;
        private final int index;

        public JTSPoint(JTSSequence geometry, int index) {
            this.parent = geometry;
            this.index = index;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Tuple getPosition() {
            return parent.getPosition(index);
        }

        /**
         * @return index in the parent.
         */
        public int getIndex() {
            return index;
        }

        @Override
        public Tuple getAttribute(String key) {
            return parent.getAttribute(index, key);
        }

        @Override
        public void setAttribute(String name, Tuple tuple) {
            parent.setAttribute(index, name, tuple);
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return parent.getCoordinateReferenceSystem();
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public AttributesType getAttributesType() {
            return parent.getAttributesType();
        }
    }

    private static class SISSequence implements CoordinateSequence {

        private final PointSequence points;

        public SISSequence(PointSequence points) {
            this.points = points;
        }

        @Override
        public int getDimension() {
            return points.getDimension();
        }

        @Override
        public Coordinate getCoordinate(int i) {
            return getCoordinateCopy(i);
        }

        @Override
        public Coordinate getCoordinateCopy(int i) {
            final Tuple<?> position = points.getPosition(i);
            if (getDimension() == 2) {
                return new CoordinateXY(position.get(0), position.get(1));
            } else {
                return new Coordinate(position.get(0), position.get(1), position.get(2));
            }
        }

        @Override
        public void getCoordinate(int index, Coordinate coord) {
            final Tuple<?> position = points.getPosition(index);
            coord.setX(position.get(0));
            coord.setY(position.get(1));
        }

        @Override
        public double getX(int index) {
            return points.getPosition(index).get(0);
        }

        @Override
        public double getY(int index) {
            return points.getPosition(index).get(1);
        }

        @Override
        public double getOrdinate(int index, int ordinateIndex) {
            return points.getPosition(index).get(ordinateIndex);
        }

        @Override
        public int size() {
            return points.size();
        }

        @Override
        public void setOrdinate(int index, int ordinateIndex, double value) {
            final Tuple<?> position = points.getPosition(index);
            position.set(ordinateIndex, value);
            points.setPosition(index, position);
        }

        @Override
        public Coordinate[] toCoordinateArray() {
            final Coordinate[] array = new Coordinate[points.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = getCoordinate(i);
            }
            return array;
        }

        @Override
        public org.locationtech.jts.geom.Envelope expandEnvelope(org.locationtech.jts.geom.Envelope env) {
            final BBox bbox = points.getAttributeRange(AttributesType.ATT_POSITION);
            env.expandToInclude(bbox.getMinimum(0), bbox.getMinimum(1));
            env.expandToInclude(bbox.getMaximum(0), bbox.getMaximum(1));
            return env;
        }

        @Override
        public CoordinateSequence copy() {
            return new org.locationtech.jts.geom.GeometryFactory().getCoordinateSequenceFactory().create(this);
        }

        @Override
        public CoordinateSequence clone() {
            return copy();
        }

    }

}
