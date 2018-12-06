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
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.MathTransform1D;
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
import org.apache.sis.util.resources.Vocabulary;
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
     * Names of attributes where to fetch missing values, in preference order.
     * The union of all "no data" values will be stored, but the category name
     * will be inferred from the first attribute declaring the "no data" value.
     */
    private static final String[] NODATA_ATTRIBUTES = {
        CDM.MISSING_VALUE,
        CDM.FILL_VALUE
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
            final SampleDimension.Builder builder = new SampleDimension.Builder();
            NumberRange<?> range = data.getValidValues();
            if (range != null) {
                /*
                 * If scale_factor and/or add_offset variable attributes are present, then this is
                 * a "packed" variable. Otherwise the transfer function is the identity transform.
                 */
                final TransferFunction tr = new TransferFunction();
                final double scale  = data.getAttributeAsNumber(CDM.SCALE_FACTOR);
                final double offset = data.getAttributeAsNumber(CDM.ADD_OFFSET);
                if (!Double.isNaN(scale))  tr.setScale (scale);
                if (!Double.isNaN(offset)) tr.setOffset(offset);
                final MathTransform1D mt = tr.getTransform();
                if (!mt.isIdentity()) {
                    /*
                     * Heuristic rule defined in UCAR documentation (see EnhanceScaleMissing interface):
                     * if the type of the range is equals to the type of the scale, and the type of the
                     * data is not wider, then assume that the minimum and maximum are real values.
                     */
                    final int dataType  = data.getDataType().number;
                    final int rangeType = Numbers.getEnumConstant(range.getElementType());
                    if (rangeType >= dataType &&
                        rangeType >= Math.max(Numbers.getEnumConstant(data.getAttributeType(CDM.SCALE_FACTOR)),
                                              Numbers.getEnumConstant(data.getAttributeType(CDM.ADD_OFFSET))))
                    {
                        final boolean isMinIncluded = range.isMinIncluded();
                        final boolean isMaxIncluded = range.isMaxIncluded();
                        double minimum = (range.getMinDouble() - offset) / scale;
                        double maximum = (range.getMaxDouble() - offset) / scale;
                        if (maximum > minimum) {
                            final double swap = maximum;
                            maximum = minimum;
                            minimum = swap;
                        }
                        if (dataType < Numbers.FLOAT && minimum >= Long.MIN_VALUE && maximum <= Long.MAX_VALUE) {
                            range = NumberRange.create(Math.round(minimum), isMinIncluded, Math.round(maximum), isMaxIncluded);
                        } else {
                            range = NumberRange.create(minimum, isMinIncluded, maximum, isMaxIncluded);
                        }
                    }
                }
                builder.addQuantitative(data.getName(), range, mt, data.getUnit());
            }
            /*
             * Adds the "no data" or "fill value" as qualitative categories.
             */
            for (final String attribute : NODATA_ATTRIBUTES) {
                InternationalString name = null;
                for (final Object value : data.getAttributeValues(attribute, true)) {
                    if (value instanceof Number) {
                        final Number n = (Number) value;
                        final double fp = n.doubleValue();
                        if (!builder.intersect(fp, fp)) {
                            if (name == null) {
                                short key = Vocabulary.Keys.Nodata;
                                if (CDM.FILL_VALUE.equalsIgnoreCase(attribute)) {
                                    key = Vocabulary.Keys.FillValue;
                                }
                                name = Vocabulary.formatInternational(key);
                            }
                            @SuppressWarnings({"unchecked", "rawtypes"})
                            NumberRange<?> r = new NumberRange(value.getClass(), n, true, n, true);
                            builder.addQualitative(name, r);
                        }
                    }
                }
            }
            definition = builder.build();
        }
        return Collections.singletonList(definition);
    }

    /**
     * Gets the paths to files used by this resource, or an empty array if unknown.
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        return (location != null) ? new Path[] {location} : new Path[0];
    }
}
