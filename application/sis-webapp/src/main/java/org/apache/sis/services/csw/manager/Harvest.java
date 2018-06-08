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
import javax.xml.datatype.Duration;
import org.apache.sis.services.csw.common.RequestBase;

/**
 *
 * @author haonguyen
 */
@XmlType(name="HarvestType",namespace = Namespaces.CSW)
@XmlRootElement(name="Harvest",namespace = Namespaces.CSW)
public class Harvest extends RequestBase{
    private String source;
    private String resourceType;
    private String resourceFormat = "application/xml";
    private Duration duration ;
    private String responseHandler;
    @XmlElement(name="Source")
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
    @XmlElement(name="ResourceType")
    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    @XmlElement(name="ResourceFormat")
    public String getResourceFormat() {
        return resourceFormat;
    }

    public void setResourceFormat(String resourceFormat) {
        this.resourceFormat = resourceFormat;
    }
    @XmlElement(name="duration")
    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }
    @XmlElement(name="ResponseHandler")
    public String getResponseHandler() {
        return responseHandler;
    }

    public void setResponseHandler(String responseHandler) {
        this.responseHandler = responseHandler;
    }
    
}
