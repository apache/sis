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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author haonguyen
 */
@XmlRootElement(namespace = Element.CSW, name = "Record")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
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

public class SummaryRecord {

    /**
     * An entity primarily responsible for making the content of the resource .
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String creator;
    /**
     * An entity responsible contributions to the content of the resource.
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String contributor;

    /**
     * An entity responsible for making the resource avaible.
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String publisher;

    /**
     * A topic of the content of the resource.
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String subject;

    /**
     * A unique reference to the record within the catalogue.
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String identifier;

    /**
     * A reference to a related resource.
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String relation;

    /**
     * The nature or genre of the content of the resource. Type can include
     * general categories, genres or aggregation levels of content.
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String type;

    /**
     * A name given to the resource.
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String title;

    /**
     * Date on which the record was created or updated within the catalogue.
     */
    @XmlElement(namespace = Element.DUBLIN_TERMS)
    private Date modified;

    /**
     * A language of the intellectual content of the catalog record.
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String language;

    /**
     * The physical or digital manifestation of the resource.
     */
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String format;

    /**
     * A bouding box for identifying a geographic area of interest.
     */
    @XmlElement(namespace = Element.OWS)
    private BoundingBox BoundingBox;

    public SummaryRecord() {
    }

    /**
     * Constructs SummaryRecord to create all of the values.
     */
    public SummaryRecord(String creator, String contributor, String publisher, String subject, String identifier, String relation, String type, String title, Date modified, String language, String format, BoundingBox BoundingBox) {
        this.creator = creator;
        this.contributor = contributor;
        this.publisher = publisher;
        this.subject = subject;
        this.identifier = identifier;
        this.relation = relation;
        this.type = type;
        this.title = title;
        this.modified = modified;
        this.language = language;
        this.format = format;
        this.BoundingBox = BoundingBox;
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
