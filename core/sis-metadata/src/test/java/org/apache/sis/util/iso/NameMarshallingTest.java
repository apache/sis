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

import java.util.Collections;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.mock.IdentifiedObjectMock;
import org.apache.sis.test.xml.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the XML marshalling of generic names.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.3
 * @module
 */
@DependsOn(DefaultNameFactoryTest.class)
public final strictfp class NameMarshallingTest extends TestCase {
    /**
     * A poll of configured {@link Marshaller} and {@link Unmarshaller}, created when first needed.
     *
     * @see #disposeMarshallerPool()
     */
    private static MarshallerPool pool;

    /**
     * Returns the XML representation of the given name, wrapped
     * in a mock {@code <gml:IO_IdentifiedObject>} element.
     */
    private String marshal(final GenericName name) throws JAXBException {
        if (pool == null) {
            pool = new MarshallerPool(JAXBContext.newInstance(IdentifiedObjectMock.class),
                    Collections.singletonMap(XML.LENIENT_UNMARSHAL, Boolean.TRUE));
        }
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.METADATA_VERSION, VERSION_2007);
        final String xml = marshal(marshaller, new IdentifiedObjectMock(null, name));
        pool.recycle(marshaller);
        return xml;
    }

    /**
     * Converse of {@link #marshal(GenericName)}.
     */
    private GenericName unmarshal(final String xml) throws JAXBException {
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final Object value = unmarshal(unmarshaller, xml);
        pool.recycle(unmarshaller);
        return ((IdentifiedObjectMock) value).alias;
    }

    /**
     * Tests XML of a {@link LocalName}.
     *
     * @throws JAXBException if (un)marshalling failed.
     */
    @Test
    public void testLocalName() throws JAXBException {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final LocalName name = factory.createLocalName(null, "An ordinary local name");
        assertEquals("An ordinary local name", name.toString());
        final String expected =
                "<gml:IO_IdentifiedObject xmlns:gml=\"" + Namespaces.GML + '"' +
                                        " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
                "  <gml:alias>\n" +
                "    <gco:LocalName>An ordinary local name</gco:LocalName>\n" +
                "  </gml:alias>\n" +
                "</gml:IO_IdentifiedObject>\n";
        final String actual = marshal(name);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(name, unmarshal(expected));
    }

    /**
     * Tests XML of a {@link LocalName} with {@code &} symbol.
     *
     * @throws JAXBException if (un)marshalling failed.
     */
    @Test
    @DependsOnMethod("testLocalName")
    public void testLocalNameWithAmp() throws JAXBException {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final LocalName name = factory.createLocalName(null, "A name with & and > and <.");
        assertEquals("A name with & and > and <.", name.toString());
        final String expected =
                "<gml:IO_IdentifiedObject xmlns:gml=\"" + Namespaces.GML + '"' +
                                        " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
                "  <gml:alias>\n" +
                "    <gco:LocalName>A name with &amp; and &gt; and &lt;.</gco:LocalName>\n" +
                "  </gml:alias>\n" +
                "</gml:IO_IdentifiedObject>\n";
        final String actual = marshal(name);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(name, unmarshal(expected));
    }

    /**
     * Tests XML of a {@link LocalName} with a scope.
     *
     * @throws JAXBException if (un)marshalling failed.
     */
    @Test
    @DependsOnMethod("testLocalName")
    public void testLocalNameWithScope() throws JAXBException {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final NameSpace scope = factory.createNameSpace(factory.createLocalName(null, "A code space"), null);
        final LocalName name = factory.createLocalName(scope, "A name in a scope");
        assertEquals("A name in a scope", name.toString());
        final String expected =
                "<gml:IO_IdentifiedObject xmlns:gml=\"" + Namespaces.GML + '"' +
                                        " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
                "  <gml:alias>\n" +
                "    <gco:LocalName codeSpace=\"A code space\">A name in a scope</gco:LocalName>\n" +
                "  </gml:alias>\n" +
                "</gml:IO_IdentifiedObject>\n";
        final String actual = marshal(name);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(name, unmarshal(expected));
    }

    /**
     * Tests XML of a {@link TypeName}.
     *
     * @throws JAXBException if (un)marshalling failed.
     */
    @Test
    public void testTypeName() throws JAXBException {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final TypeName name = factory.createTypeName(null, "An other local name");
        assertEquals("An other local name", name.toString());
        final String expected =
                "<gml:IO_IdentifiedObject xmlns:gml=\"" + Namespaces.GML + '"' +
                                        " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
                "  <gml:alias>\n" +
                "    <gco:TypeName>\n" +
                "      <gco:aName>\n" +
                "        <gco:CharacterString>An other local name</gco:CharacterString>\n" +
                "      </gco:aName>\n" +
                "    </gco:TypeName>\n" +
                "  </gml:alias>\n" +
                "</gml:IO_IdentifiedObject>\n";
        final String actual = marshal(name);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(name, unmarshal(expected));
    }

    /**
     * Tests XML of a {@link org.opengis.util.ScopedName}.
     *
     * @throws JAXBException if (un)marshalling failed.
     */
    @Test
    public void testScopedName() throws JAXBException {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final GenericName name = factory.createGenericName(null, "myScope","myName");
        assertEquals("myScope:myName", name.toString());
        final String expected =
                "<gml:IO_IdentifiedObject xmlns:gml=\"" + Namespaces.GML + '"' +
                                        " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
                "  <gml:alias>\n" +
                "    <gco:ScopedName>myScope:myName</gco:ScopedName>\n" +
                "  </gml:alias>\n" +
                "</gml:IO_IdentifiedObject>\n";
        final String actual = marshal(name);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(name, unmarshal(expected));
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
