package org.apache.sis.internal.shapefile.jdbc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.apache.sis.storage.shapefile.ShapeFileTest;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.logging.Logging;
import org.junit.Before;

/**
 * Base class to settle a common environment to all the JDBC tests : 
 * all these tests are currently based on the SignedBikeRoute_4326_clipped.dbf DBase 3 file.
 * @author Marc LE BIHAN
 */
public abstract class AbstractTestBaseForInternalJDBC extends TestCase {
    /** Logger. */
    protected Logger log = Logging.getLogger(getClass().getName());
    
    /** The database file to use for testing purpose.  */
    protected File dbfFile;

    /**
     * Connect to test database.
     * @return Connection to database.
     * @throws SQLException if the connection failed.
     */
    public Connection connect() throws SQLException {
        final Driver driver = new DBFDriver();
        return driver.connect(dbfFile.getAbsolutePath(), null);        
    }
    
    /**
     * Test setup.
     * @throws URISyntaxException If an error occurred while getting the file to the test database.
     */
    @Before
    public void setup() throws URISyntaxException {
        final URL url = ShapeFileTest.class.getResource("SignedBikeRoute_4326_clipped.dbf");
        assertNotNull("The database file used for testing doesn't exist.", url);
        dbfFile = new File(url.toURI());
        assertTrue(dbfFile.isFile());
    }
}
