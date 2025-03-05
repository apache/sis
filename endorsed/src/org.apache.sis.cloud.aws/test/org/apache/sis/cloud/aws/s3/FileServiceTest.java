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
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Path;
import java.util.HashMap;
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
public final class FileServiceTest extends TestCase {

    /**
     * Creates a new test case.
     */
    public FileServiceTest() {
    }

    /**
     * Tests AWS S3 FileSystem creation.
     */
    @Test
    public void testNewFileSystemAws()
            throws URISyntaxException, IOException {
        final FileService service = new FileService();
        final Map<String, Object> properties = new HashMap<>();
        properties.put(FileService.AWS_SECRET_ACCESS_KEY, "secret");
        
        URI uri = new URI("S3://accessKey@bucket/file");
        FileSystem fs = service.newFileSystem(uri, properties);
        assertNotNull(fs);
        assertTrue(fs instanceof ClientFileSystem);
        
        assertThrows(FileSystemAlreadyExistsException.class, () -> {
            service.newFileSystem(uri, properties);
        });

        // Test FileSystem fetch by URI
        FileSystem fetchedFs = service.getFileSystem(uri);
        assertSame(fs, fetchedFs);
    }

    /**
     * Tests Self-Hosted S3 FileSystem creation.
     */
    @Test
    public void testNewFileSystemSelfHosted()
            throws URISyntaxException, IOException {
        final FileService service = new FileService();
        final Map<String, Object> properties = new HashMap<>();
        properties.put(FileService.AWS_SECRET_ACCESS_KEY, "secret");
        
        URI uri = new URI("S3://accessKey@localhost:8080/bucket/file");
        FileSystem fs = service.newFileSystem(uri, properties);
        assertNotNull(fs);
        assertTrue(fs instanceof ClientFileSystem);
        
        assertThrows(FileSystemAlreadyExistsException.class, () -> {
            service.newFileSystem(uri, properties);
        });

        // Test FileSystem fetch by URI
        FileSystem fetchedFs = service.getFileSystem(uri);
        assertSame(fs, fetchedFs);
    }

    /**
     * Tests Self-Hosted S3 FileSystem creation with properties.
     */
    @Test
    public void testNewFileSystemSelfHostedWithPropertiesNoPort()
            throws URISyntaxException, IOException {
        final FileService service = new FileService();
        final Map<String, Object> properties = new HashMap<>();
        properties.put(FileService.AWS_SECRET_ACCESS_KEY, "secret");
        properties.put(FileService.S3_HOST_URL, "localhost");
        properties.put(FileService.S3_IS_HTTPS, false);

        URI uri = new URI("S3://accessKey@bucket/file");
        FileSystem fs = service.newFileSystem(uri, properties);
        assertNotNull(fs);
        assertTrue(fs instanceof ClientFileSystem);

        assertThrows(FileSystemAlreadyExistsException.class, () -> {
            service.newFileSystem(uri, properties);
        });

        assertEquals(80, ((ClientFileSystem) fs).port);
        Path basePath = fs.getPath("/bucket/file");
        URI generatedURI = basePath.toUri();
        assertEquals("S3://accessKey@localhost:80/bucket/file", generatedURI.toString());

        // Test FileSystem fetch by URI
        FileSystem fetchedFs = service.getFileSystem(uri);
        assertNotSame(fs, fetchedFs);
        fetchedFs = service.getFileSystem(generatedURI);
        assertSame(fs, fetchedFs);
    }

    /**
     * Tests Self-Hosted S3 FileSystem creation with properties.
     */
    @Test
    public void testNewFileSystemSelfHostedWithProperties()
            throws URISyntaxException, IOException {
        final FileService service = new FileService();
        final Map<String, Object> properties = new HashMap<>();
        properties.put(FileService.AWS_SECRET_ACCESS_KEY, "secret");
        properties.put(FileService.S3_HOST_URL, "localhost");
        properties.put(FileService.S3_PORT, 8454);
        properties.put(FileService.S3_IS_HTTPS, false);

        URI uri = new URI("S3://accessKey@bucket/file");
        FileSystem fs = service.newFileSystem(uri, properties);
        assertNotNull(fs);
        assertTrue(fs instanceof ClientFileSystem);

        assertThrows(FileSystemAlreadyExistsException.class, () -> {
            service.newFileSystem(uri, properties);
        });

        assertEquals(8454, ((ClientFileSystem) fs).port);
        Path basePath = fs.getPath("/bucket/file");
        URI generatedURI = basePath.toUri();
        assertEquals("S3://accessKey@localhost:8454/bucket/file", generatedURI.toString());

        // Test FileSystem fetch by URI
        FileSystem fetchedFs = service.getFileSystem(uri);
        assertNotSame(fs, fetchedFs);
        fetchedFs = service.getFileSystem(generatedURI);
        assertSame(fs, fetchedFs);
    }

    /**
     * Tests FileSystem creation with missing secret key.
     */
    @Test
    public void testMissingSecretKey()
            throws URISyntaxException {
        final FileService service = new FileService();
        final Map<String, Object> properties = new HashMap<>();
        
        URI uri = new URI("S3://accessKey@bucket/file");
        assertThrows(IllegalArgumentException.class, () -> {
            service.newFileSystem(uri, properties);
        });
    }
}
