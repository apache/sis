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
 * distributed under the License is distributed on an "AS IS" BASIS,z
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.isobmff.mpeg;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.TreeNode;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * Compressed units item info.
 *
 * <p><b>Source:</b> ISO/IEC 23001-17:2024/Amd 2:2025</p>
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class CompressedUnitsItemInfo extends FullBox {
    /**
     * Numerical representation of the {@code "icef"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 'c') << 8) | 'e') << 8) | 'f';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Mapping from the 3 bits encoded in the stream to the actual unit offset size.
     */
    private static final byte[] OFFSET_LENGTHS = new byte[] {0, Short.SIZE, 3*Byte.SIZE, Integer.SIZE, Long.SIZE};

    /**
     * Mapping from the 3 bits encoded in the stream to the actual unit size value.
     */
    private static final byte[] SIZE_LENGTHS = new byte[] {Byte.SIZE, Short.SIZE, 3*Byte.SIZE, Integer.SIZE, Long.SIZE};

    /**
     * Number of bits used to encode the offset of an unit.
     * This information is not needed after the box has been read, but is kept for metadata purpose.
     */
    @Interpretation(Type.UNSIGNED)
    public final byte offsetFieldLength;

    /**
     * Number of bits used to encode the size of an unit, or 0.
     * This information is not needed after the box has been read, but is kept for metadata purpose.
     *
     * @todo What is the meaning of 0? Does it means that the size has the same size as the offset?
     */
    @Interpretation(Type.UNSIGNED)
    public final byte sizeFieldLength;

    /**
     * Compressed units offsets and sizes.
     * The number of compressed units is the length of this array.
     */
    public final Unit[] units;

    /**
     * A unit specified by an offset and a size.
     */
    public static final class Unit extends TreeNode {
        /** The offset in bytes, or 0 if unspecified. */
        @Interpretation(Type.UNSIGNED)
        public final long offset;

        /** The size in bytes. */
        @Interpretation(Type.UNSIGNED)
        public final long size;

        /** Creates a new unit with the given offset and size. */
        Unit(final long offset, final long size) {
            this.offset = offset;
            this.size   = size;
        }
    }

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     */
    public CompressedUnitsItemInfo(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        final int unitLayout = input.readUnsignedByte();
        offsetFieldLength = OFFSET_LENGTHS[(unitLayout >>> (Byte.SIZE - 3)) & 0b111];
        sizeFieldLength   =   SIZE_LENGTHS[(unitLayout >>> (Byte.SIZE - 6)) & 0b111];
        final int numCompressedUnits = input.readInt();
        units = new Unit[numCompressedUnits];
        for (int i = 0; i < numCompressedUnits; i++) {
            units[i] = new Unit(input.readBits(offsetFieldLength),
                                input.readBits(sizeFieldLength));
        }
        input.skipRemainingBits();
    }
}
