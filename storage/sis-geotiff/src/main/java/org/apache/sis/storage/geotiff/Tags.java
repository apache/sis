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

import java.lang.reflect.Field;


/**
 * Numerical values of GeoTIFF tags, as <strong>unsigned</strong> short integers.
 * In this class, field names are identical to TIFF tag names.
 * For that reason, many of those field names do not follow usual Java convention for constants.
 *
 * <p>A useful (but unofficial) reference is the
 * <a href="http://www.awaresystems.be/imaging/tiff/tifftags.html">TIFF Tag Reference</a> page.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class Tags {

    //////////////////////////////////////////////////////////
    //                  BASELINE TIFF TAGS                  //
    //////////////////////////////////////////////////////////

    public static final short NewSubfileType              = 0x00FE;
    public static final short SubfileType                 = 0x00FF;
    public static final short ImageWidth                  = 0x0100;
    public static final short ImageLength                 = 0x0101;
    public static final short BitsPerSample               = 0x0102;
    public static final short Compression                 = 0x0103;
    public static final short PhotometricInterpretation   = 0x0106;
    public static final short Threshholding               = 0x0107;
    public static final short CellWidth                   = 0x0108;
    public static final short CellLength                  = 0x0109;
    public static final short FillOrder                   = 0x010A;
    public static final short DocumentName                = 0x010D;
    public static final short ImageDescription            = 0x010E;
    public static final short Make                        = 0x010F;
    public static final short Model                       = 0x0110;
    public static final short StripOffsets                = 0x0111;
    public static final short Orientation                 = 0x0112;
    public static final short SamplesPerPixel             = 0x0115;
    public static final short RowsPerStrip                = 0x0116;
    public static final short StripByteCounts             = 0x0117;
    public static final short MinSampleValue              = 0x0118;
    public static final short MaxSampleValue              = 0x0119;
    public static final short XResolution                 = 0x011A;
    public static final short YResolution                 = 0x011B;
    public static final short PlanarConfiguration         = 0x011C;
    public static final short PageName                    = 0x011D;
    public static final short XPosition                   = 0x011E;
    public static final short YPosition                   = 0x011F;
    public static final short FreeOffsets                 = 0x0120;
    public static final short FreeByteCounts              = 0x0121;
    public static final short GrayResponseUnit            = 0x0122;
    public static final short GrayResponseCurve           = 0x0123;
    public static final short T4Options                   = 0x0124;
    public static final short T6Options                   = 0x0125;
    public static final short ResolutionUnit              = 0x0128;
    public static final short PageNumber                  = 0x0129;
    public static final short TransferFunction            = 0x012D;
    public static final short Software                    = 0x0131;
    public static final short DateTime                    = 0x0132;
    public static final short DateTimeOriginal    = (short) 0x9003;
    public static final short DateTimeDigitized   = (short) 0x9004;
    public static final short Artist                      = 0x013B;
    public static final short HostComputer                = 0x013C;
    public static final short Predictor                   = 0x013D;
    public static final short WhitePoint                  = 0x013E;
    public static final short PrimaryChromaticities       = 0x013F;
    public static final short ColorMap                    = 0x0140;
    public static final short HalftoneHints               = 0x0141;
    public static final short TileWidth                   = 0x0142;
    public static final short TileLength                  = 0x0143;
    public static final short TileOffsets                 = 0x0144;
    public static final short TileByteCounts              = 0x0145;
    public static final short InkSet                      = 0x014C;
    public static final short InkNames                    = 0x014D;
    public static final short NumberOfInks                = 0x014E;
    public static final short DotRange                    = 0x0150;
    public static final short TargetPrinter               = 0x0151;
    public static final short ExtraSamples                = 0x0152;
    public static final short SampleFormat                = 0x0153;
    public static final short SMinSampleValue             = 0x0154;
    public static final short SMaxSampleValue             = 0x0155;
    public static final short TransferRange               = 0x0156;
    public static final short JPEGProc                    = 0x0200;
    public static final short JPEGInterchangeFormat       = 0x0201;
    public static final short JPEGInterchangeFormatLength = 0x0202;
    public static final short JPEGRestartInterval         = 0x0203;
    public static final short JPEGLosslessPredictors      = 0x0205;
    public static final short JPEGPointTransforms         = 0x0206;
    public static final short JPEGQTables                 = 0x0207;
    public static final short JPEGDCTables                = 0x0208;
    public static final short JPEGACTables                = 0x0209;
    public static final short YCbCrCoefficients           = 0x0211;
    public static final short YCbCrSubSampling            = 0x0212;
    public static final short YCbCrPositioning            = 0x0213;
    public static final short ReferenceBlackWhite         = 0x0214;
    public static final short Copyright           = (short) 0x8298;


    /////////////////////////////////////////////////////////
    //                 GDAL EXTENSION TAGS                 //
    /////////////////////////////////////////////////////////

    /**
     * holds an XML list of name=value 'metadata' values about the image as a whole, and about specific samples.
     *
     * @see <a href="http://www.awaresystems.be/imaging/tiff/tifftags/gdal_metadata.html">TIFF Tag GDAL_METADATA</a>
     */
    public static final short GDAL_METADATA = (short) 0xA480;             // 42112

    /**
     * Contains an ASCII encoded nodata or background pixel value.
     *
     * @see <a href="http://www.awaresystems.be/imaging/tiff/tifftags/gdal_nodata.html">TIFF Tag GDAL_NODATA</a>
     */
    public static final short GDAL_NODATA = (short) 0xA481;               // 42113


    /////////////////////////////////////////////////////////
    //                 GEOTIFF EXTENSION TAGS              //
    /////////////////////////////////////////////////////////

    /**
     * References all "GeoKeys" needed for building the Coordinate Reference System.
     * GeoTIFF keys are stored in a kind of directory inside the TIFF directory, with
     * the keys enumerated in the {@link CRSBuilder} class.
     *
     * @see GeoKeys
     */
    public static final short GeoKeyDirectory = (short) 0x87AF;           // 34735

    /**
     * References all {@code double} values referenced by the {@link GeoKeys}.
     * The keys are stored in the entry referenced by {@link #GeoKeyDirectory}.
     */
    public static final short GeoDoubleParams = (short) 0x87B0;           // 34736

    /**
     * References all {@link String} values referenced by the {@link GeoKeys}.
     * The keys are stored in the entry referenced by {@link #GeoKeyDirectory}.
     */
    public static final short GeoAsciiParams = (short) 0x87B1;            // 34737

    /**
     * Do not allow instantiation of this class.
     */
    private Tags() {
    }

    /**
     * Returns the name of the given tag. Implementation of this method is inefficient,
     * but it should rarely be invoked (mostly for formatting error messages).
     */
    static String name(final short tag) {
        try {
            for (final Field field : Tags.class.getFields()) {
                if (field.getType() == Short.TYPE) {
                    if (field.getShort(null) == tag) {
                        return field.getName();
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);        // Should never happen because we asked only for public fields.
        }
        return Integer.toHexString(Short.toUnsignedInt(tag));
    }
}
