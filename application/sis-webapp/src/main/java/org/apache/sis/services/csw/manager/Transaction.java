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
package org.apache.sis.services.csw.manager;

import javax.xml.bind.annotation.*;
import org.apache.sis.services.csw.common.RequestBase;

/**
 *
 * @author haonguyen
 */
@XmlType(name="TransactionType",namespace=Namespaces.CSW)
@XmlRootElement(name="Transaction",namespace=Namespaces.CSW)
public class Transaction extends RequestBase{
    private Object choice;
    private boolean verboseResponse = false;
    private String requestId;
    @XmlElements({
        @XmlElement(name="Insert",type=Insert.class),
        @XmlElement(name="Update",type=Update.class),
        @XmlElement(name="Delete",type=Delete.class)
    
    })
    public Object getChoice() {
        return choice;
    }

    public void setChoice(Object choice) {
        this.choice = choice;
    }
    @XmlAttribute
    public boolean isVerboseResponse() {
        return verboseResponse;
    }

    public void setVerboseResponse(boolean verboseResponse) {
        this.verboseResponse = verboseResponse;
    }
    @XmlAttribute
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
}
