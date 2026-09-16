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

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.IntFunction;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.Orientable;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.CircularString;
import org.apache.sis.geometries.curve.CompoundCurve;
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
import org.apache.sis.maths.Tuple;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Encoder and decoder for the Well-Known Binary representation of geometries.
 *
 * <p>This is the binary counterpart of {@link WellKnownText}, and covers the same geometry types:
 * those of <cite>OGC Simple Feature Access 1.2.1</cite> extended with the curved and surface-patch
 * types of <cite>ISO 13249-3</cite> (SQL/MM Part 3). Every geometry is a byte order flag, a type
 * code and a body:</p>
 *
 * <blockquote><pre>
 * byte    byteOrder;      // 0 = big endian (XDR), 1 = little endian (NDR)
 * uint32  type;           // geometry type, plus 1000 for Z, 2000 for M, 3000 for ZM
 * …                       // body, which depends on the type
 * </pre></blockquote>
 *
 * <p>The supported type codes, and the geometry type each one maps to, are:</p>
 *
 * <table class="sis">
 *   <caption>Supported Well-Known Binary type codes</caption>
 *   <tr><th>Code</th> <th>Geometry type</th></tr>
 *   <tr><td>1</td>    <td>{@link Point}</td></tr>
 *   <tr><td>2</td>    <td>{@link LineString}</td></tr>
 *   <tr><td>3</td>    <td>{@link Polygon}</td></tr>
 *   <tr><td>4</td>    <td>{@link MultiPoint}</td></tr>
 *   <tr><td>5</td>    <td>{@link MultiLineString}</td></tr>
 *   <tr><td>6</td>    <td>{@link MultiPolygon}</td></tr>
 *   <tr><td>7</td>    <td>{@link GeometryCollection}</td></tr>
 *   <tr><td>8</td>    <td>{@link CircularString}</td></tr>
 *   <tr><td>9</td>    <td>{@link CompoundCurve}</td></tr>
 *   <tr><td>10</td>   <td>{@link CurvePolygon}</td></tr>
 *   <tr><td>11</td>   <td>{@link MultiCurve}</td></tr>
 *   <tr><td>12</td>   <td>{@link MultiSurface}</td></tr>
 *   <tr><td>15</td>   <td>{@link PolyhedralSurface}</td></tr>
 *   <tr><td>16</td>   <td>{@link TIN}</td></tr>
 *   <tr><td>17</td>   <td>{@link Triangle}</td></tr>
 * </table>
 *
 * <p>Any other geometry of the Apache SIS hierarchy
 * is rejected by {@link #encode encode(…)} with an {@link IllegalArgumentException}.</p>
 *
 * <h2>Dimensions and measures</h2>
 * As in {@link WellKnownText}, the {@code Z} flag is the third ordinate of the
 * {@linkplain AttributesType#ATT_POSITION position} attribute and the {@code M} flag is the
 * separate {@linkplain AttributesType#ATT_M measure} attribute, written as the ordinate following
 * the position ones. A geometry whose position has neither 2 nor 3 dimensions cannot be written.
 * Unlike the text form, the binary form always states the flags, so nothing is ever inferred from
 * the width of a tuple.
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>A {@link org.apache.sis.geometries.curve.LinearRing} is written as a {@code LineString}:
 *       it is a {@code LineString} and Well-Known Binary has no standalone ring type. Consequently
 *       {@code decode(…)} never returns a {@code LinearRing} at the top level.</li>
 *   <li>An {@linkplain Orientable#getOrientationSign() orientation} of
 *       {@link Orientable.Sign#NEGATIVE} is written as the underlying
 *       {@linkplain Orientable#getReverse() reverse} primitive. Well-Known Binary has no notion
 *       of orientation, so that information is lost.</li>
 *   <li>An {@link Empty} geometry is written as an empty {@code GeometryCollection}, the only
 *       type-less empty form the format offers.</li>
 *   <li>The format has no empty form for a point, since a point is a bare coordinate tuple with
 *       no count in front of it. The widespread convention of writing {@link Double#NaN} ordinates
 *       is followed: an empty point is written that way, and a point whose ordinates are all
 *       {@code NaN} is read as an {@link Empty} geometry rather than as a {@code Point}, because
 *       the model has no empty point. It is therefore written back as an empty
 *       {@code GeometryCollection}, and an empty point may not appear as a member of a
 *       {@code MultiPoint}. Every other type has a genuine empty form, a count of zero, which
 *       round-trips unchanged.</li>
 *   <li>The coordinate reference system is neither written nor read: the {@code SRID} field of the
 *       extended Well-Known Binary of some databases is not part of the standard, and neither are
 *       the high order type bits it uses for the dimension flags. Unless a system is given to
 *       {@link #decode(byte[], CoordinateReferenceSystem)}, decoded geometries use
 *       {@link org.apache.sis.geometries.Geometries#getUndefinedCRS(int)}.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * Instances are cheap to create, immutable, and safe for use by multiple threads.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class WellKnownBinary {
    /**
     * Value of the byte order flag for each of the two orders.
     */
    static final byte XDR = 0, NDR = 1;

    /**
     * Type codes of the geometries which have one, shared by the encoder and the parser.
     * They are held apart rather than declared on {@code WellKnownBinary} because a field
     * named after a geometry type would obscure the type of the same name.
     */
    static final class Codes {
        /**
         * The codes of <cite>OGC Simple Feature Access 1.2.1</cite> and <cite>ISO 13249-3</cite>.
         * Codes 13 and 14, which stand for the abstract {@code Curve} and {@code Surface} types,
         * are deliberately absent: no geometry can be of an abstract type.
         */
        static final int 
                POINT = 1, 
                LINESTRING = 2, 
                POLYGON = 3, 
                MULTIPOINT = 4, 
                MULTILINESTRING = 5,
                MULTIPOLYGON = 6, 
                GEOMETRYCOLLECTION = 7, 
                CIRCULARSTRING = 8, 
                COMPOUNDCURVE = 9,
                CURVEPOLYGON = 10, 
                MULTICURVE = 11, 
                MULTISURFACE = 12, 
                POLYHEDRALSURFACE = 15,
                TIN = 16, 
                TRIANGLE = 17;

        /**
         * Value added to a type code for the {@code Z}, {@code M} and {@code ZM} flags.
         * The {@code ZM} flag is the sum of the two others.
         */
        static final int Z_OFFSET = 1000, M_OFFSET = 2000;

        /**
         * Do not allow instantiation of this holder of constants.
         */
        private Codes() {
        }
    }

    /**
     * Byte order of the written geometries. Never null. Both orders are read whatever this is.
     */
    private final ByteOrder byteOrder;

    /**
     * Creates a codec writing geometries in big endian order, also known as XDR, which is the
     * order of the byte sequences of the standard. Both orders are read.
     */
    public WellKnownBinary() {
        byteOrder = ByteOrder.BIG_ENDIAN;
    }

    /**
     * Creates a codec writing geometries in the given byte order. Both orders are read whatever
     * the order given here.
     *
     * @param  byteOrder  order of the multi-byte values to write, not null.
     */
    public WellKnownBinary(final ByteOrder byteOrder) {
        ArgumentChecks.ensureNonNull("byteOrder", byteOrder);
        this.byteOrder = byteOrder;
    }

    /**
     * Returns the Well-Known Binary of the given geometry.
     *
     * @param  geom  the geometry to encode, not null.
     * @return the geometry in Well-Known Binary.
     * @throws IllegalArgumentException if the geometry, or one of the geometries it contains,
     *         has no Well-Known Binary representation, or if its positions are neither 2
     *         nor 3 dimensional.
     */
    public byte[] encode(final Geometry geom) {
        ArgumentChecks.ensureNonNull("geom", geom);
        final Output out = new Output(byteOrder);
        format(out, geom);
        return out.toArray();
    }

    /**
     * Returns the geometry described by the given Well-Known Binary.
     *
     * @param  geom  the Well-Known Binary to decode, not null.
     * @return the decoded geometry.
     * @throws IllegalArgumentException if the bytes are malformed, or name a geometry type which
     *         is not in the table of this class javadoc.
     */
    public Geometry decode(final byte[] geom) {
        return decode(geom, null);
    }

    /**
     * Returns the geometry described by the given Well-Known Binary, in the given coordinate
     * reference system. Well-Known Binary carries no system of its own.
     *
     * @param  geom  the Well-Known Binary to decode, not null.
     * @param  crs   the coordinate reference system of the coordinates in the bytes, or
     *               {@code null}.
     * @return the decoded geometry.
     * @throws IllegalArgumentException if the bytes are malformed, name a geometry type which is
     *         not in the table of this class javadoc, or have a number of ordinates which
     *         contradicts the dimension of {@code crs}.
     */
    public Geometry decode(final byte[] geom, final CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("geom", geom);
        return new WellKnownBinaryParser(geom, crs).parse();
    }

    // ////////////////////////////////////////////////////////////////////////
    // Encoding ///////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Writes the Well-Known Binary of the given geometry, header included.
     *
     * <p>The order of the tests below is significant: the geometry interfaces form a hierarchy,
     * so every type must be tested before its supertypes. It mirrors the dispatch of
     * {@code WellKnownText.format(…)}.</p>
     */
    private void format(final Output out, final Geometry geometry) {
        if (geometry instanceof Orientable o && o.getOrientationSign() == Orientable.Sign.NEGATIVE) {
            format(out, o.getReverse());                        // Orientation is not representable.
        } else if (geometry instanceof Empty g) {
            writeHeader(out, Codes.GEOMETRYCOLLECTION, g);
            out.writeInt(0);
        } else if (geometry instanceof Point g) {
            formatPoint(out, g);
        } else if (geometry instanceof CircularString g) {      // Before Curve.
            formatPointList(out, Codes.CIRCULARSTRING, g, g.getDataPoints());
        } else if (geometry instanceof CompoundCurve g) {       // Before Curve.
            formatCompoundCurve(out, g);
        } else if (geometry instanceof LineString g) {          // Also matches LinearRing.
            formatPointList(out, Codes.LINESTRING, g, g.getDataPoints());
        } else if (geometry instanceof Triangle g) {            // Before Polygon.
            formatPolygon(out, Codes.TRIANGLE, g);
        } else if (geometry instanceof Polygon g) {             // Before Surface.
            formatPolygon(out, Codes.POLYGON, g);
        } else if (geometry instanceof CurvePolygon g) {        // Before Surface.
            formatCurvePolygon(out, g);
        } else if (geometry instanceof TIN g) {                 // Before PolyhedralSurface.
            formatPatches(out, Codes.TIN, g.getNumPatches(), g::getPatchN, g);
        } else if (geometry instanceof PolyhedralSurface<?> g) {
            formatPatches(out, Codes.POLYHEDRALSURFACE, g.getNumPatches(), g::getPatchN, g);
        } else if (geometry instanceof MultiPoint<?> g) {       // Before GeometryCollection.
            formatMembers(out, Codes.MULTIPOINT, g);
        } else if (geometry instanceof MultiLineString g) {     // Before MultiCurve.
            formatMembers(out, Codes.MULTILINESTRING, g);
        } else if (geometry instanceof MultiPolygon g) {        // Before MultiSurface.
            formatMembers(out, Codes.MULTIPOLYGON, g);
        } else if (geometry instanceof MultiCurve<?> g) {       // Before GeometryCollection.
            formatMembers(out, Codes.MULTICURVE, g);
        } else if (geometry instanceof MultiSurface<?> g) {     // Before GeometryCollection.
            formatMembers(out, Codes.MULTISURFACE, g);
        } else if (geometry instanceof GeometryCollection<?> g) {
            formatMembers(out, Codes.GEOMETRYCOLLECTION, g);
        } else {
            throw unsupported(geometry);
        }
    }

    /**
     * Writes a point as a single coordinate tuple. An empty point has no form of its own in this
     * format and is written as a tuple of {@link Double#NaN} ordinates, the usual convention.
     */
    private void formatPoint(final Output out, final Point geometry) {
        final boolean hasM = writeHeader(out, Codes.POINT, geometry);
        if (geometry.isEmpty()) {
            final int dimension = geometry.getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
            for (int i = hasM ? dimension + 1 : dimension; --i >= 0;) {
                out.writeDouble(Double.NaN);
            }
        } else {
            writePosition(out, geometry.getPosition(), hasM ? geometry.getAttribute(AttributesType.ATT_M) : null);
        }
    }

    /**
     * Writes a type whose body is a single count of coordinate tuples followed by those tuples.
     */
    private void formatPointList(final Output out, final int code,
                                 final Geometry geometry, final DataPoints points)
    {
        final boolean hasM = writeHeader(out, code, geometry);
        writePointList(out, points, hasM);
    }

    /**
     * Writes a compound curve as a count of components followed by those components, each of them
     * a complete geometry. Unlike the text form, a {@code LineString} component keeps its header.
     */
    private void formatCompoundCurve(final Output out, final CompoundCurve geometry) {
        writeHeader(out, Codes.COMPOUNDCURVE, geometry);
        final int n = geometry.getNumCurves();
        out.writeInt(n);
        for (int i = 0; i < n; i++) {
            format(out, geometry.getCurveN(i));
        }
    }

    /**
     * Writes a polygon, or a triangle, as a count of rings followed by those rings.
     * A triangle has no interior ring, so its count is 1 unless it is empty.
     */
    private void formatPolygon(final Output out, final int code, final Polygon geometry) {
        final boolean hasM = writeHeader(out, code, geometry);
        writeRings(out, geometry, hasM);
    }

    /**
     * Writes a curve polygon as a count of rings followed by those rings, each of them a complete
     * geometry. This is what lets the rings be of any curve type.
     */
    private void formatCurvePolygon(final Output out, final CurvePolygon geometry) {
        writeHeader(out, Codes.CURVEPOLYGON, geometry);
        if (geometry.isEmpty()) {
            out.writeInt(0);
            return;
        }
        final int n = geometry.getNumInteriorRing();
        out.writeInt(n + 1);
        format(out, geometry.getExteriorRing());
        for (int i = 0; i < n; i++) {
            format(out, geometry.getInteriorRingN(i));
        }
    }

    /**
     * Writes a polyhedral surface, or a TIN, as a count of patches followed by those patches,
     * each of them a complete {@code POLYGON} or {@code TRIANGLE} geometry.
     */
    private void formatPatches(final Output out, final int code, final int count,
                               final IntFunction<? extends Polygon> patches,
                               final Geometry geometry)
    {
        writeHeader(out, code, geometry);
        out.writeInt(count);
        for (int i = 0; i < count; i++) {
            format(out, patches.apply(i));
        }
    }

    /**
     * Writes a collection as a count of members followed by those members, each of them a
     * complete geometry. Every collection type shares this body, the type code alone saying
     * what the members are allowed to be.
     */
    private void formatMembers(final Output out, final int code, final GeometryCollection<?> geometry) {
        writeHeader(out, code, geometry);
        final int n = geometry.getNumGeometries();
        out.writeInt(n);
        for (int i = 0; i < n; i++) {
            format(out, geometry.getGeometryN(i));
        }
    }

    /**
     * Writes the rings of a polygon: a count followed by one coordinate list per ring.
     * A triangle is written the same way: its exterior ring already closes on its first corner.
     */
    private void writeRings(final Output out, final Polygon polygon, final boolean hasM) {
        if (polygon.isEmpty()) {
            out.writeInt(0);
            return;
        }
        final int n = polygon.getNumInteriorRing();
        out.writeInt(n + 1);
        writePointList(out, polygon.getExteriorRing().getDataPoints(), hasM);
        for (int i = 0; i < n; i++) {
            writePointList(out, polygon.getInteriorRingN(i).getDataPoints(), hasM);
        }
    }

    /**
     * Writes a count of coordinate tuples followed by those tuples.
     */
    private void writePointList(final Output out, final DataPoints points, final boolean hasM) {
        final int n = points.size();
        out.writeInt(n);
        for (int i = 0; i < n; i++) {
            writePosition(out, points.getPosition(i), hasM ? points.getAttribute(i, AttributesType.ATT_M) : null);
        }
    }

    /**
     * Writes the ordinates of one position, followed by its measure if any.
     */
    private void writePosition(final Output out, final Tuple<?> position, final Tuple<?> measure) {
        for (int i = 0, n = position.getDimension(); i < n; i++) {
            out.writeDouble(position.get(i));
        }
        if (measure != null) {
            out.writeDouble(measure.get(0));
        }
    }

    /**
     * Writes the byte order flag and the type code of a geometry, the dimension flags included.
     *
     * @return whether the positions carry a measure, which the caller has to write as the
     *         ordinate following the position ones.
     */
    private boolean writeHeader(final Output out, final int code, final Geometry geometry) {
        final CoordinateReferenceSystem crs = geometry.getCoordinateReferenceSystem();
        if (crs == null) {
            throw new IllegalArgumentException("Cannot write a " + geometry.getGeometryType()
                    + " in Well-Known Binary:"
                    + " it has no coordinate reference system, therefore no known number of dimensions."
                    + " An empty collection takes one from the factory method which creates it.");
        }
        final int dimension = crs.getCoordinateSystem().getDimension();
        final boolean hasM = hasMeasure(geometry);
        final int flags;
        switch (dimension) {
            case 2:  flags = hasM ? Codes.M_OFFSET : 0; break;
            case 3:  flags = hasM ? Codes.Z_OFFSET + Codes.M_OFFSET : Codes.Z_OFFSET; break;
            default: throw new IllegalArgumentException("Cannot write a " + geometry.getGeometryType()
                        + " in Well-Known Binary: its positions have " + dimension + " dimensions,"
                        + " but the format defines only 2 and 3.");
        }
        out.writeByteOrder();
        out.writeInt(code + flags);
        return hasM;
    }

    /**
     * Returns whether the given geometry carries the {@linkplain AttributesType#ATT_M measure}
     * attribute, which is what the {@code M} and {@code ZM} flags stand for.
     */
    private static boolean hasMeasure(final Geometry geometry) {
        final AttributesType type = geometry.getAttributesType();
        return (type != null) && type.getAttributeNames().contains(AttributesType.ATT_M);
    }

    /**
     * Returns the exception to throw for a geometry which the format cannot represent.
     */
    private static IllegalArgumentException unsupported(final Geometry geometry) {
        return new IllegalArgumentException("Well-Known Binary defines no representation for "
                + geometry.getClass().getSimpleName() + '.');
    }

    /**
     * Growable sequence of bytes, writing the multi-byte values in the order given to the
     * constructor. A {@code ByteBuffer} is not used because the length of the result is not
     * known before the geometry has been walked.
     */
    private static final class Output {
        /**
         * The bytes written so far. Only the first {@link #length} elements are meaningful.
         */
        private byte[] array = new byte[64];

        /**
         * Number of meaningful bytes in {@link #array}.
         */
        private int length;

        /**
         * Whether the multi-byte values are written most significant byte first.
         */
        private final boolean bigEndian;

        /**
         * Creates an initially empty output writing in the given byte order.
         */
        Output(final ByteOrder byteOrder) {
            bigEndian = (byteOrder == ByteOrder.BIG_ENDIAN);
        }

        /**
         * Writes the flag which tells in which order the values after it are written.
         */
        void writeByteOrder() {
            ensure(1);
            array[length++] = bigEndian ? XDR : NDR;
        }

        /**
         * Writes a 32 bits integer, which the format uses for the type codes and the counts.
         */
        void writeInt(final int value) {
            ensure(Integer.BYTES);
            for (int i = 0; i < Integer.BYTES; i++) {
                final int shift = 8 * (bigEndian ? Integer.BYTES - 1 - i : i);
                array[length++] = (byte) (value >>> shift);
            }
        }

        /**
         * Writes an IEEE 754 double precision number, which the format uses for the ordinates.
         */
        void writeDouble(final double value) {
            final long bits = Double.doubleToLongBits(value);
            ensure(Double.BYTES);
            for (int i = 0; i < Double.BYTES; i++) {
                final int shift = 8 * (bigEndian ? Double.BYTES - 1 - i : i);
                array[length++] = (byte) (bits >>> shift);
            }
        }

        /**
         * Makes room for the given number of additional bytes.
         */
        private void ensure(final int count) {
            if (length + count > array.length) {
                array = Arrays.copyOf(array, Math.max(length + count, array.length * 2));
            }
        }

        /**
         * Returns the bytes written so far, in a array of exactly the right length.
         */
        byte[] toArray() {
            return Arrays.copyOf(array, length);
        }
    }
}
