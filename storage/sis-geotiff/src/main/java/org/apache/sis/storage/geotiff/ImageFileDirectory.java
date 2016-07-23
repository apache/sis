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
import org.apache.sis.internal.storage.ChannelDataInput;


/**
 * An Image File Directory (FID) in a TIFF image.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class ImageFileDirectory {
    /**
     * The offset (in bytes since the beginning of the TIFF stream) of the Image File Directory (FID).
     * The directory may be at any location in the file after the header.
     * In particular, an Image File Directory may follow the image that it describes.
     */
    final long offset;

    /**
     * The size of the image described by this FID, or -1 if the information has not been found.
     * The image may be much bigger than the memory capacity, in which case the image shall be tiled.
     */
    private long imageWidth = -1, imageHeight = -1;

    /**
     * The size of each tile, or -1 if the information has not be found.
     * Tiles should be small enough for fitting in memory.
     */
    private int tileWidth = -1, tileHeight = -1;

    private int samplesPerPixel;

    /**
     * The compression method, or {@code null} if unknown. If the compression method is unknown
     * or unsupported we can not read the image, but we still can read the metadata.
     */
    private Compression compression;

    /**
     * Creates a new image file directory located at the given offset (in bytes) in the TIFF file.
     */
    ImageFileDirectory(final long offset) {
        this.offset = offset;
    }

    /**
     * Adds the value read from the current position in the given stream
     * for the entry identified by the given GeoTIFF tag.
     */
    void addEntry(final ChannelDataInput input, final int tag, final Type type, final long count) throws IOException {
        switch (tag) {
            case Tags.PhotometricInterpretation: {
                final boolean blackIsZero = (type.readLong(input, count) != 0);
                // TODO
                break;
            }
            case Tags.Compression: {
                compression = Compression.valueOf(type.readLong(input, count));
                break;
            }
            case Tags.ImageLength: {
                imageHeight = type.readUnsignedLong(input, count);
                break;
            }
            case Tags.ImageWidth: {
                imageWidth = type.readUnsignedLong(input, count);
                break;
            }
        }
    }
}
