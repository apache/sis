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
package org.apache.sis.xml.bind.referencing;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import jakarta.xml.bind.JAXBException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.system.Loggers;
import org.apache.sis.xml.Namespaces;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.referencing.Assertions.assertRemarksEquals;
import org.apache.sis.xml.test.TestCase;


/**
 * Tests {@link CC_GeneralOperationParameter} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-290">SIS-290</a>
 */
public final class CC_GeneralOperationParameterTest extends TestCase.WithLogs {
    /**
     * Creates a new test case.
     */
    public CC_GeneralOperationParameterTest() {
        super(Loggers.XML);
    }

    /**
     * Tests {@link CC_GeneralOperationParameter#toArray(Collection, Class)}.
     */
    @Test
    public void testToArray() {
        final String[] expected = new String[] {"One", "Two", "Three"};
        final String[] actual = CC_GeneralOperationParameter.toArray(List.of(expected), String.class);
        assertArrayEquals(expected, actual);
    }

    /**
     * Creates a parameter descriptor as unmarshalled by JAXB, without {@code valueClass}.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    private DefaultParameterDescriptor<?> unmarshal(final String name, final String remarks) throws JAXBException {
        final StringBuilder buffer = new StringBuilder(256);
        buffer.append("<gml:OperationParameter xmlns:gml=\"" + Namespaces.GML + "\">\n"
                    + "  <gml:name>").append(name).append("</gml:name>\n");
        if (remarks != null) {
            buffer.append("  <gml:remarks>").append(remarks).append("</gml:remarks>\n");
        }
        buffer.append("  <gml:minimumOccurs>0</gml:minimumOccurs>\n"
                    + "</gml:OperationParameter>");
        final DefaultParameterDescriptor<?> p = unmarshal(DefaultParameterDescriptor.class, buffer.toString());
        /*
         * Following assertions are not really the purpose of this class, but are done on an opportunist way.
         * We need the unmarshalled descriptor to have those property values for the remaining of this test.
         * The most noticeable assertion is the 'valueClass', which is required to be null (despite being an
         * illegal value) for this test.
         */
        assertEquals(name, p.getName().getCode());
        assertEquals(remarks, (remarks == null) ? null : p.getRemarks().orElseThrow().toString());
        assertTrue(p.getDescription().isEmpty());
        assertNull(p.getValueClass());
        assertEquals(0, p.getMinimumOccurs());
        assertEquals(1, p.getMaximumOccurs());
        return p;
    }

    /**
     * Creates a parameter descriptor from the given properties.
     * All properties except {@code defaultValue} can be part of GML documents.
     *
     * @param  name          the parameter descriptor name.
     * @param  remarks       the remarks, or {@code null} if none.
     * @param  mandatory     {@code true} for a mandatory parameter, or {@code false} for an optional one.
     * @param  defaultValue  the default value, or {@code null} if none.
     * @return the parameter descriptor.
     */
    private static DefaultParameterDescriptor<Integer> create(final String name, final String remarks,
            final boolean mandatory, final Integer defaultValue)
    {
        final var properties = new HashMap<String,String>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, name));
        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, remarks));
        return new DefaultParameterDescriptor<>(properties, mandatory ? 1 : 0, 1, Integer.class, null, null, defaultValue);
    }

    /**
     * Tests cases where the unmarshalled parameter can be substituted by the complete parameter.
     * The cases tested in this method should not create any new {@code ParameterDescriptor} instance.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    public void testParameterSubstitution() throws JAXBException {
        ParameterDescriptor<?> provided = unmarshal("Optional parameter", null);
        ParameterDescriptor<?> complete = create("Optional parameter", null, false, null);
        assertSame(complete, CC_GeneralOperationParameter.merge(complete, complete));
        assertSame(complete, CC_GeneralOperationParameter.merge(provided, complete));

        complete = create("OptionalParameter", null, false, null);
        assertSame(complete, CC_GeneralOperationParameter.merge(provided, complete));

        complete = create("Optional parameter", null, false, 3);
        assertSame(complete, CC_GeneralOperationParameter.merge(provided, complete));

        complete = create("Optional parameter", "More details here.", false, null);
        assertSame(complete, CC_GeneralOperationParameter.merge(provided, complete));

        provided = unmarshal("Optional parameter", "More details here.");
        assertSame(complete, CC_GeneralOperationParameter.merge(provided, complete));

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests cases where the unmarshalled parameter needs to be merged with the complete parameter.
     * All cases tested in this method should create a new {@link ParameterDescriptor} instance.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    public void testParameterMerge() throws JAXBException {
        ParameterDescriptor<?> provided = unmarshal("Test parameter", null);
        ParameterDescriptor<?> complete = create("Test parameter", null, true, null);
        ParameterDescriptor<?> merged   = (ParameterDescriptor<?>) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame(complete,           merged);
        assertSame   (complete.getName(), merged.getName());
        assertEquals (0,                  merged.getMinimumOccurs());  // From provided descriptor.
        assertEquals (1,                  merged.getMaximumOccurs());
        assertEquals (Integer.class,      merged.getValueClass());     // From complete descriptor.
        assertTrue   (                    merged.getRemarks().isEmpty());

        complete = create("Test parameter", null, false, null);
        assertSame(complete, CC_GeneralOperationParameter.merge(provided, complete));
        // Above assertion was tested by testParameterSubstitutions(), but was verified again here
        // for making sure that the following assertion verifies the effect of the remarks alone.
        provided = unmarshal("Test parameter", "More details here.");
        merged   = (ParameterDescriptor<?>) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame(complete,              merged);
        assertSame   (complete.getName(),    merged.getName());
        assertEquals (0,                     merged.getMinimumOccurs());
        assertEquals (1,                     merged.getMaximumOccurs());
        assertEquals (Integer.class,         merged.getValueClass());
        assertEquals (provided.getRemarks(), merged.getRemarks());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests case where the unmarshalled parameter group can be substituted by the complete parameter group.
     * The cases tested in this method should not create any new {@link ParameterDescriptorGroup} instance.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    public void testGroupSubstitution() throws JAXBException {
        final Map<String,String> properties = new HashMap<>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, "Group"));
        final var provided = new DefaultParameterDescriptorGroup(properties, 1, 2,
                unmarshal("Parameter A", null),
                unmarshal("Parameter B", "Remarks B."),
                unmarshal("Parameter C", null));

        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, "More details here."));
        final var complete = new DefaultParameterDescriptorGroup(properties, 1, 2,
                create("Parameter A", "Remarks A.", false, 3),
                create("Parameter B", "Remarks B.", false, 4),
                create("Parameter C", "Remarks C.", false, 5),
                create("Parameter D", "Remarks D.", false, 6));

        assertSame(complete, CC_GeneralOperationParameter.merge(provided, complete));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests case where the unmarshalled parameter group needs to be merged with the complete parameter group.
     * The reason for the group merge in this test is because the unmarshalled parameters have different remarks
     * or different obligation.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    public void testGroupMergeBecauseDifferentProperties() throws JAXBException {
        final Map<String,String> properties = new HashMap<>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, "Group"));
        final var provided = new DefaultParameterDescriptorGroup(properties, 1, 2,
                unmarshal("Parameter A", "Remarks A."),
                unmarshal("Parameter B", "Remarks B."),
                unmarshal("Parameter C", "Remarks C."));

        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, "More details here."));
        final var complete = new DefaultParameterDescriptorGroup(properties, 1, 2,
                create("Parameter A", "Remarks A.", true,  3),
                create("Parameter B", "Remarks B.", false, 4),
                create("Parameter C", "Different.", false, 5),
                create("Parameter D", "Remarks D.", false, 6));

        final var merged = (ParameterDescriptorGroup) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame(complete, provided);
        assertSame   (complete.getName(),    merged.getName());
        assertEquals (complete.getRemarks(), merged.getRemarks());
        assertEquals (1,                     merged.getMinimumOccurs());
        assertEquals (2,                     merged.getMaximumOccurs());

        final Iterator<GeneralParameterDescriptor> itc = complete.descriptors().iterator();
        final Iterator<GeneralParameterDescriptor> itm = merged  .descriptors().iterator();
        verifyParameter(itc.next(), itm.next(), false, "Remarks A.");   // Not same because different obligation.
        verifyParameter(itc.next(), itm.next(), true,  "Remarks B.");   // Same ParameterDescriptor instance.
        verifyParameter(itc.next(), itm.next(), false, "Remarks C.");   // Not same because different remarks.
        assertTrue (itc.hasNext());
        assertFalse(itm.hasNext());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests case where the unmarshalled parameter group needs to be merged with the complete parameter group.
     * The reason for the group merge in this test is because the unmarshalled parameters is missing a mandatory
     * parameter.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    public void testGroupMergeBecauseMissingParameter() throws JAXBException {
        final Map<String,String> properties = new HashMap<>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, "Group"));
        final var provided = new DefaultParameterDescriptorGroup(properties, 1, 2,
                unmarshal("Parameter A", null),
                unmarshal("Parameter C", null));

        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, "More details here."));
        final var complete = new DefaultParameterDescriptorGroup(properties, 1, 2,
                create("Parameter A", null, false, 3),
                create("Parameter B", null, true,  4),
                create("Parameter C", null, false, 5));

        final var merged = (ParameterDescriptorGroup) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame(complete, provided);
        assertSame   (complete.getName(),    merged.getName());
        assertEquals (complete.getRemarks(), merged.getRemarks());
        assertEquals (1,                     merged.getMinimumOccurs());
        assertEquals (2,                     merged.getMaximumOccurs());

        final Iterator<GeneralParameterDescriptor> itc = complete.descriptors().iterator();
        final Iterator<GeneralParameterDescriptor> itm = merged  .descriptors().iterator();
        verifyParameter(itc.next(), itm.next(), true, null);

        // Skip the parameter which is missing in the unmarshalled descriptor group.
        assertEquals("Parameter B", itc.next().getName().getCode());

        verifyParameter(itc.next(), itm.next(), true, null);
        assertFalse(itc.hasNext());
        assertFalse(itm.hasNext());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests case where the unmarshalled parameter group needs to be merged with the complete parameter group.
     * The reason for the group merge in this test is because the unmarshalled parameters have a parameter not
     * present in the "complete" group.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    public void testGroupMergeBecauseExtraParameter() throws JAXBException {
        final Map<String,String> properties = new HashMap<>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, "Group"));
        final var provided = new DefaultParameterDescriptorGroup(properties, 1, 2,
                unmarshal("Parameter A", "Remarks A."),
                unmarshal("Parameter B", "Remarks B."),
                unmarshal("Parameter C", "Remarks C."));

        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, "More details here."));
        final var complete = new DefaultParameterDescriptorGroup(properties, 1, 2,
                create("Parameter A", "Remarks A.", false, 3),
                create("Parameter C", "Remarks C.", false, 4));

        loggings.assertNoUnexpectedLog();
        final var merged = (ParameterDescriptorGroup) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame(complete, provided);
        assertSame   (complete.getName(),    merged.getName());
        assertEquals (complete.getRemarks(), merged.getRemarks());
        assertEquals (1,                     merged.getMinimumOccurs());
        assertEquals (2,                     merged.getMaximumOccurs());
        loggings.assertNextLogContains("Parameter B", "Group");
        loggings.assertNoUnexpectedLog();

        final Iterator<GeneralParameterDescriptor> itc = complete.descriptors().iterator();
        final Iterator<GeneralParameterDescriptor> itm = merged  .descriptors().iterator();
        verifyParameter(itc.next(), itm.next(), true, "Remarks A.");

        final GeneralParameterDescriptor extra = itm.next();
        assertEquals("Parameter B", extra.getName().getCode());
        assertRemarksEquals("Remarks B.", extra, null);

        verifyParameter(itc.next(), itm.next(), true, "Remarks C.");
        assertFalse(itc.hasNext());
        assertFalse(itm.hasNext());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies the properties of the given member of a {@link DefaultParameterDescriptorGroup}.
     */
    private static void verifyParameter(final GeneralParameterDescriptor complete,
                                        final GeneralParameterDescriptor merged,
                                        final boolean same, final String remarks)
    {
        assertEquals(same,               merged == complete);
        assertSame  (complete.getName(), merged.getName());
        assertEquals(0,                  merged.getMinimumOccurs());
        assertEquals(1,                  merged.getMaximumOccurs());
        assertEquals(Integer.class,      ((ParameterDescriptor<?>) merged).getValueClass());
        assertRemarksEquals(remarks,     merged, null);
    }
}
