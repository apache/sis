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
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import java.util.function.DoubleUnaryOperator;
import org.apache.sis.system.Modules;
import org.apache.sis.math.Statistics;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCaseWithLogs;


/**
 * Tests {@link StatisticsCalculator}. This will also (indirectly) tests
 * {@link org.apache.sis.image.privy.TileOpExecutor} with multi-threading.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StatisticsCalculatorTest extends TestCaseWithLogs.Isolated {
    /**
     * Size of the artificial tiles. Should be small enough so we can have many of them.
     * Width and height should be different in order to increase the chance to see bugs
     * if some code confuse them.
     */
    private static final int TILE_WIDTH = 5, TILE_HEIGHT = 3;

    /**
     * The area of interest, or {@code null} if none.
     */
    private Shape areaOfInterest;

    /**
     * The filter to apply on sample values, or {@code null} if none.
     */
    private DoubleUnaryOperator[] sampleFilters;

    /**
     * Creates a new test case.
     */
    public StatisticsCalculatorTest() {
        super(Modules.RASTER);
    }

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
    private Statistics[] computeSequentially(final RenderedImage source) {
        return (Statistics[]) new StatisticsCalculator(source, areaOfInterest, sampleFilters, false, true).computeSequentially();
    }

    /**
     * Implementation of {@link #testParallelExecution()} and other tests with various options.
     * Values in the first band are determinist and values in the second band are randoms.
     *
     * @param  minimum  expected minimal sample value in the first band (the deterministic one).
     * @param  maximum  expected maximal sample value in the first band (the deterministic one).
     */
    private void compareParallelWithSequential(final ImageProcessor operations,
            final double minimum, final double maximum)
    {
        operations.setExecutionMode(ImageProcessor.Mode.PARALLEL);
        final TiledImageMock image = createImage();
        final Statistics[] expected = computeSequentially(image);
        final Statistics[] actual = operations.valueOfStatistics(image, areaOfInterest, sampleFilters);
        for (int i=0; i<expected.length; i++) {
            final Statistics e = expected[i];
            final Statistics a = actual  [i];
            assertEquals(e.minimum(), a.minimum());
            assertEquals(e.maximum(), a.maximum());
            assertEquals(e.sum(),     a.sum());
        }
        loggings.assertNoUnexpectedLog();
        assertEquals(minimum, actual[0].minimum());
        assertEquals(maximum, actual[0].maximum());
    }

    /**
     * Tests with parallel execution. The result of sequential execution is used as a reference.
     * The expected minimum sample value is 100 because this is by definition the value written
     * in the first pixel of the first tile created by {@link #createImage()}.
     */
    @Test
    public void testParallelExecution() {
        compareParallelWithSequential(new ImageProcessor(), 100, 51324);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests with an arbitrary area of interest.
     * The expected minimum and maximum values are determined empirically.
     */
    @Test
    public void testWithAOI() {
        areaOfInterest = new Ellipse2D.Float(70, -50, TILE_WIDTH*11.6f, TILE_HEIGHT*9.2f);
        compareParallelWithSequential(new ImageProcessor(), 19723, 44501);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests with sample filters. The filter excludes the first and the few last pixels in the image
     * created by {@link #createImage()}, which produces a visible effect on minimum and maximum values.
     */
    @Test
    public void testWithSampleFilters() {
        final ImageProcessor operations = new ImageProcessor();
        sampleFilters = new DoubleUnaryOperator[] {
            ImageProcessor.filterNodataValues(100, 51324, 51323, 201, 310)
        };
        compareParallelWithSequential(operations, 101, 51322);
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
        image.failRandomly(new Random(-8739538736973900203L), true);
        var e = assertThrows(ImagingOpException.class,
                () -> operations.valueOfStatistics(image, areaOfInterest, sampleFilters));
        assertMessageContains(e, StatisticsCalculator.STATISTICS_KEY);
        assertNotNull(e.getCause());
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
        image.failRandomly(new Random(8004277484984714811L), true);
        final Statistics[] stats = operations.valueOfStatistics(image, areaOfInterest, sampleFilters);
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

    /**
     * Tests {@link StatisticsCalculator#filterNodataValues(Number[])}.
     */
    @Test
    public void testFilterNodataValues() {
        assertNull(StatisticsCalculator.filterNodataValues(new Number[] {null, Double.NaN}));
        DoubleUnaryOperator op =  StatisticsCalculator.filterNodataValues(new Number[] {100});
        assertEquals( 10, op.applyAsDouble( 10));
        assertEquals(202, op.applyAsDouble(202));
        assertTrue(Double.isNaN(op.applyAsDouble(100)));

        op =  StatisticsCalculator.filterNodataValues(new Number[] {201, null, 100, 310, Double.NaN, 201});
        assertEquals( 10, op.applyAsDouble( 10));
        assertEquals(202, op.applyAsDouble(202));
        assertTrue(Double.isNaN(op.applyAsDouble(100)));
        assertTrue(Double.isNaN(op.applyAsDouble(310)));
        assertTrue(Double.isNaN(op.applyAsDouble(201)));
        loggings.assertNoUnexpectedLog();
    }
}
