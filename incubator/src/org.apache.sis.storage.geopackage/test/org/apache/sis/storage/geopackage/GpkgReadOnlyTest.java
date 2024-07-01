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
package org.apache.sis.storage.geopackage;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.apache.sis.storage.DataStoreException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class GpkgReadOnlyTest {

    /**
     * Test opening an sqlite database on a read-only file.
     */
    @Test
    public void testOpeningReadOnlyDatabase() throws IOException, DataStoreException, SQLException {

        final Path file = Files.createTempFile("database", ".gpkg");
        try {
            Files.deleteIfExists(file);
            assertFalse(Files.exists(file));

            //create an empty database
            String url = "jdbc:sqlite:"+file.toString();
            try (Connection conn = DriverManager.getConnection(url)) {
            }
            assertTrue(Files.exists(file));

            //make file read only
            assertTrue(Files.isWritable(file));
            setReadOnly(file, true);
            /*
            TODO
            We use assume here to skip the test in case the setReadOnly fails.
            This test works on linux but not on GitLab, more research required.
            */
            assumeFalse(Files.isWritable(file));

            try (GpkgStore store = new GpkgStore(file)) {
                //should not raise any exception
                assertEquals(0, store.components().size());
            }
        } finally {
            setReadOnly(file, false);
            Files.delete(file);
        }
    }

    private static void setReadOnly(Path file, boolean readOnly) throws IOException {
        final FileStore fileStore = Files.getFileStore(file);
        if (fileStore.supportsFileAttributeView(DosFileAttributeView.class)) {
            final DosFileAttributeView attrs = Files.getFileAttributeView(file, DosFileAttributeView.class);
            attrs.setReadOnly(readOnly);
        } else if (fileStore.supportsFileAttributeView(PosixFileAttributeView.class)) {
            final PosixFileAttributeView attrs = Files.getFileAttributeView(file, PosixFileAttributeView.class);
            final Set<PosixFilePermission> permissions = new HashSet<>();
            if (readOnly) {
                permissions.add(PosixFilePermission.OWNER_READ);
            } else {
                permissions.add(PosixFilePermission.OWNER_READ);
                permissions.add(PosixFilePermission.OWNER_WRITE);
            }
            attrs.setPermissions(permissions);
        } else {
            file.toFile().setWritable(!readOnly);
        }
    }

}
