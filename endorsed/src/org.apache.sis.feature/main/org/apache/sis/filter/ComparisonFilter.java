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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.util.List;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiPredicate;
import org.apache.sis.math.Fraction;
import org.apache.sis.filter.base.Node;
import org.apache.sis.filter.base.BinaryFunctionWidening;
import org.apache.sis.temporal.TimeMethods;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.CodeList;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.MatchAction;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BetweenComparisonOperator;


/**
 * Comparison operators between two values. Values are converted to the same type before comparison, using a widening
 * conversion (for example from {@link Integer} to {@link Double}). If values cannot be compared because they cannot
 * be converted to a common type, or because a value is null or NaN, then the comparison result is {@code false}.
 * A consequence of this rule is that the conditions {@literal A < B} and {@literal A ≥ B} may be false at the same time.
 *
 * <p>If one operand is a collection, all collection elements may be compared to the other value.
 * Null elements in the collection (not to be confused with null operands) are ignored.
 * If both operands are collections, current implementation returns {@code false}.</p>
 *
 * <p>Comparisons between temporal objects are done with {@code isBefore(…)} or {@code isAfter(…)} methods when they
 * have a different semantic than the {@code compareTo(…)} methods. If the two temporal objects are not of the same
 * type, only the fields that are common two both types are compared. For example, comparison between {@code LocalDate}
 * and {@code LocalDateTime} ignores the time fields.</p>
 *
 * <p>Comparisons of numerical types shall be done by overriding one of the {@code applyAs…} methods and
 * returning 0 if {@code false} or 1 if {@code true}. Comparisons of other types is done by overriding
 * the {@code compare(…)} methods.</p>
 *
 * @todo Delegate all comparisons of temporal objects to {@link TemporalFilter}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
abstract class ComparisonFilter<R> extends BinaryFunctionWidening<R, Object, Object>
        implements BinaryComparisonOperator<R>, Optimization.OnFilter<R>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1228683039737814926L;

    /**
     * Specifies whether comparisons are case sensitive.
     */
    protected final boolean isMatchingCase;

    /**
     * Specifies how the comparisons shall be evaluated for a collection of values.
     * Values can be ALL, ANY or ONE.
     */
    protected final MatchAction matchAction;

    /**
     * Creates a new comparator.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @param  isMatchingCase  specifies whether comparisons are case sensitive.
     * @param  matchAction     specifies how the comparisons shall be evaluated for a collection of values.
     */
    ComparisonFilter(final Expression<R,?> expression1,
                     final Expression<R,?> expression2,
                     final boolean isMatchingCase, final MatchAction matchAction)
    {
        super(expression1, expression2);
        this.isMatchingCase = isMatchingCase;
        this.matchAction = Objects.requireNonNull(matchAction);
    }

    /**
     * Returns the element on the left side of the comparison expression.
     * This is the element at index 0 in the {@linkplain #getExpressions() list of expressions}.
     */
    @Override
    public final Expression<R,?> getOperand1() {
        return expression1;
    }

    /**
     * Returns the element on the right side of the comparison expression.
     * This is the element at index 1 in the {@linkplain #getExpressions() list of expressions}.
     */
    @Override
    public final Expression<R,?> getOperand2() {
        return expression2;
    }

    /**
     * Returns whether comparisons are case sensitive.
     */
    @Override
    public final boolean isMatchingCase() {
        return isMatchingCase;
    }

    /**
     * Returns how the comparisons are evaluated for a collection of values.
     */
    @Override
    public final MatchAction getMatchAction() {
        return matchAction;
    }

    /**
     * Takes in account the additional properties in hash code calculation.
     */
    @Override
    public final int hashCode() {
        return super.hashCode() + Boolean.hashCode(isMatchingCase) + 61 * matchAction.hashCode();
    }

    /**
     * Takes in account the additional properties in object comparison.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (super.equals(obj)) {
            final var other = (ComparisonFilter<?>) obj;
            return other.isMatchingCase == isMatchingCase && matchAction.equals(other.matchAction);
        }
        return false;
    }

    /**
     * Whether to convert literals to the same type as non-literal parameters during the optimization phase.
     * This is invoked by {@link Optimization} for deciding whether to attempt such replacement.
     *
     * @return whether it is okay to convert literals in advance.
     */
    @Override
    public final boolean allowLiteralConversions() {
        return true;
    }

    /**
     * Creates a new filter of the same type and parameters than this filter, except for the expressions.
     * The given array shall be of length 2 and shall contain the replacement for {@link #expression1}
     * and {@link #expression2}, in that order.
     */
    @Override
    public abstract ComparisonFilter<R> recreate(Expression<R,?>[] effective);

    /**
     * Tries to optimize this filter. Fist, this method applies the optimization documented
     * in the {@linkplain Optimization.OnFilter#optimize default method impmementation}.
     * Then, if it is possible to avoid to inspect the number types every time that the
     * filter is evaluated, this method returns a more direct implementation.
     *
     * @param  optimization  the simplifications or optimizations to apply on this filter.
     * @return the simplified or optimized filter, or {@code this} if no optimization has been applied.
     */
    @Override
    public final Filter<R> optimize(final Optimization optimization) {
        final Filter<R> result = Optimization.OnFilter.super.optimize(optimization);
        if (result instanceof ComparisonFilter<?>) {
            final var optimized = (ComparisonFilter<R>) result;
            final Class<?> t1, t2;
            if (isSpecialized(t1 = getResultClass(expression1)) &&
                isSpecialized(t2 = getResultClass(expression2)))
            {
                final var numeric = optimized.new Numeric();
                if (numeric.evaluator != null) {
                    return numeric;
                }
                final var temporal = new Time<>(TimeMethods.forTypes(t1, t2), t2);
                if (temporal.evaluator != null) {
                    return temporal;
                }
                if (Comparable.class.isAssignableFrom(t1) && t1.isAssignableFrom(t2)) {
                    if (isMatchingCase || !CharSequence.class.isAssignableFrom(t1)) {
                        return new Comparables();
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns whether the given type is non-null and something more specialized than {@code Object}.
     * This is used for avoiding unnecessary class-loading of {@link Numeric} and {@link Time} when
     * they are sure to be unsuccessful.
     */
    private static boolean isSpecialized(final Class<?> type) {
        return (type != null) && (type != Object.class);
    }

    /**
     * An optimized versions of this filter for the case where the operands are numeric.
     */
    private final class Numeric extends Node implements Optimization.OnFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 4969425622445580192L;

        /** The expression which performs the comparison and returns the result as an integer. */
        @SuppressWarnings("serial") final Expression<R, ? extends Number> evaluator;

        /** Creates a new filter. Callers must verifies that {@link #evaluator} is non-null. */
        Numeric() {evaluator = specialize();}

        /** Delegates to the enclosing class.*/
        @Override public    CodeList<?>           getOperatorType()  {return ComparisonFilter.this.getOperatorType();}
        @Override public    Class<? super R>      getResourceClass() {return ComparisonFilter.this.getResourceClass();}
        @Override public    List<Expression<R,?>> getExpressions()   {return ComparisonFilter.this.getExpressions();}
        @Override protected Collection<?>         getChildren()      {return ComparisonFilter.this.getChildren();}

        /** Determines if the test represented by this filter passes with the given operands. */
        @Override public boolean test(final R candidate) {
            return ((Integer) evaluator.apply(candidate)) != 0;
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<R> recreate(Expression<R,?>[] effective) {
            return ComparisonFilter.this.recreate(effective).new Numeric();
        }
    }

    /**
     * An optimized versions of this filter for the case where the operands are temporal.
     */
    private final class Time<T,S> extends Node implements Optimization.OnFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -5132906457258846016L;

        /** The function which performs the comparisons. */
        @SuppressWarnings("serial") final BiPredicate<T,S> evaluator;

        /** Creates a new filter. Callers must verifies that {@link #evaluator} is non-null. */
        Time(final TimeMethods<T> methods, final Class<S> otherType) {
            evaluator = (methods != null) ? methods.predicate(temporalTest(), otherType) : null;
        }

        /** Creates a new filter which is reusing an already determined evaluator. */
        Time(final BiPredicate<T,S> evaluator) {
            this.evaluator = evaluator;
        }

        /** Delegates to the enclosing class.*/
        @Override public    CodeList<?>           getOperatorType()  {return ComparisonFilter.this.getOperatorType();}
        @Override public    Class<? super R>      getResourceClass() {return ComparisonFilter.this.getResourceClass();}
        @Override public    List<Expression<R,?>> getExpressions()   {return ComparisonFilter.this.getExpressions();}
        @Override protected Collection<?>         getChildren()      {return ComparisonFilter.this.getChildren();}

        /** Determines if the test represented by this filter passes with the given operands. */
        @Override public boolean test(final R candidate) {
            @SuppressWarnings("unchecked")
            final T left = (T) expression1.apply(candidate);
            if (left != null) {
                @SuppressWarnings("unchecked")
                final S right = (S) expression2.apply(candidate);
                if (right != null) {
                    return evaluator.test(left, right);
                }
            }
            return false;
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<R> recreate(Expression<R,?>[] effective) {
            return ComparisonFilter.this.recreate(effective).new Time<>(evaluator);
        }
    }

    /**
     * An optimized versions of this filter for the case where the operands are comparable.
     */
    private final class Comparables extends Node implements Optimization.OnFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -8311546273569455269L;

        /** Delegates to the enclosing class.*/
        @Override public    CodeList<?>           getOperatorType()  {return ComparisonFilter.this.getOperatorType();}
        @Override public    Class<? super R>      getResourceClass() {return ComparisonFilter.this.getResourceClass();}
        @Override public    List<Expression<R,?>> getExpressions()   {return ComparisonFilter.this.getExpressions();}
        @Override protected Collection<?>         getChildren()      {return ComparisonFilter.this.getChildren();}

        /** Determines if the test represented by this filter passes with the given operands. */
        @Override public boolean test(final R candidate) {
            final Object left = expression1.apply(candidate);
            if (left != null) {
                final Object right = expression2.apply(candidate);
                if (right != null) {
                    if (left.getClass() == right.getClass()) {
                        @SuppressWarnings("unchecked")
                        final int result = ((Comparable) left).compareTo(right);
                        return fromCompareTo(result);
                    }
                }
            }
            return false;
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<R> recreate(Expression<R,?>[] effective) {
            return ComparisonFilter.this.recreate(effective).new Comparables();
        }
    }

    /**
     * Determines if the test(s) represented by this filter passes with the given operands.
     * Values of {@link #expression1} and {@link #expression2} can be two single values,
     * or at most one expression can produce a collection.
     */
    @Override
    public final boolean test(final R candidate) {
        final Object left = expression1.apply(candidate);
        if (left != null) {
            final Object right = expression2.apply(candidate);
            if (right != null) {
                final Iterable<?> collection;
                final boolean collectionFirst = (left instanceof Iterable<?>);
                if (collectionFirst) {
                    if (right instanceof Iterable<?>) {
                        // Current implementation does not support collection on both sides. See class javadoc.
                        return false;
                    }
                    collection = (Iterable<?>) left;
                } else if (right instanceof Iterable<?>) {
                    collection = (Iterable<?>) right;
                } else {
                    return evaluate(left, right);
                }
                /*
                 * At this point, exactly one of the operands is a collection. It may be the left or right one.
                 * All values in the collection may be compared to the other value until match condition is met.
                 * Null elements in the collection are ignored.
                 */
                boolean match = false;
                for (final Object element : collection) {
                    if (element != null) {
                        final boolean pass;
                        if (collectionFirst) {
                            pass = evaluate(element, right);
                        } else {
                            pass = evaluate(left, element);
                        }
                        switch (matchAction) {
                            default: {
                                return false;                   // Unknown enumeration.
                            }
                            case ALL: {
                                if (!pass) return false;
                                match = true;                   // Remember that we have at least 1 value.
                                break;
                            }
                            case ANY: {
                                if (pass) return true;
                                break;                          // `match` still false since no match.
                            }
                            case ONE: {
                                if (pass) {
                                    if (match) return false;    // If a value has been found previously.
                                    match = true;               // Remember that we have exactly one value.
                                }
                            }
                        }
                    }
                }
                return match;
            }
        }
        return false;
    }

    /**
     * Compares the given objects. If both values are numerical, then this method delegates to an {@code applyAs…} method.
     * If both values are temporal, then this method delegates to {@link TimeMethods} with runtime detection of the type.
     * For other kind of objects, this method delegates to a {@code compare(…)} method. If the two objects are not of the
     * same type, then the less accurate one is converted to the most accurate type if possible.
     *
     * @param  left   the first object to compare. Must be non-null.
     * @param  right  the second object to compare. Must be non-null.
     *
     * @todo Delegate all comparisons of temporal objects to {@link TemporalFilter}.
     */
    private boolean evaluate(Object left, Object right) {
        /*
         * For numbers, the apply(…) method inherited from parent class will delegate to specialized methods like
         * applyAsDouble(…). All implementations of those specialized methods in ComparisonFilter return integer,
         * so call to intValue() will not cause information lost.
         */
        if (left instanceof Number && right instanceof Number) {
            final Number r = apply((Number) left, (Number) right);
            if (r != null) return r.intValue() != 0;
        }
        try {
            Boolean c = TimeMethods.compareIfTemporal(temporalTest(), left, right);
            if (c != null) return c;
        } catch (DateTimeException e) {
            warning(e);
            return false;
        }
        /*
         * Test character strings only after all specialized types have been tested. The intent is that if an
         * object implements both CharSequence and a specialized interface, they have been compared as value
         * objects before to be compared as strings.
         */
        if (left instanceof CharSequence || right instanceof CharSequence) {            // Really ||, not &&.
            final String s1 = left.toString();
            final String s2 = right.toString();
            final int result;
            if (isMatchingCase) {
                result = s1.compareTo(s2);
            } else {
                result = s1.compareToIgnoreCase(s2);        // TODO: use Collator for taking locale in account.
            }
            return fromCompareTo(result);
        }
        /*
         * Comparison using `compareTo` method should be last because it does not take in account
         * the `isMatchingCase` flag and because the semantic is different than < or > comparator
         * for numbers and dates.
         */
        if (left.getClass() == right.getClass() && (left instanceof Comparable<?>)) {
            @SuppressWarnings("unchecked")
            final int result = ((Comparable) left).compareTo(right);
            return fromCompareTo(result);
        }
        // TODO: report a warning for non-comparable objects.
        return false;
    }

    /**
     * Converts the Boolean result as an integer for use as a return value of the {@code applyAs…} methods.
     * This is a helper class for subclasses.
     */
    private static Number number(final boolean result) {
        return result ? 1 : 0;
    }

    /**
     * Converts the result of {@link Comparable#compareTo(Object)}.
     */
    protected abstract boolean fromCompareTo(int result);

    /**
     * Returns an identification of the test to use if the operands are temporal.
     * We do not use {@code compareTo(…)} for temporal objects because that method
     * also compares chronology, which is not desired for the purpose of "is before"
     * or "is after" comparison functions.
     *
     * @return identification of the test to apply on temporal objects.
     */
    protected abstract TimeMethods.Test temporalTest();

    /** Delegates to {@link BigDecimal#compareTo(BigDecimal)} and interprets the result with {@link #fromCompareTo(int)}. */
    @Override protected final Number applyAsDecimal (BigDecimal left, BigDecimal right) {return number(fromCompareTo(left.compareTo(right)));}
    @Override protected final Number applyAsInteger (BigInteger left, BigInteger right) {return number(fromCompareTo(left.compareTo(right)));}
    @Override protected final Number applyAsFraction(Fraction   left, Fraction   right) {return number(fromCompareTo(left.compareTo(right)));}


    /**
     * The {@code "PropertyIsLessThan"} {@literal (<)} filter.
     *
     * @param  <R>  the type of resources used as inputs.
     */
    static final class LessThan<R> extends ComparisonFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 6126039112844823196L;

        /** Creates a new filter. */
        LessThan(final Expression<R,?> expression1,
                 final Expression<R,?> expression2,
                 boolean isMatchingCase, MatchAction matchAction)
        {
            super(expression1, expression2, isMatchingCase, matchAction);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public ComparisonFilter<R> recreate(final Expression<R,?>[] effective) {
            return new LessThan<>(effective[0], effective[1], isMatchingCase, matchAction);
        }

        /** Identification of the this operation. */
        @Override public ComparisonOperatorName getOperatorType() {
            return ComparisonOperatorName.PROPERTY_IS_LESS_THAN;
        }
        @Override protected char symbol() {return '<';}

        /** Converts {@link Comparable#compareTo(Object)} result to this filter result. */
        @Override protected boolean fromCompareTo(final int result) {return result < 0;}

        /** Performs the comparison and returns the result as 0 (false) or 1 (true). */
        @Override protected Number  applyAsDouble(double left, double right) {return number(left < right);}
        @Override protected Number  applyAsLong  (long   left, long   right) {return number(left < right);}

        /** For comparisons of temporal objects. */
        @Override protected TimeMethods.Test temporalTest() {return TimeMethods.Test.BEFORE;}
    }


    /**
     * The {@code "PropertyIsLessThanOrEqualTo"} (≤) filter.
     *
     * @param  <R>  the type of resources used as inputs.
     */
    static final class LessThanOrEqualTo<R> extends ComparisonFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 6357459227911760871L;

        /** Creates a new filter. */
        LessThanOrEqualTo(final Expression<R,?> expression1,
                          final Expression<R,?> expression2,
                          boolean isMatchingCase, MatchAction matchAction)
        {
            super(expression1, expression2, isMatchingCase, matchAction);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public ComparisonFilter<R> recreate(final Expression<R,?>[] effective) {
            return new LessThanOrEqualTo<>(effective[0], effective[1], isMatchingCase, matchAction);
        }

        /** Identification of the this operation. */
        @Override public ComparisonOperatorName getOperatorType() {
            return ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO;
        }
        @Override protected char symbol() {return '≤';}

        /** Converts {@link Comparable#compareTo(Object)} result to this filter result. */
        @Override protected boolean fromCompareTo(final int result) {return result <= 0;}

        /** Performs the comparison and returns the result as 0 (false) or 1 (true). */
        @Override protected Number  applyAsDouble(double left, double right) {return number(left <= right);}
        @Override protected Number  applyAsLong  (long   left, long   right) {return number(left <= right);}

        /** For comparisons of temporal objects. */
        @Override protected TimeMethods.Test temporalTest() {return TimeMethods.Test.NOT_AFTER;}
    }


    /**
     * The {@code "PropertyIsGreaterThan"} {@literal (>)} filter.
     *
     * @param  <R>  the type of resources used as inputs.
     */
    static final class GreaterThan<R> extends ComparisonFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 8605517892232632586L;

        /** Creates a new filter. */
        GreaterThan(final Expression<R,?> expression1,
                    final Expression<R,?> expression2,
                    boolean isMatchingCase, MatchAction matchAction)
        {
            super(expression1, expression2, isMatchingCase, matchAction);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public ComparisonFilter<R> recreate(final Expression<R,?>[] effective) {
            return new GreaterThan<>(effective[0], effective[1], isMatchingCase, matchAction);
        }

        /** Identification of the this operation. */
        @Override public ComparisonOperatorName getOperatorType() {
            return ComparisonOperatorName.PROPERTY_IS_GREATER_THAN;
        }
        @Override protected char symbol() {return '>';}

        /** Converts {@link Comparable#compareTo(Object)} result to this filter result. */
        @Override protected boolean fromCompareTo(final int result) {return result > 0;}

        /** Performs the comparison and returns the result as 0 (false) or 1 (true). */
        @Override protected Number  applyAsDouble(double left, double right) {return number(left > right);}
        @Override protected Number  applyAsLong  (long   left, long   right) {return number(left > right);}

        /** For comparisons of temporal objects. */
        @Override protected TimeMethods.Test temporalTest() {return TimeMethods.Test.AFTER;}
    }


    /**
     * The {@code "PropertyIsGreaterThanOrEqualTo"} (≥) filter.
     *
     * @param  <R>  the type of resources used as inputs.
     */
    static final class GreaterThanOrEqualTo<R> extends ComparisonFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 1514185657159141882L;

        /** Creates a new filter. */
        GreaterThanOrEqualTo(final Expression<R,?> expression1,
                             final Expression<R,?> expression2,
                             boolean isMatchingCase, MatchAction matchAction)
        {
            super(expression1, expression2, isMatchingCase, matchAction);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public ComparisonFilter<R> recreate(final Expression<R,?>[] effective) {
            return new GreaterThanOrEqualTo<>(effective[0], effective[1], isMatchingCase, matchAction);
        }

        /** Identification of the this operation. */
        @Override public ComparisonOperatorName getOperatorType() {
            return ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO;
        }
        @Override protected char symbol() {return '≥';}

        /** Converts {@link Comparable#compareTo(Object)} result to this filter result. */
        @Override protected boolean fromCompareTo(final int result) {return result >= 0;}

        /** Performs the comparison and returns the result as 0 (false) or 1 (true). */
        @Override protected Number  applyAsDouble(double left, double right) {return number(left >= right);}
        @Override protected Number  applyAsLong  (long   left, long   right) {return number(left >= right);}

        /** For comparisons of temporal objects. */
        @Override protected TimeMethods.Test temporalTest() {return TimeMethods.Test.NOT_BEFORE;}
    }


    /**
     * The {@code "PropertyIsEqualTo"} (=) filter.
     *
     * @param  <R>  the type of resources used as inputs.
     */
    static final class EqualTo<R> extends ComparisonFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 8502612221498749667L;

        /** Creates a new filter. */
        EqualTo(final Expression<R,?> expression1,
                final Expression<R,?> expression2,
                boolean isMatchingCase, MatchAction matchAction)
        {
            super(expression1, expression2, isMatchingCase, matchAction);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public ComparisonFilter<R> recreate(final Expression<R,?>[] effective) {
            return new EqualTo<>(effective[0], effective[1], isMatchingCase, matchAction);
        }

        /** Identification of the this operation. */
        @Override public ComparisonOperatorName getOperatorType() {
            return ComparisonOperatorName.PROPERTY_IS_EQUAL_TO;
        }
        @Override protected char symbol() {return '=';}

        /** Converts {@link Comparable#compareTo(Object)} result to this filter result. */
        @Override protected boolean fromCompareTo(final int result) {return result == 0;}

        /** Performs the comparison and returns the result as 0 (false) or 1 (true). */
        @Override protected Number  applyAsDouble(double left, double right) {return number(left == right);}
        @Override protected Number  applyAsLong  (long   left, long   right) {return number(left == right);}

        /** For comparisons of temporal objects. */
        @Override protected TimeMethods.Test temporalTest() {return TimeMethods.Test.EQUAL;}
    }


    /**
     * The {@code "PropertyIsNotEqualTo"} (≠) filter.
     *
     * @param  <R>  the type of resources used as inputs.
     */
    static final class NotEqualTo<R> extends ComparisonFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -3295957142249035362L;

        /** Creates a new filter. */
        NotEqualTo(final Expression<R,?> expression1,
                   final Expression<R,?> expression2,
                   boolean isMatchingCase, MatchAction matchAction)
        {
            super(expression1, expression2, isMatchingCase, matchAction);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public ComparisonFilter<R> recreate(final Expression<R,?>[] effective) {
            return new NotEqualTo<>(effective[0], effective[1], isMatchingCase, matchAction);
        }

        /** Identification of the this operation. */
        @Override public ComparisonOperatorName getOperatorType() {
            return ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO;
        }
        @Override protected char symbol() {return '≠';}

        /** Converts {@link Comparable#compareTo(Object)} result to this filter result. */
        @Override protected boolean fromCompareTo(final int result) {return result != 0;}

        /** Performs the comparison and returns the result as 0 (false) or 1 (true). */
        @Override protected Number  applyAsDouble(double  left, double right) {return number(left != right);}
        @Override protected Number  applyAsLong  (long    left, long   right) {return number(left != right);}

        /** For comparisons of temporal objects. */
        @Override protected TimeMethods.Test temporalTest() {return TimeMethods.Test.NOT_EQUAL;}
    }


    /**
     * The {@code "PropertyIsBetween"} filter. This can be seen as a specialization of
     * {@link org.apache.sis.filter.LogicalFilter.And} when one expression is
     * {@link LessThanOrEqualTo} and a second expression is {@link GreaterThanOrEqualTo}.
     *
     * @param  <R>  the type of resources used as inputs.
     *
     * @see org.apache.sis.filter.LogicalFilter.And
     */
    static final class Between<R> extends Node implements BetweenComparisonOperator<R>, Optimization.OnFilter<R> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -2434954008425799595L;

        /** The first  operation to apply. */ private final GreaterThanOrEqualTo<R> lower;
        /** The second operation to apply. */ private final LessThanOrEqualTo<R> upper;

        /** Creates a new filter. */
        Between(final Expression<R,?> expression,
                final Expression<R,?> lower,
                final Expression<R,?> upper)
        {
            this.lower = new GreaterThanOrEqualTo<>(expression, lower, true, MatchAction.ANY);
            this.upper = new    LessThanOrEqualTo<>(expression, upper, true, MatchAction.ANY);
        }

        /**
         * Creates a new filter of the same type but different parameters.
         */
        @Override
        public Filter<R> recreate(final Expression<R,?>[] effective) {
            return new Between<>(effective[0], effective[1], effective[2]);
        }

        /** Returns the class of resources expected by this filter. */
        @Override public final Class<? super R> getResourceClass() {
            return specializedClass(lower.getResourceClass(),
                                    upper.getResourceClass());
        }

        /**
         * Returns the 3 children of this node. Since {@code lower.expression2}
         * is the same as {@code upper.expression1}, that repetition is omitted.
         */
        @Override protected Collection<?> getChildren() {
            return getExpressions();
        }

        /** Returns the expression to be compared by this operator, together with boundaries. */
        @Override public List<Expression<R,?>> getExpressions() {
            return List.of(getExpression(), getLowerBoundary(), getUpperBoundary());
        }

        /** Returns the expression to be compared. */
        @Override public Expression<R,?> getExpression()    {return lower.expression1;}
        @Override public Expression<R,?> getLowerBoundary() {return lower.expression2;}
        @Override public Expression<R,?> getUpperBoundary() {return upper.expression2;}

        /** Executes the filter operation. */
        @Override public boolean test(final R object) {
            return lower.test(object) && upper.test(object);
        }
    }
}
