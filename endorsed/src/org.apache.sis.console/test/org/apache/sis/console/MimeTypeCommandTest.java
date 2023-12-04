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
package org.apache.sis.console;

import java.net.URL;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.storage.gpx.TestData;
import org.apache.sis.metadata.iso.extent.DefaultExtentTest;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link MimeTypeCommand} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(CommandRunnerTest.class)
public final class MimeTypeCommandTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MimeTypeCommandTest() {
    }

    /**
     * Tests the sub-command on a metadata file.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testWithMetadataXML() throws Exception {
        final URL url = DefaultExtentTest.getTestFileURL();
        var test = new MimeTypeCommand(0, new String[] {CommandRunner.TEST, url.toString()});
        test.run();
        String output = test.outputBuffer.toString().trim();
        assertTrue(output.endsWith(".xml: application/vnd.iso.19139+xml"), output);
    }

    /**
     * Tests the sub-command on a GPX file.
     *
     * @throws Exception if an error occurred while reading the test file.
     */
    @Test
    public void testWithMetadataGPX() throws Exception {
        final URL url = TestData.V1_1.getURL(TestData.METADATA);
        assertNotNull(url);
        var test = new MimeTypeCommand(0, new String[] {CommandRunner.TEST, url.toString()});
        test.run();
        String output = test.outputBuffer.toString().trim();
        assertTrue(output.endsWith("metadata.xml: application/gpx+xml"), output);
    }
}
