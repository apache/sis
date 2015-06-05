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
package org.apache.sis.internal.metadata;

import java.util.Collection;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Responsibility;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.MetadataServices;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.util.iso.Types;

import static java.util.Collections.singleton;


/**
 * Implements the metadata services needed by the {@code "sis-utility"} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class ServicesForUtility extends MetadataServices {
    /**
     * Creates a new instance. This constructor is invoked by reflection only.
     */
    public ServicesForUtility() {
    }

    /**
     * Returns the constant defined in the {@link Citations} class for the given name.
     *
     * @param  name The name of one of the citation constants defined in the {@code Citations} class.
     * @return The requested citation, or {@code null} if there is no constant for the given name.
     */
    @Override
    public Citation getCitationConstant(final String name) {
        final Citation c = Citations.fromName(name);
        /*
         * The fact that the following line uses the citation class as a non-public criterion for identifying
         * when the Citations.fromName(String) method found no match is documented in that Citations.fromName
         * method body. If we do not rely anymore on this criterion, please update the Citations.fromName(…)
         * comment accordingly.
         */
        return (c.getClass() != SimpleCitation.class) ? c : null;
    }

    /**
     * Returns the build-in citation for the given primary key, or {@code null}.
     *
     * @param  key The primary key of the desired citation.
     * @return The requested citation, or {@code null} if unknown.
     *
     * @todo The content is hard-coded for now. But the plan for a future version is to fetch richer information
     *       from a database, including for example the responsible party and the URL. However that method would
     *       need to make sure that the given key is present in the alternate titles, since we rely on that when
     *       checking for code spaces.
     */
    @Override
    public Citation createCitation(final String key) {
        CharSequence     title;
        CharSequence     alternateTitle        = null;
        CharSequence     edition               = null;
        String           code                  = null;
        String           codeSpace             = null;
        String           version               = null;
        CharSequence     citedResponsibleParty = null;
        PresentationForm presentationForm      = null;
        Citation         copyFrom              = null;  // Copy citedResponsibleParty from that citation.
        { // This is a switch(String) on the JDK7 branch
            if (key.equals("ISO 19115-1")) {
                title     = "Geographic Information — Metadata Part 1: Fundamentals";
                edition   = "ISO 19115-1:2014(E)";
                code      = "19115-1";
                codeSpace = "ISO";
                version   = "2014(E)";
                citedResponsibleParty = "International Organization for Standardization";
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
            } else if (key.equals("ISO 19115-2")) {
                title     = "Geographic Information — Metadata Part 2: Extensions for imagery and gridded data";
                edition   = "ISO 19115-2:2009(E)";
                code      = "19115-2";
                codeSpace = "ISO";
                version   = "2009(E)";
                copyFrom  = Citations.ISO_19115.get(0);
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
            } else if (key.equals(Constants.OGC)) {
                title     = "Identifier in OGC namespace";
                code      = "OGC";
                citedResponsibleParty = "Open Geospatial Consortium";
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
            } else if (key.equals(Constants.EPSG)) {
                title     = "EPSG Geodetic Parameter Dataset";
                code      = Constants.EPSG;
                codeSpace = Constants.IOGP;
                citedResponsibleParty = "International Association of Oil & Gas producers";
                presentationForm = PresentationForm.TABLE_DIGITAL;
                /*
                 * More complete information is provided as an ISO 19115 structure
                 * in EPSG Surveying and Positioning Guidance Note Number 7, part 1.
                 */
            } else if (key.equals(Constants.SIS)) {
                title = "Apache Spatial Information System";
                code  = key;
            } else if (key.equals("ISBN")) {
                title = "International Standard Book Number";
                alternateTitle = key;
            } else if (key.equals("ISSN")) {
                title = "International Standard Serial Number";
                alternateTitle = key;
            } else if (key.equals("Proj4")) {
                title = "Proj.4";
            } else if (key.equals("S57")) {
                title = "S-57";
            } else {
                return super.createCitation(key);
            }
        }
        /*
         * Do not use the 'c.getFoo().add(foo)' pattern below. Use the 'c.setFoo(singleton(foo))' pattern instead.
         * This is because this method may be invoked during XML serialization, in which case some getter methods
         * may return null (for preventing JAXB to marshal some empty elements).
         */
        final DefaultCitation c = new DefaultCitation(title);
        if (alternateTitle        != null) c.setAlternateTitles(singleton(Types.toInternationalString(alternateTitle)));
        if (edition               != null) c.setEdition(Types.toInternationalString(edition));
        if (code                  != null) c.setIdentifiers(singleton(new ImmutableIdentifier(null, codeSpace, code, version, null)));
        if (copyFrom              != null) c.setCitedResponsibleParties(copyFrom.getCitedResponsibleParties());
        if (presentationForm      != null) c.setPresentationForms(singleton(presentationForm));
        if (citedResponsibleParty != null) {
            final DefaultOrganisation organisation = new DefaultOrganisation();
            organisation.setName(Types.toInternationalString(citedResponsibleParty));
            final DefaultResponsibility r = new DefaultResponsibility(Role.PRINCIPAL_INVESTIGATOR, null, organisation);
            final Collection<Responsibility> parties = c.getCitedResponsibleParties();
            if (parties != null) {
                parties.add(r);
            } else {
                c.setCitedResponsibleParties(singleton(r));
            }
        }
        c.freeze();
        return c;
    }
}
