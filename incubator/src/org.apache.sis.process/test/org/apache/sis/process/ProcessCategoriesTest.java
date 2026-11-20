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
package org.apache.sis.process;

import java.util.Locale;
import org.opengis.util.GenericName;
import org.opengis.util.ScopedName;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link ProcessCategories} class.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ProcessCategoriesTest {
    /**
     * Creates a new test case.
     */
    public ProcessCategoriesTest() {
    }

    /**
     * Tests the locale-independent codes of root and child categories.
     */
    @Test
    public void testCodes() {
        assertEquals("vector",         ProcessCategories.VECTOR.toString());
        assertEquals("pointCloud",     ProcessCategories.POINT_CLOUD.toString());
        assertEquals("raster/terrain", ProcessCategories.RASTER_TERRAIN.toString());
        assertEquals("terrain",        ProcessCategories.RASTER_TERRAIN.tip().toString());
    }

    /**
     * Tests the localized labels, including the fallback on English for unsupported languages.
     */
    @Test
    public void testLabels() {
        final var label = ProcessCategories.RASTER_TERRAIN.tip().toInternationalString();
        assertEquals("Terrain analysis",   label.toString(Locale.ENGLISH));
        assertEquals("Analyse de terrain", label.toString(Locale.FRENCH));
        assertEquals("Terrain analysis",   label.toString(Locale.GERMAN));
        assertEquals("terrain",            label.toString(Locale.ROOT));
        assertEquals("Raster/Analyse de terrain",
                ProcessCategories.RASTER_TERRAIN.toInternationalString().toString(Locale.FRENCH));
    }

    /**
     * Tests the parent-child relationship between categories.
     */
    @Test
    public void testHierarchy() {
        final GenericName category = ProcessCategories.RASTER_TERRAIN;
        assertEquals(2, category.depth());
        assertEquals(ProcessCategories.RASTER, category.head());
        assertEquals(ProcessCategories.RASTER, ((ScopedName) category).path());
        assertEquals(1, ProcessCategories.RASTER.depth());
    }

    /**
     * Tests the creation of custom categories, as roots or below existing categories.
     */
    @Test
    public void testCreate() {
        final GenericName hydrology = ProcessCategories.create(ProcessCategories.RASTER_TERRAIN, "hydrology", "Hydrology");
        assertEquals("raster/terrain/hydrology", hydrology.toString());
        assertEquals(ProcessCategories.RASTER, hydrology.head());
        assertEquals("Hydrology", hydrology.tip().toInternationalString().toString(Locale.FRENCH));

        final GenericName custom = ProcessCategories.create(null, "custom", null);
        assertEquals("custom", custom.toString());
        assertEquals("custom", custom.toInternationalString().toString(Locale.ENGLISH));

        assertThrows(IllegalArgumentException.class, () -> ProcessCategories.create(null, "a/b", null));
        assertThrows(IllegalArgumentException.class, () -> ProcessCategories.create(null, " ", null));
    }
}
