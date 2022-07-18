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
 * @version 1.3
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
    org.apache.sis.feature.FeatureOperationsTest.class,
    org.apache.sis.feature.FeatureFormatTest.class,
    org.apache.sis.feature.FeaturesTest.class,
    org.apache.sis.filter.XPathTest.class,
    org.apache.sis.filter.CapabilitiesTest.class,
    org.apache.sis.filter.LeafExpressionTest.class,
    org.apache.sis.filter.LogicalFilterTest.class,
    org.apache.sis.filter.IdentifierFilterTest.class,
    org.apache.sis.filter.ArithmeticFunctionTest.class,
    org.apache.sis.filter.ComparisonFilterTest.class,
    org.apache.sis.filter.LikeFilterTest.class,
    org.apache.sis.filter.TemporalFilterTest.class,
    org.apache.sis.filter.BinarySpatialFilterUsingJTS_Test.class,
    org.apache.sis.filter.BinarySpatialFilterUsingESRI_Test.class,
    org.apache.sis.filter.BinarySpatialFilterUsingJava2D_Test.class,
    org.apache.sis.internal.feature.AttributeConventionTest.class,
    org.apache.sis.internal.feature.GeometryTypeTest.class,
    org.apache.sis.internal.filter.FunctionNamesTest.class,
    org.apache.sis.internal.filter.sqlmm.SQLMMTest.class,
    org.apache.sis.internal.filter.sqlmm.RegistryUsingJTS_Test.class,
    org.apache.sis.internal.filter.sqlmm.RegistryUsingESRI_Test.class,
    org.apache.sis.internal.filter.sqlmm.RegistryUsingJava2D_Test.class,
    org.apache.sis.internal.feature.j2d.ShapePropertiesTest.class,
    org.apache.sis.internal.feature.j2d.FlatShapeTest.class,
    org.apache.sis.internal.feature.j2d.FactoryTest.class,
    org.apache.sis.internal.feature.esri.FactoryTest.class,
    org.apache.sis.internal.feature.jts.FactoryTest.class,
    org.apache.sis.internal.feature.jts.JTSTest.class,
    org.apache.sis.internal.feature.jts.ShapeAdapterTest.class,
    org.apache.sis.internal.feature.jts.ShapeConverterTest.class,
    org.apache.sis.feature.builder.CharacteristicTypeBuilderTest.class,
    org.apache.sis.feature.builder.AttributeTypeBuilderTest.class,
    org.apache.sis.feature.builder.AssociationRoleBuilderTest.class,
    org.apache.sis.feature.builder.FeatureTypeBuilderTest.class,

    // Rasters
    org.apache.sis.internal.coverage.j2d.ImageUtilitiesTest.class,
    org.apache.sis.internal.coverage.j2d.ImageLayoutTest.class,
    org.apache.sis.internal.coverage.j2d.ScaledColorSpaceTest.class,
    org.apache.sis.internal.coverage.j2d.ColorizerTest.class,
    org.apache.sis.internal.coverage.j2d.SampleModelFactoryTest.class,
    org.apache.sis.internal.processing.image.IsolinesTest.class,
    org.apache.sis.image.DataTypeTest.class,
    org.apache.sis.image.PlanarImageTest.class,
    org.apache.sis.image.ComputedImageTest.class,
    org.apache.sis.image.PixelIteratorTest.class,
    org.apache.sis.image.LinearIteratorTest.class,
    org.apache.sis.image.BandedIteratorTest.class,
    org.apache.sis.image.StatisticsCalculatorTest.class,
    org.apache.sis.image.BandSelectImageTest.class,
    org.apache.sis.image.InterpolationTest.class,
    org.apache.sis.image.ResamplingGridTest.class,
    org.apache.sis.image.ResampledImageTest.class,
    org.apache.sis.image.MaskedImageTest.class,
    org.apache.sis.image.BandedSampleConverterTest.class,
    org.apache.sis.image.ImageCombinerTest.class,
    org.apache.sis.image.ImageProcessorTest.class,
    org.apache.sis.coverage.CategoryTest.class,
    org.apache.sis.coverage.CategoryListTest.class,
    org.apache.sis.coverage.SampleDimensionTest.class,
    org.apache.sis.coverage.SampleRangeFormatTest.class,
    org.apache.sis.coverage.grid.PixelTranslationTest.class,
    org.apache.sis.coverage.grid.GridOrientationTest.class,
    org.apache.sis.coverage.grid.GridExtentTest.class,
    org.apache.sis.coverage.grid.GridGeometryTest.class,
    org.apache.sis.coverage.grid.GridDerivationTest.class,
    org.apache.sis.coverage.grid.FractionalGridCoordinatesTest.class,
    org.apache.sis.coverage.grid.ReshapedImageTest.class,
    org.apache.sis.coverage.grid.GridCoverage2DTest.class,
    org.apache.sis.coverage.grid.BufferedGridCoverageTest.class,
    org.apache.sis.coverage.grid.GridCoverageBuilderTest.class,
    org.apache.sis.coverage.grid.ConvertedGridCoverageTest.class,
    org.apache.sis.coverage.grid.TranslatedGridCoverageTest.class,
    org.apache.sis.coverage.grid.ResampledGridCoverageTest.class,

    // Index and processing
    org.apache.sis.index.tree.PointTreeNodeTest.class,
    org.apache.sis.index.tree.PointTreeTest.class
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
