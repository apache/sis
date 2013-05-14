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

import org.apache.sis.internal.netcdf.Variable;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.DataBuffer;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.VariableIF;
import org.apache.sis.util.CharSequences;


/**
 * A {@link Variable} backed by the UCAR NetCDF library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class VariableWrapper extends Variable {
    /**
     * The NetCDF variable.
     */
    private final VariableIF variable;

    /**
     * The list of all variables, used in order to determine
     * if this variable seems to be a coverage.
     */
    private final List<? extends VariableIF> all;

    /**
     * Creates a new variable wrapping the given NetCDF interface.
     */
    VariableWrapper(final VariableIF variable, List<? extends VariableIF> all) {
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
     * Returns the name of the variable data type as the name of the primitive type
     * followed by the span of each dimension (in unit of grid cells) between brackets.
     */
    @Override
    public String getDataTypeName() {
        final StringBuilder buffer = new StringBuilder(variable.getDataType().getPrimitiveClassType().getSimpleName());
        final int[] shape = variable.getShape();
        for (int i=shape.length; --i>=0;) {
            buffer.append('[').append(shape[i]).append(']');
        }
        return buffer.toString();
    }

    /**
     * Returns the {@link DataBuffer} constant which most closely represents
     * the "raw" internal data of the variable.
     *
     * {@note There is no converse of this method because the unsigned values type
     *        need to be handled in a special way (through a "_Unigned" attribute).}
     *
     * @param  variable The variable for which to get the {@link DataBuffer} constant, or {@code null}.
     * @return The data type, or {@link DataBuffer#TYPE_UNDEFINED} if unknown.
     */
    private static int getRawDataType(final VariableIF variable) {
        if (variable != null) {
            final DataType type = variable.getDataType();
            if (type != null) switch (type) {
                case BOOLEAN: // Fall through
                case BYTE:    return DataBuffer.TYPE_BYTE;
                case SHORT:   return variable.isUnsigned() ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_SHORT;
                case INT:     return DataBuffer.TYPE_INT;
                case FLOAT:   return DataBuffer.TYPE_FLOAT;
                case DOUBLE:  return DataBuffer.TYPE_DOUBLE;
            }
        }
        return DataBuffer.TYPE_UNDEFINED;
    }

    /**
     * Returns {@code true} if the given variable can be used for generating an image.
     */
    @Override
    public boolean isCoverage(final int minSpan) {
        int numVectors = 0; // Number of dimension having more than 1 value.
        for (final int length : variable.getShape()) {
            if (length >= minSpan) {
                numVectors++;
            }
        }
        if (numVectors >= MIN_DIMENSION && getRawDataType(variable) != DataBuffer.TYPE_UNDEFINED) {
            final String name = getName();
            for (final VariableIF var : all) {
                if (var != variable) {
                    Dimension dim;
                    for (int d=0; (dim=var.getDimension(d)) != null; d++) {
                        if (name.equals(dim.getShortName())) {
                            // This variable is a dimension of another variable.
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the names of the dimensions of this variable.
     *
     * @return The dimension names.
     */
    @Override
    public List<String> getDimensions() {
        final List<Dimension> dimensions = variable.getDimensions();
        final List<String> names = new ArrayList<>(dimensions.size());
        for (final Dimension dimension : dimensions) {
            names.add(dimension.getShortName());
        }
        return names;
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
                    String value = attribute.getStringValue(i);
                    if (value != null && !(value = value.trim()).isEmpty()) {
                        values[i] = value.replace('_', ' ');
                        hasValues = true;
                    }
                }
            }
            if (hasValues) {
                return values;
            }
        }
        return CharSequences.EMPTY_ARRAY;
    }
}
