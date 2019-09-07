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
package org.apache.sis.internal.netcdf.ucar;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;
import ucar.nc2.Group;
import ucar.nc2.Attribute;
import ucar.nc2.VariableIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.util.CancelTask;
import ucar.nc2.units.DateUnit;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureCollection;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.netcdf.Convention;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.Node;
import org.apache.sis.internal.netcdf.Grid;
import org.apache.sis.internal.netcdf.DiscreteSampling;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;


/**
 * Provides netCDF decoding services based on the netCDF library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final class DecoderWrapper extends Decoder implements CancelTask {
    /**
     * The netCDF file to read.
     * This file is set at construction time.
     *
     * <p>This {@code DecoderWrapper} class does <strong>not</strong> close this file.
     * Closing this file after usage is the user responsibility.</p>
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
     * The discrete sampling features, or {@code null} if none.
     */
    private transient FeatureDataset features;

    /**
     * The grid geometries, computed when first needed.
     *
     * @see #getGrids()
     */
    private transient Grid[] geometries;

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
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public DecoderWrapper(final String filename, final GeometryLibrary geomlib, final StoreListeners listeners)
            throws IOException
    {
        super(geomlib, listeners);
        final NetcdfDataset ds = NetcdfDataset.openDataset(filename, false, this);
        ds.enhance(Collections.singleton(NetcdfDataset.Enhance.CoordSystems));
        file = ds;
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
     *
     * @return identification of the file format, human-readable description and version number.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public String[] getFormatDescription() {
        final String version = Utils.nonEmpty(file.getFileTypeVersion());
        final String[] format = new String[version != null ? 3 : 2];
        switch (format.length) {
            default: format[2] = version;                           // Fallthrough everywhere.
            case 2:  format[1] = file.getFileTypeDescription();
            case 1:  format[0] = file.getFileTypeId();
            case 0:  break;                                         // As a matter of principle.
        }
        return format;
    }

    /**
     * Defines the groups where to search for named attributes, in preference order.
     * The {@code null} group name stands for the global attributes.
     */
    @Override
    public void setSearchPath(final String... groupNames) {
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
     * @param  name   the name of the attribute to search (can not be null).
     * @return the attribute, or {@code null} if none.
     */
    private Attribute findAttribute(final Group group, final String name) {
        Attribute value = (group != null) ? group.findAttributeIgnoreCase(name)
                                          : file.findGlobalAttributeIgnoreCase(name);
        if (value == null) {
            final String mappedName = convention().mapAttributeName(name);
            /*
             * Identity check between String instances below is okay
             * since this is only an optimization for a common case.
             */
            if (mappedName != name) {
                value = (group != null) ? group.findAttributeIgnoreCase(mappedName)
                                        : file.findGlobalAttributeIgnoreCase(mappedName);
            }
        }
        return value;
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
                        return Utils.fixSign(value, attribute.isUnsigned());
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
    public Date dateValue(final String name) {
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
                        return new Date(date.getMillis());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Converts the given numerical values to date, using the information provided in the given unit symbol.
     * The unit symbol is typically a string like <cite>"days since 1970-01-01T00:00:00Z"</cite>.
     *
     * @param  values  the values to convert. May contains {@code null} elements.
     * @return the converted values. May contains {@code null} elements.
     */
    @Override
    public Date[] numberToDate(final String symbol, final Number... values) {
        final Date[] dates = new Date[values.length];
        final DateUnit unit;
        try {
            unit = new DateUnit(symbol);
        } catch (Exception e) {                 // Declared by the DateUnit constructor.
            listeners.warning(e);
            return dates;
        }
        for (int i=0; i<values.length; i++) {
            final Number value = values[i];
            if (value != null) {
                dates[i] = unit.makeDate(value.doubleValue());
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
    @SuppressWarnings({"ReturnOfCollectionOrArrayField", "null"})
    public Variable[] getVariables() {
        if (variables == null) {
            final List<? extends VariableIF> all = file.getVariables();
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
     */
    final VariableWrapper getWrapperFor(final VariableIF variable) {
        for (VariableWrapper c : (VariableWrapper[]) getVariables()) {
            if (c.isWrapperFor(variable)) {
                return c;
            }
        }
        // We should not reach this point, but let be safe.
        return new VariableWrapper(this, variable);
    }

    /**
     * If this decoder can handle the file content as features, returns handlers for them.
     *
     * @return {@inheritDoc}
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if the library of geometric objects is not available.
     */
    @Override
    @SuppressWarnings("null")
    public DiscreteSampling[] getDiscreteSampling() throws IOException, DataStoreException {
        if (features == null && file instanceof NetcdfDataset) {
            features = FeatureDatasetFactoryManager.wrap(null, (NetcdfDataset) file, this,
                    new Formatter(new LogAdapter(listeners), listeners.getLocale()));
        }
        List<FeatureCollection> fc = null;
        if (features instanceof FeatureDatasetPoint) {
            fc = ((FeatureDatasetPoint) features).getPointFeatureCollectionList();
        }
        final FeaturesWrapper[] wrappers = new FeaturesWrapper[(fc != null) ? fc.size() : 0];
        try {
            for (int i=0; i<wrappers.length; i++) {
                wrappers[i] = new FeaturesWrapper(fc.get(i), geomlib, listeners);
            }
        } catch (IllegalArgumentException e) {
            throw new DataStoreException(e.getLocalizedMessage(), e);
        }
        return wrappers;
    }

    /**
     * Returns all grid geometries (related to coordinate systems) found in the netCDF file.
     * This method returns a direct reference to an internal array - do not modify.
     *
     * @return all grid geometries, or an empty array if none.
     * @throws IOException if an I/O operation was necessary but failed.
     */
    @Override
    @SuppressWarnings({"ReturnOfCollectionOrArrayField", "null"})
    public Grid[] getGrids() throws IOException {
        if (geometries == null) {
            List<CoordinateSystem> systems = null;
            if (file instanceof NetcdfDataset) {
                final NetcdfDataset ds = (NetcdfDataset) file;
                final EnumSet<NetcdfDataset.Enhance> mode = EnumSet.copyOf(ds.getEnhanceMode());
                if (mode.add(NetcdfDataset.Enhance.CoordSystems)) {
                    /*
                     * Should not happen with NetcdfDataset opened by the constructor expecting a filename,
                     * because that constructor already enhanced the dataset. It may happen however if the
                     * NetcdfDataset was given explicitly by the user. We try to increase the chances to
                     * get information we need, but it is not guaranteed to work; it may be too late if we
                     * already started to use the NetcdfDataset before this point.
                     */
                    ds.enhance(mode);
                }
                systems = ds.getCoordinateSystems();
                /*
                 * If the UCAR library does not see any coordinate system in the file, verify if there is
                 * a custom convention recognizing the axes. CSBuilderFallback uses the mechanism defined
                 * by Apache SIS for determining variable role.
                 */
                if (systems.isEmpty() && convention() != Convention.DEFAULT) {
                    final CSBuilderFallback builder = new CSBuilderFallback(this);
                    builder.buildCoordinateSystems(ds);
                    systems = ds.getCoordinateSystems();
                }
            }
            geometries = new Grid[(systems != null) ? systems.size() : 0];
            for (int i=0; i<geometries.length; i++) {
                geometries[i] = new GridWrapper(systems.get(i));
            }
        }
        return geometries;
    }

    /**
     * Returns the variable or group of the given name.
     *
     * @param  name  name of the variable or group to search.
     * @return the variable or group of the given name, or {@code null} if none.
     */
    @Override
    protected Node findNode(final String name) {
        final VariableIF v = file.findVariable(name);
        if (v != null) {
            return getWrapperFor(v);
        }
        final Group group = file.findGroup(name);
        return (group != null) ? new GroupWrapper(this, group) : null;
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
     * Closes the netCDF file.
     *
     * @throws IOException if an error occurred while closing the file.
     */
    @Override
    public void close() throws IOException {
        if (features != null) {
            features.close();
            features = null;
        }
        file.close();
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
