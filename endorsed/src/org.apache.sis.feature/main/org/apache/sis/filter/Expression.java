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
package org.apache.sis.filter;

import java.util.List;
import java.util.function.Function;
import org.opengis.util.ScopedName;


/**
 * A literal or a named procedure that performs a distinct computation.
 *
 * <div class="warning"><b>Upcoming API change</b><br>
 * This is a placeholder for a GeoAPI 3.1 interface not yet released.
 * In a future version, all usages of this interface may be replaced
 * by an interface of the same name but in the {@code org.opengis.filter} package
 * instead of {@code org.apache.sis.filter}.
 * </div>
 *
 * @param  <R>  the type of resources (e.g. {@code Feature}) used as inputs.
 * @param  <V>  the type of values computed by the expression.
 */
public interface Expression<R,V> extends Function<R,V> {
    /**
     * Returns the name of the function to be called.
     *
     * @return name of the function to be called.
     */
    ScopedName getFunctionName();

    /**
     * Returns the class of resources expected by this expression.
     *
     * @return type of resources accepted by this expression.
     */
    Class<? super R> getResourceClass();

    /**
     * Returns the list sub-expressions that will be evaluated to provide the parameters to the function.
     *
     * @return the sub-expressions to be evaluated, or an empty list if none.
     */
    List<Expression<R,?>> getParameters();

    /**
     * Evaluates the expression value based on the content of the given object.
     *
     * @param  input  the object to be evaluated by the expression.
     *         Can be {@code null} if this expression allows null values.
     * @return value computed by the expression.
     * @throws NullPointerException if {@code input} is null and this expression requires non-null values.
     * @throws IllegalArgumentException if the expression can not be applied on the given object.
     */
    @Override
    V apply(R input);

    /**
     * Returns an expression doing the same evaluation than this method, but returning results
     * as values of the specified type.
     *
     * @param  <N>   compile-time value of {@code type}.
     * @param  type  desired type of expression results.
     * @return expression doing the same operation this this expression but with results of the specified type.
     * @throws ClassCastException if the specified type is not a target type supported by implementation.
     */
    <N> Expression<R,N> toValueType(Class<N> type);
}
