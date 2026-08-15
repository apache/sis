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
package org.apache.sis.referencing.dggs.s2;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.referencing.dggs.AbstractDggrsTest;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class S2Test extends AbstractDggrsTest {

    public S2Test() {
        super(new S2Dggrs());
    }

    /**
     * Check the antimeridian zone has a proper envelope
     */
    @Test
    public void testAntimeridian() throws TransformException {

        final S2Dggrs dggrs = new S2Dggrs();
        final Zone zone = dggrs.createCoder().decode("7cc");

        final GeographicExtent extent = zone.getGeographicExtent();
        final Polygon polygon = DiscreteGlobalGridSystems.toSISPolygon(extent);
        assertEquals("POLYGON ((-180.0 34.50852298766839, -180.0 22.619864948040426, -169.38034472384487 22.270575488008195, -169.38034472384487 34.04786296943431, -180.0 34.50852298766839))", polygon.asText());

        final Envelope envelope = zone.getEnvelope();
        assertEquals(-180, envelope.getMinimum(0), 0.0);
        assertEquals(-169.38034472384487, envelope.getMaximum(0), 0.0);
        assertEquals(22.270575488008195, envelope.getMinimum(1), 0.0);
        assertEquals(34.50852298766839, envelope.getMaximum(1), 0.0);

    }
}
