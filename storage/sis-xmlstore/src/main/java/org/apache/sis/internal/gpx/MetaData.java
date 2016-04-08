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
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.io.TableAppender;

import org.opengis.geometry.Envelope;

/**
 * Metadata object as defined in GPX.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class MetaData {

    private String name;
    private String description;
    private Person person;
    private CopyRight copyRight;
    private final List<URI> links = new ArrayList<>();
    private Temporal time;
    private String keywords;
    private Envelope bounds;

    /**
     * Returns the dataset name.
     *
     * @return name, may be null
     */
    public String getName() {
        return name;
    }

    /**
     * Set dataset name.
     *
     * @param name, can be null
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the dataset description.
     *
     * @return description, may be null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set dataset description.
     *
     * @param description, can be null
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns dataset author person.
     *
     * @return Person, may be null
     */
    public Person getPerson() {
        return person;
    }

    /**
     * Set dataset author person.
     *
     * @param person, can be null
     */
    public void setPerson(Person person) {
        this.person = person;
    }

    /**
     * Returns the dataset copyright
     *
     * @return copyright, may be null
     */
    public CopyRight getCopyRight() {
        return copyRight;
    }

    /**
     * Set dataset copyright.
     *
     * @param copyRight, can be null
     */
    public void setCopyRight(CopyRight copyRight) {
        this.copyRight = copyRight;
    }

    /**
     * Returns the dataset links
     * The returned list is modifiable.
     *
     * @return list of links, never null
     */
    public List<URI> getLinks() {
        return links;
    }

    /**
     * Returns the dataset creation time.
     *
     * @return Temporal, may be null
     */
    public Temporal getTime() {
        return time;
    }

    /**
     * Set dataset creation time.
     *
     * @param time, can be null
     */
    public void setTime(Temporal time) {
        this.time = time;
    }

    /**
     * Returns dataset keywords.
     *
     * @return keywords, may be null
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Set dataset keywords.
     *
     * @param keywords, can be null
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     * Returns the dataset bounding box.
     *
     * @return Envelope, may be null
     */
    public Envelope getBounds() {
        return bounds;
    }

    /**
     * Set dataset bounding box.
     *
     * @param bounds, can be null
     */
    public void setBounds(Envelope bounds) {
        this.bounds = bounds;
    }

    @Override
    public String toString() {
        final StringWriter writer = new StringWriter();
        final TableAppender tablewriter = new TableAppender(writer);

        tablewriter.appendHorizontalSeparator();
        tablewriter.append("GPX-Metadata\t \n");
        tablewriter.appendHorizontalSeparator();

        tablewriter.append("Name\t"+getName()+"\n");
        tablewriter.append("Desc\t"+getDescription()+"\n");
        tablewriter.append("Time\t"+getTime()+"\n");
        tablewriter.append("Keywords\t"+getKeywords()+"\n");
        tablewriter.append("Bounds\t"+getBounds()+"\n");

        final Person person = getPerson();
        if(person != null){
            tablewriter.append("Person - Name\t"+person.getName()+"\n");
            tablewriter.append("Person - EMail\t"+person.getEmail()+"\n");
            tablewriter.append("Person - Link\t"+person.getLink()+"\n");
        }else{
            tablewriter.append("Person\t"+person+"\n");
        }

        final CopyRight copyright = getCopyRight();
        if(copyright != null){
            tablewriter.append("CopyRight - Author\t"+copyright.getAuthor()+"\n");
            tablewriter.append("CopyRight - Year\t"+copyright.getYear()+"\n");
            tablewriter.append("CopyRight - License\t"+copyright.getLicense()+"\n");
        }else{
            tablewriter.append("CopyRight\t"+copyright+"\n");
        }

        tablewriter.append("Links\t");
        final List<URI> links = getLinks();
        if(links.isEmpty()){
            tablewriter.append("None\n");
        }else{
            tablewriter.append("\n");
            for(final URI uri : getLinks()){
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

}
