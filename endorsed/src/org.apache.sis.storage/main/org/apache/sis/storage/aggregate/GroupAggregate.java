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
import java.util.function.BiConsumer;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.coverage.SampleDimension;


/**
 * An aggregate created when, after grouping resources by CRS and other attributes,
 * more than one group still exist. Those groups become components of an aggregate.
 * This is used as temporary object during analysis, then kept alive in last resort
 * when we cannot build a single time series from a sequence of coverages at different times.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GroupAggregate extends AggregatedResource implements Aggregate {
    /**
     * Minimum number of components for keeping this aggregate after analysis.
     */
    private static final int KEEP_ALIVE = 2;

    /**
     * The components of this aggregate. Array elements are initially null, but should all become non-null
     * after a {@code fill(…)} method has been invoked. If the length is smaller than {@value #KEEP_ALIVE},
     * then this aggregate is only a temporary object.
     *
     * @see #components()
     */
    private final Resource[] components;

    /**
     * Whether all {@link #components} are {@link GridCoverageResource} elements.
     * This is used for skipping calls to {@link #simplify()} when it is known that
     * no component can be simplified.
     */
    private boolean componentsAreLeaves;

    /**
     * The sample dimensions of all children in this group, or an empty collection if they are not the same.
     * This field is initially null, but should become non-null after a {@code fill(…)} method has been invoked.
     * This is used for metadata only.
     */
    List<SampleDimension> sampleDimensions;

    /**
     * Creates a new aggregate with the specified number of components.
     * One of the {@code fill(…)} methods must be invoked after this constructor.
     *
     * @param name       name of this aggregate, or {@code null} if none.
     * @param listeners  listeners of the parent resource, or {@code null} if none.
     * @param count      expected number of components.
     *
     * @see Group#prepareAggregate(StoreListeners)
     */
    GroupAggregate(final String name, final StoreListeners listeners, final int count) {
        super(name, listeners, count < KEEP_ALIVE);
        components = new Resource[count];
    }

    /**
     * Creates a new aggregate with the specified components, which will receive no further processing.
     * This is invoked when the caller has not been able to group the slices in a multi-dimensional cube.
     * The result stay an aggregate of heterogeneous resources.
     *
     * @param name              name of this aggregate, or {@code null} if none.
     * @param listeners         listeners of the parent resource, or {@code null} if none.
     * @param components        the resources to uses as components of this aggregate.
     * @param sampleDimensions  sample dimensions common to all grid coverage resources.
     */
    GroupAggregate(final String name, final StoreListeners listeners, final GridCoverageResource[] components,
                   final List<SampleDimension> sampleDimensions)
    {
        super(name, listeners, true);
        this.components = components;
        this.componentsAreLeaves = true;
        this.sampleDimensions = sampleDimensions;
    }

    /**
     * Creates a new resource with the same data as given resource but a different merge strategy.
     *
     * @param  source      the resource to copy.
     * @param  components  components with the new merge strategy.
     */
    private GroupAggregate(final GroupAggregate source, final Resource[] components) {
        super(source);
        sampleDimensions    = source.sampleDimensions;
        componentsAreLeaves = source.componentsAreLeaves;
        this.components     = components;
    }

    /**
     * Returns an aggregate with the same data as this aggregate but a different merge strategy.
     * This is the implementation of {@link MergeStrategy#apply(Resource)} public method.
     */
    @Override
    final Resource apply(final MergeStrategy strategy) {
        synchronized (getSynchronizationLock()) {
            boolean changed = false;
            final Resource[] copy = components.clone();
            for (int i=0; i < copy.length; i++) {
                final Resource c = copy[i];
                if (c instanceof AggregatedResource) {
                    final var component = (AggregatedResource) c;
                    changed |= ((copy[i] = component.apply(strategy)) != component);
                }
            }
            return changed ? new GroupAggregate(this, copy) : this;
        }
    }

    /**
     * Sets all components of this aggregate to sub-aggregates, which are themselves initialized with the given filler.
     * This method may be invoked recursively if the sub-aggregates themselves have sub-sub-aggregates.
     *
     * @param <E>          type of object in the group.
     * @param children     data for creating children, as one sub-aggregate per member of the {@code children} group.
     * @param childFiller  the action to execute for initializing each sub-aggregate.
     *                     The first {@link BiConsumer} argument is a {@code children} member (the source)
     *                     and the second argument is the sub-aggregate to initialize (the target).
     */
    final <E extends Group<?>> void fillWithChildAggregates(final Group<E> children, final BiConsumer<E,GroupAggregate> childFiller) {
        assert components.length == children.members.size();
        for (int i=0; i < components.length; i++) {
            final E member = children.members.get(i);
            final GroupAggregate child = member.prepareAggregate(listeners);
            childFiller.accept(member, child);
            components[i] = child;
        }
    }

    /**
     * Sets all components of this aggregate to grid coverage resources.
     * Children created by this method are leaf nodes.
     *
     * @param  children  date for creating children, as one coverage per member of the {@code children} group.
     * @param  ranges    sample dimensions of the coverage to create. Stored as-is (not copied).
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")    // Copy done by GroupBySample constructor.
    final void fillWithCoverageComponents(final List<GroupByTransform> children, final List<SampleDimension> ranges) {
        componentsAreLeaves = true;
        for (int i=0; i < components.length; i++) {
            components[i] = children.get(i).createResource(listeners, ranges);
        }
    }

    /**
     * Simplifies the resource tree by removing all aggregates of 1 component.
     *
     * @param  aggregator  the aggregation builder which is invoking this method.
     * @return the resource to use after simplification.
     */
    final Resource simplify(final CoverageAggregator aggregator) {
        if (!componentsAreLeaves) {
            for (int i=0; i < components.length; i++) {
                final Resource r = components[i];
                if (r instanceof GroupAggregate) {
                    components[i] = ((GroupAggregate) r).simplify(aggregator);
                }
            }
        }
        if (components.length == 1) {
            return configureReplacement(components[0]);
        }
        return aggregator.existingAggregate(components).orElse(this);
    }

    /**
     * Returns the components of this aggregate.
     */
    @Override
    public final List<Resource> components() {
        return UnmodifiableArrayList.wrap(components);
    }

    /**
     * Creates when first requested the metadata about this aggregate.
     * The metadata contains the title for this aggregation, the sample dimensions
     * (if they are the same for all children) and the geographic bounding box.
     */
    @Override
    protected void createMetadata(final MetadataBuilder builder) throws DataStoreException {
        builder.addExtent(getEnvelope().orElse(null), listeners);
        if (sampleDimensions != null) {
            for (final SampleDimension band : sampleDimensions) {
                builder.addNewBand(band);
            }
        }
    }
}
