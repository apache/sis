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
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author  Travis L. Pinney
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class ShapeFileTest extends TestCase {
    private static String path(final String name) throws IOException, URISyntaxException {
        return new File(ShapeFileTest.class.getResource(name).toURI()).getPath();
    }

    @Test
    public void testPolyineCount() throws URISyntaxException, IOException, DataStoreException {
        ShapeFile shp = new ShapeFile(path("SignedBikeRoute_4326_clipped.shp"));
        assertEquals(shp.FeatureMap.size(), shp.FeatureCount);
    }

    @Test
    public void testPolygonCount() throws URISyntaxException, IOException, DataStoreException {
        ShapeFile shp = new ShapeFile(path("ANC90Ply_4326.shp"));
        assertEquals(shp.FeatureMap.size(), shp.FeatureCount);
    }

    @Test
    public void testPointCount() throws URISyntaxException, IOException, DataStoreException {
        ShapeFile shp = new ShapeFile(path("ABRALicenseePt_4326_clipped.shp"));
        assertEquals(shp.FeatureMap.size(), shp.FeatureCount);
    }
}
