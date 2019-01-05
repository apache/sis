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
import java.util.Locale;
import java.awt.image.RenderedImage;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;

// Branch-specific imports
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;


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
     * Returns a two-dimensional slice of grid data as a rendered image. The given {@code slicePoint} argument specifies
     * the coordinates of the slice in all dimensions that are not in the two-dimensional image. For example if this grid
     * coverage has (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>) dimensions and we want to render an image
     * of data in the (<var>x</var>, <var>y</var>) dimensions, then the given {@code slicePoint} shall contain the
     * (<var>z</var>, <var>t</var>) coordinates of the desired slice. The two coordinates of the data to be shown
     * (<var>x</var> and <var>y</var> in our example) shall be excluded from the slice point in one of the following ways:
     *
     * <ul>
     *   <li>The {@code slicePoint} has a CRS with two dimensions less than this grid coverage CRS.</li>
     *   <li>The {@code slicePoint} has the same CRS than this grid coverage, but the two coordinates to
     *       exclude are set to {@link Double#NaN}.</li>
     * </ul>
     *
     * If the {@code slicePoint} CRS is different than this grid coverage CRS (except for the number of dimensions),
     * a coordinate transformation will be applied. If the {@code slicePoint} CRS is {@code null}, it is assumed the
     * same than this grid coverage CRS. If this grid coverage is two-dimensional or can render only one image for
     * other reason, then the {@code slicePoint} can be null.
     *
     * <p>Implementations should return a view as much as possible, without copying sample values.</p>
     *
     * @param  slicePoint  coordinates of the slice in all dimensions other than the two dimensions to be shown on the image.
     *         May be {@code null} if this coverage can render only one image, for example because its CRS is two-dimensional.
     * @return the grid slice as a rendered image.
     * @throws PointOutsideCoverageException if the given slice point is illegal.
     * @throws SubspaceNotSpecifiedException if the given argument is not sufficient for reducing the grid to a two-dimensional slice.
     * @throws CannotEvaluateException if this method can not produce the rendered image for another reason.
     */
    public abstract RenderedImage render(DirectPosition slicePoint) throws CannotEvaluateException;

    /**
     * Returns a string representation of this grid coverage for debugging purpose.
     * The returned string is implementation dependent and may change in any future version.
     * Current implementation is equivalent to the following, where {@code EXTENT}, <i>etc.</i> are
     * constants defined in {@link GridGeometry} class:
     *
     * {@preformat java
     *   return toTree(Locale.getDefault(), EXTENT | ENVELOPE | CRS | GRID_TO_CRS | RESOLUTION).toString();
     * }
     *
     * @return a string representation of this grid coverage for debugging purpose.
     */
    @Override
    public String toString() {
        return toTree(Locale.getDefault(), GridGeometry.EXTENT | GridGeometry.ENVELOPE
                | GridGeometry.CRS | GridGeometry.GRID_TO_CRS | GridGeometry.RESOLUTION).toString();
    }

    /**
     * Returns a tree representation of some elements of this grid coverage.
     * The tree representation is for debugging purpose only and may change
     * in any future SIS version.
     *
     * @param  locale   the locale to use for textual labels.
     * @param  bitmask  combination of {@link GridGeometry} flags.
     * @return a tree representation of the specified elements.
     *
     * @see GridGeometry#toTree(Locale, int)
     */
    @Debug
    public TreeTable toTree(final Locale locale, final int bitmask) {
        ArgumentChecks.ensureNonNull("locale", locale);
        final Vocabulary vocabulary = Vocabulary.getResources(locale);
        final TableColumn<CharSequence> column = TableColumn.VALUE_AS_TEXT;
        final TreeTable tree = new DefaultTreeTable(column);
        final TreeTable.Node root = tree.getRoot();
        root.setValue(column, Classes.getShortClassName(this));
        TreeTable.Node branch = root.newChild();
        branch.setValue(column, vocabulary.getString(Vocabulary.Keys.CoverageDomain));
        gridGeometry.formatTo(locale, vocabulary, bitmask, branch);
        branch = root.newChild();
        branch.setValue(column, vocabulary.getString(Vocabulary.Keys.SampleDimensions));
        branch.newChild().setValue(column, SampleDimension.toString(locale, sampleDimensions));
        return tree;
    }
}
