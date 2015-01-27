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
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotates code containing workarounds for bugs or limitations in an external library.
 * This is marker annotation for source code only, in order to keep trace of code to revisit
 * when new versions of external libraries become available.
 *
 * <div class="note"><b>Usage note:</b>
 * When only a portion of a method contains a workaround and the annotation can not be applied to that specific part,
 * then it is applied to the whole method. Developers need to refer to code comments in order to locate the specific
 * part.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@Documented
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD,
         ElementType.FIELD, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.SOURCE)
public @interface Workaround {
    /**
     * A string identifying the library having a bug or limitation.
     * Examples: {@code "JDK"}, {@code "NetCDF"}, {@code "JUnit"}, {@code "SIS"}.
     *
     * @return An identifier of the library having a bug or limitation.
     */
    String library();

    /**
     * The last library version on which the bug has been verified.
     * The bug may have existed before, and may still exist later.
     *
     * @return The library version on which the bug has been observed.
     */
    String version();
}
