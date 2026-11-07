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
import java.util.Locale;
import java.util.Objects;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Change of the case of a string, to upper case or lower case.
 * The value is converted to a string before changing its case. A null value gives null.
 * The case conversion is locale-independent.
 *
 * <p>This function is available by the name {@code "ChangeCase"} in Apache SIS filter factories,
 * with the string as parameter optionally followed by the {@code "toUpper"} or {@code "toLower"} keyword.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class ChangeCase<R> extends SymbologyFunction<R,String> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7802416069587108413L;

    /**
     * The name of this function.
     */
    static final String NAME = "ChangeCase";

    /**
     * The direction of the case change.
     */
    public enum Direction {
        /**
         * Conversion to upper case. This is the default.
         */
        TO_UPPER,

        /**
         * Conversion to lower case.
         */
        TO_LOWER
    }

    /**
     * The string to convert.
     */
    private final Expression<R,?> stringValue;

    /**
     * The direction of the case change.
     */
    private final Direction direction;

    /**
     * Creates a new case change function.
     *
     * @param  stringValue  the string to convert.
     * @param  direction    the direction of the case change, or {@code null} for the default ({@code TO_UPPER}).
     */
    public ChangeCase(final Expression<R,?> stringValue, final Direction direction) {
        super(NAME, String.class);
        this.stringValue = Objects.requireNonNull(stringValue, "stringValue");
        this.direction   = (direction != null) ? direction : Direction.TO_UPPER;
    }

    /**
     * Returns the string to convert.
     *
     * @return the string value.
     */
    public Expression<R,?> getStringValue() {
        return stringValue;
    }

    /**
     * Returns the direction of the case change.
     *
     * @return the direction.
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns the parameters of this function: the string value followed by the direction keyword.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        return List.of(stringValue, new Keyword<>(keyword(direction)));
    }

    /**
     * Changes the case of the string value evaluated on the given resource.
     *
     * @param  input  the resource on which to evaluate the expressions.
     * @return the converted string, or {@code null} if the string value is null.
     */
    @Override
    public String apply(final R input) {
        final String text = string(stringValue.apply(input));
        if (text == null) {
            return null;
        }
        return (direction == Direction.TO_LOWER) ? text.toLowerCase(Locale.ROOT) : text.toUpperCase(Locale.ROOT);
    }

    @Override
    Object[] properties() {
        return new Object[] {stringValue, direction};
    }
}
