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
import org.apache.sis.internal.storage.ChannelDataInput;
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
    private final ChannelDataInput input;

    /**
     * Stream position of the first byte of the GeoTIFF file. This is usually zero.
     */
    private final long origin;

    /**
     * {@code true} if the file uses the BigTIFF format, or (@code false} for standard TIFF.
     */
    private final boolean isBigTIFF;

    /**
     * Positions of each <cite>Image File Directory</cite> (IFD) in this file. Those positions are fetched
     * when first needed.
     */
    private final List<ImageFileDirectory> imageFileDirectories = new ArrayList<>();

    /**
     * Creates a new GeoTIFF reader which will read data from the given input.
     * The input must be at the beginning of the GeoTIFF file.
     *
     * @throws IOException if an error occurred while reading bytes from the stream.
     * @throws DataStoreException if the file is not encoded in the TIFF or BigTIFF format.
     */
    Reader(final ChannelDataInput input) throws IOException, DataStoreException {
        this.input = input;
        origin = input.getStreamPosition();
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
            final short magicNumber = input.readShort();
            if (isBigTIFF = (magicNumber == 43)) {
                if (input.readShort() == Long.BYTES && input.readShort() == 0) {
                    return;
                }
            } else if (magicNumber == 42) {
                return;
            }
        }
        throw new DataStoreException(Errors.format(Errors.Keys.UnexpectedFileFormat_2, "TIFF", input.filename));
    }

    /**
     * Reads the {@code int} or {@code long} value (depending if the file is
     * a standard of big TIFF) at the current {@linkplain #input} position.
     *
     * @return The next pointer value.
     */
    private long readPointer() throws IOException {
        return isBigTIFF ? input.readLong() : input.readUnsignedInt();
    }

    /**
     * Reads the {@code short} or {@code long} value (depending if the file is
     * standard of big TIFF) at the current {@linkplain #input} position.
     *
     * @return The next directory entry value.
     */
    final long readDirectoryEntry() throws IOException {
        return isBigTIFF ? input.readLong() : input.readUnsignedShort();
    }

    /**
     * Reads the next bytes in the {@linkplain #input}, which must be the 32 or 64 bits offset to the
     * next <cite>Image File Directory</cite> (IFD).
     *
     * @return {@code true} if we found a new IFD, or {@code false} if there is no more IFD.
     */
    private boolean readImageFileDirectory() throws IOException {
        final long offset = readPointer();
        if (offset == 0) {
            return false;
        }
        input.seek(offset);
        return imageFileDirectories.add(new ImageFileDirectory(this, offset));
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
