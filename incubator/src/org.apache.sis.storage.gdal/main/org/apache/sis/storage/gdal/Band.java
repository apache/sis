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
import org.apache.sis.coverage.privy.RasterFactory;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;


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
        final List<String> categories = GDAL.toStringArray(names);
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
            final MemorySegment colorEntry = arena.allocate(ValueLayout.JAVA_SHORT, 4);
            final var colors  = (MemorySegment) gdal.getRasterColorTable.invokeExact(handle);
            if (!GDAL.isNull(colors)) {
                final int count = (int) gdal.getColorEntryCount.invokeExact(colors);
                final int[] ARGB = new int[count];
                for (int i=0; i<count; i++) {
                    final int err = (int) gdal.getColorEntryAsRGB.invokeExact(colors, i, colorEntry);
                    final short c1 = colorEntry.get(ValueLayout.JAVA_SHORT, Short.BYTES * 0);   // gray, red, cyan or hue
                    final short c2 = colorEntry.get(ValueLayout.JAVA_SHORT, Short.BYTES * 1);   // green, magenta, or lightness
                    final short c3 = colorEntry.get(ValueLayout.JAVA_SHORT, Short.BYTES * 2);   // blue, yellow, or saturation
                    final short c4 = colorEntry.get(ValueLayout.JAVA_SHORT, Short.BYTES * 3);   // alpha or blackband
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
     * Transfers (reads or writes) sample values between <abbr>GDAL</abbr> raster and Java2D raster for one band.
     * The full area of the Java2D raster is transferred. It may corresponds to a sub-area of the GDAL raster.
     *
     * <h4>Prerequisites</h4>
     * <ul>
     *   <li>The Java2D raster shall use a {@link ComponentSampleModel}.</li>
     *   <li>In read mode, the given raster shall be an instance of {@link WritableRaster}.</li>
     * </ul>
     *
     * @param  gdal    set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  rwFlag  {@link OpenFlag#READ} or {@link OpenFlag#WRITE}.
     * @param  image   the <abbr>GDAL</abbr> raster which contains the band to read or write.
     * @param  aoi     region of the image to read or write. (0,0) is the upper-left pixel.
     * @param  raster  the Java2D raster where to store of fetch the values to read or write.
     * @param  band    band of sample values in the Java2D raster.
     * @return whether the operation was successful according <abbr>GDAL</abbr>.
     * @throws ClassCastException if an above-documented prerequisite is not true.
     * @throws DataStoreException if <var>GDAL</var> reported a warning or fatal error.
     */
    final boolean transfer(final GDAL gdal, final int rwFlag,
                           final TiledResource image, final Rectangle aoi,     // GDAL model
                           final Raster raster, final int band)                // Java2D model
            throws DataStoreException
    {
        if (rwFlag == OpenFlag.READ && !(raster instanceof WritableRaster)) {
            throw new ClassCastException();
        }
        final var model    = (ComponentSampleModel) raster.getSampleModel();   // See prerequisites in Javadoc.
        final var data     = raster.getDataBuffer();
        final int dataSize = DataBuffer.getDataTypeSize(data.getDataType()) / Byte.SIZE;
        final var buffer   = RasterFactory.wrapAsBuffer(data, model.getBankIndices()[band]);
        buffer.position(model.getOffset(raster.getMinX() - raster.getSampleModelTranslateX(),
                                        raster.getMinY() - raster.getSampleModelTranslateY(), band));
        final int err;
        try (Arena arena = Arena.ofConfined()) {
            /*
             * TODO: we wanted to use `MemorySegment.ofBuffer` but it does not work.
             * We get an "IllegalArgumentException: Heap segment not allowed" error.
             * For now we copy in a temporary array as a workaround, but it needs to
             * be replaced by a call to GetLockedBlockRef.
             */
            MemorySegment tmp = arena.allocate(Math.multiplyFull(buffer.remaining(), dataSize));
            err = (int) gdal.rasterIO.invokeExact(handle, rwFlag,
                    aoi.x, aoi.y, aoi.width, aoi.height,
                    tmp,
                    raster.getWidth(),
                    raster.getHeight(),
                    image.dataType.forDataBufferType(data.getDataType()).ordinal(),
                    Math.multiplyExact(dataSize, model.getPixelStride()),
                    Math.multiplyExact(dataSize, model.getScanlineStride()));

            MemorySegment.ofBuffer(buffer).copyFrom(tmp);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return ErrorHandler.checkCPLErr(err);
    }
}
