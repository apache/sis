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
import org.junit.BeforeClass;


/**
 * All tests from the {@code sis-feature} module, in approximative dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
@Suite.SuiteClasses({
    org.apache.sis.feature.DefaultAttributeTypeTest.class,
    org.apache.sis.feature.CharacteristicTypeMapTest.class,
    org.apache.sis.feature.CharacteristicMapTest.class,
    org.apache.sis.feature.DefaultFeatureTypeTest.class,
    org.apache.sis.feature.PropertySingletonTest.class,
    org.apache.sis.feature.SingletonAttributeTest.class,
    org.apache.sis.feature.MultiValuedAttributeTest.class,
    org.apache.sis.feature.DenseFeatureTest.class,
    org.apache.sis.feature.SparseFeatureTest.class,
    org.apache.sis.feature.DefaultAssociationRoleTest.class,
    org.apache.sis.feature.SingletonAssociationTest.class,
    org.apache.sis.feature.AbstractOperationTest.class,
    org.apache.sis.feature.LinkOperationTest.class,
    org.apache.sis.feature.StringJoinOperationTest.class,
    org.apache.sis.feature.EnvelopeOperationTest.class,
    org.apache.sis.feature.FeatureFormatTest.class,
    org.apache.sis.feature.FeaturesTest.class,
    org.apache.sis.internal.feature.AttributeConventionTest.class,
    org.apache.sis.internal.feature.FeatureTypeBuilderTest.class
})
public final strictfp class FeatureTestSuite extends TestSuite {
    /**
     * Verifies the list of tests before to run the suite.
     * See {@link #verifyTestList(Class, Class[])} for more information.
     */
    @BeforeClass
    public static void verifyTestList() {
        assertNoMissingTest(FeatureTestSuite.class);
        verifyTestList(FeatureTestSuite.class);
    }
}
