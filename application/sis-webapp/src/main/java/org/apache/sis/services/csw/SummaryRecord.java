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
package org.apache.sis.services.csw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import static org.apache.sis.internal.util.CollectionsExt.first;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.MetadataScope;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.iso.Types;
import org.opengis.metadata.citation.Responsibility;


/**
 * Summary of an ISO 19115 metadata record. The summary is composed in part from Dublin Core elements.
 * The mapping from ISO 19115 metadata to {@code SummaryRecord} is defined in:
 *
 * <blockquote>
 * OpenGIS Catalog Service Specification — ISO metadata application profile (OGC 07-045),
 * table 6 at page 41.
 * </blockquote>
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @since   0.8
 * @version 0.8
 * @module
 */
@XmlRootElement(name = "Record")
@XmlType(name = "RecordType", propOrder = {
    "creator",
    "contributor",
    "publisher",
    "subject",
    "identifier",
    "relation",
    "type",
    "title",
    "modified",
    "language",
    "format",
    "BoundingBox"
})
public class SummaryRecord extends Element {
    /**
     * An entity primarily responsible for making the content of the resource .
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String creator;

    /**
     * An entity responsible contributions to the content of the resource.
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String contributor;

    /**
     * An entity responsible for making the resource avaible.
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String publisher;

    /**
     * A topic of the content of the resource.
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String subject;

    /**
     * An unique reference to the record within the catalogue.
     * This is mapped to the {@code metadata/metadataIdentifier} property of ISO 19115.
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String identifier;

    /**
     * A reference to a related resource.
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String relation;

    /**
     * The nature or genre of the content of the resource.
     * Type can include general categories, genres or aggregation levels of content.
     * This is mapped to the {@code metadata/metadataScope/resourceScope} property of ISO 19115.
     * If more than one scope is provided, only the first one is retained.
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String type;

    /**
     * A name given to the resource.
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String title;

    /**
     * Date on which the record was created or updated within the catalogue.
     * This is a DCMI metadata term {@code <http://dublincore.org/documents/dcmi-terms/>}.
     * mapped to the {@code metadata/dateInfo} property of ISO 19115, taking in account
     * only dates associated to {@link DateType#CREATION} or {@link DateType#LAST_UPDATE}.
     * If more than one date exist for those types, then only the latest date is retained.
     */
    @XmlElement(namespace = DUBLIN_TERMS)
    private Date modified;

    /**
     * A language of the intellectual content of the catalog record.
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String language;

    /**
     * The physical or digital manifestation of the resource.
     * Typically, Format may include the media-type or dimensions of the resource.
     * Format may be used to determine the software, hardware or other equipment
     * needed to display or operate the resource.
     *
     * <p>This is mapped to
     * {@code metadata/distributionInfo/distributionFormat/formatSpecificationCitation/alternateTitles}.
     * If the metadata cites more than one format, then the titles of all formats (omitting duplicated values)
     * are included in this field, separated by new-line characters.</p>
     */
    @XmlElement(namespace = DUBLIN_CORE)
    private String format;

    /**
     * A bounding box for identifying a geographic area of interest.
     */
    @XmlElement(namespace = OWS)
    private BoundingBox BoundingBox;

    /**
     * Creates an initially empty summary record.
     * This constructor is invoked by JAXB at unmarshalling time.
     */
    SummaryRecord(final Metadata object) {
        List<Responsibility> responsibility = new ArrayList<>(first(object.getIdentificationInfo()).getPointOfContacts());
        this.creator = first(responsibility.get(0).getParties()).getName().toString();
        this.publisher = first(responsibility.get(1).getParties()).getName().toString();
        this.contributor = first(responsibility.get(2).getParties()).getName().toString();
        this.subject=first(first(first(object.getIdentificationInfo()).getDescriptiveKeywords()).getKeywords()).toString();
        this.identifier=object.getFileIdentifier();
        this.relation=first(first(object.getIdentificationInfo()).getAggregationInfo()).getAggregateDataSetName().getTitle().toString();
        this.title=first(object.getIdentificationInfo()).getCitation().getTitle().toString();
        this.type=first(object.getHierarchyLevels()).name();
        this.format=first(first(object.getDistributionInfo()).getDistributionFormats()).getName().toString();
        this.language=object.getLanguage().toString();
        this.modified = first(object.getDateInfo()).getDate();
        Extent et = first(first(object.getIdentificationInfo()).getExtents());
        GeographicBoundingBox gbd = (GeographicBoundingBox) first(et.getGeographicElements());
        this.BoundingBox = new BoundingBox(gbd);
                
    }

    /**
     * Creates a summary record initialized to the values extracted from the given ISO 19115 metadata.
     * This constructor is invoked before marshalling with JAXB.
     *
     * @param metadata  the root ISO 19115 metadata instance where to get information.
     * @param locale    the locale to use for converting {@link InternationalString} to {@link String},
     *                  or {@code null} for the system default.
     */
    public SummaryRecord(final Metadata metadata, final Locale locale) {
        /*
         * Get identifier and date information from the root metadata object. Note that:
         *
         *   - Identifier is optional in ISO 19115 but mandatory in <csw:Record>.
         *   - Date information is mandatory in ISO 19115.
         *
         * First, try to get those information from the paths specified by OGC 07-045.
         * They should be there, but if for some reason those information are missing,
         * then the loop below will search for fallbacks in resource citations.
         */
        identifier = IdentifiedObjects.toString(metadata.getMetadataIdentifier());              // May be null.
        setModified(metadata.getDateInfo());
        /*
         * Collect all titles, ignoring duplicated values. Opportunistically search for
         * dates and identifier to use as fallbacks if the above code did not found them.
         * Those fallbacks are specific to Apache SIS (not part of OGC 07-045).
         */
        GeographicBoundingBox bbox = null;
        DefaultGeographicBoundingBox union = null;
        final Set<String> titles = new HashSet<>();
        for (final Identification identification : metadata.getIdentificationInfo()) {
            if (identification != null) {
                final Citation citation = identification.getCitation();
                if (citation != null) {
                    final InternationalString i18n = citation.getTitle();
                    if (i18n != null) {
                        titles.add(i18n.toString(locale));
                    }
                    if (identifier == null) {
                        for (final Identifier id : citation.getIdentifiers()) {
                            identifier = IdentifiedObjects.toString(id);
                            if (identifier != null) break;                      // Stop at the first identifier.
                        }
                    }
                    if (modified == null) {
                        setModified(citation.getDates());
                    }
                }
                for (final Extent extent : identification.getExtents()) {
                    for (final GeographicExtent geo : extent.getGeographicElements()) {
                        if (geo instanceof GeographicBoundingBox) {
                            if (bbox == null) {
                                bbox = (GeographicBoundingBox) geo;
                            } else {
                                if (union == null) {
                                    bbox = union = new DefaultGeographicBoundingBox(bbox);
                                }
                                union.add((GeographicBoundingBox) geo);
                            }
                        }
                    }
                }
            }
        }
        if (bbox != null) {
            BoundingBox = new BoundingBox(bbox);
        }
        title = toString(titles, System.lineSeparator());
        titles.clear();

        // Collect all formats, ignoring duplicated values.
        for (final Distribution distribution : metadata.getDistributionInfo()) {
            for (final Format df : distribution.getDistributionFormats()) {
                final Citation citation = df.getFormatSpecificationCitation();
                if (citation != null) {
                    for (final InternationalString i18n : citation.getAlternateTitles()) {
                        if (i18n != null) {
                            titles.add(i18n.toString(locale));
                        }
                    }
                }
            }
        }
        format = toString(titles, ", ");
        titles.clear();

        // Retain only the first type, if any.
        ScopeCode code = null;
        for (final MetadataScope scope : metadata.getMetadataScopes()) {
            code = scope.getResourceScope();
            if (code != null) break;
        }
        if (code == null) {
            code = ScopeCode.DATASET;       // Default value specified by OGC 07-045.
        }
        type = Types.getCodeName(code);
    }

    /**
     * Sets the {@link #modified} field to the latest {@code CREATION} or {@code LAST_UPDATE} date
     * found in the given collection. If the given is null or empty, then this method does nothing.
     */
    private void setModified(final Collection<? extends CitationDate> dates) {
        if (dates != null) {                            // Paranoiac check.
            for (final CitationDate date : dates) {
                final DateType dt = date.getDateType();
                if (DateType.CREATION.equals(dt) || DateType.LAST_UPDATE.equals(dt)) {
                    final Date t = date.getDate();
                    if (t != null) {
                        if (modified == null || t.after(modified)) {
                            modified = t;
                        }
                    }
                }
            }
        }
    }

    /**
     * Formats the elements in the given set as a list separated by the given string.
     * This method avoid to create a temporary buffer when the set contains 0 or 1 elements.
     *
     * @param  items      the items to format as a list.
     * @param  separator  the separator to put between items (comma or new line).
     * @return the given set formatted as a list.
     */
    private static String toString(final Set<String> items, final String separator) {
        items.remove(null);         // Safety in case the user metadata contains null elements.
        final Iterator<String> it = items.iterator();
        if (!it.hasNext()) {
            return null;
        }
        final String first = it.next();
        if (!it.hasNext()) {
            return first;            // Most common case: only one item in the set.
        }
        final StringBuilder buffer = new StringBuilder(first);
        while (it.hasNext()) {
            buffer.append(separator).append(it.next());
        }
        return buffer.toString();
    }

    /**
     * Returns a entity primarily responsible for making the content of the
     * resource .
     */
    public String getCreator() {
        return creator;
    }

    /**
     * Set a entity primarily responsible for making the content of the
     * resource.
     *
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * Return an entity responsible contributions to the content of the
     * resource.
     */
    public String getContributor() {
        return contributor;
    }

    /**
     * Set an entity responsible contributions to the content of the resource.
     */
    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    /**
     * Return an entity responsible for making the resource avaible.
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * Set an entity responsible for making the resource avaible.
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     * Return a topic of the content of the resource.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Set a topic of the content of the resource.
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Return a unique reference to the record within the catalogue.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set a unique reference to the record within the catalogue.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Return a reference to a related resource.
     */
    public String getRelation() {
        return relation;
    }

    /**
     * Set a reference to a related resource.
     */
    public void setRelation(String relation) {
        this.relation = relation;
    }

    /**
     * Return the nature or genre of the content of the resource. Type can
     * include general categories, genres or aggregation levels of content.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the nature or genre of the content of the resource. Type can include
     * general categories, genres or aggregation levels of content.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Return a name given to the resource.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set a name given to the resource.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Return date on which the record was created or updated within the
     * catalogue.
     */
    public Date getModified() {
        return modified;
    }

    /**
     * Set date on which the record was created or updated within the catalogue.
     */
    public void setModified(Date modified) {
        this.modified = modified;
    }

    /**
     * Return a language of the intellectual content of the catalog record.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Set a language of the intellectual content of the catalog record.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Return the physical or digital manifestation of the resource.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Set the physical or digital manifestation of the resource.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Return a bouding box for identifying a geographic area of interest.
     */
    public BoundingBox getBoundingBox() {
        return BoundingBox;
    }

    /**
     * Set a bouding box for identifying a geographic area of interest.
     */
    public void setBoundingBox(BoundingBox BoundingBox) {
        this.BoundingBox = BoundingBox;
    }
}
