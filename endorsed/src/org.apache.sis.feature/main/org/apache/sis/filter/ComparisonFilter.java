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
import java.util.List;
import java.util.Collection;
import java.util.Date;
import java.util.Calendar;
import java.util.Objects;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import org.apache.sis.math.Fraction;
import org.apache.sis.filter.base.Node;
import org.apache.sis.filter.base.BinaryFunctionWidening;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
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
                            default: return false;              // Unknown enumeration.
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
        /*
         * For legacy java.util.Date, the compareTo(…) method is consistent only for dates of the same class.
         * Otherwise A.compareTo(B) and B.compareTo(A) are inconsistent if one object is a java.util.Date and
         * the other object is a java.sql.Timestamp. In such case, we compare the dates as java.time objects.
         */
        if (left instanceof Date && right instanceof Date) {
            if (left.getClass() == right.getClass()) {
                return fromCompareTo(((Date) left).compareTo((Date) right));
            }
            left  = fromLegacy((Date) left);
            right = fromLegacy((Date) right);
        }
        /*
         * Temporal objects have complex conversion rules. We take Instant as the most accurate and unambiguous type.
         * So if at least one value is an Instant, try to unconditionally promote the other value to an Instant too.
         * This conversion will fail if the other object has some undefined fields; for example java.sql.Date has no
         * time fields (we do not assume that the values of those fields are zero).
         *
         * OffsetTime and OffsetDateTime are final classes that do not implement a java.time.chrono interface.
         * Note that OffsetDateTime is convertible into OffsetTime by dropping the date fields, but we do not
         * (for now) perform comparisons that would ignore the date fields of an operand.
         */
        if (left instanceof Temporal || right instanceof Temporal) {        // Use || because an operand may be Date.
            if (left instanceof Instant) {
                final Instant t = toInstant(right);
                if (t != null) return fromCompareTo(((Instant) left).compareTo(t));
            } else if (right instanceof Instant) {
                final Instant t = toInstant(left);
                if (t != null) return fromCompareTo(t.compareTo((Instant) right));
            } else if (left instanceof OffsetDateTime) {
                final OffsetDateTime t = toOffsetDateTime(right);
                if (t != null) return compare((OffsetDateTime) left, t);
            } else if (right instanceof OffsetDateTime) {
                final OffsetDateTime t = toOffsetDateTime(left);
                if (t != null) return compare(t, (OffsetDateTime) right);
            } else if (left instanceof OffsetTime && right instanceof OffsetTime) {
                return compare((OffsetTime) left, (OffsetTime) right);
            }
            /*
             * Comparisons of temporal objects implementing java.time.chrono interfaces. We need to check the most
             * complete types first. If the type are different, we reduce to the type of the less smallest operand.
             * For example if an operand is a date+time and the other operand is only a date, then the time fields
             * will be ignored and a warning will be reported.
             */
            if (left instanceof ChronoLocalDateTime<?>) {
                final ChronoLocalDateTime<?> t = toLocalDateTime(right);
                if (t != null) return compare((ChronoLocalDateTime<?>) left, t);
            } else if (right instanceof ChronoLocalDateTime<?>) {
                final ChronoLocalDateTime<?> t = toLocalDateTime(left);
                if (t != null) return compare(t, (ChronoLocalDateTime<?>) right);
            }
            if (left instanceof ChronoLocalDate) {
                final ChronoLocalDate t = toLocalDate(right);
                if (t != null) return compare((ChronoLocalDate) left, t);
            } else if (right instanceof ChronoLocalDate) {
                final ChronoLocalDate t = toLocalDate(left);
                if (t != null) return compare(t, (ChronoLocalDate) right);
            }
            if (left instanceof LocalTime) {
                final LocalTime t = toLocalTime(right);
                if (t != null) return fromCompareTo(((LocalTime) left).compareTo(t));
            } else if (right instanceof LocalTime) {
                final LocalTime t = toLocalTime(left);
                if (t != null) return fromCompareTo(t.compareTo((LocalTime) right));
            }
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
     * Converts a legacy {@code Date} object to an object from the {@link java.time} package.
     * We performs this conversion before to compare to {@code Date} instances that are not of
     * the same class, because the {@link Date#compareTo(Date)} method in such case is not well
     * defined.
     */
    private static Temporal fromLegacy(final Date value) {
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        } else if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        } else if (value instanceof java.sql.Time) {
            return ((java.sql.Time) value).toLocalTime();
        } else {
            // Implementation of above toFoo() methods use system default time zone.
            return LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
        }
    }

    /**
     * Converts the given object to an {@link Instant}, or returns {@code null} if unconvertible.
     * This method handles a few types from the {@link java.time} package and legacy types like
     * {@link Date} (with a special case for SQL dates) and {@link Calendar}.
     */
    static Instant toInstant(final Object value) {
        if (value instanceof Instant) {
            return (Instant) value;
        } else if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toInstant();
        } else if (value instanceof ChronoZonedDateTime) {
            return ((ChronoZonedDateTime) value).toInstant();
        } else if (value instanceof Date) {
            try {
                return ((Date) value).toInstant();
            } catch (UnsupportedOperationException e) {
                /*
                 * java.sql.Date and java.sql.Time cannot be converted to Instant because a part
                 * of their coordinates on the timeline is undefined.  For example in the case of
                 * java.sql.Date the hours, minutes and seconds are unspecified (which is not the
                 * same thing as assuming that those values are zero).
                 */
            }
        } else if (value instanceof Calendar) {
            return ((Calendar) value).toInstant();
        }
        return null;
    }

    /**
     * Converts the given object to an {@link OffsetDateTime}, or returns {@code null} if unconvertible.
     */
    private static OffsetDateTime toOffsetDateTime(final Object value) {
        if (value instanceof OffsetDateTime) {
            return (OffsetDateTime) value;
        } else if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toOffsetDateTime();
        } else {
            return null;
        }
    }

    /**
     * Converts the given object to a {@link ChronoLocalDateTime}, or returns {@code null} if unconvertible.
     * This method handles the case of legacy SQL {@link java.sql.Timestamp} objects.
     * Conversion may lost timezone information.
     */
    private static ChronoLocalDateTime<?> toLocalDateTime(final Object value) {
        if (value instanceof ChronoLocalDateTime<?>) {
            return (ChronoLocalDateTime<?>) value;
        } else if (value instanceof ChronoZonedDateTime) {
            ignoringField(ChronoField.OFFSET_SECONDS);
            return ((ChronoZonedDateTime) value).toLocalDateTime();
        } else if (value instanceof OffsetDateTime) {
            ignoringField(ChronoField.OFFSET_SECONDS);
            return ((OffsetDateTime) value).toLocalDateTime();
        } else if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        } else {
            return null;
        }
    }

    /**
     * Converts the given object to a {@link ChronoLocalDate}, or returns {@code null} if unconvertible.
     * This method handles the case of legacy SQL {@link java.sql.Date} objects.
     * Conversion may lost timezone information and time fields.
     */
    private static ChronoLocalDate toLocalDate(final Object value) {
        if (value instanceof ChronoLocalDate) {
            return (ChronoLocalDate) value;
        } else if (value instanceof ChronoLocalDateTime) {
            ignoringField(ChronoField.SECOND_OF_DAY);
            return ((ChronoLocalDateTime) value).toLocalDate();
        } else if (value instanceof ChronoZonedDateTime) {
            ignoringField(ChronoField.SECOND_OF_DAY);
            return ((ChronoZonedDateTime) value).toLocalDate();
        } else if (value instanceof OffsetDateTime) {
            ignoringField(ChronoField.SECOND_OF_DAY);
            return ((OffsetDateTime) value).toLocalDate();
        } else if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        } else {
            return null;
        }
    }

    /**
     * Converts the given object to a {@link LocalTime}, or returns {@code null} if unconvertible.
     * This method handles the case of legacy SQL {@link java.sql.Time} objects.
     * Conversion may lost timezone information.
     */
    private static LocalTime toLocalTime(final Object value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        } else if (value instanceof OffsetTime) {
            ignoringField(ChronoField.OFFSET_SECONDS);
            return ((OffsetTime) value).toLocalTime();
        } else if (value instanceof java.sql.Time) {
            return ((java.sql.Time) value).toLocalTime();
        } else {
            return null;
        }
    }

    /**
     * Invoked when a conversion cause a field to be ignored. For example if a "date+time" object is compared
     * with a "date" object, the "time" field is ignored. Expected values are:
     *
     * <ul>
     *   <li>{@link ChronoField#OFFSET_SECONDS}: time zone is ignored.</li>
     *   <li>{@link ChronoField#SECOND_OF_DAY}:  time of dat and time zone are ignored.</li>
     * </ul>
     *
     * @param  field  the field which is ignored.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-460">SIS-460</a>
     */
    private static void ignoringField(final ChronoField field) {
        // TODO
    }

    /**
     * Converts the boolean result as an integer for use as a return value of the {@code applyAs…} methods.
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
     * Compares two times with time-zone information. Implementations shall not use {@code compareTo(…)} because
     * that method compares more information than desired in order to ensure consistency with {@code equals(…)}.
     */
    protected abstract boolean compare(OffsetTime left, OffsetTime right);

    /**
     * Compares two dates with time-zone information. Implementations shall not use {@code compareTo(…)} because
     * that method compares more information than desired in order to ensure consistency with {@code equals(…)}.
     */
    protected abstract boolean compare(OffsetDateTime left, OffsetDateTime right);

    /**
     * Compares two dates without time-of-day and time-zone information. Implementations shall not use
     * {@code compareTo(…)} because that method also compares chronology, which is not desired for the
     * purpose of "is before" or "is after" comparison functions.
     */
    protected abstract boolean compare(ChronoLocalDate left, ChronoLocalDate right);

    /**
     * Compares two dates without time-zone information. Implementations shall not use {@code compareTo(…)}
     * because that method also compares chronology, which is not desired for the purpose of "is before" or
     * "is after" comparison functions.
     */
    protected abstract boolean compare(ChronoLocalDateTime<?> left, ChronoLocalDateTime<?> right);

    /**
     * Compares two dates with time-zone information. Implementations shall not use {@code compareTo(…)}
     * because that method also compares chronology, which is not desired for the purpose of "is before"
     * or "is after" comparison functions.
     */
    protected abstract boolean compare(ChronoZonedDateTime<?> left, ChronoZonedDateTime<?> right);

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
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
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
        @Override protected Number  applyAsDouble(double                 left, double                 right) {return number(left < right);}
        @Override protected Number  applyAsLong  (long                   left, long                   right) {return number(left < right);}
        @Override protected boolean compare      (OffsetTime             left, OffsetTime             right) {return left.isBefore(right);}
        @Override protected boolean compare      (OffsetDateTime         left, OffsetDateTime         right) {return left.isBefore(right);}
        @Override protected boolean compare      (ChronoLocalDate        left, ChronoLocalDate        right) {return left.isBefore(right);}
        @Override protected boolean compare      (ChronoLocalDateTime<?> left, ChronoLocalDateTime<?> right) {return left.isBefore(right);}
        @Override protected boolean compare      (ChronoZonedDateTime<?> left, ChronoZonedDateTime<?> right) {return left.isBefore(right);}
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
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
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
        @Override protected Number  applyAsDouble(double                 left, double                 right) {return number(left <= right);}
        @Override protected Number  applyAsLong  (long                   left, long                   right) {return number(left <= right);}
        @Override protected boolean compare      (OffsetTime             left, OffsetTime             right) {return !left.isAfter(right);}
        @Override protected boolean compare      (OffsetDateTime         left, OffsetDateTime         right) {return !left.isAfter(right);}
        @Override protected boolean compare      (ChronoLocalDate        left, ChronoLocalDate        right) {return !left.isAfter(right);}
        @Override protected boolean compare      (ChronoLocalDateTime<?> left, ChronoLocalDateTime<?> right) {return !left.isAfter(right);}
        @Override protected boolean compare      (ChronoZonedDateTime<?> left, ChronoZonedDateTime<?> right) {return !left.isAfter(right);}
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
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
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
        @Override protected Number  applyAsDouble(double                 left, double                 right) {return number(left > right);}
        @Override protected Number  applyAsLong  (long                   left, long                   right) {return number(left > right);}
        @Override protected boolean compare      (OffsetTime             left, OffsetTime             right) {return left.isAfter(right);}
        @Override protected boolean compare      (OffsetDateTime         left, OffsetDateTime         right) {return left.isAfter(right);}
        @Override protected boolean compare      (ChronoLocalDate        left, ChronoLocalDate        right) {return left.isAfter(right);}
        @Override protected boolean compare      (ChronoLocalDateTime<?> left, ChronoLocalDateTime<?> right) {return left.isAfter(right);}
        @Override protected boolean compare      (ChronoZonedDateTime<?> left, ChronoZonedDateTime<?> right) {return left.isAfter(right);}
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
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
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
        @Override protected Number  applyAsDouble(double                 left, double                 right) {return number(left >= right);}
        @Override protected Number  applyAsLong  (long                   left, long                   right) {return number(left >= right);}
        @Override protected boolean compare      (OffsetTime             left, OffsetTime             right) {return !left.isBefore(right);}
        @Override protected boolean compare      (OffsetDateTime         left, OffsetDateTime         right) {return !left.isBefore(right);}
        @Override protected boolean compare      (ChronoLocalDate        left, ChronoLocalDate        right) {return !left.isBefore(right);}
        @Override protected boolean compare      (ChronoLocalDateTime<?> left, ChronoLocalDateTime<?> right) {return !left.isBefore(right);}
        @Override protected boolean compare      (ChronoZonedDateTime<?> left, ChronoZonedDateTime<?> right) {return !left.isBefore(right);}
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
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
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
        @Override protected Number  applyAsDouble(double                 left, double                 right) {return number(left == right);}
        @Override protected Number  applyAsLong  (long                   left, long                   right) {return number(left == right);}
        @Override protected boolean compare      (OffsetTime             left, OffsetTime             right) {return left.isEqual(right);}
        @Override protected boolean compare      (OffsetDateTime         left, OffsetDateTime         right) {return left.isEqual(right);}
        @Override protected boolean compare      (ChronoLocalDate        left, ChronoLocalDate        right) {return left.isEqual(right);}
        @Override protected boolean compare      (ChronoLocalDateTime<?> left, ChronoLocalDateTime<?> right) {return left.isEqual(right);}
        @Override protected boolean compare      (ChronoZonedDateTime<?> left, ChronoZonedDateTime<?> right) {return left.isEqual(right);}
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
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
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
        @Override protected Number  applyAsDouble(double                 left, double                 right) {return number(left != right);}
        @Override protected Number  applyAsLong  (long                   left, long                   right) {return number(left != right);}
        @Override protected boolean compare      (OffsetTime             left, OffsetTime             right) {return !left.isEqual(right);}
        @Override protected boolean compare      (OffsetDateTime         left, OffsetDateTime         right) {return !left.isEqual(right);}
        @Override protected boolean compare      (ChronoLocalDate        left, ChronoLocalDate        right) {return !left.isEqual(right);}
        @Override protected boolean compare      (ChronoLocalDateTime<?> left, ChronoLocalDateTime<?> right) {return !left.isEqual(right);}
        @Override protected boolean compare      (ChronoZonedDateTime<?> left, ChronoZonedDateTime<?> right) {return !left.isEqual(right);}
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
