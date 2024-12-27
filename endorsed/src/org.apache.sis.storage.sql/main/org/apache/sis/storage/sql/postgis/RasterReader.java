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
package org.apache.sis.storage.sql.postgis;

import java.util.List;
import java.util.Arrays;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.lang.reflect.Array;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.image.privy.ColorModelBuilder;
import org.apache.sis.image.privy.ColorModelFactory;
import org.apache.sis.image.privy.ObservableImage;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.sql.feature.InfoStatements;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.Vector;
import org.apache.sis.util.resources.Errors;
import static org.apache.sis.storage.sql.postgis.Band.OPPOSITE_SIGN;
import org.apache.sis.pending.jdk.JDK18;


/**
 * A reader of rasters encoded in <i>Well Known Binary</i> (WKB) format.
 * This format is specific to PostGIS 2 (this is not yet an OGC standard at the
 * time of writing this class), but it can nevertheless be used elsewhere.
 *
 * <h2>Multi-threading</h2>
 * This class is <strong>not</strong> safe for multi-threading.
 * Furthermore, if a non-null {@link InfoStatements} has been specified to the constructor,
 * then this object is valid only as long as the caller holds a connection to the database.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class RasterReader extends RasterFormat {
    /**
     * Conversion from pixel coordinates to CRS coordinates.
     * This is defined by 6 affine transform coefficients similar to the <i>World File</i> format.
     */
    private AffineTransform2D gridToCRS;

    /**
     * The default Coordinate Reference System (CRS) if the raster does not specify a CRS.
     * This is {@code null} if there is no default.
     */
    public CoordinateReferenceSystem defaultCRS;

    /**
     * The spatial reference identifier, or 0 if undefined.
     * Note that this is a primary key in the {@code "spatial_ref_sys"} table, not necessarily an EPSG code.
     */
    private int srid;

    /**
     * Information about each band (including pixel values), or {@code null} if none.
     */
    private Band[] bands;

    /**
     * The sample model using during the last read operation, for opportunistic reuse of existing instance.
     * This is effective when reading may rasters of the same type and size.
     */
    private transient SampleModel cachedModel;

    /**
     * A temporary buffer for the bytes in the process of being decoded.
     * Initially null and created when first needed.
     */
    private ByteBuffer buffer;

    /**
     * Creates a new reader. If the {@code spatialRefSys} argument is non-null,
     * then this object is valid only as long as the caller holds a connection to the database.
     *
     * @param  spatialRefSys  the object to use for building CRS from the {@code "spatial_ref_sys"} table,
     *                        or {@code null} for using the SRID as an EPSG code.
     */
    public RasterReader(final InfoStatements spatialRefSys) {
        super(spatialRefSys);
    }

    /**
     * Restores this reader to its initial state.
     * This method can be invoked for reading more than one raster with the same reader.
     */
    public void reset(){
        gridToCRS = null;
        bands     = null;
        srid      = 0;
    }

    /**
     * Returns the conversion from pixel coordinates to CRS coordinates found in the last raster read.
     * This property is non-null only if a {@code read(…)} method has been invoked and {@link #reset()}
     * has not been invoked.
     *
     * @return conversion from pixel coordinates to CRS coordinates, or {@code null} if undefined.
     */
    public AffineTransform2D getGridToCRS() {
        return gridToCRS;
    }

    /**
     * Returns the spatial reference identifier for the last raster read.
     * This property is non-zero only if a {@code read(…)} method has been
     * invoked and {@link #reset()} has not been invoked.
     *
     * @return spatial reference identifier, or 0 if undefined.
     */
    public int getSRID(){
        return srid;
    }

    /**
     * Returns {@code true} if the sample dimensions need a transfer function
     * for specifying the "no data" value or for handling the sign of data.
     */
    private boolean needsTransferFunction() {
        for (final Band band : bands) {
            if (band.noDataValue != null || (band.getDataBufferType() & OPPOSITE_SIGN) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the exception to throw for malformed or unsupported WKB data.
     */
    private static RasterFormatException malformed(final ChannelDataInput input) {
        return new RasterFormatException(Errors.format(Errors.Keys.UnexpectedFileFormat_2, "WKB", input.filename));
    }

    /**
     * Parses a raster from the given input stream and returns a single tile.
     *
     * @param  input  source of bytes to read.
     * @return the raster, or {@code null} if the raster is empty.
     * @throws IOException in an error occurred while reading from the given input.
     * @throws RasterFormatException if the raster format is not supported by current implementation.
     * @throws ArithmeticException if the raster is too large.
     */
    @SuppressWarnings("PointlessBitwiseExpression")
    public WritableRaster readAsRaster(final ChannelDataInput input) throws IOException {
        final ByteOrder order;
        switch (input.readUnsignedByte()) {
            case 0:  order = ByteOrder.BIG_ENDIAN;    break;
            case 1:  order = ByteOrder.LITTLE_ENDIAN; break;
            default: throw malformed(input);
        }
        input.buffer.order(order);
        final int version = input.readUnsignedShort();
        if (version != 0) {
            throw new IOException(Errors.format(Errors.Keys.UnsupportedFormatVersion_2, "WKB", version));
        }
        final int  numBands = input.readUnsignedShort();
        final double scaleX = input.readDouble();
        final double scaleY = input.readDouble();
        final double ipX    = input.readDouble();
        final double ipY    = input.readDouble();
        final double skewX  = input.readDouble();
        final double skewY  = input.readDouble();
        gridToCRS = new AffineTransform2D(scaleX, skewY, skewX, scaleY, ipX, ipY);
        srid = input.readInt();
        if (numBands == 0) {      // Empty raster.
            return null;
        }
        final int width  = input.readUnsignedShort();
        final int height = input.readUnsignedShort();
        final Band[] bands = new Band[numBands];
        for (int i=0; i<numBands; i++) {
            final Band band = new Band(input.readUnsignedByte());
            final int dataType = band.getDataBufferType();
            final Number nodata;
            switch (dataType) {
                case DataBuffer.TYPE_BYTE:                 nodata = input.readUnsignedByte(); break;
                case DataBuffer.TYPE_BYTE | OPPOSITE_SIGN: nodata = input.readByte(); break;
                case DataBuffer.TYPE_SHORT:                nodata = input.readShort(); break;
                case DataBuffer.TYPE_USHORT:               nodata = input.readUnsignedShort(); break;
                case DataBuffer.TYPE_INT:                  nodata = input.readInt(); break;
                case DataBuffer.TYPE_INT | OPPOSITE_SIGN:  nodata = input.readUnsignedInt(); break;
                case DataBuffer.TYPE_FLOAT:                nodata = input.readFloat(); break;
                case DataBuffer.TYPE_DOUBLE:               nodata = input.readDouble(); break;
                case DataBuffer.TYPE_UNDEFINED:            // For detecting case conflict at compile time.
                default: throw malformed(input);
            }
            if (band.hasNodata()) {
                band.noDataValue = nodata;
            }
            if (band.isOffline()) {
                throw new RasterFormatException("Offline raster data is not yet supported.");
            } else {
                /*
                 * Read sample values for the current band.
                 * We ignore the signed or unsigned nature of values here.
                 */
                final int sampleSize  = band.getDataTypeSize();                 // In bits: 1, 2, 4, 8, 16, 32 or 64.
                final int elementSize = DataBuffer.getDataTypeSize(dataType);   // Same as above except for 1, 2, 4.
                final int length = Math.toIntExact(JDK18.ceilDiv(Math.multiplyFull(width, height) * sampleSize, elementSize));
                final Object data;
                switch (dataType & ~OPPOSITE_SIGN) {
                    case DataBuffer.TYPE_USHORT:
                    case DataBuffer.TYPE_SHORT:  data =            input.readShorts (length);  break;
                    case DataBuffer.TYPE_BYTE:   data =            input.readBytes  (length);  break;
                    case DataBuffer.TYPE_INT:    data =            input.readInts   (length);  break;
                    case DataBuffer.TYPE_FLOAT:  data = band.toNaN(input.readFloats (length)); break;
                    case DataBuffer.TYPE_DOUBLE: data = band.toNaN(input.readDoubles(length)); break;
                    default: throw malformed(input);
                }
                band.data = data;
            }
            bands[i] = band;
        }
        /*
         * All bands should be of the same type. We could convert data to
         * a common type (the largest one), but this is not yet implemented.
         */
        final Band firstBand = bands[0];
        int dataType = firstBand.getDataBufferType();
        final int length = Array.getLength(firstBand.data);
        final Object[] arrays = (Object[]) Array.newInstance(firstBand.data.getClass(), numBands);
        arrays[0] = firstBand.data;
        for (int b=1; b<numBands; b++) {
            final Band band = bands[b];
            if (band.getDataBufferType() != dataType || Array.getLength(band.data) != length) {
                throw new RasterFormatException("Bands of different types are not yet supported.");
            }
            arrays[b] = band.data;
        }
        this.bands = bands;
        /*
         * Create the `DataBuffer` and `SampleModel` for the bands.
         * Objects are only wrappers; pixel values are not copied.
         */
        final DataBuffer buffer;
        switch (dataType &= ~OPPOSITE_SIGN) {
            case DataBuffer.TYPE_BYTE:    buffer = new DataBufferByte  ((byte  [][]) arrays, length); break;
            case DataBuffer.TYPE_SHORT:   buffer = new DataBufferShort ((short [][]) arrays, length); break;
            case DataBuffer.TYPE_USHORT:  buffer = new DataBufferUShort((short [][]) arrays, length); break;
            case DataBuffer.TYPE_INT:     buffer = new DataBufferInt   ((int   [][]) arrays, length); break;
            case DataBuffer.TYPE_FLOAT:   buffer = new DataBufferFloat ((float [][]) arrays, length); break;
            case DataBuffer.TYPE_DOUBLE:  buffer = new DataBufferDouble((double[][]) arrays, length); break;
            default: throw malformed(input);
        }
        SampleModel model;
        final int sampleSize = firstBand.getDataTypeSize();
        if (sampleSize >= Byte.SIZE) {
            model = new BandedSampleModel(dataType, width, height, numBands);
        } else if (numBands == 1) {
            model = new MultiPixelPackedSampleModel(dataType, width, height, sampleSize);
        } else {
            throw new RasterFormatException("Multi-bands packed model is not yet supported.");
        }
        if (model.equals(cachedModel)) {
            model = cachedModel;
        }
        cachedModel = model;
        return WritableRaster.createWritableRaster(model, buffer, null);
    }

    /**
     * Parses a raster from the given input stream and returns as an image.
     *
     * @param  input  source of bytes to read.
     * @return the raster as an image, or {@code null} if the raster is empty.
     * @throws IOException in an error occurred while reading from the given input.
     * @throws RasterFormatException if the raster format is not supported by current implementation.
     * @throws ArithmeticException if the raster is too large.
     */
    public BufferedImage readAsImage(final ChannelDataInput input) throws IOException {
        final WritableRaster raster = readAsRaster(input);
        if (raster == null) {
            return null;
        }
        final ColorModel cm;
        final SampleModel sm = raster.getSampleModel();
        final int dataType = sm.getDataType();
        final int numBands = sm.getNumBands();
        if ((numBands == 3) && (dataType == DataBuffer.TYPE_BYTE)) {
            cm = new ColorModelBuilder().createBandedRGB();
        } else {
            final int visibleBand = 0;              // Arbitrary value (could be configurable).
            final double minimum, maximum;
            if (sm instanceof MultiPixelPackedSampleModel) {
                final int sampleSize = ((MultiPixelPackedSampleModel) sm).getPixelBitStride();
                maximum = (1 << sampleSize) - 1;
                minimum = 0;
            } else if (dataType == DataBuffer.TYPE_BYTE) {
                minimum = 0;
                maximum = 0xFF;
            } else {
                final Band band = bands[visibleBand];
                final NumberRange<?> range = Vector.create(band.data, band.isUnsigned()).range();
                minimum = range.getMinDouble();
                maximum = range.getMaxDouble();
            }
            cm = ColorModelFactory.createGrayScale(dataType, numBands, visibleBand, minimum, maximum);
        }
        return new ObservableImage(cm, raster, false, null);
    }

    /**
     * Parses a raster from the given input stream and returns as a coverage.
     *
     * @param  input  source of bytes to read.
     * @return the raster as a coverage, or {@code null} if the raster is empty.
     * @throws Exception in an error occurred while reading from the given input or creating the coverage.
     *         Exception type may be I/O, SQL, factory, data store, arithmetic, raster format, <i>etc.</i>,
     *         too numerous for enumerating them all.
     */
    public GridCoverage readAsCoverage(final ChannelDataInput input) throws Exception {
        final BufferedImage image = readAsImage(input);
        if (image == null) {
            return null;
        }
        CoordinateReferenceSystem crs = null;
        final int srid = getSRID();
        if (spatialRefSys != null) {
            crs = spatialRefSys.fetchCRS(srid);
        } else if (srid > 0) {
            crs = CRS.forCode(Constants.EPSG + ':' + srid);
        }
        if (crs == null) {
            crs = defaultCRS;
        }
        final GridExtent   extent = new GridExtent(image.getWidth(), image.getHeight());
        final GridGeometry domain = new GridGeometry(extent, ANCHOR, getGridToCRS(), crs);
        /*
         * Create pseudo-categories with a transfer function if we need to specify "no data" value,
         * or the sign of stored data do not match the sign of expected values.
         */
        List<SampleDimension> range = null;
        if (needsTransferFunction()) {
            final SampleDimension[] sd = new SampleDimension[bands.length];
            final SampleDimension.Builder builder = new SampleDimension.Builder();
            for (int b=0; b<sd.length; b++) {
                final Band band = bands[b];
                if ((band.getDataBufferType() & OPPOSITE_SIGN) != 0) {
                    // See `Band.OPPOSITE_SIGN` javadoc for more information on this limitation.
                    throw new RasterFormatException("Data type not yet supported.");
                }
                sd[b] = builder.setName(b + 1).setBackground(band.noDataValue).build();
                builder.clear();
            }
            range = Arrays.asList(sd);
        }
        return new GridCoverage2D(domain, range, image);
    }

    /**
     * Wraps the given input stream into a channel that can be used by {@code read(…)} methods in this class.
     * The returned channel should be used and discarded before to create a new {@code ChannelDataInput},
     * because this method recycles the same {@link ByteBuffer}.
     *
     * @param  input  the input stream to wrap.
     * @return a channel together with a buffer.
     * @throws IOException if an error occurred while reading data from the input stream.
     */
    public ChannelDataInput channel(final InputStream input) throws IOException {
        if (buffer == null) {
            buffer = ByteBuffer.allocate(8192);
        }
        return new ChannelDataInput("raster", Channels.newChannel(input), buffer, false);
    }
}
