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
import java.util.function.Predicate;


/**
 * Identification of a subset of resources from a collection of resources
 * whose property values satisfy a set of logically connected predicates.
 *
 * <div class="warning"><b>Upcoming API change</b><br>
 * This is a placeholder for a GeoAPI 3.1 interface not yet released.
 * In a future version, all usages of this interface may be replaced
 * by an interface of the same name but in the {@code org.opengis.filter} package
 * instead of {@code org.apache.sis.filter}.
 * </div>
 *
 * @param  <R>  the type of resources (e.g. {@link org.apache.sis.feature.AbstractFeature}) to filter.
 *
 * @since 1.1
 */
public interface Filter<R> extends Predicate<R> {
    /**
     * A filter that always evaluates to {@code true}.
     *
     * @param  <R>  the type of resources to filter.
     * @return the "no filtering" filter.
     */
    @SuppressWarnings("unchecked")
    static <R> Filter<R> include() {
        return FilterLiteral.INCLUDE;
    }

    /**
     * A filter that always evaluates to {@code false}.
     *
     * @param  <R>  the type of resources to filter.
     * @return the "exclude all" filter.
     */
    @SuppressWarnings("unchecked")
    static <R> Filter<R> exclude() {
        return FilterLiteral.EXCLUDE;
    }

    /**
     * Returns the nature of the operator.
     *
     * @return the nature of this operator.
     */
    Enum<?> getOperatorType();

    /**
     * Returns the class of resources expected by this filter.
     *
     * @return type of resources accepted by this filter.
     *
     * @since 1.4
     */
    Class<? super R> getResourceClass();

    /**
     * Returns the expressions used as arguments for this filter.
     *
     * @return the expressions used as inputs, or an empty list if none.
     */
    List<Expression<R,?>> getExpressions();

    /**
     * Given an object, determines if the test(s) represented by this filter are passed.
     *
     * @param  object  the object (often a {@code Feature} instance) to evaluate.
     * @return {@code true} if the test(s) are passed for the provided object.
     * @throws NullPointerException if {@code object} is null.
     * @throws IllegalArgumentException if the filter can not be applied on the given object.
     */
    @Override
    boolean test(R object);
}
