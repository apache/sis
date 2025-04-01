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
package org.apache.sis.temporal;

import java.util.Map;
import java.util.Date;
import java.util.function.Supplier;
import java.util.function.BiPredicate;
import java.time.Instant;
import java.time.Year;
import java.time.YearMonth;
import java.time.MonthDay;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.OffsetDateTime;
import java.time.DateTimeException;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.lang.reflect.Modifier;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.resources.Errors;


/**
 * Provides the <i>is before</i> and <i>is after</i> operations for various {@code java.time} objects.
 * This class delegates to the {@code isBefore(T)} or {@code isAfter(T)} methods of each supported classes.
 *
 * <p>Instances of this classes are immutable and thread-safe.
 * The same instance can be shared by many {@link TemporalOperation} instances.</p>
 *
 * <h2>Design note about alternative approaches</h2>
 * We do not delegate to {@link Comparable#compareTo(Object)} because the latter method compares not only
 * positions on the timeline, but also other properties not relevant to an "is before" or "is after" test.
 * We could use {@link ChronoLocalDate#timeLineOrder()} comparators instead, but those comparators are not
 * defined for every classes where the "is before" and "is after" methods differ from "compare to" method.
 * Furthermore, some temporal classes override {@code isBefore(T)} or {@code isAfter(T)} for performance.
 *
 * @param  <T>  the base type of temporal objects, or {@code Object.class} for any type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class TimeMethods<T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1075289362575825939L;

    /**
     * The type of temporal objects accepted by this set of operations.
     */
    public final Class<T> type;

    /**
     * Enumeration values for a test to apply.
     *
     * @see #compare(int, T, TemporalAccessor)
     */
    public static final int BEFORE=1, AFTER=2, EQUAL=0;

    /**
     * Predicate to execute for testing the ordering between temporal objects.
     * This comparison operator differs from the {@code compareTo(…)} method in that it compares only the
     * positions on the timeline, ignoring metadata such as the calendar used for representing positions.
     */
    public final transient BiPredicate<T,T> isBefore, isAfter, isEqual;

    /**
     * Supplier of the current time.
     * May be {@code null} if we do not know how to create an object of the expected {@linkplain #type}.
     *
     * @see #now()
     */
    public final transient Supplier<T> now;

    /**
     * Creates a new set of operators. This method is for subclasses only.
     * For getting a {@code TimeMethods} instance, see {@link #find(Class)}.
     */
    private TimeMethods(final Class<T> type,
            final BiPredicate<T,T> isBefore,
            final BiPredicate<T,T> isAfter,
            final BiPredicate<T,T> isEqual,
            final Supplier<T> now)
    {
        this.type     = type;
        this.isBefore = isBefore;
        this.isAfter  = isAfter;
        this.isEqual  = isEqual;
        this.now      = now;
    }

    /**
     * Returns whether the end point will be determined dynamically every time that a method is invoked.
     *
     * @return whether the methods are determined dynamically on an instance-by-instance basis.
     */
    public boolean isDynamic() {
        return false;
    }

    /**
     * Delegates the comparison to the method identified by the {@code test} argument.
     * This method is overridden in subclasses where the delegation can be more direct.
     *
     * @param  test   {@link #BEFORE}, {@link #AFTER} or {@link #EQUAL}.
     * @param  self   the object on which to invoke the method identified by {@code test}.
     * @param  other  the argument to give to the test method call.
     * @return the result of performing the comparison identified by {@code test}.
     */
    boolean delegate(final int test, final T self, final T other) {
        final BiPredicate<T,T> p;
        switch (test) {
            case BEFORE: p = isBefore; break;
            case AFTER:  p = isAfter;  break;
            case EQUAL:  p = isEqual;  break;
            default: throw new AssertionError(test);
        }
        return p.test(self, other);
    }

    /**
     * Compares an object of class {@code <T>} with a temporal object of unknown class.
     * The other object is typically the beginning or ending of a period.
     *
     * @param  test   {@link #BEFORE}, {@link #AFTER} or {@link #EQUAL}.
     * @param  self   the object on which to invoke the method identified by {@code test}.
     * @param  other  the argument to give to the test method call.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    @SuppressWarnings("unchecked")
    public final boolean compare(final int test, final T self, final TemporalAccessor other) {
        if (type.isInstance(other)) {
            return delegate(test, self, (T) other);         // Safe because of above `isInstance(…)` check.
        }
        return compareAsInstants(test, accessor(self), other);
    }

    /**
     * Returns {@code true} if both arguments are non-null and the specified comparison evaluates to {@code true}.
     * The type of the objects being compared is determined dynamically, which has a performance cost.
     * The {@code compare(…)} methods should be preferred when the type is known in advance.
     *
     * @param  test   {@link #BEFORE}, {@link #AFTER} or {@link #EQUAL}.
     * @param  self   the object on which to invoke the method identified by {@code test}, or {@code null} if none.
     * @param  other  the argument to give to the test method call, or {@code null} if none.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    @SuppressWarnings("unchecked")
    public static boolean compareAny(final int test, final Temporal self, final Temporal other) {
        return (self != null) && (other != null)
                && compare(test, (Class) Classes.findCommonClass(self.getClass(), other.getClass()), self, other);
    }

    /**
     * Compares two temporal objects of unknown class. This method needs to check for specialized implementations
     * before to delegate to {@link Comparable#compareTo(Object)}, because the comparison methods on the timeline
     * are not always the same as {@code compareTo(…)}.
     *
     * @param  <T>    base class of the objects to compare.
     * @param  test   {@link #BEFORE}, {@link #AFTER} or {@link #EQUAL}.
     * @param  type   base class of the {@code self} and {@code other} arguments.
     * @param  self   the object on which to invoke the method identified by {@code test}.
     * @param  other  the argument to give to the test method call.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    public static <T> boolean compare(final int test, final Class<T> type, final T self, final T other) {
        /*
         * The following cast is not strictly true, it should be `<? extends T>`.
         * However, because of the `isInstance(…)` check and because <T> is used
         * only as parameter type (no collection), it is okay to use it that way.
         */
        final TimeMethods<? super T> tc = findSpecialized(type);
        /*
         * The implementation of `TimeMethods.before/equals/after` methods delegate to this method in the
         * most generic case. In such cases, this block must be excluded for avoiding a never-ending loop.
         */
        if (tc != null && !tc.isDynamic()) {
            /*
             * Found one of the special cases listed in `INTERFACES` or `FINAL_TYPE`.
             * If the other type is compatible, the comparison is executed directly.
             * Note: the `switch` statement is equivalent to `tc.compare(test, …)`,
             * but is inlined because that method is never overridden in this context.
             */
            if (tc.type.isInstance(other)) {
                assert tc.type.isAssignableFrom(type) : tc;     // Those types are not necessarily equal.
                final BiPredicate<? super T, ? super T> p;
                switch (test) {
                    case BEFORE: p = tc.isBefore; break;
                    case AFTER:  p = tc.isAfter;  break;
                    case EQUAL:  p = tc.isEqual;  break;
                    default: throw new AssertionError(test);
                }
                return p.test(self, other);
            }
        } else if (self instanceof Comparable<?> && type.isInstance(other)) {
            /*
             * The type of the first operand is not a special case, but the second operand is compatible
             * for a call to the generic `compareTo(…)` method. This case does not happen often, because
             * not many `java.time` classes have no "is before" or "is after" operations.
             * Some examples are `Month` and `DayOfWeek`.
             */
            @SuppressWarnings("unchecked")          // Safe because verification done by `isInstance(…)`.
            final int c = ((Comparable) self).compareTo(other);
            switch (test) {
                case BEFORE: return c <  0;
                case AFTER:  return c >  0;
                case EQUAL:  return c == 0;
                default: throw new AssertionError(test);
            }
        }
        /*
         * If we reach this point, the two operands are of different classes and we cannot compare them directly.
         * Try to compare the two operands as instants on the timeline.
         */
        return compareAsInstants(test, accessor(self), accessor(other));
    }

    /**
     * Returns the given object as a temporal accessor.
     */
    private static TemporalAccessor accessor(final Object value) {
        if (value instanceof TemporalAccessor) {
            return (TemporalAccessor) value;
        } else if (value instanceof Date) {
            return ((Date) value).toInstant();      // Overridden in `Date` subclasses.
        } else {
            throw new DateTimeException(Errors.format(
                    Errors.Keys.CannotCompareInstanceOf_2, value.getClass(), TemporalAccessor.class));
        }
    }

    /**
     * Compares two temporal objects as instants.
     * This is a last-resort fallback, when objects cannot be compared by their own methods.
     *
     * @param  test   {@link #BEFORE}, {@link #AFTER} or {@link #EQUAL}.
     * @param  self   the object on which to invoke the method identified by {@code test}.
     * @param  other  the argument to give to the test method call.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    private static boolean compareAsInstants(final int test, final TemporalAccessor self, final TemporalAccessor other) {
        long t1 =  self.getLong(ChronoField.INSTANT_SECONDS);
        long t2 = other.getLong(ChronoField.INSTANT_SECONDS);
        if (t1 == t2) {
            t1 =  self.getLong(ChronoField.NANO_OF_SECOND);     // Should be present according Javadoc.
            t2 = other.getLong(ChronoField.NANO_OF_SECOND);
            if (t1 == t2) {
                return test == EQUAL;
            }
        }
        return test == ((t1 < t2) ? BEFORE : AFTER);
    }

    /**
     * Returns the set of methods that can be invoked on instances of the given type, or {@code null} if none.
     * This method returns only one of the methods defined in {@link #FINAL_TYPES} or {@link #INTERFACES}.
     * It shall not try to create fallbacks.
     *
     * @param  <T>   compile-time value of the {@code type} argument.
     * @param  type  the type of temporal object for which to get specialized methods.
     * @return set of specialized methods for the given object type, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    private static <T> TimeMethods<? super T> findSpecialized(final Class<T> type) {
        {   // Block for keeping `tc` in local scope.
            TimeMethods<?> tc = FINAL_TYPES.get(type);
            if (tc != null) {
                assert tc.type == type : tc;
                return (TimeMethods<T>) tc;             // Safe because of `==` checks.
            }
        }
        for (TimeMethods<?> tc : INTERFACES) {
            if (tc.type.isAssignableFrom(type)) {
                return (TimeMethods<? super T>) tc;     // Safe because of `isAssignableFrom(…)` checks.
            }
        }
        return null;
    }

    /**
     * Returns the set of methods that can be invoked on instances of the given type.
     *
     * @param  <T>   compile-time value of the {@code type} argument.
     * @param  type  the type of temporal object for which to get specialized methods.
     * @return set of comparison methods for the given object type.
     */
    @SuppressWarnings("unchecked")          // For (Comparable) casts.
    public static <T> TimeMethods<? super T> find(final Class<T> type) {
        final TimeMethods<? super T> tc = findSpecialized(type);
        if (tc != null) {
            return tc;
        }
        if (Modifier.isFinal(type.getModifiers())) {
            if (Comparable.class.isAssignableFrom(type)) {
                return new TimeMethods<>(type,
                        (self, other) -> ((Comparable) self).compareTo(other) < 0,
                        (self, other) -> ((Comparable) self).compareTo(other) > 0,
                        (self, other) -> ((Comparable) self).compareTo(other) == 0,
                        null);
            } else {
                throw new DateTimeException(Errors.format(Errors.Keys.CannotCompareInstanceOf_2, type, type));
            }
        } else {
            return fallback(type);
        }
    }

    /**
     * Returns the last-resort fallback when the type of temporal objects cannot be determined in advance.
     *
     * @param  <T>   compile-time value of the {@code type} argument.
     * @param  type  the type of temporal object for which to get the last-resource fallback methods.
     * @return set of last-resort comparison methods for the given object type.
     */
    private static <T> TimeMethods<? super T> fallback(final Class<T> type) {
        return new TimeMethods<>(type,
                (self, other) -> compare(BEFORE, type, self, other),
                (self, other) -> compare(AFTER,  type, self, other),
                (self, other) -> compare(EQUAL,  type, self, other),
                null)
        {
            @Override public boolean isDynamic() {
                return true;
            }
            @Override boolean delegate(final int test, final T self, final T other) {
                return compare(test, type, self, other);
            }
        };
    }

    /**
     * Returns the unique instance for the type after deserialization.
     * This is needed for avoiding to serialize the lambda functions.
     *
     * @return the object to use after deserialization.
     * @throws ObjectStreamException if the serialized object contains invalid data.
     */
    private Object readResolve() throws ObjectStreamException {
        return find(type);
    }

    /**
     * Returns the current time as a temporal object.
     *
     * @return the current time.
     * @throws ClassCastException if the {@linkplain #type} is {@link Date} or {@link MonthDay}.
     */
    public final Temporal now() {
        return (now != null) ? (Temporal) now.get() : ZonedDateTime.now();
    }

    /**
     * Operators for all supported temporal types that are interfaces or non-final classes.
     * Those types need to be checked with {@link Class#isAssignableFrom(Class)} in iteration order.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})            // For `Chrono*` interfaces, because they are parameterized.
    private static final TimeMethods<?>[] INTERFACES = {
        new TimeMethods<>(ChronoZonedDateTime.class, ChronoZonedDateTime::isBefore, ChronoZonedDateTime::isAfter, ChronoZonedDateTime::isEqual, ZonedDateTime::now),
        new TimeMethods<>(ChronoLocalDateTime.class, ChronoLocalDateTime::isBefore, ChronoLocalDateTime::isAfter, ChronoLocalDateTime::isEqual, LocalDateTime::now),
        new TimeMethods<>(    ChronoLocalDate.class,     ChronoLocalDate::isBefore,     ChronoLocalDate::isAfter,     ChronoLocalDate::isEqual,     LocalDate::now),
        new TimeMethods<>(               Date.class,                Date::  before,                Date::  after,                Date::equals,           Date::new)
    };

    /*
     * No operation on numbers for now. We could revisit this policy in a future version if we
     * allow the temporal function to have a CRS and to operate on temporal coordinate values.
     */

    /**
     * Operators for all supported temporal types for which there is no need to check for subclasses.
     * Those classes are usually final, except when wanting to intentionally ignore all subclasses.
     * Those types should be tested before {@link #INTERFACES} because this check is quick.
     *
     * <h4>Implementation note</h4>
     * {@link Year}, {@link YearMonth}, {@link MonthDay}, {@link LocalTime} and {@link Instant}
     * could be replaced by {@link Comparable}. We nevertheless keep the specialized classes in
     * case the implementations change in the future, and also for performance reason, because
     * the code working on generic {@link Comparable} needs to check for special cases again.
     */
    private static final Map<Class<?>, TimeMethods<?>> FINAL_TYPES = Map.ofEntries(
        entry(new TimeMethods<>(OffsetDateTime.class, OffsetDateTime::isBefore, OffsetDateTime::isAfter, OffsetDateTime::isEqual, OffsetDateTime::now)),
        entry(new TimeMethods<>( ZonedDateTime.class,  ZonedDateTime::isBefore,  ZonedDateTime::isAfter,  ZonedDateTime::isEqual,  ZonedDateTime::now)),
        entry(new TimeMethods<>( LocalDateTime.class,  LocalDateTime::isBefore,  LocalDateTime::isAfter,  LocalDateTime::isEqual,  LocalDateTime::now)),
        entry(new TimeMethods<>(     LocalDate.class,      LocalDate::isBefore,      LocalDate::isAfter,      LocalDate::isEqual,      LocalDate::now)),
        entry(new TimeMethods<>(    OffsetTime.class,     OffsetTime::isBefore,     OffsetTime::isAfter,     OffsetTime::isEqual,     OffsetTime::now)),
        entry(new TimeMethods<>(     LocalTime.class,      LocalTime::isBefore,      LocalTime::isAfter,      LocalTime::equals,       LocalTime::now)),
        entry(new TimeMethods<>(          Year.class,           Year::isBefore,           Year::isAfter,           Year::equals,            Year::now)),
        entry(new TimeMethods<>(     YearMonth.class,      YearMonth::isBefore,      YearMonth::isAfter,      YearMonth::equals,       YearMonth::now)),
        entry(new TimeMethods<>(      MonthDay.class,       MonthDay::isBefore,       MonthDay::isAfter,       MonthDay::equals,        MonthDay::now)),
        entry(new TimeMethods<>(       Instant.class,        Instant::isBefore,        Instant::isAfter,        Instant::equals,         Instant::now)),
        entry(fallback(Temporal.class)),    // Frequently declared type. Intentionally no "instance of" checks.
        entry(fallback(Object.class)));     // Not a final class, but to be used when the declared type is Object.

    /**
     * Helper method for adding entries to the {@link #FINAL_TYPES} map.
     * Shall be used only for final classes.
     */
    private static Map.Entry<Class<?>, TimeMethods<?>> entry(final TimeMethods<?> op) {
        return Map.entry(op.type, op);
    }

    /**
     * Returns a string representation of this set of operations for debugging purposes.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(TimeMethods.class, "type", type.getSimpleName());
    }
}
