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
package org.apache.sis.internal.metadata;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.xml.NilReason;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.metadata.InvalidMetadataException;
import org.apache.sis.internal.jaxb.PrimitiveTypeProperties;

import static org.apache.sis.metadata.iso.ISOMetadata.LOGGER;


/**
 * Miscellaneous utility methods for metadata.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final class MetadataUtilities extends Static {
    /**
     * The metadata standard name for ISO 19115-2.
     */
    public static final String STANDARD_NAME_2 = "ISO 19115-2 Geographic Information - Metadata Part 2 Extensions for imagery and gridded data";

    /**
     * The metadata standard version number for ISO 19115-2.
     */
    public static final String STANDARD_VERSION_2 = "ISO 19115-2:2009(E)";

    /**
     * Do not allow instantiation of this class.
     */
    private MetadataUtilities() {
    }

    /**
     * Returns the milliseconds value of the given date, or {@link Long#MIN_VALUE}
     * if the date us null.
     *
     * @param  value The date, or {@code null}.
     * @return The time in milliseconds, or {@code Long.MIN_VALUE} if none.
     */
    public static long toMilliseconds(final Date value) {
        return (value != null) ? value.getTime() : Long.MIN_VALUE;
    }

    /**
     * Returns the given milliseconds time to a date object, or returns null
     * if the given time is {@link Long#MIN_VALUE}.
     *
     * @param  value The time in milliseconds.
     * @return The date for the given milliseconds value, or {@code null}.
     */
    public static Date toDate(final long value) {
        return (value != Long.MIN_VALUE) ? new Date(value) : null;
    }

    /**
     * Makes sure that the given inclusion is non-nil, then returns its value.
     * If the given inclusion is {@code null}, then the default value is {@code true}.
     *
     * @param  value The {@link org.opengis.metadata.extent.GeographicBoundingBox#getInclusion()} value.
     * @return The given value as a primitive type.
     * @throws InvalidMetadataException if the given value is nil.
     */
    public static boolean getInclusion(final Boolean value) throws InvalidMetadataException {
        if (value == null) {
            return true;
        }
        final boolean p = value;
        if (p || value == Boolean.FALSE || !(PrimitiveTypeProperties.property(value) instanceof NilReason)) {
            return p;
        }
        throw new InvalidMetadataException(Errors.format(Errors.Keys.MissingValueForProperty_1, "inclusion"));
    }

    /**
     * Convenience method for logging a warning to the {@code ISOMetadata} logger.
     * The message will be produced using the {@link Messages} resources bundle.
     *
     * @param  caller    The public class which is invoking this method.
     * @param  method    The public method which is invoking this method.
     * @param  key       The key from the message resource bundle to use for creating a message.
     * @param  arguments The arguments to be used together with the key for building the message.
     */
    public static void warning(final Class<?> caller, final String method, final short key, final Object... arguments) {
        final LogRecord record = Messages.getResources(null).getLogRecord(Level.WARNING, key, arguments);
        record.setSourceClassName(caller.getCanonicalName());
        record.setSourceMethodName(method);
        record.setLoggerName(LOGGER.getName());
        LOGGER.log(record);
    }
}
