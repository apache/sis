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
package org.apache.sis.referencing.factory.sql;

import java.sql.Connection;
import javax.sql.DataSource;
import org.apache.sis.internal.metadata.sql.TestDatabase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

// Branch-dependent imports
import java.nio.file.Path;


/**
 * Tests {@link EPSGInstaller}. Every databases created by this test suite exists only in memory.
 * This class does not write anything to disk (except maybe some temporary files).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(EPSGFactoryTest.class)
public final strictfp class EPSGInstallerTest extends TestCase {
    /**
     * Tests the creation of an EPSG database on Derby.
     * This test is skipped if no Derby or JavaDB driver has been found.
     *
     * @throws Exception if an error occurred while creating the database.
     */
    @Test
    public void testCreationOnDerby() throws Exception {
        final Path scripts = TestDatabase.directory("ExternalSources");
        final DataSource ds = TestDatabase.create("test");
        try (Connection c = ds.getConnection()) {
            try (EPSGInstaller installer = new EPSGInstaller(c)) {
                installer.setSchema("EPSG");
                installer.run(scripts);
            }
        } finally {
            TestDatabase.drop(ds);
        }
    }
}
