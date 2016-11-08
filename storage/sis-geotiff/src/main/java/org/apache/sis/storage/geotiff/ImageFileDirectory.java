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

import java.util.Locale;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.measure.Unit;
import org.opengis.metadata.citation.DateType;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;


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
 *
 * @see <a href="http://www.awaresystems.be/imaging/tiff/tifftags.html">TIFF Tag Reference</a>
 */
final class ImageFileDirectory {

    /**
     * Default {@link Unit} adapted to ISO 19115 metadata.
     */
    private static final Unit METER = Units.METRE;
    /**
     * {@code true} if this {@code ImageFileDirectory} has not yet read all deferred entries.
     * When this flag is {@code true}, the {@code ImageFileDirectory} is not yet ready for use.
     */
    boolean hasDeferredEntries;

    /**
     * The size of the image described by this FID, or -1 if the information has not been found.
     * The image may be much bigger than the memory capacity, in which case the image shall be tiled.
     *
     * <p><b>Note:</b>
     * the {@link #imageHeight} attribute is named {@code ImageLength} in TIFF specification.</p>
     */
    private long imageWidth = -1, imageHeight = -1;

    /**
     * The width of each tile, or -1 if the information has not be found.
     * This is the number of columns in each tile.
     * Tiles should be small enough for fitting in memory.
     *
     * Assuming integer arithmetic, three computed values that are useful in the following field descriptions are:
     * {@preformat math
     * TilesAcross = (ImageWidth + TileWidth - 1) / TileWidth
     * TilesDown = (ImageLength + TileLength - 1) / TileLength
     * TilesPerImage = TilesAcross * TilesDown
     * }
     * These computed values are not TIFF fields; they are simply values determined by
     * the ImageWidth, TileWidth, ImageLength, and TileLength fields.
     * TileWidth must be a multiple of 16. This restriction improves performance
     * in some graphics environments and enhances compatibility with compression schemes such as JPEG.
     * Tiles need not be square.
     * Note that ImageWidth can be less than TileWidth, although this means that the tiles
     * are too large or that you are using tiling on really small images, neither of which is recommended.
     * The same observation holds for ImageLength and TileLength.
     */
    private long tileWidth = -1;

    /**
     * The size of each tile, or -1 if the information has not be found.
     * This is the number of rows in each tile.
     * Tiles should be small enough for fitting in memory.
     *
     * Assuming integer arithmetic, three computed values that are useful in the following field descriptions are:
     * {@preformat math
     *   TilesAcross = (ImageWidth + TileWidth - 1) / TileWidth
     *     TilesDown = (ImageLength + TileLength - 1) / TileLength
     * TilesPerImage = TilesAcross * TilesDown
     * }
     * These computed values are not TIFF fields; they are simply values determined by
     * the ImageWidth, TileWidth, ImageLength, and TileLength fields.
     * TileLength must be a multiple of 16. This restriction improves performance
     * in some graphics environments and enhances compatibility with compression schemes such as JPEG.
     * Tiles need not be square
     *
     * <p><b>Note:</b>
     * the {@link #tileHeight} attribute is named {@code TileLength} in TIFF specification.</p>
     */
    private long tileHeight = -1;

    /**
     * For each tile, the byte offset of that tile, as compressed and stored on disk.
     *
     * The offset is specified with respect to the beginning of the TIFF file.
     *
     * <p><b>Note</b> that this implies that each tile has a location independent of the locations of other tiles.</p>
     *
     * Offsets are ordered left-to-right and top-to-bottom. For PlanarConfiguration = 2,
     * the offsets for the first component plane are stored first,
     * followed by all the offsets for the second component plane, and so on.
     */
    private Vector tileOffsets;

    /**
     * For each tile, the number of (compressed) bytes in that tile.
     * See {@link #tileOffsets} for a description of how the byte counts are ordered.
     */
    private Vector tileByteCounts;

    /**
     * The number of rows per strip.
     * TIFF image data can be organized into strips for faster random access and efficient I/O buffering.
     * The {@code rowsPerStrip} and {@link #imageHeight} fields together tell us the number of strips in the entire image.
     * The equation is:
     *
     * {@preformat math
     *     StripsPerImage = floor ((ImageLength + RowsPerStrip - 1) / RowsPerStrip)
     * }
     *
     * {@code StripsPerImage} is not a field. It is merely a value that a TIFF reader will want to compute
     * because it specifies the number of {@code StripOffsets} and {@code StripByteCounts} for the image.
     *
     * <p>This field should be interpreted as an unsigned value.
     * The default is 2^32 - 1, which is effectively infinity (i.e. the entire image is one strip).</p>
     */
    private long rowsPerStrip = 0xFFFFFFFF;

    /**
     * For each strip, the number of bytes in the strip after compression.
     * <p>N = StripsPerImage for PlanarConfiguration equal to 1.
     * N = SamplesPerPixel * StripsPerImage for PlanarConfiguration equal to 2.</p>
     */
    private Vector stripByteCounts;

    /**
     *  For each strip, the byte offset of that strip.
     *
     * The offset is specified with respect to the beginning of the TIFF file.
     *
     * Note that this implies that each strip has a location independent of the locations of other strips.
     * This feature may be useful for editing applications.
     * This required field is the only way for a reader to find the image data.
     * (Unless TileOffsets is used; see {@link #tileOffsets}.
     *
     * Note that either SHORT or LONG values may be used to specify the strip offsets.
     * SHORT values may be used for small TIFF files. It should be noted, however,
     * that earlier TIFF specifications required LONG strip offsets and that some
     * software may not accept SHORT values.
     *
     * For maximum compatibility with operating systems such as MS-DOS and Windows,
     * the StripOffsets array should be less than or equal to 64K bytes in length,
     * and the strips themselves, in both compressed and uncompressed forms,
     * should not be larger than 64K bytes.
     */
    private Vector stripOffsets;

    /**
     * The number of components per pixel.
     * The {@code samplesPerPixel} value is usually 1 for bilevel, grayscale and palette-color images,
     * and 3 for RGB images. If this value is higher, then the {@code ExtraSamples} TIFF tag should
     * give an indication of the meaning of the additional channels.
     */
    private short samplesPerPixel = 0;

    /**
     * Number of bits per component.
     * The TIFF specification allows a different number of bits per component for each component corresponding to a pixel.
     * For example, RGB color data could use a different number of bits per component for each of the three color planes.
     * However, current Apache SIS implementation requires that all components have the same {@code BitsPerSample} value.
     */
    private short bitsPerSample = 0;

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
     * The color space of the image data.
     * 0 = WhiteIsZero. For bilevel and grayscale images: 0 is imaged as white.
     * 1 = BlackIsZero. For bilevel and grayscale images: 0 is imaged as black.
     * 2 = RGB. RGB value of (0,0,0) represents black, and (255,255,255) represents white.
     * 3 = Palette color. The value of the component is used as an index into the RGB valuesthe ColorMap.
     * 4 = Transparency Mask the defines an irregularly shaped region of another image in the same TIFF file.
     */
    private short photometricInterpretation;

    /**
     * The logical order of bits within a byte.
     *
     * The specification defines these values:
     *
     * 1 = pixels with lower column values are stored in the higher-order bits of the byte.
     * 2 = pixels with lower column values are stored in the lower-order bits of the byte.
     *
     * The specification goes on to warn that FillOrder=2 should not be used in some cases to avoid ambigouty,
     * and that support for FillOrder=2.
     *
     * In practice, the use of FillOrder=2 is very uncommon, and is not recommended.
     */
    private short fillOrder = 1;

    /**
     * Specifies that each pixel has N extra components whose interpretation is defined by one of the values listed below.
     * When this field is used, the SamplesPerPixel field has a value greater than the PhotometricInterpretation field suggests.
     * For example, full-color RGB data normally has SamplesPerPixel=3.
     * If SamplesPerPixel is greater than 3, then the ExtraSamples field describes the meaning of the extra samples.
     * If SamplesPerPixel is, say, 5 then ExtraSamples will contain 2 values, one for each extra sample.
     * ExtraSamples is typically used to include non-color information, such as opacity, in an image.
     *
     * The possible values for each item in the field's value are:
     * 0 = Unspecified data
     * 1 = Associated alpha data (with pre-multiplied color)
     * 2 = Unassociated alpha data

     * The difference between associated alpha and unassociated alpha is not just a matter of taste or a matter of maths.

     * Associated alpha is generally interpreted as true transparancy information.
     * Indeed, the original color values are lost in the case of complete transparency,
     * and rounded in the case of partial transparency. Also, associated alpha is only
     * logically possible as the single extra channel.

     * Unassociated alpha channels, on the other hand, can be used to encode a number of independent masks, for example.
     * The original color data is preserved without rounding. Any number of unassociated alpha channels can accompany an image.

     * If an extra sample is used to encode information that has little or nothing to do with alpha,
     * ExtraSample=0 (EXTRASAMPLE_UNSPECIFIED) is recommended.

     * <strong>Note also that extra components that are present must be stored as the last components in each pixel.
     * For example, if SamplesPerPixel is 4 and there is 1 extra component,
     * then it is located in the last component location (SamplesPerPixel-1) in each pixel.</strong>

     * This field must be present if there are extra samples.
     */
    private Vector extraSamples;

    /**
     * A color map for palette color images.

     * This field defines a Red-Green-Blue color map (often called a lookup table) for palette-color images.
     * In a palette-color image, a pixel value is used to index into an RGB lookup table.
     * For example, a palette-color pixel having a value of 0 would be displayed according to the 0th Red, Green, Blue triplet.

     * In a TIFF ColorMap, all the Red values come first, followed by the Green values,
     * then the Blue values. The number of values for each color is 2**BitsPerSample.
     * Therefore, the ColorMap field for an 8-bit palette-color image would have 3 * 256 values.
     * The width of each value is 16 bits, as implied by the type of SHORT. 0 represents the minimum intensity,
     * and 65535 represents the maximum intensity. Black is represented by 0,0,0, and white by 65535, 65535, 65535.

     * ColorMap must be included in all palette-color images.

     * In Specification Supplement 1, support was added for ColorMaps containing other then RGB values.
     * This scheme includes the Indexed tag, with value 1, and a PhotometricInterpretation different
     * from PaletteColor then next denotes the colorspace of the ColorMap entries.
     */
    private Vector colorMap;

    /**
     * The number of pixels per ResolutionUnit in the ImageWidth and the ImageHeight direction.
     *
     * In SIS use case, ISO 19115 Metadatas allow only one kind of resolution exprimate in meters.
     * This attribut is the maximum value from tiff XRESOLUTION and YRESOLUTION.
     * {@preformat
     * tiffResolution = Math.max(XRESOLUTION, YRESOLUTION);
     * }
     */
    private double tiffResolution = -1;

    /**
     * The unit of measurement for {@linkplain #tiffResolution XResolution and YResolution}.
     * Default value assume 2 (Inch).
     *
     * The specification defines these values:
     *
     * 1 = No absolute unit of measurement. Used for images that may have a non-square aspect ratio, but no meaningful absolute dimensions.
     * 2 = Inch.
     * 3 = Centimeter.
     */
    private Unit resolutionUnit = Units.INCH;

    /**
     * Creates a new image file directory.
     */
    ImageFileDirectory() {
    }

    /**
     * Reports a warning represented by the given message and exception.
     * At least one of message and exception shall be non-null.
     *
     * @param reader reader which manage exception and message.
     * @param message - the message to log, or null if none.
     * @param exception - the exception to log, or null if none.
     */
    private void warning(final Reader reader, final Level level, final short key, final Object ...message) {
        final LogRecord r = reader.resources().getLogRecord(level, key, message);
        reader.owner.warning(r);
    }

    /**
     * Returns {@code true} if this image contain some internaly TIFF TAGS adapted for tiled reading, else return {@code false}.
     *
     * @return {@code true} for tiled tags attributs existance, else return {@code false}.
     */
    private boolean isTiled() {
        return (tileWidth != -1        || tileHeight != -1
             || tileByteCounts != null || tileOffsets != null);
    }

    /**
     * Returns {@code true} if this image contain some internaly TIFF TAGS adapted for strip reading, else return {@code false}.
     *
     * @return {@code true} for strip tags attributs existance, else return {@code false}.
     */
    private boolean isStripped() {
        return (stripByteCounts !=  null || stripOffsets != null);
    }

    /**
     * Adds the value read from the current position in the given stream
     * for the entry identified by the given GeoTIFF tag.
     *
     * @param  reader   the input stream to read, the metadata and the character encoding.
     * @param  tag      the GeoTIFF tag to decode.
     * @param  type     the GeoTIFF type of the value to read.
     * @param  count    the number of values to read.
     * @return {@code null} on success, or the unrecognized value otherwise.
     * @throws IOException if an error occurred while reading the stream.
     * @throws ParseException if the value need to be parsed as date and the parsing failed.
     * @throws NumberFormatException if the value need to be parsed as number and the parsing failed.
     * @throws ArithmeticException if the value can not be represented in the expected Java type.
     * @throws IllegalArgumentException if a value which was expected to be a singleton is not.
     * @throws UnsupportedOperationException if the given type is {@link Type#UNDEFINED}.
     * @throws DataStoreException if a logical error is found or an unsupported TIFF feature is used.
     */
    Object addEntry(final Reader reader, final int tag, final Type type, final long count)
            throws IOException, ParseException, DataStoreException
    {
        switch (tag) {

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Essential information for being able to read the image at least as grayscale.       ////
            ////    In Java2D, following information are needed for building the SampleModel.           ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * How the components of each pixel are stored.
             * 1 = Chunky format. The component values for each pixel are stored contiguously (for example RGBRGBRGB).
             * 2 = Planar format. For example one plane of Red components, one plane of Green and one plane if Blue.
             */
            case Tags.PlanarConfiguration: {
                final long value = type.readLong(reader.input, count);
                if (value < 1 || value > 2) {
                    return value;
                }
                isPlanar = (value == 2);
                break;
            }
            /*
             * The number of columns in the image, i.e., the number of pixels per row.
             */
            case Tags.ImageWidth: {
                imageWidth = type.readUnsignedLong(reader.input, count);
                break;
            }
            /*
             * The number of rows of pixels in the image.
             */
            case Tags.ImageLength: {
                imageHeight = type.readUnsignedLong(reader.input, count);
                break;
            }

            ////////////////////////////////////////////////////////////////////
            ////                                                            ////
            ////            Internaly stored into strips hierarchy          ////
            ////                                                            ////
            ////////////////////////////////////////////////////////////////////
            /*
             * The number of rows per strip. RowsPerStrip and ImageLength together tell us the number of strips
             * in the entire image: StripsPerImage = floor((ImageLength + RowsPerStrip - 1) / RowsPerStrip).
             */
            case Tags.RowsPerStrip: {
                rowsPerStrip = type.readUnsignedLong(reader.input, count);
                break;
            }
            /*
             * For each strip, the number of bytes in the strip after compression.
             */
            case Tags.StripByteCounts: {
                stripByteCounts = type.readVector(reader.input, count);
                break;
            }
            /*
             * For each strip, the byte offset of that strip relative to the beginning of the TIFF file.
             */
            case Tags.StripOffsets: {
                stripOffsets = type.readVector(reader.input, count);
                break;
            }

            ////////////////////////////////////////////////////////////////////
            ////                                                            ////
            ////            Internaly stored into Tiles hierarchy           ////
            ////                                                            ////
            ////////////////////////////////////////////////////////////////////

            /*
             * The tile width in pixels. This is the number of columns in each tile.
             */
            case Tags.TileWidth: {
                tileWidth = type.readUnsignedLong(reader.input, count);
                break;
            }

            /*
             * The tile length (height) in pixels. This is the number of rows in each tile.
             */
            case Tags.TileLength: {
                tileHeight = type.readUnsignedLong(reader.input, count);
                break;
            }

            /*
             * The tile length (height) in pixels. This is the number of rows in each tile.
             */
            case Tags.TileOffsets: {
                tileOffsets = type.readVector(reader.input, count);
                break;
            }

            /*
             * The tile width in pixels. This is the number of columns in each tile.
             */
            case Tags.TileByteCounts: {
                tileByteCounts = type.readVector(reader.input, count);
                break;
            }

            ////////////////////////////////////////////////////////////////////
            ////                                                            ////
            ////                  Samples encoding made                     ////
            ////                                                            ////
            ////////////////////////////////////////////////////////////////////

            /*
             * Compression scheme used on the image data.
             */
            case Tags.Compression: {
                final long value = type.readLong(reader.input, count);
                compression = Compression.valueOf(value);
                if (compression == null) {
                    return value;
                }
                break;
            }
            /*
             * The logical order of bits within a byte. If this value is 2, then bits order shall be reversed in every
             * bytes before decompression.
             */
            case Tags.FillOrder: {
                fillOrder = type.readShort(reader.input, count);
                break;
            }
            /*
             * Number of bits per component. The array length should be the number of components in a
             * pixel (e.g. 3 for RGB values). Typically, all components have the same number of bits.
             * But the TIFF specification allows different values.
             */
            case Tags.BitsPerSample: {
                final Vector values = type.readVector(reader.input, count);
                /*
                 * The current implementation requires that all 'bitsPerSample' elements have the same value.
                 * This restriction may be revisited in future Apache SIS versions.
                 * Note: 'count' is never zero when this method is invoked, so we do not need to check bounds.
                 */
                bitsPerSample = values.shortValue(0);
                final int length = values.size();
                for (int i = 1; i < length; i++) {
                    if (values.shortValue(i) != bitsPerSample) {
                        throw new DataStoreContentException(reader.resources().getString(
                                Resources.Keys.ConstantValueRequired_3, "BitsPerSample", reader.input.filename, values));
                    }
                }
                break;
            }
            /*
             * The number of components per pixel. Usually 1 for bilevel, grayscale, and palette-color images,
             * and 3 for RGB images. Default value is 1.
             */
            case Tags.SamplesPerPixel: {
                samplesPerPixel = type.readShort(reader.input, count);
                break;
            }
            /*
             * Specifies that each pixel has N extra components. When this field is used, the SamplesPerPixel field
             * has a value greater than the PhotometricInterpretation field suggests. For example, a full-color RGB
             * image normally has SamplesPerPixel=3. If SamplesPerPixel is greater than 3, then the ExtraSamples field
             * describes the meaning of the extra samples. It may be an alpha channel, but not necessarily.
             */
            case Tags.ExtraSamples: {
                extraSamples = type.readVector(reader.input, count);
                break;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Information related to the color palette or the meaning of sample values.           ////
            ////    In Java2D, following information are needed for building the ColorModel.            ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * The color space of the image data.
             * 0 = WhiteIsZero. For bilevel and grayscale images: 0 is imaged as white.
             * 1 = BlackIsZero. For bilevel and grayscale images: 0 is imaged as black.
             * 2 = RGB. RGB value of (0,0,0) represents black, and (255,255,255) represents white.
             * 3 = Palette color. The value of the component is used as an index into the RGB valuesthe ColorMap.
             * 4 = Transparency Mask the defines an irregularly shaped region of another image in the same TIFF file.
             */
            case Tags.PhotometricInterpretation: {
                photometricInterpretation = type.readShort(reader.input, count);
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
                colorMap = type.readVector(reader.input, count);
                break;
            }
            /*
             * The minimum component value used. Default is 0.
             */
            case Tags.MinSampleValue: {
                reader.metadata.addMinimumSampleValue(type.readDouble(reader.input, count));
                break;
            }
            /*
             * The maximum component value used. Default is {@code (1 << BitsPerSample) - 1}.
             * This field is for statistical purposes and should not to be used to affect the
             * visual appearance of an image, unless a map styling is applied.
             */
            case Tags.MaxSampleValue: {
                reader.metadata.addMaximumSampleValue(type.readDouble(reader.input, count));
                break;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Information useful for defining the image role in a multi-images context.           ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * A general indication of the kind of data contained in this subfile, mainly useful when there
             * are multiple subfiles in a single TIFF file. This field is made up of a set of 32 flag bits.
             *
             * Bit 0 is 1 if the image is a reduced-resolution version of another image in this TIFF file.
             * Bit 1 is 1 if the image is a single page of a multi-page image (see PageNumber).
             * Bit 2 is 1 if the image defines a transparency mask for another image in this TIFF file (see PhotometricInterpretation).
             * Bit 4 indicates MRC imaging model as described in ITU-T recommendation T.44 [T.44] (See ImageLayer tag) - RFC 2301.
             */
            case Tags.NewSubfileType: {
                // TODO
                break;
            }
            /*
             * Old version (now deprecated) of above NewSubfileType.
             * 1 = full-resolution image data
             * 2 = reduced-resolution image data
             * 3 = a single page of a multi-page image (see PageNumber).
             */
            case Tags.SubfileType: {
                // TODO
                break;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Information related to the Coordinate Reference System referencement.        ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

           /*
            * References the needed "GeoKeys" to build CRS.
            * An array of unsigned SHORT values, which are primarily grouped into blocks of 4.
            * The first 4 values are special, and contain GeoKey directory header information.
            */
            case Tags.GeoKeyDirectoryTag : {
                reader.crsBuilder.setGeoKeyDirectoryTag(type.readVector(reader.input, count));
                break;
            }

           /*
            * This tag is used to store all of the DOUBLE valued GeoKeys, referenced by the GeoKeyDirectoryTag.
            */
            case Tags.GeoDoubleParamsTag : {
                reader.crsBuilder.setGeoDoubleParamsTag(type.readVector(reader.input, count));
                break;
            }

           /*
            * This tag is used to store all of the ASCII valued GeoKeys, referenced by the GeoKeyDirectoryTag.
            */
            case Tags.GeoAsciiParamsTag : {
                final String[] readString = type.readString(reader.input, count, reader.owner.encoding);
                if (readString != null && readString.length > 0)
                    reader.crsBuilder.setGeoAsciiParamsTag(readString[0]);
                break;
            }


            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Information related to the Coordinate Reference System and the bounding box.        ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * The orientation of the image with respect to the rows and columns.
             * This is an integer numeroted from 1 to 7 inclusive (see TIFF specification for meaning).
             */
            case Tags.Orientation: {
                // TODO
                break;
            }


            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Metadata for discovery purposes, conditions of use, etc.                            ////
            ////    Those metadata are not "critical" information for reading the image.                ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * A string that describes the subject of the image.
             * For example, a user may wish to attach a comment such as "1988 company picnic" to an image.
             */
            case Tags.ImageDescription: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.addTitle(value);
                }
                break;
            }
            /*
             * Person who created the image. Some older TIFF files used this tag for storing
             * Copyright information, but Apache SIS does not support this legacy practice.
             */
            case Tags.Artist: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.addAuthor(value);
                }
                break;
            }
            /*
             * Copyright notice of the person or organization that claims the copyright to the image.
             * Example: “Copyright, John Smith, 1992. All rights reserved.”
             */
            case Tags.Copyright: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.parseLegalNotice(value);
                }
                break;
            }
            /*
             * Date and time of image creation. The format is: "YYYY:MM:DD HH:MM:SS" with 24-hour clock.
             */
            case Tags.DateTime: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.add(reader.getDateFormat().parse(value), DateType.CREATION);
                }
                break;
            }
            /*
             * The computer and/or operating system in use at the time of image creation.
             */
            case Tags.HostComputer: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.addProcessing(value);
                }
                break;
            }
            /*
             * Name and version number of the software package(s) used to create the image.
             */
            case Tags.Software: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.setSoftwareReferences(value);
                }
                break;
            }
            /*
             * Manufacturer of the scanner, video digitizer, or other type of equipment used to generate the image.
             * Synthetic images should not include this field.
             */
            case Tags.Make: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.addInstrument(value);
                }
                break;
            }
            /*
             * The model name or number of the scanner, video digitizer, or other type of equipment used to
             * generate the image.
             */
            case Tags.Model: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.addInstrument(value);
                }
                break;
            }
            /*
             * The number of pixels per ResolutionUnit in the ImageWidth direction.
             */
            case Tags.XResolution: {
                tiffResolution = Math.max(type.readDouble(reader.input, count), tiffResolution);
                break;
            }
            /*
             * The number of pixels per ResolutionUnit in the ImageLength direction.
             */
            case Tags.YResolution: {
                tiffResolution = Math.max(type.readDouble(reader.input, count), tiffResolution);
                break;
            }
            /*
             * The unit of measurement for XResolution and YResolution.
             * 1 = None, 2 = Inch, 3 = Centimeter.
             */
            case Tags.ResolutionUnit: {
                final short res = type.readShort(reader.input, count);
                switch(res) {
                    case 2 : {
                        resolutionUnit = Units.INCH;
                        break;
                    }
                    case 3 : {
                        resolutionUnit = Units.CENTIMETRE;
                        break;
                    }
                    default : {
                        resolutionUnit = null;
                        break;
                    }
                }
                break;
            }
            /*
             * The technique used to convert from gray to black and white pixels (if applicable):
             * 1 = No dithering or halftoning has been applied to the image data.
             * 2 = An ordered dither or halftone technique has been applied to the image data.
             * 3 = A randomized process such as error diffusion has been applied to the image data.
             */
            case Tags.Threshholding: {
                final short value = type.readShort(reader.input, count);
                final String s;
                switch(value) {
                    case 2 : {
                        s = reader.resources().getString(Resources.Keys.Threshholding2_0);
                        break;
                    }
                    case 3 : {
                        s = reader.resources().getString(Resources.Keys.Threshholding3_0);
                        break;
                    }
                    default : {
                        s = reader.resources().getString(Resources.Keys.Threshholding1_0);
                        break;
                    }
                }
                reader.metadata.setProcedureDescription(s);
                break;
            }
            /*
             * The width of the dithering or halftoning matrix used to create a dithered or halftoned
             * bilevel file. Meaningful only if Threshholding = 2.
             */
            case Tags.CellWidth: {
                final String s = reader.resources().getString(Resources.Keys.CellWidth_1, type.readShort(reader.input, count));
                reader.metadata.setProcessingDocumentation(s);
                break;
            }
            /*
             * The height of the dithering or halftoning matrix used to create a dithered or halftoned
             * bilevel file. Meaningful only if Threshholding = 2.
             */
            case Tags.CellLength: {
                final String s = reader.resources().getString(Resources.Keys.CellHeight_1, type.readShort(reader.input, count));
                reader.metadata.setProcessingDocumentation(s);
                break;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Defined by TIFF specification but currently ignored.                                ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * For each string of contiguous unused bytes in a TIFF file, the number of bytes and the byte offset
             * in the string. Those tags are deprecated and do not need to be supported.
             */
            case Tags.FreeByteCounts:
            case Tags.FreeOffsets:
            /*
             * For grayscale data, the optical density of each possible pixel value, plus the precision of that
             * information. This is ignored by most TIFF readers.
             */
            case Tags.GrayResponseCurve:
            case Tags.GrayResponseUnit: {
                warning(reader, Level.FINE, Resources.Keys.IgnoredTag_1, Tags.name(tag));
                break;
            }
        }
        return null;
    }

    /**
     * Validate method which re-build missing attributs from others as if possible or
     * throw exception if mandatory attributs are missing or also if it is impossible
     * to resolve ambiguity between some attributs.
     *
     * @throws DataStoreContentException
     */
    final void checkTiffTags(final Reader reader)
            throws DataStoreContentException {

        if (imageWidth == -1)
            throw new DataStoreContentException(reader.resources().getString(
                                Resources.Keys.MissingValueRequired_2, "ImageWidth", reader.input.filename));

        if (imageHeight == -1)
            throw new DataStoreContentException(reader.resources().getString(
                                Resources.Keys.MissingValueRequired_2, "ImageLength", reader.input.filename));

        if ((!isTiled() && !isStripped())
          || (isTiled() &&  isStripped()))
            throw new DataStoreContentException(reader.resources().getString(
                                Resources.Keys.MissingTileStrip_1, reader.input.filename));

        if (samplesPerPixel == 0) {
            samplesPerPixel = 1;
            warning(reader, Level.FINE, Resources.Keys.DefaultAttribut_2, "SamplesPerPixel", 1);
        }

        if (bitsPerSample == 0) {
            bitsPerSample = 1;
            warning(reader, Level.FINE, Resources.Keys.DefaultAttribut_2, "BitsPerSample", 1);
        }

        if (colorMap != null) {
            final int expectedSize = 3 * (1 << bitsPerSample);
            if (colorMap.size() != expectedSize)
                warning(reader, Level.WARNING, Resources.Keys.MismatchLength_4, "ColorMap array",
                        "BitsPerSample",
                        "3* 2^bitsPerSample : "+expectedSize, colorMap.size());
        }


        final boolean canReBuilt = (!isPlanar && compression.equals(Compression.NONE));

        if (isTiled()) {
            //-- it is not efficient and dangerous to try to re-build tileOffsets.
            if (tileOffsets == null)
                throw new DataStoreContentException(reader.resources().getString(
                                    Resources.Keys.MissingValueRequired_2, "TileOffsets", reader.input.filename));

            if (canReBuilt) {
                int twThTbc = 0;
                if (tileWidth      >= 0)    twThTbc |= 4;
                if (tileHeight     >= 0)    twThTbc |= 2;
                if (tileByteCounts != null) twThTbc |= 1;

                switch(twThTbc) {
                    case 3 : {
                        //-- missing tile width twThTbc = 011
                        warning(reader, Level.WARNING, Resources.Keys.ReBuildAttribut_2, "TileWidth","TileByteCounts, TileHeight, SamplesPerPixel, BitsPerSamples");
                        tileWidth = tileByteCounts.get(0).intValue() / (tileHeight * samplesPerPixel * (bitsPerSample / Byte.SIZE));
                        break;
                    }
                    case 5 : {
                        //-- missing tileHeight twThTbc = 101
                        warning(reader, Level.WARNING, Resources.Keys.ReBuildAttribut_2, "TileHeight","TileByteCounts, TileWidth, SamplesPerPixel, BitsPerSamples");
                        tileHeight = tileByteCounts.get(0).intValue() / (tileWidth * samplesPerPixel * (bitsPerSample / Byte.SIZE));
                        break;
                    }
                    case 6 : {
                        //-- missing tileByteCount twThTbc = 110
                        warning(reader, Level.WARNING, Resources.Keys.ReBuildAttribut_2, "TileByteCounts","TileOffsets, TileHeight, TileWidth, SamplesPerPixel, BitsPerSamples");
                        final long tileByteCount        = tileHeight * tileWidth * samplesPerPixel * bitsPerSample;
                        final long[] tileByteCountArray = new long[tileOffsets.size()];
                        Arrays.fill(tileByteCountArray, tileByteCount);
                        tileByteCounts = Vector.create(tileByteCountArray, true);
                        break;
                    }
                    case 7 : {
                        //-- every thing is ok
                        break;
                    }
                    default : {
                        throw new DataStoreContentException(reader.resources().getString(
                                            Resources.Keys.MissingValueRequired_2, "TileWidth, TileHeight, TileByteCount", reader.input.filename));
                    }
                }
            }

            if (tileByteCounts == null)
                throw new DataStoreContentException(reader.resources().getString(
                                    Resources.Keys.MissingValueRequired_2, "TileByteCount", reader.input.filename));

            if (tileWidth == -1)
                throw new DataStoreContentException(reader.resources().getString(
                                Resources.Keys.MissingValueRequired_2, "TileWidth", reader.input.filename));
            if (tileHeight == -1)
                throw new DataStoreContentException(reader.resources().getString(
                                    Resources.Keys.MissingValueRequired_2, "TileLength", reader.input.filename));

            //-- Check size of ByteCounts and Offsets
            //-- important reading attributs, Level WARNING
            int expectedSize = (int) (Math.floorDiv(imageWidth, tileWidth) * Math.floorDiv(imageHeight, tileHeight));
            if (isPlanar) expectedSize *= samplesPerPixel;
            if (tileOffsets.size() != expectedSize)
                warning(reader, Level.WARNING, Resources.Keys.MismatchLength_4, "TileOffsets",
                        "ImageWidth, ImageHeight, TileWidth, TileHeight, SamplePerPixel and PlanarConfiguration",
                        expectedSize, tileOffsets.size());
            if (tileByteCounts.size() != expectedSize)
                warning(reader, Level.WARNING, Resources.Keys.MismatchLength_4, "TileByteCounts",
                        "ImageWidth, ImageHeight, TileWidth, TileHeight, SamplePerPixel and PlanarConfiguration",
                        expectedSize, tileByteCounts.size());
            if (tileByteCounts.size() != tileOffsets.size())
                warning(reader, Level.WARNING, Resources.Keys.MismatchLength_4, "TileByteCounts",
                        "TileOffsets", tileOffsets.size(), tileByteCounts.size());
        }

        if (isStripped()) {

            if (stripOffsets == null)
                throw new DataStoreContentException(reader.resources().getString(
                                    Resources.Keys.MissingValueRequired_2, "StripOffsets", reader.input.filename));

            if (rowsPerStrip == 0xFFFFFFFF) {
                rowsPerStrip = imageHeight;
                warning(reader, Level.FINE, Resources.Keys.DefaultAttribut_2, "RowsPerStrip", imageHeight+"(= imageLength)");
            }

            if (canReBuilt) {
                if (stripByteCounts == null) {
                    warning(reader, Level.WARNING, Resources.Keys.ReBuildAttribut_2, "StripByteCounts","StripOffset, RowsPerStrip, ImageWidth, SamplesPerPixel, BitsPerSamples");
                    final long stripByteCount = rowsPerStrip * imageWidth * samplesPerPixel * bitsPerSample;
                    final long[] stripByteCountsArray = new long[stripOffsets.size()];
                    Arrays.fill(stripByteCountsArray, stripByteCount);
                    stripByteCounts = Vector.create(stripByteCountsArray, true);
                }
            }

            if (stripByteCounts == null)
                throw new DataStoreContentException(reader.resources().getString(
                                    Resources.Keys.MissingValueRequired_2, "StripByteCount", reader.input.filename));

            //-- Check size of ByteCounts and Offsets
            //-- important reading attributs, Level WARNING
            int expectedSize = (int) Math.floorDiv(imageHeight, rowsPerStrip);
            if (isPlanar) expectedSize *= samplesPerPixel;
            if (stripOffsets.size() != expectedSize)
                warning(reader, Level.WARNING, Resources.Keys.MismatchLength_4, "StripOffsets",
                        "RowsPerStrip, ImageHeight, SamplePerPixel and PlanarConfiguration",
                        expectedSize, stripOffsets.size());
            if (stripByteCounts.size() != expectedSize)
                warning(reader, Level.WARNING, Resources.Keys.MismatchLength_4, "StripByteCounts",
                        "RowsPerStrip, ImageHeight, SamplePerPixel and PlanarConfiguration",
                        expectedSize, stripByteCounts.size());
            if (stripByteCounts.size() != stripOffsets.size())
                warning(reader, Level.WARNING, Resources.Keys.MismatchLength_4, "StripByteCounts",
                        "StripOffsets", stripOffsets.size(), stripByteCounts.size());
        }
    }


    /**
     * Completes the metadata with the information stored in the field of this IFD.
     * This method is invoked only if the user requested the ISO 19115 metadata.
     */
    final void completeMetadata(final MetadataBuilder metadata, final Locale locale) {
        if (compression != null) {
            metadata.addCompression(compression.name().toLowerCase(locale));
        }
        //-- add Resolution into metadata
        //-- convert into meters
        if (tiffResolution != -1 && resolutionUnit != null) {
            metadata.addResolution(resolutionUnit.getConverterTo(METER).convert(tiffResolution));
        }
    }

}
