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

import java.io.Serializable;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.collection.CheckedContainer;


/**
 * A set of minimum and maximum values of a certain class, allowing
 * a user to determine if a value of the same class is contained inside the range.
 * The minimum and maximum values do not have to be included in the range, and
 * can be null.  If the minimum or maximum values are null, the range is said to
 * be unbounded on that extreme. If both the minimum and maximum are null,
 * the range is completely unbounded and all values of that class are contained
 * within the range. Null values are always considered <em>exclusive</em>,
 * since iterations over the values will never reach the infinite bound.
 *
 * <p>To be a member of a {@code Range}, the class type defining the range must implement
 * the {@link Comparable} interface.</p>
 *
 * @param <T> The type of range elements, typically a {@link Number} subclass or {@link java.util.Date}.
 *
 * @author  Joe White
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see RangeFormat
 */
@Immutable
public class Range<T extends Comparable<? super T>> implements CheckedContainer<T>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5393896130562660517L;

    /**
     * The base type of elements in this range.
     *
     * @see #getElementType()
     */
    private final Class<T> elementType;

    /**
     * The minimal and maximal values.
     */
    final T minValue, maxValue;

    /**
     * Whether the minimal or maximum value is included.
     */
    private final boolean isMinIncluded, isMaxIncluded;

    private static String INVALID_TYPE_ERROR = "Type to be compared does not match the Range type.";

    /**
     * Creates a new range bounded by the given inclusive values.
     *
     * @param elementType  The class of the range elements.
     * @param minValue     The minimal value (inclusive), or {@code null} if none.
     * @param maxValue     The maximal value (inclusive), or {@code null} if none.
     */
    public Range(final Class<T> elementType, final T minValue,
            final T maxValue) throws IllegalArgumentException
    {
        this(elementType, minValue, true, maxValue, true);
    }

    /**
     * Creates a new range bounded by the given values.
     *
     * @param elementType    The base type of the range elements.
     * @param minValue       The minimal value, or {@code null} if none.
     * @param isMinIncluded  {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     * @param maxValue       The maximal value, or {@code null} if none.
     * @param isMaxIncluded  {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     */
    public Range(final Class<T> elementType,
            final T minValue, final boolean isMinIncluded,
            final T maxValue, final boolean isMaxIncluded) throws IllegalArgumentException
    {
        if(!checkConstructorArgs(elementType, minValue, maxValue))
        {
            throw new IllegalArgumentException();
        }
        this.elementType   = elementType;
        this.minValue      = minValue;
        this.isMinIncluded = isMinIncluded && (minValue != null);
        this.maxValue      = maxValue;
        this.isMaxIncluded = isMaxIncluded && (maxValue != null);
    }

    /**
     * Returns the base type of elements in this range.
     * This is the type specified at construction time.
     */
    @Override
    public Class<T> getElementType() {
        return elementType;
    }

    /**
     * Returns the minimal value, or {@code null} if this range has no lower limit.
     * If non-null, the returned value is either inclusive or exclusive depending on
     * the boolean returned by {@link #isMinIncluded()}.
     *
     * @return The minimal value, or {@code null} if this range is unbounded on the lower side.
     */
    public T getMinValue() {
        return minValue;
    }

    /**
     * Returns {@code true} if the {@linkplain #getMinValue() minimal value} is inclusive,
     * or {@code false} is exclusive. Note that {@code null} values are always considered
     * exclusive.
     *
     * @return {@code true} if the minimal value is inclusive, or {@code false} if exclusive.
     */
    public boolean isMinIncluded() {
        return isMinIncluded;
    }

    /**
     * Returns the maximal value, or {@code null} if this range has no upper limit.
     * If non-null, the returned value is either inclusive or exclusive depending on
     * the boolean returned by {@link #isMaxIncluded()}.
     *
     * @return The maximal value, or {@code null} if this range is unbounded on the upper side.
     */
    public T getMaxValue() {
        return maxValue;
    }

    /**
     * Returns {@code true} if the {@linkplain #getMaxValue() maximal value} is inclusive,
     * or {@code false} is exclusive. Note that {@code null} values are always considered
     * exclusive.
     *
     * @return {@code true} if the maximal value is inclusive, or {@code false} if exclusive.
     */
    public boolean isMaxIncluded() {
        return isMaxIncluded;
    }

    public boolean contains(final T value) throws IllegalArgumentException
    {

        boolean unbounded = (minValue == null && maxValue == null);
        //safety check
        if (value == null)
        {
            if (unbounded)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        //first check
        if (value.getClass() != elementType)
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }

        //set unbounded on both ends
        if (unbounded)
        {
            return true;
        }

        int minimumValueCheck = 1;
        if (minValue != null)
        {
            minimumValueCheck = value.compareTo(minValue);
        }
        int maximumValueCheck = -1;
        if (maxValue != null)
        {
            maximumValueCheck = value.compareTo(maxValue);
        }

        //set unbounded on lower end
        if (minValue == null && maximumValueCheck <= 0)
        {
            if (isMaxIncluded && minimumValueCheck > 0)
            {
                return true;
            }
            else if (!isMaxIncluded)
            {
                return true;
            }

        }

        //set unbounded on upper end
        if (maxValue == null && minimumValueCheck >= 0)
        {
            if (isMinIncluded && minimumValueCheck > 0)
            {
                return true;
            }
            else if (!isMinIncluded)
            {
                return true;
            }
        }

        //set bounded on both ends
        if (minimumValueCheck >= 0 && maximumValueCheck <= 0)
        {
            //both min and max are included
            if (isMinIncluded && isMaxIncluded)
            {
                return true;
            }
            //only min is included
            else if (!isMinIncluded && minimumValueCheck > 0)
            {
                return true;
            }
            //only max is included
            else if (!isMaxIncluded && maximumValueCheck < 0)
            {
                return true;
            }
            //neither is included
            else
            {
                return (minimumValueCheck > 0 && maximumValueCheck < 0);
            }

        }
        return false;

    }


    public boolean contains(final Range<T> value) throws IllegalArgumentException
    {
        if (!checkMethodArgs(value))
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }
        return this.contains(value.getMinValue()) && this.contains(value.getMaxValue());
    }


    public boolean intersects(final Range<T> value) throws IllegalArgumentException
    {
        if (!checkMethodArgs(value))
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }

        return this.contains(value.getMinValue()) || this.contains(value.getMaxValue());
    }

    public Range<T> union(final Range<T> value) throws IllegalArgumentException
    {
        if (!checkMethodArgs(value))
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }

        //if they are both the same, return either one
        if (this.equals(value))
        {
            return value;
        }

        //get the min and max value of both sets, compare them, then take
        //the smallest of either and the largest of either and create
        //a new Range with them.
        T rangeMin, rangeMax;
        if (value.getMinValue().compareTo(minValue) <= 0)
        {
            rangeMin = value.getMinValue();
        }
        else
        {
            rangeMin = minValue;
        }

        if (value.getMaxValue().compareTo(maxValue) >= 0)
        {
            rangeMax = value.getMaxValue();
        }
        else
        {
            rangeMax = maxValue;
        }
        return new Range<>(this.elementType, rangeMin, rangeMax );
    }

    public Range<T> intersect(final Range<T> value) throws IllegalArgumentException
    {
        if (!checkMethodArgs(value))
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }

        //return empty set if the Ranges don't intersect
        if (!this.intersects(value))
        {
            return new Range<>(elementType, maxValue, minValue);
        }

        //if they are equal, return the passed in value
        if (this.equals(value))
        {
            return value;
        }

        //we knkow they intersect, the question is where.
        T rangeMin, rangeMax;
        if (this.contains(value.getMinValue()))
        {
            rangeMin = value.getMinValue();
        }
        else
        {
            rangeMin = minValue;
        }

        if (this.contains(value.getMaxValue()))
        {
            rangeMax = value.getMaxValue();
        }
        else
        {
            rangeMax = maxValue;
        }

        return new Range<>(this.elementType, rangeMin, rangeMax );

    }

    //TODO: implement this
    public Range<T>[] subtract(final Range<T> value) throws IllegalArgumentException
    {
        if (!checkMethodArgs(value))
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }
        Range<T>[] ranges = new Range[1];
        ranges[0] = null;
        return ranges;
    }

    public boolean isEmpty()
    {
        if (isMinIncluded && isMaxIncluded)
        {
            if (minValue.compareTo(maxValue) > 0)
            {
                return true;
            }
        }
        else
        {
            if (minValue.compareTo(maxValue) >= 0)
            {
                return true;
            }

        }
        return false;
    }

    @Override
    public boolean equals(Object object)
    {
        //make sure it's not null
        if (object == null)
        {
            return false;
        }


        Range<?> value = (Range<?>) object;
        if (value == null)
        {
            return false;
        }

        boolean retVal = true;
        retVal &= this.elementType == value.getElementType();
        if (value.isEmpty() && this.isEmpty())
        {
            return retVal;
        }

        retVal &= this.maxValue == value.getMaxValue();
        retVal &= this.minValue == value.getMinValue();
        retVal &= this.isMaxIncluded == value.isMaxIncluded();
        retVal &= this.isMinIncluded == value.isMinIncluded();
        return retVal;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 13 * hash + (this.minValue != null ? this.minValue.hashCode() : 0);
        hash = 13 * hash + (this.maxValue != null ? this.maxValue.hashCode() : 0);
        hash = 13 * hash + (this.elementType != null ? this.elementType.hashCode() : 0);
        hash = 13 * hash + (this.isMinIncluded ? 1 : 0);
        hash = 13 * hash + (this.isMaxIncluded ? 1 : 0);
        return hash;
    }

    private static <T> boolean checkConstructorArgs(final Class<T> elementType,
            final T minValue, final T maxValue)
    {
        boolean retVal = true;
        if (minValue != null)
        {
            boolean minimumOk = minValue.getClass() == elementType;
            retVal &= minimumOk;
        }

        if (maxValue != null)
        {
            boolean maximumOk = maxValue.getClass() == elementType;
            retVal &= maximumOk;
        }

        if (minValue == null && maxValue == null)
        {
            Class[] interfaces = elementType.getInterfaces();
            boolean comparableFound = false;
            for (Class<?> interf : interfaces)
            {
                if (interf == Comparable.class)
                {
                    comparableFound = true;
                }
            }
            retVal &= comparableFound;
        }
        return retVal;
    }

    private boolean checkMethodArgs(final Range<T> value) {
        if (value == null)
        {
            return false;
        }
        else if (  value.getElementType() != elementType)
        {
            return false;
        }
        return true;

    }
}
