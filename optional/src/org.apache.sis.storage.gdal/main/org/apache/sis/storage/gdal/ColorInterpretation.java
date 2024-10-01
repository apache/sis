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
package org.apache.sis.storage.gdal;

import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;


/**
 * Color interpretation for a band.
 * This enumeration mirrors the C/C++ {@code GDALColorInterp} enumeration in the GDAL's API.
 * The {@linkplain #ordinal() ordinal} values shall be equal to the GDAL enumeration values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum ColorInterpretation {
    /** Undefined.                     In GDAL: {@code GCI_Undefined      =  0}. */ UNDEFINED (-1),
    /** Greyscale.                     In GDAL: {@code GCI_GrayIndex      =  1}. */ GRAYSCALE (ColorSpace.TYPE_GRAY),
    /** Paletted   (uses color table). In GDAL: {@code GCI_PaletteIndex   =  2}. */ PALETTE   (-1),
    /** Red        band of ARGB image. In GDAL: {@code GCI_RedBand        =  3}. */ RED       (ColorSpace.TYPE_RGB),
    /** Green      band of ARGB image. In GDAL: {@code GCI_GreenBand      =  4}. */ GREEN     (ColorSpace.TYPE_RGB),
    /** Blue       band of ARGB image. In GDAL: {@code GCI_BlueBand       =  5}. */ BLUE      (ColorSpace.TYPE_RGB),
    /** Alpha (255=opaque).            In GDAL: {@code GCI_AlphaBand      =  6}. */ ALPHA     (ColorSpace.TYPE_RGB),
    /** Hue        band of HLS  image. In GDAL: {@code GCI_HueBand        =  7}. */ HUE       (ColorSpace.TYPE_HLS),
    /** Saturation band of HLS  image. In GDAL: {@code GCI_SaturationBand =  8}. */ SATURATION(ColorSpace.TYPE_HLS),
    /** Lightness  band of HLS  image. In GDAL: {@code GCI_LightnessBand  =  9}. */ LIGHTNESS (ColorSpace.TYPE_HLS),
    /** Cyan       band of CMYK image. In GDAL: {@code GCI_CyanBand       = 10}. */ CYAN      (ColorSpace.TYPE_CMYK),
    /** Magenta    band of CMYK image. In GDAL: {@code GCI_MagentaBand    = 11}. */ MAGENTA   (ColorSpace.TYPE_CMYK),
    /** Yellow     band of CMYK image. In GDAL: {@code GCI_YellowBand     = 12}. */ YELLOW    (ColorSpace.TYPE_CMYK),
    /** Black      band of CMYK image. In GDAL: {@code GCI_BlackBand      = 13}. */ BLACK     (ColorSpace.TYPE_CMYK),
    /** Y Luminance.                   In GDAL: {@code GCI_YCbCr_YBand    = 14}. */ LUMINANCE (ColorSpace.TYPE_YCbCr),
    /** Cb Chroma.                     In GDAL: {@code GCI_YCbCr_CbBand   = 15}. */ CB_CHROMA (ColorSpace.TYPE_YCbCr),
    /** Cr Chroma.                     In GDAL: {@code GCI_YCbCr_CrBand   = 16}. */ CR_CHROMA (ColorSpace.TYPE_YCbCr);

    /**
     * Java2D identifier of the color space as one of {@code ColorSpace.TYPE_*} constants.
     * The name of the color space type reflects the order of the component in a {@link ComponentColorModel}.
     * For example, for {@code TYPE_RGB}, index 0 corresponds to red, index 1 to green, and index 2 to blue.
     * The value of this field may be -1 if this enumeration cannot be mapped to a Java2D type of color space.
     */
    private final int colorSpaceType;

    /**
     * Creates a new enumeration value.
     *
     * @param  colorSpaceType  Java2D identifier of the color space as one of {@code ColorSpace.CS_*} constants.
     */
    private ColorInterpretation(final int colorSpaceType) {
        this.colorSpaceType = colorSpaceType;
    }

    /**
     * For fetching enumeration instances from ordinal values.
     */
    private static final ColorInterpretation[] VALUES = values();

    /**
     * Returns an enumeration instance from the given <abbr>GDAL</abbr> value.
     * If the given value is unknown to this method, then {@link #UNDEFINED} is returned.
     */
    static ColorInterpretation valueOf(final int ordinal) {
        return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : UNDEFINED;
    }
}
