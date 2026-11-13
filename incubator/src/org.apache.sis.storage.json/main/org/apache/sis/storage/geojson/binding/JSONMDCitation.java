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
package org.apache.sis.storage.geojson.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.storage.json.DataTransferObject;
import org.apache.sis.xml.IdentifierSpace;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Responsibility;

/**
 * Binding of the {@code CI_Citation} JSON type.
 *
 * <p>Identifiers which SIS derives from other properties (ISBN, ISSN) or from XML attributes
 * ({@code gml:id}, {@code gco:uuid}, {@code xlink:href}) are not written in the {@code identifier} property.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_Citation</a>
 */
@JsonPropertyOrder({
    JSONMDCitation.PROPERTY_TITLE,
    JSONMDCitation.PROPERTY_ALTERNATE_TITLE,
    JSONMDCitation.PROPERTY_DATE,
    JSONMDCitation.PROPERTY_EDITION,
    JSONMDCitation.PROPERTY_EDITION_DATE,
    JSONMDCitation.PROPERTY_IDENTIFIER,
    JSONMDCitation.PROPERTY_CITED_RESPONSIBLE_PARTY,
    JSONMDCitation.PROPERTY_PRESENTATION_FORM,
    JSONMDCitation.PROPERTY_SERIES,
    JSONMDCitation.PROPERTY_OTHER_CITATION_DETAILS,
    JSONMDCitation.PROPERTY_ISBN,
    JSONMDCitation.PROPERTY_ISSN,
    JSONMDCitation.PROPERTY_ONLINE_RESOURCE
})
public final class JSONMDCitation extends DataTransferObject {

    public static final String PROPERTY_TITLE = "title";
    public static final String PROPERTY_ALTERNATE_TITLE = "alternateTitle";
    public static final String PROPERTY_DATE = "date";
    public static final String PROPERTY_EDITION = "edition";
    public static final String PROPERTY_EDITION_DATE = "editionDate";
    public static final String PROPERTY_IDENTIFIER = "identifier";
    public static final String PROPERTY_CITED_RESPONSIBLE_PARTY = "citedResponsibleParty";
    public static final String PROPERTY_PRESENTATION_FORM = "presentationForm";
    public static final String PROPERTY_SERIES = "series";
    public static final String PROPERTY_OTHER_CITATION_DETAILS = "otherCitationDetails";
    public static final String PROPERTY_ISBN = "ISBN";
    public static final String PROPERTY_ISSN = "ISSN";
    public static final String PROPERTY_ONLINE_RESOURCE = "onlineResource";

    /**
     * Authorities of the identifiers which SIS derives from other properties or XML attributes.
     */
    private static final List<Citation> DERIVED_AUTHORITIES = List.of(
            Citations.ISBN, Citations.ISSN,
            IdentifierSpace.ID, IdentifierSpace.UUID, IdentifierSpace.HREF, IdentifierSpace.XLINK);

    @JsonProperty(PROPERTY_TITLE)
    private String title;
    @JsonProperty(PROPERTY_ALTERNATE_TITLE)
    private List<String> alternateTitle;
    @JsonProperty(PROPERTY_DATE)
    private JSONMDDates date;
    @JsonProperty(PROPERTY_EDITION)
    private String edition;
    @JsonProperty(PROPERTY_EDITION_DATE)
    private JSONMDDateOrDateTime editionDate;
    @JsonProperty(PROPERTY_IDENTIFIER)
    private List<JSONMDIdentifier> identifier;
    @JsonProperty(PROPERTY_CITED_RESPONSIBLE_PARTY)
    private List<JSONMDResponsibility> citedResponsibleParty;
    @JsonProperty(PROPERTY_PRESENTATION_FORM)
    private List<String> presentationForm;
    @JsonProperty(PROPERTY_SERIES)
    private JSONMDSeries series;
    @JsonProperty(PROPERTY_OTHER_CITATION_DETAILS)
    private List<String> otherCitationDetails;
    @JsonProperty(PROPERTY_ISBN)
    private String isbn;
    @JsonProperty(PROPERTY_ISSN)
    private String issn;
    @JsonProperty(PROPERTY_ONLINE_RESOURCE)
    private List<JSONMDOnlineResource> onlineResource;

    public JSONMDCitation() {
    }

    public JSONMDCitation(final Citation source, final JSONMDEncoder encoder) {
        title                 = encoder.text(source.getTitle());
        alternateTitle        = encoder.texts(source.getAlternateTitles());
        date                  = encoder.dates(source.getDates());
        edition               = encoder.text(source.getEdition());
        editionDate           = encoder.date(source.getEditionDate());
        identifier            = encoder.list(source.getIdentifiers(),
                                    (Identifier e) -> isDerived(e) ? null : new JSONMDIdentifier(e, encoder));
        citedResponsibleParty = encoder.list(source.getCitedResponsibleParties(),
                                    (Responsibility e) -> new JSONMDResponsibility(e, encoder));
        presentationForm      = encoder.codes(source.getPresentationForms());
        series                = (source.getSeries() == null) ? null : new JSONMDSeries(source.getSeries(), encoder);
        otherCitationDetails  = encoder.texts(source.getOtherCitationDetails());
        isbn                  = source.getISBN();
        issn                  = source.getISSN();
        onlineResource        = encoder.list(source.getOnlineResources(),
                                    (OnlineResource e) -> new JSONMDOnlineResource(e, encoder));
    }

    private static boolean isDerived(final Identifier id) {
        final Citation authority = id.getAuthority();
        for (final Citation derived : DERIVED_AUTHORITIES) {
            if (authority == derived) {
                return true;
            }
        }
        return false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public List<String> getAlternateTitle() {
        return alternateTitle;
    }

    public void setAlternateTitle(final List<String> alternateTitle) {
        this.alternateTitle = alternateTitle;
    }

    public JSONMDDates getDate() {
        return date;
    }

    public void setDate(final JSONMDDates date) {
        this.date = date;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(final String edition) {
        this.edition = edition;
    }

    public JSONMDDateOrDateTime getEditionDate() {
        return editionDate;
    }

    public void setEditionDate(final JSONMDDateOrDateTime editionDate) {
        this.editionDate = editionDate;
    }

    public List<JSONMDIdentifier> getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final List<JSONMDIdentifier> identifier) {
        this.identifier = identifier;
    }

    public List<JSONMDResponsibility> getCitedResponsibleParty() {
        return citedResponsibleParty;
    }

    public void setCitedResponsibleParty(final List<JSONMDResponsibility> citedResponsibleParty) {
        this.citedResponsibleParty = citedResponsibleParty;
    }

    public List<String> getPresentationForm() {
        return presentationForm;
    }

    public void setPresentationForm(final List<String> presentationForm) {
        this.presentationForm = presentationForm;
    }

    public JSONMDSeries getSeries() {
        return series;
    }

    public void setSeries(final JSONMDSeries series) {
        this.series = series;
    }

    public List<String> getOtherCitationDetails() {
        return otherCitationDetails;
    }

    public void setOtherCitationDetails(final List<String> otherCitationDetails) {
        this.otherCitationDetails = otherCitationDetails;
    }

    public String getISBN() {
        return isbn;
    }

    public void setISBN(final String isbn) {
        this.isbn = isbn;
    }

    public String getISSN() {
        return issn;
    }

    public void setISSN(final String issn) {
        this.issn = issn;
    }

    public List<JSONMDOnlineResource> getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(final List<JSONMDOnlineResource> onlineResource) {
        this.onlineResource = onlineResource;
    }

    public DefaultCitation toMetadata(final JSONMDDecoder decoder) {
        final DefaultCitation target = new DefaultCitation();
        target.setTitle(decoder.text(title));
        target.setAlternateTitles(decoder.texts(alternateTitle));
        if (date != null) {
            target.setDates(date.toMetadata(decoder));
        }
        target.setEdition(decoder.text(edition));
        if (editionDate != null) {
            target.setEditionDate(editionDate.getValue());
        }
        target.setIdentifiers(decoder.list(identifier, (JSONMDIdentifier e) -> e.toMetadata(decoder)));
        target.setCitedResponsibleParties(decoder.list(citedResponsibleParty, (JSONMDResponsibility e) -> e.toMetadata(decoder)));
        target.setPresentationForms(decoder.codes(PresentationForm.class, presentationForm, PresentationForm::valueOf));
        if (series != null) {
            target.setSeries(series.toMetadata(decoder));
        }
        target.setOtherCitationDetails(decoder.texts(otherCitationDetails));
        target.setISBN(isbn);
        target.setISSN(issn);
        target.setOnlineResources(decoder.list(onlineResource, (JSONMDOnlineResource e) -> e.toMetadata(decoder)));
        return target;
    }
}
