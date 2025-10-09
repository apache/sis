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
package org.apache.sis.io.wkt;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.util.collection.BackingStoreException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import static org.apache.sis.test.Assertions.assertSetEquals;
import static org.apache.sis.test.Assertions.assertMessageContains;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;
import static org.opengis.test.Assertions.assertAxisDirectionsEqual;


/**
 * Tests {@link WKTDictionary}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class WKTDictionaryTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public WKTDictionaryTest() {
    }

    /**
     * Tests {@link WKTDictionary#addDefinitions(Stream)}. The CRS used in this test are a subset of the
     * ones used by {@link #testLoad()}. One of them is intentionally malformed for testing error index.
     *
     * @throws FactoryException if an error occurred while parsing a WKT.
     */
    @Test
    public void testAddDefinitions() throws FactoryException {
        final var factory = new WKTDictionary(null);
        factory.addDefinitions(List.of(
                "GeodCRS[\"Anguilla 1957\",\n" +
                " Datum[\"Anguilla 1957\",\n" +
                "  Ellipsoid[\"Clarke 1880\", 6378249.145, 293.465]],\n" +
                " CS[ellipsoidal, 2],\n" +
                "  Axis[\"Latitude\", north],\n" +
                "  Axis[\"Longitude\", east],\n" +
                "  Unit[\"Degree\", 0.0174532925199433],\n" +
                " Id[\"TEST\", 21]]",

                "GeodCRS[\"Error index 69 (on Ellipsoid)\", Datum[\"Erroneous\", Ellipsoid[\"Missing axis length\"]],\n" +
                " CS[ellipsoidal, 2],\n" +
                "  Axis[\"Latitude\", north],\n" +
                "  Axis[\"Longitude\", east],\n" +
                "  Unit[\"Degree\", 0.0174532925199433],\n" +
                " Id[\"TEST\", \"E1\"]]").stream());
        /*
         * Codes can be in any order. Code spaces are omitted when there is no ambiguity.
         */
        assertArrayEquals(new String[] {"TEST"}, factory.getCodeSpaces().toArray(), "getCodeSpaces()");
        assertEquals("TEST", factory.getAuthority().getTitle().toString(), "getAuthority()");
        try {
            Set<String> codes = factory.getAuthorityCodes(IdentifiedObject.class);
            assertSame( codes,  factory.getAuthorityCodes(SingleCRS.class));
            assertSame( codes,  factory.getAuthorityCodes(GeodeticCRS.class));
            assertSame( codes,  factory.getAuthorityCodes(GeographicCRS.class));
            assertEquals(0, factory.getAuthorityCodes(ProjectedCRS.class).size());
            assertSetEquals(List.of("21", "E1"), codes);
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        /*
         * Tests CRS creation, potentially with expected error.
         */
        verifyCRS(factory.createGeographicCRS("21"), "Anguilla 1957");
        verifyErroneousCRS(factory, "E1", 69);
        /*
         * Test non-existing CRS.
         */
        var exception = assertThrows(NoSuchAuthorityCodeException.class, () -> factory.createGeographicCRS("84"));
        assertMessageContains(exception, "84");
    }

    /**
     * Tests {@link WKTDictionary#load(BufferedReader)}.
     *
     * @throws IOException if an error occurred while reading the test file.
     * @throws FactoryException if an error occurred while parsing a WKT.
     */
    @Test
    public void testLoad() throws IOException, FactoryException {
        final var factory = new WKTDictionary(null);
        try (BufferedReader source = new BufferedReader(new InputStreamReader(
                WKTFormatTest.class.getResourceAsStream("ExtraCRS.txt"), "UTF-8")))
        {
            factory.load(source);
        }
        /*
         * The `load(…)` method should detect duplicated WKT elements and use references
         * to unique instances of nodes such as "AngleUnit["Degree", 0.0174532925199433]]".
         * Following line verifies if the trees of WKT elements indeed share common nodes.
         */
        new SharedValuesCheck().verify(factory);
        /*
         * "TEST" code space should be first because it is the most frequently used
         * in the test file. The authority should be "TEST" for the same reason.
         * Codes can be in any order. Code spaces are omitted when there is no ambiguity.
         */
        assertArrayEquals(new String[] {"TEST", "ESRI"}, factory.getCodeSpaces().toArray(), "getCodeSpaces()");
        assertEquals("TEST", factory.getAuthority().getTitle().toString(), "getAuthority()");
        try {
            Set<String> codes = factory.getAuthorityCodes(IdentifiedObject.class);
            assertSame( codes,  factory.getAuthorityCodes(IdentifiedObject.class));     // Test caching.
            assertSame( codes,  factory.getAuthorityCodes(SingleCRS.class));            // Test sharing.
            assertSetEquals(List.of("102018", "ESRI::102021", "TEST::102021", "TEST:v2:102021", "E1", "E2"), codes);
            assertSetEquals(List.of("102018", "ESRI::102021"), factory.getAuthorityCodes(ProjectedCRS.class));
            codes = factory.getAuthorityCodes(GeographicCRS.class);
            assertSetEquals(List.of("TEST::102021", "TEST:v2:102021", "E1", "E2"), codes);
            assertSame(codes, factory.getAuthorityCodes(GeodeticCRS.class));            // Test sharing.
            assertSame(codes, factory.getAuthorityCodes(GeographicCRS.class));          // Test caching.
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        /*
         * Test descriptions before CRS creation.
         * Implementation fetches them from `StoredTree` instances.
         */
        assertDescriptionEquals("North_Pole_Stereographic", factory, "ESRI::102018");
        assertDescriptionEquals("South_Pole_Stereographic", factory, "ESRI::102021");
        /*
         * Tests CRS creation.
         */
        verifyCRS(factory.createProjectedCRS (        "102018"), "North_Pole_Stereographic", +90);
        verifyCRS(factory.createProjectedCRS ("ESRI :  102021"), "South_Pole_Stereographic", -90);
        verifyCRS(factory.createGeographicCRS("TEST:  :102021"), "Anguilla 1957");
        verifyCRS(factory.createGeographicCRS("TEST:v2:102021"), "Anguilla 1957 (bis)");
        /*
         * Test descriptions after CRS creation.
         * Implementation fetches them from `IdentifiedObject` instances.
         */
        assertDescriptionEquals("North_Pole_Stereographic", factory, "ESRI::102018");
        assertDescriptionEquals("South_Pole_Stereographic", factory, "ESRI::102021");
        /*
         * Test creation of CRS having errors.
         *   - Verify error index.
         */
        verifyErroneousCRS(factory, "E1", 69);
        verifyErroneousCRS(factory, "E2", 42);
    }

    /**
     * Asserts that the description is equal to the expected value.
     *
     * @param expected  the expected description.
     * @param factory   the factory to use for fetching the description.
     * @param code      the code of the object for which to fetch the description.
     */
    private static void assertDescriptionEquals(String expected, WKTDictionary factory, String code) throws FactoryException {
        assertEquals(expected, factory.getDescriptionText(IdentifiedObject.class, code).orElseThrow().toString());
    }

    /**
     * Verifies that there is no duplicated nodes in the {@link StoredTree}s.
     * When a WKT element is repeated often (e.g. "AngleUnit["Degree", 0.0174532925199433]]"),
     * only one {@link org.apache.sis.io.wkt.StoredTree.Node} instance should be created and shared by all trees.
     */
    @SuppressWarnings("overloads")      // Ambiguous `andThen(…)` method is not used by this test.
    private static final class SharedValuesCheck implements Consumer<Object>, BiFunction<Integer,Integer,Integer> {
        /**
         * Counter of number of occurrences of each instance. Keys may be {@link String},
         * {@link Long}, {@link Double} or {@code StoredTree.Node} instances among others.
         * Values are number of occurrences.
         */
        private final Map<Object,Integer> counts = new IdentityHashMap<>(90);

        /**
         * Verifies all trees in the given factory.
         */
        final void verify(final WKTDictionary factory) {
            factory.forEachValue(this);
            assertEquals(new HashSet<>(counts.keySet()).size(), counts.size(),
                    "Some values are equal but distinct instances. A single instance should be shared.");
            /*
             * Verify the number of occurrences of a few values. Note that the same string representation of keys
             * value may appear twice: once because the value was already a `String`, and once because the value
             * was a `StoredTree.Node` with the same string representation.
             *
             * The `expected` values below are empirical values and may need to be updated if the content of
             * `ExtraCRS.txt` test file is modified.
             */
            for (final Map.Entry<Object,Integer> entry : counts.entrySet()) {
                final String key = entry.getKey().toString();
                final int expected;
                switch (key) {
                    case "Cartesian": expected = 2; break;
                    case "north":     expected = 6; break;
                    case "Degree":    expected = 6; break;
                    case "Latitude of natural origin": {
                        /*
                         * There is 2 parameters with that string value, but those two parameters are
                         * distinct instances because they have different parameter values (90° and -90°).
                         */
                        expected = (entry.getKey() instanceof String) ? 2 : 1;
                        break;
                    }
                    default: continue;
                }
                assertEquals(expected, entry.getValue().intValue(), key);
            }
        }

        /**
         * Invoked for each value in a WKT element. This method counts the number of occurrences of each
         * distinct instance, separated by identity comparison (not by {@link Object#equals(Object)}).
         */
        @Override public void accept(final Object value) {
            if (value instanceof StoredTree tree) {
                tree.forEachValue(this);
            }
            counts.merge(value, 1, this);
        }

        /** Invoked for incrementing a value in the {@link #counts} map. */
        @Override public Integer apply(final Integer oldValue, final Integer value) {
            return oldValue + value;
        }
    }

    /**
     * Verifies a projected CRS.
     *
     * @param  crs   the CRS to verify.
     * @param  name  expected CRS name.
     * @param  φ0    expected latitude of origin.
     */
    private static void verifyCRS(final ProjectedCRS crs, final String name, final double φ0) {
        assertEquals(name, crs.getName().getCode(), "name");
        assertAxisDirectionsEqual(crs.getCoordinateSystem(),
                new AxisDirection[] {AxisDirection.EAST, AxisDirection.NORTH}, name);
        assertEquals(φ0, crs.getConversionFromBase().getParameterValues()
                            .parameter("Latitude of natural origin").doubleValue(), "φ0");
    }

    /**
     * Verifies a geographic CRS.
     *
     * @param  crs   the CRS to verify.
     * @param  name  expected CRS name.
     */
    private static void verifyCRS(final GeographicCRS crs, final String name) {
        assertEquals(name, crs.getName().getCode(), "name");
        assertAxisDirectionsEqual(crs.getCoordinateSystem(),
                new AxisDirection[] {AxisDirection.NORTH, AxisDirection.EAST}, name);
    }

    /**
     * Verifies the error message and error offset when trying to parse an erroneous CRS.
     *
     * @param  factory      factory to use.
     * @param  code         code of erroneous CRS.
     * @param  errorOffset  expected error index.
     */
    private static void verifyErroneousCRS(final WKTDictionary factory, final String code, final int errorOffset) {
        FactoryException exception;
        exception = assertThrows(FactoryException.class, () -> factory.createGeographicCRS(code));
        /*
         * Expect a message like: Cannot create a geodetic object for "E1".
         * The exact message is locale-dependent, so we cannot test fully.
         */
        assertMessageContains(exception, code);
        /*
         * Expect a message like: Missing "semiMajorAxis" component in "Ellipsoid" element.
         * The error offset (zero-based) should point to the character after "Ellipsoid" in
         * the following WKT:
         *
         *     Datum["Erroneous", Ellipsoid["Missing axis length"]]
         */
        final UnparsableObjectException cause = assertInstanceOf(UnparsableObjectException.class, exception.getCause());
        assertMessageContains(cause, "Ellipsoid", "semiMajorAxis");
        assertEquals(errorOffset, cause.getErrorOffset(), "errorOffset");
        /*
         * Try parsing again. The exception message should have been saved,
         * i.e. the parsing process is not repeated.
         */
        final String details = cause.getMessage();
        exception = assertThrows(FactoryException.class, () -> factory.createGeographicCRS(code));
        assertEquals(details, exception.getMessage());
        assertNull(exception.getCause());
    }

    /**
     * Tests {@link WKTDictionary#load(BufferedReader)} with a malformed file.
     *
     * @throws IOException if an error occurred while reading the test file.
     */
    @Test
    public void testLoadMalformed() throws IOException {
        FactoryException exception;
        final WKTDictionary factory = new WKTDictionary(null);
        try (BufferedReader source = new BufferedReader(new InputStreamReader(
                WKTFormatTest.class.getResourceAsStream("Malformed.txt"), "UTF-8")))
        {
            exception = assertThrows(FactoryException.class, () -> factory.load(source));
        }
        /*
         * Except a message like: Cannot read file at line 13. Cause is: missing ']' in "GeodCRS" element.
         * The exact message is locale-dependent, so we test for a few keywords only.
         */
        assertMessageContains(exception, "GeodCRS", "‘]’");
    }

    /**
     * Tests {@link WKTDictionary#fetchDefinition(DefaultIdentifier)}.
     *
     * @throws FactoryException if an error occurred while parsing a WKT.
     */
    @Test
    public void testFetchDefinition() throws FactoryException {
        final WKTDictionary factory = new WKTDictionary(null) {
            @Override protected String fetchDefinition(final DefaultIdentifier identifier) {
                identifier.setCodeSpace("aNS");
                switch (identifier.getCode()) {
                    case "2C": return "GeodCRS[\"Anguilla 1957\",\n" +
                                      " Datum[\"Anguilla 1957\",\n" +
                                      "  Ellipsoid[\"Clarke 1880\", 6378249.145, 293.465]],\n" +
                                      " CS[ellipsoidal, 2],\n" +
                                      "  Axis[\"Latitude\", north],\n" +
                                      "  Axis[\"Longitude\", east],\n" +
                                      "  Unit[\"Degree\", 0.0174532925199433],\n" +
                                      " Id[\"TEST\", 21]]";     // Intentionally mismatched code.

                    case "2N": return "GeodCRS[\"Anguilla 1957\",\n" +
                                      " Datum[\"Anguilla 1957\",\n" +
                                      "  Ellipsoid[\"Clarke 1880\", 6378249.145, 293.465]],\n" +
                                      " CS[ellipsoidal, 2],\n" +
                                      "  Axis[\"Latitude\", north],\n" +
                                      "  Axis[\"Longitude\", east],\n" +
                                      "  Unit[\"Degree\", 0.0174532925199433]]";

                    default: return null;
                }
            }
        };
        /*
         * Test a CRS with an identifier specified in the WKT. We intentionally declare an ID[…] element
         * with a different code than the one recognized by the `switch` statement ("2C" versus "21")
         * for checking precedence.
         */
        GeographicCRS crs = factory.createGeographicCRS("2C");
        Identifier id = TestUtilities.getSingleton(crs.getIdentifiers());
        assertEquals("TEST", id.getCodeSpace());
        assertEquals("21",   id.getCode());
        assertSame(crs, factory.createGeographicCRS("2C"));                         // Test caching.
        /*
         * Test a CRS without identifier in the WKT. An identifier should be automatically generated
         * by `WKTFormat.Parser.complete(…)`.
         */
        crs = factory.createGeographicCRS("2N");
        id = TestUtilities.getSingleton(crs.getIdentifiers());
        assertEquals("aNS", id.getCodeSpace());
        assertEquals("2N",  id.getCode());
        assertSame(crs, factory.createGeographicCRS("2N"));                         // Test caching.
        /*
         * Test non-existent code.
         */
        var exception = assertThrows(FactoryException.class, () -> factory.createGeographicCRS("21"));
        assertMessageContains(exception, "21");
    }
}
