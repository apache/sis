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
 * Number of characters in a string.
 * The value is converted to a string before computing its length, in Unicode code points.
 * A null value gives null.
 *
 * <p>This function is available by the name {@code "StringLength"} in Apache SIS filter factories,
 * with the string as parameter.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class StringLength<R> extends SymbologyFunction<R,Integer> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6305066102478541706L;

    /**
     * The name of this function.
     */
    static final String NAME = "StringLength";

    /**
     * The string to measure.
     */
    private final Expression<R,?> stringValue;

    /**
     * Creates a new string length function.
     *
     * @param  stringValue  the string to measure.
     */
    public StringLength(final Expression<R,?> stringValue) {
        super(NAME, Integer.class);
        this.stringValue = Objects.requireNonNull(stringValue, "stringValue");
    }

    /**
     * Returns the string to measure.
     *
     * @return the string value.
     */
    public Expression<R,?> getStringValue() {
        return stringValue;
    }

    /**
     * Returns the parameters of this function, which is the string value.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        return List.of(stringValue);
    }

    /**
     * Computes the number of characters in the string value evaluated on the given resource.
     *
     * @param  input  the resource on which to evaluate the expressions.
     * @return the number of characters, or {@code null} if the string value is null.
     */
    @Override
    public Integer apply(final R input) {
        final String text = string(stringValue.apply(input));
        return (text != null) ? text.codePointCount(0, text.length()) : null;
    }

    @Override
    Object[] properties() {
        return new Object[] {stringValue};
    }
}
