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
package org.apache.sis.style.se1;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.test.TestCase;
import org.junit.Test;


/**
 * Test of XML marshalling.
 * The current version only verifies that {@link JAXBContext} can be created.
 * We do not yet have sufficient JAXB annotations and adapters for real marshalling.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class XmlTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public XmlTest() {
    }

    /**
     * Tests the creation of a JAXB context.
     *
     * @throws JAXBException if some invalid annotations were found.
     */
    @Test
    public void testContext() throws JAXBException {
        JAXBContext.newInstance(Symbolizer.class);
    }
}
