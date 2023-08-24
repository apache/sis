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
package org.apache.sis.system;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotates methods having a system-wide impact on the configuration of the Apache SIS library.
 * Also used for some static final constants fixed to arbitrary values that could be customized.
 * This annotation should not be used on test classes for avoiding to pollute usage searches.
 *
 * <h2>Application to static final constants</h2>
 * We do not annotate all static constants having arbitrary values because there is too many of them.
 * We annotate only the most interesting ones, when we can reasonably think that some developers may
 * want to change the values. This annotation is used for identifying aspects where we could add API
 * in a future version if there is an interest for controlling them.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.3
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface Configuration {
    /**
     * Tells by which API the configuration can be modified.
     * The value shall be {@code NONE} (the default) for compile-time constants.
     * At the opposite extreme, the value should be {@code GLOBAL} If the configuration
     * is accessible from the {@link org.apache.sis.setup.Configuration} public class.
     *
     * @return which API can modify the configuration.
     */
    Access writeAccess() default Access.NONE;

    /**
     * Indication of which API can modify the configuration. The enumeration is ordered from
     * non-modifiable to the preferred place where to provide control on the configuration.
     */
    enum Access {
        /**
         * The configuration is non-modifiable.
         * This value applies to all static final constants.
         */
        NONE,

        /**
         * The configuration is modifiable through an internal API.
         * There is no public API for this configuration yet.
         */
        INTERNAL,

        /**
         * The configuration is modifiable through a public method
         * that must be invoked on a particular instance of a class.
         * This configuration cannot be easily moved to {@link #GLOBAL}
         * because it would require to decide on which instance to apply.
         */
        INSTANCE,

        /**
         * The configuration is modifiable through a public static method.
         * This value means that the configuration could be moved to {@link #GLOBAL},
         * but has not been moved yet because it is not clear if there is an interest for that.
         */
        STATIC,

        /**
         * The configuration is modifiable through {@link org.apache.sis.setup.Configuration}.
         * This is the place where we try to centralize all configurations of interest.
         */
        GLOBAL
    }
}
