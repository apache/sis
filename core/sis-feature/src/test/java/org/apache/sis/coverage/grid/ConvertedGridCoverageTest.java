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
package org.apache.sis.coverage.grid;

import java.util.Collections;
import java.awt.image.DataBuffer;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.FeatureAssert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link ConvertedGridCoverage}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
public final strictfp class ConvertedGridCoverageTest extends TestCase {
    /**
     * Creates a test coverage backed by an image of 2 pixels
     * on a single row with sample values (-1, 3).
     */
    private static BufferedGridCoverage coverage() {
        /*
         * A sample dimension with an identity transfer function
         * except for value -1 which will be mapped to NaN.
         */
        final SampleDimension sd = new SampleDimension.Builder()
                .addQualitative(null, -1)
                .addQuantitative("data", 0, 10, 1, 0, Units.UNITY)
                .setName("data")
                .build();
        /*
         * The "grid to CRS" transform does not matter for this test.
         */
        final GridGeometry grid = new GridGeometry(new GridExtent(2, 1), PixelInCell.CELL_CENTER,
                new AffineTransform2D(1, 0, 0, 1, 1, 0), HardCodedCRS.WGS84);

        final BufferedGridCoverage coverage = new BufferedGridCoverage(
                grid, Collections.singletonList(sd), DataBuffer.TYPE_SHORT);

        coverage.data.setElem(0, -1);
        coverage.data.setElem(1,  3);
        return coverage;
    }

    /**
     * Tests forward conversion from packed values to "geophysics" values.
     * Test includes a conversion of an integer value to {@link Float#NaN}.
     */
    @Test
    public void testForward() {
        final BufferedGridCoverage coverage = coverage();
        /*
         * Verify values before and after conversion.
         */
        assertValuesEqual(coverage.forConvertedValues(false).render(null), 0, new double[][] {
            {-1, 3}
        });
        final float nan = MathFunctions.toNanFloat(-1);
        assertTrue(Float.isNaN(nan));
        assertValuesEqual(coverage.forConvertedValues(true).render(null), 0, new double[][] {
            {nan, 3}
        });
    }

    /**
     * Tests the creation of a converted grid coverage through {@link GridCoverageProcessor}.
     */
    @Test
    public void testProcessor() {
        final GridCoverageProcessor processor = new GridCoverageProcessor();
        final GridCoverage source = coverage();
        final GridCoverage target = processor.convert(source, new MathTransform1D[] {
            (MathTransform1D) MathTransforms.linear(10, 100)
        }, null);
        assertSame(target, target.forConvertedValues(true));
        assertSame(source, target.forConvertedValues(false));
        assertValuesEqual(target.render(null), 0, new double[][] {
            {90, 130}      // {-1, 3} × 10 + 100
        });
        final SampleDimension band = getSingleton(target.getSampleDimensions());
        final NumberRange<?> range = band.getSampleRange().get();
        assertEquals(100, range.getMinDouble(), STRICT);
        assertEquals(200, range.getMaxDouble(), STRICT);
    }
}
