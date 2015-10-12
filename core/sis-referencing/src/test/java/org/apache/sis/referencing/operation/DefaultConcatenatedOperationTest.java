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
package org.apache.sis.referencing.operation;

import org.opengis.test.Validators;
import javax.xml.bind.JAXBException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests the {@link DefaultConcatenatedOperation} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    DefaultTransformationTest.class,
    OperationMarshallingTest.class
})
public class DefaultConcatenatedOperationTest extends XMLTestCase {
    /**
     * An XML file in this package containing a projected CRS definition.
     */
    private static final String XML_FILE = "ConcatenatedOperation.xml";


    /**
     * Tests (un)marshalling of a concatenated operation.
     *
     * @throws JAXBException If an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultConcatenatedOperation op = unmarshalFile(DefaultConcatenatedOperation.class, XML_FILE);
        Validators.validate(op);
        if (false) assertEpsgNameAndIdentifierEqual("WGS 84", 4979, op);
        /*
         * Test marshalling and compare with the original file.
         */
//        assertMarshalEqualsFile(XML_FILE, op, "xmlns:*", "xsi:schemaLocation");
    }
}
