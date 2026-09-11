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
import java.util.Locale;
import java.util.Map;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.curve.CircularString;
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


/**
 * Recursive descent parser of the Well-Known Text representation of geometries.
 * One instance parses one text and is then discarded.
 *
 * <p>The grammar, the set of accepted keywords and the handling of the {@code Z}, {@code M} and
 * {@code ZM} flags are documented on {@link WellKnownText}, which is the public face of this
 * class. Everything below is that grammar written as one method per production.</p>
 *
 * <h2>Dimensions</h2>
 * A Well-Known Text is homogeneous: a single geometry cannot mix 2- and 3-dimensional positions,
 * because an Apache SIS {@link DataPoints} reports one dimension only. The flags are therefore
 * parser state rather than per-element state: whichever element states them first fixes them, a
 * later element contradicting them is an error, and if no element states them at all they are
 * inferred from the width of the first coordinate tuple.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class WellKnownTextParser {
    /**
     * Value returned by {@link #readFlags()} when the text carries no dimension flag.
     */
    private static final int UNSPECIFIED = -1;

    /**
     * Bits of the value returned by {@link #readFlags()}.
     */
    private static final int FLAG_Z = 1, FLAG_M = 2;

    /**
     * Sample system of the {@linkplain AttributesType#ATT_M measure} attribute, which is a single
     * value with no coordinate reference system of its own.
     */
    private static final SampleSystem MEASURE_SYSTEM = SampleSystem.ofSize(1);

    /**
     * The text being parsed.
     */
    private final String text;

    /**
     * The coordinate reference system given by the caller, or {@code null} for deriving an
     * {@linkplain Geometries#getUndefinedCRS(int) undefined} one from the number of ordinates.
     */
    private final CoordinateReferenceSystem userCRS;

    /**
     * Index in {@link #text} of the next character to read.
     */
    private int pos;

    /**
     * Whether {@link #hasZ} and {@link #hasM} have been established, either by a dimension flag
     * or by the width of a coordinate tuple.
     */
    private boolean flagsKnown;

    /**
     * Whether positions have a third ordinate, and whether they carry a measure.
     */
    private boolean hasZ, hasM;

    /**
     * Creates a parser for the given text.
     *
     * @param  text  the Well-Known Text to parse.
     * @param  crs   the coordinate reference system to give to the geometries, or {@code null}.
     */
    WellKnownTextParser(final String text, final CoordinateReferenceSystem crs) {
        this.text = text;
        this.userCRS = crs;
    }

    /**
     * Parses the whole text as a single geometry.
     *
     * @throws IllegalArgumentException if the text is malformed or names an unsupported type.
     */
    Geometry parse() {
        final Geometry geometry = parseGeometry();
        skipSpaces();
        if (pos < text.length()) {
            throw error("Unexpected text after the end of the geometry");
        }
        return geometry;
    }

    // ////////////////////////////////////////////////////////////////////////
    // Productions ////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Parses {@code <keyword> [Z|M|ZM] <body>}.
     */
    private Geometry parseGeometry() {
        final String keyword = readWord();
        if (keyword.isEmpty()) {
            throw error("Expected a geometry type keyword");
        }
        applyFlags(readFlags());
        switch (keyword) {
            case Point.TYPE:              return parsePoint();
            case LineString.TYPE:         return GeometryFactory.createLineString(readPointList());
            case CircularString.TYPE:     return GeometryFactory.createCircularString(readPointList());
            case CompoundCurve.TYPE:      return parseCompoundCurve();
            case Polygon.TYPE:            return readPolygonBody();
            case Triangle.TYPE:           return readTriangleBody();
            case CurvePolygon.TYPE:       return parseCurvePolygon();
            case PolyhedralSurface.TYPE:  return parsePolyhedralSurface();
            case TIN.TYPE:                return parseTIN();
            case MultiPoint.TYPE:         return parseMultiPoint();
            case MultiLineString.TYPE:    return parseMultiLineString();
            case MultiCurve.TYPE:         return parseMultiCurve();
            case MultiPolygon.TYPE:       return parseMultiPolygon();
            case MultiSurface.TYPE:       return parseMultiSurface();
            case GeometryCollection.TYPE:
            case "GEOMCOLLECTION":        return parseGeometryCollection();      // Alias in use in some databases.
            default: throw error("Well-Known Text defines no geometry type named \"" + keyword + '"');
        }
    }

    /**
     * Parses {@code (1 2)} or {@code EMPTY}.
     *
     * <p>{@code POINT EMPTY} does not map to a {@link Point}: the Apache SIS model has no empty
     * point, since a point sequence of length 0 is not a valid position of a point and
     * {@link GeometryFactory#createPoint(CoordinateReferenceSystem)} builds a point at the origin
     * rather than an absent one. It maps to {@link Empty} instead, the geometry which stands for
     * the empty point set whatever its type, and which is written back as
     * {@code GEOMETRYCOLLECTION EMPTY}.</p>
     */
    private Geometry parsePoint() {
        if (readEmpty()) {
            return GeometryFactory.createEmpty(crs());
        }
        return GeometryFactory.createPoint(readSinglePosition());
    }

    /**
     * Parses {@code ((1 2, 3 4), CIRCULARSTRING (3 4, 5 6, 7 8))} or {@code EMPTY}.
     */
    private CompoundCurve parseCompoundCurve() {
        if (readEmpty()) {
            return GeometryFactory.createCompoundCurve(crs());
        }
        final List<Curve> curves = new ArrayList<>();
        expect('(');
        do {
            curves.add(readCurve(false));
        } while (accept(','));
        expect(')');
        return GeometryFactory.createCompoundCurve(curves.toArray(Curve[]::new));
    }

    /**
     * Parses {@code (CIRCULARSTRING (…), (…))} or {@code EMPTY}.
     */
    private CurvePolygon parseCurvePolygon() {
        if (readEmpty()) {
            return GeometryFactory.createCurvePolygon(emptyRing(), List.of());
        }
        final List<Curve> rings = new ArrayList<>();
        expect('(');
        do {
            rings.add(readCurve(true));
        } while (accept(','));
        expect(')');
        return GeometryFactory.createCurvePolygon(rings.get(0), new ArrayList<>(rings.subList(1, rings.size())));
    }

    /**
     * Parses {@code (((…)), ((…)))} or {@code EMPTY}: a list of polygon patches written
     * without their keyword.
     */
    private PolyhedralSurface<Polygon> parsePolyhedralSurface() {
        if (readEmpty()) {
            return GeometryFactory.createPolyhedralSurface(crs(), new Polygon[0]);
        }
        final List<Polygon> patches = new ArrayList<>();
        expect('(');
        do {
            patches.add(readPolygonBody());
        } while (accept(','));
        expect(')');
        return GeometryFactory.createPolyhedralSurface(patches.toArray(Polygon[]::new));
    }

    /**
     * Parses {@code (((…)), ((…)))} or {@code EMPTY}, where every patch is a triangle.
     */
    private TIN parseTIN() {
        if (readEmpty()) {
            return GeometryFactory.createTIN(crs(), new Triangle[0]);
        }
        final List<Triangle> patches = new ArrayList<>();
        expect('(');
        do {
            patches.add(readTriangleBody());
        } while (accept(','));
        expect(')');
        return GeometryFactory.createTIN(patches.toArray(Triangle[]::new));
    }

    /**
     * Parses {@code ((1 2), (3 4))} or {@code (1 2, 3 4)} or {@code EMPTY}. The grammar allows
     * the parentheses around each point to be omitted, and both forms are accepted here.
     */
    private MultiPoint<?> parseMultiPoint() {
        if (readEmpty()) {
            return GeometryFactory.createMultiPoint(crs());
        }
        final List<Point> points = new ArrayList<>();
        expect('(');
        do {
            if (readEmpty()) {
                // See parsePoint(): there is no empty point to put in the collection.
                throw error("A " + MultiPoint.TYPE + " cannot hold an empty point");
            } else if (peek() == '(') {
                points.add(GeometryFactory.createPoint(readSinglePosition()));
            } else {
                final Coordinates c = new Coordinates();
                c.add(readTuple());
                points.add(GeometryFactory.createPoint(c.build()));
            }
        } while (accept(','));
        expect(')');
        return GeometryFactory.createMultiPoint(points.toArray(Point[]::new));
    }

    /**
     * Parses {@code ((1 2, 3 4), (5 6, 7 8))} or {@code EMPTY}.
     */
    private MultiLineString parseMultiLineString() {
        if (readEmpty()) {
            return GeometryFactory.createMultiLineString(crs());
        }
        final List<LineString> members = new ArrayList<>();
        expect('(');
        do {
            members.add(GeometryFactory.createLineString(readPointList()));
        } while (accept(','));
        expect(')');
        return GeometryFactory.createMultiLineString(members.toArray(LineString[]::new));
    }

    /**
     * Parses {@code (((…), (…)), ((…)))} or {@code EMPTY}.
     */
    private MultiPolygon parseMultiPolygon() {
        if (readEmpty()) {
            return GeometryFactory.createMultiPolygon(crs());
        }
        final List<Polygon> members = new ArrayList<>();
        expect('(');
        do {
            members.add(readPolygonBody());
        } while (accept(','));
        expect(')');
        return GeometryFactory.createMultiPolygon(members.toArray(Polygon[]::new));
    }

    /**
     * Parses {@code (CIRCULARSTRING (…), (…))} or {@code EMPTY}: unlike a
     * {@code MULTILINESTRING}, the members may be of any curve type and therefore keep their
     * keyword, except a line string which may be written as a bare coordinate list.
     */
    private MultiCurve<Curve> parseMultiCurve() {
        if (readEmpty()) {
            return GeometryFactory.<Curve>createMultiCurve(crs());
        }
        final List<Curve> members = new ArrayList<>();
        expect('(');
        do {
            members.add(readCurve(false));
        } while (accept(','));
        expect(')');
        return GeometryFactory.createMultiCurve(members.toArray(Curve[]::new));
    }

    /**
     * Parses {@code (CURVEPOLYGON (…), ((…)))} or {@code EMPTY}: as in a {@code MULTICURVE},
     * the members keep their keyword except a polygon, which may be written as a bare ring list.
     */
    private MultiSurface<Surface> parseMultiSurface() {
        if (readEmpty()) {
            return GeometryFactory.<Surface>createMultiSurface(crs());
        }
        final List<Surface> members = new ArrayList<>();
        expect('(');
        do {
            if (peek() == '(') {
                members.add(readPolygonBody());
            } else {
                members.add(expectType(parseGeometry(), Surface.class, MultiSurface.TYPE));
            }
        } while (accept(','));
        expect(')');
        return GeometryFactory.createMultiSurface(members.toArray(Surface[]::new));
    }

    /**
     * Parses {@code (POINT (1 2), LINESTRING (3 4, 5 6))} or {@code EMPTY}. Every member keeps
     * its keyword, since a collection puts no constraint on the type of its members.
     */
    private GeometryCollection<Geometry> parseGeometryCollection() {
        if (readEmpty()) {
            return GeometryFactory.<Geometry>createGeometryCollection(crs());
        }
        final List<Geometry> members = new ArrayList<>();
        expect('(');
        do {
            members.add(parseGeometry());
        } while (accept(','));
        expect(')');
        return GeometryFactory.createGeometryCollection(members.toArray(Geometry[]::new));
    }

    // ////////////////////////////////////////////////////////////////////////
    // Shared productions /////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Parses a curve written either as a bare coordinate list or with its own keyword.
     * A bare list is a line string, and is closed into a {@link LinearRing} when it is used as
     * the boundary of a {@link CurvePolygon}.
     *
     * @param  asRing  whether a bare coordinate list is a ring rather than a line string.
     */
    private Curve readCurve(final boolean asRing) {
        if (peek() == '(') {
            final DataPoints points = readPointList();
            return asRing ? GeometryFactory.createLinearRing(points)
                          : GeometryFactory.createLineString(points);
        }
        return expectType(parseGeometry(), Curve.class, asRing ? CurvePolygon.TYPE : CompoundCurve.TYPE);
    }

    /**
     * Parses {@code ((…), (…))} or {@code EMPTY}: the rings of a polygon, without keyword.
     */
    private Polygon readPolygonBody() {
        if (readEmpty()) {
            return GeometryFactory.createPolygon(emptyRing(), List.of());
        }
        final List<LinearRing> rings = readRings();
        return GeometryFactory.createPolygon(rings.get(0), new ArrayList<>(rings.subList(1, rings.size())));
    }

    /**
     * Parses {@code ((…))} or {@code EMPTY}: the single ring of a triangle, without keyword.
     */
    private Triangle readTriangleBody() {
        if (readEmpty()) {
            return GeometryFactory.createTriangle(emptyRing());
        }
        final List<LinearRing> rings = readRings();
        if (rings.size() != 1) {
            throw error("A " + Triangle.TYPE + " patch has no interior ring, but " + (rings.size() - 1) + " were given");
        }
        return GeometryFactory.createTriangle(rings.get(0));
    }

    /**
     * Parses {@code ((0 0, 1 0, 0 0), (…))}: one or more parenthesized coordinate lists.
     */
    private List<LinearRing> readRings() {
        final List<LinearRing> rings = new ArrayList<>();
        expect('(');
        do {
            rings.add(GeometryFactory.createLinearRing(readPointList()));
        } while (accept(','));
        expect(')');
        return rings;
    }

    /**
     * Parses {@code (1 2, 3 4)} or {@code EMPTY}: one parenthesized list of coordinate tuples.
     */
    private DataPoints readPointList() {
        if (readEmpty()) {
            return new Coordinates().build();
        }
        final Coordinates coordinates = new Coordinates();
        expect('(');
        do {
            coordinates.add(readTuple());
        } while (accept(','));
        expect(')');
        return coordinates.build();
    }

    /**
     * Parses {@code (1 2)}: exactly one parenthesized coordinate tuple, as in a point.
     */
    private DataPoints readSinglePosition() {
        final Coordinates coordinates = new Coordinates();
        expect('(');
        coordinates.add(readTuple());
        expect(')');
        return coordinates.build();
    }

    /**
     * Returns an empty ring, for the {@code EMPTY} form of the types which are made of rings.
     */
    private LinearRing emptyRing() {
        return GeometryFactory.createLinearRing(new Coordinates().build());
    }

    /**
     * Casts a parsed member to the type its container requires.
     */
    private <T> T expectType(final Geometry geometry, final Class<T> type, final String container) {
        if (type.isInstance(geometry)) {
            return type.cast(geometry);
        }
        throw error("A " + container + " cannot contain a " + geometry.getGeometryType());
    }

    // ////////////////////////////////////////////////////////////////////////
    // Dimensions /////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Returns the number of ordinates of a position, which is 2 unless the text has established
     * that positions carry a <var>z</var> ordinate. A text made only of {@code EMPTY} establishes
     * nothing, in which case the 2-dimensional default applies.
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
     *         contradicts the number of ordinates found in the text.
     */
    private CoordinateReferenceSystem crs() {
        final int dimension = positionDimension();
        if (userCRS == null) {
            return Geometries.getUndefinedCRS(dimension);
        }
        final int actual = userCRS.getCoordinateSystem().getDimension();
        if (actual != dimension) {
            throw error("The given coordinate reference system has " + actual + " dimensions,"
                    + " but the text has " + dimension + " ordinates per position");
        }
        return userCRS;
    }

    /**
     * Records the dimension flag of an element. The first element to state the flags fixes them
     * for the whole text; a later element may repeat them but not contradict them.
     */
    private void applyFlags(final int flags) {
        if (flags == UNSPECIFIED) {
            return;
        }
        final boolean z = (flags & FLAG_Z) != 0;
        final boolean m = (flags & FLAG_M) != 0;
        if (flagsKnown) {
            if (z != hasZ || m != hasM) {
                throw error("Dimension flag " + flagName(z, m) + " contradicts the "
                        + flagName(hasZ, hasM) + " established earlier in the text");
            }
        } else {
            hasZ = z;
            hasM = m;
            flagsKnown = true;
        }
    }

    /**
     * Establishes or verifies the dimension flags against the width of a coordinate tuple.
     * Called by {@link Coordinates} when it receives its first tuple.
     */
    private void inferFlags(final int width) {
        if (flagsKnown) {
            final int expected = positionDimension() + (hasM ? 1 : 0);
            if (width != expected) {
                throw error("A coordinate tuple of " + width + " ordinates contradicts the "
                        + flagName(hasZ, hasM) + " dimension flag, which requires " + expected);
            }
            return;
        }
        switch (width) {
            case 2: hasZ = false; break;
            case 3: hasZ = true;  break;
            default: throw error("A coordinate tuple of " + width + " ordinates is ambiguous"
                        + " without a Z, M or ZM flag");
        }
        hasM = false;
        flagsKnown = true;
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
     * last ordinate of each tuple in the text but a separate attribute in the model, so
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
         * Appends one coordinate tuple. The first one fixes the width for all the others,
         * and settles the dimension flags if the text did not state them.
         */
        void add(final double[] tuple) {
            if (width == 0) {
                width = tuple.length;
                inferFlags(width);
            } else if (tuple.length != width) {
                throw error("A coordinate tuple of " + tuple.length + " ordinates follows tuples of " + width);
            }
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
             * The measure is the ordinate following the position ones in each tuple of the text,
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
            attributes.put(AttributesType.ATT_POSITION, positions);
            attributes.put(AttributesType.ATT_M, measures);
            return GeometryFactory.createSequence(attributes);
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Tokens /////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Reads the numbers of one coordinate tuple, which are separated by spaces only.
     */
    private double[] readTuple() {
        double[] values = new double[4];
        int n = 0;
        for (;;) {
            skipSpaces();
            if (pos >= text.length() || !isNumberStart(text.charAt(pos))) {
                break;
            }
            if (n == values.length) {
                values = Arrays.copyOf(values, n * 2);
            }
            values[n++] = readNumber();
        }
        if (n == 0) {
            throw error("Expected a coordinate tuple");
        }
        return Arrays.copyOf(values, n);
    }

    /**
     * Reads one number. The lexical form is the one of {@link Double#parseDouble(String)}
     * restricted to decimal notation, and is therefore independent of the default locale.
     */
    private double readNumber() {
        final int start = pos;
        final int length = text.length();
        if (pos < length && isSign(text.charAt(pos))) pos++;
        while (pos < length && (isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) pos++;
        if (pos < length && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
            pos++;
            if (pos < length && isSign(text.charAt(pos))) pos++;
            while (pos < length && isDigit(text.charAt(pos))) pos++;
        }
        final String token = text.substring(start, pos);
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            pos = start;
            throw error("\"" + token + "\" is not a number");
        }
    }

    /**
     * Reads the next word, in upper case, or an empty string if the next character is not a
     * letter. Words are the keywords, the dimension flags and {@code EMPTY}.
     */
    private String readWord() {
        skipSpaces();
        final int start = pos;
        while (pos < text.length() && Character.isLetter(text.charAt(pos))) pos++;
        return text.substring(start, pos).toUpperCase(Locale.ROOT);
    }

    /**
     * Reads the optional dimension flag which may follow a keyword.
     *
     * @return {@link #FLAG_Z} and/or {@link #FLAG_M}, or {@link #UNSPECIFIED} if there is none.
     */
    private int readFlags() {
        final int mark = pos;
        switch (readWord()) {
            case "Z":  return FLAG_Z;
            case "M":  return FLAG_M;
            case "ZM": return FLAG_Z | FLAG_M;
            default: pos = mark; return UNSPECIFIED;
        }
    }

    /**
     * Consumes the {@code EMPTY} keyword if it is the next word, and returns whether it was.
     */
    private boolean readEmpty() {
        final int mark = pos;
        if ("EMPTY".equals(readWord())) {
            return true;
        }
        pos = mark;
        return false;
    }

    /**
     * Consumes the given character, which must be the next one.
     */
    private void expect(final char c) {
        if (!accept(c)) {
            throw error("Expected '" + c + '\'');
        }
    }

    /**
     * Consumes the given character if it is the next one, and returns whether it was.
     */
    private boolean accept(final char c) {
        skipSpaces();
        if (pos < text.length() && text.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Returns the next character without consuming it, or {@code 0} at the end of the text.
     */
    private char peek() {
        skipSpaces();
        return (pos < text.length()) ? text.charAt(pos) : 0;
    }

    /**
     * Advances past any whitespace.
     */
    private void skipSpaces() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
    }

    private static boolean isDigit(final char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isSign(final char c) {
        return c == '+' || c == '-';
    }

    private static boolean isNumberStart(final char c) {
        return isDigit(c) || isSign(c) || c == '.';
    }

    /**
     * Returns the exception to throw for a malformed text, pointing at the current position.
     */
    private IllegalArgumentException error(final String message) {
        return new IllegalArgumentException(message + ", at offset " + pos + " of \"" + text + '"');
    }
}
