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
package org.apache.sis.storage.netcdf;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import java.nio.file.Path;
import org.opengis.util.GenericName;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Grid;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.storage.AbstractGridResource;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Numbers;
import ucar.nc2.constants.CDM;                      // We use only String constants.


/**
 * A grid coverage in a netCDF file. We create one resource for each variable,
 * unless we determine that two variables should be handled together (for example
 * the <var>u</var> and <var>v</var> components of wind vectors).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class GridResource extends AbstractGridResource implements ResourceOnFileSystem {
    /**
     * Names of attributes where to fetch minimum and maximum sample values, in preference order.
     */
    private static final String[] RANGE_ATTRIBUTES = {
        "valid_range",      // Expected "reasonable" range for variable.
        "actual_range",     // Actual data range for variable.
        "valid_min",        // Fallback if "valid_range" is not specified.
        "valid_max"
    };

    /**
     * The identifier of this grid resource. This is the variable name.
     *
     * @see #getIdentifier()
     */
    private final GenericName identifier;

    /**
     * The grid geometry (size, CRS…) of the {@linkplain #data} cube.
     *
     * @see #getGridGeometry()
     */
    private final GridGeometry gridGeometry;

    /**
     * The netCDF variable wrapped by this resource.
     */
    private final Variable data;

    /**
     * The sample dimension for the {@link #data} variable, created when first needed.
     *
     * @see #getSampleDimensions()
     */
    private SampleDimension definition;

    /**
     * Path to the netCDF file for information purpose, or {@code null} if unknown.
     *
     * @see #getComponentFiles()
     */
    private final Path location;

    /**
     * Creates a new resource.
     *
     * @param  decoder  the implementation used for decoding the netCDF file.
     * @param  grid     the grid geometry (size, CRS…) of the {@linkplain #data} cube.
     * @param  data     the variable providing actual data.
     */
    private GridResource(final Decoder decoder, final Grid grid, final Variable data) throws IOException, DataStoreException {
        super(decoder.listeners);
        this.data    = data;
        gridGeometry = grid.getGridGeometry(decoder);
        identifier   = decoder.nameFactory.createLocalName(decoder.namespace, data.getName());
        location     = decoder.location;
    }

    /**
     * Returns a list of all grid resources found in the netCDF file opened by the given decoder.
     * This method should be invoked only once and the result cached. The returned list is modifiable;
     * caller is free to add other elements.
     */
    static List<Resource> list(final Decoder decoder) throws IOException, DataStoreException {
        final List<Resource> resources = new ArrayList<>();
        for (final Variable variable : decoder.getVariables()) {
            final Grid grid = variable.getGridGeometry(decoder);
            if (grid != null) {
                resources.add(new GridResource(decoder, grid, variable));
            }
        }
        return resources;
    }

    /**
     * Returns the variable name as an identifier of this resource.
     */
    @Override
    public GenericName getIdentifier() {
        return identifier;
    }

    /**
     * Returns an object containing the grid size, the CRS and the conversion from grid indices to CRS coordinates.
     */
    @Override
    public GridGeometry getGridGeometry() {
        return gridGeometry;
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     */
    @Override
    public List<SampleDimension> getSampleDimensions() {
        if (definition == null) {
            /*
             * Gets minimum and maximum. If a "valid_range" attribute is present, it has precedence
             * over "valid_min" and "valid_max" as specified in the UCAR documentation.
             */
            Number minimum = null;
            Number maximum = null;
            Class<? extends Number> type = null;
            for (final String attribute : RANGE_ATTRIBUTES) {
                for (final Object element : data.getAttributeValues(attribute, true)) {
                    if (element instanceof Number) {
                        Number value = (Number) element;
                        if (element instanceof Float) {
                            final float fp = (Float) element;
                            if      (fp == +Float.MAX_VALUE) value = Float.POSITIVE_INFINITY;
                            else if (fp == -Float.MAX_VALUE) value = Float.NEGATIVE_INFINITY;
                        } else if (element instanceof Double) {
                            final double fp = (Double) element;
                            if      (fp == +Double.MAX_VALUE) value = Double.POSITIVE_INFINITY;
                            else if (fp == -Double.MAX_VALUE) value = Double.NEGATIVE_INFINITY;
                        }
                        type = Numbers.widestClass(type, value.getClass());
                        minimum = Numbers.cast(minimum, type);
                        maximum = Numbers.cast(maximum, type);
                        value   = Numbers.cast(value,   type);
                        if (minimum == null || compare(value, minimum) < 0) minimum = value;
                        if (maximum == null || compare(value, maximum) > 0) maximum = value;
                    }
                }
                if (minimum != null && maximum != null) break;
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            final NumberRange<?> range = new NumberRange(type, minimum, true, maximum, true);
            /*
             * Conversion from sample values to real values. If no scale factor and offset are specified,
             * then the default will be the identity transform.
             */
            final TransferFunction tr = new TransferFunction();
            double scale  = data.getAttributeAsNumber(CDM.SCALE_FACTOR);
            double offset = data.getAttributeAsNumber(CDM.ADD_OFFSET);
            if (!Double.isNaN(scale))  tr.setScale (scale);
            if (!Double.isNaN(offset)) tr.setOffset(offset);
            definition = new SampleDimension.Builder()
                    .addQuantitative(data.getName(), range, tr.getTransform(), data.getUnit()).build();
        }
        return Collections.singletonList(definition);
    }

    @SuppressWarnings("unchecked")
    private static int compare(final Number n1, final Number n2) {
        return ((Comparable) n1).compareTo((Comparable) n2);
    }

    /**
     * Gets the paths to files used by this resource, or an empty array if unknown.
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        return (location != null) ? new Path[] {location} : new Path[0];
    }
}
