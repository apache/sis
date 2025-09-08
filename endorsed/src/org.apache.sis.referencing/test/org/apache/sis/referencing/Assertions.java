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
package org.apache.sis.referencing;

import java.util.Locale;
import java.util.Collection;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.AffineTransform;
import static java.lang.StrictMath.*;
import javax.measure.Unit;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.InternationalString;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.io.wkt.Symbols;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.util.privy.Constants;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestUtilities;
import static org.apache.sis.test.Assertions.assertMultilinesEquals;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Assertion methods used by the {@code org.apache.sis.referencing} module in addition of the ones inherited
 * from other modules and libraries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Assertions extends Static {
    /**
     * The formatter to be used by {@link #assertWktEquals(Convention, String, Object)}.
     * This formatter uses the {@code “…”} quotation marks instead of {@code "…"}
     * for easier readability of {@link String} constants in Java code.
     */
    private static final WKTFormat WKT_FORMAT = new WKTFormat();
    static {
        final Symbols s = new Symbols(Symbols.SQUARE_BRACKETS);
        s.setPairedQuotes("“”", "\"\"");
        WKT_FORMAT.setSymbols(s);
    }

    /**
     * Replacements to perform in <abbr>WKT</abbr> strings for compatibility between different versions
     * of the <abbr>EPSG</abbr> geodetic dataset. Values at even indexes are legacy names that may still
     * be present in the tests. Values at odd indexes are the names as they may be formatted when using
     * newer versions of the <abbr>EPSG</abbr> geodetic dataset.
     *
     * <p>We may remove this hack in a future <abbr>SIS</abbr> version if we abandon support of version 9
     * of <abbr>EPSG</abbr> dataset (the current version at the time of writing is 12).</p>
     */
    private static final String[] REPLACEMENTS = {
        "“World Geodetic System 1984”", "“World Geodetic System 1984 ensemble”",
        "“NGF IGN69 height”",           "“NGF-IGN69 height”"
    };

    /**
     * Do not allow instantiation of this class.
     */
    private Assertions() {
    }

    /**
     * Asserts that the two given coordinate references are equivalent.
     * This method is tolerance to differences regarding whether a datum versus datum ensemble.
     *
     * @param  expected  the expected object.
     * @param  actual    the actual object.
     *
     * @see org.apache.sis.test.Assertions#assertEqualsIgnoreMetadata(Object, Object)
     */
    public static void assertEquivalent(final CoordinateReferenceSystem expected, final CoordinateReferenceSystem actual) {
        assertTrue(Utilities.deepEquals(expected, actual, ComparisonMode.DEBUG),       "Shall be approximately equal.");
        assertTrue(Utilities.deepEquals(expected, actual, ComparisonMode.APPROXIMATE), "DEBUG inconsistent with APPROXIMATE.");
        assertTrue(CRS.equivalent(expected, actual), "CRS shall be equivalent.");
    }

    /**
     * Asserts that the given identifier has the expected code and the {@code "OGC"} code space.
     * The authority is expected to be {@link Citations#OGC}. We expect the exact same authority
     * instance because identifiers in OGC namespace are often hard-coded in SIS.
     *
     * @param expected  the expected identifier code.
     * @param actual    the identifier to verify.
     */
    public static void assertOgcIdentifierEquals(final String expected, final ReferenceIdentifier actual) {
        assertNotNull(actual);
        assertEquals(expected,      actual.getCode(), "code");
        assertEquals(Constants.OGC, actual.getCodeSpace(), "codeSpace");
        assertSame  (Citations.OGC, actual.getAuthority(), "authority");
        assertEquals(Constants.OGC + Constants.DEFAULT_SEPARATOR + expected, IdentifiedObjects.toString(actual), "identifier");
    }

    /**
     * Asserts that the given identifier has the expected code and the {@code "EPSG"} code space.
     * The authority is expected to have the {@code "EPSG"} title, alternate title or identifier.
     *
     * @param expected  the expected identifier code.
     * @param actual    the identifier to verify.
     */
    public static void assertEpsgIdentifierEquals(final String expected, final Identifier actual) {
        assertNotNull(actual);
        assertLegacyEquals(expected, actual.getCode(), "code");
        assertEquals(Constants.EPSG,  (actual instanceof ReferenceIdentifier) ? ((ReferenceIdentifier) actual).getCodeSpace() : null, "codeSpace");
        assertEquals(Constants.EPSG,  Citations.toCodeSpace(actual.getAuthority()), "authority");
        assertLegacyEquals(Constants.EPSG + Constants.DEFAULT_SEPARATOR + expected, IdentifiedObjects.toString(actual), "identifier");
    }

    /**
     * Asserts that the given object has the expected name and singleton identifier in the {@code "EPSG"} code space.
     * No other identifier than the given one is expected. The authority is expected to have the {@code "EPSG"} title,
     * alternate title or identifier.
     *
     * @param name        the expected EPSG name.
     * @param identifier  the expected EPSG identifier.
     * @param object      the object to verify.
     */
    public static void assertEpsgNameAndIdentifierEqual(final String name, final int identifier, final IdentifiedObject object) {
        assertNotNull(object, name);
        assertEpsgIdentifierEquals(name, object.getName());
        assertEpsgIdentifierEquals(String.valueOf(identifier), TestUtilities.getSingleton(object.getIdentifiers()));
    }

    /**
     * Asserts that the tip of the unique alias of the given object is equal to the expected value.
     * As a special case if the expected value is null, then this method verifies that the given object has no alias.
     *
     * @param expected  the expected alias, or {@code null} if we expect no alias.
     * @param object    the object for which to test the alias.
     */
    public static void assertAliasTipEquals(final String expected, final IdentifiedObject object) {
        final Collection<GenericName> aliases = object.getAlias();
        if (expected == null) {
            assertTrue(aliases.isEmpty(), "aliases.isEmpty()");
        } else {
            assertEquals(expected, TestUtilities.getSingleton(aliases).tip().toString(), "alias");
        }
    }

    /**
     * Asserts that the remarks of the given object are equal to the expected value.
     *
     * @param expected  the expected remarks, or {@code null}.
     * @param object    the object for which to test the remarks.
     * @param locale    the locale to test, or {@code null}.
     */
    public static void assertRemarksEquals(final String expected, final IdentifiedObject object, final Locale locale) {
        InternationalString i18n = object.getRemarks();
        String remarks = (i18n == null) ? null : (locale != null) ? i18n.toString(locale) : i18n.toString();
        assertEquals(expected, remarks, "remarks");
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
        assertEquals(name,         axis.getName().getCode(), "name");
        assertEquals(abbreviation, axis.getAbbreviation(),   "abbreviation");
        assertEquals(direction,    axis.getDirection(),      "direction");
        assertEquals(minimumValue, axis.getMinimumValue(),   "minimumValue");
        assertEquals(maximumValue, axis.getMaximumValue(),   "maximumValue");
        assertEquals(unit,         axis.getUnit(),           "unit");
        assertEquals(rangeMeaning, axis.getRangeMeaning(),   "rangeMeaning");
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
                assertEquals(f, a.doubleValue(unit), tolerance, name);
            } else if (valueClass == Float.class || valueClass == Double.class) {
                final double f = e.doubleValue();
                assertEquals(f, a.doubleValue(), tolerance, name);
            } else {
                assertEquals(e.getValue(), a.getValue(), name);
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
                final int ti=i, tj=j;       // Because lambda requires final values.
                assertEquals(e, matrix.getElement(j, i), tolerance, () -> "matrix(" + tj + ", " + ti + ")");
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
        assertEquals(expected.getScaleX(),     actual.getScaleX(),     tolerance, "scaleX");
        assertEquals(expected.getScaleY(),     actual.getScaleY(),     tolerance, "scaleY");
        assertEquals(expected.getShearX(),     actual.getShearX(),     tolerance, "shearX");
        assertEquals(expected.getShearY(),     actual.getShearY(),     tolerance, "shearY");
        assertEquals(expected.getTranslateX(), actual.getTranslateX(), tolerance, "translateX");
        assertEquals(expected.getTranslateY(), actual.getTranslateY(), tolerance, "translateY");
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
        assertEquals(expected.getMinX(),    actual.getMinX(),    tolx,   "Min X");
        assertEquals(expected.getMinY(),    actual.getMinY(),    toly,   "Min Y");
        assertEquals(expected.getMaxX(),    actual.getMaxX(),    tolx,   "Max X");
        assertEquals(expected.getMaxY(),    actual.getMaxY(),    toly,   "Max Y");
        assertEquals(expected.getCenterX(), actual.getCenterX(), tolx,   "Center X");
        assertEquals(expected.getCenterY(), actual.getCenterY(), toly,   "Center Y");
        assertEquals(expected.getWidth(),   actual.getWidth(),   tolx*2, "Width");
        assertEquals(expected.getHeight(),  actual.getHeight(),  toly*2, "Height");
    }

    /**
     * Asserts that two envelopes have the same minimum and maximum coordinates.
     * This method ignores the envelope type (i.e. the implementation class) and the CRS.
     *
     * @param expected    the expected envelope.
     * @param actual      the envelope to compare with the expected one.
     * @param tolerances  the tolerance threshold on location along each axis. If this array length is shorter
     *                    than the number of dimensions, then the last tolerance is reused for all remaining axes.
     *                    If this array is empty, then the tolerance threshold is zero.
     */
    public static void assertEnvelopeEquals(final Envelope expected, final Envelope actual, final double... tolerances) {
        final int dimension = expected.getDimension();
        assertEquals(dimension, actual.getDimension(), "dimension");
        final DirectPosition expectedLower = expected.getLowerCorner();
        final DirectPosition expectedUpper = expected.getUpperCorner();
        final DirectPosition actualLower   = actual  .getLowerCorner();
        final DirectPosition actualUpper   = actual  .getUpperCorner();
        double tolerance = 0;
        for (int i=0; i<dimension; i++) {
            if (i < tolerances.length) {
                tolerance = tolerances[i];
            }
            if (abs(expectedLower.getOrdinate(i) - actualLower.getOrdinate(i)) > tolerance ||
                abs(expectedUpper.getOrdinate(i) - actualUpper.getOrdinate(i)) > tolerance)
            {
                fail("Envelopes are not equal in dimension " + i + ":\n"
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
        assertTrue(outer.contains  (inner), "outer.contains(inner)");
        assertTrue(outer.intersects(inner), "outer.intersects(inner)");
        if (outer instanceof Rectangle2D r) {
            assertTrue (inner.intersects(r), "inner.intersects(outer)");
            assertFalse(inner.contains  (r), "inner.contains(outer)");
        }
        assertTrue(outer.contains(inner.getCenterX(), inner.getCenterY()), "outer.contains(centerX, centerY)");
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
        assertTrue(outer.contains  (inner, true),  "outer.contains(inner)");
        assertTrue(outer.contains  (inner, false), "outer.contains(inner)");
        assertTrue(outer.intersects(inner, true),  "outer.intersects(inner)");
        assertTrue(outer.intersects(inner, false), "outer.intersects(inner)");
        if (inner instanceof AbstractEnvelope ai) {
            assertTrue (ai.intersects(outer, true),  "inner.intersects(outer)");
            assertTrue (ai.intersects(outer, false), "inner.intersects(outer)");
            assertFalse(ai.contains  (outer, true),  "inner.contains(outer)");
            assertFalse(ai.contains  (outer, false), "inner.contains(outer)");
        }
        final GeneralDirectPosition median = new GeneralDirectPosition(inner.getDimension());
        for (int i=median.getDimension(); --i>=0;) {
            median.setCoordinate(i, inner.getMedian(i));
        }
        assertTrue(outer.contains(median), "outer.contains(median)");
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
        assertFalse(r1.intersects(r2), "r1.intersects(r2)");
        assertFalse(r1.contains(r2), "r1.contains(r2)");
        if (r1 instanceof Rectangle2D r) {
            assertFalse(r2.intersects(r), "r2.intersects(r1)");
            assertFalse(r2.contains  (r), "r2.contains(r1)");
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
            assertFalse(r1.contains(x, y), () -> "r1.contains(" + x + ", " + y + ')');
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
        assertFalse(e1.intersects(e2, false), "e1.intersects(e2)");
        assertFalse(e1.intersects(e2, true),  "e1.intersects(e2)");
        assertFalse(e1.contains  (e2, false), "e1.contains(e2)");
        assertFalse(e1.contains  (e2, true),  "e1.contains(e2)");
        if (e2 instanceof AbstractEnvelope ae) {
            assertFalse(ae.intersects(e1, false), "e2.intersects(e1)");
            assertFalse(ae.intersects(e1, true),  "e2.intersects(e1)");
            assertFalse(ae.contains  (e1, false), "e2.contains(e1)");
            assertFalse(ae.contains  (e1, true),  "e2.contains(e1)");
        }
        final int dimension = e1.getDimension();
        final int numCases = toIntExact(round(pow(3, dimension)));
        final GeneralDirectPosition pos = new GeneralDirectPosition(dimension);
        for (int index=0; index<numCases; index++) {
            int n = index;
            for (int i=0; i<dimension; i++) {
                final double coordinate;
                switch (n % 3) {
                    case 0: coordinate = e2.getMinimum(i); break;
                    case 1: coordinate = e2.getMedian (i); break;
                    case 2: coordinate = e2.getMaximum(i); break;
                    default: throw new AssertionError(i);
                }
                pos.setCoordinate(i, coordinate);
                n /= 3;
            }
            assertEquals(0, n); // Opportunist check of this assert method.
            assertFalse(e1.contains(pos), () -> "e1.contains(" + pos + ')');
        }
    }

    /**
     * Tests if the given transform is the identity transform.
     * If the current transform is linear, then this method will also verifies {@link Matrix#isIdentity()}.
     *
     * @param transform  the transform to test.
     */
    public static void assertIsIdentity(final MathTransform transform) {
        assertTrue(transform.isIdentity(), "isIdentity()");
        if (transform instanceof LinearTransform linear) {
            assertTrue(linear.getMatrix().isIdentity(), "getMatrix().isIdentity()");
        }
    }

    /**
     * Tests if the given transform is <strong>not</strong> the identity transform.
     * If the current transform is linear, then this method will also verifies {@link Matrix#isIdentity()}.
     *
     * @param transform  the transform to test.
     */
    public static void assertIsNotIdentity(final MathTransform transform) {
        assertFalse(transform.isIdentity(), "isIdentity()");
        if (transform instanceof LinearTransform linear) {
            assertFalse(linear.getMatrix().isIdentity(), "getMatrix().isIdentity()");
        }
    }

    /**
     * Asserts that the given string is equal to the expected string,
     * with a tolerance for name changes in <abbr>EPSG</abbr> database.
     * If the expected string is a old name while the actual string is a new name,
     * then they are considered equal.
     *
     * <p>We may remove this hack in a future <abbr>SIS</abbr> version if we abandon support of version 9
     * of <abbr>EPSG</abbr> dataset (the current version at the time of writing is 12). If this method is
     * removed, it would be replaced by an ordinary {@code assertEquals}.</p>
     *
     * @param expected  the expected string.
     * @param actual    the actual string.
     */
    public static void assertLegacyEquals(final String expected, final String actual) {
        assertLegacyEquals(expected, actual, null);
    }

    static void assertLegacyEquals(String expected, final String actual, final String message) {
        if (expected != null && actual != null) {
            for (int i=0; i < REPLACEMENTS.length;) {
                final String oldName = REPLACEMENTS[i++];
                final String newName = REPLACEMENTS[i++];
                final int ol = oldName.length() - 2;    // Omit quotes.
                final int nl = newName.length() - 2;
                final int s  = expected.length() - ol;
                if (expected.regionMatches(s, oldName, 1, ol) &&
                        actual.regionMatches(actual.length() - nl, newName, 1, nl))
                {
                    expected = expected.substring(0, s) + newName.substring(1, nl + 1);
                }
            }
            if (expected.replace("GeodeticDatum", "DatumEnsemble").equals(actual)) {
                return;
            }
        }
        if (message != null) {
            assertEquals(expected, actual, message);
        } else {
            assertEquals(expected, actual);
        }
    }

    /**
     * Asserts that the WKT of the given object according the given convention is equal to the expected one.
     * This method expected the {@code “…”} quotation marks instead of {@code "…"} for easier readability of
     * {@link String} constants in Java code.
     *
     * @param convention  the WKT convention to use.
     * @param expected    the expected text, or {@code null} if {@code object} is expected to be null.
     * @param object      the object to format in <i>Well Known Text</i> format, or {@code null}.
     */
    public static void assertWktEquals(final Convention convention, String expected, final Object object) {
        if (expected == null) {
            assertNull(object);
        } else {
            assertNotNull(object);
            final String wkt;
            synchronized (WKT_FORMAT) {
                WKT_FORMAT.setConvention(convention);
                wkt = WKT_FORMAT.format(object);
            }
            for (int i=0; i < REPLACEMENTS.length;) {
                final String oldName = REPLACEMENTS[i++];
                final String newName = REPLACEMENTS[i++];
                if (expected.contains(oldName) && wkt.contains(newName)) {
                    expected = expected.replace(oldName, newName);
                }
            }
            assertMultilinesEquals(expected, wkt, (object instanceof IdentifiedObject) ?
                    ((IdentifiedObject) object).getName().getCode() : object.getClass().getSimpleName());
        }
    }

    /**
     * Asserts that the WKT of the given object according the given convention is equal to the given regular expression.
     * This method is like {@link #assertWktEquals(Convention, String, Object)}, but the use of regular expression allows
     * some tolerance for example on numerical parameter values that may be subject to a limited form of rounding errors.
     *
     * @param convention  the WKT convention to use.
     * @param expected    the expected regular expression, or {@code null} if {@code object} is expected to be null.
     * @param object      the object to format in <i>Well Known Text</i> format, or {@code null}.
     */
    public static void assertWktEqualsRegex(final Convention convention, final String expected, final Object object) {
        if (expected == null) {
            assertNull(object);
        } else {
            assertNotNull(object);
            final String wkt;
            synchronized (WKT_FORMAT) {
                WKT_FORMAT.setConvention(convention);
                wkt = WKT_FORMAT.format(object);
            }
            if (!wkt.matches(expected.replace("\n", System.lineSeparator()))) {
                fail("WKT does not match the expected regular expression. The WKT that we got is:\n" + wkt);
            }
        }
    }
}
