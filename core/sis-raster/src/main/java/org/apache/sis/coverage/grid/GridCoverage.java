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
package org.apache.sis.coverage.grid;

import java.util.List;
import java.util.Collection;
import java.awt.image.RenderedImage;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.util.ArgumentChecks;


/**
 * Base class of coverages with domains defined as a set of grid points.
 * The essential property of coverage is to be able to generate a value for any point within its domain.
 * Since a grid coverage is represented by a grid of values, the value returned by the coverage for a point
 * is that of the grid value whose location is nearest the point.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public abstract class GridCoverage {
    /**
     * The grid extent, coordinate reference system (CRS) and conversion from cell indices to CRS.
     */
    private final GridGeometry gridGeometry;

    /**
     * List of sample dimension (band) information for the grid coverage. Information include such things
     * as description, the no data values, minimum and maximum values, <i>etc</i>. A coverage must have
     * at least one sample dimension. The content of this array shall never be modified.
     */
    private final SampleDimension[] sampleDimensions;

    /**
     * Constructs a grid coverage using the specified grid geometry and sample dimensions.
     *
     * @param grid   the grid extent, CRS and conversion from cell indices to CRS.
     * @param bands  sample dimensions for each image band.
     */
    protected GridCoverage(final GridGeometry grid, final Collection<? extends SampleDimension> bands) {
        ArgumentChecks.ensureNonNull("grid",  grid);
        ArgumentChecks.ensureNonNull("bands", bands);
        gridGeometry = grid;
        sampleDimensions = bands.toArray(new SampleDimension[bands.size()]);
        for (int i=0; i<sampleDimensions.length; i++) {
            ArgumentChecks.ensureNonNullElement("bands", i, sampleDimensions[i]);
        }
    }

    /**
     * Returns the coordinate reference system to which the values in grid domain are referenced.
     * This is the CRS used when accessing a coverage with the {@code evaluate(…)} methods.
     * This coordinate reference system is usually different than the coordinate system of the grid.
     * It is the target coordinate reference system of the {@link GridGeometry#getGridToCRS gridToCRS}
     * math transform.
     *
     * <p>The default implementation delegates to {@link GridGeometry#getCoordinateReferenceSystem()}.</p>
     *
     * @return the CRS used when accessing a coverage with the {@code evaluate(…)} methods.
     * @throws IncompleteGridGeometryException if the grid geometry has no CRS.
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return gridGeometry.getCoordinateReferenceSystem();
    }

    /**
     * Returns information about the <cite>domain</cite> of this grid coverage.
     * Information includes the grid extent, CRS and conversion from cell indices to CRS.
     * {@code GridGeometry} can also provide derived information like bounding box and resolution.
     *
     * @return grid extent, CRS and conversion from cell indices to CRS.
     */
    public GridGeometry getGridGeometry() {
        return gridGeometry;
    }

    /**
     * Returns information about the <cite>range</cite> of this grid coverage.
     * Information include names, sample value ranges, fill values and transfer functions for all bands in this grid coverage.
     *
     * @return names, value ranges, fill values and transfer functions for all bands in this grid coverage.
     */
    public List<SampleDimension> getSampleDimensions() {
        return UnmodifiableArrayList.wrap(sampleDimensions);
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method tries to return a view as much as possible (i.e. sample values are not copied).
     *
     * @param  xAxis  dimension to use for <var>x</var> axis.
     * @param  yAxis  dimension to use for <var>y</var> axis.
     * @return the grid data as a rendered image in the given CRS dimensions.
     */
    public abstract RenderedImage asRenderedImage(int xAxis, int yAxis);

    /**
     * Returns a string representation of this grid coverage for debugging purpose.
     *
     * @return a string representation of this grid coverage for debugging purpose.
     */
    @Override
    public String toString() {
        final String lineSeparator = System.lineSeparator();
        final StringBuilder buffer = new StringBuilder(1000);
        buffer.append("Grid coverage domain:").append(lineSeparator);
        gridGeometry.formatTo(buffer, lineSeparator + "  ");
        return buffer.toString();
    }
}
