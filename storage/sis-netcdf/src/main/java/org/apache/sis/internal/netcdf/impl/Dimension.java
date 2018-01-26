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

import org.apache.sis.internal.netcdf.NamedElement;
import org.apache.sis.util.Debug;


/**
 * A dimension in a netCDF file. A dimension can been seen as an axis in the grid space
 * (not the geodetic space). Dimension are referenced by their index in other parts of
 * the netCDF file header.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
final class Dimension extends NamedElement {
    /**
     * The dimension name.
     */
    final String name;

    /**
     * The number of grid cell value along this dimension, as an unsigned number.
     */
    final int length;

    /**
     * Creates a new dimension of the given name and length.
     *
     * @param name    the dimension name.
     * @param length  the number of grid cell value along this dimension, as an unsigned number.
     */
    Dimension(final String name, final int length) {
        this.name   = name;
        this.length = length;
    }

    /**
     * Returns the name of this netCDF dimension.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the number of grid cell value along this dimension.
     */
    final long length() {
        return Integer.toUnsignedLong(length);
    }

    /**
     * A string representation of this dimension for debugging purpose only.
     */
    @Debug
    @Override
    public String toString() {
        return name + '[' + length() + ']';
    }
}
