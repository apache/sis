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
import java.util.Optional;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.util.internal.shared.Strings;


/**
 * A resource which is the result of the aggregation of two or more resources.
 * Different subclasses exist depending on what is aggregated (domains versus bands),
 * and whether the components share the same grid geometry (ignoring translation terms).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class AggregatedResource extends AbstractResource {
    /**
     * The identifier for this aggregate, or {@code null} if none.
     * This is potentially non-null only on the top-level resource
     * built by {@link CoverageAggregator#build(GenericName)}.
     * No identifier should be assigned on intermediate results (i.e. components).
     *
     * @see #getIdentifier()
     */
    GenericName identifier;

    /**
     * Name of this resource to use as citation title in metadata, or {@code null} if none.
     * This is <strong>not</strong> a persistent identifier.
     *
     * @see #createMetadata()
     */
    private String name;

    /**
     * The global envelope of this resource, or {@code null} if not yet computed.
     * May also be {@code null} if no component declare an envelope, or if the union cannot be computed.
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
     * Creates a new concatenated resource.
     *
     * @param  name       name of this aggregate, or {@code null} if none.
     * @param  listeners  listeners of the parent resource, or {@code null} if none.
     * @param  hidden     whether to use the given listeners directly instead of own listeners.
     */
    AggregatedResource(final String name, final StoreListeners listeners, final boolean hidden) {
        super(listeners, hidden);
        this.name = name;
    }

    /**
     * Creates a new resource with the same data as given resource.
     *
     * @param  source  the resource to copy.
     */
    AggregatedResource(final AggregatedResource source) {
        super(source.listeners, true);
        name                = source.name;
        envelope            = source.envelope;
        envelopeIsEvaluated = source.envelopeIsEvaluated;
    }

    /**
     * Returns a resource with the same data but the specified merge strategy.
     * If this resource already uses the given strategy, then returns {@code this}.
     * Otherwise returns a new resource. This resource is not modified by this method
     * call because this method can be invoked after this resource has been published.
     *
     * <h4>API design note</h4>
     * We could try to design a common API for {@link org.apache.sis.storage.RasterLoadingStrategy}
     * and {@link MergeStrategy}. But the former changes the state of the resource while the latter
     * returns a new resource. This is because {@code RasterLoadingStrategy} does not change data,
     * while {@link MergeStrategy} can change the data obtained from the resource.
     *
     * @param  strategy  the new merge strategy to apply.
     * @return resource using the specified strategy (may be {@code this}).
     *
     * @see MergeStrategy#apply(Resource)
     */
    Resource apply(MergeStrategy strategy) {
        return this;
    }

    /**
     * Configures the given resource as a replacement of this concatenated resource.
     * This method is invoked by {@link GroupAggregate#simplify(CoverageAggregator)} when
     * an aggregate node is excluded and we want to inherit the name of the excluded node.
     * It should happen before the resource is published.
     *
     * @param  single  the single component contained in this resource.
     * @return the resource to use as a replacement for this concatenated resource.
     */
    final Resource configureReplacement(final Resource single) {
        if (single instanceof AggregatedResource) {
            ((AggregatedResource) single).name = name;
        }
        return single;
    }

    /**
     * Returns the resource persistent identifier as specified by the
     * user in {@link CoverageAggregator}. There is no default value.
     */
    @Override
    public final Optional<GenericName> getIdentifier() {
        return Optional.ofNullable(identifier);
    }

    /**
     * Creates when first requested the metadata about this resource.
     * This method delegates to {@link #createMetadata(MetadataBuilder)}.
     *
     * @return the newly created metadata.
     * @throws DataStoreException if an error occurred while reading metadata from this resource.
     */
    @Override
    protected final Metadata createMetadata() throws DataStoreException {
        final var builder = new MetadataBuilder();
        builder.addTitle(name);
        createMetadata(builder);
        return builder.build();
    }

    /**
     * Appends metadata in the given builder.
     * This method is invoked when first needed, then the result is cached.
     *
     * @param  builder  the builder where to append the metadata.
     * @throws DataStoreException if an error occurred while reading metadata from this resource.
     */
    abstract void createMetadata(MetadataBuilder builder) throws DataStoreException;

    /**
     * Returns the components of this aggregate.
     * The returned list will be handled as read-only.
     *
     * @return the components of this resource.
     */
    abstract List<Resource> components();

    /**
     * Returns the spatiotemporal envelope of this resource.
     *
     * @return the spatiotemporal resource extent.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    @Override
    public final Optional<Envelope> getEnvelope() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (!envelopeIsEvaluated) {
                final List<Resource> components = components();
                final var envelopes = new Envelope[components.size()];
                for (int i=0; i < envelopes.length; i++) {
                    final Resource r = components.get(i);
                    final Optional<Envelope> re;
                    if (r instanceof DataSet) {
                        re = ((DataSet) r).getEnvelope();
                    } else if (r instanceof AbstractResource) {
                        re = ((AbstractResource) r).getEnvelope();
                    } else {
                        continue;
                    }
                    envelopes[i] = re.orElse(null);
                }
                try {
                    envelope = ImmutableEnvelope.castOrCopy(Envelopes.union(envelopes));
                } catch (TransformException e) {
                    listeners.warning(e);
                }
                envelopeIsEvaluated = true;
            }
            return Optional.ofNullable(envelope);
        }
    }

    /**
     * Returns a string representation of this resource for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "name", name, "size", components().size());
    }
}
