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
package org.apache.sis.measure;

import java.util.Date;
import javax.measure.unit.Unit;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.resources.Errors;


/**
 * A range of dates. The elements in this range are {@link Date} objects.
 * Consequently the precision of {@code DateRange} objects is milliseconds.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 *
 * @see RangeFormat
 */
@Immutable
public class DateRange extends Range<Date> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6400011350250757942L;

    /**
     * Creates a new date range for the given dates. Start time and end time are inclusive.
     *
     * @param startTime The start time (inclusive), or {@code null} if none.
     * @param endTime   The end time (inclusive), or {@code null} if none.
     */
    public DateRange(final Date startTime, final Date endTime) {
        super(Date.class, clone(startTime), clone(endTime));
    }

    /**
     * Creates a new date range for the given dates.
     *
     * @param startTime     The start time, or {@code null} if none.
     * @param isMinIncluded {@code true} if the start time is inclusive.
     * @param endTime       The end time, or {@code null} if none.
     * @param isMaxIncluded {@code true} if the end time is inclusive.
     */
    public DateRange(final Date startTime, boolean isMinIncluded,
                     final Date   endTime, boolean isMaxIncluded)
    {
        super(Date.class, clone(startTime), isMinIncluded,
                          clone(  endTime), isMaxIncluded);
    }

    /**
     * Creates a date range from the specified measurement range. Units are converted as needed.
     *
     * @param  range The range to convert.
     * @param  origin The date to use as the origin.
     * @throws ConversionException if the given range doesn't have a
     *         {@linkplain MeasurementRange#getUnits unit} compatible with milliseconds.
     */
    public DateRange(final MeasurementRange<?> range, final Date origin) throws ConversionException {
        this(range, getConverter(range.getUnits()), origin.getTime());
    }

    /**
     * Workaround for RFE #4093999 ("Relax constraint on placement of this()/super()
     * call in constructors").
     */
    private DateRange(final MeasurementRange<?> range, final UnitConverter converter, final long origin)
            throws ConversionException
    {
        super(Date.class,
              new Date(origin + Math.round(converter.convert(range.getMinimum()))), range.isMinIncluded(),
              new Date(origin + Math.round(converter.convert(range.getMaximum()))), range.isMaxIncluded());
    }

    /**
     * Creates a new date range using the given values. This method is invoked by the
     * parent class for creating the result of an intersection or union operation.
     */
    @Override
    final DateRange create(final Date minValue, final boolean isMinIncluded,
                           final Date maxValue, final boolean isMaxIncluded)
    {
        return new DateRange(minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Returns an initially empty array of the given length.
     */
    @Override
    final DateRange[] newArray(final int length) {
        return new DateRange[length];
    }

    /**
     * Ensures that {@link #elementType} is compatible with the type expected by this range class.
     * Invoked for argument checking by the super-class constructor.
     */
    @Override
    final void ensureValidType() throws IllegalArgumentException {
        // No need to call super.checkElementClass() because Date implements Comparable.
        if (!Date.class.isAssignableFrom(elementType)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalClass_2, Date.class, elementType));
        }
    }

    /**
     * Casts the given {@code Range} object to a {@code DateRange}. This method shall be invoked
     * only in context where we have verified that the range element class is compatible.
     * This verification is performed by {@link Range#ensureCompatible(Range)} method.
     */
    private static DateRange cast(final Range<?> range) {
        if (range == null || range instanceof DateRange) {
            return (DateRange) range;
        }
        return new DateRange((Date) (Object) range.getMinValue(), range.isMinIncluded(),
                             (Date) (Object) range.getMaxValue(), range.isMaxIncluded());
    }

    /**
     * Returns a clone of the specified date.
     */
    private static Date clone(final Date date) {
        return (date != null) ? (Date) date.clone() : null;
    }

    /**
     * Workaround for RFE #4093999 ("Relax constraint on placement of this()/super()
     * call in constructors").
     */
    private static UnitConverter getConverter(final Unit<?> source) throws ConversionException {
        if (source == null) {
            throw new ConversionException(Errors.format(Errors.Keys.NoUnit));
        }
        return source.getConverterToAny(Units.MILLISECOND);
    }

    /**
     * Returns the start time, or {@code null} if none.
     */
    @Override
    public Date getMinValue() {
        return clone(super.getMinValue());
    }

    /**
     * Returns the end time, or {@code null} if none.
     */
    @Override
    public Date getMaxValue() {
        return clone(super.getMaxValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateRange union(final Range<?> range) throws IllegalArgumentException {
        return cast(super.union(range));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateRange intersect(final Range<?> range) throws IllegalArgumentException {
        return cast(super.intersect(range));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateRange[] subtract(final Range<?> range) throws IllegalArgumentException {
        return (DateRange[]) super.subtract(range);
        // Should never throw ClassCastException because super.subtract(Range) invokes newArray(int)
        // and create(...), which are overridden in this class with DateRange return type.
    }
}
