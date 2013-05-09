/**
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

package org.apache.sis.core;

import junit.framework.TestCase;

//JDK imports
import java.awt.geom.Rectangle2D;

/**
 * Tests methods from the {@link LatLonRect} class.
 * 
 * @author rlaidlaw
 */
public class TestLatLonRect extends TestCase
{
  private static final double EPSILON = 0.000001;
  
  /**
   * Tests the LatLonRect constructor.
   */
  public void testCreateLatLonRect()
  {
    LatLonRect rect = new LatLonRect(new LatLon(-50.0, -150.0), 
                                     new LatLon(50.0, 150.0));
    assertNotNull(rect);    
  }
  
  /**
   * Tests the getJavaRectangles() method.
   */
  public void testGetJavaRectangles()
  {
    LatLonRect r1 = new LatLonRect(new LatLon(0.0, 0.0), 
        new LatLon(50.0, 50.0));
    
    Rectangle2D[] rects1 = r1.getJavaRectangles();
    assertEquals(1, rects1.length);
    assertEquals(180.0, rects1[0].getX(), EPSILON);
    assertEquals(90.0, rects1[0].getY(), EPSILON);
    assertEquals(50.0, rects1[0].getWidth(), EPSILON);
    assertEquals(50.0, rects1[0].getHeight(), EPSILON);

    
    LatLonRect r2 = new LatLonRect(new LatLon(0.0, 155.0), 
        new LatLon(50.0, -155.0));

    Rectangle2D[] rects2 = r2.getJavaRectangles();
    assertEquals(2, rects2.length);

    assertEquals(0.0, rects2[0].getX(), EPSILON);
    assertEquals(90.0, rects2[0].getY(), EPSILON);
    assertEquals(25.0, rects2[0].getWidth(), EPSILON);
    assertEquals(50.0, rects2[0].getHeight(), EPSILON);

    assertEquals(335.0, rects2[1].getX(), EPSILON);
    assertEquals(90.0, rects2[1].getY(), EPSILON);
    assertEquals(25.0, rects2[1].getWidth(), EPSILON);
    assertEquals(50.0, rects2[1].getHeight(), EPSILON);
  }
}
