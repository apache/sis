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
package org.apache.sis.services.csw;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @since   0.8
 * @version 0.8
 * @module
 */
@XmlRootElement(name = "GetCapabilities")
public class GetCapabilitie extends Element {
    /**
     * Accept version use to in service.
     */
    private Capacibilities acceptversion;

    /**
     * Accept format use to in service.
     */
    private Capacibilities acceptformat;
    
    /**
     * Contructor's GetCapabilitie.
     */

    public GetCapabilitie() {

    }

    /**
     * Creates get capabilities
     *
     * @param acceptversion Accept version use to in service
     * @param acceptformat Accept format use to in service.
     */
    public GetCapabilitie(Capacibilities acceptversion, Capacibilities acceptformat) {
        this.acceptversion = acceptversion;
        this.acceptformat = acceptformat;
    }

    /**
     * Get Accept version
     *
     * @return Accept version use to in service.
     */
    @XmlElement(name = "AcceptVersion", namespace = OWS)
    public Capacibilities getAcceptversion() {
        return acceptversion;
    }

    /**
     * Set Accept version
     *
     * @param acceptversion Accept version use to in service
     */
    public void setAcceptversion(Capacibilities acceptversion) {
        this.acceptversion = acceptversion;
    }

    /**
     * Get Accept format
     *
     * @return Accept format
     */
    @XmlElement(name = "AcceptFormat", namespace = OWS)
    public Capacibilities getAcceptformat() {
        return acceptformat;
    }

    /**
     * Set Acceptformat
     *
     * @param acceptformat Accept format use to in service
     */
    public void setAcceptformat(Capacibilities acceptformat) {
        this.acceptformat = acceptformat;
    }
}
