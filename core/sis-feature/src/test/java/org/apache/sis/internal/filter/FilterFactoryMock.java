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
package org.apache.sis.internal.filter;

import java.time.Instant;
import java.util.Map;
import java.util.Arrays;
import java.util.Collection;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.simple.SimpleCitation;

// Branch-dependent imports
import org.opengis.filter.*;
import org.opengis.filter.capability.FilterCapabilities;


/**
 * Dummy implementation of filter factory.
 * The features handled by this implementation are property-value maps.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.4
 * @since   1.4
 */
final class FilterFactoryMock implements FilterFactory<Map<String,?>, Object, Object> {
    /**
     * Creates a new dummy factory.
     */
    FilterFactoryMock() {
    }

    /**
     * Returns the "vendor" responsible for creating this factory implementation.
     */
    @Override
    public Citation getVendor() {
        return new SimpleCitation("SIS-tests");
    }

    /**
     * Creates an expression retrieving the value as an instance of the specified class.
     */
    @Override
    public <V> ValueReference<Map<String,?>, V> property(String xpath, Class<V> type) {
        return new ValueReferenceMock<>(xpath, type);
    }

    /**
     * Creates a dummy function with an arbitrary number of parameters.
     */
    @Override
    public Expression<Map<String,?>, ?> function(String name, Expression<? super Map<String,?>, ?>[] parameters) {
        return new FunctionMock(name, Arrays.asList(parameters));
    }

    // ======== All operations below this point are unsupported ===================================================

    /**
     * Unsupported operation.
     */
    @Override
    public FilterCapabilities getCapabilities() {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public ResourceId<Map<String,?>> resourceId(String rid, Version version, Instant startTime, Instant endTime) {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public <V> Literal<Map<String,?>, V> literal(V value) {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinaryComparisonOperator<Map<String,?>> equal(
            Expression<? super Map<String,?>, ?> expression1,
            Expression<? super Map<String,?>, ?> expression2,
            boolean isMatchingCase, MatchAction matchAction)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinaryComparisonOperator<Map<String,?>> notEqual(
            Expression<? super Map<String,?>, ?> expression1,
            Expression<? super Map<String,?>, ?> expression2,
            boolean isMatchingCase, MatchAction matchAction)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinaryComparisonOperator<Map<String,?>> less(
            Expression<? super Map<String,?>, ?> expression1,
            Expression<? super Map<String,?>, ?> expression2,
            boolean isMatchingCase, MatchAction matchAction)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinaryComparisonOperator<Map<String,?>> greater(
            Expression<? super Map<String,?>, ?> expression1,
            Expression<? super Map<String,?>, ?> expression2,
            boolean isMatchingCase, MatchAction matchAction)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinaryComparisonOperator<Map<String,?>> lessOrEqual(
            Expression<? super Map<String,?>, ?> expression1,
            Expression<? super Map<String,?>, ?> expression2,
            boolean isMatchingCase, MatchAction matchAction)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinaryComparisonOperator<Map<String,?>> greaterOrEqual(
            Expression<? super Map<String,?>, ?> expression1,
            Expression<? super Map<String,?>, ?> expression2,
            boolean isMatchingCase, MatchAction matchAction)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BetweenComparisonOperator<Map<String,?>> between(
            Expression<? super Map<String,?>, ?> expression,
            Expression<? super Map<String,?>, ?> lowerBoundary,
            Expression<? super Map<String,?>, ?> upperBoundary)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public LikeOperator<Map<String,?>> like(
            Expression<? super Map<String,?>, ?> expression,
            String pattern, char wildcard, char singleChar, char escape, boolean isMatchingCase)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public NullOperator<Map<String,?>> isNull(Expression<? super Map<String,?>, ?> expression) {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public NilOperator<Map<String,?>> isNil(Expression<? super Map<String,?>, ?> expression, String nilReason) {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public LogicalOperator<Map<String,?>> and(Collection<? extends Filter<? super Map<String,?>>> operands) {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public LogicalOperator<Map<String,?>> or(Collection<? extends Filter<? super Map<String,?>>> operands) {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public LogicalOperator<Map<String,?>> not(Filter<? super Map<String,?>> operand) {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinarySpatialOperator<Map<String,?>> bbox(
            Expression<? super Map<String,?>, ? extends Object> geometry,
            Envelope bounds)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinarySpatialOperator<Map<String,?>> equals(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinarySpatialOperator<Map<String,?>> disjoint(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinarySpatialOperator<Map<String,?>> intersects(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinarySpatialOperator<Map<String,?>> touches(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinarySpatialOperator<Map<String,?>> crosses(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinarySpatialOperator<Map<String,?>> within(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinarySpatialOperator<Map<String,?>> contains(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public BinarySpatialOperator<Map<String,?>> overlaps(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public DistanceOperator<Map<String,?>> beyond(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2,
            Quantity<Length> distance)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public DistanceOperator<Map<String,?>> within(
            Expression<? super Map<String,?>, ? extends Object> geometry1,
            Expression<? super Map<String,?>, ? extends Object> geometry2,
            Quantity<Length> distance)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> after(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> before(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> begins(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> begunBy(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> tcontains(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> during(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> tequals(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> toverlaps(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> meets(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> ends(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> overlappedBy(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> metBy(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> endedBy(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public TemporalOperator<Map<String,?>> anyInteracts(
            Expression<? super Map<String,?>, ? extends Object> time1,
            Expression<? super Map<String,?>, ? extends Object> time2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Expression<Map<String,?>, Number> add(
            Expression<? super Map<String,?>, ? extends Number> operand1,
            Expression<? super Map<String,?>, ? extends Number> operand2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Expression<Map<String,?>, Number> subtract(
            Expression<? super Map<String,?>, ? extends Number> operand1,
            Expression<? super Map<String,?>, ? extends Number> operand2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Expression<Map<String,?>, Number> multiply(
            Expression<? super Map<String,?>, ? extends Number> operand1,
            Expression<? super Map<String,?>, ? extends Number> operand2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Expression<Map<String,?>, Number> divide(
            Expression<? super Map<String,?>, ? extends Number> operand1,
            Expression<? super Map<String,?>, ? extends Number> operand2)
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Unsupported operation.
     */
    @Override
    public SortProperty<Map<String,?>> sort(ValueReference<? super Map<String,?>, ?> property, SortOrder order) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
