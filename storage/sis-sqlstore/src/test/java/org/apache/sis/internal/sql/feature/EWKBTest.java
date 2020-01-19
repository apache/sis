package org.apache.sis.internal.sql.feature;

import java.nio.ByteBuffer;

import org.opengis.referencing.crs.GeographicCRS;

import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.test.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;


public class EWKBTest extends TestCase {

    public static final Geometries<?> GF = Geometries.implementation(GeometryLibrary.JTS);

    @ParameterizedTest
    @CsvFileSource(resources = "hexa_ewkb_4326.csv", numLinesToSkip = 1, delimiter = '\t')
    public void decodeHexadecimal(String wkt, String hexEWKB) throws Exception {
        final GeographicCRS expectedCrs = CommonCRS.defaultGeographic();
        final EWKBReader reader = new EWKBReader(GF).forCrs(expectedCrs);
        Assert.assertEquals("WKT and hexadecimal EWKB representation don't match",
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
        Assert.assertEquals(GF.createPoint(42.2, 43.3), read);
    }
}
