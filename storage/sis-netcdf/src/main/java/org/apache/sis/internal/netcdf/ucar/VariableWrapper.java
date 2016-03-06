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
package org.apache.sis.internal.netcdf.ucar;

import java.util.List;
import java.io.IOException;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.VariableIF;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.storage.DataStoreException;


/**
 * A {@link Variable} backed by the UCAR NetCDF library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
final class VariableWrapper extends Variable {
    /**
     * The NetCDF variable.
     */
    private final VariableIF variable;

    /**
     * The list of all dimensions in the NetCDF file.
     */
    private final List<? extends Dimension> all;

    /**
     * Creates a new variable wrapping the given NetCDF interface.
     */
    VariableWrapper(final VariableIF variable, List<? extends Dimension> all) {
        this.variable = variable;
        this.all = all;
    }

    /**
     * Returns the name of this variable, or {@code null} if none.
     */
    @Override
    public String getName() {
        return variable.getShortName();
    }

    /**
     * Returns the description of this variable, or {@code null} if none.
     */
    @Override
    public String getDescription() {
        return variable.getDescription();
    }

    /**
     * Returns the unit of measurement as a string, or {@code null} if none.
     */
    @Override
    public String getUnitsString() {
        return variable.getUnitsString();
    }

    /**
     * Returns the variable data type, as a primitive type if possible.
     * This method may return {@code null} (UCAR code seems to allow that).
     */
    @Override
    public Class<?> getDataType() {
        return variable.getDataType().getPrimitiveClassType();
    }

    /**
     * Returns {@code true} if the integer values shall be considered as unsigned.
     */
    @Override
    public boolean isUnsigned() {
        return variable.isUnsigned();
    }

    /**
     * Returns {@code true} if this variable seems to be a coordinate system axis,
     * determined by comparing its name with the name of all dimensions in the NetCDF file.
     */
    @Override
    public boolean isCoordinateSystemAxis() {
        String name = null;
        final Attribute attribute = variable.findAttributeIgnoreCase(_CoordinateVariableAlias);
        if (attribute != null) {
            name = attribute.getStringValue();
        }
        if (name == null) {
            name = getName();
        }
        for (final Dimension dimension : all) {
            if (name.equals(dimension.getShortName())) {
                // This variable is a dimension of another variable.
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the names of the dimensions of this variable.
     * The dimensions are those of the grid, not the dimensions of the coordinate system.
     */
    @Override
    public String[] getGridDimensionNames() {
        final List<Dimension> dimensions = variable.getDimensions();
        final String[] names = new String[dimensions.size()];
        for (int i=0; i<names.length; i++) {
            names[i] = dimensions.get(i).getShortName();
        }
        return names;
    }

    /**
     * Returns the length (number of cells) of each grid dimension. In ISO 19123 terminology, this method
     * returns the upper corner of the grid envelope plus one. The lower corner is always (0,0,…,0).
     */
    @Override
    public int[] getGridEnvelope() {
        return variable.getShape();
    }

    /**
     * Returns the sequence of values for the given attribute, or an empty array if none.
     * The elements will be of class {@link String} if {@code numeric} is {@code false},
     * or {@link Number} if {@code numeric} is {@code true}.
     */
    @Override
    public Object[] getAttributeValues(final String attributeName, final boolean numeric) {
        final Attribute attribute = variable.findAttributeIgnoreCase(attributeName);
        if (attribute != null) {
            boolean hasValues = false;
            final Object[] values = new Object[attribute.getLength()];
            for (int i=0; i<values.length; i++) {
                if (numeric) {
                    if ((values[i] = attribute.getNumericValue(i)) != null) {
                        hasValues = true;
                    }
                } else {
                    Object value = attribute.getValue(i);
                    if (value != null) {
                        String text = value.toString().trim();
                        if (!text.isEmpty()) {
                            values[i] = text;
                            hasValues = true;
                        }
                    }
                }
            }
            if (hasValues) {
                return values;
            }
        }
        return new Object[0];
    }

    /**
     * Reads all the data for this variable and returns them as an array of a Java primitive type.
     */
    @Override
    public Object read() throws IOException {
        final Array array = variable.read();
        return array.get1DJavaArray(array.getElementType());
    }

    /**
     * Reads a sub-sampled sub-area of the variable.
     *
     * @param  areaLower   Index of the first value to read along each dimension.
     * @param  areaUpper   Index after the last value to read along each dimension.
     * @param  subsampling Sub-sampling along each dimension. 1 means no sub-sampling.
     * @return The data as an array of a Java primitive type.
     */
    @Override
    public Object read(final int[] areaLower, final int[] areaUpper, final int[] subsampling)
            throws IOException, DataStoreException
    {
        final int[] size = new int[areaUpper.length];
        for (int i=0; i<size.length; i++) {
            size[i] = areaUpper[i] - areaLower[i];
        }
        final Array array;
        try {
            array = variable.read(new Section(areaLower, size, subsampling));
        } catch (InvalidRangeException e) {
            throw new DataStoreException(e);
        }
        return array.get1DJavaArray(array.getElementType());
    }
}
