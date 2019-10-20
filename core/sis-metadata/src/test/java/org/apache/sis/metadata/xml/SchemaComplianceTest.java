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
package org.apache.sis.metadata.xml;

import java.nio.file.Path;
import java.nio.file.Files;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.system.DataDirectory;
import org.apache.sis.test.ProjectDirectories;
import org.apache.sis.test.xml.SchemaCompliance;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assume.*;


/**
 * Tests conformance of JAXB annotations with XML schemas if those schemas are available.
 * This tests requires the {@code $SIS_DATA/Schemas/iso/19115/-3} directory to exists.
 * Those files must be installed manually; they are not distributed with Apache SIS for licensing reasons.
 * Content can be downloaded as ZIP files from <a href="https://standards.iso.org/iso/19115/">ISO portal</a>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public final strictfp class SchemaComplianceTest extends TestCase {
    /**
     * Verifies compliance with metadata schemas.
     *
     * @throws Exception if an error occurred while checking the schema.
     *
     * @see <a href="https://standards.iso.org/iso/19115/-3/">ISO schemas for metadata</a>
     */
    @Test
    public void verifyMetadata() throws Exception {
        Path directory = DataDirectory.SCHEMAS.getDirectory();
        assumeNotNull(directory);
        directory = directory.resolve("iso");
        assumeTrue(Files.isDirectory(directory.resolve("19115")));
        /*
         * Locate the root of metadata class directory. In a Maven build:
         * "core/sis-metadata/target/classes/org/apache/sis/metadata/iso"
         */
        final ProjectDirectories dir = new ProjectDirectories(ISOMetadata.class);
        final SchemaCompliance checker = new SchemaCompliance(dir.classesRootDirectory, directory);
        checker.loadDefaultSchemas();
        checker.verify(dir.classesPackageDirectory);
    }
}
