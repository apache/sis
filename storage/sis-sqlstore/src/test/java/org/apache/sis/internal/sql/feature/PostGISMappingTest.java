package org.apache.sis.internal.sql.feature;

import org.junit.Test;

import static org.apache.sis.internal.sql.feature.PostGISMapping.verifyVersion;
import static org.junit.Assert.*;

public class PostGISMappingTest {

    @Test
    public void parse_postgis_version() {
        assertTrue(verifyVersion("3.1 USE_GEOS=1 USE_PROJ=1 USE_STATS=1", 2));
        assertFalse(verifyVersion("2.1 USE_GEOS=1 USE_PROJ=1 USE_STATS=1", 3));

        // Verify minimal requirements: we need a text that starts with a number.
        assertTrue(verifyVersion("2", 2));
        assertTrue(verifyVersion("10.123.23", 2));

        assertFalse(verifyVersion("3d", 2));
        assertFalse(verifyVersion("d3", 2));
    }
}
