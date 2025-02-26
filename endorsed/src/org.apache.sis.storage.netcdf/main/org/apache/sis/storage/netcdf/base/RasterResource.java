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
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Path;
import java.awt.image.DataBuffer;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.IllegalSampleDimensionException;
import org.apache.sis.coverage.privy.RangeArgument;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.storage.netcdf.internal.Resources;


/**
 * A grid coverage in a netCDF file. We create one resource for each variable,
 * unless we determine that two variables should be handled together (for example
 * the <var>u</var> and <var>v</var> components of wind vectors).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class RasterResource extends AbstractGridCoverageResource implements StoreResource {
    /**
     * Words used in standard (preferred) or long (if no standard) variable names which suggest
     * that the variable is a component of a vector. Those words are used in heuristic rules
     * for deciding if two variables should be stored in a single {@code Coverage} instance.
     * For example, the eastward (u) and northward (v) components of oceanic current vectors
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
     * <p>The value set by constructor may be updated by {@link #resolveNameCollision(Decoder)},
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
    private final SampleDimension[] sampleDimensions;

    /**
     * The netCDF variables for each sample dimensions. The length of this array shall be equal to {@code sampleDimensions.length},
     * except if bands are stored as one variable dimension ({@link #bandDimension} ≥ 0) in which case the length shall be exactly 1.
     * Accesses to this array need to take in account that the length may be only 1. Example:
     *
     * {@snippet lang="java" :
     *     Variable v = data[bandDimension >= 0 ? 0 : index];
     *     }
     */
    private final Variable[] data;

    /**
     * If one of {@link #data} dimension provides values for different bands, that dimension index. Otherwise -1.
     * This is an index in a list of dimensions in "natural" order (reverse of netCDF order).
     * There are three ways to read the data, determined by this {@code bandDimension} value:
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
     * The band to use for defining pixel colors when the image is displayed on screen.
     * All other bands, if any, will exist in the raster but be ignored at display time.
     *
     * @see Convention#getVisibleBand()
     */
    private final int visibleBand;

    /**
     * Path to the netCDF file for information purpose, or {@code null} if unknown.
     *
     * @see #getFileSet()
     */
    private final Path location;

    /**
     * The object to use for synchronization. For now we use a {@code synchronized} statement,
     * but it may be changed to {@link java.util.concurrent.locks.Lock} in a future version.
     * Current lock is the whole netCDF data store (so this field is opportunistically used
     * by {@link #getOriginator()}), but it may change in future version.
     *
     * @see DiscreteSampling#lock
     * @see #getSynchronizationLock()
     */
    private final DataStore lock;

    /**
     * Creates a new resource. All variables in the {@code data} list shall have the same domain and the same grid geometry.
     *
     * @param  decoder   the implementation used for decoding the netCDF file.
     * @param  name      the name for the resource.
     * @param  grid      the grid geometry (size, CRS…) of the {@linkplain #data} cube.
     * @param  bands     the variables providing actual data. Shall contain at least one variable.
     * @param  numBands  the number of bands. Shall be {@code bands.size()} except if {@code bandsDimension} ≥ 0.
     * @param  bandDim   if one of {@link #data} dimension provides values for different bands, that dimension index. Otherwise -1.
     * @param  lock      the lock to use in {@code synchronized(lock)} statements.
     */
    private RasterResource(final Decoder decoder, final String name, final GridGeometry grid, final List<Variable> bands,
            final int numBands, final int bandDim, final DataStore lock)
    {
        super(decoder.listeners, false);
        this.lock        = lock;
        gridGeometry     = grid;
        bandDimension    = bandDim;
        location         = decoder.location;
        identifier       = decoder.nameFactory.createLocalName(decoder.namespace, name);
        visibleBand      = decoder.convention().getVisibleBand();
        sampleDimensions = new SampleDimension[numBands];
        data             = bands.toArray(Variable[]::new);
        assert data.length == (bandDimension >= 0 ? 1 : sampleDimensions.length);
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
    public static List<Resource> create(final Decoder decoder, final DataStore lock) throws IOException, DataStoreException {
        assert Thread.holdsLock(lock);
        final Variable[] variables = decoder.getVariables().clone();        // Needs a clone because may be modified.
        final var siblings  = new ArrayList<Variable>(4);                   // Usually has only 1 element, sometimes 2.
        final var resources = new ArrayList<Resource>(variables.length);    // The raster resources to be returned.
        final var byName = new HashMap<GenericName,List<RasterResource>>(); // For detecting name collisions.
        for (int i=0; i<variables.length; i++) {
            final Variable variable = variables[i];
            if (!VariableRole.isCoverage(variable)) {
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
             * the resource will have a single variable and the same name as that unique variable. The resulting raster will
             * have only one band (sample dimension). However, in some cases the raster should have more than one band:
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
                     *   - The dimension for bands shall be either the first or the last dimension; it cannot be in the middle.
                     */
                    throw new DataStoreContentException(Resources.forLocale(decoder.listeners.getLocale())
                            .getString(Resources.Keys.UnmappedDimensions_4, name, decoder.getFilename(), dataDimension, gridDimension));
                }
            } else {
                /*
                 * At this point we found a variable where all dimensions are in the CRS. This is the usual case;
                 * there are no bands explicitly declared in the netCDF file. However, in some cases, we should put
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
                            if (!VariableRole.isCoverage(candidate)) {
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
                                    if (cn.startsWith(k, prefixLength)) {
                                        siblings.add(candidate);
                                        variables[j] = null;
                                        break;
                                    }
                                }
                            }
                        }
                        /*
                         * If we have more than one variable, omit the keyword from the name. For example, instead
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
            final var r = new RasterResource(decoder, name.trim(), grid, siblings, numBands, bandDimension, lock);
            r.addToNameMap(byName);
            resources.add(r);
            siblings.clear();
        }
        /*
         * At this point all resources have been prepared. If the same standard name is used by more than one resource,
         * replace the standard name by the identifier for all resources in collision (an "all or nothing" replacement).
         * The identifiers should be unique, which should resolve the name collision. We nevertheless check again after
         * renaming until there is nothing to change.
         */
        boolean changed;
        do {
            changed = false;
            List<RasterResource> collisions = null;
            for (final Iterator<List<RasterResource>> it = byName.values().iterator(); it.hasNext();) {
                final List<RasterResource> rs = it.next();
                if (rs.size() >= 2) {
                    it.remove();                            // Remove resources before to re-insert them in next loop below.
                    if (collisions == null) {
                        collisions = rs;
                    } else {
                        collisions.addAll(rs);
                    }
                }
            }
            if (collisions != null) {                       // After above loop because may change the byName` map.
                for (final RasterResource r : collisions) {
                    changed |= r.resolveNameCollision(decoder);
                    r.addToNameMap(byName);                 // For checking if new names cause new collisions.
                }
            }
        }
        while (changed);
        return resources;
    }

    /**
     * Adds the name of the given resource to the given map. This is used for resolving name collision: any entry
     * associated to two values or more will have the resources renamed by {@link #resolveNameCollision(Decoder)}.
     */
    private void addToNameMap(final Map<GenericName,List<RasterResource>> byName) {
        byName.computeIfAbsent(identifier, (key) -> new ArrayList<>()).add(this);
    }

    /**
     * Invoked when the name of this resource needs to be changed because it collides with the name of another resource.
     * This method uses the variable name, which should be unique in each netCDF file. If this resource wraps more than
     * one variable (for example "eastward_velocity" and "northward_velocity"), then this method takes the common part
     * of all variable names ("velocity" in the above example).
     *
     * @return whether this resource has been renamed as an effect of this method call.
     */
    private boolean resolveNameCollision(final Decoder decoder) {
        String name = null;
        for (final Variable v : data) {
            name = (String) CharSequences.commonWords(name, v.getName());
        }
        if (Strings.isNullOrEmpty(name)) {
            name = data[0].getName();           // If unable to get a common name, fallback on the first one.
        }
        final GenericName newValue = decoder.nameFactory.createLocalName(decoder.namespace, name);
        if (newValue.equals(identifier)) return false;
        identifier = newValue;
        return true;
    }

    /**
     * Invoked the first time that {@link #getMetadata()} is invoked. Computes metadata based on information
     * provided by {@link #getIdentifier()}, {@link #getGridGeometry()}, {@link #getSampleDimensions()} and
     * variable names.
     *
     * @throws DataStoreException if an error occurred while reading metadata from this resource.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final var metadata = new MetadataBuilder();
        String title = null;
        for (final Variable v : data) {
            title = (String) CharSequences.commonWords(title, v.getDescription());
            metadata.addIdentifier(v.getGroupName(), v.getName(), MetadataBuilder.Scope.RESOURCE);
        }
        if (title != null && !title.isEmpty()) {
            metadata.addTitle(CharSequences.camelCaseToSentence(title).toString());
        }
        metadata.addDefaultMetadata(this, listeners);
        return metadata.build();
    }

    /**
     * Returns the standard name (if non ambiguous) or the variable name as an identifier of this resource.
     */
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.of(identifier);
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
     * if we are in the special case where all bands are in the same variable ({@link #bandDimension} ≥ 0).
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
                for (int i=0; i<sampleDimensions.length; i++) {
                    if (sampleDimensions[i] == null) {
                        if (builder == null) {
                            builder = new SampleDimension.Builder();
                        }
                        sampleDimensions[i] = createSampleDimension(builder, getVariable(i), i);
                        builder.clear();
                    }
                }
            }
        } catch (RuntimeException e) {
            throw new DataStoreContentException(e);
        }
        return UnmodifiableArrayList.wrap(sampleDimensions);
    }

    /**
     * Creates a single sample dimension for the given variable.
     *
     * @param  builder  the builder to use for creating the sample dimension.
     * @param  band     the data for which to create a sample dimension.
     * @param  index    index in the variable dimension identified by {@link #bandDimension}.
     */
    private SampleDimension createSampleDimension(final SampleDimension.Builder builder, final Variable band, final int index) {
        /*
         * Take the minimum and maximum values as determined by Apache SIS through the Convention class.  The UCAR library
         * is used only as a fallback. We give precedence to the range computed by Apache SIS instead of the range given
         * by UCAR because we need the range of packed values instead of the range of converted values.
         */
        NumberRange<?> range;
        if (!createEnumeration(builder, band) && (range = band.getValidRange()) != null) try {
            final MathTransform1D mt = band.getTransferFunction().getTransform();
            if (!mt.isIdentity() && range instanceof MeasurementRange<?>) {
                /*
                 * Heuristic rule defined in UCAR documentation (see EnhanceScaleMissingUnsigned):
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
             * or if we have not read them correctly (edu.ucar:cdm:4.6.13 sometimes confuses an
             * unsigned integer with a signed one).
             */
            if (range.isEmpty()) {
                band.warning(RasterResource.class, "getSampleDimensions", null, Resources.Keys.IllegalValueRange_4,
                        band.getFilename(), band.getName(), range.getMinValue(), range.getMaxValue());
            } else {
                String name = band.getDescription();
                if (name == null) name = band.getName();
                if (band.getRole() == VariableRole.DISCRETE_COVERAGE) {
                    builder.addQualitative(name, range);
                } else {
                    builder.addQuantitative(name, range, mt, band.getUnit());
                }
            }
        } catch (TransformException e) {
            /*
             * This exception may happen in the call to `inverse.transform`, when we tried to convert
             * a range of measurement values (in the unit of measurement) to a range of sample values.
             * If we failed to do that, we will not add quantitative category. But we still can add
             * qualitative categories for "no data" sample values in the rest of this method.
             */
            listeners.warning(e);
        }
        /*
         * Adds the "missing value" or "fill value" as qualitative categories. If the sample values are already
         * real values, then the "no data" values have been replaced by NaN values by Variable.replaceNaN(Object).
         * The qualitative categories constructed below must be consistent with NaN values created by `replaceNaN`.
         */
        boolean setBackground = true;
        int missingValueOrdinal = 0;
        int ordinal = band.hasRealValues() ? 0 : -1;
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
                final boolean isFill = setBackground && (((Integer) label) & Convention.FILL_VALUE_MASK) != 0;
                name = Vocabulary.formatInternational(isFill ? Vocabulary.Keys.FillValue : Vocabulary.Keys.MissingValue);
                if (isFill) {
                    setBackground = false;                      // Declare only one fill value.
                    builder.setBackground(name, n);
                    continue;
                }
                if (++missingValueOrdinal >= 2) {
                    name = name.toString() + " #" + missingValueOrdinal;
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
        builder.setName(name);
        SampleDimension sd;
        try {
            sd = builder.build();
        } catch (IllegalSampleDimensionException e) {
            /*
             * This error may happen if we have overlapping ranges of sample values.
             * Abandon all categories. We do not keep the quantitative category because
             * using it without taking in account the "no data" values may be dangerous.
             */
            builder.categories().clear();
            sd = builder.build();
            listeners.warning(e);
        }
        return sd;
    }

    /**
     * Appends qualitative categories in the given builder for {@code "flag_values"} or {@code "flag_masks"} attribute.
     *
     * @param  builder  the builder to use for creating the sample dimension.
     * @param  band     the data for which to create a sample dimension.
     * @return {@code true} if flag attributes have been found, or {@code false} otherwise.
     */
    private static boolean createEnumeration(final SampleDimension.Builder builder, final Variable band) {
        final Map<Long,String> enumeration = band.getEnumeration();
        if (enumeration == null) {
            return false;
        }
        for (final Map.Entry<Long,String> entry : enumeration.entrySet()) {
            final Long value = entry.getKey();
            CharSequence name = entry.getValue();
            if (name == null) {
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
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(final GridGeometry domain, final int... ranges) throws DataStoreException {
        final long startTime = System.nanoTime();
        final RangeArgument rangeIndices = RangeArgument.validate(sampleDimensions.length, ranges, listeners);
        final Variable first = data[bandDimension >= 0 ? 0 : rangeIndices.getFirstSpecified()];
        final DataType dataType = first.getDataType();
        if (bandDimension < 0) {
            for (int i=0; i<rangeIndices.getNumBands(); i++) {
                final Variable variable = data[rangeIndices.getSourceIndex(i)];
                if (!dataType.equals(variable.getDataType())) {
                    throw new DataStoreContentException(Resources.forLocale(listeners.getLocale()).getString(
                            Resources.Keys.MismatchedVariableType_3, getFilename(), first.getName(), variable.getName()));
                }
            }
        }
        /*
         * At this point the arguments and the state of this resource have been validated.
         * There are three ways to read the data, determined by `bandDimension` value:
         *
         *   • (bandDimension < 0): one variable per band (usual case).
         *   • (bandDimension = 0): one variable containing all bands, with bands in the first dimension.
         *   • (bandDimension > 0): one variable containing all bands, with bands in the last dimension.
         */
        final GridGeometry targetDomain;
        final DataBuffer imageBuffer;
        final var bands = new SampleDimension[rangeIndices.getNumBands()];
        int[] bandOffsets = null;                                                   // By default, all bands start at index 0.
        try {
            final GridDerivation targetGeometry = gridGeometry.derive()
                    .rounding(GridRoundingMode.ENCLOSING)
                    .subgrid((domain != null) ? domain : gridGeometry);
            GridExtent areaOfInterest = targetGeometry.getIntersection();           // Pixel indices of data to read.
            long[]     subsampling    = targetGeometry.getSubsampling();            // Slice to read or subsampling to apply.
            int        numBuffers     = bands.length;                               // By default, one variable per band.
            targetDomain = targetGeometry.build();                                  // Adjust user-specified domain to data geometry.
            if (bandDimension >= 0) {
                areaOfInterest = rangeIndices.insertBandDimension(areaOfInterest, bandDimension);
                subsampling    = rangeIndices.insertSubsampling  (subsampling,    bandDimension);
                if (bandDimension == 0) {
                    bandOffsets = new int[numBuffers];          // Will be set to non-zero values later.
                }
                numBuffers = 1;                                 // One variable for all bands.
            }
            /*
             * Iterate over netCDF variables in the order they appear in the file, not in the order requested
             * by the `ranges` argument. The intent is to perform sequential I/O as much as possible, without
             * seeking backward. In the (uncommon) case where bands are one of the variable dimension instead
             * than different variables, the reading of the whole variable occurs during the first iteration.
             */
            Buffer[] sampleValues = new Buffer[numBuffers];
            synchronized (lock) {
                for (int i=0; i<bands.length; i++) {
                    int indexInResource = rangeIndices.getSourceIndex(i);     // In strictly increasing order.
                    int indexInRaster   = rangeIndices.getTargetIndex(i);
                    Variable variable   = getVariable(indexInResource);
                    SampleDimension b   = sampleDimensions[indexInResource];
                    if (b == null) {
                        sampleDimensions[indexInResource] = b = createSampleDimension(rangeIndices.builder(), variable, i);
                    }
                    bands[indexInRaster] = b;
                    if (bandOffsets != null) {
                        bandOffsets[indexInRaster] = i;
                        indexInRaster = 0;                  // Pixels interleaved in one bank: sampleValues.length = 1.
                    }
                    if (i < numBuffers) try {
                        // Optional.orElseThrow() below should never fail since Variable.read(…) wraps primitive array.
                        sampleValues[indexInRaster] = variable.read(areaOfInterest, subsampling).buffer().get();
                    } catch (ArithmeticException e) {
                        throw variable.canNotComputePosition(e);
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
                        values = values.duplicate();
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
        } catch (IOException | RuntimeException e) {
            throw canNotRead(getFilename(), domain, e);
        }
        /*
         * At this point the I/O operation is completed and sample values have been stored in a NIO buffer.
         * Provide to `Raster` all information needed for building a `RenderedImage` when requested.
         */
        if (imageBuffer == null) {
            throw new DataStoreContentException(Errors.forLocale(listeners.getLocale()).getString(Errors.Keys.UnsupportedType_1, dataType.name()));
        }
        final Variable main = data[visibleBand];
        final Raster raster = new Raster(targetDomain, UnmodifiableArrayList.wrap(bands), imageBuffer,
                rangeIndices.getPixelStride(), bandOffsets, visibleBand,
                main.decoder.convention().getColors(main));
        logReadOperation(location, targetDomain, startTime);
        return raster;
    }

    /**
     * Returns the name of the netCDF file. This is used for error messages.
     */
    private String getFilename() {
        return (location != null) ? location.getFileName().toString() : listeners.getSourceName();
    }

    /**
     * Gets the paths to files used by this resource, or an empty value if unknown.
     */
    @Override
    public final Optional<FileSet> getFileSet() {
        return (location != null) ? Optional.of(new FileSet(location)) : Optional.empty();
    }

    /**
     * Returns the data store that produced this resource.
     */
    @Override
    public final DataStore getOriginator() {
        return lock;
    }

    /**
     * Returns the object on which to perform synchronizations for thread-safety.
     *
     * @return the synchronization lock.
     */
    @Override
    protected final Object getSynchronizationLock() {
        /*
         * Could be replaced by `(DataStore) listeners.getParent().get().getSource()`
         * if a future version decides to use a different kind of lock.
         */
        return lock;
    }

    /**
     * Returns a string representation of this resource for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "identifier", identifier);
    }
}
