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
package org.apache.sis.internal.feature;

import java.util.AbstractList;
import org.apache.sis.util.collection.IntegerList;
import org.apache.sis.util.collection.CheckedContainer;

// Branch-dependent imports
import java.time.Instant;


/**
 * Unmodifiable lists of instants.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class DateList extends AbstractList<Instant> implements CheckedContainer<Instant> {
    /**
     * The times in multiples of {@link #increment} milliseconds since the {@link #epoch}.
     */
    private final IntegerList times;

    /**
     * The value by which to multiply the {@link #times} values in order to get milliseconds.
     */
    private final long increment;

    /**
     * The epoch in milliseconds since January 1st, 1970 midnight UTC.
     */
    private final long epoch;

    /**
     * Creates a new list for the given times. The given array shall be a temporary one
     * since this constructor modifies the array values for computational purpose.
     */
    DateList(final long[] millis) {
        long min = Long.MAX_VALUE;
        for (final long t : millis) {
            if (t < min) min = t;
        }
        long max = 1;
        for (int i=0; i<millis.length; i++) {
            final long t = (millis[i] -= min);
            if (t > max) max = t;
        }
        long inc = max;
        for (long t : millis) {
            if ((t % inc) != 0) {
                do {
                    final long r = (inc % t);       // Search for greatest common divisor with Euclid's algorithm.
                    inc = t;
                    t = r;
                } while (t != 0);
                if (inc == 1) break;                // No need to check other values.
            }
        }
        epoch = min;
        increment = inc;
        times = new IntegerList(millis.length, Math.toIntExact(max / inc));
        for (final long t : millis) {
            times.add(Math.toIntExact(t / inc));
        }
    }

    /**
     * Returns the kind of elements in this list.
     */
    @Override
    public Class<Instant> getElementType() {
        return Instant.class;
    }

    /**
     * Returns the number of instants in this list.
     */
    @Override
    public int size() {
        return times.size();
    }

    /**
     * Returns the instant at the given index.
     */
    @Override
    public Instant get(final int index) {
        return Instant.ofEpochMilli(times.getInt(index)*increment + epoch);
    }
}
