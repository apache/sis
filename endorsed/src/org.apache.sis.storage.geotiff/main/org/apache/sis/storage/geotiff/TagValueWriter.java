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
package org.apache.sis.storage.geotiff;

import java.io.IOException;
import org.apache.sis.io.stream.UpdatableWrite;
import org.apache.sis.io.stream.ChannelDataOutput;


/**
 * Writer of a tag value or array of values which are too large for fitting directly in a tag entry.
 * The tag entry will contain the position in the stream where those values are written,
 * and the values themselves will be written after all tag entries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class TagValueWriter {
    /**
     * A handler for writing the position of tag values when this position will become known.
     * This is initialized by {@link Writer#writeLargeTag(short, short, long, TagValueWriter)}.
     */
    UpdatableWrite<?> offset;

    /**
     * Creates a new container for the values of a tag.
     */
    TagValueWriter() {
    }

    /**
     * Writes the values of the tag at the current position of the given output stream.
     *
     * @param  output  the {@link Writer#output} value, provided for convenience.
     * @throws IOException if an error occurred while writing the tag values.
     */
    abstract void write(ChannelDataOutput output) throws IOException;

    /**
     * Writes again the values at the same offset than previously.
     * This is used when those values have changed.
     *
     * @param  output  the stream where to write tag values.
     * @throws IOException if an error occurred while writing the tag values.
     *
     * @see TileMatrixWriter#isLengthChanged()
     */
    final void rewrite(final ChannelDataOutput output) throws IOException {
        /*
         * The offset is empty if the image has only one tile,
         * because in such case the value fits in the IFD entry.
         */
        output.seek(offset.getAsLong().orElse(offset.position));
        write(output);
    }
}
