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

import java.util.List;
import java.util.Collection;
import java.util.logging.LogRecord;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.citation.Telephone;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.TelephoneType;


/**
 * Tests {@link DefaultContact} and its interaction with {@link DefaultTelephone}.
 * Those two classes are a little bit tricky because of the deprecated telephone methods.
 * See {@link DefaultTelephone} class javadoc for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultContactTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultContactTest() {
        super(true);   // This test is expected to emit some warnings.
    }

    /**
     * Initializes the test for catching warning messages.
     *
     * @throws JAXBException if an error occurred while creating the context for the test.
     */
    @BeforeEach
    public void createContext() throws JAXBException {
        createContext(false, null, null);
    }

    /**
     * Tests the ISO 19115:2014 {@link DefaultContact#setPhones(Collection)} method,
     * then query with both the new and the deprecated methods.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testSetPhones() {
        final var tel1 = new DefaultTelephone("00.01", TelephoneType.SMS);
        final var tel2 = new DefaultTelephone("00.02", TelephoneType.VOICE);
        final var tel3 = new DefaultTelephone("00.03", TelephoneType.FACSIMILE);
        final var tel4 = new DefaultTelephone("00.04", TelephoneType.VOICE);
        final var tels = new DefaultTelephone[] {tel1, tel2, tel3, tel4};
        final var contact = new DefaultContact();
        contact.setPhones(List.of(tel1, tel2, tel3, tel4));
        assertArrayEquals(tels, contact.getPhones().toArray());
        /*
         * Test the deprecated `getPhone()` method. Invoking that method shall emit
         * a warning, since the ISO 19115:2003 methods cannot represent SMS numbers.
         */
        assertSame(tel2, contact.getPhone());       // Shall ignore the TelephoneType.SMS.
        final LogRecord warning = assertOneWarning();
        assertEquals("IgnoredPropertyAssociatedTo_1", warning.getMessage());
        assertArrayEquals(new String[] {"TelephoneType.SMS"}, warning.getParameters());
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
            assertArrayEquals(voices,     tel.getVoices()    .toArray());
            assertArrayEquals(facsimiles, tel.getFacsimiles().toArray());
        }
    }

    /**
     * Tests the ISO 19115:2003 {@link DefaultContact#setPhone(Telephone)} method,
     * then query with both the new and the deprecated methods.
     */
    @Test
    public void testSetPhone() {
        testSetPhone(false);
    }

    /**
     * Same as {@link #testSetPhone()}, but hiding to {@link DefaultContact} the fact that we
     * are using a SIS implementation of {@code Telephone}. This will test another code path.
     */
    @Test
    public void testSetNonSISPhone() {
        testSetPhone(true);
    }

    /**
     * Implementation of {@link #testSetPhone()} and {@link #testSetNonSISPhone()}.
     *
     * @param hideSIS  whether to hide to {@link DefaultContact} the fact that
     *                 we are using a SIS implementation of {@code Telephone}.
     */
    @SuppressWarnings("deprecation")
    private void testSetPhone(final boolean hideSIS) {
        final var tel = new DefaultTelephone();
        tel.setVoices(List.of("00.02", "00.04"));
        tel.setFacsimiles(List.of("00.03"));
        final Telephone view;
        if (hideSIS) {
            view = new Telephone() {
                @Override public String             getNumber()     {return tel.getNumber();}
                @Override public TelephoneType      getNumberType() {return tel.getNumberType();}
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
        final var contact = new DefaultContact();
        contact.setPhone(view);
        verifyLegacyLists(view);
        assertArrayEquals(new DefaultTelephone[] {
                new DefaultTelephone("00.02", TelephoneType.VOICE),
                new DefaultTelephone("00.04", TelephoneType.VOICE),
                new DefaultTelephone("00.03", TelephoneType.FACSIMILE)
            }, contact.getPhones().toArray());

        assertNoWarning();
    }
}
