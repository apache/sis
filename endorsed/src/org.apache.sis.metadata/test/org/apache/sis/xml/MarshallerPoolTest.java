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
package org.apache.sis.xml;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link MarshallerPool}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class MarshallerPoolTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MarshallerPoolTest() {
    }

    /**
     * Tests a marshaller which is acquired, then released.
     * The marshaller should be reset to its initial state
     * despite the setter method we may have invoked on it.
     *
     * @throws JAXBException if (un)marhaller construction failed.
     */
    @Test
    public void testAcquireRelease() throws JAXBException {
        final MarshallerPool pool = new MarshallerPool(JAXBContext.newInstance(new Class<?>[0]), null);
        final Marshaller marshaller = pool.acquireMarshaller();
        assertNotNull(marshaller);
        assertNull(marshaller.getProperty("org.glassfish.jaxb.xmlHeaders"));
        marshaller.setProperty("org.glassfish.jaxb.xmlHeaders", "<DTD ...>");
        assertEquals("<DTD ...>", marshaller.getProperty("org.glassfish.jaxb.xmlHeaders"));
        /*
         * MarshallerPool should reset the properties to their initial state.
         */
        pool.recycle(marshaller);
        assertSame(marshaller, pool.acquireMarshaller());
        /*
         * Following should be null, but has been replaced by "" under the hood
         * for avoiding a NullPointerException in current JAXB implementation.
         */
        assertEquals("", marshaller.getProperty("org.glassfish.jaxb.xmlHeaders"));
        pool.recycle(marshaller);
    }
}
