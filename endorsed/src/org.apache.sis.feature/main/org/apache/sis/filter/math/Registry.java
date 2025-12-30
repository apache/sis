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
package org.apache.sis.filter.math;

import java.util.Set;
import org.apache.sis.filter.FunctionRegister;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.ViewAsSet;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.capability.AvailableFunction;


/**
 * A register of mathematical functions.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Registry implements FunctionRegister {
    /**
     * Creates a new registry.
     *
     * @todo Replace by a static {@code provider()} method after we abandon classpath support.
     */
    public Registry() {
    }

    /**
     * Returns the name of body defining the functions.
     * Since we have no standard to refer to, we use "SIS" for now.
     */
    @Override
    public String getAuthority() {
        return Constants.SIS;
    }

    /**
     * Returns the names of all functions that this factory can create.
     */
    @Override
    public Set<String> getNames() {
        return new ViewAsSet<>(Function.namesAndAliases());
    }

    /**
     * Describes the parameters of a function.
     *
     * @param  name  case-sensitive name of the function to describe.
     * @return description of the function parameters.
     * @throws IllegalArgumentException if the given function name is unknown.
     */
    @Override
    public AvailableFunction describe(String name) {
        return Function.of(name);
    }

    /**
     * Creates a new function of the given name with given parameters.
     *
     * @param  <R>         the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
     * @param  name        case-sensitive name of the function to create as an expression.
     * @param  parameters  function parameters.
     * @return function for the given name and parameters.
     * @throws IllegalArgumentException if function name is unknown or some parameters are illegal.
     */
    @Override
    public <R> Expression<R,?> create(final String name, final Expression<R,?>[] parameters) {
        final Function function = Function.of(name);
        ArgumentChecks.ensureCountBetween("parameters", false,
                                          function.getMinParameterCount(),
                                          function.getMaxParameterCount(),
                                          parameters.length);

        final Expression<R, Number> p0 = parameters[0].toValueType(Number.class);
        if (parameters.length == 1) {
            if (function.filter != null) {
                return new Predicate<>(function, p0);
            } else {
                return new UnaryOperator<>(function, p0);
            }
        }
        return new BinaryOperator<>(function, p0, parameters[1].toValueType(Number.class));
    }
}
