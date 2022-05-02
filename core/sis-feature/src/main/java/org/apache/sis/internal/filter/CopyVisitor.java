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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.DistanceOperator;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.Literal;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.MatchAction;
import org.opengis.filter.NilOperator;
import org.opengis.filter.NullOperator;
import org.opengis.filter.ValueReference;
import org.opengis.geometry.Envelope;
import org.opengis.util.CodeList;

/**
 * Visitor used to copy expressions and filters from one factory to another.
 * This class purpose is to offer a way to convert filters to different and
 * often more specialized and efficient implementations such as for Coverages or SQL.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CopyVisitor<R,T> extends Visitor<R,AtomicReference> {

    private final FilterFactory<T,Object,Object> targetFactory;

    /**
     * Create a new copy visitor with given factory.
     *
     * @param targetFactory not null
     */
    public CopyVisitor(FilterFactory<T,?,?> targetFactory) {
        ArgumentChecks.ensureNonNull("factory", targetFactory);
        this.targetFactory = (FilterFactory<T, Object, Object>) targetFactory;
        setBinaryComparisonHandlers(this::copyBinaryComparison);
        setBinaryTemporalHandlers(this::copyBinaryTemporal);
        setLogicalHandlers(this::copyLogical);
        setSpatialHandlers(this::copySpatial);
        setMathHandlers(this::copyMath);
    }

    /**
     * Copy given filter using given factory.
     *
     * @param filter filter to copy
     * @return copied filter.
     */
    public Filter<T> copy(Filter<R> filter) {
        final AtomicReference ref = new AtomicReference();
        visit(filter, ref);
        return (Filter) ref.get();
    }

    /**
     * Copy given expression using given factory.
     *
     * @param expression expression to copy
     * @return copied expression.
     */
    public Expression<T,Object> copy(Expression expression) {
        final AtomicReference<Expression<T, Object>> ref = new AtomicReference();
        visit(expression, ref);
        return ref.get();
    }

    private List<Expression<T,Object>> copyExpressions(List<Expression<? super R,?>> source) {
        final List<Expression<T,Object>> results = new ArrayList<>(source.size());
        for (Expression e : source) {
            results.add(copy(e));
        }
        return results;
    }

    private List<Filter<T>> copyOperands(List<Filter<R>> source) {
        final List<Filter<T>> results = new ArrayList<>(source.size());
        for (Filter<R> e : source) {
            results.add(copy(e));
        }
        return results;
    }

    private void copyBinaryComparison(Filter<R> t, AtomicReference u) {
        final BinaryComparisonOperator co = (BinaryComparisonOperator) t;
        final List<Expression<T, Object>> exps = copyExpressions(t.getExpressions());
        final MatchAction ma = co.getMatchAction();
        final boolean mc = co.isMatchingCase();
        final Filter<T> result;
        switch (t.getOperatorType().identifier()) {
            case "PropertyIsEqualTo" :              result = targetFactory.equal(exps.get(0), exps.get(1), mc, ma); break;
            case "PropertyIsNotEqualTo" :           result = targetFactory.notEqual(exps.get(0), exps.get(1), mc, ma); break;
            case "PropertyIsLessThan" :             result = targetFactory.less(exps.get(0), exps.get(1), mc, ma); break;
            case "PropertyIsGreaterThan" :          result = targetFactory.greater(exps.get(0), exps.get(1), mc, ma); break;
            case "PropertyIsLessThanOrEqualTo" :    result = targetFactory.lessOrEqual(exps.get(0), exps.get(1), mc, ma); break;
            case "PropertyIsGreaterThanOrEqualTo" : result = targetFactory.greaterOrEqual(exps.get(0), exps.get(1), mc, ma); break;
            default : throw new IllegalArgumentException("Unknowned filter type " + t.getOperatorType().identifier());
        }
        u.set(result);
    }

    private void copyBinaryTemporal(Filter<R> t, AtomicReference u) {
        final List<Expression<T, Object>> exps = copyExpressions(t.getExpressions());
        final Filter<T> result;
        switch (t.getOperatorType().identifier()) {
            case "After" :          result = targetFactory.after(exps.get(0), exps.get(1)); break;
            case "Before" :         result = targetFactory.before(exps.get(0), exps.get(1)); break;
            case "Begins" :         result = targetFactory.begins(exps.get(0), exps.get(1)); break;
            case "BegunBy" :        result = targetFactory.begunBy(exps.get(0), exps.get(1)); break;
            case "TContains" :      result = targetFactory.tcontains(exps.get(0), exps.get(1)); break;
            case "During" :         result = targetFactory.during(exps.get(0), exps.get(1)); break;
            case "TEquals" :        result = targetFactory.tequals(exps.get(0), exps.get(1)); break;
            case "TOverlaps" :      result = targetFactory.toverlaps(exps.get(0), exps.get(1)); break;
            case "Meets" :          result = targetFactory.meets(exps.get(0), exps.get(1)); break;
            case "Ends" :           result = targetFactory.ends(exps.get(0), exps.get(1)); break;
            case "OverlappedBy" :   result = targetFactory.overlappedBy(exps.get(0), exps.get(1)); break;
            case "MetBy" :          result = targetFactory.metBy(exps.get(0), exps.get(1)); break;
            case "EndedBy" :        result = targetFactory.endedBy(exps.get(0), exps.get(1)); break;
            case "AnyInteracts" :   result = targetFactory.anyInteracts(exps.get(0), exps.get(1)); break;
            default : throw new IllegalArgumentException("Unknowned filter type " + t.getOperatorType().identifier());
        }
        u.set(result);
    }

    private void copyLogical(Filter<R> t, AtomicReference u) {
        final LogicalOperator co = (LogicalOperator) t;
        final List<Filter<T>> ops = copyOperands(co.getOperands());
        final Filter<T> result;
        switch (t.getOperatorType().identifier()) {
            case "And" : result = targetFactory.and(ops); break;
            case "Or" :  result = targetFactory.or(ops); break;
            case "Not" : result = targetFactory.not(ops.get(0)); break;
            default : throw new IllegalArgumentException("Unknowned filter type " + t.getOperatorType().identifier());
        }
        u.set(result);
    }

    private void copySpatial(Filter<R> t, AtomicReference u) {
        final List<Expression<T, Object>> exps = copyExpressions(t.getExpressions());
        final Filter<T> result;
        switch (t.getOperatorType().identifier()) {
            case "BBOX" :       result = targetFactory.bbox(exps.get(0), (Envelope) exps.get(1).apply(null)); break;
            case "Equals" :     result = targetFactory.equals(exps.get(0), exps.get(1)); break;
            case "Disjoint" :   result = targetFactory.disjoint(exps.get(0), exps.get(1)); break;
            case "Intersects" : result = targetFactory.intersects(exps.get(0), exps.get(1)); break;
            case "Touches" :    result = targetFactory.touches(exps.get(0), exps.get(1)); break;
            case "Crosses" :    result = targetFactory.crosses(exps.get(0), exps.get(1)); break;
            case "Within" :     result = targetFactory.within(exps.get(0), exps.get(1)); break;
            case "Contains" :   result = targetFactory.contains(exps.get(0), exps.get(1)); break;
            case "Overlaps" :   result = targetFactory.overlaps(exps.get(0), exps.get(1)); break;
            case "DWithin" :    result = targetFactory.within(exps.get(0), exps.get(1), ((DistanceOperator) t).getDistance()); break;
            case "Beyond" :     result = targetFactory.beyond(exps.get(0), exps.get(1), ((DistanceOperator) t).getDistance()); break;
            default : throw new IllegalArgumentException("Unknowned filter type " + t.getOperatorType().identifier());
        }
        u.set(result);
    }

    private void copyMath(Expression<R,?> t, AtomicReference u) {
        final List<Expression> exps = (List) copyExpressions(t.getParameters());
        final Expression<T,?> result;
        switch (t.getFunctionName().toString()) {
            case FunctionNames.Add : result = targetFactory.add(exps.get(0), exps.get(1)); break;
            case FunctionNames.Subtract : result = targetFactory.subtract(exps.get(0), exps.get(1)); break;
            case FunctionNames.Multiply : result = targetFactory.multiply(exps.get(0), exps.get(1)); break;
            case FunctionNames.Divide : result = targetFactory.divide(exps.get(0), exps.get(1)); break;
            default : throw new IllegalArgumentException("Unknowned expression type " + t.getFunctionName().toString());
        }
        u.set(result);
    }

    @Override
    protected void typeNotFound(CodeList<?> type, Filter<R> filter, AtomicReference u) {
        if (filter instanceof BetweenComparisonOperator) {
            final BetweenComparisonOperator op = (BetweenComparisonOperator) filter;
            u.set(targetFactory.between(copy(op.getExpression()), copy(op.getLowerBoundary()), copy(op.getUpperBoundary())));
        } else if (filter instanceof LikeOperator) {
            final LikeOperator<R> op = (LikeOperator) filter;
            u.set(targetFactory.like(
                    copy(op.getExpressions().get(0)),
                    (String) copy(op.getExpressions().get(1)).apply(null),
                    op.getWildCard(),
                    op.getSingleChar(),
                    op.getEscapeChar(),
                    op.isMatchingCase()));
        } else if (filter instanceof NilOperator) {
            final NilOperator<R> op = (NilOperator) filter;
            u.set(targetFactory.isNil(
                    copy(op.getExpressions().get(0)),
                    op.getNilReason().orElse(null)));
        } else if (filter instanceof NullOperator) {
            final NullOperator<R> op = (NullOperator) filter;
            u.set(targetFactory.isNull(copy(op.getExpressions().get(0))));
        } else {
            super.typeNotFound(type, filter, u);
        }
    }

    @Override
    protected void typeNotFound(String type, Expression<R, ?> expression, AtomicReference u) {
        if (expression instanceof Literal) {
            final Literal exp = (Literal) expression;
            u.set(targetFactory.literal(exp.getValue()));
        } else if (expression instanceof ValueReference) {
            final ValueReference exp = (ValueReference) expression;
            u.set(targetFactory.property(exp.getXPath()));
        } else {
            u.set(targetFactory.function(type, copyExpressions(expression.getParameters()).toArray(new Expression[0])));
        }
    }

}
