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

    static final int NewSubfileType              = 0x00FE;
    static final int SubfileType                 = 0x00FF;
    static final int ImageWidth                  = 0x0100;
    static final int ImageLength                 = 0x0101;
    static final int BitsPerSample               = 0x0102;
    static final int Compression                 = 0x0103;
    static final int PhotometricInterpretation   = 0x0106;
    static final int Threshholding               = 0x0107;
    static final int CellWidth                   = 0x0108;
    static final int CellLength                  = 0x0109;
    static final int FillOrder                   = 0x010A;
    static final int DocumentName                = 0x010D;
    static final int ImageDescription            = 0x010E;
    static final int Make                        = 0x010F;
    static final int Model                       = 0x0110;
    static final int StripOffsets                = 0x0111;
    static final int Orientation                 = 0x0112;
    static final int SamplesPerPixel             = 0x0115;
    static final int RowsPerStrip                = 0x0116;
    static final int StripByteCounts             = 0x0117;
    static final int MinSampleValue              = 0x0118;
    static final int MaxSampleValue              = 0x0119;
    static final int XResolution                 = 0x011A;
    static final int YResolution                 = 0x011B;
    static final int PlanarConfiguration         = 0x011C;
    static final int PageName                    = 0x011D;
    static final int XPosition                   = 0x011E;
    static final int YPosition                   = 0x011F;
    static final int FreeOffsets                 = 0x0120;
    static final int FreeByteCounts              = 0x0121;
    static final int GrayResponseUnit            = 0x0122;
    static final int GrayResponseCurve           = 0x0123;
    static final int T4Options                   = 0x0124;
    static final int T6Options                   = 0x0125;
    static final int ResolutionUnit              = 0x0128;
    static final int PageNumber                  = 0x0129;
    static final int TransferFunction            = 0x012D;
    static final int Software                    = 0x0131;
    static final int DateTime                    = 0x0132;
    static final int DateTimeOriginal            = 0x9003;
    static final int DateTimeDigitized           = 0x9004;
    static final int Artist                      = 0x013B;
    static final int HostComputer                = 0x013C;
    static final int Predictor                   = 0x013D;
    static final int WhitePoint                  = 0x013E;
    static final int PrimaryChromaticities       = 0x013F;
    static final int ColorMap                    = 0x0140;
    static final int HalftoneHints               = 0x0141;
    static final int TileWidth                   = 0x0142;
    static final int TileLength                  = 0x0143;
    static final int TileOffsets                 = 0x0144;
    static final int TileByteCounts              = 0x0145;
    static final int InkSet                      = 0x014C;
    static final int InkNames                    = 0x014D;
    static final int NumberOfInks                = 0x014E;
    static final int DotRange                    = 0x0150;
    static final int TargetPrinter               = 0x0151;
    static final int ExtraSamples                = 0x0152;
    static final int SampleFormat                = 0x0153;
    static final int SMinSampleValue             = 0x0154;
    static final int SMaxSampleValue             = 0x0155;
    static final int TransferRange               = 0x0156;
    static final int JPEGProc                    = 0x0200;
    static final int JPEGInterchangeFormat       = 0x0201;
    static final int JPEGInterchangeFormatLength = 0x0202;
    static final int JPEGRestartInterval         = 0x0203;
    static final int JPEGLosslessPredictors      = 0x0205;
    static final int JPEGPointTransforms         = 0x0206;
    static final int JPEGQTables                 = 0x0207;
    static final int JPEGDCTables                = 0x0208;
    static final int JPEGACTables                = 0x0209;
    static final int YCbCrCoefficients           = 0x0211;
    static final int YCbCrSubSampling            = 0x0212;
    static final int YCbCrPositioning            = 0x0213;
    static final int ReferenceBlackWhite         = 0x0214;
    static final int Copyright                   = 0x8298;



    /////////////////////////////////////////////////////////
    //                 GDAL EXTENSION TAGS                 //
    /////////////////////////////////////////////////////////

    static final int GDAL_METADATA = 42112;  // http://www.awaresystems.be/imaging/tiff/tifftags/gdal_metadata.html
    static final int GDAL_NODATA   = 42113;  // http://www.awaresystems.be/imaging/tiff/tifftags/gdal_nodata.html

    /**
     * Do not allow instantiation of this class.
     */
    private Tags() {
    }
}
