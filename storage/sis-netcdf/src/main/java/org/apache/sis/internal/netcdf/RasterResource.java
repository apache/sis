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
package org.apache.sis.internal.netcdf;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.Buffer;
import java.awt.image.DataBuffer;
import org.opengis.util.GenericName;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.storage.AbstractGridResource;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.internal.raster.RasterFactory;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.Resource;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * A grid coverage in a netCDF file. We create one resource for each variable,
 * unless we determine that two variables should be handled together (for example
 * the <var>u</var> and <var>v</var> components of wind vectors).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class RasterResource extends AbstractGridResource implements ResourceOnFileSystem {
    /**
     * Words used in standard (preferred) or long (if no standard) variable names which suggest
     * that the variable is a component of a vector. Those words are used in heuristic rules
     * for deciding if two variables should be stored in a single {@code Coverage} instance.
     * For example the eastward (u) and northward (v) components of oceanic current vectors
     * should be stored as two sample dimensions of a single "Current" coverage.
     * Example of standard variable names:
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
     * The same variable may be repeated if one of its dimension shall be interpreted as bands.
     */
    private final Variable[] data;

    /**
     * If one of {@link #data} dimension provides values for different bands, that dimension index.
     * Otherwise -1.
     */
    private final int bandDimension;

    /**
     * Path to the netCDF file for information purpose, or {@code null} if unknown.
     *
     * @see #getComponentFiles()
     */
    private final Path location;

    /**
     * Creates a new resource. All variables in the {@code data} list shall have the same domain and the same grid geometry.
     *
     * @param  decoder  the implementation used for decoding the netCDF file.
     * @param  name     the name for the resource.
     * @param  grid     the grid geometry (size, CRS…) of the {@linkplain #data} cube.
     * @param  bands    the variables providing actual data. Shall contain at least one variable.
     */
    private RasterResource(final Decoder decoder, final String name, final GridGeometry grid, final List<Variable> bands)
            throws IOException, DataStoreException
    {
        super(decoder.listeners);
        data         = bands.toArray(new Variable[bands.size()]);
        ranges       = new SampleDimension[data.length];
        identifier   = decoder.nameFactory.createLocalName(decoder.namespace, name);
        location     = decoder.location;
        gridGeometry = grid;
        bandDimension = -1;
    }

    /**
     * Creates all grid coverage resources from the given decoder.
     *
     * @param  decoder  the implementation used for decoding the netCDF file.
     * @return all grid coverage resources.
     * @throws IOException if an I/O operation was required and failed.
     * @throws DataStoreException if a logical error occurred.
     */
    public static List<Resource> create(final Decoder decoder) throws IOException, DataStoreException {
        final Variable[]     variables = decoder.getVariables().clone();        // Needs a clone because may be modified.
        final List<Variable> siblings  = new ArrayList<>(4);
        final List<Resource> resources = new ArrayList<>();
        for (int i=0; i<variables.length; i++) {
            final Variable variable = variables[i];
            if (variable == null || variable.getRole() != VariableRole.COVERAGE) {
                continue;                                                   // Skip variables that are not grid coverages.
            }
            final GridGeometry grid = variable.getGridGeometry();
            if (grid == null) {
                continue;                                                   // Skip variables that are not grid coverages.
            }
            siblings.add(variable);
            String name = variable.getStandardName();
            final DataType type = variable.getDataType();
            /*
             * At this point we found a variable for which to create a resource. Most of the time, there is nothing else to do;
             * the resource will have a single variable and the same name than that unique variable.  However in some cases, we
             * should put other variables together with the one we just found. Example:
             *
             *    1) baroclinic_eastward_sea_water_velocity
             *    2) baroclinic_northward_sea_water_velocity
             *
             * We use the "eastward" and "northward" keywords for recognizing such pairs, providing that everything else in the
             * name is the same and the grid geometries are the same.
             */
            for (final String keyword : VECTOR_COMPONENT_NAMES) {
                final int prefixLength = name.indexOf(keyword);
                if (prefixLength >= 0) {
                    int suffixStart  = prefixLength + keyword.length();
                    int suffixLength = name.length() - suffixStart;
                    for (int j=i; ++j < variables.length;) {
                        final Variable candidate = variables[j];
                        if (candidate == null || candidate.getRole() != VariableRole.COVERAGE) {
                            variables[j] = null;                                // For avoiding to revisit that variable again.
                            continue;
                        }
                        final String cn = candidate.getStandardName();
                        if (cn.regionMatches(cn.length() - suffixLength, name, suffixStart, suffixLength) &&
                            cn.regionMatches(0, name, 0, prefixLength) && candidate.getDataType() == type &&
                            grid.equals(candidate.getGridGeometry()))
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
            resources.add(new RasterResource(decoder, name.trim(), grid, siblings));
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
     *
     * @return extent of grid coordinates together with their mapping to "real world" coordinates.
     */
    @Override
    public GridGeometry getGridGeometry() {
        return gridGeometry;
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     *
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        SampleDimension.Builder builder = null;
        try {
            for (int i=0; i<ranges.length; i++) {
                if (ranges[i] == null) {
                    if (builder == null) builder = new SampleDimension.Builder();
                    ranges[i] = createSampleDimension(builder, data[i]);
                    builder.clear();
                }
            }
        } catch (TransformException e) {
            throw new DataStoreReferencingException(e);
        }
        return UnmodifiableArrayList.wrap(ranges);
    }

    /**
     * Creates a single sample dimension for the given variable.
     *
     * @param  builder  the builder to use for creating the sample dimension.
     * @param  band     the data for which to create a sample dimension.
     * @throws TransformException if an error occurred while using the transfer function.
     */
    private SampleDimension createSampleDimension(final SampleDimension.Builder builder, final Variable band) throws TransformException {
        /*
         * Take the minimum and maximum values as determined by Apache SIS through the Convention class.  The UCAR library
         * is used only as a fallback. We give precedence to the range computed by Apache SIS instead than the range given
         * by UCAR because we need the range of packed values instead than the range of converted values.
         */
        NumberRange<?> range = band.getValidRange();
        if (range != null) {
            final MathTransform1D mt = band.getTransferFunction().getTransform();
            if (!mt.isIdentity() && range instanceof MeasurementRange<?>) {
                /*
                 * Heuristic rule defined in UCAR documentation (see EnhanceScaleMissing interface):
                 * if the type of the range is equal to the type of the scale, and the type of the
                 * data is not wider, then assume that the minimum and maximum are real values.
                 * This is identified in Apache SIS by the range given as a MeasurementRange.
                 */
                final MathTransform1D inverse = mt.inverse();
                boolean isMinIncluded = range.isMinIncluded();
                boolean isMaxIncluded = range.isMaxIncluded();
                double minimum = inverse.transform(range.getMinDouble());
                double maximum = inverse.transform(range.getMaxDouble());
                if (maximum < minimum) {
                    final double swap = maximum;
                    maximum = minimum;
                    minimum = swap;
                    final boolean sb = isMaxIncluded;
                    isMaxIncluded = isMinIncluded;
                    isMinIncluded = sb;
                }
                if (band.getDataType().number < Numbers.FLOAT && minimum >= Long.MIN_VALUE && maximum <= Long.MAX_VALUE) {
                    range = NumberRange.create(Math.round(minimum), isMinIncluded, Math.round(maximum), isMaxIncluded);
                } else {
                    range = NumberRange.create(minimum, isMinIncluded, maximum, isMaxIncluded);
                }
            }
            /*
             * Range may be empty if min/max values declared in the netCDF files are erroneous,
             * or if we have not read them correctly (edu.ucar:cdm:4.6.13 sometime confuses an
             * unsigned integer with a signed one).
             */
            if (range.isEmpty()) {
                band.warning(RasterResource.class, "getSampleDimensions", Resources.Keys.IllegalValueRange_4,
                        band.getFilename(), band.getName(), range.getMinValue(), range.getMaxValue());
            } else {
                String name = band.getDescription();
                if (name == null) name = band.getName();
                builder.addQuantitative(name, range, mt, band.getUnit());
            }
        }
        /*
         * Adds the "missing value" or "fill value" as qualitative categories.  If a value has both roles, use "missing value"
         * as category name. If the sample values are already real values, then the "no data" values have been replaced by NaN
         * values by Variable.replaceNaN(Object). The qualitative categories constructed below must be consistent with the NaN
         * values created by 'replaceNaN'.
         */
        boolean setBackground = true;
        int ordinal = band.hasRealValues() ? 0 : -1;
        final CharSequence[] names = new CharSequence[2];
        for (final Map.Entry<Number,Object> entry : band.getNodataValues().entrySet()) {
            final Number n;
            if (ordinal >= 0) {
                n = MathFunctions.toNanFloat(ordinal++);        // Must be consistent with Variable.replaceNaN(Object).
            } else {
                n = entry.getKey();                             // Should be real number, made unique by the HashMap.
            }
            CharSequence name;
            final Object label = entry.getValue();
            if (label instanceof Integer) {
                final int role = (Integer) label;               // Bit 0 set (value 1) = pad value, bit 1 set = missing value.
                final int i = (role == 1) ? 1 : 0;              // i=1 if role is only pad value, i=0 otherwise.
                name = names[i];
                if (name == null) {
                    name = Vocabulary.formatInternational(i == 0 ? Vocabulary.Keys.MissingValue : Vocabulary.Keys.FillValue);
                    names[i] = name;
                }
                if (setBackground & (role & 1) != 0) {
                    setBackground = false;                      // Declare only one fill value.
                    builder.setBackground(name, n);
                    continue;
                }
            } else {
                name = (CharSequence) label;
            }
            builder.addQualitative(name, n, n);
        }
        return builder.setName(band.getName()).build();
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
        range = validateRangeArgument(ranges.length, range);
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
            final Buffer[] samples = new Buffer[range.length];
            final GridDerivation change = gridGeometry.derive().subgrid(domain);
            final int[] subsamplings = change.getSubsamplings();
            SampleDimension.Builder builder = null;
            /*
             * Iterate over netCDF variables in the order they appear in the file, not in the order requested
             * by the 'range' argument.  The intent is to perform sequential I/O as much as possible, without
             * seeking backward. A side effect is that even if the same sample dimension were requested many
             * times, the data would still be loaded at most once.
             */
            for (int i=0; i<data.length; i++) {
                final Variable variable = data[i];
                SampleDimension def = ranges[i];
                Buffer values = null;
                for (int j=0; j<range.length; j++) {
                    if (range[j] == i) {                // Search sample dimensions specified in the 'range' argument.
                        if (def == null) {
                            if (builder == null) builder = new SampleDimension.Builder();
                            ranges[i] = def = createSampleDimension(builder, variable);
                            builder.clear();
                        }
                        if (values == null) {
                            // Optional.orElseThrow() below should never fail since Variable.read(…) wraps primitive array.
                            values = variable.read(change.getIntersection(), subsamplings).buffer().get();
                        }
                        selected[j] = def;
                        samples[j] = values;
                    }
                }
            }
            domain = change.subsample(subsamplings).build();
            imageBuffer = RasterFactory.wrap(dataType.rasterDataType, samples);
        } catch (IOException e) {
            throw new DataStoreException(e);
        } catch (TransformException e) {
            throw new DataStoreReferencingException(e);
        } catch (RuntimeException e) {                  // Many exceptions thrown by RasterFactory.wrap(…).
            final Throwable cause = e.getCause();
            if (cause instanceof TransformException) {
                throw new DataStoreReferencingException(cause);
            }
            throw new DataStoreContentException(e);
        }
        if (imageBuffer == null) {
            throw new DataStoreContentException(Errors.format(Errors.Keys.UnsupportedType_1, dataType.name()));
        }
        return new Raster(domain, UnmodifiableArrayList.wrap(selected), imageBuffer, first.getStandardName());
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
    public Path[] getComponentFiles() {
        return (location != null) ? new Path[] {location} : new Path[0];
    }
}
