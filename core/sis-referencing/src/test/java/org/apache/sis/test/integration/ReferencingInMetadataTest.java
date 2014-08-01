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
import javax.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.spatial.GeometricObjectType;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.VectorSpatialRepresentation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.xml.NilObject;
import org.apache.sis.xml.NilReason;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.StandardCharsets;


/**
 * Tests XML (un)marshalling of referencing object inside metadata.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.datum.DefaultVerticalDatumTest.class
})
public final strictfp class ReferencingInMetadataTest extends XMLTestCase {
    /**
     * Tolerance threshold for strict floating point comparisons.
     */
    private static final double STRICT = 0;

    /**
     * The resource file which contains an XML representation
     * of a {@link Metadata} object with a {@link VerticalCRS}.
     */
    private static final String VERTICAL_CRS_XML = "Metadata with vertical CRS.xml";

    /**
     * Tests the (un)marshalling of a metadata with a vertical CRS.
     *
     * @throws JAXBException if the (un)marshalling process fails.
     */
    @Test
    public void testMetadataWithVerticalCRS() throws JAXBException {
        final Metadata metadata = unmarshalFile(Metadata.class, VERTICAL_CRS_XML);
        assertEquals("fileIdentifier", "20090901",                     metadata.getFileIdentifier());
        assertEquals("language",       Locale.ENGLISH,                 metadata.getLanguage());
        assertEquals("characterSet",   StandardCharsets.UTF_8,         metadata.getCharacterSet());
        assertEquals("dateStamp",      xmlDate("2014-01-04 00:00:00"), metadata.getDateStamp());
        /*
         * <gmd:contact>
         *   <gmd:CI_ResponsibleParty>
         *     …
         *   </gmd:CI_ResponsibleParty>
         * </gmd:contact>
         */
        final ResponsibleParty contact = getSingleton(metadata.getContacts());
        final OnlineResource onlineResource = contact.getContactInfo().getOnlineResource();
        assertNotNull("onlineResource", onlineResource);
        assertEquals("organisationName", "Apache SIS", contact.getOrganisationName().toString());
        assertEquals("linkage", URI.create("http://sis.apache.org"), onlineResource.getLinkage());
        assertEquals("function", OnLineFunction.INFORMATION, onlineResource.getFunction());
        assertEquals("role", Role.PRINCIPAL_INVESTIGATOR, contact.getRole());
        /*
         * <gmd:spatialRepresentationInfo>
         *   <gmd:MD_VectorSpatialRepresentation>
         *     …
         *   </gmd:MD_VectorSpatialRepresentation>
         * </gmd:spatialRepresentationInfo>
         */
        final SpatialRepresentation spatial = getSingleton(metadata.getSpatialRepresentationInfo());
        assertInstanceOf("spatialRepresentationInfo", VectorSpatialRepresentation.class, spatial);
        assertEquals("geometricObjectType", GeometricObjectType.POINT, getSingleton(
                ((VectorSpatialRepresentation) spatial).getGeometricObjects()).getGeometricObjectType());
        /*
         * <gmd:referenceSystemInfo>
         *   <gmd:MD_ReferenceSystem>
         *     …
         *   </gmd:MD_ReferenceSystem>
         * </gmd:referenceSystemInfo>
         */
        assertIdentifierEquals("referenceSystemInfo", null, "EPSG", null, "World Geodetic System 84",
                getSingleton(metadata.getReferenceSystemInfo()).getName());
        /*
         * <gmd:identificationInfo>
         *   <gmd:MD_DataIdentification>
         *     …
         */
        final DataIdentification identification = (DataIdentification) getSingleton(metadata.getIdentificationInfo());
        final Citation citation = identification.getCitation();
        assertInstanceOf("citation", NilObject.class, citation);
        assertEquals("nilReason", NilReason.MISSING, ((NilObject) citation).getNilReason());
        assertEquals("abstract", "SIS test", identification.getAbstract().toString());
        assertEquals("language", Locale.ENGLISH, getSingleton(identification.getLanguages()));
        /*
         * <gmd:geographicElement>
         *   <gmd:EX_GeographicBoundingBox>
         *     …
         *   </gmd:EX_GeographicBoundingBox>
         * </gmd:geographicElement>
         */
        final Extent extent = getSingleton(identification.getExtents());
        final GeographicBoundingBox bbox = (GeographicBoundingBox) getSingleton(extent.getGeographicElements());
        assertEquals("extentTypeCode", Boolean.TRUE, bbox.getInclusion());
        assertEquals("westBoundLongitude",  4.55, bbox.getWestBoundLongitude(), STRICT);
        assertEquals("eastBoundLongitude",  4.55, bbox.getEastBoundLongitude(), STRICT);
        assertEquals("southBoundLatitude", 44.22, bbox.getSouthBoundLatitude(), STRICT);
        assertEquals("northBoundLatitude", 44.22, bbox.getNorthBoundLatitude(), STRICT);
        /*
         * <gmd:verticalElement>
         *   <gmd:EX_VerticalExtent>
         *     …
         *   </gmd:EX_VerticalExtent>
         * </gmd:verticalElement>
         */
        final VerticalExtent ve = getSingleton(extent.getVerticalElements());
        assertEquals("minimumValue",   0.1, ve.getMinimumValue(), STRICT);
        assertEquals("maximumValue", 10000, ve.getMaximumValue(), STRICT);
        final VerticalCRS crs = ve.getVerticalCRS();
        verifyIdentifiers("test1", crs);
        assertEquals("scope", "World", crs.getScope().toString());
        final VerticalDatum datum = crs.getDatum();
        verifyIdentifiers("test2", datum);
        assertEquals("scope", "World", datum.getScope().toString());
        assertEquals("vertDatumType", VerticalDatumType.DEPTH, datum.getVerticalDatumType()); // Inferred from the name.
        final VerticalCS cs = crs.getCoordinateSystem();
        verifyIdentifiers("test3", cs);
        final CoordinateSystemAxis axis = cs.getAxis(0);
        verifyIdentifiers("test4", axis);
        assertEquals("axisAbbrev", "d", axis.getAbbreviation());
        assertEquals("axisDirection", AxisDirection.DOWN, axis.getDirection());
        /*
         *     …
         *   </gmd:MD_DataIdentification>
         * </gmd:identificationInfo>
         *
         * Now marshal the object and compare with the original file.
         */
        assertMarshalEqualsFile(VERTICAL_CRS_XML, metadata, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Verifies the name and identifier for the given object.
     *
     * @param code   The expected identifier code.
     * @param object The object to verify.
     */
    private static void verifyIdentifiers(final String code, final IdentifiedObject object) {
        assertIdentifierEquals("identifier", "SIS", "SIS", null, code, getSingleton(object.getIdentifiers()));
        assertIdentifierEquals("name", null, null, null, "Depth", object.getName());
    }
}
