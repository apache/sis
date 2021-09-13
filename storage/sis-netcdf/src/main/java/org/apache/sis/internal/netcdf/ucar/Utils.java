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


/**
 * Utility methods completing UCAR library for Apache SIS needs.
 * Some methods are workarounds for UCAR library methods having a different behavior than what we would expect.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
final class Utils {
    /**
     * Strings used in UCAR netCDF API for meaning that an information is not available.
     * Some {@link ucar.nc2} methods return this value instead of {@code null}.
     *
     * @see #nonEmpty(String)
     */
    private static final String NOT_AVAILABLE = "N/A";

    /**
     * Do not allow instantiation of this class.
     */
    private Utils() {
    }

    /**
     * Trims the leading and trailing spaces of the given string and excludes empty or missing texts.
     * If the given string is null, empty, contains only spaces or is {@value #NOT_AVAILABLE} (ignoring case),
     * then this method returns {@code null}.
     */
    static String nonEmpty(String text) {
        if (text != null) {
            text = text.trim();
            if (text.isEmpty() || text.equalsIgnoreCase(NOT_AVAILABLE)) {
                return null;
            }
        }
        return text;
    }

    /**
     * If {@code isUnsigned} is {@code true} but the given value is negative, makes it positive.
     *
     * @param  number  the attribute value, or {@code null}.
     * @return whether the number is unsigned.
     */
    static Number fixSign(Number number, final boolean isUnsigned) {
        if (isUnsigned) {
            if (number instanceof Byte) {
                final byte value = (byte) number;
                if (value < 0) {
                    number = (short) Byte.toUnsignedInt(value);
                }
            } else if (number instanceof Short) {
                final short value = (Short) number;
                if (value < 0) {
                    number = Short.toUnsignedInt(value);
                }
            } else if (number instanceof Integer) {
                final int value = (Integer) number;
                if (value < 0) {
                    number = Integer.toUnsignedLong(value);
                }
            }
        }
        return number;
    }
}
