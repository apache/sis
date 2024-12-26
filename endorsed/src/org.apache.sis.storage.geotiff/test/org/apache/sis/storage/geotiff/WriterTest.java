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

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.lang.reflect.Array;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import javax.imageio.plugins.tiff.TIFFTag;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.*;
import org.opengis.referencing.crs.ProjectedCRS;
import org.apache.sis.io.stream.ByteArrayChannel;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geotiff.base.Tags;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.coverage.privy.ColorModelBuilder;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.image.DataType;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.image.TiledImageMock;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;


/**
 * Tests {@link Writer}.
 *
 * @author  Erwan Roussel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WriterTest extends TestCase {
    /**
     * Arbitrary size (in pixels) of tiles in the image to test. The TIFF specification restricts those sizes
     * to multiples of 16, but the Apache SIS implementation has no such restriction. Enforcing a size of 16
     * is necessary, but this is not what we want to test in this class. For making this test easier to debug,
     * we want small sizes (less than 10) with different values for width and height.
     */
    private static final int TILE_WIDTH = 7, TILE_HEIGHT = 5;

    /**
     * The image to write.
     */
    private TiledImageMock image;

    /**
     * Mapping from pixel coordinates to "real world" coordinates, or {@code null} if none.
     */
    private GridGeometry gridGeometry;

    /**
     * The channel where the image is written.
     * The data can be obtained by a call to {@link ByteArrayChannel#toBuffer()}.
     */
    private ByteArrayChannel output;

    /**
     * The store to use for writing GeoTIFF files. This store should be closed at the end of each test,
     * but it is not a problem if they are not because the tests do not hold any resources (no files).
     */
    private GeoTiffStore store;

    /**
     * The data produced by the GeoTIFF writer. This is the sequence of bytes to verify.
     */
    private ByteBuffer data;

    /**
     * Index in {@link #data} where each tile begins.
     */
    private int[] tileOffsets;

    /**
     * Creates a new test case.
     */
    public WriterTest() {
    }

    /**
     * Initializes the test with a tiled image and a GeoTIFF writer.
     * The image is created with some random properties (pixel and tile coordinates) but verifiable pixel values.
     * The writer uses a buffer of random size.
     *
     * @param  dataType  sample data type as one of the {@link java.awt.image.DataBuffer} constants.
     * @param  order     whether to use little endian or big endian byte order.
     * @param  banded    whether to use {@link BandedSampleModel} instead of {@link PixelInterleavedSampleModel}.
     * @param  numBands  number of bands in the sample model to create.
     * @param  numTileX  number of tiles in the X direction.
     * @param  numTileY  number of tiles in the Y direction.
     * @param  options   whether to write classic TIFF or BigTIFF.
     * @throws IOException should never happen since the tests are writing in memory.
     * @throws DataStoreException should never happen since we control the output class.
     */
    private void initialize(final DataType type, final ByteOrder order, final boolean banded, final int numBands,
                            final int numTileX, final int numTileY, final FormatModifier... options)
            throws IOException, DataStoreException
    {
        final var random = TestUtilities.createRandomNumberGenerator();
        image = new TiledImageMock(type.toDataBufferType(), numBands,
                random.nextInt(16) - 8,         // minX
                random.nextInt(16) - 8,         // minY
                TILE_WIDTH  * numTileX,
                TILE_HEIGHT * numTileY,
                TILE_WIDTH,
                TILE_HEIGHT,
                random.nextInt(16) - 8,         // minTileX
                random.nextInt(16) - 8,         // minTileY
                banded);

        image.validate();
        image.initializeAllTiles();
        output = new ByteArrayChannel(new byte[image.getWidth() * image.getHeight() * numBands * type.bytes() + 800], false);
        var d = new ChannelDataOutput("TIFF", output, ByteBuffer.allocate(random.nextInt(128) + 20).order(order));
        var c = new StorageConnector(d);
        c.setOption(FormatModifier.OPTION_KEY, options);
        c.setOption(Compression.OPTION_KEY, Compression.NONE);
        store = new GeoTiffStore(null, c);
        data  = output.toBuffer().order(order);
    }

    /**
     * Creates a grid geometry to associate with the image. This is used for testing GeoTIFF tags.
     */
    private void createGridGeometry() {
        final ProjectedCRS crs = HardCodedConversions.mercator(HardCodedCRS.WGS84);
        final var env = new Envelope2D(crs, -23, -10, TILE_WIDTH * 2, TILE_HEIGHT * 4);
        gridGeometry = new GridGeometry(new GridExtent(TILE_WIDTH, TILE_HEIGHT), env, GridOrientation.REFLECTION_Y);
    }

    /**
     * Writes a single image and updates the data buffer limit.
     * After this method call, the {@linkplain #data} position is 0
     * and its limit is the number of bytes written by the TIFF encoder.
     *
     * @throws IOException should never happen since the tests are writing in memory.
     * @throws DataStoreException if the image is incompatible with writer capability.
     */
    private void writeImage() throws IOException, DataStoreException {
        store.append(image, gridGeometry, null);
        data.clear().limit(Math.toIntExact(output.size()));
    }

    /**
     * Tests the writing a gray scale image made of a single tile with pixels on 8 bits.
     * This is the simplest type of image.
     *
     * @throws IOException should never happen since the tests are writing in memory.
     * @throws DataStoreException if the image is incompatible with writer capability.
     */
    @Test
    public void testUntiledGrayScale() throws IOException, DataStoreException {
        initialize(DataType.BYTE, ByteOrder.BIG_ENDIAN, false, 1, 1, 1,
                   FormatModifier.ANY_TILE_SIZE);
        writeImage();
        verifyHeader(false, IOBase.BIG_ENDIAN);
        verifyImageFileDirectory(Writer.COMMON_NUMBER_OF_TAGS - 1,              // One less tag because stripped layout.
                                 PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO,
                                 new short[] {Byte.SIZE}, false);
        verifySampleValues(1);
        store.close();
    }

    /**
     * Same as {@link #testUntiledGrayScale()} but using BigTIFF format.
     *
     * @throws IOException should never happen since the tests are writing in memory.
     * @throws DataStoreException if the image is incompatible with writer capability.
     */
    @Test
    public void testUntiledBigTIFF() throws IOException, DataStoreException {
        initialize(DataType.BYTE, ByteOrder.LITTLE_ENDIAN, false, 1, 1, 1,
                   FormatModifier.ANY_TILE_SIZE, FormatModifier.BIG_TIFF);
        writeImage();
        verifyHeader(true, IOBase.LITTLE_ENDIAN);
        verifyImageFileDirectory(Writer.COMMON_NUMBER_OF_TAGS - 1,          // One less tag because stripped layout.
                                 PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO,
                                 new short[] {Byte.SIZE}, false);
        verifySampleValues(1);
        store.close();
    }

    /**
     * Tests the writing a gray scale image made of a multiple tiles with pixels on 8 bits.
     * We arbitrarily write 3 tiles in the X direction and 4 tiles in the Y direction.
     *
     * @throws IOException should never happen since the tests are writing in memory.
     * @throws DataStoreException if the image is incompatible with writer capability.
     */
    @Test
    public void testTiledGrayScale() throws IOException, DataStoreException {
        initialize(DataType.BYTE, ByteOrder.LITTLE_ENDIAN, false, 1, 3, 4, FormatModifier.ANY_TILE_SIZE);
        writeImage();
        verifyHeader(false, IOBase.LITTLE_ENDIAN);
        verifyImageFileDirectory(Writer.COMMON_NUMBER_OF_TAGS,
                                 PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO,
                                 new short[] {Byte.SIZE}, true);
        verifySampleValues(1);
        store.close();
    }

    /**
     * Tests the writing an RGB image made of a single tile with pixels on (8,8,8) bits.
     *
     * @throws IOException should never happen since the tests are writing in memory.
     * @throws DataStoreException if the image is incompatible with writer capability.
     */
    @Test
    public void testUntiledRGB() throws IOException, DataStoreException {
        initialize(DataType.BYTE, ByteOrder.LITTLE_ENDIAN, false, 3, 1, 1, FormatModifier.ANY_TILE_SIZE);
        image.setColorModel(new ColorModelBuilder().createRGB(image.getSampleModel()));
        writeImage();
        verifyHeader(false, IOBase.LITTLE_ENDIAN);
        verifyImageFileDirectory(Writer.COMMON_NUMBER_OF_TAGS - 1,          // One less tag because stripped layout.
                                 PHOTOMETRIC_INTERPRETATION_RGB,
                                 new short[] {Byte.SIZE, Byte.SIZE, Byte.SIZE}, false);
        verifySampleValues(3);
        store.close();
    }

    /**
     * Tests writing an image with GeoTIFF data.
     *
     * @throws IOException should never happen since the tests are writing in memory.
     * @throws DataStoreException if the image is incompatible with writer capability.
     */
    @Test
    public void testGeoTIFF() throws IOException, DataStoreException {
        initialize(DataType.BYTE, ByteOrder.LITTLE_ENDIAN, false, 1, 1, 1, FormatModifier.ANY_TILE_SIZE);
        createGridGeometry();
        writeImage();
        verifyHeader(false, IOBase.LITTLE_ENDIAN);
        /*
         * The number of tags depends on whether an EPSG database is present or not.
         * So the test cannot expects an exact number of tags.
         */
        int tagCount = data.getShort(data.position());
        assertTrue(tagCount >= Writer.COMMON_NUMBER_OF_TAGS + 3 - 1);           // 3 more for RGB, 1 less for strips.
        verifyImageFileDirectory(tagCount, PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO, new short[] {Byte.SIZE}, false);
        verifySampleValues(1);
        store.close();
    }

    /**
     * Verifies the TIFF header, before the first Image File Directory (IFD).
     *
     * @param isBigTIFF   whether the file is BigTIFF.
     * @param endianness  {@link Writer#BIG_ENDIAN} or {@link Writer#LITTLE_ENDIAN}.
     */
    private void verifyHeader(final boolean isBigTIFF, final short endianness) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ByteBuffer data = this.data;
        if (isBigTIFF) {
            assertEquals(Set.of(FormatModifier.BIG_TIFF),  store.getModifiers());
            assertEquals(endianness,                       data.getShort());
            assertEquals(Writer.BIG_TIFF,                  data.getShort());
            assertEquals(Long.BYTES,                       data.getShort());     // Byte size of offsets.
            assertEquals(0,                                data.getShort());     // Constant.
            assertEquals(data.position() + Long.BYTES,     data.getLong());      // Offset of the first IFD.
        } else {
            assertEquals(Set.of(),                         store.getModifiers());
            assertEquals(endianness,                       data.getShort());
            assertEquals(Writer.CLASSIC,                   data.getShort());
            assertEquals(data.position() + Integer.BYTES,  data.getInt());       // Offset of the first IFD.
        }
    }

    /**
     * Verifies the Image File Directory starting at the given position in the given buffer.
     *
     * <h4>Limitation</h4>
     * For making this method simpler, all TIFF data should be encoded with {@link ByteOrder#LITTLE_ENDIAN}.
     * This is a limitation of this verification method, not a limitation of {@link Writer} implementation.
     * The reason is that little endian makes possible to invoke, for example, {@link ByteBuffer#getLong()}
     * even if the data is actually a left-aligned {@code int} value followed by 4 bytes of padding zeros.
     * The same argument applies to {@link ByteBuffer#getShort()} versus {@code short} followed by padding.
     * This property works only for little endian.
     *
     * <p>Above restriction can be relaxed if the TIFF file is classic and the caller known that all values
     * verified by this method are {@code int} types, no {@code short} types.</p>
     *
     * @param tagCount        expected number of tags.
     * @param interpretation  one of {@code PHOTOMETRIC_INTERPRETATION_} constants.
     * @param bitsPerSample   expected number of bits per sample. The array length is the number of bands.
     * @param isTiled         whether the image uses tiles instead of strips.
     */
    private void verifyImageFileDirectory(int tagCount, final int interpretation, final short[] bitsPerSample, final boolean isTiled) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ByteBuffer data     = this.data;
        final boolean isBigTIFF   = store.getModifiers().contains(FormatModifier.BIG_TIFF);
        final boolean isBigEndian = data.order() == ByteOrder.BIG_ENDIAN;
        assertEquals(tagCount, isBigTIFF ? data.getLong() : data.getShort());
        /*
         * Build a list of tags considered mandatory for all images in this test.
         * Tags not in this set are considered optional.
         */
        final var expectedTags = new HashSet<Integer>(Arrays.asList(
                TAG_NEW_SUBFILE_TYPE, TAG_IMAGE_WIDTH, TAG_IMAGE_LENGTH,
                TAG_COMPRESSION, TAG_PHOTOMETRIC_INTERPRETATION, TAG_SAMPLES_PER_PIXEL,
                TAG_X_RESOLUTION, TAG_Y_RESOLUTION, TAG_RESOLUTION_UNIT));
        if (isTiled) {
            expectedTags.addAll(Arrays.asList(TAG_TILE_WIDTH, TAG_TILE_LENGTH, TAG_TILE_OFFSETS, TAG_TILE_BYTE_COUNTS));
        } else {
            expectedTags.addAll(Arrays.asList(TAG_STRIP_OFFSETS, TAG_STRIP_BYTE_COUNTS));
        }
        /*
         * Iterate over all tags. Verify that they are in increasing order,
         * and verify the value of some of them (not necessarily all of them).
         */
        short previousTag = 0;
        while (--tagCount >= 0) {
            short  tag   = data.getShort();
            short  type  = data.getShort();
            long   count = isBigTIFF ? data.getLong() : data.getInt();
            long   value = isBigTIFF ? data.getLong() : data.getInt();
            Object expected;       // The Number class will define the expected type.
            assertTrue(Short.toUnsignedInt(tag) > Short.toUnsignedInt(previousTag),
                       "Tags shall be sorted in increasing order.");
            expectedTags.remove(Integer.valueOf(tag));
            previousTag = tag;
            switch (tag) {
                case TAG_NEW_SUBFILE_TYPE:           expected = 0;                            break;
                case TAG_IMAGE_WIDTH:                expected = image.getWidth();             break;
                case TAG_IMAGE_LENGTH:               expected = image.getHeight();            break;
                case TAG_BITS_PER_SAMPLE:            expected = compact(bitsPerSample);       break;
                case TAG_COMPRESSION:                expected = (short) COMPRESSION_NONE;     break;
                case TAG_PHOTOMETRIC_INTERPRETATION: expected = (short) interpretation;       break;
                case TAG_SAMPLES_PER_PIXEL:          expected = (short) bitsPerSample.length; break;
                case TAG_RESOLUTION_UNIT:            expected = (short) RESOLUTION_UNIT_NONE; break;
                case TAG_TILE_WIDTH:                 expected = TILE_WIDTH;                   break;
                case TAG_TILE_LENGTH:
                case TAG_ROWS_PER_STRIP:             expected = TILE_HEIGHT;                  break;
                case TAG_STRIP_BYTE_COUNTS:
                case TAG_TILE_BYTE_COUNTS:           expected = expectedTileByteCounts();     break;
                case TAG_STRIP_OFFSETS:
                case TAG_TILE_OFFSETS: {
                    assertNull(tileOffsets);
                    tileOffsets = getIntegers(value, count, isBigTIFF ? Writer.TIFF_ULONG : TIFFTag.TIFF_LONG);
                    continue;
                }
                default: continue;
            }
            boolean isShort = (expected instanceof Short);
            if (isShort & isBigEndian) {
                value >>>= Short.SIZE;      // Because 16-bits values in tag entries are left-aligned on 32 bits.
            }
            isShort |= (expected instanceof short[]);
            /*
             * Compare the actual value with the expected value. The expected value may be an instance
             * of `Short`, `Integer`, `short[]` or `int[]`. The class determines the expected TIFF type.
             * We do not support all TIFF types, but only the ones that we want to verify.
             */
            Supplier<String> message = () -> Tags.name(tag);
            assertEquals(isShort ? TIFFTag.TIFF_SHORT : TIFFTag.TIFF_LONG, type, message);
            if (expected.getClass().isArray()) {
                assertEquals(Array.getLength(expected), count, message);
                final int[] actual = getIntegers(value, count, type);
                assertEquals(count, actual.length, message);
                for (int i=0; i<actual.length; i++) {
                    assertEquals(Array.getInt(expected, i), actual[i], message);
                }
            } else {
                assertEquals(1, count, message);
                assertEquals(((Number) expected).longValue(), value, message);
            }
        }
        /*
         * Verify that all mandatory tags were found.
         */
        assertNotNull(tileOffsets);
        assertTrue(expectedTags.isEmpty(), () -> "Missing mandatory TIFF tags: " +
                expectedTags.stream().map((tag) -> Tags.name(tag.shortValue())).collect(Collectors.joining(", ")));
    }

    /**
     * Returns the given array as a {@link Short} if it contains exactly one element.
     * This is necessary for allowing the {@code isShort & isBigEndian} test to work.
     */
    private static Object compact(final short[] array) {
        return (array.length == 1) ? array[0] : array;
    }

    /**
     * Returns the values stored in the tag, which may potentially be an array.
     *
     * @param  value  value stored in the tag. offsets where to read integers.
     * @param  count  number of integers to read.
     * @param  type   {@code TIFF_ULONG}, {@code TIFF_LONG} or {@code TIFF_SHORT}.
     * @return the integers at the given offset in an array of the specified length.
     */
    private int[] getIntegers(final long value, final long count, final int type) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ByteBuffer data = this.data;
        final int[] array = new int[Math.toIntExact(count)];
        if (array.length == 1) {                        // Ignoring the case of TIFF_SHORT with count of 2.
            array[0] = Math.toIntExact(value);          // Value was small enough for fitting in the TIFF tag.
        } else {
            data.mark();
            data.position(Math.toIntExact(value));
            for (int i=0; i<array.length; i++) {        // Values are stored in a separated array.
                switch (type) {
                    case  Writer.TIFF_ULONG: array[i] = Math.toIntExact(data.getLong()); break;
                    case TIFFTag.TIFF_LONG:  array[i] = data.getInt(); break;
                    case TIFFTag.TIFF_SHORT: array[i] = data.getShort(); break;
                    default: throw new AssertionError(type);
                }
            }
            data.reset();
        }
        return array;
    }

    /**
     * {@return the uncompressed size in bytes of each tile}.
     */
    private int[] expectedTileByteCounts() {
        final SampleModel sm = image.getSampleModel();
        final int[] sizes = new int[image.getNumXTiles() * image.getNumYTiles()];
        Arrays.fill(sizes, TILE_WIDTH * TILE_HEIGHT * sm.getNumBands() * DataBuffer.getDataTypeSize(sm.getDataType()) / Byte.SIZE);
        return sizes;
    }

    /**
     * Verifies the sample values. Expected values are of the form "BTYX" where B is the band (starting with 1),
     * T is the tile index (starting with 1), and X and Y are pixel coordinates starting with 0.
     *
     * @param  numBands     number of bands in each tile.
     */
    private void verifySampleValues(final int numBands) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ByteBuffer data = this.data;
        for (int i=0; i<tileOffsets.length; i++) {
            data.position(tileOffsets[i]);
            for (int y=0; y<TILE_HEIGHT; y++) {
                for (int x=0; x<TILE_WIDTH; x++) {
                    for (int b=0; b<numBands; b++) {
                        int expected = 1000*(b+1) + 100*(i+1) + 10*y + x;
                        int actual;
                        expected &= 0xFF;
                        actual = Byte.toUnsignedInt(data.get());
                        assertEquals(expected, actual);
                    }
                }
            }
        }
    }

    /**
     * Saves in a file the TIFF images created by the last test executed.
     * The file is created in the local directory.
     * This method can be used for checking the TIFF file externally.
     *
     * @throws IOException if an error occurred while writing the file.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void save() throws IOException {
        final Path path = Path.of("WriterTest.tiff");
        System.out.println("Saving test TIFF image to " + path.toAbsolutePath());
        try (OutputStream s = Files.newOutputStream(path)) {
            s.write(data.array(), 0, data.limit());
        }
    }
}
