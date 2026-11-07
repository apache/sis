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

import java.util.Optional;
import java.util.function.Function;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Base class of the Symbology Encoding functions which transform a lookup value,
 * usually a property of a feature or the raster data, into another value such as a color.
 * Those functions are {@link Categorize}, {@link Interpolate} and {@link Recode}.
 *
 * <p>Symbology Encoding defines the fallback value as the value to use by implementations
 * which do not support the function. Since this implementation supports the functions,
 * the fallback value is used instead for lookup values which are missing or invalid.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param <V>  the type of values computed by the function.
 */
abstract class AbstractLookupFunction<R,V> extends SymbologyFunction<R,V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6624478103927283436L;

    /**
     * The expression computing the value to transform.
     */
    final Expression<R,?> lookupValue;

    /**
     * Value to use when the lookup value cannot be transformed, or {@code null} if none.
     */
    final Expression<R, ? extends V> fallbackValue;

    /**
     * Creates a new function.
     *
     * @param  name           the function name, without scope.
     * @param  resultType     the type of values computed by this function.
     * @param  lookupValue    the value to transform, or {@code null} for {@code "Rasterdata"}.
     * @param  fallbackValue  value to use when the lookup value cannot be transformed, or {@code null} if none.
     */
    AbstractLookupFunction(final String name, final Class<?> resultType,
                   final Expression<R,?> lookupValue, final Expression<R, ? extends V> fallbackValue)
    {
        super(name, resultType);
        this.lookupValue   = (lookupValue != null) ? lookupValue : new Keyword<>("Rasterdata");
        this.fallbackValue = fallbackValue;
    }

    /**
     * Returns the expression computing the value to transform.
     * The default value is the literal {@code "Rasterdata"}, which designates the raster data in a color map.
     *
     * @return the value to transform.
     */
    public final Expression<R,?> getLookupValue() {
        return lookupValue;
    }

    /**
     * Returns the value to use when the lookup value is missing or invalid.
     *
     * @return the fallback value.
     */
    public final Optional<Expression<R, ? extends V>> getFallbackValue() {
        return Optional.ofNullable(fallbackValue);
    }

    /**
     * Transforms the lookup value evaluated on the given resource.
     *
     * @param  input  the resource on which to evaluate the expressions.
     * @return the transformed value, or the fallback value (possibly null).
     */
    @Override
    public final V apply(final R input) {
        return bind(input).apply(lookupValue.apply(input));
    }

    /**
     * Returns a function transforming arbitrary lookup values, with all other parameters evaluated
     * once on the given resource. This is useful for transforming many values in the same context,
     * for example all pixels of a raster.
     *
     * @param  input  the resource on which to evaluate the parameters, may be null for literals.
     * @return a function transforming lookup values.
     */
    public abstract Function<Object,V> bind(R input);

    /**
     * Evaluates the fallback value on the given resource.
     */
    final V fallback(final R input) {
        return (fallbackValue != null) ? fallbackValue.apply(input) : null;
    }
}
