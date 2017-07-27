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
package org.apache.sis.util.resources;

import java.io.File;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ResourceCompilerMojo}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
public strictfp class ResourceCompilerMojoTest {
    /**
     * Tests {@link ResourceCompilerMojo#filterLanguages(File[])}.
     */
    @Test
    public void testFilterLanguages() {
        final File[] files = {
            new File("Errors_en.properties"),
            new File("Messages_fr.properties"),
            new File("Errors.properties"),
            new File("Errors_fr.properties"),
            new File("ShallIgnore.properties"),
            new File("Messages_en.properties"),
            new File("Messages.properties")
        };
        assertEquals(2, ResourceCompilerMojo.filterLanguages(files));
        assertEquals("Errors.properties",   files[0].getName());
        assertEquals("Messages.properties", files[1].getName());
    }
}
