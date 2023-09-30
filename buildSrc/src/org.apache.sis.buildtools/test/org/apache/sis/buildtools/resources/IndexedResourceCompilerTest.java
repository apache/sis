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
package org.apache.sis.buildtools.resources;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

// Test dependencies
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;


/**
 * Tests {@link IndexedResourceCompiler}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Quentin Bialota (Geomatys)
 */
public class IndexedResourceCompilerTest {
    /**
     * Tests {@link ResourceCompiler#filterLanguages(File[])}.
     */
    @Test
    public void testFilterLanguages() {
        final var resourcesToProcess = new TreeMap<File,Boolean>();
        for (final String file : new String[] {
                "Errors_en.properties",
                "Messages_fr.properties",
                "Errors.properties",
                "Errors_fr.properties",
                "ShallIgnore.properties",
                "Messages_en.properties",
                "Messages.properties"})
        {
            assertNull(resourcesToProcess.put(new File(file), Boolean.FALSE));
        }
        IndexedResourceCompiler.filterLanguages(resourcesToProcess, false);
        final var it = resourcesToProcess.entrySet().iterator();
        Map.Entry<File,Boolean> entry;

        entry = it.next(); assertEquals("Errors.properties",      entry.getKey().getName()); assertTrue (entry.getValue());
        entry = it.next(); assertEquals("Messages.properties",    entry.getKey().getName()); assertTrue (entry.getValue());
        entry = it.next(); assertEquals("ShallIgnore.properties", entry.getKey().getName()); assertFalse(entry.getValue());
        assertFalse(it.hasNext());
    }
}
