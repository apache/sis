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

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.IntFunction;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Curve;
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
import org.apache.sis.util.StringBuilders;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Encoder and decoder for the Well-Known Text representation of geometries.
 *
 * <p>The grammar is the one of <cite>OGC Simple Feature Access 1.2.1</cite> extended with the
 * curved and surface-patch types of <cite>ISO 13249-3</cite> (SQL/MM Part 3):</p>
 *
 * <blockquote><pre>
 * &lt;keyword&gt; [Z | M | ZM] ( … )
 * &lt;keyword&gt; [Z | M | ZM] EMPTY
 * </pre></blockquote>
 *
 * <p>The supported keywords, and the geometry type each one maps to, are:</p>
 *
 * <table class="sis">
 *   <caption>Supported Well-Known Text keywords</caption>
 *   <tr><th>Keyword</th>                <th>Geometry type</th></tr>
 *   <tr><td>{@code POINT}</td>              <td>{@link Point}</td></tr>
 *   <tr><td>{@code LINESTRING}</td>         <td>{@link LineString}</td></tr>
 *   <tr><td>{@code CIRCULARSTRING}</td>     <td>{@link CircularString}</td></tr>
 *   <tr><td>{@code COMPOUNDCURVE}</td>      <td>{@link CompoundCurve}</td></tr>
 *   <tr><td>{@code POLYGON}</td>            <td>{@link Polygon}</td></tr>
 *   <tr><td>{@code TRIANGLE}</td>           <td>{@link Triangle}</td></tr>
 *   <tr><td>{@code CURVEPOLYGON}</td>       <td>{@link CurvePolygon}</td></tr>
 *   <tr><td>{@code POLYHEDRALSURFACE}</td>  <td>{@link PolyhedralSurface}</td></tr>
 *   <tr><td>{@code TIN}</td>                <td>{@link TIN}</td></tr>
 *   <tr><td>{@code MULTIPOINT}</td>         <td>{@link MultiPoint}</td></tr>
 *   <tr><td>{@code MULTILINESTRING}</td>    <td>{@link MultiLineString}</td></tr>
 *   <tr><td>{@code MULTICURVE}</td>         <td>{@link MultiCurve}</td></tr>
 *   <tr><td>{@code MULTIPOLYGON}</td>       <td>{@link MultiPolygon}</td></tr>
 *   <tr><td>{@code MULTISURFACE}</td>       <td>{@link MultiSurface}</td></tr>
 *   <tr><td>{@code GEOMETRYCOLLECTION}</td> <td>{@link GeometryCollection}</td></tr>
 * </table>
 *
 * <p>Any other geometry of the Apache SIS hierarchy
 * is rejected by {@link #encode encode(…)} with an {@link IllegalArgumentException}.</p>
 *
 * <h2>Dimensions and measures</h2>
 * The {@code Z} flag is the third ordinate of the {@linkplain AttributesType#ATT_POSITION position}
 * attribute, and the {@code M} flag is the separate {@linkplain AttributesType#ATT_M measure}
 * attribute. A geometry whose position has neither 2 nor 3 dimensions cannot be written.
 * When the text carries no flag, the number of ordinates of the first coordinate tuple decides:
 * two of them are read as <var>x y</var> and three as <var>x y z</var>. Four ordinates without a
 * flag are ambiguous — {@code ZM} and a 4-dimensional position would be written the same way —
 * and are therefore rejected.
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>A {@link org.apache.sis.geometries.curve.LinearRing} is written as a {@code LINESTRING}:
 *       it is a {@code LineString} and Well-Known Text has no standalone ring type. Consequently
 *       {@code decode(…)} never returns a {@code LinearRing} at the top level.</li>
 *   <li>An {@linkplain Orientable#getOrientationSign() orientation} of
 *       {@link Orientable.Sign#NEGATIVE} is written as the underlying
 *       {@linkplain Orientable#getReverse() reverse} primitive. Well-Known Text has no notion
 *       of orientation, so that information is lost.</li>
 *   <li>An {@link Empty} geometry is written as {@code GEOMETRYCOLLECTION EMPTY}, the only
 *       type-less empty form the grammar offers.</li>
 *   <li>{@code POINT EMPTY} is read as an {@link Empty} geometry rather than as a {@code Point},
 *       because the model has no empty point: a point holds exactly one position. It is therefore
 *       written back as {@code GEOMETRYCOLLECTION EMPTY}, and an empty point may not appear as a
 *       member of a {@code MULTIPOINT}. Every other type has a genuine empty form which
 *       round-trips unchanged.</li>
 *   <li>The coordinate reference system is neither written nor read: the {@code SRID=…;} prefix
 *       of the extended Well-Known Text of some databases is not part of the standard. Unless a
 *       system is given to {@link #decode(String, CoordinateReferenceSystem)}, decoded geometries
 *       use {@link org.apache.sis.geometries.Geometries#getUndefinedCRS(int)}.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * Instances are cheap to create but are <strong>not</strong> thread-safe, because the number
 * format they may hold is not. Use one instance per thread.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class WellKnownText {
    /**
     * Format of the ordinates, or {@code null} for writing the shortest text which parses back
     * to the same {@code double}. Not thread-safe, hence the warning in the class javadoc.
     */
    private final NumberFormat format;

    /**
     * Creates an encoder writing every ordinate as the shortest decimal text which parses back to
     * the same {@code double} value. A whole number is written without its fractional part:
     * {@code 2}, not {@code 2.0}.
     */
    public WellKnownText() {
        format = null;
    }

    /**
     * Creates an encoder rounding every ordinate to at most the given number of fraction digits,
     * half away from zero. Trailing zeros are not written, so a precision of 3 writes {@code 1.5}
     * rather than {@code 1.500}. The precision has no effect on {@link #decode decoding}.
     *
     * @param  decimalPrecision  maximal number of digits after the decimal separator, 0 or more.
     */
    public WellKnownText(final int decimalPrecision) {
        ArgumentChecks.ensurePositive("decimalPrecision", decimalPrecision);
        format = NumberFormat.getNumberInstance(Locale.ROOT);
        format.setGroupingUsed(false);
        format.setMaximumFractionDigits(decimalPrecision);
        format.setRoundingMode(RoundingMode.HALF_UP);       // Not the HALF_EVEN default, which surprises.
    }

    /**
     * Returns the Well-Known Text of the given geometry.
     *
     * @param  geom  the geometry to encode, not null.
     * @return the geometry in Well-Known Text.
     * @throws IllegalArgumentException if the geometry, or one of the geometries it contains,
     *         has no Well-Known Text representation, or if its positions are neither 2
     *         nor 3 dimensional.
     */
    public String encode(final Geometry geom) {
        ArgumentChecks.ensureNonNull("geom", geom);
        final StringBuilder sb = new StringBuilder();
        format(sb, geom);
        return sb.toString();
    }

    /**
     * Returns the geometry described by the given Well-Known Text.
     *
     * @param  geom  the Well-Known Text to decode, not null.
     * @return the decoded geometry.
     * @throws IllegalArgumentException if the text is malformed, or names a geometry type which
     *         is not in the table of this class javadoc.
     */
    public Geometry decode(final String geom) {
        return decode(geom, null);
    }

    /**
     * Returns the geometry described by the given Well-Known Text, in the given coordinate
     * reference system. Well-Known Text carries no system of its own.
     *
     * @param  geom  the Well-Known Text to decode, not null.
     * @param  crs   the coordinate reference system of the coordinates in the text, or
     *               {@code null}.
     * @return the decoded geometry.
     * @throws IllegalArgumentException if the text is malformed, names a geometry type which is
     *         not in the table of this class javadoc, or has a number of ordinates which
     *         contradicts the dimension of {@code crs}.
     */
    public Geometry decode(final String geom, final CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("geom", geom);
        return new WellKnownTextParser(geom, crs).parse();
    }

    // ////////////////////////////////////////////////////////////////////////
    // Encoding ///////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Appends the Well-Known Text of the given geometry, keyword included.
     *
     * <p>The order of the tests below is significant: the geometry interfaces form a hierarchy,
     * so every type must be tested before its supertypes. It mirrors the dispatch of
     * {@code GML3Writer.writeGeometry(…)}.</p>
     */
    private void format(final StringBuilder sb, final Geometry geometry) {
        if (geometry instanceof Orientable o && o.getOrientationSign() == Orientable.Sign.NEGATIVE) {
            format(sb, o.getReverse());                         // Orientation is not representable.
        } else if (geometry instanceof Empty) {
            sb.append(GeometryCollection.TYPE).append(" EMPTY");
        } else if (geometry instanceof Point g) {
            formatPoint(sb, g);
        } else if (geometry instanceof CircularString g) {      // Before Curve.
            formatPointList(sb, CircularString.TYPE, g, g.getDataPoints());
        } else if (geometry instanceof CompoundCurve g) {       // Before Curve.
            formatCompoundCurve(sb, g);
        } else if (geometry instanceof LineString g) {          // Also matches LinearRing.
            formatPointList(sb, LineString.TYPE, g, g.getDataPoints());
        } else if (geometry instanceof Triangle g) {            // Before Polygon.
            formatPolygon(sb, Triangle.TYPE, g);
        } else if (geometry instanceof Polygon g) {             // Before Surface.
            formatPolygon(sb, Polygon.TYPE, g);
        } else if (geometry instanceof CurvePolygon g) {        // Before Surface.
            formatCurvePolygon(sb, g);
        } else if (geometry instanceof TIN g) {                 // Before PolyhedralSurface.
            formatPatches(sb, TIN.TYPE, g.getNumPatches(), g::getPatchN, g);
        } else if (geometry instanceof PolyhedralSurface<?> g) {
            formatPatches(sb, PolyhedralSurface.TYPE, g.getNumPatches(), g::getPatchN, g);
        } else if (geometry instanceof MultiPoint<?> g) {       // Before GeometryCollection.
            formatMultiPoint(sb, g);
        } else if (geometry instanceof MultiLineString g) {     // Before MultiCurve.
            formatMultiLineString(sb, g);
        } else if (geometry instanceof MultiPolygon g) {        // Before MultiSurface.
            formatMultiPolygon(sb, g);
        } else if (geometry instanceof MultiCurve<?> g) {       // Before GeometryCollection.
            formatMembers(sb, MultiCurve.TYPE, g);
        } else if (geometry instanceof MultiSurface<?> g) {     // Before GeometryCollection.
            formatMembers(sb, MultiSurface.TYPE, g);
        } else if (geometry instanceof GeometryCollection<?> g) {
            formatMembers(sb, GeometryCollection.TYPE, g);
        } else {
            throw unsupported(geometry);
        }
    }

    /**
     * Appends {@code "POINT Z (1 2 3)"} or {@code "POINT Z EMPTY"}.
     */
    private void formatPoint(final StringBuilder sb, final Point geometry) {
        if (appendHeader(sb, Point.TYPE, geometry)) return;
        sb.append('(');
        appendPosition(sb, geometry.getPosition(), hasMeasure(geometry) ? geometry.getAttribute(AttributesType.ATT_M) : null);
        sb.append(')');
    }

    /**
     * Appends {@code "LINESTRING (1 2, 3 4)"}: a keyword followed by a single parenthesized
     * list of coordinate tuples.
     */
    private void formatPointList(final StringBuilder sb, final String keyword,
                                 final Geometry geometry, final DataPoints points)
    {
        if (appendHeader(sb, keyword, geometry)) return;
        appendPointList(sb, points, hasMeasure(geometry));
    }

    /**
     * Appends {@code "COMPOUNDCURVE ((1 2, 3 4), CIRCULARSTRING (3 4, 5 6, 7 8))"}.
     * A {@code LINESTRING} component may drop its keyword, and does so here; any other
     * component keeps it.
     */
    private void formatCompoundCurve(final StringBuilder sb, final CompoundCurve geometry) {
        if (appendHeader(sb, CompoundCurve.TYPE, geometry)) return;
        final boolean hasM = hasMeasure(geometry);
        sb.append('(');
        for (int i = 0, n = geometry.getNumCurves(); i < n; i++) {
            if (i != 0) sb.append(", ");
            appendCurveComponent(sb, geometry.getCurveN(i), hasM);
        }
        sb.append(')');
    }

    /**
     * Appends {@code "POLYGON ((0 0, 1 0, 1 1, 0 0), (…))"}, or the same with the
     * {@code TRIANGLE} keyword, in which case {@link Triangle#getInteriorRings()} is empty
     * and only the exterior ring is written.
     */
    private void formatPolygon(final StringBuilder sb, final String keyword, final Polygon geometry) {
        if (appendHeader(sb, keyword, geometry)) return;
        appendRings(sb, geometry, hasMeasure(geometry));
    }

    /**
     * Appends {@code "CURVEPOLYGON (CIRCULARSTRING (…), (…))"}. As in a compound curve, a ring
     * which is a {@code LINESTRING} drops its keyword.
     */
    private void formatCurvePolygon(final StringBuilder sb, final CurvePolygon geometry) {
        if (appendHeader(sb, CurvePolygon.TYPE, geometry)) return;
        final boolean hasM = hasMeasure(geometry);
        sb.append('(');
        appendCurveComponent(sb, geometry.getExteriorRing(), hasM);
        for (int i = 0, n = geometry.getNumInteriorRing(); i < n; i++) {
            sb.append(", ");
            appendCurveComponent(sb, geometry.getInteriorRingN(i), hasM);
        }
        sb.append(')');
    }

    /**
     * Appends {@code "POLYHEDRALSURFACE (((…)), ((…)))"} or the same with the {@code TIN}
     * keyword: a list of polygon patches, each written without its keyword.
     */
    private void formatPatches(final StringBuilder sb, final String keyword, final int count,
                               final IntFunction<? extends Polygon> patches,
                               final Geometry geometry)
    {
        if (appendHeader(sb, keyword, geometry)) return;
        final boolean hasM = hasMeasure(geometry);
        sb.append('(');
        for (int i = 0; i < count; i++) {
            if (i != 0) sb.append(", ");
            appendRings(sb, patches.apply(i), hasM);
        }
        sb.append(')');
    }

    /**
     * Appends {@code "MULTIPOINT ((1 2), (3 4))"}. The grammar also allows the parentheses around
     * each point to be omitted; they are written here because that form is unambiguous.
     */
    private void formatMultiPoint(final StringBuilder sb, final MultiPoint<?> geometry) {
        if (appendHeader(sb, MultiPoint.TYPE, geometry)) return;
        final boolean hasM = hasMeasure(geometry);
        sb.append('(');
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            if (i != 0) sb.append(", ");
            final Point point = geometry.getGeometryN(i);
            if (point.isEmpty()) {
                sb.append("EMPTY");
            } else {
                sb.append('(');
                appendPosition(sb, point.getPosition(), hasM ? point.getAttribute(AttributesType.ATT_M) : null);
                sb.append(')');
            }
        }
        sb.append(')');
    }

    /**
     * Appends {@code "MULTILINESTRING ((1 2, 3 4), (5 6, 7 8))"}: components keep no keyword.
     */
    private void formatMultiLineString(final StringBuilder sb, final MultiLineString geometry) {
        if (appendHeader(sb, MultiLineString.TYPE, geometry)) return;
        final boolean hasM = hasMeasure(geometry);
        sb.append('(');
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            if (i != 0) sb.append(", ");
            appendPointList(sb, geometry.getGeometryN(i).getDataPoints(), hasM);
        }
        sb.append(')');
    }

    /**
     * Appends {@code "MULTIPOLYGON (((…), (…)), ((…)))"}: components keep no keyword.
     */
    private void formatMultiPolygon(final StringBuilder sb, final MultiPolygon geometry) {
        if (appendHeader(sb, MultiPolygon.TYPE, geometry)) return;
        final boolean hasM = hasMeasure(geometry);
        sb.append('(');
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            if (i != 0) sb.append(", ");
            appendRings(sb, geometry.getGeometryN(i), hasM);
        }
        sb.append(')');
    }

    /**
     * Appends a collection whose members are written in full, with their own keyword: this is
     * mandatory for {@code GEOMETRYCOLLECTION}, and is what makes {@code MULTICURVE} and
     * {@code MULTISURFACE} able to hold the curved types.
     */
    private void formatMembers(final StringBuilder sb, final String keyword, final GeometryCollection<?> geometry) {
        if (appendHeader(sb, keyword, geometry)) return;
        sb.append('(');
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            if (i != 0) sb.append(", ");
            format(sb, geometry.getGeometryN(i));
        }
        sb.append(')');
    }

    /**
     * Appends a curve used as a component of a compound curve or as a ring of a curve polygon.
     * A {@code LineString} is written as a bare parenthesized list of coordinates, which the
     * grammar defines as meaning a line string; anything else is written in full.
     */
    private void appendCurveComponent(final StringBuilder sb, final Curve curve, final boolean hasM) {
        if (curve instanceof LineString g) {
            appendPointList(sb, g.getDataPoints(), hasM);
        } else {
            format(sb, curve);
        }
    }

    /**
     * Appends the rings of a polygon as {@code "((0 0, 1 0, 1 1, 0 0), (…))"}, without keyword.
     * A triangle is written the same way: its exterior ring already closes on its first corner.
     */
    private void appendRings(final StringBuilder sb, final Polygon polygon, final boolean hasM) {
        if (polygon.isEmpty()) {
            sb.append("EMPTY");
            return;
        }
        sb.append('(');
        appendPointList(sb, polygon.getExteriorRing().getDataPoints(), hasM);
        for (int i = 0, n = polygon.getNumInteriorRing(); i < n; i++) {
            sb.append(", ");
            appendPointList(sb, polygon.getInteriorRingN(i).getDataPoints(), hasM);
        }
        sb.append(')');
    }

    /**
     * Appends {@code "(1 2, 3 4)"}: a parenthesized list of coordinate tuples.
     */
    private void appendPointList(final StringBuilder sb, final DataPoints points, final boolean hasM) {
        sb.append('(');
        for (int i = 0, n = points.size(); i < n; i++) {
            if (i != 0) sb.append(", ");
            appendPosition(sb, points.getPosition(i), hasM ? points.getAttribute(i, AttributesType.ATT_M) : null);
        }
        sb.append(')');
    }

    /**
     * Appends {@code "1 2 3 4"}: the ordinates of one position, followed by its measure if any.
     */
    private void appendPosition(final StringBuilder sb, final Tuple<?> position, final Tuple<?> measure) {
        for (int i = 0, n = position.getDimension(); i < n; i++) {
            if (i != 0) sb.append(' ');
            appendNumber(sb, position.get(i));
        }
        if (measure != null) {
            sb.append(' ');
            appendNumber(sb, measure.get(0));
        }
    }

    /**
     * Appends one ordinate, rounded to the precision given to the constructor if there was one.
     * Values which are not finite have no decimal representation to round, and are written
     * as {@code NaN} or {@code Infinity} — neither of which the grammar allows, but refusing
     * to write them would make the geometry impossible to inspect at all.
     */
    private void appendNumber(final StringBuilder sb, final double value) {
        if (format == null || !Double.isFinite(value)) {
            sb.append(value);
            StringBuilders.trimFractionalPart(sb);
        } else {
            sb.append(format.format(value));
        }
    }

    /**
     * Appends the keyword and the dimension flag of a geometry, then {@code "EMPTY"} if the
     * geometry is empty. Returns {@code true} if the caller has nothing more to write.
     */
    private static boolean appendHeader(final StringBuilder sb, final String keyword, final Geometry geometry) {
        sb.append(keyword);
        final CoordinateReferenceSystem crs = geometry.getCoordinateReferenceSystem();
        if (crs == null) {
            throw new IllegalArgumentException("Cannot write a " + keyword + " in Well-Known Text:"
                    + " it has no coordinate reference system, therefore no known number of dimensions."
                    + " An empty collection takes one from the factory method which creates it.");
        }
        final int dimension = crs.getCoordinateSystem().getDimension();
        final boolean hasM = hasMeasure(geometry);
        switch (dimension) {
            case 2:  sb.append(hasM ?  " M " : " ");   break;
            case 3:  sb.append(hasM ? " ZM " : " Z "); break;
            default: throw new IllegalArgumentException("Cannot write a " + keyword + " in Well-Known Text:"
                        + " its positions have " + dimension + " dimensions,"
                        + " but the format defines only 2 and 3.");
        }
        if (geometry.isEmpty()) {
            sb.append("EMPTY");
            return true;
        }
        return false;
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
        return new IllegalArgumentException("Well-Known Text defines no representation for "
                + geometry.getClass().getSimpleName() + '.');
    }
}
