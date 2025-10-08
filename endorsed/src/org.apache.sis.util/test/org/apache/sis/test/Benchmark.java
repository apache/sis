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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotates classes, methods or fields that can be used for benchmarking.
 * The annotated methods may also be JUnit tests, but not necessarily.
 * If the method is also a test, then it should also be annotated with {@code @Tag(Benchmark.TAG)}.
 *
 * <p>This annotation is for documentation purposes only. Some annotated methods may be executed manually
 * before and after an implementation change, in order to test the impact on performance.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Benchmark {
    /**
     * The value to use in {@link org.junit.jupiter.api.Tag} annotations
     * for tests that are also benchmarks.
     *
     * @see TestCase#RUN_EXTENSIVE_TESTS
     */
    String TAG = "Benchmark";
}
