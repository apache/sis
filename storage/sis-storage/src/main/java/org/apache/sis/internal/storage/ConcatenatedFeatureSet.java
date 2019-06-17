package org.apache.sis.internal.storage;

import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * Expose a sequence of {@link FeatureSet feature sets} as a single one. A few notes :
 * <ol>
 *     <li>The feature set concatenation must be built with a non-empty array or collection of feature set. It is copied
 *     as is in a unmodifiable list, meaning that doublons won't be removed, and iteration order is driven by input
 *     sequence. </li>
 *     <li>All input feature sets must share a common type, or at least a common super-type. If you want to sequence
 *     sets which does not share any common parent, please pre-process them to modify their public type.</li>
 *     <li>For now, event system is disabled, meaning that trying to attach a listener to the feature set concatenation
 *     will throw an {@link UnsupportedOperationException}. It's meant to evolve.</li>
 * </ol>
 */
public class ConcatenatedFeatureSet implements FeatureSet {

    private final List<FeatureSet> sources;
    private final FeatureType commonType;

    /**
     * Temporarily cache union of underlying feature set envelopes. However, only a weak reference is used, because
     * we cannot prevent subsets to be updated externally. Keeping a temporary reference allows for an eventual update
     * of the concatenated envelope. The same logic is applied for {@link #getMetadata() metadata accessor}.
     */
    private WeakReference<Envelope> cachedEnvelope;

    /**
     * Cache metadata only temporarily, because it depends on {@link #getEnvelope() envelope accessor}. For more
     * information about why the cache is only temporary, please read {@link #cachedEnvelope} documentation.
     */
    private WeakReference<Metadata> cachedMetadata;

    /**
     * See {@link #ConcatenatedFeatureSet(FeatureSet...)}.
     */
    public ConcatenatedFeatureSet(final Collection<FeatureSet> sources) throws DataStoreException {
        this(sources == null? null : sources.toArray(new FeatureSet[sources.size()]));
    }

    /**
     * Create a new feature set being a view of the sequence of given sets.
     *
     * @param sources The sequence of feature set to expose in a single set. Must neither be null, empty nor contains
     *                a single element only.
     * @throws DataStoreException If given feature sets does not share any common type.
     */
    public ConcatenatedFeatureSet(final FeatureSet... sources) throws DataStoreException {
        ensureNonNull("Sources feature sets", sources);
        if (sources.length < 1) {
            throw new IllegalArgumentException("Given feature set sequence is empty.");
        } else if (sources.length == 1) {
            throw new IllegalArgumentException("You are trying to concatenate a single feature set.");
        }

        final FeatureSet[] copy = Arrays.copyOf(sources, sources.length);
        this.sources = Collections.unmodifiableList(Arrays.asList(copy));
        commonType = checkType(this.sources);
        // TODO: we should add listeners on source feature sets. By doing it, we could be notified of changes, allowing
        // a better update strategy for envelope, metadata and type.
    }

    @Override
    public FeatureType getType() {
        return commonType;
    }

    @Override
    public Stream<Feature> features(boolean parallel) {
        final Stream<FeatureSet> sets = parallel? sources.parallelStream() : sources.stream();
        return sets.flatMap(set -> {
            try {
                return set.features(parallel);
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
        });
    }

    @Override
    public synchronized Envelope getEnvelope() throws DataStoreException {
        Envelope result = cachedEnvelope == null? null : cachedEnvelope.get();
        if (result == null) {
            result = computeEnvelope();
            if (result != null)
                cachedEnvelope = new WeakReference<>(result);
        }

        return result;
    }

    private Envelope computeEnvelope() throws DataStoreException {
        Envelope tmpEnv = sources.get(0).getEnvelope();
        if (tmpEnv == null)
            return null;

        final GeneralEnvelope envelope = new GeneralEnvelope(tmpEnv);
        for (int i = 1 ; i < sources.size() ; i++) {
            tmpEnv = sources.get(i).getEnvelope();
            if (tmpEnv == null) {
                return null;
            }
            envelope.add(tmpEnv);
        }

        return envelope;
    }

    @Override
    public GenericName getIdentifier() {
        return null;
    }

    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        Metadata result = cachedMetadata == null ? null : cachedMetadata.get();
        if (result == null) {
            final MetadataBuilder builder = new MetadataBuilder();
            builder.addFeatureType(getType(), null);
            final Envelope env = getEnvelope();
            if (env != null) {
                try {
                    builder.addExtent(AbstractEnvelope.castOrCopy(env));
                } catch (TransformException e) {
                    // TODO: log error, or use a warning listener. It's fine if the extent cannot be computed
                }
            }

            result = builder.build(true);
            cachedMetadata = new WeakReference<>(result);
        }

        return result;
    }

    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
        throw new UnsupportedOperationException();
    }

    /**
     * Find a feature type common to all given feature sets. It means we will search for a super type used by all input
     * feature sets.
     *
     * @implNote A graph approach would surely be the best way to resolve this problem. However, due to the lack of
     * standard tools in the jdk, we'd rather build a map with every distinct encountered type/super type. For each, we
     * associate the number of occurrences found, as the maximum number of inheritors for this type. When done, we
     * simply give back a type with an encounter count equal to the number of input feature sets, and the least possible
     *  number of inheritors.
     *
     * @param sources The set of feature sets to find a common type for. Must not be null.
     * @return A feature type which can be found in every encountered feature set, as super type or direct one.
     * @throws DataStoreException If an error occurs while reading data types from source feature sets.
     * @throws IllegalArgumentException If we cannot find a common type, or input collection is empty.
     * @throws NullArgumentException If input collection is null.
     */
    private static FeatureType checkType(final Collection<FeatureSet> sources) throws DataStoreException, IllegalArgumentException, NullArgumentException {
        ensureNonNull("Source sets", sources);

        final java.util.Iterator<FeatureSet> it = sources.iterator();
        if (!it.hasNext())
            throw new IllegalArgumentException("Cannot find a common data type from an empty collection.");

        FeatureType nextType = it.next().getType();

        final Map<FeatureType, Indirection> typeMatchCount = new HashMap<>();
        // Coupling Stacks allow us to properly browse and document indirection levels in encountered data types.
        Deque<FeatureType> typeStack = new ArrayDeque<>();
        Deque<FeatureType> nextLevel = new ArrayDeque<>();

        nextLevel.push(nextType);
        int indirection = -1;
        do {
            indirection++;
            // Micro opti : re-use the same two deques to avoid instantiating/resizing lots of arrays.
            final Deque tmp = typeStack;
            typeStack = nextLevel;
            nextLevel = tmp;

            do {
                final FeatureType type = typeStack.pop();
                typeMatchCount.put(type, new Indirection(indirection, 1));
                nextLevel.addAll(type.getSuperTypes());
            } while (!typeStack.isEmpty());
        } while (!nextLevel.isEmpty());

        while (it.hasNext()) {
            boolean hasBeenUpdated = false;
            nextLevel.push(it.next().getType());
            indirection = -1;
            do {
                indirection++;
                // Micro opti : re-use the same two deques to avoid instantiating/resizing lots of arrays.
                final java.util.Deque tmp = typeStack;
                typeStack = nextLevel;
                nextLevel = tmp;

                do {
                    final int tmpIndi = indirection;
                    final FeatureType type = typeStack.pop();
                    final Indirection indi = typeMatchCount
                            .computeIfPresent(type, (key, oldVal) -> oldVal.merge(new Indirection(tmpIndi, 1)));
                    if (indi != null) hasBeenUpdated = true;
                    nextLevel.addAll(type.getSuperTypes());
                } while (!typeStack.isEmpty());
            } while (!nextLevel.isEmpty());

            // short-circuit here, if no feature type has the same count as already encounterd types.
            if (!hasBeenUpdated)
                throw new IllegalArgumentException("No common feature nor super type found across input feature sets");
        }

        final int requiredMatch = sources.size();
        return typeMatchCount.entrySet().stream()
                .filter(stat -> stat.getValue().matchCount == requiredMatch)
                .min(Comparator.comparingInt(stat -> stat.getValue().indirection))
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find a common super type across all feature sets to concatenate"));
    }

    private static class Indirection {
        int indirection;
        int matchCount;

        private Indirection(int indirection, final int matchCount) {
            this.indirection = indirection;
            this.matchCount = matchCount;
        }

        private Indirection merge(final Indirection other) {
            if (other.indirection > indirection)
                indirection = other.indirection;
            matchCount += other.matchCount;

            return this;
        }
    }
}
