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
package org.apache.sis.util;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotates classes or methods that are provided mostly for debugging purpose.
 * This annotation is defined in order to make easier to find which debugging tools are available in case of problem.
 * See the <cite>"Use"</cite> javadoc link for a list of annotated classes and methods.
 *
 * <p>Unless specified otherwise in javadoc, {@link Object#toString()} method implementations in Apache SIS
 * are implicitly for debugging purpose. Those methods are usually <em>not</em> annotated with {@code @Debug}
 * in order to avoid polluting {@code @Debug} usage searches with long lists of {@code toString()} methods.
 * As an exception to this convention, a {@code toString()} method may be annotated with {@code @Debug}
 * in a few cases where we want to put emphasis on the method purpose as a debugging tools.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface Debug {
}
