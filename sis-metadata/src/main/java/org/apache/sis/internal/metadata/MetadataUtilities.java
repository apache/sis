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
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.metadata.InvalidMetadataException;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


/**
 * Miscellaneous utility methods for metadata.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
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
     * Sets the bit under the given mask for the given boolean value.
     * This method uses two bits as below:
     *
     * <ul>
     *   <li>{@code 00} - {@code null}</li>
     *   <li>{@code 10} - {@code Boolean.FALSE}</li>
     *   <li>{@code 11} - {@code Boolean.TRUE}</li>
     * </ul>
     *
     * @param  flags The set of bits to modify for the given boolean value.
     * @param  mask  The bit mask, which much have exactly two consecutive bits set.
     * @param  value The boolean value to store in the {@code flags}, or {@code null}.
     * @return The updated {@code flags}.
     */
    public static int setBoolean(int flags, final int mask, final Boolean value) {
        assert 3 << Integer.numberOfTrailingZeros(mask) == mask : mask;
        if (value == null) {
            flags &= ~mask;
        } else {
            flags |= mask;
            if (!value) {
                flags &= ~(mask & (mask >>> 1));
            }
        }
        assert Objects.equals(getBoolean(flags, mask), value) : value;
        return flags;
    }

    /**
     * Returns the boolean value for the bits under the given mask.
     * This method is the reverse of {@link #setBoolean(int, int, Boolean)}.
     *
     * @param  flags The set of bits from which to read the boolean value under the given mask.
     * @param  mask  The bit mask, which much have exactly two consecutive bits set.
     * @return The boolean value under the given mask (may be {@code null}).
     */
    public static Boolean getBoolean(int flags, final int mask) {
        flags &= mask;
        return (flags == 0) ? null : Boolean.valueOf(flags == mask);
    }

    /**
     * Makes sure that the given inclusion is non-null, then returns its value.
     *
     * @param  value The {@link org.opengis.metadata.extent.GeographicBoundingBox#getInclusion()} value.
     * @return The given value as a primitive type.
     * @throws InvalidMetadataException if the given value is null.
     */
    public static boolean getInclusion(final Boolean value) throws InvalidMetadataException {
        if (value == null) {
            throw new InvalidMetadataException(Errors.format(Errors.Keys.MissingValueForProperty_1, "inclusion"));
        }
        return value;
    }
}
