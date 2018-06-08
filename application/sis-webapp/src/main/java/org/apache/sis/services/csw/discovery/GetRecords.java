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

import javax.xml.bind.annotation.*;
import org.apache.sis.services.csw.common.AbstractQuery;
import org.apache.sis.services.csw.common.BasicRetrievalOptions;
import org.apache.sis.services.csw.common.RequestBase;
/**
 *
 * @author haonguyen
 */
@XmlType(name="GetRecordsType",namespace=Namespaces.CSW,propOrder = {    
    "distributedSearch",
    "responseHandler",
    "query",
})
@XmlRootElement(name="GetRecords",namespace = Namespaces.CSW)
public class GetRecords extends RequestBase {
    private DistributedSearch distributedSearch;
    private String responseHandler;
    private Object query;
    private String requestId;
    private BasicRetrievalOptions basicRetrievalOptions;
    @XmlElement(name="DistributedSearch",namespace=Namespaces.CSW)
    public DistributedSearch getDistributedSearch() {
        return distributedSearch;
    }

    public void setDistributedSearch(DistributedSearch distributedSearch) {
        this.distributedSearch = distributedSearch;
    }
    @XmlElement(name="ResponseHandler",namespace=Namespaces.CSW)
    public String getResponseHandler() {
        return responseHandler;
    }

    public void setResponseHandler(String responseHandler) {
        this.responseHandler = responseHandler;
    }
    @XmlElements({@XmlElement(name = "AbstractQuery",type = AbstractQuery.class)})
    public Object getQuery() {
        return query;
    }

    public void setQuery(Object query) {
        this.query = query;
    }
    @XmlAttribute
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    @XmlAttribute
    public BasicRetrievalOptions getBasicRetrievalOptions() {
        return basicRetrievalOptions;
    }

    public void setBasicRetrievalOptions(BasicRetrievalOptions basicRetrievalOptions) {
        this.basicRetrievalOptions = basicRetrievalOptions;
    }
    
}
