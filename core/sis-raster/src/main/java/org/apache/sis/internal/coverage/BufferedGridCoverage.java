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
package org.apache.sis.internal.coverage;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.RasterFormatException;
import java.awt.image.RenderedImage;
import java.util.Collection;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.opengis.coverage.CannotEvaluateException;

/**
 * A GridCoverage which datas are stored in an in-memory buffer.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class BufferedGridCoverage extends GridCoverage {

    private final DataBuffer data;
    private GridCoverage converted;

    /**
     * Constructs a grid coverage using the specified grid geometry, sample dimensions and data type.
     *
     * @param grid the grid extent, CRS and conversion from cell indices to CRS.
     * @param bands sample dimensions for each image band.
     * @param dataType One of DataBuffer.TYPE_* , the native data type used to store the coverage values.
     */
    public BufferedGridCoverage(final GridGeometry grid, final Collection<? extends SampleDimension> bands, int dataType) {
        super(grid, bands);
        long nbSamples = bands.size();
        GridExtent extent = grid.getExtent();
        for (int i = 0; i<grid.getDimension(); i++) {
            nbSamples *= extent.getSize(i);
        }
        final int nbSamplesi = Math.toIntExact(nbSamples);

        switch (dataType) {
            case DataBuffer.TYPE_BYTE   : this.data = new DataBufferByte(nbSamplesi); break;
            case DataBuffer.TYPE_SHORT  : this.data = new DataBufferShort(nbSamplesi); break;
            case DataBuffer.TYPE_USHORT : this.data = new DataBufferUShort(nbSamplesi); break;
            case DataBuffer.TYPE_INT    : this.data = new DataBufferInt(nbSamplesi); break;
            case DataBuffer.TYPE_FLOAT  : this.data = new DataBufferFloat(nbSamplesi); break;
            case DataBuffer.TYPE_DOUBLE : this.data = new DataBufferDouble(nbSamplesi); break;
            default: throw new IllegalArgumentException("Unsupported data type "+ dataType);
        }
    }

    @Override
    public RenderedImage render(GridExtent sliceExtent) throws CannotEvaluateException {
        try {
            final ImageRenderer renderer = new ImageRenderer(this, sliceExtent);
            renderer.setData(data);
            return renderer.image();
        } catch (IllegalArgumentException | ArithmeticException | RasterFormatException e) {
            throw new CannotEvaluateException(e.getMessage(), e);
        }
    }

    @Override
    public GridCoverage forConvertedValues(boolean converted) {
        if (converted) {
            synchronized (this) {
                if (this.converted == null) {
                    this.converted = ConvertedGridCoverage.convert(this);
                }
                return this.converted;
            }
        }
        return this;
    }

}
