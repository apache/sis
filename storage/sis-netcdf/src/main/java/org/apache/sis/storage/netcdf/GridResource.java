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
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.Buffer;
import java.awt.image.DataBuffer;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Grid;
import org.apache.sis.internal.netcdf.DataType;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.Resources;
import org.apache.sis.internal.storage.AbstractGridResource;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridChange;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.internal.raster.RasterFactory;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.Resource;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import ucar.nc2.constants.CDM;                      // We use only String constants.


/**
 * A grid coverage in a netCDF file. We create one resource for each variable,
 * unless we determine that two variables should be handled together (for example
 * the <var>u</var> and <var>v</var> components of wind vectors).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class GridResource extends AbstractGridResource implements ResourceOnFileSystem {
    /**
     * Words used in standard (preferred) or long (if no standard) variable names which suggest
     * that the variable is a component of a vector. Example of standard variable names:
     *
     * <ul>
     *   <li>{@code baroclinic_eastward_sea_water_velocity}</li>
     *   <li>{@code baroclinic_northward_sea_water_velocity}</li>
     *   <li>{@code eastward_atmosphere_water_transport_across_unit_distance}</li>
     *   <li><i>etc.</i></li>
     * </ul>
     *
     * One element to note is that direction (e.g. "eastward") is not necessarily at the beginning
     * of variable name.
     *
     * @see <a href="http://cfconventions.org/Data/cf-standard-names/current/build/cf-standard-name-table.html">Standard name table</a>
     */
    private static final String[] VECTOR_COMPONENT_NAMES = {
        "eastward", "westward", "northward", "southward", "upward", "downward"
    };

    /**
     * The identifier of this grid resource. This is the variable name.
     *
     * @see #getIdentifier()
     */
    private final GenericName identifier;

    /**
     * The grid geometry (size, CRS…) of the {@linkplain #data} cube.
     * This defines the "domain" in "coverage function" terminology.
     *
     * @see #getGridGeometry()
     */
    private final GridGeometry gridGeometry;

    /**
     * The sample dimension for the {@link #data} variables.
     * This defines the "range" in "coverage function" terminology.
     * All elements are initially {@code null} and created when first needed.
     *
     * @see #getSampleDimensions()
     */
    private final SampleDimension[] ranges;

    /**
     * The netCDF variable wrapped by this resource.
     */
    private final Variable[] data;

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
     * @param  name     the name for the resource.
     * @param  grid     the grid geometry (size, CRS…) of the {@linkplain #data} cube.
     * @param  data     the variables providing actual data. Shall contain at least one variable.
     */
    private GridResource(final Decoder decoder, final String name, final Grid grid, final List<Variable> data)
            throws IOException, DataStoreException
    {
        super(decoder.listeners);
        this.data    = data.toArray(new Variable[data.size()]);
        ranges       = new SampleDimension[this.data.length];
        gridGeometry = grid.getGridGeometry(decoder);
        identifier   = decoder.nameFactory.createLocalName(decoder.namespace, name);
        location     = decoder.location;
    }

    /**
     * Creates all grid resources from the given decoder.
     *
     * @param  decoder  the implementation used for decoding the netCDF file.
     */
    static List<Resource> create(final Decoder decoder) throws IOException, DataStoreException {
        final Variable[]     variables = decoder.getVariables().clone();        // Needs a clone because may be modified.
        final List<Variable> siblings  = new ArrayList<>(4);
        final List<Resource> resources = new ArrayList<>();
        for (int i=0; i<variables.length; i++) {
            final Variable variable = variables[i];
            final Grid grid;
            if (variable == null || !variable.isCoverage() || (grid = variable.getGridGeometry(decoder)) == null) {
                continue;                                                   // Skip variables that are not grid coverages.
            }
            siblings.add(variable);
            String name = variable.getStandardName();
            final DataType type = variable.getDataType();
            /*
             * At this point we found a variable for which to create a resource. Most of the time, there is nothing else to do;
             * the resource will have a single variable and the same name than that unique variable. However in some cases, the
             * we should put other variables together with the one we just found. Example:
             *
             *    1) baroclinic_eastward_sea_water_velocity
             *    2) baroclinic_northward_sea_water_velocity
             *
             * We use the "eastward" and "northward" keywords for recognizing such pairs, providing that everything else in the
             * name is the same and the grid geometry are the same.
             */
            for (final String keyword : VECTOR_COMPONENT_NAMES) {
                final int prefixLength = name.indexOf(keyword);
                if (prefixLength >= 0) {
                    int suffixStart  = prefixLength + keyword.length();
                    int suffixLength = name.length() - suffixStart;
                    for (int j=i; ++j < variables.length;) {
                        final Variable candidate = variables[j];
                        if (candidate == null || !candidate.isCoverage()) {
                            variables[j] = null;                                // For avoiding to revisit that variable again.
                            continue;
                        }
                        final String cn = candidate.getStandardName();
                        if (cn.regionMatches(cn.length() - suffixLength, name, suffixStart, suffixLength) &&
                            cn.regionMatches(0, name, 0, prefixLength) && candidate.getDataType() == type &&
                            candidate.getGridGeometry(decoder) == grid)
                        {
                            /*
                             * Found another variable with the same name except for the keyword. Verify that the
                             * keyword is replaced by another word in the vector component keyword list. If this
                             * is the case, then we consider that those two variables should be kept together.
                             */
                            for (final String k : VECTOR_COMPONENT_NAMES) {
                                if (cn.regionMatches(prefixLength, k, 0, k.length())) {
                                    siblings.add(candidate);
                                    variables[j] = null;
                                    break;
                                }
                            }
                        }
                    }
                    /*
                     * If we have more than one variable, omit the keyword from the name. For example instead
                     * of "baroclinic_eastward_sea_water_velocity", construct "baroclinic_sea_water_velocity".
                     * Note that we may need to remove duplicated '_' character after keyword removal.
                     */
                    if (siblings.size() > 1) {
                        if (suffixLength != 0) {
                            final int c = name.codePointAt(suffixStart);
                            if ((prefixLength != 0) ? (c == name.codePointBefore(prefixLength)) : (c == '_')) {
                                suffixStart += Character.charCount(c);
                            }
                        }
                        name = new StringBuilder(name).delete(prefixLength, suffixStart).toString();
                    }
                }
            }
            resources.add(new GridResource(decoder, name.trim(), grid, siblings));
            siblings.clear();
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
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<SampleDimension> getSampleDimensions() {
        SampleDimension.Builder builder = null;
        for (int i=0; i<ranges.length; i++) {
            if (ranges[i] == null) {
                if (builder == null) builder = new SampleDimension.Builder();
                ranges[i] = createSampleDimension(builder, data[i]);
                builder.clear();
            }
        }
        return UnmodifiableArrayList.wrap(ranges);
    }

    /**
     * Creates a single sample dimension for the given variable.
     *
     * @param  builder  the builder to use for creating the sample dimension.
     * @param  data     the data for which to create a sample dimension.
     */
    private static SampleDimension createSampleDimension(final SampleDimension.Builder builder, final Variable data) {
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
         * Adds the "missing value" or "fill value" as qualitative categories.  If a value has both roles, use "missing value"
         * as category name. If the sample values are already real values, then the "no data" values have been replaced by NaN
         * values by Variable.replaceNaN(Object). The qualitative categories constructed below must be consistent with the NaN
         * values created by 'replaceNaN'.
         */
        boolean setBackground = true;
        int ordinal = data.hasRealValues() ? 0 : -1;
        final InternationalString[] names = new InternationalString[2];
        for (final Map.Entry<Number,Integer> entry : data.getNodataValues().entrySet()) {
            final Number n;
            if (ordinal >= 0) {
                n = MathFunctions.toNanFloat(ordinal++);        // Must be consistent with Variable.replaceNaN(Object).
            } else {
                n = entry.getKey();
                final double fp = n.doubleValue();
                if (builder.rangeCollides(fp, fp)) {
                    continue;
                }
            }
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
        return builder.build();
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
        range = validateRangeArgument(data.length, range);
        if (domain == null) {
            domain = gridGeometry;
        }
        final Variable first = data[range[0]];
        final DataType dataType = first.getDataType();
        for (int i=1; i<data.length; i++) {
            final Variable variable = data[range[i]];
            if (!dataType.equals(variable.getDataType())) {
                throw new DataStoreContentException(Resources.forLocale(getLocale()).getString(
                        Resources.Keys.MismatchedVariableType_3, getFilename(), first.getName(), variable.getName()));
            }
        }
        final DataBuffer imageBuffer;
        final SampleDimension[] selected = new SampleDimension[range.length];
        try {
            final Buffer[]   samples = new Buffer[range.length];
            final GridChange change  = new GridChange(domain, gridGeometry);
            final int[] subsamplings = change.getTargetSubsamplings();
            SampleDimension.Builder builder = null;
            /*
             * Iterate over netCDF variables in the order they appear in the file, not in the order requested
             * by the 'range' argument.  The intent is to perform sequential I/O as much as possible, without
             * seeking backward.
             */
            for (int i=0; i<data.length; i++) {
                final Variable variable = data[i];
                SampleDimension def = ranges[i];
                Buffer values = null;
                for (int j=0; j<range.length; j++) {
                    /*
                     * Check if the current variable is a sample dimension specified in the 'range' argument.
                     * Note that the same sample dimension may be requested an arbitrary amount of time, but
                     * the data will be loaded at most once.
                     */
                    if (range[j] == i) {
                        if (def == null) {
                            if (builder == null) builder = new SampleDimension.Builder();
                            ranges[i] = def = createSampleDimension(builder, variable);
                        }
                        if (values == null) {
                            // Optional.orElseThrow() below should never fail since Variable.read(…) wraps primitive array.
                            values = variable.read(change.getTargetExtent(), subsamplings).buffer().get();
                        }
                        selected[j] = def;
                        samples[j] = values;
                    }
                }
            }
            domain = change.getTargetGeometry(subsamplings);
            imageBuffer = RasterFactory.wrap(dataType.rasterDataType, samples);
        } catch (TransformException e) {
            throw new DataStoreReferencingException(e);
        } catch (IOException e) {
            throw new DataStoreException(e);
        } catch (RuntimeException e) {                  // Many exceptions thrown by RasterFactory.wrap(…).
            throw new DataStoreContentException(e);
        }
        if (imageBuffer == null) {
            throw new DataStoreContentException(Errors.format(Errors.Keys.UnsupportedType_1, dataType.name()));
        }
        return new Image(domain, UnmodifiableArrayList.wrap(selected), imageBuffer);
    }

    /**
     * Returns the name of the netCDF file. This is used for error messages.
     */
    private String getFilename() {
        if (location != null) {
            return location.getFileName().toString();
        } else {
            return Vocabulary.getResources(getLocale()).getString(Vocabulary.Keys.Unnamed);
        }
    }

    /**
     * Gets the paths to files used by this resource, or an empty array if unknown.
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        return (location != null) ? new Path[] {location} : new Path[0];
    }
}
