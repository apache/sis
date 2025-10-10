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

import java.util.List;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Transformation;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.opengis.test.Validators;
import org.apache.sis.xml.test.TestCase;
import static org.opengis.test.Assertions.assertIdentifierEquals;


/**
 * Tests the {@link DefaultPassThroughOperation} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultPassThroughOperationTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultPassThroughOperationTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing a projected CRS definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultPassThroughOperationTest.class.getResourceAsStream("PassThroughOperation.xml");
    }

    /**
     * Tests (un)marshalling of a concatenated operation.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultPassThroughOperation toTest = unmarshalFile(DefaultPassThroughOperation.class, openTestFile());
        Validators.validate(toTest);
        final CoordinateReferenceSystem sourceCRS = toTest.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = toTest.getTargetCRS();
        final CoordinateOperation       operation = toTest.getOperation();
        verifyIdentifier("passthrough", toTest,    "toTest");
        verifyIdentifier("source",      sourceCRS, "sourceCRS");
        verifyIdentifier("target",      targetCRS, "targetCRS");
        verifyIdentifier("rotation",    operation, "operation");
        assertInstanceOf(Transformation.class, operation);
        final List<CoordinateReferenceSystem> srcComponents = assertInstanceOf(CompoundCRS.class, sourceCRS).getComponents();
        final List<CoordinateReferenceSystem> tgtComponents = assertInstanceOf(CompoundCRS.class, targetCRS).getComponents();
        assertEquals(2, srcComponents.size());
        assertEquals(2, tgtComponents.size());
        assertSame  (operation.getSourceCRS(), srcComponents.get(0));
        assertSame  (operation.getTargetCRS(), tgtComponents.get(0));
        assertSame  (srcComponents.get(1),     tgtComponents.get(1));
        /*
         * Test marshalling and compare with the original file.
         */
        assertMarshalEqualsFile(openTestFile(), toTest, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Verifies an identifier in the "test" namespace.
     */
    private static void verifyIdentifier(final String code, final IdentifiedObject object, final String label) {
        assertIdentifierEquals("test", "test", null, code, assertSingleton(object.getIdentifiers()), label);
    }
}
