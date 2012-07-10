package org.apache.sis.util;

import junit.framework.TestCase;

//JDK imports
import java.util.Map;
import java.util.HashMap;

/**
 * Tests methods from the {@link GeoHashUtils} class.
 * 
 * @author rlaidlaw
 */
public class TestGeoHashUtils extends TestCase
{
  private static final double EPSILON = 0.000001;
  private HashMap<String, HashMap<String, double[]>> places;

  /**
   * Sets up objects before running tests.
   */
  public void setUp()
  {
    // Test data - locations and geohashes of some notable places
    places = new HashMap<String, HashMap<String, double[]>>();
    places.put("Empire State Building", new HashMap<String, double[]>());
    places.put("Statue Of Liberty", new HashMap<String, double[]>());
    places.put("The White House", new HashMap<String, double[]>());
    places.put("Hoover Dam", new HashMap<String, double[]>());
    places.put("Golden Gate Bridge", new HashMap<String, double[]>());
    places.put("Mount Rushmore", new HashMap<String, double[]>());
    places.put("Space Needle", new HashMap<String, double[]>());

    places.get("Empire State Building").put("dr5ru6j2c62q", new double[]{40.748433, -73.985656});
    places.get("Statue Of Liberty").put("dr5r7p4rx6kz", new double[]{40.689167, -74.044444});
    places.get("The White House").put("dqcjqcpeq70c", new double[]{38.897669, -77.03655});
    places.get("Hoover Dam").put("9qqkvh6mzfpz", new double[]{36.015556, -114.737778});
    places.get("Golden Gate Bridge").put("9q8zhuvgce0m", new double[]{37.819722, -122.478611});
    places.get("Mount Rushmore").put("9xy3teyv7ke4", new double[]{43.878947, -103.459825});
    places.get("Space Needle").put("c22yzvh0gmfy", new double[]{47.620400, -122.349100});
  }
  
  /**
   * Clears up objects after running tests.
   */
  public void tearDown()
  {
    places = null;
  }
  
  /**
   * Tests the encode() method.
   */
  public void testEncode()
  {
    for (Map.Entry<String, HashMap<String, double[]>> place : places.entrySet())
    {
      HashMap<String, double[]> geoData = place.getValue();
      
      if (geoData == null) 
      { 
        fail("incorrect test data"); 
      }
      else
      {
        for (Map.Entry<String, double[]> geoInfo : geoData.entrySet())
        {
          String geoHash = geoInfo.getKey();
          double[] point = geoInfo.getValue();
          
          if (geoHash == null || point == null) 
          { 
            fail("incorrect test data"); 
          }
          else
          {
            String encoded = GeoHashUtils.encode(point[0], point[1]);
            assertEquals(geoHash, encoded);
          }
        }        
      }
    }
  }
  
  /**
   * Tests the decode() method.
   */
  public void testDecode()
  {
    for (Map.Entry<String, HashMap<String, double[]>> place : places.entrySet())
    {
      HashMap<String, double[]> geoData = place.getValue();
      
      if (geoData == null) 
      { 
        fail("incorrect test data"); 
      }
      else
      {
        for (Map.Entry<String, double[]> geoInfo : geoData.entrySet())
        {
          String geoHash = geoInfo.getKey();
          double[] point = geoInfo.getValue();
          
          if (geoHash == null || point == null) 
          { 
            fail("incorrect test data"); 
          }
          else
          {
            double[] decoded = GeoHashUtils.decode(geoHash);
            assertEquals(point[0], decoded[0], EPSILON);
            assertEquals(point[1], decoded[1], EPSILON);
          }
        }
      }
    }
  }
}
