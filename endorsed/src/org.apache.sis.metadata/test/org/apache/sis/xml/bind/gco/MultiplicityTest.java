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
package org.apache.sis.xml.bind.gco;

import java.util.Map;
import java.io.InputStream;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.util.iso.Names;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.test.mock.FeatureAttributeMock;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.XML;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link MultiplicityRange}
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.0
 */
public final class MultiplicityTest extends TestUsingFile {
    /**
     * Opens the stream to the XML file containing multiplicity declarations.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile(final Format format) {
        return format.openTestFile("Multiplicity.xml");
    }

    /**
     * A poll of configured {@code Marshaller} and {@code Unmarshaller}.
     */
    private static MarshallerPool pool;

    /**
     * Creates a new test case.
     */
    public MultiplicityTest() {
    }

    /**
     * Returns the XML (un)marshaller pool to be shared by all test methods.
     * This test uses its own pool instead of {@link #getMarshallerPool()}
     * in order to use {@link FeatureAttributeMock}.
     *
     * @return the marshaller pool for this test.
     * @throws JAXBException if an error occurred while creating the pool.
     */
    @Override
    protected MarshallerPool getMarshallerPool() throws JAXBException {
        if (pool == null) {
            pool = new MarshallerPool(JAXBContext.newInstance(FeatureAttributeMock.class),
                                      Map.of(XML.LENIENT_UNMARSHAL, Boolean.TRUE));
        }
        return pool;
    }

    /**
     * Invoked by JUnit after the execution of every tests in order to dispose
     * the {@link MarshallerPool} instance used internally by this class.
     */
    @AfterClass
    public static void disposeMarshallerPool() {
        pool = null;
    }

    /**
     * Creates a multiplicity instance with the same content than in {@value #FILENAME} file.
     *
     * @param  requireInclusive  0 if we are allowed to create range with exclusive bounds, or
     *                           1 if all bounds shall be inclusive.
     */
    private FeatureAttributeMock create(final int requireInclusive) {
        final FeatureAttributeMock f = new FeatureAttributeMock();
        f.memberName = Names.createLocalName(null, null, "Multiplicity test");
        f.cardinality = new Multiplicity(
                new NumberRange<>(Integer.class, 0, true, 233333 - requireInclusive, requireInclusive != 0),
                new NumberRange<>(Integer.class, requireInclusive, requireInclusive != 0, null, false));
        return f;
    }

    /**
     * Tests marshalling of a few multiplicity using the specified version of metadata schema.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     */
    private void marshalAndCompare(final Format format) throws JAXBException {
        FeatureAttributeMock metadata = create(0);
        assertMarshalEqualsFile(openTestFile(format), metadata, format.schemaVersion, "xmlns:*", "xsi:*");
    }

    /**
     * Tests marshalling to legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testMarshallingLegacy() throws JAXBException {
        marshalAndCompare(Format.XML2007);
    }

    /**
     * Tests unmarshalling from legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testUnmarshallingLegacy() throws JAXBException {
        final FeatureAttributeMock metadata = unmarshalFile(FeatureAttributeMock.class, openTestFile(Format.XML2007));
        assertEquals(create(1).cardinality.range(), metadata.cardinality.range());
    }
}
