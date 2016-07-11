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
package org.apache.sis.services.csw;

import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultMetadataScope;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.apache.sis.test.TestUtilities.date;


/**
 * Tests {@link SummaryRecord}.
 *
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class SummaryRecordTest extends TestCase {
    /**
     * Programmatically create the metadata for part of the {@code <csw:Record>} example
     * given in section 6.3.3 of OGC 07-006r1 (Catalog Service Specification version 2.0.2).
     */
    private static DefaultMetadata createMetadata() {
        final DefaultCitation citation = new DefaultCitation("National Elevation Mapping Service for Texas");
        final DefaultServiceIdentification identification = new DefaultServiceIdentification(null, citation,
                "Elevation data collected for the National Elevation Dataset (NED).");
        identification.setExtents(singleton(new DefaultExtent(null, new DefaultGeographicBoundingBox(
                -108.440,       // west bound longitude
                 -96.223,       // east bound longitude
                  28.229,       // south bound matitude
                  34.353),      // north bound latitude
                null, null)));

        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setDateInfo(singleton(new DefaultCitationDate(date("2004-03-01 00:00:00"), DateType.CREATION)));
        metadata.setMetadataIdentifier(new DefaultIdentifier("ac522ef2-89a6-11db-91b1-7eea55d89593"));
        metadata.setMetadataScopes(singleton(new DefaultMetadataScope(ScopeCode.SERVICE, null)));
        metadata.setIdentificationInfo(singleton(identification));
        return metadata;
    }

    /**
     * Tests ISO 19115 to Dublin Core followed by XML marshalling.
     *
     * @throws JAXBException if an error occurred while marshalling the {@code SummaryRecord} object.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        final JAXBContext context = JAXBContext.newInstance(SummaryRecord.class);
        final Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final DefaultMetadata metadata = createMetadata();

        // TODO: temporary debugging code.
        System.out.println(metadata);
        System.out.println();
        m.marshal(new SummaryRecord(metadata, null), System.out);
    }
}
