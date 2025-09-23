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
package org.apache.sis.storage.test;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.StatisticsFormat;

// Test dependencies
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.Benchmark;


/**
 * Base class for testing the consistency of grid coverage read operations.
 * The test reads the whole grid coverage at full resolution,
 * then reads random sub-regions at random resolutions.
 * The sub-regions pixels are compared with the original image.
 *
 * <h2>Assumptions</h2>
 * Assuming that the code reading the full extent is correct, this class can detect some bugs
 * in the code reading sub-regions or applying sub-sampling. This assumption is reasonable if
 * we consider that the code reading the full extent is usually simpler than the code reading
 * a subset of data.
 *
 * <p>This class is not thread-safe. However, instances of this class can be reused for many test methods.</p>
 *
 * @param  <S> the data store class to test.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class CoverageReadConsistency<S extends DataStore> extends TestCase {
    /**
     * A constant for identifying the codes working on two dimensional slices.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * The data store to test. It will be closed after all tests finished.
     *
     * @see #closeFile()
     */
    protected final S store;

    /**
     * The resource to test. May be the same instance as {@link #store}.
     */
    private final GridCoverageResource resource;

    /**
     * The coverage at full extent, full resolution and with all bands.
     * This coverage will be used as a reference for verifying values read in sub-domains.
     * Can be {@code null} if unavailable (for example because the image is too large).
     */
    private final GridCoverage full;

    /**
     * Whether to allow random sub-regions to start elsewhere than (0,0).
     */
    private boolean allowOffsets;

    /**
     * Whether to use random subsampling.
     */
    private boolean allowSubsampling;

    /**
     * Whether to use random selection of bands.
     *
     * @see #randomRange()
     */
    private boolean allowBandSubset;

    /**
     * The random number generator to use.
     */
    private final Random random;

    /**
     * Number of random sub-regions to read.
     */
    private final int numIterations;

    /**
     * Whether mismatched pixel values should cause a test failure. The default value is {@code true}.
     * If {@code false}, then this class will only counts the failures and reports them as statistics.
     */
    private final boolean failOnMismatch;

    /**
     * Statistics about execution time.
     * Created only in benchmark mode.
     */
    @Benchmark
    private List<Statistics> statistics;

    /**
     * Creates a new tester. This constructor reads immediately the coverage at full extent and full resolution.
     * That full coverage will be used as a reference for verifying the pixel values read in sub-domains.
     * Any mismatch in pixel values will cause immediate test failure.
     *
     * @param  store  the data store to test.
     * @throws DataStoreException if the full coverage cannot be read.
     */
    public CoverageReadConsistency(final S store) throws DataStoreException {
        this.store     = store;
        resource       = resource();
        full           = resource.read(null, null);
        random         = TestUtilities.createRandomNumberGenerator();
        numIterations  = 100;
        failOnMismatch = true;
    }

    /**
     * Creates a new tester with specified configuration.
     * This tester may be used for benchmarking instead of JUnit tests.
     * Mismatched pixel values will be reported in statistics instead of causing test failure.
     *
     * @param  store      the data store to close after all tests are completed.
     * @param  tested     the resource to test.
     * @param  reference  full coverage read from the {@code resource}, or {@code null} if none.
     * @param  seed       seed for random number generator. Used for reproducible "random" values.
     * @param  readCount  number of read operations to perform.
     */
    public CoverageReadConsistency(final S store, final GridCoverageResource tested,
                final GridCoverage reference, final long seed, final int readCount)
    {
        this.store     = store;
        resource       = tested;
        full           = reference;
        random         = TestUtilities.createRandomNumberGenerator(seed);
        numIterations  = readCount;
        failOnMismatch = false;
    }

    /**
     * Returns the resource to read from the {@linkplain #store}.
     * This method shall not use any other field and shall not invoke any other method,
     * because this method is invoked during object construction.
     *
     * <p>This method is a work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors")
     * and will be removed (replaced by constructor argument) in a future version.</p>
     *
     * @return the resource to test.
     * @throws DataStoreException if an error occurred while reading the resource.
     */
    @Workaround(library="JDK", version="1.7")
    protected abstract GridCoverageResource resource() throws DataStoreException;

    /**
     * Tests reading in random sub-regions starting at coordinates (0,0).
     * Data are read at full resolution (no-subsampling) and all bands are read.
     * This is the simplest test.
     *
     * @throws DataStoreException if an error occurred while using the resource.
     */
    @Test
    public void testSubRegionAtOrigin() throws DataStoreException {
        allowOffsets     = false;
        allowBandSubset  = false;
        allowSubsampling = false;
        readAndCompareRandomRegions("Subregions at (0,0)");
    }

    /**
     * Tests reading in random sub-regions starting at random offsets.
     * Data are read at full resolution (no-subsampling) and all bands are read.
     *
     * @throws DataStoreException if an error occurred while using the resource.
     */
    @Test
    public void testSubRegionsAnywhere() throws DataStoreException {
        allowOffsets     = true;
        allowBandSubset  = false;
        allowSubsampling = false;
        readAndCompareRandomRegions("Subregions");
    }

    /**
     * Tests reading in random sub-regions starting at coordinates (0,0) with subsampling applied.
     * All bands are read.
     *
     * @throws DataStoreException if an error occurred while using the resource.
     */
    @Test
    public void testSubsamplingAtOrigin() throws DataStoreException {
        allowOffsets     = false;
        allowBandSubset  = false;
        allowSubsampling = true;
        readAndCompareRandomRegions("Subsampling at (0,0)");
    }

    /**
     * Tests reading in random sub-regions starting at random offsets with subsampling applied.
     * All bands are read.
     *
     * @throws DataStoreException if an error occurred while using the resource.
     */
    @Test
    public void testSubsamplingAnywhere() throws DataStoreException {
        allowOffsets     = true;
        allowBandSubset  = false;
        allowSubsampling = true;
        readAndCompareRandomRegions("Subsampling");
    }

    /**
     * Tests reading random subset of bands in random sub-region starting at coordinates (0,0).
     *
     * @throws DataStoreException if an error occurred while using the resource.
     */
    @Test
    public void testBandSubsetAtOrigin() throws DataStoreException {
        allowOffsets     = false;
        allowBandSubset  = true;
        allowSubsampling = false;
        readAndCompareRandomRegions("Bands at (0,0)");
    }

    /**
     * Tests reading random subset of bands in random sub-regions starting at random offsets.
     *
     * @throws DataStoreException if an error occurred while using the resource.
     */
    @Test
    public void testBandSubsetAnywhere() throws DataStoreException {
        allowOffsets     = true;
        allowBandSubset  = true;
        allowSubsampling = false;
        readAndCompareRandomRegions("Bands");
    }

    /**
     * Tests reading random subset of bands in random sub-regions starting at random offsets
     * with random subsampling applied.
     *
     * @throws DataStoreException if an error occurred while using the resource.
     */
    @Test
    public void testAllAnywhere() throws DataStoreException {
        allowOffsets     = true;
        allowBandSubset  = true;
        allowSubsampling = true;
        readAndCompareRandomRegions("All");
    }

    /**
     * Applies a random configuration on the resource.
     */
    private void randomConfigureResource() throws DataStoreException {
        final RasterLoadingStrategy[] choices = RasterLoadingStrategy.values();
        resource.setLoadingStrategy(choices[random.nextInt(choices.length)]);
    }

    /**
     * Creates a random domain to be used as a query on the {@link #resource} to test.
     * All arrays given to this method will have their values overwritten.
     *
     * @param  gg           value of {@link GridCoverage#getGridGeometry()} on the resource to test.
     * @param  low          pre-allocated array where to write the lower grid coordinates, inclusive.
     * @param  high         pre-allocated array where to write the upper grid coordinates, inclusive.
     * @param  subsampling  pre-allocated array where to write the subsampling.
     */
    private GridGeometry randomDomain(final GridGeometry gg, final long[] low, final long[] high, final long[] subsampling) {
        final GridExtent fullExtent = gg.getExtent();
        final int dimension = fullExtent.getDimension();
        for (int d=0; d<dimension; d++) {
            final int span = StrictMath.toIntExact(fullExtent.getSize(d));
            final int rs = random.nextInt(span);                            // Span of the sub-region - 1.
            if (allowOffsets) {
                low[d] = random.nextInt(span - rs);                         // Note: (span - rs) > 0.
            }
            high[d] = low[d] + rs;
            subsampling[d] = 1;
            if (allowSubsampling) {
                subsampling[d] += random.nextInt(StrictMath.max(rs / 16, 1));
            }
        }
        return gg.derive().subgrid(new GridExtent(null, low, high, true), subsampling).build();
    }

    /**
     * Returns the subset of bands to use for testing. This method never return {@code null},
     * but the set of bands is random only if {@link #allowBandSubset} is {@code true}.
     */
    private int[] randomRange(final int numBands) {
        if (!allowBandSubset) {
            return ArraysExt.range(0, numBands);
        }
        final int[] selectedBands = new int[numBands];
        for (int i=0; i<numBands; i++) {
            selectedBands[i] = random.nextInt(numBands);
        }
        // Remove duplicated elements.
        long included = 0;
        int count = 0;
        for (final int b : selectedBands) {
            if (included != (included |= Numerics.bitmask(b))) {
                selectedBands[count++] = b;
            }
        }
        return ArraysExt.resize(selectedBands, count);
    }

    /**
     * Implementation of methods testing reading in random sub-regions with random sub-samplings.
     *
     * @param  label  a label for the test being run.
     * @throws DataStoreException if an error occurred while using the resource.
     */
    private void readAndCompareRandomRegions(final String label) throws DataStoreException {
        randomConfigureResource();
        final GridGeometry gg = resource.getGridGeometry();
        final int    dimension   = gg.getDimension();
        final long[] low         = new long[dimension];
        final long[] high        = new long[dimension];
        final long[] subsampling = new long[dimension];
        final long[] subOffsets  = new long[dimension];
        final int    numBands    = resource.getSampleDimensions().size();
        /*
         * We will collect statistics on execution time only if the
         * test is executed in a more verbose mode than the default.
         */
        final Statistics durations = (VERBOSE || !failOnMismatch) ? new Statistics(label) : null;
        int failuresCount = 0;
        for (int it=0; it < numIterations; it++) {
            final GridGeometry domain = randomDomain(gg, low, high, subsampling);
            final int[] selectedBands = randomRange(numBands);
            /*
             * Read a coverage containing the requested sub-domain. Note that the reader is free to read
             * more data than requested. The extent actually read is `actualReadExtent`. It shall contain
             * fully the requested `domain`.
             */
            final long startTime = System.nanoTime();
            final GridCoverage subset = resource.read(domain, selectedBands);
            final GridExtent actualReadExtent = subset.getGridGeometry().getExtent();
            if (failOnMismatch) {
                assertEquals(dimension, actualReadExtent.getDimension(), "Unexpected number of dimensions.");
                for (int d=0; d<dimension; d++) {
                    if (subsampling[d] == 1) {
                        assertTrue(actualReadExtent.getSize(d) > high[d] - low[d], "Actual extent is too small.");
                        assertTrue(actualReadExtent.getLow (d) <= low[d],          "Actual extent is too small.");
                        assertTrue(actualReadExtent.getHigh(d) >= high[d],         "Actual extent is too small.");
                    }
                }
            }
            /*
             * If subsampling was enabled, the factors selected by the reader may be different than
             * the subsampling factors that we specified. The following block updates those values.
             */
            if (allowSubsampling && full != null) {
                final GridDerivation change = full.getGridGeometry().derive().subgrid(subset.getGridGeometry());
                System.arraycopy(change.getSubsampling(),        0, subsampling, 0, dimension);
                System.arraycopy(change.getSubsamplingOffsets(), 0, subOffsets,  0, dimension);
            }
            /*
             * Iterate over all dimensions greater than 2. In the common case where we are reading a
             * two-dimensional image, the following loop will be executed only once. If reading a 3D
             * or 4D image, the loop is executed for all possible two-dimensional slices in the cube.
             */
            final int sd = actualReadExtent.getDimension();
            final long[] sliceMin = new long[sd];
            final long[] sliceMax = new long[sd];
            for (int i=0; i<sd; i++) {
                sliceMin[i] = actualReadExtent.getLow(i);
                sliceMax[i] = actualReadExtent.getHigh(i);
            }
nextSlice:  for (;;) {
                System.arraycopy(sliceMin, BIDIMENSIONAL, sliceMax, BIDIMENSIONAL, dimension - BIDIMENSIONAL);
                final PixelIterator itr = iterator(full,   sliceMin, sliceMax, subsampling, subOffsets, allowSubsampling);
                final PixelIterator itc = iterator(subset, sliceMin, sliceMax, subsampling, subOffsets, false);
                if (itr != null) {
                    assertEquals(itr.getDomain().getSize(), itc.getDomain().getSize());
                    final double[] expected = new double[selectedBands.length];
                    double[] reference = null, actual = null;
                    while (itr.next()) {
                        assertTrue(itc.next());
                        reference = itr.getPixel(reference);
                        actual    = itc.getPixel(actual);
                        for (int i=0; i<selectedBands.length; i++) {
                            expected[i] = reference[selectedBands[i]];
                        }
                        if (!Arrays.equals(expected, actual)) {
                            failuresCount++;
                            if (!failOnMismatch) break;
                            final Point pr = itr.getPosition();
                            final Point pc = itc.getPosition();
                            final StringBuilder message = new StringBuilder(100).append("Mismatch at position (")
                                    .append(pr.x).append(", ").append(pr.y).append(") in full image and (")
                                    .append(pc.x).append(", ").append(pc.y).append(") in tested sub-image");
                            findMatchPosition(itr, pr, selectedBands, actual, message);
                            assertArrayEquals(expected, actual, message.toString());
                            /*
                             * POSSIBLE CAUSES FOR TEST FAILURE (known issues):
                             *
                             *   - If the `GridGeometry` has no `gridToCRS` transform, then `GridDerivation` manages
                             *     to save the scales (subsampling factors) anyway but the translations (subsampling
                             *     offsets) are lost. It causes an image shift if the offsets were not zero. Because
                             *     `gridToCRS` should never be null with spatial data, we do not complexify the code
                             *     for what may be a non-issue.
                             */
                        }
                    }
                    assertFalse(itc.next());
                } else {
                    // Unable to create a reference image. Just check that no exception is thrown.
                    double[] actual = null;
                    while (itc.next()) {
                        actual = itc.getPixel(actual);
                    }
                }
                /*
                 * Move to the next two-dimensional slice and read again.
                 * We stop the loop after we have read all 2D slices.
                 */
                for (int d=dimension; --d >= BIDIMENSIONAL;) {
                    if (sliceMin[d]++ <= actualReadExtent.getHigh(d)) continue nextSlice;
                    sliceMin[d] = actualReadExtent.getLow(d);
                }
                break;
            }
            if (durations != null) {
                durations.accept((System.nanoTime() - startTime) / (double) Constants.NANOS_PER_MILLISECOND);
            }
        }
        /*
         * Show statistics only if the test are executed with the `VERBOSE` flag set,
         * or if this `CoverageReadConsistency` is used for benchmark.
         */
        if (durations != null) {
            if (statistics == null) {
                statistics = new ArrayList<>();
            }
            statistics.add(durations);
            final int totalCount = durations.count();
            out.println("Number of failures: " + failuresCount + " / " + totalCount
                        + " (" + (failuresCount / (totalCount / 100f)) + "%)");
        }
    }

    /**
     * Prints statistics about execution time (in milliseconds) after all tests completed.
     */
    @AfterAll
    @Benchmark
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void printDurations() {
        if (statistics != null) {
            // It is too late for using `TestCase.out`.
            System.out.print(StatisticsFormat.getInstance().format(statistics.toArray(Statistics[]::new)));
            statistics = null;
        }
    }

    /**
     * Creates a pixel iterator for a sub-region in a slice of the specified coverage.
     * All coordinates given to this method are in the coordinate space of subsampled coverage subset.
     * This method returns {@code null} if the arguments are valid but the image cannot be created
     * because of a restriction in {@code PixelInterleavedSampleModel} constructor.
     *
     * @param  coverage     the coverage from which to get the iterator, or {@code null} if unavailable.
     * @param  sliceMin     lower bounds of the <var>n</var>-dimensional region of the coverage for which to get an iterator.
     * @param  sliceMax     upper bounds of the <var>n</var>-dimensional region of the coverage for which to get an iterator.
     * @param  subsampling  subsampling factors to apply on the image.
     * @param  subOffsets   offsets to add after multiplication by subsampling factors.
     * @return pixel iterator over requested area, or {@code null} if unavailable.
     */
    private static PixelIterator iterator(final GridCoverage coverage, long[] sliceMin, long[] sliceMax,
            final long[] subsampling, final long[] subOffsets, final boolean allowSubsampling)
    {
        if (coverage == null) {
            return null;
        }
        /*
         * Same extent as `areaOfInterest` but in two dimensions and with (0,0) origin.
         * We use that for clipping iteration to the area that we requested even if the
         * coverage gave us a larger area.
         */
        final Rectangle sliceAOI = new Rectangle(StrictMath.toIntExact(sliceMax[0] - sliceMin[0] + 1),
                                                 StrictMath.toIntExact(sliceMax[1] - sliceMin[1] + 1));
        /*
         * If the given coordinates were in a subsampled space while the coverage is at full resolution,
         * convert the coordinates to full resolution.
         */
        if (allowSubsampling) {
            sliceMin = sliceMin.clone();
            sliceMax = sliceMax.clone();
            for (int i=0; i<sliceMin.length; i++) {
                sliceMin[i] = sliceMin[i] * subsampling[i] + subOffsets[i];
                sliceMax[i] = sliceMax[i] * subsampling[i] + subOffsets[i];
            }
        }
        RenderedImage image = coverage.render(new GridExtent(null, sliceMin, sliceMax, true));
        /*
         * The subsampling offsets were included in the extent given to above `render` method call, so in principle
         * they should not be given again to `SubsampledImage` constructor. However, the `render` method is free to
         * return an image with a larger extent, which may result in different offsets. The result can be "too much"
         * offset. We want to compensate by subtracting the surplus. But because we cannot have negative offsets,
         * we shift the whole `sliceAOI` (which is equivalent to subtracting `subX|Y` in full resolution coordinates)
         * and set the offset to the complement.
         */
        if (allowSubsampling) {
            final int subX = StrictMath.toIntExact(subsampling[0]);
            final int subY = StrictMath.toIntExact(subsampling[1]);
            if (subX > image.getTileWidth() || subY > image.getTileHeight()) {
                return null;        // `SubsampledImage` does not support this case.
            }
            int offX = StrictMath.floorMod(image.getMinX(), subX);
            int offY = StrictMath.floorMod(image.getMinY(), subY);
            if (offX != 0) {sliceAOI.x--; offX = subX - offX;}
            if (offY != 0) {sliceAOI.y--; offY = subY - offY;}
            image = SubsampledImage.create(image, subX, subY, offX, offY);
            if (image == null) {
                return null;
            }
        }
        return new PixelIterator.Builder().setRegionOfInterest(sliceAOI).create(image);
    }

    /**
     * Explores pixel values around the given position in search for a pixel having the expected values.
     * If a match is found, the error message is completed with information about the match position.
     */
    private static void findMatchPosition(final PixelIterator ir, final Point pr, final int[] selectedBands,
                                          final double[] actual, final StringBuilder message)
    {
        final double[] expected = new double[actual.length];
        double[] reference = null;
        for (int dy=0; dy<10; dy++) {
            for (int dx=0; dx<10; dx++) {
                if ((dx | dy) != 0) {
                    for (int c=0; c<4; c++) {
                        final int x = (c & 1) == 0 ? -dx : dx;
                        final int y = (c & 2) == 0 ? -dy : dy;
                        try {
                            ir.moveTo(pr.x + x, pr.y + y);
                        } catch (IndexOutOfBoundsException e) {
                            continue;
                        }
                        reference = ir.getPixel(reference);
                        for (int i=0; i<selectedBands.length; i++) {
                            expected[i] = reference[selectedBands[i]];
                        }
                        if (Arrays.equals(expected, actual)) {
                            message.append(" (note: found a match at offset (").append(x).append(", ").append(y)
                                   .append(") in full image)");
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Closes the resource used by all tests.
     *
     * @throws DataStoreException if an error occurred while closing the resource.
     */
    @AfterAll
    public void closeFile() throws DataStoreException {
        store.close();
    }
}
