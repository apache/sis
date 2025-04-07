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
package org.apache.sis.filter.privy;

import java.util.Optional;
import java.util.function.Consumer;
import org.opengis.util.ScopedName;
import org.apache.sis.filter.internal.Node;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.CodeList;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;


/**
 * A warning emitted during operations on filters or expressions.
 * This class is a first draft that may move to public API in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-460">SIS-460</a>
 */
public final class WarningEvent {
    /**
     * Where to send the warning. If the value is {@code null},
     * then the warning will be logged to a default logger.
     */
    public static final ThreadLocal<Consumer<WarningEvent>> LISTENER = new ThreadLocal<>();

    /**
     * The filter or expression that produced this warning.
     */
    private final Node source;

    /**
     * The exception that occurred.
     */
    public final Exception exception;

    /**
     * Creates a new warning.
     *
     * @param  source     the filter or expression that produced this warning.
     * @param  exception  the exception that occurred.
     */
    public WarningEvent(final Node source, final Exception exception) {
        this.source    = source;
        this.exception = exception;
    }

    /**
     * If the source is a filter, returns the operator type.
     * Otherwise, returns an empty value.
     *
     * @return the operator type if the source is a filter.
     */
    public Optional<CodeList<?>> getOperatorType() {
        if (source instanceof Filter<?>) {
            return Optional.of(((Filter<?>) source).getOperatorType());
        }
        return Optional.empty();
    }

    /**
     * If the source is an expression, returns the function name.
     * Otherwise, returns an empty value.
     *
     * @return the function name if the source is an expression.
     */
    public Optional<ScopedName> getFunctionName() {
        if (source instanceof Expression<?,?>) {
            return Optional.of(((Expression<?,?>) source).getFunctionName());
        }
        return Optional.empty();
    }

    /**
     * If the source is an expression with at least one parameter of the given type, returns that parameter.
     * If there is many parameter assignable to the given type, then the first occurrence is returned.
     * The {@code type} argument is typically {@code Literal.class} or {@code ValueReference.class}.
     *
     * @param  type  the desired type of the parameter to return.
     * @return the first parameter of the given type, or empty if none.
     */
    @SuppressWarnings("unchecked")
    public <P extends Expression<?,?>> Optional<P> getParameter(final Class<P> type) {
        if (source instanceof Filter<?>) {
            for (Expression<?,?> parameter : ((Filter<?>) source).getExpressions()) {
                if (type.isInstance(parameter)) {
                    return Optional.of((P) parameter);
                }
            }
        }
        if (source instanceof Expression<?,?>) {
            for (Expression<?,?> parameter : ((Expression<?,?>) source).getParameters()) {
                if (type.isInstance(parameter)) {
                    return Optional.of((P) parameter);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a string representation of the warning for debugging purposes.
     *
     * @return a string representation of the warning.
     */
    @Override
    public String toString() {
        return source.getDisplayName() + ": " + exception.toString();
    }
}
