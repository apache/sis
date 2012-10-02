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
package org.apache.sis.test.suite;

import org.apache.sis.test.TestSuite;
import org.junit.runners.Suite;


/**
 * All tests from the {@code sis-utility} module, in approximative dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@Suite.SuiteClasses({
  // Following are testing the test tools.
  org.apache.sis.internal.test.AssertTest.class,
  org.apache.sis.internal.test.XMLComparatorTest.class,

  // Following are testing the actual SIS library.
  org.apache.sis.util.ArraysTest.class,
  org.apache.sis.util.CharactersTest.class,
  org.apache.sis.util.CharSequencesTest.class,
  org.apache.sis.util.StringBuildersTest.class,
  org.apache.sis.util.LocalesTest.class,
  org.apache.sis.util.resources.IndexedResourceBundleTest.class,
  org.apache.sis.util.logging.PerformanceLevelTest.class,
  org.apache.sis.util.type.TypesTest.class,
  org.apache.sis.util.type.SimpleInternationalStringTest.class,
  org.apache.sis.util.type.DefaultInternationalStringTest.class
})
public final strictfp class UtilityTestSuite extends TestSuite {
}
