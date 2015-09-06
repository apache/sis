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
package org.apache.sis.internal.jaxb.referencing;

import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Collections;
import javax.xml.bind.JAXBException;
import javax.measure.unit.NonSI;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.xml.Namespaces;
import org.junit.Test;

import static org.apache.sis.metadata.iso.citation.Citations.EPSG;
import static org.apache.sis.internal.jaxb.referencing.CC_GeneralOperationParameter.DEFAULT_OCCURRENCE;
import static org.junit.Assert.*;


/**
 * Tests {@link CC_OperationParameterGroup} static methods.
 * Also opportunistically tests {@link CC_OperationMethod} because we use the same data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-290">SIS-290</a>
 */
@DependsOn(CC_GeneralOperationParameterTest.class)
public final strictfp class CC_OperationParameterGroupTest extends XMLTestCase {
    /**
     * The remark to associate to the "Latitude of natural origin" parameter.
     * Should be different than the comment stored in {@link Mercator1SP} in
     * order to test parameter merges.
     */
    private static final String REMARK = "Always zero for this projection.";

    /**
     * Creates a parameter descriptor group as unmarshalled by JAXB, without {@code valueClass}.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    private DefaultParameterDescriptorGroup unmarshal() throws JAXBException {
        return unmarshal(DefaultParameterDescriptorGroup.class,
                "<gml:OperationParameterGroup xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:name>Mercator (1SP)</gml:name>\n" +
                "  <gml:parameter>\n" +
                "    <gml:OperationParameter gml:id=\"epsg-parameter-8801\">\n" +
                "      <gml:identifier codeSpace=\"IOGP\">urn:ogc:def:parameter:EPSG::8801</gml:identifier>\n" +
                "      <gml:name codeSpace=\"EPSG\">Latitude of natural origin</gml:name>\n" +
                "      <gml:remarks>" + REMARK + "</gml:remarks>\n" +
                "    </gml:OperationParameter>\n" +
                "  </gml:parameter>\n" +
                "  <gml:parameter>\n" +
                "    <gml:OperationParameter gml:id=\"epsg-parameter-8802\">\n" +
                "      <gml:identifier codeSpace=\"IOGP\">urn:ogc:def:parameter:EPSG::8802</gml:identifier>\n" +
                "      <gml:name codeSpace=\"EPSG\">Longitude of natural origin</gml:name>\n" +
                "    </gml:OperationParameter>\n" +
                "  </gml:parameter>\n" +
                // There is more parameters in a Mercator projection, but 2 is enough for this test.
                "</gml:OperationParameterGroup>");
    }

    /**
     * Creates the parameter descriptors equivalent to the result of {@link #unmarshal()}
     * but with value class, units and default values.
     *
     * @param remarks Remarks to associate to the latitude parameter, or {@code null} if none.
     */
    private static ParameterDescriptor<?>[] create(final String remarks) {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setCodeSpace(EPSG, "EPSG").setRequired(true);
        return new ParameterDescriptor<?>[] {
            builder.addIdentifier("8801").addName("Latitude of natural origin") .setRemarks(remarks).create(0, NonSI.DEGREE_ANGLE),
            builder.addIdentifier("8802").addName("Longitude of natural origin").create(0, NonSI.DEGREE_ANGLE),
        };
    }

    /**
     * Tests the substitution of unmarshalled descriptors by more complete descriptors.
     * No merging should be done in this test.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    public void testSubtitution() throws JAXBException {
        final ParameterDescriptor<?>[]         expected   = create(REMARK);
        final List<GeneralParameterDescriptor> fromXML    = unmarshal().descriptors();
        final List<GeneralParameterDescriptor> fromValues = UnmodifiableArrayList.<GeneralParameterDescriptor>wrap(expected);
        final Map<GeneralParameterDescriptor,GeneralParameterDescriptor> replacements =
                new IdentityHashMap<GeneralParameterDescriptor,GeneralParameterDescriptor>(4);
        final GeneralParameterDescriptor[] merged = CC_OperationParameterGroup.merge(fromXML,
                fromValues.toArray(new GeneralParameterDescriptor[fromValues.size()]), replacements);

        assertTrue("Expected no replacement.", replacements.isEmpty());
        assertEquals("Number of parameters", 2, merged.length);
        assertNotSame(expected, merged);
        assertArrayEquals(expected, merged);
        for (int i=0; i<merged.length; i++) {
            assertSame(expected[i], merged[i]);     // Not just equals, but actually same instance.
        }
    }

    /**
     * Tests the merge of unmarshalled descriptors with more complete descriptors.
     * This operation is expected to create new descriptor instances as a result of the merges.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    @DependsOnMethod("testSubtitution")
    public void testMerge() throws JAXBException {
        final ParameterDescriptorGroup fromXML = unmarshal();
        final ParameterDescriptor<?>[] fromValues = create(null);
        final Map<GeneralParameterDescriptor,GeneralParameterDescriptor> replacements =
                new IdentityHashMap<GeneralParameterDescriptor,GeneralParameterDescriptor>(4);
        final GeneralParameterDescriptor[] merged = CC_OperationParameterGroup.merge(fromXML.descriptors(), fromValues.clone(), replacements);
        assertNotSame(fromValues, merged);
        /*
         * "Longitude of natural origin" parameter should be the same.
         */
        assertEquals("Number of parameters", 2, merged.length);
        assertSame("Longitude of natural origin", fromValues[1], merged[1]);
        /*
         * "Latitude of natural origin" should be a new parameter, because we merged the remarks from the
         * 'fromXML' descriptor with value class, unit and default value from the 'fromValue' descriptor.
         */
        final GeneralParameterDescriptor    incomplete = fromXML.descriptors().get(0);
        final DefaultParameterDescriptor<?> fromValue  = (DefaultParameterDescriptor<?>) fromValues[0];
        final DefaultParameterDescriptor<?> complete   = (DefaultParameterDescriptor<?>) merged[0];
        assertNotSame("Latitude of natural origin", incomplete,  complete);
        assertNotSame("Latitude of natural origin", fromValue,   complete);
        assertSame   ("name",        fromValue .getName(),       complete.getName());
        assertSame   ("remarks",     incomplete.getRemarks(),    complete.getRemarks());
        assertEquals ("valueClass",  Double.class,               complete.getValueClass());
        assertSame   ("valueDomain", fromValue.getValueDomain(), complete.getValueDomain());
        /*
         * All references to 'fromValue' will need to be replaced by references to 'complete'.
         */
        assertEquals("replacements", Collections.singletonMap(fromValue, complete), replacements);
    }

    /**
     * Tests {@link CC_OperationMethod#group(Identifier, GeneralParameterDescriptor[])}.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    @DependsOnMethod("testMerge")
    public void testGroup() throws JAXBException {
        // From XML
        ParameterDescriptorGroup group = unmarshal();
        List<GeneralParameterDescriptor> descriptors = group.descriptors();

        // Merge with the parameters defined in Mercator1SP class
        group = CC_OperationMethod.group(group.getName(), descriptors.toArray(new GeneralParameterDescriptor[descriptors.size()]));
        descriptors = group.descriptors();

        assertSame("name", group.getName(), group.getName());
        assertEquals("descriptors.size", 2, descriptors.size());
        verifyMethodParameter(Mercator1SP.LATITUDE_OF_ORIGIN, REMARK, (ParameterDescriptor<?>) descriptors.get(0));
        verifyMethodParameter(Mercator1SP.LONGITUDE_OF_ORIGIN,  null, (ParameterDescriptor<?>) descriptors.get(1));
    }

    /**
     * Verifies the properties of an operation method parameter.
     *
     * @param expected A parameter descriptor containing the expected properties (except remarks).
     * @param actual   The parameter descriptor to verify.
     */
    public static void verifyMethodParameter(final ParameterDescriptor<?> expected,
                                             final ParameterDescriptor<?> actual)
    {
        assertEquals("name",          expected.getName(),         actual.getName());
        assertEquals("valueClass",    expected.getValueClass(),   actual.getValueClass());
        assertEquals("validValues",   expected.getValidValues(),  actual.getValidValues());
        assertEquals("unit",          expected.getUnit(),         actual.getUnit());
        assertEquals("minimumOccurs", DEFAULT_OCCURRENCE,         actual.getMinimumOccurs());
        assertEquals("maximumOccurs", DEFAULT_OCCURRENCE,         actual.getMaximumOccurs());
    }

    /**
     * Same verification than {@link #verifyMethodParameter(ParameterDescriptor, ParameterDescriptor)}, but stricter.
     *
     * @param expected A parameter descriptor containing the expected properties (except remarks).
     * @param remarks  The expected remarks, or {@code null} for fetching this information from {@code expected}.
     * @param actual   The parameter descriptor to verify.
     */
    private static void verifyMethodParameter(final ParameterDescriptor<?> expected,
            final String remarks, final ParameterDescriptor<?> actual)
    {
        verifyMethodParameter(expected, actual);
        assertSame("name",         expected.getName(),         actual.getName());
        assertSame("minimumValue", expected.getMinimumValue(), actual.getMinimumValue());
        assertSame("maximumValue", expected.getMaximumValue(), actual.getMaximumValue());
        assertSame("defaultValue", expected.getDefaultValue(), actual.getDefaultValue());
        if (remarks != null) {
            assertEquals("remarks", remarks, actual.getRemarks().toString());
        } else {
            assertSame("remarks", expected.getRemarks(), actual.getRemarks());
        }
    }
}
