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

    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String creator;
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String contributor;
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String publisher;
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String subject;
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String identifier;
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String relation;
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String type;
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String title;
    @XmlElement(namespace = Element.DUBLIN_TERMS)
    private Date modified;
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String language;
    @XmlElement(namespace = Element.DUBLIN_CORE)
    private String format;
    @XmlElement(namespace = Element.OWS)
    private BoundingBox BoundingBox;

    public SummaryRecord() {
    }

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

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public BoundingBox getBoundingBox() {
        return BoundingBox;
    }

    public void setBoundingBox(BoundingBox BoundingBox) {
        this.BoundingBox = BoundingBox;
    }

}
