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

import java.util.List;
import java.nio.Buffer;
import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;

// Test dependencies
import org.apache.sis.image.privy.AssertionMessages;


/**
 * Information about a single band in a <abbr>GDAL</abbr> raster.
 * Instances of {@code Band} should never escape {@link TiledResource} in order to avoid the risk that
 * {@code GDALRasterBandH} exist after the parent {@link GDALStore} has been garbage collected.
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Band {
    /**
     * Pointer to the <abbr>GDAL</abbr> object in native memory.
     * This is a {@code GDALRasterBandH} in the C/C++ <abbr>API</abbr>.
     */
    private final MemorySegment handle;

    /**
     * Creates a new instance.
     *
     * @param  handle  pointer to the <abbr>GDAL</abbr> band in native memory.
     */
    Band(final MemorySegment handle) {
        this.handle = handle;
    }

    /**
     * Returns whether the given Boolean flag is {@code true}.
     */
    static boolean isTrue(final MemorySegment flag) {
        return flag.get(ValueLayout.JAVA_INT, 0) != 0;
    }

    /**
     * Returns the minimum, maximum or fill value of the band.
     * In the minimum or maximum is not explicitly defined,
     * <abbr>GDAL</abbr> derives the value from the data type.
     *
     * @param  getter  the getter for minimum, maximum of fill value.
     * @param  flag    a pointer to a single {@code int} where to store <abbr>GDAL</abbr> status.
     * @return the requested value.
     */
    final double getValue(final MethodHandle getter, final MemorySegment flag) {
        final double value;
        try {
            value = (double) getter.invokeExact(handle, flag);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return value;
    }

    /**
     * Creates the sample dimension. Must be invoked in a synchronized block.
     * Caller should cache the returned value so that this method is invoked only once.
     *
     * @param  parent  the data set which contains the raster which contains this band.
     * @param  gdal    set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  flag    a pointer to a single {@code int} where to store <abbr>GDAL</abbr> status.
     * @return the sample dimension.
     * @throws DataStoreException if <var>GDAL</var> reported a fatal error.
     */
    final SampleDimension createSampleDimension(final GDALStore parent, final GDAL gdal, final MemorySegment flag)
            throws DataStoreException
    {
        /*
         * The minimum, maximum and "no data" values are in units of the sample data type.
         * They are not converted values.
         */
        final int band;
        final double minimum, maximum, nodata, scale, offset;
        boolean hasRange, hasNoData, convert;
        final MemorySegment names, uom;
        try {
            band    = (int)    gdal.getBandNumber       .invokeExact(handle);
            minimum = (double) gdal.getRasterMinimum    .invokeExact(handle, flag); hasRange  = isTrue(flag);
            maximum = (double) gdal.getRasterMaximum    .invokeExact(handle, flag); hasRange &= isTrue(flag);
            nodata  = (double) gdal.getRasterNoDataValue.invokeExact(handle, flag); hasNoData = isTrue(flag);
            scale   = (double) gdal.getRasterScale      .invokeExact(handle, flag); convert   = isTrue(flag);
            offset  = (double) gdal.getRasterOffset     .invokeExact(handle, flag); convert  &= isTrue(flag);
            uom     = (MemorySegment) gdal.getRasterUnitType.invokeExact(handle);
            names   = (MemorySegment) gdal.getRasterCategoryNames.invokeExact(handle);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        /*
         * If a unit of measurement is given, we consider that the band is quantitative
         * even if the transfer function (scale and offset) was not specified. The GDAL
         * default when there is no scale/offset corresponds to the identity transform.
         */
        final List<String> categories = GDAL.fromNullTerminatedStrings(names);
        final String symbol = GDAL.toString(uom);
        Unit<?> units = null;
        if (symbol != null && !symbol.isBlank()) try {
            convert = true;
            units = Units.valueOf(symbol);
        } catch (MeasurementParseException e) {
            parent.warning("getSampleDimensions", e.getMessage(), e);
        }
        /*
         * Sample dimension is quantitative if a unit of measurement and/or a transfer function is specified.
         * The first non-blank name inside the valid range is taken as the name of the quantitative category.
         * Other names in the same range are discarded, because SIS can associate only one name per range.
         *
         * If the sample dimension is not quantitative, then it is qualitative. All category names are added.
         * The range of values, if present, is added only as a fallback of there is no category names.
         */
        final var builder = new SampleDimension.Builder();
        if (hasNoData) {
            builder.setBackground(nodata);
        }
        if (hasRange | convert) {
            final var range = NumberRange.create(minimum, true, maximum, true);
            if (convert) {
                String name = null;
                if (categories != null && minimum <= maximum) {         // Comparison is for excluding NaN.
                    int index = Math.max((int) Math.ceil (minimum), 0);
                    int limit = Math.min((int) Math.floor(maximum), categories.size() - 1);   // Inclusive.
                    while (index <= limit) {
                        final String item = categories.set(index, "");
                        if (name == null && !item.isBlank()) {
                            name = item;
                        }
                    }
                }
                builder.addQuantitative(name, range, (MathTransform1D) MathTransforms.linear(scale, offset), units);
            } else if (categories == null) {
                builder.addQualitative(null, range);
            }
        }
        /*
         * If the main category was quantitative, then the remaining non-blank names
         * are categories that were outside the declared range.
         */
        if (categories != null) {
            final int n = categories.size();
            for (int i=0; i<n; i++) {
                final String item = categories.get(i);
                if (!item.isEmpty()) {
                    builder.addQualitative(item, i);
                }
            }
        }
        if (band > 0) {
            builder.setName(band);
        }
        return builder.build();
    }

    /**
     * Returns the color interpretation of this band.
     *
     * @param  gdal  set of handles for invoking <abbr>GDAL</abbr> functions.
     * @return color interpretation of this band.
     */
    final ColorInterpretation getColorInterpretation(final GDAL gdal) {
        final int n;
        try {
            n = (int) gdal.getColorInterpretation.invokeExact(handle);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return ColorInterpretation.valueOf(n);
    }

    /**
     * Returns the ARGB codes of the band. Caller may need to combine somehow the ARGB
     * codes of all bands in order to produce a Java2D {@link java.awt.image.ColorModel}.
     *
     * @param  gdal  set of handles for invoking <abbr>GDAL</abbr> functions.
     * @return ARGB codes, or {@code null} if none.
     */
    final int[] getARGB(final GDAL gdal) {
        try (Arena arena = Arena.ofConfined()) {
            final var layout = ValueLayout.JAVA_SHORT;
            final MemorySegment colorEntry = arena.allocate(layout, 4);
            final var colors  = (MemorySegment) gdal.getRasterColorTable.invokeExact(handle);
            if (!GDAL.isNull(colors)) {
                final int count = (int) gdal.getColorEntryCount.invokeExact(colors);
                final int[] ARGB = new int[count];
                for (int i=0; i<count; i++) {
                    final int err = (int) gdal.getColorEntryAsRGB.invokeExact(colors, i, colorEntry);
                    if (!ErrorHandler.checkCPLErr(err)) {
                        return null;
                    }
                    final short c1 = colorEntry.getAtIndex(layout, 0);   // gray, red, cyan or hue
                    final short c2 = colorEntry.getAtIndex(layout, 1);   // green, magenta, or lightness
                    final short c3 = colorEntry.getAtIndex(layout, 2);   // blue, yellow, or saturation
                    final short c4 = colorEntry.getAtIndex(layout, 3);   // alpha or blackband
                    ARGB[i] = (Short.toUnsignedInt(c4) << 24)
                            | (Short.toUnsignedInt(c1) << 16)
                            | (Short.toUnsignedInt(c2) <<  8)
                            | (Short.toUnsignedInt(c3));
                }
                return ARGB;
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return null;
    }

    /**
     * Transfers (reads or writes) sample values between <abbr>GDAL</abbr> raster and Java2D raster.
     * The full area of the Java2D raster is transferred. It may corresponds to a sub-area of the GDAL raster.
     *
     * <h4>Prerequisites</h4>
     * <ul>
     *   <li>The Java2D raster shall use a {@link ComponentSampleModel}.</li>
     *   <li>In read mode, the given raster shall be an instance of {@link WritableRaster}.</li>
     * </ul>
     *
     * <h4>Alternatives</h4>
     * {@code GDALReadBlock} would have been a more efficient method, but we do not use it because the actual
     * tile size given to this method is sometime different than the natural block size of the data set.
     * This difference happens when <abbr>GDAL</abbr> uses block size as width as the image and 1 row in height.
     * Such block sizes are inefficient for Apache <abbr>SIS</abbr>, therefore we request a different size.
     *
     * <p>A yet more efficient approach would be to use {@code GDALRasterBlock::GetLockedBlockRef(…)}
     * for copying the data from the cache without intermediate buffer. But the latter is C++ API.
     * We cannot use it as of Java 22.</p>
     *
     * @param  gdal             set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  readWriteFlags   {@link OpenFlag#READ} or {@link OpenFlag#WRITE}.
     * @param  selectedBands    all bands to read, in the same order as they appear in the given Java2D raster.
     * @param  resourceType     the <abbr>GDAL</abbr> data type of all specified bands, as stored in the resource.
     * @param  resourceBounds   region to read or write in resource coordinates. (0,0) is the upper-left pixel.
     * @param  raster           the Java2D raster where to store of fetch the values to read or write.
     * @param  rasterBounds     region to write or read in raster coordinates.
     * @param  transferBuffer   a temporary buffer used for copying data.
     * @return whether the operation was successful according <abbr>GDAL</abbr>.
     * @throws ClassCastException if an above-documented prerequisite is not true.
     * @throws DataStoreException if <var>GDAL</var> reported a warning or fatal error.
     */
    static boolean transfer(final GDAL          gdal,
                            final int           readWriteFlags,
                            final Band[]        selectedBands,
                            final DataType      resourceType,
                            final Rectangle     resourceBounds,
                            final Raster        raster,
                            final Rectangle     rasterBounds,
                            final MemorySegment transferBuffer)
            throws DataStoreException
    {
        if (readWriteFlags == OpenFlag.READ && !(raster instanceof WritableRaster)) {
            throw new ClassCastException();
        }
        final var   dataBuffer     = raster.getDataBuffer();
        final var   rasterType     = resourceType.forDataBufferType(dataBuffer.getDataType());
        final int   dataSize       = DataBuffer.getDataTypeSize(dataBuffer.getDataType()) / Byte.SIZE;
        final var   sampleModel    = (ComponentSampleModel) raster.getSampleModel();   // See prerequisites in Javadoc.
        final int   pixelStride    = Math.multiplyExact(dataSize, sampleModel.getPixelStride());
        final int   scanlineStride = Math.multiplyExact(dataSize, sampleModel.getScanlineStride());
        final int[] bankIndices    = sampleModel.getBankIndices();
        /*
         * The following assertions are critical: if those conditions are not true, it may crash the JVM.
         * For that reason, we test them unconditionally instead of using the `assert` statement.
         */
        if (!raster.getBounds().contains(rasterBounds)) {
            throw new AssertionError(AssertionMessages.notContained(raster.getBounds(), rasterBounds));
        }
        if (transferBuffer.byteSize() < Math.multiplyFull(rasterBounds.width, rasterBounds.height) * dataSize) {
            throw new AssertionError(rasterBounds);
        }
        for (int b=0; b < selectedBands.length; b++) {
            final int x = rasterBounds.x - raster.getSampleModelTranslateX();
            final int y = rasterBounds.y - raster.getSampleModelTranslateY();
            final int offset = sampleModel.getOffset(x, y, b);      // Where to write in the Java array.
            final Buffer buffer = RasterFactory.wrapAsBuffer(dataBuffer, bankIndices[b]).position(offset);
            final int err;
            try {
                err = (int) gdal.rasterIO.invokeExact(
                        selectedBands[b].handle,
                        readWriteFlags,         // Either GF_Read to read a region of data, or GF_Write to write a region of data.
                        resourceBounds.x,       // First column of the region to be accessed. Zero to start from the left side.
                        resourceBounds.y,       // First row of the region to be accessed. Zero to start from the top.
                        resourceBounds.width,   // The width of the region of the band to be accessed in pixels.
                        resourceBounds.height,  // The height of the region of the band to be accessed in lines.
                        transferBuffer,         // The buffer into which the data is read, or from which it is written.
                        rasterBounds.width,     // The width of the region of the Java2D raster to be accessed.
                        rasterBounds.height,    // The height of the region of the Java2D raster to be accessed.
                        rasterType.ordinal(),   // The type of the pixel values in the destinaton image.
                        pixelStride,            // The byte offset from the start of one pixel to the start of the next pixel.
                        scanlineStride);        // The byte offset from the start of one scanline to the start of the next.
            } catch (Throwable e) {
                throw GDAL.propagate(e);
            }
            if (!ErrorHandler.checkCPLErr(err)) {
                return false;
            }
            MemorySegment.ofBuffer(buffer).copyFrom(transferBuffer);
        }
        return true;
    }

    /**
     * Advise driver of upcoming read requests. Contrarily to the above {@code read(…)} method which receives
     * a rectangle for one tile at a time, the rectangle received by this method is for all tiles to be read.
     *
     * @param  gdal             set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  resourceBounds   region to read in resource coordinates. (0,0) is the upper-left pixel.
     * @param  imageBounds      region to write in image coordinates (may cover more than one tile).
     * @param  imageType        the <abbr>GDAL</abbr> data type of the destination raster.
     * @return whether the operation was successful according <abbr>GDAL</abbr>.
     * @throws DataStoreException if <var>GDAL</var> reported a warning or fatal error.
     */
    final boolean adviseRead(final GDAL      gdal,
                             final Rectangle resourceBounds,
                             final Rectangle imageBounds,       // Not the same as `rasterBounds`.
                             final DataType  imageType)
            throws DataStoreException
    {
        final int err;
        try {
            err = (int) gdal.adviseRead.invokeExact(
                    handle,
                    resourceBounds.x,       // First column of the region to be accessed. Zero to start from the left side.
                    resourceBounds.y,       // First row of the region to be accessed. Zero to start from the top.
                    resourceBounds.width,   // The width of the region of the band to be accessed in pixels.
                    resourceBounds.height,  // The height of the region of the band to be accessed in lines.
                    imageBounds.width,      // The width of the destination image.
                    imageBounds.height,     // The height of the destination image.
                    imageType.ordinal(),    // The type of the pixel values in the destinaton image.
                    MemorySegment.NULL);    // A list of name=value strings with special control options.
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return ErrorHandler.checkCPLErr(err);
    }
}
