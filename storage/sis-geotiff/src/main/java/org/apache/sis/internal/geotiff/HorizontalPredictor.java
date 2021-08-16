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
package org.apache.sis.internal.geotiff;

import java.nio.ByteBuffer;
import org.apache.sis.internal.jdk9.JDK9;


/**
 * Implementation of {@link Predictor#HORIZONTAL}.
 * Current implementation works only on 8-bits samples.
 *
 * <p><b>Note:</b> if we want to support 16 bits, 32 bits <i>etc.</i> sample values,
 * the main difficulty is that if there buffer ends in the middle of a sample value,
 * we need to stop the processing before that last value and stores it somewhere for
 * processing in the next call to {@link InflaterChannel#read(ByteBuffer)}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class HorizontalPredictor extends InflaterPredictor {
    /**
     * Data on the previous column. The length of this array is the pixel stride.
     */
    private final byte[] previousColumns;

    /**
     * Number of sample values between a row and the next row (in saùme .
     */
    private final int scanlineStride;

    /**
     * Column index (as a count of sample values, not a count of pixels).
     * Used for detecting when the decoding process starts a new row.
     */
    private int column;

    /**
     * Creates a new predictor.
     *
     * @param  input        the channel that decompress data.
     * @param  pixelStride  number of sample values per pixel in the source image.
     * @param  width        number of pixels in the source image.
     */
    HorizontalPredictor(final InflaterChannel input, final int pixelStride, final int width) {
        super(input);
        previousColumns = new byte[pixelStride];
        scanlineStride  = Math.multiplyExact(width, pixelStride);
    }

    /**
     * Applies the predictor on data in the given buffer,
     * from the given start position until current buffer position.
     *
     * @param  buffer  the buffer on which to apply the predictor.
     * @param  start   position of first sample value to process.
     */
    @Override
    protected void uncompress(final ByteBuffer buffer, final int start) {
        final int pixelStride = previousColumns.length;
        final int limit = buffer.position();
        int position = start;
        while (position < limit) {
            /*
             * This loop body should be executed on a row-by-row basis. But the `startOfRow` and `endOfRow` indices
             * may not be the real start/end of row if the previous call to this method finished before end of row,
             * or if current call to this method also finishes before end of row (because of buffer limit).
             */
            final int startOfRow = position;
            final int endOfRow   = Math.min(position + (scanlineStride - column), limit);
            final int head       = Math.min(position + pixelStride, endOfRow);
            if (column < pixelStride) {
                // Pixels in the first column are left unchanged.
                position += Math.min(pixelStride, endOfRow - position);
            }
            while (position < head) {
                buffer.put(position, (byte) (buffer.get(position) + previousColumns[position - startOfRow]));
                position++;
            }
            while (position < endOfRow) {
                buffer.put(position, (byte) (buffer.get(position) + buffer.get(position - pixelStride)));
                position++;
            }
            column += position - startOfRow;
            if (column >= scanlineStride) {
                column = 0;
            }
        }
        /*
         * Save the last bytes for next invocation of this method.
         */
        final int capacity = limit - start;
        if (capacity >= pixelStride) {
            JDK9.get(buffer, limit - pixelStride, previousColumns);
        } else {
            final int keep = pixelStride - capacity;
            System.arraycopy(previousColumns, keep, previousColumns, 0, capacity);
            JDK9.get(buffer, start, previousColumns, keep, capacity);
        }
    }
}
