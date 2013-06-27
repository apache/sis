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
package org.apache.sis.util.iso;

import java.io.StringWriter;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.test.mock.IdentifiedObjectMock;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the XML marshalling of generic names.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@DependsOn(DefaultNameFactoryTest.class)
public final strictfp class NameMarshallingTest extends XMLTestCase {
    /**
     * The JAXB context, created when first needed.
     */
    private static MarshallerPool pool;

    /**
     * Returns the XML representation of the given name, wrapped
     * in a mock {@code <gml:IO_IdentifiedObject>} element.
     */
    private static String marshall(final GenericName name) throws JAXBException {
        if (pool == null) {
            pool = new MarshallerPool(JAXBContext.newInstance(IdentifiedObjectMock.class), null);
        }
        final Marshaller marshaller = pool.acquireMarshaller();
        final StringWriter out = new StringWriter();
        marshaller.marshal(new IdentifiedObjectMock(name), out);
        pool.recycle(marshaller);
        return out.toString();
    }

    /**
     * Tests XML of a {@link LocalName}.
     *
     * @throws JAXBException Should not happen.
     */
    @Test
    public void testLocalName() throws JAXBException {
        final NameFactory factory = DefaultFactories.NAMES;
        final LocalName name = factory.createLocalName(null, "An ordinary local name");
        assertEquals("An ordinary local name", name.toString());
        final String expected =
                "<gml:IO_IdentifiedObject>\n" +
                "  <gml:alias>\n" +
                "    <gco:LocalName>An ordinary local name</gco:LocalName>\n" +
                "  </gml:alias>\n" +
                "</gml:IO_IdentifiedObject>\n";
        final String actual = marshall(name);
        assertXmlEquals(expected, actual, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests XML of a {@link LocalName} with {@code &} symbol.
     *
     * @throws JAXBException Should not happen.
     */
    @Test
    public void testLocalNameWithAmp() throws JAXBException {
        final NameFactory factory = DefaultFactories.NAMES;
        final LocalName name = factory.createLocalName(null, "A name with & and > and <.");
        assertEquals("A name with & and > and <.", name.toString());
        final String expected =
                "<gml:IO_IdentifiedObject>\n" +
                "  <gml:alias>\n" +
                "    <gco:LocalName>A name with &amp; and &gt; and &lt;.</gco:LocalName>\n" +
                "  </gml:alias>\n" +
                "</gml:IO_IdentifiedObject>\n";
        final String actual = marshall(name);
        assertXmlEquals(expected, actual, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests XML of a {@link TypeName}.
     *
     * @throws JAXBException Should not happen.
     */
    @Test
    public void testTypeName() throws JAXBException {
        final NameFactory factory = DefaultFactories.NAMES;
        final TypeName name = factory.createTypeName(null, "An other local name");
        assertEquals("An other local name", name.toString());
        final String expected =
                "<gml:IO_IdentifiedObject>\n" +
                "  <gml:alias>\n" +
                "    <gco:TypeName>\n" +
                "      <gco:aName>\n" +
                "        <gco:CharacterString>An other local name</gco:CharacterString>\n" +
                "      </gco:aName>\n" +
                "    </gco:TypeName>\n" +
                "  </gml:alias>\n" +
                "</gml:IO_IdentifiedObject>\n";
        final String actual = marshall(name);
        assertXmlEquals(expected, actual, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests XML of a {@link org.opengis.util.ScopedName}.
     *
     * @throws JAXBException Should not happen.
     */
    @Test
    public void testScopedName() throws JAXBException {
        final NameFactory factory = DefaultFactories.NAMES;
        final GenericName name = factory.createGenericName(null, "myScope","myName");
        assertEquals("myScope:myName", name.toString());
        final String expected =
                "<gml:IO_IdentifiedObject>\n" +
                "  <gml:alias>\n" +
                "    <gco:ScopedName>myScope:myName</gco:ScopedName>\n" +
                "  </gml:alias>\n" +
                "</gml:IO_IdentifiedObject>\n";
        final String actual = marshall(name);
        assertXmlEquals(expected, actual, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Invoked by JUnit after the execution of every tests in order to dispose
     * the {@link MarshallerPool} instance used internally by this class.
     */
    @AfterClass
    public static void disposeMarshallerPool() {
        pool = null;
    }
}
