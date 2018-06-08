package org.apache.sis.services.csw.manager;
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

import org.apache.sis.services.csw.discovery.BriefRecord;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author haonguyen
 */
@XmlType(name = "InsertResultType", namespace = Namespaces.CSW, propOrder = {    
    "briefRecord",
})
@XmlRootElement(name = "InsertResult")
public class InsertResult {
    private BriefRecord briefRecord;
    private String handleRef;
    
    @XmlElement(name = "BriefRecord")
    public BriefRecord getBriefRecord() {
        return briefRecord;
    }

    public void setBriefRecord(BriefRecord briefRecord) {
        this.briefRecord = briefRecord;
    }
    @XmlAttribute
    public String getHandleRef() {
        return handleRef;
    }

    public void setHandleRef(String handleRef) {
        this.handleRef = handleRef;
    }
    
}
