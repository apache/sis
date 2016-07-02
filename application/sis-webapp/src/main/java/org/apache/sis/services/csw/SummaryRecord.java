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

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.util.iso.Types;


/**
 * Summary of an ISO 19115 metadata record.
 * This class wraps a {@link DefaultMetadata} instance and exposes its main properties as Dublin Core properties.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @since   0.8
 * @version 0.8
 * @module
 */
@XmlRootElement(namespace = Element.DUBLIN_CORE, name = "SummaryRecord")
public class SummaryRecord extends Element {
    /**
     * The ISO 19115 metadata where to get and store information.
     */
    private DefaultMetadata metadata;

    private long id;
    private String identifier;
    private String type;
    private String format;
    private Date modified;
    private BoundingBox bbox;
    private List<Link> links = new ArrayList<>();

    private Locale locale;

    public SummaryRecord() {
        metadata = new DefaultMetadata();
    }

    @XmlElement(name = "id")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @XmlElement(name = "identifier")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns a name given to the resource, or {@code null} if none.
     * This method maps to the {@code metadata/identificationInfo/citation/title} property of ISO 19115.
     * If the metadata contains information about more than one resource, then the title of all resources
     * (omitting duplicated values) are included in the returned string.
     *
     * @return a name given to the resource.
     */
    @XmlElement(name = "title")
    public String getTitle() {
        final Set<String> titles = new HashSet<>();
        for (final Identification identification : metadata.getIdentificationInfo()) {
            if (identification != null) {
                final Citation citation = identification.getCitation();
                if (citation != null) {
                    final InternationalString i18n = citation.getTitle();
                    if (i18n != null) {
                        titles.add(i18n.toString(locale));
                    }
                }
            }
        }
        return toString(titles, System.lineSeparator());
    }

    /**
     * Sets a name for to the resource.
     *
     * @param title a name given to the resource.
     */
    public void setTitle(final String title) {
        final InternationalString i18n = Types.toInternationalString(title);
        for (final Identification identification : metadata.getIdentificationInfo()) {
            if (identification != null) {
                Citation citation = identification.getCitation();
                if (citation instanceof DefaultCitation) {
                    ((DefaultCitation) citation).setTitle(i18n);
                }
            }
        }
        // TODO: what to do if we couldn't set the citation title?
    }

    /**
     * Returns the physical or digital manifestation of the resource, or {@code null} if none.
     *
     * @return the physical or digital manifestation of the resource.
     */
    @XmlElement(name = "format")
    public String getFormat() {
        metadata.getDistributionInfo();
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @XmlElement(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(namespace = "http://purl.org/dc/terms", name = "modified")
    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    @XmlElement(namespace = "http://www.opengis.net/ows", name = "bbox")
    public BoundingBox getBoundingBox() {
        return bbox;
    }

    public void setBoundingBox(BoundingBox bbox) {
        this.bbox = bbox;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void addLink(String url,String rel){
        Link link = new Link();
        link.setLink(url);
        link.setRel(rel);
        links.add(link);
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
}
