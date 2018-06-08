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
@XmlSchema(
    elementFormDefault=XmlNsForm.QUALIFIED,
    namespace="http://www.opengis.net/cat/csw/3.0",
    xmlns={
        @XmlNs(prefix="csw30",namespaceURI=Namespaces.CSW),
        @XmlNs(prefix="dc", namespaceURI=Namespaces.DC),
        @XmlNs(prefix="dct",namespaceURI=Namespaces.DCT),
        @XmlNs(prefix="ows20",namespaceURI=Namespaces.OWS),
        @XmlNs(prefix="fes",namespaceURI=Namespaces.FES),
        @XmlNs(prefix="gml",namespaceURI=Namespaces.GML),
    }
)
package org.apache.sis.services.csw.manager;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;