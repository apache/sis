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
package org.apache.sis.internal.storage;

import java.util.Collection;
import java.nio.charset.Charset;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.AbstractParty;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.iso.Types;


/**
 * Helper methods for the metadata created by {@code DataStore} implementations.
 * This class creates the metadata objects only when first needed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class MetadataBuilder {
    /**
     * Elements to ignore in the legal notice to be parsed by {@link #parseLegalNotice(String)}.
     * Some of those elements are implied by the metadata were the legal notice will be stored.
     */
    private static final String[] IGNORABLE_NOTICE = {
        "(C)", "®", "All rights reserved"
    };

    /**
     * {@code true} for creating responsible parties as organization, or {@code false} for creating them as individual.
     */
    private boolean authorIsOrganization;

    /**
     * The metadata created by this reader, or {@code null} if none.
     */
    private DefaultMetadata metadata;

    /**
     * The identification information that are part of {@linkplain #metadata}, or {@code null} if none.
     */
    private DefaultDataIdentification identification;

    /**
     * The citation of data {@linkplain #identification}, or {@code null} if none.
     */
    private DefaultCitation citation;

    /**
     * Part of the responsible party of the {@linkplain #citation}, or {@code null} if none.
     */
    private DefaultResponsibility responsibility;

    /**
     * Part of the responsible party of the {@linkplain #citation}, or {@code null} if none.
     */
    private AbstractParty party;

    /**
     * The extent information that are part of {@linkplain #identification}, or {@code null} if none.
     */
    private DefaultExtent extent;

    /**
     * Copyright information, or {@code null} if none.
     */
    private DefaultLegalConstraints constraints;

    /**
     * Creates a new metadata reader.
     */
    public MetadataBuilder() {
    }

    /**
     * Returns the metadata as an unmodifiable object, or {@cod null} if none.
     * After this method has been invoked, the metadata can not be modified anymore.
     *
     * @return the metadata, or {@code null} if none.
     */
    public final DefaultMetadata result() {
        commit();
        if (metadata != null) {
            metadata.freeze();
        }
        return metadata;
    }

    /**
     * Performs the links between the objects created in this builder. After this method has been invoked,
     * all metadata objects should be reachable from the root {@link DefaultMetadata} object.
     */
    private void commit() {
        /*
         * Construction shall be ordered from children to parents.
         */
        if (extent != null) {
            identification().getExtents().add(extent);
            extent = null;
        }
        if (party != null) {
            responsibility().getParties().add(party);
            party = null;
        }
        if (responsibility != null) {
            citation().getCitedResponsibleParties().add(responsibility);
            responsibility = null;
        }
        if (citation != null) {
            identification().setCitation(citation);
            citation = null;
        }
        if (constraints != null) {
            identification().getResourceConstraints().add(constraints);
            constraints = null;
        }
        if (identification != null) {
            metadata().getIdentificationInfo().add(identification);
            identification = null;
        }
    }

    /**
     * Creates the metadata object if it does not already exists, then return it.
     *
     * @return the metadata (never {@code null}).
     */
    private DefaultMetadata metadata() {
        if (metadata == null) {
            metadata = new DefaultMetadata();
        }
        return metadata;
    }

    /**
     * Creates the identification information object if it does not already exists, then return it.
     *
     * @return the identification information (never {@code null}).
     */
    private DefaultDataIdentification identification() {
        if (identification == null) {
            identification = new DefaultDataIdentification();
        }
        return identification;
    }

    /**
     * Creates the citation object if it does not already exists, then return it.
     *
     * @return the citation information (never {@code null}).
     */
    private DefaultCitation citation() {
        if (citation == null) {
            citation = new DefaultCitation();
        }
        return citation;
    }

    /**
     * Creates the responsibility object if it does not already exists, then return it.
     *
     * @return the responsibility party (never {@code null}).
     */
    private DefaultResponsibility responsibility() {
        if (responsibility == null) {
            responsibility = new DefaultResponsibility();
        }
        return responsibility;
    }

    /**
     * Creates the person or organization information object if it does not already exists, then return it.
     *
     * @return the person or organization information (never {@code null}).
     */
    private AbstractParty party() {
        if (party == null) {
            party = authorIsOrganization ? new DefaultOrganisation() : new DefaultIndividual();
        }
        return party;
    }

    /**
     * Creates the extent information object if it does not already exists, then return it.
     *
     * @return the extent information (never {@code null}).
     */
    private DefaultExtent extent() {
        if (extent == null) {
            extent = new DefaultExtent();
        }
        return extent;
    }

    /**
     * Creates the constraints information object if it does not already exists, then return it.
     *
     * @return the constraints information (never {@code null}).
     */
    private DefaultLegalConstraints constraints() {
        if (constraints == null) {
            constraints = new DefaultLegalConstraints();
        }
        return constraints;
    }

    /**
     * Adds the given character encoding to the metadata.
     *
     * @param encoding  the character encoding to add.
     */
    public final void add(final Charset encoding) {
        if (encoding != null) {
            metadata().getCharacterSets().add(encoding);
        }
    }

    /**
     * Adds the given coordinate reference system to metadata, if it does not already exists.
     * This method ensures that there is no duplicated values. Comparisons ignore metadata.
     *
     * @param  crs  the coordinate reference system to add to the metadata, or {@code null} if none.
     */
    public final void add(final CoordinateReferenceSystem crs) {
        if (crs != null) {
            final Collection<ReferenceSystem> systems = metadata().getReferenceSystemInfo();
            for (final ReferenceSystem existing : systems) {
                if (Utilities.equalsIgnoreMetadata(crs, existing)) {
                    return;
                }
            }
            systems.add(crs);
        }
    }

    /**
     * Adds the given envelope, including its CRS, to the metadata. If the metadata already contains a geographic
     * bounding box, then a new bounding box is added; this method does not compute the union of the two boxes.
     *
     * @param  envelope  the extent to add in the metadata, or {@code null} if none.
     * @throws TransformException if an error occurred while converting the given envelope to extents.
     */
    public final void add(final AbstractEnvelope envelope) throws TransformException {
        if (envelope != null) {
            add(envelope.getCoordinateReferenceSystem());
            if (!envelope.isAllNaN()) {
                extent().addElements(envelope);
            }
        }
    }

    /**
     * Adds an author name.
     *
     * @param  author  the name of the author or publisher, or {@code null} if none.
     */
    public final void addAuthorName(final CharSequence author) {
        if (author != null) {
            if (party != null) {
                responsibility().getParties().add(party);
                party = null;
            }
            party().setName(Types.toInternationalString(author));
        }
    }

    /**
     * Parses the legal notice. The method expects a string of the form
     * “Copyright, John Smith, 1992. All rights reserved.”
     *
     * @param  notice  the legal notice, or {@code null} if none.
     */
    public final void parseLegalNotice(final String notice) {
        if (notice != null) {
            int year = 0;                                           // The copyright year, or 0 if none.
            Restriction restriction = null;                         // The kind of restriction (copyright, licence, etc.).
            final int length = notice.length();
            final StringBuilder name = new StringBuilder(length);   // Everything which is not one of the above or an ignored text.
            int start = CharSequences.skipLeadingWhitespaces(notice, 0, length);
parse:      for (int i = start; i < length;) {
                final int c = notice.codePointAt(i);
                final int n = Character.charCount(c);
                if (!Character.isLetterOrDigit(c)) {
                    /*
                     * Ignore text like "(C)" or "All rights reserved". Some of those statements
                     * are implied by the metadata were the legal notice will be stored.
                     */
                    for (final String ignorable : IGNORABLE_NOTICE) {
                        if (notice.regionMatches(true, start, ignorable, 0, ignorable.length())) {
                            start = i = CharSequences.skipLeadingWhitespaces(notice, i + ignorable.length(), length);
                            continue parse;
                        }
                    }
                    /*
                     * Convert text like "Copyright" or "Licence" into one of the Restriction enumerated values.
                     */
                    if (restriction == null) {
                        restriction = Types.forCodeName(Restriction.class, notice.substring(start, i), false);
                        if (restriction != null) {
                            constraints().getAccessConstraints().add(restriction);
                            start = i = CharSequences.skipLeadingWhitespaces(notice, i, length);
                            continue;
                        }
                    } else {
                        /*
                         * After we determined that the string begins by "Copyright" or another recognized string
                         * and is not one of the ignorable statements, store the remaining either in the buffer
                         * or as the copyright year.
                         */
                        if (year == 0 && Character.isDigit(notice.charAt(start))
                                      && Character.isDigit(notice.codePointBefore(i)))
                        {
                            try {
                                year = Integer.parseInt(notice.substring(start, i));
                                if (year >= 1800 && year <= 9999) {   // Those limits are arbitrary.
                                    start = i;                        // Accept as a copyright year.
                                    i += n;
                                    continue;
                                } else {
                                    year = 0;                         // Reject - not a copyright year.
                                }
                            } catch (NumberFormatException ex) {      // Ignore - not an integer.
                            }
                        }
                        name.append(notice, start, i);
                    }
                }
                i += n;
            }
            // TODO: store year and name.
        }
    }
}
