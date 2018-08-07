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
package org.apache.sis.metadata.sql;

import org.opengis.util.CodeList;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.xml.NilReason;

import static java.util.Collections.singleton;

// Branch-dependent import
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;


/**
 * A fallback providing hard-coded values of metadata entities.
 * Used when connection to the spatial metadata can not be established.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class MetadataFallback extends MetadataSource {
    /**
     * The unique instance of this fallback.
     */
    static final MetadataFallback INSTANCE = new MetadataFallback();

    /**
     * Creates the singleton.
     */
    private MetadataFallback() {
    }

    /**
     * Searches for the given metadata in the hard-coded list.
     *
     * @param  metadata  the metadata to search for.
     * @return the identifier of the given metadata, or {@code null} if none.
     */
    @Override
    public String search(final Object metadata) {
        ArgumentChecks.ensureNonNull("metadata", metadata);
        return null;
    }

    /**
     * Returns a hard-coded metadata filled with the data referenced by the specified identifier.
     * Alternatively, this method can also return a {@code CodeList} or {@code Enum} element.
     *
     * @param  <T>         the parameterized type of the {@code type} argument.
     * @param  type        the interface to implement, or {@code CodeList} or some {@code Enum} types.
     * @param  identifier  the identifier of hard-coded values for the metadata entity to be returned.
     * @return an implementation of the required interface, or the code list element.
     */
    @Override
    public <T> T lookup(final Class<T> type, final String identifier) {
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        Object value;
        if (CodeList.class.isAssignableFrom(type)) {
            value = getCodeList(type, identifier);
        } else {
            value = null;
            if (type == Citation.class) {
                value = createCitation(identifier);
            }
            if (value == null) {
                return NilReason.MISSING.createNilObject(type);
            }
        }
        return type.cast(value);
    }

    /**
     * Returns the build-in citation for the given primary key, or {@code null}.
     * The content in this method should be consistent with the content provided
     * in the {@code "Citations.sql"} script (this is verified by JUnit tests).
     *
     * @param  key  the primary key of the desired citation.
     * @return the requested citation, or {@code null} if unknown.
     */
    static Citation createCitation(final String key) {
        CharSequence     title;
        CharSequence     alternateTitle        = null;
        CharSequence     edition               = null;
        String           code                  = null;
        String           codeSpace             = null;
        String           version               = null;
        CharSequence     citedResponsibleParty = null;
        PresentationForm presentationForm      = null;
        String           copyFrom              = null;      // Copy citedResponsibleParty from those citations.
        switch (key) {
            case "ISO 19115-1": {
                title     = "Geographic Information — Metadata Part 1: Fundamentals";
                edition   = "ISO 19115-1:2014(E)";
                code      = "19115-1";
                codeSpace = "ISO";
                version   = "2014(E)";
                citedResponsibleParty = "International Organization for Standardization";
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
                break;
            }
            case "ISO 19115-2": {
                title     = "Geographic Information — Metadata Part 2: Extensions for imagery and gridded data";
                edition   = "ISO 19115-2:2009(E)";
                code      = "19115-2";
                codeSpace = "ISO";
                version   = "2009(E)";
                copyFrom  = "ISO 19115-1";
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
                break;
            }
            case "WMS": {
                title             = "Web Map Server";
                alternateTitle    = "WMS";
                edition = version = "1.3";
                code              = "WMS";      // Note: OGC internal code is 06-042.
                codeSpace         = "OGC";
                copyFrom          = "OGC";
                presentationForm  = PresentationForm.DOCUMENT_DIGITAL;
                break;
            }
            case "OGC": {
                title                 = "OGC Naming Authority";
                code                  = Constants.OGC;
                citedResponsibleParty = "Open Geospatial Consortium";
                presentationForm      = PresentationForm.DOCUMENT_DIGITAL;
                break;
            }
            case "IOGP": {          // Not in public API (see Citations.IOGP javadoc)
                title = "IOGP Surveying and Positioning Guidance Note 7";
                code             = Constants.IOGP;
                copyFrom         = Constants.EPSG;
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
                break;
            }
            case Constants.EPSG: {
                title                 = "EPSG Geodetic Parameter Dataset";
                alternateTitle        = "EPSG Dataset";
                code                  = Constants.EPSG;
                codeSpace             = Constants.IOGP;
                citedResponsibleParty = "International Association of Oil & Gas producers";
                presentationForm      = PresentationForm.TABLE_DIGITAL;
                break;
            }
            case Constants.SIS: {
                title     = "Apache Spatial Information System";
                code      = key;
                codeSpace = "Apache";
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
            case Constants.PROJ4: {
                title     = "Proj";
                code      = "Proj4";
                codeSpace = "OSGeo";
                break;
            }
            case "IHO S-57": {
                title = code     = "S-57";
                codeSpace        = "IHO";
                version          = "3.1";
                presentationForm = PresentationForm.DOCUMENT_DIGITAL;
                break;
            }
            default: return null;
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
            c.setCitedResponsibleParties(createCitation(copyFrom).getCitedResponsibleParties());
        }
        c.transition(DefaultCitation.State.FINAL);
        return c;
    }

    /**
     * Ignored.
     */
    @Override
    public void addWarningListener(WarningListener<? super MetadataSource> listener) {
    }

    /**
     * Ignored.
     */
    @Override
    public void removeWarningListener(WarningListener<? super MetadataSource> listener) {
    }

    /**
     * Ignored.
     */
    @Override
    public void close() {
    }
}
