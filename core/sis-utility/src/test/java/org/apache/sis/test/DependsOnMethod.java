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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Declares that a test method depends on an other test method. If any of the dependencies of
 * a test can not be executed successfully, (i.e. those tests failed, threw an error, or were
 * skipped) then the annotated test will be skipped.
 *
 * @author  Stephen Connolly
 * @version 0.3
 * @since   0.3 (derived from <a href="http://github.com/junit-team/junit.contrib/tree/master/assumes">junit-team</a>)
 * @module
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DependsOnMethod {
    /**
     * The names of test methods on which the annotated method depends.
     *
     * @return the names of test methods on which the annotated method depends.
     */
    String[] value();
}
