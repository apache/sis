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
package org.apache.sis.metadata.iso.citation;

import java.util.Date;
import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Series;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Standardized resource reference.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
public class DefaultCitation extends ISOMetadata implements Citation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2595269795652984755L;

    /**
     * Name by which the cited resource is known.
     */
    private InternationalString title;

    /**
     * Short name or other language name by which the cited information is known.
     * Example: "DCW" as an alternative title for "Digital Chart of the World.
     */
    private Collection<InternationalString> alternateTitles;

    /**
     * Reference date for the cited resource.
     */
    private Collection<CitationDate> dates;

    /**
     * Version of the cited resource.
     */
    private InternationalString edition;

    /**
     * Date of the edition in milliseconds elapsed sine January 1st, 1970,
     * or {@link Long#MIN_VALUE} if none.
     */
    private long editionDate;

    /**
     * Name and position information for an individual or organization that is responsible
     * for the resource. Returns an empty string if there is none.
     */
    private Collection<ResponsibleParty> citedResponsibleParties;

    /**
     * Mode in which the resource is represented, or an empty string if none.
     */
    private Collection<PresentationForm> presentationForms;

    /**
     * Information about the series, or aggregate dataset, of which the dataset is a part.
     * May be {@code null} if none.
     */
    private Series series;

    /**
     * Other information required to complete the citation that is not recorded elsewhere.
     * May be {@code null} if none.
     */
    private InternationalString otherCitationDetails;

    /**
     * Common title with holdings note. Note: title identifies elements of a series
     * collectively, combined with information about what volumes are available at the
     * source cited. May be {@code null} if there is no title.
     */
    private InternationalString collectiveTitle;

    /**
     * Constructs an initially empty citation.
     */
    public DefaultCitation() {
        editionDate = Long.MIN_VALUE;
    }

    /**
     * Returns the name by which the cited resource is known.
     */
    @Override
    public synchronized InternationalString getTitle() {
        return title;
    }

    /**
     * Sets the name by which the cited resource is known.
     *
     * @param newValue The new title.
     */
    public synchronized void setTitle(final InternationalString newValue) {
        checkWritePermission();
        title = newValue;
    }

    /**
     * Returns the short name or other language name by which the cited information is known.
     * Example: "DCW" as an alternative title for "Digital Chart of the World".
     */
    @Override
    public synchronized Collection<InternationalString> getAlternateTitles() {
        return alternateTitles = nonNullCollection(alternateTitles, InternationalString.class);
    }

    /**
     * Sets the short name or other language name by which the cited information is known.
     *
     * @param newValues The new alternate titles.
     */
    public synchronized void setAlternateTitles(final Collection<? extends InternationalString> newValues) {
        alternateTitles = copyCollection(newValues, alternateTitles, InternationalString.class);
    }

    /**
     * Returns the reference date for the cited resource.
     */
    @Override
    public synchronized Collection<CitationDate> getDates() {
        return dates = nonNullCollection(dates, CitationDate.class);
    }

    /**
     * Sets the reference date for the cited resource.
     *
     * @param newValues The new dates.
     */
    public synchronized void setDates(final Collection<? extends CitationDate> newValues) {
        dates = copyCollection(newValues, dates, CitationDate.class);
    }

    /**
     * Returns the version of the cited resource.
     */
    @Override
    public synchronized InternationalString getEdition() {
        return edition;
    }

    /**
     * Sets the version of the cited resource.
     *
     * @param newValue The new edition.
     */
    public synchronized void setEdition(final InternationalString newValue) {
        checkWritePermission();
        edition = newValue;
    }

    /**
     * Returns the date of the edition, or {@code null} if none.
     */
    @Override
    public synchronized Date getEditionDate() {
        return (editionDate != Long.MIN_VALUE) ? new Date(editionDate) : null;
    }

    /**
     * Sets the date of the edition, or {@code null} if none.
     *
     * @param newValue The new edition date.
     */
    public synchronized void setEditionDate(final Date newValue) {
        checkWritePermission();
        editionDate = (newValue != null) ? newValue.getTime() : Long.MIN_VALUE;
    }

    /**
     * Returns the unique identifier for the resource. Example: Universal Product Code (UPC),
     * National Stock Number (NSN).
     */
    @Override
    public Collection<Identifier> getIdentifiers() {
        return java.util.Collections.emptyList(); // TODO: Not yet implemented on intend.
    }

    /**
     * Returns the name and position information for an individual or organization that is
     * responsible for the resource. Returns an empty string if there is none.
     */
    @Override
    public synchronized Collection<ResponsibleParty> getCitedResponsibleParties() {
        return citedResponsibleParties = nonNullCollection(citedResponsibleParties, ResponsibleParty.class);
    }

    /**
     * Sets the name and position information for an individual or organization that is responsible
     * for the resource. Returns an empty string if there is none.
     *
     * @param newValues The new cited responsible parties.
     */
    public synchronized void setCitedResponsibleParties(final Collection<? extends ResponsibleParty> newValues) {
        citedResponsibleParties = copyCollection(newValues, citedResponsibleParties, ResponsibleParty.class);
    }

    /**
     * Returns the mode in which the resource is represented, or an empty string if none.
     */
    @Override
    public synchronized Collection<PresentationForm> getPresentationForms() {
        return presentationForms = nonNullCollection(presentationForms, PresentationForm.class);
    }

    /**
     * Sets the mode in which the resource is represented, or an empty string if none.
     *
     * @param newValues The new presentation form.
     */
    public synchronized void setPresentationForms(final Collection<? extends PresentationForm> newValues) {
        presentationForms = copyCollection(newValues, presentationForms, PresentationForm.class);
    }

    /**
     * Returns the information about the series, or aggregate dataset, of which the dataset is
     * a part. Returns {@code null} if none.
     */
    @Override
    public synchronized Series getSeries() {
        return series;
    }

    /**
     * Sets the information about the series, or aggregate dataset, of which the dataset is
     * a part. Set to {@code null} if none.
     *
     * @param newValue The new series.
     */
    public synchronized void setSeries(final Series newValue) {
        checkWritePermission();
        series = newValue;
    }

    /**
     * Returns other information required to complete the citation that is not recorded elsewhere.
     * Returns {@code null} if none.
     */
    @Override
    public synchronized InternationalString getOtherCitationDetails() {
        return otherCitationDetails;
    }

    /**
     * Sets other information required to complete the citation that is not recorded elsewhere.
     * Sets to {@code null} if none.
     *
     * @param newValue Other citations details.
     */
    public synchronized void setOtherCitationDetails(final InternationalString newValue) {
        checkWritePermission();
        otherCitationDetails = newValue;
    }

    /**
     * Returns the common title with holdings note. Note: title identifies elements of a series
     * collectively, combined with information about what volumes are available at the
     * source cited. Returns {@code null} if there is no title.
     */
    @Override
    public synchronized InternationalString getCollectiveTitle() {
        return collectiveTitle;
    }

    /**
     * Sets the common title with holdings note. Note: title identifies elements of a series
     * collectively, combined with information about what volumes are available at the
     * source cited. Set to {@code null} if there is no title.
     *
     * @param newValue The new collective title.
     */
    public synchronized void setCollectiveTitle(final InternationalString newValue) {
        checkWritePermission();
        collectiveTitle = newValue;
    }

    /**
     * Returns the International Standard Book Number, or {@code null} if none.
     */
    @Override
    public synchronized String getISBN() {
        return null; // Not yet implemented on intend.
    }

    /**
     * Returns the International Standard Serial Number, or {@code null} if none.
     */
    @Override
    public synchronized String getISSN() {
        return null; // Not yet implemented on intend.
    }
}
