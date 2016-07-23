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


/**
 * Possible values for {@link Tags#Compression}.
 * Data compression applies only to raster image data. All other TIFF fields are unaffected.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
enum Compression {
    /**
     * No compression, but pack data into bytes as tightly as possible, leaving no unused bits except
     * potentially at the end of rows. The component values are stored as an array of type byte.
     */
    NONE(1),

    /**
     * CCITT Group 3, 1-Dimensional Modified Huffman run length encoding.
     */
    CCITT(2),

    /**
     * PackBits compression, a simple byte-oriented run length scheme.
     */
    PACK_BITS(32773),

    // ---- End of baseline GeoTIFF. Remaining are extensions ----

    /**
     * LZW compression.
     */
    LZW(5),

    /**
     * Deflate compression, like ZIP format.
     */
    DEFLATE(8),

    /**
     * JPEG compression.
     */
    JPEG(6),

    /**
     * JPEG compression.
     * @todo what is the difference with JPEG?
     */
    JPEG_2(7);

    /**
     * The TIFF code for this compression.
     */
    final int code;

    /**
     * Creates a new compression enumeration.
     */
    Compression(final int code) {
        this.code = code;
    }

    /**
     * Returns the compression method for the given GeoTIFF code, or {@code null} if none.
     */
    static Compression valueOf(final long code) {
        if ((code & ~0xFFFF) == 0) {                // Should be a short according TIFF specification.
            switch ((int) code) {
                case 1:     return NONE;
                case 2:     return CCITT;
                case 5:     return LZW;
                case 6:     return JPEG;
                case 7:     return JPEG_2;
                case 8:     return DEFLATE;
                case 32773: return PACK_BITS;
            }
        }
        return null;
    }
}
