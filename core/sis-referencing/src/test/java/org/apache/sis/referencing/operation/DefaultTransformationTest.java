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
import org.apache.sis.internal.referencing.OperationMethods;
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
     * Tolerance threshold for strict floating point comparisons.
     */
    private static final double STRICT = 0;

    /**
     * Creates a geocentric CRS using the given datum.
     */
    private static DefaultGeocentricCRS createCRS(final String name, final GeodeticDatum datum) {
        Map<String,?> properties = IdentifiedObjects.getProperties(datum, DefaultGeocentricCRS.IDENTIFIERS_KEY);
        if (name != null) {
            final Map<String,Object> copy = new HashMap<>(properties);
            copy.put(DefaultGeocentricCRS.NAME_KEY, name);
            properties = copy;
        }
        return new DefaultGeocentricCRS(properties, datum, HardCodedCS.GEOCENTRIC);
    }

    /**
     * Creates a “Tokyo to JGD2000 (GSI)” transformation.
     */
    private static DefaultTransformation createGeocentricTranslation() {
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
        final Map<String, Object> properties = new HashMap<>(4);
        properties.put(DefaultTransformation.NAME_KEY, "Tokyo to JGD2000 (GSI)");
        properties.put(OperationMethods.PARAMETERS_KEY, pg);
        return new DefaultTransformation(properties,
                createCRS(null,      HardCodedDatum.TOKYO),     // SourceCRS
                createCRS("JGD2000", HardCodedDatum.JGD2000),   // TargetCRS
                null,                                           // InterpolationCRS
                method,
                MathTransforms.linear(translation));
    }

    /**
     * Tests construction.
     */
    @Test
    public void testConstruction() {
        final DefaultTransformation op = createGeocentricTranslation();
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
     * Tests WKT formatting.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testWKT() {
        final DefaultTransformation op = createGeocentricTranslation();
        assertWktEquals(
                "CoordinateOperation[“Tokyo to JGD2000 (GSI)”,\n" +
                "  SourceCRS[GeodeticCRS[“Tokyo 1918”,\n" +
                "    Datum[“Tokyo 1918”,\n" +
                "      Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128, LengthUnit[“metre”, 1]]],\n" +
                "      PrimeMeridian[“Greenwich”, 0.0, AngleUnit[“degree”, 0.017453292519943295]],\n" +
                "    CS[“Cartesian”, 3],\n" +
                "      Axis[“(X)”, geocentricX, Order[1]],\n" +
                "      Axis[“(Y)”, geocentricY, Order[2]],\n" +
                "      Axis[“(Z)”, geocentricZ, Order[3]],\n" +
                "      LengthUnit[“metre”, 1]]],\n" +
                "  TargetCRS[GeodeticCRS[“JGD2000”,\n" +
                "    Datum[“Japanese Geodetic Datum 2000”,\n" +
                "      Ellipsoid[“GRS 1980”, 6378137.0, 298.257222101, LengthUnit[“metre”, 1]]],\n" +
                "      PrimeMeridian[“Greenwich”, 0.0, AngleUnit[“degree”, 0.017453292519943295]],\n" +
                "    CS[“Cartesian”, 3],\n" +
                "      Axis[“(X)”, geocentricX, Order[1]],\n" +
                "      Axis[“(Y)”, geocentricY, Order[2]],\n" +
                "      Axis[“(Z)”, geocentricZ, Order[3]],\n" +
                "      LengthUnit[“metre”, 1]]],\n" +
                "  Method[“Geocentric translations”, Id[“EPSG”, 1031, Citation[“IOGP”]]],\n" +
                "  Parameter[“X-axis translation”, -146.414, Id[“EPSG”, 8605]],\n" +
                "  Parameter[“Y-axis translation”, 507.337, Id[“EPSG”, 8606]],\n" +
                "  Parameter[“Z-axis translation”, 680.507, Id[“EPSG”, 8607]]]", op);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "CoordinateOperation[“Tokyo to JGD2000 (GSI)”,\n" +
                "  SourceCRS[GeodeticCRS[“Tokyo 1918”,\n" +
                "    Datum[“Tokyo 1918”,\n" +
                "      Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128]],\n" +
                "    CS[“Cartesian”, 3],\n" +
                "      Axis[“(X)”, geocentricX],\n" +
                "      Axis[“(Y)”, geocentricY],\n" +
                "      Axis[“(Z)”, geocentricZ],\n" +
                "      Unit[“metre”, 1]]],\n" +
                "  TargetCRS[GeodeticCRS[“JGD2000”,\n" +
                "    Datum[“Japanese Geodetic Datum 2000”,\n" +
                "      Ellipsoid[“GRS 1980”, 6378137.0, 298.257222101]],\n" +
                "    CS[“Cartesian”, 3],\n" +
                "      Axis[“(X)”, geocentricX],\n" +
                "      Axis[“(Y)”, geocentricY],\n" +
                "      Axis[“(Z)”, geocentricZ],\n" +
                "      Unit[“metre”, 1]]],\n" +
                "  Method[“Geocentric translations”],\n" +
                "  Parameter[“X-axis translation”, -146.414],\n" +
                "  Parameter[“Y-axis translation”, 507.337],\n" +
                "  Parameter[“Z-axis translation”, 680.507]]", op);
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testSerialization() {
        assertSerializedEquals(createGeocentricTranslation());
    }
}
