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

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.RayCrossingCounter;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;


/**
 * Encoders and decoders for shape types.
 * This class should be kept separate because I might be used in ESRI geodatabase format.
 *
 * @author Johann Sorel (Geomatys)
 * @param <T> encoder geometry type
 */
public abstract class ShapeGeometryEncoder<T extends Geometry> {

    private static final GeometryFactory GF = new GeometryFactory();

    /**
     * Encoder shape type.
     */
    protected final ShapeType shapeType;
    /**
     * Encoder java value class.
     */
    protected final Class<T> geometryClass;
    /**
     * Number of dimension in the geometry.
     */
    protected final int dimension;
    /**
     * Number of measures in the geometry.
     */
    protected final int measures;
    /**
     * Sum of dimension and measures.
     */
    protected final int nbOrdinates;

    /**
     * Get encoder for given shape type.
     *
     * @param shapeType shape type to encode
     * @return requested encoder
     */
    public static ShapeGeometryEncoder getEncoder(ShapeType shapeType) {
        switch(shapeType) {
            //2D
            case NULL: return Null.INSTANCE;
            case POINT: return PointXY.INSTANCE;
            case POLYLINE: return Polyline.INSTANCE;
            case POLYGON: return Polygon.INSTANCE;
            case MULTIPOINT: return MultiPointXY.INSTANCE;
            //2D+1
            case POINT_M: return PointXYM.INSTANCE;
            case POLYLINE_M: return Polyline.INSTANCE_M;
            case POLYGON_M: return Polygon.INSTANCE_M;
            case MULTIPOINT_M: return MultiPointXYM.INSTANCE;
            //3D+1
            case POINT_ZM: return PointXYZM.INSTANCE;
            case POLYLINE_ZM: return Polyline.INSTANCE_ZM;
            case POLYGON_ZM: return Polygon.INSTANCE_ZM;
            case MULTIPOINT_ZM: return MultiPointXYZM.INSTANCE;
            case MULTIPATCH_ZM: return MultiPatch.INSTANCE;
            default: throw new IllegalArgumentException("unknown shape type");
        }
    }

    /**
     * Constructor.
     *
     * @param shapeType shape type code.
     * @param geometryClass java geometry class
     * @param dimension number of dimensions in processed geometries.
     * @param measures number of measures in processed geometries.
     */
    protected ShapeGeometryEncoder(ShapeType shapeType, Class<T> geometryClass, int dimension, int measures) {
        this.shapeType = shapeType;
        this.geometryClass = geometryClass;
        this.dimension = dimension;
        this.measures = measures;
        this.nbOrdinates = dimension + measures;
    }

    /**
     * Get shape type.
     *
     * @return shape type.
     */
    public ShapeType getShapeType() {
        return shapeType;
    }

    /**
     * Get java geometry value class.
     *
     * @return geometry class handled by this encoder
     */
    public Class<T> getValueClass() {
        return geometryClass;
    }

    /**
     * Get number of dimensions in processed geometries.
     *
     * @return number of dimensions in processed geometries.
     */
    public final int getDimension() {
        return dimension;
    }

    /**
     * Get number of measures in processed geometries.
     *
     * @return number of measures in processed geometries.
     */
    public final int getMeasures() {
        return measures;
    }

    /**
     * Decode geometry and store it in ShapeRecord.
     * This method creates and fill the record bbox if it is null.
     *
     * @param channel to read from
     * @param record to read into
     * @param filter optional filter envelope to stop geometry decoding as soon as possible
     * @return true if geometry pass the filter
     * @throws IOException If an I/O error occurs
     */
    public abstract boolean decode(ChannelDataInput channel, ShapeRecord record, Rectangle2D.Double filter) throws IOException;

    /**
     * Encode geometry.
     *
     * @param channel to write into
     * @param shape geometry to encode
     * @throws IOException If an I/O error occurs
     */
    public abstract void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException;

    /**
     * Compute the encoded size of a geometry.
     * @param geom to estimate
     * @return geometry size in bytes once encoded.
     */
    public abstract int getEncodedLength(Geometry geom);

    /**
     * Calculate geometry bounding box.
     *
     * @param geom to compute
     * @return geometry bounding box
     */
    public abstract GeneralEnvelope getBoundingBox(Geometry geom);

    /**
     * Read 2D Bounding box from channel.
     *
     * @param channel to read from
     * @param shape to write into
     * @param filter optional filter envelope to stop geometry decoding as soon as possible
     * @return true if filter match or is null
     * @throws IOException If an I/O error occurs
     */
    protected boolean readBBox2D(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
        final double minX = channel.readDouble();
        if (filter != null && minX > (filter.x + filter.width)) return false;
        final double minY = channel.readDouble();
        if (filter != null && minY > (filter.y + filter.height)) return false;
        final double maxX = channel.readDouble();
        if (filter != null && maxX < filter.x) return false;
        final double maxY = channel.readDouble();
        if (filter != null && maxY < filter.y) return false;
        shape.bbox = new GeneralEnvelope(getDimension());
        shape.bbox.getLowerCorner().setCoordinate(0, minX);
        shape.bbox.getLowerCorner().setCoordinate(1, minY);
        shape.bbox.getUpperCorner().setCoordinate(0, maxX);
        shape.bbox.getUpperCorner().setCoordinate(1, maxY);
        return true;
    }

    /**
     * Write 2D Bounding box.
     *
     * @param channel to write into
     * @param shape to read from
     * @throws IOException If an I/O error occurs
     */
    protected void writeBBox2D(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
        final Envelope env2d = shape.geometry.getEnvelopeInternal();
        channel.writeDouble(env2d.getMinX());
        channel.writeDouble(env2d.getMinY());
        channel.writeDouble(env2d.getMaxX());
        channel.writeDouble(env2d.getMaxY());
    }

    /**
     * Read encoded lines.
     *
     * @param channel to read from
     * @param shape to write into
     * @param filter optional filter envelope to stop geometry decoding as soon as possible
     * @param asRing true to produce LinearRing instead of LineString
     * @return null if filter do no match
     * @throws IOException If an I/O error occurs
     */
    protected LineString[] readLines(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter, boolean asRing) throws IOException {
        if (!readBBox2D(channel, shape, filter)) return null;
        final int numParts = channel.readInt();
        final int numPoints = channel.readInt();
        final int[] offsets = channel.readInts(numParts);

        if (!shape.bbox.isFinite()) {
            //a broken geometry with NaN, until we replace JTS we need to create an empty geometry
            switch (nbOrdinates) {
                case 2 : channel.seek(channel.getStreamPosition() + 2*8*numPoints); break;
                case 3 : channel.seek(channel.getStreamPosition() + 3*8*numPoints); break;
                case 4 : channel.seek(channel.getStreamPosition() + 4*8*numPoints); break;
            }
            return new LineString[0];
        }

        final LineString[] lines = new LineString[numParts];

        //XY
        for (int i = 0; i < numParts; i++) {
            final int nbValues = (i == numParts - 1) ? numPoints - offsets[i] : offsets[i + 1] - offsets[i];
            final double[] values;
            if (nbOrdinates == 2) {
                values = channel.readDoubles(nbValues * 2);
            } else {
                values = channel.readDoubles(nbValues * nbOrdinates);
                for (int k = 0; k < nbValues; k++) {
                    values[k * nbOrdinates  ] = channel.readDouble();
                    values[k * nbOrdinates + 1] = channel.readDouble();
                }
            }
            final PackedCoordinateSequence.Double pc = new PackedCoordinateSequence.Double(values, getDimension(), getMeasures());
            lines[i] = asRing ? GF.createLinearRing(pc) : GF.createLineString(pc);
        }
        //Z and M
        if (nbOrdinates >= 3)  readLineOrdinates(channel, shape, lines, 2);
        if (nbOrdinates == 4)  readLineOrdinates(channel, shape, lines, 3);
        return lines;
    }

    /**
     * Read lines ordinates.
     *
     * @param  channel to read from
     * @param  shape to update
     * @param  lines to update
     * @param ordinateIndex ordinate index to read
     * @throws IOException If an I/O error occurs
     */
    protected void readLineOrdinates(ChannelDataInput channel, ShapeRecord shape, LineString[] lines, int ordinateIndex) throws IOException {
        final int nbDim = getDimension() + getMeasures();
        shape.bbox.setRange(ordinateIndex, channel.readDouble(), channel.readDouble());
        for (LineString line : lines) {
            final double[] values = ((PackedCoordinateSequence.Double) line.getCoordinateSequence()).getRawCoordinates();
            final int nbValues = values.length / nbDim;
            for (int k = 0; k < nbValues; k++) {
                values[k * nbDim + ordinateIndex] = channel.readDouble();
            }
        }
    }

    /**
     * Write given lines.
     *
     * @param channel to write into
     * @param shape to write
     * @throws IOException if an I/O exception occurs
     */
    protected void writeLines(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
        writeBBox2D(channel, shape);
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
        channel.writeInt(nbLines);
        channel.writeInt(nbPts);
        channel.writeInts(offsets);

        //second loop write points
        for (int i = 0; i < nbLines; i++) {
            final LineString line = lines.get(i);
            final CoordinateSequence cs = line.getCoordinateSequence();
            for (int k = 0, kn =cs.size(); k < kn; k++) {
                channel.writeDouble(cs.getX(k));
                channel.writeDouble(cs.getY(k));
            }
        }

        //Z and M
        if (nbOrdinates >= 3)  writeLineOrdinates(channel, shape, lines, 2, nbPts);
        if (nbOrdinates == 4)  writeLineOrdinates(channel, shape, lines, 3, nbPts);
    }

    /**
     * Write given lines ordinates.
     *
     * @param channel to write into
     * @param shape to write
     * @param lines to write
     * @param ordinateIndex line coordinate ordinate to write
     * @param nbPts number of points
     * @throws IOException if an I/O exception occurs
     */
    protected void writeLineOrdinates(ChannelDataOutput channel, ShapeRecord shape,List<LineString> lines, int ordinateIndex, int nbPts) throws IOException {

        final double[] values = new double[nbPts];
        double minK = Double.MAX_VALUE;
        double maxK = -Double.MAX_VALUE;
        int i = 0;
        for (LineString line : lines) {
            final CoordinateSequence cs = line.getCoordinateSequence();
            for (int k = 0, kn =cs.size(); k < kn; k++) {
                values[i] = cs.getOrdinate(k, ordinateIndex);
                minK = Double.min(minK, values[i]);
                maxK = Double.max(maxK, values[i]);
                i++;
            }
        }
        channel.writeDouble(minK);
        channel.writeDouble(maxK);
        channel.writeDoubles(values);
    }

    /**
     * Extract all linear elements of given geometry.
     *
     * @param geom to extract lines from.
     * @return list of lines
     */
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

    /**
     * Create a MultiPolygon from given set of rings.
     * @param rings to create MultiPolygon from
     * @return created MultiPolygon
     */
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

    /**
     * Compute geometry bounding box.
     *
     * @param geom to compute
     * @return geometry bounding box
     */
    protected GeneralEnvelope getLinesBoundingBox(Geometry geom) {
        final List<LineString> lines = extractRings(geom);

        int nbOrdinate = 0;
        GeneralEnvelope env = null;

        for (int k = 0, kn = lines.size(); k < kn; k++) {
            final LineString line = lines.get(k);
            final CoordinateSequence cs = line.getCoordinateSequence();

            if (nbOrdinate == 0) {
                nbOrdinate = cs.getDimension();
            }

            for (int i = 0, n = cs.size(); i < n; i++) {
                if (env == null) {
                    env = new GeneralEnvelope(nbOrdinate);
                    switch (nbOrdinate) {
                        case 4 :
                            double m = cs.getOrdinate(i, 3);
                            env.setRange(3, m, m);
                        case 3 :
                            double z = cs.getOrdinate(i, 2);
                            env.setRange(2, z, z);
                        case 2 :
                            double y = cs.getOrdinate(i, 1);
                            env.setRange(1, y, y);
                            double x = cs.getOrdinate(i, 0);
                            env.setRange(0, x, x);
                    }
                } else {
                    switch (nbOrdinate) {
                        case 4 :
                            env.add(new GeneralDirectPosition(cs.getOrdinate(i,0), cs.getOrdinate(i,1), cs.getOrdinate(i,2), cs.getOrdinate(i,3))); break;
                        case 3 :
                            env.add(new GeneralDirectPosition(cs.getOrdinate(i,0), cs.getOrdinate(i,1), cs.getOrdinate(i,2))); break;
                        case 2 :
                            env.add(new GeneralDirectPosition(cs.getOrdinate(i,0), cs.getOrdinate(i,1))); break;
                    }
                }
            }
        }
        return env;
    }

    private static class Null extends ShapeGeometryEncoder<Geometry> {

        private static final Null INSTANCE = new Null();

        private Null() {
            super(ShapeType.NULL, Geometry.class, 2, 0);
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 0;
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            return true;
        }

        @Override
        public void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
        }

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            return new GeneralEnvelope(0);
        }
    }

    private static class PointXY extends ShapeGeometryEncoder<Point> {

        private static final PointXY INSTANCE = new PointXY();

        private PointXY() {
            super(ShapeType.POINT, Point.class, 2,0);
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            final double x = channel.readDouble();
            if (filter != null && (x < filter.x || x > (filter.x + filter.width)) ) return false;
            final double y = channel.readDouble();
            if (filter != null && (y < filter.y || y > (filter.y + filter.height)) ) return false;
            shape.bbox = new GeneralEnvelope(2);
            shape.bbox.setRange(0, x, x);
            shape.bbox.setRange(1, y, y);
            shape.geometry = GF.createPoint(new CoordinateXY(x, y));
            return true;
        }

        @Override
        public void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
            final Point pt = (Point) shape.geometry;
            final Coordinate coord = pt.getCoordinate();
            final double[] xy = new double[]{coord.getX(), coord.getY()};
            channel.writeDoubles(xy);
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 2*8; //2 ordinates
        }

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            final Point pt = (Point) geom;
            final Coordinate coord = pt.getCoordinate();
            final double[] xy = new double[]{coord.getX(), coord.getY()};
            return new GeneralEnvelope(xy, xy);
        }
    }

    private static class PointXYM extends ShapeGeometryEncoder<Point> {

        private static final PointXYM INSTANCE = new PointXYM();
        private PointXYM() {
            super(ShapeType.POINT_M, Point.class, 2, 1);
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            final double x = channel.readDouble();
            if (filter != null && (x < filter.x || x > (filter.x + filter.width)) ) return false;
            final double y = channel.readDouble();
            if (filter != null && (y < filter.y || y > (filter.y + filter.height)) ) return false;
            final double z = channel.readDouble();
            shape.bbox = new GeneralEnvelope(3);
            shape.bbox.setRange(0, x, x);
            shape.bbox.setRange(1, y, y);
            shape.bbox.setRange(2, z, z);
            shape.geometry = GF.createPoint(new CoordinateXYM(x, y, z));
            return true;
        }

        @Override
        public void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
            final Point pt = (Point) shape.geometry;
            final Coordinate coord = pt.getCoordinate();
            final double[] xym = new double[]{coord.getX(), coord.getY(), coord.getM()};
            channel.writeDoubles(xym);
            if (shape.bbox == null) {
                shape.bbox = new GeneralEnvelope(xym,xym);
            }
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 3*8; //3 ordinates
        }

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            final Point pt = (Point) geom;
            final Coordinate coord = pt.getCoordinate();
            final double[] xym = new double[]{coord.getX(), coord.getY(), coord.getM()};
            return new GeneralEnvelope(xym, xym);
        }
    }

    private static class PointXYZM extends ShapeGeometryEncoder<Point> {

        private static final PointXYZM INSTANCE = new PointXYZM();

        private PointXYZM() {
            super(ShapeType.POINT_ZM, Point.class, 3, 1);
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            final double x = channel.readDouble();
            if (filter != null && (x < filter.x || x > (filter.x + filter.width)) ) return false;
            final double y = channel.readDouble();
            if (filter != null && (y < filter.y || y > (filter.y + filter.height)) ) return false;
            final double z = channel.readDouble();
            final double m = channel.readDouble();
            shape.bbox = new GeneralEnvelope(4);
            shape.bbox.setRange(0, x, x);
            shape.bbox.setRange(1, y, y);
            shape.bbox.setRange(2, z, z);
            shape.bbox.setRange(3, m, m);
            shape.geometry = GF.createPoint(new CoordinateXYZM(x, y, z, m));
            return true;
        }

        @Override
        public void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
            final Point pt = (Point) shape.geometry;
            final Coordinate coord = pt.getCoordinate();
            final double[] xyzm = new double[]{coord.getX(), coord.getY(), coord.getZ(), coord.getM()};
            channel.writeDoubles(xyzm);
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 4*8; //4 ordinates
        }

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            final Point pt = (Point) geom;
            final Coordinate coord = pt.getCoordinate();
            final double[] xyzm = new double[]{coord.getX(), coord.getY(), coord.getZ(), coord.getM()};
            return new GeneralEnvelope(xyzm, xyzm);
        }
    }

    private static class MultiPointXY extends ShapeGeometryEncoder<MultiPoint> {

        private static final MultiPointXY INSTANCE = new MultiPointXY();
        private MultiPointXY() {
            super(ShapeType.MULTIPOINT, MultiPoint.class, 2, 0);
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            if (!readBBox2D(channel, shape, filter)) return false;
            int nbPt = channel.readInt();
            final double[] coords = channel.readDoubles(nbPt * 2);
            shape.geometry = GF.createMultiPoint(new PackedCoordinateSequence.Double(coords,2,0));
            return true;
        }

        @Override
        public void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
            writeBBox2D(channel, shape);
            final MultiPoint geometry = (MultiPoint) shape.geometry;
            final int nbPts = geometry.getNumGeometries();
            channel.writeInt(nbPts);
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                final double[] xy = new double[]{pt.getX(), pt.getY()};
                channel.writeDoubles(xy);
            }
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 4 * 8 //bbox
                 + 4 //nbPts
                 + ((MultiPoint) geom).getNumGeometries() * 2 * 8;
        }

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            final GeneralEnvelope env = new GeneralEnvelope(2);
            final MultiPoint pts = (MultiPoint) geom;
            for (int i = 0, n = pts.getNumGeometries(); i < n; i++) {
                final Point pt = (Point)pts.getGeometryN(i);
                final Coordinate coord = pt.getCoordinate();
                final double[] xy = new double[]{coord.getX(), coord.getY()};
                if (i == 0) env.setEnvelope(xy[0], xy[1], xy[0], xy[1]);
                else env.add(new GeneralDirectPosition(xy));
            }
            return env;
        }
    }

    private static class MultiPointXYM extends ShapeGeometryEncoder<MultiPoint> {

        private static final MultiPointXYM INSTANCE = new MultiPointXYM();

        private MultiPointXYM() {
            super(ShapeType.MULTIPOINT_M, MultiPoint.class, 2, 1);
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            if (!readBBox2D(channel, shape, filter)) return false;
            int nbPt = channel.readInt();
            final double[] coords = new double[nbPt * 3];
            for (int i = 0; i < nbPt; i++) {
                coords[i * 3    ] = channel.readDouble();
                coords[i * 3 + 1] = channel.readDouble();
            }
            shape.bbox.setRange(2, channel.readDouble(), channel.readDouble());
            for (int i = 0; i < nbPt; i++) {
                coords[i * 3 + 2] = channel.readDouble();
            }
            shape.geometry = GF.createMultiPoint(new PackedCoordinateSequence.Double(coords, 2, 1));
            return true;
        }

        @Override
        public void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
            writeBBox2D(channel, shape);
            final MultiPoint geometry = (MultiPoint) shape.geometry;
            final int nbPts = geometry.getNumGeometries();
            channel.writeInt(nbPts);
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                final double[] xy = new double[]{pt.getX(), pt.getY()};
                channel.writeDoubles(xy);
            }
            final double[] m = new double[nbPts];
            double minM = Double.MAX_VALUE;
            double maxM = -Double.MAX_VALUE;
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                m[i] = pt.getCoordinate().getM();
                minM = Double.min(minM, m[i]);
                maxM = Double.max(maxM, m[i]);
            }
            channel.writeDouble(minM);
            channel.writeDouble(maxM);
            channel.writeDoubles(m);
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 6 * 8 //bbox
                 + 4 //nbPts
                 + ((MultiPoint) geom).getNumGeometries() * 3 * 8;
        }

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            final GeneralEnvelope env = new GeneralEnvelope(3);
            final MultiPoint pts = (MultiPoint) geom;
            for (int i = 0, n = pts.getNumGeometries(); i < n; i++) {
                final Point pt = (Point)pts.getGeometryN(i);
                final Coordinate coord = pt.getCoordinate();
                final double[] xym = new double[]{coord.getX(), coord.getY(), coord.getM()};
                if (i == 0) env.setEnvelope(xym[0], xym[1], xym[2], xym[0], xym[1], xym[2]);
                else env.add(new GeneralDirectPosition(xym));
            }
            return env;
        }
    }

    private static class MultiPointXYZM extends ShapeGeometryEncoder<MultiPoint> {

        private static final MultiPointXYZM INSTANCE = new MultiPointXYZM();

        private MultiPointXYZM() {
            super(ShapeType.MULTIPOINT_ZM, MultiPoint.class, 3, 1);
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            if (!readBBox2D(channel, shape, filter)) return false;
            int nbPt = channel.readInt();
            final double[] coords = new double[nbPt * 4];
            for (int i = 0; i < nbPt; i++) {
                coords[i * 4    ] = channel.readDouble();
                coords[i * 4 + 1] = channel.readDouble();
            }
            shape.bbox.setRange(2, channel.readDouble(), channel.readDouble());
            for (int i = 0; i < nbPt; i++) {
                coords[i * 4 + 2] = channel.readDouble();
            }
            shape.bbox.setRange(3, channel.readDouble(), channel.readDouble());
            for (int i = 0; i < nbPt; i++) {
                coords[i * 4 + 3] = channel.readDouble();
            }
            shape.geometry = GF.createMultiPoint(new PackedCoordinateSequence.Double(coords, 3, 1));
            return true;
        }

        @Override
        public void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
            writeBBox2D(channel, shape);
            final MultiPoint geometry = (MultiPoint) shape.geometry;
            final int nbPts = geometry.getNumGeometries();
            channel.writeInt(nbPts);
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                channel.writeDouble(pt.getX());
                channel.writeDouble(pt.getY());
            }

            final double[] z = new double[nbPts];
            double minZ = Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                z[i] = pt.getCoordinate().getZ();
                minZ = Double.min(minZ, z[i]);
                maxZ = Double.max(maxZ, z[i]);
            }
            channel.writeDouble(minZ);
            channel.writeDouble(maxZ);
            channel.writeDoubles(z);


            final double[] m = new double[nbPts];
            double minM = Double.MAX_VALUE;
            double maxM = -Double.MAX_VALUE;
            for (int i = 0; i < nbPts; i++) {
                final Point pt = (Point) geometry.getGeometryN(i);
                m[i] = pt.getCoordinate().getM();
                minM = Double.min(minM, m[i]);
                maxM = Double.max(maxM, m[i]);
            }
            channel.writeDouble(minM);
            channel.writeDouble(maxM);
            channel.writeDoubles(m);
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            return 8 * 8 //box
                 + 4 //nbPts
                 + ((MultiPoint) geom).getNumGeometries() * 4 * 8;
        }

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            final GeneralEnvelope env = new GeneralEnvelope(4);
            final MultiPoint pts = (MultiPoint) geom;
            for (int i = 0, n = pts.getNumGeometries(); i < n; i++) {
                final Point pt = (Point)pts.getGeometryN(i);
                final Coordinate coord = pt.getCoordinate();
                final double[] xyzm = new double[]{coord.getX(), coord.getY(), coord.getZ(), coord.getM()};
                if (i == 0) env.setEnvelope(xyzm[0], xyzm[1], xyzm[2], xyzm[3], xyzm[0], xyzm[1], xyzm[2], xyzm[3]);
                else env.add(new GeneralDirectPosition(xyzm));
            }
            return env;
        }
    }

    private static class Polyline extends ShapeGeometryEncoder<MultiLineString> {

        private static final Polyline INSTANCE = new Polyline(ShapeType.POLYLINE, 2, 0);
        private static final Polyline INSTANCE_M = new Polyline(ShapeType.POLYLINE_M, 3, 0);
        private static final Polyline INSTANCE_ZM = new Polyline(ShapeType.POLYLINE_ZM, 3, 1);

        private Polyline(ShapeType shapeType, int dimension, int measures) {
            super(shapeType, MultiLineString.class, dimension, measures);
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            final LineString[] lines = readLines(channel, shape, filter, false);
            if (lines == null) return false;
            shape.geometry = GF.createMultiLineString(lines);
            return true;
        }

        @Override
        public void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
            writeLines(channel, shape);
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

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            return getLinesBoundingBox(geom);
        }
    }

    private static class Polygon extends ShapeGeometryEncoder<MultiPolygon> {

        private static final Polygon INSTANCE = new Polygon(ShapeType.POLYGON, 2, 0);
        private static final Polygon INSTANCE_M = new Polygon(ShapeType.POLYGON_M, 3, 0);
        private static final Polygon INSTANCE_ZM = new Polygon(ShapeType.POLYGON_ZM, 3, 1);

        private Polygon(ShapeType shapeType, int dimension, int measures) {
            super(shapeType, MultiPolygon.class, dimension, measures);
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            final LineString[] rings = readLines(channel, shape, filter, true);
            if (rings == null) return false;
            shape.geometry = rebuild(Stream.of(rings).map(LinearRing.class::cast).collect(Collectors.toList()));
            return true;
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

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            return getLinesBoundingBox(geom);
        }
    }

    private static class MultiPatch extends ShapeGeometryEncoder<MultiPolygon> {

        private static final MultiPatch INSTANCE = new MultiPatch();

        private MultiPatch() {
            super(ShapeType.MULTIPATCH_ZM, MultiPolygon.class, 3, 1);
        }

        @Override
        public boolean decode(ChannelDataInput channel, ShapeRecord shape, Rectangle2D.Double filter) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void encode(ChannelDataOutput channel, ShapeRecord shape) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getEncodedLength(Geometry geom) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public GeneralEnvelope getBoundingBox(Geometry geom) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
