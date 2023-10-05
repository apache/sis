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
package org.apache.sis.storage.gpx;

import java.net.URI;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.Types;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.identification.BrowseGraphic;
import org.apache.sis.util.SimpleInternationalString;


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
 */
public final class Copyright implements LegalConstraints, Responsibility, Party, Citation, CitationDate {
    /**
     * The copyright holder.
     * This field is mandatory in principle, but {@code Copyright} implementation is robust to null value.
     * This field is mapped to the {@linkplain #getName() responsible party name} in ISO 19115 metadata.
     *
     * @see #getResponsibleParties()
     * @see #getParties()
     * @see #getName()
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
     * This field is mapped to the {@linkplain #getOnlineResources() online resources} in ISO 19115 metadata.
     *
     * @see #getOnlineResources()
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
resp:   for (final Responsibility r : c.getResponsibleParties()) {
            for (final Party p : r.getParties()) {
                author = Types.toString(p.getName(), locale);
                if (author != null) break resp;
            }
        }
        for (final Citation ci : c.getReferences()) {
            for (final CitationDate d : ci.getDates()) {
                final Date date = d.getDate();
                if (date != null) {
                    year = date.getYear() + 1900;
                    break;
                }
            }
            for (final OnlineResource r : ci.getOnlineResources()) {
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
     *
     * @see #getReferences()
     */
    @Override
    public Collection<Restriction> getUseConstraints() {
        if (license != null) {
            return List.of(Restriction.COPYRIGHT, Restriction.LICENCE);
        } else {
            return Collections.singleton(Restriction.COPYRIGHT);
        }
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return graphics or symbols indicating the constraint.
     */
    @Override
    public Collection<BrowseGraphic> getGraphics() {
        return Collections.emptySet();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #year} and {@link #license} fields.
     * Invoking this method is one of the steps in the path from the {@code LegalConstraints} root
     * to the {@link #getDate()} and {@link #getOnlineResources()} methods.
     *
     * @return citations for the limitation of constraint.
     *
     * @see #getTitle()
     * @see #getDates()
     * @see #getPresentationForms()
     * @see #getOnlineResources()
     */
    @Override
    public Collection<? extends Citation> getReferences() {
        return thisOrEmpty(year != null || license != null);
    }

    /**
     * ISO 19115 metadata property determined by the {@link #author} field.
     * Invoking this method is one of the steps in the path from the {@code LegalConstraints} root
     * to the {@link #getName()} method.
     *
     * @return parties responsible for the resource constraints.
     *
     * @see #getRole()
     * @see #getParties()
     * @see #getCitedResponsibleParties()
     */
    @Override
    public Collection<? extends Responsibility> getResponsibleParties() {
        return thisOrEmpty(author != null);
    }


    /* -------------------------------------------------------------------------------------------------
     * Implementation of the Responsibility object returned by LegalConstraints.getResponsibleParties().
     * Contains information about 'author' only (not 'year' or 'license').
     * ------------------------------------------------------------------------------------------------- */

    /**
     * ISO 19115 metadata property fixed to {@link Role#OWNER}.
     * This is part of the properties returned by {@link #getResponsibleParties()}.
     *
     * @return function performed by the responsible party.
     */
    @Override
    public Role getRole() {
        return Role.OWNER;
    }

    /**
     * ISO 19115 metadata property determined by the {@link #author} field.
     * This is part of the properties returned by {@link #getResponsibleParties()}.
     * Invoking this method is one of the steps in the path from the {@code LegalConstraints} root
     * to the {@link #getName()} method.
     *
     * @return information about the parties.
     *
     * @see #getName()
     */
    @Override
    public Collection<? extends Party> getParties() {
        return thisOrEmpty(author != null);
    }

    /**
     * ISO 19115 metadata property determined by the {@link #author} field.
     * This is part of the properties returned by {@link #getParties()}.
     *
     * @return name of the party, or {@code null} if none.
     */
    @Override
    public InternationalString getName() {
        return (author != null) ? new SimpleInternationalString(author) : null;
    }

    /**
     * Returns citation or party identifiers, which is an empty list.
     *
     * @return empty.
     */
    @Override
    public Collection<Identifier> getIdentifiers() {
        return Collections.emptyList();
    }


    /* -----------------------------------------------------------------------------------
     * Implementation of the Citation object returned by LegalConstraints.getReferences().
     * Contains information about 'year' or 'license' only (not 'author').
     * ----------------------------------------------------------------------------------- */

    /**
     * ISO 19115 metadata property not specified by GPX.
     * This is part of the properties returned by {@link #getReferences()}.
     * It would be the license title if that information was provided.
     *
     * @return the license name.
     */
    @Override
    public InternationalString getTitle() {
        return null;
    }

    /**
     * ISO 19115 metadata property determined by the {@link #year} field.
     * This is part of the properties returned by {@link #getReferences()}.
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
     * ISO 19115 metadata property fixed to {@link DateType#IN_FORCE}.
     * This is part of the properties returned by {@link #getDates()}.
     *
     * @return event used for reference date.
     */
    @Override
    public DateType getDateType() {
        return DateType.IN_FORCE;
    }

    /**
     * ISO 19115 metadata property fixed to {@link PresentationForm#DOCUMENT_DIGITAL}.
     * This is part of the properties returned by {@link #getReferences()}.
     *
     * @return the presentation mode of the license.
     */
    @Override
    public Collection<PresentationForm> getPresentationForms() {
        return Collections.singleton(PresentationForm.DOCUMENT_DIGITAL);
    }

    /**
     * ISO 19115 metadata property determined by the {@link #license} field.
     * This is part of the properties returned by {@link #getReferences()}.
     *
     * @return online references to the cited resource.
     */
    @Override
    public Collection<OnlineResource> getOnlineResources() {
        return (license != null) ? Collections.singleton(new Link(license)) : Collections.emptySet();
    }

    /**
     * Returns this object as a singleton if the given condition is {@code true},
     * or an empty set if the given condition is {@code false}.
     */
    private Collection<Copyright> thisOrEmpty(final boolean condition) {
        return condition ? Collections.singleton(this) : Collections.emptySet();
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