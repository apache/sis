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
package org.apache.sis.internal.util;

import java.util.Date;
import org.apache.sis.internal.geoapi.temporal.Instant;
import org.apache.sis.internal.geoapi.temporal.Period;
import org.apache.sis.internal.geoapi.temporal.TemporalFactory;
import org.opengis.temporal.TemporalPrimitive;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.DefaultFactories;


/**
 * Utilities related to ISO 19108 objects. This class may disappear after we reviewed
 * the GeoAPI-pending temporal interfaces.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final class TemporalUtilities extends Static {
    /**
     * {@code true} if the SIS library should log the "This operation requires the sis-temporal module" warning.
     * This flag can be {@code true} during development phase, but should be set to {@code false} in SIS releases
     * until we can really provide a sis-temporal module.
     *
     * This constant will be removed after SIS release a sis-temporal module.
     *
     * @see <a href="http://sis.apache.org/source.html#master">Differences between SIS master and branches</a>
     */
    public static final boolean REPORT_MISSING_MODULE = false;

    /**
     * Do not allow instantiation of this class.
     */
    private TemporalUtilities() {
    }

    /**
     * Returns a temporal factory if available.
     *
     * @return the temporal factory.
     * @throws UnsupportedOperationException if the temporal factory is not available on the classpath.
     */
    public static TemporalFactory getTemporalFactory() throws UnsupportedOperationException {
        final TemporalFactory factory = DefaultFactories.forClass(TemporalFactory.class);
        if (factory != null) {
            return factory;
        }
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.MissingRequiredModule_1, "sis-temporal"));
    }

    /**
     * Creates an instant for the given date using the given factory.
     */
    private static Instant createInstant(final TemporalFactory factory, final Date date) {
        return factory.createInstant(date);
    }

    /**
     * Creates an instant for the given date.
     *
     * @param  time  the date for which to create instant, or {@code null}.
     * @return the instant, or {@code null} if the given time was null.
     * @throws UnsupportedOperationException if the temporal factory is not available on the classpath.
     */
    public static Instant createInstant(final Date time) throws UnsupportedOperationException {
        return (time != null) ? createInstant(getTemporalFactory(), time) : null;
    }

    /**
     * Creates a period for the given begin and end dates. The given arguments can be null if the
     * {@link TemporalFactory#createInstant(Date)} method accepts null dates, which stand for
     * undetermined position.
     *
     * @param  begin  the begin date, inclusive.
     * @param  end    the end date, inclusive.
     * @return the period.
     * @throws UnsupportedOperationException if the temporal factory is not available on the classpath.
     */
    public static Period createPeriod(final Date begin, final Date end) throws UnsupportedOperationException {
        final TemporalFactory factory = getTemporalFactory();
        return factory.createPeriod(createInstant(factory, begin), createInstant(factory, end));
    }

    /**
     * Infers a value from the extent as a {@link Date} object.
     * This method is used for compatibility with legacy API and may disappear in future SIS version.
     *
     * @param  time  the instant or period for which to get a date, or {@code null}.
     * @return the requested time as a Java date, or {@code null} if none.
     *
     * @since 1.0
     */
    public static Date getDate(final TemporalPrimitive time) {
        Instant instant;
        if (time instanceof Instant) {
            instant = (Instant) time;
        } else if (time instanceof Period) {
            instant = ((Period) time).getEnding();
            if (instant == null) {
                instant = ((Period) time).getBeginning();
            }
        } else {
            return null;
        }
        return instant.getDate();
    }
}
