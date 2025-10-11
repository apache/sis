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
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.util.InternationalString;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Assert;


/**
 * Temporary class for test methods that are expected to be provided in next GeoAPI release.
 * Those methods are defined in a separated class in order to make easier for us to identify
 * which methods may be removed from SIS (actually moved to GeoAPI) in a future GeoAPI release.
 *
 * <p>This class is needed for Apache SIS main branch, since the later is linked to GeoAPI official release.
 * But this class can be removed on Apache SIS branches which are linked to a GeoAPI development branch.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GeoapiAssert {
    /**
     * A flag for code that are pending next GeoAPI release before to be enabled.
     * This flag is always set to {@code false}, except occasionally just before
     * a GeoAPI release for testing purpose. It shall be used as below:
     *
     * {@snippet lang="java" :
     *     if (PENDING_NEXT_GEOAPI_RELEASE) {
     *         // Do some stuff here.
     *     }
     *     }
     *
     * The intend is to make easier to identify test cases that fail with the current version
     * of the {@code geoapi-conformance} module, but should pass with the development snapshot.
     */
    public static final boolean PENDING_NEXT_GEOAPI_RELEASE = false;

    /**
     * The keyword for unrestricted value in {@link String} arguments.
     */
    private static final String UNRESTRICTED = "##unrestricted";

    /**
     * Do not allow instantiation of this class.
     */
    private GeoapiAssert() {
    }

    private static String nonNull(final String message) {
        return (message != null) ? message.trim().concat(" ") : "";
    }

    /**
     * Returns the concatenation of the given message with the given extension.
     * This method returns the given extension if the message is null or empty.
     *
     * <p>Invoking this method is equivalent to invoking {@code nonNull(message) + ext},
     * but avoid the creation of temporary objects in the common case where the message
     * is null.</p>
     *
     * @param  message  the message, or {@code null}.
     * @param  ext      the extension to append after the message.
     * @return the concatenated string.
     */
    private static String concat(String message, final String ext) {
        if (message == null || (message = message.trim()).isEmpty()) {
            return ext;
        }
        return message + ' ' + ext;
    }

    /**
     * Verifies if we expected a null value, then returns {@code true} if the value is null as expected.
     */
    private static boolean isNull(final Object expected, final Object actual, final String message) {
        final boolean isNull = (actual == null);
        if (isNull != (expected == null)) {
            fail(concat(message, isNull ? "Value is null." : "Expected null."));
        }
        return isNull;
    }

    public static void assertPositive(final int value, final String message) {
        Assert.assertPositive(message, value);
    }

    public static void assertStrictlyPositive(final int value, final String message) {
        Assert.assertStrictlyPositive(message, value);
    }

    public static <T> void assertValidRange(final Comparable<T> minimum, final Comparable<T> maximum, final String message) {
        Assert.assertValidRange(message, minimum, maximum);
    }

    public static void assertValidRange(final int minimum, final int maximum, final String message) {
        Assert.assertValidRange(message, minimum, maximum);
    }

    public static void assertValidRange(final double minimum, final double maximum, final String message) {
        Assert.assertValidRange(message, minimum, maximum);
    }

    public static <T> void assertBetween(final Comparable<T> minimum, final Comparable<T> maximum, T value, final String message) {
        Assert.assertBetween(message, minimum, maximum, value);
    }

    public static void assertBetween(final int minimum, final int maximum, final int value, final String message) {
        Assert.assertBetween(message, minimum, maximum, value);
    }

    public static void assertBetween(final double minimum, final double maximum, final double value, final String message) {
        Assert.assertBetween(message, minimum, maximum, value);
    }

    public static void assertContains(final Collection<?> collection, final Object value, final String message) {
        Assert.assertContains(message, collection, value);
    }

    /**
     * Asserts that the title or an alternate title of the given citation is equal to the given string.
     * This method is typically used for testing if a citation stands for the OGC, OGP or EPSG authority
     * for instance. Such abbreviations are often declared as {@linkplain Citation#getAlternateTitles()
     * alternate titles} rather than the main {@linkplain Citation#getTitle() title}, but this method
     * tests both for safety.
     *
     * @param expected  the expected title or alternate title.
     * @param actual    the citation to test.
     * @param message   header of the exception message in case of failure, or {@code null} if none.
     */
    public static void assertAnyTitleEquals(final String expected, final Citation actual, final String message) {
        if (isNull(expected, actual, message)) {
            return;
        }
        InternationalString title = actual.getTitle();
        if (title != null && expected.equals(title.toString())) {
            return;
        }
        for (final InternationalString t : actual.getAlternateTitles()) {
            if (expected.equals(t.toString())) {
                return;
            }
        }
        fail(concat(message, '"' + expected + "\" not found in title or alternate titles."));
    }

    /**
     * Asserts that the given identifier is equal to the given authority, code space, version and code.
     * If any of the above-cited properties is {@code ""##unrestricted"}, then it will not be verified.
     * This flexibility is useful in the common case where a test accepts any {@code version} value.
     *
     * @param authority  the expected authority title or alternate title (may be {@code null}), or {@code "##unrestricted"}.
     * @param codeSpace  the expected code space (may be {@code null}), or {@code "##unrestricted"}.
     * @param version    the expected version    (may be {@code null}), or {@code "##unrestricted"}.
     * @param code       the expected code value (may be {@code null}), or {@code "##unrestricted"}.
     * @param actual     the identifier to test.
     * @param message    header of the exception message in case of failure, or {@code null} if none.
     */
    public static void assertIdentifierEquals(final String authority, final String codeSpace, final String version,
            final String code, final ReferenceIdentifier actual, final String message)
    {
        if (actual == null) {
            fail(concat(message, "Identifier is null"));
        } else {
            if (!UNRESTRICTED.equals(authority)) assertAnyTitleEquals(authority, actual.getAuthority(), message);
            if (!UNRESTRICTED.equals(codeSpace)) assertEquals(codeSpace, actual.getCodeSpace(), () -> concat(message, "Wrong code space"));
            if (!UNRESTRICTED.equals(version))   assertEquals(version,   actual.getVersion(),   () -> concat(message, "Wrong version"));
            if (!UNRESTRICTED.equals(code)) assertEquals(code, actual.getCode(), () -> concat(message, "Wrong code"));
        }
    }

    /**
     * Asserts that all axes in the given coordinate system are pointing toward the given directions, in the same order.
     *
     * @param cs        the coordinate system to test.
     * @param expected  the expected axis directions.
     */
    public static void assertAxisDirectionsEqual(final CoordinateSystem cs, final AxisDirection... expected) {
        assertAxisDirectionsEqual(cs, expected, null);
    }

    /**
     * Asserts that all axes in the given coordinate system are pointing toward the given directions,
     * in the same order.
     *
     * @param cs        the coordinate system to test.
     * @param expected  the expected axis directions.
     * @param message   header of the exception message in case of failure, or {@code null} if none.
     */
    public static void assertAxisDirectionsEqual(final CoordinateSystem cs, final AxisDirection[] expected, final String message) {
        assertEquals(expected.length, cs.getDimension(), () -> concat(message, "Wrong coordinate system dimension."));
        for (int i=0; i<expected.length; i++) {
            final int ci = i;   // Because lambda expressions require final values.
            assertEquals(expected[i], cs.getAxis(i).getDirection(),
                    () -> concat(message, "Wrong axis direction at index" + ci + '.'));
        }
    }

    /**
     * Asserts that the given matrix is equal to the expected one, with a tolerance of zero.
     * Positive zeros are considered equal to negative zeros, and any NaN value is considered equal
     * to all other NaN values.
     *
     * @param expected  the expected matrix, which may be {@code null}.
     * @param actual    the matrix to compare, or {@code null}.
     * @param label     header of the exception message in case of failure, or {@code null} if none.
     */
    public static void assertMatrixEquals(final Matrix expected, final Matrix actual, final String label) {
        assertMatrixEquals(expected, actual, 0, label);
    }

    /**
     * Asserts that the given matrix is equal to the expected one, up to the given tolerance value.
     *
     * @param expected   the expected matrix, which may be {@code null}.
     * @param actual     the matrix to compare, or {@code null}.
     * @param tolerance  the tolerance threshold.
     * @param message    header of the exception message in case of failure, or {@code null} if none.
     *
     * @see org.opengis.test.referencing.TransformTestCase#assertMatrixEquals(String, Matrix, Matrix, Matrix)
     */
    public static void assertMatrixEquals(final Matrix expected, final Matrix actual, final double tolerance, final String message) {
        if (isNull(expected, actual, message)) {
            return;
        }
        final int numRow = actual.getNumRow();
        final int numCol = actual.getNumCol();
        assertEquals(expected.getNumRow(), numRow, "numRow");
        assertEquals(expected.getNumCol(), numCol, "numCol");
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double e = expected.getElement(j,i);
                final double a = actual.getElement(j,i);
                if (!(StrictMath.abs(e - a) <= tolerance) && Double.doubleToLongBits(a) != Double.doubleToLongBits(e)) {
                    fail(nonNull(message) + "Matrix.getElement(" + j + ", " + i + "): expected " + e + " but got " + a);
                }
            }
        }
    }
}
