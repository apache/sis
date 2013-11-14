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

import java.util.Map;
import java.util.HashMap;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.referencing.GeodeticObjects;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link DefaultGeodeticDatum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({DefaultEllipsoidTest.class, BursaWolfParametersTest.class})
public final strictfp class DefaultGeodeticDatumTest extends TestCase {
    /**
     * Tests {@link DefaultGeodeticDatum#getPositionVectorTransformation(GeodeticDatum, Extent)}.
     */
    @Test
    public void testGetPositionVectorTransformation() {
        final Map<String,Object> properties = new HashMap<>();
        assertNull(properties.put(DefaultGeodeticDatum.NAME_KEY, "Invalid dummy datum"));
        /*
         * Associate two BursaWolfParameters, one valid only in a local area and the other one
         * valid globaly.  Note that we are building an invalid set of parameters, because the
         * source datum are not the same in both case. But for this test we are not interrested
         * in datum consistency - we only want any Bursa-Wolf parameters having different area
         * of validity.
         */
        assertNull(properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, new BursaWolfParameters[] {
            BursaWolfParametersTest.createED87_to_WGS84(),  // Local area (North Sea)
            BursaWolfParametersTest.createWGS72_to_WGS84()  // Global area (World)
        }));
        /*
         * Build the datum.
         */
        final GeodeticDatum targetDatum = null; // TODO: need a WGS84 mock.
        final DefaultGeodeticDatum datum = new DefaultGeodeticDatum(properties,
                GeodeticObjects.WGS84.ellipsoid(),
                GeodeticObjects.WGS84.primeMeridian()
        );
        // TODO: test getPositionVectorTransformation
    }
}
