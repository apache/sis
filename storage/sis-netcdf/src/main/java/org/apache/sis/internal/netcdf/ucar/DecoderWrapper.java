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

import java.util.Date;
import java.util.List;
import java.util.EnumSet;
import java.util.Formatter;
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
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.internal.netcdf.DiscreteSampling;


/**
 * Provides NetCDF decoding services based on the NetCDF library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
public final class DecoderWrapper extends Decoder implements CancelTask {
    /**
     * The NetCDF file to read.
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
    private transient Variable[] variables;

    /**
     * The discrete sampling features, or {@code null} if none.
     */
    private transient FeatureDataset features;

    /**
     * The grid geometries, computed when first needed.
     *
     * @see #getGridGeometries()
     */
    private transient GridGeometry[] geometries;

    /**
     * Creates a new decoder for the given NetCDF file. While this constructor accepts arbitrary
     * {@link NetcdfFile} instance, the {@link NetcdfDataset} subclass is necessary in order to
     * get coordinate system information.
     *
     * @param listeners  where to send the warnings.
     * @param file       the NetCDF file from which to read data.
     */
    public DecoderWrapper(final WarningListeners<?> listeners, final NetcdfFile file) {
        super(listeners);
        this.file = file;
    }

    /**
     * Creates a new decoder for the given filename.
     *
     * @param  listeners  where to send the warnings.
     * @param  filename   the name of the NetCDF file from which to read data.
     * @throws IOException if an error occurred while opening the NetCDF file.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public DecoderWrapper(final WarningListeners<?> listeners, final String filename) throws IOException {
        super(listeners);
        file = NetcdfDataset.openDataset(filename, false, this);
    }

    /**
     * Returns a filename for information purpose only. This is used for formatting error messages.
     *
     * @return a filename to report in warning or error messages.
     */
    @Override
    public String getFilename() {
        return file.getLocation();
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
     * groups which have been found in the NetCDF file are returned by this method.
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
     * Returns the NetCDF attribute of the given name in the given group, or {@code null} if none.
     * This method is invoked for every global and group attributes to be read by this class (but
     * not {@linkplain VariableSimpleIF variable} attributes), thus providing a single point where
     * we can filter the attributes to be read - if we want to do that in a future version.
     *
     * <p>The {@code name} argument is typically (but is not restricted too) one of the constants
     * defined in the {@link AttributeNames} class.</p>
     *
     * @param  group  the group in which to search the attribute, or {@code null} for global attributes.
     * @param  name   the name of the attribute to search (can not be null).
     * @return the attribute, or {@code null} if none.
     */
    private Attribute findAttribute(final Group group, final String name) {
        return (group != null) ? group.findAttributeIgnoreCase(name) : file.findGlobalAttributeIgnoreCase(name);
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
                    String value = attribute.getStringValue();
                    if (value != null && !(value = value.trim()).isEmpty()) {
                        return value;
                    }
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
                        return value;
                    }
                    String asString = attribute.getStringValue();
                    if (asString != null && !(asString = asString.trim()).isEmpty()) {
                        return parseNumber(asString);
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
                    String value = attribute.getStringValue();
                    if (value != null && !(value = value.trim()).isEmpty()) {
                        final CalendarDate date;
                        try {
                            date = CalendarDateFormatter.isoStringToCalendarDate(Calendar.proleptic_gregorian, value);
                        } catch (IllegalArgumentException e) {
                            listeners.warning(null, e);
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
        } catch (Exception e) { // Declared by the DateUnit constructor.
            listeners.warning(null, e);
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
        return file.getId();
    }

    /**
     * Returns the human readable title as determined by the UCAR library.
     *
     * @return the dataset title, or {@code null} if none.
     */
    @Override
    public String getTitle() {
        return file.getTitle();
    }

    /**
     * Returns all variables found in the NetCDF file.
     * This method returns a direct reference to an internal array - do not modify.
     *
     * @return all variables, or an empty array if none.
     */
    @Override
    @SuppressWarnings({"ReturnOfCollectionOrArrayField", "null"})
    public Variable[] getVariables() {
        if (variables == null) {
            final List<? extends VariableIF> all = file.getVariables();
            variables = new Variable[(all != null) ? all.size() : 0];
            for (int i=0; i<variables.length; i++) {
                variables[i] = new VariableWrapper(all.get(i));
            }
        }
        return variables;
    }

    /**
     * If this decoder can handle the file content as features, returns handlers for them.
     *
     * @return {@inheritDoc}
     * @throws IOException if an I/O operation was necessary but failed.
     */
    @Override
    @SuppressWarnings("null")
    public DiscreteSampling[] getDiscreteSampling() throws IOException {
        if (features == null && file instanceof NetcdfDataset) {
            features = FeatureDatasetFactoryManager.wrap(null, (NetcdfDataset) file, this,
                    new Formatter(new LogAdapter(listeners), listeners.getLocale()));
        }
        List<FeatureCollection> fc = null;
        if (features instanceof FeatureDatasetPoint) {
            fc = ((FeatureDatasetPoint) features).getPointFeatureCollectionList();
        }
        final FeaturesWrapper[] wrappers = new FeaturesWrapper[(fc != null) ? fc.size() : 0];
        for (int i=0; i<wrappers.length; i++) {
            wrappers[i] = new FeaturesWrapper(fc.get(i));
        }
        return wrappers;
    }

    /**
     * Returns all grid geometries (related to coordinate systems) found in the NetCDF file.
     * This method returns a direct reference to an internal array - do not modify.
     *
     * @return all grid geometries, or an empty array if none.
     * @throws IOException if an I/O operation was necessary but failed.
     */
    @Override
    @SuppressWarnings({"ReturnOfCollectionOrArrayField", "null"})
    public GridGeometry[] getGridGeometries() throws IOException {
        if (geometries == null) {
            List<CoordinateSystem> systems = null;
            if (file instanceof NetcdfDataset) {
                final NetcdfDataset ds = (NetcdfDataset) file;
                final EnumSet<NetcdfDataset.Enhance> mode = EnumSet.copyOf(ds.getEnhanceMode());
                if (mode.add(NetcdfDataset.Enhance.CoordSystems)) {
                    ds.enhance(mode);
                }
                systems = ds.getCoordinateSystems();
            }
            geometries = new GridGeometry[(systems != null) ? systems.size() : 0];
            for (int i=0; i<geometries.length; i++) {
                geometries[i] = new GridGeometryWrapper(systems.get(i));
            }
        }
        return geometries;
    }

    /**
     * Invoked by the UCAR NetCDF library for checking if the reading process has been canceled.
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
     * Invoked by the UCAR NetCDF library when an error occurred.
     *
     * @param  message  the error message.
     */
    @Override
    public void setError(final String message) {
        listeners.warning(message, null);
    }

    /**
     * Closes the NetCDF file.
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
    @Debug
    @Override
    public String toString() {
        return "UCAR driver: “" + getFilename() + '”';
    }
}
