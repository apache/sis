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
package org.apache.sis.storage.panama;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.function.Function;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.system.Shutdown;


/**
 * A helper class for loading a native library.
 * This object exists only for the duration of {@link NativeFunctions} construction.
 *
 * @param <F> the class of {@code NativeFunctions} to construct using this loader.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LibraryLoader<F extends NativeFunctions> {
    /**
     * Value to assign to {@link NativeFunctions#libraryName}.
     */
    String filename;

    /**
     * Value to assign to {@link NativeFunctions#arena}.
     */
    Arena arena;

    /**
     * Value to assign to {@link NativeFunctions#symbols}.
     */
    SymbolLookup symbols;

    /**
     * The constructor to call for building a {@code NativeFunctions}.
     * This is reset to {@code null} after usage as a way to ensure
     * that each {@code LibraryLoader} instance is used only once.
     */
    private Function<LibraryLoader<F>, F> creator;

    /**
     * Whether the library has been found.
     *
     * @see #status()
     */
    private LibraryStatus status;

    /**
     * Cause of failure to load the library, or {@code null} if none.
     */
    private RuntimeException error;

    /**
     * Creates a new instance.
     *
     * @param  creator  the constructor to call for building a {@code NativeFunctions}.
     */
    public LibraryLoader(final Function<LibraryLoader<F>, F> creator) {
        this.creator = creator;
    }

    /**
     * Loads the native library from the given file.
     * Callers should register the returned instance in a {@link java.lang.ref.Cleaner}.
     *
     * @param  library  the library to load.
     * @return handles to native functions needed by the data store.
     * @throws IllegalArgumentException if the native library has not been found.
     * @throws IllegalCallerException if this Apache SIS module is not authorized to call native methods.
     */
    @SuppressWarnings("restricted")
    public F load(final Path library) {
        final var c = creator;
        creator  = null;
        filename = library.getFileName().toString();
        status   = LibraryStatus.LIBRARY_NOT_FOUND;         // In case an exception occurs before completion.
        arena    = Arena.ofShared();
        final F instance;
        try {
            symbols  = SymbolLookup.libraryLookup(library, arena);
            status   = LibraryStatus.FUNCTION_NOT_FOUND;    // Update the status to report if we cannot complete.
            instance = c.apply(this);
            status   = LibraryStatus.LOADED;
        } catch (Throwable e) {
            arena.close();
            throw e;
        }
        return instance;
    }

    /**
     * Searches the native library in the default library path. If the native library is not found on the library path,
     * defaults to {@link SymbolLookup#loaderLookup()} for allowing users to invoke {@link System#loadLibrary(String)}
     * as a fallback. The method handlers created with this instance are valid for the Java Virtual Machine lifetime,
     * i.e. they will be associated to the {@linkplain Arena#global() global arena}.
     *
     * <p><em>It is caller's responsibility to ensure that this method is invoked at most once per library.</em>
     * Invoking this method many times for the same library may cause unpredictable behavior.</p>
     *
     * <h4>Error handling</h4>
     * Contrarily to {@link #load(Path)}, this method does not throw exception. Callers should invoke
     * {@link #validate()} or {@link #getError(String)} after this method call,
     * depending on whether an error should be fatal or not.
     *
     * @param  library   base filename (without the {@code "lib"} prefix and {@code ".so"} or
     *                   {@code ".dll"} suffix) of the native library to load.
     * @return handles to native functions needed by the data store.
     */
    @SuppressWarnings("restricted")
    public F global(final String library) {
        final var c = creator;
        creator     = null;
        filename    = (File.separatorChar == '\\') ? (library + ".dll") : ("lib" + library + ".so");
        status      = LibraryStatus.LIBRARY_NOT_FOUND;      // Default value if an exception occurs below.
        F instance  = null;
create: try {
            try {
                symbols = SymbolLookup.libraryLookup(filename, Arena.global());
            } catch (IllegalArgumentException e) {
                error    = e;
                filename = "system";
                symbols  = SymbolLookup.loaderLookup();     // In case user called `System.loadLibrary(…)`.
            }
            try {
                instance = c.apply(this);
                status   = LibraryStatus.LOADED;
            } catch (RuntimeException e) {
                if (error != null) {
                    error.addSuppressed(e);
                } else {
                    error = e;
                }
                break create;
            }
            /*
             * Note: registering a shutdown hook cause a reference to be kept for the JVM lifetime.
             * But such reference should exist anyway because this `global(String)` method should
             * be invoked for initialization of a static variable. Furthermore, subclass may need
             * to do a last native method class for flushing some cache.
             *
             * TODO: unregister if the library had a fatal error and should not be used anymore.
             */
            Shutdown.register(instance);
        } catch (IllegalCallerException e) {
            error  = e;
            status = LibraryStatus.UNAUTHORIZED;
        } catch (NoSuchElementException e) {
            error  = e;
            status = LibraryStatus.FUNCTION_NOT_FOUND;
        }
        return instance;
    }

    /**
     * Returns the status after a call to {@code load(…)} or {@code global(…)}.
     * This method can be invoked in a pattern like below, in order to remember
     * that the loading failed:
     *
     * {@snippet lang="java" :
     *     LibraryLoader<?> loader = ...;
     *     try {
     *         loader.global("foo");
     *     } finally {
     *         globalStatus = loader.status();
     *     }
     *     }
     *
     * @return the status of the native library.
     */
    public final LibraryStatus status() {
        return status;
    }

    /**
     * Throws an exception if the loading of the native library failed.
     *
     * @param  library  name of the library, used if an error message needs to be produced.
     * @throws DataStoreException if the native library has not been found
     *         or if SIS is not allowed to call native functions.
     */
    public void validate(String library) throws DataStoreException {
        status.report(library, error);
    }

    /**
     * Returns the error as a log message.
     *
     * @param  name  the library name.
     * @return a log message describing the error, or {@code null} if the operation was successful.
     */
    public Optional<LogRecord> getError(final String name) {
        if (error == null) {
            return Optional.empty();
        }
        LogRecord record = Resources.forLocale(null).createLogRecord(Level.CONFIG, Resources.Keys.CannotInitialize_1, name);
        record.setThrown(error);
        return Optional.of(record);
    }
}
