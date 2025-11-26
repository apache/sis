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
package org.apache.sis.storage.geotiff.base;

import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.*;


/**
 * Possible values for {@code BaselineTIFFTagSet.TAG_COMPRESSION}.
 * Data compression applies only to raster image data. All other TIFF fields are unaffected.
 *
 * <p>Except otherwise noted, field names in this class are upper-case variant of the names
 * used in Web Coverage Service (WCS) as specified in the following specification:</p>
 *
 * <blockquote>OGC 12-100: GML Application Schema - Coverages - GeoTIFF Coverage Encoding Profile</blockquote>
 *
 * The main exception is {@code CCITT}, which has different name in WCS query and response.
 *
 * <p>This enumeration contains a relatively large number of compressions in order to put a name
 * on the numerical codes that the reader may find. However the Apache SIS reader and writer do
 * not support all those compressions. This enumeration is not put in public API for that reason.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
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
    NONE(COMPRESSION_NONE),

    /**
     * CCITT Group 3, 1-Dimensional Modified Huffman run length encoding.
     * <ul>
     *   <li>Name in WCS query:    "Huffman"</li>
     *   <li>Name in WCS response: "CCITTRLE"</li>
     * </ul>
     */
    CCITTRLE(COMPRESSION_CCITT_RLE),

    /**
     * PackBits compression, a simple byte-oriented run length scheme.
     * <ul>
     *   <li>Name in WCS query:    "PackBits"</li>
     *   <li>Name in WCS response: "PackBits"</li>
     * </ul>
     */
    PACKBITS(COMPRESSION_PACKBITS),

    // ---- End of baseline GeoTIFF. Remaining are extensions cited by OGC standard ----

    /**
     * LZW compression.
     * <ul>
     *   <li>Name in WCS query:    "LZW"</li>
     *   <li>Name in WCS response: "LZW"</li>
     * </ul>
     */
    LZW(COMPRESSION_LZW),

    /**
     * Deflate compression, like ZIP format. This is sometimes named {@code "ADOBE_DEFLATE"},
     * with the {@code "DEFLATE"} name used for another compression method with code 32946.
     * <ul>
     *   <li>Name in WCS query:    "Deflate"</li>
     *   <li>Name in WCS response: "Deflate"</li>
     *   <li>Other name:           "ADOBE_DEFLATE"</li>
     * </ul>
     */
    DEFLATE(COMPRESSION_ZLIB),

    /**
     * JPEG compression.
     * <ul>
     *   <li>Name in WCS query:    "JPEG"</li>
     *   <li>Name in WCS response: "JPEG"</li>
     *   <li>Name of old JPEG:     "OJPEG" (code 6)</li>
     * </ul>
     */
    JPEG(COMPRESSION_JPEG),

    // ---- Remaining are extension to both baseline and OGC standard ----

    /** Unsupported. */ CCITTFAX3(COMPRESSION_CCITT_T_4),
    /** Unsupported. */ CCITTFAX4(COMPRESSION_CCITT_T_6),
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
    public final int code;

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
            case COMPRESSION_DEFLATE:   // Fall through
            case COMPRESSION_ZLIB:      return DEFLATE;
            case COMPRESSION_OLD_JPEG:  // "old-style" JPEG, later overriden in Technical Notes 2.
            case COMPRESSION_JPEG:      return JPEG;
            case COMPRESSION_CCITT_RLE: return CCITTRLE;
            case COMPRESSION_LZW:       return LZW;
            case COMPRESSION_PACKBITS:  return PACKBITS;
            case COMPRESSION_NONE:      return NONE;
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

    /**
     * Whether the decompression uses native library.
     * In such case, the use of direct buffer may be more efficient.
     *
     * @return whether the compression may use a native library.
     */
    public final boolean useNativeLibrary() {
        return this == DEFLATE;
    }

    /**
     * Returns whether the compression can be configured with different levels.
     */
    public final boolean supportLevels() {
        return this == DEFLATE;
    }
}
