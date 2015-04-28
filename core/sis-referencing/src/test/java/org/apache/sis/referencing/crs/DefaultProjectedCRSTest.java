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
package org.apache.sis.referencing.crs;

import java.util.Collections;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link DefaultProjectedCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    DefaultGeographicCRSTest.class
})
public final strictfp class DefaultProjectedCRSTest extends TestCase {
    /**
     * Creates the "NTF (Paris) / Lambert zone II" CRS.
     *
     * @see HardCodedCRS#NTF
     *
     * @todo Move this kind of code in a helper class.
     */
    private static DefaultProjectedCRS create() throws FactoryException {
        final MathTransformFactory mtFactory = DefaultFactories.forBuildin(MathTransformFactory.class);
        final ParameterValueGroup p = mtFactory.getDefaultParameters("Lambert Conic Conformal (1SP)");
        p.parameter("Latitude of natural origin").setValue(52, NonSI.GRADE);
        p.parameter("Scale factor at natural origin").setValue(0.99987742);
        p.parameter("False easting").setValue(600000, SI.METRE);
        p.parameter("False northing").setValue(2200000, SI.METRE);
        final MathTransform mt = mtFactory.createBaseToDerived(HardCodedCRS.NTF, p, HardCodedCS.PROJECTED);
        final DefaultConversion conversion = new DefaultConversion(Collections.singletonMap(
                DefaultConversion.NAME_KEY, "Lambert zone II"), mtFactory.getLastMethodUsed(), mt);
        return new DefaultProjectedCRS(Collections.singletonMap(DefaultConversion.NAME_KEY,
                "NTF (Paris) / Lambert zone II"), conversion, HardCodedCRS.NTF, HardCodedCS.PROJECTED);
    }

    /**
     * Tests WKT 1 formatting.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testWKT1() throws FactoryException {
        final DefaultProjectedCRS crs = create();
        assertWktEquals(Convention.WKT1,
                "PROJCS[“NTF (Paris) / Lambert zone II”,\n" +
                "  GEOGCS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "    PRIMEM[“Paris”, 2.33722917],\n" +
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  PROJECTION[“Lambert_Conformal_Conic_1SP”, AUTHORITY[“EPSG”, “9801”]],\n" +
                "  PARAMETER[“latitude_of_origin”, 46.8],\n" +
                "  PARAMETER[“scale_factor”, 0.99987742],\n" +
                "  PARAMETER[“false_easting”, 600000.0],\n" +
                "  PARAMETER[“false_northing”, 2200000.0],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Easting”, EAST],\n" +
                "  AXIS[“Northing”, NORTH]]",
                crs);
    }
}
