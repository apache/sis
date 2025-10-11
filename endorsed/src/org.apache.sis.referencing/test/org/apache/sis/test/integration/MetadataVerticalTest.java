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
package org.apache.sis.test.integration;

import java.net.URI;
import java.util.Locale;
import java.time.LocalDate;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.*;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.spatial.GeometricObjectType;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.VectorSpatialRepresentation;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.VerticalDatum;
import org.apache.sis.system.Loggers;
import org.apache.sis.xml.NilObject;
import org.apache.sis.xml.NilReason;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertSingletonBBox;
import static org.apache.sis.test.Assertions.assertSingletonScope;
import org.apache.sis.xml.test.TestCase;

// Specific to the main branch:
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.temporal.TemporalDate;
import static org.apache.sis.test.GeoapiAssert.assertIdentifierEquals;


/**
 * Tests XML (un)marshalling of a metadata object containing a vertical extent
 * together with its vertical CRS description.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.metadata.iso.DefaultMetadataTest
 */
public final class MetadataVerticalTest extends TestCase.WithLogs {
    /**
     * Creates a new test case.
     */
    public MetadataVerticalTest() {
        super(Loggers.XML);
    }

    /**
     * Opens the stream to the XML representation of a {@link Metadata} object with a {@link VerticalCRS}.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return MetadataVerticalTest.class.getResourceAsStream("Metadata with vertical CRS.xml");
    }

    /**
     * Tests the (un)marshalling of a metadata with a vertical CRS.
     *
     * @throws JAXBException if the (un)marshalling process fails.
     */
    @Test
    public void testMetadataWithVerticalCRS() throws JAXBException {
        final Metadata metadata = unmarshalFile(Metadata.class, openTestFile());
        assertEquals("20090901",               metadata.getFileIdentifier());
        assertEquals(Locale.ENGLISH,           metadata.getLanguage());
        assertEquals(CharacterSet.UTF_8,       metadata.getCharacterSet());
        assertEquals(LocalDate.of(2014, 1, 4), TemporalDate.toTemporal(metadata.getDateStamp()));
        /*
         * <gmd:contact>
         *   <gmd:CI_ResponsibleParty>
         *     …
         *   </gmd:CI_ResponsibleParty>
         * </gmd:contact>
         */
        final ResponsibleParty contact = assertSingleton(metadata.getContacts());
        final OnlineResource onlineResource = contact.getContactInfo().getOnlineResource();
        assertNotNull(onlineResource);
        assertEquals("Apache SIS", contact.getOrganisationName().toString());
        assertEquals(URI.create("http://sis.apache.org"), onlineResource.getLinkage());
        assertEquals(OnLineFunction.INFORMATION, onlineResource.getFunction());
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, contact.getRole());
        /*
         * <gmd:spatialRepresentationInfo>
         *   <gmd:MD_VectorSpatialRepresentation>
         *     …
         *   </gmd:MD_VectorSpatialRepresentation>
         * </gmd:spatialRepresentationInfo>
         */
        final SpatialRepresentation spatial = assertSingleton(metadata.getSpatialRepresentationInfo());
        assertEquals(
                GeometricObjectType.POINT,
                assertSingleton(assertInstanceOf(VectorSpatialRepresentation.class, spatial).getGeometricObjects()).getGeometricObjectType());
        /*
         * <gmd:referenceSystemInfo>
         *   <gmd:MD_ReferenceSystem>
         *     …
         *   </gmd:MD_ReferenceSystem>
         * </gmd:referenceSystemInfo>
         */
        assertIdentifierEquals(null, "EPSG", null, "World Geodetic System 84",
                assertSingleton(metadata.getReferenceSystemInfo()).getName(),
                "referenceSystemInfo");
        /*
         * <gmd:identificationInfo>
         *   <gmd:MD_DataIdentification>
         *     …
         */
        final DataIdentification identification = assertInstanceOf(
                DataIdentification.class,
                assertSingleton(metadata.getIdentificationInfo()));
        final Citation citation = identification.getCitation();
        assertInstanceOf(NilObject.class, citation);
        assertEquals(NilReason.MISSING, ((NilObject) citation).getNilReason());
        assertEquals("SIS test", identification.getAbstract().toString());
        assertEquals(Locale.ENGLISH, assertSingleton(identification.getLanguages()));
        /*
         * <gmd:geographicElement>
         *   <gmd:EX_GeographicBoundingBox>
         *     …
         *   </gmd:EX_GeographicBoundingBox>
         * </gmd:geographicElement>
         */
        final GeographicBoundingBox bbox = assertSingletonBBox(identification);
        assertEquals(Boolean.TRUE, bbox.getInclusion());
        assertEquals( 4.55, bbox.getWestBoundLongitude());
        assertEquals( 4.55, bbox.getEastBoundLongitude());
        assertEquals(44.22, bbox.getSouthBoundLatitude());
        assertEquals(44.22, bbox.getNorthBoundLatitude());
        /*
         * <gmd:verticalElement>
         *   <gmd:EX_VerticalExtent>
         *     …
         *   </gmd:EX_VerticalExtent>
         * </gmd:verticalElement>
         */
        final Extent extent = assertSingleton(identification.getExtents());
        final VerticalExtent ve = assertSingleton(extent.getVerticalElements());
        assertEquals(  0.1, ve.getMinimumValue());
        assertEquals(10000, ve.getMaximumValue());
        final VerticalCRS crs = ve.getVerticalCRS();
        verifyIdentifiers("test1", crs);
        assertEquals("World", assertSingletonScope(crs));
        final VerticalDatum datum = crs.getDatum();
        verifyIdentifiers("test2", datum);
        assertEquals("World", assertSingletonScope(datum));
        assertEquals(VerticalDatumType.DEPTH, datum.getVerticalDatumType());    // Inferred from the name.
        final VerticalCS cs = crs.getCoordinateSystem();
        verifyIdentifiers("test3", cs);
        final CoordinateSystemAxis axis = cs.getAxis(0);
        verifyIdentifiers("test4", axis);
        assertEquals("d", axis.getAbbreviation());
        assertEquals(AxisDirection.DOWN, axis.getDirection());
        /*
         *     …
         *   </gmd:MD_DataIdentification>
         * </gmd:identificationInfo>
         *
         * Now marshal the object and compare with the original file.
         */
        assertMarshalEqualsFile(openTestFile(), metadata, VERSION_2007, "xmlns:*", "xsi:schemaLocation");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies the name and identifier for the given object.
     *
     * @param  code    the expected identifier code.
     * @param  object  the object to verify.
     */
    private static void verifyIdentifiers(final String code, final IdentifiedObject object) {
        assertIdentifierEquals("Apache Spatial Information System", "SIS",
                null, code, assertSingleton(object.getIdentifiers()), "identifier");
        assertIdentifierEquals(null, null, null, "Depth", object.getName(), "name");
    }
}
