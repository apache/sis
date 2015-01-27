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
package org.apache.sis.measure;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;


/**
 * The range of values assignable to a field, or to a JavaBean property.
 * When used with JavaBeans, this annotation shall be applied on the getter method
 * as in the following example:
 *
 * {@preformat java
 *     &#64;ValueRange(minimum=0, maximum=100)
 *     public double getCloudCoverPercentage() {
 *         // Method implementation here...
 *     }
 * }
 *
 * By default, both endpoints are inclusive. To make an endpoint exclusive,
 * a {@code isFooInclusive} argument needs to be explicitely provided. This
 * is useful mostly for floating point numbers. In the following example,
 * values can be very close to zero but not equals, since a value of exactly
 * zero makes no sense. Note also that the {@code maximum} value is not explicitely
 * provided, in which case it defaults to infinity.
 *
 * {@preformat java
 *     &#64;@ValueRange(minimum=0, isMinIncluded=false)
 *     public double getSpatialResolution() {
 *         // Method implementation here...
 *     }
 * }
 *
 * It is sometime convenient to convert {@code ValueRange} to {@link NumberRange} instances
 * in order to leverage the various {@code NumberRange} operations. The following example
 * uses a convenience constructor for this purpose. Note that the {@code Double} type could
 * by inferred from {@link java.lang.reflect.Method#getReturnType()}.
 *
 * {@preformat java
 *     Method myMethod = ...;
 *     ValueRange annotation = myMethod.getAnnotation(ValueRange.class);
 *     if (annotation != null) {
 *         NumberRange<Double> range = new NumberRange(Double.class, annotation);
 *         // Use the range here.
 *     }
 * }
 *
 * The {@link org.apache.sis.metadata.AbstractMetadata} class uses this annotation for inferring
 * {@link org.opengis.parameter.ParameterDescriptor} from metadata interfaces and implementation
 * classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see NumberRange#NumberRange(Class, ValueRange)
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
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
     * {@code true} if the {@linkplain #minimum() minimal} value is inclusive, or {@code false}
     * if it is exclusive. By default the minimum value is inclusive.
     *
     * @return {@code true} if the minimum value is inclusive.
     */
    boolean isMinIncluded() default true;

    /**
     * Returns the maximal value that a method can return. The default value is
     * {@linkplain Double#POSITIVE_INFINITY positive infinity}, which means that
     * there is no maximal value.
     *
     * @return The maximal value.
     */
    double maximum() default Double.POSITIVE_INFINITY;

    /**
     * {@code true} if the {@linkplain #maximum() maximal} value is inclusive, or {@code false}
     * if it is exclusive. By default the maximum value is <strong>inclusive</strong>.
     *
     * @return {@code true} if the maximum value is inclusive.
     */
    boolean isMaxIncluded() default true;
}
