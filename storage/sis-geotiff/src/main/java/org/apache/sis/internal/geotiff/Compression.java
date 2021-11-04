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
package org.apache.sis.internal.geotiff;


/**
 * Possible values for {@link org.apache.sis.storage.geotiff.Tags#Compression}.
 * Data compression applies only to raster image data. All other TIFF fields are unaffected.
 *
 * <p>Except otherwise noted, field names in this class are upper-case variant of the names
 * used in Web Coverage Service (WCS) as specified in the following specification:</p>
 *
 * <blockquote>OGC 12-100: GML Application Schema - Coverages - GeoTIFF Coverage Encoding Profile</blockquote>
 *
 * The main exception is {@code CCITT}, which has different name in WCS query and response.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.8
 * @module
 */
public enum Compression {
    /**
     * No compression, but pack data into bytes as tightly as possible, leaving no unused bits except
     * potentially at the end of rows. The component values are stored as an array of type byte.
     * <ul>
     *   <li>Name in WCS query:    "None"</li>
     *   <li>Name in WCS response: "None"</li>
     * </ul>
     */
    NONE(1),

    /**
     * CCITT Group 3, 1-Dimensional Modified Huffman run length encoding.
     * <ul>
     *   <li>Name in WCS query:    "Huffman"</li>
     *   <li>Name in WCS response: "CCITTRLE"</li>
     * </ul>
     */
    CCITTRLE(2),

    /**
     * PackBits compression, a simple byte-oriented run length scheme.
     * <ul>
     *   <li>Name in WCS query:    "PackBits"</li>
     *   <li>Name in WCS response: "PackBits"</li>
     * </ul>
     */
    PACKBITS(32773),

    // ---- End of baseline GeoTIFF. Remaining are extensions cited by OGC standard ----

    /**
     * LZW compression.
     * <ul>
     *   <li>Name in WCS query:    "LZW"</li>
     *   <li>Name in WCS response: "LZW"</li>
     * </ul>
     */
    LZW(5),

    /**
     * Deflate compression, like ZIP format. This is sometime named {@code "ADOBE_DEFLATE"},
     * withe the {@code "DEFLATE"} name used for another compression method with code 32946.
     * <ul>
     *   <li>Name in WCS query:    "Deflate"</li>
     *   <li>Name in WCS response: "Deflate"</li>
     *   <li>Other name:           "ADOBE_DEFLATE"</li>
     * </ul>
     */
    DEFLATE(8),

    /**
     * JPEG compression.
     * <ul>
     *   <li>Name in WCS query:    "JPEG"</li>
     *   <li>Name in WCS response: "JPEG"</li>
     *   <li>Name of old JPEG:     "OJPEG" (code 6)</li>
     * </ul>
     */
    JPEG(7),

    // ---- Remaining are extension to both baseline and OGC standard ----

    /** Unsupported. */ CCITTFAX3(3),
    /** Unsupported. */ CCITTFAX4(4),
    /** Unsupported. */ NEXT(32766),
    /** Unsupported. */ CCITTRLEW(32771),
    /** Unsupported. */ THUNDERSCAN(32809),
    /** Unsupported. */ IT8CTPAD(32895),
    /** Unsupported. */ IT8LW(32896),
    /** Unsupported. */ IT8MP(32897),
    /** Unsupported. */ IT8BL(32898),
    /** Unsupported. */ PIXARFILM(32908),
    /** Unsupported. */ PIXARLOG(32909),
    /** Unsupported. */ DCS(32947),
    /** Unsupported. */ JBIG(34661),
    /** Unsupported. */ SGILOG(34676),
    /** Unsupported. */ SGILOG24(34677),
    /** Unsupported. */ JP2000(34712),

    /**
     * Sentinel value for unknown projection.
     */
    UNKNOWN(0);

    /**
     * The TIFF code for this compression.
     */
    final int code;

    /**
     * Creates a new compression enumeration.
     */
    private Compression(final int code) {
        this.code = code;
    }

    /**
     * Returns the compression method for the given GeoTIFF code, or {@code UNKNOWN} if none.
     *
     * @param  code  the TIFF code for which to get a compression enumeration value.
     * @return enumeration value for the given code, or {@link #UNKNOWN} if none.
     */
    public static Compression valueOf(final int code) {
        switch (code) {
            case 1:     return NONE;
            case 2:     return CCITTRLE;
            case 5:     return LZW;
            case 6:     // "old-style" JPEG, later overriden in Technical Notes 2.
            case 7:     return JPEG;
            case 8:
            case 32946: return DEFLATE;
            case 32773: return PACKBITS;
            default: {
                // Fallback for uncommon formats.
                for (final Compression c : values()) {
                    if (c.code == code) return c;
                }
                break;
            }
        }
        return UNKNOWN;
    }
}
