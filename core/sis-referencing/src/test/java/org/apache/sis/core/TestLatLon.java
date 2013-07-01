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

package org.apache.sis.core;

import junit.framework.TestCase;

/**
 * Tests methods from the {@link LatLon} class.
 *
 * @author rlaidlaw
 */
public class TestLatLon extends TestCase
{
  private static final double EPSILON = 0.000001;
  private LatLon point;

  /**
   * Set up objects prior to tests.
   */
  public void setUp()
  {
    point = new LatLon(-75.0, -145.0);
  }

  /**
   * Clear up objects after testing.
   */
  public void tearDown()
  {
    point = null;
  }

  /**
   * Tests the LatLon constructor.
   */
  public void testCreateLatLon()
  {
    assertNotNull(point);
  }

  /**
   * Tests the getLat() method.
   */
  public void testGetLat()
  {
    assertEquals(-75.0, point.getLat(), EPSILON);
  }

  /**
   * Tests the getLon() method.
   */
  public void testGetLon()
  {
    assertEquals(-145.0, point.getLon(), EPSILON);
  }

  /**
   * Tests the getShiftedLat() method.
   */
  public void testGetShiftedLat()
  {
    assertEquals(15.0, point.getShiftedLat(), EPSILON);
  }

  /**
   * Tests the getShiftedLon() method.
   */
  public void testGetShiftedLon()
  {
    assertEquals(35.0, point.getShiftedLon(), EPSILON);
  }

  /**
   * Tests the getNormLon() method.
   */
  public void testGetNormLon()
  {
    LatLon pointHighLon = new LatLon(0.0, 545.0);
    assertEquals(-175.0, pointHighLon.getNormLon(), EPSILON);

    LatLon pointLowLon = new LatLon(0.0, -545.0);
    assertEquals(175.0, pointLowLon.getNormLon(), EPSILON);
  }

  /**
   * Tests the toString() method.
   */
  public void testToString()
  {
    assertEquals("-75.0,-145.0", point.toString());
  }
}
