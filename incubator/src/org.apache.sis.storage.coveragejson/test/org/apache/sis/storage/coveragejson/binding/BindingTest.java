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
 * distributed under the License is distributed on anz "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.coveragejson.binding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.eclipse.yasson.YassonConfig;

// Test dependencies
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test coverage-json bindings.
 *
 * @author Johann Sorel (Geomatys)
 */
public class BindingTest {

    private static final JsonbConfig CONFIG = new YassonConfig().withFormatting(true);

    private static Jsonb jsonb;

    public BindingTest() {
    }

    public static String readResource(String path) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];
        try (InputStream in = BindingTest.class.getResourceAsStream(path)) {
            while ((nRead = in.read(data, 0, data.length)) != -1) {
              buffer.write(data, 0, nRead);
            }
        }
        buffer.flush();
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private void compare(String jsonpath, Object expected) throws IOException {
        String json = readResource(jsonpath);
        //reformat it the same way.
        JsonObject map = jsonb.fromJson(json, JsonObject.class);
        String formattedJson = jsonb.toJson(map);

        final Object candidate = jsonb.fromJson(json, expected.getClass());
        expected.equals(candidate);
        assertEquals(expected, candidate);
        assertEquals(formattedJson, jsonb.toJson(candidate));
    }

    @BeforeClass
    public static void beforeClass() {
        jsonb = JsonbBuilder.create(CONFIG);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        jsonb.close();
    }

    @Test
    public void testAxeBounds() throws Exception {
        final Axe expected = new Axe();
        expected.values = asList(20,21);
        expected.bounds = asList(19.5,20.5,20.5,21.5);
        compare("axe_bounds.json", expected);
    }

    @Test
    public void testAxePolygon() throws Exception {
        final Axe expected = new Axe();
        expected.dataType = "polygon";
        expected.coordinates = Arrays.asList("x","y");
        expected.values = Arrays.asList(
            Arrays.asList(
                Arrays.asList(
                    asList(100.0, 0.0),
                    asList(101.0, 0.0),
                    asList(101.0, 1.0),
                    asList(100.0, 1.0),
                    asList(100.0, 0.0)
                )
            )
        );
        compare("axe_polygon.json", expected);
    }

    @Test
    public void testAxeRegular() throws Exception {
        final Axe expected = new Axe();
        expected.start = 0.0;
        expected.stop = 5.0;
        expected.num = 6;
        compare("axe_regular.json", expected);
    }

    @Test
    public void testAxeTuples() throws Exception {
        final Axe expected = new Axe();
        expected.dataType = "tuple";
        expected.coordinates = Arrays.asList("t","x","y");
        expected.values = Arrays.asList(
                asList("2008-01-01T04:00:00Z",1,20),
                asList("2008-01-01T04:30:00Z",2,21)
        );
        compare("axe_tuples.json", expected);
    }

    @Test
    public void testCoverageVerticalProfile() throws Exception {
        final GeographicCRS geoCrs = new GeographicCRS();
        geoCrs.id = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

        final VerticalCRS zCrs = new VerticalCRS();
        //"cs":{"csAxes":[{"name":{"en":"Pressure"},"direction":"down","unit":{"symbol":"Pa"}}]}
        final Map<String,Object> axe = new LinkedHashMap<>();
        axe.put("name", Map.of("en", "Pressure"));
        axe.put("direction", "down");
        axe.put("unit", Map.of("symbol", "Pa"));
        final Map<String,Object> cs = new HashMap<>();
        cs.put("csAxes", Arrays.asList(axe));
        //zCrs.setAnyProperty("cs", cs); //TODO undefined attributes ignored

        final TemporalRS tCrs = new TemporalRS();
        tCrs.calendar = "Gregorian";

        final ReferenceSystemConnection georsc = new ReferenceSystemConnection();
        georsc.coordinates = Arrays.asList("x", "y");
        georsc.system = geoCrs;

        final ReferenceSystemConnection zrsc = new ReferenceSystemConnection();
        zrsc.coordinates = Arrays.asList("z");
        zrsc.system = zCrs;

        final ReferenceSystemConnection trsc = new ReferenceSystemConnection();
        trsc.coordinates = Arrays.asList("t");
        trsc.system = tCrs;

        final Domain domain = new Domain();
        domain.domainType = "VerticalProfile";
        domain.axes = new Axes();
        domain.axes.x = new Axe();
        domain.axes.y = new Axe();
        domain.axes.z = new Axe();
        domain.axes.t = new Axe();
        domain.axes.x.values = asList(-10.1);
        domain.axes.y.values = asList(-40.2);
        domain.axes.z.values = asList(5.4562, 8.9282);
        domain.axes.t.values = asList("2013-01-13T11:12:20Z");
        domain.referencing = Arrays.asList(georsc, zrsc, trsc);

        final Parameter PSAL = new Parameter();
        PSAL.description = new I18N("en", "The measured salinity, in practical salinity units (psu) of the sea water ");
        PSAL.unit = new Unit(null,null,"psu");
        PSAL.observedProperty = new ObservedProperty("http://vocab.nerc.ac.uk/standard_name/sea_water_salinity/", new I18N("en", "Sea Water Salinity"), null, null);

        final Parameter POTM = new Parameter();
        POTM.description = new I18N("en", "The potential temperature, in degrees celcius, of the sea water");
        POTM.unit = new Unit(null,null,"°C");
        POTM.observedProperty = new ObservedProperty("http://vocab.nerc.ac.uk/standard_name/sea_water_potential_temperature/", new I18N("en", "Sea Water Potential Temperature"), null, null);

        final Parameters parameters = new Parameters();
        parameters.setAnyProperty("PSAL", PSAL);
        parameters.setAnyProperty("POTM", POTM);

        final NdArray PSALr = new NdArray();
        PSALr.dataType ="float";
        PSALr.shape = new int[]{2};
        PSALr.axisNames = new String[]{"z"};
        PSALr.values = asList(43.9599, 43.9599);

        final NdArray POTMr = new NdArray();
        POTMr.dataType ="float";
        POTMr.shape = new int[]{2};
        POTMr.axisNames = new String[]{"z"};
        POTMr.values = asList(23.8, 23.7);

        final Ranges ranges = new Ranges();
        ranges.setAnyProperty("PSAL", PSALr);
        ranges.setAnyProperty("POTM", POTMr);

        final Coverage expected = new Coverage();
        expected.domain = domain;
        expected.parameters = parameters;
        expected.ranges = ranges;

        compare("coverage_vertical_profile_nocs.json", expected);
    }

    /**
     * Convert numeric values to BigDecimal for equality tests.
     */
    private static List<Object> asList(Object... array) {
        final List<Object> lst = new ArrayList<>(array.length);
        for (int i=0;i<array.length;i++) {
            if (array[i] instanceof Integer) {
                lst.add(BigDecimal.valueOf((Integer) array[i]));
            } else if (array[i] instanceof Double) {
                lst.add(BigDecimal.valueOf((Double) array[i]));
            } else {
                lst.add(array[i]);
            }
        }
        return lst;
    }
}
