package org.apache.sis.services.csw;

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

import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.sis.services.csw.request.AbstractRecord;
import org.apache.sis.xml.Namespaces;

/**
 *
 * @author haonguyen
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SearchResults")
public class RecordResult {
    @XmlAttribute(name = "numberOfRecordsMatched")
    private int numberofrecordsmatched;
    @XmlAttribute(name = "numberOfRecordsReturned")
    private int numberofrecordsreturned;
    @XmlAttribute(name = "elementSet")
    private String elementset;
    @XmlAttribute(name = "nextRecord")
    private int nextrecord;
    @XmlElement(name="SummaryRecord",namespace=Namespaces.CSW)
    private Collection<AbstractRecord> summary;
    @XmlElement(name="Record",namespace=Namespaces.CSW)
    private Collection<AbstractRecord> record;
    @XmlElement(name="BriefRecord",namespace=Namespaces.CSW)
    private Collection<AbstractRecord> brif;

    public Collection<AbstractRecord> getSummary() {
        return summary;
    }

    public void setSummary(Collection<AbstractRecord> summary) {
        this.summary = summary;
    }

    public Collection<AbstractRecord> getRecord() {
        return record;
    }

    public void setRecord(Collection<AbstractRecord> record) {
        this.record = record;
    }

    public Collection<AbstractRecord> getBrif() {
        return brif;
    }

    public void setBrif(Collection<AbstractRecord> brif) {
        this.brif = brif;
    }

    

    public int getNumberofrecordsmatched() {
        return numberofrecordsmatched;
    }

    public void setNumberofrecordsmatched(int numberofrecordsmatched) {
        this.numberofrecordsmatched = numberofrecordsmatched;
    }

    public int getNumberofrecordsreturned() {
        return numberofrecordsreturned;
    }

    public void setNumberofrecordsreturned(int numberofrecordsreturned) {
        this.numberofrecordsreturned = numberofrecordsreturned;
    }

    public String getElementset() {
        return elementset;
    }

    public void setElementset(String elementset) {
        this.elementset = elementset;
    }

    public int getNextrecord() {
        return nextrecord;
    }

    public void setNextrecord(int nextrecord) {
        this.nextrecord = nextrecord;
    }
}
