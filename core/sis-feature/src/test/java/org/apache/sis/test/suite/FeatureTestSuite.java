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
import org.junit.BeforeClass;
import org.junit.runners.Suite;


/**
 * All tests from the {@code sis-feature} module, in rough dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   0.5
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
    org.apache.sis.feature.AbstractFeatureTest.class,
    org.apache.sis.feature.DefaultAssociationRoleTest.class,
    org.apache.sis.feature.SingletonAssociationTest.class,
    org.apache.sis.feature.AbstractOperationTest.class,
    org.apache.sis.feature.LinkOperationTest.class,
    org.apache.sis.feature.StringJoinOperationTest.class,
    org.apache.sis.feature.EnvelopeOperationTest.class,
    org.apache.sis.feature.FeatureFormatTest.class,
    org.apache.sis.feature.FeaturesTest.class,
    org.apache.sis.filter.CapabilitiesTest.class,
    org.apache.sis.filter.LeafExpressionTest.class,
    org.apache.sis.filter.LogicalFunctionTest.class,
    org.apache.sis.filter.UnaryFunctionTest.class,
    org.apache.sis.filter.DefaultObjectIdTest.class,
    org.apache.sis.filter.FilterByIdentifierTest.class,
    org.apache.sis.filter.ArithmeticFunctionTest.class,
    org.apache.sis.filter.ComparisonFunctionTest.class,
    org.apache.sis.filter.BetweenFunctionTest.class,
    org.apache.sis.filter.LikeFunctionTest.class,
    org.apache.sis.filter.SpatialFunctionTest.class,
    org.apache.sis.filter.TemporalFunctionTest.class,
    org.apache.sis.internal.filter.sqlmm.SQLMMTest.class,
    org.apache.sis.internal.feature.AttributeConventionTest.class,
    org.apache.sis.internal.feature.j2d.ShapePropertiesTest.class,
    org.apache.sis.internal.feature.Java2DTest.class,
    org.apache.sis.internal.feature.ESRITest.class,
    org.apache.sis.internal.feature.JTSTest.class,
    org.apache.sis.internal.feature.jts.JTSTest.class,
    org.apache.sis.feature.builder.CharacteristicTypeBuilderTest.class,
    org.apache.sis.feature.builder.AttributeTypeBuilderTest.class,
    org.apache.sis.feature.builder.AssociationRoleBuilderTest.class,
    org.apache.sis.feature.builder.FeatureTypeBuilderTest.class,

    // Rasters
    org.apache.sis.image.DefaultIteratorTest.class,
    org.apache.sis.image.LinearIteratorTest.class,
    org.apache.sis.image.RelocatedImageTest.class,
    org.apache.sis.coverage.CategoryTest.class,
    org.apache.sis.coverage.CategoryListTest.class,
    org.apache.sis.coverage.SampleDimensionTest.class,
    org.apache.sis.coverage.SampleRangeFormatTest.class,
    org.apache.sis.coverage.grid.PixelTranslationTest.class,
    org.apache.sis.coverage.grid.GridExtentTest.class,
    org.apache.sis.coverage.grid.GridGeometryTest.class,
    org.apache.sis.coverage.grid.GridDerivationTest.class,
    org.apache.sis.coverage.grid.FractionalGridCoordinates.class,
    org.apache.sis.coverage.grid.GridCoverage2DTest.class,
    org.apache.sis.internal.coverage.j2d.ImageUtilitiesTest.class,
    org.apache.sis.internal.coverage.j2d.ScaledColorSpaceTest.class,
    org.apache.sis.internal.coverage.j2d.PlanarImageTest.class,
    org.apache.sis.internal.coverage.j2d.BufferedGridCoverageTest.class
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
