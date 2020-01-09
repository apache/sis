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
package org.apache.sis.internal.coverage.j2d;

import java.util.List;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2DTest;
import org.apache.sis.coverage.grid.GridGeometry;


/**
 * Tests the {@link BufferedGridCoverage} implementation.
 * This method inherits the tests defined in {@link GridCoverage2DTest},
 * changing only the implementation class to test.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public final strictfp class BufferedGridCoverageTest extends GridCoverage2DTest {
    /**
     * Creates a {@link GridCoverage} instance to test with fixed sample values.
     * The coverage returned by this method shall contain the following values:
     *
     * {@preformat text
     *    2    5
     *   -5  -10
     * }
     *
     * @param  grid  the grid geometry of the coverage to create.
     * @param  sd    the sample dimensions of the coverage to create.
     * @return the coverage instance to test, with above-cited values.
     */
    protected GridCoverage createTestCoverage(final GridGeometry grid, final List<SampleDimension> sd) {
        /*
         * Create the grid coverage, gets its image and set values directly as short integers.
         */
        GridCoverage   coverage = new BufferedGridCoverage(grid, sd, DataBuffer.TYPE_SHORT);
        WritableRaster raster = ((BufferedImage) coverage.render(null)).getRaster();
        raster.setSample(0, 0, 0,   2);
        raster.setSample(1, 0, 0,   5);
        raster.setSample(0, 1, 0,  -5);
        raster.setSample(1, 1, 0, -10);
        return coverage;
    }
}
