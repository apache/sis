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
package org.apache.sis.internal.referencing;


/**
 * Semaphores that need to be shared across different referencing packages. Each thread has its own set of semaphores.
 * The {@link #clear(int)} method <strong>must</strong> be invoked after the {@link #queryAndSet(int)} method in a
 * {@code try ... finally} block.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-3.00)
 * @version 0.5
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
    public static final int COMPARING = 1;

    /**
     * A flag to indicate that {@link org.apache.sis.referencing.operation.DefaultSingleOperation}
     * is querying {@link org.apache.sis.referencing.operation.transform.ConcatenatedTransform} in
     * the intend to format WKT (normally a {@code "PROJCS"} element).
     */
    public static final int PROJCS = 2;

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
     * @param  flag One of {@link #COMPARING} or {@link #PROJCS} constants.
     * @return {@code true} if the given flag is set.
     */
    public static boolean query(final int flag) {
        final Semaphores s = FLAGS.get();
        return (s != null) && (s.flags & flag) != 0;
    }

    /**
     * Sets the given flag.
     *
     * @param  flag One of {@link #COMPARING} or {@link #PROJCS} constants.
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
     * @param flag One of {@link #COMPARING} or {@link #PROJCS} constants.
     */
    public static void clear(final int flag) {
        final Semaphores s = FLAGS.get();
        if (s != null) {
            s.flags &= ~flag;
        }
    }
}
