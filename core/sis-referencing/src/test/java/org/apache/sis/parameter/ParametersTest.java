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

import java.util.Collection;
import java.util.Set;
import javax.measure.unit.SI;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import javax.measure.unit.Unit;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Parameters} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({DefaultParameterDescriptorTest.class, DefaultParameterValueTest.class})
public final class ParametersTest extends TestCase {
    /**
     * Tests {@link Parameters#getValueDomain(ParameterDescriptor)
     */
    @Test
    public void testValueDomain() {
        assertNull(Parameters.getValueDomain(null));
        verifyValueDomain(null,
                DefaultParameterDescriptorTest.createSimpleOptional("No range", String.class));
        verifyValueDomain(NumberRange.create(1, true, 4, true),
                DefaultParameterDescriptorTest.create("Integers", 1, 4, 2));
        verifyValueDomain(MeasurementRange.create(1d, true, 4d, true, SI.METRE),
                DefaultParameterDescriptorTest.create("Doubles", 1d, 4d, 2d, SI.METRE));
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
            @Override public ReferenceIdentifier      getName()          {return descriptor.getName();}
            @Override public Collection<GenericName>  getAlias()         {return descriptor.getAlias();}
            @Override public Set<ReferenceIdentifier> getIdentifiers()   {return descriptor.getIdentifiers();}
            @Override public InternationalString      getRemarks()       {return descriptor.getRemarks();}
            @Override public int                      getMinimumOccurs() {return descriptor.getMinimumOccurs();}
            @Override public int                      getMaximumOccurs() {return descriptor.getMaximumOccurs();}
            @Override public Class<T>                 getValueClass()    {return descriptor.getValueClass();}
            @Override public Set<T>                   getValidValues()   {return descriptor.getValidValues();}
            @Override public Comparable<T>            getMinimumValue()  {return descriptor.getMinimumValue();}
            @Override public Comparable<T>            getMaximumValue()  {return descriptor.getMaximumValue();}
            @Override public T                        getDefaultValue()  {return descriptor.getDefaultValue();}
            @Override public Unit<?>                  getUnit()          {return descriptor.getUnit();}
            @Override public ParameterValue<T>        createValue()      {return descriptor.createValue();}
            @Override public String                   toWKT()            {return descriptor.toWKT();}
        }));
    }
}
