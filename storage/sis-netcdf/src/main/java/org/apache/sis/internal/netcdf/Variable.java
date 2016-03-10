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
package org.apache.sis.internal.netcdf;

import java.io.IOException;
import java.awt.image.DataBuffer;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;


/**
 * A NetCDF variable created by {@link Decoder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public abstract class Variable {
    /**
     * Minimal number of dimension for accepting a variable as a coverage variable.
     */
    public static final int MIN_DIMENSION = 2;

    /**
     * The {@value} attribute name, used by {@link #isCoordinateSystemAxis()} implementations.
     * If this attribute is defined, then that name will be used as the variable name when
     * determining if the variable is a coordinate system axis.
     *
     * <p>This constants may be removed in any future SIS version if it is added to the
     * {@link ucar.nc2.constants._Coordinate} class.</p>
     */
    protected static final String _CoordinateVariableAlias = "_CoordinateVariableAlias";

    /**
     * Creates a new variable.
     */
    protected Variable() {
    }

    /**
     * Returns the name of this variable, or {@code null} if none.
     *
     * @return The name of this variable, or {@code null}.
     */
    public abstract String getName();

    /**
     * Returns the description of this variable, or {@code null} if none.
     *
     * @return The description of this variable, or {@code null}.
     */
    public abstract String getDescription();

    /**
     * Returns the unit of measurement as a string, or {@code null} if none.
     *
     * @return The unit of measurement, or {@code null}.
     */
    public abstract String getUnitsString();

    /**
     * Returns the variable data type, as a primitive type if possible.
     *
     * @return The variable data type, or {@code null} if unknown.
     */
    public abstract Class<?> getDataType();

    /**
     * Returns the name of the variable data type as the name of the primitive type
     * followed by the span of each dimension (in unit of grid cells) between brackets.
     * Example: {@code "short[180][360]"}.
     *
     * @return The name of the variable data type.
     */
    public final String getDataTypeName() {
        final StringBuilder buffer = new StringBuilder(20);
        if (isUnsigned()) {
            buffer.append("unsigned ");
        }
        buffer.append(Classes.getShortName(getDataType()));
        final int[] shape = getGridEnvelope();
        for (int i=shape.length; --i>=0;) {
            buffer.append('[').append(shape[i] & 0xFFFFFFFFL).append(']');
        }
        return buffer.toString();
    }

    /**
     * Returns the {@link DataBuffer} constant which most closely represents the "raw" internal data of the variable.
     * This is the value to be returned by {@link java.awt.image.SampleModel#getDataType()} for the Java2D rasters
     * created from this variable data.
     *
     * @return The Java2D data type, or {@link DataBuffer#TYPE_UNDEFINED} if this variable data type
     *         can not be mapped to a Java2D data type.
     */
    public final int getRasterDataType() {
        final Class<?> type = getDataType();
        if (type == boolean.class || type == byte.class) {
            return DataBuffer.TYPE_BYTE;
        }
        if (type == short .class) return isUnsigned() ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_SHORT;
        if (type == int   .class) return DataBuffer.TYPE_INT;
        if (type == float .class) return DataBuffer.TYPE_FLOAT;
        if (type == double.class) return DataBuffer.TYPE_DOUBLE;
        return DataBuffer.TYPE_UNDEFINED;
    }

    /**
     * Returns {@code true} if the integer values shall be considered as unsigned. The OGC NetCDF standard version 1.0
     * does not define unsigned data types. However some data providers attach an {@code "_Unsigned = true"} attribute
     * to the variable.
     *
     * @return {@code false} for signed data type (the default), or {@code true} for unsigned data type.
     */
    public abstract boolean isUnsigned();

    /**
     * Returns {@code true} if the given variable can be used for generating an image.
     * This method checks for the following conditions:
     *
     * <ul>
     *   <li>Images require at least {@value #MIN_DIMENSION} dimensions of size equals or greater
     *       than {@code minLength}. They may have more dimensions, in which case a slice will be
     *       taken later.</li>
     *   <li>Exclude axes. Axes are often already excluded by the above condition
     *       because axis are usually 1-dimensional, but some axes are 2-dimensional
     *       (e.g. a localization grid).</li>
     *   <li>Excludes characters, strings and structures, which can not be easily
     *       mapped to an image type. In addition, 2-dimensional character arrays
     *       are often used for annotations and we don't want to confuse them
     *       with images.</li>
     * </ul>
     *
     * @param  minSpan Minimal span (in unit of grid cells) along the dimensions.
     * @return {@code true} if the variable can be considered a coverage.
     */
    public final boolean isCoverage(final int minSpan) {
        int numVectors = 0;                                     // Number of dimension having more than 1 value.
        for (final int length : getGridEnvelope()) {
            if ((length & 0xFFFFFFFFL) >= minSpan) {
                numVectors++;
            }
        }
        if (numVectors >= MIN_DIMENSION && getRasterDataType() != DataBuffer.TYPE_UNDEFINED) {
            return !isCoordinateSystemAxis();
        }
        return false;
    }

    /**
     * Returns {@code true} if this variable seems to be a coordinate system axis instead than the actual data.
     * By NetCDF convention, coordinate system axes have the name of one of the dimensions defined in the NetCDF header.
     *
     * @return {@code true} if this variable seems to be a coordinate system axis.
     */
    public abstract boolean isCoordinateSystemAxis();

    /**
     * Returns the names of the dimensions of this variable, in the order they are declared in the NetCDF file.
     * The dimensions are those of the grid, not the dimensions of the coordinate system.
     *
     * @return The names of all dimension of the grid, in NetCDF order (reverse of "natural" order).
     */
    public abstract String[] getGridDimensionNames();

    /**
     * Returns the length (number of cells) of each grid dimension, in the order they are declared in the NetCDF file.
     * The length of this array shall be equals to the length of the {@link #getGridDimensionNames()} array.
     *
     * <p>In ISO 19123 terminology, this method returns the upper corner of the grid envelope plus one.
     * The lower corner is always (0,0,…,0).</p>
     *
     * @return The number of grid cells for each dimension, in NetCDF order (reverse of "natural" order).
     */
    public abstract int[] getGridEnvelope();

    /**
     * Returns the sequence of values for the given attribute, or an empty array if none.
     * The elements will be of class {@link String} if {@code numeric} is {@code false},
     * or {@link Number} if {@code numeric} is {@code true}.
     *
     * @param  attributeName The name of the attribute for which to get the values.
     * @param  numeric {@code true} if the values are expected to be numeric, or {@code false} for strings.
     * @return The sequence of {@link String} or {@link Number} values for the named attribute.
     */
    public abstract Object[] getAttributeValues(String attributeName, boolean numeric);

    /**
     * Reads all the data for this variable and returns them as an array of a Java primitive type.
     *
     * @return The data as an array of a Java primitive type.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     */
    public abstract Object read() throws IOException, DataStoreException;

    /**
     * Reads a sub-sampled sub-area of the variable.
     * Constraints on the argument values are:
     *
     * <ul>
     *   <li>All arrays length shall be equal to the length of the {@link #getGridEnvelope()} array.</li>
     *   <li>For each index <var>i</var>, value of {@code area[i]} shall be in the range from 0 inclusive
     *       to {@code Integer.toUnsignedLong(getGridEnvelope()[i])} exclusive.</li>
     * </ul>
     *
     * @param  areaLower   Index of the first value to read along each dimension.
     * @param  areaUpper   Index after the last value to read along each dimension.
     * @param  subsampling Sub-sampling along each dimension. 1 means no sub-sampling.
     * @return The data as an array of a Java primitive type.
     * @throws IOException if an error occurred while reading the data.
     * @throws DataStoreException if a logical error occurred.
     */
    public abstract Object read(int[] areaLower, int[] areaUpper, int[] subsampling) throws IOException, DataStoreException;

    /**
     * Returns a string representation of this variable for debugging purpose.
     *
     * @return A string representation of this variable.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(getName())
                .append(" : ").append(Classes.getShortName(getDataType()));
        final int[] shape = getGridEnvelope();
        for (int i=shape.length; --i>=0;) {
            buffer.append('[').append(shape[i] & 0xFFFFFFFFL).append(']');
        }
        return buffer.toString();
    }
}
