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
import java.util.HashMap;
import java.util.Locale;
import org.opengis.parameter.GeneralParameterDescriptor;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Validators.*;
import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;


/**
 * Tests the {@link DefaultParameterDescriptorGroup} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
@DependsOn(DefaultParameterDescriptorTest.class)
public final strictfp class DefaultParameterDescriptorGroupTest extends TestCase {
    /**
     * Returns a group of 4 parameters of type {@link Integer} with default value 10.
     * The two first parameters are mandatory, while the two last parameters are optional.
     * The very last parameter has a maximum number of occurrence of 2, which is illegal
     * according ISO 19111 but nevertheless supported by Apache SIS.
     */
    static final DefaultParameterDescriptorGroup createGroupOfIntegers() {
        final Integer DEFAULT = 10;
        final Class<Integer> type = Integer.class;
        return new DefaultParameterDescriptorGroup(name("The group"), 0, 1,
            new DefaultParameterDescriptor<>(name("Mandatory 1"), type, null, null, DEFAULT, true),
            new DefaultParameterDescriptor<>(name("Mandatory 2"), type, null, null, DEFAULT, true),
            new DefaultParameterDescriptor<>(name( "Optional 3"), type, null, null, DEFAULT, false),
            new MultiOccurrenceDescriptor <>(name( "Optional 4"), type, null, null, DEFAULT, false)
        );
    }

    /**
     * Returns a map with only one entry, which is {@code "name"}=<var>name</var>.
     */
    static Map<String,String> name(final String name) {
        return singletonMap(NAME_KEY, name);
    }

    /**
     * Tests descriptor validation.
     */
    @Test
    public void testValidate() {
        for (final GeneralParameterDescriptor descriptor : createGroupOfIntegers().descriptors()) {
            AssertionError error = null;
            try {
                validate(descriptor);
            } catch (AssertionError e) {
                error = e;
            }
            if (descriptor instanceof MultiOccurrenceDescriptor) {
                assertNotNull("Validation methods should have detected that the descriptor is invalid.", error);
            } else if (error != null) {
                throw error;
            }
        }
    }
}
