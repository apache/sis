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
package org.apache.sis.io.wkt;

import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link MathTransformParser}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(org.apache.sis.referencing.operation.transform.MathTransformsTest.class)
public final strictfp class MathTransformParserTest extends TestCase {
    /**
     * The parser to use for the test.
     */
    private MathTransformParser parser;

    /**
     * Parses the given text.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    private MathTransform parse(final String text) throws ParseException {
        if (parser == null) {
            parser = new MathTransformParser(DefaultFactories.forBuildin(MathTransformFactory.class));
            assertEquals(DefaultMathTransformFactory.class.getCanonicalName(), parser.getPublicFacade());
        }
        final ParsePosition position = new ParsePosition(0);
        final MathTransform mt = (MathTransform) parser.parseObject(text, position);
        assertEquals("errorIndex", -1, position.getErrorIndex());
        assertEquals("index", text.length(), position.getIndex());
        return mt;
    }

    /**
     * Tests parsing of a {@code PARAM_MT["Affine", …]} element.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testParamMT() throws ParseException {
        MathTransform tr;
        tr = parse("PARAM_MT[\"Affine\","
                    + "PARAMETER[\"num_row\",2],"
                    + "PARAMETER[\"num_col\",2],"
                    + "PARAMETER[\"elt_0_1\",7]]");

        assertMatrixEquals("Affine", new Matrix2(
                1, 7,
                0, 1), MathTransforms.getMatrix(tr), STRICT);
        /*
         * Larger matrix, mix quote and bracket styles and insert spaces.
         */
        tr = parse("Param_MT(\"Affine\", "
                + "PARAMETER(“num_row”, 3), "
                + "Parameter(“num_col”, 3),\n"
                + "parameter[“elt_0_1”, 1], "
                + "parameter[“elt_0_2”, 2], "
                + "parameter[“elt_1_2”, 3] )");

        assertMatrixEquals("Affine", new Matrix3(
                1, 1, 2,
                0, 1, 3,
                0, 0, 1), MathTransforms.getMatrix(tr), STRICT);
    }

    /**
     * Tests parsing of a {@code INVERSE_MT[…]} element.
     * This test uses an affine transform for the inner {@code PARAM_MT[…]} element,
     * which is useless since we could as well inverse the matrix in-place. But this
     * approach is easier to test.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    @DependsOnMethod("testParamMT")
    public void testInverseMT() throws ParseException {
        final MathTransform tr = parse(
                "INVERSE_MT["
                    + "PARAM_MT[\"Affine\","
                        + "PARAMETER[\"num_row\",3],"
                        + "PARAMETER[\"num_col\",3],"
                        + "PARAMETER[\"elt_0_0\",2],"
                        + "PARAMETER[\"elt_1_1\",4],"
                        + "PARAMETER[\"elt_0_2\",5],"
                        + "PARAMETER[\"elt_1_2\",3]]]");

        assertMatrixEquals("Affine", new Matrix3(
                0.5,  0,     -2.50,
                0,    0.25,  -0.75,
                0,    0,      1), MathTransforms.getMatrix(tr), STRICT);
    }

    /**
     * Tests parsing of a {@code CONCAT_MT[…]} element.
     * This test uses affine transforms for the inner {@code PARAM_MT[…]} elements,
     * which is useless since we could as well concatenate the matrices in-place.
     * But this approach is easier to test.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    @DependsOnMethod("testInverseMT")
    public void testConcatMT() throws ParseException {
        final MathTransform tr = parse(
                "CONCAT_MT["
                    + "PARAM_MT[\"Affine\","
                        + "PARAMETER[\"num_row\",3],"
                        + "PARAMETER[\"num_col\",3],"
                        + "PARAMETER[\"elt_0_0\",2],"
                        + "PARAMETER[\"elt_1_1\",4]],"
                    + "INVERSE_MT["
                        + "PARAM_MT[\"Affine\","
                            + "PARAMETER[\"num_row\",3],"
                            + "PARAMETER[\"num_col\",3],"
                            + "PARAMETER[\"elt_0_2\",5],"
                            + "PARAMETER[\"elt_1_2\",3]]]]");

        assertMatrixEquals("Affine", new Matrix3(
                2,  0,  -5,
                0,  4,  -3,
                0,  0,   1), MathTransforms.getMatrix(tr), STRICT);
    }
}
