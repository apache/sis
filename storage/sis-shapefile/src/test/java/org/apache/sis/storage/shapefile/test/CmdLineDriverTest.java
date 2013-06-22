package org.apache.sis.storage.shapefile.test;

import java.io.IOException;

import org.apache.sis.storage.shapefile.ShapeFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class CmdLineDriverTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CmdLineDriverTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( CmdLineDriverTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() throws IOException
    {
    	ShapeFile shp;
    	int count;
    	
    	shp = new ShapeFile("data/SignedBikeRoute_4326_clipped.shp");
    	
		//print(shp);
		
		count = 0;
		for (Integer i: shp.FeatureMap.keySet()) {
			//print(i);
			//print(shp.FeatureMap.get(i));
			//print("-----------------");
			count++;
			//System.exit(0);
		}
    	
		
		assertEquals(count, shp.FeatureCount);
		
		//Polyline poly = (Polyline) shp.FeatureMap.get(1).geom;
		
		
    	
    	shp = new ShapeFile("data/ANC90Ply_4326.shp");
		//print(shp);
		
		count = 0;
		for (Integer i: shp.FeatureMap.keySet()) {
			//print(i);
			//print(shp.FeatureMap.get(i));
			//print("-----------------");
			count++;
			//System.exit(0);
		}
		
		assertEquals(count, shp.FeatureCount);
		
	
		
		shp = new ShapeFile("data/ABRALicenseePt_4326_clipped.shp");
		//print(shp);
		
		count = 0;
		for (Integer i: shp.FeatureMap.keySet()) {
			//print(i);
			//print(shp.FeatureMap.get(i));
			//print("-----------------");
			count++;
			//System.exit(0);
		}
		
		assertEquals(count, shp.FeatureCount);
		
    	
    	
    	
        assertTrue( true );
    }
}
