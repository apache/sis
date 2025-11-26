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
package org.apache.sis.map.coverage;

import java.util.List;
import java.awt.image.RenderedImage;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.Names;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Test {@link MultiResolutionCoverageLoader}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MultiResolutionCoverageLoaderTest extends TestCase {
    /**
     * The loader being tested.
     */
    private final MultiResolutionCoverageLoader loader;

    /**
     * Transform from data CRS to the CRS for rendering, or {@code null} if none.
     */
    private MathTransform dataToObjective;

    /**
     * Point where to compute resolution, in coordinates of objective CRS.
     * Can be null if {@link #dataToObjective} is null or linear.
     */
    private DirectPosition objectivePOI;

    /**
     * Verifies that a transform with the given scale factors result in the given level to be found.
     */
    private void assertLevelEquals(final double sx, final double sy, final double sz, final int expected)
            throws TransformException
    {
        final LinearTransform objectiveToDisplay = MathTransforms.scale(1/sx, 1/sy, 1/sz);
        final int level = loader.findPyramidLevel(dataToObjective, objectiveToDisplay, objectivePOI);
        assertEquals(expected, level);
    }

    /**
     * Verifies that loading a coverage at the specified level result in a grid coverage
     * with the given scale factors.
     */
    private void assertLoadEquals(final int level, final double sx, final double sy, final double sz)
            throws DataStoreException
    {
        final GridCoverage coverage = loader.getOrLoad(level);
        final MathTransform gridToCRS = coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CORNER);
        final MathTransform expected = MathTransforms.scale(sx, sy, sz);
        assertEquals(expected, gridToCRS);
        assertSame(coverage, loader.getOrLoad(level));
    }

    /**
     * Creates a new test case with a loader for a dummy resource.
     *
     * @throws DataStoreException if an error occurred while querying the dummy resource.
     */
    public MultiResolutionCoverageLoaderTest() throws DataStoreException {
        loader = new MultiResolutionCoverageLoader(new DummyResource(), null, null);
    }

    /**
     * A dummy resource with arbitrary resolutions for testing purpose.
     * Resolutions are ordered from finest (smallest numbers) to coarsest (largest numbers).
     */
    private static final class DummyResource extends AbstractGridCoverageResource {
        /** Creates a dummy resource. */
        DummyResource() {
            super(null);
        }

        /** Returns the preferred resolutions in units of CRS axes. */
        @Override public List<double[]> getResolutions() {
            return List.of(new double[] {2, 3, 1},
                           new double[] {4, 4, 3},
                           new double[] {8, 9, 5});
        }

        /** Returns a grid geometry with the resolution of finest level. */
        @Override public GridGeometry getGridGeometry() {
            return new GridGeometry(new GridExtent(null, null, new long[] {10, 10, 10}, true),
                                PixelInCell.CELL_CORNER, MathTransforms.scale(2, 3, 1), null);
        }

        /** Not needed for this test. */
        @Override public List<SampleDimension> getSampleDimensions() {
            throw new UnsupportedOperationException();
        }

        /** Returns a dummy value (will not be used by this test). */
        @Override public GridCoverage read(final GridGeometry domain, final int... ranges) {
            final SampleDimension band = new SampleDimension(Names.createLocalName(null, null, "dummy"), null, List.of());
            return new GridCoverage(domain, List.of(band)) {
                @Override public RenderedImage render(GridExtent sliceExtent) {
                    throw new UnsupportedOperationException();                      // Not needed by this test.
                }
            };
        }
    }

    /**
     * Tests {@link MultiResolutionCoverageLoader#findPyramidLevel(MathTransform, LinearTransform, DirectPosition)}
     * with no "data to objective" transform.
     *
     * @throws TransformException if an error occurred while computing the resolution from a transform.
     */
    @Test
    public void testFindPyramidLevel() throws TransformException {
        assertLevelEquals(3, 2, 2, 0);
        assertLevelEquals(4, 5, 2, 0);
        assertLevelEquals(4, 5, 4, 1);
        assertLevelEquals(9, 9, 5, 2);
        assertLevelEquals(9, 8, 5, 1);
    }

    /**
     * Tests {@link MultiResolutionCoverageLoader#findPyramidLevel(MathTransform, LinearTransform, DirectPosition)}
     * with a "data to objective" transform set to a translation. Because translation has no effect on scale factors,
     * the result should be identical to {@link #testFindPyramidLevel()}.
     *
     * @throws TransformException if an error occurred while computing the resolution from a transform.
     */
    @Test
    public void testFindWithTranslation() throws TransformException {
        dataToObjective = MathTransforms.translation(-5, 7, 3);
        testFindPyramidLevel();
    }

    /**
     * Tests {@link MultiResolutionCoverageLoader#getOrLoad(int)}.
     *
     * @throws DataStoreException if an error occurred while querying the dummy resource.
     */
    @Test
    public void testGetOrLoad() throws DataStoreException {
        assertLoadEquals(2, 8, 9, 5);
        assertLoadEquals(0, 2, 3, 1);
        assertLoadEquals(1, 4, 4, 3);
    }
}
