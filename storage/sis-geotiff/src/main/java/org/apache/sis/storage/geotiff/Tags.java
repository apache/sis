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
 * Numerical values of GeoTIFF tags. In this class, field names are identical to TIFF tag names.
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

    public static final int NewSubfileType              = 0x00FE;
    public static final int SubfileType                 = 0x00FF;
    public static final int ImageWidth                  = 0x0100;
    public static final int ImageLength                 = 0x0101;
    public static final int BitsPerSample               = 0x0102;
    public static final int Compression                 = 0x0103;
    public static final int PhotometricInterpretation   = 0x0106;
    public static final int Threshholding               = 0x0107;
    public static final int CellWidth                   = 0x0108;
    public static final int CellLength                  = 0x0109;
    public static final int FillOrder                   = 0x010A;
    public static final int DocumentName                = 0x010D;
    public static final int ImageDescription            = 0x010E;
    public static final int Make                        = 0x010F;
    public static final int Model                       = 0x0110;
    public static final int StripOffsets                = 0x0111;
    public static final int Orientation                 = 0x0112;
    public static final int SamplesPerPixel             = 0x0115;
    public static final int RowsPerStrip                = 0x0116;
    public static final int StripByteCounts             = 0x0117;
    public static final int MinSampleValue              = 0x0118;
    public static final int MaxSampleValue              = 0x0119;
    public static final int XResolution                 = 0x011A;
    public static final int YResolution                 = 0x011B;
    public static final int PlanarConfiguration         = 0x011C;
    public static final int PageName                    = 0x011D;
    public static final int XPosition                   = 0x011E;
    public static final int YPosition                   = 0x011F;
    public static final int FreeOffsets                 = 0x0120;
    public static final int FreeByteCounts              = 0x0121;
    public static final int GrayResponseUnit            = 0x0122;
    public static final int GrayResponseCurve           = 0x0123;
    public static final int T4Options                   = 0x0124;
    public static final int T6Options                   = 0x0125;
    public static final int ResolutionUnit              = 0x0128;
    public static final int PageNumber                  = 0x0129;
    public static final int TransferFunction            = 0x012D;
    public static final int Software                    = 0x0131;
    public static final int DateTime                    = 0x0132;
    public static final int DateTimeOriginal            = 0x9003;
    public static final int DateTimeDigitized           = 0x9004;
    public static final int Artist                      = 0x013B;
    public static final int HostComputer                = 0x013C;
    public static final int Predictor                   = 0x013D;
    public static final int WhitePoint                  = 0x013E;
    public static final int PrimaryChromaticities       = 0x013F;
    public static final int ColorMap                    = 0x0140;
    public static final int HalftoneHints               = 0x0141;
    public static final int TileWidth                   = 0x0142;
    public static final int TileLength                  = 0x0143;
    public static final int TileOffsets                 = 0x0144;
    public static final int TileByteCounts              = 0x0145;
    public static final int InkSet                      = 0x014C;
    public static final int InkNames                    = 0x014D;
    public static final int NumberOfInks                = 0x014E;
    public static final int DotRange                    = 0x0150;
    public static final int TargetPrinter               = 0x0151;
    public static final int ExtraSamples                = 0x0152;
    public static final int SampleFormat                = 0x0153;
    public static final int SMinSampleValue             = 0x0154;
    public static final int SMaxSampleValue             = 0x0155;
    public static final int TransferRange               = 0x0156;
    public static final int JPEGProc                    = 0x0200;
    public static final int JPEGInterchangeFormat       = 0x0201;
    public static final int JPEGInterchangeFormatLength = 0x0202;
    public static final int JPEGRestartInterval         = 0x0203;
    public static final int JPEGLosslessPredictors      = 0x0205;
    public static final int JPEGPointTransforms         = 0x0206;
    public static final int JPEGQTables                 = 0x0207;
    public static final int JPEGDCTables                = 0x0208;
    public static final int JPEGACTables                = 0x0209;
    public static final int YCbCrCoefficients           = 0x0211;
    public static final int YCbCrSubSampling            = 0x0212;
    public static final int YCbCrPositioning            = 0x0213;
    public static final int ReferenceBlackWhite         = 0x0214;
    public static final int Copyright                   = 0x8298;



    /////////////////////////////////////////////////////////
    //                 GDAL EXTENSION TAGS                 //
    /////////////////////////////////////////////////////////

    public static final int GDAL_METADATA = 42112;  // http://www.awaresystems.be/imaging/tiff/tifftags/gdal_metadata.html
    public static final int GDAL_NODATA   = 42113;  // http://www.awaresystems.be/imaging/tiff/tifftags/gdal_nodata.html


    /////////////////////////////////////////////////////////
    //                 GEOTIFF EXTENSION TAGS              //
    /////////////////////////////////////////////////////////

    //------------------------------- CRS ------------------------------------//
    /**
     * References the needed "GeoKeys" to build CRS.
     */
    public static final int GeoKeyDirectoryTag = 0x87AF; //-- 34735

    /**
     * This tag is used to store all of the DOUBLE valued GeoKeys, referenced by the GeoKeyDirectoryTag.
     */
    public static final int GeoDoubleParamsTag = 0x87B0; //-- 34736

    /**
     * This tag is used to store all of the ASCII valued GeoKeys, referenced by the GeoKeyDirectoryTag.
     */
    public static final int GeoAsciiParamsTag = 0x87B1; //-- 34737

    static final class CRSKeys {

    }





    /**
     * Do not allow instantiation of this class.
     */
    private Tags() {
    }

    /**
     * Returns the name of the given tag. Implementation of this method is inefficient,
     * but it should rarely be invoked (mostly for formatting error messages).
     */
    static String name(final int tag) {
        try {
            for (final Field field : Tags.class.getFields()) {
                if (field.getType() == Integer.TYPE) {
                    if (field.getInt(null) == tag) {
                        return field.getName();
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);        // Should never happen because we asked only for public fields.
        }
        return Integer.toHexString(tag);
    }
}
