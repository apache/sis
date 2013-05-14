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

import java.util.List;


/**
 * A NetCDF variable created by {@link Decoder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class Variable {
    /**
     * Minimal number of dimension for accepting a variable as a coverage variable.
     */
    public static final int MIN_DIMENSION = 2;

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
     * Returns the name of the variable data type as the name of the primitive type
     * followed by the span of each dimension (in unit of grid cells) between brackets.
     *
     * @return The name of the variable data type.
     */
    public abstract String getDataTypeName();

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
    public abstract boolean isCoverage(int minSpan);

    /**
     * Returns the names of the dimensions of this variable.
     *
     * @return The dimension names.
     */
    public abstract List<String> getDimensions();

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
}
