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

import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.xml.bind.JAXBException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.test.Validators;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.GeodeticObjectBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
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
    DefaultGeographicCRSTest.class,
    org.apache.sis.referencing.operation.DefaultConversionTest.class
})
public final strictfp class DefaultProjectedCRSTest extends XMLTestCase {
    /**
     * An XML file in this package containing a projected CRS definition.
     */
    private static final String XML_FILE = "NTF.xml";

    /**
     * Creates the "NTF (Paris) / Lambert zone II" CRS.
     *
     * @see HardCodedCRS#NTF
     */
    private static ProjectedCRS create() throws FactoryException {
        return new GeodeticObjectBuilder()
                .setConversionMethod("Lambert Conic Conformal (1SP)")
                .setConversionName("Lambert zone II")
                .setParameter("Latitude of natural origin",             52, NonSI.GRADE)
                .setParameter("Scale factor at natural origin", 0.99987742, Unit.ONE)
                .setParameter("False easting",                      600000, SI.METRE)
                .setParameter("False northing",                    2200000, SI.METRE)
                .setCodeSpace(Citations.EPSG, Constants.EPSG)
                .addName("NTF (Paris) / Lambert zone II")
                .addIdentifier("27572")
                .createProjectedCRS(HardCodedCRS.NTF, HardCodedCS.PROJECTED);
    }

    /**
     * Tests WKT 1 formatting.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testWKT1() throws FactoryException {
        final ProjectedCRS crs = create();
        Validators.validate(crs);   // Opportunist check.
        assertWktEquals(Convention.WKT1,
                "PROJCS[“NTF (Paris) / Lambert zone II”,\n" +
                "  GEOGCS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PRIMEM[“Paris”, 2.33722917],\n" +                    // Note the conversion from 2.5969213 grades.
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  PROJECTION[“Lambert_Conformal_Conic_1SP”, AUTHORITY[“EPSG”, “9801”]],\n" +
                "  PARAMETER[“latitude_of_origin”, 46.8],\n" +              // Note the conversion from 52 grades.
                "  PARAMETER[“central_meridian”, 0.0],\n" +
                "  PARAMETER[“scale_factor”, 0.99987742],\n" +
                "  PARAMETER[“false_easting”, 600000.0],\n" +
                "  PARAMETER[“false_northing”, 2200000.0],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Easting”, EAST],\n" +
                "  AXIS[“Northing”, NORTH],\n" +
                "  AUTHORITY[“EPSG”, “27572”]]",
                crs);
    }

    /**
     * Tests WKT 2 formatting.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    @DependsOnMethod("testWKT1")
    public void testWKT2() throws FactoryException {
        final ProjectedCRS crs = create();
        assertWktEquals(Convention.WKT2,
                "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BaseGeodCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Francaise”,\n" +
                "      Ellipsoid[“NTF”, 6378249.2, 293.4660212936269, LengthUnit[“metre”, 1]]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213, AngleUnit[“grade”, 0.015707963267948967]]],\n" +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”, Id[“EPSG”, 9801, Citation[“IOGP”]]],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0, AngleUnit[“grade”, 0.015707963267948967], Id[“EPSG”, 8801]],\n" +
                "    Parameter[“Longitude of natural origin”, 0.0, AngleUnit[“degree”, 0.017453292519943295], Id[“EPSG”, 8802]],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.99987742, ScaleUnit[“unity”, 1], Id[“EPSG”, 8805]],\n" +
                "    Parameter[“False easting”, 600000.0, LengthUnit[“metre”, 1], Id[“EPSG”, 8806]],\n" +
                "    Parameter[“False northing”, 2200000.0, LengthUnit[“metre”, 1], Id[“EPSG”, 8807]]],\n" +
                "  CS[“Cartesian”, 2],\n" +
                "    Axis[“Easting (E)”, east, Order[1]],\n" +
                "    Axis[“Northing (N)”, north, Order[2]],\n" +
                "    LengthUnit[“metre”, 1],\n" +
                "  Id[“EPSG”, 27572, Citation[“IOGP”], URI[“urn:ogc:def:crs:EPSG::27572”]]]",
                crs);
    }

    /**
     * Tests (un)marshalling of a projected coordinate reference system.
     *
     * @throws FactoryException if the CRS creation failed.
     * @throws JAXBException If an error occurred during (un)marshalling.
     */
    @Test
    @org.junit.Ignore("Still missing some JAXB annotations.")
    public void testXML() throws FactoryException, JAXBException {
        final DefaultProjectedCRS crs = unmarshalFile(DefaultProjectedCRS.class, XML_FILE);
        Validators.validate(crs);
        assertEquals("scope", "Large and medium scale topographic mapping and engineering survey.", crs.getScope().toString());
        /*
         * Marshal and compare with the original file.
         */
        assertMarshalEqualsFile(XML_FILE, crs, "xmlns:*", "xsi:schemaLocation");
    }
}
