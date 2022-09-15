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
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.ImmutableEnvelope;


/**
 * An aggregate created when, after grouping resources by CRS and other attributes,
 * more than one group still exist. Those groups become components of an aggregate.
 * This is used as temporary object during analysis, then kept alive in last resort
 * when we can not build a single time series from a sequence of coverages at different times.
 *
 * <p>This class intentionally does not override {@link #getIdentifier()} because
 * it would not be a persistent identifier.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
final class GroupAggregate extends AbstractResource implements Aggregate, AggregatedResource {
    /**
     * Minimum number of components for keeping this aggregate after analysis.
     */
    private static final int KEEP_ALIVE = 2;

    /**
     * Name of this aggregate, or {@code null} if none.
     * This is <strong>not</strong> a persistent identifier.
     */
    private String name;

    /**
     * The components of this aggregate. Array elements are initially null, but should all become non-null
     * after a {@code fill(…)} method has been invoked. If the length is smaller than {@value #KEEP_ALIVE},
     * then this aggregate is only a temporary object.
     */
    private final Resource[] components;

    /**
     * Whether all {@link #components} are {@link GridCoverageResource} elements.
     * This is used for skipping calls to {@link #simplify()} when it is known that
     * no component can be simplified.
     */
    private boolean componentsAreLeaves;

    /**
     * The envelope of this aggregate, or {@code null} if not yet computed.
     * May also be {@code null} if no component declare an envelope, or if the union can not be computed.
     *
     * @see #getEnvelope()
     */
    private ImmutableEnvelope envelope;

    /**
     * Whether {@link #envelope} has been initialized.
     * The envelope may still be null if the initialization failed.
     */
    private boolean envelopeIsEvaluated;

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
     * @param listeners  listeners of the parent resource, or {@code null} if none.
     * @param name       name of this aggregate, or {@code null} if none.
     * @param count      expected number of components.
     *
     * @see Group#prepareAggregate(StoreListeners)
     */
    GroupAggregate(final StoreListeners listeners, final String name, final int count) {
        super(listeners, count < KEEP_ALIVE);
        components = new Resource[count];
        this.name = name;
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
            final Resource c = components[0];
            if (c instanceof AggregatedResource) {
                ((AggregatedResource) c).setName(name);
            }
            return c;
        }
        return aggregator.existingAggregate(components).orElse(this);
    }

    /**
     * Returns the components of this aggregate.
     */
    @Override
    public Collection<Resource> components() {
        return UnmodifiableArrayList.wrap(components);
    }

    /**
     * Returns the spatiotemporal envelope of this resource.
     *
     * @return the spatiotemporal resource extent.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    @Override
    public synchronized Optional<Envelope> getEnvelope() throws DataStoreException {
        if (!envelopeIsEvaluated) {
            try {
                envelope = unionOfComponents(components);
            } catch (TransformException e) {
                listeners.warning(e);
            }
            envelopeIsEvaluated = true;
        }
        return Optional.ofNullable(envelope);
    }

    /**
     * Computes the union of envelopes provided by all the given resources.
     *
     * @param  components  the components for which to extract the envelope.
     * @return union of envelope of all components, or {@code null} if none.
     */
    static ImmutableEnvelope unionOfComponents(final Resource[] components)
            throws DataStoreException, TransformException
    {
        final Envelope[] envelopes = new Envelope[components.length];
        for (int i=0; i < components.length; i++) {
            final Resource r = components[i];
            if (r instanceof AbstractResource) {
                envelopes[i] = ((AbstractResource) r).getEnvelope().orElse(null);
            } else if (r instanceof GridCoverageResource) {
                envelopes[i] = ((GridCoverageResource) r).getEnvelope().orElse(null);
            }
        }
        return ImmutableEnvelope.castOrCopy(Envelopes.union(envelopes));
    }

    /**
     * Modifies the name of the resource.
     * This information is used for metadata.
     */
    @Override
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Creates when first requested the metadata about this aggregate.
     * The metadata contains the title for this aggregation, the sample dimensions
     * (if they are the same for all children) and the geographic bounding box.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addTitle(name);
        try {
            builder.addExtent(envelope);
        } catch (TransformException e) {
            listeners.warning(e);
        }
        if (sampleDimensions != null) {
            for (final SampleDimension band : sampleDimensions) {
                builder.addNewBand(band);
            }
        }
        return builder.build();
    }

    /**
     * Returns a string representation of this aggregate for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "name", name, "size", components.length);
    }
}
