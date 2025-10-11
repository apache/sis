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

import java.net.URI;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.DefaultInternationalString;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.metadata.MetadataCopier;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.UnmodifiableMetadataException;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.extent.Extents;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.metadata.xml.TestUsingFile;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertTitleEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Responsibility;


/**
 * Tests {@link DefaultCitation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
@SuppressWarnings("exports")
public final class DefaultCitationTest extends TestUsingFile {
    /**
     * Creates a new test case.
     */
    public DefaultCitationTest() {
    }

    /**
     * Opens the stream to the XML file containing a citation.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile(final Format format) {
        return format.openTestFile("Citation.xml");
    }

    /**
     * Creates a citation with an arbitrary title, presentation form and other properties.
     *
     * @return an arbitrary citation.
     */
    public static DefaultCitation create() {
        final var citation = new DefaultCitation();
        final var title = new DefaultInternationalString();
        title.add(Locale.JAPANESE, "アンダーカレント");
        title.add(Locale.ENGLISH,  "Undercurrent");
        citation.setTitle(title);
        citation.setISBN("9782505004509");
        citation.setPresentationForms(List.of(
                PresentationForm.DOCUMENT_HARDCOPY,
                PresentationForm.DOCUMENT_DIGITAL));
        citation.setAlternateTitles(Set.of(
                new SimpleInternationalString("Andākarento")));   // Actually a different script of the Japanese title.
        citation.setCitedResponsibleParties(List.of(
                new DefaultResponsibility(Role.AUTHOR, null, new DefaultIndividual("Testsuya Toyoda", null, null)),
                new DefaultResponsibility(Role.EDITOR, Extents.WORLD, new DefaultOrganisation("Kōdansha", null, null, null))));
        return citation;
    }

    /**
     * Tests the identifier map, which handles ISBN and ISSN codes in a special way.
     */
    @Test
    public void testIdentifierMap() {
        final var citation = new DefaultCitation();
        final Collection<Identifier> identifiers = citation.getIdentifiers();
        final IdentifierMap identifierMap = citation.getIdentifierMap();
        assertTrue(identifiers.isEmpty(), "Expected an initially empty set of identifiers.");
        /*
         * Set the ISBN code, and ensure that the ISBN is reflected in the identifier map.
         */
        citation.setISBN("MyISBN");
        assertEquals("MyISBN", citation.getISBN());
        assertEquals(1, identifiers.size(), "ISBN code shall be included in the set of identifiers.");
        assertEquals("{ISBN=“MyISBN”}", identifierMap.toString());
        /*
         * Set the identifiers with a list containing ISBN and ISSN codes.
         * The ISBN code shall be ignored because and ISBN property was already set.
         * The ISSN code shall be retained because it is a new code.
         */
        assertNull(citation.getISSN(), "ISSN shall be initially null.");
        citation.setIdentifiers(List.of(
                new DefaultIdentifier(Citations.NETCDF, "MyNetCDF"),
                new DefaultIdentifier(Citations.EPSG,   "MyEPSG"),
                new DefaultIdentifier(Citations.ISBN,   "NewISBN"),
                new DefaultIdentifier(Citations.ISSN,   "MyISSN")));

        assertEquals("NewISBN", citation.getISBN(), "The ISBN value shall have been overwritten.");
        assertEquals("MyISSN",  citation.getISSN(), "The ISSN value shall have been added, because new.");
        assertEquals("{NetCDF=“MyNetCDF”, EPSG=“MyEPSG”, ISBN=“NewISBN”, ISSN=“MyISSN”}", identifierMap.toString());
    }

    /**
     * Tests {@link DefaultCitation#transitionTo(DefaultCitation.State)} to the final state.
     */
    @Test
    public void testTransitionToFinal() {
        final DefaultCitation original = create();
        final DefaultCitation clone = create();
        clone.transitionTo(DefaultCitation.State.FINAL);
        assertEquals(DefaultCitation.State.EDITABLE, original.state());
        assertEquals(DefaultCitation.State.FINAL,    clone.state());
        assertEquals(original, clone);
        SimpleInternationalString title = new SimpleInternationalString("Undercurrent");
        original.setTitle(title);

        var e = assertThrows(UnmodifiableMetadataException.class, () -> clone.setTitle(title),
                             "Frozen metadata shall not be modifiable.");
        assertNotNull(e);
    }

    /**
     * Tests {@link MetadataCopier} on a citation.
     */
    public void testCopy() {
        final DefaultCitation original = create();
        final var clone = assertInstanceOf(DefaultCitation.class,
                new MetadataCopier(MetadataStandard.ISO_19115).copy(original));
        assertCopy(original, clone);
    }

    /**
     * Verifies that {@code clone} is a copy of {@code original}, sharing same instance of values when possible.
     */
    private static void assertCopy(final DefaultCitation original, final DefaultCitation clone) {
        assertNotSame(original, clone);
        assertSame(original.getISBN(),  clone.getISBN());
        assertSame(original.getTitle(), clone.getTitle());
        assertSame(assertSingleton(original.getAlternateTitles()),
                   assertSingleton(clone.getAlternateTitles()));

        assertCopy(original.getIdentifiers(),             clone.getIdentifiers());
        assertCopy(original.getCitedResponsibleParties(), clone.getCitedResponsibleParties());
        assertCopy(original.getPresentationForms(),       clone.getPresentationForms());
        /*
         * Verify the unique identifier, which is the ISBN code. ISBN and ISSN codes are handled
         * in a special way by DefaultCitation (they are instances of SpecializedIdentifier), but
         * the should nevertheless be cloned.
         */
        final Identifier ide = assertSingleton(original.getIdentifiers());
        final Identifier ida = assertSingleton(   clone.getIdentifiers());
        assertNotSame(ide, ida);
        assertSame(ide.getCode(),      ida.getCode());
        assertSame(ide.getAuthority(), ida.getAuthority());
        /*
         * Verify the author metadata.
         */
        final Responsibility re = CollectionsExt.first(original.getCitedResponsibleParties());
        final Responsibility ra = CollectionsExt.first(clone   .getCitedResponsibleParties());
        assertNotSame(re, ra);
        assertSame(re.getRole(), ra.getRole());
        assertSame(assertSingleton(re.getParties()).getName(),
                   assertSingleton(ra.getParties()).getName());
    }

    /**
     * Verifies that {@code actual} is an unmodifiable copy of {@code expected}.
     */
    private static <T> void assertCopy(final Collection<T> expected, final Collection<T> actual) {
        assertNotSame(expected, actual, "ModifiableMetadata.transitionTo(FINAL) shall have copied the collection.");
        assertEquals(expected, actual, "The copied collection shall have the same content as the original.");
        var e = assertThrows(UnsupportedOperationException.class, () -> actual.add(null),
                             "The copied collection shall be unmodifiable.");
        assertNotNull(e);
    }

    /**
     * Tests XML marshalling using the format derived form ISO 19115:2014 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        testMarshalling(Format.XML2016);
    }

    /**
     * Tests XML marshalling using the format derived form ISO 19115:2003 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshallingLegacy() throws JAXBException {
        testMarshalling(Format.XML2007);
    }

    /**
     * Tests XML marshalling for the given metadata version.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     */
    private void testMarshalling(final Format format) throws JAXBException {
        final var rs = new DefaultOnlineResource(URI.create("https://tools.ietf.org/html/rfc1149"));
        rs.setName(new SimpleInternationalString("IP over Avian Carriers"));
        rs.setDescription(new SimpleInternationalString("High delay, low throughput, and low altitude service."));
        rs.setFunction(OnLineFunction.OFFLINE_ACCESS);

        final var contact = new DefaultContact(rs);
        contact.setContactInstructions(new SimpleInternationalString("Send carrier pigeon."));
        contact.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "ip-protocol");
        final DefaultCitation c = new DefaultCitation("Fight against poverty");
        c.setCitedResponsibleParties(List.of(
                new DefaultResponsibility(Role.ORIGINATOR, null, new DefaultIndividual("Maid Marian", null, contact)),
                new DefaultResponsibility(Role.FUNDER,     null, new DefaultIndividual("Robin Hood",  null, contact))
        ));
        c.getDates().add(new DefaultCitationDate(OffsetDateTime.of(2015, 10, 17, 2, 0, 0, 0, ZoneOffset.ofHours(2)), DateType.ADOPTED));
        c.getPresentationForms().add(PresentationForm.PHYSICAL_OBJECT);
        /*
         * Check that XML file built by the marshaller is the same as the example file.
         */
        assertMarshalEqualsFile(openTestFile(format), c, format.schemaVersion, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests XML unmarshalling using the format derived form ISO 19115:2014 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        testUnmarshalling(Format.XML2016);
    }

    /**
     * Tests XML unmarshalling using the format derived form ISO 19115:2003 model.
     * This method also tests usage of {@code gml:id} and {@code xlink:href}.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshallingLegacy() throws JAXBException {
        testUnmarshalling(Format.XML2007);
    }

    /**
     * Tests XML unmarshalling for a metadata version.
     * The version is not specified since it should be detected automatically.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     */
    private void testUnmarshalling(final Format format) throws JAXBException {
        verifyUnmarshalledCitation(unmarshalFile(DefaultCitation.class, openTestFile(format)));
    }

    /**
     * Verifies the citation unmarshalled from the XML file.
     *
     * @param c  the citation.
     */
    public static void verifyUnmarshalledCitation(final Citation c) {
        assertTitleEquals("Fight against poverty", c, "citation");

        final CitationDate date = assertSingleton(c.getDates());
        assertEquals(date.getReferenceDate(), OffsetDateTime.of(2015, 10, 17, 2, 0, 0, 0, ZoneOffset.ofHours(2)));
        assertEquals(DateType.ADOPTED, date.getDateType());
        assertEquals(PresentationForm.PHYSICAL_OBJECT, assertSingleton(c.getPresentationForms()));

        final Iterator<? extends Responsibility> it = c.getCitedResponsibleParties().iterator();
        final Contact contact = assertResponsibilityEquals(Role.ORIGINATOR, "Maid Marian", it.next());
        assertEquals("Send carrier pigeon.", String.valueOf(contact.getContactInstructions()));

        final OnlineResource resource = assertSingleton(contact.getOnlineResources());
        assertEquals("IP over Avian Carriers", String.valueOf(resource.getName()));
        assertEquals("High delay, low throughput, and low altitude service.", String.valueOf(resource.getDescription()));
        assertEquals("https://tools.ietf.org/html/rfc1149", String.valueOf(resource.getLinkage()));
        assertEquals(OnLineFunction.OFFLINE_ACCESS, resource.getFunction());

        // Thanks to xlink:href, the Contact shall be the same instance as above.
        assertSame(contact, assertResponsibilityEquals(Role.FUNDER, "Robin Hood", it.next()));
        assertFalse(it.hasNext());
    }

    /**
     * Asserts that the given responsibility has the expected properties, then returns its contact info.
     */
    private static Contact assertResponsibilityEquals(final Role role, final String name, final Responsibility actual) {
        assertEquals(role, actual.getRole());
        final Party p = assertSingleton(actual.getParties());
        assertEquals(name, String.valueOf(p.getName()));
        return assertSingleton(p.getContactInfo());
    }
}
