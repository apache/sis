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
package org.apache.sis.referencing.operation.projection;

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.transform.ContextualParameters;

import org.junit.Assert;
import org.junit.Test;

import static java.lang.Math.sqrt;


/**
 * Tests {@link ObliqueStereographic} projection.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class ObliqueStereographicTest extends MapProjectionTestCase {
    /**
     * Parameter values provided by the <a href = http://www.iogp.org/pubs/373-07-2.pdf>EPSG guide</a>
     * for testing {@link ObliqueStereographic} transform conformity.
     *
     * @see ContextualParameters#getMatrix(boolean) where boolean is true for value n.
     * @see ContextualParameters#getMatrix(boolean) where boolean is false for k0, a, FE, FN and R.
     * @see Initializer#radiusOfConformalSphere(double) for value R
     *
     */
    private final static double eSQUARED = 0.08169683 * 0.08169683,     // Excentricity squared.
                                φ0       = 0.910296727,                 // Latitude of natural origin (rad)

                                //-- Some attributs are considered as linear and put into normalize matrix and apply before transform
                                n        = sqrt(1 + (eSQUARED * Math.pow(Math.cos(φ0), 4)) / (1 - eSQUARED)),

                                //-- Some attributs are considered as linear and put into denormalize matrix and apply just after
                                k0       = 0.9999079,
                                a        = 6377397.155,
                                FE       = 155000.00,
                                FN       = 463000.00,
                                R        = 6382644.571 / a;

    /**
     * Tested {@link MathTransform} projection.
     */
    private final NormalizedProjection ObliqueStereographic;

    /**
     * Buid tested {@link ObliqueStereographic} {@link MathTransform}.
     */
    public ObliqueStereographicTest() {
        final OperationMethod op = new org.apache.sis.internal.referencing.provider.ObliqueStereographic();

        final ParameterValueGroup p = op.getParameters().createValue();

        //-- implicit names from OGC.
        p.parameter("semi_major").setValue(6377397.155);
        p.parameter("inverse_flattening").setValue(299.15281);

        //-- Name parameters from Epsg registry
        p.parameter("Latitude of natural origin").setValue(52.156160556);
        p.parameter("Longitude of natural origin").setValue(5.387638889);
        p.parameter("Scale factor at natural origin").setValue(0.9999079);
        p.parameter("False easting").setValue(155000.00);
        p.parameter("False northing").setValue(463000.00);

        ObliqueStereographic = new ObliqueStereographic(op, (Parameters) p);
    }

    /**
     * {@link MathTransform#transform(org.opengis.geometry.DirectPosition, org.opengis.geometry.DirectPosition) }
     * test with expected values from
     * <a href = http://www.iogp.org/pubs/373-07-2.pdf> EPSG guide</a>
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testEPSGTransform() throws FactoryException, TransformException {

        final double[] srcPts = new double[]{6, 53}; //-- deg
        srcPts[0] = Math.toRadians(srcPts[0] - 5.387638889) ;
        srcPts[1] = Math.toRadians(srcPts[1]);

        final double[] dstPts = new double[2];

        srcPts[0] = srcPts[0] * n;

        ObliqueStereographic.transform(srcPts, 0, dstPts, 0, 1);

        final double destE = dstPts[0] * k0 * a * 2 * R + FE;
        final double destN = dstPts[1] * k0 * a * 2 * R + FN;

        Assert.assertEquals("destination East coordinate",  196105.283, destE, Formulas.LINEAR_TOLERANCE);
        Assert.assertEquals("destination North coordinate", 557057.739, destN, Formulas.LINEAR_TOLERANCE);
    }


   /**
     * Test method {@link ObliqueStereographic#inverseTransform(double[], int, double[], int)}
     * test with expected values from
     * <a href = http://www.iogp.org/pubs/373-07-2.pdf> EPSG guide</a>
     *
     * @throws org.apache.sis.referencing.operation.projection.ProjectionException
     */
    @Test
    public void testEPSGinvertTransform() throws ProjectionException {

        double srcEast  = 196105.28;
        double srcNorth = 557057.74;

        srcEast  -= FE;
        srcNorth -= FN;
        srcEast  /= k0;
        srcNorth /= k0;
        srcEast  /= a;
        srcNorth /= a;
        srcEast  /= (2 * R);
        srcNorth /= (2 * R);

        //-- tcheck transform
        final double[] srcPts = new double[]{srcEast, srcNorth}; //-- meter

        final double[] dstPts = new double[2];
        ObliqueStereographic.inverseTransform(srcPts, 0, dstPts, 0);

        final double λO = 0.094032038;

        double destλ = dstPts[0] / n + λO;
        double destφ = dstPts[1];

        destλ = Math.toDegrees(destλ);
        destφ = Math.toDegrees(destφ);

        Assert.assertEquals("destination East coordinate",  6, destλ, Formulas.ANGULAR_TOLERANCE);
        Assert.assertEquals("destination North coordinate", 53, destφ, Formulas.ANGULAR_TOLERANCE);
    }

    /**
     * Tests the <cite>Oblique Stereographic</cite> case (EPSG:9809).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testObliqueStereographic()
     */
    @Test
    public void testGeoapi() throws FactoryException, TransformException {
        createGeoApiTest(new org.apache.sis.internal.referencing.provider.ObliqueStereographic()).testObliqueStereographic();
    }
}
