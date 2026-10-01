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
package org.apache.sis.storage.landsat;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.OptionKey;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.WarningEvent;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link LandsatStoreProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class LandsatStoreProviderTest extends TestCase implements StoreListener<WarningEvent> {
    /**
     * Whether a warning is expected.
     */
    private boolean isWarningExpected;

    /**
     * Creates a new test case.
     */
    public LandsatStoreProviderTest() {
    }

    /**
     * Tests {@link LandsatStoreProvider#probeContent(StorageConnector)} method.
     *
     * @throws DataStoreException if an error occurred while reading the test file.
     */
    @Test
    public void testProbeContentFromReader() throws DataStoreException {
        final var connector = new StorageConnector(MetadataReaderTest.class.getResourceAsStream("LandsatTest.txt"));
        connector.setOption(OptionKey.ENCODING, StandardCharsets.UTF_8);
        final var provider = new LandsatStoreProvider();
        assertEquals(ProbeResult.SUPPORTED, provider.probeContent(connector));
    }

    /**
     * Creates a temporary file with one band and read it.
     * The path of the image for the single band is returned.
     *
     * @param  tmpDir     temporary directory where to write a scene.
     * @param  sceneName  name of the scene. Will be the sub-directory filename.
     * @param  tiffFile   path of the <abbr>TIFF</abbr> file.
     * @return paths of the file as provided by the resource.
     * @throws IOException if an error occurred while writing the temporary file.
     * @throws DataStoreException if an error occurred while reading the temporary file.
     */
    private Collection<Path> readSingleBand(final Path tmpDir, final String sceneName, final String tiffFile)
            throws IOException, DataStoreException
    {
        final Path sceneDir = Files.createDirectories(tmpDir.resolve(sceneName));
        final Path sceneFile = sceneDir.resolve(sceneName + "_MTL.txt");
        Files.write(sceneFile, Arrays.asList(
                "GROUP = LANDSAT_METADATA_FILE",
                "  GROUP = PRODUCT_CONTENTS",
                "    FILE_NAME_BAND_1 = \"" + tiffFile + "\"",
                "  END_GROUP = PRODUCT_CONTENTS",
                "END_GROUP = LANDSAT_METADATA_FILE",
                "END"));

        final var paths = new ArrayList<Path>();
        try (var store = new LandsatStore(null, new StorageConnector(sceneDir))) {
            store.addListener(WarningEvent.class, this);
            for (Resource component : store.components()) {
                Aggregate group = assertInstanceOf(Aggregate.class, component);
                Resource band = assertSingleton(group.components());
                paths.addAll(band.getFileSet().orElseThrow().getPaths());
            }
        }
        return paths;
    }

    /**
     * Verifies that the Landsat reader detects when a band path is outside the scene directory.
     *
     * @param  tmpDir  temporary directory where to write a scene.
     * @throws IOException if an error occurred while writing the temporary file.
     * @throws DataStoreException if an error occurred while reading the temporary file.
     */
    @Test
    public void testBandPathValidation(@TempDir final Path tmpDir) throws IOException, DataStoreException {
        final Path expected = tmpDir.resolve("valid", "B1.TIFF");
        final Path actual = assertSingleton(readSingleBand(tmpDir, "valid", "B1.TIFF"));
        assertEquals(expected.toAbsolutePath(), actual.toAbsolutePath());
        isWarningExpected = true;
        assertTrue(readSingleBand(tmpDir, "invalid", "../outside/secret.tiff").isEmpty());
        assertFalse(isWarningExpected, "Warning should have been emitted.");
    }

    /**
     * Invoked when a warning is emitted.
     * Verifies if the warning was expected, and if so, if it contains the expected message.
     *
     * @param  event  the warning.
     */
    @Override
    public void eventOccurred(final WarningEvent event) {
        final String message = event.getMessage(null);
        assertTrue(isWarningExpected, message);
        assertTrue(message.contains("../outside/secret.tiff"), "../outside/secret.tiff");
        assertTrue(message.contains("Coastal Aerosol"), "Coastal Aerosol");
        isWarningExpected = false;
    }
}
