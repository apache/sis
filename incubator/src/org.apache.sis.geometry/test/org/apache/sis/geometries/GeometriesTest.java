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
package org.apache.sis.geometries;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class GeometriesTest {

    /**
     * Test creation of undefined CRS.
     */
    @Test
    public void testUndefinedCrs() {

        for (int i = 1; i < 10; i++) {
            CoordinateReferenceSystem crs = Geometries.getUndefinedCRS(i);

            assertTrue(Geometries.isUndefined(crs));

            try {
                CRS.findOperation(crs, CommonCRS.WGS84.geographic(), null);
                fail("Conversion to geographic crs should have fail");
            } catch (FactoryException ex) {
                //ok
            }
        }
    }

    /**
     * Test transform between cartesian crs
     */
    @Test
    @Disabled
    public void testCartesianCrsTransform() throws FactoryException {

        MathTransform trs = CRS.findOperation(Geometries.RIGHT_HAND_3D, Geometries.PSEUDOGEO_3D, null).getMathTransform();
        System.out.println(trs);

    }

}
