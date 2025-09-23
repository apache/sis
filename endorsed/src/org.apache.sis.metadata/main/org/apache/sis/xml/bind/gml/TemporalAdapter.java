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
package org.apache.sis.xml.bind.gml;

import java.time.temporal.Temporal;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.xml.internal.shared.XmlUtilities;
import org.apache.sis.xml.bind.Context;


/**
 * JAXB adapter wrapping a temporal value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TemporalAdapter extends XmlAdapter<XMLGregorianCalendar, Temporal> {
    /**
     * Empty constructor for JAXB only.
     */
    public TemporalAdapter() {
    }

    /**
     * Converts a date read from a XML stream to the object which will contain the value.
     * JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value  the XML date, or {@code null}.
     * @return the temporal object, or {@code null}.
     */
    @Override
    public Temporal unmarshal(final XMLGregorianCalendar value) {
        return XmlUtilities.toTemporal(Context.current(), value);
    }

    /**
     * Converts the date to the object to be marshalled in a XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value  the temporal object, or {@code null}.
     * @return the XML date, or {@code null}.
     */
    @Override
    public XMLGregorianCalendar marshal(final Temporal value) {
        if (value != null) try {
            return XmlUtilities.toXML(Context.current(), value);
        } catch (DatatypeConfigurationException e) {
            Context.warningOccured(Context.current(), TemporalAdapter.class, "marshal", e, true);
        }
        return null;
    }
}
