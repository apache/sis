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
package org.apache.sis.feature;

import org.apache.sis.test.DependsOn;


/**
 * Tests {@link SparseFeature}.
 * This class inherits all tests defined in {@link FeatureTestCase}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn({
    DefaultFeatureTypeTest.class,
    SingletonAttributeTest.class,
    PropertySingletonTest.class
})
public final strictfp class SparseFeatureTest extends FeatureTestCase {
    /**
     * Creates a new feature for the given type.
     */
    @Override
    final AbstractFeature createFeature(final DefaultFeatureType type) {
        return new SparseFeature(type);
    }

    /**
     * Clones the {@link #feature} instance.
     */
    @Override
    final AbstractFeature cloneFeature() throws CloneNotSupportedException {
        return ((SparseFeature) feature).clone();
    }

    // Inherit all tests from the super-class.
}
