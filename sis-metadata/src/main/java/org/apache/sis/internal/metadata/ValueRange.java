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
package org.apache.sis.internal.metadata;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;


/**
 * The range of values that a method can return.
 * This is used mostly for metadata objects performing runtime checks.
 *
 * <p>We do not put (yet) this annotation in public API because it may be
 * replaced by some <cite>checker framework</cite> in a future JDK version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.04)
 * @version 0.3
 * @module
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueRange {
    /**
     * Returns the minimal value that a method can return. The default value is
     * {@linkplain Double#NEGATIVE_INFINITY negative infinity}, which means that
     * there is no minimal value.
     *
     * @return The minimal value.
     */
    double minimum() default Double.NEGATIVE_INFINITY;

    /**
     * Returns the maximal value that a method can return. The default value is
     * {@linkplain Double#POSITIVE_INFINITY positive infinity}, which means that
     * there is no maximal value.
     *
     * @return The maximal value.
     */
    double maximum() default Double.POSITIVE_INFINITY;

    /**
     * {@code true} if the {@linkplain #minimum() minimal} value is inclusive, or {@code false}
     * if it is exclusive. By default the minimum value is inclusive.
     *
     * @return {@code true} if the minimum value is inclusive.
     */
    boolean isMinIncluded() default true;

    /**
     * {@code true} if the {@linkplain #maximum() maximal} value is inclusive, or {@code false}
     * if it is exclusive. By default the maximum value is <strong>inclusive</strong>.
     *
     * @return {@code true} if the maximum value is inclusive.
     */
    boolean isMaxIncluded() default true;
}
