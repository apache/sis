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
 * All tests from the {@code sis-metadata} module, in rough dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.3
 * @module
 */
@Suite.SuiteClasses({
    org.apache.sis.internal.metadata.IdentifiersTest.class,
    org.apache.sis.internal.metadata.AxisNamesTest.class,
    org.apache.sis.internal.metadata.NameMeaningTest.class,
    org.apache.sis.internal.metadata.MetadataUtilitiesTest.class,

    // Classes using Java reflection.
    org.apache.sis.metadata.PropertyInformationTest.class,
    org.apache.sis.metadata.PropertyAccessorTest.class,
    org.apache.sis.metadata.SpecialCasesTest.class,
    org.apache.sis.metadata.NameMapTest.class,
    org.apache.sis.metadata.TypeMapTest.class,
    org.apache.sis.metadata.InformationMapTest.class,
    org.apache.sis.metadata.ValueMapTest.class,
    org.apache.sis.metadata.TreeNodeChildrenTest.class,
    org.apache.sis.metadata.TreeNodeTest.class,
    org.apache.sis.metadata.TreeTableViewTest.class,
    org.apache.sis.metadata.TreeTableFormatTest.class,
    org.apache.sis.metadata.MetadataStandardTest.class,
    org.apache.sis.metadata.HashCodeTest.class,
    org.apache.sis.metadata.PrunerTest.class,
    org.apache.sis.metadata.AbstractMetadataTest.class,
    org.apache.sis.metadata.ModifiableMetadataTest.class,
    org.apache.sis.metadata.MetadataCopierTest.class,
    org.apache.sis.internal.metadata.MergerTest.class,

    // XML marshalling.
    org.apache.sis.internal.test.DocumentComparatorTest.class,
    org.apache.sis.xml.NamespacesTest.class,
    org.apache.sis.xml.XLinkTest.class,
    org.apache.sis.xml.XPointerTest.class,
    org.apache.sis.xml.NilReasonTest.class,
    org.apache.sis.xml.LegacyCodesTest.class,
    org.apache.sis.xml.ValueConverterTest.class,
    org.apache.sis.xml.MarshallerPoolTest.class,
    org.apache.sis.xml.TransformingNamespacesTest.class,
    org.apache.sis.xml.TransformerTest.class,
    org.apache.sis.internal.xml.XmlUtilitiesTest.class,
    org.apache.sis.internal.jaxb.IdentifierMapAdapterTest.class,
    org.apache.sis.internal.jaxb.ModifiableIdentifierMapTest.class,
    org.apache.sis.internal.jaxb.gco.StringAdapterTest.class,
    org.apache.sis.internal.jaxb.gco.PropertyTypeTest.class,
    org.apache.sis.internal.jaxb.gco.MultiplicityTest.class,
    org.apache.sis.internal.jaxb.lan.PT_LocaleTest.class,
    org.apache.sis.internal.jaxb.lan.OtherLocalesTest.class,
    org.apache.sis.internal.jaxb.lan.LanguageCodeTest.class,
    org.apache.sis.internal.jaxb.lan.FreeTextMarshallingTest.class,
    org.apache.sis.internal.jaxb.cat.EnumAdapterTest.class,
    org.apache.sis.internal.jaxb.cat.EnumMarshallingTest.class,
    org.apache.sis.internal.jaxb.cat.CodeListMarshallingTest.class,
    org.apache.sis.internal.jaxb.gml.TimePeriodTest.class,
    org.apache.sis.internal.jaxb.gml.MeasureTest.class,
    org.apache.sis.xml.NilReasonMarshallingTest.class,
    org.apache.sis.xml.CharSequenceSubstitutionTest.class,
    org.apache.sis.xml.UUIDMarshallingTest.class,
    org.apache.sis.xml.XLinkMarshallingTest.class,

    // GeoAPI most basic types.
    org.apache.sis.internal.simple.SimpleIdentifierTest.class,
    org.apache.sis.util.iso.TypesTest.class,
    org.apache.sis.util.iso.DefaultLocalNameTest.class,
    org.apache.sis.util.iso.DefaultScopedNameTest.class,
    org.apache.sis.util.iso.DefaultNameFactoryTest.class,
    org.apache.sis.util.iso.NamesTest.class,
    org.apache.sis.util.iso.TypeNamesTest.class,
    org.apache.sis.util.iso.DefaultRecordTypeTest.class,
    org.apache.sis.util.iso.DefaultRecordSchemaTest.class,
    org.apache.sis.util.iso.DefaultRecordTest.class,
    org.apache.sis.util.iso.NameMarshallingTest.class,

    // ISO implementations.
    org.apache.sis.metadata.iso.citation.DefaultContactTest.class,
    org.apache.sis.metadata.iso.citation.DefaultResponsibilityTest.class,
    org.apache.sis.metadata.iso.citation.DefaultCitationDateTest.class,
    org.apache.sis.metadata.iso.citation.DefaultCitationTest.class,
    org.apache.sis.metadata.iso.maintenance.DefaultScopeDescriptionTest.class,
    org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBoxTest.class,
    org.apache.sis.metadata.iso.extent.DefaultExtentTest.class,
    org.apache.sis.metadata.iso.extent.ExtentsTest.class,
    org.apache.sis.metadata.iso.content.DefaultBandTest.class,
    org.apache.sis.metadata.iso.spatial.DefaultGeorectifiedTest.class,
    org.apache.sis.metadata.iso.identification.DefaultKeywordsTest.class,
    org.apache.sis.metadata.iso.identification.DefaultRepresentativeFractionTest.class,
    org.apache.sis.metadata.iso.identification.DefaultResolutionTest.class,
    org.apache.sis.metadata.iso.identification.DefaultBrowseGraphicTest.class,
    org.apache.sis.metadata.iso.identification.DefaultDataIdentificationTest.class,
    org.apache.sis.internal.jaxb.metadata.replace.ServiceParameterTest.class,
    org.apache.sis.metadata.iso.identification.DefaultCoupledResourceTest.class,
    org.apache.sis.metadata.iso.identification.DefaultServiceIdentificationTest.class,
    org.apache.sis.metadata.iso.quality.AbstractElementTest.class,
    org.apache.sis.metadata.iso.quality.AbstractPositionalAccuracyTest.class,
    org.apache.sis.metadata.iso.quality.ScopeCodeTest.class,
    org.apache.sis.metadata.iso.lineage.DefaultLineageTest.class,
    org.apache.sis.metadata.iso.lineage.DefaultProcessStepTest.class,
    org.apache.sis.metadata.iso.constraint.DefaultLegalConstraintsTest.class,
    org.apache.sis.metadata.iso.DefaultIdentifierTest.class,
    org.apache.sis.metadata.iso.DefaultMetadataTest.class,
    org.apache.sis.metadata.iso.CustomMetadataTest.class,
    org.apache.sis.metadata.iso.AllMetadataTest.class,
    org.apache.sis.metadata.iso.MarshallingTest.class,
    org.apache.sis.metadata.iso.APIVerifier.class,

    org.apache.sis.internal.metadata.sql.SQLUtilitiesTest.class,
    org.apache.sis.internal.metadata.sql.TypeMapperTest.class,
    org.apache.sis.internal.metadata.sql.ScriptRunnerTest.class,
    org.apache.sis.metadata.sql.IdentifierGeneratorTest.class,
    org.apache.sis.metadata.sql.MetadataSourceTest.class,
    org.apache.sis.metadata.sql.MetadataWriterTest.class,
    org.apache.sis.metadata.iso.citation.CitationsTest.class,
    org.apache.sis.metadata.xml.SchemaComplianceTest.class
})
public final strictfp class MetadataTestSuite extends TestSuite {
    /**
     * Verifies the list of tests before to run the suite.
     * See {@link #verifyTestList(Class, Class[])} for more information.
     */
    @BeforeClass
    public static void verifyTestList() {
        assertNoMissingTest(MetadataTestSuite.class);
        verifyTestList(MetadataTestSuite.class);
    }
}
