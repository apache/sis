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

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Marker annotation for methods which may return {@code java.util.Optional} in a future version.
 * This is applied on some methods introduced at a time when Apache SIS was targeting Java 6 or 7.
 * Replacing the return value by an {@link java.util.Optional} would be an incompatible change,
 * so this change will not be applied in SIS 1.x. But it may be applied in SIS 2.0.
 *
 * <p><b>Note:</b> the use of this annotation is not a guarantee that the annotated method will return
 * {@link java.util.Optional} in SIS 2. It only means that this possibility is under consideration.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface OptionalCandidate {
}
