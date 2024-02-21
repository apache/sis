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
package org.apache.sis.test;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * A test method producing an object to be used by another test method.
 * This annotation is for documentation purpose only. It is used in replacement to
 * {@link org.junit.jupiter.api.Test} when the method is invoked by another method.
 * It may be because the methods need to be invoked inside a {@code try … finally}
 * block, or because the result of a test is used by a next step.
 *
 * <p>We currently do not use {@link org.junit.jupiter.api.TestMethodOrder} annotation for
 * the purposes described above because the semantic during parallel execution is unclear,
 * and we do not want to force {@code Lifecycle.PER_CLASS} on those tests.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface TestStep {
}
