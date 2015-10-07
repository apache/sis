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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.annotation.UML;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.Types;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifierMap;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.internal.metadata.MetadataUtilities.toDate;
import static org.apache.sis.internal.metadata.MetadataUtilities.toMilliseconds;


/**
 * Standardized resource reference.
 *
 * <div class="section">Unified identifiers view</div>
 * The ISO 19115 model provides specific attributes for the {@linkplain #getISBN() ISBN} and
 * {@linkplain #getISSN() ISSN} codes. However the SIS library handles those codes like any
 * other identifiers. Consequently the ISBN and ISSN codes are included in the collection
 * returned by {@link #getIdentifiers()}, except at XML marshalling time (for ISO 19139 compliance).
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@XmlType(name = "CI_Citation_Type", propOrder = {
    "title",
    "alternateTitles",
    "dates",
    "edition",
    "editionDate",
    "identifiers",
    "citedResponsibleParties",
    "presentationForms",
    "series",
    "otherCitationDetails",
    "collectiveTitle",
    "ISBN",
    "ISSN"
})
@XmlRootElement(name = "CI_Citation")
public class DefaultCitation extends ISOMetadata implements Citation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3490090845236158848L;

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
    private long editionDate = Long.MIN_VALUE;

    /**
     * Roles, Name, contact, and position information for an individual or organization that is responsible
     * for the resource.
     */
    private Collection<ResponsibleParty> citedResponsibleParties;

    /**
     * Mode in which the resource is represented, or an empty collection if none.
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
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Deprecated
    private InternationalString collectiveTitle;

    /**
     * Online references to the cited resource.
     */
    private Collection<OnlineResource> onlineResources;

    /**
     * Citation graphic or logo for cited party.
     */
    private Collection<BrowseGraphic> graphics;

    /**
     * Constructs an initially empty citation.
     */
    public DefaultCitation() {
    }

    /**
     * Constructs a citation with the specified title.
     *
     * @param title The title as a {@link String} or an {@link InternationalString} object,
     *        or {@code null} if none.
     */
    public DefaultCitation(final CharSequence title) {
        this.title = Types.toInternationalString(title);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Citation)
     */
    public DefaultCitation(final Citation object) {
        super(object);
        if (object != null) {
            title                   = object.getTitle();
            alternateTitles         = copyCollection(object.getAlternateTitles(), InternationalString.class);
            dates                   = copyCollection(object.getDates(), CitationDate.class);
            edition                 = object.getEdition();
            editionDate             = toMilliseconds(object.getEditionDate());
            identifiers             = copyCollection(object.getIdentifiers(), Identifier.class);
            citedResponsibleParties = copyCollection(object.getCitedResponsibleParties(), ResponsibleParty.class);
            presentationForms       = copyCollection(object.getPresentationForms(), PresentationForm.class);
            series                  = object.getSeries();
            otherCitationDetails    = object.getOtherCitationDetails();
            collectiveTitle         = object.getCollectiveTitle();
            if (object instanceof DefaultCitation) {
                final DefaultCitation c = (DefaultCitation) object;
                onlineResources = copyCollection(c.getOnlineResources(), OnlineResource.class);
                graphics        = copyCollection(c.getGraphics(), BrowseGraphic.class);
            }
            final String id1        = object.getISBN();
            final String id2        = object.getISSN();
            if (id1 != null || id2 != null) {
                final IdentifierMap map = super.getIdentifierMap();
                if (id1 != null) map.putSpecialized(Citations.ISBN, id1);
                if (id2 != null) map.putSpecialized(Citations.ISSN, id2);
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultCitation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultCitation} instance is created using the
     *       {@linkplain #DefaultCitation(Citation) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCitation castOrCopy(final Citation object) {
        if (object == null || object instanceof DefaultCitation) {
            return (DefaultCitation) object;
        }
        return new DefaultCitation(object);
    }

    /**
     * Returns the name by which the cited resource is known.
     *
     * @return The cited resource name, or {@code null}.
     */
    @Override
    @XmlElement(name = "title", required = true)
    public InternationalString getTitle() {
        return title;
    }

    /**
     * Sets the name by which the cited resource is known.
     *
     * @param newValue The new title, or {@code null} if none.
     */
    public void setTitle(final InternationalString newValue) {
        checkWritePermission();
        title = newValue;
    }

    /**
     * Returns short name or other language name by which the cited information is known.
     *
     * <div class="note"><b>Example:</b> "DCW" as an alternative title for "Digital Chart of the World".</div>
     *
     * @return Other names for the resource, or an empty collection if none.
     */
    @Override
    @XmlElement(name = "alternateTitle")
    public Collection<InternationalString> getAlternateTitles() {
        return alternateTitles = nonNullCollection(alternateTitles, InternationalString.class);
    }

    /**
     * Sets the short name or other language name by which the cited information is known.
     *
     * @param newValues The new alternate titles, or {@code null} if none.
     */
    public void setAlternateTitles(final Collection<? extends InternationalString> newValues) {
        alternateTitles = writeCollection(newValues, alternateTitles, InternationalString.class);
    }

    /**
     * Returns the reference date for the cited resource.
     *
     * @return The reference date.
     */
    @Override
    @XmlElement(name = "date", required = true)
    public Collection<CitationDate> getDates() {
        return dates = nonNullCollection(dates, CitationDate.class);
    }

    /**
     * Sets the reference date for the cited resource.
     *
     * @param newValues The new dates, or {@code null} if none.
     */
    public void setDates(final Collection<? extends CitationDate> newValues) {
        dates = writeCollection(newValues, dates, CitationDate.class);
    }

    /**
     * Returns the version of the cited resource.
     *
     * @return The version, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "edition")
    public InternationalString getEdition() {
        return edition;
    }

    /**
     * Sets the version of the cited resource.
     *
     * @param newValue The new edition, or {@code null} if none.
     */
    public void setEdition(final InternationalString newValue) {
        checkWritePermission();
        edition = newValue;
    }

    /**
     * Returns the date of the edition.
     *
     * @return The edition date, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "editionDate")
    public Date getEditionDate() {
        return toDate(editionDate);
    }

    /**
     * Sets the date of the edition.
     *
     * @param newValue The new edition date, or {@code null} if none.
     */
    public void setEditionDate(final Date newValue) {
        checkWritePermission();
        editionDate = toMilliseconds(newValue);
    }

    /**
     * Returns the unique identifier for the resource.
     *
     * <div class="note"><b>Example:</b> Universal Product Code (UPC), National Stock Number (NSN).</div>
     *
     * <div class="section">Unified identifiers view</div>
     * In this SIS implementation, the collection returned by this method includes the XML identifiers
     * ({@linkplain IdentifierSpace#ID ID}, {@linkplain IdentifierSpace#UUID UUID}, <i>etc.</i>),
     * as well as the {@linkplain #getISBN() ISBN} and {@linkplain #getISSN() ISSN} codes, thus
     * providing a unified view of every kind of identifiers associated to this citation.
     *
     * <div class="note"><b>XML note:</b>
     * The {@code <gmd:identifier>} element marshalled to XML will exclude all the above cited identifiers,
     * for ISO 19139 compliance. Those identifiers will appear in other XML elements or attributes.</div>
     *
     * @return The identifiers, or an empty collection if none.
     *
     * @see #getISBN()
     * @see #getISSN()
     * @see #getIdentifierMap()
     */
    @Override
    @XmlElement(name = "identifier")
    public Collection<Identifier> getIdentifiers() {
        return NonMarshalledAuthority.filterOnMarshalling(super.getIdentifiers());
    }

    /**
     * Sets the unique identifier for the resource.
     * Example: Universal Product Code (UPC), National Stock Number (NSN).
     *
     * <p>XML identifiers ({@linkplain IdentifierSpace#ID ID}, {@linkplain IdentifierSpace#UUID UUID}, <i>etc.</i>),
     * {@linkplain #getISBN() ISBN} and {@linkplain #getISSN() ISSN} codes are not affected by this method, unless
     * they are explicitely provided in the given collection.</p>
     *
     * @param newValues The new identifiers, or {@code null} if none.
     *
     * @see #setISBN(String)
     * @see #setISSN(String)
     */
    public void setIdentifiers(Collection<? extends Identifier> newValues) {
        newValues = NonMarshalledAuthority.setMarshallables(identifiers, newValues);
        identifiers = writeCollection(newValues, identifiers, Identifier.class);
    }

    /**
     * Returns the role, name, contact and position information for an individual or organization
     * that is responsible for the resource.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @return The individual or organization that is responsible, or an empty collection if none.
     */
    @Override
    @XmlElement(name = "citedResponsibleParty")
    public Collection<ResponsibleParty> getCitedResponsibleParties() {
        return citedResponsibleParties = nonNullCollection(citedResponsibleParties, ResponsibleParty.class);
    }

    /**
     * Sets the role, name, contact and position information for an individual or organization
     * that is responsible for the resource.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @param newValues The new cited responsible parties, or {@code null} if none.
     */
    public void setCitedResponsibleParties(final Collection<? extends ResponsibleParty> newValues) {
        citedResponsibleParties = writeCollection(newValues, citedResponsibleParties, ResponsibleParty.class);
    }

    /**
     * Returns the mode in which the resource is represented.
     *
     * @return The presentation modes, or an empty collection if none.
     */
    @Override
    @XmlElement(name = "presentationForm")
    public Collection<PresentationForm> getPresentationForms() {
        return presentationForms = nonNullCollection(presentationForms, PresentationForm.class);
    }

    /**
     * Sets the mode in which the resource is represented.
     *
     * @param newValues The new presentation form, or {@code null} if none.
     */
    public void setPresentationForms(final Collection<? extends PresentationForm> newValues) {
        presentationForms = writeCollection(newValues, presentationForms, PresentationForm.class);
    }

    /**
     * Returns the information about the series, or aggregate dataset, of which the dataset is a part.
     *
     * @return The series of which the dataset is a part, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "series")
    public Series getSeries() {
        return series;
    }

    /**
     * Sets the information about the series, or aggregate dataset, of which the dataset is a part.
     *
     * @param newValue The new series.
     */
    public void setSeries(final Series newValue) {
        checkWritePermission();
        series = newValue;
    }

    /**
     * Returns other information required to complete the citation that is not recorded elsewhere.
     *
     * <div class="warning"><b>Upcoming API change — multiplicity</b><br>
     * As of ISO 19115:2014, this singleton has been replaced by a collection.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @return Other details, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "otherCitationDetails")
    public InternationalString getOtherCitationDetails() {
        return otherCitationDetails;
    }

    /**
     * Sets other information required to complete the citation that is not recorded elsewhere.
     *
     * <div class="warning"><b>Upcoming API change — multiplicity</b><br>
     * As of ISO 19115:2014, this singleton has been replaced by a collection.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @param newValue Other citations details, or {@code null} if none.
     */
    public void setOtherCitationDetails(final InternationalString newValue) {
        checkWritePermission();
        otherCitationDetails = newValue;
    }

    /**
     * Returns the common title with holdings note.
     *
     * @return The common title, or {@code null} if none.
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Override
    @Deprecated
    @XmlElement(name = "collectiveTitle")
    public InternationalString getCollectiveTitle() {
        return collectiveTitle;
    }

    /**
     * Sets the common title with holdings note. This title identifies elements of a series collectively,
     * combined with information about what volumes are available at the source cited.
     *
     * @param newValue The new collective title, or {@code null} if none.
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Deprecated
    public void setCollectiveTitle(final InternationalString newValue) {
        checkWritePermission();
        collectiveTitle = newValue;
    }

    /**
     * Returns the International Standard Book Number.
     * In this SIS implementation, invoking this method is equivalent to:
     *
     * {@preformat java
     *   return getIdentifierMap().getSpecialized(Citations.ISBN);
     * }
     *
     * @return The ISBN, or {@code null} if none.
     *
     * @see #getIdentifiers()
     * @see Citations#ISBN
     */
    @Override
    @XmlElement(name = "ISBN")
    public String getISBN() {
        return isNullOrEmpty(identifiers) ? null : getIdentifierMap().get(Citations.ISBN);
    }

    /**
     * Sets the International Standard Book Number.
     * In this SIS implementation, invoking this method is equivalent to:
     *
     * {@preformat java
     *   getIdentifierMap().putSpecialized(Citations.ISBN, newValue);
     * }
     *
     * @param newValue The new ISBN, or {@code null} if none.
     *
     * @see #setIdentifiers(Collection)
     * @see Citations#ISBN
     */
    public void setISBN(final String newValue) {
        checkWritePermission();
        if (newValue != null || !isNullOrEmpty(identifiers)) {
            getIdentifierMap().putSpecialized(Citations.ISBN, newValue);
        }
    }

    /**
     * Returns the International Standard Serial Number.
     * In this SIS implementation, invoking this method is equivalent to:
     *
     * {@preformat java
     *   return getIdentifierMap().getSpecialized(Citations.ISSN);
     * }
     *
     * @return The ISSN, or {@code null} if none.
     *
     * @see #getIdentifiers()
     * @see Citations#ISSN
     */
    @Override
    @XmlElement(name = "ISSN")
    public String getISSN() {
        return isNullOrEmpty(identifiers) ? null : getIdentifierMap().get(Citations.ISSN);
    }

    /**
     * Sets the International Standard Serial Number.
     * In this SIS implementation, invoking this method is equivalent to:
     *
     * {@preformat java
     *   getIdentifierMap().putSpecialized(Citations.ISSN, newValue);
     * }
     *
     * @param newValue The new ISSN.
     *
     * @see #setIdentifiers(Collection)
     * @see Citations#ISSN
     */
    public void setISSN(final String newValue) {
        checkWritePermission();
        if (newValue != null || !isNullOrEmpty(identifiers)) {
            getIdentifierMap().putSpecialized(Citations.ISSN, newValue);
        }
    }

    /**
     * Returns online references to the cited resource.
     *
     * @return Online references to the cited resource, or an empty collection if there is none.
     *
     * @since 0.5
     */
/// @XmlElement(name = "onlineResource")
    @UML(identifier="onlineResource", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<OnlineResource> getOnlineResources() {
        return onlineResources = nonNullCollection(onlineResources, OnlineResource.class);
    }

    /**
     * Sets online references to the cited resource.
     *
     * @param newValues The new online references to the cited resource.
     *
     * @since 0.5
     */
    public void setOnlineResources(final Collection<? extends OnlineResource> newValues) {
        onlineResources = writeCollection(newValues, onlineResources, OnlineResource.class);
    }

    /**
     * Returns citation graphics or logo for cited party.
     *
     * @return Graphics or logo for cited party, or an empty collection if there is none.
     *
     * @since 0.5
     */
/// @XmlElement(name = "graphic")
    @UML(identifier="graphic", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<BrowseGraphic> getGraphics() {
        return graphics = nonNullCollection(graphics, BrowseGraphic.class);
    }

    /**
     * Sets citation graphics or logo for cited party.
     *
     * @param newValues The new citation graphics or logo for cited party.
     *
     * @since 0.5
     */
    public void setGraphics(final Collection<? extends BrowseGraphic> newValues) {
        graphics = writeCollection(newValues, graphics, BrowseGraphic.class);
    }
}
