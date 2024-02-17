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
package org.apache.sis.filter.sqlmm;

import com.esri.core.geometry.Geometry;

// Test dependencies
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link SpatialFunction} implementations using JTS library.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class RegistryUsingESRI_Test extends RegistryTestCase<Geometry> {
    /**
     * Creates a new test.
     */
    public RegistryUsingESRI_Test() {
        super(Geometry.class, false);
    }

    @Test
    @Override
    @Disabled("Reprojection not yet implemented.")
    public void testTransform() {
    }

    @Test
    @Override
    @Disabled("Reprojection not yet implemented.")
    public void testOptimization() {
    }

    @Test
    @Override
    @Disabled("Reprojection not yet implemented.")
    public void testFeatureOptimization() {
    }

    @Test
    @Override
    @Disabled("Reprojection not yet implemented.")
    public void testIntersectsWithReprojection() {
    }

    @Test
    @Override
    @Disabled("Current implementation ignores the distance parameter.")
    public void testSimplify() {
    }

    @Test
    @Override
    @Disabled("Operation not yet implemented.")
    public void testSimplifyPreserveTopology() {
    }
}
