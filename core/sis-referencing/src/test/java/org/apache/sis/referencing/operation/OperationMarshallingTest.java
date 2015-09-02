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
import javax.xml.bind.JAXBException;
import javax.measure.unit.NonSI;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.jaxb.referencing.CC_OperationParameterGroupTest;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.XML;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.metadata.iso.citation.Citations.EPSG;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.test.ReferencingAssert.*;


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
    CC_OperationParameterGroupTest.class,
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
            builder.addIdentifier("8801").addName("Latitude of natural origin" ).create(0, NonSI.DEGREE_ANGLE),
            builder.addIdentifier("8802").addName("Longitude of natural origin").create(0, NonSI.DEGREE_ANGLE)
            // There is more parameters for a Mercator projection, but 2 is enough for this test.
        };
        builder.addName(null, "Mercator (1SP)");
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
        assertXmlEquals("<gml:OperationMethod xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                        "  <gml:name>Mercator (1SP)</gml:name>\n" +
                        "  <gml:formula>See EPSG guide.</gml:formula>\n" +
                        "  <gml:sourceDimensions>2</gml:sourceDimensions>\n" +
                        "  <gml:targetDimensions>2</gml:targetDimensions>\n" +
                        "  <gml:parameter>\n" +
                        "    <gml:OperationParameter gml:id=\"epsg-parameter-8801\">\n" +
                        "      <gml:identifier codeSpace=\"IOGP\">urn:ogc:def:parameter:EPSG::8801</gml:identifier>\n" +
                        "      <gml:name codeSpace=\"EPSG\">Latitude of natural origin</gml:name>\n" +
                        "    </gml:OperationParameter>\n" +
                        "  </gml:parameter>\n" +
                        "  <gml:parameter>\n" +
                        "    <gml:OperationParameter gml:id=\"epsg-parameter-8802\">\n" +
                        "      <gml:identifier codeSpace=\"IOGP\">urn:ogc:def:parameter:EPSG::8802</gml:identifier>\n" +
                        "      <gml:name codeSpace=\"EPSG\">Longitude of natural origin</gml:name>\n" +
                        "    </gml:OperationParameter>\n" +
                        "  </gml:parameter>\n" +
                        "</gml:OperationMethod>", xml, "xmlns:*");

        verifyMethod((DefaultOperationMethod) XML.unmarshal(xml));
    }

    /**
     * Verifies the unmarshalled parameter descriptors.
     */
    private static void verifyMethod(final OperationMethod method) {
        assertIdentifierEquals("name", null, null, null, "Mercator (1SP)", method.getName());
        assertEquals("formula", "See EPSG guide.", method.getFormula().getFormula().toString());
        assertEquals("sourceDimensions", Integer.valueOf(2), method.getSourceDimensions());
        assertEquals("targetDimensions", Integer.valueOf(2), method.getTargetDimensions());
        final ParameterDescriptorGroup parameters = method.getParameters();
        assertEquals("parameters.name", "Mercator (1SP)", parameters.getName().getCode());
        final Iterator<GeneralParameterDescriptor> it = parameters.descriptors().iterator();
        CC_OperationParameterGroupTest.verifyMethodParameter(Mercator1SP.LATITUDE_OF_ORIGIN,  (ParameterDescriptor<?>) it.next());
        CC_OperationParameterGroupTest.verifyMethodParameter(Mercator1SP.LONGITUDE_OF_ORIGIN, (ParameterDescriptor<?>) it.next());
        assertFalse("Unexpected parameter.", it.hasNext());
    }

    /**
     * Tests unmarshalling of a conversion.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    @DependsOnMethod("testOperationMethod")
    public void testConversionUnmarshalling() throws JAXBException {
        final DefaultConversion c = unmarshalFile(DefaultConversion.class, "Conversion.xml");
        assertEquals("name", "World Mercator", c.getName().getCode());
        assertEquals("identifier", "3395", getSingleton(c.getIdentifiers()).getCode());
        assertEquals("scope", "Very small scale mapping.", String.valueOf(c.getScope()));

        final GeographicBoundingBox e = (GeographicBoundingBox) getSingleton(c.getDomainOfValidity().getGeographicElements());
        assertEquals("eastBoundLongitude", +180, e.getEastBoundLongitude(), STRICT);
        assertEquals("westBoundLongitude", -180, e.getWestBoundLongitude(), STRICT);
        assertEquals("northBoundLatitude",   84, e.getNorthBoundLatitude(), STRICT);
        assertEquals("southBoundLatitude",  -80, e.getSouthBoundLatitude(), STRICT);

        // The most difficult part.
        verifyMethod(c.getMethod());
    }
}
