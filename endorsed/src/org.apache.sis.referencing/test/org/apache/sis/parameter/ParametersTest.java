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
package org.apache.sis.parameter;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import javax.measure.Unit;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Optional;
import org.opengis.parameter.ParameterDirection;
import org.opengis.util.TypeName;

// Specific to the geoapi-4.0 branch:
import org.opengis.metadata.Identifier;


/**
 * Tests the static methods in the {@link Parameters} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ParametersTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ParametersTest() {
    }

    /**
     * Tests the {@link Parameters#cast(ParameterDescriptor, Class)} and
     * {@link Parameters#cast(ParameterValue, Class)} methods.
     */
    @Test
    public void testCast() {
        final ParameterDescriptor<Integer> descriptor = DefaultParameterDescriptorTest.create("My param", 5, 15, 10);
        assertSame(descriptor, Parameters.cast(descriptor, Integer.class));

        ClassCastException e;
        e = assertThrows(ClassCastException.class, () -> Parameters.cast(descriptor, Double.class));
        assertMessageContains(e, "My param", "Integer");
        /*
         * Tests the cast of values.
         */
        final ParameterValue<Integer> value = descriptor.createValue();
        assertEquals(10, value.intValue(), "Expected a parameter initialized to the default value.");
        assertSame(value, Parameters.cast(value, Integer.class));

        e = assertThrows(ClassCastException.class, () -> Parameters.cast(value, Double.class));
        assertMessageContains(e, "My param", "Integer");
    }

    /**
     * Tests {@link Parameters#getValueDomain(ParameterDescriptor)}.
     */
    @Test
    public void testValueDomain() {
        assertNull(Parameters.getValueDomain(null));
        verifyValueDomain(null,
                DefaultParameterDescriptorTest.createSimpleOptional("No range", String.class));
        verifyValueDomain(NumberRange.create(1, true, 4, true),
                DefaultParameterDescriptorTest.create("Integers", 1, 4, 2));
        verifyValueDomain(MeasurementRange.create(1d, true, 4d, true, Units.METRE),
                DefaultParameterDescriptorTest.create("Doubles", 1d, 4d, 2d, Units.METRE));
    }

    /**
     * Implementation of {@link #testValueDomain()} on a single descriptor instance.
     * This method test two paths:
     *
     * <ul>
     *   <li>The special case for {@link DefaultParameterDescriptor} instances.</li>
     *   <li>The fallback for generic cases. For that test, we wrap the descriptor in an anonymous class
     *       for hiding the fact that the descriptor is an instance of {@code DefaultParameterDescriptor}.</li>
     * </ul>
     */
    private static <T extends Comparable<? super T>> void verifyValueDomain(
            final Range<T> valueDomain, final ParameterDescriptor<T> descriptor)
    {
        assertEquals(valueDomain, Parameters.getValueDomain(descriptor));
        assertEquals(valueDomain, Parameters.getValueDomain(new ParameterDescriptor<T>() {
            @Override public Identifier                    getName()          {return descriptor.getName();}
            @Override public Collection<GenericName>       getAlias()         {return descriptor.getAlias();}
            @Override public Set<Identifier>               getIdentifiers()   {return descriptor.getIdentifiers();}
            @Override public Optional<InternationalString> getRemarks()       {return descriptor.getRemarks();}
            @Override public Optional<InternationalString> getDescription()   {return descriptor.getDescription();}
            @Override public ParameterDirection            getDirection()     {return descriptor.getDirection();}
            @Override public int                           getMinimumOccurs() {return descriptor.getMinimumOccurs();}
            @Override public int                           getMaximumOccurs() {return descriptor.getMaximumOccurs();}
            @Override public TypeName                      getValueType()     {return descriptor.getValueType();}
            @Override public Class<T>                      getValueClass()    {return descriptor.getValueClass();}
            @Override public Set<T>                        getValidValues()   {return descriptor.getValidValues();}
            @Override public Comparable<T>                 getMinimumValue()  {return descriptor.getMinimumValue();}
            @Override public Comparable<T>                 getMaximumValue()  {return descriptor.getMaximumValue();}
            @Override public T                             getDefaultValue()  {return descriptor.getDefaultValue();}
            @Override public Unit<?>                       getUnit()          {return descriptor.getUnit();}
            @Override public ParameterValue<T>             createValue()      {return descriptor.createValue();}
            @Override public String                        toWKT()            {return descriptor.toWKT();}
        }));
    }

    /**
     * Tests {@link Parameters#copy(ParameterValueGroup, ParameterValueGroup)}.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-202">SIS-202</a>
     */
    @Test
    public void testCopy() {
        /*
         * The descriptor to be used for this test. This descriptor contain at least
         * one subgroup, for testing the Parameters.copy(...) method recursion.
         */
        final String subgroupName = DefaultParameterDescriptorGroupTest.M1_M1_O1_O2.getName().getCode();
        final DefaultParameterDescriptorGroup descriptor = new DefaultParameterDescriptorGroup(
                Map.of(DefaultParameterDescriptorGroup.NAME_KEY, "parent"), 1, 1,
                DefaultParameterDescriptorTest.createSimpleOptional("A parent parameter", String.class),
                DefaultParameterDescriptorGroupTest.M1_M1_O1_O2);
        /*
         * Create the parameter value to copy. We set some values, but intentionally not all of them.
         * The unset values will be used for verifying that they do not overwrite destination values.
         */
        final ParameterValueGroup source = descriptor.createValue();
        final ParameterValueGroup sourceSubgroup = source.addGroup(subgroupName);
        final ParameterValue<?> o1 = sourceSubgroup.parameter("Optional 4");
        final ParameterValue<?> o2 = o1.getDescriptor().createValue();      // See ParameterFormatTest.testMultiOccurrence()
        sourceSubgroup.parameter("Mandatory 2").setValue(20);
        sourceSubgroup.values().add(o2);
        o1.setValue(40);
        o2.setValue(50);
        source.parameter("A parent parameter").setValue("A value from the source");
        /*
         * Create the parameter to use as the destination. We put some value in those parameters in order to
         * verify that those values are overwritten (only those for which the value is set in the source).
         */
        final ParameterValueGroup target = descriptor.createValue();
        final ParameterValueGroup targetSubgroup = target.addGroup(subgroupName);
        targetSubgroup.parameter("Mandatory 1").setValue(-10);      // We expect this value to be overwritten.
        targetSubgroup.parameter("Optional 3") .setValue( 30);      // We expect this value to be preserved.
        target.parameter("A parent parameter") .setValue("A value to be overwritten");
        /*
         * The actual test.
         */
        Parameters.copy(source, target);
        assertSame(sourceSubgroup, assertSingleton(source.groups(subgroupName)));
        assertSame(targetSubgroup, assertSingleton(target.groups(subgroupName)));
        assertEquals("A value from the source", target.parameter("A parent parameter").getValue());
        assertEquals(10, targetSubgroup.parameter("Mandatory 1").intValue());
        assertEquals(20, targetSubgroup.parameter("Mandatory 2").intValue());
        assertEquals(30, targetSubgroup.parameter("Optional 3") .intValue());
        assertEquals(40, ((ParameterValue<?>) targetSubgroup.values().get(3)).intValue());
        assertEquals(50, ((ParameterValue<?>) targetSubgroup.values().get(4)).intValue());
    }

    /**
     * Tests {@link Parameters#getValue(ParameterDescriptor)} and {@link Parameters#intValue(ParameterDescriptor)}.
     */
    @Test
    public void testGetIntValue() {
        final ParameterDescriptor<Integer> descriptor = DefaultParameterDescriptorTest.create("My param", 5, 15, 10);
        final ParameterDescriptor<Integer> incomplete = DefaultParameterDescriptorTest.createSimpleOptional("My param", Integer.class);
        final Parameters group = Parameters.castOrWrap(new DefaultParameterDescriptorGroup(Map.of(
                DefaultParameterDescriptorGroup.NAME_KEY, "My group"), 1, 1, incomplete).createValue());
        /*
         * Test when the ParameterValueGroup is empty. We test both with the "incomplete" descriptor,
         * which contain no default value, and with the complete one, which provide a default value.
         */
        assertNull(group.getValue(incomplete));
        assertEquals(Integer.valueOf(10), group.getValue(descriptor));
        var e = assertThrows(IllegalStateException.class, () -> group.intValue(incomplete));
        assertMessageContains(e, "My param");
        /*
         * Define a value and test again.
         */
        group.parameter("My param").setValue(12);
        assertEquals(Integer.valueOf(12), group.getValue(incomplete));
        assertEquals(Integer.valueOf(12), group.getValue(descriptor));
        assertEquals(12, group.intValue(incomplete));
        assertEquals(12, group.intValue(descriptor));
    }
}
