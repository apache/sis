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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.io.IOException;
import java.nio.file.Path;
import ucar.nc2.constants.CF;       // String constants are copied by the compiler with no UCAR reference left.
import org.opengis.util.NameSpace;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.system.Modules;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.PerformanceLevel;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.referencing.internal.shared.ReferencingFactoryContainer;


/**
 * The API used internally by Apache SIS for fetching variables and attribute values from a netCDF file.
 *
 * <p>This {@code Decoder} class and subclasses are <strong>not</strong> thread-safe.
 * Synchronizations are caller's responsibility.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class Decoder extends ReferencingFactoryContainer {
    /**
     * The logger to use for messages other than warnings specific to the file being read.
     * This is rarely used directly because {@code listeners.getLogger()} should be preferred.
     *
     * @see #listeners
     */
    public static final Logger LOGGER = Logger.getLogger(Modules.NETCDF);

    /**
     * The format name to use in error message. We use lower-case "n" because it seems to be what the netCDF community uses.
     * By contrast, {@code NetcdfStoreProvider} uses upper-case "N" because it is considered at the beginning of sentences.
     */
    public static final String FORMAT_NAME = "netCDF";

    /**
     * The locale of data such as number formats, dates and names.
     * This is used, for example, for the conversion to lower-cases before case-insensitive searches.
     * This is not the locale for error messages or warnings reported to the user.
     */
    public static final Locale DATA_LOCALE = Locale.US;

    /**
     * The path to the netCDF file, or {@code null} if unknown.
     * This is set by netCDF store constructor and shall not be modified afterward.
     * This is used for information purpose only, not for actual reading operation.
     */
    public Path location;

    /**
     * Conventions to apply in addition of netCDF conventions.
     * Shall never be {@code null} after {@link #initialize()}.
     *
     * @see #convention()
     */
    private Convention convention;

    /**
     * The data store identifier created from the global attributes, or {@code null} if none.
     * Defined as a namespace for use as the scope of children resources (the variables).
     * This is set by netCDF store constructor and shall not be modified afterward.
     */
    public NameSpace namespace;

    /**
     * The factory to use for creating variable identifiers.
     */
    public final DefaultNameFactory nameFactory;

    /**
     * The library for geometric objects, or {@code null} for the default.
     * This will be used only if there is geometric objects to create.
     * If the netCDF file contains only raster data, this value is ignored.
     */
    public final GeometryLibrary geomlib;

    /**
     * The geodetic reference frame, created when first needed. The datum are generally not specified in netCDF files.
     * To make that clearer, we will build datum with names like "Unknown datum presumably based on GRS 1980".
     * Index in the cache are one of the {@code CACHE_INDEX} constants declared in {@link CRSBuilder}.
     *
     * @see CRSBuilder#build(Decoder, boolean)
     */
    final Datum[] datumCache;

    /**
     * Information for building <abbr>CRS</abbr>s and <i>grid to CRS</i> transforms for variables.
     * The {@link GridMapping} class supports different conventions: the <abbr>CF</abbr> conventions
     * are tried first, followed by <abbr>GDAL</abbr> conventions (pair of {@code "spatial_ref_sys"}
     * and {@code "GeoTransform"} attributes), then <abbr>ESRI</abbr> conventions.
     * The keys are variable names from two sources:
     *
     * <ol>
     *   <li>Name of an usually empty variable referenced by a {@code "grid_mapping"} attribute on the actual data.
     *       This is the standard approach, as it allows many variables to reference the same <abbr>CRS</abbr>
     *       definition by declaring the same value in their {@code "grid_mapping"} attribute.</li>
     *   <li>Name of the actual data variable when the attributes are found directly on that variable.
     *       This approach is non-standard, as it does not allow the sharing of the same <abbr>CRS</abbr>
     *       definition by many variables. But it is nevertheless observed in practice.</li>
     * </ol>
     *
     * @see GridMapping#forVariable(Variable)
     */
    final Map<String,GridMapping> gridMapping;

    /**
     * Cache of localization grids created for a given pair of (<var>x</var>,<var>y</var>) axes.
     * Localization grids are expensive to compute and consume a significant amount of memory.
     * The {@link Grid} instances returned by {@link #getGridCandidates()} share localization
     * grids only between variables using the exact same list of dimensions.
     * This {@code localizationGrids} cache allows to cover other cases.
     *
     * <h4>Example</h4>
     * A netCDF file may have a variable with (<var>longitude</var>, <var>latitude</var>) dimensions and another
     * variable with (<var>longitude</var>, <var>latitude</var>, <var>depth</var>) dimensions, with both variables
     * using the same localization grid for the (<var>longitude</var>, <var>latitude</var>) part.
     *
     * @see GridCacheKey#cached(Decoder)
     */
    final Map<GridCacheKey,GridCacheValue> localizationGrids;

    /**
     * Where to send the warnings.
     */
    public final StoreListeners listeners;

    /**
     * Sets to {@code true} for canceling a reading process.
     * This flag is honored on a <em>best effort</em> basis only.
     */
    public volatile boolean canceled;

    /**
     * Creates a new decoder.
     *
     * @param  geomlib    the library for geometric objects, or {@code null} for the default.
     * @param  listeners  where to send the warnings.
     */
    protected Decoder(final GeometryLibrary geomlib, final StoreListeners listeners) {
        this.geomlib      = geomlib;
        this.listeners    = Objects.requireNonNull(listeners);
        this.nameFactory  = DefaultNameFactory.provider();
        this.datumCache   = new Datum[CRSBuilder.DATUM_CACHE_SIZE];
        this.gridMapping  = new HashMap<>();
        localizationGrids = new HashMap<>();
    }

    /**
     * Shall be invoked by subclass constructors after the finished their construction, for completing initialization.
     * This method checks if an extension to CF-convention applies to the current file.
     */
    protected final void initialize() {
        convention = Convention.find(this);
    }

    /**
     * Checks and potentially modifies the content of this dataset for conventions other than CF-conventions.
     * This method should be invoked after construction for handling the particularities of some datasets
     * (HYCOM, …).
     *
     * @throws IOException if an error occurred while reading the channel.
     * @throws DataStoreException if an error occurred while interpreting the netCDF file content.
     */
    public final void applyOtherConventions() throws IOException, DataStoreException {
        final var t = new VariableTransformer(this);
        for (Variable variable : getVariables()) {
            t.analyze(variable);
        }
    }

    /**
     * Returns information about modifications to apply to netCDF conventions in order to handle this netCDF file.
     * Customized conventions are necessary when the variables and attributes in a netCDF file do not follow CF-conventions.
     *
     * @return conventions to apply.
     */
    public final Convention convention() {
        // Convention are still null if this method is invoked from Convention.isApplicableTo(Decoder).
        return (convention != null) ? convention : Convention.DEFAULT;
    }

    /**
     * Adds netCDF attributes to the given node, including variables and sub-groups attributes.
     * Groups are shown first, then variables attributes, and finally global attributes.
     * Showing global attributes last is consistent with ncML ("netCDF dump") output.
     *
     * @param  root  the node where to add netCDF attributes.
     */
    public abstract void addAttributesTo(TreeTable.Node root);

    /**
     * Returns a filename for formatting error message and for information purpose.
     * The filename should not contain path, but may contain file extension.
     *
     * @return a filename to include in warnings or error messages.
     */
    public abstract String getFilename();

    /**
     * Adds to the given metadata an identification of the file format.
     * Subclasses should invoke the following methods:
     *
     * <ul>
     *   <li>{@link MetadataBuilder#setPredefinedFormat(String, StoreListeners, boolean)}</li>
     *   <li>{@link MetadataBuilder#addFormatReaderSIS(String)} (if applicable)</li>
     * </ul>
     */
    public abstract void addFormatDescription(MetadataBuilder builder);

    /**
     * Defines the groups where to search for named attributes, in preference order.
     * The {@code null} group name stands for attributes in the root group.
     *
     * @param  groupNames  the name of the group where to search, in preference order.
     *
     * @see Convention#getSearchPath()
     */
    public abstract void setSearchPath(String... groupNames);

    /**
     * Returns the path which is currently set. The array returned by this method may be only
     * a subset of the array given to {@link #setSearchPath(String[])} since only the name of
     * groups which have been found in the netCDF file are returned by this method.
     *
     * @return the current search path.
     */
    public abstract String[] getSearchPath();

    /**
     * Returns the names of all global attributes found in the file.
     *
     * @return names of all global attributes in the file.
     */
    public abstract Collection<String> getAttributeNames();

    /**
     * Returns the value for the attribute of the given name, or {@code null} if none.
     * This method searches in the groups specified by the last call to {@link #setSearchPath(String[])}.
     * Null values and empty strings are ignored.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or empty or if the given name was null.
     */
    public abstract String stringValue(String name);

    /**
     * Returns the value of the attribute of the given name as a number, or {@code null} if none.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or unparsable or if the given name was null.
     */
    public abstract Number numericValue(String name);

    /**
     * Convenience method for {@link #numericValue(String)} implementation.
     *
     * @param  name   the attribute name, used only in case of error.
     * @param  value  the attribute value to parse.
     * @return the parsed attribute value, or {@code null} if the given value cannot be parsed.
     */
    protected final Number parseNumber(final String name, String value) {
        final int s = value.indexOf(' ');
        if (s >= 0) {
            /*
             * Sometimes, numeric values as string are followed by
             * a unit of measurement. We ignore that unit for now.
             */
            value = value.substring(0, s);
        }
        Number n;
        try {
            if (value.indexOf('.') >= 0) {
                n = Double.valueOf(value);
            } else {
                n = Long.valueOf(value);
            }
        } catch (NumberFormatException e) {
            illegalAttributeValue(name, value, e);
            n = null;
        }
        return n;
    }

    /**
     * Logs a warning for an illegal attribute value. This may be due to a failure to parse a string as a number.
     * This method should be invoked from methods that are invoked only once per attribute because we do not keep
     * track of which warnings have already been emitted.
     *
     * @param  name   the attribute name.
     * @param  value  the illegal value.
     * @param  e      the exception, or {@code null} if none.
     */
    final void illegalAttributeValue(final String name, final String value, final Exception e) {
        listeners.warning(resources().getString(Resources.Keys.IllegalAttributeValue_3, getFilename(), name, value), e);
    }

    /**
     * Returns the value of the attribute of the given name as a date, or {@code null} if none.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or unparsable or if the given name was null.
     */
    public abstract Temporal dateValue(String name);

    /**
     * Converts the given numerical values to date, using the information provided in the given unit symbol.
     * The unit symbol is typically a string like <q>days since 1970-01-01T00:00:00Z</q>.
     *
     * @param  symbol  the temporal unit name or symbol, followed by the epoch.
     * @param  values  the values to convert. May contains {@code null} elements.
     * @return the converted values. May contains {@code null} elements.
     */
    public abstract Temporal[] numberToDate(String symbol, Number... values);

    /**
     * Returns the timezone for decoding dates. Currently fixed to UTC.
     *
     * @return the timezone for dates.
     */
    public ZoneOffset getTimeZone() {
        return ZoneOffset.UTC;
    }

    /**
     * Returns the value of the {@code "_Id"} global attribute. The UCAR library defines a
     * {@link ucar.nc2.NetcdfFile#getId()} method for that purpose, which we will use when
     * possible in case that {@code getId()} method is defined in another way.
     *
     * <p>This method is used by {@link org.apache.sis.storage.netcdf.NetcdfStore#getMetadata()} in last resort
     * when no value were found for the attributes defined by the CF standard or by THREDDS.</p>
     *
     * @return the global dataset identifier, or {@code null} if none.
     */
    public String getId() {
        return stringValue("_Id");
    }

    /**
     * Returns the value of the {@code "_Title"} global attribute. The UCAR library defines a
     * {@link ucar.nc2.NetcdfFile#getTitle()} method for that purpose, which we will use when
     * possible in case that {@code getTitle()} method is defined in another way.
     *
     * <p>This method is used by {@link org.apache.sis.storage.netcdf.NetcdfStore#getMetadata()} in last resort
     * when no value were found for the attributes defined by the CF standard or by THREDDS.</p>
     *
     * @return the dataset title, or {@code null} if none.
     */
    public String getTitle() {
        return stringValue("_Title");
    }

    /**
     * Returns all variables found in the netCDF file.
     * This method may return a direct reference to an internal array - do not modify.
     *
     * @return all variables, or an empty array if none.
     */
    public abstract Variable[] getVariables();

    /**
     * If the file contains features encoded as discrete sampling (for example profiles or trajectories),
     * returns objects for handling them. This method does not need to cache the returned array, because
     * it will be invoked only once by {@link org.apache.sis.storage.netcdf.NetcdfStore#components()}.
     *
     * @param  lock  the lock to use in {@code synchronized(lock)} statements.
     * @return a handler for the features, or an empty array if none.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     */
    public DiscreteSampling[] getDiscreteSampling(final DataStore lock) throws IOException, DataStoreException {
        final String type = stringValue(CF.FEATURE_TYPE);
        if (type == null || type.equalsIgnoreCase(FeatureSet.TRAJECTORY)) try {
            return FeatureSet.create(this, lock);
        } catch (IllegalArgumentException | ArithmeticException e) {
            // Illegal argument is not a problem with content, but rather with configuration.
            throw new DataStoreException(e.getLocalizedMessage(), e);
        }
        return new FeatureSet[0];
    }

    /**
     * Returns all grid geometries (related to coordinate systems) found in the netCDF file.
     * This method may return a direct reference to an internal array - do not modify.
     *
     * <p>The number of grid geometries returned by this method may be greater that the actual number of
     * grids in the file. A more extensive analysis is done by {@link Variable#findGrid(GridAdjustment)},
     * which may result in some grid candidates being filtered out.</p>
     *
     * @return all grid geometries, or an empty array if none.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     */
    public abstract Grid[] getGridCandidates() throws IOException, DataStoreException;

    /**
     * Returns for information purpose only the Coordinate Reference Systems present in this file.
     * The CRS returned by this method may not be exactly the same as the ones used by variables.
     * For example, axis order is not guaranteed. This method is provided for metadata purposes.
     *
     * @return coordinate reference systems present in this file.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     */
    public final List<CoordinateReferenceSystem> getReferenceSystemInfo() throws IOException, DataStoreException {
        final var list = new ArrayList<CoordinateReferenceSystem>();
        for (final Variable variable : getVariables()) {
            final var gm = GridMapping.forVariable(variable);
            if (gm != null) {
                addIfNotPresent(list, gm.crs());
            }
        }
        /*
         * Add the CRS computed by grids only if we did not found any grid mapping information.
         * This is because grid mapping information override the CRS inferred by Grid from axes.
         * Consequently, if such information is present, grid CRS may be inaccurate.
         */
        if (list.isEmpty()) {
            final var warnings = new ArrayList<Exception>();    // For internal usage by Grid.
            for (final Grid grid : getGridCandidates()) {
                addIfNotPresent(list, grid.getCRSFromAxes(this, warnings, null, null));
            }
        }
        return list;
    }

    /**
     * Adds the given coordinate reference system to the given list, provided that an equivalent CRS
     * (ignoring axes) is not already present. We ignore axes because the same CRS may be repeated
     * with different axis order if values in the localization grid do not vary at the same speed in
     * the same directions.
     */
    private static void addIfNotPresent(final List<CoordinateReferenceSystem> list, final CoordinateReferenceSystem crs) {
        if (crs != null) {
            for (int i=list.size(); --i >= 0;) {
                if (Utilities.deepEquals(crs, list.get(i), ComparisonMode.ALLOW_VARIANT)) {
                    return;
                }
            }
            list.add(crs);
        }
    }

    /**
     * Returns the dimension of the given name (eventually ignoring case), or {@code null} if none.
     * This method searches in all dimensions found in the netCDF file, regardless of variables.
     *
     * @param  dimName  the name of the dimension to search.
     * @return dimension of the given name, or {@code null} if none.
     */
    protected abstract Dimension findDimension(String dimName);

    /**
     * Returns the netCDF variable of the given name, or {@code null} if none.
     *
     * @param  name  the name of the variable to search, or {@code null}.
     * @return the variable of the given name, or {@code null} if none.
     *
     * @see #getVariables()
     */
    protected abstract Variable findVariable(String name);

    /**
     * Returns the variable or group of the given name. Groups exist in netCDF 4 but not in netCDF 3.
     *
     * @param  name  name of the variable or group to search.
     * @return the variable or group of the given name, or {@code null} if none.
     */
    protected abstract Node findNode(String name);

    /**
     * Logs a message about a potentially slow operation. This method does use the listeners registered to the netCDF reader
     * because this is not a warning.
     *
     * @param  caller       the class to report as the source.
     * @param  method       the method to report as the source.
     * @param  resourceKey  a {@link Resources} key expecting filename as first argument and elapsed time as second argument.
     * @param  time         value of {@link System#nanoTime()} when the operation started.
     */
    final void performance(final Class<?> caller, final String method, final short resourceKey, long time) {
        time = System.nanoTime() - time;
        final Level level = PerformanceLevel.forDuration(time, TimeUnit.NANOSECONDS);
        final Logger logger = listeners.getLogger();
        if (logger.isLoggable(level)) {
            final LogRecord record = resources().createLogRecord(level, resourceKey,
                    getFilename(), time / (double) Constants.NANOS_PER_SECOND);
            Logging.completeAndLog(logger, caller, method, record);
        }
    }

    /**
     * Returns the netCDF-specific resource bundle for the locale given by {@link StoreListeners#getLocale()}.
     *
     * @return the localized error resource bundle.
     */
    public final Resources resources() {
        return Resources.forLocale(getLocale());
    }

    /**
     * Returns the locale used for error message, or {@code null} if unspecified.
     * In the latter case, the platform default locale will be used.
     *
     * @return the locale for messages (typically specified by the data store), or {@code null} if unknown.
     */
    @Override
    public final Locale getLocale() {
        return listeners.getLocale();
    }

    /**
     * Closes this decoder and releases resources.
     *
     * @param  lock  the lock to use in {@code synchronized(lock)} statements.
     * @throws IOException if an error occurred while closing the decoder.
     */
    public abstract void close(DataStore lock) throws IOException;
}
