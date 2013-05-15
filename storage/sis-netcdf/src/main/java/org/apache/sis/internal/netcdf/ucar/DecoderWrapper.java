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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.io.IOException;
import ucar.nc2.Group;
import ucar.nc2.Attribute;
import ucar.nc2.VariableIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.units.DateUnit;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.internal.netcdf.WarningProducer;


/**
 * Provides NetCDF decoding services based on the NetCDF library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.20)
 * @version 0.3
 * @module
 */
public final class DecoderWrapper extends Decoder {
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
     * Creates a new decoder for the given NetCDF file. While this constructor accepts arbitrary
     * {@link NetcdfFile} instance, the {@link NetcdfDataset} subclass is necessary in order to
     * get coordinate system information.
     *
     * @param parent Where to send the warnings, or {@code null} if none.
     * @param file The NetCDF file from which to parse metadata.
     */
    public DecoderWrapper(final WarningProducer parent, final NetcdfFile file) {
        super(parent);
        this.file = file;
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
                    continue; // Group not found - do not increment the counter.
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
     * Returns the NetCDF attribute of the given name in the given group, or {@code null} if none.
     * This method is invoked for every global and group attributes to be read by this class (but
     * not {@linkplain VariableSimpleIF variable} attributes), thus providing a single point where
     * we can filter the attributes to be read - if we want to do that in a future version.
     *
     * <p>The {@code name} argument is typically (but is not restricted too) one of the constants
     * defined in the {@link AttributeNames} class.</p>
     *
     * @param  group The group in which to search the attribute, or {@code null} for global attributes.
     * @param  name  The name of the attribute to search (can not be null).
     * @return The attribute, or {@code null} if none.
     */
    private Attribute findAttribute(final Group group, final String name) {
        return (group != null) ? group.findAttributeIgnoreCase(name) : file.findGlobalAttributeIgnoreCase(name);
    }

    /**
     * Returns the value for the attribute of the given name, or {@code null} if none.
     * This method searches in the groups specified by the last call to {@link #setSearchPath(String[])}.
     * Null values and empty strings are ignored.
     *
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute value, or {@code null} if none or empty or if the given name was null.
     */
    @Override
    public String stringValue(final String name) {
        if (name != null) { // For createResponsibleParty(...) convenience.
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
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute value, or {@code null} if none or unparseable or if the given name was null.
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
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute value, or {@code null} if none or unparseable or if the given name was null.
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
                            warning("dateValue", e);
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
     * The unit symbol is typically a string like "<cite>days since 1970-01-01T00:00:00Z</cite>".
     *
     * @param  values The values to convert. May contains {@code null} elements.
     * @return The converted values. May contains {@code null} elements.
     */
    @Override
    public Date[] numberToDate(final String symbol, final Number... values) {
        final Date[] dates = new Date[values.length];
        final DateUnit unit;
        try {
            unit = new DateUnit(symbol);
        } catch (Exception e) { // Declared by the DateUnit constructor.
            warning("numberToDate", e);
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
     */
    @Override
    public String getId() {
        return file.getId();
    }

    /**
     * Returns the human readable title as determined by the UCAR library.
     */
    @Override
    public String getTitle() {
        return file.getTitle();
    }

    /**
     * Returns all variables found in the NetCDF file.
     */
    @Override
    public List<Variable> getVariables() {
        final List<? extends VariableIF> all = file.getVariables();
        if (all == null) {
            return Collections.emptyList();
        }
        final List<Variable> variables = new ArrayList<>(all.size());
        for (final VariableIF variable : all) {
            variables.add(new VariableWrapper(variable, all));
        }
        return variables;
    }

    /**
     * Returns all grid geometries (related to coordinate systems) found in the NetCDF file.
     */
    @Override
    public List<GridGeometry> getGridGeometries() throws IOException {
        if (file instanceof NetcdfDataset) {
            final NetcdfDataset ds = (NetcdfDataset) file;
            final EnumSet<NetcdfDataset.Enhance> mode = EnumSet.copyOf(ds.getEnhanceMode());
            if (mode.add(NetcdfDataset.Enhance.CoordSystems)) {
                ds.enhance(mode);
            }
            final List<CoordinateSystem> systems = ds.getCoordinateSystems();
            if (systems != null) {
                final List<GridGeometry> geometries = new ArrayList<>(systems.size());
                for (final CoordinateSystem cs : systems) {
                    geometries.add(new GridGeometryWrapper(this, cs));
                }
                return geometries;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Closes the NetCDF file.
     *
     * @throws IOException If an error occurred while closing the file.
     */
    @Override
    public void close() throws IOException {
        file.close();
    }
}
