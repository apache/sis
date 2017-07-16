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
package org.apache.sis.storage.gdal;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link Proj4Parser}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class Proj4ParserTest extends TestCase {
    /**
     * The factory needed for calls to {@code Proj4Parser.method(…)}.
     */
    private final DefaultCoordinateOperationFactory opFactory;

    /**
     * Creates a new test.
     */
    public Proj4ParserTest() {
        opFactory = DefaultFactories.forBuildin(CoordinateOperationFactory.class, DefaultCoordinateOperationFactory.class);
    }

    /**
     * Tests parsing of a Mercator projection definition string.
     *
     * @throws FactoryException if the parsing failed.
     */
    @Test
    public void testMercator() throws FactoryException {
        final Proj4Parser parser = new Proj4Parser("+init=epsg:3395 +proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0"
                      + " +datum=WGS84 +units=m +no_defs +ellps=WGS84 +towgs84=0,0,0");

        assertInstanceOf("method", Mercator1SP.class, parser.method(opFactory));
        final ParameterValueGroup pg = parser.parameters();
        assertEquals("scale_factor", pg.parameter("scale_factor").getValue(), 1.0);
    }
}
