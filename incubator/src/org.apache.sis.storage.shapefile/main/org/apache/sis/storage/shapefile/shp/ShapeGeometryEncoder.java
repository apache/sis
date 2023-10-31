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
package org.apache.sis.storage.shapefile.shp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.RayCrossingCounter;

/**
 * Encoders and decoders for shape types.
 * This class should be kept separate because I might be used in ESRI geodatabase format.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class ShapeGeometryEncoder {

    private static final GeometryFactory GF = new GeometryFactory();

    protected final int shapeType;
    protected final int dimension;
    protected final int measures;
    protected final int nbOrdinates;

    /**
     *
     * @param shapeType shape type to encode
     * @return requested encoder
     */
    public static ShapeGeometryEncoder getEncoder(int shapeType) {
        switch(shapeType) {
            //2D
            case ShapeType.VALUE_NULL: return Null.INSTANCE;
            case ShapeType.VALUE_POINT: return PointXY.INSTANCE;
            case ShapeType.VALUE_POLYLINE: return Polyline.INSTANCE;
            case ShapeType.VALUE_POLYGON: return Polygon.INSTANCE;
            case ShapeType.VALUE_MULTIPOINT: return MultiPointXY.INSTANCE;
            //2D+1
            case ShapeType.VALUE_POINT_M: return PointXYM.INSTANCE;
            case ShapeType.VALUE_POLYLINE_M: return Polyline.INSTANCE_M;
            case ShapeType.VALUE_POLYGON_M: return Polygon.INSTANCE_M;
            case ShapeType.VALUE_MULTIPOINT_M: return MultiPointXYM.INSTANCE;
            //3D+1
            case ShapeType.VALUE_POINT_ZM: return PointXYZM.INSTANCE;
            case ShapeType.VALUE_POLYLINE_ZM: return Polyline.INSTANCE_ZM;
            case ShapeType.VALUE_POLYGON_ZM: return Polygon.INSTANCE_ZM;
            case ShapeType.VALUE_MULTIPOINT_ZM: return MultiPointXYZM.INSTANCE;
            case ShapeType.VALUE_MULTIPATCH_ZM: return MultiPatch.INSTANCE;
            default: throw new IllegalArgumentException("unknown shape type");
        }
    }

    /**
     * @param shapeType shape type code.
     * @param dimension number of dimensions in processed geometries.
     * @param measures number of measures in processed geometries.
     */
    protected ShapeGeometryEncoder(int shapeType, int dimension, int measures) {
        this.shapeType = shapeType;
        this.dimension = dimension;
        this.measures = measures;
        this.nbOrdinates = dimension + measures;
    }

    /**
     * @return shape type code.
     */
    public int getShapeType() {
        return shapeType;
    }

    /**
     * @return number of dimensions in processed geometries.
     */
    public final int getDimension() {
        return dimension;
    }

    /**
     * @return number of measures in processed geometries.
     */
    public final int getMeasures() {
        return measures;
    }

    public abstract void decode(ChannelDataInput ds, ShapeRecord record) throws IOException;

    public abstract void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException;

    /**
     * Compute the encoded size of a geometry.
     * @param geom to estimate
     * @return geometry size in bytes once encoded.
     */
    public abstract int getEncodedLength(Geometry geom);

    protected void readBBox2D(ChannelDataInput ds, ShapeRecord shape) throws IOException {
        shape.bbox = new GeneralEnvelope(getDimension());
        shape.bbox.getLowerCorner().setOrdinate(0, ds.readDouble());
        shape.bbox.getLowerCorner().setOrdinate(1, ds.readDouble());
        shape.bbox.getUpperCorner().setOrdinate(0, ds.readDouble());
        shape.bbox.getUpperCorner().setOrdinate(1, ds.readDouble());
    }

    protected void writeBBox2D(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
        ds.writeDouble(shape.bbox.getMinimum(0));
        ds.writeDouble(shape.bbox.getMinimum(1));
        ds.writeDouble(shape.bbox.getMaximum(0));
        ds.writeDouble(shape.bbox.getMaximum(1));
    }

    protected LineString[] readLines(ChannelDataInput ds, ShapeRecord shape, boolean asRing) throws IOException {
        readBBox2D(ds, shape);
        final int numParts = ds.readInt();
        final int numPoints = ds.readInt();
        final int[] offsets = ds.readInts(numParts);

        final LineString[] lines = new LineString[numParts];

        //XY
        for (int i = 0; i < numParts; i++) {
            final int nbValues = (i == numParts - 1) ? numPoints - offsets[i] : offsets[i + 1] - offsets[i];
            final double[] values;
            if (nbOrdinates == 2) {
                values = ds.readDoubles(nbValues * 2);
            } else {
                values = ds.readDoubles(nbValues * nbOrdinates);
                for (int k = 0; k < nbValues; k++) {
                    values[k * nbOrdinates  ] = ds.readDouble();
                    values[k * nbOrdinates + 1] = ds.readDouble();
                }
            }
            final PackedCoordinateSequence.Double pc = new PackedCoordinateSequence.Double(values, getDimension(), getMeasures());
            lines[i] = asRing ? GF.createLinearRing(pc) : GF.createLineString(pc);
        }
        //Z and M
        if (nbOrdinates >= 3)  readLineOrdinates(ds, shape, lines, 2);
        if (nbOrdinates == 4)  readLineOrdinates(ds, shape, lines, 3);
        return lines;
    }

    protected void readLineOrdinates(ChannelDataInput ds, ShapeRecord shape, LineString[] lines, int ordinateIndex) throws IOException {
        final int nbDim = getDimension() + getMeasures();
        shape.bbox.setRange(ordinateIndex, ds.readDouble(), ds.readDouble());
        for (LineString line : lines) {
            final double[] values = ((PackedCoordinateSequence.Double) line.getCoordinateSequence()).getRawCoordinates();
            final int nbValues = values.length / nbDim;
            for (int k = 0; k < nbValues; k++) {
                values[k * nbDim + ordinateIndex] = ds.readDouble();
            }
        }
    }

    protected void writeLines(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
        writeBBox2D(ds, shape);
        final List<LineString> lines = extractRings(shape.geometry);
        final int nbLines = lines.size();
        final int[] offsets = new int[nbLines];
        int nbPts = 0;
        //first loop write offsets
        for (int i = 0; i < nbLines; i++) {
            final LineString line = lines.get(i);
            offsets[i] = nbPts;
            nbPts += line.getCoordinateSequence().size();
        }
        ds.writeInt(nbLines);
        ds.writeInt(nbPts);
        ds.writeInts(offsets);

        //second loop write points
        for (int i = 0; i < nbLines; i++) {
            final LineString line = lines.get(i);
            final CoordinateSequence cs = line.getCoordinateSequence();
            for (int k = 0, kn =cs.size(); k < kn; k++) {
                ds.writeDouble(cs.getX(k));
                ds.writeDouble(cs.getY(k));
            }
        }

        //Z and M
        if (nbOrdinates >= 3)  writeLineOrdinates(ds, shape, lines, 2);
        if (nbOrdinates == 4)  writeLineOrdinates(ds, shape, lines, 3);
    }

    protected void writeLineOrdinates(ChannelDataOutput ds, ShapeRecord shape,List<LineString> lines, int ordinateIndex) throws IOException {
        ds.writeDouble(shape.bbox.getMinimum(ordinateIndex));
        ds.writeDouble(shape.bbox.getMaximum(ordinateIndex));
        for (LineString line : lines) {
            final CoordinateSequence cs = line.getCoordinateSequence();
            for (int k = 0, kn =cs.size(); k < kn; k++) {
                ds.writeDouble(cs.getOrdinate(k, ordinateIndex));
            }
        }
    }

    protected List<LineString> extractRings(Geometry geom) {
        final List<LineString> lst = new ArrayList();
        extractRings(geom, lst);
        return lst;
    }

    private void extractRings(Geometry geom, List lst) {
        if (geom instanceof GeometryCollection) {
            final GeometryCollection gc = (GeometryCollection) geom;
            for (int i = 0, n = gc.getNumGeometries(); i < n; i++) {
                extractRings(gc.getGeometryN(i), lst);
            }
        } else if (geom instanceof org.locationtech.jts.geom.Polygon) {
            final org.locationtech.jts.geom.Polygon poly = (org.locationtech.jts.geom.Polygon) geom;
            lst.add(poly.getExteriorRing());
            for (int i = 0, n = poly.getNumInteriorRing(); i < n; i++) {
                lst.add(poly.getInteriorRingN(i));
            }
        } else if (geom instanceof LineString) {
            lst.add(geom);
        } else {
            throw new RuntimeException("Unexpected geometry type "+geom);
        }
    }

    protected MultiPolygon rebuild(List<LinearRing> rings) {

        final int nbRing = rings.size();
        if (nbRing == 0) {
            return GF.createMultiPolygon();
        } else if (rings.size() == 1) {
            return GF.createMultiPolygon(new org.locationtech.jts.geom.Polygon[]{
                    GF.createPolygon(rings.get(0))});
        } else {
            /*
             * In the specification, outer rings should be in clockwise orientation and holes in
             * counterclockwise.
             */
            final List<LinearRing> outers = new ArrayList<>();
            final List<LinearRing> inners = new ArrayList<>();
            for (LinearRing ls : rings) {
                if (Orientation.isCCW(ls.getCoordinateSequence())) {
                    inners.add(ls);
                } else {
                    outers.add(ls);
                }
            }
            if (outers.isEmpty()) {
                //no exterior ? bad geometry, let's consider all inner loops as outer
                outers.addAll(inners);
                inners.clear();
            }

            //build the exterior polygon
            if (inners.isEmpty()) {
                return GF.createMultiPolygon(outers.stream().map(GF::createPolygon).toArray(org.locationtech.jts.geom.Polygon[]::new));
            }

            //find which hole goes into each exterior
            final List<org.locationtech.jts.geom.Polygon> polygones = new ArrayList<>(outers.size());
            for (LinearRing out : outers) {
                final List<LinearRing> holes = new ArrayList<>();
                for (int i = inners.size() - 1; i >= 0; i--) {
                    final LinearRing in = inners.get(i);
                    final Coordinate aPt = in.getCoordinateSequence().getCoordinate(0);
                    if (RayCrossingCounter.locatePointInRing(aPt, out.getCoordinateSequence()) != Location.EXTERIOR) {
                        //consider the ring to be inside
                        holes.add(inners.remove(i));
                    }
                }
                polygones.add(GF.createPolygon(out, GeometryFactory.toLinearRingArray(holes)));
            }

            //handle unused inners rings as exteriors
            for (LinearRing r : inners) {
                polygones.add(GF.createPolygon(r));
            }

            return GF.createMultiPolygon(GeometryFactory.toPolygonArray(polygones));
        }
    }

    private static class Null extends ShapeGeometryEncoder {

        private static final Null INSTANCE = new Null();

        private Null() {
            super(ShapeType.VALUE_NULL, 2,0);
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 0;
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
        }

    }

    private static class PointXY extends ShapeGeometryEncoder {

        private static final PointXY INSTANCE = new PointXY();

        private PointXY() {
            super(ShapeType.VALUE_POINT, 2,0);
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
            shape.bbox = new GeneralEnvelope(2);
            final double x = ds.readDouble();
            final double y = ds.readDouble();
            shape.bbox.setRange(0, x, x);
            shape.bbox.setRange(1, y, y);
            shape.geometry = GF.createPoint(new CoordinateXY(x, y));
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
            final Point pt = (Point) shape.geometry;
            final Coordinate coord = pt.getCoordinate();
            ds.writeDouble(coord.getX());
            ds.writeDouble(coord.getY());
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 2*8; //2 ordinates
        }
    }

    private static class PointXYM extends ShapeGeometryEncoder {

        private static final PointXYM INSTANCE = new PointXYM();
        private PointXYM() {
            super(ShapeType.VALUE_POINT_M, 2,1);
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
            shape.bbox = new GeneralEnvelope(3);
            final double x = ds.readDouble();
            final double y = ds.readDouble();
            final double z = ds.readDouble();
            shape.bbox.setRange(0, x, x);
            shape.bbox.setRange(1, y, y);
            shape.bbox.setRange(2, z, z);
            shape.geometry = GF.createPoint(new CoordinateXYM(x, y, z));
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
            final Point pt = (Point) shape.geometry;
            final Coordinate coord = pt.getCoordinate();
            ds.writeDouble(coord.getX());
            ds.writeDouble(coord.getY());
            ds.writeDouble(coord.getM());
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 3*8; //3 ordinates
        }
    }

    private static class PointXYZM extends ShapeGeometryEncoder {

        private static final PointXYZM INSTANCE = new PointXYZM();

        private PointXYZM() {
            super(ShapeType.VALUE_POINT_ZM, 3,1);
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
            shape.bbox = new GeneralEnvelope(4);
            final double x = ds.readDouble();
            final double y = ds.readDouble();
            final double z = ds.readDouble();
            final double m = ds.readDouble();
            shape.bbox.setRange(0, x, x);
            shape.bbox.setRange(1, y, y);
            shape.bbox.setRange(2, z, z);
            shape.bbox.setRange(3, m, m);
            shape.geometry = GF.createPoint(new CoordinateXYZM(x, y, z, m));
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
            final Point pt = (Point) shape.geometry;
            final Coordinate coord = pt.getCoordinate();
            ds.writeDouble(coord.getX());
            ds.writeDouble(coord.getY());
            ds.writeDouble(coord.getZ());
            ds.writeDouble(coord.getM());
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 4*8; //4 ordinates
        }
    }

    private static class MultiPointXY extends ShapeGeometryEncoder {

        private static final MultiPointXY INSTANCE = new MultiPointXY();
        private MultiPointXY() {
            super(ShapeType.VALUE_MULTIPOINT, 2,0);
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
            readBBox2D(ds, shape);
            int nbPt = ds.readInt();
            final double[] coords = ds.readDoubles(nbPt * 2);
            shape.geometry = GF.createMultiPoint(new PackedCoordinateSequence.Double(coords,2,0));
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
            writeBBox2D(ds, shape);
            final MultiPoint geometry = (MultiPoint) shape.geometry;
            final int nbPts = geometry.getNumGeometries();
            ds.writeInt(nbPts);
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                ds.writeDouble(pt.getX());
                ds.writeDouble(pt.getY());
            }
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 4 * 8 //bbox
                 + 4 //nbPts
                 + ((MultiPoint) geom).getNumGeometries() * 2 * 8;
        }
    }

    private static class MultiPointXYM extends ShapeGeometryEncoder {

        private static final MultiPointXYM INSTANCE = new MultiPointXYM();

        private MultiPointXYM() {
            super(ShapeType.VALUE_MULTIPOINT_M, 2,1);
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
            readBBox2D(ds, shape);
            int nbPt = ds.readInt();
            final double[] coords = new double[nbPt * 3];
            for (int i = 0; i < nbPt; i++) {
                coords[i * 3    ] = ds.readDouble();
                coords[i * 3 + 1] = ds.readDouble();
            }
            shape.bbox.setRange(2, ds.readDouble(), ds.readDouble());
            for (int i = 0; i < nbPt; i++) {
                coords[i * 3 + 2] = ds.readDouble();
            }
            shape.geometry = GF.createMultiPoint(new PackedCoordinateSequence.Double(coords, 2, 1));
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
            writeBBox2D(ds, shape);
            final MultiPoint geometry = (MultiPoint) shape.geometry;
            final int nbPts = geometry.getNumGeometries();
            ds.writeInt(nbPts);
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                ds.writeDouble(pt.getX());
                ds.writeDouble(pt.getY());
            }
            ds.writeDouble(shape.bbox.getMinimum(2));
            ds.writeDouble(shape.bbox.getMaximum(2));
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                ds.writeDouble(pt.getCoordinate().getM());
            }
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 6 * 8 //bbox
                 + 4 //nbPts
                 + ((MultiPoint) geom).getNumGeometries() * 3 * 8;
        }
    }

    private static class MultiPointXYZM extends ShapeGeometryEncoder {

        private static final MultiPointXYZM INSTANCE = new MultiPointXYZM();

        private MultiPointXYZM() {
            super(ShapeType.VALUE_MULTIPOINT_ZM, 3,1);
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
            readBBox2D(ds, shape);
            int nbPt = ds.readInt();
            final double[] coords = new double[nbPt * 4];
            for (int i = 0; i < nbPt; i++) {
                coords[i * 4    ] = ds.readDouble();
                coords[i * 4 + 1] = ds.readDouble();
            }
            shape.bbox.setRange(2, ds.readDouble(), ds.readDouble());
            for (int i = 0; i < nbPt; i++) {
                coords[i * 4 + 2] = ds.readDouble();
            }
            shape.bbox.setRange(3, ds.readDouble(), ds.readDouble());
            for (int i = 0; i < nbPt; i++) {
                coords[i * 4 + 3] = ds.readDouble();
            }
            shape.geometry = GF.createMultiPoint(new PackedCoordinateSequence.Double(coords, 3, 1));
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
            writeBBox2D(ds, shape);
            final MultiPoint geometry = (MultiPoint) shape.geometry;
            final int nbPts = geometry.getNumGeometries();
            ds.writeInt(nbPts);
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                ds.writeDouble(pt.getX());
                ds.writeDouble(pt.getY());
            }
            ds.writeDouble(shape.bbox.getMinimum(2));
            ds.writeDouble(shape.bbox.getMaximum(2));
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                ds.writeDouble(pt.getCoordinate().getZ());
            }
            ds.writeDouble(shape.bbox.getMinimum(3));
            ds.writeDouble(shape.bbox.getMaximum(3));
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                ds.writeDouble(pt.getCoordinate().getM());
            }
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 8 * 8 //box
                 + 4 //nbPts
                 + ((MultiPoint) geom).getNumGeometries() * 4 * 8;
        }
    }

    private static class Polyline extends ShapeGeometryEncoder {

        private static final Polyline INSTANCE = new Polyline(ShapeType.VALUE_POLYLINE, 2, 0);
        private static final Polyline INSTANCE_M = new Polyline(ShapeType.VALUE_POLYLINE_M, 3, 0);
        private static final Polyline INSTANCE_ZM = new Polyline(ShapeType.VALUE_POLYLINE_ZM, 3, 1);

        private Polyline(int shapeType, int dimension, int measures) {
            super(shapeType, dimension, measures);
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
            shape.geometry = GF.createMultiLineString(readLines(ds, shape, false));
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
            writeLines(ds, shape);
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            final MultiLineString ml = (MultiLineString) geom;
            final int nbGeom = ml.getNumGeometries();
            final int nbPoints = ml.getNumPoints();
            return nbOrdinates * 2 * 8 //bbox
                 + 4 * 2 //num parts and num points
                 + nbGeom * 4 //offsets table
                 + nbPoints * nbOrdinates * 8; //all ordinates
        }
    }

    private static class Polygon extends ShapeGeometryEncoder {

        private static final Polygon INSTANCE = new Polygon(ShapeType.VALUE_POLYGON, 2, 0);
        private static final Polygon INSTANCE_M = new Polygon(ShapeType.VALUE_POLYGON_M, 3, 0);
        private static final Polygon INSTANCE_ZM = new Polygon(ShapeType.VALUE_POLYGON_ZM, 3, 1);

        private Polygon(int shapeType, int dimension, int measures) {
            super(shapeType, dimension, measures);
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
            final LineString[] rings = readLines(ds, shape, true);
            shape.geometry = rebuild(Stream.of(rings).map(LinearRing.class::cast).collect(Collectors.toList()));
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
            writeLines(ds, shape);
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            final MultiPolygon mp = (MultiPolygon) geom;
            int nbGeom = mp.getNumGeometries();
            for (int i = 0, n = nbGeom; i < n; i++) {
                org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) mp.getGeometryN(i);
                nbGeom += polygon.getNumInteriorRing();
            }
            final int nbPoints = mp.getNumPoints();
            return nbOrdinates * 2 * 8 //bbox
                 + 4 * 2 //num parts and num points
                 + nbGeom * 4 //offsets table
                 + nbPoints * nbOrdinates * 8; //all ordinates
        }
    }

    private static class MultiPatch extends ShapeGeometryEncoder {

        private static final MultiPatch INSTANCE = new MultiPatch();

        private MultiPatch() {
            super(ShapeType.VALUE_MULTIPATCH_ZM, 3, 1);
        }

        @Override
        public void decode(ChannelDataInput ds, ShapeRecord shape) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void encode(ChannelDataOutput ds, ShapeRecord shape) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

}
