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

import java.util.HashMap;
import java.util.Iterator;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.provider.Mercator1SP;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.measure.Units;
import org.apache.sis.system.Loggers;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.XML;
import static org.apache.sis.metadata.iso.citation.Citations.EPSG;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.xml.bind.referencing.CC_OperationParameterGroupTest;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertSingletonAuthorityCode;
import static org.apache.sis.test.Assertions.assertSingletonScope;
import static org.apache.sis.test.Assertions.assertSingletonDomainOfValidity;
import static org.apache.sis.referencing.Assertions.assertMatrixEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertIdentifierEquals;


/**
 * Tests XML (un)marshalling of single operations (conversions and transformations).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class SingleOperationMarshallingTest extends TestCase.WithLogs {
    /**
     * Creates a new test case.
     */
    public SingleOperationMarshallingTest() {
        super(Loggers.XML);
    }

    /**
     * Opens the stream to the XML file in this package containing an operation definition.
     *
     * @param  transformation  {@code true} for a transformation or {@code false} for a conversion.
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile(final boolean transformation) {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return SingleOperationMarshallingTest.class.getResourceAsStream(
                transformation ? "Transformation.xml" : "Conversion.xml");
    }

    /**
     * Creates the test operation method.
     */
    private static DefaultOperationMethod createMercatorMethod() {
        final var builder = new ParameterBuilder();
        builder.setCodeSpace(EPSG, "EPSG").setRequired(true);
        ParameterDescriptor<?>[] parameters = {
            builder.addIdentifier("8801").addName("Latitude of natural origin" ).create(0, Units.DEGREE),
            builder.addIdentifier("8802").addName("Longitude of natural origin").create(0, Units.DEGREE)
            // There is more parameters for a Mercator projection, but 2 is enough for this test.
        };
        builder.addName(null, "Mercator (1SP)");
        final ParameterDescriptorGroup descriptor = builder.createGroup(parameters);
        final var properties = new HashMap<String,Object>(4);
        properties.put(DefaultOperationMethod.NAME_KEY, descriptor.getName());
        properties.put(DefaultOperationMethod.FORMULA_KEY, new DefaultFormula("See EPSG guide."));
        return new DefaultOperationMethod(properties, descriptor);
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

        final var method = (OperationMethod) XML.unmarshal(xml);
        verifyMethod(method);
        Validators.validate(method);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies the unmarshalled parameter descriptors.
     */
    private static void verifyMethod(final OperationMethod method) {
        assertIdentifierEquals(null, null, null, "Mercator (1SP)", method.getName(), "name");
        assertEquals("See EPSG guide.", method.getFormula().getFormula().toString(), "formula");
        final ParameterDescriptorGroup parameters = method.getParameters();
        assertEquals("Mercator (1SP)", parameters.getName().getCode(), "parameters.name");
        final Iterator<GeneralParameterDescriptor> it = parameters.descriptors().iterator();
        CC_OperationParameterGroupTest.verifyMethodParameter(Mercator1SP.LATITUDE_OF_ORIGIN,  (ParameterDescriptor<?>) it.next());
        CC_OperationParameterGroupTest.verifyMethodParameter(Mercator1SP.LONGITUDE_OF_ORIGIN, (ParameterDescriptor<?>) it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Tests unmarshalling of a defining conversion.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testConversionUnmarshalling() throws JAXBException {
        final DefaultConversion c = unmarshalFile(DefaultConversion.class, openTestFile(false));
        assertEquals("World Mercator", c.getName().getCode(), "name");
        assertEquals("3395", assertSingletonAuthorityCode(c), "identifier");
        assertEquals("Very small scale mapping.", assertSingletonScope(c), "scope");
        assertTrue  (c.getOperationVersion().isEmpty(), "operationVersion");

        final GeographicBoundingBox e = assertSingletonDomainOfValidity(c);
        assertEquals(+180, e.getEastBoundLongitude(), "eastBoundLongitude");
        assertEquals(-180, e.getWestBoundLongitude(), "westBoundLongitude");
        assertEquals(  84, e.getNorthBoundLatitude(), "northBoundLatitude");
        assertEquals( -80, e.getSouthBoundLatitude(), "southBoundLatitude");

        // This is a defining conversion, so we do not expect CRS.
        assertNull(c.getSourceCRS(), "sourceCRS");
        assertNull(c.getTargetCRS(), "targetCRS");
        assertTrue(c.getInterpolationCRS().isEmpty(), "interpolationCRS");
        assertNull(c.getMathTransform(), "mathTransform");

        // The most difficult part.
        final OperationMethod method = c.getMethod();
        assertNotNull(method, "method");
        verifyMethod(method);

        final ParameterValueGroup parameters = c.getParameterValues();
        assertNotNull(parameters, "parameters");
        final Iterator<GeneralParameterValue> it = parameters.values().iterator();
        verifyParameter(method, parameters,  -0.0, (ParameterValue<?>) it.next());
        verifyParameter(method, parameters, -90.0, (ParameterValue<?>) it.next());
        assertFalse(it.hasNext());
        /*
         * Validate object, then discard warnings caused by duplicated identifiers.
         * Those duplications are intentional, see comment in `Conversion.xml`.
         */
        Validators.validate(c);
        loggings.assertNextLogContains("EPSG::8801");
        loggings.assertNextLogContains("EPSG::8802");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verify a parameter value. The descriptor is expected to be the same instance as the descriptors
     * defined in the {@link ParameterValueGroup} and in the {@link OperationMethod}.
     *
     * @param  method         the method of the enclosing operation.
     * @param  group          the group which contain the given parameter.
     * @param  expectedValue  the expected parameter value.
     * @param  parameter      the parameter to verify.
     */
    private static void verifyParameter(final OperationMethod method, final ParameterValueGroup group,
            final double expectedValue, final ParameterValue<?> parameter)
    {
        final ParameterDescriptor<?> descriptor = parameter.getDescriptor();
        final String name = descriptor.getName().getCode();
        assertSame(descriptor,  group.getDescriptor().descriptor(name), "parameterValues.descriptor");
        assertSame(descriptor, method.getParameters().descriptor(name), "method.descriptor");
        assertEquals(expectedValue, parameter.doubleValue(), "value");
    }

    /**
     * Tests unmarshalling of a transformation.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testTransformationUnmarshalling() throws JAXBException {
        final DefaultTransformation c = unmarshalFile(DefaultTransformation.class, openTestFile(true));
        assertEquals("NTF (Paris) to NTF (1)", c.getName().getCode(), "name");
        assertEquals("1763", assertSingletonAuthorityCode(c), "identifier");
        assertEquals("Change of prime meridian.", assertSingletonScope(c), "scope");
        assertEquals("IGN-Fra", c.getOperationVersion().get(), "operationVersion");

        final OperationMethod method = c.getMethod();
        assertNotNull(method, "method");
        assertEquals("Longitude rotation", method.getName().getCode(), "method.name");
        assertEquals("9601", assertSingletonAuthorityCode(method), "method.identifier");
        assertEquals("Target_longitude = Source_longitude + longitude_offset.", method.getFormula().getFormula().toString(), "method.formula");

        final var descriptor = assertInstanceOf(ParameterDescriptor.class, assertSingleton(method.getParameters().descriptors()));
        assertEquals("Longitude offset", descriptor.getName().getCode(), "descriptor.name");
        assertEquals("8602", assertSingletonAuthorityCode(descriptor), "descriptor.identifier");
        assertEquals(Double.class, descriptor.getValueClass(), "descriptor.valueClass");

        final ParameterValueGroup parameters = c.getParameterValues();
        assertNotNull(parameters, "parameters");
        assertSame(method.getParameters(), parameters.getDescriptor(), "parameters.descriptors");

        final var parameter = assertInstanceOf(ParameterValue.class, assertSingleton(parameters.values()));
        assertSame  (descriptor, parameter.getDescriptor(), "parameters.descriptor");
        assertEquals(Units.GRAD, parameter.getUnit(),       "parameters.unit");
        assertEquals(2.5969213,  parameter.getValue(),      "parameters.value");

        final CoordinateReferenceSystem sourceCRS = c.getSourceCRS();
        assertInstanceOf(GeodeticCRS.class, sourceCRS, "sourceCRS");
        assertEquals("NTF (Paris)", sourceCRS.getName().getCode(), "sourceCRS.name");
        assertEquals("Geodetic survey.", assertSingletonScope(sourceCRS), "sourceCRS.scope");
        assertEquals("4807", assertSingletonAuthorityCode(sourceCRS), "sourceCRS.identifier");

        final CoordinateReferenceSystem targetCRS = c.getTargetCRS();
        assertInstanceOf(GeodeticCRS.class,  targetCRS, "targetCRS");
        assertEquals("NTF", targetCRS.getName().getCode(), "targetCRS.name");
        assertEquals("Geodetic survey.", assertSingletonScope(targetCRS), "targetCRS.scope");
        assertEquals("4275", assertSingletonAuthorityCode(targetCRS), "targetCRS.identifier");

        assertMatrixEquals(
                new Matrix3(1, 0, 0,
                            0, 1, 2.33722917,
                            0, 0, 1),
                c.getMathTransform(),
                "mathTransform.matrix");
        /*
         * Validate object, then discard warnings caused by duplicated identifiers.
         * Those duplications are intentional, see comment in `Transformation.xml`.
         */
        Validators.validate(c);
        loggings.assertNextLogContains("EPSG::8602");
        loggings.assertNoUnexpectedLog();
    }
}
