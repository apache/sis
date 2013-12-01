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
package org.apache.sis.referencing.datum;

import javax.measure.unit.NonSI;
import javax.xml.bind.JAXBException;
import org.junit.Test;

import static org.apache.sis.referencing.Assert.*;
import static org.apache.sis.test.mock.PrimeMeridianMock.GREENWICH;


/**
 * Tests the {@link DefaultPrimeMeridian} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class DefaultPrimeMeridianTest extends DatumTestCase {
    /**
     * Tests {@link DefaultPrimeMeridian#toWKT()}.
     */
    @Test
    public void testToWKT() {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(GREENWICH);
        assertWktEquals(pm, "PRIMEM[“Greenwich”, 0.0]");
    }

    /**
     * Tests unmarshalling.
     *
     * @throws JAXBException If an error occurred during unmarshalling.
     *
     * @see <a href="http://epsg-registry.org/export.htm?gml=urn:ogc:def:meridian:EPSG::8901">GML export of EPSG:8901</a>
     */
    @Test
    public void testUnmarshall() throws JAXBException {
        DefaultPrimeMeridian pm = unmarshall(DefaultPrimeMeridian.class, "Greenwich.xml");
        assertEquals("greenwichLongitude", pm.getGreenwichLongitude(), 0, 0);
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, pm.getAngularUnit());
    }
}
