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
package org.apache.sis.image;

import java.util.Random;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.math.Statistics;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.test.TestCase;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link StatisticsCalculator}. This will also (indirectly) tests
 * {@link org.apache.sis.internal.coverage.j2d.TileOpExecutor} with multi-threading.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class StatisticsCalculatorTest extends TestCase {
    /**
     * Size of the artificial tiles. Should be small enough so we can have many of them.
     * Width and height should be different in order to increase the chance to see bugs
     * if some code confuse them.
     */
    private static final int TILE_WIDTH = 5, TILE_HEIGHT = 3;

    /**
     * Intercepts log records for verifying them.
     */
    @Rule
    public final LoggingWatcher loggings = new LoggingWatcher(Logging.getLogger(Modules.RASTER));

    /**
     * Creates a dummy image for testing purpose. This image will contain many small tiles
     * of two bands. The first band has deterministic values and the second band contains
     * random values.
     */
    private static TiledImageMock createImage() {
        final TiledImageMock image = new TiledImageMock(
                DataBuffer.TYPE_USHORT, 2,
                +51,                            // minX
                -72,                            // minY
                TILE_WIDTH  * 27,               // width
                TILE_HEIGHT * 19,               // height
                TILE_WIDTH,
                TILE_HEIGHT,
                -3,                             // minTileX
                +2,                             // minTileY
                false);
        image.initializeAllTiles(0);
        image.setRandomValues(1, new Random(), 1000);
        image.validate();
        return image;
    }

    /**
     * Computes statistics on the given image in a sequential way (everything computed in current thread).
     *
     * @param  source  the image on which to compute statistics.
     * @return statistics on the given image computed sequentially.
     */
    private static Statistics[] computeSequentially(final RenderedImage source) {
        return (Statistics[]) new StatisticsCalculator(source, null, false, true).computeSequentially();
    }

    /**
     * Tests with parallel execution. The result of sequential execution is used as a reference.
     */
    @Test
    public void testParallelExecution() {
        final ImageProcessor operations = new ImageProcessor();
        operations.setExecutionMode(ImageProcessor.Mode.PARALLEL);
        final TiledImageMock image = createImage();
        final Statistics[] expected = computeSequentially(image);
        final Statistics[] actual = operations.valueOfStatistics(image, null);
        for (int i=0; i<expected.length; i++) {
            final Statistics e = expected[i];
            final Statistics a = actual  [i];
            assertEquals("minimum", e.minimum(), a.minimum(), STRICT);
            assertEquals("maximum", e.maximum(), a.maximum(), STRICT);
            assertEquals("sum",     e.sum(),     a.sum(),     STRICT);
        }
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests with random failures propagated as exceptions.
     */
    @Test
    public void testWithFailures() {
        final ImageProcessor operations = new ImageProcessor();
        operations.setExecutionMode(ImageProcessor.Mode.PARALLEL);
        final TiledImageMock image = createImage();
        image.failRandomly(new Random(-8739538736973900203L));
        try {
            operations.valueOfStatistics(image, null);
            fail("Expected ImagingOpException.");
        } catch (ImagingOpException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains(StatisticsCalculator.STATISTICS_KEY));
            assertNotNull("Expected a cause.", e.getCause());
        }
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests with random failures that are logged.
     */
    @Test
    public void testWithLoggings() {
        final ImageProcessor operations = new ImageProcessor();
        operations.setExecutionMode(ImageProcessor.Mode.PARALLEL);
        operations.setErrorHandler(ErrorHandler.LOG);
        final TiledImageMock image = createImage();
        image.failRandomly(new Random(8004277484984714811L));
        final Statistics[] stats = operations.valueOfStatistics(image, null);
        for (final Statistics a : stats) {
            assertTrue(a.count() > 0);
        }
        /*
         * Verifies that a logging message has been emitted because of the errors.
         * All errors (there is many) should have been consolidated in a single record.
         */
        loggings.assertNextLogContains(/* no keywords we could rely on. */);
        loggings.assertNoUnexpectedLog();
    }
}
