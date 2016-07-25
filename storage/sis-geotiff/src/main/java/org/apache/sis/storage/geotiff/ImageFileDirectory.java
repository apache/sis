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
     * If {@code true}, the components are stored in separate “component planes”.
     * The default is {@code false}, which stands for the "chunky" format
     * (for example RGB data stored as RGBRGBRGB).
     */
    private boolean isPlanar;

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
     *
     * @param  reader   the input stream to read, the metadata and the character encoding.
     * @param  tag      the GeoTIFF tag to decode.
     * @param  type     the GeoTIFF type of the value to read.
     * @param  count    the number of values to read.
     * @return {@code true} on success, or {@code false} for unrecognized value.
     * @throws IOException if an error occurred while reading the stream.
     * @throws NumberFormatException if the value was stored in ASCII and can not be parsed.
     * @throws ArithmeticException if the value can not be represented in the expected Java type.
     * @throws IllegalArgumentException if a value which was expected to be a singleton is not.
     * @throws UnsupportedOperationException if the given type is {@link Type#UNDEFINED}.
     */
    boolean addEntry(final Reader reader, final int tag, final Type type, final long count) throws IOException {
        switch (tag) {
            /*
             * Person who created the image. Some older TIFF files used this tag for storing
             * Copyright information, but Apache SIS does not support this legacy practice.
             */
            case Tags.Artist: {
                for (final String value : type.readString(reader.input, count, reader.encoding)) {
                    reader.metadata.addAuthorName(value);
                }
                break;
            }
            /*
             * Number of bits per component. The array length should be the number of components in a
             * pixel (e.g. 3 for RGB values). Typically, all components have the same number of bits.
             * But the TIFF specification allows different values.
             */
            case Tags.BitsPerSample: {
                // TODO
                break;
            }
            /*
             * The height of the dithering or halftoning matrix used to create a dithered or halftoned
             * bilevel file. Meaningful only if Threshholding = 2.
             */
            case Tags.CellLength: {
                // TODO
                break;
            }
            /*
             * The width of the dithering or halftoning matrix used to create a dithered or halftoned
             * bilevel file. Meaningful only if Threshholding = 2.
             */
            case Tags.CellWidth: {
                // TODO
                break;
            }
            /*
             * The Red-Green-Blue lookup table map for palette-color images (IndexColorModel in Java2D).
             * All the Red values come first, followed by the Green values, then the Blue values.
             * The number of values for each color is (1 << BitsPerSample) and the type of each value is USHORT.
             * 0 represents the minimum intensity (black is 0,0,0) and 65535 represents the maximum intensity.
             * If the Indexed tag has value 1 and PhotometricInterpretation is different from PaletteColor,
             * then the color space may be different than RGB.
             */
            case Tags.ColorMap: {
                // TODO
                break;
            }
            /*
             * Compression scheme used on the image data.
             */
            case Tags.Compression: {
                final long value = type.readLong(reader.input, count);
                compression = Compression.valueOf(value);
                if (compression == null) {
                    return false;
                }
                break;
            }
            /*
             * Copyright notice of the person or organization that claims the copyright to the image.
             */
            case Tags.Copyright: {
                for (final String value : type.readString(reader.input, count, reader.encoding)) {
                    reader.metadata.parseLegalNotice(value);
                }
                break;
            }
            case Tags.PlanarConfiguration: {
                final long value = type.readLong(reader.input, count);
                if (value < 1 || value > 2) {
                    return false;
                }
                isPlanar = (value == 2);
                break;
            }
            case Tags.PhotometricInterpretation: {
                final boolean blackIsZero = (type.readLong(reader.input, count) != 0);
                // TODO
                break;
            }
            case Tags.ImageLength: {
                imageHeight = type.readUnsignedLong(reader.input, count);
                break;
            }
            case Tags.ImageWidth: {
                imageWidth = type.readUnsignedLong(reader.input, count);
                break;
            }
        }
        return true;
    }
}
