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
package org.apache.sis.referencing.datum;

import java.util.Date;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.internal.referencing.Formulas.JULIAN_YEAR_LENGTH;


/**
 * Tests {@link TimeDependentBWP}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(BursaWolfParametersTest.class)
public final strictfp class TimeDependentBWPTest extends TestCase {
    /**
     * Tests the {@link BursaWolfParameters#setPositionVectorTransformation(Matrix, double)} method
     * using the example given in the EPSG database for operation method EPSG:1053.
     *
     * @throws NoninvertibleMatrixException Should not happen.
     */
    @Test
    public void testSetPositionVectorTransformation() throws NoninvertibleMatrixException {
        final TimeDependentBWP p = new TimeDependentBWP(null, null, date("1994-01-01 00:00:00"));
        p.tX = -84.68  / 1000;  p.dtX = +1.42;
        p.tY = -19.42  / 1000;  p.dtY = +1.34;
        p.tZ = +32.01  / 1000;  p.dtZ = +0.90;
        p.rX = +0.4254 / 1000;  p.drX = -1.5461;
        p.rY = -2.2578 / 1000;  p.drY = -1.1820;
        p.rZ = -2.4015 / 1000;  p.drZ = -1.1551;
        p.dS = +0.00971;        p.ddS = +0.000109;
        /*
         * The transformation that we are going to test use as input
         * geocentric coordinates on ITRF2008 at epoch 2013.9.
         */
        final Date time = p.getTimeReference();
        time.setTime(time.getTime() + StrictMath.round((2013.9 - 1994) * JULIAN_YEAR_LENGTH));
        assertEquals(date("2013-11-25 11:24:00"), time);
        /*
         * Transform the point and compare with the expected value given in the EPSG examples.
         */
        final MatrixSIS toGDA94    = MatrixSIS.castOrCopy(p.getPositionVectorTransformation(time));
        final MatrixSIS toITRF2008 = toGDA94.inverse();
        final MatrixSIS source     = Matrices.create(4, 1, new double[] {-3789470.702, 4841770.411, -1690893.950, 1});
        final MatrixSIS target     = Matrices.create(4, 1, new double[] {-3789470.008, 4841770.685, -1690895.103, 1});
//      assertMatrixEquals("toGDA94",    target, toGDA94   .multiply(source), 0.001);
//      assertMatrixEquals("toITRF2008", source, toITRF2008.multiply(target), 0.001);
    }
}
