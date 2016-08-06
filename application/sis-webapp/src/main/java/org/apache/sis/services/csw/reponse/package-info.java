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

/**
 * Partial implementation of OGC Catalog Services on the Web (CSW).
 * This package implements a framework for publishing and accessing digital catalogues
 * of metadata for geospatial data, services, and related resource information.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @since   0.8
 * @version 0.8
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.CSW, xmlns = {
    @XmlNs(prefix = "csw", namespaceURI = Namespaces.CSW),
    @XmlNs(prefix = "ows", namespaceURI = CswConfigure.OWS),
    @XmlNs(prefix = "dc",  namespaceURI = CswConfigure.DUBLIN_CORE),
    @XmlNs(prefix = "dct", namespaceURI =CswConfigure.DUBLIN_TERMS)
})
@XmlAccessorType(XmlAccessType.FIELD)
package org.apache.sis.services.csw.reponse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import org.apache.sis.services.csw.CswConfigure;
import org.apache.sis.xml.Namespaces;
