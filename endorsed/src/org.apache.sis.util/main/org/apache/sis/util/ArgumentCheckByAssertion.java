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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marker annotation for methods which use assertions for validating their arguments.
 * Arguments validation with assertions is usually not a recommended practice,
 * but Apache SIS does that when unconditional argument checks would be too expensive.
 * Examples:
 *
 * <ul>
 *   <li>Checking wether two Coordinate Reference Systems are equal (ignoring metadata)
 *       in methods potentially executed millions of times (e.g. adding points).</li>
 *   <li>Checking the class of argument values when those classes should never be wrong
 *       if the code has been compiled without Java compiler warnings.</li>
 * </ul>
 *
 * This annotation is used for documenting methods where some preconditions are checked with assertions.
 * The Javadoc should document what those preconditions are.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@Target({
    ElementType.METHOD,
    ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface ArgumentCheckByAssertion {
}
