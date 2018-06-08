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
package org.apache.sis.services.csw.discovery;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author haonguyen
 */
@XmlType(name = "GetRecordsType", namespace = Namespaces.CSW, propOrder = {    
    "requestID",
    "searchStatus",
    "searchResults",
})
@XmlRootElement(name = "GetRecords", namespace = Namespaces.CSW)
public class GetRecordsResponse {
    private String requestID;
    private RequestStatus searchStatus;
    private SearchResults searchResults;
//    private 

    /**
     *
     * @return
     */
    @XmlElement(name = "RequestID",namespace=Namespaces.CSW)
    public String getRequestID() {
        return requestID;
    }

    /**
     *
     * @param requestID
     */
    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "SearchStatus",namespace=Namespaces.CSW)
    public RequestStatus getSearchStatus() {
        return searchStatus;
    }

    /**
     *
     * @param searchStatus
     */
    public void setSearchStatus(RequestStatus searchStatus) {
        this.searchStatus = searchStatus;
    }

    /**
     *
     * @return
     */
    @XmlElement(name = "SearchResults",namespace=Namespaces.CSW)
    public SearchResults getSearchResults() {
        return searchResults;
    }

    /**
     *
     * @param searchResults
     */
    public void setSearchResults(SearchResults searchResults) {
        this.searchResults = searchResults;
    }
    
}
