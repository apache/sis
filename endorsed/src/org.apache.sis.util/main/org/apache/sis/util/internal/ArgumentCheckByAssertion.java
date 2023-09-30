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
package org.apache.sis.util.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marker annotation for public methods which use assertions for validating users arguments.
 * This is not recommended for public API, but we do that in a few places where unconditional
 * argument checks may be too expensive. This annotation is used for tracking those methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Target({
    ElementType.METHOD,
    ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface ArgumentCheckByAssertion {
}
