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
package org.apache.sis.filter.visitor;

import java.util.Map;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.apache.sis.filter.DefaultFilterFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests copies using {@link CopyVisitor}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class CopyVisitorTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CopyVisitorTest() {
    }

    /**
     * Tests copy a value reference.
     */
    @Test
    public void copyValueReference() {
        final FilterFactory<Feature, ?, ?>                source  = DefaultFilterFactory.forFeatures();
        final FilterFactory<Map<String,?>, ?, ?>          target  = new FilterFactoryMock();
        final CopyVisitor  <Feature, Map<String,?>, ?, ?> visitor = new CopyVisitor<>(target, true);

        final String xpath = "someProperty";
        final Expression<Feature, ?> exp = source.property(xpath);
        final Expression<Map<String,?>, ?> result = visitor.copy(exp);

        assertTrue(result instanceof ValueReferenceMock);
        assertEquals(xpath, ((ValueReferenceMock) result).getXPath());
    }

    /**
     * Tests copy of a function.
     */
    @Test
    public void copyFunction() {
        final FilterFactory<Feature, ?, ?>                source  = DefaultFilterFactory.forFeatures();
        final FilterFactory<Map<String,?>, ?, ?>          target  = new FilterFactoryMock();
        final CopyVisitor  <Feature, Map<String,?>, ?, ?> visitor = new CopyVisitor<>(target, true);

        final Expression<Feature, ?> exp1 = source.property("someProperty");
        final Expression<Feature, ?> exp2 = source.property("crs");
        final Expression<Feature, ?> fct  = source.function("ST_GeomFromText", exp1, exp2);
        final Expression<Map<String,?>, ?> result = visitor.copy(fct);

        assertTrue(result instanceof FunctionMock);
        var resultfct = ((FunctionMock) result).getParameters();
        assertEquals(2, resultfct.size());
        assertTrue(resultfct.get(0) instanceof ValueReferenceMock);
        assertTrue(resultfct.get(1) instanceof ValueReferenceMock);
    }
}
