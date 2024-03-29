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
package org.apache.sis.geometry.wrapper;

import org.opengis.geometry.Geometry;
import org.apache.sis.setup.GeometryLibrary;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link StandardGeometries} implementation for geometries exposed as GeoAPI objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StandardGeometriesTest extends GeometriesTestCase {
    /**
     * Creates a new test case.
     */
    public StandardGeometriesTest() {
        super(new StandardGeometries<>(org.apache.sis.geometry.wrapper.j2d.Factory.INSTANCE));
    }

    /**
     * Tests {@link Geometries#factory(GeometryLibrary)}.
     */
    @Test
    @Override
    public void testFactory() {
        assertInstanceOf(StandardGeometries.class, Geometries.factory(GeometryLibrary.GEOAPI));
    }

    /**
     * Tests {@link Factory#createPolyline(boolean, int, Vector...)}.
     */
    @Test
    @Override
    public void testCreatePolyline() {
        super.testCreatePolyline();
        assertInstanceOf(Geometry.class, geometry);
    }
}
