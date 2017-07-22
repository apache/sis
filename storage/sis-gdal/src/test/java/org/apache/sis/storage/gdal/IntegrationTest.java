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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.referencing.CRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.referencing.operation.TransformException;

import static org.opengis.test.Assert.*;


/**
 * Tests the integration of the {@code sis-gdal} module in the Apache SIS framework.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class IntegrationTest extends TestCase {
    /**
     * Tests usage of {@link CRS#forCode(String)}.
     *
     * @throws FactoryException if the coordinate reference system can not be created.
     * @throws TransformException if an error occurred while testing a coordinate transformation.
     */
    @Test
    @org.junit.Ignore
    public void testCRS() throws FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = CRS.forCode("Proj4:+init=epsg:4326");
        final CoordinateReferenceSystem targetCRS = CRS.forCode("Proj4:+init=epsg:3395");
        final CoordinateOperation op = CRS.findOperation(sourceCRS, targetCRS, null);
        assertInstanceOf("Expected Proj.4 wrapper.", Transform.class, op.getMathTransform());
        Proj4FactoryTest.testMercatorProjection(op.getMathTransform());
    }
}
