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
package org.apache.sis.filter.visitor;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.util.CodeList;
import org.opengis.geometry.Envelope;
import org.opengis.filter.*;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.feature.internal.Resources;


/**
 * Visitor used to copy expressions and filters with potentially a change of parameterized types.
 * This class can be used when filters need to be recreated using a different {@link FilterFactory},
 * for example because the type of resources changed. For example different filter implementations
 * may be needed if the filters need to operate on {@link org.apache.sis.coverage.grid.GridCoverage}
 * resources instead of {@link org.opengis.feature.Feature} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <SR>  the type of resources expected by the filters to copy (source resource).
 * @param  <TR>  the type of resources expected by the copied filters (target resource).
 * @param  <G>   base class of geometry objects.
 * @param  <T>   base class of temporal objects.
 */
public class CopyVisitor<SR,TR,G,T> extends Visitor<SR, List<Object>> {
    /**
     * The factory to use for creating the new filters and expressions.
     * Note that some methods in this factory may return {@code null}.
     * See <cite>Partially implemented factory</cite> in {@link EditVisitor} Javadoc.
     */
    protected final FilterFactory<TR,G,T> factory;

    /**
     * Whether to force creation of new filters or expressions even when the operands did not changed.
     * If {@code false}, new filters and expressions are created only when at least one operand changed.
     * This flag should be {@code true} if the {@code <SR>} and {@code <TR>} types are not the same,
     * or if the user knows that the {@linkplain #factory} may create a different kind of object even
     * when the operands are the same.
     */
    private final boolean forceNew;

    /**
     * Whether to force the use of newly created filters or expressions even when they are equal to the original ones.
     * If {@code false}, then the previously existing filters or expressions will be reused when the newly created
     * instances are equal according to {@link Object#equals(Object)}. Note this sharing requires that the filter
     * or expression to reuse expects a base resource type {@code <R>} which is common to both {@code <? super SR>}
     * and {@code <? super TR>}. We have no way to verify that.
     *
     * <p>This flag should be {@code true} if the filters or expressions are mutable.
     * Otherwise it may be {@code false} as a way to share existing instances.</p>
     */
    private final boolean forceUse;

    /**
     * The factory method to invoke for creating a binary comparison operator.
     * This is used for invoking one of the {@link FilterFactory} methods for
     * a given {@link ComparisonOperatorName}. Example:
     *
     * {@snippet lang="java" :
     *     copyVisitor.setCopyHandler(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, FilterFactory::equal);
     * }
     *
     * @param  <R>  the same type of resources specified by the enclosing {@link CopyVisitor} class.
     *
     * @see CopyVisitor#setCopyHandler(ComparisonOperatorName, BinaryComparisonFactory)
     */
    @FunctionalInterface
    protected interface BinaryComparisonFactory<R> {
        /**
         * Creates a new binary comparison operator from the given operands.
         *
         * @param  factory         the factory to use for creating the filter
         * @param  expression1     the first of the two expressions to be used by this comparator.
         * @param  expression2     the second of the two expressions to be used by this comparator.
         * @param  isMatchingCase  specifies whether comparisons are case sensitive.
         * @param  matchAction     specifies how the comparisons shall be evaluated for a collection of values.
         * @return the new filter, or {@code null} if this method cannot create a filter from the given arguments.
         */
        BinaryComparisonOperator<R> create(
                FilterFactory<R,?,?> factory,
                Expression<R,?> expression1,
                Expression<R,?> expression2,
                boolean isMatchingCase, MatchAction matchAction);
    }

    /**
     * The factory method to invoke for creating a temporal operator.
     * This is used for invoking one of the {@link FilterFactory} methods
     * for a given {@link TemporalOperatorName}. Example:
     *
     * {@snippet lang="java" :
     *     copyVisitor.setCopyHandler(TemporalOperatorName.AFTER, FilterFactory::after);
     * }
     *
     * @param  <R>  the same type of resources specified by the enclosing {@link CopyVisitor} class.
     * @param  <T>  base class of temporal objects specified by the enclosing class.
     *
     * @see CopyVisitor#setCopyHandler(TemporalOperatorName, TemporalComparisonFactory)
     */
    @FunctionalInterface
    protected interface TemporalFactory<R,T> {
        /**
         * Creates a new temporal operator from the given operands.
         *
         * @param  factory  the factory to use for creating the filter.
         * @param  time1    expression fetching the first temporal value.
         * @param  time2    expression fetching the second temporal value.
         * @return the new filter, or {@code null} if this method cannot create a filter from the given arguments.
         */
        TemporalOperator<R> create(
                FilterFactory<R,?,T> factory,
                Expression<R, ? extends T> time1,
                Expression<R, ? extends T> time2);
    }

    /**
     * The factory method to invoke for creating a spatial operator.
     * This is used for invoking one of the {@link FilterFactory} methods
     * for a given {@link SpatialOperatorName}. Example:
     *
     * {@snippet lang="java" :
     *     copyVisitor.setCopyHandler(SpatialOperatorName.EQUALS, FilterFactory::equals);
     * }
     *
     * @param  <R>  the same type of resources specified by the enclosing {@link CopyVisitor} class.
     * @param  <G>  base class of geometry objects specified by the enclosing class.
     *
     * @see CopyVisitor#setCopyHandler(SpatialOperatorName, SpatialFactory)
     */
    @FunctionalInterface
    protected interface SpatialFactory<R,G> {
        /**
         * Creates a new spatial operator from the given operands.
         *
         * @param  factory    the factory to use for creating the filter.
         * @param  geometry1  expression fetching the first geometry of the binary operator.
         * @param  geometry2  expression fetching the second geometry of the binary operator.
         * @return the new filter, or {@code null} if this method cannot create a filter from the given arguments.
         */
        BinarySpatialOperator<R> create(
                FilterFactory<R,G,?> factory,
                Expression<R, ? extends G> geometry1,
                Expression<R, ? extends G> geometry2);

        /**
         * Bridge for the "bbox" operation, because it uses a different method signature.
         * Usage example:
         *
         * {@snippet lang="java" :
         *     copyVisitor.setCopyHandler(SpatialOperatorName.BBOX, SpatialFactory::bbox);
         * }
         *
         * @param  <R>        the {@link CopyVisitor} type of resources.
         * @param  <G>        base class of geometry objects specified by {@code CopyVisitor}.
         * @param  factory    the factory to use for creating the filter.
         * @param  geometry1  expression fetching the geometry to check for interaction with bounds.
         * @param  geometry2  the bounds to check geometry against.
         * @return the new filter, or {@code null} if this method cannot create a filter from the given arguments.
         *
         * @see SpatialOperatorName#BBOX
         */
        static <R,G> BinarySpatialOperator<R> bbox(
                final FilterFactory<R,G,?> factory,
                final Expression<R, ? extends G> geometry1,
                final Expression<R, ? extends G> geometry2)
        {
            if (geometry2 instanceof Literal<?,?>) {
                final Object bounds = ((Literal<?,?>) geometry2).getValue();
                if (bounds instanceof Envelope) {
                    return factory.bbox(geometry1, (Envelope) bounds);
                }
            }
            return null;
        }
    }

    /**
     * The factory method to invoke for creating a distance operator.
     * This is used for invoking one of the {@link FilterFactory} methods
     * for a given {@link DistanceOperatorName}. Example:
     *
     * {@snippet lang="java" :
     *     copyVisitor.setCopyHandler(DistanceOperatorName.BEYOND, FilterFactory::beyond);
     * }
     *
     * @param  <R>  the same type of resources specified by the enclosing {@link CopyVisitor} class.
     * @param  <G>  base class of geometry objects specified by the enclosing class.
     *
     * @see CopyVisitor#setCopyHandler(DistanceOperatorName, DistanceFactory)
     */
    @FunctionalInterface
    protected interface DistanceFactory<R,G> {
        /**
         * Creates a new spatial operator from the given operands.
         *
         * @param  factory    the factory to use for creating the filter.
         * @param  geometry1  expression fetching the first geometry of the binary operator.
         * @param  geometry2  expression fetching the second geometry of the binary operator.
         * @param  distance   distance for evaluating the expression as {@code true}.
         * @return the new filter, or {@code null} if this method cannot create a filter from the given arguments.
         */
        DistanceOperator<R> create(
                FilterFactory<R,G,?> factory,
                Expression<R, ? extends G> geometry1,
                Expression<R, ? extends G> geometry2,
                Quantity<Length> distance);
    }

    /**
     * The factory method to invoke for creating a logical operator.
     * This is used for invoking one of the {@link FilterFactory}
     * methods for a given {@link LogicalOperatorName}. Example:
     *
     * {@snippet lang="java" :
     *     copyVisitor.setCopyHandler(LogicalOperatorName.AND, FilterFactory::and);
     * }
     *
     * @param  <R>  the same type of resources specified by the enclosing {@link CopyVisitor} class.
     *
     * @see CopyVisitor#setCopyHandler(LogicalOperatorName, LogicalFactory)
     */
    @FunctionalInterface
    protected interface LogicalFactory<R> {
        /**
         * Creates a new binary comparison operator from the given operands.
         *
         * @param  factory   the factory to use for creating the filter.
         * @param  operands  a collection of operands.
         * @return the new filter, or {@code null} if this method cannot create a filter from the given arguments.
         */
        LogicalOperator<R> create(
                FilterFactory<R,?,?> factory,
                Collection<? extends Filter<R>> operands);

        /**
         * Bridge for the "not" operation, because it uses a different method signature.
         * This method signature shall be identical to {@code create(…)} method signature.
         * Usage example:
         *
         * {@snippet lang="java" :
         *     copyVisitor.setCopyHandler(LogicalOperatorName.NOT, LogicalFactory::not);
         * }
         *
         * @param  <R>       the {@link CopyVisitor} type of resources.
         * @param  factory   the factory to use for creating the filter.
         * @param  operands  the operand of the NOT operation.
         * @return a filter evaluating {@code NOT operand}.
         */
        static <R> LogicalOperator<R> not(
                final FilterFactory<R,?,?> factory,
                final Collection<? extends Filter<R>> operands)
        {
            Filter<R> op = Containers.peekIfSingleton(operands);
            return (op != null) ? factory.not(op) : null;
        }
    }

    /**
     * The factory method to invoke for creating a binary function.
     * This is used for invoking one of the {@link FilterFactory}
     * methods for a given arithmetic function name. Example:
     *
     * {@snippet lang="java" :
     *     copyVisitor.setCopyHandler("Multiply", FilterFactory::multiply);
     * }
     *
     * @param  <R>  the same type of resources specified by the enclosing {@link CopyVisitor} class.
     *
     * @see CopyVisitor#setCopyHandler(String, BinaryFunctionFactory)
     */
    @FunctionalInterface
    protected interface BinaryFunctionFactory<R> {
        /**
         * Creates a new binary function from the given operands.
         *
         * @param  factory   the factory to use for creating the filter.
         * @param  operand1  the first of the two expressions to be used by this function.
         * @param  operand2  the second of the two expressions to be used by this function.
         * @return the new expression, or {@code null} if this method cannot create an expression from the given arguments.
         */
        Expression<R,Number> create(
                FilterFactory<R,?,?> factory,
                Expression<R, ? extends Number> operand1,
                Expression<R, ? extends Number> operand2);
    }

    /**
     * Creates a new copy visitor with the given factory.
     *
     * @param  factory  the factory to use for creating the new filters and expressions.
     * @param  force    whether to force new filters or expressions even when existing instances could be reused.
     */
    public CopyVisitor(final FilterFactory<TR,G,T> factory, final boolean force) {
        this(factory, true, force);
    }

    /**
     * Creates a new copy visitor with the given factory.
     * The {@code forceNew} argument can be {@code false}
     * only if {@code <SR>} and {@code <TR>} are the same type.
     *
     * @param  newFactory  the factory to use for creating the new filters and expressions.
     * @param  forceNew    whether to force creation of new filters or expressions even when the operands did not changed.
     * @param  forceUse    whether to force the use of newly created filters or expressions even when they are equal to the original ones.
     */
    @SuppressWarnings("this-escape")
    CopyVisitor(final FilterFactory<TR,G,T> newFactory, final boolean forceNew, final boolean forceUse) {
        this.factory  = Objects.requireNonNull(newFactory);
        this.forceNew = forceNew;
        this.forceUse = forceUse;
        setCopyHandler(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO,                 FilterFactory::equal);
        setCopyHandler(ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO,             FilterFactory::notEqual);
        setCopyHandler(ComparisonOperatorName.PROPERTY_IS_LESS_THAN,                FilterFactory::less);
        setCopyHandler(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN,             FilterFactory::greater);
        setCopyHandler(ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO,    FilterFactory::lessOrEqual);
        setCopyHandler(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO, FilterFactory::greaterOrEqual);
        setCopyHandler(  TemporalOperatorName.AFTER,                                FilterFactory::after);
        setCopyHandler(  TemporalOperatorName.BEFORE,                               FilterFactory::before);
        setCopyHandler(  TemporalOperatorName.BEGINS,                               FilterFactory::begins);
        setCopyHandler(  TemporalOperatorName.BEGUN_BY,                             FilterFactory::begunBy);
        setCopyHandler(  TemporalOperatorName.CONTAINS,                             FilterFactory::tcontains);
        setCopyHandler(  TemporalOperatorName.DURING,                               FilterFactory::during);
        setCopyHandler(  TemporalOperatorName.EQUALS,                               FilterFactory::tequals);
        setCopyHandler(  TemporalOperatorName.OVERLAPS,                             FilterFactory::toverlaps);
        setCopyHandler(  TemporalOperatorName.MEETS,                                FilterFactory::meets);
        setCopyHandler(  TemporalOperatorName.ENDS,                                 FilterFactory::ends);
        setCopyHandler(  TemporalOperatorName.OVERLAPPED_BY,                        FilterFactory::overlappedBy);
        setCopyHandler(  TemporalOperatorName.MET_BY,                               FilterFactory::metBy);
        setCopyHandler(  TemporalOperatorName.ENDED_BY,                             FilterFactory::endedBy);
        setCopyHandler(  TemporalOperatorName.ANY_INTERACTS,                        FilterFactory::anyInteracts);
        setCopyHandler(   SpatialOperatorName.BBOX,                                SpatialFactory::bbox);
        setCopyHandler(   SpatialOperatorName.EQUALS,                               FilterFactory::equals);
        setCopyHandler(   SpatialOperatorName.DISJOINT,                             FilterFactory::disjoint);
        setCopyHandler(   SpatialOperatorName.INTERSECTS,                           FilterFactory::intersects);
        setCopyHandler(   SpatialOperatorName.TOUCHES,                              FilterFactory::touches);
        setCopyHandler(   SpatialOperatorName.CROSSES,                              FilterFactory::crosses);
        setCopyHandler(   SpatialOperatorName.WITHIN,                               FilterFactory::within);
        setCopyHandler(   SpatialOperatorName.CONTAINS,                             FilterFactory::contains);
        setCopyHandler(   SpatialOperatorName.OVERLAPS,                             FilterFactory::overlaps);
        setCopyHandler(  DistanceOperatorName.WITHIN,                               FilterFactory::within);
        setCopyHandler(  DistanceOperatorName.BEYOND,                               FilterFactory::beyond);
        setCopyHandler(   LogicalOperatorName.AND,                                  FilterFactory::and);
        setCopyHandler(   LogicalOperatorName.OR,                                   FilterFactory::or);
        setCopyHandler(   LogicalOperatorName.NOT,                                 LogicalFactory::not);
        setCopyHandler(         FunctionNames.Add,                                  FilterFactory::add);
        setCopyHandler(         FunctionNames.Subtract,                             FilterFactory::subtract);
        setCopyHandler(         FunctionNames.Multiply,                             FilterFactory::multiply);
        setCopyHandler(         FunctionNames.Divide,                               FilterFactory::divide);
        /*
         * Following are factory methods with different signatures, but where each signature appears only once.
         * It is not worth to create e.g. a `TrinaryComparisonFactory` functional interface for only one method.
         */
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_BETWEEN), (filter, accumulator) -> {
            BetweenComparisonOperator<TR> target = null;
            BetweenComparisonOperator<SR> source = (BetweenComparisonOperator<SR>) filter;
            var exps = copyExpressions(source.getExpressions());
            if (exps != null && exps.size() == 2) {
                target = factory.between(exps.get(0), exps.get(1), exps.get(2));
            }
            accept(accumulator, source, target);
        });
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_LIKE), (filter, accumulator) -> {
            LikeOperator<TR> target = null;
            LikeOperator<SR> source = (LikeOperator<SR>) filter;
            var exps = copyExpressions(source.getExpressions());
            if (exps != null && exps.size() == 2) {
                final Expression<?,?> p2 = exps.get(1);
                if (p2 instanceof Literal<?,?>) {
                    final Object literal = ((Literal<?,?>) p2).getValue();
                    if (literal instanceof String) {
                        target = factory.like(exps.get(0), (String) literal, source.getWildCard(),
                                source.getSingleChar(), source.getEscapeChar(), source.isMatchingCase());
                    }
                }
            }
            accept(accumulator, source, target);
        });
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_NIL), (filter, accumulator) -> {
            NilOperator<TR> target = null;
            NilOperator<SR> source = (NilOperator<SR>) filter;
            var exps = copyExpressions(source.getExpressions());
            if (exps != null && exps.size() == 1) {
                target = factory.isNil(exps.get(0), source.getNilReason().orElse(null));
            }
            accept(accumulator, source, target);
        });
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_NULL), (filter, accumulator) -> {
            NullOperator<TR> target = null;
            NullOperator<SR> source = (NullOperator<SR>) filter;
            var exps = copyExpressions(source.getExpressions());
            if (exps != null && exps.size() == 1) {
                target = factory.isNull(exps.get(0));
            }
            accept(accumulator, source, target);
        });
        setExpressionHandler(FunctionNames.Literal, (expression, accumulator) -> {
            Literal<SR,?> source = (Literal<SR,?>) expression;
            Literal<TR,?> target = factory.literal(source.getValue());
            accept(accumulator, source, target);
        });
        setExpressionHandler(FunctionNames.ValueReference, (expression, accumulator) -> {
            ValueReference<SR,?> source = (ValueReference<SR,?>) expression;
            ValueReference<TR,?> target = factory.property(source.getXPath());
            accept(accumulator, source, target);
        });
    }

    /**
     * Sets the action to execute for the given type of binary comparison operator.
     * Example:
     *
     * {@snippet lang="java" :
     *     setCopyHandler(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, FilterFactory::equal);
     * }
     *
     * @param  type    identification of the filter type.
     * @param  action  the action to execute when the identified filter is found.
     */
    protected final void setCopyHandler(final ComparisonOperatorName type, final BinaryComparisonFactory<TR> action) {
        setFilterHandler(type, (filter, accumulator) -> {
            BinaryComparisonOperator<TR> target = null;
            BinaryComparisonOperator<SR> source = (BinaryComparisonOperator<SR>) filter;
            var exps = copyExpressions(source.getExpressions());
            if (exps != null && exps.size() == 2) {
                target = action.create(factory, exps.get(0), exps.get(1), source.isMatchingCase(), source.getMatchAction());
            }
            accept(accumulator, source, target);
        });
    }

    /**
     * Sets the action to execute for the given type of temporal operator.
     * Example:
     *
     * {@snippet lang="java" :
     *     setCopyHandler(TemporalOperatorName.AFTER, FilterFactory::after);
     * }
     *
     * @param  type    identification of the filter type.
     * @param  action  the action to execute when the identified filter is found.
     */
    protected final void setCopyHandler(final TemporalOperatorName type, final TemporalFactory<TR,T> action) {
        setFilterHandler(type, (filter, accumulator) -> {
            TemporalOperator<TR> target = null;
            TemporalOperator<SR> source = (TemporalOperator<SR>) filter;
            List<Expression<TR,T>> exps = copyExpressions(source.getExpressions());
            if (exps != null && exps.size() == 2) {
                target = action.create(factory, exps.get(0), exps.get(1));
            }
            accept(accumulator, source, target);
        });
    }

    /**
     * Sets the action to execute for the given type of spatial operator.
     * Example:
     *
     * {@snippet lang="java" :
     *     setCopyHandler(SpatialOperatorName.EQUALS, FilterFactory::equals);
     * }
     *
     * @param  type    identification of the filter type.
     * @param  action  the action to execute when the identified filter is found.
     */
    protected final void setCopyHandler(final SpatialOperatorName type, final SpatialFactory<TR,G> action) {
        setFilterHandler(type, (filter, accumulator) -> {
            BinarySpatialOperator<TR> target = null;
            BinarySpatialOperator<SR> source = (BinarySpatialOperator<SR>) filter;
            List<Expression<TR,G>> exps = copyExpressions(source.getExpressions());
            if (exps != null && exps.size() == 2) {
                target = action.create(factory, exps.get(0), exps.get(1));
            }
            accept(accumulator, source, target);
        });
    }

    /**
     * Sets the action to execute for the given type of distance operator.
     * Example:
     *
     * {@snippet lang="java" :
     *     setCopyHandler(DistanceOperatorName.BEYOND, FilterFactory::beyond);
     * }
     *
     * @param  type    identification of the filter type.
     * @param  action  the action to execute when the identified filter is found.
     */
    protected final void setCopyHandler(final DistanceOperatorName type, final DistanceFactory<TR,G> action) {
        setFilterHandler(type, (filter, accumulator) -> {
            DistanceOperator<TR> target = null;
            DistanceOperator<SR> source = (DistanceOperator<SR>) filter;
            List<Expression<TR,G>> exps = copyExpressions(source.getExpressions());
            if (exps != null && exps.size() == 3) {
                target = action.create(factory, exps.get(0), exps.get(1), source.getDistance());
            }
            accept(accumulator, source, target);
        });
    }

    /**
     * Sets the action to execute for the given type of logical operator.
     * Example:
     *
     * {@snippet lang="java" :
     *     setCopyHandler(LogicalOperatorName.AND, FilterFactory::and);
     * }
     *
     * @param  type    identification of the filter type.
     * @param  action  the action to execute when the identified filter is found.
     */
    protected final void setCopyHandler(final LogicalOperatorName type, final LogicalFactory<TR> action) {
        setFilterHandler(type, (filter, accumulator) -> {
            LogicalOperator<TR> target = null;
            LogicalOperator<SR> source = (LogicalOperator<SR>) filter;
            final var exps = copyFilters(source.getOperands());
            if (exps != null) {
                target = action.create(factory, exps);
            }
            accept(accumulator, source, target);
        });
    }

    /**
     * Sets the action to execute for the given function.
     * Example:
     *
     * {@snippet lang="java" :
     *     setCopyHandler("Multiply", FilterFactory::multiply);
     * }
     *
     * @param  name    identification of the function.
     * @param  action  the action to execute when the identified function is found.
     */
    protected final void setCopyHandler(final String name, final BinaryFunctionFactory<TR> action) {
        setExpressionHandler(name, (source, accumulator) -> {
            Expression<TR,?> target = null;
            List<Expression<TR,Number>> exps = copyExpressions(source.getParameters());
            if (exps != null) {
                target = action.create(factory, exps.get(0), exps.get(1));
            }
            accept(accumulator, source, target);
        });
    }

    /**
     * Adds the target filter or expression in the list of copied elements.
     *
     * @param  accumulator  the list of copied elements where to add the {@code target} element.
     * @param  source       the original filter or expression which has been copied.
     * @param  target       the copied filter or expression, or {@code null} if the copy could not be done.
     * @throws IllegalArgumentException if {@code source} copy was mandated and couldn't be done.
     */
    private void accept(final List<Object> accumulator, final Object source, Object target) {
        if (target == null) {
            if (forceNew) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.CanNotCopy_1, source));
            }
            target = source;
        } else if (!forceUse && target.equals(source)) {
            target = source;
        }
        accumulator.add(target);
    }

    /**
     * Copies all expressions that are in the given list.
     * The returned list has the same length as the given list.
     * If all copied expressions are equal to the original expressions and {@link #forceNew} is {@code false},
     * then this method returns {@code null} for telling that filter or expression does not need to be created.
     *
     * <h4>Note on parameterized types</h4>
     * This method cannot guarantee that the elements in the returned list have really the parameterized types
     * declared in this method signature. They <em>should be</em> if the {@link FilterFactory}, {@link Filter}
     * and {@link Expression} methods invoked by the caller fulfill the API contract. For example the operands
     * of an arithmetic function should have {@link Number} value. But we have no way to verify that.
     *
     * @param  <V>  expected type of values returned by the expressions. This type <em>is not verified</em>,
     *         so this method is not really type-safe. This parameterized type is nevertheless used for more
     *         convenient casts by the callers, but should not be used outside private methods.
     * @param  source  the list of expressions to copy.
     * @return the copies expressions, or {@code null} if no copy is needed.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private <V> List<Expression<TR,V>> copyExpressions(final List<Expression<SR,?>> source) {
        final List<Object> results = new ArrayList<>(source.size());
        for (final Expression<SR,?> e : source) {
            visit(e, results);
        }
        // Cast to <TR> is safe because of factory method signatures.
        return !forceNew && results.equals(source) ? null : (List) results;
    }

    /**
     * Copies all filters that are in the given list.
     * The returned list has the same length as the given list.
     * If all copied filters are equal to the original filters and {@link #forceNew} is {@code false},
     * then this method returns {@code null} for telling that filter does not need to be created.
     *
     * @param  source  the list of filters to copy.
     * @return the copies filters, or {@code null} if no copy is needed.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private List<Filter<TR>> copyFilters(final List<Filter<SR>> source) {
        final var results = new ArrayList<Object>(source.size());
        for (final Filter<SR> e : source) {
            visit(e, results);
        }
        // Cast to <TR> is safe because of factory method signatures.
        return !forceNew && results.equals(source) ? null : (List) results;
    }

    /**
     * Copies the given filter using the factory specified at construction time.
     *
     * @param  source  the filter to copy.
     * @return the copied filter.
     * @throws IllegalArgumentException if the filter cannot be copied.
     */
    @SuppressWarnings("unchecked")
    public Filter<TR> copy(final Filter<SR> source) {
        final var accumulator = new ArrayList<Object>(1);
        visit(source, accumulator);
        switch (accumulator.size()) {
            case 0:  return null;
            case 1:  return (Filter<TR>) accumulator.get(0);
            default: throw new AssertionError(accumulator);     // Should never happen.
        }
    }

    /**
     * Copies the given expression using the factory specified at construction time.
     *
     * @param  <V>     the type of values computed by the expression.
     * @param  source  the expression to copy.
     * @return the copied expression.
     * @throws IllegalArgumentException if the expression cannot be copied.
     */
    @SuppressWarnings("unchecked")
    public <V> Expression<TR,V> copy(final Expression<SR,V> source) {
        final var accumulator = new ArrayList<Object>(1);
        visit(source, accumulator);
        switch (accumulator.size()) {
            case 0:  return null;
            case 1:  return (Expression<TR,V>) accumulator.get(0);
            default: throw new AssertionError(accumulator);     // Should never happen.
        }
    }

    /**
     * Invoked when no copy operation is registered for the given filter.
     * The default implementation throws an {@link IllegalArgumentException}.
     *
     * @param  type         the filter type which has not been found.
     * @param  filter       the filter to copy.
     * @param  accumulator  where to add filters.
     * @throws IllegalArgumentException if a copy of the given filter was required by cannot be performed.
     */
    @Override
    protected void typeNotFound(final CodeList<?> type, final Filter<SR> filter, final List<Object> accumulator) {
        if (forceNew) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.CanNotVisit_2, 0, type));
        }
        accumulator.add(filter);
    }

    /**
     * Invoked when no copy operation is registered for the given expression.
     * The default implementation creates a new function of the same name using the generic API.
     *
     * @param  name         the expression type which has not been found.
     * @param  expression   the expression.
     * @param  accumulator  where to add expressions.
     * @throws IllegalArgumentException if an expression cannot be copied.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void typeNotFound(final String name, Expression<SR,?> expression, final List<Object> accumulator) {
        var exps = copyExpressions(expression.getParameters());
        if (exps != null) {
            expression = factory.function(name, exps.toArray(Expression[]::new));
        }
        accumulator.add(expression);
    }
}
