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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author haonguyen
 */
@XmlType(name = "FederatedExceptionType", namespace = Namespaces.CSW, propOrder = {    
    "exceptionReport",
})
@XmlRootElement(name = "FederatedException", namespace = Namespaces.CSW)
public class FederatedException extends FederatedSearchResultBase {
    private List<String> exceptionReport;

    /**
     *
     * @return
     */
    @XmlElement(name = "ExceptionReport",namespace=Namespaces.OWS)
    public List<String> getExceptionReport() {
        return exceptionReport;
    }

    /**
     *
     * @param exceptionReport
     */
    public void setExceptionReport(List<String> exceptionReport) {
        this.exceptionReport = exceptionReport;
    }
    
}
