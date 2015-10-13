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

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import javax.xml.bind.JAXBException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.test.Validators;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


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
    SingleOperationMarshallingTest.class
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
        assertEquals("operations.size()", 2, op.getOperations().size());
        final CoordinateOperation step1 = op.getOperations().get(0);
        final CoordinateOperation step2 = op.getOperations().get(1);
        final CoordinateReferenceSystem sourceCRS = op.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = op.getTargetCRS();

        assertIdentifierEquals(          "identifier", "test", "test", null, "concatenated", getSingleton(op       .getIdentifiers()));
        assertIdentifierEquals("sourceCRS.identifier", "test", "test", null, "source",       getSingleton(sourceCRS.getIdentifiers()));
        assertIdentifierEquals("targetCRS.identifier", "test", "test", null, "target",       getSingleton(targetCRS.getIdentifiers()));
        assertIdentifierEquals(    "step1.identifier", "test", "test", null, "step-1",       getSingleton(step1    .getIdentifiers()));
        assertIdentifierEquals(    "step2.identifier", "test", "test", null, "step-2",       getSingleton(step2    .getIdentifiers()));
        assertSame("tmp CRS",   step1.getTargetCRS(), step2.getSourceCRS());
        assertSame("sourceCRS", step1.getSourceCRS(), sourceCRS);
        assertSame("targetCRS", step2.getTargetCRS(), targetCRS);
        /*
         * Test marshalling and compare with the original file.
         */
        assertMarshalEqualsFile(XML_FILE, op, "xmlns:*", "xsi:schemaLocation");
    }
}
