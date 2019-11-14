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
package org.apache.sis.cql;

import java.time.Instant;
import org.opengis.filter.FilterFactory2;
import org.locationtech.jts.geom.GeometryFactory;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.internal.util.StandardDateFormat;
import org.apache.sis.test.TestCase;


/**
 * Base class of all CQL tests.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
strictfp class CQLTestCase extends TestCase {
    /**
     * The factory to use for creating filter and expressions.
     */
    final FilterFactory2 FF;

    /**
     * The factory to use for creating Java Topology Suite (JTS) objects.
     */
    final GeometryFactory GF;

    /**
     * Creates a new test case.
     */
    CQLTestCase() {
        FF = new DefaultFilterFactory();
        GF = new GeometryFactory();
    }

    /**
     * Returns a date from the given string.
     */
    static Instant parseDate(final String date) {
        return Instant.from(StandardDateFormat.FORMAT.parse(date));
    }
}
