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
package org.apache.sis.coverage;

import java.awt.Dimension;
import java.awt.image.DataBufferFloat;
import javax.measure.IncommensurableException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.Test;
import static org.junit.Assert.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link CoverageCombiner}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CoverageCombinerTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CoverageCombinerTest() {
    }

    /**
     * Tests a coverage combination involving unit conversion.
     *
     * @throws TransformException if the coordinates of a given coverage cannot be transformed.
     * @throws IncommensurableException if the unit of measurement is not convertible.
     */
    @Test
    public void testUnitConversion() throws TransformException, IncommensurableException {
        final var s = new Dimension(2,2);
        GridCoverage c1 = new GridCoverageBuilder()
                .setDomain(new Envelope2D(null, 2, 2, s.width, s.height))
                .setRanges(new SampleDimension.Builder().addQuantitative("C1", 0, 10, Units.METRE).build())
                .setValues(new DataBufferFloat(new float[] {4, 8, 2, 3}, s.width * s.height), s)
                .build();

        GridCoverage c2 = new GridCoverageBuilder()
                .setDomain(new Envelope2D(null, 3, 2, s.width, s.height))
                .setRanges(new SampleDimension.Builder().addQuantitative("C1", 0, 10, Units.CENTIMETRE).build())
                .setValues(new DataBufferFloat(new float[] {500, 600, 900, 700}, s.width * s.height), s)
                .build();

        final var combiner = new CoverageCombiner(c1);
        combiner.acceptAll(c2);
        GridCoverage r = combiner.result();

        float[] data = null;
        data = r.render(null).getData().getSamples(0, 0, s.width, s.height, 0, data);
        assertArrayEquals(new float[] {4, 5, 2, 9}, data, 0);
    }
}
