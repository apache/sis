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

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.opengis.referencing.operation.PassThroughOperation;


/**
 * Tests {@link OperationMethodSet}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class OperationMethodSetTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public OperationMethodSetTest() {
    }

    /**
     * Creates a new two-dimensional operation method for an operation of the given name.
     *
     * @param  type    the value to be returned by {@link DefaultOperationMethod#getOperationType()}.
     * @param  method  the operation name (example: "Mercator (variant A)").
     * @return the operation method.
     */
    @SuppressWarnings("serial")
    private static DefaultOperationMethod createMethod(final Class<? extends SingleOperation> type, String method) {
        Map<String,?> properties = Map.of(DefaultOperationMethod.NAME_KEY, method);
        final ParameterDescriptorGroup parameters = new DefaultParameterDescriptorGroup(properties, 1, 1);
        /*
         * Recycle the ParameterDescriptorGroup name for DefaultOperationMethod.
         * This save us one object creation, and is often the same name anyway.
         */
        properties = Map.of(DefaultOperationMethod.NAME_KEY, parameters.getName());
        return new DefaultOperationMethod(properties, parameters) {
            @Override public Class<? extends SingleOperation> getOperationType() {
                return type;
            }
        };
    }

    /**
     * Creates an {@code OperationMethodSet} from the given methods, using an iterable
     * which will guarantee that the iteration is performed at most once.
     *
     * @param  type     the type of coordinate operation for which to retain methods.
     * @param  methods  the {@link DefaultMathTransformFactory#methods} used for fetching the initial methods.
     */
    private static OperationMethodSet create(final Class<? extends SingleOperation> type,
                                             final DefaultOperationMethod... methods)
    {
        @SuppressWarnings("serial")
        final Iterable<DefaultOperationMethod> asList = new AbstractCollection<DefaultOperationMethod>() {
            private boolean isIterationDone;

            @Override
            public Iterator<DefaultOperationMethod> iterator() {
                assertFalse(isIterationDone);
                isIterationDone = true;
                return Arrays.asList(methods).iterator();
            }

            @Override
            public int size() {
                return methods.length;
            }
        };
        synchronized (asList) {                           // Needed for avoiding assertion error in OperationMethodSet.
            return new OperationMethodSet(type, asList);
        }
    }

    /**
     * Tests construction from an empty list.
     */
    @Test
    public void testEmpty() {
        assertEmpty(create(Conversion.class));
    }

    /**
     * Asserts that the given {@link OperationMethodSet} is empty.
     */
    private static void assertEmpty(final OperationMethodSet set) {
        assertTrue  (set.isEmpty());
        assertEquals(0, set.size());
        final Iterator<OperationMethod> iterator = set.iterator();
        assertFalse(iterator.hasNext());
        var e = assertThrows(NoSuchElementException.class, () -> iterator.next());
        assertNotNull(e);
    }

    /**
     * Tests a non-empty set.
     */
    @Test
    public void testMixedCases() {
        final DefaultOperationMethod merA = createMethod(Conversion.class, "Mercator (variant A)");
        final DefaultOperationMethod merB = createMethod(Conversion.class, "Mercator (variant B)");
        final DefaultOperationMethod merC = createMethod(Conversion.class, "Mercator (variant C)");
        final DefaultOperationMethod dup  = createMethod(Conversion.class, "Mercator (variant B)");
        final DefaultOperationMethod nad  = createMethod(Transformation.class, "NADCON");
        final var methods = new DefaultOperationMethod[] {merA, merB, merC, dup, nad};
        final OperationMethodSet mercators = create(Conversion.class, methods);
        final OperationMethodSet shifts    = create(Transformation.class, methods);
        final OperationMethodSet all       = create(SingleOperation.class, methods);
        /*
         * Mercator case.
         *   - Intentionally start the iteration without checking `hasNext()` - the iterator shall be robust to that.
         *   - Intentionally start another iteration (indirectly) in the middle of the first one.
         */
        final Iterator<OperationMethod> iterator = mercators.iterator();
        assertSame(merA, iterator.next());
        assertSame(merB, iterator.next());
        assertArrayEquals(new DefaultOperationMethod[] {merA, merB, merC}, mercators.toArray());
        assertSame(merC, iterator.next());
        assertFalse (iterator.hasNext());
        assertFalse (mercators.isEmpty());
        assertEquals(3, mercators.size());
        /*
         * NADCON case. Test twice because the two excecutions will take different code paths.
         */
        assertEquals(Set.of(nad), shifts);
        assertEquals(Set.of(nad), shifts);
        /*
         * Test filtering: the test should not contain any pass-through operation.
         */
        assertEmpty(create(PassThroughOperation.class, methods));
        /*
         * Opportunist tests.
         */
        assertFalse(shifts.containsAll(all));
        assertTrue(all.containsAll(shifts));
        assertTrue(all.containsAll(mercators));
    }
}
