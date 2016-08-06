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
package org.apache.sis.services.csw.request;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.sis.services.csw.CswConfigure;
import static org.apache.sis.services.csw.CswConfigure.OWS;

/**
 *
 * @author Thi Phuong Hao Nguyen (VNSC)
 * @since 0.8
 * @version 0.8
 * @module
 */
@XmlRootElement(namespace = OWS)
public class Version extends CswConfigure {

    /**
     * Version for service.
     */
    private String[] Version;

    /**
     * Format that service reponse.
     */
    private String[] OutputFormat;

    /**
     * Return version for service.
     *
     * @return version for service.
     */
    @XmlElement(namespace = OWS, name = "Version")
    public String[] getVersion() {
        return Version;
    }

    /**
     * Set version for service.
     *
     * @param Version version for service
     */
    public void setVersion(String[] Version) {
        this.Version = Version;
    }

    /**
     * Return format that service reponse.
     *
     * @return format that service reponse
     */
    @XmlElement(namespace = OWS, name = "OutputFormat")
    public String[] getOutputFormat() {
        return OutputFormat;
    }

    /**
     * Set format that service reponse.
     *
     * @param OutputFormat format that service reponse
     */
    public void setOutputFormat(String[] OutputFormat) {
        this.OutputFormat = OutputFormat;
    }
}
