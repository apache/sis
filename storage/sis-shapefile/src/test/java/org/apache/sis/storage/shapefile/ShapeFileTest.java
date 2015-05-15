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
package org.apache.sis.storage.shapefile;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.feature.Feature;


/**
 * Tests the {@link ShapeFile} class.
 *
 * @author  Travis L. Pinney
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class ShapeFileTest extends TestCase {
    /**
     * Returns URI path to a resource.
     * @param name Resource name.
     * @return URI path.
     * @throws URISyntaxException if the resource name is incorrect.
     */
    private static String path(final String name) throws URISyntaxException {
        return new File(ShapeFileTest.class.getResource(name).toURI()).getPath();
    }

    /**
     * Test polylines count.
     * @throws URISyntaxException if the resource name is incorrect.
     * @throws DataStoreException if a general file reading trouble occurs.
     */
    @Test
    public void testPolyineCount() throws URISyntaxException, DataStoreException {
        ShapeFile shp = new ShapeFile(path("SignedBikeRoute_4326_clipped.shp"));
        readAll(shp);
    }

    /**
     * Test polygon count.
     * @throws URISyntaxException if the resource name is incorrect.
     * @throws DataStoreException if a general file reading trouble occurs.
     */
     @Test
     public void testPolygonCount() throws URISyntaxException, DataStoreException {
        ShapeFile shp = new ShapeFile(path("ANC90Ply_4326.shp"));
        readAll(shp);
    }

     /**
      * Test point count.
      * @throws URISyntaxException if the resource name is incorrect.
      * @throws DataStoreException if a general file reading trouble occurs.
      */
     @Test
     public void testPointCount() throws URISyntaxException, DataStoreException {
        ShapeFile shp = new ShapeFile(path("ABRALicenseePt_4326_clipped.shp"));
        readAll(shp);
     }

    /**
     * Read all the shapefile content.
     * @param shp Shapefile to read.
     * @throws DataStoreException if a general file reading trouble occurs.
     */
    private void readAll(ShapeFile shp) throws DataStoreException {
        try(InputFeatureStream is = shp.findAll()) {
            Feature feature = is.readFeature();

            while(feature != null) {
                feature = is.readFeature();
            }
        }
    }
}
