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

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import javax.xml.bind.JAXBException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.LoggingWatcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link CC_GeneralOperationParameter} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-290">SIS-290</a>
 */
@DependsOn(org.apache.sis.parameter.DefaultParameterDescriptorTest.class)
public final strictfp class CC_GeneralOperationParameterTest extends XMLTestCase {
    /**
     * A JUnit rule for listening to log events emitted during execution of {@link #testGroupMergeBecauseExtraParameter()}.
     * This rule is used by test methods for verifying that the log message contains the expected information.
     * The expected message is something like "No parameter named "Parameter B" was found".
     *
     * <p>This field is public because JUnit requires us to do so, but should be considered as an implementation details
     * (it should have been a private field).</p>
     */
    @Rule
    public final LoggingWatcher loggings = new LoggingWatcher(Loggers.XML);

    /**
     * Verifies that no unexpected warning has been emitted in any test defined in this class.
     */
    @After
    public void assertNoUnexpectedLog() {
        loggings.assertNoUnexpectedLog();
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
        assertEquals("name",       name, p.getName().getCode());
        assertEquals("remarks", remarks, (remarks == null) ? null : p.getRemarks().toString());
        assertNull  ("description",      p.getDescription());
        assertNull  ("valueClass",       p.getValueClass());
        assertEquals("minimumOccurs", 0, p.getMinimumOccurs());
        assertEquals("maximumOccurs", 1, p.getMaximumOccurs());
        return p;
    }

    /**
     * Creates a parameter descriptor from the given properties.
     * All properties except {@code defaultValue} can be part of GML documents.
     *
     * @param  name          The parameter descriptor name.
     * @param  remarks       The remarks, or {@code null} if none.
     * @param  mandatory     {@code true} for a mandatory parameter, or {@code false} for an optional one.
     * @param  defaultValue  The default value, or {@code null} if none.
     * @return The parameter descriptor.
     */
    private static DefaultParameterDescriptor<Integer> create(final String name, final String remarks,
            final boolean mandatory, final Integer defaultValue)
    {
        final Map<String,String> properties = new HashMap<String,String>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, name));
        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, remarks));
        return new DefaultParameterDescriptor<Integer>(properties, mandatory ? 1 : 0, 1, Integer.class, null, null, defaultValue);
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
        assertSame("Trivial case.",    complete, CC_GeneralOperationParameter.merge(complete, complete));
        assertSame("Same properties.", complete, CC_GeneralOperationParameter.merge(provided, complete));

        complete = create("OptionalParameter", null, false, null);
        assertSame("Slightly different name.", complete, CC_GeneralOperationParameter.merge(provided, complete));

        complete = create("Optional parameter", null, false, 3);
        assertSame("With default value.", complete, CC_GeneralOperationParameter.merge(provided, complete));

        complete = create("Optional parameter", "More details here.", false, null);
        assertSame("With additional property.", complete, CC_GeneralOperationParameter.merge(provided, complete));

        provided = unmarshal("Optional parameter", "More details here.");
        assertSame("With same remark.", complete, CC_GeneralOperationParameter.merge(provided, complete));
    }

    /**
     * Tests cases where the unmarshalled parameter needs to be merged with the complete parameter.
     * All cases tested in this method should create a new {@link ParameterDescriptor} instance.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    @DependsOnMethod("testParameterSubstitution")
    public void testParameterMerge() throws JAXBException {
        ParameterDescriptor<?> provided = unmarshal("Test parameter", null);
        ParameterDescriptor<?> complete = create("Test parameter", null, true, null);
        ParameterDescriptor<?> merged   = (ParameterDescriptor<?>) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame("Different obligation.", complete,           merged);
        assertSame   ("name",                  complete.getName(), merged.getName());
        assertEquals ("minimumOccurs",         0,                  merged.getMinimumOccurs());  // From provided descriptor.
        assertEquals ("maximumOccurs",         1,                  merged.getMaximumOccurs());
        assertEquals ("valueClass",            Integer.class,      merged.getValueClass());     // From complete descriptor.
        assertNull   ("remarks",                                   merged.getRemarks());

        complete = create("Test parameter", null, false, null);
        assertSame(complete, CC_GeneralOperationParameter.merge(provided, complete));
        // Above assertion was tested by testParameterSubstitutions(), but was verified again here
        // for making sure that the following assertion verifies the effect of the remarks alone.
        provided = unmarshal("Test parameter", "More details here.");
        merged   = (ParameterDescriptor<?>) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame("Different remark.", complete,              merged);
        assertSame   ("name",              complete.getName(),    merged.getName());
        assertEquals ("minimumOccurs",     0,                     merged.getMinimumOccurs());
        assertEquals ("maximumOccurs",     1,                     merged.getMaximumOccurs());
        assertEquals ("valueClass",        Integer.class,         merged.getValueClass());
        assertSame   ("remarks",           provided.getRemarks(), merged.getRemarks());
    }

    /**
     * Tests case where the unmarshalled parameter group can be substituted by the complete parameter group.
     * The cases tested in this method should not create any new {@link ParameterDescriptorGroup} instance.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    @DependsOnMethod("testParameterSubstitution")
    public void testGroupSubstitution() throws JAXBException {
        final Map<String,String> properties = new HashMap<String,String>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, "Group"));
        final ParameterDescriptorGroup provided = new DefaultParameterDescriptorGroup(properties, 1, 2,
                unmarshal("Parameter A", null),
                unmarshal("Parameter B", "Remarks B."),
                unmarshal("Parameter C", null));

        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, "More details here."));
        final ParameterDescriptorGroup complete = new DefaultParameterDescriptorGroup(properties, 1, 2,
                create("Parameter A", "Remarks A.", false, 3),
                create("Parameter B", "Remarks B.", false, 4),
                create("Parameter C", "Remarks C.", false, 5),
                create("Parameter D", "Remarks D.", false, 6));

        assertSame(complete, CC_GeneralOperationParameter.merge(provided, complete));
    }

    /**
     * Tests case where the unmarshalled parameter group needs to be merged with the complete parameter group.
     * The reason for the group merge in this test is because the unmarshalled parameters have different remarks
     * or different obligation.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    @DependsOnMethod({"testGroupSubstitution", "testParameterMerge"})
    public void testGroupMergeBecauseDifferentProperties() throws JAXBException {
        final Map<String,String> properties = new HashMap<String,String>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, "Group"));
        final ParameterDescriptorGroup provided = new DefaultParameterDescriptorGroup(properties, 1, 2,
                unmarshal("Parameter A", "Remarks A."),
                unmarshal("Parameter B", "Remarks B."),
                unmarshal("Parameter C", "Remarks C."));

        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, "More details here."));
        final ParameterDescriptorGroup complete = new DefaultParameterDescriptorGroup(properties, 1, 2,
                create("Parameter A", "Remarks A.", true,  3),
                create("Parameter B", "Remarks B.", false, 4),
                create("Parameter C", "Different.", false, 5),
                create("Parameter D", "Remarks D.", false, 6));

        final ParameterDescriptorGroup merged =
                (ParameterDescriptorGroup) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame(complete, provided);
        assertSame   ("name",          complete.getName(),    merged.getName());
        assertSame   ("remarks",       complete.getRemarks(), merged.getRemarks());
        assertEquals ("minimumOccurs", 1,                     merged.getMinimumOccurs());
        assertEquals ("maximumOccurs", 2,                     merged.getMaximumOccurs());

        final Iterator<GeneralParameterDescriptor> itc = complete.descriptors().iterator();
        final Iterator<GeneralParameterDescriptor> itm = merged  .descriptors().iterator();
        verifyParameter(itc.next(), itm.next(), false, "Remarks A.");   // Not same because different obligation.
        verifyParameter(itc.next(), itm.next(), true,  "Remarks B.");   // Same ParameterDescriptor instance.
        verifyParameter(itc.next(), itm.next(), false, "Remarks C.");   // Not same because different remarks.
        assertTrue ("Missing descriptor.",    itc.hasNext());
        assertFalse("Unexpected descriptor.", itm.hasNext());
    }

    /**
     * Tests case where the unmarshalled parameter group needs to be merged with the complete parameter group.
     * The reason for the group merge in this test is because the unmarshalled parameters is missing a mandatory
     * parameter.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    @DependsOnMethod("testGroupMergeBecauseDifferentProperties")
    public void testGroupMergeBecauseMissingParameter() throws JAXBException {
        final Map<String,String> properties = new HashMap<String,String>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, "Group"));
        final ParameterDescriptorGroup provided = new DefaultParameterDescriptorGroup(properties, 1, 2,
                unmarshal("Parameter A", null),
                unmarshal("Parameter C", null));

        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, "More details here."));
        final ParameterDescriptorGroup complete = new DefaultParameterDescriptorGroup(properties, 1, 2,
                create("Parameter A", null, false, 3),
                create("Parameter B", null, true,  4),
                create("Parameter C", null, false, 5));

        final ParameterDescriptorGroup merged =
                (ParameterDescriptorGroup) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame(complete, provided);
        assertSame   ("name",          complete.getName(),    merged.getName());
        assertSame   ("remarks",       complete.getRemarks(), merged.getRemarks());
        assertEquals ("minimumOccurs", 1,                     merged.getMinimumOccurs());
        assertEquals ("maximumOccurs", 2,                     merged.getMaximumOccurs());

        final Iterator<GeneralParameterDescriptor> itc = complete.descriptors().iterator();
        final Iterator<GeneralParameterDescriptor> itm = merged  .descriptors().iterator();
        verifyParameter(itc.next(), itm.next(), true, null);

        // Skip the parameter which is missing in the unmarshalled descriptor group.
        assertEquals("Missing parameter.", "Parameter B", itc.next().getName().getCode());

        verifyParameter(itc.next(), itm.next(), true, null);
        assertFalse("Unexpected descriptor.", itc.hasNext());
        assertFalse("Unexpected descriptor.", itm.hasNext());
    }

    /**
     * Tests case where the unmarshalled parameter group needs to be merged with the complete parameter group.
     * The reason for the group merge in this test is because the unmarshalled parameters have a parameter not
     * present in the "complete" group.
     *
     * @throws JAXBException if this method failed to create test data.
     */
    @Test
    @DependsOnMethod("testGroupMergeBecauseDifferentProperties")
    public void testGroupMergeBecauseExtraParameter() throws JAXBException {
        final Map<String,String> properties = new HashMap<String,String>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, "Group"));
        final ParameterDescriptorGroup provided = new DefaultParameterDescriptorGroup(properties, 1, 2,
                unmarshal("Parameter A", "Remarks A."),
                unmarshal("Parameter B", "Remarks B."),
                unmarshal("Parameter C", "Remarks C."));

        assertNull(properties.put(DefaultParameterDescriptor.REMARKS_KEY, "More details here."));
        final ParameterDescriptorGroup complete = new DefaultParameterDescriptorGroup(properties, 1, 2,
                create("Parameter A", "Remarks A.", false, 3),
                create("Parameter C", "Remarks C.", false, 4));

        loggings.assertNoUnexpectedLog();
        final ParameterDescriptorGroup merged =
                (ParameterDescriptorGroup) CC_GeneralOperationParameter.merge(provided, complete);
        assertNotSame(complete, provided);
        assertSame   ("name",          complete.getName(),    merged.getName());
        assertSame   ("remarks",       complete.getRemarks(), merged.getRemarks());
        assertEquals ("minimumOccurs", 1,                     merged.getMinimumOccurs());
        assertEquals ("maximumOccurs", 2,                     merged.getMaximumOccurs());
        loggings.assertNextLogContains("Parameter B", "Group");
        loggings.assertNoUnexpectedLog();

        final Iterator<GeneralParameterDescriptor> itc = complete.descriptors().iterator();
        final Iterator<GeneralParameterDescriptor> itm = merged  .descriptors().iterator();
        verifyParameter(itc.next(), itm.next(), true, "Remarks A.");

        final GeneralParameterDescriptor extra = itm.next();
        assertEquals("name",   "Parameter B", extra.getName().getCode());
        assertEquals("remark", "Remarks B.",  extra.getRemarks().toString());

        verifyParameter(itc.next(), itm.next(), true, "Remarks C.");
        assertFalse("Unexpected descriptor.", itc.hasNext());
        assertFalse("Unexpected descriptor.", itm.hasNext());
    }

    /**
     * Verifies the properties of the given member of a {@link DefaultParameterDescriptorGroup}.
     */
    private static void verifyParameter(final GeneralParameterDescriptor complete,
                                        final GeneralParameterDescriptor merged,
                                        final boolean same, final String remarks)
    {
        assertEquals("same",          same,               merged == complete);
        assertSame  ("name",          complete.getName(), merged.getName());
        assertEquals("minimumOccurs", 0,                  merged.getMinimumOccurs());
        assertEquals("maximumOccurs", 1,                  merged.getMaximumOccurs());
        assertEquals("valueClass",    Integer.class,      ((ParameterDescriptor<?>) merged).getValueClass());
        assertEquals("remarks",       remarks,            (remarks == null) ? null : merged.getRemarks().toString());
    }
}
