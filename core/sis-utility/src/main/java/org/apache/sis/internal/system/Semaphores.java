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
package org.apache.sis.internal.system;

import org.apache.sis.util.Workaround;


/**
 * Thread-local booleans that need to be shared across different packages. Each thread has its own set of booleans.
 * The {@link #clear(int)} method <strong>must</strong> be invoked after the {@link #queryAndSet(int)} method in a
 * {@code try ... finally} block.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
public final class Semaphores {
    /**
     * A lock for avoiding never-ending recursivity in the {@code equals} method of
     * {@link org.apache.sis.referencing.crs.AbstractDerivedCRS} and
     * {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation}.
     * It is set to {@code true} when a comparison is in progress. This lock is necessary because
     * {@code AbstractDerivedCRS} objects contain a {@code conversionFromBase} field, which contains a
     * {@code DefaultConversion.targetCRS} field referencing back the {@code AbstractDerivedCRS} object.
     */
    public static final byte CONVERSION_AND_CRS = 1;

    /**
     * A flag to indicate that {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation}
     * is querying parameters of a {@code MathTransform} enclosed in the operation. This is often in the
     * intend to format WKT of a {@code "ProjectedCRS"} element.
     */
    public static final byte ENCLOSED_IN_OPERATION = 2;

    /**
     * A flag to indicate that empty collections should be returned as {@code null}. Returning null
     * collections is not a recommended practice, but is useful in some situations like marshalling
     * a XML document with JAXB, when we want to omit empty XML blocks.
     */
    public static final byte NULL_COLLECTION = 4;

    /**
     * A flag to indicate that a parameter value outside its domain of validity should not cause an exception
     * to be thrown. This flag is set only when creating a deprecated operation from the EPSG database.
     * Typically the operation is deprecated precisely because it used invalid parameter values,
     * but SIS should still be able to create those deprecated objects if a user request them.
     *
     * <p><b>Example:</b> EPSG:3752 was a Mercator (variant A) projection but set the latitude of origin to 41°S.</p>
     */
    @Workaround(library = "EPSG:3752", version = "8.9")        // Deprecated in 2007 but still present in 2016.
    public static final byte SUSPEND_PARAMETER_CHECK = 8;

    /**
     * The flags per running thread.
     */
    private static final ThreadLocal<Semaphores> FLAGS = new ThreadLocal<Semaphores>();

    /**
     * The bit flags.
     */
    private byte flags;

    /**
     * For internal use only.
     */
    private Semaphores() {
    }

    /**
     * Returns {@code true} if the given flag is set.
     *
     * @param flag One of {@link #CONVERSION_AND_CRS}, {@link #ENCLOSED_IN_OPERATION} or other constants.
     * @return {@code true} if the given flag is set.
     */
    public static boolean query(final byte flag) {
        final Semaphores s = FLAGS.get();
        return (s != null) && (s.flags & flag) != 0;
    }

    /**
     * Sets the given flag.
     *
     * @param flag One of {@link #CONVERSION_AND_CRS}, {@link #ENCLOSED_IN_OPERATION} or other constants.
     * @return {@code true} if the given flag was already set.
     */
    public static boolean queryAndSet(final byte flag) {
        Semaphores s = FLAGS.get();
        if (s == null) {
            s = new Semaphores();
            FLAGS.set(s);
        }
        final boolean isSet = ((s.flags & flag) != 0);
        s.flags |= flag;
        return isSet;
    }

    /**
     * Clears the given flag.
     *
     * @param flag One of {@link #CONVERSION_AND_CRS}, {@link #ENCLOSED_IN_OPERATION} or other constants.
     */
    public static void clear(final byte flag) {
        final Semaphores s = FLAGS.get();
        if (s != null) {
            s.flags &= ~flag;
        }
    }
}
