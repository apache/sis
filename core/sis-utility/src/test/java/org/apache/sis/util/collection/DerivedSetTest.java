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
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Arrays;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link DerivedSet}. For the purpose of this test, this class implements an
 * {@link ObjectConverter} for which input values are multiplied by 10, except value
 * {@value #EXCLUDED} which is converted to {@code null} (meaning: excluded from the
 * converted set).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class DerivedSetTest extends TestCase implements ObjectConverter<Integer,Integer> {
    /**
     * The value to replace by {@code null}.
     */
    protected static final int EXCLUDED = 19; // non-private for javadoc purpose.

    /**
     * Tests {@link DerivedSet} without excluded value.
     */
    @Test
    public void testNoExclusion() {
        final Set<Integer> source = new HashSet<Integer>(Arrays.asList(2,  7,  12,  17,  20 ));
        final Set<Integer> target = new HashSet<Integer>(Arrays.asList(20, 70, 120, 170, 200));
        final Set<Integer> tested = DerivedSet.create(source, this);
        assertEquals(target.size(), tested.size());
        assertEquals(target, tested);

        assertFalse("contains(2)",       tested.contains(2 )); // Original value
        assertTrue ("contains(20)",      tested.contains(20)); // Derived value
        assertTrue ("before remove(70)", source.contains(7 ));
        assertTrue (       "remove(70)", tested.remove  (70));
        assertFalse( "after remove(70)", source.contains(7 ));
        assertTrue (       "remove(70)", target.remove(70)); // For comparison purpose.
        assertEquals(target, tested);

        assertFalse("before add(30)", source.contains(3 ));
        assertTrue (       "add(30)", tested.add     (30));
        assertTrue ( "after add(30)", source.contains(3 ));
        assertTrue (       "add(30)", target.add     (30)); // For comparison purpose.
        assertEquals(target, tested);
    }

    /**
     * Tests {@link DerivedSet} with an excluded value.
     */
    @Test
    public void testWithExclusion() {
        final Set<Integer> source = new HashSet<Integer>(Arrays.asList(2,  7,  12,  EXCLUDED, 20));
        final Set<Integer> target = new HashSet<Integer>(Arrays.asList(20, 70, 120, 200));
        final Set<Integer> tested = DerivedSet.create(source, this);
        assertEquals(target.size(), tested.size());
        assertEquals(target, tested);
        assertFalse(tested.contains(EXCLUDED * 10));
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
     * Multiply the given value by 10, except value {@value #EXCLUDED}.
     *
     * @param  value The value to multiply.
     * @return The multiplied value, or {@code null}.
     */
    @Override
    public Integer apply(final Integer value) {
        if (value.intValue() == EXCLUDED) {
            return null;
        }
        return value * 10;
    }

    /**
     * Returns the inverse of this object converter.
     */
    @Override
    public ObjectConverter<Integer,Integer> inverse() {
        return new ObjectConverter<Integer,Integer>() {
            @Override public ObjectConverter<Integer,Integer> inverse() {return DerivedSetTest.this;}
            @Override public Class<Integer> getSourceClass()            {return Integer.class;}
            @Override public Class<Integer> getTargetClass()            {return Integer.class;}
            @Override public Integer        apply(Integer value)        {return value / 10;}
            @Override public Set<FunctionProperty> properties() {
                return EnumSet.of(FunctionProperty.SURJECTIVE, FunctionProperty.ORDER_PRESERVING);
            }
        };
    }
}
