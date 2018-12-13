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

import java.util.Map;
import java.util.List;
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
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.math.Vector;
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
    GridResource(final Decoder decoder, final Grid grid, final Variable data) throws IOException, DataStoreException {
        super(decoder.listeners);
        this.data    = data;
        gridGeometry = grid.getGridGeometry(decoder);
        identifier   = decoder.nameFactory.createLocalName(decoder.namespace, data.getName());
        location     = decoder.location;
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
             * Adds the "missing value" or "fill value" as qualitative categories.
             * If a value has both roles, use "missing value" for category name.
             */
            boolean setBackground = true;
            final InternationalString[] names = new InternationalString[2];
            for (final Map.Entry<Number,Integer> entry : data.getNodataValues().entrySet()) {
                final Number n = entry.getKey();
                final double fp = n.doubleValue();
                if (!builder.rangeCollides(fp, fp)) {
                    final int role = entry.getValue();          // Bit 0 set (value 1) = pad value, bit 1 set = missing value.
                    final int i = (role == 1) ? 1 : 0;          // i=1 if role is only pad value, i=0 otherwise.
                    InternationalString name = names[i];
                    if (name == null) {
                        name = Vocabulary.formatInternational(i == 0 ? Vocabulary.Keys.MissingValue : Vocabulary.Keys.FillValue);
                        names[i] = name;
                    }
                    if (setBackground & (role & 1) != 0) {
                        setBackground = false;                  // Declare only one fill value.
                        builder.setBackground(name, n);
                    } else {
                        builder.addQualitative(name, n, n);
                    }
                }
            }
            definition = builder.build();
        }
        return Collections.singletonList(definition);
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based index of sample dimensions to read, or an empty sequence for reading all ranges.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(GridGeometry domain, final int... range) throws DataStoreException {
        domain = validateReadArgument(domain);
        final GridExtent extent = domain.getExtent();
        final int   dimension   = domain.getDimension();
        final int[] areaLower   = new int[dimension];
        final int[] areaUpper   = new int[dimension];
        final int[] subsampling = new int[dimension];
        for (int i=0; i<dimension; i++) {
            final int j = (dimension - 1) - i;
            areaLower[j] = unsigned(extent.getLow (i));             // Inclusive.
            areaUpper[j] = unsigned(extent.getHigh(i) + 1);         // Exclusive.
            subsampling[j] = 1;
        }
        final Vector samples;
        try {
            samples = data.read(areaLower, areaUpper, subsampling);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        // Optional.orElseThrow() below should never fail since Variable.read(…) wraps primitive array.
        return new Image(domain, getSampleDimensions(), data.getDataType().toJava2D(samples.buffer().get()));
    }

    /**
     * Returns the given value as an unsigned integer.
     */
    private static int unsigned(final long value) throws DataStoreException {
        if (value < 0L || value > 0xFFFFFFFFL) {
            throw new DataStoreException(Errors.format(Errors.Keys.IndexOutOfBounds_1, value));
        }
        return (int) value;
    }

    /**
     * Gets the paths to files used by this resource, or an empty array if unknown.
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        return (location != null) ? new Path[] {location} : new Path[0];
    }
}
