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
package org.apache.sis.referencing.operation.transform;

import java.util.Map;
import java.util.Iterator;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.ConicProjection;
import org.opengis.referencing.operation.PlanarProjection;
import org.opengis.referencing.operation.CylindricalProjection;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link OperationMethodSet}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.operation.DefaultOperationMethodTest.class,
})
public final strictfp class OperationMethodSetTest extends TestCase {
    /**
     * Creates a new two-dimensional operation method for an operation of the given name.
     *
     * @param  type The value to be returned by {@link DefaultOperationMethod#getOperationType()}.
     * @param  method The operation name (example: "Mercator (variant A)").
     * @return The operation method.
     */
    @SuppressWarnings("serial")
    private static DefaultOperationMethod createMethod(final Class<? extends Projection> type, final String method) {
        Map<String,?> properties = Collections.singletonMap(DefaultOperationMethod.NAME_KEY, method);
        final ParameterDescriptorGroup parameters = new DefaultParameterDescriptorGroup(properties, 1, 1);
        /*
         * Recycle the ParameterDescriptorGroup name for DefaultOperationMethod.
         * This save us one object creation, and is often the same name anyway.
         */
        properties = Collections.singletonMap(DefaultOperationMethod.NAME_KEY, parameters.getName());
        return new DefaultOperationMethod(properties, 2, 2, parameters) {
            @Override public Class<? extends Projection> getOperationType() {
                return type;
            }
        };
    }

    /**
     * Creates an {@code OperationMethodSet} from the given methods, using an iterable
     * which will guarantee that the iteration is performed at most once.
     *
     * @param type The type of coordinate operation for which to retain methods.
     * @param methods The {@link DefaultMathTransformFactory#methods} used for fetching the initial methods.
     */
    private static OperationMethodSet create(final Class<? extends Projection> type, final DefaultOperationMethod... methods) {
        @SuppressWarnings("serial")
        final Iterable<DefaultOperationMethod> asList = new UnmodifiableArrayList<DefaultOperationMethod>(methods) {
            private boolean isIterationDone;

            @Override
            public Iterator<DefaultOperationMethod> iterator() {
                assertFalse("Expected no more than one iteration.", isIterationDone);
                isIterationDone = true;
                return super.iterator();
            }
        };
        synchronized (asList) { // Needed for avoiding assertion error in OperationMethodSet.
            return new OperationMethodSet(type, asList);
        }
    }

    /**
     * Tests construction from an empty list.
     */
    @Test
    public void testEmpty() {
        assertEmpty(create(Projection.class));
    }

    /**
     * Asserts that the given {@link OperationMethodSet} is empty.
     */
    private static void assertEmpty(final OperationMethodSet set) {
        assertTrue  ("isEmpty", set.isEmpty());
        assertEquals("size", 0, set.size());
        final Iterator<OperationMethod> iterator = set.iterator();
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests a non-empty set.
     */
    @Test
    @DependsOnMethod("testEmpty")
    public void testMixedCases() {
        final DefaultOperationMethod merA = createMethod(CylindricalProjection.class, "Mercator (variant A)");
        final DefaultOperationMethod merB = createMethod(CylindricalProjection.class, "Mercator (variant B)");
        final DefaultOperationMethod merC = createMethod(CylindricalProjection.class, "Mercator (variant C)");
        final DefaultOperationMethod dup  = createMethod(CylindricalProjection.class, "Mercator (variant B)");
        final DefaultOperationMethod lamb = createMethod(ConicProjection.class, "Lambert");
        final DefaultOperationMethod[] methods = new DefaultOperationMethod[] {merA, merB, merC, dup, lamb};
        final OperationMethodSet mercators = create(CylindricalProjection.class, methods);
        final OperationMethodSet lambert   = create(      ConicProjection.class, methods);
        final OperationMethodSet all       = create(           Projection.class, methods);
        /*
         * Mercator case.
         *   - Intentionally start the iteration without checking 'hasNext()' - the iterator shall be robust to that.
         *   - Intentionally start an other iteration (indirectly) in the middle of the first one.
         */
        final Iterator<OperationMethod> iterator = mercators.iterator();
        assertSame(merA, iterator.next());
        assertSame(merB, iterator.next());
        assertArrayEquals("toArray", new DefaultOperationMethod[] {merA, merB, merC}, mercators.toArray());
        assertSame(merC, iterator.next());
        assertFalse (iterator.hasNext());
        assertFalse ("isEmpty", mercators.isEmpty());
        assertEquals("size", 3, mercators.size());
        /*
         * Lambert case. Test twice since the two excecutions will take different code paths.
         */
        assertEquals(Collections.singleton(lamb), lambert);
        assertEquals(Collections.singleton(lamb), lambert);
        /*
         * Test filtering: the test should not contain any conic projection.
         */
        assertEmpty(create(PlanarProjection.class, methods));
        /*
         * Opportunist tests.
         */
        assertFalse(lambert.containsAll(all));
        assertTrue(all.containsAll(lambert));
        assertTrue(all.containsAll(mercators));
    }
}
