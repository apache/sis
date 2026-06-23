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
package org.apache.sis.cloud.aws.s3;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Path;
import java.util.Map;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link FileService}.
 *
 * @author  Quentin Bialota (Geomatys)
 */
@SuppressWarnings("exports")
public final class FileServiceTest extends TestCase {
    /**
     * The service to test.
     */
    private final FileService service;

    /**
     * Creates a new test case.
     */
    public FileServiceTest() {
        service = new FileService();
    }

    /**
     * Creates a new file system and performs a few consistency checks.
     *
     * @param  uri         an <abbr>URI</abbr> of the form {@code "s3://accessKey@[host:port/]bucket/file"}.
     * @param  complete    whether the <abbr>URI</abbr> is sufficient for finding the file system.
     * @param  properties  properties to configure the file system, or {@code null} if none.
     * @return the new file system.
     * @throws IOException if the tested method failed.
     */
    private ClientFileSystem newFileSystem(final URI uri, final boolean complete, final Map<String, ?> properties)
            throws IOException
    {
        ClientFileSystem fs = assertInstanceOf(ClientFileSystem.class, service.newFileSystem(uri, properties));
        var exception = assertThrows(FileSystemAlreadyExistsException.class, () -> {
            service.newFileSystem(uri, properties);
        });
        assertNotNull(exception.getMessage());
        if (complete) {
            assertSame(fs, service.getFileSystem(uri));
        }
        return fs;
    }

    /**
     * Tests AWS S3 FileSystem creation.
     *
     * @throws URISyntaxException if the <abbr>URI</abbr> used for this test is invalid.
     * @throws IOException if the tested method failed.
     */
    @Test
    public void testNewFileSystemAws() throws URISyntaxException, IOException {
        newFileSystem(new URI("S3://accessKey@bucket/file"), true,
                Map.of(FileService.AWS_REGION, ClientFileSystemTest.region(),
                       FileService.AWS_SECRET_ACCESS_KEY, "secret"));
    }

    /**
     * Tests Self-Hosted S3 FileSystem creation.
     *
     * @throws URISyntaxException if the <abbr>URI</abbr> used for this test is invalid.
     * @throws IOException if the tested method failed.
     */
    @Test
    public void testNewFileSystemSelfHosted() throws URISyntaxException, IOException {
        newFileSystem(new URI("S3://accessKey@localhost:8080/bucket/file"), true,
                Map.of(FileService.AWS_REGION, ClientFileSystemTest.region(),
                       FileService.AWS_SECRET_ACCESS_KEY, "secret"));
    }

    /**
     * Tests Self-Hosted S3 FileSystem creation with properties.
     *
     * @throws URISyntaxException if the <abbr>URI</abbr> used for this test is invalid.
     * @throws IOException if the tested method failed.
     */
    @Test
    public void testNewFileSystemSelfHostedWithPropertiesNoPort() throws URISyntaxException, IOException {
        final var uri = new URI("S3://accessKey@bucket/file");
        final ClientFileSystem fs = newFileSystem(uri, false, Map.of(
                FileService.AWS_REGION, ClientFileSystemTest.region(),
                FileService.AWS_SECRET_ACCESS_KEY, "secret",
                FileService.HOST_URL,              "localhost",
                FileService.IS_HTTPS,              Boolean.FALSE));

        assertEquals(FileService.HTTP_PORT, fs.server.port);
        Path basePath = fs.getPath("/bucket/file");
        URI generatedURI = basePath.toUri();
        assertEquals("S3://accessKey@localhost:80/bucket/file", generatedURI.toString());

        // Test FileSystem fetch by URI.
        assertSame(fs, service.getFileSystem(generatedURI));
    }

    /**
     * Tests Self-Hosted S3 FileSystem creation with properties.
     *
     * @throws URISyntaxException if the <abbr>URI</abbr> used for this test is invalid.
     * @throws IOException if the tested method failed.
     */
    @Test
    public void testNewFileSystemSelfHostedWithProperties() throws URISyntaxException, IOException {
        final URI uri = new URI("S3://accessKey@bucket/file");
        final ClientFileSystem fs = newFileSystem(uri, false, Map.of(
                FileService.AWS_REGION, ClientFileSystemTest.region(),
                FileService.AWS_SECRET_ACCESS_KEY, "secret",
                FileService.HOST_URL,              "localhost",
                FileService.PORT,                  8454,
                FileService.IS_HTTPS,              Boolean.FALSE));

        assertEquals(8454, fs.server.port);
        Path basePath = fs.getPath("/bucket/file");
        URI generatedURI = basePath.toUri();
        assertEquals("S3://accessKey@localhost:8454/bucket/file", generatedURI.toString());

        // Test FileSystem fetch by URI.
        assertSame(fs, service.getFileSystem(generatedURI));
    }

    /**
     * Tests FileSystem creation with missing secret key.
     *
     * @throws URISyntaxException if the <abbr>URI</abbr> used for this test is invalid.
     */
    @Test
    public void testMissingSecretKey() throws URISyntaxException {
        final Map<String, Object> properties = Map.of(FileService.AWS_REGION, ClientFileSystemTest.region());
        final var uri = new URI("S3://accessKey@bucket/file");
        var exception = assertThrows(IllegalArgumentException.class, () -> {
            service.newFileSystem(uri, properties);
        });
        assertNotNull(exception.getMessage());
    }
}
