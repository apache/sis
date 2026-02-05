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
package org.apache.sis.storage.gsf.panama;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.gsf.GSF;
import org.apache.sis.system.Shutdown;


/**
 * A helper class for loading a native library.
 *
 * @see org.apache.sis.storage.panama.LibraryLoader
 */
public final class LibraryLoader {
    /**
     * Value to assign to {@link NativeFunctions#arena}.
     */
    Arena arena;

    /**
     * Value to assign to {@link NativeFunctions#symbols}.
     */
    SymbolLookup symbols;

    /**
     * Whether the library has been found.
     */
    public LibraryStatus status;

    /**
     * Cause of failure to load the library, or {@code null} if none.
     */
    private RuntimeException error;

    /**
     * Creates a new instance.
     */
    public LibraryLoader() {
    }

    /**
     * Loads the native library from the given file.
     * Callers should register the returned instance in a {@link java.lang.ref.Cleaner}.
     */
    @SuppressWarnings("restricted")
    public GSF load(final Path library) {
        status = LibraryStatus.CANNOT_LOAD_LIBRARY;
        arena  = Arena.ofShared();
        final GSF instance;
        try {
            symbols  = SymbolLookup.libraryLookup(library, arena);
            status   = LibraryStatus.FUNCTION_NOT_FOUND;
            instance = new GSF(this);
            status   = LibraryStatus.LOADED;
        } catch (Throwable e) {
            arena.close();
            throw e;
        }
        return instance;
    }

    /**
     * Searches the native library in the default library path.
     */
    @SuppressWarnings("restricted")
    public GSF global() {
        status = LibraryStatus.CANNOT_LOAD_LIBRARY;
        String filename = System.mapLibraryName("gsf");
        try {
            symbols = SymbolLookup.libraryLookup(filename, Arena.global());
            GSF instance = new GSF(this);
            status = LibraryStatus.LOADED;
            Shutdown.register(instance);
            return instance;
        } catch (RuntimeException e) {
            error = e;
            return null;
        }
    }

    /**
     * Throws an exception if the loading of the native library failed.
     */
    public void validate() throws DataStoreException {
        if (error != null) {
            throw new DataStoreException(error);
        }
    }

    /**
     * Returns the error as a log message.
     */
    public Optional<LogRecord> getError() {
        if (error == null) {
            return Optional.empty();
        }
        var record = new LogRecord(Level.CONFIG, "Cannot initialize the GSF library.");
        record.setThrown(error);
        return Optional.of(record);
    }
}
