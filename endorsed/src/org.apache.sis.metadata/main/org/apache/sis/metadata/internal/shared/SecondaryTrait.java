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
package org.apache.sis.metadata.internal.shared;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Indicates that an interface implemented by a class is considered less significant compared to the primary interface.
 * This annotation is used when a class implements two or more interfaces, but some of those interfaces can be ignored.
 * This information is needed for identifying the main interface in metadata.
 *
 * <h2>Design note</h2>
 * {@link ElementType#TYPE_USE} would be more appropriate. However, in the way that the metadata module currently uses
 * this interface, an annotation on the class is more convenient (more straightforward code).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecondaryTrait {
    /**
     * The interface which is less significant compared to the primary interface.
     *
     * @return the less significant interface.
     */
    Class<?> value();
}
