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
 * All tests from the {@code sis-metadata} module, in approximative dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@Suite.SuiteClasses({
    org.apache.sis.internal.metadata.MetadataUtilitiesTest.class,
    org.apache.sis.metadata.iso.citation.DefaultCitationDateTest.class,
    org.apache.sis.metadata.iso.citation.DefaultCitationTest.class,
    org.apache.sis.metadata.iso.identification.DefaultKeywordsTest.class,
    org.apache.sis.metadata.iso.identification.DefaultResolutionTest.class,
    org.apache.sis.metadata.iso.spatial.DefaultGeorectifiedTest.class,
    org.apache.sis.metadata.iso.maintenance.DefaultScopeDescriptionTest.class,
    org.apache.sis.metadata.iso.quality.AbstractElementTest.class,

    // Classes using Java reflection.
    org.apache.sis.metadata.PropertyInformationTest.class,
    org.apache.sis.metadata.PropertyAccessorTest.class,
    org.apache.sis.metadata.NameMapTest.class,
    org.apache.sis.metadata.TypeMapTest.class,
    org.apache.sis.metadata.InformationMapTest.class,
    org.apache.sis.metadata.ValueMapTest.class,
    org.apache.sis.metadata.TreeNodeChildrenTest.class,
    org.apache.sis.metadata.TreeNodeTest.class,
    org.apache.sis.metadata.TreeTableViewTest.class,
    org.apache.sis.metadata.TreeTableFormatTest.class,
    org.apache.sis.metadata.MetadataStandardTest.class,
    org.apache.sis.metadata.PrunerTest.class,
    org.apache.sis.metadata.AbstractMetadataTest.class,
    org.apache.sis.metadata.iso.ImmutableIdentifierTest.class,
    org.apache.sis.metadata.iso.AllMetadataTest.class,

    // XML marshalling.
    org.apache.sis.internal.jaxb.code.CodeListMarshallingTest.class,
    org.apache.sis.internal.jaxb.gmd.LanguageMarshallingTest.class,
    org.apache.sis.internal.jaxb.gml.TimePeriodTest.class,
    org.apache.sis.xml.FreeTextMarshallingTest.class,
    org.apache.sis.xml.NilReasonMarshallingTest.class,
    org.apache.sis.xml.AnchorMarshallingTest.class,
    org.apache.sis.xml.ObjectReferenceMarshallingTest.class,
    org.apache.sis.xml.CustomMetadataTest.class,
    org.apache.sis.xml.ImageryMarshallingTest.class,
    org.apache.sis.xml.MetadataMarshallingTest.class
})
public final strictfp class MetadataTestSuite extends TestSuite {
    /**
     * Verifies the list of tests before to run the suite.
     * See {@link #verifyTestList(Class, Class[])} for more information.
     */
    @BeforeClass
    public static void verifyTestList() {
        verifyTestList(MetadataTestSuite.class, BASE_TEST_CLASSES);
    }
}
