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
package org.apache.sis.test;

import java.util.Collection;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.AffineTransform;
import javax.measure.unit.Unit;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.util.GenericName;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.internal.util.Constants;

import static java.lang.StrictMath.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Assertion methods used by the {@code sis-referencing} module in addition of the ones inherited
 * from other modules and libraries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public strictfp class ReferencingAssert extends MetadataAssert {
    /**
     * For subclass constructor only.
     */
    protected ReferencingAssert() {
    }

    /**
     * Asserts that the given identifier has the expected code and the {@code "OGC"} code space.
     * The authority is expected to be {@link Citations#OGC}. We expect the exact same authority
     * instance because identifiers in OGC namespace are often hard-coded in SIS.
     *
     * @param expected  the expected identifier code.
     * @param actual    the identifier to verify.
     *
     * @since 0.6
     */
    public static void assertOgcIdentifierEquals(final String expected, final ReferenceIdentifier actual) {
        assertNotNull(actual);
        assertEquals("code",       expected,      actual.getCode());
        assertEquals("codeSpace",  Constants.OGC, actual.getCodeSpace());
        assertSame  ("authority",  Citations.OGC, actual.getAuthority());
        assertEquals("identifier", Constants.OGC + DefaultNameSpace.DEFAULT_SEPARATOR + expected,
                IdentifiedObjects.toString(actual));
    }

    /**
     * Asserts that the given identifier has the expected code and the {@code "EPSG"} code space.
     * The authority is expected to have the {@code "EPSG"} title, alternate title or identifier.
     *
     * @param expected  the expected identifier code.
     * @param actual    the identifier to verify.
     *
     * @since 0.5
     */
    public static void assertEpsgIdentifierEquals(final String expected, final Identifier actual) {
        assertNotNull(actual);
        assertEquals("code",       expected,        actual.getCode());
        assertEquals("codeSpace",  Constants.EPSG,  (actual instanceof ReferenceIdentifier) ? ((ReferenceIdentifier) actual).getCodeSpace() : null);
        assertEquals("authority",  Constants.EPSG,  Citations.getIdentifier(actual.getAuthority()));
        assertEquals("identifier", Constants.EPSG + DefaultNameSpace.DEFAULT_SEPARATOR + expected,
                IdentifiedObjects.toString(actual));
    }

    /**
     * Asserts that the given object has the expected name and singleton identifier in the {@code "EPSG"} code space.
     * No other identifier than the given one is expected. The authority is expected to have the {@code "EPSG"} title,
     * alternate title or identifier.
     *
     * @param name        the expected EPSG name.
     * @param identifier  the expected EPSG identifier.
     * @param object      the object to verify.
     *
     * @since 0.6
     */
    public static void assertEpsgNameAndIdentifierEqual(final String name, final int identifier, final IdentifiedObject object) {
        assertNotNull(name, object);
        assertEpsgIdentifierEquals(name, object.getName());
        assertEpsgIdentifierEquals(String.valueOf(identifier), TestUtilities.getSingleton(object.getIdentifiers()));
    }

    /**
     * Asserts that the tip of the unique alias of the given object is equals to the expected value.
     * As a special case if the expected value is null, then this method verifies that the given object has no alias.
     *
     * @param expected  the expected alias, or {@code null} if we expect no alias.
     * @param object    the object for which to test the alias.
     */
    public static void assertAliasTipEquals(final String expected, final IdentifiedObject object) {
        final Collection<GenericName> aliases = object.getAlias();
        if (expected == null) {
            assertTrue("aliases.isEmpty()", aliases.isEmpty());
        } else {
            assertEquals("alias", expected, TestUtilities.getSingleton(aliases).tip().toString());
        }
    }

    /**
     * Compares the given coordinate system axis against the expected values.
     *
     * @param name           the expected axis name code.
     * @param abbreviation   the expected axis abbreviation.
     * @param direction      the expected axis direction.
     * @param minimumValue   the expected axis minimal value.
     * @param maximumValue   the expected axis maximal value.
     * @param unit           the expected axis unit of measurement.
     * @param rangeMeaning   the expected axis range meaning.
     * @param axis           the axis to verify.
     */
    public static void assertAxisEquals(final String name, final String abbreviation, final AxisDirection direction,
            final double minimumValue, final double maximumValue, final Unit<?> unit, final RangeMeaning rangeMeaning,
            final CoordinateSystemAxis axis)
    {
        assertEquals("name",         name,         axis.getName().getCode());
        assertEquals("abbreviation", abbreviation, axis.getAbbreviation());
        assertEquals("direction",    direction,    axis.getDirection());
        assertEquals("minimumValue", minimumValue, axis.getMinimumValue(), TestCase.STRICT);
        assertEquals("maximumValue", maximumValue, axis.getMaximumValue(), TestCase.STRICT);
        assertEquals("unit",         unit,         axis.getUnit());
        assertEquals("rangeMeaning", rangeMeaning, axis.getRangeMeaning());
    }

    /**
     * Asserts that the given parameter values are equal to the expected ones within
     * a positive delta. Only the elements in the given descriptor are compared, and
     * the comparisons are done in the units declared in the descriptor.
     *
     * @param expected   the expected parameter values.
     * @param actual     the actual parameter values.
     * @param tolerance  the tolerance threshold for comparison of numerical values.
     */
    public static void assertParameterEquals(final ParameterValueGroup expected,
            final ParameterValueGroup actual, final double tolerance)
    {
        for (final GeneralParameterValue candidate : expected.values()) {
            if (!(candidate instanceof ParameterValue<?>)) {
                throw new UnsupportedOperationException("Not yet implemented.");
            }
            final ParameterValue<?> value = (ParameterValue<?>) candidate;
            final ParameterDescriptor<?> descriptor = value.getDescriptor();
            final String   name       = descriptor.getName().getCode();
            final Unit<?>  unit       = descriptor.getUnit();
            final Class<?> valueClass = descriptor.getValueClass();
            final ParameterValue<?> e = expected.parameter(name);
            final ParameterValue<?> a = actual  .parameter(name);
            if (unit != null) {
                final double f = e.doubleValue(unit);
                assertEquals(name, f, a.doubleValue(unit), tolerance);
            } else if (valueClass == Float.class || valueClass == Double.class) {
                final double f = e.doubleValue();
                assertEquals(name, f, a.doubleValue(), tolerance);
            } else {
                assertEquals(name, e.getValue(), a.getValue());
            }
        }
    }

    /**
     * Asserts that the given matrix is diagonal, and that all elements on the diagonal are equal
     * to the given values. The matrix doesn't need to be square. The last row is handled especially
     * if the {@code affine} argument is {@code true}.
     *
     * @param expected   the values which are expected on the diagonal. If the length of this array
     *                   is less than the matrix size, then the last element in the array is repeated
     *                   for all remaining diagonal elements.
     * @param affine     if {@code true}, then the last row is expected to contains the value 1
     *                   in the last column, and all other columns set to 0.
     * @param matrix     the matrix to test.
     * @param tolerance  the tolerance threshold while comparing floating point values.
     */
    public static void assertDiagonalEquals(final double[] expected, final boolean affine,
            final Matrix matrix, final double tolerance)
    {
        final int numRows = matrix.getNumRow();
        final int numCols = matrix.getNumCol();
        final StringBuilder buffer = new StringBuilder("matrix(");
        final int bufferBase = buffer.length();
        for (int j=0; j<numRows; j++) {
            for (int i=0; i<numCols; i++) {
                final double e;
                if (affine && j == numRows-1) {
                    e = (i == numCols-1) ? 1 : 0;
                } else if (i == j) {
                    e = expected[min(expected.length-1, i)];
                } else {
                    e = 0;
                }
                buffer.setLength(bufferBase);
                assertEquals(buffer.append(j).append(',').append(i).append(')').toString(),
                        e, matrix.getElement(j, i), tolerance);
            }
        }
    }

    /**
     * Compares two affine transforms for equality.
     *
     * @param expected   the expected affine transform.
     * @param actual     the actual affine transform.
     * @param tolerance  the tolerance threshold.
     */
    public static void assertTransformEquals(final AffineTransform expected, final AffineTransform actual, final double tolerance) {
        assertEquals("scaleX",     expected.getScaleX(),     actual.getScaleX(),     tolerance);
        assertEquals("scaleY",     expected.getScaleY(),     actual.getScaleY(),     tolerance);
        assertEquals("shearX",     expected.getShearX(),     actual.getShearX(),     tolerance);
        assertEquals("shearY",     expected.getShearY(),     actual.getShearY(),     tolerance);
        assertEquals("translateX", expected.getTranslateX(), actual.getTranslateX(), tolerance);
        assertEquals("translateY", expected.getTranslateY(), actual.getTranslateY(), tolerance);
    }

    /**
     * Asserts that two rectangles have the same location and the same size.
     *
     * @param expected  the expected rectangle.
     * @param actual    the rectangle to compare with the expected one.
     * @param tolx      the tolerance threshold on location along the <var>x</var> axis.
     * @param toly      the tolerance threshold on location along the <var>y</var> axis.
     */
    public static void assertRectangleEquals(final RectangularShape expected,
            final RectangularShape actual, final double tolx, final double toly)
    {
        assertEquals("Min X",    expected.getMinX(),    actual.getMinX(),    tolx);
        assertEquals("Min Y",    expected.getMinY(),    actual.getMinY(),    toly);
        assertEquals("Max X",    expected.getMaxX(),    actual.getMaxX(),    tolx);
        assertEquals("Max Y",    expected.getMaxY(),    actual.getMaxY(),    toly);
        assertEquals("Center X", expected.getCenterX(), actual.getCenterX(), tolx);
        assertEquals("Center Y", expected.getCenterY(), actual.getCenterY(), toly);
        assertEquals("Width",    expected.getWidth(),   actual.getWidth(),   tolx*2);
        assertEquals("Height",   expected.getHeight(),  actual.getHeight(),  toly*2);
    }

    /**
     * Asserts that two envelopes have the same minimum and maximum ordinates.
     * This method ignores the envelope type (i.e. the implementation class) and the CRS.
     *
     * @param expected    the expected envelope.
     * @param actual      the envelope to compare with the expected one.
     * @param tolerances  the tolerance threshold on location along each axis. If this array length is shorter
     *                    than the number of dimensions, then the last tolerance is reused for all remaining axes.
     *                    If this array is empty, then the tolerance threshold is zero.
     *
     * @since 0.7
     */
    public static void assertEnvelopeEquals(final Envelope expected, final Envelope actual, final double... tolerances) {
        final int dimension = expected.getDimension();
        assertEquals("dimension", dimension, actual.getDimension());
        double tolerance = 0;
        for (int i=0; i<dimension; i++) {
            if (i < tolerances.length) {
                tolerance = tolerances[i];
            }
            if (abs(expected.getMinimum(i) - actual.getMinimum(i)) > tolerance ||
                abs(expected.getMaximum(i) - actual.getMaximum(i)) > tolerance)
            {
                fail("Envelopes are not equal:\n"
                        + "expected " + Envelopes.toString(expected) + "\n"
                        + " but got " + Envelopes.toString(actual));
            }
        }
    }

    /**
     * Tests if the given {@code outer} shape contains the given {@code inner} rectangle.
     * This method will also verify class consistency by invoking the {@code intersects}
     * method, and by interchanging the arguments.
     *
     * <p>This method can be used for testing the {@code outer} implementation -
     * it should not be needed for standard JDK implementations.</p>
     *
     * @param outer  the shape which is expected to contains the given rectangle.
     * @param inner  the rectangle which should be contained by the shape.
     */
    public static void assertContains(final RectangularShape outer, final Rectangle2D inner) {
        assertTrue("outer.contains(inner)",   outer.contains  (inner));
        assertTrue("outer.intersects(inner)", outer.intersects(inner));
        if (outer instanceof Rectangle2D) {
            assertTrue ("inner.intersects(outer)", inner.intersects((Rectangle2D) outer));
            assertFalse("inner.contains(outer)",   inner.contains  ((Rectangle2D) outer));
        }
        assertTrue("outer.contains(centerX, centerY)",
                outer.contains(inner.getCenterX(), inner.getCenterY()));
    }

    /**
     * Tests if the given {@code outer} envelope contains the given {@code inner} envelope.
     * This method will also verify class consistency by invoking the {@code intersects}
     * method, and by interchanging the arguments.
     *
     * @param outer  the envelope which is expected to contains the given inner envelope.
     * @param inner  the envelope which should be contained by the outer envelope.
     */
    public static void assertContains(final AbstractEnvelope outer, final Envelope inner) {
        assertTrue("outer.contains(inner)",   outer.contains  (inner, true));
        assertTrue("outer.contains(inner)",   outer.contains  (inner, false));
        assertTrue("outer.intersects(inner)", outer.intersects(inner, true));
        assertTrue("outer.intersects(inner)", outer.intersects(inner, false));
        if (inner instanceof AbstractEnvelope) {
            final AbstractEnvelope ai = (AbstractEnvelope) inner;
            assertTrue ("inner.intersects(outer)", ai.intersects(outer, true));
            assertTrue ("inner.intersects(outer)", ai.intersects(outer, false));
            assertFalse("inner.contains(outer)",   ai.contains  (outer, true));
            assertFalse("inner.contains(outer)",   ai.contains  (outer, false));
        }
        final GeneralDirectPosition median = new GeneralDirectPosition(inner.getDimension());
        for (int i=median.getDimension(); --i>=0;) {
            median.setOrdinate(i, inner.getMedian(i));
        }
        assertTrue("outer.contains(median)", outer.contains(median));
    }

    /**
     * Tests if the given {@code r1} shape is disjoint with the given {@code r2} rectangle.
     * This method will also verify class consistency by invoking the {@code contains}
     * method, and by interchanging the arguments.
     *
     * <p>This method can be used for testing the {@code r1} implementation - it should not
     * be needed for standard implementations.</p>
     *
     * @param r1  the first shape to test.
     * @param r2  the second rectangle to test.
     */
    public static void assertDisjoint(final RectangularShape r1, final Rectangle2D r2) {
        assertFalse("r1.intersects(r2)", r1.intersects(r2));
        assertFalse("r1.contains(r2)",   r1.contains(r2));
        if (r1 instanceof Rectangle2D) {
            assertFalse("r2.intersects(r1)", r2.intersects((Rectangle2D) r1));
            assertFalse("r2.contains(r1)",   r2.contains  ((Rectangle2D) r1));
        }
        for (int i=0; i<9; i++) {
            final double x, y;
            switch (i % 3) {
                case 0: x = r2.getMinX();    break;
                case 1: x = r2.getCenterX(); break;
                case 2: x = r2.getMaxX();    break;
                default: throw new AssertionError(i);
            }
            switch (i / 3) {
                case 0: y = r2.getMinY();    break;
                case 1: y = r2.getCenterY(); break;
                case 2: y = r2.getMaxY();    break;
                default: throw new AssertionError(i);
            }
            assertFalse("r1.contains(" + x + ", " + y + ')', r1.contains(x, y));
        }
    }

    /**
     * Tests if the given {@code e1} envelope is disjoint with the given {@code e2} envelope.
     * This method will also verify class consistency by invoking the {@code contains} method,
     * and by interchanging the arguments.
     *
     * @param e1  the first envelope to test.
     * @param e2  the second envelope to test.
     */
    public static void assertDisjoint(final AbstractEnvelope e1, final Envelope e2) {
        assertFalse("e1.intersects(e2)", e1.intersects(e2, false));
        assertFalse("e1.intersects(e2)", e1.intersects(e2, true));
        assertFalse("e1.contains(e2)",   e1.contains  (e2, false));
        assertFalse("e1.contains(e2)",   e1.contains  (e2, true));
        if (e2 instanceof AbstractEnvelope) {
            final AbstractEnvelope ae = (AbstractEnvelope) e2;
            assertFalse("e2.intersects(e1)", ae.intersects(e1, false));
            assertFalse("e2.intersects(e1)", ae.intersects(e1, true));
            assertFalse("e2.contains(e1)",   ae.contains  (e1, false));
            assertFalse("e2.contains(e1)",   ae.contains  (e1, true));
        }
        final int dimension = e1.getDimension();
        final int numCases = JDK8.toIntExact(round(pow(3, dimension)));
        final GeneralDirectPosition pos = new GeneralDirectPosition(dimension);
        for (int index=0; index<numCases; index++) {
            int n = index;
            for (int i=0; i<dimension; i++) {
                final double ordinate;
                switch (n % 3) {
                    case 0: ordinate = e2.getMinimum(i); break;
                    case 1: ordinate = e2.getMedian (i); break;
                    case 2: ordinate = e2.getMaximum(i); break;
                    default: throw new AssertionError(i);
                }
                pos.setOrdinate(i, ordinate);
                n /= 3;
            }
            assertEquals(0, n); // Opportunist check of this assert method.
            assertFalse("e1.contains(" + pos + ')', e1.contains(pos));
        }
    }

    /**
     * Tests if the given transform is the identity transform.
     * If the current transform is linear, then this method will also verifies {@link Matrix#isIdentity()}.
     *
     * @param transform  the transform to test.
     *
     * @since 0.6
     */
    public static void assertIsIdentity(final MathTransform transform) {
        assertTrue("isIdentity()", transform.isIdentity());
        if (transform instanceof LinearTransform) {
            assertTrue("getMatrix().isIdentity()", ((LinearTransform) transform).getMatrix().isIdentity());
        }
    }

    /**
     * Tests if the given transform is <strong>not</strong> the identity transform.
     * If the current transform is linear, then this method will also verifies {@link Matrix#isIdentity()}.
     *
     * @param transform  the transform to test.
     *
     * @since 0.6
     */
    public static void assertIsNotIdentity(final MathTransform transform) {
        assertFalse("isIdentity()", transform.isIdentity());
        if (transform instanceof LinearTransform) {
            assertFalse("getMatrix().isIdentity()", ((LinearTransform) transform).getMatrix().isIdentity());
        }
    }
}
