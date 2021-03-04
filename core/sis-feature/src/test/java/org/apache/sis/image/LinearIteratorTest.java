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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import org.opengis.coverage.grid.SequenceType;

import static org.junit.Assert.*;


/**
 * Tests the linear read-write iterator on signed short integer values.
 *
 * <p>Historical note: in a previous version, this iteration order was implemented in a separated class
 * named {@code LinearIterator}. Linear order has been retrofitted in {@link PixelIterator} but we keep
 * that test class separated as an easy way to execute the same set tests with the two iteration orders
 * (default and linear).</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
public final strictfp class LinearIteratorTest extends PixelIteratorTest {
    /**
     * Creates a new test case.
     */
    public LinearIteratorTest() {
        super(DataBuffer.TYPE_SHORT, SequenceType.LINEAR);
    }

    /**
     * Returns the sequence of (x,y) coordinates expected for the iterator being tested.
     *
     * @param  subArea  the ranges of pixel coordinates in which to iterate.
     * @return sequence of (x,y) tuples inside the given ranges, in the order to be traversed by the iterator.
     */
    @Override
    int[] getCoordinatesInExpectedOrder(final Rectangle subArea) {
        final int[] coordinates = new int[subArea.width * subArea.height * 2];
        final int subMaxX = subArea.x + subArea.width;
        final int subMaxY = subArea.y + subArea.height;
        int i = 0;
        for (int y = subArea.y; y < subMaxY; y++) {
            for (int x = subArea.x; x < subMaxX; x++) {
                coordinates[i++] = x;
                coordinates[i++] = y;
            }
        }
        assertEquals(coordinates.length, i);
        return coordinates;
    }

    /**
     * Returns the index in iteration of the given coordinates. For example a return value of 2 means
     * that the given (x,y) should be the third point in iteration (iteration starts at index zero).
     * This method must be overridden for each kind of iterator to test.
     *
     * @param  x       <var>x</var> coordinate for which the iterator position is desired.
     * @param  y       <var>y</var> coordinate for which the iterator position is desired.
     * @return point index in iterator order for the given (x,y) coordinates.
     */
    @Override
    int getIndexOf(int x, int y) {
        final Rectangle bounds = getImageBounds();
        x -= bounds.x;
        y -= bounds.y;
        return y * bounds.width + x;
    }

    /**
     * Returns the expected sequence type.
     *
     * @param  singleTile  {@code true} if iteration occurs in a single tile, or {@code false} for the whole image.
     * @return the iteration order, which is {@link SequenceType#LINEAR} for the iterator to be tested in this class.
     */
    @Override
    SequenceType getIterationOrder(boolean singleTile) {
        return SequenceType.LINEAR;
    }

    /**
     * Returns the values of the given sub-region, organized in a {@link SequenceType#LINEAR} fashion.
     * This method is invoked for {@link #verifyWindow(Dimension)} purpose.
     *
     * @param  window  the sub-region for which to get values in a linear fashion.
     * @param  values  where to store the expected window values in linear order.
     */
    @Override
    void getExpectedWindowValues(final Rectangle window, final float[] values) {
        final Rectangle bounds = getImageBounds();
        int index = 0;
        int source = window.x + window.y * bounds.width;
        for (int y=0; y<window.height; y++) {
            final int length = window.width;
            copyExpectedPixels(source, values, index, length);
            source += bounds.width;
            index += length;
        }
    }
}
