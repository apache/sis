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
import java.util.logging.Level;
import javax.xml.bind.annotation.XmlElement;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.Keywords;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.InternationalString;

import org.apache.sis.internal.simple.SimpleMetadata;
import org.apache.sis.io.TableAppender;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.logging.Logging;


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
     */
    @XmlElement
    public Envelope bounds;

    /**
     * Creates an initially empty metadata object.
     */
    public Metadata() {
    }

    @Override
    public Collection<Identifier> getIdentifiers() {
        if (name != null) {
            return Collections.singleton(new DefaultIdentifier(name));
        }
        return super.getIdentifiers();
    }

    @Override
    public InternationalString getAbstract() {
        if (description != null) {
            return new SimpleInternationalString(description);
        }
        return super.getAbstract();
    }

    @Override
    public Collection<Keywords> getDescriptiveKeywords() {
        if (keywords != null) {
            return Collections.singleton(new DefaultKeywords(keywords));
        }
        return null;
    }

    @Override
    public Collection<Responsibility> getPointOfContacts() {
        if (author != null) {
            return Collections.singletonList(author);
        }
        return super.getPointOfContacts();
    }

    @Override
    public Collection<Constraints> getResourceConstraints() {
        if (copyright != null) {
            return Collections.singleton(copyright);
        }
        return super.getResourceConstraints();
    }

    @Override
    public Collection<Extent> getExtents() {
        if (bounds != null) {
            final DefaultExtent ext = new DefaultExtent();
            try {
                ext.addElements(bounds);
            } catch (TransformException ex) {
                Logging.getLogger("oeg.apache.storage").log(Level.WARNING, ex.getMessage(),ex);
            }
            return Collections.singleton(ext);
        }
        return super.getExtents();
    }

    @Override
    public Collection<OnlineResource> getOnlineResources() {
        if (!links.isEmpty()) {
            final List<OnlineResource> resources = new ArrayList<>();
            for (URI uri : links) {
                resources.add(new DefaultOnlineResource(uri));
            }
            return resources;
        }
        return super.getOnlineResources();
    }

    @Override
    public Collection<CitationDate> getDates() {
        if (time != null) {
            final CitationDate date = new DefaultCitationDate(Date.from(Instant.from(time)), DateType.CREATION);
            return Collections.singleton(date);
        }
        return super.getDates();
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
