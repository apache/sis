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
package org.apache.sis.internal.jdk7;


/**
 * Place holder for methods defined only in JDK7. Those methods are defined in existing classes,
 * so we can not creates a new class of the same name like we did for {@link Objects}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from GeoAPI)
 * @version 0.3
 * @module
 */
public final class JDK7 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK7() {
    }

    /**
     * Returns the platform specific line-separator.
     *
     * @return The platform-specific line separator.
     *
     * @see System#lineSeparator()
     */
    public static String lineSeparator() {
        return System.getProperty("line.separator", "\n");
    }

    /**
     * Returns {@code true} if the given code point is in the BMP plane.
     *
     * @param  c The code point.
     * @return {@code true} if the given code point is in the BMP plane.
     *
     * @see Character#isBmpCodePoint(int)
     */
    public static boolean isBmpCodePoint(final int c) {
        return c >= Character.MIN_VALUE &&
               c <= Character.MAX_VALUE;
    }

    /**
     * Returns the leading surrogate for the given code point.
     *
     * @param  c The code point.
     * @return The leading surrogate.
     *
     * @see Character#highSurrogate(int)
     */
    public static char highSurrogate(int c) {
        c -= Character.MIN_SUPPLEMENTARY_CODE_POINT;
        c >>>= 10;
        c += Character.MIN_HIGH_SURROGATE;
        return (char) c;
    }

    /**
     * Returns the trailing surrogate for the given code point.
     *
     * @param  c The code point.
     * @return The trailing surrogate.
     *
     * @see Character#lowSurrogate(int)
     */
    public static char lowSurrogate(int c) {
        c &= 0x3FF;
        c += Character.MIN_LOW_SURROGATE;
        return (char) c;
    }
}
