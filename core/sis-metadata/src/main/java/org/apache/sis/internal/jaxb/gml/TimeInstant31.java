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
package org.apache.sis.internal.jaxb.gml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.XMLGregorianCalendar;
import org.opengis.temporal.Instant;
import org.apache.sis.internal.jaxb.LegacyNamespaces;


/**
 * A copy of {@link TimeInstant} using GML 3.1 namespace instead than GML 3.2.
 * This class will be deleted in a future SIS version if we find a better way
 * to support evolution of GML schemas.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@XmlRootElement(name="TimeInstant", namespace = LegacyNamespaces.GML)
public final class TimeInstant31 extends GMLAdapter {
    /**
     * Same as {@link TimeInstant#timePosition}, but using GML 3.1 namespace.
     */
    @XmlElement(namespace = LegacyNamespaces.GML)
    public XMLGregorianCalendar timePosition;

    /**
     * Same as {@link TimeInstant#TimeInstant()} but for GML 3.1 namespace.
     */
    public TimeInstant31() {
    }

    /**
     * Same as {@link TimeInstant#TimeInstant(Instant)} but for GML 3.1 namespace.
     *
     * @param instant The initial instant value.
     */
    public TimeInstant31(final Instant instant) {
        timePosition = TimeInstant.toXML(instant);
    }
}
