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
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link DefaultOperationMethod}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
@DependsOn({
    DefaultFormulaTest.class,
    org.apache.sis.referencing.AbstractIdentifiedObjectTest.class
})
public final strictfp class DefaultOperationMethodTest extends TestCase {
    /**
     * Creates a new two-dimensional operation method for an operation of the given name and identifier.
     *
     * @param  method     The operation name (example: "Mercator (variant A)").
     * @param  identifier The EPSG numeric identifier (example: "9804").
     * @param  formula    Formula citation (example: "EPSG guidance note #7-2").
     * @return The operation method.
     */
    private static DefaultOperationMethod create(final String method, final String identifier, final String formula) {
        final Map<String,Object> properties = new HashMap<>(8);
        assertNull(properties.put(OperationMethod.NAME_KEY, method));
        assertNull(properties.put(Identifier.CODESPACE_KEY, "EPSG"));
        assertNull(properties.put(Identifier.AUTHORITY_KEY, HardCodedCitations.OGP));
        /*
         * The parameter group for a Mercator projection is actually not empty, but it is not the purpose of
         * this class to test DefaultParameterDescriptorGroup. So we use an empty group of parameters here.
         */
        final ParameterDescriptorGroup parameters = new DefaultParameterDescriptorGroup(properties, 1, 1);
        /*
         * NAME_KEY share the same Identifier instance for saving a little bit of memory.
         * Then define the other properties to be given to OperationMethod.
         */
        assertNotNull(properties.put(OperationMethod.NAME_KEY, parameters.getName()));
        assertNull(properties.put(OperationMethod.IDENTIFIERS_KEY, new ImmutableIdentifier(HardCodedCitations.OGP, "EPSG", identifier)));
        assertNull(properties.put(OperationMethod.FORMULA_KEY, new DefaultCitation(formula)));
        return new DefaultOperationMethod(properties, 2, 2, parameters);
    }

    /**
     * Tests the {@link DefaultOperationMethod#DefaultOperationMethod(Map)} constructor.
     */
    @Test
    public void testConstruction() {
        final OperationMethod method = create("Mercator (variant A)", "9804", "EPSG guidance note #7-2");
        assertEpsgIdentifierEquals("Mercator (variant A)", method.getName());
        assertEpsgIdentifierEquals("9804", getSingleton(method.getIdentifiers()));
        assertEquals("formula", "EPSG guidance note #7-2", method.getFormula().getCitation().getTitle().toString());
        assertEquals("sourceDimensions", Integer.valueOf(2), method.getSourceDimensions());
        assertEquals("targetDimensions", Integer.valueOf(2), method.getTargetDimensions());
    }

    /**
     * Tests {@link DefaultOperationMethod#equals(Object, ComparisonMode)}.
     */
    @Test
    public void testEquals() {
        final DefaultOperationMethod m1 = create("Mercator (variant A)", "9804", "EPSG guidance note #7-2");
        final DefaultOperationMethod m2 = create("Mercator (variant A)", "9804", "E = FE + a*ko(lon - lonO)");
        assertFalse ("STRICT",          m1.equals(m2, ComparisonMode.STRICT));
        assertFalse ("BY_CONTRACT",     m1.equals(m2, ComparisonMode.BY_CONTRACT));
        assertTrue  ("IGNORE_METADATA", m1.equals(m2, ComparisonMode.IGNORE_METADATA));
        assertEquals("Hash code should ignore metadata.", m1.hashCode(), m2.hashCode());

        final DefaultOperationMethod m3 = create("Mercator (variant B)", "9805", "EPSG guidance note #7-2");
        final DefaultOperationMethod m4 = create("mercator (variant b)", "9805", "EPSG guidance note #7-2");
        assertFalse("IGNORE_METADATA", m1.equals(m3, ComparisonMode.IGNORE_METADATA));
        assertTrue ("IGNORE_METADATA", m3.equals(m4, ComparisonMode.IGNORE_METADATA));
        assertFalse("BY_CONTRACT",     m3.equals(m4, ComparisonMode.BY_CONTRACT));
    }

    /**
     * Tests {@link DefaultOperationMethod#redimension(OperationMethod, Integer, Integer)}.
     */
    @Test
    @DependsOnMethod({"testConstruction", "testEquals"})
    public void testResize() {
        final OperationMethod method = create("Affine geometric transformation", "9623", "EPSG guidance note #7-2");
        OperationMethod other = DefaultOperationMethod.redimension(method, 2, 2);
        assertSame(method, other);
        assertTrue(method.equals(other));
        assertEquals("sourceDimensions", Integer.valueOf(2), other.getSourceDimensions());
        assertEquals("targetDimensions", Integer.valueOf(2), other.getTargetDimensions());

        other = DefaultOperationMethod.redimension(method, 2, 3);
        assertNotSame(method, other);
        assertFalse(method.equals(other));
        assertEquals("sourceDimensions", Integer.valueOf(2), other.getSourceDimensions());
        assertEquals("targetDimensions", Integer.valueOf(3), other.getTargetDimensions());

        other = DefaultOperationMethod.redimension(method, 3, 2);
        assertNotSame(method, other);
        assertFalse(method.equals(other));
        assertEquals("sourceDimensions", Integer.valueOf(3), other.getSourceDimensions());
        assertEquals("targetDimensions", Integer.valueOf(2), other.getTargetDimensions());
    }

    /**
     * Tests {@link DefaultOperationMethod#toWKT()}.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testWKT() {
        final OperationMethod method = create("Mercator (variant A)", "9804", "EPSG guidance note #7-2");
        assertWktEquals("Method[“Mercator (variant A)”, Id[“EPSG”, 9804, Citation[“OGP”], URI[“urn:ogc:def:method:EPSG::9804”]]]", method);
        assertWktEquals(Convention.WKT1, "PROJECTION[“Mercator (variant A)”, AUTHORITY[“EPSG”, “9804”]]", method);
    }
}
