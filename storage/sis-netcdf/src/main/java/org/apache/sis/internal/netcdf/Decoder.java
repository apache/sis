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

import java.util.Date;
import java.util.Objects;
import java.util.Collection;
import java.io.Closeable;
import java.io.IOException;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.WarningListeners;


/**
 * The API used internally by Apache SIS for fetching variables and attribute values from a NetCDF file.
 *
 * <p>This {@code Decoder} class and subclasses are <strong>not</strong> thread-safe.
 * Synchronizations are caller's responsibility.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
public abstract class Decoder implements Closeable {
    /**
     * Where to send the warnings.
     */
    public final WarningListeners<DataStore> listeners;

    /**
     * Sets to {@code true} for canceling a reading process.
     * This flag is honored on a <cite>best effort</cite> basis only.
     */
    public volatile boolean canceled;

    /**
     * Creates a new decoder.
     *
     * @param  listeners  where to send the warnings.
     */
    protected Decoder(final WarningListeners<DataStore> listeners) {
        Objects.requireNonNull(listeners);
        this.listeners = listeners;
    }

    /**
     * Returns a filename for information purpose only. This is used for formatting error messages.
     *
     * @return a filename to report in warning or error messages.
     */
    public abstract String getFilename();

    /**
     * Defines the groups where to search for named attributes, in preference order.
     * The {@code null} group name stands for the global attributes.
     *
     * @param  groupNames  the name of the group where to search, in preference order.
     */
    public abstract void setSearchPath(final String... groupNames);

    /**
     * Returns the path which is currently set. The array returned by this method may be only
     * a subset of the array given to {@link #setSearchPath(String[])} since only the name of
     * groups which have been found in the NetCDF file are returned by this method.
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
    public abstract String stringValue(final String name);

    /**
     * Returns the value of the attribute of the given name as a number, or {@code null} if none.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or unparsable or if the given name was null.
     */
    public abstract Number numericValue(final String name);

    /**
     * Convenience method for {@link #numericValue(String)} implementation.
     *
     * @param  value  the attribute value to parse.
     * @return the parsed attribute value, or {@code null} if the given value can not be parsed.
     */
    protected final Number parseNumber(String value) {
        final int s = value.indexOf(' ');
        if (s >= 0) {
            /*
             * Sometime, numeric values as string are followed by
             * a unit of measurement. We ignore that unit for now...
             */
            value = value.substring(0, s);
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            listeners.warning(null, e);
        }
        return null;
    }

    /**
     * Returns the value of the attribute of the given name as a date, or {@code null} if none.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or unparsable or if the given name was null.
     */
    public abstract Date dateValue(final String name);

    /**
     * Returns the value of the attribute of the given name as a unit of measurement, or {@code null} if none.
     *
     * @param  name  the name of the attribute to search, or {@code null}.
     * @return the attribute value, or {@code null} if none or unparsable or if the given name was null.
     *
     * @todo Current Units.valueOf(String) implementation ignore direction in "degrees_east" or "degrees_west".
     *       We may need to take that in account (with "degrees_west" to "degrees_east" converter that reverse
     *       the sign).
     */
    public final Unit<?> unitValue(final String name) {
        final String unit = stringValue(name);
        if (unit != null) try {
            return Units.valueOf(unit);
        } catch (ParserException e) {
            listeners.warning(null, e);
        }
        return null;
    }

    /**
     * Converts the given numerical values to date, using the information provided in the given unit symbol.
     * The unit symbol is typically a string like <cite>"days since 1970-01-01T00:00:00Z"</cite>.
     *
     * @param  symbol  the temporal unit name or symbol, followed by the epoch.
     * @param  values  the values to convert. May contains {@code null} elements.
     * @return the converted values. May contains {@code null} elements.
     */
    public abstract Date[] numberToDate(final String symbol, final Number... values);

    /**
     * Returns the value of the {@code "_Id"} global attribute. The UCAR library defines a
     * {@link ucar.nc2.NetcdfFile#getId()} method for that purpose, which we will use when
     * possible in case that {@code getId()} method is defined in an other way.
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
     * possible in case that {@code getTitle()} method is defined in an other way.
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
     * Returns all variables found in the NetCDF file.
     * This method may return a direct reference to an internal array - do not modify.
     *
     * @return all variables, or an empty array if none.
     */
    public abstract Variable[] getVariables();

    /**
     * If the file contains features encoded as discrete sampling (for example profiles or trajectories),
     * returns objects for handling them.
     * This method may return a direct reference to an internal array - do not modify.
     *
     * @return a handler for the features, or an empty array if none.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     */
    public abstract DiscreteSampling[] getDiscreteSampling() throws IOException, DataStoreException;

    /**
     * Returns all grid geometries (related to coordinate systems) found in the NetCDF file.
     * This method may return a direct reference to an internal array - do not modify.
     *
     * @return all grid geometries, or an empty array if none.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     */
    public abstract GridGeometry[] getGridGeometries() throws IOException, DataStoreException;
}
