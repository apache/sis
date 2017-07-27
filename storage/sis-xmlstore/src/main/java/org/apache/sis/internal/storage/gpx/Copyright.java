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
package org.apache.sis.internal.storage.gpx;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.Types;

// Branch-dependent imports
import org.opengis.metadata.citation.ResponsibleParty;
import org.apache.sis.metadata.iso.citation.AbstractParty;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.constraint.DefaultConstraints;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;


/**
 * Information about the copyright holder and any license governing use of a GPX file.
 * Information in this element allow to place data into the public domain or grant additional usage rights.
 * This element provides 3 properties:
 *
 * <ul>
 *   <li>The {@linkplain #author}, which is the only mandatory property.</li>
 *   <li>The copyright {@linkplain #year} (optional).</li>
 *   <li>An URI to the {@linkplain #license} (optional).</li>
 * </ul>
 *
 * Those properties can be read or modified directly. All methods defined in this class are bridges to
 * the ISO 19115 metadata model and can be ignored if the user only wants to manipulate the GPX model.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class Copyright implements LegalConstraints, ResponsibleParty, Citation, CitationDate {
    /**
     * The copyright holder.
     * This field is mandatory in principle, but {@code Copyright} implementation is robust to null value.
     * This field is mapped to the {@code getName() responsible party name} in ISO 19115 metadata.
     */
    @XmlAttribute(name = Attributes.AUTHOR, required = true)
    public String author;

    /**
     * The copyright year, or {@code null} if unspecified.
     * This field is mapped to the {@linkplain #getDate() citation date} in ISO 19115 metadata.
     *
     * @see #getDate()
     */
    @XmlElement(name = Tags.YEAR)
    public Integer year;

    /**
     * Link to an external file containing the license text, or {@code null} if none.
     * This field is mapped to the {@code getOnlineResources() online resources} in ISO 19115 metadata.
     */
    @XmlElement(name = Tags.LICENSE)
    public URI license;

    /**
     * Creates an initially empty instance.
     * Callers should set at least the {@link #author} field after construction.
     */
    public Copyright() {
    }

    /**
     * Copies properties from the given ISO 19115 metadata.
     */
    private Copyright(final LegalConstraints c, final Locale locale) {
        if (!(c instanceof DefaultConstraints)) {
            return;
        }
resp:   for (final DefaultResponsibility r : ((DefaultConstraints) c).getResponsibleParties()) {
            for (final AbstractParty p : r.getParties()) {
                author = Types.toString(p.getName(), locale);
                if (author != null) break resp;
            }
        }
        for (final Citation ci : ((DefaultConstraints) c).getReferences()) {
            for (final CitationDate d : ci.getDates()) {
                final Date date = d.getDate();
                if (date != null) {
                    year = date.getYear() + 1900;
                    break;
                }
            }
            if (!(ci instanceof DefaultCitation)) continue;
            for (final OnlineResource r : ((DefaultCitation) ci).getOnlineResources()) {
                license = r.getLinkage();
                if (license != null) break;
            }
        }
    }

    /**
     * Returns the given ISO 19115 metadata as a {@code Copyright} instance.
     * This method copies the data only if needed.
     *
     * @param  c       the ISO 19115 metadata, or {@code null}.
     * @param  locale  the locale to use for localized strings.
     * @return the GPX metadata, or {@code null}.
     */
    public static Copyright castOrCopy(final LegalConstraints c, final Locale locale) {
        return (c == null || c instanceof Copyright) ? (Copyright) c : new Copyright(c, locale);
    }

    /**
     * ISO 19115 metadata property fixed to {@link Restriction#COPYRIGHT}.
     *
     * @return restrictions or limitations on obtaining the data.
     */
    @Override
    public Collection<Restriction> getAccessConstraints() {
        return Collections.singleton(Restriction.COPYRIGHT);
    }

    /**
     * ISO 19115 metadata property determined by the {@link #license} field.
     *
     * @return restrictions or limitations or warnings on using the data.
     */
    @Override
    public Collection<Restriction> getUseConstraints() {
        if (license != null) {
            return Arrays.asList(Restriction.COPYRIGHT, Restriction.valueOf("LICENCE"));
        } else {
            return Collections.singleton(Restriction.COPYRIGHT);
        }
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return other restrictions and legal prerequisites for accessing and using the resource.
     */
    @Override
    public Collection<InternationalString> getOtherConstraints() {
        return Collections.emptySet();
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return limitation affecting the fitness for use of the resource.
     */
    @Override
    public Collection<InternationalString> getUseLimitations() {
        return Collections.emptySet();
    }


    /* -------------------------------------------------------------------------------------------------
     * Implementation of the Responsibility object returned by LegalConstraints.getResponsibleParties().
     * Contains information about 'author' only (not 'year' or 'license').
     * ------------------------------------------------------------------------------------------------- */

    /**
     * ISO 19115 metadata property fixed to {@link Role#OWNER}.
     * This is part of the properties returned by {@code getResponsibleParties()}.
     *
     * @return function performed by the responsible party.
     */
    @Override
    public Role getRole() {
        return Role.OWNER;
    }

    /**
     * ISO 19115 metadata property not specified by GPX. Actually could be the {@link #author},
     * but we have no way to know if the author is an individual or an organization.
     *
     * @return name of the organization, or {@code null} if none.
     */
    @Override
    public InternationalString getOrganisationName() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return position of the individual in the organization, or {@code null} if none.
     */
    @Override
    public InternationalString getPositionName() {
        return null;
    }

    /**
     * ISO 19115 metadata property determined by the {@link #author} field.
     * This is part of the properties returned by {@code getParties()}.
     *
     * @return name of the party, or {@code null} if none.
     */
    @Override
    public String getIndividualName() {
        return author;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getParties()}.
     *
     * @return contact information for the party.
     */
    @Override
    public Contact getContactInfo() {
        return null;
    }


    /* -----------------------------------------------------------------------------------
     * Implementation of the Citation object returned by LegalConstraints.getReferences().
     * Contains information about 'year' or 'license' only (not 'author').
     * ----------------------------------------------------------------------------------- */

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     * It would be the license title if that information was provided.
     *
     * @return the license name.
     */
    @Override
    public InternationalString getTitle() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return other names for the resource.
     */
    @Override
    public Collection<InternationalString> getAlternateTitles() {
        return Collections.emptySet();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #year} field.
     * This is part of the properties returned by {@code getReferences()}.
     * Invoking this method is one of the steps in the path from the {@code LegalConstraints} root
     * to the {@link #getDate()} method.
     *
     * @return reference date for the cited resource.
     *
     * @see #getDate()
     * @see #getDateType()
     */
    @Override
    public Collection<? extends CitationDate> getDates() {
        return thisOrEmpty(year != null);
    }

    /**
     * ISO 19115 metadata property determined by the {@link #year} field.
     * This is part of the properties returned by {@link #getDates()}.
     *
     * @return reference date for the cited resource.
     */
    @Override
    public Date getDate() {
        if (year != null) {
            return new Date(year - 1900, 0, 1);
        }
        return null;
    }

    /**
     * ISO 19115 metadata property fixed to {@code DateType.IN_FORCE}.
     * This is part of the properties returned by {@link #getDates()}.
     *
     * @return event used for reference date.
     */
    @Override
    public DateType getDateType() {
        return DateType.valueOf("IN_FORCE");
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return the license version, or {@code null} if none.
     */
    @Override
    public InternationalString getEdition() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return the license edition date, or {@code null} if none.
     */
    @Override
    public Date getEditionDate() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return the identifiers of the license.
     */
    @Override
    public Collection<Identifier> getIdentifiers() {
        return Collections.emptySet();
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     * It would be the license author if that information was provided.
     *
     * @return the information for individuals or organisations that are responsible for the license.
     */
    @Override
    public Collection<ResponsibleParty> getCitedResponsibleParties() {
        return Collections.emptySet();
    }

    /**
     * ISO 19115 metadata property fixed to {@link PresentationForm#DOCUMENT_DIGITAL}.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return the presentation mode of the license.
     */
    @Override
    public Collection<PresentationForm> getPresentationForms() {
        return Collections.singleton(PresentationForm.DOCUMENT_DIGITAL);
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return the series or aggregate dataset of which the dataset is a part.
     */
    @Override
    public Series getSeries() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return other details.
     */
    @Override
    public InternationalString getOtherCitationDetails() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return the common title.
     */
    @Override
    @Deprecated
    public InternationalString getCollectiveTitle() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return the International Standard Book Number.
     */
    @Override
    public String getISBN() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@code getReferences()}.
     *
     * @return the International Standard Serial Number.
     */
    @Override
    public String getISSN() {
        return null;
    }

    /**
     * Returns this object as a singleton if the given condition is {@code true},
     * or an empty set if the given condition is {@code false}.
     */
    private Collection<Copyright> thisOrEmpty(final boolean condition) {
        return condition ? Collections.<Copyright>singleton(this)
                         : Collections.<Copyright>emptySet();
    }

    /**
     * Compares this {@code Copyright} with the given object for equality.
     *
     * @param  obj  the object to compare with this {@code Copyright}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Copyright) {
            final Copyright that = (Copyright) obj;
            return Objects.equals(this.author,  that.author) &&
                   Objects.equals(this.year,    that.year) &&
                   Objects.equals(this.license, that.license);
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code Copyright}.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(author, year, license);
    }

    /**
     * Returns a string representation of the copyright statement.
     * The statement is formatted in a way similar to the copyright statements found in file header.
     * Example:
     *
     * <blockquote>
     * Copyright 2016 John Smith
     * http://john.smith.com
     * </blockquote>
     *
     * @return a string representation of the copyright statement.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Copyright");
        if (year    != null) sb.append(' ').append(year);
        if (author  != null) sb.append(' ').append(author);
        if (license != null) {
            sb.append(System.lineSeparator()).append(license);
        }
        return sb.toString();
    }
}
