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

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.GregorianCalendar;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.util.StandardDateFormat;


/**
 * Handles particularity of HYCOM format. It is not yet clear whether those particularities are used elsewhere or not.
 * We handle them in a separated class for now and may refactor later in a more general mechanism for providing extensions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-315">SIS-315</a>
 *
 * @since 1.0
 * @module
 */
final class HYCOM {
    /**
     * The pattern to use for identifying temporal units of the form "day as %Y%m%d.%f".
     * "%Y" is year formatted as at least four digits, "%m" is month formatted as two digits,
     * and "%d" is day of month formatted as two digits.
     *
     * Example: 20181017.0000 for 2018-10-17.
     */
    private static final Pattern DATE_PATTERN = Pattern.compile("days?\\s+as\\s+(?-i)%Y%m%d.*", Pattern.CASE_INSENSITIVE);

    /**
     * Do not allow instantiation of this class.
     */
    private HYCOM() {
    }

    /**
     * If any variable uses the "day as %Y%m%d.%f" pseudo-units, converts to a number of days since the epoch.
     * The epoch is taken from the unit of the dimension. Example of netCDF file header:
     *
     * {@preformat text
     *     dimensions:
     *         MT = UNLIMITED ; // (1 currently)
     *         Y = 3298 ;
     *         X = 4500 ;
     *     variables:
     *         double MT(MT) ;
     *             MT:long_name = "time" ;
     *             MT:units = "days since 1900-12-31 00:00:00" ;
     *             MT:calendar = "standard" ;
     *             MT:axis = "T" ;
     *         double Date(MT) ;
     *             Date:long_name = "date" ;
     *             Date:units = "day as %Y%m%d.%f" ;
     *             Date:C_format = "%13.4f" ;
     *             Date:FORTRAN_format = "(f13.4)" ;
     *     data:
     *         MT = 43024 ;
     *         Date = 20181017.0000 ;
     * }
     *
     * In this example, the real units of {@code Date(MT)} will be taken from {@code MT(MT)}, which is
     * "days since 1900-12-31 00:00:00".
     */
    static void convert(final Decoder decoder, final Variable[] variables) throws IOException, DataStoreException {
        Matcher matcher = null;
        for (final Variable variable : variables) {
            if (variable.getNumDimensions() == 1) {
                final String units = variable.getUnitsString();
                if (units != null) {
                    if (matcher == null) {
                        matcher = DATE_PATTERN.matcher(units);
                    } else {
                        matcher.reset(units);
                    }
                    if (matcher.matches()) {
                        final Dimension dimension = variable.getGridDimensions().get(0);
                        Instant epoch = variable.setUnit(decoder.findVariable(dimension.getName()), Units.DAY);
                        if (epoch == null) {
                            epoch = Instant.EPOCH;
                        }
                        final long origin = epoch.toEpochMilli();
                        /*
                         * Convert all dates into numbers of days since the epoch.
                         */
                        Vector values = variable.read();
                        final double[] times = new double[values.size()];
                        final GregorianCalendar calendar = new GregorianCalendar(decoder.getTimeZone(), Locale.US);
                        calendar.clear();
                        for (int i=0; i<times.length; i++) {
                            double time = values.doubleValue(i);                            // Date encoded as a double (e.g. 20181017)
                            long date = (long) time;                                        // Round toward zero.
                            time -= date;                                                   // Fractional part of the day.
                            int day   = (int) (date % 100); date /= 100;
                            int month = (int) (date % 100); date /= 100;
                            calendar.set(Math.toIntExact(date), month - 1, day, 0, 0, 0);
                            date = calendar.getTimeInMillis() - origin;                     // Milliseconds since epoch.
                            time += date / (double) StandardDateFormat.MILLISECONDS_PER_DAY;
                            times[i] = time;
                        }
                        variable.setValues(times);
                    }
                }
            }
        }
    }
}
