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
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultCitation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final strictfp class DefaultCitationTest extends XMLTestCase {
    /**
     * An XML file in this package containing a citation.
     */
    private static final String XML_FILE = "Citation.xml";

    /**
     * Creates a citation with an arbitrary title, presentation form and other properties.
     *
     * @return An arbitrary citation.
     *
     * @since 0.7
     */
    public static DefaultCitation create() {
        final DefaultCitation citation = new DefaultCitation();
        final DefaultInternationalString title = new DefaultInternationalString();
        title.add(Locale.JAPANESE, "アンダーカレント");
        title.add(Locale.ENGLISH,  "Undercurrent");
        citation.setTitle(title);
        citation.setISBN("9782505004509");
        citation.setPresentationForms(Arrays.asList(
                PresentationForm.DOCUMENT_HARDCOPY,
                PresentationForm.DOCUMENT_DIGITAL));
        citation.setAlternateTitles(Collections.singleton(
                new SimpleInternationalString("Andākarento")));   // Actually a different script of the Japanese title.

        final DefaultResponsibleParty author = new DefaultResponsibleParty(Role.AUTHOR);
        author.setParties(Collections.singleton(new DefaultIndividual("Testsuya Toyoda", null, null)));

        final DefaultResponsibleParty editor = new DefaultResponsibleParty(Role.valueOf("EDITOR"));
        editor.setParties(Collections.singleton(new DefaultOrganisation("Kōdansha", null, null, null)));
        editor.setExtents(Collections.singleton(Extents.WORLD));

        citation.setCitedResponsibleParties(Arrays.asList(author, editor));
        return citation;
    }

    /**
     * Tests the identifier map, which handles ISBN and ISSN codes in a special way.
     */
    @Test
    public void testIdentifierMap() {
        final DefaultCitation citation = new DefaultCitation();
        final Collection<Identifier> identifiers = citation.getIdentifiers();
        final IdentifierMap identifierMap = citation.getIdentifierMap();
        assertTrue("Expected an initially empty set of identifiers.", identifiers.isEmpty());
        /*
         * Set the ISBN code, and ensure that the the ISBN is reflected in the identifier map.
         */
        citation.setISBN("MyISBN");
        assertEquals("MyISBN", citation.getISBN());
        assertEquals("ISBN code shall be included in the set of identifiers.", 1, identifiers.size());
        assertEquals("{ISBN=“MyISBN”}", identifierMap.toString());
        /*
         * Set the identifiers with a list containing ISBN and ISSN codes.
         * The ISBN code shall be ignored because and ISBN property was already set.
         * The ISSN code shall be retained because it is a new code.
         */
        assertNull("ISSN shall be initially null.", citation.getISSN());
        citation.setIdentifiers(Arrays.asList(
                new DefaultIdentifier(Citations.NETCDF, "MyNetCDF"),
                new DefaultIdentifier(Citations.EPSG,   "MyEPSG"),
                new DefaultIdentifier(Citations.ISBN,   "NewISBN"),
                new DefaultIdentifier(Citations.ISSN,   "MyISSN")));

        assertEquals("The ISBN value shall have been overwritten.",       "NewISBN", citation.getISBN());
        assertEquals("The ISSN value shall have been added, because new.", "MyISSN", citation.getISSN());
        assertEquals("{NetCDF=“MyNetCDF”, EPSG=“MyEPSG”, ISBN=“NewISBN”, ISSN=“MyISSN”}", identifierMap.toString());
    }

    /**
     * Tests {@link DefaultCitation#freeze()}, which is needed for the constants defined in {@link Citations}.
     */
    @Test
    public void testFreeze() {
        final DefaultCitation original = create();
        final DefaultCitation clone = (DefaultCitation) original.unmodifiable();  // This will invoke 'freeze()'.
        assertNotSame(original, clone);
        assertTrue ("original.isModifiable",        original.isModifiable());
        assertFalse(   "clone.isModifiable",           clone.isModifiable());
        assertSame ("original.unmodifiable", clone, original.unmodifiable());
        assertSame (   "clone.unmodifiable", clone,    clone.unmodifiable());

        assertSame ("ISBN",  original.getISBN(),  clone.getISBN());
        assertSame ("title", original.getTitle(), clone.getTitle());
        assertSame ("alternateTitle", getSingleton(original.getAlternateTitles()),
                                     getSingleton(clone.getAlternateTitles()));

        assertCopy(original.getIdentifiers(),             clone.getIdentifiers());
        assertCopy(original.getCitedResponsibleParties(), clone.getCitedResponsibleParties());
        assertCopy(original.getPresentationForms(),       clone.getPresentationForms());
        /*
         * Verify the unique identifier, which is the ISBN code. ISBN and ISSN codes are handled
         * in a special way by DefaultCitation (they are instances of SpecializedIdentifier), but
         * the should nevertheless be cloned.
         */
        final Identifier ide = getSingleton(original.getIdentifiers());
        final Identifier ida = getSingleton(   clone.getIdentifiers());
        assertNotSame("identifier", ide, ida);
        assertSame("code",      ide.getCode(),      ida.getCode());
        assertSame("authority", ide.getAuthority(), ida.getAuthority());
        /*
         * Verify the author metadata.
         */
        final ResponsibleParty re = CollectionsExt.first(original.getCitedResponsibleParties());
        final ResponsibleParty ra = CollectionsExt.first(clone   .getCitedResponsibleParties());
        assertNotSame("citedResponsibleParty", re, ra);
        assertSame("role", re.getRole(), ra.getRole());
        assertSame("name", re.getIndividualName(),
                           ra.getIndividualName());
    }

    /**
     * Verifies that {@code actual} is an unmodifiable copy of {@code expected}.
     */
    private static <T> void assertCopy(final Collection<T> expected, final Collection<T> actual) {
        assertNotSame("ModifiableMetadata.freeze() shall have copied the collection.", expected, actual);
        assertEquals("The copied collection shall have the same content than the original.", expected, actual);
        try {
            actual.add(null);
            fail("The copied collection shall be unmodifiable.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests XML marshalling using the format derived form ISO 19115:2003 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @since 0.7
     */
    @Test
    public void testMarshalling() throws JAXBException {
        final DefaultContact contact = new DefaultContact();
        contact.setContactInstructions(new SimpleInternationalString("Send carrier pigeon."));
        contact.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "ip-protocol");
        final DefaultCitation c = new DefaultCitation("Fight against poverty");
        final DefaultResponsibleParty r1 = new DefaultResponsibleParty(Role.ORIGINATOR);
        final DefaultResponsibleParty r2 = new DefaultResponsibleParty(Role.valueOf("funder"));
        r1.setParties(Collections.singleton(new DefaultIndividual("Maid Marian", null, contact)));
        r2.setParties(Collections.singleton(new DefaultIndividual("Robin Hood",  null, contact)));
        c.setCitedResponsibleParties(Arrays.asList(r1, r2));
        c.getDates().add(new DefaultCitationDate(TestUtilities.date("2015-10-17 00:00:00"), DateType.valueOf("adopted")));
        assertMarshalEqualsFile(XML_FILE, c, "xlmns:*", "xsi:schemaLocation");
    }

    /**
     * Tests XML unmarshalling using the format derived form ISO 19115:2003 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     *
     * @since 0.7
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        final DefaultCitation c = unmarshalFile(DefaultCitation.class, XML_FILE);
        assertTitleEquals("title", "Fight against poverty", c);

        final CitationDate date = getSingleton(c.getDates());
        assertEquals("date", date.getDate(), TestUtilities.date("2015-10-17 00:00:00"));
        assertEquals("dateType", date.getDateType(), DateType.valueOf("adopted"));

        final Iterator<ResponsibleParty> it = c.getCitedResponsibleParties().iterator();
        final Contact contact = assertResponsibilityEquals(Role.ORIGINATOR, "Maid Marian", it.next());
        assertEquals("Contact instruction", "Send carrier pigeon.", contact.getContactInstructions().toString());

        // Thanks to xlink:href, the Contact shall be the same instance than above.
        assertSame("contact", contact, assertResponsibilityEquals(Role.valueOf("funder"), "Robin Hood", it.next()));
        assertFalse(it.hasNext());
    }

    /**
     * Asserts that the given responsibility has the expected properties, then returns its contact info.
     */
    private static Contact assertResponsibilityEquals(final Role role, final String name, final ResponsibleParty actual) {
        assertEquals("role", role, actual.getRole());
        final AbstractParty p = getSingleton(((DefaultResponsibleParty) actual).getParties());
        assertEquals("name", name, p.getName().toString());
        return getSingleton(p.getContactInfo());
    }
}
