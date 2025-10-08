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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import static java.lang.StrictMath.*;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.Units;
import static org.apache.sis.metadata.internal.shared.ReferencingServices.NAUTICAL_MILE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests {@link GeodesicsOnEllipsoid}. Some of the tests in this class use data published by
 * <a href="https://link.springer.com/content/pdf/10.1007%2Fs00190-012-0578-z.pdf">Algorithms
 * for geodesics from Charles F. F. Karney (SRI International)</a>.
 *
 * @author  Matthieu Bastianelli (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class GeodesicsOnEllipsoidTest extends GeodeticCalculatorTest {
    /**
     * The {@link GeodesicsOnEllipsoid} instance to be tested.
     * A specialized type is used for tracking locale variables.
     */
    private Calculator testedEarth;

    /**
     * Values of local variables in {@link GeodesicsOnEllipsoid} methods. If values for the same key are added
     * many times, the new values are appended after the current ones. This happen for example during iterations.
     *
     * @see GeodesicsOnEllipsoid#STORE_LOCAL_VARIABLES
     */
    private Map<String,double[]> localVariables;

    /**
     * {@code true} if {@link GeodesicsOnEllipsoid#store(String, double)} shall verify consistency instead of
     * storing local variables.
     */
    private boolean verifyConsistency;

    /**
     * Creates a new test case.
     */
    public GeodesicsOnEllipsoidTest() {
    }

    /**
     * Returns the calculator to use for testing purpose.
     *
     * @param  normalized  whether to force (longitude, latitude) axis order.
     */
    @Override
    GeodeticCalculator create(final boolean normalized) {
        testedEarth = new Calculator(normalized ? HardCodedCRS.WGS84 : HardCodedCRS.WGS84_LATITUDE_FIRST);
        return testedEarth;
    }

    /**
     * Creates a calculator which will store locale variables in the {@link #localVariables} map.
     * This is used for the test cases that compare intermediate calculations with values provided
     * in tables of Karney (2013) <cite>Algorithms for geodesics</cite> publication.
     */
    private void createTracked() {
        localVariables = new HashMap<>();
        testedEarth = new Calculator(HardCodedCRS.WGS84) {
            /** Replaces a computed value by the value given in Karney table. */
            @Override double computedToGiven(final double α1) {
                return (abs(TRUNCATED_α1 - toDegrees(α1)) < 1E-3) ? toRadians(TRUNCATED_α1) : α1;
            }

            /** Invoked when {@link GeodesicsOnEllipsoid} computed an intermediate value. */
            @Override void store(final String name, final double value) {
                super.store(name, value);
                if (verifyConsistency) {
                    assertValueEquals(name, 0, value, 0, false);
                } else {
                    double[] values = localVariables.putIfAbsent(name, new double[] {value});
                    if (values != null) {
                        final int i = values.length;
                        values = Arrays.copyOf(values, i+1);
                        values[i] = value;
                        localVariables.put(name, values);
                    }
                }
            }
        };
    }

    /**
     * The {@link GeodesicsOnEllipsoid} implementation to use for tests. This implementation compares the
     * quartic root computed by {@link GeodesicsOnEllipsoid#μ(double, double)} against the values computed
     * by {@link MathFunctions#polynomialRoots(double...)}.
     */
    private static class Calculator extends GeodesicsOnEllipsoid {
        /**
         * {@code true} if iteration stopped before to reach the desired accuracy because of limitation
         * in {@code double} precision. This field must be reset to {@code false} before any new point.
         *
         * @see #relaxIfConfirmed(KnownProblem)
         * @see #clear()
         */
        private boolean iterationReachedPrecisionLimit;

        /** Values needed for computation of μ. */
        private double x, y;

        /** Creates a new calculator for the given coordinate reference system. */
        Calculator(final GeographicCRS crs) {
            super(crs, DatumOrEnsemble.getEllipsoid(crs).orElseThrow());
        }

        /** Invoked when {@link GeodesicsOnEllipsoid} computed an intermediate value. */
        @Override void store(final String name, final double value) {
            if (name.equals("dα₁ ≪ α₁")) {
                iterationReachedPrecisionLimit = true;
            } else if (name.length() == 1) {
                switch (name.charAt(0)) {
                    case 'x': x = value; break;
                    case 'y': y = value; break;
                    case 'μ': {
                        double μ = 0;
                        final double y2 = y*y;
                        for (final double r : MathFunctions.polynomialRoots(-y2, -2*y2, (1 - x*x)-y2, 2, 1)) {
                            if (r > μ) μ = r;
                        }
                        if (μ > 0) {
                            /*
                             * This assertion fails if y² is too close to zero. We that as a criterion for choosing
                             * the threshold value that determine when to use Karney (2013) equation 57 instead.
                             * That threshold is in `GeodesicOnEllipsoid.computeDistance()` method.
                             */
                            assertEquals(μ, value, abs(μ) * 1E-8, "μ(x²,y²)");
                        }
                    }
                }
            }
        }
    }

    /**
     * Clears the tested {@link GeodeticCalculator} before to test a new point.
     * This is invoked by parent class between two tests using the same calculator.
     * The intent is to make sure that data from previous test are not mixed with current test.
     */
    @Override
    void clear() {
        if (localVariables != null) {
            localVariables.clear();
        }
        testedEarth.x = Double.NaN;
        testedEarth.y = Double.NaN;
        testedEarth.iterationReachedPrecisionLimit = false;
    }

    /**
     * Asserts that variable of the given name is equal to the given value. This is used for comparing an
     * intermediate value computed by Apache SIS against the value published in a table of Karney (2013)
     * <cite>Algorithms for geodesics</cite>.
     *
     * @param  name       name of the variable to verify.
     * @param  index      if the same variable has been set many times, its index with numbering starting at 1. Otherwise 0.
     * @param  expected   the expected value.
     * @param  tolerance  tolerance threshold for comparison.
     * @param  angular    whether the stored value needs to be converted from radians to degrees.
     */
    private void assertValueEquals(final String name, int index, final double expected, final double tolerance, final boolean angular) {
        final double[] values = localVariables.get(name);
        if (values != null) {
            if (index > 0) {
                index--;
            } else if (values.length != 1) {
                fail("Expected exactly one value for " + name + " but got " + Arrays.toString(values));
            }
            double value = values[index];
            if (angular) value = toDegrees(value);
            assertEquals(expected, value, tolerance, name);
        } else if (GeodesicsOnEllipsoid.STORE_LOCAL_VARIABLES) {
            fail("Missing value: " + name);
        }
    }

    /**
     * Takes a snapshot of some intermediate calculations in {@link GeodesicsOnEllipsoid}.
     * If {@link GeodesicsOnEllipsoid#STORE_LOCAL_VARIABLES} is {@code true}, then we already
     * have snapshots of those variables and this method will instead verify consistency.
     */
    private void snapshot() {
        verifyConsistency = true;
        testedEarth.snapshot();
    }

    /**
     * Verifies that the constant parameters set by constructor are equal to the ones used by Karney
     * for his example. Those values do not depend on the point being tested.
     *
     * <p><b>Source:</b>Karney (2013) table 1.</p>
     */
    private void verifyParametersForWGS84() {
        assertEquals(6378137,             testedEarth.semiMajorAxis,                          "a");
        assertEquals(6356752.314245,      testedEarth.ellipsoid.getSemiMinorAxis(),     1E-6, "b");
        assertEquals(298.257223563,       testedEarth.ellipsoid.getInverseFlattening(), 1E-9, "1/f");
        assertEquals(0.00669437999014132, testedEarth.eccentricitySquared,              1E-17, "ℯ²");
        assertEquals(0.00673949674227643, testedEarth.secondEccentricitySquared,        1E-17, "ℯ′²");
        assertEquals(0.00167922038638370, testedEarth.thirdFlattening,                  1E-16, "n");
        assertEquals(Units.METRE,         testedEarth.getDistanceUnit());
    }

    /**
     * Tests solving the direct geodesic problem by a call to {@link GeodesicsOnEllipsoid#computeEndPoint()}.
     * The data used by this test are provided by the example published in Karney (2013) table 2.
     * Input values are:
     *
     * <ul>
     *   <li>Starting longitude:  λ₁  =  any</li>
     *   <li>Starting latitude:   φ₁  =  40°</li>
     *   <li>Starting azimuth:    α₁  =  30°</li>
     *   <li>Geodesic distance:   s₁₂ =  10 000 000 m</li>
     * </ul>
     *
     * About 20 intermediate values are published in Karney table 2 and copied in this method body.
     * Those values are verified if {@link GeodesicsOnEllipsoid#STORE_LOCAL_VARIABLES} is {@code true}.
     *
     * <p><b>Source:</b> Karney (2013), <u>Algorithms for geodesics</u> table 2.</p>
     */
    @Test
    public void testComputeEndPoint() {
        createTracked();
        verifyParametersForWGS84();
        testedEarth.setStartGeographicPoint(40, 10);
        testedEarth.setStartingAzimuth(30);
        testedEarth.setGeodesicDistance(10000000);
        assertEquals(toDegrees(testedEarth.λ1), 10.0, Formulas.ANGULAR_TOLERANCE, "λ₁");
        assertEquals(toDegrees(testedEarth.φ1), 40.0, Formulas.ANGULAR_TOLERANCE, "φ₁");
        assertEquals(testedEarth.geodesicDistance, 10000000, "∆s");
        /*
         * The following method invocation causes calculation of all intermediate values.
         * Some intermediate values are verified by `assertValueEquals` statements below.
         * If some values are wrong, it is easier to check correctness with step-by-step
         * debugging of `computeDistance()` method while keeping table 3 of Karney aside.
         */
        final DirectPosition endPoint = testedEarth.getEndPoint();
        snapshot();
        /*
         * Start point on auxiliary sphere:
         *
         *   β₁  —  reduced latitude of starting point.
         *   α₀  —  azimuth at equator in the direction given by α₁ azimuth at the β₁ reduced latitude.
         *   σ₁  —  arc length from equatorial point E to starting point on auxiliary sphere.
         *   ω₁  —  spherical longitude of the starting point (from equatorial point E) on the auxiliary sphere.
         *   s₁  —  distance from equatorial point E to starting point along the geodesic.
         */
        assertValueEquals("β₁", 0, 39.90527714601, 1E-11, true);
        assertValueEquals("α₀", 0, 22.55394020262, 1E-11, true);
        assertValueEquals("σ₁", 0, 43.99915364500, 1E-11, true);
        assertValueEquals("ω₁", 0, 20.32371827837, 1E-11, true);
        /*
         * End point on auxiliary sphere.
         *
         *   σ₂  —  arc lentgth from equator to the ending point σ₂ on auxiliary sphere.
         *   β₂  —  reduced latitude of ending point.
         *   ω₂  —  spherical longitude of the ending point (from equatorial point E) on the auxiliary sphere.
         */
        assertValueEquals("k²",     0, 0.00574802962857, 1E-14, false);
        assertValueEquals("ε",      0, 0.00143289220416, 1E-14, false);
        assertValueEquals("A₁",     0, 1.00143546236207, 1E-14, false);
        assertValueEquals("I₁(σ₁)", 0, 0.76831538886412, 1E-14, false);
        assertValueEquals("s₁",     0,  4883990.626232,  1E-6,  false);
        assertValueEquals("s₂",     0, 14883990.626232,  1E-6,  false);
        assertValueEquals("τ₂",     0, 133.96266050208,  1E-11, true);
        assertValueEquals("σ₂",     0, 133.92164083038,  1E-11, true);
        assertValueEquals("α₂",     0, 149.09016931807,  1E-11, true);
        assertValueEquals("β₂",     0,  41.69771809250,  1E-11, true);
        assertValueEquals("ω₂",     0, 158.28412147112,  1E-11, true);
        /*
         * Conversion of end point coordinates to geodetic longitude.
         *
         *   λ₁  —  longitudes of the starting point, measured from equatorial point E.
         *   λ₂  —  longitudes of the ending point, measured from Equatorial point E.
         */
        assertValueEquals("A₃",    0,   0.99928424306, 1E-11, false);
        assertValueEquals("I₃(σ)", 1,   0.76773786069, 1E-11, false);
        assertValueEquals("I₃(σ)", 2,   2.33534322170, 1E-11, false);
        assertValueEquals("λ₁",    0,  20.26715038016, 1E-11, true);
        assertValueEquals("λ₂",    0, 158.11205042393, 1E-11, true);
        assertValueEquals("Δλ",    0, 137.84490004377, 1E-11, true);
        /*
         * Final result:
         *
         *   φ₂  —  end point latitude.
         *   λ₂  —  end point longitude. Shifted by 10° compared to Karney because of λ₁ = 10°.
         *   α₂  —  azimuth at end point.
         */
        assertEquals( 41.79331020506, endPoint.getCoordinate(1), 1E-11, "φ₂");
        assertEquals(147.84490004377, endPoint.getCoordinate(0), 1E-11, "λ₂");
        assertEquals(149.09016931807, testedEarth.getEndingAzimuth(), 1E-11, "α₂");
    }

    /**
     * Tests solving the inverse geodesic problem by a call to {@link GeodesicsOnEllipsoid#computeDistance()}
     * for a short distance. The data used by this test are provided by the example published in Karney (2013)
     * table 3. Input values are:
     *
     * <ul>
     *   <li>Starting longitude:  λ₁ = any</li>
     *   <li>Starting latitude:   φ₁ = -30.12345°</li>
     *   <li>Ending   longitude:  λ₂ = λ₁ + 0.00005°</li>
     *   <li>Ending   latitude:   φ₂ = -30.12344°</li>
     * </ul>
     *
     * About 5 intermediate values are published in Karney table 3 and copied in this method body.
     * Those values are verified if {@link GeodesicsOnEllipsoid#STORE_LOCAL_VARIABLES} is {@code true}.
     *
     * <p><b>Source:</b> Karney (2013), <u>Algorithms for geodesics</u> table 3.</p>
     */
    @Test
    public void testComputeShortDistance() {
        createTracked();
        verifyParametersForWGS84();
        testedEarth.setStartGeographicPoint(-30.12345, 2);
        testedEarth.setEndGeographicPoint  (-30.12344, 2.00005);
        /*
         * The following method invocation causes calculation of all intermediate values.
         * Some intermediate values are verified by `assertValueEquals` statements below.
         * If some values are wrong, it is easier to check correctness with step-by-step
         * debugging of `computeDistance()` method while keeping table 3 of Karney aside.
         *
         *   β₁  —  reduced latitude of starting point.
         *   β₂  —  reduced latitude of ending point.
         */
        final double distance = testedEarth.getGeodesicDistance();
        assertValueEquals("β₁", 0, -30.03999083821, 1E-11, true);
        assertValueEquals("β₂", 0, -30.03998085491, 1E-11, true);
        assertValueEquals("ωb", 0,   0.99748847744, 1E-11, false);
        assertValueEquals("Δω", 0,   0.00005012589, 1E-11, true);
        assertValueEquals("Δσ", 0,   0.00004452641, 1E-11, true);
        assertValueEquals("α₁", 1,  77.04353354237, 1E-8,  true);
        assertValueEquals("α₂", 1,  77.04350844913, 1E-8,  true);
        /*
         * Final result. Values are slightly different than the values published
         * in Karney table 3 because GeodeticCalculator has done an iteration.
         */
        assertEquals(77.04353354237, testedEarth.getStartingAzimuth(), 1E-8, "α₁");     // Last 3 digits differ.
        assertEquals(77.04350844913, testedEarth.getEndingAzimuth(),   1E-8, "α₂");
        assertEquals(4.944208,       distance,                         1E-6, "Δs");
    }

    /**
     * Tests computing a shorter distance than {@link #testComputeShortDistance()}.
     * This is based on the empirical observation that for distances short enough,
     * the {@literal α₁ -= dα₁} calculation leaves α₁ unchanged when {@literal dα₁ ≪ α₁}.
     * {@link GeodesicsOnEllipsoid} shall detect this situation and stop iteration.
     * This tests verify that {@link GeodeticException} is not thrown.
     *
     * @throws GeodeticException if the {@literal dα₁ ≪ α₁} check did not worked.
     */
    @Test
    public void testComputeShorterDistance() throws GeodeticException {
        final GeodeticCalculator c = create(false);
        c.setStartGeographicPoint(-0.000014, -29.841548);
        c.setEndGeographicPoint  (-0.000014, -29.841319);
        assertEquals(25.49, c.getGeodesicDistance(), 0.01);
    }

    /**
     * Result of Karney table 4, used as input in Karney table 5. We need to truncated that intermediate result
     * to the same number of digits than Karney in order to get the numbers published in table 5 and 6.
     * This value is used in {@link #testComputeNearlyAntipodal()} only.
     */
    private static final double TRUNCATED_α1 = 161.914;

    /**
     * Tests solving the inverse geodesic problem by a call to {@link GeodesicsOnEllipsoid#computeDistance()} for nearly
     * antipodal points. The data used by this test are provided by the example published in Karney (2013) tables 4 to 6.
     * Input values are:
     *
     * <ul>
     *   <li>Starting longitude:  λ₁  =  any</li>
     *   <li>Starting latitude:   φ₁  =  -30°</li>
     *   <li>Ending   longitude:  λ₂  =  λ₁ + 179.8°</li>
     *   <li>Ending   latitude:   φ₂  =  29.9°</li>
     * </ul>
     *
     * Some intermediate values are published in Karney tables 4 to 6 and copied in this method body.
     * Those values are verified if {@link GeodesicsOnEllipsoid#STORE_LOCAL_VARIABLES} is {@code true}.
     *
     * <p><b>Source:</b> Karney (2013), <u>Algorithms for geodesics</u> tables 4, 5 and 6.</p>
     */
    @Test
    public void testComputeNearlyAntipodal() {
        createTracked();
        verifyParametersForWGS84();
        testedEarth.setStartGeographicPoint(-30, 0);
        testedEarth.setEndGeographicPoint(29.9, 179.8);
        /*
         * The following method invocation causes calculation of all intermediate values.
         * Values β₁ and β₂ are kept constant during all iterations.
         * Other values are given in Karney table 4.
         */
        final double distance = testedEarth.getGeodesicDistance();
        assertValueEquals("β₁", 0, -29.91674771324, 1E-11, true);
        assertValueEquals("β₂", 0,  29.81691642189, 1E-11, true);
        assertValueEquals("x",  0,  -0.382344,      1E-6, false);
        assertValueEquals("y",  0,  -0.220189,      1E-6, false);
        assertValueEquals("μ",  0,   0.231633,      1E-6, false);
        assertValueEquals("α₁", 1,   TRUNCATED_α1,  1E-3,  true);               // Initial value before iteration.
        /*
         * Following values are updated during iterations. Note that in order to get the same values as the ones
         * published in Karney table 5, we need to truncate the α₁ initial value to the same number of digits than
         * Karney. This is done automatically if GeodesicsOnEllipsoid.STORE_LOCAL_VARIABLES is true.
         */
        assertValueEquals("α₀",    1,    15.60939746414,    1E-11, true);
        assertValueEquals("σ₁",    1,  -148.81253566596,    1E-11, true);
        assertValueEquals("ω₁",    1,  -170.74896696128,    1E-11, true);
        assertValueEquals("α₂",    1,    18.06728796231,    1E-11, true);
        assertValueEquals("σ₂",    1,    31.08244976895,    1E-11, true);
        assertValueEquals("ω₂",    1,     9.21345761110,    1E-11, true);
        assertValueEquals("k²",    1,     0.00625153791662, 1E-14, false);
        assertValueEquals("ε",     1,     0.00155801826780, 1E-14, false);
        assertValueEquals("λ₁",    1,  -170.61483552458,    1E-11, true);
        assertValueEquals("λ₂",    1,     9.18542009839,    1E-11, true);
        assertValueEquals("Δλ",    1,   179.80025562297,    1E-11, true);
        assertValueEquals("δλ",    1,     0.00025562297,    1E-11, true);
        assertValueEquals("J(σ₁)", 1,    -0.00948040927640, 1E-14, false);
        assertValueEquals("J(σ₂)", 1,     0.00031349128630, 1E-14, false);
        assertValueEquals("Δm",    1, 57288.000110,         1E-6,  false);
        assertValueEquals("dλ/dα", 1,     0.01088931716115, 1E-14, false);
        assertValueEquals("δσ₁",   1,    -0.02347465519,    1E-11, true);
        assertValueEquals("α₁",    2,   161.89052534481,    1E-11, true);
        /*
         * After second iteration.
         */
        assertValueEquals("δλ",    2,     0.00000000663,    1E-11, true);
        assertValueEquals("α₁",    3,   161.89052473633,    1E-11, true);
        /*
         * After third iteration.
         */
        assertValueEquals("α₀",    3,    15.62947966537,    1E-11, true);
        assertValueEquals("σ₁",    3,  -148.80913691776,    1E-11, true);
        assertValueEquals("ω₁",    3,  -170.73634378066,    1E-11, true);
        assertValueEquals("α₂",    3,    18.09073724574,    1E-11, true);
        assertValueEquals("σ₂",    3,    31.08583447040,    1E-11, true);
        assertValueEquals("ω₂",    3,     9.22602862110,    1E-11, true);
        assertValueEquals("s₁",    3,  -16539979.064227,    1E-6, false);
        assertValueEquals("s₂",    3,    3449853.763383,    1E-6, false);
        assertValueEquals("Δs",    3,   19989832.827610,    1E-6, false);
        assertValueEquals("λ₁",    3,  -170.60204712148,    1E-11, true);
        assertValueEquals("λ₂",    3,     9.19795287852,    1E-11, true);
        assertValueEquals("Δλ",    3,   179.80000000000,    1E-11, true);
        /*
         * Final result:
         */
        assertEquals(161.89052473633, testedEarth.getStartingAzimuth(), 1E-11, "α₁");
        assertEquals( 18.09073724574, testedEarth.getEndingAzimuth(),   1E-11, "α₂");
        assertEquals(19989832.827610, distance, 1E-6, "Δs");
    }

    /**
     * Same test as the one defined in parent class, but with expected results modified for ellipsoidal formulas.
     * Input points are from <a href="https://en.wikipedia.org/wiki/Great-circle_navigation#Example">Wikipedia</a>.
     * Outputs were computed with GeographicLib.
     */
    @Test
    @Override
    public void testGeodesicDistanceAndAzimuths() {
        final GeodeticCalculator c = create(false);
        c.setStartGeographicPoint(-33.0, -71.6);            // Valparaíso
        c.setEndGeographicPoint  ( 31.4, 121.8);            // Shanghai
        /*
         *                   α₁        α₂       distance
         *   Spherical:     -94.41°   -78.42°   18743 km
         *   Ellipsoidal:   -94.82°   -78.29°   18752 km
         */
        assertEquals(Units.METRE,  c.getDistanceUnit());
        assertEquals(-94.82071749, c.getStartingAzimuth(), 1E-8, "α₁");
        assertEquals(-78.28609385, c.getEndingAzimuth(),   1E-8, "α₂");
        assertEquals(18752.493521, c.getGeodesicDistance() / 1000, 1E-6, "distance");
        assertPositionEquals(31.4, 121.8, c.getEndPoint(), 1E-12);    // Should be the specified value.
        /*
         * Keep start point unchanged, but set above azimuth and distance.
         * Verify that we get the Shanghai coordinates.
         */
        c.setStartingAzimuth(-94.82);
        c.setGeodesicDistance(18752494);
        assertEquals(-94.82,       c.getStartingAzimuth(), 1E-12, "α₁");    // Should be the specified value.
        assertEquals(-78.28678389, c.getEndingAzimuth(),   1E-8,  "α₂");
        assertEquals(18752.494,    c.getGeodesicDistance() / 1000, "Δs");   // Should be the specified value.
        assertPositionEquals(31.4, 121.8, c.getEndPoint(), 0.0002);
    }

    /**
     * Returns a value {@literal > 1} if iteration stopped before to reach the desired accuracy because of limitation
     * in {@code double} precision. This problem may happen in the {@link GeodesicsOnEllipsoid#computeDistance()}
     * method when {@literal dα₁ ≪ α₁}. If locale variable storage is enabled, this situation is flagged by the
     * {@code "dα₁ ≪ α₁"} key. Otherwise we conservatively assume that this situation occurred.
     */
    @Override
    double relaxIfConfirmed(final KnownProblem potentialProblem) {
        if (potentialProblem == KnownProblem.ITERATION_REACHED_PRECISION_LIMIT) {
            if (GeodesicsOnEllipsoid.STORE_LOCAL_VARIABLES) {
                if (testedEarth.iterationReachedPrecisionLimit) {
                    return 2;
                }
            } else {
                // No information about whether the problem really occurred.
                return 2;
            }
        }
        return super.relaxIfConfirmed(potentialProblem);
    }

    /**
     * Tests {@link GeodesicsOnEllipsoid#getRhumblineLength()} using the example 1 given in Bennett (1996) appendix.
     */
    @Test
    @Override
    public void testRhumblineLength() {
        createTracked();
        verifyParametersForWGS84();
        /*
         * Bennett (1996) example 1. For comparing with values given by Bennett:
         *
         *   M = Ψ * 10800/PI.
         *   m = m * semiMajor / 1852
         */
        testedEarth.setStartGeographicPoint(10+18.4/60,  37+41.7/60);
        testedEarth.setEndGeographicPoint  (53+29.5/60, 113+17.1/60);
        final double distance = testedEarth.getRhumblineLength();
        final double scale = testedEarth.semiMajorAxis / NAUTICAL_MILE;
        assertValueEquals("Δλ", 0, 75+35.4 / 60,         1E-11, true);
        assertValueEquals("ΔΨ", 0, 3176.89 / (10800/PI), 1E-5, false);
        assertValueEquals("m₁", 0,  615.43 / scale,      1E-6, false);
        assertValueEquals("m₂", 0, 3201.59 / scale,      1E-6, false);
        assertValueEquals("Δm", 0, 2586.16 / scale,      1E-6, false);
        assertEquals(54.99008056, testedEarth.getConstantAzimuth(), 1E-8, "azimuth");
        assertEquals(4507.7 * NAUTICAL_MILE, distance, 0.05 * NAUTICAL_MILE, "distance");   // From Bennett (1996)
        assertEquals(8348285.202, distance, Formulas.LINEAR_TOLERANCE, "distance");         // From Karney's online calculator.
    }

    /**
     * Tests {@link GeodesicsOnEllipsoid#getRhumblineLength()} using the example 3 given in Bennett (1996) appendix.
     */
    @Test
    @Override
    public void testRhumblineNearlyEquatorial() {
        createTracked();
        verifyParametersForWGS84();
        testedEarth.setStartGeographicPoint(-52-47.8/60, -97-31.6/60);
        testedEarth.setEndGeographicPoint  (-53-10.8/60, -41-34.6/60);
        final double distance = testedEarth.getRhumblineLength();
        assertValueEquals("Δλ", 0,  55+57.0 / 60,         1E-11, true);
        assertValueEquals("ΔΨ", 0,   -38.12 / (10800/PI), 1E-5, false);
//      assertValueEquals("C",  0,  90.6505,              1E-4, true);
        assertEquals(90.65049570, testedEarth.getConstantAzimuth(), 1E-8, "azimuth");
        assertEquals(2028.9 * NAUTICAL_MILE, distance, 0.05 * NAUTICAL_MILE, "distance");   // From Bennett (1996)
        assertEquals(3757550.656, distance, Formulas.LINEAR_TOLERANCE, "distance");         // From Karney's online calculator.
    }

    /**
     * Tests {@link GeodesicsOnEllipsoid#getRhumblineLength()} using the example 4 given in Bennett (1996) appendix.
     */
    @Test
    @Override
    public void testRhumblineEquatorial() {
        createTracked();
        verifyParametersForWGS84();
        testedEarth.setStartGeographicPoint(48+45.0/60, -61-31.1/60);
        testedEarth.setEndGeographicPoint  (48+45.0/60,   5+13.2/60);
        final double distance = testedEarth.getRhumblineLength();
        assertValueEquals("Δλ", 0, 4004.3 / 60, 1E-11, true);
        assertEquals(90.00000000, testedEarth.getConstantAzimuth(), 1E-8, "azimuth");
        assertEquals(2649.9 * NAUTICAL_MILE, distance, 0.1 * NAUTICAL_MILE, "distance");    // From Bennett (1996)
        assertEquals(4907757.375, distance, Formulas.LINEAR_TOLERANCE, "distance");         // From Karney's online calculator.
    }
}
