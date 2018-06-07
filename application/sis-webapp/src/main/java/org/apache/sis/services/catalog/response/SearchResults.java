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
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author haonguyen
 */
@XmlType(name = "SearchResultsType", namespace = Namespaces.CSW)
@XmlRootElement(name = "SearchResults", namespace = Namespaces.CSW)
public class SearchResults {
    private String resultSetId;
    private String elementSet;
    private String recordSchema;
    private int numberOfRecordsMatched;
    private int numberOfRecordsReturned;
    private int nextRecord;
    private Date expires;
    private long elapsedTime;
    private RequestStatusType status = RequestStatusType.subset;
    private List<AbstractRecord> abstractRecord;
    private List<FederatedSearchResultBase> federatedSearchResultBase;
    /**
     *
     * @return
     */
    @XmlAttribute
    public String getResultSetId() {
        return resultSetId;
    }

    /**
     *
     * @param resultSetId
     */
    public void setResultSetId(String resultSetId) {
        this.resultSetId = resultSetId;
    }

    /**
     *
     * @return
     */
    @XmlAttribute
    public String getElementSet() {
        return elementSet;
    }

    /**
     *
     * @param elementSet
     */
    public void setElementSet(String elementSet) {
        this.elementSet = elementSet;
    }

    /**
     *
     * @return
     */
    @XmlAttribute
    public String getRecordSchema() {
        return recordSchema;
    }

    /**
     *
     * @param recordSchema
     */
    public void setRecordSchema(String recordSchema) {
        this.recordSchema = recordSchema;
    }

    /**
     *
     * @return
     */
    @XmlAttribute
    public int getNumberOfRecordsMatched() {
        return numberOfRecordsMatched;
    }

    /**
     *
     * @param numberOfRecordsMatched
     */
    public void setNumberOfRecordsMatched(int numberOfRecordsMatched) {
        this.numberOfRecordsMatched = numberOfRecordsMatched;
    }

    /**
     *
     * @return
     */
    @XmlAttribute
    public int getNumberOfRecordsReturned() {
        return numberOfRecordsReturned;
    }

    /**
     *
     * @param numberOfRecordsReturned
     */
    public void setNumberOfRecordsReturned(int numberOfRecordsReturned) {
        this.numberOfRecordsReturned = numberOfRecordsReturned;
    }

    /**
     *
     * @return
     */
    @XmlAttribute
    public int getNextRecord() {
        return nextRecord;
    }

    /**
     *
     * @param nextRecord
     */
    public void setNextRecord(int nextRecord) {
        this.nextRecord = nextRecord;
    }

    /**
     *
     * @return
     */
    @XmlAttribute
    public Date getExpires() {
        return expires;
    }

    /**
     *
     * @param expires
     */
    public void setExpires(Date expires) {
        this.expires = expires;
    }

    /**
     *
     * @return
     */
    @XmlAttribute
    public long getElapsedTime() {
        return elapsedTime;
    }

    /**
     *
     * @param elapsedTime
     */
    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    /**
     *
     * @return
     */
    @XmlAttribute
    public RequestStatusType getStatus() {
        return status;
    }

    /**
     *
     * @param status
     */
    public void setStatus(RequestStatusType status) {
        this.status = status;
    }

    /**
     *
     * @return
     */
    @XmlElementRefs({
        @XmlElementRef(name = "BriefRecord", type = BriefRecord.class),
        @XmlElementRef(name = "SummaryRecord", type = SummaryRecord.class),
        @XmlElementRef(name = "Record", type = Record.class)
    })
    public List<AbstractRecord> getAbstractRecord() {
        return abstractRecord;
    }

    /**
     *
     * @param abstractRecord
     */
    public void setAbstractRecord(List<AbstractRecord> abstractRecord) {
        this.abstractRecord = abstractRecord;
    }

    /**
     *
     * @return
     */
    @XmlElement(namespace=Namespaces.CSW)
    public List<FederatedSearchResultBase> getFederatedSearchResultBase() {
        return federatedSearchResultBase;
    }

    /**
     *
     * @param federatedSearchResultBase
     */
    @XmlElementRefs({
        @XmlElementRef(name = "FederatedSearchResult", type =FederatedSearchResult.class),
        @XmlElementRef(name = "FederatedException", type = FederatedException.class),
    })
    public void setFederatedSearchResultBase(List<FederatedSearchResultBase> federatedSearchResultBase) {
        this.federatedSearchResultBase = federatedSearchResultBase;
    }
    
    
}
