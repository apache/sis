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
package org.apache.sis.util.internal.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotates fields that may be replaced by lazy initialization.
 * This is a marker annotation for code to revisit if and when the
 * JEP for Lazy Static Final Fields become available.
 *
 * <h2>Alternative</h2>
 * We could have used the inner class pattern instead, but it it not clear that it is worth the cost.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://openjdk.org/jeps/8209964">JEP draft: Lazy Static Final Fields</a>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface LazyCandidate {
}
