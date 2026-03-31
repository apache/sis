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
import java.util.List;
import java.util.ArrayList;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link DerivedList}. For the purpose of this test, this class implements
 * an {@link ObjectConverter} for which input values are multiplied by 10.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DerivedListTest extends TestCase implements ObjectConverter<Integer,Integer> {
    /**
     * Creates a new test case.
     */
    public DerivedListTest() {
    }

    /**
     * Tests {@link DerivedList} with read and write operations.
     */
    @Test
    public void testReadWrite() {
        final List<Integer> source = new ArrayList<>(List.of(2,  7,  12,  17,  20 ));
        final List<Integer> target = new ArrayList<>(List.of(20, 70, 120, 170, 200));
        final List<Integer> tested = WritableDerivedList.create(source, this);
        assertEquals(target.size(), tested.size());
        assertEquals(target, tested);

        assertFalse(tested.contains(2 ));           // Original value.
        assertTrue (tested.contains(20));           // Derived value.
        assertTrue (source.contains(7 ));           // Test before change.
        assertTrue (tested.remove(Integer.valueOf(70)));
        assertFalse(source.contains(7 ));
        assertTrue (target.remove(Integer.valueOf(70)));
        assertEquals(target, tested);

        assertFalse(source.contains(3 ));
        assertTrue (tested.add     (30));
        assertTrue (source.contains(3 ));
        assertTrue (target.add     (30));           // For comparison purpose.
        assertEquals(target, tested);
    }

    /**
     * Returns the converter properties.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return Set.of(FunctionProperty.INVERTIBLE);
    }

    @Override public Class<Integer> getSourceClass() {return Integer.class;}
    @Override public Class<Integer> getTargetClass() {return Integer.class;}

    /**
     * Multiply the given value by 10.
     *
     * @param  value  the value to multiply.
     * @return the multiplied value.
     */
    @Override
    public Integer apply(final Integer value) {
        return value * 10;
    }

    /**
     * Returns the inverse of this object converter.
     */
    @Override
    public ObjectConverter<Integer,Integer> inverse() {
        return new ObjectConverter<Integer,Integer>() {
            @Override public ObjectConverter<Integer,Integer> inverse() {return DerivedListTest.this;}
            @Override public Class<Integer> getSourceClass()            {return Integer.class;}
            @Override public Class<Integer> getTargetClass()            {return Integer.class;}
            @Override public Integer        apply(Integer value)        {return value / 10;}
            @Override public Set<FunctionProperty> properties() {
                return Set.of(FunctionProperty.INVERTIBLE);
            }
        };
    }
}
