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
package org.apache.sis.filter;

import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.math.Vector;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.pending.geoapi.filter.Literal;
import org.apache.sis.pending.geoapi.filter.DistanceOperatorName;


/**
 * {@link BinarySpatialFilter} tests shared by all geometry libraries.
 * Subclasses must specify a geometry library such as JTS, ESRI or Java2D.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 *
 * @param  <G> root class of geometry implementation.
 */
public abstract class BinarySpatialFilterTestCase<G> extends TestCase {
    /**
     * The factory to use for testing purpose.
     */
    private final DefaultFilterFactory<AbstractFeature, G, Object> factory;

    /**
     * The geometry library used by this factory.
     */
    private final Geometries<G> library;

    /**
     * Creates a new test case.
     *
     * @param  rootGeometry  the root geometry class as on of JTS, ESRI or Java2D root class.
     */
    @SuppressWarnings("unchecked")
    protected BinarySpatialFilterTestCase(final Class<G> rootGeometry) {
        factory = new DefaultFilterFactory.Features<>(rootGeometry, Object.class, WraparoundMethod.SPLIT);
        library = (Geometries<G>) Geometries.factory(rootGeometry);
        assertEquals(rootGeometry, library.rootClass);
    }

    /**
     * Expressions used as constant for the tests.
     */
    private enum Polygon {
        DISTANCE_1, DISTANCE_3, INTERSECT, CONTAINS, CROSSES, TOUCHES, RIGHT
    }

    /**
     * Creates the polygon identified by the given enumeration value.
     */
    private Literal<AbstractFeature,G> literal(final Polygon p) {
        final byte[] coordinates;
        boolean polygon = true;
        switch (p) {
            case RIGHT:      coordinates = new byte[] {5,  5,    5, 10,   10, 10,   10, 5,   5,  5}; break;
            case DISTANCE_1: coordinates = new byte[] {5, +1,   10, +1,   10,  4,   5,  4,   5, +1}; break;
            case DISTANCE_3: coordinates = new byte[] {5, -1,   10, -1,   10,  2,   5,  2,   5, -1}; break;
            case INTERSECT:  coordinates = new byte[] {7,  3,    9,  3,    9,  6,   7,  6,   7,  3}; break;
            case CONTAINS:   coordinates = new byte[] {1,  1,   11,  1,   11, 20,   1, 20,   1,  1}; break;
            case CROSSES:    coordinates = new byte[] {4,  6,    7,  8,   12,  9};  polygon = false; break;
            case TOUCHES:    coordinates = new byte[] {4,  2,    7,  5,    9,  3};  polygon = false; break;
            default: throw new AssertionError(p);
        }
        return (Literal<AbstractFeature,G>)
                factory.literal(library.createPolyline(polygon, 2, Vector.create(coordinates, false)));
    }

    /**
     * Tests {@code bbox(Expression, Envelope)}.
     */
    @Test
    public void testBBOX() {
        double x, y;
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.bbox(geometry, new Envelope2D(null, x=1, y=1, 6-x, 6-y));
        assertTrue(filter.test(null));

        filter = factory.bbox(geometry, new Envelope2D(null, x=-3, y=-2, 4-x, 1-y));
        assertFalse(filter.test(null));
    }

    /**
     * Ensures that expressions provided as arguments for BBOX filter are not hidden.
     * If they are wrapped for internal purpose, the wrappers should not be publicly exposed.
     */
    @Test
    @DisplayName("BBOX preserve expression type")
    public void testExpressionType() {
        var geometry = literal(Polygon.RIGHT);
        var filter   = factory.bbox(geometry, new Envelope2D(null, 0, 0, 1, 1));
        var arg2     = assertInstanceOf(Literal.class, filter.getExpressions().get(1));
        assertInstanceOf(Envelope.class, arg2.getValue(), "Second argument value should be an envelope.");
        assertSame(arg2, filter.getExpressions().get(1), "The two ways to acquire the second argument return different values.");
    }

    /**
     * Tests {@code beyond(Expression, Expression, Quantity)}.
     */
    @Test
    public void testBeyond() {
        var geometry = literal(Polygon.RIGHT);
        var distance = Quantities.create(1.5, Units.METRE);
        var filter   = factory.beyond(geometry, literal(Polygon.DISTANCE_1), distance);
        assertFalse(filter.test(null));

        filter = factory.beyond(geometry, literal(Polygon.DISTANCE_3), distance);
        assertTrue(filter.test(null));
    }

    /**
     * Tests {@code contains(Expression, Expression)}.
     */
    @Test
    public void testContains() {
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.contains(literal(Polygon.CONTAINS), geometry);
        assertTrue(filter.test(null));

        filter = factory.contains(literal(Polygon.DISTANCE_1), geometry);
        assertFalse(filter.test(null));
    }

    /**
     * Tests {@code crosses(Expression, Expression)}.
     */
    @Test
    public void testCrosses() {
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.crosses(literal(Polygon.CONTAINS), geometry);
        assertFalse(filter.test(null));

        filter = factory.crosses(literal(Polygon.CROSSES), geometry);
        assertTrue(filter.test(null));

        filter = factory.crosses(literal(Polygon.DISTANCE_1), geometry);
        assertFalse(filter.test(null));
    }

    /**
     * Tests {@code within(Expression, Expression, Quantity)}.
     */
    @Test
    public void testDWithin() {
        var geometry = literal(Polygon.RIGHT);
        var distance = Quantities.create(1.5, Units.METRE);
        var filter   = factory.within(geometry, literal(Polygon.DISTANCE_1), distance);
        assertTrue(filter.test(null));

        filter = factory.within(geometry, literal(Polygon.DISTANCE_3), distance);
        assertFalse(filter.test(null));
    }

    /**
     * Tests {@code disjoint(Expression, Expression)}.
     */
    @Test
    public void testDisjoint() {
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.disjoint(literal(Polygon.CONTAINS), geometry);
        assertFalse(filter.test(null));

        filter = factory.disjoint(literal(Polygon.CROSSES), geometry);
        assertFalse(filter.test(null));

        filter = factory.disjoint(literal(Polygon.DISTANCE_1), geometry);
        assertTrue(filter.test(null));
    }

    /**
     * Tests {@code equals(Expression, Expression)}.
     */
    @Test
    public void testEquals() {
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.equals(literal(Polygon.CONTAINS), geometry);
        assertFalse(filter.test(null));

        filter = factory.equals(literal(Polygon.CROSSES), geometry);
        assertFalse(filter.test(null));

        filter = factory.equals(literal(Polygon.RIGHT), geometry);
        assertTrue(filter.test(null));
    }

    /**
     * Tests {@code intersects(Expression, Expression)}.
     */
    @Test
    public void testIntersect() {
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.intersects(literal(Polygon.CONTAINS), geometry);
        assertTrue(filter.test(null));

        filter = factory.intersects(literal(Polygon.CROSSES), geometry);
        assertTrue(filter.test(null));

        filter = factory.intersects(literal(Polygon.INTERSECT), geometry);
        assertTrue(filter.test(null));

        filter = factory.intersects(literal(Polygon.DISTANCE_1), geometry);
        assertFalse(filter.test(null));

        filter = factory.intersects(literal(Polygon.DISTANCE_3), geometry);
        assertFalse(filter.test(null));
    }

    /**
     * Tests {@code overlaps(Expression, Expression)}.
     */
    @Test
    public void testOverlaps() {
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.overlaps(literal(Polygon.CONTAINS), geometry);
        assertFalse(filter.test(null));

        filter = factory.overlaps(literal(Polygon.DISTANCE_1), geometry);
        assertFalse(filter.test(null));

        filter = factory.overlaps(literal(Polygon.CROSSES), geometry);
        assertFalse(filter.test(null));

        filter = factory.overlaps(literal(Polygon.INTERSECT), geometry);
        assertTrue(filter.test(null));
    }

    /**
     * Tests {@code touches(Expression, Expression)}.
     */
    @Test
    public void testTouches() {
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.touches(literal(Polygon.CONTAINS), geometry);
        assertFalse(filter.test(null));

        filter = factory.touches(literal(Polygon.CROSSES), geometry);
        assertFalse(filter.test(null));

        filter = factory.touches(literal(Polygon.DISTANCE_1), geometry);
        assertFalse(filter.test(null));

        filter = factory.touches(literal(Polygon.TOUCHES), geometry);
        assertTrue(filter.test(null));
    }

    /**
     * Tests {@code within(Expression, Expression)}.
     */
    @Test
    public void testWithin() {
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.within(literal(Polygon.CONTAINS), geometry);
        assertFalse(filter.test(null));

        filter = factory.within(literal(Polygon.CROSSES), geometry);
        assertFalse(filter.test(null));

        filter = factory.within(literal(Polygon.DISTANCE_1), geometry);
        assertFalse(filter.test(null));

        filter = factory.within(literal(Polygon.TOUCHES), geometry);
        assertFalse(filter.test(null));

        filter = factory.within(geometry, literal(Polygon.CONTAINS) );
        assertTrue(filter.test(null));
    }

    /**
     * Tests an operation with operands in different CRS. The filter evaluation requires a map projection.
     * To make the test harder, the CRS uses an authority different than EPSG.
     */
    @Test
    public void testWithReprojection() {
        final var crs = HardCodedConversions.ESRI();
        final var geometry = literal(Polygon.TOUCHES);
        library.castOrWrap(geometry.getValue()).setCoordinateReferenceSystem(crs);

        // Initial verification without reprojection.
        var envelope = new Envelope2D(crs, 0, 0, 10, 10);
        var filter = factory.bbox(geometry, envelope);
        assertTrue(filter.test(null));

        // Ensure no error is raised, even if a reprojection is involved.
        envelope = new Envelope2D(HardCodedCRS.WGS84, -1.36308465, -5.98385631, 6.56E-5, 6.57E-5);
        filter = factory.bbox(geometry, envelope);
        filter.test(null);
    }

    /**
     * Tests serialization of a filter.
     */
    @Test
    public void testSerialization() {
        var geometry = literal(Polygon.RIGHT);
        var filter = factory.overlaps(literal(Polygon.CONTAINS), geometry);
        assertSerializedEquals(filter);
    }

    /**
     * Ensures that a world geographic envelope, once converted to a polygon and reprojected, remain coherent.
     * This is a regression test. In the past, the operation pipeline [envelope → polygon → reprojected polygon]
     * caused the result to degenerate to single line following the anti-meridian.
     *
     * @throws FactoryException if an error occurred while fetching a CRS.
     * @throws TransformException if a coordinate conversion was required but failed.
     */
    @Test
    public void testSpatialContextDoesNotDegenerateEnvelope() throws FactoryException, TransformException {
        var sourceCRS = HardCodedCRS.WGS84;
        var e1 = new Envelope2D(sourceCRS, -180, -90, 360, 180);
        var filter = new DistanceFilter<>(DistanceOperatorName.WITHIN,
                library, factory.literal(e1),
                factory.literal(new DirectPosition2D(sourceCRS, 44, 2)),
                Quantities.create(1.0, Units.METRE));

        GeneralEnvelope envInCtx = filter.context.transform(filter.expression1.apply(null)).getEnvelope();
        double xmin = envInCtx.getMinimum(0);
        double xmax = envInCtx.getMaximum(0);
        assertNotEquals(xmin, xmax, 1000, "Degenerated envelope.");

        double expected = sourceCRS.getDatum().getEllipsoid().getSemiMajorAxis() * (2*Math.PI);
        assertEquals(expected, xmax - xmin, expected / 1000);
    }
}
