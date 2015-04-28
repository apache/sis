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
import org.apache.sis.xml.NilReason;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.InvalidMetadataException;
import org.apache.sis.internal.jaxb.PrimitiveTypeProperties;
import org.apache.sis.internal.jaxb.Context;


/**
 * Miscellaneous utility methods for metadata.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
public final class MetadataUtilities extends Static {
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
        // (value == Boolean.FALSE) is an optimization for a common case avoiding PrimitiveTypeProperties check.
        // DO NOT REPLACE BY 'equals' OR 'booleanValue()' - the exact reference value matter.
        if (p || value == Boolean.FALSE || !(PrimitiveTypeProperties.property(value) instanceof NilReason)) {
            return p;
        }
        throw new InvalidMetadataException(Errors.format(Errors.Keys.MissingValueForProperty_1, "inclusion"));
    }

    /**
     * Convenience method invoked when an argument was expected to be positive, but the user gave a negative value
     * or (in some case) zero. This method logs a warning if we are in process of (un)marshalling a XML document,
     * or throw an exception otherwise.
     *
     * <p><b>When to use:</b></p>
     * <ul>
     *   <li>This method is for setter methods that may be invoked by JAXB. Constructors or methods ignored
     *       by JAXB should use the simpler {@link org.apache.sis.util.ArgumentChecks} class instead.</li>
     *   <li>This method should be invoked only when ignoring the warning will not cause information lost.
     *       The stored metadata value may be invalid, but not lost.</li>
     * </ul>
     * <div class="note"><b>Note:</b> the later point is the reason why problems during XML (un)marshalling
     * are only warnings for this method, while they are errors by default for
     * {@link org.apache.sis.xml.ValueConverter} (the later can not store the value in case of error).</div>
     *
     * @param  classe   The caller class.
     * @param  property The property name. Method name will be inferred by the usual Java bean convention.
     * @param  strict   {@code true} if the value was expected to be strictly positive, or {@code false} if 0 is accepted.
     * @param  value    The invalid argument value.
     * @throws IllegalArgumentException if we are not (un)marshalling a XML document.
     */
    public static void warnNonPositiveArgument(final Class<?> classe, final String property, final boolean strict,
            final Number value) throws IllegalArgumentException
    {
        final String msg = logOrFormat(classe, property,
                strict ? Errors.Keys.ValueNotGreaterThanZero_2 : Errors.Keys.NegativeArgument_2, property, value);
        if (msg != null) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Convenience method invoked when an argument is outside the expected range of values. This method logs
     * a warning if we are in process of (un)marshalling a XML document, or throw an exception otherwise.
     *
     * <p><b>When to use:</b></p>
     * <ul>
     *   <li>This method is for setter methods that may be invoked by JAXB. Constructors or methods ignored
     *       by JAXB should use the simpler {@link org.apache.sis.util.ArgumentChecks} class instead.</li>
     *   <li>This method should be invoked only when ignoring the warning will not cause information lost.
     *       The stored metadata value may be invalid, but not lost.</li>
     * </ul>
     * <div class="note"><b>Note:</b> the later point is the reason why problems during XML (un)marshalling
     * are only warnings for this method, while they are errors by default for
     * {@link org.apache.sis.xml.ValueConverter} (the later can not store the value in case of error).</div>
     *
     * @param  classe   The caller class.
     * @param  property The property name. Method name will be inferred by the usual Java bean convention.
     * @param  minimum  The minimal legal value.
     * @param  maximum  The maximal legal value.
     * @param  value    The invalid argument value.
     * @throws IllegalArgumentException if we are not (un)marshalling a XML document.
     */
    public static void warnOutOfRangeArgument(final Class<?> classe, final String property,
            final Number minimum, final Number maximum, final Number value) throws IllegalArgumentException
    {
        final String msg = logOrFormat(classe, property, Errors.Keys.ValueOutOfRange_4, property, minimum, maximum, value);
        if (msg != null) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Formats an error message and logs it if we are (un)marshalling a document, or return the message otherwise.
     * In the later case, it is caller's responsibility to use the message for throwing an exception.
     *
     * @param  classe    The caller class, used only in case of warning message to log.
     * @param  property  The property name. Method name will be inferred by the usual Java bean convention.
     * @param  key       A {@code Errors.Keys} value.
     * @param  arguments The argument to use for formatting the error message.
     * @return {@code null} if the message has been logged, or the message to put in an exception otherwise.
     */
    private static String logOrFormat(final Class<?> classe, final String property, final short key, final Object... arguments) {
        final Context context = Context.current();
        if (context == null) {
            return Errors.format(key, arguments);
        } else {
            final StringBuilder buffer = new StringBuilder(property.length() + 3).append("set").append(property);
            buffer.setCharAt(3, Character.toUpperCase(buffer.charAt(3)));
            Context.warningOccured(context, ISOMetadata.LOGGER, classe, buffer.toString(), Errors.class, key, arguments);
            return null;
        }
    }
}
