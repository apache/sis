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

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.math.Vector;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.DistanceOperator;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.filter.BinarySpatialOperator;


/**
 * {@link BinarySpatialFilter} tests shared by all geometry libraries.
 * Subclasses must specify a geometry library such as JTS, ESRI or Java2D.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.4
 *
 * @param  <G> root class of geometry implementation.
 *
 * @since 1.1
 */
public abstract class BinarySpatialFilterTestCase<G> extends TestCase {
    /**
     * The factory to use for testing purpose.
     */
    private final FilterFactory<Feature, G, Object> factory;

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
        library = (Geometries<G>) Geometries.implementation(rootGeometry);
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
    private Literal<Feature,G> literal(final Polygon p) {
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
        return factory.literal(library.createPolyline(polygon, 2, Vector.create(coordinates, false)));
    }

    /**
     * Tests {@link DefaultFilterFactory#bbox(Expression, Envelope)}
     */
    @Test
    public void testBBOX() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);

        double x=1, y=1;
        BinarySpatialOperator<Feature> bbox = factory.bbox(right, new Envelope2D(null, x, y, 6-x, 6-y));
        assertTrue(bbox.test(null));

        x = -3; y = -2;
        bbox = factory.bbox(right, new Envelope2D(null, x, y, 4-x, 1-y));
        assertFalse(bbox.test(null));
    }

    /**
     * Ensures that expressions provided as arguments for BBOX filter are not hidden.
     * If they are wrapped for internal purpose, the wrappers should not be publicly exposed.
     */
    @Test
    public void bbox_preserve_expression_type() {
        final BinarySpatialOperator<Feature> bbox = factory.bbox(literal(Polygon.RIGHT), new Envelope2D(null, 0, 0, 1, 1));
        final Expression<Feature,?> arg2 = bbox.getOperand2();
        assertSame("The two ways to acquire the second argument return different values.", arg2, bbox.getExpressions().get(1));
        assertInstanceOf("Second argument value should be an envelope.", Envelope.class,
                         ((Literal<Feature,?>) arg2).getValue());
    }

    /**
     * Tests {@link DefaultFilterFactory#beyond(Expression, Expression, Quantity)}
     */
    @Test
    public void testBeyond() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);
        final Length distance = Quantities.create(1.5, Units.METRE);

        DistanceOperator<Feature> beyond = factory.beyond(right, literal(Polygon.DISTANCE_1), distance);
        assertFalse(beyond.test(null));

        beyond = factory.beyond(right, literal(Polygon.DISTANCE_3), distance);
        assertTrue(beyond.test(null));
    }

    /**
     * Tests {@link DefaultFilterFactory#contains(Expression, Expression)}
     */
    @Test
    public void testContains() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);

        BinarySpatialOperator<Feature> contains = factory.contains(literal(Polygon.CONTAINS), right);
        assertTrue(contains.test(null));

        contains = factory.contains(literal(Polygon.DISTANCE_1), right);
        assertFalse(contains.test(null));
    }

    /**
     * Tests {@link DefaultFilterFactory#crosses(Expression, Expression)}
     */
    @Test
    public void testCrosses() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);

        BinarySpatialOperator<Feature> crosses = factory.crosses(literal(Polygon.CONTAINS), right);
        assertFalse(crosses.test(null));

        crosses = factory.crosses(literal(Polygon.CROSSES), right);
        assertTrue(crosses.test(null));

        crosses = factory.crosses(literal(Polygon.DISTANCE_1), right);
        assertFalse(crosses.test(null));
    }

    /**
     * Tests {@link DefaultFilterFactory#within(Expression, Expression, Quantity)}
     */
    @Test
    public void testDWithin() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);
        final Length distance = Quantities.create(1.5, Units.METRE);

        DistanceOperator<Feature> within = factory.within(right, literal(Polygon.DISTANCE_1), distance);
        assertTrue(within.test(null));

        within = factory.within(right, literal(Polygon.DISTANCE_3), distance);
        assertFalse(within.test(null));
    }

    /**
     * Tests {@link DefaultFilterFactory#disjoint(Expression, Expression)}
     */
    @Test
    public void testDisjoint() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);

        BinarySpatialOperator<Feature> disjoint = factory.disjoint(literal(Polygon.CONTAINS), right);
        assertFalse(disjoint.test(null));

        disjoint = factory.disjoint(literal(Polygon.CROSSES), right);
        assertFalse(disjoint.test(null));

        disjoint = factory.disjoint(literal(Polygon.DISTANCE_1), right);
        assertTrue(disjoint.test(null));
    }

    /**
     * Tests {@link DefaultFilterFactory#equals(Expression, Expression)}
     */
    @Test
    public void testEquals() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);

        BinarySpatialOperator<Feature> equal = factory.equals(literal(Polygon.CONTAINS), right);
        assertFalse(equal.test(null));

        equal = factory.equals(literal(Polygon.CROSSES), right);
        assertFalse(equal.test(null));

        equal = factory.equals(literal(Polygon.RIGHT), right);
        assertTrue(equal.test(null));
    }

    /**
     * Tests {@link DefaultFilterFactory#intersects(Expression, Expression)}
     */
    @Test
    public void testIntersect() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);

        BinarySpatialOperator<Feature> intersect = factory.intersects(literal(Polygon.CONTAINS), right);
        assertTrue(intersect.test(null));

        intersect = factory.intersects(literal(Polygon.CROSSES), right);
        assertTrue(intersect.test(null));

        intersect = factory.intersects(literal(Polygon.INTERSECT), right);
        assertTrue(intersect.test(null));

        intersect = factory.intersects(literal(Polygon.DISTANCE_1), right);
        assertFalse(intersect.test(null));

        intersect = factory.intersects(literal(Polygon.DISTANCE_3), right);
        assertFalse(intersect.test(null));
    }

    /**
     * Tests {@link DefaultFilterFactory#overlaps(Expression, Expression)}
     */
    @Test
    public void testOverlaps() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);

        BinarySpatialOperator<Feature> overlaps = factory.overlaps(literal(Polygon.CONTAINS), right);
        assertFalse(overlaps.test(null));

        overlaps = factory.overlaps(literal(Polygon.DISTANCE_1), right);
        assertFalse(overlaps.test(null));

        overlaps = factory.overlaps(literal(Polygon.CROSSES), right);
        assertFalse(overlaps.test(null));

        overlaps = factory.overlaps(literal(Polygon.INTERSECT), right);
        assertTrue(overlaps.test(null));
    }

    /**
     * Tests {@link DefaultFilterFactory#touches(Expression, Expression)}
     */
    @Test
    public void testTouches() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);

        BinarySpatialOperator<Feature> touches = factory.touches(literal(Polygon.CONTAINS), right);
        assertFalse(touches.test(null));

        touches = factory.touches(literal(Polygon.CROSSES), right);
        assertFalse(touches.test(null));

        touches = factory.touches(literal(Polygon.DISTANCE_1), right);
        assertFalse(touches.test(null));

        touches = factory.touches(literal(Polygon.TOUCHES), right);
        assertTrue(touches.test(null));
    }

    /**
     * Tests {@link DefaultFilterFactory#within(Expression, Expression)}
     */
    @Test
    public void testWithin() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);

        BinarySpatialOperator<Feature> within = factory.within(literal(Polygon.CONTAINS), right);
        assertFalse(within.test(null));

        within = factory.within(literal(Polygon.CROSSES), right);
        assertFalse(within.test(null));

        within = factory.within(literal(Polygon.DISTANCE_1), right);
        assertFalse(within.test(null));

        within = factory.within(literal(Polygon.TOUCHES), right);
        assertFalse(within.test(null));

        within = factory.within(right, literal(Polygon.CONTAINS) );
        assertTrue(within.test(null));
    }

    /**
     * Tests an operation with operands in different CRS. The filter evaluation requires a map projection.
     * To make the test harder, the CRS uses an authority different than EPSG.
     */
    @Test
    public void testWithReprojection() {
        final CoordinateReferenceSystem crs = HardCodedConversions.ESRI();
        final Literal<Feature,G> geom = literal(Polygon.TOUCHES);
        library.castOrWrap(geom.getValue()).setCoordinateReferenceSystem(crs);

        // Initial verification without reprojection.
        Envelope2D envelope = new Envelope2D(crs, 0, 0, 10, 10);
        BinarySpatialOperator<Feature> filter = factory.bbox(geom, envelope);
        assertTrue(filter.test(null));

        // Ensure no error is raised, even if a reprojection is involved.
        envelope = new Envelope2D(HardCodedCRS.WGS84, -1.36308465, -5.98385631, 6.56E-5, 6.57E-5);
        filter = factory.bbox(geom, envelope);
        assertTrue(filter.test(null));
    }

    /**
     * Tests serialization of a filter.
     */
    @Test
    public void testSerialization() {
        final Literal<Feature,G> right = literal(Polygon.RIGHT);
        BinarySpatialOperator<Feature> overlaps = factory.overlaps(literal(Polygon.CONTAINS), right);
        assertSerializedEquals(overlaps);
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
        final GeographicCRS sourceCRS = HardCodedCRS.WGS84;
        final Envelope e1 = new Envelope2D(sourceCRS, -180, -90, 360, 180);
        final DistanceFilter<?, G> within = new DistanceFilter<>(DistanceOperatorName.WITHIN,
                library, factory.literal(e1),
                factory.literal(new DirectPosition2D(sourceCRS, 44, 2)),
                Quantities.create(1.0, Units.METRE));

        final GeneralEnvelope envInCtx = within.context.transform(within.expression1.apply(null)).getEnvelope();
        final double xmin = envInCtx.getMinimum(0);
        final double xmax = envInCtx.getMaximum(0);
        assertNotEquals("Degenerated envelope.", xmin, xmax, 1000);

        final double expected = sourceCRS.getDatum().getEllipsoid().getSemiMajorAxis() * (2*Math.PI);
        assertEquals(expected, xmax - xmin, expected / 1000);
    }
}
