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

import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Deque;
import java.util.Queue;
import java.util.Set;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RasterFormatException;
import javax.imageio.plugins.tiff.TIFFTag;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.*;
import static javax.imageio.plugins.tiff.GeoTIFFTagSet.*;
import javax.measure.IncommensurableException;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.coverage.privy.ImageUtilities;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.IncompatibleResourceException;
import org.apache.sis.storage.ReadOnlyStorageException;
import org.apache.sis.storage.base.MetadataFetcher;
import org.apache.sis.storage.geotiff.writer.TagValue;
import org.apache.sis.storage.geotiff.writer.TileMatrix;
import org.apache.sis.storage.geotiff.writer.GeoEncoder;
import org.apache.sis.storage.geotiff.writer.ReformattedImage;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.io.stream.UpdatableWrite;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.math.Fraction;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.CannotEvaluateException;


/**
 * An image writer for GeoTIFF files. This writer duplicates the implementations performed by other libraries,
 * but we nevertheless provide our own writer in Apache SIS for better control on the internal file structure,
 * such as keeping metadata close to each other (for Cloud Optimized GeoTIFF) and tiles order.
 * This image writer can also handle <i>Big TIFF</i> images.
 *
 * <p>This writer supports only the tile layout. It does not support the writing of stripped images,
 * because they are not useful for geospatial applications. This restriction does not reduce the set
 * of Java2D images that this writer can encode.</p>
 *
 * <p>The TIFF format specification version 6.0 (June 3, 1992) is available
 * <a href="https://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf">here</a>.</p>
 *
 * @author  Erwan Roussel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Writer extends IOBase implements Flushable {
    /**
     * BigTIFF code for unsigned 64-bits integer type.
     *
     * @see Type#ULONG
     */
    static final short TIFF_ULONG = 16;

    /**
     * Sizes of a few TIFF tags used in this writer.
     *
     * @see #writeTag(short, short, int[])
     * @see #writeTag(short, short, double[])
     */
    private static final byte[] TYPE_SIZES = new byte[TIFF_ULONG + 1];
    static {
        TYPE_SIZES[TIFFTag.TIFF_ASCII]       =                      // TIFF uses US-ASCII encoding as bytes.
        TYPE_SIZES[TIFFTag.TIFF_BYTE]        =
        TYPE_SIZES[TIFFTag.TIFF_SBYTE]       = Byte.BYTES;
        TYPE_SIZES[TIFFTag.TIFF_SHORT]       =
        TYPE_SIZES[TIFFTag.TIFF_SSHORT]      = Short.BYTES;
        TYPE_SIZES[TIFFTag.TIFF_LONG]        =
        TYPE_SIZES[TIFFTag.TIFF_SLONG]       = Integer.BYTES;       // What TIFF calls "long" is Java integer.
        TYPE_SIZES[TIFFTag.TIFF_RATIONAL]    =
        TYPE_SIZES[TIFFTag.TIFF_SRATIONAL]   = Integer.BYTES * 2;
        TYPE_SIZES[TIFFTag.TIFF_FLOAT]       = Float.BYTES;
        TYPE_SIZES[TIFFTag.TIFF_DOUBLE]      = Double.BYTES;
        TYPE_SIZES[TIFFTag.TIFF_IFD_POINTER] = Integer.BYTES;       // Assuming standard TIFF (not BigTIFF).
        TYPE_SIZES[        TIFF_ULONG]       = Long.BYTES;
    }

    /**
     * Common number of tags which will be written. This amount is for tiled grayscale images with no metadata
     * and no statistics. For stripped images, there is one less tag. For RGB images, there is one more tag.
     * For color maps, there are two more tags. This number is only a hint for avoiding the need to update
     * this information if the number appears to be right.
     */
    static final int COMMON_NUMBER_OF_TAGS = 16;

    /**
     * The processor to use for transforming the image before to write it.
     * Created only if needed.
     *
     * @see #processor()
     */
    private ImageProcessor processor;

    /**
     * The stream where to write the data.
     */
    private final ChannelDataOutput output;

    /**
     * Whether the lengths and offsets shall be written as 64-bits integers instead of 32-bits integers.
     *
     * @see #getFormat()
     */
    private final boolean isBigTIFF;

    /**
     * Whether to disable the <abbr>TIFF</abbr> requirement that tile sizes are multiple of 16 pixels.
     */
    private final boolean anyTileSize;

    /**
     * Index of the image to write. This information is not needed by the writer, but is
     * needed by {@link WritableStore} for determining the "effectively added resource".
     */
    int imageIndex;

    /**
     * Offset where to write the offset of current image, or {@code null} when writing the first image of the file.
     * If null, the IFD offset is assumed already written and the {@linkplain #output} already at that position.
     * Otherwise, the value at the specified offset is initially zero and will be updated after it become known.
     *
     * <p>After the image has been successfully written, this pointer is replaced by the pointer where the next
     * image (if any) can be appended.</p>
     */
    private UpdatableWrite<?> currentIFD;

    /**
     * All values that couldn't be written immediately.
     * Values shall be sorted in increasing order of stream position.
     */
    private final Deque<UpdatableWrite<?>> deferredWrites = new ArrayDeque<>();

    /**
     * Write operations for tag having data too large for fitting inside a IFD tag entry.
     * The writing of those data need to be delayed somewhere after the sequence of entries.
     */
    private final Queue<TagValue> largeTagData = new ArrayDeque<>();

    /**
     * Number of TIFF tag entries in the image being written.
     * This is a temporary information used during the writing of an Image File Directory (IFD).
     */
    private int numberOfTags;

    /**
     * Creates a new GeoTIFF writer which will write data in the given output.
     * The byte order of the given output determines the byte order of the GeoTIFF file to write.
     *
     * @param  store    the store writing data.
     * @param  output   where to write the bytes.
     * @param  options  the format modifiers (BigTIFF, COG…), or {@code null} if none.
     * @throws IOException if an error occurred while writing the first bytes to the stream.
     */
    Writer(final GeoTiffStore store, final ChannelDataOutput output, final FormatModifier[] options)
            throws IOException, DataStoreException
    {
        super(store);
        this.output = output;
        isBigTIFF   = ArraysExt.contains(options, FormatModifier.BIG_TIFF);
        anyTileSize = ArraysExt.contains(options, FormatModifier.ANY_TILE_SIZE);
        /*
         * Write the TIFF file header before first IFD. Stream position matter and must start at zero.
         * Note that it does not necessarily mean that the stream has no bytes before current position.
         */
        output.relocateOrigin();
        output.writeShort((output.buffer.order() == ByteOrder.LITTLE_ENDIAN) ? LITTLE_ENDIAN : BIG_ENDIAN);
        output.writeShort(isBigTIFF ? BIG_TIFF : CLASSIC);
        if (isBigTIFF) {
            output.writeShort((short) Long.BYTES);            // Byte size of offsets.
            output.writeShort((short) 0);                     // Constant.
            output.writeLong(Long.BYTES + 4*Short.BYTES);     // Position of the first IFD.
        } else {
            output.writeInt(Integer.BYTES + 2*Short.BYTES);
        }
    }

    /**
     * Creates a new writer which will append images a the end of an existing file.
     * It is caller's responsibility to keep reader and writer positions consistent.
     * This is done by invoking {@link #synchronize(Reader, boolean)} before and after
     * write operations.
     *
     * @param  reader  the reader of the existing GeoTIFF file.
     * @throws ReadOnlyStorageException if the channel is read-only.
     * @throws DataStoreException if the writer cannot be created.
     * @throws IOException if an I/O error occurred.
     */
    Writer(final Reader reader) throws IOException, DataStoreException {
        super(reader.store);
        isBigTIFF = (reader.intSizeExpansion != 0);
        anyTileSize = false;
        try {
            output = new ChannelDataOutput(reader.input);
        } catch (ClassCastException e) {
            throw new ReadOnlyStorageException(store.readOrWriteOnly(0), e);
        }
        moveAfterExisting(reader);
    }

    /**
     * Prepares the writer to write after the last image.
     *
     * @param  reader  the reader of images.
     */
    final void moveAfterExisting(final Reader reader) throws IOException, DataStoreException {
        Class<? extends Number> type = isBigTIFF ? Long.class : Integer.class;
        currentIFD = UpdatableWrite.ofZeroAt(reader.offsetOfWritableIFD(), type);
        imageIndex = reader.getImageCacheSize();
    }

    /**
     * Ensures that the reader and writer positions are consistent. It is caller's responsibility to invoke
     * {@link #flush()} before to invoke {@code synchronize(reader, true)}, unless the write operation failed.
     * In the latter case, the caller should cancel the write operation if possible.
     *
     * @param  reader  the reader, or {@code null} if none.
     * @param  finish  {@code false} if invoked before write operations, or {@code true} if invoked after.
     */
    final void synchronize(final Reader reader, final boolean finish) throws IOException {
        if (reader != null) {
            if (finish) {
                output.yield(reader.input);
            } else {
                reader.input.yield(output);
            }
        }
    }

    /**
     * {@return the modifiers (BigTIFF, COG…) used by this writer}.
     */
    @Override
    public final Set<FormatModifier> getModifiers() {
        return isBigTIFF ? Set.of(FormatModifier.BIG_TIFF) : Set.of();
    }

    /**
     * {@return the processor to use for reformatting the image before to write it}.
     * The processor is created only when this method is first invoked.
     */
    private ImageProcessor processor() {
        if (processor == null) {
            processor = new ImageProcessor();
        }
        return processor;
    }

    /**
     * Encodes the given image to the output stream given at construction time.
     * The image is appended after any previous images written before the given one.
     * This method does not handle pyramids such as Cloud Optimized GeoTIFF (COG).
     * It is caller responsibility to append image overviews if a pyramid is wanted.
     *
     * @param  image     the image to encode.
     * @param  grid      mapping from pixel coordinates to "real world" coordinates, or {@code null} if none.
     * @param  metadata  title, author and other information, or {@code null} if none.
     * @return offset if {@link #output} where the Image File Directory (IFD) starts.
     * @throws RasterFormatException if the raster uses an unsupported sample model.
     * @throws ArithmeticException if an integer overflow occurs.
     * @throws IOException if an error occurred while writing to the output.
     * @throws DataStoreException if the given {@code image} has a property
     *         which is not supported by TIFF specification or by this writer.
     */
    public final long append(final RenderedImage image, final GridGeometry grid, final Metadata metadata)
            throws IOException, DataStoreException
    {
        final var exportable = new ReformattedImage(image, this::processor, anyTileSize);
        /*
         * Offset where the image IFD will start. The current version appends the new image at the end of file.
         * A future version could perform a more extensive search for free space in the middle of the file.
         * It could be useful when images have been deleted.
         */
        final long offsetIFD = output.length();
        if (currentIFD != null) {
            currentIFD.setAsLong(offsetIFD);
            writeOrQueue(currentIFD);
            output.seek(offsetIFD);
        }
        /*
         * Write the Image File Directory (IFD) followed by the raster data.
         */
        try {
            final TileMatrix tiles;
            try {
                tiles = writeImageFileDirectory(exportable, grid, metadata, false);
            } finally {
                largeTagData.clear();       // For making sure that there is no memory retention.
            }
            tiles.writeRasters(output);
            wordAlign(output);
            tiles.writeOffsetsAndLengths(output);
            flush();
            currentIFD = tiles.nextIFD;     // Set only after the operation succeeded.
        } catch (Throwable e) {
            try {
                deferredWrites.clear();
                output.truncate(offsetIFD);
                if (currentIFD != null) {
                    currentIFD.setAsLong(0);
                    currentIFD.update(output);
                }
            } catch (Throwable more) {
                e.addSuppressed(more);
            }
            throw e;
        }
        return offsetIFD;
    }

    /**
     * Writes the Image File Directory (IFD) of the given image at the current {@link #output} position.
     * This method does not write the pixel values. Those values must be written by the caller.
     * This separation makes possible to write directories in any order compared to pixel data.
     *
     * @param  image       the image for which to write the IFD.
     * @param  grid        mapping from pixel coordinates to "real world" coordinates, or {@code null} if none.
     * @param  metadata    title, author and other information, or {@code null} if none.
     * @param  oveverview  whether the image is an overview of another image.
     * @return handler for writing offsets and lengths of the tiles to write.
     * @throws IOException if an error occurred while writing to the output.
     * @throws DataStoreException if the given {@code image} has a property
     *         which is not supported by TIFF specification or by this writer.
     */
    private TileMatrix writeImageFileDirectory(final ReformattedImage image, final GridGeometry grid, final Metadata metadata,
            final boolean overview) throws IOException, DataStoreException
    {
        final SampleModel sm = image.exportable.getSampleModel();
        Compression compression = store.getCompression().orElse(Compression.DEFLATE);
        if (!ImageUtilities.isIntegerType(sm)) {
            compression = compression.withPredictor(PREDICTOR_NONE);
        }
        /*
         * Extract all image properties and metadata that we will need to encode in the Image File Directory.
         * It allows us to know if we will be able to encode the image before we start writing in the stream,
         * so that the TIFF file is not corrupted if we cannot write that image. It is also more convenient
         * because the tags need to be written in increasing code order, which causes ColorModel-related tags
         * (for example) to be interleaved with other aspects.
         */
        numberOfTags = COMMON_NUMBER_OF_TAGS;       // Only a guess at this stage. Real number computed later.
        if (compression.usePredictor()) numberOfTags++;
        final int colorInterpretation = image.getColorInterpretation();
        if (colorInterpretation == PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR) {
            numberOfTags++;
        }
        if (image.extraSamples != null) {
            numberOfTags++;
        }
        final int   sampleFormat  = image.getSampleFormat();
        final int[] bitsPerSample = sm.getSampleSize();
        final int   numBands      = sm.getNumBands();
        final int   numPlanes, planarConfiguration;
        if (sm instanceof BandedSampleModel) {
            planarConfiguration = PLANAR_CONFIGURATION_PLANAR;
            numPlanes = numBands;
        } else {
            planarConfiguration = PLANAR_CONFIGURATION_CHUNKY;
            numPlanes = 1;
        }
        /*
         * Metadata (optional) and GeoTIFF. They are managed by separated classes.
         */
        final double[][] statistics = image.statistics(numBands);
        final  short[][] shortStats = toShorts(statistics, sampleFormat);
        final MetadataFetcher<String> mf = new MetadataFetcher<>(store.dataLocale) {
            @Override protected boolean accept(final CitationDate info) {
                return super.accept(info) || creationDate != null;          // Limit to a singleton.
            }

            @Override protected String convertDate(final Date date) {
                return store.getDateFormat().format(date);
            }
        };
        mf.accept(metadata);
        GeoEncoder geoKeys = null;
        if (grid != null) try {
            geoKeys = new GeoEncoder(store.listeners());
            geoKeys.write(grid, mf);
        } catch (IncompleteGridGeometryException | CannotEvaluateException | TransformException e) {
            throw new IncompatibleResourceException(e.getMessage(), e).addAspect("gridGeometry");
        } catch (FactoryException | IncommensurableException | RuntimeException e) {
            throw new DataStoreReferencingException(e.getMessage(), e);
        }
        /*
         * Conversion factor from physical size to pixel size. "Physical size" here should be understood as
         * paper size, as suggested by the units of measurement which are restricted to inch or centimeters.
         * This is not very useful for geospatial applications, except as aspect ratio.
         */
        final Fraction xres = new Fraction(1, 1);       // TODO
        final Fraction yres = xres;
        /*
         * If the image has any unsupported feature, the exception should have been thrown before this point.
         * Now start writing the entries. The entries in an IFD must be sorted in ascending order by tag code.
         */
        output.flush();       // Make room in the buffer for increasing our ability to modify previous values.
        largeTagData.clear();
        final UpdatableWrite<?> tagCountWriter =
                isBigTIFF ? UpdatableWrite.of(output, (long)  numberOfTags)
                          : UpdatableWrite.of(output, (short) numberOfTags);

        final var tiling = new TileMatrix(image.exportable, numPlanes, bitsPerSample,
                                          compression.method, compression.level, compression.predictor);
        /*
         * Reminder: TIFF tags should be written in increasing numerical order.
         */
        numberOfTags = 0;
        writeTag((short) TAG_NEW_SUBFILE_TYPE,           (short) TIFFTag.TIFF_LONG,  overview ? 1 : 0);
        writeTag((short) TAG_IMAGE_WIDTH,                (short) TIFFTag.TIFF_LONG,  image.exportable.getWidth());
        writeTag((short) TAG_IMAGE_LENGTH,               (short) TIFFTag.TIFF_LONG,  image.exportable.getHeight());
        writeTag((short) TAG_BITS_PER_SAMPLE,            (short) TIFFTag.TIFF_SHORT, bitsPerSample);
        writeTag((short) TAG_COMPRESSION,                (short) TIFFTag.TIFF_SHORT, compression.method.code);
        writeTag((short) TAG_PHOTOMETRIC_INTERPRETATION, (short) TIFFTag.TIFF_SHORT, colorInterpretation);
        writeTag((short) TAG_DOCUMENT_NAME,              /* TIFF_ASCII */            mf.series);
        writeTag((short) TAG_IMAGE_DESCRIPTION,          /* TIFF_ASCII */            mf.title);
        writeTag((short) TAG_MODEL,                      /* TIFF_ASCII */            mf.instrument);
        writeTag((short) TAG_STRIP_OFFSETS,              /* TIFF_LONG  */            tiling, true);
        writeTag((short) TAG_SAMPLES_PER_PIXEL,          (short) TIFFTag.TIFF_SHORT, numBands);
        writeTag((short) TAG_ROWS_PER_STRIP,             /* TIFF_LONG  */            tiling, true);
        writeTag((short) TAG_STRIP_BYTE_COUNTS,          /* TIFF_LONG  */            tiling, true);
        writeTag((short) TAG_MIN_SAMPLE_VALUE,           /* TIFF_SHORT */            shortStats[0]);
        writeTag((short) TAG_MAX_SAMPLE_VALUE,           /* TIFF_SHORT */            shortStats[1]);
        writeTag((short) TAG_X_RESOLUTION,               /* TIFF_RATIONAL */         xres);
        writeTag((short) TAG_Y_RESOLUTION,               /* TIFF_RATIONAL */         yres);
        writeTag((short) TAG_PLANAR_CONFIGURATION,       (short) TIFFTag.TIFF_SHORT, planarConfiguration);
        writeTag((short) TAG_RESOLUTION_UNIT,            (short) TIFFTag.TIFF_SHORT, RESOLUTION_UNIT_NONE);
        writeTag((short) TAG_SOFTWARE,                   /* TIFF_ASCII */            mf.software);
        writeTag((short) TAG_DATE_TIME,                  /* TIFF_ASCII */            mf.creationDate);
        writeTag((short) TAG_ARTIST,                     /* TIFF_ASCII */            mf.party);
        writeTag((short) TAG_HOST_COMPUTER,              /* TIFF_ASCII */            mf.procedure);
        if (compression.usePredictor()) {
            writeTag((short) TAG_PREDICTOR, (short) TIFFTag.TIFF_SHORT, compression.predictor.code);
        }
        if (colorInterpretation == PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR) {
            writeColorPalette((IndexColorModel) image.exportable.getColorModel(), 1L << bitsPerSample[0]);
        }
        writeTag((short) TAG_TILE_WIDTH,                 /* TIFF_LONG */             tiling, false);
        writeTag((short) TAG_TILE_LENGTH,                /* TIFF_LONG */             tiling, false);
        writeTag((short) TAG_TILE_OFFSETS,               /* TIFF_LONG */             tiling, false);
        writeTag((short) TAG_TILE_BYTE_COUNTS,           /* TIFF_LONG */             tiling, false);
        writeTag((short) TAG_EXTRA_SAMPLES,              /* TIFF_SHORT */            image.extraSamples);
        writeTag((short) TAG_SAMPLE_FORMAT,              (short) TIFFTag.TIFF_SHORT, sampleFormat);
        writeTag((short) TAG_S_MIN_SAMPLE_VALUE,         (short) TIFFTag.TIFF_FLOAT, statistics[0]);
        writeTag((short) TAG_S_MAX_SAMPLE_VALUE,         (short) TIFFTag.TIFF_FLOAT, statistics[1]);
        if (geoKeys != null) {
            writeTag((short) TAG_MODEL_TRANSFORMATION, (short) TIFFTag.TIFF_DOUBLE, geoKeys.modelTransformation());
            writeTag((short) TAG_GEO_KEY_DIRECTORY,    /* TIFF_SHORT */             geoKeys.keyDirectory());
            writeTag((short) TAG_GEO_DOUBLE_PARAMS,    (short) TIFFTag.TIFF_DOUBLE, geoKeys.doubleParams());
            writeTag((short) TAG_GEO_ASCII_PARAMS,     /* TIFF_ASCII */             geoKeys.asciiParams());
        }
        /*
         * At this point, all tags have been written. Update the number of tags,
         * then write all values that couldn't be written directly in the tags.
         */
        tagCountWriter.setAsLong(numberOfTags);
        writeOrQueue(tagCountWriter);
        tiling.nextIFD = writeOffset(0);
        for (final TagValue tag : largeTagData) {
            UpdatableWrite<?> offset = tag.writeHere(output);
            if (offset != null) deferredWrites.add(offset);
        }
        return tiling;
    }

    /**
     * Writes a tag related to the location of the data. We use a separated method instead of
     * inlining this code inside the {@code writeImageFileDirectory(…)} method for readability.
     * It allows us to keep {@code writeImageFileDirectory(…)} formatted more like a table.
     */
    private void writeTag(final short tag, final TileMatrix tiling, final boolean useStrips) throws IOException {
        if (tiling.useStrips() == useStrips) {
            final int value;
            switch (tag) {
                case TAG_TILE_WIDTH:        value = tiling.tileWidth;  break;
                case TAG_TILE_LENGTH:
                case TAG_ROWS_PER_STRIP:    value = tiling.tileHeight; break;
                case TAG_TILE_OFFSETS:
                case TAG_STRIP_OFFSETS:     tiling.offsetsTag = writeTag(tag, tiling.offsets); return;
                case TAG_TILE_BYTE_COUNTS:
                case TAG_STRIP_BYTE_COUNTS: tiling.lengthsTag = writeTag(tag, (short) TIFFTag.TIFF_LONG, tiling.lengths); return;
                default: throw new AssertionError(tag);
            }
            writeTag(tag, (short) TIFFTag.TIFF_LONG, value);
        }
    }

    /**
     * Writes a 32-bits or 64-bits offset, depending on whether the format is classic TIFF or BigTIFF.
     *
     * @param  offset  an initial guess of the offset value.
     * @return a handler for updating later the offset with its actual value.
     * @throws IOException if an error occurred while writing to the output.
     */
    private UpdatableWrite<?> writeOffset(final long offset) throws IOException {
        return isBigTIFF ? UpdatableWrite.of(output, offset)
                         : UpdatableWrite.of(output, (int) offset);
        // No need to check the validity of above cast because the value is only a guess.
    }

    /**
     * Forces 16-bits word alignment.
     * The TIFF specification requires that tag values are aligned.
     *
     * @param  channel  the channel on which to apply 16-bits word alignment.
     * @throws IOException if an error occurred while writing to the output stream.
     */
    private static void wordAlign(final ChannelDataOutput output) throws IOException {
        if ((output.getStreamPosition() & 1) != 0) {
            output.writeByte(0);
        }
    }

    /**
     * If the sample format is integer, cast statistics to integer type and clears the given array.
     * Otherwise do nothing. This is used for choosing only one of {@code TAG_MIN_SAMPLE_VALUE} and
     * {@code TAG_S_MIN_SAMPLE_VALUE} tags (same for maximum).
     *
     * @param  statistics    the statistic to potentially cast and clear.
     * @param  sampleFormat  the sample format.
     * @return statistics for the tags restricted to integer types.
     */
    private static short[][] toShorts(final double[][] statistics, final int sampleFormat) {
        final short[][] c = new short[statistics.length][];
        final long min, max;
        switch (sampleFormat) {
            case SAMPLE_FORMAT_UNSIGNED_INTEGER: min = 0;               max = 0xFFFF;          break;
            case SAMPLE_FORMAT_SIGNED_INTEGER:   min = Short.MIN_VALUE; max = Short.MAX_VALUE; break;
            default: return c;
        }
        for (int j=0; j < c.length; j++) {
            final double[] source = statistics[j];
            if (source != null) {
                final short[] target = new short[source.length];
                for (int i=0; i < source.length; i++) {
                    target[i] = (short) Math.max(min, Math.min(max, Math.round(source[i])));
                    // Unsigned values may look signed after the cast, but this is okay.
                }
                c[j] = target;
            }
        }
        return c;
    }

    /**
     * Writes a new tag except for the value. This method ensures that the buffer has enough room for a full tag entry,
     * so the caller can append an {@code int} (classical TIFF) or a {@code long} (big TIFF) directly in the buffer.
     *
     * @param  tag    the code of the tag to write, usually a constant defined by the TIFF specification.
     * @param  type   one of the {@link TIFFTag} constants such as {@code TIFF_SHORT} or {@code TIFF_LONG}.
     * @param  count  number of values.
     * @return number of bytes available for the IFD entry value.
     * @throws IOException if an error occurred while writing to the output.
     * @throws ArithmeticException if the count is too large for the TIFF format in use.
     */
    private int writeTagHeader(final short tag, final short type, final long count) throws IOException {
        numberOfTags++;
        output.ensureBufferAccepts(isBigTIFF
                ? (2*Short.BYTES + 2*Long.BYTES)
                : (2*Short.BYTES + 2*Integer.BYTES));
        final ByteBuffer buffer = output.buffer;
        buffer.putShort(tag);
        buffer.putShort(type);
        if (isBigTIFF) {
            buffer.putLong(count);
            return Long.BYTES;
        } else if ((count & Numerics.HIGH_BITS_MASK) == 0) {
            // Note: unsigned integer may look negative after cast, this is okay.
            buffer.putInt((int) count);
            return Integer.BYTES;
        } else {
            throw new ArithmeticException(errors().getString(Errors.Keys.IntegerOverflow_1, Integer.SIZE));
        }
    }

    /**
     * Writes a tag value which is potentially too large for fitting in the IFD entry.
     *
     * @param  tag    the code of the tag to write, usually a constant defined by the TIFF specification.
     * @param  type   one of the {@link TIFFTag} constants such as {@code TIFF_SHORT} or {@code TIFF_LONG}.
     * @param  count  number of values.
     * @throws IOException if an error occurred while writing to the output.
     * @throws ArithmeticException if the count is too large for the TIFF format in use.
     */
    private TagValue writeLargeTag(final short tag, final short type, final long count, final TagValue deferred) throws IOException {
        final long r = writeTagHeader(tag, type, count) - TYPE_SIZES[type] * count;
        if (r >= 0) {
            deferred.markAndWrite(output);
            output.repeat(r, (byte) 0);
        } else {
            deferred.mark(writeOffset(0));
            largeTagData.add(deferred);
        }
        return deferred;
    }

    /**
     * Writes the color map tag.
     *
     * @param  cm     color model from which to read color values.
     * @param  count  number of colors to write, <strong>not</strong> multiplied by 3 for the RGB bands.
     * @throws IOException if an error occurred while writing to the output.
     */
    private void writeColorPalette(final IndexColorModel cm, final long count) throws IOException {
        final int numBands = 3;
        writeLargeTag((short) TAG_COLOR_MAP, (short) TIFFTag.TIFF_SHORT, count * numBands, new TagValue() {
            @Override protected void write(final ChannelDataOutput output) throws IOException {
                final int n = (int) Math.min(cm.getMapSize(), count);
                for (int band=0; band < numBands; band++) {
                    for (int i=0; i<n; i++) {
                        final int c;
                        switch (band) {
                            case 0: c = cm.getRed  (i); break;
                            case 1: c = cm.getGreen(i); break;
                            case 2: c = cm.getBlue (i); break;
                            default: throw new AssertionError(band);
                        }
                        output.writeShort(c | (c << Byte.SIZE));
                    }
                    output.repeat((count - n) * Short.BYTES, (byte) 0);
                }
            }
        });
    }

    /**
     * Writes a tag with string values stored as ASCII characters.
     * The list of valid tag code is defined by TIFF specification.
     *
     * @param  tag     the code of the tag to write, usually a constant defined by the TIFF specification.
     * @param  values  the values to write, or {@code null} if none.
     * @throws IOException if an error occurred while writing to the output.
     * @throws ArithmeticException if the combined string length is too large.
     */
    private void writeTag(final short tag, final List<String> values) throws IOException {
        if (values == null) {
            return;
        }
        long count = 0;
        final var chars = new byte[values.size()][];
        for (int i=0; i<chars.length; i++) {
            String value = values.get(i).trim();
            if (StandardCharsets.US_ASCII.equals(store.encoding)) {
                value = CharSequences.toASCII(value).toString();
            }
            final byte[] ascii = value.getBytes(store.encoding);
            int length = 0;
            for (final byte c : ascii) {
                if (c != 0) ascii[length++] = c;        // Remove any NUL character that may appear in the string.
            }
            if (length != 0) {
                count += length + 1L;                   // Count shall include the trailing NUL character.
                chars[i] = ArraysExt.resize(ascii, length);
            }
        }
        if (count != 0) {
            writeLargeTag(tag, (short) TIFFTag.TIFF_ASCII, count, new TagValue() {
                @Override protected void write(final ChannelDataOutput output) throws IOException {
                    for (final byte[] c : chars) {
                        if (c != null) {
                            output.write(c);
                            output.writeByte(0);
                            wordAlign(output);
                        }
                    }
                }
            });
        }
    }

    /**
     * Writes a tag as a rational number. Rational numbers are made of two integers: the numerator and denominator,
     * in that order. In BigTIFF format, those two numbers fit in the entry and this method returns {@code null}.
     * In classical format, those two numbers do not fit and must be stored in an array after the directory entries.
     * In such case, this method saves a handle for performing that deferred write operation later.
     *
     * @param  tag    the code of the tag to write, usually a constant defined by the TIFF specification.
     * @param  value  numerator and denominator of the rational number to store, or {@code null} if none.
     * @throws IOException if an error occurred while writing to the output.
     */
    private void writeTag(final short tag, final Fraction value) throws IOException {
        if (value == null) {
            return;
        }
        writeLargeTag(tag, (short) TIFFTag.TIFF_RATIONAL, 1, new TagValue() {
            @Override protected void write(final ChannelDataOutput output) throws IOException {
                output.writeInt(value.numerator);
                output.writeInt(value.denominator);
            }
        });
    }

    /**
     * Writes a tag with values stored as 32 or 64 bits floating point numbers.
     * The list of valid tag codes is defined by TIFF specification.
     *
     * @param  tag     the code of the tag to write, usually a constant defined by the TIFF specification.
     * @param  type    {@code TIFF_FLOAT} or {@code TIFF_DOUBLE}.
     * @param  values  the values to write as floating point values.
     * @return a handler for rewriting the data if the array content changes.
     * @throws IOException if an error occurred while writing to the output.
     */
    private TagValue writeTag(final short tag, final short type, final double[] values) throws IOException {
        if (values == null || values.length == 0) {
            return null;
        }
        return writeLargeTag(tag, type, values.length, new TagValue() {
            @Override protected void write(final ChannelDataOutput output) throws IOException {
                switch (type) {
                    default: throw new AssertionError(type);
                    case TIFFTag.TIFF_DOUBLE: output.writeDoubles(values); break;
                    case TIFFTag.TIFF_FLOAT: {
                        for (final double value : values) {
                            output.writeFloat((float) value);
                        }
                        break;
                    }
                }
            }
        });
    }

    /**
     * Writes a tag with an arbitrary number of values stored as 64 or 32 bits unsigned integers.
     * The number of bits depends on whether this writer is writing BigTIFF or classic TIFF.
     *
     * @param  tag     the code of the tag to write, usually a constant defined by the TIFF specification.
     * @param  values  the values to write as unsigned 64 or 32 bits integers.
     * @return a handler for rewriting the data if the array content changes.
     * @throws IOException if an error occurred while writing to the output.
     */
    private TagValue writeTag(final short tag, final long[] values) throws IOException {
        if (values == null || values.length == 0) {
            return null;
        }
        final short type = isBigTIFF ? TIFF_ULONG : TIFFTag.TIFF_LONG;
        return writeLargeTag(tag, type, values.length, new TagValue() {
            @Override protected void write(final ChannelDataOutput output) throws IOException {
                switch (type) {
                    default: throw new AssertionError(type);
                    case TIFF_ULONG: output.writeLongs(values); break;
                    case TIFFTag.TIFF_LONG: {
                        for (final long value : values) {
                            output.writeInt(Math.toIntExact(value));
                        }
                        break;
                    }
                }
            }
        });
    }

    /**
     * Writes a tag with an arbitrary number of values stored as 16 bits integers.
     *
     * @param  tag     the code of the tag to write, usually a constant defined by the TIFF specification.
     * @param  values  the values to write as 16 bits integers.
     * @return a handler for rewriting the data if the array content changes.
     * @throws IOException if an error occurred while writing to the output.
     */
    private TagValue writeTag(final short tag, final short[] values) throws IOException {
        if (values == null || values.length == 0) {
            return null;
        }
        return writeLargeTag(tag, (short) TIFFTag.TIFF_SHORT, values.length, new TagValue() {
            @Override protected void write(final ChannelDataOutput output) throws IOException {
                output.writeShorts(values);
            }
        });
    }

    /**
     * Writes a tag with an arbitrary number of values stored as 16 or 32 bits unsigned integers.
     * The list of valid tag codes is defined by TIFF specification.
     *
     * @param  tag     the code of the tag to write, usually a constant defined by the TIFF specification.
     * @param  type    {@code TIFF_SHORT} or {@code TIFF_LONG}.
     * @param  values  the values to write as unsigned integers.
     * @return a handler for rewriting the data if the array content changes.
     * @throws IOException if an error occurred while writing to the output.
     */
    private TagValue writeTag(final short tag, final short type, final int[] values) throws IOException {
        if (values == null || values.length == 0) {
            return null;
        }
        return writeLargeTag(tag, type, values.length, new TagValue() {
            @Override protected void write(final ChannelDataOutput output) throws IOException {
                switch (type) {
                    default: throw new AssertionError(type);
                    case TIFFTag.TIFF_LONG: output.writeInts(values); break;
                    case TIFFTag.TIFF_SHORT: {
                        for (final int value : values) {
                            output.writeShort(value);
                        }
                        break;
                    }
                }
            }
        });
    }

    /**
     * Writes a tag with a single value stored as a 16 or 32 bits unsigned integer.
     * The list of valid tag codes is defined by TIFF specification.
     *
     * <p>The {@code TIFF_LONG} type is preferred when TIFF specification leaves the choice between 16 or 32 bits,
     * because the TIFF structure is such as encoding those numbers on 16 bits does not provide any performance or
     * space benefit. It was maybe a performance advantage when 16 bits processors were common.</p>
     *
     * @param  tag    the code of the tag to write, usually a constant defined by the TIFF specification.
     * @param  type   {@code TIFF_SHORT} or {@code TIFF_LONG}.
     * @param  value  the value to write as an unsigned integer.
     * @throws IOException if an error occurred while writing to the output.
     */
    private void writeTag(final short tag, final short type, final int value) throws IOException {
        writeTagHeader(tag, type, 1);
        final ByteBuffer buffer = output.buffer;
        switch (type) {
            case TIFFTag.TIFF_LONG: {               // TIFF "long" is Java `int` but unsigned.
                buffer.putInt(value);
                break;
            }
            case TIFFTag.TIFF_SHORT: {
                assert value >= 0 && value <= 0xFFFF : value;
                buffer.putShort((short) value);     // Value shall be left-aligned.
                buffer.putShort((short) 0);         // This space is lost.
                break;
            }
            default: throw new AssertionError(type);
        }
        if (isBigTIFF) {
            buffer.putInt(0);                       // Make the slot 64 bits large, left-aligned value.
        }
    }

    /**
     * Executes the given deferred write operation immediately if doing so is cheap,
     * or queue the operation for later execution otherwise.
     *
     * @param  value  the deferred value to write immediately or later.
     * @throws IOException if an error occurred while writing the value.
     */
    private void writeOrQueue(final UpdatableWrite<?> value) throws IOException {
        if (!value.tryUpdateBuffer(output)) {
            deferredWrites.add(value);
        }
    }

    /**
     * Sends to the writable channel any information that are still in buffers.
     * This method does not flush the writable channel itself.
     *
     * @throws IOException if an error occurred while closing this writer.
     */
    @Override
    public void flush() throws IOException {
        UpdatableWrite<?> change;
        while ((change = deferredWrites.pollFirst()) != null) {
            change.update(output);
        }
        output.flush();     // Flush buffer → channel, but not channel → disk.
    }

    /**
     * Closes this writer and the associated writable channel.
     *
     * @throws IOException if an error occurred while closing this writer.
     */
    @Override
    public void close() throws IOException {
        try (output.channel) {
            flush();
        }
    }
}
