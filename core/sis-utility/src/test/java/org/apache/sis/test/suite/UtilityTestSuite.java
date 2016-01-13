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
 * All tests from the {@code sis-utility} module, in approximative dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@Suite.SuiteClasses({
    // Following are testing the test tools.
    org.apache.sis.internal.test.AssertTest.class,
    org.apache.sis.internal.test.TestUtilitiesTest.class,
    org.apache.sis.internal.test.XMLComparatorTest.class,

    // Most basic functions of SIS library.
    org.apache.sis.internal.system.LoggersTest.class,
    org.apache.sis.internal.util.NumericsTest.class,
    org.apache.sis.setup.OptionKeyTest.class,
    org.apache.sis.util.ArraysExtTest.class,
    org.apache.sis.util.CharactersTest.class,
    org.apache.sis.util.CharSequencesTest.class,
    org.apache.sis.util.StringBuildersTest.class,
    org.apache.sis.util.ExceptionsTest.class,
    org.apache.sis.util.UtilitiesTest.class,
    org.apache.sis.util.NumbersTest.class,
    org.apache.sis.util.ClassesTest.class,
    org.apache.sis.util.VersionTest.class,
    org.apache.sis.util.LocalesTest.class,
    org.apache.sis.util.resources.LoaderTest.class,
    org.apache.sis.util.resources.IndexedResourceBundleTest.class,
    org.apache.sis.util.ArgumentChecksTest.class, // Uses resources.
    org.apache.sis.util.logging.PerformanceLevelTest.class,
    org.apache.sis.util.logging.WarningListenersTest.class,
    org.apache.sis.util.logging.MonolineFormatterTest.class,
    org.apache.sis.util.logging.LoggerAdapterTest.class,
    org.apache.sis.math.MathFunctionsTest.class,
    org.apache.sis.math.DecimalFunctionsTest.class,
    org.apache.sis.math.StatisticsTest.class,
    org.apache.sis.math.StatisticsFormatTest.class,
    org.apache.sis.internal.util.UtilitiesTest.class,
    org.apache.sis.internal.util.DoubleDoubleTest.class,
    org.apache.sis.math.LineTest.class,
    org.apache.sis.math.PlaneTest.class,

    // Collections.
    org.apache.sis.internal.util.CheckedArrayListTest.class,
    org.apache.sis.internal.system.ReferenceQueueConsumerTest.class,
    org.apache.sis.util.collection.IntegerListTest.class,
    org.apache.sis.util.collection.WeakHashSetTest.class,
    org.apache.sis.util.collection.WeakValueHashMapTest.class,
    org.apache.sis.util.collection.CacheTest.class,
    org.apache.sis.util.collection.DerivedSetTest.class,
    org.apache.sis.util.collection.DerivedMapTest.class,
    org.apache.sis.util.collection.TableColumnTest.class,
    org.apache.sis.util.collection.DefaultTreeTableTest.class,
    org.apache.sis.util.collection.TreeTablesTest.class,
    org.apache.sis.util.collection.CodeListSetTest.class,
    org.apache.sis.internal.util.CollectionsExtTest.class,
    org.apache.sis.internal.util.AbstractMapTest.class,
    org.apache.sis.internal.util.LazySetTest.class,

    // GeoAPI most basic types.
    org.apache.sis.internal.util.DefinitionURITest.class,
    org.apache.sis.internal.util.XPathsTest.class,
    org.apache.sis.internal.util.CitationsTest.class,
    org.apache.sis.util.iso.TypesTest.class,
    org.apache.sis.util.iso.SimpleInternationalStringTest.class,
    org.apache.sis.util.iso.DefaultInternationalStringTest.class,
    org.apache.sis.internal.util.LocalizedParseExceptionTest.class,
    org.apache.sis.util.iso.AbstractNameTest.class,
    org.apache.sis.util.iso.DefaultNameFactoryTest.class,
    org.apache.sis.util.iso.NamesTest.class,
    org.apache.sis.util.iso.TypeNamesTest.class,
    org.apache.sis.internal.simple.SimpleIdentifierTest.class,
    org.apache.sis.util.iso.DefaultRecordTypeTest.class,
    org.apache.sis.util.iso.DefaultRecordSchemaTest.class,
    org.apache.sis.util.iso.DefaultRecordTest.class,

    // Measurements and formatting.
    org.apache.sis.measure.SexagesimalConverterTest.class,
    org.apache.sis.measure.UnitsTest.class,
    org.apache.sis.measure.RangeTest.class,
    org.apache.sis.measure.DateRangeTest.class,
    org.apache.sis.measure.NumberRangeTest.class,
    org.apache.sis.measure.MeasurementRangeTest.class,
    org.apache.sis.measure.FormattedCharacterIteratorTest.class,
    org.apache.sis.measure.RangeFormatTest.class,
    org.apache.sis.measure.AngleFormatTest.class,
    org.apache.sis.measure.AngleTest.class,
    org.apache.sis.internal.util.X364Test.class,
    org.apache.sis.io.LineAppenderTest.class,
    org.apache.sis.io.LeftMarginTest.class,
    org.apache.sis.io.TabulationExpansionTest.class,
    org.apache.sis.io.WordWrapTest.class,
    org.apache.sis.io.WordWrapWithLineSeparatorTest.class,
    org.apache.sis.io.TableAppenderTest.class,
    org.apache.sis.util.collection.TreeTableFormatTest.class,
    org.apache.sis.util.collection.RangeSetTest.class,
    org.apache.sis.internal.util.StandardDateFormatTest.class,

    // Converters.
    org.apache.sis.internal.converter.AngleConverterTest.class,
    org.apache.sis.internal.converter.StringConverterTest.class,
    org.apache.sis.internal.converter.PathConverterTest.class,
    org.apache.sis.internal.converter.FallbackConverterTest.class,
    org.apache.sis.internal.converter.ArrayConverterTest.class,
    org.apache.sis.internal.converter.ConverterRegistryTest.class,
    org.apache.sis.internal.converter.SystemRegistryTest.class,
    org.apache.sis.internal.converter.NumberConverterTest.class, // Shall be after SystemRegistryTest.

    // XML most basic types.
    org.apache.sis.xml.XLinkTest.class,
    org.apache.sis.xml.NilReasonTest.class,
    org.apache.sis.xml.LegacyCodesTest.class,
    org.apache.sis.xml.ValueConverterTest.class,
    org.apache.sis.xml.OGCNamespacePrefixMapperTest.class,
    org.apache.sis.xml.MarshallerPoolTest.class,
    org.apache.sis.internal.jaxb.XmlUtilitiesTest.class,
    org.apache.sis.internal.jaxb.IdentifierMapAdapterTest.class,
    org.apache.sis.internal.jaxb.ModifiableIdentifierMapTest.class,
    org.apache.sis.internal.jaxb.gco.StringAdapterTest.class,
    org.apache.sis.internal.jaxb.gco.PropertyTypeTest.class,
    org.apache.sis.internal.jaxb.gmd.LanguageCodeTest.class,
    org.apache.sis.internal.jaxb.gml.TimePeriodTest.class,
    org.apache.sis.internal.jaxb.gml.MeasureTest.class,
    org.apache.sis.util.iso.NameMarshallingTest.class
})
public final strictfp class UtilityTestSuite extends TestSuite {
    /**
     * Verifies the list of tests before to run the suite.
     * See {@link #verifyTestList(Class, Class[])} for more information.
     */
    @BeforeClass
    public static void verifyTestList() {
        assertNoMissingTest(UtilityTestSuite.class);
        verifyTestList(UtilityTestSuite.class);
    }
}
