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

import java.util.Map;
import java.util.HashMap;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.parameter.DefaultParameterDescriptorTest;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.DefaultGeocentricCRS;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultTransformation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    AbstractSingleOperationTest.class,
    org.apache.sis.referencing.crs.DefaultGeocentricCRSTest.class
})
public final strictfp class DefaultTransformationTest extends TestCase {
    /**
     * Creates a geocentric CRS using the given datum.
     */
    private static DefaultGeocentricCRS createCRS(final String name, final GeodeticDatum datum) {
        Map<String,?> properties = IdentifiedObjects.getProperties(datum, DefaultGeocentricCRS.IDENTIFIERS_KEY);
        if (name != null) {
            final Map<String,Object> copy = new HashMap<String,Object>(properties);
            copy.put(DefaultGeocentricCRS.NAME_KEY, name);
            properties = copy;
        }
        return new DefaultGeocentricCRS(properties, datum, HardCodedCS.GEOCENTRIC);
    }

    /**
     * Creates a “Tokyo to JGD2000 (GSI)” transformation.
     */
    static DefaultTransformation createGeocentricTranslation() {
        /*
         * The following code fills the parameter values AND creates itself the MathTransform instance
         * (indirectly, through the matrix). The later step is normally not our business, since we are
         * supposed to only fill the parameter values and let MathTransformFactory creates the transform
         * from the parameters. But we don't do the normal steps here because this class is a unit test:
         * we want to test DefaultTransformation in isolation of MathTransformFactory.
         */
        final Matrix4 translation = new Matrix4();
        final OperationMethod method = DefaultOperationMethodTest.create(
                "Geocentric translations", "1031", "EPSG guidance note #7-2", 3,
                DefaultParameterDescriptorTest.createEPSG("X-axis translation", (short) 8605),
                DefaultParameterDescriptorTest.createEPSG("Y-axis translation", (short) 8606),
                DefaultParameterDescriptorTest.createEPSG("Z-axis translation", (short) 8607));
        final ParameterValueGroup pg = method.getParameters().createValue();
        pg.parameter("X-axis translation").setValue(translation.m02 = -146.414);
        pg.parameter("Y-axis translation").setValue(translation.m12 =  507.337);
        pg.parameter("Z-axis translation").setValue(translation.m22 =  680.507);
        /*
         * In theory we should not need to provide the parameters explicitly to the constructor since
         * we are supposed to be able to find them from the MathTransform. But in this simple test we
         * did not bothered to define a specialized MathTransform class for our case. So we will help
         * a little bit DefaultTransformation by telling it the parameters that we used.
         */
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        properties.put(DefaultTransformation.NAME_KEY, "Tokyo to JGD2000 (GSI)");
        properties.put(ReferencingServices.PARAMETERS_KEY, pg);
        return new DefaultTransformation(properties,
                createCRS(null,      HardCodedDatum.TOKYO),     // SourceCRS
                createCRS("JGD2000", HardCodedDatum.JGD2000),   // TargetCRS
                null,                                           // InterpolationCRS
                method,
                MathTransforms.linear(translation));
    }

    /**
     * Asserts that at least some of the properties of the given {@code op} instance have the expected values
     * for an instance created by {@link #createGeocentricTranslation()}.
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    private static void verifyProperties(final DefaultTransformation op) {
        assertEquals("name",       "Tokyo to JGD2000 (GSI)",  op.getName().getCode());
        assertEquals("sourceCRS",  "Tokyo 1918",              op.getSourceCRS().getName().getCode());
        assertEquals("targetCRS",  "JGD2000",                 op.getTargetCRS().getName().getCode());
        assertEquals("method",     "Geocentric translations", op.getMethod().getName().getCode());
        assertEquals("parameters", "Geocentric translations", op.getParameterDescriptors().getName().getCode());

        final ParameterValueGroup parameters = op.getParameterValues();
        final ParameterValue<?>[] values = parameters.values().toArray(new ParameterValue<?>[3]);
        assertEquals("parameters",    "Geocentric translations", parameters.getDescriptor().getName().getCode());
        assertEquals("parameters[0]", "X-axis translation",      values[0] .getDescriptor().getName().getCode());
        assertEquals("parameters[1]", "Y-axis translation",      values[1] .getDescriptor().getName().getCode());
        assertEquals("parameters[2]", "Z-axis translation",      values[2] .getDescriptor().getName().getCode());
        assertEquals("parameters[0]", -146.414, values[0].doubleValue(), STRICT);
        assertEquals("parameters[1]",  507.337, values[1].doubleValue(), STRICT);
        assertEquals("parameters[2]",  680.507, values[2].doubleValue(), STRICT);
        assertEquals(3, values.length);

        final Matrix m = MathTransforms.getMatrix(op.getMathTransform());
        assertNotNull("transform", m);
        for (int j=m.getNumRow(); --j >= 0;) {
            for (int i=m.getNumCol(); --i >= 0;) {
                double expected = (i == j) ? 1 : 0;
                if (i == 2) switch (j) {
                    case 0: expected = -146.414; break;
                    case 1: expected =  507.337; break;
                    case 2: expected =  680.507; break;
                }
                assertEquals(expected, m.getElement(j,i), STRICT);
            }
        }
    }

    /**
     * Tests construction.
     */
    @Test
    public void testConstruction() {
        verifyProperties(createGeocentricTranslation());
    }

    /**
     * Tests WKT formatting.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testWKT() {
        final DefaultTransformation op = createGeocentricTranslation();
        assertWktEquals(Convention.WKT2,
                "COORDINATEOPERATION[“Tokyo to JGD2000 (GSI)”,\n" +
                "  SOURCECRS[GEODCRS[“Tokyo 1918”,\n" +
                "    DATUM[“Tokyo 1918”,\n" +
                "      ELLIPSOID[“Bessel 1841”, 6377397.155, 299.1528128, LENGTHUNIT[“metre”, 1]]],\n" +
                "      PRIMEM[“Greenwich”, 0.0, ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "    CS[Cartesian, 3],\n" +
                "      AXIS[“(X)”, geocentricX, ORDER[1]],\n" +
                "      AXIS[“(Y)”, geocentricY, ORDER[2]],\n" +
                "      AXIS[“(Z)”, geocentricZ, ORDER[3]],\n" +
                "      LENGTHUNIT[“metre”, 1]]],\n" +
                "  TARGETCRS[GEODCRS[“JGD2000”,\n" +
                "    DATUM[“Japanese Geodetic Datum 2000”,\n" +
                "      ELLIPSOID[“GRS 1980”, 6378137.0, 298.257222101, LENGTHUNIT[“metre”, 1]]],\n" +
                "      PRIMEM[“Greenwich”, 0.0, ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "    CS[Cartesian, 3],\n" +
                "      AXIS[“(X)”, geocentricX, ORDER[1]],\n" +
                "      AXIS[“(Y)”, geocentricY, ORDER[2]],\n" +
                "      AXIS[“(Z)”, geocentricZ, ORDER[3]],\n" +
                "      LENGTHUNIT[“metre”, 1]]],\n" +
                "  METHOD[“Geocentric translations”, ID[“EPSG”, 1031]],\n" +
                "    PARAMETER[“X-axis translation”, -146.414, ID[“EPSG”, 8605]],\n" +
                "    PARAMETER[“Y-axis translation”, 507.337, ID[“EPSG”, 8606]],\n" +
                "    PARAMETER[“Z-axis translation”, 680.507, ID[“EPSG”, 8607]]]", op);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "CoordinateOperation[“Tokyo to JGD2000 (GSI)”,\n" +
                "  SourceCRS[GeodeticCRS[“Tokyo 1918”,\n" +
                "    Datum[“Tokyo 1918”,\n" +
                "      Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128]],\n" +
                "    CS[Cartesian, 3],\n" +
                "      Axis[“(X)”, geocentricX],\n" +
                "      Axis[“(Y)”, geocentricY],\n" +
                "      Axis[“(Z)”, geocentricZ],\n" +
                "      Unit[“metre”, 1]]],\n" +
                "  TargetCRS[GeodeticCRS[“JGD2000”,\n" +
                "    Datum[“Japanese Geodetic Datum 2000”,\n" +
                "      Ellipsoid[“GRS 1980”, 6378137.0, 298.257222101]],\n" +
                "    CS[Cartesian, 3],\n" +
                "      Axis[“(X)”, geocentricX],\n" +
                "      Axis[“(Y)”, geocentricY],\n" +
                "      Axis[“(Z)”, geocentricZ],\n" +
                "      Unit[“metre”, 1]]],\n" +
                "  Method[“Geocentric translations”],\n" +
                "    Parameter[“X-axis translation”, -146.414],\n" +
                "    Parameter[“Y-axis translation”, 507.337],\n" +
                "    Parameter[“Z-axis translation”, 680.507]]", op);
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testSerialization() {
        verifyProperties(assertSerializedEquals(createGeocentricTranslation()));
    }
}
