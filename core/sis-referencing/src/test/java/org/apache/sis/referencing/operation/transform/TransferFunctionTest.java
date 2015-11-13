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

import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link TransferFunction}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(ExponentialTransform1DTest.class)
public final strictfp class TransferFunctionTest extends TestCase {
    /**
     * Tests the creation of a linear transfer function.
     */
    @Test
    public void testLinear() {
        final TransferFunction f = new TransferFunction();
        assertEquals("type", TransferFunctionType.LINEAR, f.getType());
        assertEquals("base",   1, f.getBase(),   STRICT);
        assertEquals("scale",  1, f.getScale(),  STRICT);
        assertEquals("offset", 0, f.getOffset(), STRICT);
        assertEquals("toString", "y = x", f.toString());

        f.setScale(0.15);
        f.setOffset(-2);
        assertEquals("toString", "y = 0.15⋅x − 2", f.toString());
        final MathTransform1D transform = f.getTransform();
        assertInstanceOf("transform", LinearTransform.class, transform);
        assertMatrixEquals("transform.matrix", new Matrix2(0.15, -2, 0, 1),
                ((LinearTransform) transform).getMatrix(), STRICT);
        /*
         * Get back the coefficients.
         */
        final TransferFunction b = new TransferFunction();
        b.setTransform(transform);
        assertEquals("type", TransferFunctionType.LINEAR, b.getType());
        assertEquals("base",    1,    b.getBase(),   STRICT);
        assertEquals("scale",   0.15, b.getScale(),  STRICT);
        assertEquals("offset", -2,    b.getOffset(), STRICT);
    }

    /**
     * Tests the creation of a logarithmic transfer function.
     */
    @Test
    @DependsOnMethod("testLinear")
    public void testLogarithmic() {
        final TransferFunction f = new TransferFunction();
        f.setType(TransferFunctionType.LOGARITHMIC);
        f.setOffset(-2);
        assertEquals("base", 10, f.getBase(), STRICT);
        assertEquals("toString", "y = ㏒⒳ − 2", f.toString());
        final MathTransform1D transform = f.getTransform();
        assertInstanceOf("transform", LogarithmicTransform1D.class, transform);
        /*
         * Get back the coefficients.
         */
        final TransferFunction b = new TransferFunction();
        b.setTransform(transform);
        assertEquals("type", TransferFunctionType.LOGARITHMIC, b.getType());
        assertEquals("base",   10, b.getBase(),   STRICT);
        assertEquals("scale",   1, b.getScale(),  STRICT);
        assertEquals("offset", -2, b.getOffset(), STRICT);
    }

    /**
     * Tests the creation of an exponential transfer function.
     */
    @Test
    @DependsOnMethod("testLinear")
    public void testExponential() {
        final TransferFunction f = new TransferFunction();
        f.setType(TransferFunctionType.EXPONENTIAL);
        f.setScale(0.15);
        assertEquals("base", 10, f.getBase(), STRICT);
        assertEquals("toString", "y = 0.15⋅10ˣ", f.toString());
        final MathTransform1D transform = f.getTransform();
        assertInstanceOf("transform", ExponentialTransform1D.class, transform);
        /*
         * Get back the coefficients.
         */
        final TransferFunction b = new TransferFunction();
        b.setTransform(transform);
        assertEquals("type", TransferFunctionType.EXPONENTIAL, b.getType());
        assertEquals("base",   10,    b.getBase(),   STRICT);
        assertEquals("scale",   0.15, b.getScale(),  STRICT);
        assertEquals("offset",  0,    b.getOffset(), STRICT);
    }

    /**
     * Tests the creation of a concatenated transfer function.
     */
    @Test
    @DependsOnMethod("testLogarithmic")
    public void testConcatenated() {
        final TransferFunction f = new TransferFunction();
        f.setType(TransferFunctionType.LOGARITHMIC);
        f.setScale(0.15);
        f.setOffset(-2);
        assertEquals("toString", "y = 0.15⋅㏒⒳ − 2", f.toString());
        final MathTransform1D transform = f.getTransform();
        assertInstanceOf("transform", ConcatenatedTransformDirect1D.class, transform);
        /*
         * Get back the coefficients.
         */
        final TransferFunction b = new TransferFunction();
        b.setTransform(transform);
        assertEquals("type", TransferFunctionType.LOGARITHMIC, b.getType());
        assertEquals("base",   10,    b.getBase(),   STRICT);
        assertEquals("scale",   0.15, b.getScale(),  1E-16);
        assertEquals("offset", -2,    b.getOffset(), 1E-16);
    }
}
