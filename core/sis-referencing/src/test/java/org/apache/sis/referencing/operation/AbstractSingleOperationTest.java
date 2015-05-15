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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.referencing.operation.transform.MathTransformsTest;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link AbstractSingleOperation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    DefaultOperationMethodTest.class,
    MathTransformsTest.class
})
public final strictfp class AbstractSingleOperationTest extends TestCase {
    /**
     * Creates a dummy operation method of the given dimensions.
     */
    private static OperationMethod createOperationMethod(final int sourceDimension, final int targetDimension) {
        final ParameterDescriptorGroup parameters = new DefaultParameterDescriptorGroup(
                Collections.singletonMap(ParameterDescriptorGroup.NAME_KEY, "Dummy"), 1, 1);
        return new DefaultOperationMethod(Collections.singletonMap(OperationMethod.NAME_KEY, parameters.getName()),
                sourceDimension, targetDimension, parameters);
    }

    /**
     * Tests {@link AbstractSingleOperation#checkDimensions(OperationMethod, MathTransform, Map)}
     * without interpolation dimension.
     */
    @Test
    public void testCheckDimensions() {
        final Map<String,?> properties = Collections.singletonMap(DefaultOperationMethod.LOCALE_KEY, Locale.ENGLISH);
        final MathTransform tr = MathTransformsTest.createConcatenateAndPassThrough();
        AbstractSingleOperation.checkDimensions(createOperationMethod(3, 3), 0, tr, properties);
        AbstractSingleOperation.checkDimensions(createOperationMethod(1, 1), 0, tr, properties);
        try {
            AbstractSingleOperation.checkDimensions(createOperationMethod(2, 2), 0, tr, properties);
            fail("MathTransform.sourceDimension == 3 shall be considered incompatible.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertEquals(e.getMessage(), "The transform has 1 source dimension, while 2 was expected.");
        }
        try {
            AbstractSingleOperation.checkDimensions(createOperationMethod(3, 1), 0, tr, properties);
            fail("MathTransform.targetDimension == 3 shall be considered incompatible.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertEquals(e.getMessage(), "The transform has 3 target dimensions, while 1 was expected.");
        }
    }
}
