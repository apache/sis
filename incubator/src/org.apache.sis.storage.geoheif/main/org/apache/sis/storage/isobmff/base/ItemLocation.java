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
package org.apache.sis.storage.isobmff.base;

import java.io.IOException;
import org.apache.sis.math.Vector;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;
import org.apache.sis.storage.isobmff.VectorReader;
import org.apache.sis.storage.isobmff.ByteReader;
import org.apache.sis.storage.isobmff.TreeNode;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;


/**
 * Directory of resources in the file, located by offset within their container and their length.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ItemLocation extends FullBox {
    /**
     * Numerical representation of the {@code "iloc"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 'l') << 8) | 'o') << 8) | 'c';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * All items contained in this box.
     */
    public final Item[] items;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws NegativeArraySizeException if the unsigned integer exceeds the capacity of 32-bits signed integers.
     */
    @SuppressWarnings("fallthrough")
    public ItemLocation(final Reader reader) throws IOException, DataStoreContentException {
        super(reader);
        final ChannelDataInput input = reader.input;
        /*
         * `sizes` contains 4 values on 4 bits each, for a total of 16 bits.
         * Those values are in order (from highest bits to lowest bits):
         *
         *   - offset_size
         *   - length_size
         *   - base_offset_size
         *   - index_size         present only on version > 0, otherwise should be 0.
         */
        int sizes = input.readUnsignedShort();
        final int version = version();
        final int count;
        switch (version) {
            case 0:  sizes &= 0xFFF0;    // Fall through
            case 1:  count = input.readUnsignedShort(); break;
            case 2:  count = input.readInt(); break;
            default: throw new UnsupportedVersionException(BOXTYPE, version);
        }
        items = new Item[count];
        for (int i=0; i<count; i++) {
            items[i] = new Item(input, version, sizes);
        }
    }

    /**
     * One of the resources in the enclosing item location. This resource may be fragmented into extents.
     * An extent is a continuous subset of the bytes of the resource. The full resource is formed by the
     * concatenation of the extents in the order specified in this {@code Item} object.
     */
    public static final class Item extends TreeNode implements ByteReader {
        /**
         * Identifier of this item, as an unsigned integer.
         */
        @Interpretation(Type.IDENTIFIER)
        public final int itemID;

        /**
         * A construction method where offsets are absolute into the file or the payload
         * of {@link IdentifiedMediaData} box referenced by {@link #dataReferenceIndex}.
         */
        private static final byte FILE_OFFSET = 0;

        /**
         * A construction method where offsets are into the {@link ItemData} box of the current {@link Meta} box.
         */
        private static final byte IDAT_OFFSET = 1;

        /**
         * A construction method where offsets are into the items indicated by {@link #itemReferenceIndex}.
         */
        private static final byte ITEM_OFFSET = 2;

        /**
         * How data should be accessed.
         *
         * <ul>
         *   <li>{@value #FILE_OFFSET}: by absolute byte offsets into the file or the payload
         *       of {@link IdentifiedMediaData} box referenced by {@link #dataReferenceIndex}.</li>
         *   <li>{@value #IDAT_OFFSET}: by byte offsets into the {@link ItemData} box of the current {@link Meta} box.</li>
         *   <li>{@value #ITEM_OFFSET}: by byte offset into the items indicated by {@link #itemReferenceIndex}.</li>
         * </ul>
         */
        public final byte constructionMethod;

        /**
         * An identification of the location of the data. 0 means "this file" and other values
         * are 1-based index of a {@code DataInformation} box in the enclosing {@link Meta} box.
         * This is used only for construction methods {@value #FILE_OFFSET} or {@value #ITEM_OFFSET}.
         */
        public final short dataReferenceIndex;

        /**
         * Origin of data offsets.
         * This origin is itself relative to another origin that depends on the construction method:
         *
         * <ul>
         *   <li>{@value #FILE_OFFSET}:<ul>
         *     <li>enclosing {@code MovieFragment} if any and if {@link #dataReferenceIndex] = 0,</li>
         *     <li>otherwise {@link IdentifiedMediaData} referenced (indirectly) by {@link #dataReferenceIndex},</li>
         *     <li>otherwise the beginning of the file identifier by {@link #dataReferenceIndex}.</li>
         *   </ul></li>
         *   <li>{@value #IDAT_OFFSET}: beginning of {@link ItemData}.</li>
         *   <li>{@value #ITEM_OFFSET}: the first byte of the concatenated data of the item referenced
         *       by {@link #itemReferenceIndex}.</li>
         * </ul>
         */
        @Interpretation(Type.UNSIGNED)
        public final long baseOffset;

        /**
         * The 1-based index of the {@link ItemReference} with {@code referenceType} = {@code "iloc"}.
         * This is used only for {@link #constructionMethod} = {@link #ITEM_OFFSET}.
         */
        public final Vector itemReferenceIndex;

        /**
         * Offsets in bytes from the data origin of the container for each extent data.
         * If {@code null}, then the resource has only one extent with an implicit offset of zero.
         */
        public final Vector extentOffset;

        /**
         * Length in bytes for each extent data.
         * If {@code null} or if the value is equal to 0, then the resource has only one extent
         * with an implicit length equals to the length of the entire referenced container.
         */
        public final Vector extentLength;

        /**
         * Decodes a single item.
         *
         * @param  input    the input stream from which to read the item.
         * @param  version  version of the enclosing {@link ItemLocation}.
         * @param  sizes    sizes of (offset, length, base, index) in that order on 4 bits each.
         * @throws IOException if an error occurred while reading the payload.
         * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
         */
        Item(final ChannelDataInput input, final int version, int sizes) throws IOException, DataStoreContentException {
            itemID = (version < 2) ? input.readUnsignedShort() : input.readInt();
            constructionMethod = (version == 0) ? 0 : (byte) (input.readShort() & 0x0F);
            dataReferenceIndex = input.readShort();
            baseOffset = VectorReader.readSingle(input, (sizes >>> 4) & 0x0F);
            final int count        = input.readUnsignedShort();
            final var indexReader  = VectorReader.create((sizes       ) & 0x0F, count);
            final var lengthReader = VectorReader.create((sizes >>>= 8) & 0x0F, count);
            final var offsetReader = VectorReader.create((sizes >>>  4) & 0x0F, count);
            for (int i=0; i<count; i++) {
                if (indexReader  != null)  indexReader.read(input, i);
                if (offsetReader != null) offsetReader.read(input, i);
                if (lengthReader != null) lengthReader.read(input, i);
            }
            itemReferenceIndex = result( indexReader);
            extentOffset       = result(offsetReader);
            extentLength       = result(lengthReader);
        }

        /**
         * Returns the result of the given reader, or {@code null} if none.
         * A vector containing only the value 0 is considered as missing,
         * in order to handle the special case of length 0.
         */
        @SuppressWarnings("fallthrough")
        private static Vector result(final VectorReader reader) {
            if (reader != null) {
                final Vector result = reader.result;
                switch (result.size()) {
                    case 0:  break;
                    case 1:  if (result.longValue(0) == 0) break;   // Else fall through.
                    default: return result;
                }
            }
            return null;
        }

        /**
         * Converts an offset relative to the data of this item to an offset relative to the origin of the input stream.
         * This method updates the {@link FileRegion#offset} value in-place. It may also replace {@link FileRegion#input}
         * if the bytes to read are spread in different regions of the item.
         *
         * @param  request  the input stream, offset and length of the region to read. Modified in-place by this method.
         * @throws UnsupportedEncodingException if this item uses an unsupported construction method.
         * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
         * @throws ArithmeticException if an integer overflow occurred.
         */
        @Override
        public void resolve(final FileRegion request) throws DataStoreException {
            switch (constructionMethod) {
                default: throw new DataStoreContentException("Unexpected construction method.");
                case IDAT_OFFSET:
                case ITEM_OFFSET: throw new UnsupportedEncodingException("Not supported yet");
                case FILE_OFFSET: {
                    if (dataReferenceIndex != 0) {
                        throw new UnsupportedEncodingException("Not supported yet");
                    }
                    long start = 0;
                    if (extentLength != null) {
                        final int n = extentLength.size();
                        for (int i=0; i<n; i++) {
                            long available = extentLength.longValue(i);
                            if (request.offset >= available) {
                                request.offset -= available;
                                continue;           // Extent is before requested data.
                            }
                            available -= request.offset;
                            if (request.length > available && i < n-1) {
                                /*
                                 * There is at least two extents to read. Create a channel
                                 * which will read each extent as if they were consecutive.
                                 * TODO: assign a temporary instance to `request.input`.
                                 */
                                throw new UnsupportedEncodingException("Not supported yet");
                            }
                            if (extentOffset != null && i < extentOffset.size()) {
                                start = extentOffset.longValue(i);
                            }
                            if (request.length > available || request.length < 0) {
                                request.length = available;
                            }
                            break;
                        }
                    }
                    /*
                     * All the bytes to read are in a single extent. We can
                     * use the existing channel directly, without wrapping.
                     */
                    request.offset = Math.addExact(request.offset, Math.addExact(baseOffset, start));
                }
            }
        }
    }
}
