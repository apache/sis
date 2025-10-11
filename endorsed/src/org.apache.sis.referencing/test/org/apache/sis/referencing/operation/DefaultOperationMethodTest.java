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

import java.util.Map;
import java.util.HashMap;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertTitleEquals;
import static org.apache.sis.referencing.Assertions.assertEpsgNameAndIdentifierEqual;
import static org.apache.sis.referencing.Assertions.assertWktEquals;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Tests {@link DefaultOperationMethod}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultOperationMethodTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultOperationMethodTest() {
    }

    /**
     * Creates a new two-dimensional operation method for an operation of the given name and identifier.
     *
     * @param  method      the operation name (example: "Mercator (variant A)").
     * @param  identifier  the EPSG numeric identifier (example: "9804").
     * @param  formula     formula citation (example: "EPSG guidance note #7-2").
     * @param  parameters  the parameters (can be empty).
     * @return the operation method.
     */
    static DefaultOperationMethod create(final String method, final String identifier, final String formula,
                                         final ParameterDescriptor<?>... parameters)
    {
        final Map<String,Object> properties = new HashMap<>(8);
        assertNull(properties.put(OperationMethod.NAME_KEY, method));
        assertNull(properties.put(ReferenceIdentifier.CODESPACE_KEY, "EPSG"));
        assertNull(properties.put(Identifier.AUTHORITY_KEY, Citations.EPSG));
        /*
         * The parameter group for a Mercator projection is actually not empty, but it is not the purpose of
         * this class to test DefaultParameterDescriptorGroup. So we use an empty group of parameters here.
         */
        final var pg = new DefaultParameterDescriptorGroup(properties, 1, 1, parameters);
        /*
         * NAME_KEY share the same Identifier instance for saving a little bit of memory.
         * Then define the other properties to be given to OperationMethod.
         */
        assertNotNull(properties.put(OperationMethod.NAME_KEY, pg.getName()));
        assertNull(properties.put(OperationMethod.IDENTIFIERS_KEY, new ImmutableIdentifier(Citations.EPSG, "EPSG", identifier)));
        assertNull(properties.put(OperationMethod.FORMULA_KEY, new DefaultCitation(formula)));
        return new DefaultOperationMethod(properties, pg);
    }

    /**
     * Tests the {@link DefaultOperationMethod#DefaultOperationMethod(Map, ParameterDescriptorGroup)} constructor.
     */
    @Test
    public void testConstruction() {
        final OperationMethod method = create("Mercator (variant A)", "9804", "EPSG guidance note #7-2");
        assertEpsgNameAndIdentifierEqual("Mercator (variant A)", 9804, method);
        assertTitleEquals("EPSG guidance note #7-2", method.getFormula().getCitation(), "formula");
    }

    /**
     * Tests {@link DefaultOperationMethod#equals(Object, ComparisonMode)}.
     */
    @Test
    public void testEquals() {
        final DefaultOperationMethod m1 = create("Mercator (variant A)", "9804", "EPSG guidance note #7-2");
        final DefaultOperationMethod m2 = create("Mercator (variant A)", "9804", "E = FE + a*ko(lon - lonO)");
        assertFalse (m1.equals(m2, ComparisonMode.STRICT));
        assertFalse (m1.equals(m2, ComparisonMode.BY_CONTRACT));
        assertTrue  (m1.equals(m2, ComparisonMode.IGNORE_METADATA));
        assertEquals(m1.hashCode(), m2.hashCode(), "Hash code should ignore metadata.");

        final DefaultOperationMethod m3 = create("Mercator (variant B)", "9805", "EPSG guidance note #7-2");
        final DefaultOperationMethod m4 = create("mercator (variant b)", "9805", "EPSG guidance note #7-2");
        assertFalse(m1.equals(m3, ComparisonMode.IGNORE_METADATA));
        assertTrue (m3.equals(m4, ComparisonMode.IGNORE_METADATA));
        assertFalse(m3.equals(m4, ComparisonMode.BY_CONTRACT));
    }

    /**
     * Tests {@link DefaultOperationMethod#toWKT()}.
     * Since the WKT format of {@code OperationMethod} does not include parameters,
     * we do not bother specifying the parameters in the object created here.
     */
    @Test
    public void testWKT() {
        final OperationMethod method = create("Mercator (variant A)", "9804", "EPSG guidance note #7-2");
        assertWktEquals(Convention.WKT2, "METHOD[“Mercator (variant A)”, ID[“EPSG”, 9804, URI[“urn:ogc:def:method:EPSG::9804”]]]", method);
        assertWktEquals(Convention.WKT1, "PROJECTION[“Mercator (variant A)”, AUTHORITY[“EPSG”, “9804”]]", method);
    }
}
