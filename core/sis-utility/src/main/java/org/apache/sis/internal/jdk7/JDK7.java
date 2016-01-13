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

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import javax.imageio.stream.ImageInputStream;
import java.lang.reflect.InvocationTargetException;


/**
 * Place holder for methods defined only in JDK7. Those methods are defined in existing classes,
 * so we can not creates a new class of the same name like we did for {@link Objects}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from GeoAPI)
 * @version 0.7
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

    /**
     * Compares to integer values.
     *
     * @param x First value to compare.
     * @param y Second value to compare.
     * @return Comparison result.
     */
    public static int compare(final int x, final int y) {
        return (x < y) ? -1 : (x == y) ? 0 : 1;
    }

    /**
     * Simulates the {@code object instanceof AutoCloseable} code.
     *
     * @param  object The object to check, or {@code null}.
     * @return {@code true} if the given object is closeable.
     */
    public static boolean isAutoCloseable(final Object object) {
        return (object instanceof Closeable) || (object instanceof ImageInputStream) ||
               (object instanceof Connection) || (object instanceof Statement) || (object instanceof ResultSet) ||
               (object != null && object.getClass().isAnnotationPresent(AutoCloseable.class));
    }

    /**
     * Simulates the {@code ((AutoCloseable) object).close()} method call.
     * The given object shall be an instance of which {@link #isAutoCloseable(Object)}
     * returned {@code true}, otherwise a {@link ClassCastException} will be thrown.
     *
     * @param  object The object to close.
     * @throws Exception If an error occurred while closing the object.
     */
    public static void close(final Object object) throws Exception {
             if (object instanceof Closeable)        ((Closeable)        object).close();
        else if (object instanceof ImageInputStream) ((ImageInputStream) object).close();
        else if (object instanceof Connection)       ((Connection)       object).close();
        else if (object instanceof Statement)        ((Statement)        object).close();
        else if (object instanceof ResultSet)        ((ResultSet)        object).close();
        else try {
            object.getClass().getMethod("close", (Class<?>[]) null).invoke(object, (Object[]) null);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getTargetException();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw e;
            }
        }
    }
}
