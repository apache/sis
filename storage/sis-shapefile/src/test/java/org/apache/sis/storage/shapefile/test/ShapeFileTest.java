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
package org.apache.sis.storage.shapefile.test;

import java.io.IOException;

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.shapefile.ShapeFile;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/**
 *
 * @author  Travis L. Pinney
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class ShapeFileTest {
    @Test
    public void testPolyineCount() throws IOException, DataStoreException {
        ShapeFile shp = new ShapeFile("data/SignedBikeRoute_4326_clipped.shp");
        assertEquals(shp.FeatureMap.size(), shp.FeatureCount);
    }

    @Test
    public void testPolygonCount() throws IOException, DataStoreException {
        ShapeFile shp = new ShapeFile("data/ANC90Ply_4326.shp");
        assertEquals(shp.FeatureMap.size(), shp.FeatureCount);
    }

    @Test
    public void testPointCount() throws IOException, DataStoreException {
        ShapeFile shp = new ShapeFile("data/ABRALicenseePt_4326_clipped.shp");
        assertEquals(shp.FeatureMap.size(), shp.FeatureCount);
    }
}
