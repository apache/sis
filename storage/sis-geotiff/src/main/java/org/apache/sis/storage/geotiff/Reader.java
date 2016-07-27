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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.ByteOrder;
import java.text.ParseException;
import org.apache.sis.internal.storage.ChannelDataInput;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.resources.Errors;


/**
 * An image reader for GeoTIFF files. This reader duplicates the implementations performed by other libraries, but we
 * nevertheless provide our own reader in Apache SIS for better control on the process of decoding geospatial metadata.
 * We also measured better performance with this reader at least for uncompressed files, and added support for some
 * unusual data layout not supported by other libraries.
 *
 * <p>This image reader can also process <cite>Big TIFF</cite> images.</p>
 *
 * <p>The TIFF format specification version 6.0 (June 3, 1992) is available
 * <a href="https://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf">here</a>.</p>
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class Reader extends GeoTIFF {
    /**
     * The stream from which to read the data.
     */
    final ChannelDataInput input;

    /**
     * Stream position of the first byte of the GeoTIFF file. This is usually zero.
     */
    private final long origin;

    /**
     * A multiplication factor for the size of pointers, expressed as a power of 2.
     * The pointer size in bytes is given by {@code Integer.BYTES << pointerExpansion}.
     *
     * <p>Values can be:</p>
     * <ul>
     *   <li>0 for the classical TIFF format, which uses 4 bytes.</li>
     *   <li>1 for the BigTIFF format, which uses 8 bytes.</li>
     *   <li>2 for 16 bytes (not yet used, but the BigTIFF specification makes provision for it).
     * </ul>
     *
     * Those values are defined that way for making easier (like a boolean flag) to test if
     * the file is a BigTIFF format, with statement like {@code if (intSizeExpansion != 0)}.
     */
    private final byte intSizeExpansion;

    /**
     * Positions of each <cite>Image File Directory</cite> (IFD) in this file.
     * Those positions are fetched when first needed.
     */
    private final List<ImageFileDirectory> imageFileDirectories = new ArrayList<>();

    /**
     * Builder for the metadata.
     */
    final MetadataBuilder metadata;

    /**
     * Creates a new GeoTIFF reader which will read data from the given input.
     * The input must be at the beginning of the GeoTIFF file.
     *
     * @throws IOException if an error occurred while reading bytes from the stream.
     * @throws DataStoreException if the file is not encoded in the TIFF or BigTIFF format.
     */
    Reader(final GeoTiffStore owner, final ChannelDataInput input) throws IOException, DataStoreException {
        super(owner);
        this.input    = input;
        this.origin   = input.getStreamPosition();
        this.metadata = new MetadataBuilder();
        /*
         * A TIFF file begins with either "II" (0x4949) or "MM" (0x4D4D) characters.
         * Those characters identify the byte order. Note we we do not need to care
         * about the byte order for this flag since the two bytes shall have the same value.
         */
        final short order = input.readShort();
        final boolean isBigEndian = (order == 0x4D4D);
        if (isBigEndian || order == 0x4949) {
            input.buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            /*
             * Magic number of TIFF file is 42, followed by nothing else.
             * The pointer type is implicitely the Java Integer type (4 bytes).
             *
             * Magic number of BigTIFF file is 43, followed by the pointer size.
             * Currently, that pointer type canonly be the Java Long type (8 bytes),
             * but a future BigTIFF version may allow 16 bytes wide pointers.
             */
            switch (input.readShort()) {
                case 42: {                                          // Magic number of classical format.
                    intSizeExpansion = 0;
                    return;
                }
                case 43: {                                          // Magic number of BigTIFF format.
                    final int numBits  = input.readUnsignedShort();
                    final int powerOf2 = Integer.numberOfTrailingZeros(numBits);    // In the [0 … 32] range.
                    if (numBits == (1L << powerOf2) && input.readShort() == 0) {
                        intSizeExpansion = (byte) (powerOf2 - 2);
                        if (intSizeExpansion == 1) {
                            /*
                             * Above 'intSizeExpension' calculation was a little bit useless since we accept only
                             * one result in the end, but we did that generic computation anyway for keeping the
                             * code almost ready if the BigTIFF specification adds support for 16 bytes pointer.
                             */
                            return;
                        }
                    }
                }
            }
        }
        // Do not invoke errors() yet because GeoTiffStore construction may not be finished.
        throw new DataStoreException(Errors.format(Errors.Keys.UnexpectedFileFormat_2, "TIFF", input.filename));
    }

    /**
     * Returns a default message for parsing error.
     */
    private String canNotRead() {
        return errors().getString(Errors.Keys.CanNotParseFile_2, "TIFF", input.filename);
    }

    /**
     * Reads the {@code int} or {@code long} value (depending if the file is
     * a standard of big TIFF) at the current {@linkplain #input} position.
     *
     * @return The next pointer value.
     */
    private long readUnsignedInt() throws IOException, DataStoreException {
        if (intSizeExpansion == 0) {
            return input.readUnsignedInt();         // Classical format.
        }
        final long pointer = input.readLong();      // BigTIFF format.
        if (pointer >= 0) {
            return pointer;
        }
        throw new DataStoreException(canNotRead());
    }

    /**
     * Reads the {@code short} or {@code long} value (depending if the file is
     * standard of big TIFF) at the current {@linkplain #input} position.
     *
     * @return The next directory entry value.
     */
    private long readUnsignedShort() throws IOException, DataStoreException {
        if (intSizeExpansion == 0) {
            return input.readUnsignedShort();       // Classical format.
        }
        final long entry = input.readLong();        // BigTIFF format.
        if (entry >= 0) {
            return entry;
        }
        throw new DataStoreException(canNotRead());
    }

    /**
     * Reads the next bytes in the {@linkplain #input}, which must be the 32 or 64 bits
     * offset to the next <cite>Image File Directory</cite> (IFD), then parses that IFD.
     * The IFD consists of a 2 (classical) or 8 (BigTiff)-bytes count of the number of
     * directory entries, followed by a sequence of 12-byte field entries, followed by
     * a pointer to the next IFD (or 0 if none).
     *
     * <p>The parsed entry is added to the {@link #imageFileDirectories} list.</p>
     *
     * @return {@code true} if we found a new IFD, or {@code false} if there is no more IFD.
     * @throws ArithmeticException if the pointer to the next IFD is too far.
     */
    private boolean nextImageFileDirectory() throws IOException, DataStoreException {
        final long offset = readUnsignedInt();
        if (offset == 0) {
            return false;
        }
        /*
         * Design note: we parse the Image File Directory entry now because even if we were
         * not interrested in that IFD, we need to go anyway after its last record in order
         * to get the pointer to the next IFD.
         */
        input.seek(Math.addExact(origin, offset));
        final int offsetSize = Integer.BYTES << intSizeExpansion;
        final ImageFileDirectory dir = new ImageFileDirectory(offset);
        for (long remaining = readUnsignedShort(); --remaining >= 0;) {
            /*
             * Each entry in the Image File Directory has the following format:
             *   - The tag that identifies the field (see constants in the Tags class).
             *   - The field type (see constants inherited from the GeoTIFF class).
             *   - The number of values of the indicated type.
             *   - The value, or the file offset to the value elswhere in the file.
             */
            final int   tag   = input.readUnsignedShort();
            final Type  type  = Type.valueOf(input.readShort());        // May be null.
            final long  count = readUnsignedInt();
            try {
                final long  size  = (type != null) ? Math.multiplyExact(type.size, count) : 0;
                if (size <= offsetSize) {
                    /*
                     * If the value can fit inside the number of bytes given by 'offsetSize', then the value is
                     * stored directly at that location. This is the most common way TIFF tag values are stored.
                     */
                    final long position = input.getStreamPosition();
                    if (size != 0) {
                        /*
                         * A size of zero means that we have an unknown type, in which case the TIFF specification
                         * recommends to ignore it (for allowing them to add new types in the future), or an entry
                         * without value (count = 0) - in principle illegal but we make this reader tolerant.
                         */
                        dir.addEntry(this, tag, type, count);
                    }
                    input.seek(position + offsetSize);
                } else {
                    // offset from beginning of file where the values are stored.
                    final long value = readUnsignedInt();
                    // TODO
                }
            } catch (IOException | ParseException | RuntimeException e) {
                owner.warning(errors().getString(Errors.Keys.CanNotSetPropertyValue_1, Tags.name(tag)), e);
            }
        }
        return imageFileDirectories.add(dir);
    }

    /**
     * Closes this reader.
     *
     * @throws IOException if an error occurred while closing this reader.
     */
    @Override
    public void close() throws IOException {
        input.channel.close();
    }
}
