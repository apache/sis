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
package org.apache.sis.map.service.common;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.apache.sis.map.service.Scene2D;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link ResourceLoader}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ResourceLoaderTest {
    /**
     * Creates a new test case.
     */
    public ResourceLoaderTest() {
    }

    /**
     * Tests the loading of a raster image, and the cache.
     *
     * @throws IOException if the test image cannot be written.
     */
    @Test
    public void testRaster() throws IOException {
        final Path file = Files.createTempFile("resource", ".png");
        try {
            assertTrue(ImageIO.write(new BufferedImage(12, 7, BufferedImage.TYPE_INT_ARGB), "png", file.toFile()));
            final ResourceLoader loader = new ResourceLoader();
            final ExternalImage image = loader.image(file.toUri(), null);
            assertInstanceOf(ExternalImage.Raster.class, image);
            assertEquals(12, image.getWidth());
            assertEquals(7,  image.getHeight());
            assertEquals(12, image.image(24, 14).getWidth(), "Raster images are returned at their native resolution.");
            assertSame(image, loader.image(file.toUri(), "image/png"), "Shall be cached.");
            assertNull(loader.font(file.toUri(), null), "Not a font.");
        } finally {
            Files.delete(file);
        }
    }

    /**
     * Tests the loading of SVG documents, with and without inherent size.
     *
     * @throws IOException if the test documents cannot be written.
     */
    @Test
    public void testSVG() throws IOException {
        final ResourceLoader loader = new ResourceLoader();
        assumeTrue(loader.isSVGSupported(), "Apache Batik is not available.");
        final Path sized   = Files.createTempFile("resource", ".svg");
        final Path unsized = Files.createTempFile("resource", ".xml");
        try {
            Files.writeString(sized, "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='10'>"
                    + "<rect width='20' height='10' fill='#00FF00'/></svg>", StandardCharsets.UTF_8);
            Files.writeString(unsized, "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 30 10'>"
                    + "<rect width='30' height='10' fill='#00FF00'/></svg>", StandardCharsets.UTF_8);

            final ExternalImage image = loader.image(sized.toUri(), null);
            assertEquals(20, image.getWidth());
            assertEquals(10, image.getHeight());
            final BufferedImage rendered = image.image(40, 20);
            assertEquals(40, rendered.getWidth(), "SVG documents are rendered at the requested size.");
            assertEquals(0xFF00FF00, rendered.getRGB(20, 10));
            assertSame(rendered, image.image(40, 20), "The last rendered image shall be reused.");

            final ExternalImage defaultSize = loader.image(unsized.toUri(), "image/svg+xml");
            assertEquals(16, defaultSize.getHeight(), "Default height of documents without size.");
            assertEquals(48, defaultSize.getWidth(), 1E-9);
        } finally {
            Files.delete(sized);
            Files.delete(unsized);
        }
    }

    /**
     * Tests resources which cannot be loaded.
     * The warnings logged for those resources are hidden during this test.
     */
    @Test
    public void testFailures() {
        final ResourceLoader loader = new ResourceLoader();
        assertNull(loader.image(URI.create("file:///font.ttf"), null), "Fonts are not images.");
        final Level level = Scene2D.LOGGER.getLevel();
        Scene2D.LOGGER.setLevel(Level.OFF);
        try {
            assertNull(loader.image(URI.create("relative/image.png"), null));
            assertNull(loader.image(Path.of("does-not-exist.png").toAbsolutePath().toUri(), null));
        } finally {
            Scene2D.LOGGER.setLevel(level);
        }
    }
}
