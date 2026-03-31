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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import org.apache.sis.storage.DataStoreProvider;


/**
 * A helper class for loading a native library.
 * This object exists only for the duration of {@link NativeFunctions} construction
 * followed by the construction of the service ({@link DataStoreProvider}) that uses
 * the native functions.
 *
 * @param <F> the class of {@code NativeFunctions} to construct using this loader.
 * @param <S> type of service which will use the native functions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LibraryLoader<F extends NativeFunctions, S extends DataStoreProvider> {
    /**
     * Value to assign to {@link NativeFunctions#libraryName}.
     */
    String filename;

    /**
     * Value to assign to {@link NativeFunctions#libraryArena}.
     */
    Arena libraryArena;

    /**
     * Value to assign to {@link NativeFunctions#symbols}.
     */
    SymbolLookup symbols;

    /**
     * The constructor to call for building a {@code NativeFunctions}.
     * This is reset to {@code null} after usage as a way to ensure
     * that each {@code LibraryLoader} instance is used only once.
     */
    private Function<LibraryLoader<F,S>, F> functionsCreator;

    /**
     * The constructor to call for building a service using the native functions.
     * This is reset to {@code null} after usage as a way to ensure
     * that each {@code LibraryLoader} instance is used only once.
     */
    private Function<F, S> serviceCreator;

    /**
     * Exact class (not a parent class or interface) of the service to return.
     */
    final Class<S> serviceType;

    /**
     * Whether the library has been found.
     *
     * @see #status()
     */
    private LibraryStatus status;

    /**
     * Creates a new instance.
     *
     * @param  functionsCreator  a callback for creating a {@code NativeFunctions} if needed.
     * @param  serviceCreator    a callback for creating the service from the {@code NativeFunctions}.
     * @param  serviceType       exact class (not a parent class or interface) of the service to return.
     */
    public LibraryLoader(final Function<LibraryLoader<F,S>, F> functionsCreator,
                         final Function<F, S> serviceCreator,
                         final Class<S> serviceType)
    {
        this.functionsCreator = functionsCreator;
        this.serviceCreator   = serviceCreator;
        this.serviceType      = serviceType;
    }

    /**
     * Loads the native library and creates the handlers for native functions.
     * This method can be invoked at most once.
     *
     * @return handler of native functions.
     */
    final F createNativeFunctions() {
        Function<LibraryLoader<F,S>, F> c = functionsCreator;
        functionsCreator = null;     // For making sure to not invoke twice.
        return c.apply(this);
    }

    /**
     * Creates the service which will use the given native functions.
     * This method is invoked by {@link LibraryReference#getOrCreateService(Object, LibraryLoader)}
     * only and should not be invoked in other circumstances, because the caller must ensure that
     * the returned instance is registered for automatic call to {@link NativeFunctions#destroy()}
     * when the service is garbage-collected or at <abbr>JVM</abbr> shutdown time.
     *
     * @param  handlers  handlers created by {@link #createNativeFunctions()}.
     * @return service using the given native functions.
     */
    final S createService(final F handlers) {
        Function<F,S> c = serviceCreator;
        serviceCreator = null;      // For making sure to not invoke twice.
        return c.apply(handlers);
    }

    /**
     * Loads the native library from the given file, then builds the service using that library.
     *
     * @param  library  the library to load.
     * @return service using the native functions in the given library.
     * @throws IOException if an error occurred while getting a unique identifier of the library.
     * @throws IllegalArgumentException if path does not point to a valid native library.
     * @throws IllegalCallerException if this Apache SIS module is not authorized to call native methods.
     */
    @SuppressWarnings("restricted")
    public S load(final Path library) throws IOException {
        final Object libraryKey;
        libraryKey   = Files.readAttributes(library, BasicFileAttributes.class).fileKey();
        filename     = library.getFileName().toString();
        status       = LibraryStatus.LIBRARY_NOT_FOUND;     // In case an exception occurs before completion.
        libraryArena = Arena.ofShared();
        final S service;
        try {
            symbols = SymbolLookup.libraryLookup(library, libraryArena);
            status  = LibraryStatus.FUNCTION_NOT_FOUND;    // Update the status to report if we cannot complete.
            service = LibraryReference.getOrCreateService(libraryKey, this, false);
            status  = LibraryStatus.LOADED;
        } catch (Throwable e) {
            libraryArena.close();
            throw e;
        }
        return service;
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
     * @param  library   base filename (without the {@code "lib"} prefix and {@code ".so"} or
     *                   {@code ".dll"} suffix) of the native library to load.
     * @return service using the native functions in the given library.
     * @throws IllegalCallerException if this Apache SIS module is not authorized to call native methods.
     */
    @SuppressWarnings("restricted")
    public S global(final String library) {
        filename     = System.mapLibraryName(library);
        status       = LibraryStatus.LIBRARY_NOT_FOUND;     // Default value if an exception occurs below.
        libraryArena = Arena.global();
        S service = null;
        try {
            RuntimeException error = null;
            try {
                symbols = SymbolLookup.libraryLookup(filename, libraryArena);
            } catch (IllegalArgumentException e) {
                error    = e;
                filename = "system";
                symbols  = SymbolLookup.loaderLookup();     // In case user called `System.loadLibrary(…)`.
            }
            try {
                service = LibraryReference.getOrCreateService(serviceType, this, true);
                status  = LibraryStatus.LOADED;
            } catch (RuntimeException e) {
                if (e instanceof NoSuchElementException) {
                    status = LibraryStatus.FUNCTION_NOT_FOUND;
                }
                if (error != null) {
                    error.addSuppressed(e);
                    throw error;
                }
                throw e;
            }
        } catch (IllegalCallerException e) {
            status = LibraryStatus.UNAUTHORIZED;
            throw e;
        }
        return service;
    }

    /**
     * Returns the status after a call to {@code load(…)} or {@code global(…)}.
     * This method can be invoked in a pattern like below, in order to remember
     * that the loading failed:
     *
     * {@snippet lang="java" :
     *     LibraryLoader<?,?> loader = ...;
     *     try {
     *         service = loader.global("foo");
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
}
