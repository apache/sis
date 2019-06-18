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
import java.util.HashMap;
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
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.internal.coverage.RasterFactory;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.storage.netcdf.AttributeNames;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.Resource;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.Vector;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.jdk9.JDK9;


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
     * The identifier of this grid resource. This is {@link Variable#getStandardName()}. We prefer netCDF standard name instead
     * than variable name because the former is controlled vocabulary. The use of controlled vocabulary for identifiers increases
     * the chances of stability or consistency between similar products.
     *
     * <p>The value set by constructor may be updated by {@link #resolveNameCollision(RasterResource, Decoder)},
     * but should not be modified after that point.</p>
     *
     * @see #getIdentifier()
     */
    private GenericName identifier;

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
     * The netCDF variables for each sample dimensions. The length of this array shall be equal to {@code ranges.length},
     * except if bands are stored as one variable dimension ({@link #bandDimension} ≧ 0) in which case the length shall
     * be exactly 1. Accesses to this array need to take in account that the length may be only 1. Example:
     *
     * {@preformat java
     *     Variable v = data[bandDimension >= 0 ? 0 : index];
     * }
     */
    private final Variable[] data;

    /**
     * If one of {@link #data} dimension provides values for different bands, that dimension index. Otherwise -1.
     * This is an index in a list of dimensions in "natural" order (reverse of netCDF order).
     * There is three ways to read the data, determined by this {@code bandDimension} value:
     *
     * <ul>
     *   <li>{@code (bandDimension < 0)}: one variable per band (usual case).</li>
     *   <li>{@code (bandDimension = 0)}: one variable containing all bands, with bands in the first dimension.</li>
     *   <li>{@code (bandDimension > 0)}: one variable containing all bands, with bands in the last dimension.</li>
     * </ul>
     *
     * @see Variable#bandDimension
     */
    private final int bandDimension;

    /**
     * Path to the netCDF file for information purpose, or {@code null} if unknown.
     *
     * @see #getComponentFiles()
     */
    private final Path location;

    /**
     * The object to use for synchronization. For now we use a {@code synchronized} statement,
     * but it may be changed to {@link java.util.concurrent.locks.Lock} in a future version.
     */
    private final Object lock;

    /**
     * Creates a new resource. All variables in the {@code data} list shall have the same domain and the same grid geometry.
     *
     * @param  decoder   the implementation used for decoding the netCDF file.
     * @param  name      the name for the resource.
     * @param  grid      the grid geometry (size, CRS…) of the {@linkplain #data} cube.
     * @param  bands     the variables providing actual data. Shall contain at least one variable.
     * @param  numBands  the number of bands. Shall be {@code bands.size()} except if {@code bandsDimension} ≧ 0.
     * @param  bandDim   if one of {@link #data} dimension provides values for different bands, that dimension index. Otherwise -1.
     * @param  lock      the lock to use in {@code synchronized(lock)} statements.
     */
    private RasterResource(final Decoder decoder, final String name, final GridGeometry grid, final List<Variable> bands,
            final int numBands, final int bandDim, final Object lock)
    {
        super(decoder.listeners);
        data          = bands.toArray(new Variable[bands.size()]);
        ranges        = new SampleDimension[numBands];
        identifier    = decoder.nameFactory.createLocalName(decoder.namespace, name);
        location      = decoder.location;
        gridGeometry  = grid;
        bandDimension = bandDim;
        this.lock     = lock;
        assert data.length == (bandDimension >= 0 ? 1 : ranges.length);
    }

    /**
     * Creates all grid coverage resources from the given decoder.
     * This method shall be invoked in a method synchronized on {@link #lock}.
     *
     * @param  decoder  the implementation used for decoding the netCDF file.
     * @param  lock     the lock to use in {@code synchronized(lock)} statements.
     * @return all grid coverage resources.
     * @throws IOException if an I/O operation was required and failed.
     * @throws DataStoreException if a logical error occurred.
     */
    public static List<Resource> create(final Decoder decoder, final Object lock) throws IOException, DataStoreException {
        assert Thread.holdsLock(lock);
        final Variable[]     variables = decoder.getVariables().clone();        // Needs a clone because may be modified.
        final List<Variable> siblings  = new ArrayList<>(4);                    // Usually has only 1 element, sometime 2.
        final List<Resource> resources = new ArrayList<>(variables.length);     // The raster resources to be returned.
        final Map<GenericName,RasterResource> firstOfName = new HashMap<>();    // For detecting name collisions.
        for (int i=0; i<variables.length; i++) {
            final Variable variable = variables[i];
            if (variable == null || variable.getRole() != VariableRole.COVERAGE) {
                continue;                                                   // Skip variables that are not grid coverages.
            }
            final GridGeometry grid = variable.getGridGeometry();
            if (grid == null) {
                continue;                                                   // Skip variables that are not grid coverages.
            }
            siblings.add(variable);                                         // Variable will the first band of raster.
            String name = variable.getStandardName();
            /*
             * At this point we found a variable for which to create a resource. Most of the time, there is nothing else to do;
             * the resource will have a single variable and the same name than that unique variable. The resulting raster will
             * have only one band (sample dimension). However in some cases the raster should have more than one band:
             *
             *   1) if the variable has an extra dimension compared to the grid geometry;
             *   2) of if two or more variables should be grouped together.
             *
             * The following  if {…} else {…}  blocks implement those two cases.
             */
            final List<Dimension> gridDimensions = variable.getGridDimensions();
            final int dataDimension = gridDimensions.size();
            final int gridDimension = grid.getDimension();
            final int bandDimension, numBands;
            if (dataDimension != gridDimension) {
                bandDimension = variable.bandDimension;                            // One variable dimension is interpreted as bands.
                Dimension dim = gridDimensions.get(dataDimension - 1 - bandDimension);  // Note: "natural" → netCDF index conversion.
                numBands = Math.toIntExact(dim.length());
                if (dataDimension != gridDimension + 1 || (bandDimension > 0 && bandDimension != gridDimension)) {
                    /*
                     * One of the following restrictions it not met for the requested data:
                     *
                     *   - Only 1 dimension can be used for bands. Variables with 2 or more band dimensions are not supported.
                     *   - The dimension for bands shall be either the first or the last dimension; it can not be in the middle.
                     */
                    throw new DataStoreContentException(Resources.forLocale(decoder.listeners.getLocale())
                            .getString(Resources.Keys.UnmappedDimensions_4, name, decoder.getFilename(), dataDimension, gridDimension));
                }
            } else {
                /*
                 * At this point we found a variable where all dimensions are in the CRS. This is the usual case;
                 * there is no band explicitly declared in the netCDF file. However in some cases, we should put
                 * other variables together with the one we just found. Example:
                 *
                 *    1) baroclinic_eastward_sea_water_velocity
                 *    2) baroclinic_northward_sea_water_velocity
                 *
                 * We use the "eastward" and "northward" keywords for recognizing such pairs, providing that everything else in the
                 * name is the same and the grid geometries are the same.
                 */
                bandDimension = -1;                                                 // No dimension to be interpreted as bands.
                final DataType type = variable.getDataType();
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
                numBands = siblings.size();
            }
            final RasterResource r = new RasterResource(decoder, name.trim(), grid, siblings, numBands, bandDimension, lock);
            r.resolveNameCollision(firstOfName.putIfAbsent(r.identifier, r), decoder);
            resources.add(r);
            siblings.clear();
        }
        return resources;
    }

    /**
     * If the given resource is non-null, modifies the name of this resource for avoiding name collision.
     * The {@code other} resource shall be non-null when the caller detected that there is a name collision
     * with that resource.
     *
     * @param  other  the other resource for which there is a name collision, or {@code null} if no collision.
     */
    private void resolveNameCollision(final RasterResource other, final Decoder decoder) {
        if (other != null) {
            if (identifier.equals(other.identifier)) {
                other.resolveNameCollision(decoder);
            }
            resolveNameCollision(decoder);
        }
    }

    /**
     * Invoked when the name of this resource needs to be changed because it collides with the name of another resource.
     * This method appends the variable name, which should be unique in each netCDF file.
     */
    private void resolveNameCollision(final Decoder decoder) {
        String name = identifier + " (" + data[0].getName() + ')';
        identifier = decoder.nameFactory.createLocalName(decoder.namespace, name);
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
     * Returns the variable at the given index. This method can be invoked when the caller has not verified
     * if we are in the special case where all bands are in the same variable ({@link #bandDimension} ≧ 0).
     */
    private Variable getVariable(final int i) {
        return data[bandDimension >= 0 ? 0 : i];
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
            synchronized (lock) {
                for (int i=0; i<ranges.length; i++) {
                    if (ranges[i] == null) {
                        if (builder == null) builder = new SampleDimension.Builder();
                        ranges[i] = createSampleDimension(builder, getVariable(i), i);
                        builder.clear();
                    }
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
     * @param  index    index in the variable dimension identified by {@link #bandDimension}.
     * @throws TransformException if an error occurred while using the transfer function.
     */
    private SampleDimension createSampleDimension(final SampleDimension.Builder builder, final Variable band, final int index)
            throws TransformException
    {
        /*
         * Take the minimum and maximum values as determined by Apache SIS through the Convention class.  The UCAR library
         * is used only as a fallback. We give precedence to the range computed by Apache SIS instead than the range given
         * by UCAR because we need the range of packed values instead than the range of converted values.
         */
        NumberRange<?> range;
        if (!createEnumeration(builder, band, index) && (range = band.getValidRange()) != null) {
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
                if (band.getDataType().number <= Numbers.LONG && minimum >= Long.MIN_VALUE && maximum <= Long.MAX_VALUE) {
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
        /*
         * At this point we have the list of all categories to put in the sample dimension.
         * Now create the sample dimension using the variable short name as dimension name.
         * The index is appended to the name only if bands are all in the same variable.
         */
        String name = band.getName();
        if (bandDimension >= 0) {
            name = Strings.toIndexed(name, index);
        }
        return builder.setName(name).build();
    }

    /**
     * Appends qualitative categories in the given builder for {@code "flag_values"} or {@code "flag_masks"} attribute.
     *
     * @param  builder  the builder to use for creating the sample dimension.
     * @param  band     the data for which to create a sample dimension.
     * @param  index    index in the variable dimension identified by {@link #bandDimension}.
     * @return {@code true} if flag attributes have been found, or {@code false} otherwise.
     */
    private static boolean createEnumeration(final SampleDimension.Builder builder, final Variable band, final int index) {
        CharSequence[] names = band.getAttributeAsStrings(AttributeNames.FLAG_NAMES, ' ');
        if (names == null) {
            names = band.getAttributeAsStrings(AttributeNames.FLAG_MEANINGS, ' ');
            if (names == null) return false;
        }
        Vector values = band.getAttributeAsVector(AttributeNames.FLAG_VALUES);
        if (values == null) {
            values = band.getAttributeAsVector(AttributeNames.FLAG_MASKS);
            if (values == null) return false;
        }
        final int length = values.size();
        for (int i=0; i<length; i++) {
            final Number value = values.get(i);
            final CharSequence name;
            if (i < names.length) {
                name = names[i];
            } else {
                name = Vocabulary.formatInternational(Vocabulary.Keys.Unnamed);
            }
            builder.addQualitative(name, value, value);
        }
        return true;
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
    public GridCoverage read(GridGeometry domain, final int... range) throws DataStoreException {
        final RangeArgument rangeIndices = validateRangeArgument(ranges.length, range);
        if (domain == null) {
            domain = gridGeometry;
        }
        final Variable first = data[bandDimension >= 0 ? 0 : rangeIndices.getFirstSpecified()];
        final DataType dataType = first.getDataType();
        if (bandDimension < 0) {
            for (int i=0; i<rangeIndices.getNumBands(); i++) {
                final Variable variable = data[rangeIndices.getSourceIndex(i)];
                if (!dataType.equals(variable.getDataType())) {
                    throw new DataStoreContentException(Resources.forLocale(getLocale()).getString(
                            Resources.Keys.MismatchedVariableType_3, getFilename(), first.getName(), variable.getName()));
                }
            }
        }
        /*
         * At this point the arguments and the state of this resource have been validated.
         * There is three ways to read the data, determined by `bandDimension` value:
         *
         *   • (bandDimension < 0): one variable per band (usual case).
         *   • (bandDimension = 0): one variable containing all bands, with bands in the first dimension.
         *   • (bandDimension > 0): one variable containing all bands, with bands in the last dimension.
         */
        final DataBuffer imageBuffer;
        final SampleDimension[] bands = new SampleDimension[rangeIndices.getNumBands()];
        int[] bandOffsets = null;                                                   // By default, all bands start at index 0.
        try {
            GridDerivation targetGeometry = gridGeometry.derive()
                    .rounding(GridRoundingMode.ENCLOSING)
                    .subgrid(domain);
            GridExtent     areaOfInterest = targetGeometry.getIntersection();       // Pixel indices of data to read.
            int[]          subsamplings   = targetGeometry.getSubsamplings();       // Slice to read or subsampling to apply.
            int            numBuffers     = bands.length;                           // By default, one variable per band.
            domain = targetGeometry.subsample(subsamplings).build();                // Adjust user-specified domain to data geometry.
            if (bandDimension >= 0) {
                areaOfInterest = rangeIndices.insertBandDimension(areaOfInterest, bandDimension);
                subsamplings   = rangeIndices.insertSubsampling  (subsamplings,   bandDimension);
                if (bandDimension == 0) {
                    bandOffsets = new int[numBuffers];          // Will be set to non-zero values later.
                }
                numBuffers = 1;                                 // One variable for all bands.
            }
            /*
             * Iterate over netCDF variables in the order they appear in the file, not in the order requested
             * by the 'range' argument.  The intent is to perform sequential I/O as much as possible, without
             * seeking backward. In the (uncommon) case where bands are one of the variable dimension instead
             * than different variables, the reading of the whole variable occurs during the first iteration.
             */
            Buffer[] sampleValues = new Buffer[numBuffers];
            synchronized (lock) {
                for (int i=0; i<bands.length; i++) {
                    int indexInResource = rangeIndices.getSourceIndex(i);     // In strictly increasing order.
                    int indexInRaster   = rangeIndices.getTargetIndex(i);
                    Variable variable   = getVariable(indexInResource);
                    SampleDimension b   = ranges[indexInResource];
                    if (b == null) {
                        ranges[indexInResource] = b = createSampleDimension(rangeIndices.builder(), variable, i);
                    }
                    bands[indexInRaster] = b;
                    if (bandOffsets != null) {
                        bandOffsets[indexInRaster] = i;
                        indexInRaster = 0;                  // Pixels interleaved in one bank: sampleValues.length = 1.
                    }
                    if (i < numBuffers) {
                        // Optional.orElseThrow() below should never fail since Variable.read(…) wraps primitive array.
                        sampleValues[indexInRaster] = variable.read(areaOfInterest, subsamplings).buffer().get();
                    }
                }
            }
            /*
             * The following block is executed only if all bands are in a single variable, and the bands dimension is
             * the last one (in "natural" order). In such case, the sample model to construct is a BandedSampleModel.
             * Contrarily to PixelInterleavedSampleModel (the case when the band dimension is first), banded sample
             * model force us to split the buffer in a buffer for each band.
             */
            if (bandDimension > 0) {                // Really > 0, not >= 0.
                final int stride = Math.toIntExact(data[0].getBandStride());
                Buffer values = sampleValues[0].limit(stride);
                sampleValues = new Buffer[bands.length];
                for (int i=0; i<sampleValues.length; i++) {
                    if (i != 0) {
                        values = JDK9.duplicate(values);
                        final int p = values.limit();
                        values.position(p).limit(Math.addExact(p, stride));
                    }
                    sampleValues[i] = values;
                }
            }
            /*
             * Convert NIO Buffer into Java2D DataBuffer. May throw various RuntimeException.
             */
            imageBuffer = RasterFactory.wrap(dataType.rasterDataType, sampleValues);
        } catch (IOException e) {
            throw new DataStoreException(e);
        } catch (TransformException e) {
            throw new DataStoreReferencingException(e);
        } catch (RuntimeException e) {                          // Many exceptions thrown by RasterFactory.wrap(…).
            final Throwable cause = e.getCause();
            if (cause instanceof TransformException) {
                throw new DataStoreReferencingException(cause);
            } else {
                throw new DataStoreContentException(e);
            }
        }
        if (imageBuffer == null) {
            throw new DataStoreContentException(Errors.getResources(getLocale()).getString(Errors.Keys.UnsupportedType_1, dataType.name()));
        }
        return new Raster(domain, UnmodifiableArrayList.wrap(bands), imageBuffer,
                rangeIndices.getPixelStride(), bandOffsets, String.valueOf(identifier));
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
