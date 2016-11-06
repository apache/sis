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
package org.apache.sis.internal.gpx;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;

import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Keywords;
import org.opengis.util.InternationalString;

import org.apache.sis.internal.simple.SimpleMetadata;
import org.apache.sis.io.TableAppender;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.util.iso.SimpleInternationalString;


/**
 * Information about the GPX file, author, and copyright restrictions.
 * This is the root of the {@code <metadata>} element in a GPX file.
 * At most one such element may appear in the document.
 * The XML content is like below:
 *
 * {@preformat java
 *   <metadata>
 *     <name> String </name>
 *     <desc> String </desc>
 *     <author> Person </author>
 *     <copyright> Copyright </copyright>
 *     <link> URI </link>
 *     <time> Temporal </time>
 *     <keywords> String </keywords>
 *     <bounds> Envelope </bounds>
 *     <extensions> (ignored) </extensions>
 *   </metadata>
 * }
 *
 * Those properties can be read or modified directly. All methods defined in this class are bridges to
 * the ISO 19115 metadata model and can be ignored if the user only wants to manipulate the GPX model.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class Metadata extends SimpleMetadata {
    /**
     * The name of the GPX file.
     */
    @XmlElement
    public String name;

    /**
     * A description of the contents of the GPX file.
     */
    @XmlElement(name = "desc")
    public String description;

    /**
     * The person or organization who created the GPX file.
     */
    @XmlElement
    public Person author;

    /**
     * Copyright and license information governing use of the file.
     */
    @XmlElement
    public Copyright copyright;

    /**
     * URLs associated with the location described in the file.
     */
    @XmlElement(name = "link")
    public final List<URI> links = new ArrayList<>();

    /**
     * The creation date of the file.
     */
    @XmlElement
    public Temporal time;

    /**
     * Keywords associated with the file.
     * Search engines or databases can use this information to classify the data.
     */
    @XmlElement
    public String keywords;

    /**
     * Minimum and maximum coordinates which describe the extent of the coordinates in the file.
     * The GPX 1.1 specification restricts the coordinate reference system to WGS84.
     */
    @XmlElement
    public GeographicBoundingBox bounds;

    /**
     * Creates an initially empty metadata object.
     */
    public Metadata() {
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
        return (keywords != null) ? Collections.singleton(new DefaultKeywords(keywords)) : super.getDescriptiveKeywords();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #author} field.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     *
     * @return means of communication with person(s) and organisations(s) associated with the resource.
     */
    @Override
    public Collection<Responsibility> getPointOfContacts() {
        return (author != null) ? Collections.singletonList(author) : super.getPointOfContacts();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #copyright} field.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     *
     * @return constraints which apply to the resource(s).
     */
    @Override
    public Collection<Constraints> getResourceConstraints() {
        return (copyright != null) ? Collections.singleton(copyright) : super.getResourceConstraints();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #bounds} field.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     *
     * @return spatial and temporal extent of the resource.
     */
    @Override
    public Collection<Extent> getExtents() {
        if (bounds != null) {
            return Collections.singleton(new DefaultExtent(null, bounds, null, null));
        } else {
            return super.getExtents();
        }
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
            final CitationDate date = new DefaultCitationDate(Date.from(Instant.from(time)), DateType.CREATION);
            return Collections.singleton(date);
        }
        return super.getDates();
    }

    /**
     * ISO 19115 metadata property determined by the {@link #bounds} field.
     * This is part of the information returned by {@link #getCitation()}.
     *
     * @return online references to the cited resource.
     */
    @Override
    public Collection<OnlineResource> getOnlineResources() {
        final int size = links.size();
        if (size != 0) {
            final List<OnlineResource> resources = new ArrayList<>(size);
            for (URI uri : links) {
                resources.add(new DefaultOnlineResource(uri));
            }
            return resources;
        }
        return super.getOnlineResources();
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
            final Metadata that = (Metadata) obj;
            return Objects.equals(this.name,        that.name)        &&
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
        return Objects.hash(name, description, author, copyright, links, time, keywords, bounds);
    }

    /**
     * Returns a string representation of this metadata object.
     *
     * @return a string representation of this metadata.
     */
    @Override
    public String toString() {
        final TableAppender table = new TableAppender();
        table.setMultiLinesCells(true);
        table.appendHorizontalSeparator();
        table.append("GPX metadata").nextLine();
        table.appendHorizontalSeparator();
        append(table, "Name",        name);
        append(table, "Description", description);
        append(table, "Author",      author);
        append(table, "Copyright",   copyright);
        String label = "link";
        for (final URI uri : links) {
            append(table, label, uri);
            label = null;
        }
        append(table, "Time",     time);
        append(table, "Keywords", keywords);
        append(table, "Bounds",   bounds);
        table.append("Links\t");
        table.appendHorizontalSeparator();
        return table.toString();
    }

    /**
     * Appends a row to the given table if the given value is non-null.
     *
     * @param  table  the table where to append a row.
     * @param  label  the label, or {@code null} if none.
     * @param  value  the value, or {@code null} if none.
     */
    private static void append(final TableAppender table, final String label, final Object value) {
        if (value != null) {
            if (label != null) {
                table.append(label).append(':');
            }
            table.nextColumn();
            table.append(value.toString()).nextLine();
        }
    }
}
