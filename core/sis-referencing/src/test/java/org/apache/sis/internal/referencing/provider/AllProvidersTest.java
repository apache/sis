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
package org.apache.sis.internal.referencing.provider;

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.ServiceLoader;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests all providers defined in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.operation.DefaultOperationMethodTest.class,
    AffineTest.class,
    MapProjectionTest.class
})
public final strictfp class AllProvidersTest extends TestCase {
    /**
     * Returns all registered operation methods.
     */
    private static Iterable<OperationMethod> methods() {
        return ServiceLoader.load(OperationMethod.class, AbstractProvider.class.getClassLoader());
    }

    /**
     * Ensures that every parameter instance is unique. Actually this test is not strong requirement.
     * This is only for sharing existing resources by avoiding unnecessary objects duplication.
     */
    @Test
    public void ensureParameterUniqueness() {
        final Map<GeneralParameterDescriptor, String> groupNames = new IdentityHashMap<>();
        final Map<GeneralParameterDescriptor, GeneralParameterDescriptor> existings = new HashMap<>();
        for (final OperationMethod method : methods()) {
            final ParameterDescriptorGroup group = method.getParameters();
            final String name = group.getName().getCode();
            for (final GeneralParameterDescriptor param : group.descriptors()) {
                assertFalse("Parameter declared twice in the same group.", name.equals(groupNames.put(param, name)));
                final GeneralParameterDescriptor existing = existings.put(param, param);
                if (existing != null && existing != param) {
                    fail("Parameter “" + param.getName().getCode() + "” defined in “" + name + '”'
                            + " was already defined in “" + groupNames.get(existing) + "”."
                            + " The same instance could be shared.");
                }
            }
        }
    }
}
