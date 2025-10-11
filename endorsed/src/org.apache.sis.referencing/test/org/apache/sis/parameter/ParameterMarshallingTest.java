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
package org.apache.sis.parameter;

import java.util.Map;
import java.util.Iterator;
import java.util.Objects;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import jakarta.xml.bind.JAXBException;
import javax.measure.Unit;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.system.Loggers;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.XML;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.referencing.Assertions.assertAliasTipEquals;
import static org.apache.sis.referencing.Assertions.assertEpsgNameAndIdentifierEqual;


/**
 * Tests XML (un)marshalling of {@link DefaultParameterValue}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ParameterMarshallingTest extends TestCase.WithLogs {
    /**
     * Creates a new test case.
     */
    public ParameterMarshallingTest() {
        super(Loggers.XML);
    }

    /**
     * Enumeration of test files.
     */
    private enum TestFile {
        /** GML document of a parameter descriptor group. */
        DESCRIPTOR("ParameterDescriptorGroup.xml"),

        /** GML document of a parameter value group. */
        VALUE("ParameterValueGroup.xml"),

        /** GML document containing duplicated parameters. */
        DUPLICATED("DuplicatedParameters.xml");

        /** Relative filename of the GML document. */
        private final String filename;

        /** Creates an enumeration for a GML document in the specified file. */
        private TestFile(final String filename) {
            this.filename = filename;
        }

        /** Opens the stream to the XML file containing the test document. */
        final InputStream openTestFile() {
            // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
            return ParameterMarshallingTest.class.getResourceAsStream(filename);
        }
    }

    /**
     * Creates a parameter value for marshalling test.
     */
    private static <T> DefaultParameterValue<T> create(final Class<T> type, final Range<?> valueDomain) {
        return new DefaultParameterValue<>(new DefaultParameterDescriptor<>(
                Map.of(DefaultParameterDescriptor.NAME_KEY, "A parameter of type " + type.getSimpleName()),
                1, 1, type, valueDomain, null, null));
    }

    /**
     * Marshals the given object, then unmarshals it and compare with the original value.
     *
     * @param  parameter  the parameter to marshal.
     * @param  expected   the expected XML (ignoring {@code xmlns}).
     */
    private static void testMarshallAndUnmarshall(final DefaultParameterValue<?> parameter, final String expected)
            throws JAXBException
    {
        final String xml = XML.marshal(parameter);
        assertXmlEquals(expected, xml, "xmlns:*");
        final DefaultParameterValue<?> r = (DefaultParameterValue<?>) XML.unmarshal(xml);
        if (!Objects.deepEquals(parameter.getValue(), r.getValue())) {
            /*
             * If we enter in this block, then the line below should always fail.
             * But we use this assertion for getting a better error message.
             */
            assertEquals(parameter.getValue(), r.getValue(), "value");
        }
        assertEquals(parameter.getUnit(), r.getUnit(), "unit");
        /*
         * Verify the descriptor, especially the 'valueClass' property. That property is not part of GML,
         * so Apache SIS has to rely on some tricks for finding this information (see CC_OperationParameter).
         */
        final ParameterDescriptor<?> reference = parameter.getDescriptor();
        final ParameterDescriptor<?> descriptor = r.getDescriptor();
        assertNotNull(                              descriptor,                    "descriptor");
        assertEquals (reference.getName(),          descriptor.getName(),          "descriptor.name");
        assertEquals (reference.getUnit(),          descriptor.getUnit(),          "descriptor.unit");
        assertEquals (reference.getValueClass(),    descriptor.getValueClass(),    "descriptor.valueClass");
        assertEquals (reference.getMinimumOccurs(), descriptor.getMinimumOccurs(), "descriptor.minimumOccurs");
        assertEquals (reference.getMaximumOccurs(), descriptor.getMaximumOccurs(), "descriptor.maximumOccurs");
        Validators.validate(r);
    }

    /**
     * Tests (un)marshalling of a parameter descriptor.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testDescriptor() throws JAXBException {
        final DefaultParameterDescriptor<Double> descriptor = new DefaultParameterDescriptor<>(
                Map.of(DefaultParameterDescriptor.NAME_KEY, "A descriptor"),
                0, 1, Double.class, null, null, null);

        final String xml = XML.marshal(descriptor);
        assertXmlEquals(
                "<gml:OperationParameter xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:name>A descriptor</gml:name>\n"
              + "  <gml:minimumOccurs>0</gml:minimumOccurs>\n"
              + "</gml:OperationParameter>", xml, "xmlns:*");

        final var r = (DefaultParameterDescriptor<?>) XML.unmarshal(xml);
        assertEquals("A descriptor", r.getName().getCode(), "name");
        assertEquals(0, r.getMinimumOccurs(), "minimumOccurs");
        assertEquals(1, r.getMaximumOccurs(), "maximumOccurs");
        /*
         * A DefaultParameterDescriptor with null 'valueClass' is illegal, but there is no way we can guess
         * this information if the <gml:OperationParameter> element was not a child of <gml:ParameterValue>.
         * The current implementation leaves 'valueClass' to null despite being illegal. This behavior may
         * change in any future Apache SIS version.
         */
        assertNull(r.getValueDomain(), "valueDomain");
        assertNull(r.getValueClass(),  "valueClass");               // May change in any future SIS release.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests (un)marshalling of a parameter with a string value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testStringValue() throws JAXBException {
        final DefaultParameterValue<String> parameter = create(String.class, null);
        parameter.setValue("A string value");
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:stringValue>A string value</gml:stringValue>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type String</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests (un)marshalling of a parameter with a URI value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     * @throws URISyntaxException should never happen.
     */
    @Test
    public void testURIValue() throws JAXBException, URISyntaxException {
        final DefaultParameterValue<URI> parameter = create(URI.class, null);
        parameter.setValue(new URI("http://www.opengis.org"));
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:valueFile>http://www.opengis.org</gml:valueFile>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type URI</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests (un)marshalling of a parameter with an integer value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testBooleanValue() throws JAXBException {
        final DefaultParameterValue<Boolean> parameter = create(Boolean.class, null);
        parameter.setValue(Boolean.TRUE);
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:booleanValue>true</gml:booleanValue>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type Boolean</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests (un)marshalling of a parameter with an integer value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testIntegerValue() throws JAXBException {
        final DefaultParameterValue<Integer> parameter = create(Integer.class, null);
        parameter.setValue(2000);
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:integerValue>2000</gml:integerValue>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type Integer</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests (un)marshalling of a parameter with a list of integer values.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testIntegerValueList() throws JAXBException {
        final DefaultParameterValue<int[]> parameter = create(int[].class, null);
        parameter.setValue(new int[] {101, 105, 208});
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:integerValueList>101 105 208</gml:integerValueList>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type int[]</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests (un)marshalling of a parameter with a floating point value.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testDoubleValue() throws JAXBException {
        final DefaultParameterValue<Double> parameter = create(Double.class,
                new MeasurementRange<>(Double.class, null, false, null, false, Units.METRE));
        parameter.setValue(3000, Units.METRE);
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:value uom=\"urn:ogc:def:uom:EPSG::9001\">3000.0</gml:value>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type Double</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests (un)marshalling of a parameter with a list of floating point values.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testValueList() throws JAXBException {
        final DefaultParameterValue<double[]> parameter = create(double[].class,
                new MeasurementRange<>(Double.class, null, false, null, false, Units.METRE));
        parameter.setValue(new double[] {203, 207, 204}, Units.METRE);
        testMarshallAndUnmarshall(parameter,
                "<gml:ParameterValue xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:valueList uom=\"urn:ogc:def:uom:EPSG::9001\">203.0 207.0 204.0</gml:valueList>\n"
              + "    <gml:operationParameter>"
              + "      <gml:OperationParameter>"
              + "        <gml:name>A parameter of type double[]</gml:name>"
              + "      </gml:OperationParameter>"
              + "    </gml:operationParameter>"
              + "</gml:ParameterValue>");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests (un)marshalling of a parameter descriptor group.
     *
     * @throws JAXBException if an error occurred during marshalling or unmarshalling.
     */
    @Test
    public void testDescriptorGroup() throws JAXBException {
        // Test marshalling.
        assertMarshalEqualsFile(TestFile.DESCRIPTOR.openTestFile(),
                ParameterFormatTest.createMercatorParameters(), "xmlns:*", "xsi:schemaLocation");

        // Test unmarshalling.
        verifyDescriptorGroup(unmarshalFile(DefaultParameterDescriptorGroup.class, TestFile.DESCRIPTOR.openTestFile()));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies that the properties of the given parameter descriptor group are the expected properties,
     * ignoring the {@code valueClass} and {@code unit} (because not part of GML schema for descriptors).
     *
     * @param group The descriptor group to verify.
     */
    private static void verifyDescriptorGroup(final ParameterDescriptorGroup group) {
        assertEpsgNameAndIdentifierEqual("Mercator (variant A)", 9804, group);

        // Verify the ParameterDescriptors properties.
        final Iterator<GeneralParameterDescriptor> it = group.descriptors().iterator();
        verifyDescriptor(8801, "Latitude of natural origin",     "latitude_of_origin", true,  it.next());
        verifyDescriptor(8802, "Longitude of natural origin",    "central_meridian",   true,  it.next());
        verifyDescriptor(8805, "Scale factor at natural origin", "scale_factor",       true,  it.next());
        verifyDescriptor(8806, "False easting",                  "false_easting",      false, it.next());
        verifyDescriptor(8807, "False northing",                 "false_northing",     false, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Verifies that the given parameter descriptor has the expected EPSG code, name and OGC alias.
     *
     * @param  code        the expected EPSG code.
     * @param  name        the expected EPSG name.
     * @param  alias       the expected OGC alias.
     * @param  required    {@code true} if the parameter should be mandatory, or {@code false} if optional.
     * @param  descriptor  the parameter descriptor to verify.
     */
    private static void verifyDescriptor(final int code, final String name, final String alias,
            final boolean required, final GeneralParameterDescriptor descriptor)
    {
        assertEpsgNameAndIdentifierEqual(name, code, descriptor);
        assertAliasTipEquals(alias, descriptor);
        assertEquals(1, descriptor.getMaximumOccurs(), "maximumOccurs");
        assertEquals(required ? 1 : 0, descriptor.getMinimumOccurs(), "minimumOccurs");
    }

    /**
     * Verifies that the given parameter value has the expected value and descriptor properties.
     *
     * @param  code        the expected EPSG code.
     * @param  name        the expected EPSG name.
     * @param  alias       the expected OGC alias.
     * @param  value       the expected value.
     * @param  unit        the expected unit of measurement for both the value and the descriptor.
     * @param  descriptor  the expected parameter descriptor associated to the parameter value.
     * @param  parameter   the parameter value to verify.
     */
    private static void verifyParameter(final int code, final String name, final String alias,
            final double value, final Unit<?> unit, final GeneralParameterDescriptor descriptor,
            final GeneralParameterValue parameter)
    {
        assertInstanceOf(ParameterValue.class, parameter, name);
        final ParameterValue<?> p = (ParameterValue<?>) parameter;
        final ParameterDescriptor<?> d = p.getDescriptor();
        verifyDescriptor(code, name, alias, true, d);
        assertSame  (descriptor,   d,                 "descriptor");
        assertEquals(value,        p.doubleValue(),   "value");
        assertEquals(unit,         p.getUnit(),       "unit");
        assertEquals(Double.class, d.getValueClass(), "valueClass");
        assertEquals(unit,         d.getUnit(),       "unit");
    }

    /**
     * Tests marshalling of a parameter value group.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testValueGroupMmarshalling() throws JAXBException {
        assertMarshalEqualsFile(TestFile.VALUE.openTestFile(),
                ParameterFormatTest.createMercatorParameters().createValue(),
                "xmlns:*", "xsi:schemaLocation");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests unmarshalling of a parameter value group. The XML file use {@code xlink:href} attributes
     * for avoiding to repeat the full definition of descriptors.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testValueGroupUnmarshalling() throws JAXBException {
        testValueGroupUnmarshalling(TestFile.VALUE);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests unmarshalling of a parameter value group. The XML file does not use {@code xlink:href} attributes;
     * descriptor definitions are repeated. The intent of this test is to test Apache SIS capability to replace
     * duplicates instances by unique instances.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testDuplicatedParametersUnmarshalling() throws JAXBException {
        testValueGroupUnmarshalling(TestFile.DUPLICATED);
        loggings.assertNextLogContains("EPSG::8801");
        loggings.assertNextLogContains("EPSG::8802");
        loggings.assertNextLogContains("EPSG::8805");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests unmarshalling of the given file.
     */
    private void testValueGroupUnmarshalling(final TestFile file) throws JAXBException {
        final DefaultParameterValueGroup group = unmarshalFile(DefaultParameterValueGroup.class, file.openTestFile());
        verifyDescriptorGroup(group.getDescriptor());
        final Iterator<GeneralParameterValue> it = group.values().iterator();
        final Iterator<GeneralParameterDescriptor> itd = group.getDescriptor().descriptors().iterator();
        verifyParameter(8801, "Latitude of natural origin",     "latitude_of_origin", 40, Units.DEGREE, itd.next(), it.next());
        verifyParameter(8802, "Longitude of natural origin",    "central_meridian",  -60, Units.DEGREE, itd.next(), it.next());
        verifyParameter(8805, "Scale factor at natural origin", "scale_factor",        1, Units.UNITY,    itd.next(), it.next());
        assertFalse(it.hasNext());
    }
}
