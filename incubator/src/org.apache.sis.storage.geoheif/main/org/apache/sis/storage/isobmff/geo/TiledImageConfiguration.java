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
package org.apache.sis.storage.isobmff.geo;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.BoxRegistry;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.util.collection.TreeTable;


/**
 * Configuration of the tile matrix. All tiles have the same size.
 * Tiles at the right or bottom border may extend beyond the total image size.
 *
 * <h4>Container</h4>
 * The container can be an {@link org.apache.sis.storage.isobmff.base.ItemPropertyContainer}.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class TiledImageConfiguration extends FullBox {
    /**
     * Numerical representation of the {@code "tilC"} box type.
     */
    public static final int BOXTYPE = ((((('t' << 8) | 'i') << 8) | 'l') << 8) | 'C';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Tile width in pixels.
     */
    @Interpretation(Type.UNSIGNED)
    public final int tileWidth;

    /**
     * Tile height in pixels.
     */
    @Interpretation(Type.UNSIGNED)
    public final int tileHeight;

    /**
     * The image type used for all tiles.
     * Examples: {@code hvc1} for h265 compression or {@code j2k1} for JPEG2000.
     */
    @Interpretation(value=Type.FOURCC, summary=true)
    public final int tileItemType;

    /**
     * The size of dimensions other than the two first dimensions.
     * This array is empty for two-dimensional images.
     * The sizes of the first two dimensions are obtained from the mandatory
     * {@link org.apache.sis.storage.isobmff.image.ImageSpatialExtents} box.
     */
    @Interpretation(Type.UNSIGNED)
    public final int[] extraDimensionSizes;

    /**
     * Image item properties used when decoding a tile image.
     * This includes at least all mandatory item properties for an image item of type {@link #tileItemType}
     * with the exception of {@code ispe}. If {@code tileImageProperties} does not contain an {@code ispe}
     * property box, the decoder shall synthesize an {@code ispe} property with {@link #tileWidth} and
     * {@link #tileHeight} as the size.
     */
    public final Box[] tileImageProperties;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws DataStoreException if the box creation failed for a logical error.
     */
    public TiledImageConfiguration(final Reader reader) throws IOException, DataStoreException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        tileWidth           = input.readInt();
        tileHeight          = input.readInt();
        tileItemType        = input.readInt();
        extraDimensionSizes = input.readInts(input.readUnsignedByte());
        tileImageProperties = new Box[input.readUnsignedByte()];
        for (int i = 0; i < tileImageProperties.length; i++) {
            tileImageProperties[i] = reader.readBox(BoxRegistry.global());
        }
    }

    /**
     * Mapping from {@link #flags} to the offset length.
     */
    private static final byte[] OFFSET_LENGTHS = {Integer.SIZE, 40, 48, Long.SIZE};

    /**
     * Mapping from {@link #flags} to the size length.
     * Value 0 means that size is not stored. Instead, it is computed from offsets.
     */
    private static final byte[] SIZE_LENGTHS = {0, 3*Byte.SIZE, Integer.SIZE, Long.SIZE};

    /**
     * Returns the number of bits used to store the offset to the image data of a specific tile.
     *
     * @return the number of bits for tile offset.
     */
    public final int offsetFieldLength() {
        return OFFSET_LENGTHS[flags & 0x03];
        // `IndexOutOfBoundsException` cannot happen because of `0x03` mask.
    }

    /**
     * Returns the number of bits used to store the length of the image data of a specific tile.
     *
     * @return the number of bits for tile length.
     */
    public final int sizeFieldLength() {
        return SIZE_LENGTHS[(flags >>> 2) & 0x03];
        // `IndexOutOfBoundsException` cannot happen because of `0x03` mask.
    }

    /**
     * Returns a hint about whether tiles are in sequential order.
     *
     * @return whether tiles are in sequential order.
     */
    public final boolean sequential() {
        return (flags & 0x10) != 0;
    }

    /**
     * Appends a description of the flags.
     *
     * @param  tree    builder of the tree to format.
     * @param  target  the {@code flag} node where to add properties.
     */
    @Override
    protected void appendFlagDescriptions(final TreeBuilder tree, final TreeTable.Node target) {
        tree.addNode(target, "offsetFieldLength", offsetFieldLength());
        tree.addNode(target, "sizeFieldLength",   sizeFieldLength());
        tree.addNode(target, "sequential",        sequential());
    }
}
