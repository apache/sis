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
package org.apache.sis.storage.esri;

import java.util.Map;
import java.util.LinkedHashMap;
import java.io.IOException;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.WritableGridCoverageResource;
import org.apache.sis.storage.IncompatibleResourceException;
import org.apache.sis.storage.base.WritableGridCoverageSupport;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.StringBuilders;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.grid.SequenceType;


/**
 * An ASCII Grid store with writing capabilities.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class WritableStore extends AsciiGridStore implements WritableGridCoverageResource {
    /**
     * The line separator for writing the ASCII file.
     */
    private final String lineSeparator;

    /**
     * The output if this store is write-only, or {@code null} if this store is read/write.
     * This is set to {@code null} when the store is closed.
     */
    private ChannelDataOutput output;

    /**
     * Creates a new ASCII Grid store from the given file, URL or stream.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the stream.
     */
    public WritableStore(final AsciiGridStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector, false);
        lineSeparator = System.lineSeparator();
        if (!super.canReadOrWrite(false)) {
            output = connector.commit(ChannelDataOutput.class, AsciiGridStoreProvider.NAME);
        }
    }

    /**
     * Returns whether this store can read or write.
     */
    @Override
    boolean canReadOrWrite(final boolean write) {
        return write || super.canReadOrWrite(write);
    }

    /**
     * Returns an estimation of how close the "CRS to grid" transform is to integer values.
     * This is used for choosing whether to map pixel centers or pixel centers.
     */
    private static double distanceFromIntegers(final MathTransform gridToCRS) throws TransformException {
        final Matrix m = MathTransforms.getMatrix(gridToCRS.inverse());
        if (m != null && Matrices.isAffine(m)) {
            final int last = m.getNumCol() - 1;
            double sum = 0;
            for (int j=0; j<last; j++) {
                final double e = m.getElement(j, last);
                sum += Math.abs(Math.rint(e) - e);
            }
            return sum;
        }
        return Double.NaN;
    }

    /**
     * Gets the coefficients of the affine transform.
     *
     * @param  header  the map where to put the affine transform coefficients.
     * @param  gg      the grid geometry from which to get the affine transform.
     * @param  h       set of helper methods.
     * @return the iteration order (e.g. from left to right, then top to bottom).
     * @throws DataStoreException if the header cannot be written.
     */
    private static SequenceType getAffineCoefficients(
            final Map<String,Object> header, final GridGeometry gg,
            final WritableGridCoverageSupport h) throws DataStoreException
    {
        String xll = XLLCORNER;
        String yll = YLLCORNER;
        MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CORNER);
        try {
            final MathTransform alternative = gg.getGridToCRS(PixelInCell.CELL_CENTER);
            if (distanceFromIntegers(alternative) < distanceFromIntegers(gridToCRS)) {
                gridToCRS = alternative;
                xll = XLLCENTER;
                yll = YLLCENTER;
            }
        } catch (TransformException e) {
            throw new DataStoreReferencingException(h.canNotWrite(), e);
        }
        final AffineTransform at = h.getAffineTransform2D(gg.getExtent(), gridToCRS);
        if (at.getShearX() != 0 || at.getShearY() != 0) {
            throw new IncompatibleResourceException(h.rotationNotSupported(AsciiGridStoreProvider.NAME)).addAspect("gridToCRS");
        }
        double scaleX =  at.getScaleX();
        double scaleY = -at.getScaleY();
        double x = at.getTranslateX();
        double y = at.getTranslateY();
        if (scaleX > 0 && scaleY > 0) {
            y -= scaleY * (Integer) header.get(NROWS);
        } else {
            /*
             * TODO: future version could support other signs, provided that
             * we implement `PixelIterator` for other `SequenceType` values.
             */
            throw new IncompatibleResourceException(h.canNotWrite()).addAspect("gridToCRS");
        }
        header.put(xll, x);
        header.put(yll, y);
        if (scaleX == scaleY) {
            header.put(CELLSIZE, scaleX);
        } else {
            header.put(CELLSIZES[0], scaleX);
            header.put(CELLSIZES[1], scaleY);
        }
        return SequenceType.LINEAR;
    }

    /**
     * Writes the content of the given map as the header of ASCII Grid file.
     */
    private void writeHeader(final Map<String,Object> header, final ChannelDataOutput out) throws IOException {
        int maxKeyLength = 0;
        int maxValLength = 0;
        for (final Map.Entry<String,Object> entry : header.entrySet()) {
            final String text = entry.getValue().toString();
            entry.setValue(text);
            maxValLength = Math.max(maxValLength, text.length());
            maxKeyLength = Math.max(maxKeyLength, entry.getKey().length());
        }
        for (final Map.Entry<String,Object> entry : header.entrySet()) {
            String text = entry.getKey();
            write(text, out);
            write(CharSequences.spaces(maxKeyLength - text.length() + 1), out);
            text = (String) entry.getValue();
            write(CharSequences.spaces(maxValLength - text.length()), out);
            write(text, out);
            write(lineSeparator, out);
        }
    }

    /**
     * Writes a new coverage in the data store for this resource. If a coverage already exists for this resource,
     * then it will be overwritten only if the {@code TRUNCATE} or {@code UPDATE} option is specified.
     *
     * @param  coverage  new data to write in the data store for this resource.
     * @param  options   configuration of the write operation.
     * @throws DataStoreException if an error occurred while writing data in the underlying data store.
     */
    @Override
    public synchronized void write(GridCoverage coverage, final Option... options) throws DataStoreException {
        final var h = new WritableGridCoverageSupport(this, options);       // Does argument validation.
        final int band = 0;                                 // May become configurable in a future version.
        try {
            /*
             * If `output` is non-null, we are in write-only mode and there is no previously existing image.
             * Otherwise an image may exist and the behavior will depend on which options were supplied.
             */
            if (output == null && !h.replace(input().input)) {
                coverage = h.update(coverage);
            }
            final RenderedImage data = coverage.render(null);               // Fail if not two-dimensional.
            final Map<String,Object> header = new LinkedHashMap<>();
            header.put(NCOLS, data.getWidth());
            header.put(NROWS, data.getHeight());
            final SequenceType order = getAffineCoefficients(header, coverage.getGridGeometry(), h);
            /*
             * Open the destination channel only after the coverage has been validated by above method calls.
             * After this point we should not have any validation errors. Write the nodata value even if it is
             * "NaN" because the default is -9999, and we need to overwrite that default if it cannot be used.
             */
            final ChannelDataOutput out = (output != null) ? output : h.channel(input().input);
            final Number nodataValue = setCoverage(coverage, data, band);
            header.put(NODATA_VALUE, nodataValue);
            writeHeader(header, out);
            /*
             * Writes all sample values.
             */
            final float  nodataAsFloat  = nodataValue.floatValue();
            final double nodataAsDouble = nodataValue.doubleValue();
            final StringBuilder buffer  = new StringBuilder();
            final PixelIterator it      = new PixelIterator.Builder().setIteratorOrder(order).create(data);
            final int dataType          = it.getDataType().toDataBufferType();
            final int width             = it.getDomain().width;
            int remaining = width;
            while (it.next()) {
                switch (dataType) {
                    case DataBuffer.TYPE_DOUBLE: {
                        double value = it.getSampleDouble(band);
                        if (Double.isNaN(value)) {
                            value = nodataAsDouble;
                        }
                        buffer.append(value);
                        StringBuilders.trimFractionalPart(buffer);
                        break;
                    }
                    case DataBuffer.TYPE_FLOAT: {
                        float value = it.getSampleFloat(band);
                        if (Float.isNaN(value)) {
                            value = nodataAsFloat;
                        }
                        buffer.append(value);
                        StringBuilders.trimFractionalPart(buffer);
                        break;
                    }
                    default: {
                        buffer.append(it.getSample(band));
                        break;
                    }
                }
                write(buffer, out);
                buffer.setLength(0);
                if (--remaining != 0) {
                    out.writeByte(' ');
                } else {
                    write(lineSeparator, out);
                    remaining = width;
                }
            }
            out.flush();
            writePRJ();
            /*
             * If the channel is write-only (e.g. if we are writing in an `OutputStream`),
             * we will not be able to write a second time.
             */
            if (output != null) {
                output = null;
                out.channel.close();
            }
        } catch (IOException e) {
            closeOnError(e);
            throw new DataStoreException(e);
        }
    }

    /**
     * Writes the given text to the output. All characters must be US-ASCII (this is not verified).
     */
    private static void write(final CharSequence text, final ChannelDataOutput out) throws IOException {
        final int length = text.length();
        out.ensureBufferAccepts(length);
        for (int i=0; i<length; i++) {
            out.buffer.put((byte) text.charAt(i));
        }
    }

    /**
     * Closes this data store and releases any underlying resources.
     * If a read or write operation is in progress in another thread,
     * then this method blocks until that operation completed.
     * This restriction is for avoiding data lost.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        listeners.close();                      // Should never fail.
        final ChannelDataOutput out = output;
        output = null;
        if (out != null) try {
            out.channel.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        /*
         * No need for try-with-resource because only one
         * of `input` and `output` should be non-null.
         */
        super.close();
    }
}
