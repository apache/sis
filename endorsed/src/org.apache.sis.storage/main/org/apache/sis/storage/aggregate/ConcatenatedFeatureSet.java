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

import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.apache.sis.feature.Features;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.storage.internal.Resources;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Exposes a sequence of {@link FeatureSet}s as a single one.
 * The concatenation is built from an array or collection of input feature sets,
 * copied verbatim in iteration order and without removal of duplicated elements.
 * All input feature sets must share a common type, or at least a common super-type.
 * The {@linkplain #getType() feature type of this concatenated set} will be the
 * {@linkplain Features#findCommonParent(Iterable) most specific type} found among all input feature sets.
 *
 * <h2>Identification</h2>
 * There is no {@linkplain #getIdentifier() identifier} since this feature set is a computation result.
 *
 * <h2>Multi-threading</h2>
 * Concatenated feature set is immutable and thread-safe if all input feature sets
 * are immutable and thread-safe.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   1.0
 */
public class ConcatenatedFeatureSet extends AggregatedFeatureSet {
    /**
     * The sequence of feature sets whose feature instances will be returned.
     */
    private final List<FeatureSet> sources;

    /**
     * The most specific feature type common to all feature sets in the {@linkplain #sources} list.
     */
    private final FeatureType commonType;

    /**
     * Creates a new concatenated feature set with the same types as the given feature set,
     * but different sources. This is used for creating {@linkplain #subset(Query) subsets}.
     */
    private ConcatenatedFeatureSet(final FeatureSet[] sources, final ConcatenatedFeatureSet original) {
        super(original);
        this.sources = Containers.viewAsUnmodifiableList(sources);
        commonType = original.commonType;
    }

    /**
     * Creates a new feature set as a concatenation of the sequence of features given by the {@code sources}.
     * This constructor does not verify that the given {@code sources} array contains at least two elements;
     * this verification must be done by the caller. This constructor retains the given {@code sources} array
     * by direct reference; clone, if desired, shall be done by the caller.
     *
     * @param  parent   the parent resource, or {@code null} if none.
     * @param  sources  the sequence of feature sets to expose in a single set.
     *                  Must contains at least two elements.
     * @throws DataStoreException if given feature sets does not share any common type.
     */
    protected ConcatenatedFeatureSet(final Resource parent, final FeatureSet[] sources) throws DataStoreException {
        super(parent);
        for (int i=0; i<sources.length; i++) {
            ArgumentChecks.ensureNonNullElement("sources", i, sources[i]);
        }
        this.sources = Containers.viewAsUnmodifiableList(sources);
        final var types = new FeatureType[sources.length];
        for (int i=0; i<types.length; i++) {
            types[i] = sources[i].getType();
        }
        commonType = Features.findCommonParent(Arrays.asList(types));
        if (commonType == null) {
            throw new DataStoreContentException(Resources.format(Resources.Keys.NoCommonFeatureType));
        }
    }

    /**
     * Creates a new feature set as a concatenation of the sequence of features given by the {@code sources}.
     * The given array shall be non-empty. If the array contains only 1 element, that element is returned.
     *
     * @param  sources  the sequence of feature sets to expose in a single set.
     * @return the concatenation of given feature set.
     * @throws DataStoreException if given feature sets does not share any common type.
     */
    public static FeatureSet create(final FeatureSet... sources) throws DataStoreException {
        ArgumentChecks.ensureNonEmpty("sources", sources);
        if (sources.length == 1) {
            final FeatureSet fs = sources[0];
            ArgumentChecks.ensureNonNullElement("sources", 0, fs);
            return fs;
        } else {
            return new ConcatenatedFeatureSet(null, sources.clone());
        }
    }

    /**
     * Creates a new feature set as a concatenation of the sequence of features given by the {@code sources}.
     * The given collection shall be non-empty. If the collection contains only 1 element, that element is returned.
     *
     * @param  sources  the sequence of feature sets to expose in a single set.
     * @return the concatenation of given feature set.
     * @throws DataStoreException if given feature sets does not share any common type.
     */
    public static FeatureSet create(final Collection<? extends FeatureSet> sources) throws DataStoreException {
        final int size = sources.size();
        switch (size) {
            case 0: {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "sources"));
            }
            case 1: {
                final FeatureSet fs = Containers.peekFirst(sources);
                ArgumentChecks.ensureNonNullElement("sources", 0, fs);
                return fs;
            }
            default: {
                return new ConcatenatedFeatureSet(null, sources.toArray(new FeatureSet[size]));
            }
        }
    }

    /**
     * Returns all feature set used by this aggregation. This method is invoked for implementation of
     * {@link #getEnvelope()} and {@link #createMetadata(MetadataBuilder)}.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Safe because the list is unmodifiable.
    final List<FeatureSet> dependencies() {
        return sources;
    }

    /**
     * Returns the most specific feature type common to all feature sets given to the constructor.
     *
     * @return the common type of all features returned by this set.
     */
    @Override
    public FeatureType getType() {
        return commonType;
    }

    /**
     * Returns an estimation of the number of features in this set, or an empty value if unknown.
     * This is the sum of the estimations provided by all source sets, or empty if at least one
     * source could not provide an estimation.
     *
     * @return estimation of the number of features.
     */
    @Override
    public OptionalLong getFeatureCount() {
        long sum = 0;
        for (final FeatureSet fs : sources) {
            if (fs instanceof AbstractFeatureSet) {
                final OptionalLong count = ((AbstractFeatureSet) fs).getFeatureCount();
                if (count.isPresent()) {
                    final long c = count.getAsLong();
                    if (c >= 0) {                               // Paranoiac check.
                        if ((sum += c) < 0) {                   // Integer overflow.
                            sum = Long.MAX_VALUE;
                            break;
                        }
                        continue;
                    }
                }
            }
            return OptionalLong.empty();                        // A source cannot provide estimation.
        }
        return OptionalLong.of(sum);
    }

    /**
     * Returns a stream of all features contained in this concatenated dataset. If the {@code parallel} argument
     * is {@code false}, then datasets are traversed in the order they were specified at construction time.
     * If the {@code parallel} argument is {@code true}, then datasets are traversed in no determinist order.
     * If an error occurred while reading the feature instances from a source, then the error is wrapped in a
     * {@link BackingStoreException}.
     *
     * @param  parallel  {@code true} for a parallel stream, or {@code false} for a sequential stream.
     * @return all features contained in this dataset.
     */
    @Override
    public Stream<Feature> features(final boolean parallel) {
        final Stream<FeatureSet> sets = parallel ? sources.parallelStream() : sources.stream();
        return sets.flatMap(set -> {
            try {
                return set.features(parallel);
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
        });
    }

    /**
     * Requests a subset of features and/or feature properties from this resource.
     *
     * @param  query  definition of feature and feature properties filtering applied at reading time.
     * @return resulting subset of features (never {@code null}).
     * @throws DataStoreException if an error occurred while processing the query.
     */
    @Override
    public FeatureSet subset(final Query query) throws DataStoreException {
        final var subsets = new FeatureSet[sources.size()];
        boolean modified = false;
        for (int i=0; i<subsets.length; i++) {
            FeatureSet source = sources.get(i);
            subsets[i] = source.subset(query);
            modified |= (subsets[i] != source);
        }
        return modified ? new ConcatenatedFeatureSet(subsets, this) : this;
    }
}
