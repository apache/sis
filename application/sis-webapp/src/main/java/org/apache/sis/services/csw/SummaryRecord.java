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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author haonguyen
 */
@XmlRootElement(namespace = "http://www.opengis.net/cat/csw/2.0.2",name="Record")
public class SummaryRecord {

    private long id;
    private String identifier;
    private String title;
    private String type;
    private String format;
    private Date modified;
    private String subject;
    private String creator;
    private String publisher;
    private String contributor;
    private String language;
    private String relation;
    private String name;
@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    private BoundingBox BoundingBox;

   

@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "subject")
    public String getSubject() {
        return subject;
    }

    public void setSubject(String suject) {
        this.subject = suject;
    }

@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "creator")
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "publisher")
    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "contributor")
    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }
@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "language")
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "relation")
    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }
@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "id")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "identifier")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
@XmlElement(namespace = "http://purl.org/dc/elements/1.1/", name = "format")
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
@XmlElement(namespace = "http://purl.org/dc/terms", name = "modified")
    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }
@XmlElement(namespace = "http://www.opengis.net/ows", name = "BoundingBox")
    public BoundingBox getBoundingBox() {
        return BoundingBox;
    }

    public void setBoundingBox(BoundingBox BoundingBox) {
        this.BoundingBox = BoundingBox;
    }
 public SummaryRecord() {

    }
    public SummaryRecord(long id,String name, String identifier, String title, String type, String format, Date modified, String subject, String creator, String publisher, String contributor, String language, String relation, BoundingBox BoundingBox) {
        this.id = id;
        this.identifier = identifier;
        this.title = title;
        this.type = type;
        this.format = format;
        this.modified = modified;
        this.subject = subject;
        this.creator = creator;
        this.publisher = publisher;
        this.contributor = contributor;
        this.language = language;
        this.relation = relation;
        this.BoundingBox = BoundingBox;
        this.name = name;
    }

}
