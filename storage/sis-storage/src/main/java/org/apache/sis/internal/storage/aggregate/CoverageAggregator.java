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
package org.apache.sis.internal.storage.aggregate;

import java.util.Locale;
import java.util.List;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Creates a grid coverage resource from an aggregation of an arbitrary amount of other resources.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final class CoverageAggregator extends Group<GroupBySample> {
    /**
     * The listeners of the parent resource (typically a {@link DataStore}), or {@code null} if none.
     */
    private final StoreListeners listeners;

    /**
     * The aggregates which where the sources of components added during a call to {@link #addComponents(Aggregate)}.
     * This is used for reusing existing aggregates instead of {@link GroupAggregate} when the content is the same.
     */
    private final Map<Set<Resource>, Queue<Aggregate>> aggregates;

    /**
     * Creates an initially empty aggregator.
     *
     * @param listeners  listeners of the parent resource, or {@code null} if none.
     *        This is usually the listeners of the {@link org.apache.sis.storage.DataStore}.
     */
    public CoverageAggregator(final StoreListeners listeners) {
        this.listeners = listeners;
        aggregates = new HashMap<>();
    }

    /**
     * Returns a name of the aggregate to be created.
     * This is used only if this aggregator find resources having different sample dimensions.
     *
     * @param  locale  the locale for the name to return, or {@code null} for the default.
     * @return a name which can be used as aggregation name, or {@code null} if none.
     */
    @Override
    final String getName(final Locale locale) {
        return (listeners != null) ? listeners.getSourceName() : null;
    }

    /**
     * Adds all grid resources provided by the given stream. This method can be invoked from any thread.
     * It delegates to {@link #add(GridCoverageResource)} for each element in the stream.
     *
     * @param  resources  resources to add.
     * @throws DataStoreException if a resource can not be used.
     */
    public void addAll(final Stream<? extends GridCoverageResource> resources) throws DataStoreException {
        try {
            resources.forEach((resource) -> {
                try {
                    add(resource);
                } catch (DataStoreException e) {
                    throw new BackingStoreException(e);
                }
            });
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(DataStoreException.class);
        }
    }

    /**
     * Adds the given resource. This method can be invoked from any thread.
     * This method does <em>not</em> recursively decomposes an {@link Aggregate} into its component.
     *
     * @param  resource  resource to add.
     * @throws DataStoreException if the resource can not be used.
     */
    public void add(final GridCoverageResource resource) throws DataStoreException {
        final GroupBySample bySample = GroupBySample.getOrAdd(members, resource.getSampleDimensions());
        final GridSlice slice = new GridSlice(resource);
        final List<GridSlice> slices;
        try {
            slices = slice.getList(bySample.members).members;
        } catch (NoninvertibleTransformException e) {
            throw new DataStoreContentException(e);
        }
        synchronized (slices) {
            slices.add(slice);
        }
    }

    /**
     * Adds all components of the given aggregate. This method can be invoked from any thread.
     * It delegates to {@link #add(GridCoverageResource)} for each component in the aggregate
     * which is an instance of {@link GridCoverageResource}.
     * Components that are themselves instance of {@link Aggregate} are decomposed recursively.
     *
     * @param  resource  resource to add.
     * @throws DataStoreException if a component of the resource can not be used.
     *
     * @todo Instead of ignoring non-coverage instances, we should put them in a separated aggregate.
     */
    public void addComponents(final Aggregate resource) throws DataStoreException {
        boolean hasDuplicated = false;
        final Set<Resource> components = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final Resource component : resource.components()) {
            if (components.add(component)) {
                if (component instanceof GridCoverageResource) {
                    add((GridCoverageResource) component);
                } else if (component instanceof Aggregate) {
                    addComponents((Aggregate) component);
                }
            } else {
                hasDuplicated = true;       // Should never happen, but we are paranoiac.
            }
        }
        if (!(hasDuplicated || components.isEmpty())) {
            /*
             * We should not have 2 aggregates with the same components.
             * But if it happens anyway, put the aggregates in a queue.
             * Each aggregate will be used at most once.
             */
            synchronized (aggregates) {
                aggregates.computeIfAbsent(components, (k) -> new ArrayDeque<>(1)).add(resource);
            }
        }
    }

    /**
     * If an user-supplied aggregate exists for all the given components, returns that aggregate.
     * The returned aggregate is removed from the pool; aggregates are not returned twice.
     * This method is thread-safe.
     *
     * @param  components  the components for which to get user-supplied aggregate.
     * @return user-supplied aggregate if it exists. The returned aggregate is removed from the pool.
     */
    final Optional<Aggregate> existingAggregate(final Resource[] components) {
        final Set<Resource> key = Collections.newSetFromMap(new IdentityHashMap<>());
        if (Collections.addAll(key, components)) {
            final Queue<Aggregate> r;
            synchronized (aggregates) {
                r = aggregates.get(key);
            }
            if (r != null) {
                return Optional.ofNullable(r.poll());
            }
        }
        return Optional.empty();
    }

    /**
     * Builds a resource which is the aggregation or concatenation of all components added to this aggregator.
     * The returned resource will be an instance of {@link GridCoverageResource} if possible,
     * or an instance of {@link Aggregate} if some heterogeneity in grid geometries or sample dimensions
     * prevent the concatenation of all coverages in a single resource.
     *
     * <p>This method is not thread safe. If the {@code add(…)} and {@code addAll(…)} methods were invoked
     * in background threads, then all additions must be finished before this method is invoked.</p>
     *
     * @return the aggregation or concatenation of all components added to this aggregator.
     */
    public Resource build() {
        final GroupAggregate aggregate = prepareAggregate(listeners);
        aggregate.fillWithChildAggregates(this, GroupBySample::createComponents);
        return aggregate.simplify(this);
    }
}
