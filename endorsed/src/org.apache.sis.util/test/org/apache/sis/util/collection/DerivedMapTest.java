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
package org.apache.sis.util.collection;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumSet;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link DerivedMap}. For the purpose of this test, this class implements an
 * {@link ObjectConverter} for which input values are multiplied by 100, except value
 * {@value #EXCLUDED} which is converted to {@code null} (meaning: excluded from the
 * converted map).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DerivedMapTest extends TestCase implements ObjectConverter<Integer,Integer> {
    /**
     * The value to replace by {@code null}.
     */
    protected static final int EXCLUDED = 17;                       // non-private for javadoc purpose.

    /**
     * Creates a new test case.
     */
    public DerivedMapTest() {
    }

    /**
     * Fills test values in the given maps.
     */
    private static void fill(final Map<Integer,Integer> source,
                             final Map<Integer,Integer> target)
    {
        assertNull(source.put(4,   7 ));
        assertNull(target.put(400, 70));
        assertNull(source.put(3,   8 ));
        assertNull(target.put(300, 80));
        assertNull(source.put(9,   1 ));
        assertNull(target.put(900, 10));
        assertNull(source.put(2,   1 ));
        assertNull(target.put(200, 10));
    }

    /**
     * Tests {@link DerivedMap} without excluded value.
     */
    @Test
    public void testNoExclusion() {
        final Map<Integer,Integer> source = new HashMap<>();
        final Map<Integer,Integer> target = new HashMap<>();
        final Map<Integer,Integer> tested = DerivedMap.create(source, this, new DerivedSetTest());
        fill(source, target);
        assertEquals(target.size(),     tested.size());
        assertEquals(target.keySet(),   tested.keySet());
        assertEquals(target.entrySet(), tested.entrySet());
        assertEquals(target,            tested);
        assertTrue (tested.containsKey(400));
        assertFalse(tested.containsKey(4));

        assertEquals(8,  source.get        (3  ).intValue());       // Test before change.
        assertEquals(80, tested.remove     (300).intValue());
        assertFalse (    source.containsKey(3  ));                  // Test after change.
        assertEquals(80, target.remove     (300).intValue());       // For comparison purpose.
        assertEquals(target, tested);

        assertEquals(1,  source.get(9      ).intValue());
        assertEquals(10, tested.put(900, 30).intValue());
        assertEquals(3,  source.get(9      ).intValue());
    }

    /**
     * Tests {@link DerivedMap} with an excluded key.
     */
    @Test
    public void testWithExclusion() {
        final Map<Integer,Integer> source = new HashMap<>();
        final Map<Integer,Integer> target = new HashMap<>();
        final Map<Integer,Integer> tested = DerivedMap.create(source, this, new DerivedSetTest());
        fill(source, target);
        assertNull(source.put(EXCLUDED, 4));
        assertEquals(target.size(),     tested.size());
        assertEquals(target.keySet(),   tested.keySet());
        assertEquals(target.entrySet(), tested.entrySet());
        assertEquals(target,            tested);
    }

    /**
     * Returns the converter properties, which is injective and preserve order.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.ORDER_PRESERVING);
    }
    @Override public Class<Integer> getSourceClass() {return Integer.class;}
    @Override public Class<Integer> getTargetClass() {return Integer.class;}

    /**
     * Multiplies the given value by 10, except value {@value #EXCLUDED}.
     *
     * @param  value  the value to multiply.
     * @return the multiplied value, or {@code null}.
     */
    @Override
    public Integer apply(final Integer value) {
        if (value == EXCLUDED) {
            return null;
        }
        return value * 100;
    }

    /**
     * Returns the inverse of this object converter.
     */
    @Override
    public ObjectConverter<Integer,Integer> inverse() {
        return new ObjectConverter<Integer,Integer>() {
            @Override public ObjectConverter<Integer,Integer> inverse() {return DerivedMapTest.this;}
            @Override public Class<Integer> getSourceClass()            {return Integer.class;}
            @Override public Class<Integer> getTargetClass()            {return Integer.class;}
            @Override public Integer        apply(Integer value)        {return value / 100;}
            @Override public Set<FunctionProperty> properties() {
                return EnumSet.of(FunctionProperty.SURJECTIVE, FunctionProperty.ORDER_PRESERVING);
            }
        };
    }
}
