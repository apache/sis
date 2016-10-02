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
package org.apache.sis.internal.netcdf.impl;

import java.util.Map;
import java.io.IOException;
import java.lang.reflect.Array;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;
import org.apache.sis.internal.netcdf.DataType;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.storage.ChannelDataInput;
import org.apache.sis.internal.storage.HyperRectangleReader;
import org.apache.sis.internal.storage.Region;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Numbers;
import org.apache.sis.math.Vector;


/**
 * Description of a variable found in a NetCDF file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
final class VariableInfo extends Variable {
    /**
     * The array to be returned by {@link #numberValues(Object)} when the given value is null.
     */
    private static final Number[] EMPTY = new Number[0];

    /**
     * The names of attributes where to look for the description to be returned by {@link #getDescription()}.
     * We use the same attributes than the one documented in the {@link ucar.nc2.Variable#getDescription()} javadoc.
     */
    private static final String[] DESCRIPTION_ATTRIBUTES = {
        CDM.LONG_NAME,
        CDM.DESCRIPTION,
        CDM.TITLE,
        CF.STANDARD_NAME
    };

    /**
     * Helper class for reading a sub-area with a sub-sampling,
     * or {@code null} if {@code dataType} is not a supported type.
     */
    private final HyperRectangleReader reader;

    /**
     * The variable name.
     */
    private final String name;

    /**
     * The dimensions of this variable, in order. When iterating over the values stored in this variable
     * (a flattened one-dimensional sequence of values), index in the domain of {@code dimensions[0]}
     * varies faster, followed by index in the domain of {@code dimensions[1]}, <i>etc.</i>
     */
    final Dimension[] dimensions;

    /**
     * The attributes associates to the variable, or an empty map if none.
     * Values can be:
     *
     * <ul>
     *   <li>a {@link String}</li>
     *   <li>A {@link Number}</li>
     *   <li>an array of primitive type</li>
     * </ul>
     *
     * If the value is a {@code String}, then leading and trailing spaces and control characters
     * should be trimmed by {@link String#trim()}.
     *
     * @see #stringValues(Object)
     * @see #numberValues(Object)
     * @see #booleanValue(Object)
     */
    private final Map<String,Object> attributes;

    /**
     * The NetCDF type of data, or {@code null} if unknown.
     */
    private final DataType dataType;

    /**
     * The grid geometry associated to this variable,
     * computed by {@link ChannelDecoder#getGridGeometries()} when first needed.
     */
    GridGeometryInfo gridGeometry;

    /**
     * {@code true} if this variable seems to be a coordinate system axis, as determined by comparing its name
     * with the name of all dimensions in the NetCDF file. This information is computed at construction time
     * because requested more than once.
     *
     * @see #isCoordinateSystemAxis()
     */
    private final boolean isCoordinateSystemAxis;

    /**
     * Creates a new variable.
     *
     * @param  input          the channel together with a buffer for reading the variable data.
     * @param  name           the variable name.
     * @param  dimensions     the dimensions of this variable.
     * @param  attributes     the attributes associates to the variable, or an empty map if none.
     * @param  dataType       the NetCDF type of data, or {@code null} if unknown.
     * @param  size           the variable size, used for verification purpose only.
     * @param  offset         the offset where the variable data begins in the NetCDF file.
     */
    VariableInfo(final ChannelDataInput      input,
                 final String                name,
                 final Dimension[]           dimensions,
                 final Map<String,Object>    attributes,
                       DataType              dataType,
                 final int                   size,
                 final long                  offset) throws DataStoreException
    {
        final Object isUnsigned = attributes.get(CDM.UNSIGNED);
        if (isUnsigned != null) {
            dataType = dataType.unsigned(booleanValue(isUnsigned));
        }
        this.name       = name;
        this.dimensions = dimensions;
        this.attributes = attributes;
        this.dataType   = dataType;
        /*
         * The 'size' value is provided in the NetCDF files, but doesn't need to be stored since it
         * is redundant with the dimension lengths and is not large enough for big variables anyway.
         */
        if (dataType != null && dataType.number >= Numbers.BYTE && dataType.number <= Numbers.DOUBLE) {
            reader = new HyperRectangleReader(dataType.number, input, offset);
        } else {
            reader = null;
        }
        /*
         * If the "_CoordinateAliasForDimension" attribute is defined, then its value will be used
         * instead of the variable name when determining if the variable is a coordinate system axis.
         * "_CoordinateVariableAlias" seems to be a legacy attribute name for the same purpose.
         */
        if (dimensions.length == 1) {
            Object value = getAttributeValue(_Coordinate.AliasForDimension, "_coordinatealiasfordimension");
            if (value == null) {
                value = getAttributeValue("_CoordinateVariableAlias", "_coordinatevariablealias");
                if (value == null) {
                    value = name;
                }
            }
            isCoordinateSystemAxis = dimensions[0].name.equals(value);
        } else {
            isCoordinateSystemAxis = false;
        }
    }

    /**
     * Returns the name of this variable.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the description of this variable, or {@code null} if none.
     * This method searches for the first attribute named {@code "long_name"},
     * {@code "description"}, {@code "title"} or {@code "standard_name"}.
     */
    @Override
    public String getDescription() {
        for (final String attributeName : DESCRIPTION_ATTRIBUTES) {
            final Object value = getAttributeValue(attributeName);
            if (value instanceof String) {
                return (String) value;
            }
        }
        return null;
    }

    /**
     * Returns the unit of measurement as a string, or {@code null} if none.
     */
    @Override
    public String getUnitsString() {
        final Object value = getAttributeValue(CDM.UNITS);
        return (value instanceof String) ? (String) value : null;
    }

    /**
     * Returns the type of data, or {@code UNKNOWN} if the data type is unknown to this method.
     * If this variable has a {@code "_Unsigned = true"} attribute, then the returned data type
     * will be a unsigned variant.
     */
    @Override
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Returns {@code true} if this variable seems to be a coordinate system axis,
     * determined by comparing its name with the name of all dimensions in the NetCDF file.
     */
    @Override
    public boolean isCoordinateSystemAxis() {
        return isCoordinateSystemAxis;
    }

    /**
     * Returns the value of the {@code "_CoordinateAxisType"} attribute, or {@code null} if none.
     */
    final String getAxisType() {
        final Object value = getAttributeValue(_Coordinate.AxisType, "_coordinateaxistype");
        return (value instanceof String) ? (String) value : null;
    }

    /**
     * Returns the names of the dimensions of this variable.
     * The dimensions are those of the grid, not the dimensions of the coordinate system.
     */
    @Override
    public String[] getGridDimensionNames() {
        final String[] names = new String[dimensions.length];
        for (int i=0; i<names.length; i++) {
            names[i] = dimensions[i].name;
        }
        return names;
    }

    /**
     * Returns the length (number of cells) of each grid dimension. In ISO 19123 terminology, this method
     * returns the upper corner of the grid envelope plus one. The lower corner is always (0,0,…,0).
     *
     * @return the number of grid cells for each dimension, as unsigned integers.
     */
    @Override
    public int[] getGridEnvelope() {
        final int[] shape = new int[dimensions.length];
        for (int i=0; i<shape.length; i++) {
            shape[i] = dimensions[i].length;
        }
        return shape;
    }

    /**
     * Returns the value of the given attribute, or {@code null} if none.
     * This method should be invoked only for hard-coded names that mix lower-case and upper-case letters.
     *
     * @param  attributeName  name of attribute to search, in the expected case.
     * @param  lowerCase      the all lower-case variant of {@code attributeName}.
     * @return variable attribute value of the given name, or {@code null} if none.
     */
    private Object getAttributeValue(final String attributeName, final String lowerCase) {
        Object value = attributes.get(attributeName);
        if (value == null) {
            value = attributes.get(lowerCase);
        }
        return value;
    }

    /**
     * Returns the value of the given attribute, or {@code null} if none.
     * This method does not search the lower-case variant of the given name because the argument given to this method
     * is usually a hard-coded value from {@link CF} or {@link CDM} conventions, which are already in lower-cases.
     *
     * @param  attributeName  name of attribute to search, in the expected case.
     * @return variable attribute value of the given name, or {@code null} if none.
     */
    final Object getAttributeValue(final String attributeName) {
        return attributes.get(attributeName);
    }

    /**
     * Returns the sequence of values for the given attribute, or an empty array if none.
     * The elements will be of class {@link String} if {@code numeric} is {@code false},
     * or {@link Number} if {@code numeric} is {@code true}.
     */
    @Override
    public Object[] getAttributeValues(final String attributeName, final boolean numeric) {
        final Object value = getAttributeValue(attributeName);
        return numeric ? numberValues(value) : stringValues(value);
    }

    /**
     * Returns the attribute values as an array of {@link String}s, or an empty array if none.
     * The given argument is typically a value of the {@link #attributes} map.
     *
     * @see #getAttributeValues(String, boolean)
     */
    static String[] stringValues(final Object value) {
        if (value == null) {
            return CharSequences.EMPTY_ARRAY;
        }
        if (value.getClass().isArray()) {
            final String[] values = new String[Array.getLength(value)];
            for (int i=0; i<values.length; i++) {
                values[i] = Array.get(value, i).toString();
            }
            return values;
        }
        return new String[] {value.toString()};
    }

    /**
     * Returns the attribute values as an array of {@link Number}, or an empty array if none.
     * The given argument is typically a value of the {@link #attributes} map.
     *
     * @see #getAttributeValues(String, boolean)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    static Number[] numberValues(final Object value) {
        if (value != null) {
            if (value.getClass().isArray()) {
                final Number[] values = new Number[Array.getLength(value)];
                for (int i=0; i<values.length; i++) {
                    values[i] = (Number) Array.get(value, i);
                }
                return values;
            }
            if (value instanceof Number) {
                return new Number[] {(Number) value};
            }
        }
        return EMPTY;
    }

    /**
     * Returns the attribute value as a boolean, or {@code false} if the attribute is not a boolean.
     * The given argument is typically a value of the {@link #attributes} map.
     */
    private static boolean booleanValue(final Object value) {
        return (value instanceof String) && Boolean.valueOf((String) value);
    }

    /**
     * Reads all the data for this variable and returns them as an array of a Java primitive type.
     */
    @Override
    public Vector read() throws IOException, DataStoreException {
        if (reader == null) {
            throw new DataStoreContentException(unknownType());
        }
        long length = 1;
        for (final Dimension dimension : dimensions) {
            length *= dimension.length;
        }
        if (length > Integer.MAX_VALUE) {
            throw new DataStoreContentException(Errors.format(Errors.Keys.ExcessiveListSize_2, name, length));
        }
        final int dimension = dimensions.length;
        final long[] size  = new long[dimension];
        final int [] sub   = new int [dimension];
        for (int i=0; i<dimension; i++) {
            sub [i] = 1;
            size[i] = dimensions[(dimension - 1) - i].length();
        }
        return Vector.create(reader.read(new Region(size, new long[dimension], size, sub)), dataType.isUnsigned);
    }

    /**
     * Reads a sub-sampled sub-area of the variable.
     *
     * @param  areaLower    index of the first value to read along each dimension, as unsigned integers.
     * @param  areaUpper    index after the last value to read along each dimension, as unsigned integers.
     * @param  subsampling  sub-sampling along each dimension. 1 means no sub-sampling.
     * @return the data as an array of a Java primitive type.
     */
    @Override
    public Vector read(int[] areaLower, int[] areaUpper, int[] subsampling) throws IOException, DataStoreException {
        if (reader == null) {
            throw new DataStoreContentException(unknownType());
        }
        /*
         * NetCDF sorts datas in reverse dimension order. Example:
         *
         * DIMENSIONS:
         *   time: 3
         *   lat : 2
         *   lon : 4
         *
         * VARIABLES:
         *   temperature (time,lat,lon)
         *
         * DATA INDICES:
         *   (0,0,0) (0,0,1) (0,0,2) (0,0,3)
         *   (0,1,0) (0,1,1) (0,1,2) (0,1,3)
         *   (1,0,0) (1,0,1) (1,0,2) (1,0,3)
         *   (1,1,0) (1,1,1) (1,1,2) (1,1,3)
         *   (2,0,0) (2,0,1) (2,0,2) (2,0,3)
         *   (2,1,0) (2,1,1) (2,1,2) (2,1,3)
         */
        final int dimension = dimensions.length;
        final long[] size  = new long[dimension];
        final long[] lower = new long[dimension];
        final long[] upper = new long[dimension];
        final int [] sub   = new int [dimension];
        for (int i=0; i<dimension; i++) {
            final int j = (dimension - 1) - i;
            lower[i] = Integer.toUnsignedLong(areaLower[j]);
            upper[i] = Integer.toUnsignedLong(areaUpper[j]);
            sub  [i] = subsampling[j];
            size [i] = dimensions[j].length();
        }
        return Vector.create(reader.read(new Region(size, lower, upper, sub)), dataType.isUnsigned);
    }

    /**
     * Returns the error message for an unknown data type.
     */
    private String unknownType() {
        return Errors.format(Errors.Keys.UnknownType_1, "NetCDF:" + dataType);
    }
}
