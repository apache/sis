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
import ucar.nc2.constants.CF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.storage.ChannelDataInput;
import org.apache.sis.internal.storage.HyperRectangleReader;
import org.apache.sis.internal.storage.Region;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Numbers;


/**
 * Description of a variable found in a NetCDF file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
final class VariableInfo extends Variable {
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
     * The NetCDF type of data. Number of bits and endianness are same as in the Java language except {@code CHAR},
     * which is defined as an unsigned 8-bits value.
     */
    static final int BYTE=1, CHAR=2, SHORT=3, INT=4, FLOAT=5, DOUBLE=6;

    /**
     * Mapping from the NetCDF data type to the enumeration used by our {@link Numbers} class.
     */
    private static final byte[] NUMBER_TYPES = new byte[] {
        Numbers.BYTE,
        Numbers.BYTE,       // NOT Numbers.CHARACTER
        Numbers.SHORT,
        Numbers.INTEGER,
        Numbers.FLOAT,
        Numbers.DOUBLE,
    };

    /**
     * The size in bytes of the above constants.
     *
     * @see #sizeOf(int)
     */
    private static final byte[] SIZES = new byte[] {
        Byte   .SIZE / Byte.SIZE,
        Byte   .SIZE / Byte.SIZE,      // NOT Character.BYTES
        Short  .SIZE / Byte.SIZE,
        Integer.SIZE / Byte.SIZE,
        Float  .SIZE / Byte.SIZE,
        Double .SIZE / Byte.SIZE,
    };

    /**
     * The Java primitive type of the above constants.
     *
     * @see #getDataType()
     */
    private static final Class<?>[] TYPES = new Class<?>[] {
       byte  .class,
       char  .class,
       short .class,
       int   .class,
       float .class,
       double.class
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
     * The dimensions of this variable.
     */
    final Dimension[] dimensions;

    /**
     * All dimensions in the NetCDF files.
     */
    private final Dimension[] allDimensions;

    /**
     * The attributes associates to the variable, or an empty map if none.
     */
    private final Map<String,Attribute> attributes;

    /**
     * The type of data, as one of the {@code BYTE}, {@code SHORT} and similar constants defined in this class.
     */
    private final int dataType;

    /**
     * The grid geometry associated to this variable,
     * computed by {@link ChannelDecoder#getGridGeometries()} when first needed.
     */
    GridGeometryInfo gridGeometry;

    /**
     * Creates a new variable.
     *
     * @param input         The channel together with a buffer for reading the variable data.
     * @param name          The variable name.
     * @param dimensions    The dimensions of this variable.
     * @param allDimensions All dimensions in the NetCDF files.
     * @param attributes    The attributes associates to the variable, or an empty map if none.
     * @param dataType      The type of data, as one of the {@code BYTE} and similar constants defined in this class.
     * @param size          The variable size, used for verification purpose only.
     * @param offset        The offset where the variable data begins in the NetCDF file.
     */
    VariableInfo(final ChannelDataInput input, final String name,
            final Dimension[] dimensions, final Dimension[] allDimensions,
            final Map<String,Attribute> attributes, int dataType, final int size, final long offset)
            throws DataStoreException
    {
        this.name          = name;
        this.dimensions    = dimensions;
        this.allDimensions = allDimensions;
        this.attributes    = attributes;
        this.dataType      = dataType;
        /*
         * The 'size' value is provided in the NetCDF files, but doesn't need to be stored since it
         * is redundant with the dimension lengths and is not large enough for big variables anyway.
         */
        if (--dataType >= 0 && dataType < NUMBER_TYPES.length) {
            reader = new HyperRectangleReader(NUMBER_TYPES[dataType], input, offset);
        } else {
            reader = null;
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
            final Attribute attribute = attributes.get(attributeName);
            if (attribute != null && attribute.value instanceof String) {
                return (String) attribute.value;
            }
        }
        return null;
    }

    /**
     * Returns the unit of measurement as a string, or {@code null} if none.
     */
    @Override
    public String getUnitsString() {
        final Attribute attribute = attributes.get(CDM.UNITS);
        if (attribute != null && attribute.value instanceof String) {
            return (String) attribute.value;
        }
        return null;
    }

    /**
     * Returns the type of data as a Java primitive type if possible,
     * or {@code null} if the data type is unknown to this method.
     */
    @Override
    public Class<?> getDataType() {
        final int i = dataType - 1;
        return (i >= 0 && i < TYPES.length) ? TYPES[i] : null;
    }

    /**
     * Returns the size of the given data type, or 0 if unknown.
     */
    static int sizeOf(int datatype) {
        return (--datatype >= 0 && datatype < SIZES.length) ? SIZES[datatype] : 0;
    }

    /**
     * Returns {@code true} if the integer values shall be considered as unsigned.
     * Current implementation searches for an {@code "_Unsigned = true"} attribute.
     */
    @Override
    public boolean isUnsigned() {
        final Attribute attribute = attributes.get(CDM.UNSIGNED);
        return (attribute != null) && attribute.booleanValue();
    }

    /**
     * Returns {@code true} if this variable seems to be a coordinate system axis,
     * determined by comparing its name with the name of all dimensions in the NetCDF file.
     */
    @Override
    public boolean isCoordinateSystemAxis() {
        String name = this.name;
        final Attribute attribute = attributes.get(_CoordinateVariableAlias);
        if (attribute != null && attribute.value instanceof String) {
            name = (String) attribute.value;
        }
        for (final Dimension dimension : allDimensions) {
            if (name.equals(dimension.name)) {
                // This variable is a dimension of another variable.
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value of the {@code "_CoordinateAxisType"} attribute, or {@code null} if none.
     */
    final String getAxisType() {
        final Attribute attribute = attributes.get(_Coordinate.AxisType);
        if (attribute != null && attribute.value instanceof String) {
            return (String) attribute.value;
        }
        return null;
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
     * @return The number of grid cells for each dimension, as unsigned integers.
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
     * Returns the sequence of values for the given attribute, or an empty array if none.
     * The elements will be of class {@link String} if {@code numeric} is {@code false},
     * or {@link Number} if {@code numeric} is {@code true}.
     */
    @Override
    public Object[] getAttributeValues(final String attributeName, final boolean numeric) {
        Attribute attribute = attributes.get(attributeName);
        if (attribute != null) {
            return numeric ? attribute.numberValues() : attribute.stringValues();
        }
        return new Object[0];
    }

    /**
     * Reads all the data for this variable and returns them as an array of a Java primitive type.
     */
    @Override
    public Object read() throws IOException, DataStoreException {
        if (reader == null) {
            throw new DataStoreException(unknownType());
        }
        long length = 1;
        for (final Dimension dimension : dimensions) {
            length *= dimension.length;
        }
        if (length > Integer.MAX_VALUE) {
            throw new DataStoreException(Errors.format(Errors.Keys.ExcessiveListSize_2, name, length));
        }
        final int dimension = dimensions.length;
        final long[] size  = new long[dimension];
        final int [] sub   = new int [dimension];
        for (int i=0; i<dimension; i++) {
            sub [i] = 1;
            size[i] = dimensions[(dimension - 1) - i].length();
        }
        return reader.read(new Region(size, new long[dimension], size, sub));
    }

    /**
     * Reads a sub-sampled sub-area of the variable.
     *
     * @param  areaLower   Index of the first value to read along each dimension, as unsigned integers.
     * @param  areaUpper   Index after the last value to read along each dimension, as unsigned integers.
     * @param  subsampling Sub-sampling along each dimension. 1 means no sub-sampling.
     * @return The data as an array of a Java primitive type.
     */
    @Override
    public Object read(int[] areaLower, int[] areaUpper, int[] subsampling) throws IOException, DataStoreException {
        if (reader == null) {
            throw new DataStoreException(unknownType());
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
            lower[i] = areaLower[j] & 0xFFFFFFFFL;
            upper[i] = areaUpper[j] & 0xFFFFFFFFL;
            sub  [i] = subsampling[j];
            size [i] = dimensions[j].length();
        }
        return reader.read(new Region(size, lower, upper, sub));
    }

    /**
     * Returns the error message for an unknown data type.
     */
    private String unknownType() {
        return Errors.format(Errors.Keys.UnknownType_1, "NetCDF:" + dataType);
    }
}
