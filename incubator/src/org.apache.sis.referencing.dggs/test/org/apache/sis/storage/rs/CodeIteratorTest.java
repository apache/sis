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
package org.apache.sis.storage.rs;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests the default methods of {@link CodeIterator}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CodeIteratorTest {

    /**
     * Sample values of the single cell of {@link SingleCellIterator}.
     */
    private static final double[] SAMPLES = {1.5, -2.25, 3.75};

    /**
     * An iterator over a single cell. Band indices outside {@link #SAMPLES} throw an exception.
     */
    private static final class SingleCellIterator implements CodeIterator {
        @Override public int[]   getPosition()             {return new int[1];}
        @Override public void    moveTo(int[] zid)         {}
        @Override public boolean next()                    {return false;}
        @Override public int     getNumBands()             {return SAMPLES.length;}
        @Override public double  getSampleDouble(int band) {return SAMPLES[band];}
        @Override public void    rewind()                  {}
    }

    /**
     * Tests that {@link CodeIterator#getCell(float[])} keeps the fraction digits.
     */
    @Test
    public void testGetCellFloat() {
        final float[] cell = new SingleCellIterator().getCell((float[]) null);
        assertArrayEquals(new float[] {1.5f, -2.25f, 3.75f}, cell);
    }

    /**
     * Tests {@link CodeIterator#getCell(double[])} with a buffer longer than the number of bands.
     * The extra elements shall be left unchanged.
     */
    @Test
    public void testGetCellDoubleInLargerBuffer() {
        final double[] buffer = {Double.NaN, Double.NaN, Double.NaN, 7, 8};
        final double[] cell = new SingleCellIterator().getCell(buffer);
        assertSame(buffer, cell);
        assertArrayEquals(new double[] {1.5, -2.25, 3.75, 7, 8}, cell);
    }
}
