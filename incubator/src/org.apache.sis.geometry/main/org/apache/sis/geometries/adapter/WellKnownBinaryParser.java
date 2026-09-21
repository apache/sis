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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.curve.CompoundCurve;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.MultiCurve;
import org.apache.sis.geometries.curve.MultiLineString;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.MultiPolygon;
import org.apache.sis.geometries.surface.MultiSurface;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.geometries.surface.TIN;
import org.apache.sis.geometries.surface.Triangle;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;


/**
 * Recursive descent parser of the Well-Known Binary representation of geometries.
 * One instance parses one byte sequence and is then discarded.
 *
 * <p>The structure of the bodies, the set of accepted type codes and the handling of the
 * {@code Z}, {@code M} and {@code ZM} flags are documented on {@link WellKnownBinary}, which is
 * the public face of this class. Everything below is that structure written as one method per
 * type.</p>
 *
 * <h2>Dimensions</h2>
 * A Well-Known Binary is homogeneous: a single geometry cannot mix 2- and 3-dimensional positions,
 * because an Apache SIS {@link DataPoints} reports one dimension only. Unlike the text form, every
 * element states its flags, so nothing has to be inferred; but the flags of the outermost element
 * still fix them for the whole sequence, and a nested element contradicting them is an error.
 *
 * <h2>Byte order</h2>
 * The order is not parser state: each element carries its own flag and the standard allows them to
 * differ, so {@link #bigEndian} is re-read at the start of every element and applies until the next
 * one is read.
 *
 * <h2>Spatial reference identifier</h2>
 * The identifier belongs to the {@link WellKnownBinary.Flavor#EWKB} dialect alone, where a bit of
 * the type code announces it. It describes the whole sequence rather than the element which
 * carries it, so the outermost geometry is the one expected to hold it.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class WellKnownBinaryParser {
    /**
     * Bits of the dimension flags, which are the thousands of a type code.
     */
    private static final int FLAG_Z = 1, FLAG_M = 2;

    /**
     * Sample system of the {@link DataPointsType#ATT_M measure} attribute, which is a single
     * value with no coordinate reference system of its own.
     */
    private static final SampleSystem MEASURE_SYSTEM = SampleSystem.ofSize(1);

    /**
     * The bytes being parsed.
     */
    private final byte[] data;

    /**
     * The coordinate reference system given by the caller, or {@code null} for deriving one from
     * the spatial reference identifier, or failing that from the number of ordinates.
     */
    private final CoordinateReferenceSystem userCRS;

    /**
     * The dialect being parsed, which decides how the dimension flags are read and whether a
     * spatial reference identifier is allowed.
     */
    private final WellKnownBinary.Flavor flavor;

    /**
     * The spatial reference identifier the bytes carry, or {@link Srid#UNDEFINED} if they carry
     * none. Always {@link Srid#UNDEFINED} in the {@code OGC} dialect, which has no field for it.
     */
    private int srid;

    /**
     * The coordinate reference system of {@link #srid}, resolved by the first call to
     * {@link #crs()} which needs it. Null as long as it has not been resolved.
     */
    private CoordinateReferenceSystem sridCRS;

    /**
     * Number of dimensions {@link #sridCRS} was resolved for, since a two-dimensional identifier
     * gives a different system depending on whether the positions carry a <var>z</var> ordinate.
     */
    private int sridDimension;

    /**
     * Index in {@link #data} of the next byte to read.
     */
    private int pos;

    /**
     * Whether the multi-byte values of the element being read are most significant byte first.
     */
    private boolean bigEndian;

    /**
     * Whether {@link #hasZ} and {@link #hasM} have been established by the flags of an element.
     */
    private boolean flagsKnown;

    /**
     * Whether positions have a third ordinate, and whether they carry a measure.
     */
    private boolean hasZ, hasM;

    /**
     * Creates a parser for the given bytes.
     *
     * @param  data    the Well-Known Binary to parse.
     * @param  crs     the coordinate reference system to give to the geometries, or {@code null}.
     * @param  flavor  the dialect to parse.
     */
    WellKnownBinaryParser(final byte[] data, final CoordinateReferenceSystem crs,
                          final WellKnownBinary.Flavor flavor)
    {
        this.data = data;
        this.userCRS = crs;
        this.flavor = flavor;
    }

    /**
     * Parses the whole byte sequence as a single geometry.
     *
     * @throws IllegalArgumentException if the bytes are malformed or name an unsupported type.
     */
    Geometry parse() {
        final Geometry geometry = parseGeometry();
        if (pos < data.length) {
            throw error("Unexpected bytes after the end of the geometry");
        }
        return geometry;
    }

    // ////////////////////////////////////////////////////////////////////////
    // Productions ////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Parses one complete geometry: a byte order flag, a type code and a body.
     */
    private Geometry parseGeometry() {
        readByteOrder();
        final int type = readInt();
        final boolean extended = (flavor == WellKnownBinary.Flavor.EWKB);
        int code = type;
        int flags = 0;
        if (extended) {
            /*
             * The high order bits are the dimension flags and the presence of an identifier.
             * The base which remains may still carry the thousands of the OGC dialect, which
             * this dialect understands as well, so that a plain Well-Known Binary decodes here.
             */
            if ((type & WellKnownBinary.EWKB_Z) != 0) flags |= FLAG_Z;
            if ((type & WellKnownBinary.EWKB_M) != 0) flags |= FLAG_M;
            code = type & WellKnownBinary.EWKB_BASE_MASK;
        } else if (code < 0) {
            throw error("Type code " + Integer.toUnsignedString(code) + " is out of range."
                    + " The extended Well-Known Binary, which puts the dimension flags in the"
                    + " high order bits, is read by the " + WellKnownBinary.Flavor.EWKB + " flavor");
        }
        final int thousands = code / 1000;
        if (thousands > 3) {
            throw error("Type code " + code + " has no dimension flag: the thousands must be"
                    + " 0 for XY, 1 for Z, 2 for M or 3 for ZM");
        }
        flags |= thousands;
        applyFlags(flags);
        if (extended && (type & WellKnownBinary.EWKB_SRID) != 0) {
            readSrid();
        }
        switch (code % 1000) {
            case WellKnownBinary.Codes.POINT:               return parsePoint();
            case WellKnownBinary.Codes.LINESTRING:          return GeometryFactory.createLineString(readPointList());
            case WellKnownBinary.Codes.CIRCULARSTRING:      return GeometryFactory.createCircularString(readPointList());
            case WellKnownBinary.Codes.COMPOUNDCURVE:       return parseCompoundCurve();
            case WellKnownBinary.Codes.POLYGON:             return readPolygonBody();
            case WellKnownBinary.Codes.TRIANGLE:            return readTriangleBody();
            case WellKnownBinary.Codes.CURVEPOLYGON:        return parseCurvePolygon();
            case WellKnownBinary.Codes.POLYHEDRALSURFACE:   return parsePolyhedralSurface();
            case WellKnownBinary.Codes.TIN:                 return parseTIN();
            case WellKnownBinary.Codes.MULTIPOINT:          return parseMultiPoint();
            case WellKnownBinary.Codes.MULTILINESTRING:     return parseMultiLineString();
            case WellKnownBinary.Codes.MULTICURVE:          return parseMultiCurve();
            case WellKnownBinary.Codes.MULTIPOLYGON:        return parseMultiPolygon();
            case WellKnownBinary.Codes.MULTISURFACE:        return parseMultiSurface();
            case WellKnownBinary.Codes.GEOMETRYCOLLECTION:  return parseGeometryCollection();
            default: throw error("Well-Known Binary defines no geometry type of code " + code);
        }
    }

    /**
     * Reads the spatial reference identifier which follows a type code whose
     * {@link WellKnownBinary#EWKB_SRID} bit is set.
     *
     * <p>Only the outermost geometry is expected to carry one, but a nested geometry repeating it
     * is accepted as long as it repeats the same value: the whole sequence describes positions in
     * a single system, so two different identifiers would contradict each other.</p>
     */
    private void readSrid() {
        final int declared = readInt();
        if (srid == Srid.UNDEFINED) {
            srid = declared;
        } else if (srid != declared) {
            throw error("A nested geometry declares the spatial reference identifier " + declared
                    + " while the enclosing one declares " + srid);
        }
    }

    /**
     * Parses the single coordinate tuple of a point.
     *
     * <p>A tuple of {@link Double#NaN} ordinates is the usual way of writing an empty point, and
     * does not map to a {@link Point}: the Apache SIS model has no empty point, since a point
     * sequence of length 0 is not a valid position of a point. It maps to {@link Empty} instead,
     * the geometry which stands for the empty point set whatever its type, and which is written
     * back as an empty {@code GEOMETRYCOLLECTION}.</p>
     */
    private Geometry parsePoint() {
        final double[] tuple = readTuple();
        for (final double ordinate : tuple) {
            if (!Double.isNaN(ordinate)) {
                final Coordinates c = new Coordinates();
                c.add(tuple);
                return GeometryFactory.createPoint(c.build());
            }
        }
        return GeometryFactory.createEmpty(crs());
    }

    /**
     * Parses a count of components followed by that many complete curves.
     */
    private CompoundCurve parseCompoundCurve() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.createCompoundCurve(crs());
        }
        final Curve[] curves = new Curve[n];
        for (int i = 0; i < n; i++) {
            curves[i] = readMember(Curve.class, CompoundCurve.TYPE);
        }
        return GeometryFactory.createCompoundCurve(curves);
    }

    /**
     * Parses a count of rings followed by that many complete curves, the first one being the
     * exterior ring. A count of zero is the empty curve polygon.
     */
    private CurvePolygon parseCurvePolygon() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.createCurvePolygon(emptyRing(), List.of());
        }
        final Curve exterior = readMember(Curve.class, CurvePolygon.TYPE);
        final List<Curve> interiors = new ArrayList<>(n - 1);
        for (int i = 1; i < n; i++) {
            interiors.add(readMember(Curve.class, CurvePolygon.TYPE));
        }
        return GeometryFactory.createCurvePolygon(exterior, interiors);
    }

    /**
     * Parses a count of patches followed by that many complete polygons.
     */
    private PolyhedralSurface<Polygon> parsePolyhedralSurface() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.createPolyhedralSurface(crs(), new Polygon[0]);
        }
        final Polygon[] patches = new Polygon[n];
        for (int i = 0; i < n; i++) {
            patches[i] = readMember(Polygon.class, PolyhedralSurface.TYPE);
        }
        return GeometryFactory.createPolyhedralSurface(patches);
    }

    /**
     * Parses a count of patches followed by that many complete triangles.
     */
    private TIN parseTIN() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.createTIN(crs(), new Triangle[0]);
        }
        final Triangle[] patches = new Triangle[n];
        for (int i = 0; i < n; i++) {
            patches[i] = readMember(Triangle.class, TIN.TYPE);
        }
        return GeometryFactory.createTIN(patches);
    }

    /**
     * Parses a count of members followed by that many complete points.
     */
    private MultiPoint<?> parseMultiPoint() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.createMultiPoint(crs());
        }
        final Point[] members = new Point[n];
        for (int i = 0; i < n; i++) {
            // See parsePoint(): an empty point decodes to Empty, which is not a Point.
            members[i] = readMember(Point.class, MultiPoint.TYPE);
        }
        return GeometryFactory.createMultiPoint(members);
    }

    /**
     * Parses a count of members followed by that many complete line strings.
     */
    private MultiLineString parseMultiLineString() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.createMultiLineString(crs());
        }
        final LineString[] members = new LineString[n];
        for (int i = 0; i < n; i++) {
            members[i] = readMember(LineString.class, MultiLineString.TYPE);
        }
        return GeometryFactory.createMultiLineString(members);
    }

    /**
     * Parses a count of members followed by that many complete polygons.
     */
    private MultiPolygon parseMultiPolygon() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.createMultiPolygon(crs());
        }
        final Polygon[] members = new Polygon[n];
        for (int i = 0; i < n; i++) {
            members[i] = readMember(Polygon.class, MultiPolygon.TYPE);
        }
        return GeometryFactory.createMultiPolygon(members);
    }

    /**
     * Parses a count of members followed by that many complete curves, which unlike those of a
     * {@code MULTILINESTRING} may be of any curve type.
     */
    private MultiCurve<Curve> parseMultiCurve() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.<Curve>createMultiCurve(crs());
        }
        final Curve[] members = new Curve[n];
        for (int i = 0; i < n; i++) {
            members[i] = readMember(Curve.class, MultiCurve.TYPE);
        }
        return GeometryFactory.createMultiCurve(members);
    }

    /**
     * Parses a count of members followed by that many complete surfaces, which unlike those of a
     * {@code MULTIPOLYGON} may be of any surface type.
     */
    private MultiSurface<Surface> parseMultiSurface() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.<Surface>createMultiSurface(crs());
        }
        final Surface[] members = new Surface[n];
        for (int i = 0; i < n; i++) {
            members[i] = readMember(Surface.class, MultiSurface.TYPE);
        }
        return GeometryFactory.createMultiSurface(members);
    }

    /**
     * Parses a count of members followed by that many complete geometries of any type.
     */
    private GeometryCollection<Geometry> parseGeometryCollection() {
        final int n = readCount();
        if (n == 0) {
            return GeometryFactory.<Geometry>createGeometryCollection(crs());
        }
        final Geometry[] members = new Geometry[n];
        for (int i = 0; i < n; i++) {
            members[i] = parseGeometry();
        }
        return GeometryFactory.createGeometryCollection(members);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Shared productions /////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Parses a count of rings followed by that many coordinate lists, without header.
     * A count of zero is the empty polygon.
     */
    private Polygon readPolygonBody() {
        final List<LinearRing> rings = readRings();
        if (rings.isEmpty()) {
            return GeometryFactory.createPolygon(emptyRing(), List.of());
        }
        return GeometryFactory.createPolygon(rings.get(0), new ArrayList<>(rings.subList(1, rings.size())));
    }

    /**
     * Parses the single ring of a triangle, in the same form as the rings of a polygon.
     */
    private Triangle readTriangleBody() {
        final List<LinearRing> rings = readRings();
        if (rings.isEmpty()) {
            return GeometryFactory.createTriangle(emptyRing());
        }
        if (rings.size() != 1) {
            throw error("A " + Triangle.TYPE + " patch has no interior ring, but " + (rings.size() - 1) + " were given");
        }
        return GeometryFactory.createTriangle(rings.get(0));
    }

    /**
     * Parses a count of rings followed by that many coordinate lists.
     */
    private List<LinearRing> readRings() {
        final int n = readCount();
        final List<LinearRing> rings = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            rings.add(GeometryFactory.createLinearRing(readPointList()));
        }
        return rings;
    }

    /**
     * Parses a count of coordinate tuples followed by that many tuples.
     */
    private DataPoints readPointList() {
        final int n = readCount();
        final Coordinates coordinates = new Coordinates();
        for (int i = 0; i < n; i++) {
            coordinates.add(readTuple());
        }
        return coordinates.build();
    }

    /**
     * Returns an empty ring, for the zero counts of the types which are made of rings.
     */
    private LinearRing emptyRing() {
        return GeometryFactory.createLinearRing(new Coordinates().build());
    }

    /**
     * Parses one complete geometry and casts it to the type its container requires.
     *
     * @param  type       the type the container accepts.
     * @param  container  name of the container type, for the error message.
     */
    private <T> T readMember(final Class<T> type, final String container) {
        final Geometry geometry = parseGeometry();
        if (type.isInstance(geometry)) {
            return type.cast(geometry);
        }
        throw error("A " + container + " cannot contain a " + geometry.getGeometryType());
    }

    // ////////////////////////////////////////////////////////////////////////
    // Dimensions /////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Returns the number of ordinates of a position, which is 2 unless the bytes have established
     * that positions carry a <var>z</var> ordinate.
     */
    private int positionDimension() {
        return (flagsKnown && hasZ) ? 3 : 2;
    }

    /**
     * Returns the coordinate reference system to give to the geometries, which is the one the
     * caller supplied if any, and an {@linkplain Geometries#getUndefinedCRS(int) undefined} one
     * of the right dimension otherwise.
     *
     * @throws IllegalArgumentException if the caller supplied a system whose dimension
     *         contradicts the number of ordinates announced by the flags.
     */
    private CoordinateReferenceSystem crs() {
        final int dimension = positionDimension();
        if (userCRS != null) {
            final int actual = userCRS.getCoordinateSystem().getDimension();
            if (actual != dimension) {
                throw error("The given coordinate reference system has " + actual + " dimensions,"
                        + " but the bytes have " + dimension + " ordinates per position");
            }
            return userCRS;
        }
        if (srid != Srid.UNDEFINED) {
            /*
             * Srid.forCode(…) returns a system of exactly the requested number of dimensions,
             * adding an ellipsoidal height to a two-dimensional one when the type code has the
             * Z flag. The dimension is therefore part of the cache key.
             */
            if (sridCRS == null || sridDimension != dimension) {
                sridCRS = Srid.forCode(srid, dimension);
                sridDimension = dimension;
            }
            return sridCRS;
        }
        return Geometries.getUndefinedCRS(dimension);
    }

    /**
     * Records the dimension flags of an element. The outermost element fixes them for the whole
     * sequence; a nested element may repeat them but not contradict them.
     */
    private void applyFlags(final int flags) {
        final boolean z = (flags & FLAG_Z) != 0;
        final boolean m = (flags & FLAG_M) != 0;
        if (flagsKnown) {
            if (z != hasZ || m != hasM) {
                throw error("Dimension flag " + flagName(z, m) + " contradicts the "
                        + flagName(hasZ, hasM) + " established by the enclosing geometry");
            }
        } else {
            hasZ = z;
            hasM = m;
            flagsKnown = true;
        }
    }

    /**
     * Returns {@code "Z"}, {@code "M"}, {@code "ZM"} or {@code "XY"} for an error message.
     */
    private static String flagName(final boolean z, final boolean m) {
        if (z) return m ? "ZM" : "Z";
        return m ? "M" : "XY";
    }

    /**
     * Accumulator of the coordinate tuples of one element, and factory of the
     * {@link DataPoints} the Apache SIS geometry model is built upon.
     *
     * <p>Ordinates are accumulated in a single flat array in row-major order, which is the layout
     * {@link NDArrays#of(SampleSystem, double...)} expects. The measure, when there is one, is the
     * last ordinate of each tuple in the bytes but a separate attribute in the model, so
     * {@link #build()} splits the two apart.</p>
     */
    private final class Coordinates {
        /**
         * The accumulated ordinates. Only the first {@link #count} elements are meaningful.
         */
        private double[] values = new double[12];

        /**
         * Number of meaningful ordinates in {@link #values}, always a multiple of {@link #width}.
         */
        private int count;

        /**
         * Number of ordinates per tuple, or 0 if no tuple has been added yet.
         */
        private int width;

        /**
         * Appends one coordinate tuple. Every tuple has the width the dimension flags announce,
         * since {@link WellKnownBinaryParser#readTuple()} reads exactly that many ordinates.
         */
        void add(final double[] tuple) {
            width = tuple.length;
            if (count + width > values.length) {
                values = Arrays.copyOf(values, Math.max(count + width, values.length * 2));
            }
            System.arraycopy(tuple, 0, values, count, width);
            count += width;
        }

        /**
         * Builds the point sequence of the accumulated tuples, possibly empty.
         */
        DataPoints build() {
            final int posWidth = positionDimension();
            final SampleSystem posSystem = SampleSystem.of(crs());
            final int size = (width != 0) ? count / width : 0;
            if (size == 0) {
                final Array empty = NDArrays.of(posSystem, DataType.DOUBLE, 0);
                if (!hasM) {
                    return GeometryFactory.createSequence(empty);
                }
                return createSequence(empty, NDArrays.of(MEASURE_SYSTEM, DataType.DOUBLE, 0));
            }
            if (!hasM) {
                return GeometryFactory.createSequence(NDArrays.of(posSystem, Arrays.copyOf(values, count)));
            }
            /*
             * The measure is the ordinate following the position ones in each tuple of the bytes,
             * but a one dimensional attribute of its own in the model.
             */
            final double[] p = new double[size * posWidth];
            final double[] m = new double[size];
            for (int i = 0; i < size; i++) {
                System.arraycopy(values, i * width, p, i * posWidth, posWidth);
                m[i] = values[i * width + posWidth];
            }
            return createSequence(NDArrays.of(posSystem, p), NDArrays.of(MEASURE_SYSTEM, m));
        }

        /**
         * Returns a sequence holding both the positions and the measures.
         */
        private DataPoints createSequence(final Array positions, final Array measures) {
            final Map<String,Array> attributes = new LinkedHashMap<>(4);
            attributes.put(DataPointsType.ATT_POSITION, positions);
            attributes.put(DataPointsType.ATT_M, measures);
            return GeometryFactory.createSequence(attributes);
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Primitives /////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Reads the ordinates of one coordinate tuple. Their number is not written anywhere:
     * it is the one the dimension flags of the element announce.
     */
    private double[] readTuple() {
        final double[] tuple = new double[positionDimension() + (hasM ? 1 : 0)];
        for (int i = 0; i < tuple.length; i++) {
            tuple[i] = readDouble();
        }
        return tuple;
    }

    /**
     * Reads a count of elements, and verifies that the bytes which remain could hold that many.
     * The check costs nothing and keeps a malformed count from asking for a huge allocation:
     * no element of any type is written in less than one byte.
     */
    private int readCount() {
        final int count = readInt();
        if (count < 0 || count > data.length - pos) {
            throw error("A count of " + Integer.toUnsignedString(count) + " elements exceeds the "
                    + (data.length - pos) + " bytes which remain");
        }
        return count;
    }

    /**
     * Reads the flag which tells in which order the values after it are written.
     */
    private void readByteOrder() {
        final byte flag = readByte();
        switch (flag) {
            case WellKnownBinary.XDR: bigEndian = true;  break;
            case WellKnownBinary.NDR: bigEndian = false; break;
            default: throw error("Byte order flag " + flag + " is neither 0 for big endian nor 1 for little endian");
        }
    }

    /**
     * Reads one byte.
     */
    private byte readByte() {
        if (pos >= data.length) {
            throw error("Unexpected end of the geometry");
        }
        return data[pos++];
    }

    /**
     * Reads a 32 bits integer in the byte order of the element being read.
     */
    private int readInt() {
        if (pos + Integer.BYTES > data.length) {
            throw error("Unexpected end of the geometry");
        }
        int value = 0;
        for (int i = 0; i < Integer.BYTES; i++) {
            final int shift = 8 * (bigEndian ? Integer.BYTES - 1 - i : i);
            value |= (data[pos++] & 0xFF) << shift;
        }
        return value;
    }

    /**
     * Reads an IEEE 754 double precision number in the byte order of the element being read.
     */
    private double readDouble() {
        if (pos + Double.BYTES > data.length) {
            throw error("Unexpected end of the geometry");
        }
        long bits = 0;
        for (int i = 0; i < Double.BYTES; i++) {
            final int shift = 8 * (bigEndian ? Double.BYTES - 1 - i : i);
            bits |= (long) (data[pos++] & 0xFF) << shift;
        }
        return Double.longBitsToDouble(bits);
    }

    /**
     * Returns the exception to throw for malformed bytes, pointing at the current position.
     */
    private IllegalArgumentException error(final String message) {
        return new IllegalArgumentException(message + ", at offset " + pos + " of a Well-Known Binary of "
                + data.length + " bytes");
    }
}
