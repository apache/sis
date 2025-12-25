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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sql.DataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.Workaround;


/**
 * A helper class for initializing a {@code GpkgStore}.
 * A temporary instance exists at construction time, then is discarded.
 *
 * This is a work around for RFE #4093999 in Sun's bug database
 * ("Relax constraint on placement of this()/super() call in constructors").
 *
 * @author Martin Desruisseaux (Geomatys).
 */
@Workaround(library="JDK", version="7", fixed="25")
final class Initializer {
    /**
     * Suffixes of auxiliary files.
     */
    static final String WAL_SUFFIX = "-wal", SHM_SUFFIX = "-shm";

    /**
     * The user-supplied file, or {@code null} if none.
     */
    Path path;

    /**
     * The storage connector with {@link #source} as the storage object.
     * If the user-supplied storage connector is already wrapping a {@link DataSource}, then it is used.
     * Otherwise, a new storage connector is created with a new SQLite {@code DataSource} as the storage.
     */
    StorageConnector connector;

    /**
     * The PRAGMA statements, or an empty map if none.
     */
    Map<String,String> pragmas;

    /**
     * Whether a new dabase is created.
     */
    boolean create;

    /**
     * Creates a new instance for the given storage connector.
     */
    Initializer(StorageConnector connector) throws DataStoreException {
        pragmas = Map.of();
        path = connector.getStorageAs(Path.class);
        if (path != null) {
            /*
             * Note: SQLite makes a difference between read-only and immutable.
             * In read-only mode, the driver may still create and use the -wal and -shm files.
             * In immutable mode, the driver is not allowed to touch or create anything.
             * Reference: https://www.sqlite.org/c3ref/open.html
             */
            boolean isReadOnly = true;
            final OpenOption[] options = connector.getOption(OptionKey.OPEN_OPTIONS);
            if (ArraysExt.contains(options, StandardOpenOption.WRITE)) {
                create = ArraysExt.contains(options, StandardOpenOption.CREATE_NEW) ||
                        (ArraysExt.contains(options, StandardOpenOption.CREATE) && isAbsentOrEmpty(path));
                isReadOnly = !(create || Files.isWritable(path));
            }
            final var url = new StringBuilder(60).append("jdbc:sqlite:").append(path.toAbsolutePath());
            StringBuilders.replace(url, File.separatorChar, '/');
            if (!create && createAuxiliaryFiles(path)) {
                url.append("?immutable=1");
            }
            final var source = new SQLiteDataSource();
            source.setDatabaseName(IOUtilities.filenameWithoutExtension(path.getFileName().toString()));
            source.setUrl(url.toString());
            source.setReadOnly(isReadOnly);
            if (!isReadOnly) {
                // TODO: need to find a list of pragma not causing errors in read-only mode.
                if ((pragmas = connector.getOption(InternalOptionKey.PRAGMAS)) == null) {
                    pragmas = Map.of();
                } else try {
                    pragmas = Map.copyOf(pragmas);
                    for (Map.Entry<String,String> entry : pragmas.entrySet()) {
                        source.getConfig().setPragma(SQLiteConfig.Pragma.valueOf(entry.getKey()), entry.getValue());
                    }
                } catch (IllegalArgumentException e) {
                    throw new DataStoreException("Illegal PRAGMA: " + e.getLocalizedMessage());
                }
            }
            connector = new StorageConnector(source);
        }
        /*
         * We need a read/write lock for SQLite. The connection pool is not a real pool.
         * Sqlite has good support for concurrent read operations but has troubles when
         * write operations are involved: "SQLite busy" exceptions may pop up.
         * To avoid such errors, the data store needs to handle locks itself.
         */
        connector.setOption(InternalOptionKey.LOCKS, new ReentrantReadWriteLock());
        this.connector = connector;
    }

    /**
     * Returns whether the given file is absent or empty.
     */
    private static boolean isAbsentOrEmpty(final Path file) throws DataStoreException {
        try {
            return Files.notExists(file) || Files.size(file) == 0;
        } catch (IOException e) {
            throw GpkgStore.cannotExecute(null, e);
        }
    }

    /**
     * Creates WAL and SHM files if they do not already exist. For some strange reason,
     * even with write permissions, SQLite may fail to open a connection with the following exception:
     *
     * <blockquote>[SQLITE_CANTOPEN] Unable to open the database file (unable to open database file).</blockquote>
     *
     * But if we create the {@code -wal} and {@code -shm} empty files, it will work.
     * Note: disabling WAL journal or forcing it to memory does not solve the problem.
     *
     * @param  path  path to the SQLite file.
     * @return whether to consider the database as immutable.
     *
     * @see <a href="https://www.sqlite.org/wal.html#read_only_databases">SQLite documentation</a>
     *
     * @todo Verify if the problem still exists with latest SQLite versions.
     */
    @Workaround(library="SQLite", version="unspecified")
    private static boolean createAuxiliaryFiles(final Path path) {
        final String filename = path.getFileName().toString();
        try {
            Files.createFile(path.resolveSibling(filename.concat(WAL_SUFFIX)));
            Files.createFile(path.resolveSibling(filename.concat(SHM_SUFFIX)));
        } catch (FileAlreadyExistsException ex) {
            // Ignore. The database is considered mutable.
        } catch (IOException ex) {
            return true;
        }
        return false;
        /*
         * Note: it may happen that only one file has been created. Maybe we should cleanup
         * by deleting the other file. But we have no guarantee that the other file has not
         * been created concurrently.
         */
    }
}
