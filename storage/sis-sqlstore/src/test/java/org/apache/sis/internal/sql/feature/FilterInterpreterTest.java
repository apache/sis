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
package org.apache.sis.internal.sql.feature;

import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.SpatialOperator;


/**
 * Tests the formatting of {@link Filter} as a SQL {@code WHERE} statement body.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class FilterInterpreterTest extends TestCase {
    /**
     * The factory to use for creating the filter objects.
     */
    private final FilterFactory<Feature,Object,Object> FF;

    /**
     * Creates a new test.
     */
    public FilterInterpreterTest() {
        FF = DefaultFilterFactory.forFeatures();
    }

    /**
     * Creates a filter with a geometric objects and verifies that it is translated to the expected SQL fragment.
     */
    @Test
    public void testGeometricFilter() {
        final SpatialOperator<Feature> filter = FF.bbox(FF.property("Toto"),
                new GeneralEnvelope(new DefaultGeographicBoundingBox(-12.3, 2.1, 43.3, 51.7)));

        verifySQL(filter,
                "ST_Intersects(" +
                            "\"Toto\", " +
                            "ST_GeomFromText(" +
                                "'POLYGON ((-12.3 43.3, -12.3 51.7, 2.1 51.7, 2.1 43.3, -12.3 43.3))'" +
                            ")" +
                        ")"
        );
    }

    /**
     * Creates a filter without geometry and verifies that it is translated to the expected SQL fragment.
     */
    @Test
    public void testSimpleFilter() {
        final Filter<Feature> filter = FF.and(
                FF.greater(FF.property("mySchema/myTable"), FF.property("otter")),
                FF.or(
                        FF.isNull(FF.property("whatever")),
                        FF.equal(FF.literal(3.14), FF.property("π"))
                )
        );
        verifySQL(filter, "((\"mySchema\".\"myTable\" > \"otter\") AND (\"whatever\" IS NULL OR (3.14 = \"π\")))");
    }

    /**
     * Formats the given filter as a SQL {@code WHERE} statement body
     * and verifies that the result is equal to the expected string.
     */
    private static void verifySQL(final Filter<Feature> source, final String expected) {
        final StringBuilder sb = new StringBuilder();
        new ANSIInterpreter().visit(source, sb);
        assertEquals(expected, sb.toString());
    }
}
