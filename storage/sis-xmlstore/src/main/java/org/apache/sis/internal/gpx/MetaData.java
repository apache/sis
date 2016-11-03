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

import java.io.IOException;
import java.io.StringWriter;
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
import org.apache.sis.io.TableAppender;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.logging.Logging;

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

/**
 * Metadata object as defined in GPX.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
public class MetaData extends SimpleMetadata {

    public String name;
    public String description;
    public Person person;
    public Copyright copyRight;
    public final List<URI> links = new ArrayList<>();
    public Temporal time;
    public String keywords;
    public Envelope bounds;

    @Override
    public Collection<? extends Identifier> getIdentifiers() {
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
    public Collection<? extends Keywords> getDescriptiveKeywords() {
        if (keywords != null) {
            return Collections.singleton(new DefaultKeywords(keywords));
        }
        return null;
    }

    @Override
    public Collection<? extends Responsibility> getPointOfContacts() {
        if (person != null) {
            return Collections.singletonList(person);
        }
        return super.getPointOfContacts();
    }

    @Override
    public Collection<? extends Constraints> getResourceConstraints() {
        if (copyRight != null) {
            return Collections.singleton(copyRight);
        }
        return super.getResourceConstraints();
    }

    @Override
    public Collection<? extends Extent> getExtents() {
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
    public Collection<? extends OnlineResource> getOnlineResources() {
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
    public Collection<? extends CitationDate> getDates() {
        if (time != null) {
            final CitationDate date = new DefaultCitationDate(Date.from(Instant.from(time)), DateType.CREATION);
            return Collections.singleton(date);
        }
        return super.getDates();
    }

    @Override
    public String toString() {
        final StringWriter writer = new StringWriter();
        final TableAppender tablewriter = new TableAppender(writer);

        tablewriter.appendHorizontalSeparator();
        tablewriter.append("GPX-Metadata\t \n");
        tablewriter.appendHorizontalSeparator();

        tablewriter.append("Name\t"+name+"\n");
        tablewriter.append("Desc\t"+description+"\n");
        tablewriter.append("Time\t"+time+"\n");
        tablewriter.append("Keywords\t"+keywords+"\n");
        tablewriter.append("Bounds\t"+bounds+"\n");

        if(person != null){
            tablewriter.append("Person - Name\t"+person.name+"\n");
            tablewriter.append("Person - EMail\t"+person.email+"\n");
            tablewriter.append("Person - Link\t"+person.link+"\n");
        }else{
            tablewriter.append("Person\t"+person+"\n");
        }

        if(copyRight != null){
            tablewriter.append("Copyright - Author\t"+copyRight.author+"\n");
            tablewriter.append("Copyright - Year\t"+copyRight.year+"\n");
            tablewriter.append("Copyright - License\t"+copyRight.license+"\n");
        }else{
            tablewriter.append("Copyright\t"+copyRight+"\n");
        }

        tablewriter.append("Links\t");
        if(links.isEmpty()){
            tablewriter.append("None\n");
        }else{
            tablewriter.append("\n");
            for(final URI uri : links){
                tablewriter.append("\t"+uri+"\n");
            }
        }

        tablewriter.appendHorizontalSeparator();

        try {
            tablewriter.flush();
            writer.flush();
        } catch (IOException ex) {
            //will never happen is this case
            ex.printStackTrace();
        }

        return writer.getBuffer().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MetaData other = (MetaData) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.description, other.description)) {
            return false;
        }
        if (!Objects.equals(this.keywords, other.keywords)) {
            return false;
        }
        if (!Objects.equals(this.person, other.person)) {
            return false;
        }
        if (!Objects.equals(this.copyRight, other.copyRight)) {
            return false;
        }
        if (!Objects.equals(this.links, other.links)) {
            return false;
        }
        if (!Objects.equals(this.time, other.time)) {
            return false;
        }
        if (!Objects.equals(this.bounds, other.bounds)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 43;
    }

}
