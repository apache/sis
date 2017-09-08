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
package org.apache.sis.internal.storage.folder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.test.TestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link FolderStore}.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class FolderStoreTest extends TestCase {

    /**
     * Test if sub stores are correctly detected.
     */
    @Test
    public void testOpeningFolder() throws IOException, DataStoreException {

        //create a folder with a prj file
        final Path directory = Files.createTempDirectory("folder");
        final Path prjFile = directory.resolve("crs.prj");
        try (BufferedWriter writer = Files.newBufferedWriter(prjFile)) {
            writer.write(CommonCRS.WGS84.normalizedGeographic().toWKT());
        }

        final FolderStore store = new FolderStore(directory.toUri());
        assertEquals(1,store.components().size());
        final Resource prjResource = store.components().iterator().next();
        assertTrue(prjResource instanceof DataStore);



    }

}
