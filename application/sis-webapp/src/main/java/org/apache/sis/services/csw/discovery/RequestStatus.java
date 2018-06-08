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

import org.apache.sis.services.csw.discovery.RequestStatusType;
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 *
 * @author haonguyen
 */
@XmlType(name = "RequestStatusType", namespace = Namespaces.CSW)
@XmlRootElement(name = "RequestStatus", namespace = Namespaces.CSW)
public class RequestStatus {
    
    private Date timestamp;
//    @XmlEnum(String.class)
    
    private RequestStatusType value;

    /**
     *
     * @return
     */
    @XmlValue
    public RequestStatusType getValue() {
        return value;
    }

    /**
     *
     * @param value
     */
    public void setValue(RequestStatusType value) {
        this.value = value;
    }
    
//    public RequestStatusType status;
//    @XmlValue
//    public RequestStatusType getStatus() {
//        return status;
//    }
//    
//    public void setStatus(RequestStatusType status) {
//        this.status = status;
//    }

    /**
     *
     * @return
     */
    @XmlAttribute(name="timestamp")
    
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     *
     * @param timestamp
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
}
