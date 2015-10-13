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
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.test.Validators;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.jaxb.referencing.CC_OperationParameterGroupTest;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.matrix.Matrix3;
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
 * Tests XML (un)marshalling of single operations (conversions and transformations).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@DependsOn({
    DefaultOperationMethodTest.class,
    CC_OperationParameterGroupTest.class,
    org.apache.sis.parameter.ParameterMarshallingTest.class
})
public final strictfp class SingleOperationMarshallingTest extends XMLTestCase {
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
        final Map<String,Object> properties = new HashMap<String,Object>(4);
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

        final OperationMethod method = (OperationMethod) XML.unmarshal(xml);
        verifyMethod(method);
        Validators.validate(method);
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
     * Tests unmarshalling of a defining conversion.
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
        assertNull  ("operationVersion", c.getOperationVersion());

        final GeographicBoundingBox e = (GeographicBoundingBox) getSingleton(c.getDomainOfValidity().getGeographicElements());
        assertEquals("eastBoundLongitude", +180, e.getEastBoundLongitude(), STRICT);
        assertEquals("westBoundLongitude", -180, e.getWestBoundLongitude(), STRICT);
        assertEquals("northBoundLatitude",   84, e.getNorthBoundLatitude(), STRICT);
        assertEquals("southBoundLatitude",  -80, e.getSouthBoundLatitude(), STRICT);

        // This is a defining conversion, so we do not expect CRS.
        assertNull("sourceCRS",        c.getSourceCRS());
        assertNull("targetCRS",        c.getTargetCRS());
        assertNull("interpolationCRS", c.getInterpolationCRS());
        assertNull("mathTransform",    c.getMathTransform());

        // The most difficult part.
        final OperationMethod method = c.getMethod();
        assertNotNull("method", method);
        verifyMethod(method);

        final ParameterValueGroup parameters = c.getParameterValues();
        assertNotNull("parameters", parameters);
        final Iterator<GeneralParameterValue> it = parameters.values().iterator();
        verifyParameter(method, parameters,  -0.0, (ParameterValue<?>) it.next());
        verifyParameter(method, parameters, -90.0, (ParameterValue<?>) it.next());
        assertFalse("Unexpected parameter.", it.hasNext());

        Validators.validate(c);
    }

    /**
     * Verify a parameter value. The descriptor is expected to be the same instance than the descriptors
     * defined in the {@link ParameterValueGroup} and in the {@link OperationMethod}.
     *
     * @param method        The method of the enclosing operation.
     * @param group         The group which contain the given parameter.
     * @param expectedValue The expected parameter value.
     * @param parameter     The parameter to verify.
     */
    private static void verifyParameter(final OperationMethod method, final ParameterValueGroup group,
            final double expectedValue, final ParameterValue<?> parameter)
    {
        final ParameterDescriptor<?> descriptor = parameter.getDescriptor();
        final String name = descriptor.getName().getCode();
        assertSame("parameterValues.descriptor", descriptor,  group.getDescriptor().descriptor(name));
        assertSame("method.descriptor",          descriptor, method.getParameters().descriptor(name));
        assertEquals("value", expectedValue, parameter.doubleValue(), STRICT);
    }

    /**
     * Tests unmarshalling of a transformation.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    @DependsOnMethod("testConversionUnmarshalling")
    public void testTransformationUnmarshalling() throws JAXBException {
        final DefaultTransformation c = unmarshalFile(DefaultTransformation.class, "Transformation.xml");
        assertEquals("name",             "NTF (Paris) to NTF (1)",    c.getName().getCode());
        assertEquals("identifier",       "1763",                      getSingleton(c.getIdentifiers()).getCode());
        assertEquals("scope",            "Change of prime meridian.", String.valueOf(c.getScope()));
        assertEquals("operationVersion", "IGN-Fra",                   c.getOperationVersion());

        final OperationMethod method = c.getMethod();
        assertNotNull("method", method);
        assertEquals ("method.name", "Longitude rotation", method.getName().getCode());
        assertEquals ("method.identifier", "9601", getSingleton(method.getIdentifiers()).getCode());
        assertEquals ("method.formula", "Target_longitude = Source_longitude + longitude_offset.", method.getFormula().getFormula().toString());

        final ParameterDescriptor<?> descriptor = (ParameterDescriptor<?>) getSingleton(method.getParameters().descriptors());
        assertEquals("descriptor.name",       "Longitude offset", descriptor.getName().getCode());
        assertEquals("descriptor.identifier", "8602", getSingleton(descriptor.getIdentifiers()).getCode());
        assertEquals("descriptor.valueClass", Double.class, descriptor.getValueClass());

        final ParameterValueGroup parameters = c.getParameterValues();
        assertNotNull("parameters", parameters);
        assertSame("parameters.descriptors", method.getParameters(), parameters.getDescriptor());

        final ParameterValue<?> parameter = (ParameterValue<?>) getSingleton(parameters.values());
        assertSame  ("parameters.descriptor", descriptor,  parameter.getDescriptor());
        assertEquals("parameters.unit",       NonSI.GRADE, parameter.getUnit());
        assertEquals("parameters.value",      2.5969213,   parameter.getValue());

        final CoordinateReferenceSystem sourceCRS = c.getSourceCRS();
        assertInstanceOf("sourceCRS",            GeodeticCRS.class,  sourceCRS);
        assertEquals    ("sourceCRS.name",       "NTF (Paris)",      sourceCRS.getName().getCode());
        assertEquals    ("sourceCRS.scope",      "Geodetic survey.", sourceCRS.getScope().toString());
        assertEquals    ("sourceCRS.identifier", "4807",             getSingleton(sourceCRS.getIdentifiers()).getCode());

        final CoordinateReferenceSystem targetCRS = c.getTargetCRS();
        assertInstanceOf("targetCRS",            GeodeticCRS.class,  targetCRS);
        assertEquals    ("targetCRS.name",       "NTF",              targetCRS.getName().getCode());
        assertEquals    ("targetCRS.scope",      "Geodetic survey.", targetCRS.getScope().toString());
        assertEquals    ("targetCRS.identifier", "4275",             getSingleton(targetCRS.getIdentifiers()).getCode());

        final MathTransform tr = c.getMathTransform();
        assertInstanceOf("mathTransform", LinearTransform.class, tr);
        assertMatrixEquals("mathTransform.matrix",
                new Matrix3(1, 0, 0,
                            0, 1, 2.33722917,
                            0, 0, 1), ((LinearTransform) tr).getMatrix(), STRICT);

        Validators.validate(c);
    }
}
