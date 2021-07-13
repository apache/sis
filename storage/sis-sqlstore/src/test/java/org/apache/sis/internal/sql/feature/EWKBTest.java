package org.apache.sis.internal.sql.feature;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.opengis.referencing.crs.GeographicCRS;

import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.test.TestCase;

import org.junit.Test;
import static org.junit.Assert.*;


public class EWKBTest extends TestCase {

    public static final Geometries<?> GF = Geometries.implementation(GeometryLibrary.JTS);

    public void decodeHexadecimal(String wkt, String hexEWKB) throws Exception {
        final GeographicCRS expectedCrs = CommonCRS.defaultGeographic();
        final EWKBReader reader = new EWKBReader(GF).forCrs(expectedCrs);
        assertEquals("WKT and hexadecimal EWKB representation don't match",
                GF.parseWKT(wkt).implementation(), reader.readHexa(hexEWKB));
    }

    /**
     * The purpose of this test is not to check complex geometries, which is validated by above one. We just want to
     * ensure that decoding directly a byte stream behaves in the same way than through hexadecimal.
     */
    @Test
    public void testBinary() {
        final ByteBuffer point = ByteBuffer.allocate(163);
        // Skip first byte: XDR mode
        point.position(1);
        // Create a 2D point
        point.putInt(1);
        point.putDouble(42.2);
        point.putDouble(43.3);

        // Prepare for reading
        point.position(0);

        final Object read = new EWKBReader(GeometryLibrary.JTS).read(point);
        assertEquals(GF.createPoint(42.2, 43.3), read);
    }

    /**
     * Temporary test for simulating JUnit 5 execution of {@link #decodeHexadecimal(String, String)}
     * as a parameterized test. To be removed after migration to JUnit 5.
     *
     * @throws Exception if test file can not be decoded.
     */
    @Test
    public void testDecodeHexadecimal() throws Exception {
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(
                EWKBTest.class.getResourceAsStream("hexa_ewkb_4326.csv"), StandardCharsets.UTF_8)))
        {
            String line;
            int numLinesToSkip = 1;
            while ((line = in.readLine()) != null) {
                if (!(line = line.trim()).isEmpty() && line.charAt(0) != '#' && --numLinesToSkip < 0) {
                    final String[] columns = line.split("\t");
                    assertEquals(2, columns.length);
                    decodeHexadecimal(columns[0], columns[1]);
                }
            }
        }
    }
}
