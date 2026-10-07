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
package org.apache.sis.geometries.operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.adapter.WellKnownText;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.MultiCurve;
import org.apache.sis.geometries.curve.MultiLineString;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.geometries.surface.MultiPolygon;
import org.apache.sis.geometries.surface.MultiSurface;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Base class of the tests of the spatial relationships and of the operations derived from them.
 * It holds the geometries those tests share, so that a test class inherits them by extending
 * this class instead of importing them one by one.
 *
 * <p>The geometries are immutable, which allows the test classes to declare them once and to
 * reuse the same instances in every test case. They are declared as Well-Known Text, so that
 * the shape under test can be read at a glance, and their coordinates are chosen so that every
 * expected value is exact.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractD9IMTest {
    /**
     * The coordinate reference system of all the geometries declared in this class.
     */
    public static final CoordinateReferenceSystem CRS_2D = CommonCRS.WGS84.geographic();

    /**
     * Tolerance threshold on the measurements computed by the operations. 
     * For floating point arithmetic errors.
     */
    protected static final double TOLERANCE = 1E-9;

    /**
     * An empty geometry. Together with {@link #EMPTY_2}, it allows to verify that the result of an
     * operation on the empty set depends on the emptiness of the operands, not on their identity.
     */
    public static final Empty EMPTY_1 = GeometryFactory.createEmpty(CRS_2D);

    /**
     * Another empty geometry, distinct from {@link #EMPTY_1} but equal to it as a set of positions.
     */
    public static final Empty EMPTY_2 = GeometryFactory.createEmpty(CRS_2D);

    /**
     * An arbitrary geometry which is not empty, used as the other operand of the operations
     * tested against the empty set.
     */
    public static final Point NON_EMPTY = GeometryFactory.createPoint(CRS_2D, 10.0, 5.0);

    /**
     * An arbitrary point. Together with {@link #POINT_A_BIS}, it allows to verify that the result
     * of an operation on two points depends on their positions, not on their identity.
     */
    public static final Point POINT_A = GeometryFactory.createPoint(CRS_2D, 10.0, 5.0);

    /**
     * Another point, distinct from {@link #POINT_A} but at the same position.
     */
    public static final Point POINT_A_BIS = GeometryFactory.createPoint(CRS_2D, 10.0, 5.0);

    /**
     * A point at a position different than {@link #POINT_A}.
     */
    public static final Point POINT_B = GeometryFactory.createPoint(CRS_2D, 20.0, 15.0);

    /**
     * The empty geometry expected as the result of an operation which found no position,
     * for example the intersection of {@link #POINT_A} with {@link #POINT_B}.
     * It is a distinct instance from {@link #EMPTY_1} on purpose: an operation builds its
     * result rather than returning an operand when neither operand is empty.
     */
    public static final Empty EMPTY_RESULT = GeometryFactory.createEmpty(CRS_2D);

    /*
     * ────────────────────────────────────────────────────────────────────────────────────────
     * Geometries used by the tests of the measurements, of the derived positions and of the
     * spatial relationships. They describe what those operations shall answer, not how they
     * compute it, so that they keep their meaning if the implementation changes. They are
     * declared as Well-Known Text so that the shape under test can be read at a glance.
     *
     * The operations are specified in terms of the interior, the boundary and the exterior of
     * their operands, so one geometry per combination of those is enough: a `LinearRing` is a
     * `LineString`, and asking the same question about both asks the same question twice. The
     * combinations covered are a position, a set of positions, a curve, a set of curves, a
     * surface, a surface with a hole, a set of surfaces and a heterogeneous collection.
     *
     * All the geometries below are laid out on the following grid, so that every expected
     * result is an exact value which can be checked by hand:
     *
     *     y=20  ├─ LINE_FAR ─┤
     *     y=15                    ┌────────────┐
     *     y=10  ┌────────────┬────┼───┐        │   SQUARE_OVERLAP = [5…15]²
     *           │   SQUARE   │ SQUARE_TOUCHING │
     *           │  [0…10]²   │    │   │        │
     *     y=0   └────────────┴────┴───┘────────┘
     *           x=0         x=10      x=15   x=20
     * ────────────────────────────────────────────────────────────────────────────────────────
     */

    /**
     * A position strictly inside {@link #SQUARE}, and strictly inside the hole of
     * {@link #SQUARE_WITH_HOLE} and therefore outside that polygon.
     */
    public static final Point POINT_CENTER = (Point) wkt("POINT (5 5)");

    /**
     * A position on the boundary of {@link #SQUARE} and in the interior of {@link #LINE_BOTTOM}.
     */
    public static final Point POINT_ON_EDGE = (Point) wkt("POINT (5 0)");

    /**
     * A corner of {@link #SQUARE}, which is also an end point of {@link #LINE_BOTTOM}
     * and therefore on the boundary of that curve.
     */
    public static final Point POINT_CORNER = (Point) wkt("POINT (0 0)");

    /**
     * The other end point of {@link #LINE_BOTTOM}.
     */
    public static final Point POINT_LINE_END = (Point) wkt("POINT (10 0)");

    /**
     * A position outside every other geometry declared in this class, except the aggregates
     * which include {@link #SQUARE_DISJOINT}.
     */
    public static final Point POINT_OUTSIDE = (Point) wkt("POINT (20 20)");

    /**
     * A curve of length 10 lying exactly on the bottom edge of {@link #SQUARE}.
     * Its interior is therefore entirely on the boundary of that polygon.
     */
    public static final LineString LINE_BOTTOM = (LineString) wkt("LINESTRING (0 0, 10 0)");

    /**
     * A curve of length 10√2 joining two opposite corners of {@link #SQUARE}.
     * It meets {@link #LINE_BOTTOM} only at the end point they have in common.
     */
    public static final LineString LINE_DIAGONAL = (LineString) wkt("LINESTRING (0 0, 10 10)");

    /**
     * A curve passing right through {@link #SQUARE}, entering by its bottom edge and leaving by
     * its top edge. It meets {@link #LINE_BOTTOM} at one position interior to both of them.
     */
    public static final LineString LINE_CROSSING = (LineString) wkt("LINESTRING (5 -5, 5 15)");

    /**
     * A curve of length 6 strictly inside {@link #SQUARE}, end points included.
     */
    public static final LineString LINE_INSIDE = (LineString) wkt("LINESTRING (2 5, 8 5)");

    /**
     * A curve collinear with {@link #LINE_BOTTOM}, which it overlaps on half of its length.
     */
    public static final LineString LINE_COLLINEAR = (LineString) wkt("LINESTRING (5 0, 15 0)");

    /**
     * A curve of length 10 parallel to {@link #LINE_BOTTOM}, at a distance of 20 from it
     * and at a distance of 10 from {@link #SQUARE}.
     */
    public static final LineString LINE_FAR = (LineString) wkt("LINESTRING (0 20, 10 20)");

    /**
     * A curve crossing itself at (5 5). It is {@linkplain Geometry#isValid() valid}, since a
     * curve is allowed to cross itself, but it is not {@linkplain Geometry#isSimple() simple}.
     */
    public static final LineString LINE_SELF_CROSSING = (LineString) wkt("LINESTRING (0 0, 10 10, 10 0, 0 10)");

    /**
     * A square of 10 × 10, therefore of area 100 and of perimeter 40, with its centroid at (5 5).
     */
    public static final Polygon SQUARE = (Polygon) wkt("POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0))");

    /**
     * Another square at the same position as {@link #SQUARE}, but a distinct instance.
     * It allows to verify that the operations depend on the positions, not on the identity.
     */
    public static final Polygon SQUARE_BIS = (Polygon) wkt("POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0))");

    /**
     * {@link #SQUARE} with a square hole of 2 × 2 at its center, therefore of area 96 and of
     * boundary length 48. Its centroid stays at (5 5) by symmetry, but that position is in the
     * hole and therefore outside the polygon.
     */
    public static final Polygon SQUARE_WITH_HOLE =
            (Polygon) wkt("POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0), (4 4, 6 4, 6 6, 4 6, 4 4))");

    /**
     * A square of area 4 strictly inside {@link #SQUARE}.
     */
    public static final Polygon SQUARE_INNER = (Polygon) wkt("POLYGON ((2 2, 4 2, 4 4, 2 4, 2 2))");

    /**
     * A square of area 100 overlapping {@link #SQUARE} on a quarter of its area.
     */
    public static final Polygon SQUARE_OVERLAP = (Polygon) wkt("POLYGON ((5 5, 15 5, 15 15, 5 15, 5 5))");

    /**
     * A square sharing a whole edge with {@link #SQUARE} but no interior position.
     */
    public static final Polygon SQUARE_TOUCHING = (Polygon) wkt("POLYGON ((10 0, 20 0, 20 10, 10 10, 10 0))");

    /**
     * A square of area 100 having no position in common with {@link #SQUARE},
     * the shortest distance between the two being 10√2.
     */
    public static final Polygon SQUARE_DISJOINT = (Polygon) wkt("POLYGON ((20 20, 30 20, 30 30, 20 30, 20 20))");

    /**
     * A polygon whose ring crosses itself at (5 5), and which is therefore not
     * {@linkplain Geometry#isValid() valid}.
     */
    public static final Polygon SQUARE_BOWTIE = (Polygon) wkt("POLYGON ((0 0, 10 10, 10 0, 0 10, 0 0))");

    /**
     * Two positions strictly inside {@link #SQUARE}.
     */
    public static final MultiPoint<?> MULTI_POINT_INSIDE = (MultiPoint<?>) wkt("MULTIPOINT ((2 2), (5 5))");

    /**
     * Two positions, one inside {@link #SQUARE} and one outside it. A set of positions straddling
     * a surface this way crosses it, which a single position can never do.
     */
    public static final MultiPoint<?> MULTI_POINT_SPREAD = (MultiPoint<?>) wkt("MULTIPOINT ((5 5), (20 20))");

    /**
     * The same position twice, which makes this set of positions not
     * {@linkplain Geometry#isSimple() simple}.
     */
    public static final MultiPoint<?> MULTI_POINT_REPEATED = (MultiPoint<?>) wkt("MULTIPOINT ((5 5), (5 5))");

    /**
     * Two disjoint curves of length 10 each, therefore of total length 20.
     */
    public static final MultiLineString MULTI_LINE =
            (MultiLineString) wkt("MULTILINESTRING ((0 0, 10 0), (0 20, 10 20))");

    /**
     * Two disjoint squares of area 100 each, therefore of total area 200,
     * with their centroid half way between the two centroids.
     */
    public static final MultiPolygon MULTI_POLYGON = (MultiPolygon) wkt("MULTIPOLYGON (((0 0, 10 0, 10 10, 0 10, 0 0)), ((20 20, 30 20, 30 30, 20 30, 20 20)))");

    /**
     * A heterogeneous collection of a position and a curve. Only the components of the largest
     * dimension contribute to the centroid, which is therefore the centroid of the curve.
     */
    public static final GeometryCollection<?> COLLECTION =
            (GeometryCollection<?>) wkt("GEOMETRYCOLLECTION (POINT (5 5), LINESTRING (0 0, 10 0))");

    /**
     * Decodes the given Well-Known Text in the {@link #CRS_2D} reference system.
     * The geometries of this class are declared this way so that the shape under
     * test can be read at a glance.
     *
     * @param  text  the Well-Known Text to decode.
     * @return the geometry represented by the given text.
     */
    public static Geometry wkt(final String text) {
        return new WellKnownText().decode(text, CRS_2D);
    }

    /**
     * Returns the area of the given geometry, which shall be a surface or a set of surfaces.
     * This method also verifies that the geometry has the expected dimension, an operation
     * which returns a surface never being allowed to return a curve or a position instead.
     *
     * @param  geometry  the geometry from which to get the area.
     * @return area of the given geometry, in the units of the coordinate system axes.
     */
    public static double areaOf(final Geometry geometry) {
        if (geometry instanceof Surface s) {
            return s.getArea().getValue().doubleValue();
        }
        if (geometry instanceof MultiSurface<?> m) {
            return m.getArea().getValue().doubleValue();
        }
        throw new AssertionError("Not a surface: " + geometry.getClass().getSimpleName());
    }

    /**
     * Returns the length of the given geometry, which shall be a curve or a set of curves.
     *
     * @param  geometry  the geometry from which to get the length.
     * @return length of the given geometry, in the units of the coordinate system axes.
     */
    public static double lengthOf(final Geometry geometry) {
        if (geometry instanceof Curve c) {
            return c.getLength().getValue().doubleValue();
        }
        if (geometry instanceof MultiCurve<?> m) {
            return m.getLength().getValue().doubleValue();
        }
        throw new AssertionError("Not a curve: " + geometry.getClass().getSimpleName());
    }

    /**
     * Creates a new test case. Reserved to the subclasses,
     * this class holding the shared geometries but no test.
     */
    protected AbstractD9IMTest() {
    }

    /**
     * Asserts that the given collection contains exactly the positions of the given points,
     * in any order. This is used for the results of the operations which are specified as a
     * set of positions, the order of which is left to the implementation.
     *
     * @param  actual    the collection of points to verify.
     * @param  expected  the points which shall be in the given collection, in any order.
     */
    public static void assertPositionsEqual(final MultiPoint<?> actual, final Point... expected) {
        final List<String> remaining = new ArrayList<>(expected.length);
        for (final Point point : expected) {
            remaining.add(Arrays.toString(point.getPosition().toArrayDouble()));
        }
        for (int i = 0; i < actual.getNumGeometries(); i++) {
            final String position = Arrays.toString(actual.getGeometryN(i).getPosition().toArrayDouble());
            assertTrue(remaining.remove(position), () -> "Unexpected position " + position + '.');
        }
        assertTrue(remaining.isEmpty(), () -> "Missing positions " + remaining + '.');
    }
}
