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
package org.apache.sis.internal.sql.postgis;

import java.util.List;
import java.util.Arrays;
import java.nio.ByteOrder;
import java.io.IOException;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import java.awt.image.SampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.sql.feature.Resources;
import org.apache.sis.internal.sql.feature.InfoStatements;
import org.apache.sis.internal.storage.io.ChannelDataOutput;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.util.resources.Errors;


/**
 * A writer of rasters encoded in <cite>Well Known Binary</cite> (WKB) format.
 * This format is specific to PostGIS 2 (this is not yet an OGC standard at the
 * time of writing this class), but it can nevertheless be used elsewhere.
 *
 * <h2>Multi-threading</h2>
 * This class is <strong>not</strong> safe for multi-threading.
 * Furthermore if a non-null {@link InfoStatements} has been specified to the constructor,
 * then this object is valid only as long as the caller holds a connection to the database.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final class RasterWriter extends RasterFormat {
    /**
     * The byte order to use for encoding the WKB.
     */
    private ByteOrder byteOrder;

    /**
     * Conversion from pixel coordinates to CRS coordinates.
     * This is defined by 6 affine transform coefficients similar to the <cite>World File</cite> format.
     */
    private AffineTransform gridToCRS;

    /**
     * The spatial reference identifier, or 0 if undefined.
     * Note that this is a primary key in the {@code "spatial_ref_sys"} table, not necessarily an EPSG code.
     */
    private int srid;

    /**
     * The "no data" value for each band, or {@code null} if none.
     * If the array length is greater than the number of bands, the last elements are ignored.
     * If the array length is smaller than the number of bands, all remaining bands are assumed
     * to have no "no data" value.
     */
    private Number[] noDataValues;

    /**
     * Creates a new writer. If the {@code spatialRefSys} argument is non-null,
     * then this object is valid only as long as the caller holds a connection to the database.
     *
     * @param  spatialRefSys  the object to use for mapping CRS to the {@code "spatial_ref_sys"} table,
     *                        or {@code null} for using the EPSG codes.
     */
    public RasterWriter(final InfoStatements spatialRefSys) {
        super(spatialRefSys);
        byteOrder = ByteOrder.nativeOrder();
    }

    /**
     * Restores this writer to its initial state.
     * It resets the byte order, grid to CRS transform and SRID to their default values.
     */
    public void reset() {
        byteOrder = ByteOrder.nativeOrder();
        gridToCRS = null;
        srid      = 0;
    }

    /**
     * Sets the conversion from grid coordinates to CRS coordinates together with target CRS.
     * The conversion must be affine. Other grid properties (e.g. envelope and extent) are ignored.
     *
     * @param  gg  the grid to CRS conversion together with target CRS.
     * @throws IllegalArgumentException if the "grid to CRS" transform is not affine.
     * @throws Exception if an error occurred during the search for SRID code.
     *         May be SQL error, WKT parsing error, factory error, <i>etc.</i>
     */
    public void setGridToCRS(final GridGeometry gg) throws Exception {
        if (gg.isDefined(GridGeometry.CRS)) {
            final CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem();
            /*
             * If there is connection to `spatialRefSys` table, use EPSG code.
             * Otherwise use EPSG code only as a starting point, ignoring axis order,
             * and search for the corresponding SRID.
             */
            if (spatialRefSys != null) {
                srid = spatialRefSys.findSRID(crs);
            } else {
                final Integer epsg = IdentifiedObjects.lookupEPSG(crs);
                if (epsg == null) {
                    throw new IllegalArgumentException(Resources.format(
                            Resources.Keys.CanNotFindSRID_1, IdentifiedObjects.getDisplayName(crs, null)));
                }
                srid = epsg;
            }
        } else {
            srid = 0;
        }
        gridToCRS = AffineTransforms2D.castOrCopy(gg.isDefined(GridGeometry.GRID_TO_CRS) ? gg.getGridToCRS(ANCHOR) : null);
    }

    /**
     * Infers the "no data" values from the given sample dimensions.
     * This method uses the {@linkplain SampleDimension#getBackground() background value} if available.
     *
     * @param  bands  the sample dimensions from which to infer the "no data" values.
     */
    public void setNodataValues(final List<? extends SampleDimension> bands) {
        noDataValues = new Number[bands.size()];
        for (int b=0; b < noDataValues.length; b++) {
            final SampleDimension sd = bands.get(b);
            noDataValues[b] = sd.getBackground().orElse(null);
        }
    }

    /**
     * Encodes the given grid coverage to the specified output.
     * This method {@linkplain #setGridToCRS sets the "grid to CRS" conversion}
     * and the {@linkplain #setNodataValues no data values},
     * then delegates to {@link #write(RenderedImage, ChannelDataOutput)}.
     *
     * @param  coverage  the grid coverage to encode.
     * @param  output    where to write the bytes.
     * @throws RasterFormatException if the raster to write is not supported.
     * @throws IOException in an error occurred while writing to the given output.
     * @throws Exception if an error occurred during the search for SRID code.
     *         May be SQL error, WKT parsing error, factory error, <i>etc.</i>
     */
    public void write(final GridCoverage coverage, final ChannelDataOutput output) throws Exception {
        setGridToCRS(coverage.getGridGeometry());
        setNodataValues(coverage.getSampleDimensions());
        write(coverage.render(null), output);
    }

    /**
     * Encodes the given image to the specified output.
     *
     * @param  image   the image to encode.
     * @param  output  where to write the bytes.
     * @throws RasterFormatException if the raster to write is not supported.
     * @throws IOException in an error occurred while writing to the given output.
     */
    public void write(final RenderedImage image, final ChannelDataOutput output) throws IOException {
        final Raster raster;
        if (image.getNumXTiles() == 1 && image.getNumYTiles() == 1) {
            raster = image.getTile(image.getMinTileX(), image.getMinTileY());
        } else {
            raster = image.getData();
        }
        write(raster, output);
    }

    /**
     * Encodes the given raster to the specified output.
     *
     * @param  raster  the raster to encode.
     * @param  output  where to write the bytes.
     * @throws RasterFormatException if the raster to write is not supported.
     * @throws IOException in an error occurred while writing to the given output.
     */
    public void write(final Raster raster, final ChannelDataOutput output) throws IOException {
        if (gridToCRS == null) {
            gridToCRS = new AffineTransform();
        }
        final SampleModel sm = raster.getSampleModel();
        final int numBands = sm.getNumBands();
        final int width    = raster.getWidth();
        final int height   = raster.getHeight();
        /*
         * The `direct` flag tells whether we can write the backing array directly or whether we need
         * to use the pixel iterator because of sample model complexity. We can write arrays directly
         * if each array contains a single band and the scanline stride is equal to the raster width.
         */
        boolean direct = false;                 // Default value for sample models of unknown type.
        int dataType   = sm.getDataType();
        int pixelType  = Band.bufferToPixelType(dataType);
        if (sm instanceof SinglePixelPackedSampleModel) {
            if (numBands == 1) {
                direct = ((SinglePixelPackedSampleModel) sm).getScanlineStride() == width;
            } else {
                final int sampleSize = Arrays.stream(sm.getSampleSize()).max().orElse(0);
                if (sampleSize >= 1 && sampleSize <= Short.SIZE) {
                    dataType  = (sampleSize <= Byte.SIZE ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT);
                    pixelType = Band.bufferToPixelType(dataType);
                }
            }
        } else if (sm instanceof MultiPixelPackedSampleModel) {
            if (dataType == DataBuffer.TYPE_BYTE) {
                final MultiPixelPackedSampleModel mp = (MultiPixelPackedSampleModel) sm;
                final int sampleSize = mp.getPixelBitStride();
                direct = (mp.getScanlineStride() * Byte.SIZE == width * sampleSize);
                pixelType = Band.sizeToPixelType(sampleSize);
            }
        } else if (sm instanceof ComponentSampleModel) {
            direct = (((ComponentSampleModel) sm).getScanlineStride() == width);
        }
        /*
         * Write the header followed by all bands.
         */
        output.buffer.order(byteOrder);
        output.writeByte(ByteOrder.LITTLE_ENDIAN.equals(byteOrder) ? 1 : 0);
        output.writeShort(0);                  // WKB version number.
        output.writeShort(ensureUnsignedShort("numBands", numBands));
        output.writeDouble(gridToCRS.getScaleX());
        output.writeDouble(gridToCRS.getScaleY());
        output.writeDouble(gridToCRS.getTranslateX());
        output.writeDouble(gridToCRS.getTranslateY());
        output.writeDouble(gridToCRS.getShearX());
        output.writeDouble(gridToCRS.getShearY());
        output.writeInt(srid);
        output.writeShort(ensureUnsignedShort("width",  width));
        output.writeShort(ensureUnsignedShort("height", height));
        for (int b=0; b<numBands; b++) {
            final Number fill = (noDataValues != null && b < noDataValues.length) ? noDataValues[b] : null;
            final Band band = new Band(pixelType, fill);
            output.writeByte(band.header);
            switch (dataType) {
                case DataBuffer.TYPE_USHORT: // Fall through
                case DataBuffer.TYPE_SHORT:  output.writeShort (fill != null ? fill.intValue()    :          0); break;
                case DataBuffer.TYPE_BYTE:   output.writeByte  (fill != null ? fill.intValue()    :          0); break;
                case DataBuffer.TYPE_INT:    output.writeInt   (fill != null ? fill.intValue()    :          0); break;
                case DataBuffer.TYPE_FLOAT:  output.writeFloat (fill != null ? fill.floatValue()  :  Float.NaN); break;
                case DataBuffer.TYPE_DOUBLE: output.writeDouble(fill != null ? fill.doubleValue() : Double.NaN); break;
                default: throw new RasterFormatException(Errors.format(Errors.Keys.UnsupportedType_1, dataType));
            }
            if (direct) {
                final DataBuffer buffer = raster.getDataBuffer();
                final int offset = buffer.getOffsets()[b];
                final int length = width * height;
                switch (dataType) {
                    case DataBuffer.TYPE_BYTE:   output.write       (((DataBufferByte)   buffer).getData(b), offset, length); break;
                    case DataBuffer.TYPE_USHORT: output.writeShorts (((DataBufferUShort) buffer).getData(b), offset, length); break;
                    case DataBuffer.TYPE_SHORT:  output.writeShorts (((DataBufferShort)  buffer).getData(b), offset, length); break;
                    case DataBuffer.TYPE_INT:    output.writeInts   (((DataBufferInt)    buffer).getData(b), offset, length); break;
                    case DataBuffer.TYPE_FLOAT:  output.writeFloats (((DataBufferFloat)  buffer).getData(b), offset, length); break;
                    case DataBuffer.TYPE_DOUBLE: output.writeDoubles(((DataBufferDouble) buffer).getData(b), offset, length); break;
                }
            } else {
                final PixelIterator it = new PixelIterator.Builder().create(raster);
                while (it.next()) {
                    switch (dataType) {
                        case DataBuffer.TYPE_USHORT: // Fall through
                        case DataBuffer.TYPE_SHORT:  output.writeShort (it.getSample(b)); break;
                        case DataBuffer.TYPE_BYTE:   output.writeByte  (it.getSample(b)); break;
                        case DataBuffer.TYPE_INT:    output.writeInt   (it.getSample(b)); break;
                        case DataBuffer.TYPE_FLOAT:  output.writeFloat (it.getSample(b)); break;
                        case DataBuffer.TYPE_DOUBLE: output.writeDouble(it.getSample(b)); break;
                    }
                }
            }
        }
    }

    /**
     * Ensures that the given value is in the range of unsigned short.
     *
     * @param  name    name of the value, for error message.
     * @param  value   the value to check.
     * @return {@code value}.
     */
    private static int ensureUnsignedShort(final String name, final int value) {
        if ((value & ~0xFFFF) == 0) {
            return value;
        }
        throw new RasterFormatException(Errors.format(Errors.Keys.ValueOutOfRange_4, name, 1, 0xFFFF, value));
    }
}
