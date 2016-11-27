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
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.nio.charset.Charset;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.metadata.citation.DateType;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.storage.ChannelDataInput;
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
     * Possible value for the {@link #tileTagFamily} field. That field tells whether image tiling
     * was specified using the {@code Tile*} family of TIFF tags or the {@code Strip*} family.
     */
    private static final byte TILE = 1, STRIP = 2;

    /**
     * The GeoTIFF reader which contain this {@code ImageFileDirectory}.
     * Used for fetching information like the input channel and where to report warnings.
     */
    private final Reader reader;

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
     * The size of each tile, or -1 if the information has not be found.
     * Tiles shall be small enough for fitting in memory, typically in a {@link java.awt.image.Raster} object.
     * The TIFF specification requires that tile width and height must be a multiple of 16, but the SIS reader
     * implementation works for any size. Tiles need not be square.
     *
     * <p>Assuming integer arithmetic, the number of tiles in an image can be computed as below
     * (these computed values are not TIFF fields):</p>
     *
     * {@preformat math
     *   tilesAcross   = (imageWidth  + tileWidth  - 1) / tileWidth
     *   tilesDown     = (imageHeight + tileHeight - 1) / tileHeight
     *   tilesPerImage = tilesAcross * tilesDown
     * }
     *
     * Note that {@link #imageWidth} can be less than {@code tileWidth} and/or {@link #imageHeight} can be less
     * than {@code tileHeight}. Such case means that the tiles are too large or that the tiled image is too small,
     * neither of which is recommended.
     *
     * <p><b>Note:</b>
     * the {@link #tileHeight} attribute is named {@code TileLength} in TIFF specification.</p>
     *
     * <div class="section">Strips considered as tiles</div>
     * The TIFF specification also defines a {@code RowsPerStrip} tag, which is equivalent to the
     * height of tiles having the same width than the image. While the TIFF specification handles
     * "tiles" and "strips" separately, Apache SIS handles strips as a special kind of tiles where
     * only {@code tileHeight} is specified and {@code tileWidth} defaults to {@link #imageWidth}.
     */
    private int tileWidth = -1, tileHeight = -1;

    /**
     * For each tile, the byte offset of that tile, as compressed and stored on disk.
     * The offset is specified with respect to the beginning of the TIFF file.
     * Each tile has a location independent of the locations of other tiles
     *
     * <p>Offsets are ordered left-to-right and top-to-bottom. if {@link #isPlanar} is {@code true}
     * (i.e. components are stored in separate “component planes”), then the offsets for the first
     * component plane are stored first, followed by all the offsets for the second component plane,
     * and so on.</p>
     *
     * <div class="section">Strips considered as tiles</div>
     * The TIFF specification also defines a {@code StripOffsets} tag, which contains the byte offset
     * of each strip. In Apache SIS implementation, strips are considered as a special kind of tiles
     * having a width equals to {@link #imageWidth}.
     */
    private Vector tileOffsets;

    /**
     * For each tile, the number of (compressed) bytes in that tile.
     * See {@link #tileOffsets} for a description of how the byte counts are ordered.
     *
     * <div class="section">Strips considered as tiles</div>
     * The TIFF specification also defines a {@code RowsPerStrip} tag, which is the number
     * of bytes in the strip after compression. In Apache SIS implementation, strips are
     * considered as a special kind of tiles having a width equals to {@link #imageWidth}.
     */
    private Vector tileByteCounts;

    /**
     * Whether the tiling was specified using the {@code Tile*} family of TIFF tags or the {@code Strip*}
     * family of tags. Value can be {@link #TILE}, {@link #STRIP} or 0 if unspecified. This field is used
     * for error detection since Each TIFF file shall use exactly one family of tags.
     */
    private byte tileTagFamily;

    /**
     * If {@code true}, the components are stored in separate “component planes”.
     * The default is {@code false}, which stands for the "chunky" format
     * (for example RGB data stored as RGBRGBRGB).
     */
    private boolean isPlanar;

    /**
     * Whether the bit order should be reversed. This boolean value is determined from the {@code FillOrder} TIFF tag.
     *
     * <ul>
     *   <li>Value 1 (mapped to {@code false}) means that pixels with lower column values are stored in the
     *       higher-order bits of the byte. This is the default value.</li>
     *   <li>Value 2 (mapped to {@code true}) means that pixels with lower column values are stored in the
     *       lower-order bits of the byte. In practice, this order is very uncommon and is not recommended.</li>
     * </ul>
     *
     * Value 1 is mapped to {@code false} and 2 is mapped to {@code true}.
     */
    private boolean reverseBitsOrder;

    /**
     * Number of bits per component.
     * The TIFF specification allows a different number of bits per component for each component corresponding to a pixel.
     * For example, RGB color data could use a different number of bits per component for each of the three color planes.
     * However, current Apache SIS implementation requires that all components have the same {@code BitsPerSample} value.
     */
    private short bitsPerSample;

    /**
     * The number of components per pixel.
     * The {@code samplesPerPixel} value is usually 1 for bilevel, grayscale and palette-color images,
     * and 3 for RGB images. If this value is higher, then the {@code ExtraSamples} TIFF tag should
     * give an indication of the meaning of the additional channels.
     */
    private short samplesPerPixel;

    /**
     * Specifies that each pixel has {@code extraSamples.size()} extra components whose interpretation is defined
     * by one of the values listed below. When this field is used, the {@link #samplesPerPixel} field has a value
     * greater than what the {@link #photometricInterpretation} field suggests. For example, full-color RGB data
     * normally has {@link #samplesPerPixel} = 3. If {@code samplesPerPixel} is greater than 3, then this
     * {@code extraSamples} field describes the meaning of the extra samples. If {@code samplesPerPixel} is,
     * say, 5 then this {@code extraSamples} field will contain 2 values, one for each extra sample.
     *
     * <p>Extra components that are present must be stored as the last components in each pixel.
     * For example, if {@code samplesPerPixel} is 4 and there is 1 extra component, then it is
     * located in the last component location in each pixel.</p>
     *
     * <p>ExtraSamples is typically used to include non-color information, such as opacity, in an image.
     * The possible values for each item are:</p>
     *
     * <ul>
     *   <li>0 = Unspecified data.</li>
     *   <li>1 = Associated alpha data (with pre-multiplied color).</li>
     *   <li>2 = Unassociated alpha data.</li>
     * </ul>
     *
     * Associated alpha is generally interpreted as true transparency information. Indeed, the original color
     * values are lost in the case of complete transparency, and rounded in the case of partial transparency.
     * Also, associated alpha is only logically possible as the single extra channel.
     * Unassociated alpha channels, on the other hand, can be used to encode a number of independent masks.
     * The original color data is preserved without rounding. Any number of unassociated alpha channels can
     * accompany an image.
     *
     * <p>If an extra sample is used to encode information that has little or nothing to do with alpha,
     * then {@code extraSample} = 0 ({@code EXTRASAMPLE_UNSPECIFIED}) is recommended.</p>
     */
    private Vector extraSamples;

    /**
     * The color space of the image data, or -1 if unspecified.
     *
     * <table>
     *   <caption>Color space codes</caption>
     *   <tr><th>Value</th> <th>Label</th>        <th>Description</th></tr>
     *   <tr><td>0</td> <td>WhiteIsZero</td>      <td>For bilevel and grayscale images. 0 is imaged as white.</td></tr>
     *   <tr><td>1</td> <td>BlackIsZero</td>      <td>For bilevel and grayscale images. 0 is imaged as black.</td></tr>
     *   <tr><td>2</td> <td>RGB</td>              <td>RGB value of (0,0,0) represents black, and (255,255,255) represents white.</td></tr>
     *   <tr><td>3</td> <td>PaletteColor</td>     <td>The value of the component is used as an index into the RGB values of the {@link #colorMap}.</td></tr>
     *   <tr><td>4</td> <td>TransparencyMask</td> <td>Defines an irregularly shaped region of another image in the same TIFF file.</td></tr>
     * </table>
     */
    private byte photometricInterpretation = -1;

    /**
     * A color map for palette color images ({@link #photometricInterpretation} = 3).
     * This vector defines a Red-Green-Blue color map (often called a lookup table) for palette-color images.
     * In a palette-color image, a pixel value is used to index into an RGB lookup table. For example, a
     * palette-color pixel having a value of 0 would be displayed according to the 0th Red, Green, Blue triplet.
     *
     * <p>In a TIFF ColorMap, all the Red values come first, followed by all Green values, then all Blue values.
     * The number of values for each color is 1 {@literal <<} {@link #bitsPerSample}. Therefore, the {@code ColorMap}
     * vector for an 8-bit palette-color image would have 3 * 256 values. 0 represents the minimum intensity and 65535
     * represents the maximum intensity. Black is represented by 0,0,0 and white by 65535, 65535, 65535.</p>
     *
     * <p>{@code ColorMap} must be included in all palette-color images.
     * In Specification Supplement 1, support was added for color maps containing other then RGB values.
     * This scheme includes the {@code Indexed} tag, with value 1, and a {@link #photometricInterpretation}
     * different from {@code PaletteColor}.</p>
     */
    private Vector colorMap;

    /**
     * The size of the dithering or halftoning matrix used to create a dithered or halftoned bilevel file.
     * This field should be present only if {@code Threshholding} tag is 2 (an ordered dither or halftone
     * technique has been applied to the image data). Special values:
     *
     * <ul>
     *   <li>-1 means that {@code Threshholding} is 1 or unspecified.</li>
     *   <li>-2 means that {@code Threshholding} is 2 but the matrix size has not yet been specified.</li>
     *   <li>-3 means that {@code Threshholding} is 3 (randomized process such as error diffusion).</li>
     * </ul>
     */
    private short cellWidth = -1, cellHeight = -1;

    /**
     * The minimum or maximum sample value found in the image, or {@code NaN} if unspecified.
     */
    private double minValue = Double.NaN, maxValue = Double.NaN;

    /**
     * The number of pixels per {@link #resolutionUnit} in the {@link #imageWidth} and the {@link #imageHeight}
     * directions, or {@link Double#NaN} is unspecified. Since ISO 19115 does not have separated resolution fields
     * for image width and height, Apache SIS stores only the maximal value.
     */
    private double resolution = Double.NaN;

    /**
     * The unit of measurement for the {@linkplain #resolution} value, or {@code null} if none.
     * A null value is used for images that may have a non-square aspect ratio, but no meaningful
     * absolute dimensions. Default value for TIFF files is inch.
     */
    private Unit<Length> resolutionUnit = Units.INCH;

    /**
     * The compression method, or {@code null} if unknown. If the compression method is unknown
     * or unsupported we can not read the image, but we still can read the metadata.
     */
    private Compression compression;

    /**
     * References the {@link GeoKeys} needed for building the Coordinate Reference System.
     * This is a GeoTIFF extension to the TIFF specification.
     * Content will be parsed by {@link CRSBuilder}.
     */
    private Vector geoKeyDirectory;

    /**
     * The numeric values referenced by the {@link #geoKeyDirectory}.
     * This is a GeoTIFF extension to the TIFF specification.
     * Content will be parsed by {@link CRSBuilder}.
     */
    private Vector numericGeoParameters;

    /**
     * The characters referenced by the {@link #geoKeyDirectory}.
     * This is a GeoTIFF extension to the TIFF specification.
     * Content will be parsed by {@link CRSBuilder}.
     */
    private String asciiGeoParameters;

    /**
     * Creates a new image file directory.
     *
     * @param reader  information about the input stream to read, the metadata and the character encoding.
     */
    ImageFileDirectory(final Reader reader) {
        this.reader = reader;
    }

    /**
     * Shortcut for a frequently requested information.
     */
    private ChannelDataInput input() {
        return reader.input;
    }

    /**
     * Shortcut for a frequently requested information.
     */
    private Charset encoding() {
        return reader.owner.encoding;
    }

    /**
     * Adds the value read from the current position in the given stream for the entry identified
     * by the given GeoTIFF tag. This method may store the value either in a field of this class,
     * or directly in the {@link MetadataBuilder}. However in the later case, this method should
     * not write anything under the {@code "metadata/contentInfo"} node.
     *
     * @param  tag    the GeoTIFF tag to decode.
     * @param  type   the GeoTIFF type of the value to read.
     * @param  count  the number of values to read.
     * @return {@code null} on success, or the unrecognized value otherwise.
     * @throws IOException if an error occurred while reading the stream.
     * @throws ParseException if the value need to be parsed as date and the parsing failed.
     * @throws NumberFormatException if the value need to be parsed as number and the parsing failed.
     * @throws ArithmeticException if the value can not be represented in the expected Java type.
     * @throws IllegalArgumentException if a value which was expected to be a singleton is not.
     * @throws UnsupportedOperationException if the given type is {@link Type#UNDEFINED}.
     * @throws DataStoreException if a logical error is found or an unsupported TIFF feature is used.
     */
    Object addEntry(final short tag, final Type type, final long count)
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
                final int value = type.readInt(input(), count);
                switch (value) {
                    case 1:  isPlanar = false; break;
                    case 2:  isPlanar = true;  break;
                    default: return value;                  // Cause a warning to be reported by the caller.
                }
                break;
            }
            /*
             * The number of columns in the image, i.e., the number of pixels per row.
             */
            case Tags.ImageWidth: {
                imageWidth = type.readUnsignedLong(input(), count);
                break;
            }
            /*
             * The number of rows of pixels in the image.
             */
            case Tags.ImageLength: {
                imageHeight = type.readUnsignedLong(input(), count);
                break;
            }
            /*
             * The tile width in pixels. This is the number of columns in each tile.
             */
            case Tags.TileWidth: {
                setTileTagFamily(TILE);
                tileWidth = type.readInt(input(), count);
                break;
            }
            /*
             * The tile length (height) in pixels. This is the number of rows in each tile.
             */
            case Tags.TileLength: {
                setTileTagFamily(TILE);
                tileHeight = type.readInt(input(), count);
                break;
            }
            /*
             * The number of rows per strip. This is considered by SIS as a special kind of tiles.
             * From this point of view, TileLength = RowPerStrip and TileWidth = ImageWidth.
             */
            case Tags.RowsPerStrip: {
                setTileTagFamily(STRIP);
                tileHeight = type.readInt(input(), count);
                break;
            }
            /*
             * The tile length (height) in pixels. This is the number of rows in each tile.
             */
            case Tags.TileOffsets: {
                setTileTagFamily(TILE);
                tileOffsets = type.readVector(input(), count);
                break;
            }
            /*
             * For each strip, the byte offset of that strip relative to the beginning of the TIFF file.
             * In Apache SIS implementation, strips are considered as a special kind of tiles.
             */
            case Tags.StripOffsets: {
                setTileTagFamily(STRIP);
                tileOffsets = type.readVector(input(), count);
                break;
            }
            /*
             * The tile width in pixels. This is the number of columns in each tile.
             */
            case Tags.TileByteCounts: {
                setTileTagFamily(TILE);
                tileByteCounts = type.readVector(input(), count);
                break;
            }
            /*
             * For each strip, the number of bytes in the strip after compression.
             * In Apache SIS implementation, strips are considered as a special kind of tiles.
             */
            case Tags.StripByteCounts: {
                setTileTagFamily(STRIP);
                tileByteCounts = type.readVector(input(), count);
                break;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Information that defines how the sample values are organized (their layout).        ////
            ////    In Java2D, following information are needed for building the SampleModel.           ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * Compression scheme used on the image data.
             */
            case Tags.Compression: {
                final long value = type.readLong(input(), count);
                compression = Compression.valueOf(value);
                if (compression == null) {
                    return value;                           // Cause a warning to be reported by the caller.
                }
                break;
            }
            /*
             * The logical order of bits within a byte. If this value is 2, then
             * bits order shall be reversed in every bytes before decompression.
             */
            case Tags.FillOrder: {
                final int value = type.readInt(input(), count);
                switch (value) {
                    case 1: reverseBitsOrder = false; break;
                    case 2: reverseBitsOrder = true;  break;
                    default: return value;                  // Cause a warning to be reported by the caller.
                }
                break;
            }
            /*
             * Number of bits per component. The array length should be the number of components in a
             * pixel (e.g. 3 for RGB values). Typically, all components have the same number of bits.
             * But the TIFF specification allows different values.
             */
            case Tags.BitsPerSample: {
                final Vector values = type.readVector(input(), count);
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
                                Resources.Keys.ConstantValueRequired_3, "BitsPerSample", input().filename, values));
                    }
                }
                break;
            }
            /*
             * The number of components per pixel. Usually 1 for bilevel, grayscale, and palette-color images,
             * and 3 for RGB images. Default value is 1.
             */
            case Tags.SamplesPerPixel: {
                samplesPerPixel = type.readShort(input(), count);
                break;
            }
            /*
             * Specifies that each pixel has N extra components. When this field is used, the SamplesPerPixel field
             * has a value greater than the PhotometricInterpretation field suggests. For example, a full-color RGB
             * image normally has SamplesPerPixel=3. If SamplesPerPixel is greater than 3, then the ExtraSamples field
             * describes the meaning of the extra samples. It may be an alpha channel, but not necessarily.
             */
            case Tags.ExtraSamples: {
                extraSamples = type.readVector(input(), count);
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
             * 2 = RGB. RGB value of (0,0,0) represents black, and (65535,65535,65535) represents white.
             * 3 = Palette color. The value of the component is used as an index into the RGB values of the ColorMap.
             * 4 = Transparency Mask the defines an irregularly shaped region of another image in the same TIFF file.
             */
            case Tags.PhotometricInterpretation: {
                final short value = type.readShort(input(), count);
                if (value < 0 || value > Byte.MAX_VALUE) return value;
                photometricInterpretation = (byte) value;
                break;
            }
            /*
             * The lookup table for palette-color images. This is represented by IndexColorModel in Java2D.
             * Color space is RGB if PhotometricInterpretation is "PaletteColor", or another color space otherwise.
             * In the RGB case, all the Red values come first, followed by all Green values, then all Blue values.
             * The number of values for each color is (1 << BitsPerSample) where 0 represents the minimum intensity
             * (black is 0,0,0) and 65535 represents the maximum intensity.
             */
            case Tags.ColorMap: {
                colorMap = type.readVector(input(), count);
                break;
            }
            /*
             * The minimum component value used. Default is 0.
             */
            case Tags.MinSampleValue: {
                final double v = type.readDouble(input(), count);
                if (Double.isNaN(minValue) || v < minValue) minValue = v;
                break;
            }
            /*
             * The maximum component value used. Default is {@code (1 << BitsPerSample) - 1}.
             * This field is for statistical purposes and should not to be used to affect the
             * visual appearance of an image, unless a map styling is applied.
             */
            case Tags.MaxSampleValue: {
                final double v = type.readDouble(input(), count);
                if (Double.isNaN(maxValue) || v > maxValue) maxValue = v;
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
            ////    Information related to the Coordinate Reference System and the bounding box.        ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * References the "GeoKeys" needed for building the Coordinate Reference System.
             * An array of unsigned SHORT values, which are primarily grouped into blocks of 4.
             * The first 4 values are special, and contain GeoKey directory header information.
             */
            case Tags.GeoKeyDirectory: {
                geoKeyDirectory = type.readVector(input(), count);
                break;
            }
            /*
             * Stores all of the 'double' valued GeoKeys, referenced by the GeoKeyDirectory.
             */
            case Tags.GeoDoubleParams: {
                numericGeoParameters = type.readVector(input(), count);
                break;
            }
            /*
             * Stores all the characters referenced by the GeoKeyDirectory. Should contains exactly one string
             * which will be splitted by CRSBuilder, but we allow an arbitrary amount as a paranoiac check.
             * Note that TIFF files use 0 as the end delimiter in strings (C/C++ convention).
             */
            case Tags.GeoAsciiParams: {
                final String[] values = type.readString(input(), count, encoding());
                switch (values.length) {
                    case 0:  break;
                    case 1:  asciiGeoParameters = values[0]; break;
                    default: asciiGeoParameters = String.join("\u0000", values).concat("\u0000"); break;
                }
                break;
            }
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
            ////    Should not write anything under 'metadata/contentInfo' node.                        ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * A string that describes the subject of the image.
             * For example, a user may wish to attach a comment such as "1988 company picnic" to an image.
             */
            case Tags.ImageDescription: {
                for (final String value : type.readString(input(), count, encoding())) {
                    reader.metadata.addTitle(value);
                }
                break;
            }
            /*
             * Person who created the image. Some older TIFF files used this tag for storing
             * Copyright information, but Apache SIS does not support this legacy practice.
             */
            case Tags.Artist: {
                for (final String value : type.readString(input(), count, encoding())) {
                    reader.metadata.addAuthor(value);
                }
                break;
            }
            /*
             * Copyright notice of the person or organization that claims the copyright to the image.
             * Example: “Copyright, John Smith, 1992. All rights reserved.”
             */
            case Tags.Copyright: {
                for (final String value : type.readString(input(), count, encoding())) {
                    reader.metadata.parseLegalNotice(value);
                }
                break;
            }
            /*
             * Date and time of image creation. The format is: "YYYY:MM:DD HH:MM:SS" with 24-hour clock.
             */
            case Tags.DateTime: {
                for (final String value : type.readString(input(), count, encoding())) {
                    reader.metadata.add(reader.getDateFormat().parse(value), DateType.CREATION);
                }
                break;
            }
            /*
             * The computer and/or operating system in use at the time of image creation.
             */
            case Tags.HostComputer: {
                for (final String value : type.readString(input(), count, encoding())) {
                    reader.metadata.addHostComputer(value);
                }
                break;
            }
            /*
             * Name and version number of the software package(s) used to create the image.
             */
            case Tags.Software: {
                for (final String value : type.readString(input(), count, encoding())) {
                    reader.metadata.addSoftwareReference(value);
                }
                break;
            }
            /*
             * Manufacturer of the scanner, video digitizer, or other type of equipment used to generate the image.
             * Synthetic images should not include this field.
             */
            case Tags.Make: {
                // TODO: is Instrument.citation.citedResponsibleParty.party.name an appropriate place?
                // what would be the citation title? A copy of Tags.Model?
                break;
            }
            /*
             * The model name or number of the scanner, video digitizer, or other type of equipment used to
             * generate the image.
             */
            case Tags.Model: {
                for (final String value : type.readString(input(), count, encoding())) {
                    reader.metadata.addInstrument(value);
                }
                break;
            }
            /*
             * The number of pixels per ResolutionUnit in the ImageWidth or ImageHeight direction.
             */
            case Tags.XResolution:
            case Tags.YResolution: {
                final double r = type.readDouble(input(), count);
                if (Double.isNaN(resolution) || r > resolution) {
                    resolution = r;
                }
                break;
            }
            /*
             * The unit of measurement for XResolution and YResolution.
             *
             *   1 = None. Used for images that may have a non-square aspect ratio.
             *   2 = Inch (default).
             *   3 = Centimeter.
             */
            case Tags.ResolutionUnit: {
                final short unit = type.readShort(input(), count);
                switch (unit) {
                    case 1:  resolutionUnit = null;             break;
                    case 2:  resolutionUnit = Units.INCH;       break;
                    case 3:  resolutionUnit = Units.CENTIMETRE; break;
                    default: return unit;                   // Cause a warning to be reported by the caller.
                }
                break;
            }
            /*
             * For black and white TIFF files that represent shades of gray, the technique used to convert
             * from gray to black and white pixels. The default value is 1 (nothing done on the image).
             *
             *   1 = No dithering or halftoning has been applied to the image data.
             *   2 = An ordered dither or halftone technique has been applied to the image data.
             *   3 = A randomized process such as error diffusion has been applied to the image data.
             */
            case Tags.Threshholding: {
                final short value = type.readShort(input(), count);
                switch (value) {
                    case 1:  break;
                    case 2:  if (cellWidth >= 0 || cellHeight >= 0) return null; else break;
                    case 3:  break;
                    default: return value;                  // Cause a warning to be reported by the caller.
                }
                cellWidth = cellHeight = (short) -value;
                break;
            }
            /*
             * The width and height of the dithering or halftoning matrix used to create
             * a dithered or halftoned bilevel file. Meaningful only if Threshholding = 2.
             */
            case Tags.CellWidth: {
                cellWidth = type.readShort(input(), count);
                break;
            }
            case Tags.CellLength: {
                cellHeight = type.readShort(input(), count);
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
                warning(Level.FINE, Resources.Keys.IgnoredTag_1, Tags.name(tag));
                break;
            }
        }
        return null;
    }

    /**
     * Sets the {@link #tileTagFamily} field to the given value if it does not conflict with previous value.
     *
     * @param  family  either {@link #TILE} or {@link #STRIP}.
     * @throws DataStoreContentException if {@link #tileTagFamily} is already set to another value.
     */
    private void setTileTagFamily(final byte family) throws DataStoreContentException {
        if (tileTagFamily != family && tileTagFamily != 0) {
            throw new DataStoreContentException(reader.resources().getString(
                    Resources.Keys.InconsistentTileStrip_1, input().filename));
        }
        tileTagFamily = family;
    }

    /**
     * Multiplies the given value by the number of bytes in one pixel,
     * or return -1 if the result is not an integer.
     *
     * @throws ArithmeticException if the result overflows.
     */
    private long pixelToByteCount(long value) {
        value = Math.multiplyExact(value, samplesPerPixel * (int) bitsPerSample);
        return (value % Byte.SIZE == 0) ? value / Byte.SIZE : -1;
    }

    /**
     * Computes the tile width or height from the other size,
     * or returns a negative number if the size can not be computed.
     *
     * @param  knownSize the tile width or height.
     * @return the tile width if the known size was height, or the tile height if the known size was width,
     *         or a negative number if the width or height can not be computed.
     * @throws ArithmeticException if the result overflows.
     */
    private int computeTileSize(final int knownSize) {
        final int n = tileByteCounts.size();
        if (n != 0) {
            final long count = tileByteCounts.longValue(0);
            int i = 0;
            do if (++i == n) {
                // At this point, we verified that all vector values are equal.
                final long length = pixelToByteCount(knownSize);
                if (count % length != 0) break;
                return Math.toIntExact(count / length);
            } while (tileByteCounts.longValue(i) == n);
        }
        return -1;
    }

    /**
     * Verifies that the mandatory tags are present and consistent with each others.
     * If a mandatory tag is absent, then there is a choice:
     *
     * <ul>
     *   <li>If the tag can be inferred from other tag values, performs that computation and logs a warning.</li>
     *   <li>Otherwise throws an exception.</li>
     * </ul>
     *
     * @throws DataStoreContentException if a mandatory tag is missing and can not be inferred.
     */
    final void validateMandatoryTags() throws DataStoreContentException {
        if (imageWidth  < 0) throw missingTag(Tags.ImageWidth);
        if (imageHeight < 0) throw missingTag(Tags.ImageLength);
        final short offsetsTag, byteCountsTag;
        switch (tileTagFamily) {
            case STRIP: {
                if (tileWidth  < 0) tileWidth  = Math.toIntExact(imageWidth);
                if (tileHeight < 0) tileHeight = Math.toIntExact(imageHeight);
                offsetsTag    = Tags.StripOffsets;
                byteCountsTag = Tags.StripByteCounts;
                break;
            }
            case TILE:  {
                offsetsTag    = Tags.TileOffsets;
                byteCountsTag = Tags.TileByteCounts;
                break;
            }
            default: {
                throw new DataStoreContentException(reader.resources().getString(
                        Resources.Keys.InconsistentTileStrip_1, input().filename));
            }
        }
        if (tileOffsets == null) {
            throw missingTag(offsetsTag);
        }
        if (samplesPerPixel == 0) {
            samplesPerPixel = 1;
            missingTag(Tags.SamplesPerPixel, 1, false);
        }
        if (bitsPerSample == 0) {
            bitsPerSample = 1;
            missingTag(Tags.BitsPerSample, 1, false);
        }
        if (colorMap != null) {
            ensureSameLength(Tags.ColorMap, Tags.BitsPerSample, colorMap.size(),  3 * (1 << bitsPerSample));
        }
        /*
         * All of tile width, height and length information should be provided. But if only one of them is missing,
         * we can compute it provided that the file does not use any compression method. If there is a compression,
         * then we set a bit for preventing the 'switch' block to perform a calculation but we let the code performs
         * the other checks in order to get an exception to be thrown with a good message.
         */
        int missing = !isPlanar && compression.equals(Compression.NONE) ? 0 : 0b1000;
        if (tileWidth      < 0)     missing |= 0b0001;
        if (tileHeight     < 0)     missing |= 0b0010;
        if (tileByteCounts == null) missing |= 0b0100;
        switch (missing) {
            case 0:
            case 0b1000: {          // Every thing is ok.
                break;
            }
            case 0b0001: {          // Compute missing tile width.
                tileWidth = computeTileSize(tileHeight);
                missingTag(Tags.TileWidth, tileWidth, true);
                break;
            }
            case 0b0010: {          // Compute missing tile height.
                tileHeight = computeTileSize(tileWidth);
                missingTag(Tags.TileLength, tileHeight, true);
                break;
            }
            case 0b0100: {          // Compute missing tile byte count.
                final long tileByteCount = pixelToByteCount(Math.multiplyExact(tileWidth, tileHeight));
                final long[] tileByteCountArray = new long[tileOffsets.size()];
                Arrays.fill(tileByteCountArray, tileByteCount);
                tileByteCounts = Vector.create(tileByteCountArray, true);
                missingTag(byteCountsTag, tileByteCount, true);
                break;
            }
            default: {
                final short tag;
                switch (Integer.lowestOneBit(missing)) {
                    case 0b0001: tag = Tags.TileWidth;  break;
                    case 0b0010: tag = Tags.TileLength; break;
                    default:     tag = byteCountsTag;   break;
                }
                throw missingTag(tag);
            }
        }
        /*
         * Log a warning if the tile offset and tile byte count vectors do not have the same length. Then
         * ensure that the number of tiles is equal to the expected number.  The formula below is the one
         * documented in the TIFF specification and reproduced in tileWidth & tileHeight fields javadoc.
         */
        ensureSameLength(offsetsTag, byteCountsTag, tileOffsets.size(), tileByteCounts.size());
        long expectedCount = Math.multiplyExact(
                Math.addExact(imageWidth,  tileWidth  - 1) / tileWidth,
                Math.addExact(imageHeight, tileHeight - 1) / tileHeight);
        if (isPlanar) {
            expectedCount = Math.multiplyExact(expectedCount, samplesPerPixel);
        }
        final int actualCount = Math.min(tileOffsets.size(), tileByteCounts.size());
        if (actualCount != expectedCount) {
            throw new DataStoreContentException(reader.resources().getString(Resources.Keys.UnexpectedTileCount_3,
                    input().filename, expectedCount, actualCount));
        }
    }

    /**
     * Completes the metadata with the information stored in the field of this IFD.
     * This method is invoked only if the user requested the ISO 19115 metadata.
     * This method creates a new {@code "metadata/contentInfo"} node for this image.
     * Information not under the {@code "metadata/contentInfo"} node will be merged
     * with the current content of the given {@code MetadataBuilder}.
     *
     * @param metadata  where to write metadata information. Caller should have already invoked
     *        {@link MetadataBuilder#setFormat(String)} before {@code completeMetadata(…)} calls.
     */
    final void completeMetadata(final MetadataBuilder metadata, final Locale locale)
            throws DataStoreContentException, FactoryException
    {
        metadata.newCoverage(false);
        if (compression != null) {
            metadata.addCompression(compression.name().toLowerCase(locale));
        }
        metadata.addMinimumSampleValue(minValue);
        metadata.addMaximumSampleValue(maxValue);
        /*
         * Add the resolution into the metadata. Our current ISO 19115 implementation restricts
         * the resolution unit to metres, but it may be relaxed in a future SIS version.
         */
        if (!Double.isNaN(resolution) && resolutionUnit != null) {
            metadata.addResolution(resolutionUnit.getConverterTo(Units.METRE).convert(resolution));
        }
        /*
         * Cell size is relevant only if the Threshholding TIFF tag value is 2. By convention in
         * this implementation class, other Threshholding values are stored as negative cell sizes:
         *
         *   -1 means that Threshholding is 1 or unspecified.
         *   -2 means that Threshholding is 2 but the matrix size has not yet been specified.
         *   -3 means that Threshholding is 3 (randomized process such as error diffusion).
         */
        switch (Math.min(cellWidth, cellHeight)) {
            case -1: {
                // Nothing to report.
                break;
            }
            case -3: {
                metadata.addProcessDescription(Resources.formatInternational(Resources.Keys.RandomizedProcessApplied));
                break;
            }
            default: {
                metadata.addProcessDescription(Resources.formatInternational(
                            Resources.Keys.DitheringOrHalftoningApplied_2,
                            (cellWidth  >= 0) ? cellWidth  : '?',
                            (cellHeight >= 0) ? cellHeight : '?'));
                break;
            }
        }
        /*
         * Add Coordinate Reference System built from GeoTIFF tags.  Note that the CRS may not exist,
         * in which case the CRS builder returns null. This is safe since all MetadataBuilder methods
         * ignore null values (a design choice because this pattern come very often).
         */
        if (geoKeyDirectory != null) {
            final CRSBuilder helper = new CRSBuilder(reader);
            metadata.add(helper.build(geoKeyDirectory, numericGeoParameters, asciiGeoParameters));
            helper.complete(metadata);
            geoKeyDirectory      = null;            // Not needed anymore, so let GC do its work.
            numericGeoParameters = null;
            asciiGeoParameters   = null;
        }
    }

    /**
     * Reports a warning with a message created from the given resource keys and parameters.
     *
     * @param level       the logging level for the message to log.
     * @param key         the {@code Resources} key of the message to format.
     * @param parameters  the parameters to put in the message.
     */
    private void warning(final Level level, final short key, final Object... parameters) {
        final LogRecord r = reader.resources().getLogRecord(level, key, parameters);
        reader.owner.warning(r);
    }

    /**
     * Verifies that the given tags have the same length and reports a warning if they do not.
     */
    private void ensureSameLength(final short tag1, final short tag2, final int length1, final int length2) {
        if (length1 != length2) {
            warning(Level.WARNING, Resources.Keys.MismatchedLength_4, Tags.name(tag1), Tags.name(tag2), length1, length2);
        }
    }

    /**
     * Reports a warning for a missing TIFF tag for which a default value can be computed.
     *
     * @param  missing   the numerical value of the missing tag.
     * @param  value     the default value or the computed value.
     * @param  computed  whether the default value has been computed.
     */
    private void missingTag(final short missing, final long value, final boolean computed) {
        warning(computed ? Level.WARNING : Level.FINE,
                computed ? Resources.Keys.ComputedValueForAttribute_2 : Resources.Keys.DefaultValueForAttribute_2,
                Tags.name(missing), value);
    }

    /**
     * Builds an exception for a missing TIFF tag for which no default value can be computed.
     *
     * @param  missing  the numerical value of the missing tag.
     */
    private DataStoreContentException missingTag(final short missing) {
        return new DataStoreContentException(reader.resources().getString(
                Resources.Keys.MissingValue_2, input().filename, Tags.name(missing)));
    }
}
