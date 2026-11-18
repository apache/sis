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
import java.util.Calendar;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.time.Instant;
import java.time.Year;
import java.time.YearMonth;
import java.time.MonthDay;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.logging.Logging;
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
public final class TimeMethods<T> implements Serializable {
    /**
     * Logger for warnings.
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.temporal");

    /**
     * The test to apply: equal, before or after.
     *
     * @see #convertAndCompare(Test, T, TemporalAccessor)
     */
    public enum Test {
        /** Identifies the <var>A</var> = <var>B</var> test. */
        EQUAL() {
            @Override <T> BiPredicate<T,T> predicate(TimeMethods<T> m) {return m.isEqual;}
            @Override <T> boolean compare(TimeMethods<T> m, T a, T b)  {return m.isEqual.test(a, b);}
            @Override     boolean fromCompareTo(int result)            {return result == 0;}
        },

        /** Identifies the <var>A</var> {@literal <} <var>B</var> test. */
        BEFORE() {
            @Override <T> BiPredicate<T,T> predicate(TimeMethods<T> m) {return m.isBefore;}
            @Override <T> boolean compare(TimeMethods<T> m, T a, T b)  {return m.isBefore.test(a, b);}
            @Override     boolean fromCompareTo(int result)            {return result < 0;}
        },

        /** Identifies the <var>A</var> {@literal >} <var>B</var> test. */
        AFTER() {
            @Override <T> BiPredicate<T,T> predicate(TimeMethods<T> m) {return m.isAfter;}
            @Override <T> boolean compare(TimeMethods<T> m, T a, T b)  {return m.isAfter.test(a, b);}
            @Override     boolean fromCompareTo(int result)            {return result > 0;}
        },

        /** Identifies the <var>A</var> ≠ <var>B</var> test. */
        NOT_EQUAL() {
            @Override <T> BiPredicate<T,T> predicate(TimeMethods<T> m) {return  m.isEqual.negate();}
            @Override <T> boolean compare(TimeMethods<T> m, T a, T b)  {return !m.isEqual.test(a, b);}
            @Override     boolean fromCompareTo(int result)            {return result != 0;}
        },

        /** Identifies the <var>A</var> ≥ <var>B</var> test. */
        NOT_BEFORE() {
            @Override <T> BiPredicate<T,T> predicate(TimeMethods<T> m) {return  m.isBefore.negate();}
            @Override <T> boolean compare(TimeMethods<T> m, T a, T b)  {return !m.isBefore.test(a, b);}
            @Override     boolean fromCompareTo(int result)            {return result >= 0;}
        },

        /** Identifies the <var>A</var> ≤ <var>B</var> test. */
        NOT_AFTER() {
            @Override <T> BiPredicate<T,T> predicate(TimeMethods<T> m)  {return  m.isAfter.negate();}
            @Override <T> boolean compare(TimeMethods<T> m, T a, T b)   {return !m.isAfter.test(a, b);}
            @Override     boolean fromCompareTo(int result)             {return result <= 0;}
        };

        /**
         * Returns the predicate to use for this test.
         *
         * @param  <T>  the type of temporal objects expected by the predicate.
         * @param  m    the collection of predicate for the type of temporal objects.
         * @return the predicate for this test.
         */
        abstract <T> BiPredicate<T,T> predicate(TimeMethods<T> m);

        /**
         * Executes the test between the given temporal objects.
         *
         * @param  <T>    the type of temporal objects expected by the predicate.
         * @param  m      the collection of predicate for the type of temporal objects.
         * @param  self   the object on which to invoke the method identified by this test.
         * @param  other  the argument to give to the test method call.
         * @return the result of performing the comparison identified by this test.
         */
        abstract <T> boolean compare(TimeMethods<T> m, T self, T other);

        /**
         * Returns whether the test pass according the result of a {@code compareTo(…)} method.
         *
         * @param  result  the {@code compareTo(…)} result.
         * @return whether the test pass.
         */
        abstract boolean fromCompareTo(int result);
    }

    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3421610320642857317L;

    /**
     * The type of temporal objects accepted by this set of operations.
     */
    public final Class<T> type;

    /**
     * Converter from an object of arbitrary class to an object of class {@code <T>}, or {@code null} if none.
     * The function may return {@code null} if the given object is an instance of unsupported type.
     *
     * <p><b>Design note:</b> the purpose of this field overlaps with {@link org.apache.sis.util.ObjectConverters}.
     * Those converters are not yet registered as {@link org.apache.sis.util.ObjectConverter}s because they expect
     * {@code Object} in argument (which is too generic for {@code ObjectConverter}) for performance reasons,
     * conversions may lost information, and we may report these lost with {@link #ignoringField(ChronoField)}
     * in the future. Another difference is that the function returns {@code null} instead of throwing an exception
     * if an operand is not recognized.</p>
     *
     * @see #toOffsetDateTime(Object)
     * @see #toLocalDateTime(Object)
     * @see #toLocalDate(Object)
     * @see #toLocalTime(Object)
     * @see #toInstant(Object)
     */
    private final transient Function<Object, T> converter;

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
    private final transient Supplier<T> now;

    /**
     * Function to execute for getting another temporal object with the given timezone, or {@code null} if none.
     * If the temporal object already has a timezone, then this function returns an object at the same instant.
     * The returned object may be of a different class than the object given in input, especially if a timezone
     * is added to a local time. If the temporal object is only a date with no time, this field is {@code null}.
     * If the function supports only {@link ZoneOffset} and not the more generic {@link ZoneId} class, then the
     * function returns {@code null}.
     *
     * @see #withZone(Object, ZoneId, boolean)
     */
    private final transient BiFunction<T, ZoneId, Temporal> withZone;

    /**
     * Whether the temporal object have a time zone, explicitly or implicitly.
     * Used only for {@link Temporal} objects.
     */
    private final transient boolean hasZone;

    /**
     * Whether the end point may be determined dynamically every time that a method is invoked.
     * Used for avoiding infinite recursive call.
     */
    public final transient boolean isDynamic;

    /**
     * Creates a new set of operators. This method is for subclasses only.
     * For getting a {@code TimeMethods} instance, see {@link #forType(Class)}.
     */
    private TimeMethods(
            final Class<T>           type,
            final Function<Object,T> converter,
            final BiPredicate<T,T>   isBefore,
            final BiPredicate<T,T>   isAfter,
            final BiPredicate<T,T>   isEqual,
            final Supplier<T>        now,
            final BiFunction<T, ZoneId, Temporal> withZone,
            final boolean hasZone,
            final boolean isDynamic)
    {
        this.type      = type;
        this.converter = converter;
        this.isBefore  = isBefore;
        this.isAfter   = isAfter;
        this.isEqual   = isEqual;
        this.now       = now;
        this.withZone  = withZone;
        this.hasZone   = hasZone;
        this.isDynamic = isDynamic;
    }

    /**
     * Returns a converter from {@code otherType} to {@link #type},
     * or {@code null} if there is no need for converter.
     *
     * @param  <S>        compile-time value of {@code otherType}.
     * @param  otherType  the type of values expected as input by the converter.
     * @return converter from {@code otherType} to {@link #type}, or {@code null}.
     */
    @SuppressWarnings("unchecked")
    private <S> Function<? super S, T> converter(final Class<S> otherType) {
        if (type.isAssignableFrom(otherType)) {
            return null;
        }
        /*
         * Check for more specific conversions before to fallback on `converter` because the most specific
         * cases are likely to be more efficient. We must exclude the `java.util.Date` type because of the
         * complication of the `java.sql.Date` and `java.sql.Time` subtypes.
         */
        if (otherType != Object.class && !Date.class.isAssignableFrom(otherType)) try {
            final Function<? super S, ? extends T> castOrConvert = ObjectConverters.find(otherType, type);
            return (S other) -> {
                try {
                    return castOrConvert.apply(other);
                } catch (UnconvertibleObjectException e) {
                    Logging.ignorableException(LOGGER, TimeMethods.class, "converter", e);
                    return null;
                }
            };
        } catch (UnconvertibleObjectException e) {
            Logging.ignorableException(LOGGER, TimeMethods.class, "converter", e);
        }
        if (converter != null) {
            return converter;
        }
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Class<T> type = this.type;
        return (S other) -> type.isInstance(other) ? (T) other : null;
    }

    /**
     * Returns the predicate to use for this test.
     * The expected type of the first operand is always {@code <T>}.
     * The expected type of the second operand will be either {@code <T>} or {@code Object},
     * depending on the value of {@code t2}.
     *
     * @param  <S>        compile-time value of {@code otherType}.
     * @param  test       the test to apply (before, after and/or equal).
     * @param  otherType  expected class of the second operand.
     * @return the predicate for the requested test.
     */
    @SuppressWarnings("unchecked")
    public final <S> BiPredicate<T,S> predicate(final Test test, final Class<S> otherType) {
        final BiPredicate<T,T> predicate = test.predicate(this);
        final Function<? super S, T> castOrConvert = converter(otherType);
        if (castOrConvert == null) {
            return (BiPredicate) predicate;
        }
        return (T self, S other) -> {
            final T converted = castOrConvert.apply(other);
            return (converted != null) && predicate.test(self, converted);
        };
    }

    /**
     * Returns {@code true} if both arguments are non-null and this comparison evaluates to {@code true}.
     * The type of the objects being compared is determined dynamically, which has a performance cost.
     * The {@code TimeMethods.compare(…)} methods should be preferred when the type is known in advance.
     *
     * <p>This method is equivalent to {@link #compareIfTemporal(Test, Object, Object)} except for the
     * return type, which is simplified to the primitive type.</p>
     *
     * @param  test   the test to apply (before, after and/or equal).
     * @param  self   the object on which to invoke the method identified by {@code test}, or {@code null} if none.
     * @param  other  the argument to give to the test method call, or {@code null} if none.
     * @return the comparison result, or {@code false} if the given objects were not recognized as temporal.
     * @throws DateTimeException if the two objects are temporal objects but cannot be compared.
     */
    private static boolean compareIfTemporalElseFalse(final Test test, final Object self, final Object other) {
        Boolean c = compareIfTemporal(test, self, other);
        return (c != null) && c;
    }

    /**
     * Returns {@code TRUE} if both arguments are non-null and the specified comparison evaluates to {@code true}.
     * If the two objects are not of compatible type, they are converted. If at least one object is not temporal,
     * then this method returns {@code null} rather than throwing {@link DateTimeException}.
     *
     * <p>This method should be used in last resort because it may be expensive.</p>
     *
     * @param  test   the test to apply (before, after and/or equal).
     * @param  self   the object on which to invoke the method identified by {@code test}, or {@code null} if none.
     * @param  other  the argument to give to the test method call, or {@code null} if none.
     * @return the comparison result, or {@code null} if the given objects were not recognized as temporal.
     * @throws DateTimeException if the two objects are temporal objects but cannot be compared.
     */
    public static Boolean compareIfTemporal(final Test test, Object self, Object other) {
        if (self == null || other == null) {
            return Boolean.FALSE;
        }
        boolean isTemporal;     // For skipping unecessary `if (x instanceof Temporal)` checks later.
        isTemporal  = (self  != (self  = unwrapTemporal(self )));
        isTemporal |= (other != (other = unwrapTemporal(other)));
        /*
         * For legacy java.util.Date, the compareTo(…) method is consistent only for dates of the same class.
         * Otherwise A.compareTo(B) and B.compareTo(A) are inconsistent if one object is a java.util.Date and
         * the other object is a java.sql.Timestamp. In such case, we compare the dates as java.time objects.
         */
        if (self instanceof Date && other instanceof Date) {
            if (self.getClass() == other.getClass()) {
                return test.fromCompareTo(((Date) self).compareTo((Date) other));
            }
            self  = fromLegacy((Date) self);
            other = fromLegacy((Date) other);
            isTemporal = true;          // For skipping unecessary `if (x instanceof Temporal)` checks.
        }
        if (self != null && other != null) {
            // Use `||` because an operand by still be a `java.utl.Date`.
            if (isTemporal || self instanceof Temporal || other instanceof Temporal) {
                return compareTemporalOrDate(test, self, other);
            }
        }
        return null;
    }

    /**
     * Unwraps the {@link Temporal} component, or returns the object itself if it is not a wrapper.
     * If the returned value is different, then it is an instance of {@link Temporal}.
     */
    private static Object unwrapTemporal(final Object value) {
        if (value instanceof TemporalDate) {
            return ((TemporalDate) value).temporal;
        } else if (value instanceof org.opengis.temporal.Instant) {
            return ((org.opengis.temporal.Instant) value).getPosition();
        } else {
            return value;
        }
    }

    /**
     * Returns whether the given type is considered temporal. This is necessary for avoiding that
     * comparable objects such as {@link String} are wrongly handled by {@code TimeMethods}.
     */
    private static boolean isTemporal(final Class<?> type) {
        return (type != null) &&
                (Temporal.class.isAssignableFrom(type)
                  || Date.class.isAssignableFrom(type)
                  || org.opengis.temporal.Instant.class.isAssignableFrom(type));
    }

    /**
     * Returns {@code true} if both arguments are non-null and the specified comparison evaluates to {@code true}.
     * The type of the objects being compared is determined dynamically, which has a performance cost.
     * The {@code compare(…)} methods should be preferred when the type is known in advance.
     *
     * @param  test   the test to apply (before, after and/or equal).
     * @param  self   the object on which to invoke the method identified by {@code test}, or {@code null} if none.
     * @param  other  the argument to give to the test method call, or {@code null} if none.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    public static boolean compareLenient(final Test test, final Temporal self, final Temporal other) {
        if (self == null || other == null) {
            return false;
        }
        return compareTemporalOrDate(test, self, other);
    }

    /**
     * Implementation of lenient comparisons between non-null instances of arbitrary temporal or date types.
     * Temporal objects have complex conversion rules. We take Instant as the most accurate and unambiguous type.
     * So if at least one value is an Instant, try to unconditionally promote the other value to an Instant too.
     * This conversion will fail if the other object has some undefined fields. For example {@link java.sql.Date}
     * has no time fields (we do not assume that the values of those fields are zero).
     *
     * @param  test   the test to apply (before, after and/or equal).
     * @param  self   the object on which to invoke the method identified by {@code test}.
     * @param  other  the argument to give to the test method call.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    @SuppressWarnings("unchecked")
    private static boolean compareTemporalOrDate(final Test test, Object self, Object other) {
        Class<?> type = self.getClass();
adapt:  if (type != other.getClass()) {
            Temporal converted;
            /*
             * OffsetTime and OffsetDateTime are final classes that do not implement a java.time.chrono interface.
             * Note that OffsetDateTime is convertible into OffsetTime by dropping the date fields, but we do not
             * (for now) perform comparisons that would ignore the date fields of an operand.
             */
            if (self instanceof Instant) {
                converted = toInstant(other);
                if (converted != null) {
                    other = converted;
                    type  = Instant.class;
                    break adapt;
                }
            } else if (other instanceof Instant) {
                converted = toInstant(self);
                if (converted != null) {
                    self = converted;
                    type = Instant.class;
                    break adapt;
                }
            } else if (self instanceof OffsetDateTime) {
                converted = toOffsetDateTime(other);
                if (converted != null) {
                    other = converted;
                    type  = OffsetDateTime.class;
                    break adapt;
                }
            } else if (other instanceof OffsetDateTime) {
                converted = toOffsetDateTime(self);
                if (converted != null) {
                    self = converted;
                    type = OffsetDateTime.class;
                    break adapt;
                }
            }
            /*
             * Comparisons of temporal objects implementing java.time.chrono interfaces. We need to check the most
             * complete types first. If the type are different, we reduce to the type of the less smallest operand.
             * For example if an operand is a date+time and the other operand is only a date, then the time fields
             * will be ignored and a warning will be reported.
             */
            if (self instanceof ChronoLocalDateTime<?>) {
                converted = toLocalDateTime(other);
                if (converted != null) {
                    other = converted;
                    type  = ChronoLocalDateTime.class;
                    break adapt;
                }
            } else if (other instanceof ChronoLocalDateTime<?>) {
                converted = toLocalDateTime(self);
                if (converted != null) {
                    self = converted;
                    type = ChronoLocalDateTime.class;
                    break adapt;
                }
            }
            // No else, we want this fallback.
            if (self instanceof ChronoLocalDate) {
                converted = toLocalDate(other);
                if (converted != null) {
                    other = converted;
                    type  = ChronoLocalDate.class;
                    break adapt;
                }
            } else if (other instanceof ChronoLocalDate) {
                converted = toLocalDate(self);
                if (converted != null) {
                    self = converted;
                    type = ChronoLocalDate.class;
                    break adapt;
                }
            }
            // No else, we want this fallback.
            if (self instanceof LocalTime) {
                converted = toLocalTime(other);
                if (converted != null) {
                    other = converted;
                    type  = LocalTime.class;
                    break adapt;
                }
            } else if (other instanceof LocalTime) {
                converted = toLocalTime(self);
                if (converted != null) {
                    self = converted;
                    type = LocalTime.class;
                    break adapt;
                }
            }
            // No else, we want this fallback.
            final TimeMethods<?> methods = forTypes(self.getClass(), other.getClass(), false);
            if (methods != null && !methods.isDynamic) {
                assert methods.type.isInstance(self) : self;
                return ((TimeMethods) methods).convertAndCompare(test, self, other);
            }
            throw new DateTimeException(Errors.format(Errors.Keys.CannotCompareInstanceOf_2, self.getClass(), other.getClass()));
        }
        /*
         * The implementation of `TimeMethods.before/equals/after` functions delegate to this method in the
         * most generic cases (the `Object` and `Temporal` types declared in the `FOR_EXACT_TYPES` map).
         * Therefore, we must exclude the following block when `isDynamic` is true for avoiding infinite
         * recursive calls.
         */
        final TimeMethods<?> methods = forType(type, false);
        if (methods != null && !methods.isDynamic) {
            assert methods.type.isInstance(self)  : self;
            assert methods.type.isInstance(other) : other;
            return test.compare((TimeMethods) methods, self, other);
        } else if (self instanceof Comparable<?> && self.getClass().isInstance(other)) {
            // Case of `Month` and `DayOfWeek` which have no "is before" or "is after" operations.
            return test.fromCompareTo(((Comparable) self).compareTo(other));
        }
        /*
         * If we reach this point, the two operands are of different classes and we cannot compare them directly.
         * Try to compare the two operands as instants on the timeline.
         */
        return compareAsInstants(test, accessor(self), accessor(other));
    }

    /**
     * Compares an object of class {@code <T>} with a temporal object of arbitrary class.
     * The other object is typically the beginning or ending instant of a period and may
     * be converted to the {@code <T>} type before comparison.
     *
     * <p>The type of the {@code other} argument should be {@link TemporalAccessor},
     * but the method signature uses {@code Object} for accepting also {@link Date}.</p>
     *
     * @param  test   the test to apply (before, after and/or equal).
     * @param  self   the object on which to invoke the method identified by {@code test}.
     * @param  other  the argument to give to the test method call after conversion.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    @SuppressWarnings("unchecked")
    public boolean convertAndCompare(final Test test, final T self, final Object other) {
        if (converter != null) {
            final T converted = converter.apply(other);
            if (converted != null) {
                return test.compare(this, self, converted);
            }
        } else if (type.isInstance(other)) {
            return test.compare(this, self, (T) other);     // Safe because of above `isInstance(…)` check.
        } else if (other instanceof TemporalAccessor) {
            return compareAsInstants(test, accessor(self), (TemporalAccessor) other);
        }
        throw new DateTimeException(Errors.format(Errors.Keys.CannotCompareInstanceOf_2, self.getClass(), other.getClass()));
    }

    /**
     * Returns the given object as a temporal accessor.
     *
     * @throws DateTimeException if the object cannot be converted.
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
     * @param  test   the test to apply (before, after and/or equal).
     * @param  self   the object on which to invoke the method identified by {@code test}.
     * @param  other  the argument to give to the test method call.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    private static boolean compareAsInstants(final Test test, final TemporalAccessor self, final TemporalAccessor other) {
        long t1 =  self.getLong(ChronoField.INSTANT_SECONDS);
        long t2 = other.getLong(ChronoField.INSTANT_SECONDS);
        if (t1 == t2) {
            t1 =  self.getLong(ChronoField.NANO_OF_SECOND);     // Should be present according Javadoc.
            t2 = other.getLong(ChronoField.NANO_OF_SECOND);
        }
        return test.fromCompareTo(Long.compare(t1, t2));
    }

    /**
     * Returns the set of methods that can be invoked on instances of the given types.
     * If the types are too generic, then this method returns a fallback which will check
     * for a more specific type during filter execution.
     *
     * <p>It is guaranteed that {@code self} will be assignable to the {@link #type} of the returned value.
     * However, {@code other} will be assignable to {@link #type} only on a best-effort basis.
     * The {@link #convertAndCompare(Test, Object, TemporalAccessor)} method can be used when
     * {@code other} is not of that type.</p>
     *
     * @param  self   the type of the first operand in comparisons.
     * @param  other  the type of the second operand in comparisons.
     * @return set of comparison methods for operands of the given types, or {@code null} if not found.
     */
    public static TimeMethods<?> forTypes(final Class<?> self, final Class<?> other) {
        if (isTemporal(self) && isTemporal(other)) {
            return forTypes(self, other, true);
        }
        return null;
    }

    /**
     * Returns the set of methods that can be invoked on instances of the given types.
     * The {@code fallback} argument control whether to create a fallback if the types are too generic.
     * Fallback must be disabled when this method is invoked from {@link #compareTemporalOrDate(Test, Object, Object)},
     * otherwise never-ending recursive calls may happen.
     *
     * <p>It is guaranteed that {@code self} will be assignable to the {@link #type} of the returned value.
     * However, {@code other} will be assignable to {@link #type} only on a best-effort basis.</p>
     *
     * @param  self      the type of the first operand in comparisons.
     * @param  other     the type of the second operand in comparisons.
     * @param  fallback  whether fallback is allowed.
     * @return set of comparison methods for operands of the given types, or {@code null} if not found.
     */
    private static TimeMethods<?> forTypes(final Class<?> self, final Class<?> other, final boolean fallback) {
        if (self.isAssignableFrom(other)) return forType(self,  fallback);
        if (other.isAssignableFrom(self)) return forType(other, fallback);
        for (final Class<?> type : Classes.findCommonInterfaces(self, other)) {
            final TimeMethods<?> methods = forType(type, false);   // Fallback not wanted here.
            if (methods != null) {
                return methods;
            }
        }
        Class<?> type = Classes.findCommonClass(self, other);
        if (type == Object.class) {
            type = self.getClass();     // See method javadoc.
        }
        return forType(type, fallback);
    }

    /**
     * Returns the set of methods that can be invoked on instances of the given type, or {@code null} if none.
     * If {@code fallback} is {@code false}, then this method returns only one of the methods defined in
     * {@link #FOR_EXACT_TYPES} or {@link #FOR_PARENT_TYPES} without trying to create fallbacks.
     *
     * @param  <T>       compile-time value of the {@code type} argument.
     * @param  type      the type of temporal object for which to get specialized methods.
     * @param  fallback  whether to allow the creation of fallbacks.
     * @return set of specialized methods for the given object type, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    private static <T> TimeMethods<? super T> forType(final Class<T> type, final boolean fallback) {
        {   // Block for keeping `methods` in local scope.
            TimeMethods<?> methods = FOR_EXACT_TYPES.get(type);
            if (methods != null) {
                assert methods.type == type : methods;
                return (TimeMethods<T>) methods;             // Safe because of `==` checks.
            }
        }
        for (TimeMethods<?> methods : FOR_PARENT_TYPES) {
            if (methods.type.isAssignableFrom(type)) {
                return (TimeMethods<? super T>) methods;     // Safe because of `isAssignableFrom(…)` checks.
            }
        }
        if (fallback) {
            if (!Modifier.isFinal(type.getModifiers())) {
                return fallback(type);
            }
            if (Comparable.class.isAssignableFrom(type)) {
                return new TimeMethods<>(type, null,
                        (self, other) -> ((Comparable<T>) self).compareTo(other) < 0,
                        (self, other) -> ((Comparable<T>) self).compareTo(other) > 0,
                        (self, other) -> ((Comparable<T>) self).compareTo(other) == 0,
                        null, null, false, false);
            }
        }
        return null;
    }

    /**
     * Returns the last-resort fallback when the type of temporal objects cannot be determined in advance.
     * All methods delegate (indirectly) to {@link #compareIfTemporal(Test, Object, Object)}, which will
     * check the type at runtime.
     *
     * @param  <T>   compile-time value of the {@code type} argument.
     * @param  type  the type of temporal object for which to get the last-resource fallback methods.
     * @return set of last-resort comparison methods for the given object type.
     */
    @SuppressWarnings("unchecked")
    private static <T> TimeMethods<? super T> fallback(final Class<T> type) {
        return new TimeMethods<>(type, null,
                (self, other) -> compareIfTemporalElseFalse(Test.BEFORE, self, other),
                (self, other) -> compareIfTemporalElseFalse(Test.AFTER,  self, other),
                (self, other) -> compareIfTemporalElseFalse(Test.EQUAL,  self, other),
                type.isAssignableFrom(Instant.class) ? (Supplier) Instant::now : null,
                null, false, true);
    }

    /**
     * Returns the set of methods that can be invoked on instances of the given type.
     * If the type is too generic, then this method returns a fallback which will check
     * for a more specific type during filter execution.
     *
     * <p>The {@links Test tests} (is before, is after, is equal, <i>etc.</i>) expect
     * the first operand to be of type {@code <T>}. However, the second operand may be of
     * a different type if the {@link #convertAndCompare(Test, Object, TemporalAccessor)}
     * method is used.</p>
     *
     * @param  <T>   compile-time value of the {@code type} argument.
     * @param  type  the type of temporal object for which to get specialized methods.
     * @return set of comparison methods for the given object type.
     * @throws DateTimeException if it is known in advance that comparisons will not be possible.
     */
    public static <T> TimeMethods<? super T> forType(final Class<T> type) {
        final TimeMethods<? super T> methods = forType(type, true);
        if (methods != null) {
            return methods;
        }
        throw new DateTimeException(Errors.format(Errors.Keys.CannotCompareInstanceOf_2, type, type));
    }

    /**
     * Returns the unique instance for the type after deserialization.
     * This is needed for avoiding to serialize the lambda functions.
     *
     * @return the object to use after deserialization.
     * @throws ObjectStreamException if the serialized object contains invalid data.
     */
    private Object readResolve() throws ObjectStreamException {
        return forType(type);
    }

    /**
     * Returns the current time as a temporal object. This is the value returned by {@link #now},
     * except for the following types which are not {@link Temporal}: {@link Date}, {@link MonthDay}
     *
     * @return the current time. Never {@code null}, but may be an instance of a class different than {@linkplain #type}.
     */
    public final Temporal now() {
        if (now != null) {
            final T time = now.get();
            if (time instanceof Temporal) {
                return (Temporal) time;
            } else if (time instanceof Date) {
                return ((Date) time).toInstant();
            } else if (time instanceof MonthDay) {
                return LocalDate.now();
            }
        }
        return ZonedDateTime.now();
    }

    /**
     * Returns the given temporal object with the given timezone.
     * This method handles the following scenarios:
     *
     * <ul class="verbose">
     *   <li>
     *     If the given temporal object already has a timezone, then an object with the specified timezone is returned.
     *     It may be of the same class or a different class, depending on whether the timezone is a {@link ZoneOffset}.
     *   </li><li>
     *     If the given temporal object is a local time and if {@code allowAdd} is {@code true}, then a different class
     *     of object with the given timezone is returned. Otherwise, an empty value is returned.
     *   </li><li>
     *     If the given temporal object is a {@link LocalDate} or {@link MonthDay} or any other class of object
     *     for which a timezone cannot be added, then this method returns an empty value.
     *   </li>
     * </ul>
     *
     * @param  <T>       type of the {@code time} argument.
     * @param  time      the temporal object to return with the specified timezone, or {@code null} if none.
     * @param  timezone  the desired timezone. Cannot be {@code null}.
     * @param  allowAdd  whether to allow the addition of a time zone in an object that initially had none.
     * @return a temporal object with the specified timezone, if it was possible to apply a timezone.
     */
    public static <T extends Temporal> Optional<Temporal> withZone(final T time, final ZoneId timezone, final boolean allowAdd) {
        if (time != null) {
            final TimeMethods<? super T> methods = forType(Classes.getClass(time), false);
            if (methods != null && (methods.hasZone | allowAdd) && methods.withZone != null) {
                return Optional.ofNullable(methods.withZone.apply(time, timezone));
            }
        }
        return Optional.empty();
    }

    /**
     * Operators for all supported temporal types that are interfaces or non-final classes.
     * Those types need to be checked with {@link Class#isAssignableFrom(Class)} in iteration order.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})            // For `Chrono*` interfaces, because they are parameterized.
    private static final TimeMethods<?>[] FOR_PARENT_TYPES = {
        new TimeMethods<>(ChronoZonedDateTime.class,                         null, ChronoZonedDateTime::isBefore, ChronoZonedDateTime::isAfter, ChronoZonedDateTime::isEqual, ZonedDateTime::now, ChronoZonedDateTime::withZoneSameInstant, true, false),
        new TimeMethods<>(ChronoLocalDateTime.class, TimeMethods::toLocalDateTime, ChronoLocalDateTime::isBefore, ChronoLocalDateTime::isAfter, ChronoLocalDateTime::isEqual, LocalDateTime::now, ChronoLocalDateTime::atZone, false, false),
        new TimeMethods<>(    ChronoLocalDate.class,     TimeMethods::toLocalDate,     ChronoLocalDate::isBefore,     ChronoLocalDate::isAfter,     ChronoLocalDate::isEqual,     LocalDate::now, null, false, false),
        new TimeMethods<>(               Date.class,                         null,                Date::  before,                Date::  after,                Date::equals,           Date::new, TimeMethods::atZone, true, false),
        new TimeMethods<>(org.opengis.temporal.Instant.class,
                (object) -> (object instanceof Temporal) ? TemporalObjects.createInstant((Temporal) object) : null,
                (self, other) -> compareLenient(Test.BEFORE, self.getPosition(), other.getPosition()),
                (self, other) -> compareLenient(Test.AFTER,  self.getPosition(), other.getPosition()),
                (self, other) -> compareLenient(Test.EQUAL,  self.getPosition(), other.getPosition()),
                () -> DefaultInstant.NOW,
                null,     // withZone
                false,    // hasZone (not applicable)
                true)     // isDynamic (conservative value)
    };

    /*
     * No operation on numbers for now. We could revisit this policy in a future version if we
     * allow the temporal function to have a CRS and to operate on temporal coordinate values.
     */

    /**
     * Operators for all supported temporal types for which there is no need to check for subclasses.
     * Those classes should be final because they are compared by equality instead of "instance of".
     * The two last entries are not final, but we really want to ignore all their subtypes.
     * All those types should be tested before {@link #FOR_PARENT_TYPES} because this check is quick.
     *
     * <p><b>Implementation note:</b>
     * {@link Year}, {@link YearMonth}, {@link MonthDay}, {@link LocalTime} and {@link Instant}
     * could be replaced by {@link Comparable}. We nevertheless keep the specialized classes in
     * case the implementations change in the future, and also for performance reason, because
     * the code working on generic {@link Comparable} needs to check for special cases again.</p>
     */
    private static final Map<Class<?>, TimeMethods<?>> FOR_EXACT_TYPES = Map.ofEntries(
        entry(new TimeMethods<>(OffsetDateTime.class, TimeMethods::toOffsetDateTime, OffsetDateTime::isBefore, OffsetDateTime::isAfter, OffsetDateTime::isEqual, OffsetDateTime::now, TimeMethods::withZoneSameInstant, true, false)),
        entry(new TimeMethods<>( ZonedDateTime.class,                          null,  ZonedDateTime::isBefore,  ZonedDateTime::isAfter,  ZonedDateTime::isEqual,  ZonedDateTime::now, ZonedDateTime::withZoneSameInstant, true, false)),
        entry(new TimeMethods<>( LocalDateTime.class,                          null,  LocalDateTime::isBefore,  LocalDateTime::isAfter,  LocalDateTime::isEqual,  LocalDateTime::now, LocalDateTime::atZone, false, false)),
        entry(new TimeMethods<>(     LocalDate.class,                          null,      LocalDate::isBefore,      LocalDate::isAfter,      LocalDate::isEqual,      LocalDate::now, null, false, false)),
        entry(new TimeMethods<>(    OffsetTime.class,                          null,     OffsetTime::isBefore,     OffsetTime::isAfter,     OffsetTime::isEqual,     OffsetTime::now, TimeMethods::withOffsetSameInstant, true, false)),
        entry(new TimeMethods<>(     LocalTime.class,      TimeMethods::toLocalTime,      LocalTime::isBefore,      LocalTime::isAfter,      LocalTime::equals,       LocalTime::now, TimeMethods::atOffset, false, false)),
        entry(new TimeMethods<>(          Year.class,                          null,           Year::isBefore,           Year::isAfter,           Year::equals,            Year::now, null, false, false)),
        entry(new TimeMethods<>(     YearMonth.class,                          null,      YearMonth::isBefore,      YearMonth::isAfter,      YearMonth::equals,       YearMonth::now, null, false, false)),
        entry(new TimeMethods<>(      MonthDay.class,                          null,       MonthDay::isBefore,       MonthDay::isAfter,       MonthDay::equals,        MonthDay::now, null, false, false)),
        entry(new TimeMethods<>(       Instant.class,        TimeMethods::toInstant,        Instant::isBefore,        Instant::isAfter,        Instant::equals,         Instant::now, Instant::atZone, true, false)),
        entry(fallback(Temporal.class)),    // Frequently declared type. Intentionally no "instance of" checks.
        entry(fallback(Object.class)));     // Not a final class, but to be used when the declared type is Object.

    /**
     * Helper method for adding entries to the {@link #FOR_EXACT_TYPES} map.
     * Shall be used only for final classes.
     */
    private static Map.Entry<Class<?>, TimeMethods<?>> entry(final TimeMethods<?> op) {
        return Map.entry(op.type, op);
    }

    /**
     * Returns the given date at a specific time zone.
     */
    private static Temporal atZone(final Date date, final ZoneId timezone) {
        final Instant time = date.toInstant();
        if (timezone instanceof ZoneOffset) {
            return time.atOffset((ZoneOffset) timezone);
        } else {
            return time.atZone(timezone);
        }
    }

    /**
     * Returns a temporal object with the specified timezone, keeping the same class if possible.
     * This method may change the class of the temporal object.
     */
    private static Temporal withZoneSameInstant(final OffsetDateTime time, final ZoneId timezone) {
        if (timezone instanceof ZoneOffset) {
            return time.withOffsetSameInstant((ZoneOffset) timezone);
        } else {
            return time.atZoneSameInstant(timezone);
        }
    }

    /**
     * Returns a temporal object with the specified timezone, or {@code null} if not possible.
     */
    private static Temporal withOffsetSameInstant(final OffsetTime time, final ZoneId timezone) {
        if (timezone instanceof ZoneOffset) {
            return time.withOffsetSameInstant((ZoneOffset) timezone);
        } else {
            return null;
        }
    }

    /**
     * Returns a temporal object with the specified timezone, or {@code null} if not possible.
     */
    private static Temporal atOffset(final LocalTime time, final ZoneId timezone) {
        if (timezone instanceof ZoneOffset) {
            return time.atOffset((ZoneOffset) timezone);
        } else {
            return null;
        }
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
    private static Instant toInstant(final Object value) {
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
                 * of their coordinates on the timeline is undefined. For example, in the case of
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
     * Invoked when a conversion causes a field to be ignored. For example, if a "date+time" object
     * is compared with a "date" object, the "time" field is ignored. Expected values are:
     *
     * <ul>
     *   <li>{@link ChronoField#OFFSET_SECONDS}: time zone is ignored.</li>
     *   <li>{@link ChronoField#SECOND_OF_DAY}:  time of {@code DateTime} and time zone are ignored.</li>
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
     * Returns a string representation of this set of operations for debugging purposes.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(TimeMethods.class, "type", type.getSimpleName());
    }
}
