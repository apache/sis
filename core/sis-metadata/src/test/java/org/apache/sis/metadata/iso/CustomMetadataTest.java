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
package org.apache.sis.metadata.iso;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Locale;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import javax.xml.bind.JAXBException;
import org.opengis.util.NameFactory;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.util.InternationalString;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.xml.TestCase;
import org.apache.sis.xml.XML;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.junit.Assert.*;


/**
 * Tests XML marshalling of custom implementation of metadata interfaces. The custom implementations
 * need to be converted to implementations from the {@link org.apache.sis.metadata.iso} package by
 * the JAXB converters.
 *
 * @author  Damiano Albani (for code snippet on the mailing list)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.3
 * @module
 */
public final strictfp class CustomMetadataTest extends TestCase {
    /**
     * Tests the marshalling of a metadata implemented by {@link Proxy}.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testProxy() throws JAXBException {
        /*
         * A trivial metadata implementation which return the method name
         * for every attribute of type InternationalString.
         */
        final InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if (method.getReturnType() == InternationalString.class) {
                return new SimpleInternationalString(method.getName());
            }
            return null;
        };
        Citation data = (Citation) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { Citation.class }, handler);
        /*
         * Wrap the metadata in a DefaultMetadata, and ensure
         * we can marshal it without an exception being throw.
         */
        data = new DefaultCitation(data);
        final String xml = XML.marshal(data);
        /*
         * A few simple checks.
         */
        assertTrue(xml.contains("title"));
        assertTrue(xml.contains("edition"));
    }

    /**
     * Tests that the attributes defined in subtypes are also marshalled.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testSubtypeAttributes() throws JAXBException {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final DataIdentification identification = new DataIdentification() {
            @Override public InternationalString getAbstract() {
                Map<Locale, String> names = new HashMap<>();
                names.put(Locale.ENGLISH, "Description");
                return factory.createInternationalString(names);
            }

            @Override public InternationalString getEnvironmentDescription() {
                Map<Locale, String> names = new HashMap<>();
                names.put(Locale.ENGLISH, "Environment");
                return factory.createInternationalString(names);
            }

            @Override public Citation                  getCitation()           {return null;}
            @Override public Collection<TopicCategory> getTopicCategories()    {return null;}
            @Override public Collection<Extent>        getExtents()            {return null;}
        };
        final DefaultMetadata data = new DefaultMetadata();
        data.setIdentificationInfo(singleton(identification));
        final String xml = XML.marshal(data);
        /*
         * A few simple checks.
         */
        assertTrue("Missing Identification attribute.",     xml.contains("Description"));
        assertTrue("Missing DataIdentification attribute.", xml.contains("Environment"));
    }
}
