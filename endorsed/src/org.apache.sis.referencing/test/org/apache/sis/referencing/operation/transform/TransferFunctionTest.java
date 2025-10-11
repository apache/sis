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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.operation.matrix.Matrix2;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.referencing.Assertions.assertMatrixEquals;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link TransferFunction}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class TransferFunctionTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TransferFunctionTest() {
    }

    /**
     * Tests the creation of a linear transfer function.
     */
    @Test
    public void testLinear() {
        final TransferFunction f = new TransferFunction();
        assertEquals(TransferFunctionType.LINEAR, f.getType());
        assertEquals(1, f.getBase());
        assertEquals(1, f.getScale());
        assertEquals(0, f.getOffset());
        assertEquals("y = x", f.toString());
        assertTrue(f.isIdentity());

        f.setScale(0.15);
        f.setOffset(-2);
        assertEquals("y = 0.15⋅x − 2", f.toString());
        final MathTransform1D transform = f.getTransform();
        assertMatrixEquals(new Matrix2(0.15, -2, 0, 1), transform, "transform.matrix");
        /*
         * Get back the coefficients.
         */
        final var b = new TransferFunction();
        b.setTransform(transform);
        assertEquals(TransferFunctionType.LINEAR, b.getType());
        assertEquals( 1,    b.getBase());
        assertEquals( 0.15, b.getScale());
        assertEquals(-2,    b.getOffset());
        assertFalse(b.isIdentity());
    }

    /**
     * Tests the creation of a logarithmic transfer function.
     */
    @Test
    public void testLogarithmic() {
        final TransferFunction f = new TransferFunction();
        f.setType(TransferFunctionType.LOGARITHMIC);
        f.setOffset(-2);
        assertEquals(10, f.getBase());
        assertEquals("y = ㏒⒳ − 2", f.toString());
        final MathTransform1D transform = f.getTransform();
        assertInstanceOf(LogarithmicTransform1D.class, transform);
        /*
         * Get back the coefficients.
         */
        final TransferFunction b = new TransferFunction();
        b.setTransform(transform);
        assertEquals(TransferFunctionType.LOGARITHMIC, b.getType());
        assertEquals(10, b.getBase());
        assertEquals( 1, b.getScale());
        assertEquals(-2, b.getOffset());
        assertFalse(b.isIdentity());
    }

    /**
     * Tests the creation of an exponential transfer function.
     */
    @Test
    public void testExponential() {
        final TransferFunction f = new TransferFunction();
        f.setType(TransferFunctionType.EXPONENTIAL);
        f.setScale(0.15);
        assertEquals(10, f.getBase());
        assertEquals("y = 0.15⋅10ˣ", f.toString());
        final MathTransform1D transform = f.getTransform();
        assertInstanceOf(ExponentialTransform1D.class, transform);
        /*
         * Get back the coefficients.
         */
        final TransferFunction b = new TransferFunction();
        b.setTransform(transform);
        assertEquals(TransferFunctionType.EXPONENTIAL, b.getType());
        assertEquals(10,   b.getBase());
        assertEquals(0.15, b.getScale());
        assertEquals(0,    b.getOffset());
        assertFalse(b.isIdentity());
    }

    /**
     * Tests the creation of a concatenated transfer function.
     */
    @Test
    public void testConcatenated() {
        final TransferFunction f = new TransferFunction();
        f.setType(TransferFunctionType.LOGARITHMIC);
        f.setScale(0.15);
        f.setOffset(-2);
        assertEquals("y = 0.15⋅㏒⒳ − 2", f.toString());
        final MathTransform1D transform = f.getTransform();
        assertInstanceOf(ConcatenatedTransformDirect1D.class, transform);
        /*
         * Get back the coefficients.
         */
        final TransferFunction b = new TransferFunction();
        b.setTransform(transform);
        assertEquals(TransferFunctionType.LOGARITHMIC, b.getType());
        assertEquals(10,   b.getBase());
        assertEquals(0.15, b.getScale(),  1E-16);
        assertEquals(-2,   b.getOffset(), 1E-16);
        assertFalse(b.isIdentity());
    }

    /**
     * Same tests as above, but using a math transform factory.
     *
     * @throws FactoryException if the factory failed to create a transform.
     */
    @Test
    public void testCreateTransform() throws FactoryException {
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        final TransferFunction f = new TransferFunction();
        f.setScale(0.15);
        f.setOffset(-2);
        MathTransform transform = f.createTransform(factory);
        assertMatrixEquals(new Matrix2(0.15, -2, 0, 1), transform, "transform.matrix");
        /*
         * Logarithmic case.
         */
        f.setType(TransferFunctionType.LOGARITHMIC);
        f.setScale(1);
        f.setOffset(-2);
        transform = f.getTransform();
        assertInstanceOf(LogarithmicTransform1D.class, transform);
        /*
         * Exponential case.
         */
        f.setType(TransferFunctionType.EXPONENTIAL);
        f.setScale(0.15);
        f.setOffset(0);
        transform = f.getTransform();
        assertInstanceOf(ExponentialTransform1D.class, transform);
    }
}
