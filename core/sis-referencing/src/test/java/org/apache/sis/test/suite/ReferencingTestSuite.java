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
package org.apache.sis.test.suite;

import org.apache.sis.test.TestSuite;
import org.junit.runners.Suite;
import org.junit.BeforeClass;


/**
 * All tests from the {@code sis-referencing} module, in approximative dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@Suite.SuiteClasses({
    // Test matrix first because they may be used in about every SIS corners.
    org.apache.sis.referencing.operation.matrix.GeneralMatrixTest.class,
    org.apache.sis.referencing.operation.matrix.SolverTest.class,
    org.apache.sis.referencing.operation.matrix.Matrix1Test.class,
    org.apache.sis.referencing.operation.matrix.Matrix2Test.class,
    org.apache.sis.referencing.operation.matrix.Matrix3Test.class,
    org.apache.sis.referencing.operation.matrix.Matrix4Test.class,
    org.apache.sis.referencing.operation.matrix.NonSquareMatrixTest.class, // Expected to be last MatrixTestCase - see javadoc.
    org.apache.sis.referencing.operation.matrix.MatricesTest.class,
    org.apache.sis.referencing.operation.matrix.AffineTransforms2DTest.class,

    org.apache.sis.internal.referencing.FormulasTest.class,
    org.apache.sis.internal.referencing.VerticalDatumTypesTest.class,
    org.apache.sis.internal.referencing.AxisDirectionsTest.class,
    org.apache.sis.internal.referencing.ReferencingUtilitiesTest.class,
    org.apache.sis.io.wkt.ConventionTest.class,
    org.apache.sis.io.wkt.SymbolsTest.class,
    org.apache.sis.io.wkt.FormatterTest.class,
    org.apache.sis.internal.jaxb.referencing.CodeTest.class,
    org.apache.sis.internal.jaxb.referencing.SecondDefiningParameterTest.class,
    org.apache.sis.referencing.IdentifiedObjectsTest.class,
    org.apache.sis.referencing.NamedIdentifierTest.class,
    org.apache.sis.referencing.AbstractIdentifiedObjectTest.class,
    org.apache.sis.referencing.AbstractReferenceSystemTest.class,
    org.apache.sis.referencing.datum.BursaWolfParametersTest.class,
    org.apache.sis.referencing.datum.TimeDependentBWPTest.class,
    org.apache.sis.referencing.datum.DefaultEllipsoidTest.class,
    org.apache.sis.referencing.datum.DefaultPrimeMeridianTest.class,
    org.apache.sis.referencing.datum.DefaultVerticalDatumTest.class,
    org.apache.sis.referencing.datum.DefaultTemporalDatumTest.class,
    org.apache.sis.referencing.datum.DefaultGeodeticDatumTest.class,
    org.apache.sis.referencing.cs.DirectionAlongMeridianTest.class,
    org.apache.sis.referencing.cs.DefaultCoordinateSystemAxisTest.class,
    org.apache.sis.referencing.cs.ComparableAxisWrapperTest.class,
    org.apache.sis.referencing.cs.AbstractCSTest.class,
    org.apache.sis.referencing.cs.DefaultCartesianCSTest.class,
    org.apache.sis.referencing.cs.DefaultEllipsoidalCSTest.class,
    org.apache.sis.referencing.cs.CoordinateSystemsTest.class,
    org.apache.sis.referencing.crs.DefaultGeodeticCRSTest.class,
    org.apache.sis.referencing.StandardDefinitionsTest.class,
    org.apache.sis.referencing.GeodeticObjectsTest.class,

    org.apache.sis.geometry.AbstractDirectPositionTest.class,
    org.apache.sis.geometry.GeneralDirectPositionTest.class,
    org.apache.sis.geometry.DirectPosition1DTest.class,
    org.apache.sis.geometry.DirectPosition2DTest.class,
    org.apache.sis.geometry.AbstractEnvelopeTest.class,
    org.apache.sis.geometry.GeneralEnvelopeTest.class,
    org.apache.sis.geometry.SubEnvelopeTest.class,
    org.apache.sis.geometry.ImmutableEnvelopeTest.class,
    org.apache.sis.geometry.Envelope2DTest.class,

    org.apache.sis.test.integration.ReferencingInMetadataTest.class
})
public final strictfp class ReferencingTestSuite extends TestSuite {
    /**
     * Verifies the list of tests before to run the suite.
     * See {@link #verifyTestList(Class, Class[])} for more information.
     */
    @BeforeClass
    public static void verifyTestList() {
        assertNoMissingTest(ReferencingTestSuite.class);
        verifyTestList(ReferencingTestSuite.class);
    }
}
