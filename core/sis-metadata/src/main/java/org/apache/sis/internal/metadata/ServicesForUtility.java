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

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import javax.sql.DataSource;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.MetadataServices;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;

import static java.util.Collections.singleton;


/**
 * Implements the metadata services needed by the {@code "sis-utility"} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
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
    public CitationConstant getCitationConstant(final String name) {
        final Citation c = Citations.fromName(name);
        return (c instanceof CitationConstant) ? (CitationConstant) c : null;
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
        Identifier[]     alternateIdentifiers  = null;
        CharSequence     citedResponsibleParty = null;
        PresentationForm presentationForm      = null;
        Citation[]       copyFrom              = null;      // Copy citedResponsibleParty from those citations.
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
                copyFrom  = new Citation[] {Citations.ISO_19115.get(0)};
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
            } else if (key.equals("WMS")) {
                title                = "Web Map Server";                                      // OGC title
                alternateTitle       = "Geographic Information — Web map server interface";   // ISO title
                alternateIdentifiers = new Identifier[] {
                    new ImmutableIdentifier(null, "OGC", "06-042",  null, null),
                    new ImmutableIdentifier(null, "ISO", "19128", "2005", null)
                };
                edition          = "1.3";
                code             = "WMS";
                codeSpace        = "OGC";
                copyFrom         = new Citation[] {Citations.OGC, Citations.ISO_19115.get(0)};
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
            } else if (key.equals(Constants.OGC)) {
                title = "Identifiers in OGC namespace";
                code = Constants.OGC;
                citedResponsibleParty = "Open Geospatial Consortium";
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
            } else if (key.equals(Constants.IOGP)) {                    // Not in public API (see Citations.IOGP javadoc)
                title = "Using the EPSG Geodetic Parameter Dataset";    // Geomatics Guidance Note number 7, part 1
                code = Constants.IOGP;
                copyFrom = new Citation[] {Citations.EPSG};
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
                 * EPSGDataAccess.getAuthority() also add more information.
                 * After we moved the content of this citation in a database,
                 * EPSGDataAccess.getAuthority() should use this citation as a template.
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
        if (presentationForm      != null) c.setPresentationForms(singleton(presentationForm));
        if (citedResponsibleParty != null) {
            final DefaultResponsibleParty r = new DefaultResponsibleParty(Role.PRINCIPAL_INVESTIGATOR);
            r.setParties(singleton(new DefaultOrganisation(citedResponsibleParty, null, null, null)));
            c.setCitedResponsibleParties(singleton(r));
        }
        if (copyFrom != null) {
            for (final Citation other : copyFrom) {
                final Collection<? extends ResponsibleParty> parties = other.getCitedResponsibleParties();
                final Collection<ResponsibleParty> current = c.getCitedResponsibleParties();
                if (current != null) {
                    current.addAll(parties);
                } else {
                    c.setCitedResponsibleParties(parties);
                }
            }
        }
        if (alternateIdentifiers != null) {
            // getIdentifiers() should not return null at this point.
            c.getIdentifiers().addAll(Arrays.asList(alternateIdentifiers));
        }
        c.freeze();
        return c;
    }

    /**
     * Returns information about the Apache SIS configuration.
     * See super-class for a list of keys.
     *
     * @param  key A key identifying the information to return.
     * @param  locale Language to use if possible.
     * @return The information, or {@code null} if none.
     */
    @Override
    public String getInformation(final String key, final Locale locale) {
        /* switch (key) */ {
            if (key.equals("DataSource")) {
                Object server = null, database = null;
                try {
                    final DataSource ds = Initializer.getDataSource();
                    if (ds != null) {
                        final Class<?> type = ds.getClass();
                        database = type.getMethod("getDatabaseName", (Class[]) null).invoke(ds, (Object[]) null);
                        server   = type.getMethod("getServerName", (Class[]) null).invoke(ds, (Object[]) null);
                    }
                } catch (NoSuchMethodException e) {
                    Logging.recoverableException(Logging.getLogger(Loggers.SYSTEM),
                            MetadataServices.class, "getInformation", e);
                } catch (Exception e) {
                    // Leave the message alone if it contains at least 2 words.
                    String message = Exceptions.getLocalizedMessage(e, locale);
                    if (message == null || message.indexOf(' ') < 0) {
                        message = Classes.getShortClassName(e) + ": " + message;
                    }
                    return message;
                }
                if (database != null) {
                    if (server != null) {
                        database = "//" + server + '/' + database;
                    }
                    return database.toString();
                }
                return null;
            }
            // More cases may be added in future SIS versions.
        }
        return ReferencingServices.getInstance().getInformation(key, locale);
    }
}
