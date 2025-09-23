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
import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.nio.charset.Charset;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.RasterFormatException;
import static javax.imageio.plugins.tiff.GeoTIFFTagSet.*;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.*;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.DateType;
import org.opengis.util.GenericName;
import org.opengis.util.NameSpace;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.geotiff.base.Tags;
import org.apache.sis.storage.geotiff.base.Resources;
import org.apache.sis.storage.geotiff.base.Predictor;
import org.apache.sis.storage.geotiff.base.Compression;
import org.apache.sis.storage.geotiff.reader.Type;
import org.apache.sis.storage.geotiff.reader.GridGeometryBuilder;
import org.apache.sis.storage.geotiff.reader.ImageMetadataBuilder;
import org.apache.sis.storage.modifier.CoverageModifier;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.image.DataType;
import org.apache.sis.image.internal.shared.ColorModelBuilder;
import org.apache.sis.image.internal.shared.ColorModelFactory;
import org.apache.sis.image.internal.shared.SampleModelBuilder;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.pending.jdk.JDK18;


/**
 * An Image File Directory (FID) in a TIFF image.
 *
 * <h2>Thread-safety</h2>
 * Public methods should be synchronized because they can be invoked directly by users.
 * Package-private methods are not synchronized; synchronization is caller's responsibility.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="http://www.awaresystems.be/imaging/tiff/tifftags.html">TIFF Tag Reference</a>
 */
final class ImageFileDirectory extends DataCube {
    /**
     * Possible value for the {@link #tileTagFamily} field. That field tells whether image tiling
     * was specified using the {@code Tile*} family of TIFF tags or the {@code Strip*} family.
     * JPEG was also used to have its own set of tags.
     */
    private static final byte TILE = 1, STRIP = 2, JPEG=3;

    /**
     * Possible value for {@link #sampleFormat} specifying how to interpret each data sample in a pixel.
     * Those values are not necessarily the same as the ones documented in {@code TAG_SAMPLE_FORMAT}.
     * Default value is {@link #UNSIGNED}.
     */
    private static final byte SIGNED = 1, UNSIGNED = 0, FLOAT = 3;

    /**
     * The band to make visible. May become configurable in a future version.
     */
    private static final int VISIBLE_BAND = ColorModelFactory.DEFAULT_VISIBLE_BAND;

    /**
     * Index of the (potentially pyramided) image containing this Image File Directory (IFD).
     * All reduced-resolution (overviews) images are ignored when computing this index value.
     * If the TIFF file does not contain reduced-resolution (overview) images, then
     * {@code index} value is the same as the index of this IFD in the TIFF file.
     *
     * <p>If this IFD is a reduced-resolution (overview) image, then this index is off by one.
     * It has the value of the next pyramid. This is an artifact of the way index is computed
     * but should be invisible to user because they should not handle overviews directly.</p>
     */
    private final int index;

    /**
     * The identifier as a sequence number in the namespace of the {@link GeoTiffStore}.
     * The first image has the sequence number "1". This is computed when first needed.
     *
     * @see #getIdentifier()
     */
    private GenericName identifier;

    /**
     * Builder for the metadata. This field is reset to {@code null} when not needed anymore.
     */
    private ImageMetadataBuilder metadata;

    /**
     * {@code true} if this {@code ImageFileDirectory} has not yet read all deferred entries.
     * When this flag is {@code true}, the {@code ImageFileDirectory} is not yet ready for use.
     */
    boolean hasDeferredEntries;

    /**
     * {@code true} if {@link #validateMandatoryTags()} has already been invoked.
     */
    private boolean isValidated;

    /**
     * A general indication of the kind of data contained in this subfile, mainly useful when there
     * are multiple subfiles in a single TIFF file. This field is made up of a set of 32 flag bits.
     *
     * Bit 0 is 1 if the image is a reduced-resolution version of another image in this TIFF file.
     * Bit 1 is 1 if the image is a single page of a multi-page image (see PageNumber).
     * Bit 2 is 1 if the image defines a transparency mask for another image in this TIFF file (see PhotometricInterpretation).
     * Bit 4 indicates MRC imaging model as described in ITU-T recommendation T.44 [T.44] (See ImageLayer tag) - RFC 2301.
     *
     * @see #isReducedResolution()
     */
    private int subfileType;

    /**
     * The size of the image described by this FID, or -1 if the information has not been found.
     * The image may be much bigger than the memory capacity, in which case the image shall be tiled.
     *
     * <p><b>Note:</b>
     * the {@link #imageHeight} attribute is named {@code ImageLength} in TIFF specification.</p>
     *
     * @see #getExtent()
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
     * {@snippet lang="java" :
     *   tilesAcross   = (imageWidth  + tileWidth  - 1) / tileWidth;
     *   tilesDown     = (imageHeight + tileHeight - 1) / tileHeight;
     *   tilesPerImage = tilesAcross * tilesDown;
     *   }
     *
     * Note that {@link #imageWidth} can be less than {@code tileWidth} and/or {@link #imageHeight} can be less
     * than {@code tileHeight}. Such case means that the tiles are too large or that the tiled image is too small,
     * neither of which is recommended.
     *
     * <p><b>Note:</b>
     * the {@link #tileHeight} attribute is named {@code TileLength} in TIFF specification.</p>
     *
     * <h4>Strips considered as tiles</h4>
     * The TIFF specification also defines a {@code RowsPerStrip} tag, which is equivalent to the
     * height of tiles having the same width as the image. While the TIFF specification handles
     * "tiles" and "strips" separately, Apache SIS handles strips as a special kind of tiles where
     * only {@code tileHeight} is specified and {@code tileWidth} defaults to {@link #imageWidth}.
     *
     * @see #getTileSize()
     * @see #getNumTiles()
     */
    private int tileWidth = -1, tileHeight = -1;

    /**
     * For each tile, the byte offset of that tile, as compressed and stored on disk.
     * The offset is specified with respect to the beginning of the TIFF file.
     * Each tile has a location independent of the locations of other tiles
     *
     * <p>Offsets are ordered left-to-right and top-to-bottom. If {@link #isPlanar} is {@code true}
     * (i.e. components are stored in separate “component planes”), then the offsets for the first
     * component plane are stored first, followed by all the offsets for the second component plane,
     * and so on.</p>
     *
     * <h4>Strips considered as tiles</h4>
     * The TIFF specification also defines a {@code StripOffsets} tag, which contains the byte offset
     * of each strip. In Apache SIS implementation, strips are considered as a special kind of tiles
     * having a width equals to {@link #imageWidth}.
     */
    private Vector tileOffsets;

    /**
     * For each tile, the number of (compressed) bytes in that tile.
     * See {@link #tileOffsets} for a description of how the byte counts are ordered.
     *
     * <h4>Strips considered as tiles</h4>
     * The TIFF specification also defines a {@code RowsPerStrip} tag, which is the number
     * of bytes in the strip after compression. In Apache SIS implementation, strips are
     * considered as a special kind of tiles having a width equals to {@link #imageWidth}.
     */
    private Vector tileByteCounts;

    /**
     * Whether the tiling was specified using the {@code Tile*} family of TIFF tags or the {@code Strip*}
     * family of tags. Value can be {@link #TILE}, {@link #STRIP} or 0 if unspecified. This field is used
     * for error detection since each TIFF file shall use exactly one family of tags.
     */
    private byte tileTagFamily;

    /**
     * If {@code true}, the components are stored in separate “component planes”.
     * The default is {@code false}, which stands for the "chunky" format
     * (for example RGB data stored as RGBRGBRGB).
     */
    private boolean isPlanar;

    /**
     * How to interpret each data sample in a pixel.
     * Possible values are {@link #SIGNED}, {@link #UNSIGNED} or {@link #FLOAT}.
     */
    private byte sampleFormat;

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
    private boolean isBitOrderReversed;

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
     *
     * @see #getNumBands()
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
     * For example if {@code samplesPerPixel} is 4 and there is 1 extra component, then it is
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
     * <table class="sis">
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
     * The minimum or maximum sample value found in the image, with one value per band.
     * May be a vector of length 1 if the same single value applies to all bands.
     *
     * @see #getValidValues(int, double)
     */
    private Vector minValues, maxValues;

    /**
     * {@code true} if {@link #minValues} and {@link #maxValues} have been explicitly specified
     * in the TIFF file, or {@code false} if they have been inferred from {@link #bitsPerSample}.
     */
    private boolean isMinSpecified, isMaxSpecified;

    /**
     * The "no data" or background pixel value, or NaN if undefined.
     *
     * @see #getFillValue(boolean)
     */
    private double noData = Double.NaN;

    /**
     * The compression method, or {@code null} if unspecified. If the compression method is unknown
     * or unsupported we cannot read the image, but we still can read the metadata.
     *
     * @see #getCompression()
     */
    private Compression compression;

    /**
     * Mathematical operator that is applied to the image data before an encoding scheme is applied.
     * This is used mostly with LZW compression. Current values are:
     *
     * <ul>
     *   <li>1: No prediction scheme used before coding.</li>
     *   <li>2: Horizontal differencing.</li>
     * </ul>
     *
     * @see #getPredictor()
     */
    private Predictor predictor;

    /**
     * A helper class for building Coordinate Reference System and complete related metadata.
     * Contains the following information:
     *
     * <ul>
     *   <li>{@link GridGeometryBuilder#keyDirectory}</li>
     *   <li>{@link GridGeometryBuilder#numericParameters}</li>
     *   <li>{@link GridGeometryBuilder#asciiParameters}</li>
     *   <li>{@link GridGeometryBuilder#modelTiePoints}</li>
     * </ul>
     *
     * @see #getGridGeometry()
     */
    private GridGeometryBuilder referencing;

    /**
     * Returns {@link #referencing}, created when first needed. We delay its creation since
     * this object is not needed for ordinary TIFF files (i.e. without the GeoTIFF extension).
     * This method is invoked only during the parsing of TIFF tags. If no GeoTIFF information
     * is found, then this field keeps the {@code null} value.
     */
    private GridGeometryBuilder referencing() {
        if (referencing == null) {
            referencing = new GridGeometryBuilder();
        }
        return referencing;
    }

    /**
     * The grid geometry created by {@link GridGeometryBuilder#build(Reader, long, long)}.
     * It has 2 or 3 dimensions, depending on whether the CRS declares a vertical axis or not.
     *
     * @see #getGridGeometry()
     */
    private GridGeometry gridGeometry;

    /**
     * The sample dimensions, or {@code null} if not yet created.
     *
     * @see #getSampleDimensions()
     */
    private List<SampleDimension> sampleDimensions;

    /**
     * The image sample model, created when first needed. The raster size is the tile size.
     * Sample models with different size and number of bands can be derived from this model.
     *
     * @see #getSampleModel(int[])
     */
    private SampleModel sampleModel;

    /**
     * The image color model, created when first needed.
     *
     * @see #getColorModel(int[])
     */
    private ColorModel colorModel;

    /**
     * Creates a new image file directory.
     * The index arguments is used for metadata identifier only.
     *
     * @param reader  information about the input stream to read, the metadata and the character encoding.
     * @param index   the pyramided image index as a sequence number starting with 0 for the first pyramid.
     */
    ImageFileDirectory(final Reader reader, final int index) {
        super(reader);
        this.index = index;
        metadata = new ImageMetadataBuilder();
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
        return reader.store.encoding;
    }

    /**
     * Returns the identifier in the namespace of the {@link GeoTiffStore}.
     * The first image has the sequence number "1", optionally customized.
     * If this image is an overview, then its namespace should be the name of the base image
     * and the tip should be "overview-level" where "level" is a number starting at 1.
     *
     * <p>The returned value should never be empty. An empty value would be a
     * failure to {@linkplain #setOverviewIdentifier initialize overviews}.</p>
     *
     * @see #getMetadata()
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (identifier == null) {
                if (isReducedResolution()) {
                    // Should not happen because `setOverviewIdentifier(…)` should have been invoked.
                    return Optional.empty();
                }
                GenericName name = reader.store.createLocalName(String.valueOf(index + 1));
                name = name.toFullyQualifiedName();     // Because "1" alone is not very informative.
                final var source = new CoverageModifier.Source(reader.store, index, getDataType());
                identifier = reader.store.customizer.customize(source, name);
                if (identifier == null) {
                    identifier = name;
                }
            }
            return Optional.of(identifier);
        }
    }

    /**
     * Sets the identifier for an overview level. This is used only for a pyramid.
     * The image with finest resolution is used as the namespace for all overviews.
     *
     * @param  base      name of the image with finest resolution.
     * @param  overview  1 for the first overview, 2 for the next one, etc.
     */
    final void setOverviewIdentifier(final NameSpace base, final int overview) {
        identifier = reader.store.nameFactory.createLocalName(base, "overview-" + overview);
    }

    /**
     * Adds the value read from the current position in the given stream for the entry identified
     * by the given GeoTIFF tag. This method may store the value either in a field of this class,
     * or directly in the {@link ImageMetadataBuilder}. However, in the latter case, this method
     * should not write anything under the {@code "metadata/contentInfo"} node.
     *
     * @param  tag    the GeoTIFF tag to decode.
     * @param  type   the GeoTIFF type of the value to read.
     * @param  count  the number of values to read.
     * @return {@code null} on success, or the unrecognized value otherwise.
     * @throws IOException if an error occurred while reading the stream.
     * @throws ParseException if the value need to be parsed as date and the parsing failed.
     * @throws NumberFormatException if the value need to be parsed as number and the parsing failed.
     * @throws ArithmeticException if the value cannot be represented in the expected Java type.
     * @throws IllegalArgumentException if a value which was expected to be a singleton is not.
     * @throws UnsupportedOperationException if the given type is {@link Type#UNDEFINED}.
     * @throws DataStoreException if a logical error is found or an unsupported TIFF feature is used.
     */
    Object addEntry(final short tag, final Type type, final long count) throws Exception {
        switch (tag) {

            //  ╔═════════════════════════════════════════════════════════════════════════════════════╗
            //  ║                                                                                     ║
            //  ║    Essential information for being able to read the image at least as grayscale.    ║
            //  ║    In Java2D, following information are needed for building the SampleModel.        ║
            //  ║                                                                                     ║
            //  ╚═════════════════════════════════════════════════════════════════════════════════════╝

            /*
             * How the components of each pixel are stored.
             * 1 = Chunky format. The component values for each pixel are stored contiguously (for example RGBRGBRGB).
             * 2 = Planar format. For example, one plane of Red components, one plane of Green and one plane if Blue.
             */
            case TAG_PLANAR_CONFIGURATION: {
                final int value = type.readAsInt(input(), count);
                switch (value) {
                    case PLANAR_CONFIGURATION_CHUNKY: isPlanar = false; break;
                    case PLANAR_CONFIGURATION_PLANAR: isPlanar = true;  break;
                    default: return value;      // Cause a warning to be reported by the caller.
                }
                break;
            }
            /*
             * The number of columns in the image, i.e., the number of pixels per row.
             */
            case TAG_IMAGE_WIDTH: {
                imageWidth = type.readAsUnsignedLong(input(), count);
                break;
            }
            /*
             * The number of rows of pixels in the image.
             */
            case TAG_IMAGE_LENGTH: {
                imageHeight = type.readAsUnsignedLong(input(), count);
                break;
            }
            /*
             * The tile width in pixels. This is the number of columns in each tile.
             */
            case TAG_TILE_WIDTH: {
                setTileTagFamily(TILE);
                tileWidth = type.readAsInt(input(), count);
                break;
            }
            /*
             * The tile length (height) in pixels. This is the number of rows in each tile.
             */
            case TAG_TILE_LENGTH: {
                setTileTagFamily(TILE);
                tileHeight = type.readAsInt(input(), count);
                break;
            }
            /*
             * The number of rows per strip. This is considered by SIS as a special kind of tiles.
             * From this point of view, TileLength = RowPerStrip and TileWidth = ImageWidth.
             */
            case TAG_ROWS_PER_STRIP: {
                setTileTagFamily(STRIP);
                tileHeight = type.readAsInt(input(), count);
                break;
            }
            /*
             * The tile length (height) in pixels. This is the number of rows in each tile.
             */
            case TAG_TILE_OFFSETS: {
                setTileTagFamily(TILE);
                tileOffsets = type.readAsVector(input(), count);
                break;
            }
            /*
             * For each strip, the byte offset of that strip relative to the beginning of the TIFF file.
             * In Apache SIS implementation, strips are considered as a special kind of tiles.
             */
            case TAG_STRIP_OFFSETS: {
                setTileTagFamily(STRIP);
                tileOffsets = type.readAsVector(input(), count);
                break;
            }
            /*
             * The tile width in pixels. This is the number of columns in each tile.
             */
            case TAG_TILE_BYTE_COUNTS: {
                setTileTagFamily(TILE);
                tileByteCounts = type.readAsVector(input(), count);
                break;
            }
            /*
             * For each strip, the number of bytes in the strip after compression.
             * In Apache SIS implementation, strips are considered as a special kind of tiles.
             */
            case TAG_STRIP_BYTE_COUNTS: {
                setTileTagFamily(STRIP);
                tileByteCounts = type.readAsVector(input(), count);
                break;
            }
            /*
             * Legacy tags for JPEG formats, to be also interpreted as a tile.
             */
            case TAG_JPEG_INTERCHANGE_FORMAT: {
                setTileTagFamily(JPEG);
                tileOffsets = type.readAsVector(input(), count);
                break;
            }
            case TAG_JPEG_INTERCHANGE_FORMAT_LENGTH: {
                setTileTagFamily(JPEG);
                tileByteCounts = type.readAsVector(input(), count);
                break;
            }

            //  ╔═══════════════════════════════════════════════════════════════════════════════════╗
            //  ║                                                                                   ║
            //  ║    Information that define how the sample values are organized (their layout).    ║
            //  ║    In Java2D, following information are needed for building the SampleModel.      ║
            //  ║                                                                                   ║
            //  ╚═══════════════════════════════════════════════════════════════════════════════════╝

            /*
             * Compression scheme used on the image data.
             */
            case TAG_COMPRESSION: {
                final int value = type.readAsInt(input(), count);
                compression = Compression.valueOf(value);
                if (compression == Compression.UNKNOWN) {
                    return value;                           // Cause a warning to be reported by the caller.
                }
                break;
            }
            /*
             * Mathematical operator that is applied to the image data before an encoding scheme is applied.
             * 1=none, 2=horizontal differencing. More values may be added in the future.
             */
            case TAG_PREDICTOR: {
                final int value = type.readAsInt(input(), count);
                predictor = Predictor.valueOf(value);
                if (predictor == Predictor.UNKNOWN) {
                    return value;                           // Cause a warning to be reported by the caller.
                }
                break;
            }
            /*
             * The logical order of bits within a byte. If this value is 2, then
             * bits order shall be reversed in every bytes before decompression.
             */
            case TAG_FILL_ORDER: {
                final int value = type.readAsInt(input(), count);
                switch (value) {
                    case FILL_ORDER_LEFT_TO_RIGHT: isBitOrderReversed = false; break;
                    case FILL_ORDER_RIGHT_TO_LEFT: isBitOrderReversed = true;  break;
                    default: return value;      // Cause a warning to be reported by the caller.
                }
                break;
            }
            /*
             * How to interpret each data sample in a pixel. The size of data samples is still
             * specified by the BitsPerSample field.
             */
            case TAG_SAMPLE_FORMAT: {
                final int value = type.readAsInt(input(), count);
                switch (value) {
                    default: return value;      // Warning to be reported by the caller.
                    case SAMPLE_FORMAT_UNSIGNED_INTEGER: sampleFormat = UNSIGNED; break;
                    case SAMPLE_FORMAT_SIGNED_INTEGER:   sampleFormat = SIGNED;   break;
                    case SAMPLE_FORMAT_FLOATING_POINT:   sampleFormat = FLOAT;    break;
                    case SAMPLE_FORMAT_UNDEFINED: {
                        warning(Level.WARNING, Resources.Keys.UndefinedDataFormat_1, filename());
                        break;
                    }
                }
                break;
            }
            /*
             * Number of bits per component. The array length should be the number of components in a
             * pixel (e.g. 3 for RGB values). Typically, all components have the same number of bits.
             * But the TIFF specification allows different values.
             */
            case TAG_BITS_PER_SAMPLE: {
                final Vector values = type.readAsVector(input(), count);
                /*
                 * The current implementation requires that all `bitsPerSample` elements have the same value.
                 * This restriction may be revisited in future Apache SIS versions.
                 * Note: `count` is never zero when this method is invoked, so we do not need to check bounds.
                 */
                bitsPerSample = values.shortValue(0);
                final int length = values.size();
                for (int i = 1; i < length; i++) {
                    if (values.shortValue(i) != bitsPerSample) {
                        throw new DataStoreContentException(reader.resources().getString(
                                Resources.Keys.ConstantValueRequired_3, "BitsPerSample", filename(), values));
                    }
                }
                break;
            }
            /*
             * The number of components per pixel. Usually 1 for bilevel, grayscale, and palette-color images,
             * and 3 for RGB images. Default value is 1. May be greater than 3 if there is extra samples.
             */
            case TAG_SAMPLES_PER_PIXEL: {
                samplesPerPixel = type.readAsShort(input(), count);
                break;
            }
            /*
             * Specifies that each pixel has N extra components. When this field is used, the SamplesPerPixel field
             * has a value greater than the PhotometricInterpretation field suggests. For example, a full-color RGB
             * image normally has SamplesPerPixel=3. If SamplesPerPixel is greater than 3, then the ExtraSamples field
             * describes the meaning of the extra samples. It may be an alpha channel, but not necessarily.
             */
            case TAG_EXTRA_SAMPLES: {
                extraSamples = type.readAsVector(input(), count);
                break;
            }

            //  ╔═════════════════════════════════════════════════════════════════════════════════╗
            //  ║                                                                                 ║
            //  ║    Information related to the color palette or the meaning of sample values.    ║
            //  ║    In Java2D, following information are needed for building the ColorModel.     ║
            //  ║                                                                                 ║
            //  ╚═════════════════════════════════════════════════════════════════════════════════╝

            /*
             * The color space of the image data.
             * 0 = WhiteIsZero. For bilevel and grayscale images: 0 is imaged as white.
             * 1 = BlackIsZero. For bilevel and grayscale images: 0 is imaged as black.
             * 2 = RGB. RGB value of (0,0,0) represents black, and (65535,65535,65535) represents white.
             * 3 = Palette color. The value of the component is used as an index into the RGB values of the ColorMap.
             * 4 = Transparency Mask. Defines an irregularly shaped region of another image in the same TIFF file.
             */
            case TAG_PHOTOMETRIC_INTERPRETATION: {
                final short value = type.readAsShort(input(), count);
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
            case TAG_COLOR_MAP: {
                colorMap = type.readAsVector(input(), count);
                break;
            }
            /*
             * The minimum component value used. MinSampleValue is a single value that apply to all bands
             * while SMinSampleValue lists separated values for each band. Default is 0.
             */
            case TAG_MIN_SAMPLE_VALUE:
            case TAG_S_MIN_SAMPLE_VALUE: {
                minValues = extremum(minValues, type.readAsVector(input(), count), false);
                isMinSpecified = true;
                break;
            }
            /*
             * The maximum component value used. Default is {@code (1 << BitsPerSample) - 1}.
             * This field is for statistical purposes and should not to be used to affect the
             * visual appearance of an image, unless a map styling is applied.
             */
            case TAG_MAX_SAMPLE_VALUE:
            case TAG_S_MAX_SAMPLE_VALUE: {
                maxValues = extremum(maxValues, type.readAsVector(input(), count), true);
                isMaxSpecified = true;
                break;
            }

            //  ╔═════════════════════════════════════════════════════════════════════════════════╗
            //  ║                                                                                 ║
            //  ║    Information useful for defining the image role in a multi-images context.    ║
            //  ║                                                                                 ║
            //  ╚═════════════════════════════════════════════════════════════════════════════════╝

            /*
             * A general indication of the kind of data contained in this subfile, mainly useful when there
             * are multiple subfiles in a single TIFF file. This field is made up of a set of 32 flag bits.
             *
             * Bit 0 is 1 if the image is a reduced-resolution version of another image in this TIFF file.
             * Bit 1 is 1 if the image is a single page of a multi-page image (see PageNumber).
             * Bit 2 is 1 if the image defines a transparency mask for another image in this TIFF file (see PhotometricInterpretation).
             * Bit 4 indicates MRC imaging model as described in ITU-T recommendation T.44 [T.44] (See ImageLayer tag) - RFC 2301.
             */
            case TAG_NEW_SUBFILE_TYPE: {
                subfileType = type.readAsInt(input(), count);
                break;
            }
            /*
             * Old version (now deprecated) of above NewSubfileType.
             * 1 = full-resolution image data
             * 2 = reduced-resolution image data
             * 3 = a single page of a multi-page image (see PageNumber).
             */
            case TAG_SUBFILE_TYPE: {
                final int value = type.readAsInt(input(), count);
                switch (value) {
                    default: return value;                // Warning to be reported by the caller.
                    case SUBFILE_TYPE_FULL_RESOLUTION:    subfileType &= ~NEW_SUBFILE_TYPE_REDUCED_RESOLUTION; break;
                    case SUBFILE_TYPE_REDUCED_RESOLUTION: subfileType |=  NEW_SUBFILE_TYPE_REDUCED_RESOLUTION; break;
                    case SUBFILE_TYPE_SINGLE_PAGE:        subfileType |=  NEW_SUBFILE_TYPE_SINGLE_PAGE;        break;
                }
                break;
            }

            //  ╔════════════════════════════════════════════════════════════════════════════════════╗
            //  ║                                                                                    ║
            //  ║    Information related to the Coordinate Reference System and the bounding box.    ║
            //  ║                                                                                    ║
            //  ╚════════════════════════════════════════════════════════════════════════════════════╝

            /*
             * References the "GeoKeys" needed for building the Coordinate Reference System.
             * An array of unsigned SHORT values, which are primarily grouped into blocks of 4.
             * The first 4 values are special, and contain GeoKey directory header information.
             */
            case (short) TAG_GEO_KEY_DIRECTORY: {
                referencing().keyDirectory = type.readAsVector(input(), count);
                break;
            }
            /*
             * Stores all of the `double` valued GeoKeys, referenced by the GeoKeyDirectory.
             */
            case (short) TAG_GEO_DOUBLE_PARAMS: {
                referencing().numericParameters = type.readAsVector(input(), count);
                break;
            }
            /*
             * Stores all the characters referenced by the GeoKeyDirectory. Should contain exactly one string
             * which will be split by CRSBuilder, but we allow an arbitrary amount as a paranoiac check.
             * Note that TIFF files use 0 as the end delimiter in strings (C/C++ convention).
             */
            case (short) TAG_GEO_ASCII_PARAMS: {
                referencing().setAsciiParameters(type.readAsStrings(input(), count, encoding()));
                break;
            }
            /*
             * The orientation of the image with respect to the rows and columns.
             * This is an integer numeroted from 1 to 7 inclusive (see TIFF specification for meaning).
             */
            case TAG_ORIENTATION: {
                // TODO
                break;
            }
            /*
             * Specifies the "grid to CRS" conversion between the raster space and the model space.
             * If specified, the tag shall have the 16 values of a 4×4 matrix in row-major fashion.
             * The last matrix row (i.e. the last 4 values) should be [0 0 0 1].
             * The row before should be [0 0 0 0] if the conversion is two-dimensional.
             * This block does not reduce the number of dimensions from 3 to 2.
             * Only one of `ModelPixelScaleTag` and `ModelTransformationTag` should be used.
             */
            case (short) TAG_MODEL_TRANSFORMATION: {
                final Vector m = type.readAsVector(input(), count);
                final int n;
                switch (m.size()) {
                    case  6:                    // Assume 2D model with implicit [0 0 1] last row.
                    case  9: n = 3; break;      // Assume 2D model with full 3×3 matrix.
                    case 12:                    // Assume 3D model with implicit [0 0 0 1] last row.
                    case 16: n = 4; break;      // 3D model with full 4×4 matrix, as required by GeoTIFF spec.
                    default: return m;
                }
                referencing().setGridToCRS(m, n);
                break;
            }
            /*
             * A vector of 3 floating-point values defining the "grid to CRS" conversion without rotation.
             * The conversion is defined as below, when (I,J,K,X,Y,Z) is the tie point singleton record:
             *
             * ┌                       ┐
             * │   Sx   0    0    Tx   │       Tx = X - I/Sx
             * │   0   -Sy   0    Ty   │       Ty = Y + J/Sy
             * │   0    0    Sz   Tz   │       Tz = Z - K/Sz  (if not 0)
             * │   0    0    0    1    │
             * └                       ┘
             *
             * This block sets the translation column to NaN, meaning that it will need to be computed from
             * the tie point. Only one of `ModelPixelScaleTag` and `ModelTransformationTag` should be used.
             */
            case (short) TAG_MODEL_PIXEL_SCALE: {
                final Vector m = type.readAsVector(input(), count);
                final int size = m.size();
                if (size < 2 || size > 3) {     // Length should be exactly 3, but we make this reader tolerant.
                    return m;
                }
                referencing().setScaleFactors(m);
                break;
            }
            /*
             * The mapping from pixel coordinates to CRS coordinates as a sequence of (I,J,K, X,Y,Z) records.
             * This tag is also known as `Georeference`.
             */
            case (short) TAG_MODEL_TIE_POINT: {
                referencing().modelTiePoints = type.readAsVector(input(), count);
                break;
            }

            //  ╔════════════════════════════════════════════════════════════════════════════╗
            //  ║                                                                            ║
            //  ║    Metadata for discovery purposes, conditions of use, etc.                ║
            //  ║    Those metadata are not "critical" information for reading the image.    ║
            //  ║    Should not write anything under `metadata/contentInfo` node.            ║
            //  ║                                                                            ║
            //  ╚════════════════════════════════════════════════════════════════════════════╝

            /*
             * The name of the document from which this image was scanned.
             *
             * Destination: metadata/identificationInfo/citation/series/name
             */
            case TAG_DOCUMENT_NAME: {
                for (final String value : type.readAsStrings(input(), count, encoding())) {
                    metadata.addSeries(value);
                }
                break;
            }
            /*
             * The name of the page from which this image was scanned.
             *
             * Destination: metadata/identificationInfo/citation/series/page
             */
            case TAG_PAGE_NAME: {
                for (final String value : type.readAsStrings(input(), count, encoding())) {
                    metadata.addPage(value);
                }
                break;
            }
            /*
             * The page number of the page from which this image was scanned.
             * Should be a vector of length 2 containing the page number and
             * the total number of pages (with 0 meaning unavailable).
             *
             * Destination: metadata/identificationInfo/citation/series/page
             */
            case TAG_PAGE_NUMBER: {
                final Vector v = type.readAsVector(input(), count);
                final int n = v.size();
                if (n >= 1) {
                    metadata.addPage(v.intValue(0), (n >= 2) ? v.intValue(1) : 0);
                }
                break;
            }
            /*
             * A string that describes the subject of the image.
             * For example, a user may wish to attach a comment such as "1988 company picnic" to an image.
             *
             * Destination: metadata/identificationInfo/citation/title
             */
            case TAG_IMAGE_DESCRIPTION: {
                for (final String value : type.readAsStrings(input(), count, encoding())) {
                    metadata.addTitle(Strings.singleLine(" ", value));
                }
                break;
            }
            /*
             * Person who created the image. Some older TIFF files used this tag for storing
             * Copyright information, but Apache SIS does not support this legacy practice.
             *
             * Destination: metadata/identificationInfo/citation/party/name
             */
            case TAG_ARTIST: {
                for (final String value : type.readAsStrings(input(), count, encoding())) {
                    metadata.addAuthor(value);
                }
                break;
            }
            /*
             * Copyright notice of the person or organization that claims the copyright to the image.
             * Example: “Copyright, John Smith, 1992. All rights reserved.”
             *
             * Destination: metadata/identificationInfo/resourceConstraint
             */
            case (short) TAG_COPYRIGHT: {
                for (final String value : type.readAsStrings(input(), count, encoding())) {
                    metadata.parseLegalNotice(null, value);
                }
                break;
            }
            /*
             * Date and time of image creation. The format is: "YYYY:MM:DD HH:MM:SS" with 24-hour clock.
             *
             * Destination: metadata/identificationInfo/citation/date
             */
            case TAG_DATE_TIME: {
                for (final String value : type.readAsStrings(input(), count, encoding())) {
                    metadata.addCitationDate(reader.store.getDateFormat().parse(value).toInstant(),
                            DateType.CREATION, ImageMetadataBuilder.Scope.RESOURCE);
                }
                break;
            }
            /*
             * The computer and/or operating system in use at the time of image creation.
             *
             * Destination: metadata/resourceLineage/processStep/processingInformation/procedureDescription
             */
            case TAG_HOST_COMPUTER: {
                for (final String value : type.readAsStrings(input(), count, encoding())) {
                    metadata.addHostComputer(value);
                }
                break;
            }
            /*
             * Name and version number of the software package(s) used to create the image.
             *
             * Destination: metadata/resourceLineage/processStep/processingInformation/softwareReference/title
             */
            case TAG_SOFTWARE: {
                for (final String value : type.readAsStrings(input(), count, encoding())) {
                    metadata.addSoftwareReference(value);
                }
                break;
            }
            /*
             * Manufacturer of the scanner, video digitizer, or other type of equipment used to generate the image.
             * Synthetic images should not include this field.
             */
            case TAG_MAKE: {
                // TODO: is Instrument.citation.citedResponsibleParty.party.name an appropriate place?
                // what would be the citation title? A copy of Tags.Model?
                break;
            }
            /*
             * The model name or number of the scanner, video digitizer, or other type of equipment used to
             * generate the image.
             *
             * Destination: metadata/acquisitionInformation/platform/instrument/identifier
             */
            case TAG_MODEL: {
                for (final String value : type.readAsStrings(input(), count, encoding())) {
                    metadata.addInstrument(null, value);
                }
                break;
            }
            /*
             * The number of pixels per ResolutionUnit in the ImageWidth or ImageHeight direction.
             */
            case TAG_X_RESOLUTION:
            case TAG_Y_RESOLUTION: {
                metadata.setResolution(type.readAsDouble(input(), count));
                break;
            }
            /*
             * The unit of measurement for XResolution and YResolution.
             *
             *   1 = None. Used for images that may have a non-square aspect ratio.
             *   2 = Inch (default).
             *   3 = Centimeter.
             */
            case TAG_RESOLUTION_UNIT: {
                return metadata.setResolutionUnit(type.readAsInt(input(), count));
                // Non-null return value cause a warning to be reported by the caller.
            }
            /*
             * For black and white TIFF files that represent shades of gray, the technique used to convert
             * from gray to black and white pixels. The default value is 1 (nothing done on the image).
             *
             *   1 = No dithering or halftoning has been applied to the image data.
             *   2 = An ordered dither or halftone technique has been applied to the image data.
             *   3 = A randomized process such as error diffusion has been applied to the image data.
             */
            case TAG_THRESHHOLDING: {
                return metadata.setThreshholding(type.readAsShort(input(), count));
                // Non-null return value cause a warning to be reported by the caller.
            }
            /*
             * The width and height of the dithering or halftoning matrix used to create
             * a dithered or halftoned bilevel file. Meaningful only if Threshholding = 2.
             */
            case TAG_CELL_WIDTH:
            case TAG_CELL_LENGTH: {
                metadata.setCellSize(type.readAsShort(input(), count), tag == TAG_CELL_WIDTH);
                break;
            }

            //  ╔════════════════════════════════════════════════════════════╗
            //  ║                                                            ║
            //  ║    Defined by TIFF specification but currently ignored.    ║
            //  ║                                                            ║
            //  ╚════════════════════════════════════════════════════════════╝

            /*
             * For each string of contiguous unused bytes in a TIFF file, the number of bytes and the byte offset
             * in the string. Those tags are deprecated and do not need to be supported.
             */
            case TAG_FREE_BYTE_COUNTS:
            case TAG_FREE_OFFSETS:
            /*
             * For grayscale data, the optical density of each possible pixel value, plus the precision of that
             * information. This is ignored by most TIFF readers.
             */
            case TAG_GRAY_RESPONSE_CURVE:
            case TAG_GRAY_RESPONSE_UNIT: {
                warning(Level.FINE, Resources.Keys.IgnoredTag_1, Tags.name(tag));
                break;
            }

            //  ╔════════════════════════════════════════════╗
            //  ║                                            ║
            //  ║    Extensions defined by DGIWG or GDAL.    ║
            //  ║                                            ║
            //  ╚════════════════════════════════════════════╝

            case Tags.GEO_METADATA:
            case Tags.GDAL_METADATA: {
                metadata.addXML(reader.readXML(type, count, tag));
                break;
            }
            case Tags.GDAL_NODATA: {
                noData = type.readAsDouble(input(), count);
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
                    Resources.Keys.InconsistentTileStrip_1, filename()));
        }
        tileTagFamily = family;
    }

    /**
     * Computes the minimal or maximal values of the given vector.
     * Those vectors do not need to have the same length.
     * One of those two vector will be modified in-place.
     * This is a paranoiac safety in case a tag for minimal or maximum values appears more than once.
     * This duplication is not so unlikely since each extremum can be described by two different tags.
     *
     * @param  a    the first vector, or {@code null} if none.
     * @param  b    the new vector to combine with the existing one. Cannot be null.
     * @param  max  {@code true} for computing the maximal values, or {@code false} for the minimal value.
     */
    private static Vector extremum(Vector a, Vector b, final boolean max) {
        if (a != null) {
            int s = b.size();
            int i = a.size();
            if (i > s) {                            // If a vector is longer than b, swap a and b.
                i = s;
                final Vector t = a; a = b; b = t;
            }
            while (--i >= 0) {                      // At this point, `b` shall be the longest vector.
                final double va = a.doubleValue(i);
                final double vb = b.doubleValue(i);
                if (Double.isNaN(vb) || (max ? va > vb : va < vb)) {
                    b.set(i, va);
                }
            }
        }
        return b;
    }

    /**
     * Multiplies the given value by the number of bytes in one pixel,
     * or return 0 if the result is not an integer.
     *
     * @throws ArithmeticException if the result overflows.
     */
    private long pixelToByteCount(long value) {
        value = Math.multiplyExact(value, samplesPerPixel * (int) bitsPerSample);
        return (value % Byte.SIZE == 0) ? value / Byte.SIZE : 0;
    }

    /**
     * Computes the tile width or height from the other size,
     * or returns a negative number if the size cannot be computed.
     *
     * @param  knownSize  the tile width or height.
     * @return the tile width if the known size was height, or the tile height if the known size was width,
     *         or a negative number if the width or height cannot be computed.
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
                if (length == 0 || (count % length) != 0) break;
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
     * This method opportunistically computes default value of optional fields
     * when those values can be computed from other (usually mandatory) fields.
     *
     * @return {@code true} if the method has been invoked for the first time.
     * @throws DataStoreContentException if a mandatory tag is missing and cannot be inferred.
     */
    final boolean validateMandatoryTags() throws DataStoreContentException {
        if (isValidated) return false;
        if (imageWidth  < 0) throw missingTag((short) TAG_IMAGE_WIDTH);
        if (imageHeight < 0) throw missingTag((short) TAG_IMAGE_LENGTH);
        final short offsetsTag, byteCountsTag;
        switch (tileTagFamily) {
            case JPEG:                      // Handled as strips.
            case STRIP: {
                if (tileWidth  < 0) tileWidth  = Math.toIntExact(imageWidth);
                if (tileHeight < 0) tileHeight = Math.toIntExact(imageHeight);
                offsetsTag    = TAG_STRIP_OFFSETS;
                byteCountsTag = TAG_STRIP_BYTE_COUNTS;
                break;
            }
            case TILE:  {
                offsetsTag    = TAG_TILE_OFFSETS;
                byteCountsTag = TAG_TILE_BYTE_COUNTS;
                break;
            }
            default: {
                throw new DataStoreContentException(reader.resources().getString(
                        Resources.Keys.InconsistentTileStrip_1, filename()));
            }
        }
        if (tileOffsets == null) {
            throw missingTag(offsetsTag);
        }
        if (samplesPerPixel == 0) {
            samplesPerPixel = 1;
            missingTag((short) TAG_SAMPLES_PER_PIXEL, 1, false, false);
        }
        if (bitsPerSample == 0) {
            bitsPerSample = 1;
            missingTag((short) TAG_BITS_PER_SAMPLE, 1, false, false);
        }
        if (colorMap != null) {
            ensureSameLength((short) TAG_COLOR_MAP, (short) TAG_BITS_PER_SAMPLE, colorMap.size(),  3 * (1 << bitsPerSample));
        }
        if (sampleFormat != FLOAT) {
            long minValue, maxValue;
            if (sampleFormat == UNSIGNED) {
                minValue =  0L;
                maxValue = -1L;                 // All bits set to 1.
            } else {
                minValue = Long.MIN_VALUE;
                maxValue = Long.MAX_VALUE;
            }
            final int shift = Long.SIZE - bitsPerSample;
            if (shift >= 0 && shift < Long.SIZE) {
                minValue >>>= shift;
                maxValue >>>= shift;
                if (minValue < maxValue) {      // Exclude the unsigned long case since we cannot represent it.
                    minValues = extremum(minValues, Vector.createSequence(minValue, 0, samplesPerPixel), false);
                    maxValues = extremum(maxValues, Vector.createSequence(maxValue, 0, samplesPerPixel), true);
                }
            }
        }
        /*
         * All of tile width, height and length information should be provided. But if only one of them is missing,
         * we can compute it provided that the file does not use any compression method. If there is a compression,
         * then we set a bit for preventing the `switch` block to perform a calculation but we let the code performs
         * the other checks in order to get an exception thrown with a better message.
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
                missingTag((short) TAG_TILE_WIDTH, tileWidth, true, true);
                break;
            }
            case 0b0010: {          // Compute missing tile height.
                tileHeight = computeTileSize(tileWidth);
                missingTag((short) TAG_TILE_LENGTH, tileHeight, true, true);
                break;
            }
            case 0b0100: {          // Compute missing tile byte count in uncompressed case.
                final long tileByteCount = pixelToByteCount(Math.multiplyExact(tileWidth, tileHeight));
                if (tileByteCount == 0) {
                    throw missingTag(byteCountsTag);
                }
                final long[] tileByteCountArray = new long[tileOffsets.size()];
                Arrays.fill(tileByteCountArray, tileByteCount);
                tileByteCounts = Vector.create(tileByteCountArray, true);
                missingTag(byteCountsTag, tileByteCount, true, true);
                break;
            }
            default: {
                final short tag;
                switch (Integer.lowestOneBit(missing)) {
                    case 0b0001: tag = TAG_TILE_WIDTH;  break;
                    case 0b0010: tag = TAG_TILE_LENGTH; break;
                    default:     tag = byteCountsTag;   break;
                }
                throw missingTag(tag);
            }
        }
        /*
         * Report an error if the tile offset and tile byte count vectors do not have the same length.
         * Then ensure that the number of tiles is equal to the expected number.
         */
        ensureSameLength(byteCountsTag, offsetsTag, tileByteCounts.size(), tileOffsets.size());
        long expectedCount = getNumTiles();
        if (isPlanar) {
            expectedCount = Math.multiplyExact(expectedCount, samplesPerPixel);
        }
        final int actualCount = Math.min(tileOffsets.size(), tileByteCounts.size());
        if (actualCount != expectedCount) {
            throw new DataStoreContentException(reader.resources().getString(Resources.Keys.UnexpectedTileCount_3,
                    filename(), expectedCount, actualCount));
        }
        /*
         * If a "grid to CRS" conversion has been specified with only the scale factor,
         * we need to compute the translation terms now.
         */
        if (referencing != null && !referencing.validateMandatoryTags()) {
            listeners.warning(missingTag((short) TAG_MODEL_TIE_POINT));
        }
        isValidated = true;
        return true;
    }

    /**
     * Builds the metadata with the information stored in the fields of this IFD.
     * This method is invoked only if the user requested the ISO 19115 metadata.
     *
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ImageMetadataBuilder metadata = this.metadata;
        if (metadata == null) {
            /*
             * We enter in this block only if an exception occurred during the first attempt to build metadata.
             * If the user insists for getting metadata, fallback on the default (less complete) implementation.
             */
            return super.createMetadata();
        }
        this.metadata = null;       // Clear now in case an exception happens.
        final CoverageModifier.Source source = source();
        if (source != null) {
            if (metadata.getTitle() == null) {
                // Note: `GeoTiffStore.getMetadata()` relies on this value not being a `String`.
                metadata.addTitle(Vocabulary.formatInternational(Vocabulary.Keys.Image_1, index + 1));
            }
        }
        metadata.addIdentifier(getIdentifier().orElse(null), ImageMetadataBuilder.Scope.RESOURCE);
        /*
         * Add information about sample dimensions.
         *
         * Destination: metadata/contentInfo/attributeGroup/attribute
         */
        metadata.newCoverage(source != null && reader.store.customizer.isElectromagneticMeasurement(source));
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final List<SampleDimension> sampleDimensions = getSampleDimensions();
        for (int band = 0; band < samplesPerPixel; band++) {
            metadata.addNewBand(sampleDimensions.get(band));
            metadata.setBitPerSample(bitsPerSample);
            if (!metadata.hasSampleValueRange()) {
                if (isMinSpecified) metadata.addMinimumSampleValue(extremum(minValues, band).doubleValue());
                if (isMaxSpecified) metadata.addMaximumSampleValue(extremum(maxValues, band).doubleValue());
            }
        }
        /*
         * Add Coordinate Reference System built from GeoTIFF tags.
         * Note that the CRS may not exist.
         *
         * Destination: metadata/spatialRepresentationInfo and others.
         */
        if (referencing != null) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final GridGeometry gridGeometry = getGridGeometry();
            if (gridGeometry.isDefined(GridGeometry.ENVELOPE)) {
                metadata.addExtent(gridGeometry.getEnvelope(), listeners);
            }
            referencing.completeMetadata(gridGeometry, metadata);
        }
        /*
         * Add information about the file format.
         *
         * Destination: metadata/identificationInfo/resourceFormat
         */
        if (reader.store.hidden) {
            reader.store.setFormatInfo(metadata);   // Should be before `addCompression(…)`.
        }
        if (compression != null) {
            metadata.addCompression(CharSequences.upperCaseToSentence(compression.name()));
        }
        /*
         * End of metadata construction from TIFF tags.
         */
        metadata.finish(reader.store, listeners);
        final DefaultMetadata md = metadata.build();
        return (source != null) ? reader.store.customizer.customize(source, md) : md;
    }

    /**
     * Returns {@code true} if this image is a reduced resolution (overview) version
     * of another image in this TIFF file.
     */
    final boolean isReducedResolution() {
        return (subfileType & NEW_SUBFILE_TYPE_REDUCED_RESOLUTION) != 0;
    }

    /**
     * If this IFD has no grid geometry information, derives a grid geometry by applying a scale factor
     * on the grid geometry of another IFD. Information about bands are also copied if compatible.
     * This method should be invoked only when {@link #isReducedResolution()} is {@code true}.
     *
     * @param  fullResolution  the full-resolution image.
     * @param  scales  <var>size of full resolution image</var> / <var>size of this image</var> for each grid axis.
     */
    final void initReducedResolution(final ImageFileDirectory fullResolution, final double[] scales)
            throws DataStoreException, TransformException
    {
        if (referencing == null) {
            gridGeometry = new GridGeometry(fullResolution.getGridGeometry(), getExtent(), MathTransforms.scale(scales));
        }
        if (samplesPerPixel == fullResolution.samplesPerPixel) {
            sampleDimensions = fullResolution.getSampleDimensions();
        }
    }

    /**
     * Returns the source to declare when invoking a {@link CoverageModifier} method.
     * This method returns {@code null} if the {@link #index} value would be invalid.
     */
    private CoverageModifier.Source source() {
        return isReducedResolution() ? null : new CoverageModifier.Source(reader.store, index, getDataType());
    }

    /**
     * Returns an object containing the image size, the CRS and the conversion from pixel indices to CRS coordinates.
     * The grid geometry has 2 or 3 dimensions, depending on whether the CRS declares a vertical axis or not.
     *
     * <h4>Thread-safety</h4>
     * This method must be thread-safe because it can be invoked directly by the user.
     *
     * @see #getExtent()
     * @see #getTileSize()
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            GridGeometry domain = gridGeometry;
            if (domain == null) {
                if (referencing != null) try {
                    domain = referencing.build(reader.store.listeners(), imageWidth, imageHeight);
                } catch (FactoryException e) {
                    throw new DataStoreContentException(reader.resources().getString(Resources.Keys.CanNotComputeGridGeometry_1, filename()), e);
                } else {
                    // Fallback if the TIFF file has no GeoKeys.
                    domain = new GridGeometry(getExtent(), null, null);
                }
                final CoverageModifier.Source source = source();
                gridGeometry = (source != null) ? reader.store.customizer.customize(source, domain) : domain;
            }
            return domain;
        }
    }

    /**
     * Returns the image width and height without building the full grid geometry.
     *
     * @see #getTileSize()
     * @see #getGridGeometry()
     */
    final GridExtent getExtent() {
        return new GridExtent(imageWidth, imageHeight);
    }

    /**
     * Returns the minimum and maximum non-fill values in the specified band.
     * This is the values explicitly defined in <abbr>TIFF</abbr> tags if present,
     * otherwise the values derived from the data type if it is an integer type.
     *
     * @param  band     the band for which to get the minimum and maximum values.
     * @param  exclude  the fill value to exclude, or NaN if none.
     * @return the minimum and maximum values in the specified band.
     */
    private Optional<NumberRange<?>> getValidValues(final int band, final double exclude) {
        Number min = extremum(minValues, band);
        if (min != null) {
            Number max = extremum(maxValues, band);
            if (max != null) {
                return Optional.of(NumberRange.createBestFit(
                        sampleFormat == FLOAT,
                        min, min.doubleValue() != exclude,      // Always true if `exclude` is NaN.
                        max, max.doubleValue() != exclude));
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts a value from the vector of minimum or maximum values.
     *
     * @param  values  the vector of minimum or maximum values.
     * @param  band    the index of the valud to get.
     * @return the value for the given band, or {@code null} if none.
     */
    private static Number extremum(final Vector values, final int band) {
        return (values == null) ? null : values.get(Math.min(band, values.size() - 1));
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     *
     * <h4>Thread-safety</h4>
     * This method must be thread-safe because it can be invoked directly by the user.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (sampleDimensions == null) {
                /*
                 * For floating point type, `DataSubset.createWritableRaster(…)` has already replaced
                 * fill value by NaN. Therefore, it shall not appear anymore in the `SampleDimension`.
                 */
                final Number fill = (sampleFormat != FLOAT) ? getFillValue(true) : null;
                final DataType dataType = getDataType();
                final var dimensions = new SampleDimension[samplesPerPixel];
                final var builder = new SampleDimension.Builder();
                final boolean isIndexValid = !isReducedResolution();
                for (int band = 0; band < dimensions.length; band++) {
                    short nameKey = 0;
                    switch (photometricInterpretation) {
                        case PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO:
                        case PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO: nameKey = Vocabulary.Keys.Grayscale;  break;
                        case PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR: nameKey = Vocabulary.Keys.ColorIndex; break;
                        case PHOTOMETRIC_INTERPRETATION_RGB: {
                            switch (band) {
                                case 0: nameKey = Vocabulary.Keys.Red;   break;
                                case 1: nameKey = Vocabulary.Keys.Green; break;
                                case 2: nameKey = Vocabulary.Keys.Blue;  break;
                            }
                            break;
                        }
                    }
                    if (nameKey != 0) {
                        builder.setName(Vocabulary.formatInternational(nameKey));
                    } else {
                        builder.setName(band + 1);
                    }
                    /*
                     * If a "no data" value is present, declare both as background value and a qualitative category.
                     * The latter will cause the image to be converted to floating point during resample operations,
                     * with "no data" replaced by NaN. For completeness, we need to declare the range of real values.
                     */
                    final Optional<NumberRange<?>> sampleRange;
                    if (fill != null) {
                        builder.setBackground(fill);
                        sampleRange = getValidValues(band, fill.doubleValue());
                        sampleRange.ifPresent((range) -> {
                            if (!range.containsAny(fill)) {
                                builder.addQuantitative(null, range, null, null);
                                builder.addQualitative(null, fill, fill);
                            }
                        });
                    } else {
                        // Do not declare any category. It will be understood as "visualization only".
                        sampleRange = null;
                    }
                    /*
                     * Give a change to users to replace the above default categories by their own.
                     * The information provided to the user include the optional range of values,
                     * computed only when requested if it was not already computed.
                     */
                    final SampleDimension sd;
                    if (isIndexValid) {
                        var source = new CoverageModifier.BandSource(reader.store, index, band, samplesPerPixel, dataType) {
                            @Override public Optional<NumberRange<?>> getSampleRange() {
                                if (sampleRange != null) return sampleRange;
                                return getValidValues(getBandIndex(), Double.NaN);
                            }
                        };
                        sd = reader.store.customizer.customize(source, builder);
                    } else {
                        sd = builder.build();
                    }
                    dimensions[band] = sd;
                    builder.clear();
                }
                sampleDimensions = UnmodifiableArrayList.wrap(dimensions);
            }
            return sampleDimensions;        // Safe because unmodifiable.
        }
    }

    /**
     * Returns the Java2D sample model describing pixel type and layout.
     * The raster size is the tile size and the number of bands is {@link #samplesPerPixel}.
     * A sample model of different size or number of bands can be derived after construction
     * by call to one of {@code SampleModel.create…} methods.
     *
     * @param  bands  should always be {@code null} if this implementation.
     * @throws DataStoreContentException if the data type is not supported.
     *
     * @see SampleModel#createCompatibleSampleModel(int, int)
     * @see SampleModel#createSubsetSampleModel(int[])
     * @see #getColorModel(int[])
     * @see #getTileSize()
     */
    @Override
    protected SampleModel getSampleModel(final int[] bands) throws DataStoreContentException {
        assert Thread.holdsLock(getSynchronizationLock());
        if (bands != null) {
            return null;    // Let `TileGridResource` derive a model itself.
        }
        if (sampleModel == null) {
            RuntimeException error = null;
            final DataType type = getDataType();
            if (type != null) try {
                var size = new Dimension(tileWidth, tileHeight);
                var numBits = new int[samplesPerPixel];
                Arrays.fill(numBits, bitsPerSample);
                sampleModel = new SampleModelBuilder(type, size, numBits, isPlanar).build();
            } catch (IllegalArgumentException | RasterFormatException e) {
                error = e;
            }
            if (sampleModel == null) {
                Object message = type;
                if (message == null) {
                    final String format;
                    switch (sampleFormat) {
                        case SIGNED:   format = "int";      break;
                        case UNSIGNED: format = "unsigned"; break;
                        case FLOAT:    format = "float";    break;
                        default:       format = "unknown";  break;
                    }
                    message = format + ' ' + bitsPerSample + " bits";
                }
                throw new DataStoreContentException(Errors.format(Errors.Keys.UnsupportedType_1, message), error);
            }
        }
        return sampleModel;
    }

    /**
     * Returns the number of sample values for moving to the next row in a tile of the <abbr>TIFF</abbr> file.
     * The given {@code pixelStride} argument should be {@code sampleModel.getPixelString()} and the returned
     * value should be {@code sampleModel.getScanlineStride()}.
     *
     * @param  pixelStride  number of sample values for moving to the next pixel.
     * @return number of sample values for moving to the next row.
     */
    @Override
    final long getScanlineStride(final int pixelStride) {
        return Math.multiplyFull(pixelStride, tileWidth);
    }

    /**
     * Returns the number of components per pixel.
     */
    @Override
    protected int getNumBands() {
        return samplesPerPixel;
    }

    /**
     * Returns the size of tiles. This is also the size of the image sample model.
     * The number of dimensions is always 2 for {@code ImageFileDirectory}.
     *
     * @see #getExtent()
     * @see #getSampleModel(int[])
     */
    @Override
    protected int[] getTileSize() {
        return new int[] {tileWidth, tileHeight};
    }

    /**
     * Returns the total number of tiles. The formulas used in this method are derived from the formulas
     * documented in the TIFF specification and reproduced in {@link #tileWidth} and {@link #tileHeight}
     * fields javadoc.
     */
    @Override
    final long getNumTiles() {
        return Math.multiplyExact(
                JDK18.ceilDiv(imageWidth,  tileWidth),
                JDK18.ceilDiv(imageHeight, tileHeight));
    }

    /**
     * Returns the type of raster data. The enumeration values are restricted to types compatible with Java2D,
     * at the cost of using more bits than {@link #bitsPerSample} if there is no exact match.
     *
     * @return the type, or {@code null} if the type is not recognized.
     */
    private DataType getDataType() {
        switch (sampleFormat) {
            case SIGNED: {
                if (bitsPerSample <  Byte   .SIZE) return DataType.BYTE;
                if (bitsPerSample <= Short  .SIZE) return DataType.SHORT;
                if (bitsPerSample <= Integer.SIZE) return DataType.INT;
                break;
            }
            case UNSIGNED: {
                if (bitsPerSample <= Byte   .SIZE) return DataType.BYTE;
                if (bitsPerSample <= Short  .SIZE) return DataType.USHORT;
                if (bitsPerSample <= Integer.SIZE) return DataType.UINT;
                break;
            }
            case FLOAT: {
                if (bitsPerSample == Float  .SIZE) return DataType.FLOAT;
                if (bitsPerSample == Double .SIZE) return DataType.DOUBLE;
                break;
            }
        }
        return null;
    }

    /**
     * Returns the Java2D color model.
     *
     * @throws DataStoreContentException if the data type is not supported.
     *
     * @see #getSampleModel(int[])
     */
    @Override
    protected ColorModel getColorModel(final int[] bands) throws DataStoreContentException {
        assert Thread.holdsLock(getSynchronizationLock());
        if (bands != null) {
            return null;    // Let `TileGridResource` derive a model itself.
        }
        if (colorModel == null) {
            /*
             * The index of the alpha band is relative to extra samples.
             * Before to be used, the number of color bands must be added.
             * That number depends on the color interpretation.
             *
             * The alpha channel information should be used for all color models.
             * However, this is only partially honored in current implementation.
             */
            int alphaBand = -1;
            boolean isAlphaPremultiplied = false;
            if (extraSamples != null) {
                final int n = extraSamples.size();
                for (int i=0; i<n; i++) {
                    switch (extraSamples.intValue(i)) {
                        case EXTRA_SAMPLES_ASSOCIATED_ALPHA: isAlphaPremultiplied = true; break;
                        case EXTRA_SAMPLES_UNASSOCIATED_ALPHA: break;
                        default: continue;
                    }
                    alphaBand = i;
                    break;
                }
            }
            short missing = 0;              // Non-zero if there is a warning about missing information.
            switch (photometricInterpretation) {
                default: {                  // For any unrecognized code, fallback on grayscale with 0 as black.
                    unsupportedTagValue((short) TAG_PHOTOMETRIC_INTERPRETATION, photometricInterpretation);
                    break;
                }
                case -1: {
                    missing = TAG_PHOTOMETRIC_INTERPRETATION;
                    break;
                }
                case PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO: {
                    createSingleBandColorModel(Color.WHITE, Color.BLACK);
                    break;
                }
                case PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO: {
                    createSingleBandColorModel(Color.BLACK, Color.WHITE);
                    break;
                }
                case PHOTOMETRIC_INTERPRETATION_RGB: {
                    if (alphaBand >= 0) alphaBand += 3;     // Must add the number of color bands.
                    final var builder = new ColorModelBuilder().bitsPerSample(bitsPerSample)
                            .alphaBand(alphaBand).alphaPremultiplied(isAlphaPremultiplied);
                    if (getSampleModel(null) instanceof SinglePixelPackedSampleModel) {
                        colorModel = builder.createPackedRGB();
                    } else {
                        colorModel = builder.createBandedRGB();
                    }
                    break;
                }
                case PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR: {
                    if (colorMap == null) {
                        missing = TAG_COLOR_MAP;
                        break;
                    }
                    int gi = colorMap.size() / 3;
                    int bi = gi * 2;
                    final int[] ARGB = new int[gi];
                    for (int i=0; i < ARGB.length; i++) {
                        ARGB[i] = 0xFF000000
                                | ((colorMap.intValue(i   ) & 0xFF00) << Byte.SIZE)
                                | ((colorMap.intValue(gi++) & 0xFF00))
                                | ((colorMap.intValue(bi++) & 0xFF00) >>> Byte.SIZE);
                    }
                    int transparent = Double.isFinite(noData) ? (int) Math.round(noData) : -1;
                    colorModel = ColorModelFactory.createIndexColorModel(null, 0,
                            samplesPerPixel, VISIBLE_BAND, ARGB, true, transparent);
                    break;
                }
            }
            if (missing != 0) {
                missingTag(missing, "GrayScale", false, true);
            }
            if (colorModel == null) {
                createSingleBandColorModel(Color.BLACK, Color.WHITE);
            }
        }
        return colorModel;
    }

    /**
     * Creates a color model for a (theoretically) one-banded image.
     * May also be invoked as a fallback for image with more bands if more suitable color model couldn't be created.
     */
    private void createSingleBandColorModel(final Color zero, final Color high) throws DataStoreContentException {
        double min = 0;
        double max = Numerics.bitmask(bitsPerSample);   // Exclusive.
        switch (sampleFormat) {
            default: break;
            case FLOAT:  max = 1; break;
            case SIGNED: max /= 2; min = -max; break;
        }
        if (minValues != null) min = Math.max(min, minValues.doubleValue(VISIBLE_BAND));
        if (maxValues != null) max = Math.min(max, maxValues.doubleValue(VISIBLE_BAND) + 1);
        colorModel = ColorModelFactory.createColorScale(getSampleModel(null).getDataType(),
                        samplesPerPixel, VISIBLE_BAND, min, max, zero, high);
    }

    /**
     * Returns the fill value to be replaced by NaN at reading time.
     * This is possible only for images of floating point type.
     */
    @Override
    Number getReplaceableFillValue() {
        return (sampleFormat == FLOAT) ? noData : null;
    }

    /**
     * Returns the values to use for filling empty spaces in the raster, or {@code null} if none,
     * not different than zero or not valid for the target data type.
     * The zero value is excluded because tiles are already initialized to zero by default.
     */
    @Override
    protected Number[] getFillValues(final int[] bands) {
        final Number fill;
        if (sampleFormat == FLOAT) {
            fill = Double.NaN;
        } else if ((fill = getFillValue(false)) == null) {
            return null;
        }
        final var values = new Number[(bands != null) ? bands.length : getNumBands()];
        Arrays.fill(values, fill);
        return values;
    }

    /**
     * Returns the value to use for filling empty spaces in the raster, or {@code null} if none.
     * The exclusion of zero value is optional, controlled by the {@code acceptZero} argument.
     * If the value is outside the range of valid sample values, then {@code null} is returned.
     *
     * @param  acceptZero  whether to return a number for the zero value.
     */
    private Number getFillValue(final boolean acceptZero) {
        if (Double.isFinite(noData) && (acceptZero || noData != 0)) {
            final long min, max;
            switch (sampleFormat) {
                case UNSIGNED: max = 1L << (bitsPerSample    ); min =    0; break;
                case SIGNED:   max = 1L << (bitsPerSample - 1); min = ~max; break;
                default: return noData;
            }
            final long value = Math.round(noData);
            if (value >= min && value <= max && (acceptZero || value != 0)) {
                return Numbers.narrowestNumber(value);
            }
        }
        return null;
    }

    /**
     * Gets the stream position or the length in bytes of compressed tile arrays in the GeoTIFF file.
     * Values in the returned vector are {@code long} primitive type.
     *
     * @param  length  {@code false} for requesting tile offsets, or {@code true} for tile lengths.
     * @return stream position (relative to file beginning) or length of compressed tile arrays, in bytes.
     */
    @Override
    Vector getTileArrayInfo(final boolean length) {
        return length ? tileByteCounts : tileOffsets;
    }

    /**
     * Returns {@code true} if {@link Integer#reverseBytes(int)} should be invoked on each byte read.
     * This mode is very rare and should apply only to uncompressed image or CCITT 1D/2D compressions.
     */
    @Override
    boolean isBitOrderReversed() {
        return isBitOrderReversed;
    }

    /**
     * Returns the compression method, or {@code null} if unspecified.
     */
    @Override
    Compression getCompression() {
        return compression;
    }

    /**
     * Returns the mathematical operator that is applied to the image data before an encoding scheme is applied.
     */
    @Override
    Predictor getPredictor() {
        return (predictor != null) ? predictor : Predictor.NONE;
    }

    /**
     * Reports a warning with a message created from the given resource keys and parameters.
     * Note that the log record will not necessarily be sent to the logging framework;
     * if the user has registered at least one listener, then the record will be sent to the listeners instead.
     *
     * <p>This method sets the {@linkplain LogRecord#setSourceClassName(String) source class name} and
     * {@linkplain LogRecord#setSourceMethodName(String) source method name} to hard-coded values.
     * Those values assume that the warnings occurred indirectly from a call to {@link GeoTiffStore#components()}.
     * We do not report private classes or methods as the source of warnings.</p>
     *
     * @param  level       the logging level for the message to log.
     * @param  key         the {@code Resources} key of the message to format.
     * @param  parameters  the parameters to put in the message.
     */
    private void warning(final Level level, final short key, final Object... parameters) {
        final LogRecord record = reader.resources().createLogRecord(level, key, parameters);
        record.setSourceClassName(GeoTiffStore.class.getName());
        record.setSourceMethodName("components()");
        // Logger name will be set by listeners.warning(record).
        listeners.warning(record);
    }

    /**
     * Verifies that the given tags have the same length and reports a warning if they do not.
     *
     * @param  tag1      the TIFF tag with inconsistent length.
     * @param  tag2      the TIFF tag used as a reference.
     * @param  actual    length of list associated to {@code tag1}.
     * @param  expected  length of list associated to {@code tag2}.
     */
    private void ensureSameLength(final short tag1, final short tag2, final int actual, final int expected) {
        if (actual != expected) {
            warning(Level.WARNING, Resources.Keys.MismatchedLength_4, Tags.name(tag1), Tags.name(tag2), actual, expected);
        }
    }

    /**
     * Reports a warning for a missing TIFF tag for which a default value can be computed.
     *
     * @param  missing   the numerical value of the missing tag.
     * @param  value     the default value or the computed value.
     * @param  computed  whether the default value has been computed.
     * @param  warning   whether the problem should be reported as a warning.
     */
    private void missingTag(final short missing, final Object value, final boolean computed, final boolean warning) {
        warning(warning ? Level.WARNING : Level.FINE,
                computed ? Resources.Keys.ComputedValueForAttribute_2 : Resources.Keys.DefaultValueForAttribute_2,
                Tags.name(missing), value);
    }

    /**
     * Reports a warning for an unsupported TIFF tag value.
     *
     * @param  tag    the numerical value of the tag.
     * @param  value  the unsupported value.
     */
    private void unsupportedTagValue(final short tag, final Object value) {
        warning(Level.WARNING, Resources.Keys.UnsupportedTagValue_2, Tags.name(tag), value);
    }

    /**
     * Builds an exception for a missing TIFF tag for which no default value can be computed.
     *
     * @param  missing  the numerical value of the missing tag.
     */
    private DataStoreContentException missingTag(final short missing) {
        return new DataStoreContentException(reader.resources().getString(
                Resources.Keys.MissingValue_2, filename(), Tags.name(missing)));
    }
}
