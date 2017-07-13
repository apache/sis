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
import org.apache.sis.math.Vector;
import org.apache.sis.util.collection.CheckedContainer;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Instant;


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
     * The times in milliseconds since the epoch.
     */
    private final Vector times;

    /**
     * Creates a new list for the given times.
     */
    DateList(final long[] millis) {
        times = Vector.create(millis, false).compress(0);
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
        return Instant.ofEpochMilli(times.longValue(index));
    }
}
