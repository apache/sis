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
package org.apache.sis.storage.gdal;

import org.opengis.util.FactoryException;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Proj4} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class Proj4Test extends TestCase {
    /**
     * Tests {@link Proj4#definition(CoordinateReferenceSystem)} on geographic CRS.
     *
     * @throws FactoryException if an error occurred while computing the Proj.4 definition string.
     */
    @Test
    public void testGeographicDefinition() throws FactoryException {
        assertEquals("+proj=latlon +a=6378137.0 +b=6356752.314245179 +pm=0.0 +axis=enu", Proj4.definition(HardCodedCRS.WGS84));
        assertEquals("+proj=latlon +a=6378137.0 +b=6356752.314245179 +pm=0.0 +axis=neu", Proj4.definition(HardCodedCRS.WGS84_φλ));
    }
}
