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
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.NonMarshalledAuthority;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.ISOMetadata;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;

// Specific to the geoapi-4.0 branch:
import org.opengis.metadata.citation.Responsibility;


/**
 * Standardized resource reference.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code CI_Citation}
 * {@code   ├─title………………………} Name by which the cited resource is known.
 * {@code   └─date…………………………} Reference date for the cited resource.</div>
 *
 * <h2>Unified identifiers view</h2>
 * The ISO 19115 model provides specific attributes for the {@linkplain #getISBN() ISBN} and
 * {@linkplain #getISSN() ISSN} codes. However, the SIS library handles those codes like any
 * other identifiers. Consequently, the ISBN and ISSN codes are included in the collection
 * returned by {@link #getIdentifiers()}, except at XML marshalling time (for ISO 19115-3 compliance).
 *
 * <h2>Limitations</h2>
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
 * @author  Cullen Rombach (Image Matters)
 * @version 2.0
 * @since   0.3
 */
@TitleProperty(name = "title")
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
    "ISSN",
    "onlineResource",           // New in ISO 19115:2014
    "graphic"                   // Ibid.
})
@XmlRootElement(name = "CI_Citation")
public class DefaultCitation extends ISOMetadata implements Citation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 996219577522074939L;

    /**
     * Name by which the cited resource is known.
     */
    @SuppressWarnings("serial")
    private InternationalString title;

    /**
     * Short name or other language name by which the cited information is known.
     * Example: "DCW" as an alternative title for "Digital Chart of the World.
     */
    @SuppressWarnings("serial")
    private Collection<InternationalString> alternateTitles;

    /**
     * Reference date for the cited resource.
     */
    @SuppressWarnings("serial")
    private Collection<CitationDate> dates;

    /**
     * Version of the cited resource.
     */
    @SuppressWarnings("serial")
    private InternationalString edition;

    /**
     * Date of the edition.
     */
    @SuppressWarnings("serial")     // Standard Java implementations are serializable.
    private Temporal editionDate;

    /**
     * Roles, Name, contact, and position information for an individual or organization that is responsible
     * for the resource.
     */
    @SuppressWarnings("serial")
    private Collection<Responsibility> citedResponsibleParties;

    /**
     * Mode in which the resource is represented, or an empty collection if none.
     */
    @SuppressWarnings("serial")
    private Collection<PresentationForm> presentationForms;

    /**
     * Information about the series, or aggregate dataset, of which the dataset is a part.
     * May be {@code null} if none.
     */
    @SuppressWarnings("serial")
    private Series series;

    /**
     * Other information required to complete the citation that is not recorded elsewhere.
     * May be {@code null} if none.
     */
    @SuppressWarnings("serial")
    private Collection<InternationalString> otherCitationDetails;

    /**
     * Common title with holdings note. Note: title identifies elements of a series
     * collectively, combined with information about what volumes are available at the
     * source cited. May be {@code null} if there is no title.
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Deprecated(since="1.0")
    @SuppressWarnings("serial")
    private InternationalString collectiveTitle;

    /**
     * Online references to the cited resource.
     */
    @SuppressWarnings("serial")
    private Collection<OnlineResource> onlineResources;

    /**
     * Citation graphic or logo for cited party.
     */
    @SuppressWarnings("serial")
    private Collection<BrowseGraphic> graphics;

    /**
     * Constructs an initially empty citation.
     */
    public DefaultCitation() {
    }

    /**
     * Constructs a citation with the specified title.
     *
     * @param title  the title as a {@link String} or an {@link InternationalString} object,
     *               or {@code null} if none.
     */
    public DefaultCitation(final CharSequence title) {
        this.title = Types.toInternationalString(title);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Citation)
     */
    @SuppressWarnings({"this-escape", "deprecation"})
    public DefaultCitation(final Citation object) {
        super(object);
        if (object != null) {
            title                   = object.getTitle();
            alternateTitles         = copyCollection(object.getAlternateTitles(), InternationalString.class);
            dates                   = copyCollection(object.getDates(), CitationDate.class);
            edition                 = object.getEdition();
            editionDate             = object.getEditionDate();
            identifiers             = copyCollection(object.getIdentifiers(), Identifier.class);
            citedResponsibleParties = copyCollection(object.getCitedResponsibleParties(), Responsibility.class);
            presentationForms       = copyCollection(object.getPresentationForms(), PresentationForm.class);
            series                  = object.getSeries();
            otherCitationDetails    = copyCollection(object.getOtherCitationDetails(), InternationalString.class);
            collectiveTitle         = object.getCollectiveTitle();
            onlineResources         = copyCollection(object.getOnlineResources(), OnlineResource.class);
            graphics                = copyCollection(object.getGraphics(), BrowseGraphic.class);
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
     *       {@linkplain #DefaultCitation(Citation) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     * @return the cited resource name, or {@code null}.
     */
    @Override
    @XmlElement(name = "title", required = true)
    public InternationalString getTitle() {
        return title;
    }

    /**
     * Sets the name by which the cited resource is known.
     *
     * @param  newValue  the new title, or {@code null} if none.
     */
    public void setTitle(final InternationalString newValue) {
        checkWritePermission(title);
        title = newValue;
    }

    /**
     * Returns short name or other language name by which the cited information is known.
     *
     * <h4>Example</h4>
     * "DCW" as an alternative title for "Digital Chart of the World".
     *
     * @return other names for the resource, or an empty collection if none.
     */
    @Override
    @XmlElement(name = "alternateTitle")
    public Collection<InternationalString> getAlternateTitles() {
        return alternateTitles = nonNullCollection(alternateTitles, InternationalString.class);
    }

    /**
     * Sets the short name or other language name by which the cited information is known.
     *
     * @param  newValues  the new alternate titles, or {@code null} if none.
     */
    public void setAlternateTitles(final Collection<? extends InternationalString> newValues) {
        alternateTitles = writeCollection(newValues, alternateTitles, InternationalString.class);
    }

    /**
     * Returns the reference date for the cited resource.
     *
     * @return the reference date.
     */
    @Override
    @XmlElement(name = "date")
    public Collection<CitationDate> getDates() {
        return dates = nonNullCollection(dates, CitationDate.class);
    }

    /**
     * Sets the reference date for the cited resource.
     *
     * @param  newValues  the new dates, or {@code null} if none.
     */
    public void setDates(final Collection<? extends CitationDate> newValues) {
        dates = writeCollection(newValues, dates, CitationDate.class);
    }

    /**
     * Returns the version of the cited resource.
     *
     * @return the version, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "edition")
    public InternationalString getEdition() {
        return edition;
    }

    /**
     * Sets the version of the cited resource.
     *
     * @param  newValue  the new edition, or {@code null} if none.
     */
    public void setEdition(final InternationalString newValue) {
        checkWritePermission(edition);
        edition = newValue;
    }

    /**
     * Returns the date of the edition.
     *
     * @return the edition date, or {@code null} if none.
     * @version 2.0
     */
    @Override
    @XmlElement(name = "editionDate")
    public Temporal getEditionDate() {
        return editionDate;
    }

    /**
     * Sets the date of the edition.
     * The specified value should be an instance of {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
     * {@link java.time.OffsetDateTime} or {@link java.time.ZonedDateTime}, depending whether hours are defined
     * and how the timezone (if any) is defined. But other types are also allowed.
     * For example, a citation date may be merely a {@link java.time.Year}.
     *
     * @param  newValue  the new edition date, or {@code null} if none.
     *
     * @since 1.5
     */
    public void setEditionDate(final Temporal newValue) {
        checkWritePermission(editionDate);
        editionDate = newValue;
    }

    /**
     * Sets the date of the edition.
     *
     * @param  newValue  the new edition date, or {@code null} if none.
     *
     * @deprecated Replaced by {@link #setEditionDate(Temporal)}.
     */
    @Deprecated(since="1.5")
    public void setEditionDate(final Date newValue) {
        setEditionDate(TemporalDate.toTemporal(newValue));
    }

    /**
     * Returns the unique identifier for the resource.
     *
     * <h4>Examples</h4>
     * <ul>
     *   <li>Universal Product Code (UPC)</li>
     *   <li>National Stock Number (NSN)</li>
     * </ul>
     *
     * <h4>Unified identifiers view</h4>
     * In this SIS implementation, the collection returned by this method includes the XML identifiers
     * ({@linkplain IdentifierSpace#ID ID}, {@linkplain IdentifierSpace#UUID UUID}, <i>etc.</i>),
     * as well as the {@linkplain #getISBN() ISBN} and {@linkplain #getISSN() ISSN} codes, thus
     * providing a unified view of every kind of identifiers associated to this citation.
     *
     * <h4>XML marshalling note</h4>
     * The {@code <cit:identifier>} element marshalled to XML will exclude all the above cited identifiers,
     * for ISO 19115-3 compliance. Those identifiers will appear in other XML elements or attributes.
     *
     * @return the identifiers, or an empty collection if none.
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
     * they are explicitly provided in the given collection.</p>
     *
     * @param  newValues  the new identifiers, or {@code null} if none.
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
     * @return the individual or organization that is responsible, or an empty collection if none.
     */
    @Override
    @XmlElement(name = "citedResponsibleParty")
    public Collection<Responsibility> getCitedResponsibleParties() {
        return citedResponsibleParties = nonNullCollection(citedResponsibleParties, Responsibility.class);
    }

    /**
     * Sets the role, name, contact and position information for an individual or organization
     * that is responsible for the resource.
     *
     * @param  newValues  the new cited responsible parties, or {@code null} if none.
     */
    public void setCitedResponsibleParties(final Collection<? extends Responsibility> newValues) {
        citedResponsibleParties = writeCollection(newValues, citedResponsibleParties, Responsibility.class);
    }

    /**
     * Returns the mode in which the resource is represented.
     *
     * @return the presentation modes, or an empty collection if none.
     */
    @Override
    @XmlElement(name = "presentationForm")
    public Collection<PresentationForm> getPresentationForms() {
        return presentationForms = nonNullCollection(presentationForms, PresentationForm.class);
    }

    /**
     * Sets the mode in which the resource is represented.
     *
     * @param  newValues  the new presentation form, or {@code null} if none.
     */
    public void setPresentationForms(final Collection<? extends PresentationForm> newValues) {
        presentationForms = writeCollection(newValues, presentationForms, PresentationForm.class);
    }

    /**
     * Returns the information about the series, or aggregate dataset, of which the dataset is a part.
     *
     * @return the series of which the dataset is a part, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "series")
    public Series getSeries() {
        return series;
    }

    /**
     * Sets the information about the series, or aggregate dataset, of which the dataset is a part.
     *
     * @param  newValue  the new series.
     */
    public void setSeries(final Series newValue) {
        checkWritePermission(series);
        series = newValue;
    }

    /**
     * Returns other information required to complete the citation that is not recorded elsewhere.
     *
     * @return other details, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "otherCitationDetails")
    public Collection<InternationalString> getOtherCitationDetails() {
        return otherCitationDetails = nonNullCollection(otherCitationDetails, InternationalString.class);
    }

    /**
     * Sets other information required to complete the citation that is not recorded elsewhere.
     *
     * @param newValues Other citations details.
     */
    public void setOtherCitationDetails(final Collection<? extends InternationalString> newValues) {
        otherCitationDetails = writeCollection(newValues, otherCitationDetails, InternationalString.class);
    }

    /**
     * Returns the common title with holdings note.
     *
     * @return the common title, or {@code null} if none.
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Override
    @Deprecated(since="1.0")
    @XmlElement(name = "collectiveTitle", namespace = LegacyNamespaces.GMD)
    public InternationalString getCollectiveTitle() {
        return FilterByVersion.LEGACY_METADATA.accept() ? collectiveTitle : null;
    }

    /**
     * Sets the common title with holdings note. This title identifies elements of a series collectively,
     * combined with information about what volumes are available at the source cited.
     *
     * @param  newValue  the new collective title, or {@code null} if none.
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Deprecated(since="1.0")
    public void setCollectiveTitle(final InternationalString newValue) {
        checkWritePermission(collectiveTitle);
        collectiveTitle = newValue;
    }

    /**
     * Returns the ISBN or ISSN identifier for the given authority, or {@code null} if none.
     */
    private String getIdentifier(final Citation authority) {
        return isNullOrEmpty(identifiers) ? null : getIdentifierMap().get(authority);
    }

    /**
     * Returns the International Standard Book Number.
     * In this SIS implementation, invoking this method is equivalent to:
     *
     * {@snippet lang="java" :
     *     return getIdentifierMap().getSpecialized(Citations.ISBN);
     *     }
     *
     * @return the ISBN, or {@code null} if none.
     *
     * @see #getIdentifiers()
     * @see Citations#ISBN
     */
    @Override
    @XmlElement(name = "ISBN")
    public String getISBN() {
        return getIdentifier(Citations.ISBN);
    }

    /**
     * Sets the International Standard Book Number.
     * In this SIS implementation, invoking this method is equivalent to:
     *
     * {@snippet lang="java" :
     *     getIdentifierMap().putSpecialized(Citations.ISBN, newValue);
     *     }
     *
     * @param  newValue  the new ISBN, or {@code null} if none.
     *
     * @see #setIdentifiers(Collection)
     * @see Citations#ISBN
     */
    public void setISBN(final String newValue) {
        checkWritePermission(getIdentifier(Citations.ISBN));
        if (newValue != null || !isNullOrEmpty(identifiers)) {
            getIdentifierMap().putSpecialized(Citations.ISBN, newValue);
        }
    }

    /**
     * Returns the International Standard Serial Number.
     * In this SIS implementation, invoking this method is equivalent to:
     *
     * {@snippet lang="java" :
     *     return getIdentifierMap().getSpecialized(Citations.ISSN);
     *     }
     *
     * @return the ISSN, or {@code null} if none.
     *
     * @see #getIdentifiers()
     * @see Citations#ISSN
     */
    @Override
    @XmlElement(name = "ISSN")
    public String getISSN() {
        return getIdentifier(Citations.ISSN);
    }

    /**
     * Sets the International Standard Serial Number.
     * In this SIS implementation, invoking this method is equivalent to:
     *
     * {@snippet lang="java" :
     *     getIdentifierMap().putSpecialized(Citations.ISSN, newValue);
     *     }
     *
     * @param  newValue  the new ISSN.
     *
     * @see #setIdentifiers(Collection)
     * @see Citations#ISSN
     */
    public void setISSN(final String newValue) {
        checkWritePermission(getIdentifier(Citations.ISSN));
        if (newValue != null || !isNullOrEmpty(identifiers)) {
            getIdentifierMap().putSpecialized(Citations.ISSN, newValue);
        }
    }

    /**
     * Returns online references to the cited resource.
     *
     * @return online references to the cited resource, or an empty collection if there is none.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<OnlineResource> getOnlineResources() {
        return onlineResources = nonNullCollection(onlineResources, OnlineResource.class);
    }

    /**
     * Sets online references to the cited resource.
     *
     * @param  newValues  the new online references to the cited resource.
     *
     * @since 0.5
     */
    public void setOnlineResources(final Collection<? extends OnlineResource> newValues) {
        onlineResources = writeCollection(newValues, onlineResources, OnlineResource.class);
    }

    /**
     * Returns citation graphics or logo for cited party.
     *
     * @return graphics or logo for cited party, or an empty collection if there is none.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<BrowseGraphic> getGraphics() {
        return graphics = nonNullCollection(graphics, BrowseGraphic.class);
    }

    /**
     * Sets citation graphics or logo for cited party.
     *
     * @param  newValues  the new citation graphics or logo for cited party.
     *
     * @since 0.5
     */
    public void setGraphics(final Collection<? extends BrowseGraphic> newValues) {
        graphics = writeCollection(newValues, graphics, BrowseGraphic.class);
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "onlineResource")
    private Collection<OnlineResource> getOnlineResource() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getOnlineResources() : null;
    }

    @XmlElement(name = "graphic")
    private Collection<BrowseGraphic> getGraphic() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getGraphics() : null;
    }
}
