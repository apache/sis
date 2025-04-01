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
package org.apache.sis.storage.netcdf.ucar;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.Formatter;
import java.util.Collection;
import java.time.Instant;
import java.time.temporal.Temporal;
import ucar.nc2.Group;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.util.CancelTask;
import ucar.nc2.units.DateUnit;
import ucar.units.UnitException;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.DsgFeatureCollection;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.Version;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.metadata.sql.MetadataSource;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.netcdf.base.Decoder;
import org.apache.sis.storage.netcdf.base.Variable;
import org.apache.sis.storage.netcdf.base.Dimension;
import org.apache.sis.storage.netcdf.base.Node;
import org.apache.sis.storage.netcdf.base.Grid;
import org.apache.sis.storage.netcdf.base.Convention;
import org.apache.sis.storage.netcdf.base.DiscreteSampling;
import org.apache.sis.setup.GeometryLibrary;


/**
 * Provides netCDF decoding services based on the netCDF library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DecoderWrapper extends Decoder implements CancelTask {
    /**
     * Version of the <abbr>UCAR</abbr> library, fetched when first requested.
     * May be {@code null} if no version information was found.
     */
    private static Version version;

    /**
     * Whether {@link #version} has been initialized. The result may still be null.
     */
    private static boolean versionInitialized;

    /**
     * The netCDF file to read.
     * This file is set at construction time.
     */
    private final NetcdfFile file;

    /**
     * The groups where to look for named attributes, in preference order. When used for constructing
     * ISO 19115 metadata, the first group shall be {@code null} (which stands for global attributes)
     * and all other groups shall be non-null values for the {@code "NCISOMetadata"}, {@code "THREDDSMetadata"}
     * and {@code "CFMetadata"} groups, if they exist.
     *
     * @see #setSearchPath(String[])
     */
    private Group[] groups;

    /**
     * The variables, computed when first needed.
     *
     * @see #getVariables()
     */
    private transient VariableWrapper[] variables;

    /**
     * The discrete sampling features or grids found by UCAR library, or {@code null} if none.
     * This reference is kept for making possible to close it in {@link #close(DataStore)}.
     *
     * @see #getDiscreteSampling(Object)
     */
    private transient FeatureDataset features;

    /**
     * The grid geometries, computed when first needed.
     *
     * @see #getGridCandidates()
     */
    private transient Grid[] geometries;

    /**
     * Sets to {@code true} for declaring that the operation completed, either successfully or with an error.
     *
     * @see #isDone()
     */
    private boolean done;

    /**
     * Creates a new decoder for the given netCDF file. While this constructor accepts arbitrary
     * {@link NetcdfFile} instance, the {@link NetcdfDataset} subclass is necessary in order to
     * get coordinate system information.
     *
     * @param geomlib    the library for geometric objects, or {@code null} for the default.
     * @param file       the netCDF file from which to read data.
     * @param listeners  where to send the warnings.
     */
    public DecoderWrapper(final NetcdfFile file, final GeometryLibrary geomlib, final StoreListeners listeners) {
        super(geomlib, listeners);
        this.file = file;
        groups = new Group[1];
        initialize();
    }

    /**
     * Creates a new decoder for the given filename.
     *
     * @param  geomlib    the library for geometric objects, or {@code null} for the default.
     * @param  filename   the name of the netCDF file from which to read data.
     * @param  listeners  where to send the warnings.
     * @throws IOException if an error occurred while opening the netCDF file.
     */
    public DecoderWrapper(final String filename, final GeometryLibrary geomlib, final StoreListeners listeners)
            throws IOException
    {
        super(geomlib, listeners);
        final DatasetUrl url = DatasetUrl.findDatasetUrl(filename);
        /*
         * It is important to specify only the `CoordSystems` enhancement. In particular the `ConvertUnsigned`
         * enhancement shall NOT be enabled because it causes the use of integer types twice bigger than needed
         * (e.g. `int` instead of `short`).
         */
        file = NetcdfDatasets.openDataset(url, Set.of(NetcdfDataset.Enhance.CoordSystems), -1, this, null);
        groups = new Group[1];
        initialize();
    }

    /**
     * Returns a filename for formatting error message and for information purpose.
     * The filename should not contain path, but may contain file extension.
     *
     * @return a filename to include in warnings or error messages.
     */
    @Override
    public String getFilename() {
        String filename = Utils.nonEmpty(file.getLocation());
        if (filename != null) {
            int s = filename.lastIndexOf(File.separatorChar);
            if (s < 0 && File.separatorChar != '/') {
                s = filename.lastIndexOf('/');
            }
            if (s >= 0) {
                filename = filename.substring(s+1);
            }
        }
        return filename;
    }

    /**
     * Returns the file format information provided by the UCAR library.
     * The information includes:
     *
     * <ol>
     *   <li>{@code "NetCDF"}, {@code "NetCDF-4"} or other values defined by the UCAR library.
     *       If known, it will be used as an identifier for a more complete description to be
     *       provided by {@link org.apache.sis.metadata.sql.MetadataSource#lookup(Class, String)}.</li>
     *   <li>Optionally a human-readable description.</li>
     *   <li>Optionally a file format version.</li>
     * </ol>
     *
     * @return identification of the file format, human-readable description and version number.
     */
    @Override
    public void addFormatDescription(MetadataBuilder builder) {
        String name = Utils.nonEmpty(file.getFileTypeId());
        if (builder.setPredefinedFormat(name, null, false)) {
            name = null;
        }
        builder.addFormatName(Utils.nonEmpty(file.getFileTypeDescription()));
        builder.setFormatEdition(Utils.nonEmpty(file.getFileTypeVersion()));
        builder.addFormatName(name);        // Do nothing if `name` is null.
        Citation provider;
        try {
            provider = MetadataSource.getProvided().lookup(Citation.class, "Unidata");
        } catch (MetadataStoreException e) {
            provider = null;
        }
        builder.addFormatReader((provider != null) ? provider.getTitle() : Constants.NETCDF, getVersion());
    }

    /**
     * Returns the version number of the netCDF library, or {@code null} if not found.
     */
    private static synchronized Version getVersion() {
        if (!versionInitialized) {
            versionInitialized = true;
            version = Version.ofLibrary(NetcdfFile.class).orElse(null);
        }
        return version;
    }

    /**
     * Defines the groups where to search for named attributes, in preference order.
     * The {@code null} group name stands for the global attributes.
     */
    @Override
    public void setSearchPath(final String... groupNames) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Group[] groups = new Group[groupNames.length];
        int count = 0;
        for (final String name : groupNames) {
            if (name != null) {
                final Group group = file.findGroup(name);
                if (group == null) {
                    continue;                   // Group not found - do not increment the counter.
                }
                groups[count] = group;
            }
            count++;
        }
        this.groups = ArraysExt.resize(groups, count);
    }

    /**
     * Returns the path which is currently set. The array returned by this method may be only
     * a subset of the array given to {@link #setSearchPath(String[])} since only the name of
     * groups which have been found in the netCDF file are returned by this method.
     *
     * @return the current search path.
     */
    @Override
    public String[] getSearchPath() {
        final String[] path = new String[groups.length];
        for (int i=0; i<path.length; i++) {
            final Group group = groups[i];
            if (group != null) {
                path[i] = group.getShortName();
            }
        }
        return path;
    }

    /**
     * Returns the names of all global attributes found in the file.
     *
     * @return names of all global attributes in the file.
     */
    @Override
    public Collection<String> getAttributeNames() {
        return VariableWrapper.toNames(file.getGlobalAttributes());
    }

    /**
     * Returns the netCDF attribute of the given name in the given group, or {@code null} if none.
     * This method is invoked for every global and group attributes to be read by this class (but
     * not {@linkplain ucar.nc2.VariableSimpleIF variable} attributes), thus providing a single point
     * where we can filter the attributes to be read.
     *
     * <p>The {@code name} argument is typically (but is not restricted too) one of the constants
     * defined in the {@link org.apache.sis.storage.netcdf.AttributeNames} class.</p>
     *
     * @param  group  the group in which to search the attribute, or {@code null} for global attributes.
     * @param  name   the name of the attribute to search (cannot be null).
     * @return the attribute, or {@code null} if none.
     */
    private Attribute findAttribute(final Group group, final String name) {
        int index = 0;
        String mappedName;
        final Convention convention = convention();
        while ((mappedName = convention.mapAttributeName(name, index++)) != null) {
            Attribute value = (group != null) ? group.attributes().findAttributeIgnoreCase(mappedName)
                                              : file.findGlobalAttributeIgnoreCase(mappedName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Returns the value for the attribute of the given name, or {@code null} if none.
     * This method searches in the groups specified by the last call to {@link #setSearchPath(String[])}.
     * Null values and empty strings are ignored.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or empty or if the given name was null.
     */
    @Override
    public String stringValue(final String name) {
        if (name != null) {                                 // For createResponsibleParty(...) convenience.
            for (final Group group : groups) {
                final Attribute attribute = findAttribute(group, name);
                if (attribute != null && attribute.isString()) {
                    return Utils.nonEmpty(attribute.getStringValue());
                }
            }
        }
        return null;
    }

    /**
     * Returns the value of the attribute of the given name as a number, or {@code null} if none.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or unparsable or if the given name was null.
     */
    @Override
    public Number numericValue(final String name) {
        if (name != null) {
            for (final Group group : groups) {
                final Attribute attribute = findAttribute(group, name);
                if (attribute != null) {
                    final Number value = attribute.getNumericValue();
                    if (value != null) {
                        return Utils.fixSign(value, attribute.getDataType().isUnsigned());
                    }
                    String asString = Utils.nonEmpty(attribute.getStringValue());
                    if (asString != null) {
                        return parseNumber(name, asString);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the value of the attribute of the given name as a date, or {@code null} if none.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or unparsable or if the given name was null.
     */
    @Override
    public Temporal dateValue(final String name) {
        if (name != null) {
            for (final Group group : groups) {
                final Attribute attribute = findAttribute(group, name);
                if (attribute != null && attribute.isString()) {
                    String value = Utils.nonEmpty(attribute.getStringValue());
                    if (value != null) {
                        final CalendarDate date;
                        try {
                            date = CalendarDateFormatter.isoStringToCalendarDate(Calendar.proleptic_gregorian, value);
                        } catch (IllegalArgumentException e) {
                            listeners.warning(e);
                            continue;
                        }
                        return Instant.ofEpochMilli(date.getMillis());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Converts the given numerical values to date, using the information provided in the given unit symbol.
     * The unit symbol is typically a string like <q>days since 1970-01-01T00:00:00Z</q>.
     *
     * @param  values  the values to convert. May contains {@code null} elements.
     * @return the converted values. May contains {@code null} elements.
     */
    @Override
    public Temporal[] numberToDate(final String symbol, final Number... values) {
        final var dates = new Instant[values.length];
        final DateUnit unit;
        try {
            unit = new DateUnit(symbol);
        } catch (UnitException e) {
            listeners.warning(e);
            return dates;
        }
        for (int i=0; i<values.length; i++) {
            final Number value = values[i];
            if (value != null) {
                dates[i] = unit.makeDate(value.doubleValue()).toInstant();
            }
        }
        return dates;
    }

    /**
     * Returns the globally unique dataset identifier as determined by the UCAR library.
     *
     * @return the global dataset identifier, or {@code null} if none.
     */
    @Override
    public String getId() {
        return Utils.nonEmpty(file.getId());
    }

    /**
     * Returns the human readable title as determined by the UCAR library.
     *
     * @return the dataset title, or {@code null} if none.
     */
    @Override
    public String getTitle() {
        return Utils.nonEmpty(file.getTitle());
    }

    /**
     * Returns all variables found in the netCDF file.
     * This method returns a direct reference to an internal array - do not modify.
     *
     * @return all variables, or an empty array if none.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Variable[] getVariables() {
        if (variables == null) {
            final List<? extends ucar.nc2.Variable> all = file.getVariables();
            variables = new VariableWrapper[(all != null) ? all.size() : 0];
            for (int i=0; i<variables.length; i++) {
                variables[i] = new VariableWrapper(this, all.get(i));
            }
        }
        return variables;
    }

    /**
     * Returns the Apache SIS wrapper for the given UCAR variable. The given variable shall be non-null
     * and should be one of the variables wrapped by the instances returned by {@link #getVariables()}.
     *
     * @param  variable  the netCDF variable.
     * @return the SIS variable wrapping the given netCDF variable.
     */
    final VariableWrapper getWrapperFor(final ucar.nc2.Variable variable) {
        for (VariableWrapper c : (VariableWrapper[]) getVariables()) {
            if (c.isWrapperFor(variable)) {
                return c;
            }
        }
        // We should not reach this point, but let be safe.
        return new VariableWrapper(this, variable);
    }

    /**
     * Returns the set of features found by UCAR, or {@code null} if none.
     * May be an instance of {@link FeatureDataset} or {@link GridDataset} among others.
     *
     * <p>Note that invoking this method may be costly. It seems that the UCAR library
     * attemps to read at least the coordinate values of coordinate system axes.</p>
     */
    private FeatureDataset getFeatureDataSet() throws IOException {
        if (features == null && file instanceof NetcdfDataset) {
            features = FeatureDatasetFactoryManager.wrap(null, (NetcdfDataset) file, this,
                    new Formatter(new LogAdapter(listeners), getLocale()));
        }
        return features;
    }

    /**
     * If this decoder can handle the file content as features, returns handlers for them.
     *
     * @param  lock  the lock to use in {@code synchronized(lock)} statements.
     * @return a handler for the features, or an empty array if none.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if the library of geometric objects is not available.
     */
    @Override
    public DiscreteSampling[] getDiscreteSampling(final DataStore lock) throws IOException, DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final FeatureDataset features = getFeatureDataSet();
        if (features instanceof FeatureDatasetPoint) {
            final List<DsgFeatureCollection> fc = ((FeatureDatasetPoint) features).getPointFeatureCollectionList();
            if (fc != null && !fc.isEmpty()) {
                final FeaturesWrapper[] wrappers = new FeaturesWrapper[fc.size()];
                try {
                    for (int i=0; i<wrappers.length; i++) {
                        wrappers[i] = new FeaturesWrapper(fc.get(i), geomlib, listeners, lock);
                    }
                } catch (IllegalArgumentException e) {
                    throw new DataStoreException(e.getLocalizedMessage(), e);
                }
                return wrappers;
            }
        }
        /*
         * If the UCAR library did not recognized the features in this file, ask to SIS.
         */
        return super.getDiscreteSampling(lock);
    }

    /**
     * Returns all grid geometries (related to coordinate systems) found in the netCDF file.
     * This method returns a direct reference to an internal array - do not modify.
     *
     * <p>In the case of those wrappers, this method may return more grid geometries than
     * what the actual number of rasters (or data cubes) in the file. This is because an
     * {@linkplain VariableWrapper#findGrid additional filtering is done by the variable}.
     * Consequently, this method is not completely reliable for determining if the file
     * contains grids.</p>
     *
     * @return all grid geometries, or an empty array if none.
     * @throws IOException if an I/O operation was necessary but failed.
     */
    @Override
    @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
    public Grid[] getGridCandidates() throws IOException {
        if (geometries == null) {
            List<CoordinateSystem> systems = List.of();
            if (file instanceof NetcdfDataset) {
                /*
                 * We take all coordinate systems as associated to a grid. As an alternative,
                 * we tried to invoke `getFeatureDataSet()` and cast to UCAR `GridDataset`,
                 * but it causes the loading of large data for an end result often the same.
                 */
                final NetcdfDataset ds = (NetcdfDataset) file;
                systems = ds.getCoordinateSystems();
            }
            geometries = systems.stream().map(GridWrapper::new).toArray(Grid[]::new);
        }
        return geometries;
    }

    /**
     * Returns the dimension of the given name (eventually ignoring case), or {@code null} if none.
     * This method searches in all dimensions found in the netCDF file, regardless of variables.
     *
     * @param  dimName  the name of the dimension to search.
     * @return dimension of the given name, or {@code null} if none.
     */
    @Override
    protected Dimension findDimension(final String dimName) {
        final ucar.nc2.Dimension dimension = file.findDimension(dimName);
        return (dimension != null) ? new DimensionWrapper(dimension, -1) : null;
    }

    /**
     * Returns the netCDF variable of the given name, or {@code null} if none.
     *
     * @param  name  the name of the variable to search, or {@code null}.
     * @return the variable of the given name, or {@code null} if none.
     */
    @Override
    protected Variable findVariable(final String name) {
        final ucar.nc2.Variable v = file.findVariable(name);
        return (v != null) ? getWrapperFor(v) : null;
    }

    /**
     * Returns the variable or group of the given name.
     *
     * @param  name  name of the variable or group to search.
     * @return the variable or group of the given name, or {@code null} if none.
     */
    @Override
    protected Node findNode(final String name) {
        final ucar.nc2.Variable v = file.findVariable(name);
        if (v != null) {
            return getWrapperFor(v);
        }
        final Group group = file.findGroup(name);
        return (group != null) ? new GroupWrapper(this, group) : null;
    }

    /**
     * Adds netCDF attributes to the given node, including variables and sub-groups attributes.
     * Groups are shown first, then variables attributes, and finally global attributes.
     *
     * @param  root  the node where to add netCDF attributes.
     */
    @Override
    public void addAttributesTo(final TreeTable.Node root) {
        addAttributesTo(root, file.getRootGroup());
    }

    /**
     * Adds all attributes of the given group, then create nodes for sub-groups (if any).
     * This method invokes itself recursively.
     *
     * @param  branch  where to add new nodes for the children of given group.
     * @param  group   group for which to add sub-group, variables and attributes.
     */
    private void addAttributesTo(final TreeTable.Node branch, final Group group) {
        for (final Group sub : group.getGroups()) {
            final TreeTable.Node node = branch.newChild();
            node.setValue(TableColumn.NAME, sub.getShortName());
            addAttributesTo(node, sub);
        }
        for (final ucar.nc2.Variable variable : group.getVariables()) {
            final TreeTable.Node node = branch.newChild();
            node.setValue(TableColumn.NAME, variable.getShortName());
            addAttributesTo(node, variable.attributes());
        }
        addAttributesTo(branch, group.attributes());
    }

    /**
     * Adds the given attributes to the given node. This is used for building the tree
     * returned by {@link org.apache.sis.storage.netcdf.NetcdfStore#getNativeMetadata()}.
     * This tree is for information purpose only.
     *
     * @param  branch      where to add new nodes for the given attributes.
     * @param  attributes  the attributes to add to the specified branch.
     */
    private static void addAttributesTo(final TreeTable.Node branch, final Iterable<Attribute> attributes) {
        if (attributes != null) {
            for (final Attribute attribute : attributes) {
                final TreeTable.Node node = branch.newChild();
                node.setValue(TableColumn.NAME, attribute.getShortName());
                final int length = attribute.getLength();
                final Object value;
                switch (length) {
                    case 0: continue;
                    case 1: {
                        value = attribute.getValue(0);
                        break;
                    }
                    default: {
                        final Object[] values = new Object[length];
                        for (int i=0; i<length; i++) {
                            values[i] = attribute.getValue(i);
                        }
                        value = values;
                        break;
                    }
                }
                node.setValue(TableColumn.VALUE, value);
            }
        }
    }

    /**
     * Invoked by the UCAR netCDF library for checking if the reading process has been canceled.
     * This method returns the {@link #canceled} flag.
     *
     * @return the {@link #canceled} flag.
     */
    @Override
    public boolean isCancel() {
        return canceled;
    }

    /**
     * Invoked by the UCAR library during the reading process for progress information.
     *
     * @param  message   the message to show to the user.
     * @param  progress  count of progress, or -1 if unknown. This is not necessarily a percentage done.
     */
    @Override
    public void setProgress(final String message, final int progress) {
    }

    /**
     * Invoked by the UCAR netCDF library when an error occurred.
     *
     * @param  message  the error message.
     */
    @Override
    public void setError(final String message) {
        listeners.warning(message);
    }

    /**
     * Invoked by UCAR netCDF library when the operation completed, either successfully or with an error.
     *
     * @param  done  the completion status.
     */
    @Override
    public void setDone(final boolean done) {
        this.done = done;
    }

    /**
     * Returns {@code true} if the operation completed, either successfully or with an error.
     *
     * @return the completion status.
     */
    @Override
    public boolean isDone() {
        return done;
    }

    /**
     * Closes the netCDF file.
     *
     * @param  lock  the lock to use in {@code synchronized(lock)} statements.
     * @throws IOException if an error occurred while closing the file.
     */
    @Override
    public void close(final DataStore lock) throws IOException {
        synchronized (lock) {
            if (features != null) {
                features.close();
                features = null;
            }
            file.close();
        }
    }

    /**
     * Returns a string representation to be inserted in {@link org.apache.sis.storage.netcdf.NetcdfStore#toString()}
     * result. This is for debugging purpose only any may change in any future SIS version.
     */
    @Override
    public String toString() {
        return "UCAR driver: “" + getFilename() + '”';
    }
}
