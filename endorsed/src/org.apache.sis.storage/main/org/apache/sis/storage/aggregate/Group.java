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
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Stream;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.storage.base.ArrayOfLongs;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.coverage.grid.GridCoverageProcessor;


/**
 * Base class for containers for a list of elements grouped by some attribute.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <E>  type of objects in this group.
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
    final List<E> members = new ArrayList<>();

    /**
     * A pool of shared objects that may be reused in many places. There is a single map per instance
     * of {@link CoverageAggregator}, which is then shared by all groups contained in the aggregator.
     * The keys and values in the pool shall be immutable or objects handled as if they were immutable.
     */
    private final Map<Object,Object> sharedInstances;

    /**
     * The processor to use for creating grid coverages. This is the place where, for example,
     * specifying the color model to use when creating band aggregated resources.
     *
     * @see CoverageAggregator#setColorizer(Colorizer)
     */
    final GridCoverageProcessor processor;

    /**
     * Creates a new group of objects associated to some attribute defined by subclasses.
     */
    Group(final GridCoverageProcessor processor) {
        this.processor  = processor;
        sharedInstances = new HashMap<>();
    }

    /**
     * Creates a new group of objects which are children of the given group.
     *
     * @param  parent  the parent group in which this group is a child.
     */
    Group(final Group<?> parent) {
        sharedInstances = parent.sharedInstances;
        processor = parent.processor;
    }

    /**
     * Creates a name for this group for use in metadata (not a persistent identifier).
     * This is used as the resource name if an aggregated resource needs to be created.
     *
     * @param  locale  the locale for the name to return, or {@code null} for the default.
     * @return a name which can be used as aggregation name for metadata purposes.
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
        return new GroupAggregate(getName(listeners), listeners, members.size());
    }

    /**
     * Returns a unique instance of the given object.
     *
     * @param  <E>     type of the object to share.
     * @param  object  the object for which to get a unique instance.
     * @return shared instance of the given object.
     */
    @SuppressWarnings("unchecked")
    final <E> E unique(final E object) {
        Object existing = sharedInstances.putIfAbsent(object, object);
        return (E) (existing != null ? existing : object);
    }

    /**
     * Returns a unique instance of the given array.
     *
     * @param  array  the array for which to get a unique instance.
     * @return shared instance of the given array.
     */
    final long[] unique(final long[] array) {
        return new ArrayOfLongs(array).unique(sharedInstances);
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
