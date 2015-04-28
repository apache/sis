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
package org.apache.sis.metadata.iso.citation;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.LogRecord;
import org.opengis.metadata.citation.Telephone;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.geoapi.evolution.UnsupportedCodeList;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DefaultContact} and its interaction with {@link DefaultTelephone}.
 * Those two classes are a little bit tricky because of the deprecated telephone methods.
 * See {@link DefaultTelephone} class javadoc for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class DefaultContactTest extends XMLTestCase implements WarningListener<Object> {
    /**
     * The resource key for the message of the warning that occurred, or {@code null} if none.
     */
    private Object resourceKey;

    /**
     * The parameter of the warning that occurred, or {@code null} if none.
     */
    private Object[] parameters;

    /**
     * For internal {@code DefaultContact} usage.
     *
     * @return {@code Object.class}.
     */
    @Override
    public Class<Object> getSourceClass() {
        return Object.class;
    }

    /**
     * Invoked when a warning occurred while unmarshalling a test XML fragment. This method ensures that no other
     * warning occurred before this method call (i.e. each test is allowed to cause at most one warning), then
     * remember the warning parameters for verification by the test method.
     *
     * @param source  Ignored.
     * @param warning The warning.
     */
    @Override
    public void warningOccured(final Object source, final LogRecord warning) {
        assertNull(resourceKey);
        assertNull(parameters);
        assertNotNull(resourceKey = warning.getMessage());
        assertNotNull(parameters  = warning.getParameters());
    }

    /**
     * Initializes the test for catching warning messages.
     */
    private void init() {
        context = new Context(0, null, null, null, null, null, null, this);
    }

    /**
     * Tests the ISO 19115:2014 {@link DefaultContact#setPhones(Collection)} method,
     * then query with both the new and the deprecated methods.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testSetPhones() {
        init();
        final DefaultTelephone   tel1 = new DefaultTelephone("00.01", UnsupportedCodeList.valueOf("SMS"));
        final DefaultTelephone   tel2 = new DefaultTelephone("00.02", UnsupportedCodeList.VOICE);
        final DefaultTelephone   tel3 = new DefaultTelephone("00.03", UnsupportedCodeList.FACSIMILE);
        final DefaultTelephone   tel4 = new DefaultTelephone("00.04", UnsupportedCodeList.VOICE);
        final DefaultTelephone[] tels = new DefaultTelephone[] {tel1, tel2, tel3, tel4};
        final DefaultContact  contact = new DefaultContact();
        contact.setPhones(Arrays.asList(tel1, tel2, tel3, tel4));
        assertArrayEquals("getPhones", tels, contact.getPhones().toArray());
        /*
         * Test the deprecated 'getPhone()' method. Invoking that method shall emit
         * a warning, since the ISO 19115:2003 methods can not represent SMS numbers.
         */
        assertSame("getPhone", tel2, contact.getPhone()); // Shall ignore the TelephoneType.SMS.
        assertEquals("warningOccured", "IgnoredPropertyAssociatedTo_1", resourceKey);
        assertArrayEquals("warningOccured", new String[] {"SMS"}, parameters);
        verifyLegacyLists(tels);
    }

    /**
     * Verify that the {@link DefaultTelephone#getVoices()} and {@link DefaultTelephone#getFacsimiles()} methods see
     * all numbers declared in the parent {@link DefaultContact}. This method presumes that each telephone contains:
     *
     * <ul>
     *   <li>Two voice numbers: "00.02" and "00.04" in that order.</li>
     *   <li>One facsimile number: "00.03".</li>
     * </ul>
     */
    @SuppressWarnings("deprecation")
    private static void verifyLegacyLists(final Telephone... tels) {
        final String[] voices = {"00.02", "00.04"};
        final String[] facsimiles = {"00.03"};
        for (final Telephone tel : tels) {
            assertArrayEquals("getVoices",     voices,     tel.getVoices()    .toArray());
            assertArrayEquals("getFacsimiles", facsimiles, tel.getFacsimiles().toArray());
        }
    }

    /**
     * Tests the ISO 19115:2003 {@link DefaultContact#setPhone(Telephone)} method,
     * then query with both the new and the deprecated methods.
     */
    @Test
    @DependsOnMethod("testSetPhones")
    public void testSetPhone() {
        testSetPhone(false);
    }

    /**
     * Same as {@link #testSetPhone()}, but hiding to {@link DefaultContact} the fact that we
     * are using a SIS implementation of {@code Telephone}. This will test an other code path.
     */
    @Test
    @DependsOnMethod("testSetPhones")
    public void testSetNonSISPhone() {
        testSetPhone(true);
    }

    /**
     * Implementation of {@link #testSetPhone()} and {@link #testSetNonSISPhone()}.
     *
     * @param hideSIS Whether to hide to {@link DefaultContact} the fact that we
     *        are using a SIS implementation of {@code Telephone}.
     */
    @SuppressWarnings("deprecation")
    private void testSetPhone(final boolean hideSIS) {
        init();
        final DefaultTelephone tel = new DefaultTelephone();
        tel.setVoices(Arrays.asList("00.02", "00.04"));
        tel.setFacsimiles(Arrays.asList("00.03"));
        final Telephone view;
        if (hideSIS) {
            view = new Telephone() {
                @Override public Collection<String> getVoices()     {return tel.getVoices();}
                @Override public Collection<String> getFacsimiles() {return tel.getFacsimiles();}
            };
        } else {
            view = tel;
        }
        verifyLegacyLists(tel);
        /*
         * Give the telephone to a contact, and verify that new telephone instances were created.
         */
        final DefaultContact contact = new DefaultContact();
        contact.setPhone(view);
        verifyLegacyLists(view);
        assertArrayEquals("getPhones", new DefaultTelephone[] {
                new DefaultTelephone("00.02", UnsupportedCodeList.VOICE),
                new DefaultTelephone("00.04", UnsupportedCodeList.VOICE),
                new DefaultTelephone("00.03", UnsupportedCodeList.FACSIMILE)
            }, contact.getPhones().toArray());
    }
}
