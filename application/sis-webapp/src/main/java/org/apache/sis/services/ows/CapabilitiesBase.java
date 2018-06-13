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
package org.apache.sis.services.ows;

import java.util.List;
import javax.xml.bind.annotation.*;
import org.apache.sis.services.csw.common.Namespaces;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.identification.OperationMetadata;
import org.opengis.metadata.identification.ServiceIdentification;

/**
 *
 * @author haonguyen
 */
@XmlType(name="CapabilitiesBaseType", namespace=Namespaces.OWS)
@XmlRootElement(name="CapabilitiesBase",namespace=Namespaces.OWS)
public class CapabilitiesBase {
    private ServiceIdentification serviceIdentification;
    private ServiceIdentification serviceProvider;
    private List<OperationMetadata> operationMetadata;
    private List<Language> languages; 
    private List<Metadata> content;
    @XmlElement(name="ServiceIdentification")
    public ServiceIdentification getServiceIdentification() {
        return serviceIdentification;
    }

    public void setServiceIdentification(ServiceIdentification serviceIdentification) {
        this.serviceIdentification = serviceIdentification;
    }
    @XmlElement(name="ServiceProvider")
    public ServiceIdentification getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(ServiceIdentification serviceProvider) {
        this.serviceProvider = serviceProvider;
    }
    @XmlElement(name="OperationMetadata")
    public List<OperationMetadata> getOperationMetadata() {
        return operationMetadata;
    }

    public void setOperationMetadata(List<OperationMetadata> operationMetadata) {
        this.operationMetadata = operationMetadata;
    }
    @XmlElement(name="Languages")
    public List<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(List<Language> languages) {
        this.languages = languages;
    }
    @XmlElement(name="Content")
    public List<Metadata> getContent() {
        return content;
    }

    public void setContent(List<Metadata> content) {
        this.content = content;
    }
    
}
