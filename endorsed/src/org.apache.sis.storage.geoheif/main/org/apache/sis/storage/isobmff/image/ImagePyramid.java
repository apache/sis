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
package org.apache.sis.storage.isobmff.image;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.TreeNode;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;
import org.apache.sis.storage.isobmff.base.EntityToGroup;


/**
 * Information about a pyramid of images.
 *
 * @todo At the time of writing this class, this box type is defined in a draft ISO standard and not yet approved.
 *       Needs to be reviewed after ISO revision approval.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ImagePyramid extends EntityToGroup {
    /**
     * Numerical representation of the {@code "pymd"} box type.
     */
    public static final int BOXTYPE = ((((('p' << 8) | 'y') << 8) | 'm') << 8) | 'd';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * A tile matrix in an image pyramid.
     *
     * @todo At the time of writing this class, this box type is defined in a draft ISO standard and not yet approved.
     *       Needs to be reviewed after ISO revision approval.
     */
    public static final class Matrix extends TreeNode {
        /**
         * Identification of the layer.
         */
        public final int layerBinning;

        /**
         * Number of tiles in each row.
         */
        public final int tilesInLayerRow;

        /**
         * Number of tiles each column.
         */
        public final int tilesInLayerColumn;

        /**
         * Creates a new tile matrix.
         *
         * @param  input  the stream from which to read the tile matrix.
         * @throws IOException if an error occurred while reading the tile matrix.
         */
        Matrix(final ChannelDataInput input) throws IOException {
            layerBinning       = input.readUnsignedShort();
            tilesInLayerRow    = input.readUnsignedShort() + 1;
            tilesInLayerColumn = input.readUnsignedShort() + 1;
        }
    }

    /**
     * Tile width in pixels.
     */
    public final int tileSizeX;

    /**
     * Tile height in pixels.
     */
    public final int tileSizeY;

    /**
     * The tile matrices for each pyramid level.
     */
    public final Matrix[] matrices;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public ImagePyramid(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        final ChannelDataInput input = reader.input;
        tileSizeX = input.readUnsignedShort();
        tileSizeY = input.readUnsignedShort();
        matrices = new Matrix[entityID.length];
        for (int i=0; i<matrices.length; i++) {
            matrices[i] = new Matrix(input);
        }
    }
}
