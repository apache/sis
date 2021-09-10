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
package org.apache.sis.internal.sql.postgis;

import org.apache.sis.test.TestCase;
import org.apache.sis.util.Version;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Postgres}.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class PostgresTest extends TestCase {
    /**
     * Tests {@link Postgres#parseVersion(String)}.
     */
    @Test
    public void parse_postgis_version() {
        final Version version = Postgres.parseVersion("3.1 USE_GEOS=1 USE_PROJ=1 USE_STATS=1");
        assertEquals(3, version.getMajor());
        assertEquals(1, version.getMinor());
        assertNull  (   version.getRevision());
    }
}
