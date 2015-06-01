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

import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.MetadataServices;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.util.iso.Types;


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
        CharSequence     citedResponsibleParty = null;
        PresentationForm presentationForm      = null;
        Citation         copyFrom              = null;  // Copy citedResponsibleParty from that citation.
        switch (key) {
            case "ISO 19115-1": {
                title     = "Geographic Information — Metadata Part 1: Fundamentals";
                edition   = "ISO 19115-1:2014(E)";
                code      = "19115-1";
                codeSpace = "ISO";
                citedResponsibleParty = "International Organization for Standardization";
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
                break;
            }
            case "ISO 19115-2": {
                title     = "Geographic Information — Metadata Part 2: Extensions for imagery and gridded data";
                edition   = "ISO 19115-2:2009(E)";
                code      = "19115-2";
                codeSpace = "ISO";
                copyFrom  = Citations.ISO_19115.get(0);
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
                break;
            }
            case Constants.OGC: {
                title     = "Identifier in OGC namespace";
                code      = "OGC";
                citedResponsibleParty = "Open Geospatial Consortium";
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
                break;
            }
            case Constants.EPSG: {
                title     = "EPSG Geodetic Parameter Dataset";
                code      = Constants.EPSG;
                codeSpace = Constants.IOGP;
                citedResponsibleParty = "International Association of Oil & Gas producers";
                presentationForm = PresentationForm.TABLE_DIGITAL;
                /*
                 * More complete information is provided as an ISO 19115 structure
                 * in EPSG Surveying and Positioning Guidance Note Number 7, part 1.
                 */
                break;
            }
            case Constants.SIS: {
                title = "Apache Spatial Information System";
                code  = key;
                break;
            }
            case "ISBN": {
                title = "International Standard Book Number";
                alternateTitle = key;
                break;
            }
            case "ISSN": {
                title = "International Standard Serial Number";
                alternateTitle = key;
                break;
            }
            case "Proj4": {
                title = "Proj.4";
                break;
            }
            case "S57": {
                title = "S-57";
                break;
            }
            default: return super.createCitation(key);
        }
        final DefaultCitation c = new DefaultCitation(title);
        if (alternateTitle        != null) c.getAlternateTitles().add(Types.toInternationalString(alternateTitle));
        if (edition               != null) c.setEdition(Types.toInternationalString(edition));
        if (code                  != null) c.getIdentifiers().add(new ImmutableIdentifier(null, codeSpace, code));
        if (copyFrom              != null) c.setCitedResponsibleParties(copyFrom.getCitedResponsibleParties());
        if (presentationForm      != null) c.getPresentationForms().add(presentationForm);
        if (citedResponsibleParty != null) {
            final DefaultOrganisation organisation = new DefaultOrganisation();
            organisation.setName(Types.toInternationalString(citedResponsibleParty));
            c.getCitedResponsibleParties().add(new DefaultResponsibility(Role.PRINCIPAL_INVESTIGATOR, null, organisation));
        }
        c.freeze();
        return c;
    }
}
