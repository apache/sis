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


/**
 * Description of a variable found in a NetCDF file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class VariableInfo {
    /**
     * The type of data. Number of bits and endianness are same as in the Java language
     * except {@code CHAR}, which is defined as an unsigned 8-bits value.
     */
    static final int BYTE=1, CHAR=2, SHORT=3, INT=4, FLOAT=5, DOUBLE=6;

    /**
     * The size in bytes of the above constants.
     */
    private static final int[] SIZES = new int[] {
        Byte   .SIZE / Byte.SIZE,
        Byte   .SIZE / Byte.SIZE, // NOT Java char
        Short  .SIZE / Byte.SIZE,
        Integer.SIZE / Byte.SIZE,
        Float  .SIZE / Byte.SIZE,
        Double .SIZE / Byte.SIZE,
    };

    /**
     * The variable name.
     */
    final String name;

    /**
     * The dimensions of that variable.
     */
    final Dimension[] dimensions;

    /**
     * The attributes associates to the variable, or {@code null} if none.
     */
    final Attribute[] attributes;

    /**
     * The type of data, as one of the {@code BYTE}, {@code SHORT} and similar constants defined
     * in {@link ChannelDecoder}.
     */
    final int datatype;

    /**
     * The offset where the variable data begins in the NetCDF file.
     */
    final long offset;

    /**
     * Creates a new variable.
     */
    VariableInfo(final String name, final Dimension[] dimensions, final Attribute[] attributes,
            final int datatype, final int size, final long offset)
    {
        this.name       = name;
        this.dimensions = dimensions;
        this.attributes = attributes;
        this.datatype   = datatype;
        this.offset     = offset;
        // TODO: verify 'size'.
    }

    /**
     * Returns the size of the given data type, or 0 if unknown.
     */
    static int sizeOf(int datatype) {
        return (--datatype >= 0 && datatype < SIZES.length) ? SIZES[datatype] : 0;
    }
}
