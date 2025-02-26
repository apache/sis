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
package org.apache.sis.storage.isobmff.gimi;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.TreeNode;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * Mapping from pixel space to "real world" space.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ModelTiePoint extends FullBox {
    /**
     * Numerical representation of the {@code "tiep"} box type.
     */
    public static final int BOXTYPE = ((((('t' << 8) | 'i') << 8) | 'e') << 8) | 'p';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Pixel coordinates and corresponding "real world" coordinates for a single point.
     */
    public static class TiePoint extends TreeNode {
        /**
         * The pixel coordinate values. The array length is the number of dimensions.
         */
        public final int[] ijk;

        /**
         * The "real world" coordinate values. The array length is the number of dimensions.
         */
        public final double[] xyz;

        /**
         * Reads the coordinate values of a single point.
         *
         * @param  reader     the reader from which to read the payload.
         * @param  dimension  number of dimensions of the point to read.
         * @throws IOException if an error occurred while reading the payload.
         */
        TiePoint(final ChannelDataInput channel, final int dimension) throws IOException {
            ijk = channel.readInts(dimension);
            xyz = channel.readDoubles(dimension);
        }
    }

    /**
     * All tie points in this box.
     */
    public final TiePoint[] tiepoints;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public ModelTiePoint(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final int dimension = ((flags & 0x01) == 1) ? 2 : 3;
        final ChannelDataInput input = reader.input;
        tiepoints = new TiePoint[input.readUnsignedShort()];
        for (int i=0; i<tiepoints.length; i++) {
            tiepoints[i] = new TiePoint(input, dimension);
        }
    }
}
