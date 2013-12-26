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
package org.apache.sis.referencing.datum;

import java.net.URL;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.measure.unit.NonSI;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.test.XMLTestCase;

import static org.apache.sis.test.Assert.*;


/**
 * Base class of test in the datum package that need XML (un)marshalling.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
abstract strictfp class DatumTestCase extends XMLTestCase {
    /**
     * Creates a new test case.
     */
    DatumTestCase() {
    }

    /**
     * Asserts the the given prime meridian is the Greenwich one.
     */
    static void assertIsGreenwichMeridian(final PrimeMeridian meridian) {
        assertEquals("name", "Greenwich", meridian.getName().getCode());
        assertEquals("greenwichLongitude", 0, meridian.getGreenwichLongitude(), 0);
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, meridian.getAngularUnit());
    }

    /**
     * Asserts the the given prime meridian is the Paris one.
     */
    static void assertIsParisMeridian(final PrimeMeridian meridian) {
        assertEquals("name", "Paris", meridian.getName().getCode());
        assertEquals("greenwichLongitude", 2.5969213, meridian.getGreenwichLongitude(), 0);
        assertEquals("angularUnit", NonSI.GRADE, meridian.getAngularUnit());
    }

    /**
     * Marshals the given object and ensure that the result is equals to the content of the given file,
     * ignoring namespace declarations.
     *
     * @param  filename The name of the XML file.
     * @param  object The object to marshal.
     * @throws JAXBException If an error occurred during marshalling.
     */
    final void assertMarshalEqualsFile(final String filename, final Object object) throws JAXBException {
        assertXmlEquals(getResource(filename), marshal(object), "xlmns:*", "xsi:schemaLocation",
                "xmlns:xsi"); // Not necessary on JDK7 but needed on JDK6, not sure why...
    }

    /**
     * Unmarshalls the given test file.
     *
     * @param  type The expected object type.
     * @param  filename The name of the XML file.
     * @return The object unmarshalled from the given file.
     * @throws JAXBException If an error occurred during unmarshalling.
     */
    static <T> T unmarshall(final Class<T> type, final String filename) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final Object object = unmarshaller.unmarshal(getResource(filename));
        pool.recycle(unmarshaller);
        assertInstanceOf(filename, type, object);
        return type.cast(object);
    }

    /**
     * Returns the URL to the XML file of the given name.
     *
     * @param  filename The name of the XML file.
     * @return The URL to the given XML file.
     */
    private static URL getResource(final String filename) {
        final URL resource = DatumTestCase.class.getResource(filename);
        assertNotNull(filename, resource);
        return resource;
    }
}
