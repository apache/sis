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
package org.apache.sis.filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Predicate;

import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;

import org.apache.sis.feature.Features;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.collection.BackingStoreException;

/**
 * @implNote AMBIGUITY : Description of BBOX operator from <a href="http://docs.opengeospatial.org/is/09-026r2/09-026r2.html#60">
 *     filter encoding 2.0.2</a> is rather succinct, and do not well explain if both tested expressions must be
 *     envelopes, or if we should test an envelope against a real geometry. What we will do in this implementation is
 *     testing bbox only, because the test for a bbox against a complex geometry can be realized using ST_Intersect
 *     operator.
 *
 * Border management: From above reference, bbox should be equivalent to Not ST_Disjoint (from SQLMM/ISO:19125).
 * Disjoint operation specifies that both geometry boundaries must not touch (their intersection is an empty space), so
 * we will consider as valid envelopes with only a common boundary.
 *
 *     TODO: CRS check.
 */
final class DefaultBBOX implements BBOX, Serializable {

    private static final long serialVersionUID = 3068335120981348484L;

    final Expression left;
    final Expression right;

    private transient Predicate intersects;

    DefaultBBOX(Expression left, Expression right) {
        this.left = left;
        this.right = right;
        init();
    }

    /**
     * Initialize this filter state. It is necessary because of serialization compliance.
     */
    private void init() {
        if (left == null && right == null) {
            throw new NullArgumentException(
                    "Both arguments are null, but at least one must be given " +
                            "(as stated in OGC Filter encoding corrigendum 2.0.2, section 7.8.3.2)."
            );
        }

        if (left instanceof Literal) {
            intersects = asOptimizedTest((Literal) left, right);
        } else if (right instanceof Literal) {
            intersects = asOptimizedTest((Literal) right, left);
        } else intersects = this::nonOptimizedIntersect;
    }

    @Override
    public Expression getExpression1() {
        return left;
    }

    @Override
    public Expression getExpression2() {
        return right;
    }

    @Override
    public boolean evaluate(Object object) {
        return intersects.test(object);
    }

    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    private boolean nonOptimizedIntersect(Object candidate) {
        Envelope leftEval = left == null ? null : asEnvelope(left, candidate);
        Envelope rightEval = right == null ? null : asEnvelope(right, candidate);
        if (left == null) {
            return multiIntersect(candidate, rightEval);
        } else if (right == null) {
            return multiIntersect(candidate, leftEval);
        }

        /* OGC Filter encoding corrigendum 2.0.2 section 7.8.3.4 states that false must be returned if any of the
         * operand is null. It does not state what to do if both are null, but we'll follow the same behavior.
         */
        if (leftEval == null || rightEval == null) return false;
        return intersect(AbstractEnvelope.castOrCopy(leftEval), rightEval);
    }

    private static boolean intersect(final Object candidate, final Expression valueExtractor, final AbstractEnvelope constEnvelope) {
        final Envelope candidateEnv = asEnvelope(valueExtractor, candidate);
        if (candidateEnv == null) return false;
        return intersect(constEnvelope, candidateEnv);
    }

    /**
     * Ensure that all geometric properties in given candidate intersect input envelope. This method tries to match OGC
     * Filter Encoding corrigendum 2.0.2 section 7.8.3.2 that says if one of the expressions given at built is null, we
     * have to ensure all geometric properties of the candidate intersect the other expression.
     *
     * @param candidate The object to extract all geometric properties from.
     * @param fixed
     * @return
     */
    private static boolean multiIntersect(Object candidate, Envelope fixed) {
        // TODO: We could optimize by caching feature-type properties. The best way would be an initialisation
        // procedure freezing target data type, but I'm not sure such a mechanism would be possible.
        final GeneralEnvelope constEnv = GeneralEnvelope.castOrCopy(fixed);
        if (candidate instanceof Feature) {
            final Feature f = (Feature) candidate;
            final FeatureType type = f.getType();
            /* Note: for now, we could have doublons, but have no simple mean to eliminate link operations. Relying on
             * convention naming is too risky, as some drivers could use it directly on their attributes, or create a
             * computational operation (create point from numeric columns, reproject geometry, etc.). In such case, we
             * would drop valuable information.
             */
            return type.getProperties(true)
                    .stream()
                    .filter(p
                            -> Features.castOrUnwrap(p)
                            .map(AttributeType::getValueClass)
                            .filter(Geometries::isKnownType)
                            .isPresent()
                    )
                    .map(p -> p.getName().toString())
                    .map(f::getPropertyValue)
                    .map(Geometries::getEnvelope)
                    .allMatch(fEnv -> fEnv != null && intersect(constEnv, fEnv));
        } else if (candidate instanceof Envelope) {
            return intersect(constEnv, (Envelope) candidate);
        } else {
            final Envelope env = Geometries.getEnvelope(candidate);
            if (env == null) throw new UnsupportedOperationException(
                    "Candidate type unsupported: "+candidate == null ? "null" : candidate.getClass().getCanonicalName()
            );
            return intersect(constEnv, env);
        }
    }

    private static Envelope asEnvelope(final Expression evaluator, final Object data) {
        Envelope eval = evaluator.evaluate(data, Envelope.class);
        if (eval == null) {
            final Object tmpVal = evaluator.evaluate(data);
            if (tmpVal instanceof Envelope) {
                eval = (Envelope) tmpVal;
            } else if (tmpVal instanceof GeographicBoundingBox) {
                eval = new GeneralEnvelope((GeographicBoundingBox) tmpVal);
            } else {
                eval = Geometries.getEnvelope(tmpVal);
            }
        }

        return eval;
    }

    private static Predicate asOptimizedTest(Literal constant, Expression other) {
        final ImmutableEnvelope constEnv = new ImmutableEnvelope(asEnvelope(constant, null));
        return other == null ? it -> multiIntersect(it, constEnv) : it -> intersect(it, other, constEnv);
    }

    /**
     * Ensure that given envelopes intersect, transforming them in a common suitable system if needed.
     */
    private static boolean intersect(AbstractEnvelope left, Envelope right) {
        final CRSMatching.Match bridge = CRSMatching
                .left(left.getCoordinateReferenceSystem())
                .right(right.getCoordinateReferenceSystem());
        final CoordinateOperation left2CommonOp = bridge.fromLeft().orElse(null);
        final CoordinateOperation right2CommonOp = bridge.fromRight().orElse(null);
        try {
            if (left2CommonOp != null) left = Envelopes.transform(left2CommonOp, left);
            if (right2CommonOp != null) right = Envelopes.transform(right2CommonOp, right);
        } catch (TransformException e) {
            throw new BackingStoreException(e);
        }

        return left.intersects(right, true); // See class doc for why true here.
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        init();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultBBOX that = (DefaultBBOX) o;
        return Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
    /*
     * DEPRECATED OPERATIONS: NOT IMPLEMENTED
     */

    @Override
    public String getPropertyName() {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 08/10/2019
    }

    @Override
    public String getSRS() {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 08/10/2019
    }

    @Override
    public double getMinX() {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 08/10/2019
    }

    @Override
    public double getMinY() {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 08/10/2019
    }

    @Override
    public double getMaxX() {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 08/10/2019
    }

    @Override
    public double getMaxY() {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 08/10/2019
    }
}
