/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.measure;

/**
 * A range is a set of minimum and maximum values of a certain class, allowing
 * a user to determine if a value of the same class is contained inside the range.
 * The minimum and maximum values do not have to be included in the range, and
 * can be null.  If the minimum or maximum values are null, the range is said to
 * be unbounded on that extreme. If both the minimum and maximum are null,
 * the range is completely unbounded and all values of that class are contained
 * within the range.
 *
 * To be a member of a Range, the class type defining the Range must implement
 * the Comparable interface.
 *
 * @author Joe White
 */
public class Range
{

    private Comparable minimumValue;
    private Comparable maximumValue;
    private Class rangeType;
    private boolean isMinimumIncluded;
    private boolean isMaximumIncluded;
    private static String INVALID_TYPE_ERROR = "Type to be compared does not match the Range type.";

    public Range(Class elementClass, Comparable minValue, boolean isMinIncluded,
          Comparable maxValue, boolean isMaxIncluded) throws IllegalArgumentException
    {
        if(!checkConstructorArgs(elementClass, minValue, maxValue))
        {
            throw new IllegalArgumentException();
        }
        rangeType = elementClass;
        minimumValue = minValue;
        isMinimumIncluded = isMinIncluded;
        maximumValue = maxValue;
        isMaximumIncluded = isMaxIncluded;
    }

    public Range(Class elementClass, Comparable minValue,
            Comparable maxValue) throws IllegalArgumentException
    {
        if(!checkConstructorArgs(elementClass, minValue, maxValue))
        {
            throw new IllegalArgumentException();
        }

        rangeType = elementClass;
        minimumValue = minValue;
        isMinimumIncluded = true;
        maximumValue = maxValue;
        isMaximumIncluded = true;
    }

    public boolean contains(Comparable value) throws IllegalArgumentException
    {

        boolean unbounded = (minimumValue == null && maximumValue == null);
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
        if (value.getClass() != rangeType)
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }

        //set unbounded on both ends
        if (unbounded)
        {
            return true;
        }

        int minimumValueCheck = 1;
        if (minimumValue != null)
        {
            minimumValueCheck = value.compareTo(minimumValue);
        }
        int maximumValueCheck = -1;
        if (maximumValue != null)
        {
            maximumValueCheck = value.compareTo(maximumValue);
        }

        //set unbounded on lower end
        if (minimumValue == null && maximumValueCheck <= 0)
        {
            if (isMaximumIncluded && minimumValueCheck > 0)
            {
                return true;
            }
            else if (!isMaximumIncluded)
            {
                return true;
            }

        }

        //set unbounded on upper end
        if (maximumValue == null && minimumValueCheck >= 0)
        {
            if (isMinimumIncluded && minimumValueCheck > 0)
            {
                return true;
            }
            else if (!isMinimumIncluded)
            {
                return true;
            }
        }

        //set bounded on both ends
        if (minimumValueCheck >= 0 && maximumValueCheck <= 0)
        {
            //both min and max are included
            if (isMinimumIncluded && isMaximumIncluded)
            {
                return true;
            }
            //only min is included
            else if (!isMinimumIncluded && minimumValueCheck > 0)
            {
                return true;
            }
            //only max is included
            else if (!isMaximumIncluded && maximumValueCheck < 0)
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


    public boolean contains(Range value) throws IllegalArgumentException
    {
        if (!checkMethodArgs(value))
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }
        return this.contains(value.getMinValue()) && this.contains(value.getMaxValue());
    }


    public boolean intersects(Range value) throws IllegalArgumentException
    {
        if (!checkMethodArgs(value))
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }

        return this.contains(value.getMinValue()) || this.contains(value.getMaxValue());
    }

    public Range union(Range value) throws IllegalArgumentException
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
        Comparable rangeMin, rangeMax;
        if (value.getMinValue().compareTo(minimumValue) <= 0)
        {
            rangeMin = value.getMinValue();
        }
        else
        {
            rangeMin = minimumValue;
        }

        if (value.getMaxValue().compareTo(maximumValue) >= 0)
        {
            rangeMax = value.getMaxValue();
        }
        else
        {
            rangeMax = maximumValue;
        }
        return new Range(this.rangeType, rangeMin, rangeMax );
    }

    public Range intersect(Range value) throws IllegalArgumentException
    {
        if (!checkMethodArgs(value))
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }

        //return empty set if the Ranges don't intersect
        if (!this.intersects(value))
        {
            return new Range(rangeType, maximumValue, minimumValue);
        }

        //if they are equal, return the passed in value
        if (this.equals(value))
        {
            return value;
        }

        //we knkow they intersect, the question is where.
        Comparable rangeMin, rangeMax;
        if (this.contains(value.getMinValue()))
        {
            rangeMin = value.getMinValue();
        }
        else
        {
            rangeMin = minimumValue;
        }

        if (this.contains(value.getMaxValue()))
        {
            rangeMax = value.getMaxValue();
        }
        else
        {
            rangeMax = maximumValue;
        }

        return new Range(this.rangeType, rangeMin, rangeMax );

    }

    //TODO: implement this
    public Range[] subtract(Range value) throws IllegalArgumentException
    {
        if (!checkMethodArgs(value))
        {
            throw new IllegalArgumentException(INVALID_TYPE_ERROR);
        }
        Range[] ranges = new Range[1];
        ranges[0] = null;
        return ranges;
    }

    public boolean isEmpty()
    {
        if (isMinimumIncluded && isMaximumIncluded)
        {
            if (minimumValue.compareTo(maximumValue) > 0)
            {
                return true;
            }
        }
        else
        {
            if (minimumValue.compareTo(maximumValue) >= 0)
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


        Range value = (Range)object;
        if (value == null)
        {
            return false;
        }

        boolean retVal = true;
        retVal &= this.rangeType == value.getElementClass();
        if (value.isEmpty() && this.isEmpty())
        {
            return retVal;
        }

        retVal &= this.maximumValue == value.getMaxValue();
        retVal &= this.minimumValue == value.getMinValue();
        retVal &= this.isMaximumIncluded == value.isMaxIncluded();
        retVal &= this.isMinimumIncluded == value.isMinIncluded();
        return retVal;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 13 * hash + (this.minimumValue != null ? this.minimumValue.hashCode() : 0);
        hash = 13 * hash + (this.maximumValue != null ? this.maximumValue.hashCode() : 0);
        hash = 13 * hash + (this.rangeType != null ? this.rangeType.hashCode() : 0);
        hash = 13 * hash + (this.isMinimumIncluded ? 1 : 0);
        hash = 13 * hash + (this.isMaximumIncluded ? 1 : 0);
        return hash;
    }

    public boolean isMinIncluded()
    {
        return isMinimumIncluded;
    }

    public boolean isMaxIncluded()
    {
        return isMaximumIncluded;
    }

    public Class getElementClass()
    {
        return rangeType;
    }

    public Comparable getMinValue()
    {
        return minimumValue;
    }

    public Comparable getMaxValue()
    {
        return maximumValue;
    }


    private boolean checkConstructorArgs(Class elementClass, Comparable minValue,
            Comparable maxValue)
    {
        boolean retVal = true;
        if (minValue != null)
        {
            boolean minimumOk = minValue.getClass() == elementClass;
            retVal &= minimumOk;
        }

        if (maxValue != null)
        {
            boolean maximumOk = maxValue.getClass() == elementClass;
            retVal &= maximumOk;
        }

        if (minValue == null && maxValue == null)
        {
            Class[] interfaces = elementClass.getInterfaces();
            boolean comparableFound = false;
            for (Class interf : interfaces)
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

    private boolean checkMethodArgs(Range value)
    {
        if (value == null)
        {
            return false;
        }
        else if (  value.getElementClass() != rangeType)
        {
            return false;
        }
        return true;

    }
}
