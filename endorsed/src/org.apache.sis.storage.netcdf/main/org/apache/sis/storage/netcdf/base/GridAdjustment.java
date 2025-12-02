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
package org.apache.sis.storage.netcdf.base;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * Contains information computed together with {@link Variable#findGrid(GridAdjustment)} but which are still specific
 * to the variable. Those information are kept in a class separated from {@link Grid} because the same {@code Grid}
 * instance may apply to many variables while {@code GridAdjustment} may contain amendments that are specific to a
 * particular {@link Variable} instance.
 *
 * <p>Instance is created by {@link Variable#getGridGeometry()} and updated by {@link Variable#findGrid(GridAdjustment)}.
 * Subclasses of {@link Variable} do not need to know the details of this class; they just need to pass it verbatim
 * to their parent class.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GridAdjustment {
    /**
     * Factors by which to multiply a grid index in order to get the corresponding data index, or {@code null} if none.
     * This is usually null, meaning that there is an exact match between grid indices and data indices. This array may
     * be non-null if the localization grid has shorter dimensions than the dimensions of the variable, as documented
     * in {@link Convention#nameOfDimension(Variable, int)} javadoc.
     *
     * <p>Created by {@link Variable#findGrid(GridAdjustment)} and is consumed by {@link Variable#getGridGeometry()}.
     * Some values may be {@link Double#NaN} if the {@code "resampling_interval"} attribute was not found.
     * This array may be longer than necessary.</p>
     *
     * @see #dataToGridIndices()
     */
    private double[] gridToDataIndices;

    /**
     * Maps grid dimensions to variable dimensions when those dimensions are not the same. This map should always be empty,
     * except in the case described in {@link #mapLabelToGridDimensions mapLabelToGridDimensions(…)} method. If non-empty,
     * then the keys are dimensions in the {@link Grid} and values are corresponding dimensions in the {@link Variable}.
     */
    final Map<Dimension,Dimension> gridToVariable;

    /**
     * Only {@link Variable#getGridGeometry()} should instantiate this class.
     */
    GridAdjustment() {
        gridToVariable = new HashMap<>();
    }

    /**
     * Builds a map of "dimension labels" to the actual {@link Dimension} instances of the grid.
     * The dimension labels are not the dimension names, but some other convention-dependent identifiers.
     * The mechanism is documented in {@link Convention#nameOfDimension(Variable, int)}.
     * For example, given a file with the following netCDF variables:
     *
     * <pre class="text">
     *     float Latitude(grid_y, grid_x)
     *       dim0 = "Line grids"
     *       dim1 = "Pixel grids"
     *       resampling_interval = 10
     *     float Longitude(grid_y, grid_x)
     *       dim0 = "Line grids"
     *       dim1 = "Pixel grids"
     *       resampling_interval = 10
     *     ushort SST(data_y, data_x)
     *       dim0 = "Line grids"
     *       dim1 = "Pixel grids"</pre>
     *
     * this method will add the following entries in the {@code toGridDimensions} map, provided that
     * the dimensions are not already keys in that map:
     *
     * <pre class="text">
     *     "Line grids"   →  Dimension[grid_x]
     *     "Pixel grids"  →  Dimension[grid_y]</pre>
     *
     * @param  variable          the variable for which a "label to grid dimensions" mapping is desired.
     * @param  axes              all axes in the netCDF file (not only the variable axes).
     * @param  toGridDimensions  in input, the dimensions to accept. In output, "label → grid dimension" entries.
     * @param  convention        convention for getting dimension labels.
     * @return {@code true} if the {@code Variable.findGrid(…)} caller should abort.
     *
     * @see Convention#nameOfDimension(Variable, int)
     */
    boolean mapLabelToGridDimensions(final Variable variable, final List<Variable> axes,
            final Map<Object,Dimension> toGridDimensions, final Convention convention)
    {
        final Set<Dimension> requestedByConvention = new HashSet<>();                       // Only in case of ambiguities.
        final String[] namesOfAxisVariables = convention.namesOfAxisVariables(variable);    // Only in case of ambiguities.
        for (final Variable axis : axes) {
            final boolean isRequested = ArraysExt.containsIgnoreCase(namesOfAxisVariables, axis.getName());
            final List<Dimension> candidates = axis.getGridDimensions();
            for (int j=candidates.size(); --j >= 0;) {
                final Dimension dim = candidates.get(j);
                if (toGridDimensions.containsKey(dim)) {
                    /*
                     * Found a dimension that has not already be taken by the 'dimensions' array.
                     * If this dimension has a name defined by an attribute like "Dim0" or "Dim1",
                     * make this dimension available for consideration by 'dimensions[i] = …' later.
                     */
                    final String name = convention.nameOfDimension(axis, j);
                    if (name != null) {
                        if (gridToDataIndices == null) {
                            gridToDataIndices = new double[axes.size()];    // Conservatively use longest possible length.
                        }
                        gridToDataIndices[j] = convention.gridToDataIndices(axis);
                        final boolean overwrite = isRequested && requestedByConvention.add(dim);
                        final Dimension previous = toGridDimensions.put(name, dim);
                        if (previous != null && !previous.equals(dim)) {
                            /*
                             * The same name maps to two different dimensions. Given the ambiguity, we should give up.
                             * However, we make an exception if only one dimension is part of a variable that has been
                             * explicitly requested. We identify this disambiguation in the following ways:
                             *
                             *   isRequested = true   →  ok if overwrite = true  →  keep the newly added dimension.
                             *   isRequested = false  →  if was previously in requestedByConvention, restore previous.
                             */
                            if (!overwrite) {
                                if (!isRequested && requestedByConvention.contains(dim)) {
                                    toGridDimensions.put(name, previous);
                                } else {
                                    // Variable.getGridGeometry() is (indirectly) the caller of this method.
                                    variable.error(Variable.class, "getGridGeometry", null, Errors.Keys.DuplicatedIdentifier_1, name);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the factors by which to multiply a data index in order to get the corresponding grid index,
     * or {@code null} if none. This array may be non-null if the localization grid has shorter dimensions
     * than the ones of the variable (see {@link #mapLabelToGridDimensions mapLabelToGridDimensions(…)}).
     * Caller needs to verify that the returned array, if non-null, is long enough.
     */
    final double[] dataToGridIndices() {
        double[] dataToGridIndices = null;
        if (gridToDataIndices != null) {
            for (int i=gridToDataIndices.length; --i >= 0;) {
                final double s = gridToDataIndices[i];
                if (s > 0 && s != Double.POSITIVE_INFINITY) {
                    if (dataToGridIndices == null) {
                        dataToGridIndices = new double[i + 1];
                    }
                    dataToGridIndices[i] = 1 / s;
                } else {
                    dataToGridIndices = null;
                    // May return a shorter array.
                }
            }
        }
        return dataToGridIndices;
    }

    /**
     * Creates a new grid geometry with a scale factor applied in grid coordinates before the "grid to CRS" conversion.
     *
     * @param  geometry           the grid geometry to scale.
     * @param  extent             the extent to allocate to the new grid geometry.
     * @param  anchor             the transform to adjust: "center to CRS" or "corner to CRS".
     * @param  dataToGridIndices  value of {@link #dataToGridIndices()}.
     * @return scaled grid geometry.
     */
    static GridGeometry scale(final GridGeometry geometry, final GridExtent extent, final PixelInCell anchor,
                              final double[] dataToGridIndices)
    {
        MathTransform gridToCRS = geometry.getGridToCRS(anchor);
        final LinearTransform scale = MathTransforms.scale(dataToGridIndices);
        gridToCRS = MathTransforms.concatenate(scale, gridToCRS);
        return new GridGeometry(extent, anchor, gridToCRS,
                geometry.isDefined(GridGeometry.CRS) ? geometry.getCoordinateReferenceSystem() : null);
    }
}
