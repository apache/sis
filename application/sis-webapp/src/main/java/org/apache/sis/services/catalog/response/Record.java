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
package org.apache.sis.services.catalog.response;

import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author haonguyen
 */
@XmlType(name = "RecordType", namespace = Namespaces.CSW, propOrder = {
    "title",
    "creator",
    "subject",
    "description",             
    "publisher",
    "contributor",
    "date",
    "type",
    "format",
    "identifier",
    "source",     
    "language",
    "relation",
    "rights",
    "coverage",
    
})
@XmlRootElement(name = "Record", namespace = Namespaces.CSW)

public class Record extends AbstractRecord{
    private String title;
    private String creator;
    private String subject;
    private String description;
    private String publisher;
    private String contributor;
    private Date date;
    private String type;
    private String format;
    private String identifier;
    private String source;
    private String language;
    private String relation;
    private BoundingBox coverage;
    private String rights;

    /**
     *
     * @return
     */
    @XmlElement(name = "title",namespace=Namespaces.DC)
    public String getTitle() {
        return title;
    }

    /**
     *
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "creator",namespace=Namespaces.DC)
    public String getCreator() {
        return creator;
    }

    /**
     *
     * @param creator
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "subject",namespace=Namespaces.DC)
    public String getSubject() {
        return subject;
    }

    /**
     *
     * @param subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "abstract",namespace=Namespaces.DCT)
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "publisher",namespace=Namespaces.DC)
    public String getPublisher() {
        return publisher;
    }

    /**
     *
     * @param publisher
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "contributor",namespace=Namespaces.DC)
    public String getContributor() {
        return contributor;
    }

    /**
     *
     * @param contributor
     */
    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "modified",namespace=Namespaces.DCT)
    public Date getDate() {
        return date;
    }

    /**
     *
     * @param date
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "type",namespace=Namespaces.DC)
    public String getType() {
        return type;
    }

    /**
     *
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "format",namespace=Namespaces.DC)
    public String getFormat() {
        return format;
    }

    /**
     *
     * @param format
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "identifier",namespace=Namespaces.DC)
    public String getIdentifier() {
        return identifier;
    }

    /**
     *
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "source",namespace=Namespaces.DC)
    public String getSource() {
        return source;
    }

    /**
     *
     * @param source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "language",namespace=Namespaces.DC)
    public String getLanguage() {
        return language;
    }

    /**
     *
     * @param language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "relation",namespace=Namespaces.DC)
    public String getRelation() {
        return relation;
    }

    /**
     *
     * @param relation
     */
    public void setRelation(String relation) {
        this.relation = relation;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "BoundingBox",namespace=Namespaces.OWS)
    public BoundingBox getCoverage() {
        return coverage;
    }

    /**
     *
     * @param coverage
     */
    public void setCoverage(BoundingBox coverage) {
        this.coverage = coverage;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "rights",namespace=Namespaces.DC)
    public String getRights() {
        return rights;
    }

    /**
     *
     * @param rights
     */
    public void setRights(String rights) {
        this.rights = rights;
    } 
}