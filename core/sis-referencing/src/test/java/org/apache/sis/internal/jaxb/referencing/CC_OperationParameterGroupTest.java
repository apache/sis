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
import javax.xml.bind.JAXBException;
import javax.measure.unit.NonSI;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.xml.Namespaces;
import org.junit.Test;

import static org.apache.sis.metadata.iso.citation.Citations.EPSG;
import static org.junit.Assert.*;


/**
 * Tests {@link CC_GeneralOperationParameter} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(CC_GeneralOperationParameterTest.class)
public final strictfp class CC_OperationParameterGroupTest extends XMLTestCase {
    /**
     * Creates a parameter descriptor group as unmarshalled by JAXB, without {@code valueClass}.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    private DefaultParameterDescriptorGroup unmarshal() throws JAXBException {
        return unmarshal(DefaultParameterDescriptorGroup.class,
                "<gml:OperationParameterGroup xmlns:gml=\"" + Namespaces.GML + "\">\n"
              + "  <gml:name>Mercator (1SP)</gml:name>\n"
              + "  <gml:parameter>\n"
              + "    <gml:OperationParameter gml:id=\"epsg-parameter-8801\">\n"
              + "      <gml:identifier codeSpace=\"IOGP\">urn:ogc:def:parameter:EPSG::8801</gml:identifier>\n"
              + "      <gml:name codeSpace=\"EPSG\">Latitude of natural origin</gml:name>\n"
              + "    </gml:OperationParameter>\n"
              + "  </gml:parameter>\n"
              + "  <gml:parameter>\n"
              + "    <gml:OperationParameter gml:id=\"epsg-parameter-8802\">\n"
              + "      <gml:identifier codeSpace=\"IOGP\">urn:ogc:def:parameter:EPSG::8802</gml:identifier>\n"
              + "      <gml:name codeSpace=\"EPSG\">Longitude of natural origin</gml:name>\n"
              + "    </gml:OperationParameter>\n"
              + "  </gml:parameter>\n"
              //   There is more parameters in a Mercator projection, but 2 is enough for this test.
              + "</gml:OperationParameterGroup>");
    }

    /**
     * Creates a parameter descriptor equivalent to the result of {@link #unmarshal()}
     * but with arbitrary default values.
     */
    private static ParameterDescriptorGroup create() {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setCodeSpace(EPSG, "EPSG").setRequired(true);
        ParameterDescriptor<?>[] parameters = {
            builder.addIdentifier("8801").addName("Latitude of natural origin") .create( 40, NonSI.DEGREE_ANGLE),
            builder.addIdentifier("8802").addName("Longitude of natural origin").create(-60, NonSI.DEGREE_ANGLE),
        };
        builder.addName("Mercator (1SP)");
        return builder.createGroup(parameters);
    }

    /**
     * Tests the substitution of unmarshalled descriptors by more complete descriptors.
     * No merging should be done in this test.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    public void testSubtitution() throws JAXBException {
        final ParameterDescriptorGroup provided = unmarshal();
        final ParameterDescriptorGroup complete = create();

        // Normal usage: merge to existing descriptors the more complete information found in parameter values.
        verifySubtitution(provided.descriptors(), complete.descriptors(), complete.descriptors());

        // Unusual case, tested for safety: the existing descriptors were actually more complete.
        verifySubtitution(complete.descriptors(), provided.descriptors(), complete.descriptors());
    }

    /**
     * Implementation of {@link #testSubtitution()}.
     *
     * @param descriptors Simulates the descriptors already present in a {@code ParameterDescriptorGroup}.
     * @param fromValues  Simulates the descriptors created from {@code ParameterValue} instances.
     * @param expected    The expected descriptors.
     */
    private static void verifySubtitution(
            final List<GeneralParameterDescriptor> descriptors,
            final List<GeneralParameterDescriptor> fromValues,
            final List<GeneralParameterDescriptor> expected)
    {
        final Map<GeneralParameterDescriptor,GeneralParameterDescriptor> replacements = new IdentityHashMap<>(4);
        final GeneralParameterDescriptor[] merged = CC_OperationParameterGroup.merge(descriptors,
                fromValues.toArray(new GeneralParameterDescriptor[fromValues.size()]), replacements);

        assertTrue("Expected no replacement.", replacements.isEmpty());
        assertEquals("Number of parameters", 2, merged.length);
        for (int i=0; i<merged.length; i++) {
            assertSame(expected.get(i), merged[i]);
        }
    }
}
