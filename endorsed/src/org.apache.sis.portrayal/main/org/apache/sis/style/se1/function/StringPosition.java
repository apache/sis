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
package org.apache.sis.style.se1.function;

import java.util.List;
import java.util.Objects;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Position of a string in another string.
 * Returns the position of the first occurrence (counting from 1) of the lookup string in the string value,
 * or zero if the lookup string is not found. The search can be done from the front to the back (the default)
 * or from the back to the front, in which case the last occurrence is found. Positions are counted in
 * Unicode code points. Values are converted to strings before the search, and null values give zero.
 *
 * <p>This function is available by the name {@code "StringPosition"} in Apache SIS filter factories,
 * with the lookup string and the string value as parameters, optionally followed by the {@code "frontToBack"}
 * or {@code "backToFront"} keyword.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class StringPosition<R> extends SymbologyFunction<R,Integer> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4467690426395216094L;

    /**
     * The name of this function.
     */
    static final String NAME = "StringPosition";

    /**
     * The direction of the search.
     */
    public enum SearchDirection {
        /**
         * Search from the beginning of the string. This is the default.
         */
        FRONT_TO_BACK,

        /**
         * Search from the end of the string.
         */
        BACK_TO_FRONT
    }

    /**
     * The string to search.
     */
    private final Expression<R,?> lookupString;

    /**
     * The string in which to search.
     */
    private final Expression<R,?> stringValue;

    /**
     * The direction of the search.
     */
    private final SearchDirection searchDirection;

    /**
     * Creates a new string position function.
     *
     * @param  lookupString     the string to search.
     * @param  stringValue      the string in which to search.
     * @param  searchDirection  the direction of the search, or {@code null} for the default ({@code FRONT_TO_BACK}).
     */
    public StringPosition(final Expression<R,?> lookupString, final Expression<R,?> stringValue,
                          final SearchDirection searchDirection)
    {
        super(NAME, Integer.class);
        this.lookupString    = Objects.requireNonNull(lookupString, "lookupString");
        this.stringValue     = Objects.requireNonNull(stringValue,  "stringValue");
        this.searchDirection = (searchDirection != null) ? searchDirection : SearchDirection.FRONT_TO_BACK;
    }

    /**
     * Returns the string to search.
     *
     * @return the lookup string.
     */
    public Expression<R,?> getLookupString() {
        return lookupString;
    }

    /**
     * Returns the string in which to search.
     *
     * @return the string value.
     */
    public Expression<R,?> getStringValue() {
        return stringValue;
    }

    /**
     * Returns the direction of the search.
     *
     * @return the search direction.
     */
    public SearchDirection getSearchDirection() {
        return searchDirection;
    }

    /**
     * Returns the parameters of this function: the lookup string, the string value and the direction keyword.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        return List.of(lookupString, stringValue, new Keyword<>(keyword(searchDirection)));
    }

    /**
     * Searches the lookup string in the string value, both evaluated on the given resource.
     *
     * @param  input  the resource on which to evaluate the expressions.
     * @return the position counting from 1, or 0 if not found.
     */
    @Override
    public Integer apply(final R input) {
        final String lookup = string(lookupString.apply(input));
        final String text   = string(stringValue.apply(input));
        if (lookup == null || text == null) {
            return 0;
        }
        final int index = (searchDirection == SearchDirection.BACK_TO_FRONT) ? text.lastIndexOf(lookup) : text.indexOf(lookup);
        return (index < 0) ? 0 : text.codePointCount(0, index) + 1;
    }

    @Override
    Object[] properties() {
        return new Object[] {lookupString, stringValue, searchDirection};
    }
}
