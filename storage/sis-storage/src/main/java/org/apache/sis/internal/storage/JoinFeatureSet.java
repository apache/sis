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
package org.apache.sis.internal.storage;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultLiteral;
import org.apache.sis.filter.DefaultPropertyIsEqualTo;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.logging.WarningListeners;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.MatchAction;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

/**
 * A join feature set merges features from two different sources following a
 * SQL Join condition.
 *
 * <p>
 * This implementation is read-only.
 * </p>
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class JoinFeatureSet extends AbstractFeatureSet implements FeatureSet {

    /**
     * Join operation type.
     */
    public enum Type {
        /**
         * Both side must have a value to be included.
         */
        INNER,

        /**
         * A least left side must have a value to be included.
         */
        LEFT_OUTER,

        /**
         * A least right side must have a value to be included.
         */
        RIGHT_OUTER
    }

    private final FeatureSet left;
    private final FeatureSet right;
    private String leftAlias;
    private String rightAlias;
    private final Type joinType;
    private final PropertyIsEqualTo condition;

    //cache
    private FeatureType type;


    public JoinFeatureSet(final WarningListeners<DataStore> listeners, FeatureSet left,
            String leftAlias, FeatureSet right, String rightAlias, Type joinType, PropertyIsEqualTo condition) {
        super(listeners);
        this.left = left;
        this.right = right;
        this.leftAlias = leftAlias;
        this.rightAlias = rightAlias;
        this.joinType = joinType;
        this.condition = condition;
    }

    /**
     * Gets the join condition.
     *
     * @return Filter
     */
    public PropertyIsEqualTo getJoinCondition() {
        return condition;
    }

    /**
     * Gets the join type.
     *
     * @return join type
     */
    public Type getJoinType() {
        return joinType;
    }

    /**
     * Gets the left feature source.
     *
     * @return left join feature set
     */
    public FeatureSet getLeft() {
        return left;
    }

    /**
     * Gets the right feature source.
     *
     * @return right join feature set
     */
    public FeatureSet getRight() {
        return right;
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        if (type == null) {
            final FeatureType leftType = left.getType();
            final FeatureType rightType = right.getType();
            final GenericName leftName = leftType.getName();
            final GenericName rightName = rightType.getName();
            if (leftAlias == null) leftAlias = leftName.toString();
            if (rightAlias == null) rightAlias = rightName.toString();

            final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
            ftb.addAttribute(String.class).setName(AttributeConvention.IDENTIFIER_PROPERTY).setDefaultValue("");
            ftb.addAssociation(leftType).setName(leftAlias).setMinimumOccurs(0).setMaximumOccurs(1);
            ftb.addAssociation(rightType).setName(rightAlias).setMinimumOccurs(0).setMaximumOccurs(1);
            ftb.setName(leftName.tip().toString() + '-' + rightName.tip().toString());
            type = ftb.build();
        }

        return type;
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        final FeatureType type = getType();
        final DefaultMetadata metadata = new DefaultMetadata();
        final DefaultDataIdentification ident = new DefaultDataIdentification();
        final DefaultCitation citation = new DefaultCitation();
        citation.setTitle(new SimpleInternationalString(type.getName().toString()));
        citation.setIdentifiers(Arrays.asList(new NamedIdentifier(type.getName())));
        ident.setCitation(citation);
        metadata.setIdentificationInfo(Arrays.asList(ident));
        return metadata;
    }

    /**
     * Envelope is not stored or computed.
     *
     * @return always null
     * @throws DataStoreException
     */
    @Override
    public Envelope getEnvelope() throws DataStoreException {
        return null;
    }

    @Override
    public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
        if (query instanceof SimpleQuery) {
            return SimpleQuery.executeOnCPU(this, (SimpleQuery) query);
        }
        return FeatureSet.super.subset(query);
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        final JoinIterator ite;
        switch (joinType) {
            case INNER :       ite = new JoinInnerRowIterator(); break;
            case LEFT_OUTER :  ite = new JoinOuterRowIterator(true); break;
            case RIGHT_OUTER : ite = new JoinOuterRowIterator(false); break;
            default:
                throw new IllegalArgumentException("Unknown Join type : " + joinType);
        }
        final Stream<Feature> stream = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(ite, Spliterator.ORDERED),
            false);
        stream.onClose(ite::close);
        return stream;
    }

    /**
     * Aggregate all feature from selectors to a single complex feature.
     *
     * @return aggregated features
     * @throws DataStoreException
     */
    private Feature toFeature(final Feature left, final Feature right) throws DataStoreException{
        final FeatureType type = getType(); //force creating type.
        final Feature f = type.newInstance();

        String id = "";

        if (left != null) {
            id += left.getPropertyValue(AttributeConvention.IDENTIFIER_PROPERTY.toString());
            f.setPropertyValue(leftAlias,left);
        }
        if (right != null) {
            if (left != null) id += " ";
            id += right.getPropertyValue(AttributeConvention.IDENTIFIER_PROPERTY.toString());
            f.setPropertyValue(rightAlias,right);
        }

        f.setPropertyValue(AttributeConvention.IDENTIFIER_PROPERTY.toString(), id);
        return f;
    }

    private interface JoinIterator extends Iterator<Feature>, AutoCloseable {

        @Override
        public default void close() {}

    }

    /**
     * Iterate on both collections with an Inner join condition.
     */
    private class JoinInnerRowIterator implements JoinIterator {

        private final Stream<Feature> leftStream;
        private final Iterator<Feature> leftIterator;
        private Stream<Feature> rightStream;
        private Iterator<Feature> rightIterator;
        private Feature leftFeature;
        private Feature combined;

        JoinInnerRowIterator() throws DataStoreException {
            leftStream = left.features(false);
            leftIterator = leftStream.iterator();
        }

        @Override
        public Feature next() {
            try {
                searchNext();
            } catch (DataStoreException ex) {
                throw new BackingStoreException(ex);
            }
            Feature f = combined;
            combined = null;
            return f;
        }

        @Override
        public void close() {
            leftStream.close();
            if (rightStream != null) {
                rightStream.close();
            }
        }

        @Override
        public boolean hasNext() {
            try {
                searchNext();
            } catch (DataStoreException ex) {
                throw new BackingStoreException(ex);
            }
            return combined != null;
        }

        private void searchNext() throws DataStoreException {
            if (combined != null) return;

            final PropertyIsEqualTo equal = getJoinCondition();
            final Expression leftProperty = equal.getExpression1();
            final Expression rightProperty = equal.getExpression2();

            //we might have several right features for one left
            if (leftFeature != null && rightIterator != null) {
                while (combined == null && rightIterator.hasNext()) {
                    final Feature rightRes = rightIterator.next();
                    combined = toFeature(leftFeature, rightRes);
                }
            }

            if (rightIterator != null && !rightIterator.hasNext()){
                //no more results in right iterator, close iterator
                rightStream.close();
                rightStream = null;
                rightIterator = null;
            }

            while (combined == null && leftIterator.hasNext()) {
                leftFeature = leftIterator.next();

                final Object leftValue = leftProperty.evaluate(leftFeature);

                if (rightIterator == null) {
                    final SimpleQuery rightQuery = new SimpleQuery();
                    rightQuery.setFilter(new DefaultPropertyIsEqualTo(rightProperty, new DefaultLiteral<>(leftValue), true, MatchAction.ALL));
                    rightStream = right.subset(rightQuery).features(false);
                    rightIterator = rightStream.iterator();
                }

                while (combined == null && rightIterator.hasNext()) {
                    final Feature rightRow = rightIterator.next();
                    combined = toFeature(leftFeature, rightRow);
                }

                if (combined == null) {
                    //no more results in right iterator, close iterator
                    rightStream.close();
                    rightStream = null;
                    rightIterator = null;
                }
            }

        }

    }

    /**
     * Iterate on both collections with an outer join condition.
     */
    private class JoinOuterRowIterator implements JoinIterator {

        private final Stream<Feature> primeStream;
        private final Iterator<Feature> primeIterator;
        private Stream<Feature> secondStream;
        private Iterator<Feature> secondIterator;

        private final boolean leftJoint;
        private Feature primeFeature;
        private Feature nextFeature;

        JoinOuterRowIterator(final boolean leftJoint) throws DataStoreException{
            this.leftJoint = leftJoint;
            if (leftJoint) {
                primeStream = left.features(false);
            } else {
                primeStream = right.features(false);
            }
            primeIterator = primeStream.iterator();

        }

        @Override
        public Feature next() {
            try {
                searchNext();
            } catch (DataStoreException ex) {
                throw new BackingStoreException(ex);
            }
            Feature f = nextFeature;
            nextFeature = null;
            return f;
        }

        @Override
        public void close() {
            primeStream.close();
            if (secondStream != null) {
                secondStream.close();
            }
        }

        @Override
        public boolean hasNext() {
            try {
                searchNext();
            } catch (DataStoreException ex) {
                throw new BackingStoreException(ex);
            }
            return nextFeature != null;
        }

        private void searchNext() throws DataStoreException {
            if (nextFeature != null) return;

            final PropertyIsEqualTo equal = getJoinCondition();
            final Expression leftProperty = equal.getExpression1();
            final Expression rightProperty = equal.getExpression2();

            //we might have several right features for one left
            if (primeFeature != null && secondIterator != null) {
                while (nextFeature == null && secondIterator.hasNext()) {
                    final Feature secondCandidate = secondIterator.next();
                    nextFeature = checkValid(primeFeature, secondCandidate, leftJoint);
                }
            }

            while (nextFeature == null && primeIterator.hasNext()) {
                primeFeature = primeIterator.next();

                if (secondIterator != null) {
                    secondStream.close();
                    secondStream = null;
                    secondIterator = null;
                }

                final Object primeValue;
                if (leftJoint) {
                    primeValue = leftProperty.evaluate(primeFeature);
                } else {
                    primeValue = rightProperty.evaluate(primeFeature);
                }

                if (secondIterator == null) {
                    final SimpleQuery query = new SimpleQuery();
                    if (leftJoint) {
                        query.setFilter(new DefaultPropertyIsEqualTo(rightProperty, new DefaultLiteral<>(primeValue), true, MatchAction.ALL));
                        secondStream = right.subset(query).features(false);
                        secondIterator = secondStream.iterator();
                    } else {
                        query.setFilter(new DefaultPropertyIsEqualTo(leftProperty, new DefaultLiteral<>(primeValue), true, MatchAction.ALL));
                        secondStream = left.subset(query).features(false);
                        secondIterator = secondStream.iterator();
                    }
                }

                while (nextFeature == null && secondIterator.hasNext()) {
                    final Feature rightRow = secondIterator.next();
                    nextFeature = checkValid(primeFeature, rightRow,leftJoint);
                }

                if (nextFeature == null) {
                    //outer left effect, no right match but still we must return the left side
                    if (leftJoint) {
                        nextFeature = toFeature(primeFeature,null);
                    } else {
                        nextFeature = toFeature(null,primeFeature);
                    }
                }

            }

        }

        private Feature checkValid(final Feature left, final Feature right, final boolean leftJoin) throws DataStoreException{
            final Feature candidate;
            if (leftJoin) {
                candidate = toFeature(left,right);
            } else {
                candidate = toFeature(right,left);
            }

            return candidate;
        }

    }

}
