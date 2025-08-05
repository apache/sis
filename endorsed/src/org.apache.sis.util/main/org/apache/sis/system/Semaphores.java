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
package org.apache.sis.system;

import org.apache.sis.util.Workaround;


/**
 * Thread-local booleans that need to be shared across different packages. Each thread has its own set of booleans.
 * The {@link #clear(int)} method <strong>must</strong> be invoked after the {@link #queryAndSet(int)} method in
 * a {@code try ... finally} block.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Semaphores {
    /**
     * A flag to indicate that empty collections should be returned as {@code null}. Returning null
     * collections is not a recommended practice, but is useful in some situations like marshalling
     * a XML document with JAXB, when we want to omit empty XML blocks.
     */
    public static final int NULL_COLLECTION = 1;

    /**
     * A flag to indicate that only metadata are desired and that there is no need to create costly objects.
     * This flag is used during iteration over many coordinate operations before to select a single one by
     * inspecting only their metadata.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-327">SIS-327</a>
     */
    public static final int METADATA_ONLY = 2;

    /**
     * A lock for avoiding never-ending recursion in the {@code equals} method of {@code AbstractDerivedCRS}
     * and {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation}.
     * It is set to {@code true} when a comparison is in progress. This lock is necessary because
     * {@code AbstractDerivedCRS} objects contain a {@code conversionFromBase} field, which contains a
     * {@code DefaultConversion.targetCRS} field referencing back the {@code AbstractDerivedCRS} object.
     */
    public static final int CONVERSION_AND_CRS = 4;

    /**
     * A flag to indicate that {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation}
     * is querying parameters of a {@code MathTransform} enclosed in the operation. This is often at the
     * time of formatting the WKT of a {@code "ProjectedCRS"} element.
     */
    public static final int ENCLOSED_IN_OPERATION = 8;

    /**
     * A flag to indicate that a parameter value outside its domain of validity should not cause an exception
     * to be thrown. This flag is set only when creating a deprecated operation from the EPSG database.
     * Typically the operation is deprecated precisely because it used invalid parameter values,
     * but SIS should still be able to create those deprecated objects if a user request them.
     *
     * <p><b>Example:</b> EPSG:3752 was a Mercator (variant A) projection but set the latitude of origin to 41°S.</p>
     */
    @Workaround(library = "EPSG:3752", version = "8.9")        // Deprecated in 2007 but still present in 2016.
    public static final int SUSPEND_PARAMETER_CHECK = 16;

    /**
     * A flag to indicate that a finer logging level should be used for reporting geodetic object creations.
     * This flag is used during operations that potentially create a large number of CRSs, for example when
     * trying many CRS candidates in search for a CRS compliant with some criteria.
     */
    public static final int FINER_OBJECT_CREATION_LOGS = 32;

    /**
     * The flags per running thread.
     */
    private static final ThreadLocal<Semaphores> FLAGS = new ThreadLocal<>();

    /**
     * The bit flags.
     */
    private int flags;

    /**
     * For internal use only.
     */
    private Semaphores() {
    }

    /**
     * Returns {@code true} if the given flag is set.
     *
     * @param  flag  one of {@link #CONVERSION_AND_CRS}, {@link #ENCLOSED_IN_OPERATION} or other constants.
     * @return {@code true} if the given flag is set.
     */
    public static boolean query(final int flag) {
        final Semaphores s = FLAGS.get();
        return (s != null) && (s.flags & flag) != 0;
    }

    /**
     * Sets the given flag.
     *
     * @param  flag  one of {@link #CONVERSION_AND_CRS}, {@link #ENCLOSED_IN_OPERATION} or other constants.
     * @return {@code true} if the given flag was already set.
     */
    public static boolean queryAndSet(final int flag) {
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
     * @param  flag  one of {@link #CONVERSION_AND_CRS}, {@link #ENCLOSED_IN_OPERATION} or other constants.
     */
    public static void clear(final int flag) {
        final Semaphores s = FLAGS.get();
        if (s != null) {
            s.flags &= ~flag;
        }
    }

    /**
     * Clears the given flag only if it was previously cleared.
     * This is a convenience method for a common pattern with {@code try … finally} blocks.
     *
     * @param  flag      one of {@link #CONVERSION_AND_CRS}, {@link #ENCLOSED_IN_OPERATION} or other constants.
     * @param  previous  value returned by {@link #queryAndSet(int)}.
     */
    public static void clearIfFalse(final int flag, final boolean previous) {
        if (!previous) {
            clear(flag);
        }
    }
}
