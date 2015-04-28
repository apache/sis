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

import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link MarshallerPool}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(OGCNamespacePrefixMapperTest.class)
public final strictfp class MarshallerPoolTest extends TestCase {
    /**
     * Tests a marshaller which is acquired, then released.
     * The marshaller should be reset to its initial state
     * despite the setter method we may have invoked on it.
     *
     * @throws JAXBException Should not happen.
     */
    @Test
    public void testAcquireRelease() throws JAXBException {
        final MarshallerPool pool = new MarshallerPool(JAXBContext.newInstance(new Class<?>[0]), null);
        final Marshaller marshaller = pool.acquireMarshaller();
        assertNotNull(marshaller);
        /*
         * PooledMarshaller should convert the property name from "com.sun.xml.bind.xmlHeaders" to
         * "com.sun.xml.internal.bind.xmlHeaders" if we are running JDK implementation of JAXB.
         */
        assertNull(marshaller.getProperty("com.sun.xml.bind.xmlHeaders"));
        marshaller.setProperty("com.sun.xml.bind.xmlHeaders", "<DTD ...>");
        assertEquals("<DTD ...>", marshaller.getProperty("com.sun.xml.bind.xmlHeaders"));
        /*
         * MarshallerPool should reset the properties to their initial state.
         */
        pool.recycle(marshaller);
        assertSame(marshaller, pool.acquireMarshaller());
        /*
         * Following should be null, but has been replaced by "" under the hood
         * for avoiding a NullPointerException in current JAXB implementation.
         */
        assertEquals("", marshaller.getProperty("com.sun.xml.bind.xmlHeaders"));
        pool.recycle(marshaller);
    }
}
