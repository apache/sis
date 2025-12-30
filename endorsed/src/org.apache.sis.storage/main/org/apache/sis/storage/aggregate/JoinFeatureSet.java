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

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.opengis.util.GenericName;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.filter.Optimization;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.filter.Filter;
import org.apache.sis.filter.Expression;
import org.apache.sis.pending.geoapi.filter.BinaryComparisonOperator;


/**
 * Features containing association to features from two different sources, joined by a SQL-like {@code JOIN} condition.
 * Each feature in this {@code FeatureSet} contains two or three properties:
 *
 * <ul>
 *   <li>An optional identifier created from the identifiers of the left and right features.</li>
 *   <li>Zero or one association to a "left"  feature.</li>
 *   <li>Zero or one association to a "right" feature.</li>
 * </ul>
 *
 * The left and right features appear together in an {@code JoinFeatureSet} instance when a value from
 * {@code leftProperty} in the first feature is equal to a value from {@code rightProperty} in the second feature.
 *
 * <h2>Implementation note</h2>
 * If iterations in one feature set is cheaper than iterations in the other feature set, then the "costly" or larger
 * {@code FeatureSet} should be on the left side and the "cheap" {@code FeatureSet} should be on the right side.
 *
 * <p>This implementation is read-only.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   1.0
 */
public class JoinFeatureSet extends AggregatedFeatureSet {
    /**
     * Specifies whether values on both sides are required (inner join), or only one side (outer join).
     */
    public enum Type {
        /**
         * Only records having a value on both side will be included.
         * The {@link JoinFeatureSet} {@code "left"} and {@code "right"} properties will never be null.
         */
        INNER(false, false),

        /**
         * All records from the left side will be included. If there is no matching feature on the right side,
         * then the {@link JoinFeatureSet} {@code "right"} property will be {@code null}.
         */
        LEFT_OUTER(true, false),

        /**
         * All records from the right side will be included. If there is no matching feature on the left side,
         * then the {@link JoinFeatureSet} {@code "left"} property will be {@code null}.
         */
        RIGHT_OUTER(true, true);

        /**
         * Whether to include all "main" feature instances even if there is no match in the other side.
         * This is {@code true} for outer joins and {@code false} for inner joins.
         */
        final boolean isOuterJoin;

        /**
         * {@code true} if the "main" side is the right side instead of the left side.
         * See {@link JoinFeatureSet.Iterator} for a definition of "main side".
         */
        final boolean swapSides;

        /**
         * Creates an enumeration.
         */
        private Type(final boolean isOuterJoin, final boolean swapSides) {
            this.isOuterJoin = isOuterJoin;
            this.swapSides   = swapSides;
        }

        /**
         * Returns the minimum occurrences for properties on the left or right side.
         *
         * @param right  {@code false} for the left side, or {@code true} for the right side.
         */
        final int minimumOccurs(final boolean right) {
            return !isOuterJoin | (swapSides == right) ? 1 : 0;
        }

        /**
         * Returns the enumeration value for the given characteristics.
         */
        static Type valueOf(final boolean isOuterJoin, final boolean swapSides) {
            return isOuterJoin ? (swapSides ? RIGHT_OUTER : LEFT_OUTER) : INNER;
        }
    }

    /**
     * The type of features included in this set. Contains two associations as described in class javadoc.
     */
    private final DefaultFeatureType type;

    /**
     * The first source of features.
     */
    public final FeatureSet left;

    /**
     * The second source of features.
     */
    public final FeatureSet right;

    /**
     * Name of the associations to the {@link #left} features.
     * This may be the name of the {@link #left} feature type, but not necessarily.
     */
    private final String leftName;

    /**
     * Name of the associations to the {@link #right} features.
     * This may be the name of the {@link #right} feature type, but not necessarily.
     */
    private final String rightName;

    /**
     * {@code true} if the "main" side is the right side instead of the left side.
     * See {@link JoinFeatureSet.Iterator} for a definition of "main side".
     */
    private final boolean swapSides;

    /**
     * Whether to include all "main" feature instances even if there is no match in the other side.
     * This is {@code true} for outer joins and {@code false} for inner joins.
     */
    private final boolean isOuterJoin;

    /**
     * The join condition in the form <var>property from left feature</var> = <var>property from right feature</var>.
     * This condition specifies also if the comparison is {@linkplain BinaryComparisonOperator#isMatchingCase() case
     * sensitive} and {@linkplain BinaryComparisonOperator#getMatchAction() how to compare multi-values}.
     */
    public final BinaryComparisonOperator<AbstractFeature> condition;

    /**
     * The factory to use for creating {@code Query} expressions for retrieving subsets of feature sets.
     */
    private final DefaultFilterFactory<AbstractFeature,?,?> factory;

    /**
     * Creates a new feature set joining the two given sets. The {@code featureInfo} map defines the name,
     * description or other information for the {@code FeatureType} created by this method. It can contain all
     * the properties described in {@link org.apache.sis.feature.DefaultFeatureType} plus the following ones:
     *
     * <ul>
     *   <li>{@code "identifierDelimiter"} — string to insert between left and right identifiers in the identifiers
     *     generated by the join operation. If this property is not specified, then no identifier will be generated.</li>
     *   <li>{@code "identifierPrefix"} — string to insert at the beginning of join identifiers (optional).</li>
     *   <li>{@code "identifierSuffix"} — string to insert at the end of join identifiers (optional).</li>
     * </ul>
     *
     * @param  parent       the parent resource, or {@code null} if none.
     * @param  left         the first source of features. This is often (but not necessarily) the largest set.
     * @param  leftAlias    name of the associations to the {@code left} features, or {@code null} for a default name.
     * @param  right        the second source of features. Should be the set in which iterations are cheapest.
     * @param  rightAlias   name of the associations to the {@code right} features, or {@code null} for a default name.
     * @param  joinType     whether values on both sides are required (inner join), or only one side (outer join).
     * @param  condition    join condition as <var>property from left feature</var> = <var>property from right feature</var>.
     * @param  featureInfo  information about the {@code FeatureType} of this feature set.
     * @throws DataStoreException if an error occurred while creating the feature set.
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public JoinFeatureSet(final Resource parent,
                          final FeatureSet left,  String leftAlias,
                          final FeatureSet right, String rightAlias,
                          final Type joinType, final BinaryComparisonOperator<AbstractFeature> condition,
                          Map<String,?> featureInfo)
            throws DataStoreException
    {
        super(parent);
        final DefaultFeatureType leftType  = left.getType();
        final DefaultFeatureType rightType = right.getType();
        final GenericName leftName  = leftType.getName();
        final GenericName rightName = rightType.getName();
        if (leftAlias  == null) leftAlias  = leftName.toString();
        if (rightAlias == null) rightAlias = rightName.toString();
        this.left        = left;
        this.right       = right;
        this.leftName    = leftAlias;
        this.rightName   = rightAlias;
        this.swapSides   = joinType.swapSides;
        this.isOuterJoin = joinType.isOuterJoin;
        this.condition   = condition;
        this.factory     = DefaultFilterFactory.forFeatures();
        /*
         * We could build the FeatureType only when first needed, but the type is required by the iterators.
         * Since we are going to need the type for any use of this JoinFeatureSet, better to create it now.
         */
        AbstractIdentifiedType[] properties = new AbstractIdentifiedType[] {
            new DefaultAssociationRole(properties(leftAlias),  leftType,  joinType.minimumOccurs(false), 1),
            new DefaultAssociationRole(properties(rightAlias), rightType, joinType.minimumOccurs(true),  1)
        };
        final String identifierDelimiter = Containers.property(featureInfo, "identifierDelimiter", String.class);
        if (identifierDelimiter != null && hasIdentifier(leftType) && hasIdentifier(rightType)) {
            final AbstractOperation identifier = FeatureOperations.compound(
                    properties(AttributeConvention.IDENTIFIER_PROPERTY), identifierDelimiter,
                    Containers.property(featureInfo, "identifierPrefix", String.class),
                    Containers.property(featureInfo, "identifierSuffix", String.class), properties);
            properties = ArraysExt.insert(properties, 0, 1);
            properties[0] = identifier;
        }
        if (featureInfo == null) {
            featureInfo = properties(leftName.tip().toString() + '-' + rightName.tip());
        }
        type = new DefaultFeatureType(featureInfo, false, null, properties);
    }

    /**
     * Returns whether the given feature type has a {@value AttributeConvention#IDENTIFIER} property.
     */
    private static boolean hasIdentifier(final DefaultFeatureType feature) {
        return feature.hasProperty(AttributeConvention.IDENTIFIER) &&
               feature.getProperty(AttributeConvention.IDENTIFIER) != null;
    }

    /**
     * Creates a minimal {@code properties} map for feature type or property type constructors.
     * This minimalist map contain only the mandatory entry, which is the name.
     */
    private static Map<String,?> properties(final Object name) {
        return Map.of(DefaultFeatureType.NAME_KEY, name);
    }

    /**
     * Returns the two feature sets used by this {@code JoinFeatureSet}.
     * The "main" dependency is at index 0 and the other dependency at index 1.
     *
     * @return the dependencies in a list of size 2 with the "main" dependency first.
     */
    @Override
    final List<FeatureSet> dependencies() {
        final FeatureSet[] sets = new FeatureSet[] {left, right};
        if (swapSides) ArraysExt.swap(sets, 0, 1);
        return Arrays.asList(sets);
    }

    /**
     * Specifies whether values on both sides are required (inner join), or only one side (outer join).
     *
     * @return whether values on both sides are required (inner join), or only one side (outer join).
     */
    public Type getJoinType() {
        return Type.valueOf(isOuterJoin, swapSides);
    }

    /**
     * Returns a description of properties that are common to all features in this dataset.
     * This type may contain one identifier and always contains two associations,
     * to the {@linkplain #left} and {@link #right} set of features respectively.
     *
     * @return a description of properties that are common to all features in this dataset.
     */
    @Override
    public DefaultFeatureType getType() {
        return type;
    }

    /**
     * Returns a stream of all features contained in this dataset.
     *
     * @param  parallel  {@code true} for a parallel stream (if supported), or {@code false} for a sequential stream.
     * @return all features contained in this dataset.
     * @throws DataStoreException if an error occurred while creating the stream.
     */
    @Override
    public Stream<AbstractFeature> features(final boolean parallel) throws DataStoreException {
        final Iterator it = new Iterator();
        return StreamSupport.stream(it, parallel).onClose(it);
    }

    /**
     * Configures the optimization of a query with information about the expected type of all feature instances.
     * The default implementation uses the knowledge that all features will be instances of exactly the type
     * returned by {@link #getType()}, with no sub-type.
     * Subclasses should override this method if they break this assertion.
     *
     * @since 1.6
     */
    @Override
    protected void prepareQueryOptimization(FeatureQuery query, Optimization optimizer) throws DataStoreException {
        optimizer.setFinalFeatureType(getType());
    }

    /**
     * Creates a new features containing an association to the two given features.
     * The {@code main} feature cannot be null (this is not verified).
     */
    private AbstractFeature join(AbstractFeature main, AbstractFeature filtered) {
        if (swapSides) {
            final AbstractFeature t = main;
            main = filtered;
            filtered = t;
        }
        final AbstractFeature f = type.newInstance();
        f.setPropertyValue(leftName,  main);
        f.setPropertyValue(rightName, filtered);
        return f;
    }

    /**
     * Iterator over the features resulting from the inner or outer join operation.
     * The {@link #run()} method disposes the resources.
     */
    private final class Iterator implements Spliterator<AbstractFeature>, Consumer<AbstractFeature>, Runnable {
        /**
         * The main stream or a split iterator to close when the {@link #run()} method will be invoked.
         * This is initially the stream from which {@link #mainIterator} has been created. However, if
         * {@link #trySplit()} has been invoked, then this handler may be the other {@code Iterator}
         * instance which itself contains a reference to the stream to close, thus forming a chain.
         */
        private Runnable mainCloseHandler;

        /**
         * An iterator over all features in the "main" (usually left) side. The "main" side is the side which
         * may include all features: in a "left outer join" this is the left side, and in a "right outer join"
         * this is the right side. For inner join we arbitrarily take the left side in accordance with public
         * class javadoc, which suggests to put the most costly or larger set on the left side.
         *
         * <p>Only one iteration will be performed on those features, contrarily to the other side where we may
         * iterate over the same elements many times.</p>
         */
        private final Spliterator<AbstractFeature> mainIterator;

        /**
         * A feature fetched from the {@link #mainIterator}. The join operation will match this feature with
         * zero, one or more features from the other side. A {@code null} value means that this feature needs
         * to be retrieved with {@code mainIterator.tryAdvance(…)}.
         */
        private AbstractFeature mainFeature;

        /**
         * The stream over features in the other (usually right) side. A new stream will be created every time a new
         * feature from the main side is processed. For this reason, it should be the cheapest stream if possible.
         */
        private Stream<AbstractFeature> filteredStream;

        /**
         * Iterator for the {@link #filteredStream}. A new iterator will be recreated every time a new feature
         * from the main side is processed.
         */
        private Spliterator<AbstractFeature> filteredIterator;

        /**
         * A feature fetched from the {@link #filteredIterator}, or {@code null} if none.
         */
        private AbstractFeature filteredFeature;

        /**
         * Creates a new iterator. We do not use parallelized {@code mainStream} here because the {@code accept(…)}
         * methods used by this {@code Iterator} cannot be invoked concurrently by different threads. It does not
         * present parallelization at a different level since this {@code Iterator} supports {@link #trySplit()},
         * so the {@link Stream} wrapping it can use parallelization.
         */
        Iterator() throws DataStoreException {
            final Stream<AbstractFeature> mainStream = (swapSides ? right : left).features(false);
            mainCloseHandler = mainStream::close;
            mainIterator = mainStream.spliterator();
        }

        /**
         * Creates an iterator resulting from the call to {@link #trySplit()}.
         */
        private Iterator(final Spliterator<AbstractFeature> it) {
            mainIterator = it;
        }

        /**
         * If this iterator can be partitioned, returns a spliterator covering a prefix of the feature set.
         * Upon return from this method, this iterator will cover a suffix of the feature set.
         * Returns {@code null} if this iterator cannot be partitioned.
         */
        @Override
        public Spliterator<AbstractFeature> trySplit() {
            final Spliterator<AbstractFeature> s = mainIterator.trySplit();
            if (s == null) {
                return null;
            }
            final Iterator it = new Iterator(s);
            it.mainCloseHandler = mainCloseHandler;
            mainCloseHandler = it;
            return it;
        }

        /**
         * Specifies that the iterator will return only non-null elements. Whether those elements will
         * be ordered depends on whether the main iterator provides ordered elements in the first place.
         *
         * <p><b>NOTE:</b> to be strict, we should check if the "filtered" stream is also ordered. But this
         * is more difficult to check. Current implementation assumes that if the "mean" stream is ordered,
         * then the other stream is ordered too. Furthermore, the {@link #trySplit()} method works only on
         * the main stream, so at least the {@code trySplit} requirement about prefix and suffix order is
         * still fulfill even if the other stream is unordered.</p>
         */
        @Override
        public int characteristics() {
            return (mainIterator.characteristics() & ORDERED) | NONNULL;
        }

        /**
         * Estimated size is unknown.
         */
        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        /**
         * Closes the streams used by this iterator, together with the streams used by any spliterator
         * created by {@link #trySplit()}. This method is registered to {@link Stream#onClose(Runnable)}.
         */
        @Override
        public void run() {
            closeFilteredIterator();
            final Runnable toClose = mainCloseHandler;
            if (toClose != null) {
                mainCloseHandler = null;            // Cleared first in case of error.
                toClose.run();
            }
        }

        /**
         * Invoked when iteration on the filtered stream ended, before to move on the next feature of the main stream.
         * This method is idempotent: it has no effect if the stream is already closed.
         */
        private void closeFilteredIterator() {
            final Stream<AbstractFeature> stream = filteredStream;
            filteredStream   = null;                // Cleared before call to close() in case of error.
            filteredIterator = null;
            filteredFeature  = null;                // Used as a sentinel value by this.forEachRemaining(…).
            mainFeature      = null;                // Indicate that we will need to advance in mainIterator.
            if (stream != null) {
                stream.close();
            }
        }

        /**
         * Creates a new iterator over the filtered set of features (usually the right side).
         * The filtering condition is determined by the current {@link #mainFeature}.
         */
        private void createFilteredIterator() {
            final Expression<AbstractFeature,?> expression1, expression2;
            final FeatureSet filteredSet;
            if (swapSides) {
                expression1 = condition.getOperand2();
                expression2 = condition.getOperand1();
                filteredSet = left;
            } else {
                expression1 = condition.getOperand1();
                expression2 = condition.getOperand2();
                filteredSet = right;
            }
            final Object mainValue = expression1.apply(mainFeature);
            final Filter<AbstractFeature> filter;
            if (mainValue != null) {
                filter = factory.equal(expression2, factory.literal(mainValue));
            } else {
                filter = factory.isNull(expression2);
            }
            final FeatureQuery query = new FeatureQuery();
            query.setSelection(filter);
            try {
                filteredStream = filteredSet.subset(query).features(false);
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
            filteredIterator = filteredStream.spliterator();
        }

        /**
         * Executes the given action on all remaining features in the {@code JoinFeatureSet}.
         */
        @Override
        public void forEachRemaining(final Consumer<? super AbstractFeature> action) {
            final Consumer<AbstractFeature> forFiltered = (final AbstractFeature feature) -> {
                if (feature != null) {
                    action.accept(join(mainFeature, filteredFeature = feature));
                }
            };
            final Consumer<AbstractFeature> forMain = (final AbstractFeature feature) -> {
                if (feature != null) {
                    mainFeature = feature;
                    createFilteredIterator();
                    filteredIterator.forEachRemaining(forFiltered);
                    final boolean none = (filteredFeature == null);
                    closeFilteredIterator();
                    if (none && isOuterJoin) {
                        action.accept(join(feature, null));
                    }
                    // Do not close the main stream since it may be in use by other Spliterators.
                }
            };
            forMain.accept(mainFeature);                // In case some 'tryAdvance' has been invoked before.
            mainIterator.forEachRemaining(forMain);
        }

        /**
         * Callback for {@code Spliterator.tryAdvance(this)} on {@link #filteredIterator}.
         * Used by {@link #tryAdvance(Consumer)} implementation only.
         */
        @Override
        public void accept(final AbstractFeature feature) {
            filteredFeature = feature;
        }

        /**
         * Executes the given action on the next feature in the {@code JoinFeatureSet}.
         */
        @Override
        public boolean tryAdvance(final Consumer<? super AbstractFeature> action) {
            for (;;) {
                if (mainFeature == null) {
                    do if (!mainIterator.tryAdvance(this)) {
                        return false;
                    } while (filteredFeature == null);
                    mainFeature = filteredFeature;
                    filteredFeature = null;
                }
                if (filteredIterator == null) {
                    createFilteredIterator();
                }
                final boolean none = (filteredFeature == null);
                while (filteredIterator.tryAdvance(this)) {
                    if (filteredFeature != null) {
                        action.accept(join(mainFeature, filteredFeature));
                        return true;
                    }
                }
                final AbstractFeature feature = mainFeature;
                closeFilteredIterator();
                if (none && isOuterJoin) {
                    action.accept(join(feature, null));
                    return true;
                }
            }
        }
    }
}
