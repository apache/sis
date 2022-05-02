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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.Names;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.DistanceOperator;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.InvalidFilterValueException;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.Literal;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.MatchAction;
import org.opengis.filter.NilOperator;
import org.opengis.filter.NullOperator;
import org.opengis.filter.ResourceId;
import org.opengis.filter.SortOrder;
import org.opengis.filter.SortProperty;
import org.opengis.filter.TemporalOperator;
import org.opengis.filter.ValueReference;
import org.opengis.filter.Version;
import org.opengis.filter.capability.FilterCapabilities;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.citation.Citation;
import org.opengis.util.ScopedName;

/**
 * Verifies copy from {@link CopyVisitor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CopyVisitorTest extends TestCase {

    /**
     * Test copy a value reference.
     */
    @Test
    public void copyValueReference() {
        final FilterFactory<Feature,Object,?> source = DefaultFilterFactory.forFeatures();
        final FilterFactory<Map,Object,Object> target = new MockFactory();

        final Expression<Feature,?> exp = source.property("name");
        final Expression<Map, Object> result = new CopyVisitor<>(target).copy(exp);

        assertTrue(result instanceof MockValueReference);
    }

    /**
     * Test copy a function.
     */
    @Test
    public void copyFunction() {
        final FilterFactory<Feature,Object,?> source = DefaultFilterFactory.forFeatures();
        final FilterFactory<Map,Object,Object> target = new MockFactory();

        final Expression<Feature,?> exp1 = source.property("name");
        final Expression<Feature,?> exp2 = source.property("crs");
        final Expression<Feature,?> fct = source.function("ST_GeomFromText", exp1, exp2);
        final Expression<Map, Object> result = new CopyVisitor<>(target).copy(fct);

        assertTrue(result instanceof MockFunction);
        final MockFunction resultfct = (MockFunction) result;
        assertEquals(2, resultfct.getParameters().size());
        assertTrue(resultfct.getParameters().get(0) instanceof MockValueReference);
        assertTrue(resultfct.getParameters().get(1) instanceof MockValueReference);
    }
}

final class MockFactory implements FilterFactory<Map, Object, Object>{

    @Override
    public FilterCapabilities getCapabilities() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ResourceId<Map> resourceId(String rid, Version version, Instant startTime, Instant endTime) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public <V> ValueReference property(String xpath, Class<V> type) {
        return new MockValueReference(xpath);
    }

    @Override
    public <V> Literal<Map, V> literal(V value) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinaryComparisonOperator<Map> equal(Expression<? super Map, ?> expression1, Expression<? super Map, ?> expression2, boolean isMatchingCase, MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinaryComparisonOperator<Map> notEqual(Expression<? super Map, ?> expression1, Expression<? super Map, ?> expression2, boolean isMatchingCase, MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinaryComparisonOperator<Map> less(Expression<? super Map, ?> expression1, Expression<? super Map, ?> expression2, boolean isMatchingCase, MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinaryComparisonOperator<Map> greater(Expression<? super Map, ?> expression1, Expression<? super Map, ?> expression2, boolean isMatchingCase, MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinaryComparisonOperator<Map> lessOrEqual(Expression<? super Map, ?> expression1, Expression<? super Map, ?> expression2, boolean isMatchingCase, MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinaryComparisonOperator<Map> greaterOrEqual(Expression<? super Map, ?> expression1, Expression<? super Map, ?> expression2, boolean isMatchingCase, MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BetweenComparisonOperator<Map> between(Expression<? super Map, ?> expression, Expression<? super Map, ?> lowerBoundary, Expression<? super Map, ?> upperBoundary) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public LikeOperator<Map> like(Expression<? super Map, ?> expression, String pattern, char wildcard, char singleChar, char escape, boolean isMatchingCase) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NullOperator<Map> isNull(Expression<? super Map, ?> expression) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NilOperator<Map> isNil(Expression<? super Map, ?> expression, String nilReason) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public LogicalOperator<Map> and(Collection<? extends Filter<? super Map>> operands) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public LogicalOperator<Map> or(Collection<? extends Filter<? super Map>> operands) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public LogicalOperator<Map> not(Filter<? super Map> operand) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinarySpatialOperator<Map> bbox(Expression<? super Map, ? extends Object> geometry, Envelope bounds) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinarySpatialOperator<Map> equals(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinarySpatialOperator<Map> disjoint(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinarySpatialOperator<Map> intersects(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinarySpatialOperator<Map> touches(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinarySpatialOperator<Map> crosses(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinarySpatialOperator<Map> within(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinarySpatialOperator<Map> contains(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public BinarySpatialOperator<Map> overlaps(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DistanceOperator<Map> beyond(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2, Quantity<Length> distance) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DistanceOperator<Map> within(Expression<? super Map, ? extends Object> geometry1, Expression<? super Map, ? extends Object> geometry2, Quantity<Length> distance) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> after(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> before(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> begins(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> begunBy(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> tcontains(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> during(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> tequals(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> toverlaps(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> meets(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> ends(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> overlappedBy(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> metBy(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> endedBy(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TemporalOperator<Map> anyInteracts(Expression<? super Map, ? extends Object> time1, Expression<? super Map, ? extends Object> time2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Expression<Map, Number> add(Expression<? super Map, ? extends Number> operand1, Expression<? super Map, ? extends Number> operand2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Expression<Map, Number> subtract(Expression<? super Map, ? extends Number> operand1, Expression<? super Map, ? extends Number> operand2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Expression<Map, Number> multiply(Expression<? super Map, ? extends Number> operand1, Expression<? super Map, ? extends Number> operand2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Expression<Map, Number> divide(Expression<? super Map, ? extends Number> operand1, Expression<? super Map, ? extends Number> operand2) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Expression<Map, ?> function(String name, Expression<? super Map, ?>[] parameters) {
        return new MockFunction(name, Arrays.asList(parameters));
    }

    @Override
    public SortProperty<Map> sort(ValueReference<? super Map, ?> property, SortOrder order) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Citation getVendor() {
        throw new UnsupportedOperationException("Not supported.");
    }

}

final class MockValueReference implements ValueReference<Map, Object> {

    private final String xpath;

    public MockValueReference(String xpath) {
        this.xpath = xpath;
    }

    @Override
    public String getXPath() {
        return xpath;
    }

    @Override
    public Object apply(Map input) throws InvalidFilterValueException {
        return input.get(xpath);
    }

    @Override
    public <N> Expression<Map, N> toValueType(Class<N> target) {
        throw new UnsupportedOperationException("Not supported.");
    }

}

final class MockFunction implements Expression<Map,Object> {

    private final String name;
    private final List<Expression<? super Map, ?>> parameters;

    public MockFunction(String name, List<Expression<? super Map, ?>> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public ScopedName getFunctionName() {
        return Names.createScopedName(null, null, name);
    }

    @Override
    public List<Expression<? super Map, ?>> getParameters() {
        return parameters;
    }

    @Override
    public Object apply(Map input) throws InvalidFilterValueException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public <N> Expression<Map, N> toValueType(Class<N> target) {
        throw new UnsupportedOperationException("Not supported.");
    }

}
