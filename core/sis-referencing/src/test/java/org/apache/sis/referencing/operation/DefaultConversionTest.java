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
import java.util.Collections;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.referencing.OperationMethods;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.parameter.DefaultParameterDescriptorTest;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultConversion}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    DefaultTransformationTest.class     // Because similar to DefaultConversion but simpler.
})
public final strictfp class DefaultConversionTest extends TestCase {
    /**
     * Tolerance threshold for strict floating point comparisons.
     */
    private static final double STRICT = 0;

    /**
     * The rotation from a CRS using the Paris prime meridian to a CRS using the Greenwich prime meridian,
     * in degrees. The definitive value is 2.5969213 grades.
     */
    private static final double OFFSET = 2.33722917;

    /**
     * Creates a CRS using the WGS84 datum, except for the prime meridian which is set to Paris.
     * This is not a CRS is real usage (the CRS with Paris prime meridian were using an older datum),
     * but this is convenient for testing a conversion consisting of only a longitude rotation.
     *
     * @param cs {@link HardCodedCS#GEODETIC_2D}, {@link HardCodedCS#GEODETIC_φλ} or other compatible coordinate system.
     */
    private static GeographicCRS createParisCRS(final EllipsoidalCS cs) {
        final Map<String, ?> name = Collections.singletonMap(GeographicCRS.NAME_KEY, HardCodedDatum.PARIS.getName());
        return new DefaultGeographicCRS(name, new DefaultGeodeticDatum(name,
                HardCodedDatum.WGS84.getEllipsoid(), HardCodedDatum.PARIS), cs);
    }

    /**
     * Creates a very simple conversion performing a longitude rotation on the WGS84 datum.
     * The source CRS shall use the Paris prime meridian and the target CRS the Greenwich
     * prime meridian.
     *
     * @param sourceCRS A CRS using the Paris prime meridian.
     * @param targetCRS A CRS using the Greenwich prime meridian.
     */
    private static DefaultConversion createLongitudeRotation(final GeographicCRS sourceCRS, final GeographicCRS targetCRS) {
        final Matrix rotation = Matrices.createDiagonal(
                targetCRS.getCoordinateSystem().getDimension() + 1,     // Number of rows.
                sourceCRS.getCoordinateSystem().getDimension() + 1);    // Number of columns.
        rotation.setElement(0, rotation.getNumCol() - 1, OFFSET);

        final OperationMethod method = DefaultOperationMethodTest.create(
                "Longitude rotation", "9601", "EPSG guidance note #7-2",
                sourceCRS.getCoordinateSystem().getDimension(),
                DefaultParameterDescriptorTest.createEPSG("Longitude offset", (short) 9601));
        final ParameterValueGroup pg = method.getParameters().createValue();
        pg.parameter("Longitude offset").setValue(OFFSET);

        final Map<String, Object> properties = new HashMap<>(4);
        properties.put(DefaultTransformation.NAME_KEY, "Paris to Greenwich");
        properties.put(OperationMethods.PARAMETERS_KEY, pg);
        return new DefaultConversion(properties, sourceCRS, targetCRS, null, method, MathTransforms.linear(rotation));
    }

    /**
     * Tests a simple two-dimensional conversion performing a longitude rotation on the WGS84 datum.
     */
    @Test
    public void testConstruction() {
        final DefaultConversion op = createLongitudeRotation(createParisCRS(HardCodedCS.GEODETIC_2D), HardCodedCRS.WGS84);
        assertEquals("name",       "Paris to Greenwich", op.getName().getCode());
        assertEquals("sourceCRS",  "Paris",              op.getSourceCRS().getName().getCode());
        assertEquals("targetCRS",  "WGS 84",             op.getTargetCRS().getName().getCode());
        assertEquals("method",     "Longitude rotation", op.getMethod().getName().getCode());
        assertEquals("parameters", "Longitude rotation", op.getParameterDescriptors().getName().getCode());

        final ParameterValueGroup parameters = op.getParameterValues();
        final ParameterValue<?>[] values = parameters.values().toArray(new ParameterValue<?>[1]);
        assertEquals("parameters",    "Longitude rotation", parameters.getDescriptor().getName().getCode());
        assertEquals("parameters[0]", "Longitude offset",    values[0].getDescriptor().getName().getCode());
        assertEquals("parameters[0]", OFFSET, values[0].doubleValue(), STRICT);

        final Matrix m = MathTransforms.getMatrix(op.getMathTransform());
        assertNotNull("transform", m);
        final int numCol = m.getNumCol();
        for (int j=m.getNumRow(); --j >= 0;) {
            for (int i=numCol; --i >= 0;) {
                assertEquals((j == 0 && i == numCol-1) ? OFFSET : (i == j) ? 1 : 0, m.getElement(j,i), STRICT);
            }
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testSerialization() {
        assertSerializedEquals(createLongitudeRotation(createParisCRS(HardCodedCS.GEODETIC_2D), HardCodedCRS.WGS84));
    }
}
