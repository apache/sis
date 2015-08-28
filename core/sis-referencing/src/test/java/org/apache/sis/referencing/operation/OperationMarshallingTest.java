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
package org.apache.sis.referencing.operation;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import org.opengis.parameter.GeneralParameterDescriptor;
import javax.xml.bind.JAXBException;
import javax.measure.unit.NonSI;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.XML;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.metadata.iso.citation.Citations.EPSG;


/**
 * Tests XML (un)marshalling of various coordinate operation objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    DefaultOperationMethodTest.class,
    org.apache.sis.parameter.ParameterMarshallingTest.class
})
public final strictfp class OperationMarshallingTest extends XMLTestCase {
    /**
     * Creates the test operation method.
     */
    private static DefaultOperationMethod createMercatorMethod() {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setCodeSpace(EPSG, "EPSG").setRequired(true);
        ParameterDescriptor<?>[] parameters = {
            builder.addName("Latitude of natural origin" ).create(0, NonSI.DEGREE_ANGLE),
            builder.addName("Longitude of natural origin").create(0, NonSI.DEGREE_ANGLE)
            // There is more parameters for a Mercator projection, but 2 is enough for this test.
        };
        builder.addName(null, "Mercator");
        final ParameterDescriptorGroup descriptor = builder.createGroup(parameters);
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(DefaultOperationMethod.NAME_KEY, descriptor.getName());
        properties.put(DefaultOperationMethod.FORMULA_KEY, new DefaultFormula("See EPSG guide."));
        return new DefaultOperationMethod(properties, 2, 2, descriptor);
    }

    /**
     * Tests (un)marshalling of an operation method.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testOperationMethod() throws JAXBException {
        final String xml = XML.marshal(createMercatorMethod());
        assertXmlEquals("<gml:OperationMethod xmlns:gml=\"" + Namespaces.GML + "\">\n"
                      + "  <gml:name>Mercator</gml:name>\n"
                      + "  <gml:formula>See EPSG guide.</gml:formula>\n"
                      + "  <gml:sourceDimensions>2</gml:sourceDimensions>\n"
                      + "  <gml:targetDimensions>2</gml:targetDimensions>\n"
                      + "  <gml:parameter>\n"
                      + "    <gml:OperationParameter gml:id=\"LatitudeOfNaturalOrigin\">\n"
                      + "      <gml:name codeSpace=\"EPSG\">Latitude of natural origin</gml:name>\n"
                      + "    </gml:OperationParameter>\n"
                      + "  </gml:parameter>\n"
                      + "  <gml:parameter>\n"
                      + "    <gml:OperationParameter gml:id=\"LongitudeOfNaturalOrigin\">\n"
                      + "      <gml:name codeSpace=\"EPSG\">Longitude of natural origin</gml:name>\n"
                      + "    </gml:OperationParameter>\n"
                      + "  </gml:parameter>\n"
                      + "</gml:OperationMethod>", xml, "xmlns:*");

        verifyMethod((DefaultOperationMethod) XML.unmarshal(xml));
    }

    /**
     * Verifies the unmarshalled parameter descriptors.
     */
    private static void verifyMethod(final DefaultOperationMethod method) {
        assertIdentifierEquals("name", null, null, null, "Mercator", method.getName());
        assertEquals("formula", "See EPSG guide.", method.getFormula().getFormula().toString());
        assertEquals("sourceDimensions", Integer.valueOf(2), method.getSourceDimensions());
        assertEquals("targetDimensions", Integer.valueOf(2), method.getTargetDimensions());
        final ParameterDescriptorGroup parameters = method.getParameters();
        assertEquals("parameters.name", method.getName(), parameters.getName());
        final Iterator<GeneralParameterDescriptor> it = parameters.descriptors().iterator();
        verifyIncompleteDescriptor("Latitude of natural origin",  it.next());
        verifyIncompleteDescriptor("Longitude of natural origin", it.next());
        assertFalse("Unexpected parameter.", it.hasNext());
    }

    /**
     * Verifies that the given parameter descriptor has the expected EPSG name. This method does not
     * verify that {@link ParameterDescriptor#getValueClass()} returns {@code Double.class}, because
     * this information is not known to {@code OperationMethod}.
     *
     * @param name       The expected EPSG name.
     * @param descriptor The parameter descriptor to verify.
     */
    private static void verifyIncompleteDescriptor(final String name, final GeneralParameterDescriptor descriptor) {
        assertIdentifierEquals("name", "##unrestricted", "EPSG", null, name, descriptor.getName());
        assertEquals("maximumOccurs", 1, descriptor.getMaximumOccurs());
        assertEquals("minimumOccurs", 1, descriptor.getMinimumOccurs());
    }
}
