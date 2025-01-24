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
package org.apache.sis.storage.netcdf.base;

import org.apache.sis.util.resources.Vocabulary;


/**
 * A dimension in a netCDF file. A dimension can be seen as an axis in the grid space
 * (not the geodetic space). Dimension are referenced by their index in other parts of
 * the netCDF file header.
 *
 * <p>{@code Dimension} instances shall be suitable for use in {@link java.util.HashSet}
 * and {@code Dimension.equals(object)} must return {@code true} if two {@code Dimension}
 * instances represent the same netCDF dimensions. This may require subclasses to override
 * {@link #hashCode()} and {@link #equals(Object)} if uniqueness is not guaranteed.
 * This is needed by {@link Variable#findGrid(GridAdjustment)} default implementation.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class Dimension extends NamedElement {
    /**
     * Creates a new dimension.
     */
    protected Dimension() {
    }

    /**
     * Returns the name of this netCDF dimension.
     * Note that dimensions in HDF5 files may be unnamed.
     *
     * @return the name of this netCDF dimension, or {@code null} if unnamed.
     */
    @Override
    public abstract String getName();

    /**
     * Returns the number of grid cell values along this dimension, or a negative number if undetermined.
     * The length may be undetermined if this dimension {@linkplain #isUnlimited() is unlimited}.
     *
     * @return number of grid cell values.
     */
    public abstract long length();

    /**
     * Returns whether this dimension can grow.
     * In netCDF 3 classic format, only the first dimension can be unlimited.
     *
     * @return whether this dimension can grow.
     *
     * @see Variable#isUnlimited()
     */
    protected abstract boolean isUnlimited();

    /**
     * Returns a dimension with its index decremented by 1. This method is invoked for trailing dimensions
     * after a previous dimension has been removed from a list. This is useful only for subclasses that
     * need to know the index of this dimension in a list of dimensions.
     *
     * <p>Note: this method may be removed in a future version if we do not need to store index anymore.
     * See <a href="https://github.com/Unidata/netcdf-java/issues/951">Issue #951 on netcdf-java</a>.</p>
     *
     * @return a dimension equals to this one but with its list index (if any) decremented.
     */
    protected Dimension decrementIndex() {
        return this;
    }

    /**
     * Writes in the given buffer the length of this dimension between bracket.
     * The length may be unknown (represented by {@code '?'}).
     */
    final void writeLength(final StringBuilder buffer) {
        final long length = length();
        buffer.append('[');
        if (length > 0) {
            buffer.append(length);
        } else {
            buffer.append('?');
        }
        buffer.append(']');
    }

    /**
     * A string representation of this dimension for debugging purpose only.
     *
     * @return a string representation of this dimension.
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder(30);
        String name = getName();
        if (name != null) {
            buffer.append(name);
        } else {
            buffer.append('(').append(Vocabulary.format(Vocabulary.Keys.Unnamed)).append(')');
        }
        writeLength(buffer);
        if (isUnlimited()) {
            buffer.append(" (unlimited)");
        }
        return buffer.toString();
    }
}
