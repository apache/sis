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
import org.opengis.filter.Expression;


/**
 * Concatenation of strings.
 * Each value is converted to a string before concatenation. Null values are ignored.
 *
 * <p>This function is available by the name {@code "Concatenate"} in Apache SIS filter factories,
 * with the strings to concatenate as parameters.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class Concatenate<R> extends SymbologyFunction<R,String> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2066413766612577339L;

    /**
     * The name of this function.
     */
    static final String NAME = "Concatenate";

    /**
     * The values to concatenate.
     */
    private final List<Expression<R,?>> stringValues;

    /**
     * Creates a new concatenation function.
     *
     * @param  stringValues  the values to concatenate, at least one.
     * @throws IllegalArgumentException if the list is empty.
     */
    public Concatenate(final List<? extends Expression<R,?>> stringValues) {
        super(NAME, String.class);
        this.stringValues = List.copyOf(stringValues);
        if (this.stringValues.isEmpty()) {
            throw new IllegalArgumentException("Concatenate requires at least one value.");
        }
    }

    /**
     * Returns the values to concatenate.
     *
     * @return the values to concatenate.
     */
    public List<Expression<R,?>> getStringValues() {
        return stringValues;
    }

    /**
     * Returns the parameters of this function, which are the values to concatenate.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        return stringValues;
    }

    /**
     * Concatenates the values evaluated on the given resource.
     *
     * @param  input  the resource on which to evaluate the expressions.
     * @return the concatenated string, never null.
     */
    @Override
    public String apply(final R input) {
        final StringBuilder buffer = new StringBuilder();
        for (Expression<R,?> e : stringValues) {
            final Object value = e.apply(input);
            if (value != null) buffer.append(value);
        }
        return buffer.toString();
    }

    @Override
    Object[] properties() {
        return new Object[] {stringValues};
    }
}
