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
 * Declares that a test class depends on another test class.
 * This annotation was used by a customized test runner but is no longer used in SIS 1.4.
 * See <a href="https://issues.apache.org/jira/browse/SIS-580">SIS-580</a>.
 *
 * @todo Replace by JUnit 5 annotations for class ordering. We could use the ordering based on an integer values,
 *       with constants defined in {@code TestCase} for some broad categories of tests to run in priority.
 *       Note that the following classes need particular attention:
 *       <ul>
 *         <li>{@link org.apache.sis.referencing.operation.matrix.NonSquareMatrixTest}</li>
 *         <li>{@link org.apache.sis.referencing.IdentifiedObjectsTest}</li>
 *       </ul>
 *
 * @author  Martin Desruisseaux
 * @version 1.4
 * @since   0.3
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DependsOn {
    /**
     * The other test classes on which this test depends.
     *
     * @return the test dependencies.
     */
    Class<?>[] value();
}
