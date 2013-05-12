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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.IOException;
import javax.measure.unit.Unit;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.logging.Logging;


/**
 * The API used internally by Apache SIS for fetching all data from NetCDF files.
 * We use this API for isolating Apache SIS from the library used for reading the
 * NetCDF file: it can be either the UCAR library, or our own internal library.
 *
 * <p>We do not use systematically the UCAR library because it is quite large (especially when including
 * all dependencies) while SIS uses only a fraction of it. This is because the UCAR library provides some
 * features like referencing services which overlap with SIS services. In addition, SIS often needs "raw"
 * data instead of "high level" data. For example we need the minimal and maximal values of a variable in
 * its raw format, while the UCAR high level API provides the values converted by the offset and scale
 * factors.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
abstract class Decoder {
    /**
     * Creates a new decoder.
     */
    Decoder() {
    }

    /**
     * Reports a warning. The current implementation just logs the warning. However if we want
     * to implement a listener mechanism in a future version, this could be done here.
     *
     * @param method    The method in which the warning occurred.
     * @param exception The exception to log.
     */
    static void warning(final String method, final Exception exception) {
        final LogRecord record = new LogRecord(Level.WARNING, Exceptions.formatChainedMessages(null, null, exception));
        Logging.log(Decoder.class, method, record);
    }

    /**
     * Defines the groups where to search for named attributes, in preference order.
     * The {@code null} group name stands for the global attributes.
     *
     * @param  groupNames The name of the group where to search, in preference order.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    public abstract void setSearchPath(final String... groupNames) throws IOException;

    /**
     * Returns the path which is currently set. The array returned by this method may be only
     * a subset of the array given to {@link #setSearchPath(String[])} since only the name of
     * groups which have been found in the NetCDF file are returned by this method.
     *
     * @throws IOException If an I/O operation was necessary but failed.
     */
    public abstract String[] getSearchPath() throws IOException;

    /**
     * Returns the value for the attribute of the given name, or {@code null} if none.
     * This method searches in the groups specified by the last call to {@link #setSearchPath(String[])}.
     * Null values and empty strings are ignored.
     *
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute value, or {@code null} if none or empty or if the given name was null.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    public abstract String stringValue(final String name) throws IOException;

    /**
     * Returns the value of the attribute of the given name as a number, or {@code null} if none.
     *
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute value, or {@code null} if none or unparseable or if the given name was null.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    public abstract Number numericValue(final String name) throws IOException;

    /**
     * Returns the value of the attribute of the given name as a date, or {@code null} if none.
     *
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute value, or {@code null} if none or unparseable or if the given name was null.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    public abstract Date dateValue(final String name) throws IOException;

    /**
     * Returns the value of the attribute of the given name as a unit of measurement, or {@code null} if none.
     *
     * @param  name The name of the attribute to search, or {@code null}.
     * @return The attribute value, or {@code null} if none or unparseable or if the given name was null.
     * @throws IOException If an I/O operation was necessary but failed.
     *
     * @todo Current Units.valueOf(String) implementation ignore direction in "degrees_east" or "degrees_west".
     *       We may need to take that in account (with "degrees_west" to "degrees_east" converter that reverse
     *       the sign).
     */
    public final Unit<?> unitValue(final String name) throws IOException {
        final String unit = stringValue(name);
        if (unit != null) try {
            return Units.valueOf(unit);
        } catch (IllegalArgumentException e) {
            warning("unitValue", e);
        }
        return null;
    }

    /**
     * Returns the value of the {@code "_Id"} global attribute. The UCAR library defines a
     * {@link ucar.nc2.NetcdfFile#getId()} method for that purpose, which we will use when
     * possible in case that {@code getId()} method is defined in an other way.
     *
     * @throws IOException If an I/O operation was necessary but failed.
     */
    public String getId() throws IOException {
        return stringValue("_Id");
    }

    /**
     * Returns the value of the {@code "_Title"} global attribute. The UCAR library defines a
     * {@link ucar.nc2.NetcdfFile#getTitle()} method for that purpose, which we will use when
     * possible in case that {@code getTitle()} method is defined in an other way.
     *
     * @throws IOException If an I/O operation was necessary but failed.
     */
    public String getTitle() throws IOException {
        return stringValue("_Title");
    }
}
