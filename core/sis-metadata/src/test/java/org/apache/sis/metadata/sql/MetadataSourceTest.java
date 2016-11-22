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
package org.apache.sis.metadata.sql;

import java.sql.Connection;
import javax.sql.DataSource;
import org.apache.sis.internal.metadata.sql.TestDatabase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;


/**
 * Tests {@link MetadataSource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(org.apache.sis.internal.metadata.sql.ScriptRunnerTest.class)
public final strictfp class MetadataSourceTest extends TestCase {
    /**
     * Tests installation.
     *
     * @throws Exception if an error occurred while executing the installation script.
     */
    @Test
    public void testInstall() throws Exception {
        final DataSource ds = TestDatabase.create("metadata");
        try (final Connection c = ds.getConnection()) {
            final Installer install = new Installer(c);
            install.run();
        } finally {
            TestDatabase.drop(ds);
        }
    }
}
