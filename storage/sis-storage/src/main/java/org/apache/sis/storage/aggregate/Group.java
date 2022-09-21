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
package org.apache.sis.storage.aggregate;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Stream;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.storage.event.StoreListeners;


/**
 * Base class for containers for a list of elements grouped by some attribute.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @param  <E>  type of objects in this group.
 *
 * @since 1.3
 * @module
 */
abstract class Group<E> {
    /**
     * The name of this group, or {@code null} if not yet computed.
     *
     * @see #getName(StoreListeners)
     */
    private String name;

    /**
     * All members of this group. This list is populated by calls to {@link GridSlice#addTo(List)}.
     * Accesses to this list should be synchronized during the phase when this list is populated,
     * because that part may be parallelized by {@link CoverageAggregator#addResources(Stream)}.
     * No synchronization is needed after.
     */
    final List<E> members;

    /**
     * Creates a new group of objects associated to some attribute defined by subclasses.
     */
    Group() {
        members = new ArrayList<>();
    }

    /**
     * Creates a name for this group.
     * This is used as the resource name if an aggregated resource needs to be created.
     *
     * @param  locale  the locale for the name to return, or {@code null} for the default.
     * @return a name which can be used as aggregation name.
     */
    abstract String createName(Locale locale);

    /**
     * Returns the name of this group.
     *
     * @param  listeners  listeners from which to get the locale, or {@code null} for the default.
     * @return a name which can be used as aggregation name.
     */
    final String getName(final StoreListeners listeners) {
        if (name == null) {
            name = createName(listeners == null ? null : listeners.getLocale());
        }
        return name;
    }

    /**
     * Prepares an initially empty aggregate.
     * One of the {@code GroupAggregate.fillFoo(…)} methods must be invoked after this method.
     *
     * @param listeners  listeners of the parent resource, or {@code null} if none.
     * @return an initially empty aggregate.
     */
    final GroupAggregate prepareAggregate(final StoreListeners listeners) {
        return new GroupAggregate(listeners, getName(listeners), members.size());
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        final int count;
        synchronized (members) {
            count = members.size();
        }
        return Strings.toString(getClass(), "name", getName(null), "count", count);
    }
}
