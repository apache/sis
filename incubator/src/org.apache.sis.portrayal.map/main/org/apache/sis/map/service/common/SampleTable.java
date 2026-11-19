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
package org.apache.sis.map.service.common;


/**
 * The sample values of the cells of a two-dimensional grid, in row-major order.
 * A missing value (no data, or a cell outside the coverage) is NaN.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SampleTable {
    /**
     * Number of cells in a row.
     */
    private final int width;

    /**
     * Number of rows.
     */
    private final int height;

    /**
     * Sample values as {@code values[band][y*width + x]}.
     */
    private final double[][] values;

    /**
     * Whether each band holds 8 bits integers.
     */
    private final boolean[] bytes;

    /**
     * Whether each cell is inside the coverage.
     */
    private final boolean[] inside;

    /**
     * Creates a table of sample values.
     *
     * @param  width   number of cells in a row.
     * @param  height  number of rows.
     * @param  values  sample values as {@code values[band][y*width + x]}.
     * @param  bytes   whether each band holds 8 bits integers.
     * @param  inside  whether each cell is inside the coverage.
     */
    SampleTable(final int width, final int height, final double[][] values, final boolean[] bytes, final boolean[] inside) {
        this.width  = width;
        this.height = height;
        this.values = values;
        this.bytes  = bytes;
        this.inside = inside;
    }

    /**
     * Returns the number of cells in a row.
     *
     * @return the grid width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the number of rows.
     *
     * @return the grid height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the number of cells.
     *
     * @return width × height.
     */
    public int size() {
        return width * height;
    }

    /**
     * Returns the number of bands.
     *
     * @return the number of values of each cell.
     */
    public int getNumBands() {
        return values.length;
    }

    /**
     * Returns the sample value of a cell.
     *
     * @param  band  the band index.
     * @param  cell  the cell index.
     * @return the sample value, NaN if missing.
     */
    public double value(final int band, final int cell) {
        return values[band][cell];
    }

    /**
     * Returns a copy of the values of all cells in the given band.
     *
     * @param  band  the band index.
     * @return the sample values in row-major order, NaN if missing.
     */
    public double[] band(final int band) {
        return values[band].clone();
    }

    /**
     * Returns whether the given band holds 8 bits integers.
     *
     * @param  band  the band index.
     * @return whether the values are between 0 and 255.
     */
    public boolean isByte(final int band) {
        return bytes[band];
    }

    /**
     * Returns whether the given cell is inside the coverage. A cell inside the coverage may have no data.
     *
     * @param  cell  the cell index.
     * @return whether the cell center is inside a coverage cell.
     */
    public boolean isInside(final int cell) {
        return inside[cell];
    }

    /**
     * Returns whether all sample values of the given cell are missing.
     *
     * @param  cell  the cell index.
     * @return whether the cell has no data.
     */
    public boolean isEmpty(final int cell) {
        for (final double[] band : values) {
            if (!Double.isNaN(band[cell])) {
                return false;
            }
        }
        return true;
    }
}
