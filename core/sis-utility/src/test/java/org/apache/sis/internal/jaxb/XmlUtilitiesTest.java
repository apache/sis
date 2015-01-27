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
package org.apache.sis.internal.jaxb;

import java.util.Date;
import java.util.Locale;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED;


/**
 * Test {@link XmlUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class XmlUtilitiesTest extends XMLTestCase {
    /**
     * Tests the {@link XmlUtilities#toXML} method.
     * This test arbitrarily uses the CET timezone.
     *
     * @throws DatatypeConfigurationException Should never happen.
     */
    @Test
    public void testToXML() throws DatatypeConfigurationException {
        createContext(false, Locale.FRANCE, "CET");
        final XMLGregorianCalendar calendar = XmlUtilities.toXML(context, new Date(1230786000000L));
        assertEquals("2009-01-01T06:00:00.000+01:00", calendar.toString());

        calendar.setMillisecond(FIELD_UNDEFINED);
        assertEquals("2009-01-01T06:00:00+01:00", calendar.toString());
    }
}
