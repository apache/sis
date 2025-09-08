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
package org.apache.sis.referencing.datum;

import java.util.HashMap;
import java.util.Locale;
import java.time.LocalDate;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.extent.Extent;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.measure.Units;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.internal.AnnotatedMatrix;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import static org.apache.sis.referencing.GeodeticObjectVerifier.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.test.TestStep;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.test.TestUtilities.getScope;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.referencing.Assertions.assertRemarksEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertMatrixEquals;


/**
 * Tests the {@link DefaultGeodeticDatum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultGeodeticDatumTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultGeodeticDatumTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing a geodetic reference frame definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultGeodeticDatumTest.class.getResourceAsStream("GeodeticDatum.xml");
    }

    /**
     * Tests the creation and serialization of a {@link DefaultGeodeticDatum}.
     */
    @Test
    public void testCreateAndSerialize() {
        final var properties = new HashMap<String,Object>();
        assertNull(properties.put(DefaultEllipsoid.NAME_KEY, "Asteroid"));
        final var ellipsoid = DefaultEllipsoid.createEllipsoid(properties, 1200, 1000, Units.METRE);

        properties.clear();
        assertNull(properties.put(DefaultEllipsoid.NAME_KEY, "Somewhere"));
        final var primeMeridian = new DefaultPrimeMeridian(properties, 12, Units.DEGREE);

        properties.clear();
        assertNull(properties.put("name",       "This is a name"));
        assertNull(properties.put("scope",      "This is a scope"));
        assertNull(properties.put("scope_fr",   "Valide pour tel usage"));
        assertNull(properties.put("remarks",    "There is remarks"));
        assertNull(properties.put("remarks_fr", "Voici des remarques"));
        assertNull(properties.put("remarks_ja", "注です。"));
        final var datum = new DefaultGeodeticDatum(properties, ellipsoid, primeMeridian);

        validate(datum);
        validate(assertSerializedEquals(datum));
    }

    /**
     * Compares the properties of the given datum objects with the properties set by the
     * {@link #testCreateAndSerialize()} method.
     */
    private static void validate(final DefaultGeodeticDatum datum) {
        Validators.validate(datum);
        InternationalString scope = getSingleton(datum.getDomains()).getScope();
        assertEquals("This is a name",        datum.getName().getCode());
        assertEquals("This is a scope",       scope.toString(Locale.ROOT));
        assertEquals("Valide pour tel usage", scope.toString(Locale.FRENCH));
        assertRemarksEquals("There is remarks",      datum, Locale.ROOT);
        assertRemarksEquals("Voici des remarques",   datum, Locale.FRENCH);
        assertRemarksEquals("注です。",              datum, Locale.JAPANESE);
    }

    /**
     * Tests {@link DefaultGeodeticDatum#isHeuristicMatchForName(String)}.
     */
    @Test
    public void testIsHeuristicMatchForName() {
        var datum = new DefaultGeodeticDatum(GeodeticDatumMock.WGS84);
        assertFalse(datum.isHeuristicMatchForName("WGS72"));
        assertTrue (datum.isHeuristicMatchForName("WGS84"));
        assertTrue (datum.isHeuristicMatchForName("WGS 84"));
        assertTrue (datum.isHeuristicMatchForName("WGS_84"));
        assertTrue (datum.isHeuristicMatchForName("D_WGS_84"));
        assertFalse(datum.isHeuristicMatchForName("E_WGS_84"));

        datum = HardCodedDatum.NTF;
        assertFalse(datum.isHeuristicMatchForName("WGS84"));
        assertTrue (datum.isHeuristicMatchForName("Nouvelle Triangulation Française"));
        assertTrue (datum.isHeuristicMatchForName("Nouvelle Triangulation Francaise"));
        assertTrue (datum.isHeuristicMatchForName("Nouvelle Triangulation Française (Paris)"));
        assertTrue (datum.isHeuristicMatchForName("Nouvelle Triangulation Francaise (Paris)"));
        assertFalse(datum.isHeuristicMatchForName("Nouvelle Triangulation Francaise (Greenwich)"));
    }

    /**
     * Tests {@link DefaultGeodeticDatum#getPositionVectorTransformation(GeodeticDatum, Extent)}.
     */
    @Test
    public void testGetPositionVectorTransformation() {
        final var properties = new HashMap<String,Object>();
        assertNull(properties.put(DefaultGeodeticDatum.NAME_KEY, "Invalid dummy datum"));
        /*
         * Associate two BursaWolfParameters, one valid only in a local area and the other one
         * valid globaly.  Note that we are building an invalid set of parameters, because the
         * source datum are not the same in both case. But for this test we are not interested
         * in datum consistency - we only want any Bursa-Wolf parameters having different area
         * of validity.
         */
        final BursaWolfParameters local  = BursaWolfParametersTest.createED87_to_WGS84();   // Local area (North Sea)
        final BursaWolfParameters global = BursaWolfParametersTest.createWGS72_to_WGS84();  // Global area (World)
        assertNull(properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, new BursaWolfParameters[] {local, global}));
        /*
         * Build the datum using WGS 72 ellipsoid (so at least one of the BursaWolfParameters is real).
         */
        final var datum = new DefaultGeodeticDatum(properties,
                GeodeticDatumMock.WGS72.getEllipsoid(),
                GeodeticDatumMock.WGS72.getPrimeMeridian());
        /*
         * Search for BursaWolfParameters around the North Sea area.
         */
        final var areaOfInterest = new DefaultGeographicBoundingBox(-2, 8, 55, 60);
        final var extent = new DefaultExtent("Around the North Sea", areaOfInterest, null, null);
        Matrix matrix = datum.getPositionVectorTransformation(GeodeticDatumMock.NAD83, extent);
        assertNull(matrix, "No BursaWolfParameters for NAD83");
        matrix = datum.getPositionVectorTransformation(GeodeticDatumMock.WGS84, extent);
        assertNotNull(matrix, "BursaWolfParameters for WGS84");
        checkTransformationSignature(local, matrix, 0);
        /*
         * Expand the area of interest to something greater than North Sea, and test again.
         */
        areaOfInterest.setWestBoundLongitude(-8);
        matrix = datum.getPositionVectorTransformation(GeodeticDatumMock.WGS84, extent);
        assertNotNull(matrix, "BursaWolfParameters for WGS84");
        checkTransformationSignature(global, matrix, 0);
        /*
         * Search in the reverse direction.
         */
        final var targetDatum = new DefaultGeodeticDatum(GeodeticDatumMock.WGS84);
        matrix = targetDatum.getPositionVectorTransformation(datum, extent);
        global.invert(); // Expected result is the inverse.
        checkTransformationSignature(global, matrix, 1E-6);
    }

    /**
     * Verifies if the given matrix is for the expected Position Vector transformation.
     * The easiest way to verify that is to check the translation terms (last matrix column),
     * which should have been copied verbatim from the {@code BursaWolfParameters} to the matrix.
     * Other terms in the matrix are modified compared to the {@code BursaWolfParameters} ones.
     */
    private static void checkTransformationSignature(final BursaWolfParameters expected, final Matrix actual,
            final double tolerance)
    {
        assertEquals(expected.tX, actual.getElement(0, 3), tolerance, "tX");
        assertEquals(expected.tY, actual.getElement(1, 3), tolerance, "tY");
        assertEquals(expected.tZ, actual.getElement(2, 3), tolerance, "tZ");
    }

    /**
     * Tests {@link DefaultGeodeticDatum#getPositionVectorTransformation(GeodeticDatum, Extent)}
     * going through an indirect transformation. The main purpose of this test is to verify that
     * the matrix is associated with {@link PositionalAccuracyConstant#INDIRECT_SHIFT_APPLIED}.
     */
    @Test
    public void testIndirectTransformation() {
        final var properties = new HashMap<String,Object>();
        assertNull(properties.put(DefaultGeodeticDatum.NAME_KEY, "Invalid dummy datum"));
        assertNull(properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, BursaWolfParametersTest.createWGS72_to_WGS84()));
        final var global = new DefaultGeodeticDatum(properties,
                GeodeticDatumMock.WGS72.getEllipsoid(),
                GeodeticDatumMock.WGS72.getPrimeMeridian());
        /*
         * Create a datum valid only in a specific region of the world and with no direct transformation to WGS72.
         * However, an indirect transformation to WGS72 is available through WGS84.
         */
        properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, BursaWolfParametersTest.createED87_to_WGS84());
        final var local = new DefaultGeodeticDatum(properties,
                GeodeticDatumMock.ED50.getEllipsoid(),
                GeodeticDatumMock.ED50.getPrimeMeridian());
        /*
         * Main test: verify that the transformation found is associated with accuracy information.
         */
        final Matrix m = local.getPositionVectorTransformation(global, null);
        assertSame(PositionalAccuracyConstant.INDIRECT_SHIFT_APPLIED,
                assertInstanceOf(AnnotatedMatrix.class, m, "Should have accuracy information.").accuracy);
        /*
         * Following is an anti-regression test only (no authoritative values).
         * Verified only opportunistically.
         */
        assertMatrixEquals(new Matrix4(1,   7.961E-7,  7.287E-7,   -82.981,
                               -7.961E-7,          1,  2.461E-6,   -99.719,
                               -7.287E-7,  -2.461E-6,         1,  -115.209,
                                       0,          0,         0,         1),
                m, 0.01, "getPositionVectorTransformation");
    }

    /**
     * Tests {@link DefaultGeodeticDatum#toWKT()}.
     */
    @Test
    public void testToWKT() {
        final var datum = new DefaultGeodeticDatum(GeodeticDatumMock.WGS84);
        assertWktEquals(Convention.WKT2_2015,
                "DATUM[“WGS84”,\n" +
                "  ELLIPSOID[“WGS84”, 6378137.0, 298.257223563, LENGTHUNIT[“metre”, 1]]]",
                datum);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "GeodeticDatum[“WGS84”,\n" +
                "  Ellipsoid[“WGS84”, 6378137.0, 298.257223563]]",
                datum);
    }

    /**
     * Tests marshalling.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        assertXmlEquals(
                "<gml:GeodeticDatum xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:name codeSpace=\"test\">WGS84</gml:name>\n" +
                "  <gml:primeMeridian>\n" +
                "    <gml:PrimeMeridian>\n" +
                "      <gml:name codeSpace=\"test\">Greenwich</gml:name>\n" +
                "      <gml:greenwichLongitude uom=\"urn:ogc:def:uom:EPSG::9102\">0.0</gml:greenwichLongitude>\n" +
                "    </gml:PrimeMeridian>\n" +
                "  </gml:primeMeridian>\n" +
                "  <gml:ellipsoid>\n" +
                "    <gml:Ellipsoid>\n" +
                "      <gml:name codeSpace=\"test\">WGS84</gml:name>\n" +
                "      <gml:semiMajorAxis uom=\"urn:ogc:def:uom:EPSG::9001\">6378137.0</gml:semiMajorAxis>\n" +
                "      <gml:secondDefiningParameter>\n" +
                "        <gml:SecondDefiningParameter>\n" +
                "          <gml:inverseFlattening uom=\"urn:ogc:def:uom:EPSG::9201\">298.257223563</gml:inverseFlattening>\n" +
                "        </gml:SecondDefiningParameter>\n" +
                "      </gml:secondDefiningParameter>\n" +
                "    </gml:Ellipsoid>\n" +
                "  </gml:ellipsoid>\n" +
                "</gml:GeodeticDatum>",
                marshal(new DefaultGeodeticDatum(GeodeticDatumMock.WGS84)), "xmlns:*");
    }

    /**
     * Tests unmarshalling.
     *
     * <p>This method is part of a chain.
     * The next method is {@link #testUnmarshalledWKT()}.</p>
     *
     * @return the unmarshalled datum.
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @TestStep
    public DefaultGeodeticDatum testUnmarshalling() throws JAXBException {
        final DefaultGeodeticDatum datum = unmarshalFile(DefaultGeodeticDatum.class, openTestFile());
        assertIsWGS84(datum, true);
        /*
         * Values in the following tests are specific to our XML file.
         * The actual texts in the EPSG database are more descriptive.
         */
        assertRemarksEquals("No distinction between the original and subsequent WGS 84 frames.", datum, null);
        assertEquals("Satellite navigation.", getScope(datum));
        assertEquals("Station coordinates changed by a few centimetres in 1994, 1997, 2002 and 2012.",
                     datum.getAnchorDefinition().orElseThrow().toString());
        assertEquals(LocalDate.of(1984, 1, 1), datum.getAnchorEpoch().orElseThrow());
        assertRemarksEquals("Defining parameters cited in EPSG database.", datum.getEllipsoid(), null);
        return datum;
    }

    /**
     * Tests the WKT formatting of the datum created by {@link #testUnmarshalling()}.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshalledWKT() throws JAXBException {
        final DefaultGeodeticDatum datum = testUnmarshalling();
        assertWktEquals(Convention.WKT1,
                "DATUM[“World Geodetic System 1984”,\n" +
                "  SPHEROID[“WGS 84”, 6378137.0, 298.257223563],\n" +
                "  AUTHORITY[“EPSG”, “6326”]]",
                datum);

        assertWktEquals(Convention.WKT2_2015,
                "DATUM[“World Geodetic System 1984”,\n" +
                "  ELLIPSOID[“WGS 84”, 6378137.0, 298.257223563, LENGTHUNIT[“metre”, 1]],\n" +
                "  ID[“EPSG”, 6326, URI[“urn:ogc:def:datum:EPSG::6326”]]]",
                datum);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "GeodeticDatum[“World Geodetic System 1984”,\n" +
                "  Ellipsoid[“WGS 84”, 6378137.0, 298.257223563],\n" +
                "  Id[“EPSG”, 6326, URI[“urn:ogc:def:datum:EPSG::6326”]]]",
                datum);

        assertWktEquals(Convention.INTERNAL,
                "GeodeticDatum[“World Geodetic System 1984”,\n" +
                "  Ellipsoid[“WGS 84”, 6378137.0, 298.257223563, Id[“EPSG”, 7030],\n" +
                "    Remark[“Defining parameters cited in EPSG database.”]],\n" +
                "  Anchor[“Station coordinates changed by a few centimetres in 1994, 1997, 2002 and 2012.”],\n" +
                "  AnchorEpoch[1984.000],\n" +  // The 3 digits are because of <gml:realizationEpoch> in test file.
                "  Usage[\n" +
                "    Scope[“Satellite navigation.”],\n" +
                "    Area[“World.”],\n" +
                "    BBox[-90.00, -180.00, 90.00, 180.00]],\n" +
                "  Id[“EPSG”, 6326],\n" +
                "  Remark[“No distinction between the original and subsequent WGS 84 frames.”]]",
                datum);
    }
}
