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

import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.util.InternationalString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Temporary class for test methods that are expected to be provided in next GeoAPI release.
 * Those methods are defined in a separated class in order to make easier for us to identify
 * which methods may be removed from SIS (actually moved to GeoAPI) in a future GeoAPI release.
 *
 * <p>This class is needed for Apache SIS trunk, since the later is linked to GeoAPI official release.
 * But this class can be removed on Apache SIS branches which are linked to a GeoAPI development branch.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
strictfp class GeoapiAssert extends org.opengis.test.Assert {
    /**
     * A flag for code that are pending next GeoAPI release before to be enabled.
     * This flag is always set to {@code false}, except occasionally just before
     * a GeoAPI release for testing purpose. It shall be used as below:
     *
     * {@preformat java
     *     if (PENDING_NEXT_GEOAPI_RELEASE) {
     *         // Do some stuff here.
     *     }
     * }
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
     * For subclass constructor only.
     */
    GeoapiAssert() {
    }

    /**
     * Returns the concatenation of the given message with the given extension.
     * This method returns the given extension if the message is null or empty.
     *
     * <p>Invoking this method is equivalent to invoking {@code nonNull(message) + ext},
     * but avoid the creation of temporary objects in the common case where the message
     * is null.</p>
     *
     * @param  message The message, or {@code null}.
     * @param  ext The extension to append after the message.
     * @return The concatenated string.
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
    private static boolean isNull(final String message, final Object expected, final Object actual) {
        final boolean isNull = (actual == null);
        if (isNull != (expected == null)) {
            fail(concat(message, isNull ? "Value is null." : "Expected null."));
        }
        return isNull;
    }

    /**
     * Asserts that the title or an alternate title of the given citation is equal to the given string.
     * This method is typically used for testing if a citation stands for the OGC, OGP or EPSG authority
     * for instance. Such abbreviations are often declared as {@linkplain Citation#getAlternateTitles()
     * alternate titles} rather than the main {@linkplain Citation#getTitle() title}, but this method
     * tests both for safety.
     *
     * @param message  Header of the exception message in case of failure, or {@code null} if none.
     * @param expected The expected title or alternate title.
     * @param actual   The citation to test.
     */
    public static void assertAnyTitleEquals(final String message, final String expected, final Citation actual) {
        if (isNull(message, expected, actual)) {
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
     * Asserts that the given identifier is equals to the given authority, code space, version and code.
     * If any of the above-cited properties is {@code ""##unrestricted"}, then it will not be verified.
     * This flexibility is useful in the common case where a test accepts any {@code version} value.
     *
     * @param message    Header of the exception message in case of failure, or {@code null} if none.
     * @param authority  The expected authority title or alternate title (may be {@code null}), or {@code "##unrestricted"}.
     * @param codeSpace  The expected code space (may be {@code null}), or {@code "##unrestricted"}.
     * @param version    The expected version    (may be {@code null}), or {@code "##unrestricted"}.
     * @param code       The expected code value (may be {@code null}), or {@code "##unrestricted"}.
     * @param actual     The identifier to test.
     */
    public static void assertIdentifierEquals(final String message, final String authority, final String codeSpace,
            final String version, final String code, final ReferenceIdentifier actual)
    {
        if (actual == null) {
            fail(concat(message, "Identifier is null"));
        } else {
            if (!UNRESTRICTED.equals(authority)) assertAnyTitleEquals(message,                      authority, actual.getAuthority());
            if (!UNRESTRICTED.equals(codeSpace)) assertEquals (concat(message, "Wrong code space"), codeSpace, actual.getCodeSpace());
            if (!UNRESTRICTED.equals(version))   assertEquals (concat(message, "Wrong version"),    version,   actual.getVersion());
            if (!UNRESTRICTED.equals(code))      assertEquals (concat(message, "Wrong code"),       code,      actual.getCode());
        }
    }

    /**
     * Asserts that all axes in the given coordinate system are pointing toward the given directions, in the same order.
     *
     * @param message  Header of the exception message in case of failure, or {@code null} if none.
     * @param cs       The coordinate system to test.
     * @param expected The expected axis directions.
     */
    public static void assertAxisDirectionsEqual(String message,
            final CoordinateSystem cs, final AxisDirection... expected)
    {
        assertEquals(concat(message, "Wrong coordinate system dimension."), expected.length, cs.getDimension());
        message = concat(message, "Wrong axis direction.");
        for (int i=0; i<expected.length; i++) {
            assertEquals(message, expected[i], cs.getAxis(i).getDirection());
        }
    }

    /**
     * Asserts that the given matrix is equals to the expected one, up to the given tolerance value.
     *
     * @param message   Header of the exception message in case of failure, or {@code null} if none.
     * @param expected  The expected matrix, which may be {@code null}.
     * @param actual    The matrix to compare, or {@code null}.
     * @param tolerance The tolerance threshold.
     */
    public static void assertMatrixEquals(final String message, final Matrix expected, final Matrix actual, final double tolerance) {
        if (isNull(message, expected, actual)) {
            return;
        }
        final int numRow = actual.getNumRow();
        final int numCol = actual.getNumCol();
        assertEquals("numRow", expected.getNumRow(), numRow);
        assertEquals("numCol", expected.getNumCol(), numCol);
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double e = expected.getElement(j,i);
                final double a = actual.getElement(j,i);
                if (!(StrictMath.abs(e - a) <= tolerance) && Double.doubleToLongBits(a) != Double.doubleToLongBits(e)) {
                    fail("Matrix.getElement(" + j + ", " + i + "): expected " + e + " but got " + a);
                }
            }
        }
    }
}
