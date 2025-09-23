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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.io.IOException;
import java.io.UncheckedIOException;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlElement;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.distribution.Format;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;
import org.apache.sis.io.TableAppender;
import org.apache.sis.metadata.simple.SimpleMetadata;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.iso.Types;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.metadata.citation.ResponsibleParty;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.Responsibility;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * Information about the GPX file, author, and copyright restrictions.
 * This is the root of the {@code <metadata>} element in a GPX file.
 * At most one such element may appear in the document.
 * The XML content is like below:
 *
 * {@snippet lang="xml" :
 *   <metadata>
 *     <name> String </name>
 *     <desc> String </desc>
 *     <author> Person </author>
 *     <copyright> Copyright </copyright>
 *     <link href="URI"/>
 *     <time> Temporal </time>
 *     <keywords> String[] </keywords>
 *     <bounds> Envelope </bounds>
 *     <extensions> any object known to JAXB </extensions>
 *   </metadata>
 *   }
 *
 * Those properties can be read or modified directly. All methods defined in this class are bridges to
 * the ISO 19115 metadata model and can be ignored if the user only wants to manipulate the GPX model.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Metadata extends SimpleMetadata {
    /**
     * The data store that created this metadata, or {@code null} if none. This information is used for fetching
     * information that are constants for all GPX files, for example the feature types and the format description.
     *
     * <p>This field needs to be set after construction. It cannot be set at construction time because JAXB needs
     * to invoke a no-argument constructor.</p>
     */
    Store store;

    /**
     * The creator of the GPX file. The creator is a property of the GPX node;
     * it is not part of the content marshalled in a GPX {@code <metadata>} element.
     */
    public String creator;

    /**
     * The name of the GPX file.
     *
     * @see #getTitle()
     */
    @XmlElement(name = Tags.NAME)
    public String name;

    /**
     * A description of the contents of the GPX file.
     *
     * @see #getAbstract()
     */
    @XmlElement(name = Tags.DESCRIPTION)
    public String description;

    /**
     * The person or organization who created the GPX file.
     *
     * @see #getPointOfContacts()
     */
    @XmlElement(name = Tags.AUTHOR)
    public Person author;

    /**
     * Copyright and license information governing use of the file.
     *
     * @see #getResourceConstraints()
     */
    @XmlElement(name = Tags.COPYRIGHT)
    public Copyright copyright;

    /**
     * URLs associated with the location described in the file, or {@code null} if none.
     *
     * @see #getOnlineResources()
     */
    @XmlElement(name = Tags.LINK)
    public List<Link> links;

    /**
     * The creation date of the file.
     *
     * @see #getDates()
     *
     * @todo We would like to use {@link java.time}, but it does not yet work out-of-the-box with JAXB
     *       (we need adapter). Furthermore, current GeoAPI interfaces does not yet use {@code java.time}.
     */
    @XmlElement(name = Tags.TIME)
    public Date time;

    /**
     * Keywords associated with the file, or {@code null} if unspecified.
     * Search engines or databases can use this information to classify the data.
     *
     * @see #getDescriptiveKeywords()
     */
    @XmlList
    @XmlElement(name = Tags.KEYWORDS)
    public List<String> keywords;

    /**
     * Minimum and maximum coordinates which describe the extent of the coordinates in the file.
     * The GPX 1.1 specification restricts the coordinate reference system to WGS84.
     *
     * @see #getExtents()
     */
    @XmlElement(name = Tags.BOUNDS)
    public Bounds bounds;

    /**
     * The format returned by {@link #getResourceFormats()}, created when first needed.
     *
     * @see #getResourceFormats()
     */
    private Format format;

    /**
     * Creates an initially empty metadata object.
     */
    public Metadata() {
    }

    /**
     * Copies properties from the given ISO 19115 metadata.
     * If a property has more than one value, only the first one will be retained
     * (except for links and keywords where multi-values are allowed).
     */
    Metadata(final org.opengis.metadata.Metadata md, final Locale locale) {
        for (final Identification id : md.getIdentificationInfo()) {
            /*
             * identificationInfo.citation.title                    →   name
             * identificationInfo.citation.date.date                →   time
             * identificationInfo.citation.onlineResource.name      →   link.text
             * identificationInfo.citation.onlineResource.linkage   →   link.uri
             * identificationInfo.abstract                          →   description
             */
            final Citation ci = id.getCitation();
            if (ci != null) {
                if (name == null) {
                    name = Types.toString(ci.getTitle(), locale);
                }
                if (time == null) {
                    for (final CitationDate d : ci.getDates()) {
                        time = d.getDate();
                        if (time != null) break;
                    }
                }
                for (final OnlineResource r : ci.getOnlineResources()) {
                    links = addIfNonNull(links, Link.castOrCopy(r, locale));
                }
            }
            if (description == null) {
                description = Types.toString(id.getAbstract(), locale);
            }
            /*
             * identificationInfo.pointOfContact.party.name   →   creator        if role is ORIGINATOR
             * identificationInfo.pointOfContact.party.name   →   author.name    if role is AUTHOR
             */
            for (final Responsibility r : id.getPointOfContacts()) {
                final Person p = Person.castOrCopy(r, locale);
                if (p != null) {
                    if (p.isCreator) {
                        if (creator == null) {
                            creator = p.name;
                        }
                    } else if (author == null) {
                        author = p;
                    }
                }
            }
            /*
             * identificationInfo.resourceConstraints.responsibleParty.party.name        →   copyright.author
             * identificationInfo.resourceConstraints.reference.date.date                →   copyright.year
             * identificationInfo.resourceConstraints.reference.onlineResource.linkage   →   copyright.license
             */
            if (copyright == null) {
                for (final Constraints c : id.getResourceConstraints()) {
                    if (c instanceof LegalConstraints) {
                        copyright = Copyright.castOrCopy((LegalConstraints) c, locale);
                        if (copyright != null) break;
                    }
                }
            }
            /*
             * identificationInfo.descriptiveKeywords.keyword   →   keywords
             */
            for (final Keywords k : id.getDescriptiveKeywords()) {
                for (final InternationalString word : k.getKeywords()) {
                    keywords = addIfNonNull(keywords, Types.toString(word, locale));
                }
            }
            /*
             * identificationInfo.extent.geographicElement   →   bounds
             */
            if (bounds == null) {
                bounds = Bounds.castOrCopy(Extents.getGeographicBoundingBox(md));
            }
        }
    }

    /**
     * Returns the given ISO 19115 metadata as a {@code Metadata} instance.
     * This method copies the data only if needed.
     *
     * @param  md      the ISO 19115 metadata, or {@code null}.
     * @param  locale  the locale to use for localized strings.
     * @return the GPX metadata, or {@code null}.
     */
    public static Metadata castOrCopy(final org.opengis.metadata.Metadata md, final Locale locale) {
        return (md == null || md instanceof Metadata) ? (Metadata) md : new Metadata(md, locale);
    }

    /**
     * ISO 19115 metadata property determined by the {@link #name} field.
     * This is part of the information returned by {@link #getCitation()}.
     *
     * @return the cited resource name.
     */
    @Override
    public InternationalString getTitle() {
        return (name != null) ? new SimpleInternationalString(name) : super.getTitle();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #description} field.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     *
     * @return brief narrative summary of the resource.
     */
    @Override
    public InternationalString getAbstract() {
        return (description != null) ? new SimpleInternationalString(description) : super.getAbstract();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #keywords} field.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     *
     * @return category keywords, their type, and reference source.
     */
    @Override
    public Collection<Keywords> getDescriptiveKeywords() {
        if (keywords != null) {
            return Collections.singletonList(new DefaultKeywords(keywords.toArray(String[]::new)));
        }
        return Collections.emptyList();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #author} field.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     *
     * @return means of communication with person(s) and organisations(s) associated with the resource.
     */
    @Override
    public Collection<ResponsibleParty> getPointOfContacts() {
        if (creator != null) {
            final var p = new Person(creator);
            return (author != null) ? UnmodifiableArrayList.wrap(new ResponsibleParty[] {p, author})
                                    : Collections.singletonList(author);
        }
        return (author != null) ? Collections.singletonList(author) : Collections.emptyList();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #copyright} field.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     *
     * @return constraints which apply to the resource(s).
     */
    @Override
    public Collection<Constraints> getResourceConstraints() {
        return (copyright != null) ? Collections.singletonList(copyright) : Collections.emptyList();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #bounds} field.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     *
     * @return spatial and temporal extent of the resource.
     */
    @Override
    public Collection<Extent> getExtents() {
        return (bounds != null) ? Collections.singletonList(bounds) : Collections.emptyList();
    }

    /**
     * Description of the spatial and temporal reference systems used in the dataset.
     * This is fixed to WGS 84 in GPX files. We use (latitude, longitude) axis order.
     *
     * @return WGS 84 (EPSG:4326).
     */
    @Override
    public Collection<ReferenceSystem> getReferenceSystemInfo() {
        return Collections.singletonList(CommonCRS.WGS84.geographic());
    }

    /**
     * ISO 19115 metadata property determined by the {@link #time} field.
     * This is part of the information returned by {@link #getCitation()}.
     *
     * @return reference dates for the cited resource.
     */
    @Override
    public Collection<CitationDate> getDates() {
        if (time != null) {
            return Collections.singletonList(new DefaultCitationDate(time, DateType.CREATION));
        }
        return Collections.emptyList();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #links} field.
     * This is part of the information returned by {@link #getCitation()}.
     *
     * @return online references to the cited resource.
     */
    @Override
    public Collection<OnlineResource> getOnlineResources() {
        return (links != null) ? Collections.unmodifiableList(links) : Collections.emptyList();
    }

    /**
     * Names of features types available for the GPX format, or an empty list if none.
     * This property is not part of metadata described in GPX file; it is rather a hard-coded value shared by all
     * GPX files. Users could however filter the list of features, for example with only routes and no tracks.
     *
     * @return information about the feature characteristics.
     */
    @Override
    public Collection<ContentInformation> getContentInfo() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Store store = this.store;
        return (store != null) ? store.types.metadata : Collections.emptyList();
    }

    /**
     * Description of the format of the resource(s).
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     *
     * @return description of the format of the resource(s).
     */
    @Override
    public Collection<Format> getResourceFormats() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Store store = this.store;
        if (store != null) {
            Format f;
            synchronized (store) {
                if ((f = format) == null) {
                    format = f = store.getFormat();
                }
            }
            return Collections.singletonList(f);
        }
        return Collections.emptyList();
    }

    /**
     * Citation(s) for the standard(s) to which the metadata conform.
     *
     * @return ISO 19115-1 citation.
     */
    @Override
    public Collection<Citation> getMetadataStandards() {
        return Collections.singleton(Citations.ISO_19115.get(0));
    }

    /**
     * Compares this {@code Metadata} with the given object for equality.
     *
     * @param  obj  the object to compare with this {@code Metadata}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Metadata) {
            final var that = (Metadata) obj;
            return Objects.equals(this.creator,     that.creator)     &&
                   Objects.equals(this.name,        that.name)        &&
                   Objects.equals(this.description, that.description) &&
                   Objects.equals(this.author,      that.author)      &&
                   Objects.equals(this.copyright,   that.copyright)   &&
                   Objects.equals(this.links,       that.links)       &&
                   Objects.equals(this.time,        that.time)        &&
                   Objects.equals(this.keywords,    that.keywords)    &&
                   Objects.equals(this.bounds,      that.bounds);
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code Metadata}.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(creator, name, description, author, copyright, links, time, keywords, bounds);
    }

    /**
     * Returns a string representation of this metadata object.
     *
     * @return a string representation of this metadata.
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder();
        buffer.append("GPX metadata").append(System.lineSeparator());
        final var table = new TableAppender(buffer);
        table.setMultiLinesCells(true);
        table.appendHorizontalSeparator();
        append(table, "Creator",     creator);
        append(table, "Name",        name);
        append(table, "Description", description);
        append(table, "Author",      author);
        append(table, "Copyright",   copyright);
        append(table, "Link(s)",     links, System.lineSeparator());
        append(table, "Time",        (time != null) ? time.toInstant() : null);
        append(table, "Keywords",    keywords, " ");
        append(table, "Bounds",      bounds);
        table.appendHorizontalSeparator();
        try {
            table.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // Should never happen since we are writing to a StringBuilder.
        }
        return buffer.toString();
    }

    /**
     * Appends a row to the given table if the given value is non-null.
     *
     * @param  table  the table where to append a row.
     * @param  label  the label.
     * @param  value  the value, or {@code null} if none.
     */
    private static void append(final TableAppender table, final String label, final Object value) {
        if (value != null) {
            table.append(label).append(':').nextColumn();
            table.append(value.toString()).nextLine();
        }
    }

    /**
     * Appends a multi-values to the given table if the given list is non-null.
     *
     * @param  table      the table where to append a row.
     * @param  label      the label.
     * @param  values     the values, or {@code null} if none.
     * @param  separator  the separator to insert between each value.
     */
    private static void append(final TableAppender table, final String label, final List<?> values, final String separator) {
        if (values != null && !values.isEmpty()) {
            table.append(label).append(':').nextColumn();
            final int n = values.size();
            for (int i=0; i<n; i++) {
                if (i != 0) table.append(separator);
                table.append(values.get(i).toString());
            }
            table.nextLine();
        }
    }

    /**
     * Adds the given element to the given list if non null, or do nothing otherwise.
     * This is a convenience method for storing {@code <link>} elements in way points,
     * routes or tracks among others.
     *
     * @param  list     the list where to add the element, or {@code null} if not yet created.
     * @param  element  the element to add, or {@code null} if none.
     * @return the list where the element has been added.
     */
    static <T> List<T> addIfNonNull(List<T> list, final T element) {
        if (element != null) {
            if (list == null) {
                list = new ArrayList<>(4);         // Small capacity since there is usually few elements.
            }
            list.add(element);
        }
        return list;
    }
}
