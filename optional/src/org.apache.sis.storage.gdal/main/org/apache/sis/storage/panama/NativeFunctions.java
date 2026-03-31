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
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Logger;
import java.util.concurrent.Callable;
import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.UndeclaredThrowableException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.storage.DataStoreException;


/**
 * Base class for sets of method handles to native functions.
 * The native library can be unloaded by invoking {@link #call()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class NativeFunctions implements Callable<Object> {
    /**
     * Name of the library which has been loaded, for information purposes.
     */
    public final String libraryName;

    /**
     * The arena used for loading the library.
     * May be a shared or a global arena.
     */
    protected final Arena libraryArena;

    /**
     * The lookup for retrieving the address of a symbol in the native library.
     */
    public final SymbolLookup symbols;

    /**
     * The linker to use for fetching method handles from the {@linkplain #symbols}.
     * In current version, this is always {@link Linker#nativeLinker()}.
     */
    public final Linker linker;

    /**
     * Creates a new set of handles to native functions.
     *
     * @param  loader  the object used for loading the library.
     */
    protected NativeFunctions(final LibraryLoader<?,?> loader) {
        libraryName  = loader.filename;
        libraryArena = loader.libraryArena;
        symbols      = loader.symbols;
        linker       = Linker.nativeLinker();
    }

    /**
     * Returns the method handler for the <abbr>GDAL</abbr> function of given name and signature.
     * This is a convenience method for initialization of fields in subclasses.
     *
     * @param  function   name of the C/C++ <abbr>GDAL</abbr> function to invoke.
     * @param  signature  type of arguments and return type.
     * @return method handler for the <abbr>GDAL</abbr> function.
     * @throws NoSuchElementException if the given function has not been found.
     */
    @SuppressWarnings("restricted")
    public final MethodHandle lookup(final String function, final FunctionDescriptor signature) {
        return linker.downcallHandle(symbols.find(function).orElseThrow(), signature);
    }

    /**
     * Invokes a C/C++ function which expects a {@code char*} argument and returns a {@code char*}.
     * This method should be used only for rarely invoked C/C++ functions, because it fetches the
     * method handler and create a confined arena on every calls.
     *
     * @param  function  name of the C/C++ <abbr>GDAL</abbr> function to invoke.
     * @param  arg       the argument.
     * @return whether the return value, or empty if the method was not found or returned {@code null}.
     */
    @SuppressWarnings("restricted")
    protected final Optional<String> invokeGetString(final String function, final String arg) {
        return symbols.find(function).map((method) -> {
            var acceptPointerReturnPointer = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS);
            MethodHandle handle = linker.downcallHandle(method, acceptPointerReturnPointer);
            MemorySegment result;
            try (Arena local = Arena.ofConfined()) {
                result = (MemorySegment) handle.invokeExact(local.allocateFrom(arg));
            } catch (Throwable e) {
                throw propagate(e);
            }
            return toString(result);
        });
    }

    /**
     * Invokes a C/C++ function which expects no argument and returns an integer.
     * This method should be used only for rarely invoked C/C++ functions,
     * because it fetches the method handle on every calls.
     *
     * @param  function  name of the C/C++ <abbr>GDAL</abbr> function to invoke.
     * @return whether the return value, or empty if the method was not found.
     */
    @SuppressWarnings("restricted")
    public final OptionalInt invokeGetInt(final String function) {
        final Optional<MemorySegment> method = symbols.find(function);
        if (method.isEmpty()) {
            return OptionalInt.empty();
        }
        final MethodHandle handle = linker.downcallHandle(method.get(), FunctionDescriptor.of(ValueLayout.JAVA_INT));
        final int result;
        try {
            result = (int) handle.invokeExact();
        } catch (Throwable e) {
            throw propagate(e);
        }
        return OptionalInt.of(result);
    }

    /**
     * Invokes a C/C++ function which expects no argument and returns no value.
     * This method should be used only for rarely invoked C/C++ functions,
     * because it fetches the method handle on every calls.
     *
     * @param  function  name of the C/C++ <abbr>GDAL</abbr> function to invoke.
     * @return whether the function has been found and executed.
     */
    @SuppressWarnings("restricted")
    protected final boolean invoke(final String function) {
        final Optional<MemorySegment> method = symbols.find(function);
        if (method.isEmpty()) {
            return false;
        }
        final MethodHandle handle = linker.downcallHandle(method.get(), FunctionDescriptor.ofVoid());
        try {
            handle.invokeExact();
        } catch (Throwable e) {
            throw propagate(e);
        }
        return true;
    }

    /**
     * Returns whether the given result is null.
     *
     * @param  result  the result of a native method call, or {@code null}.
     * @return whether the given result is null or a C/C++ {@code NULL}.
     */
    public static boolean isNull(final MemorySegment result) {
        return (result == null) || result.address() == 0;
    }

    /**
     * Returns the value of a native function returning a null-terminated {@code char*}.
     * The string is assumed encoded in UTF-8.
     *
     * @param  result  the result of a native method call, or {@code null}.
     * @return the result as a string, or {@code null} if the result was null.
     */
    @SuppressWarnings("restricted")
    public static String toString(final MemorySegment result) {
        return isNull(result) ? null : result.reinterpret(Integer.MAX_VALUE).getString(0);
    }

    /**
     * Propagates the given exception as an unchecked exception. This method should be invoked only
     * for exceptions thrown during calls to native methods by {@code MethodHandler.invokeExact(…)},
     * because it assumes that no checked exception should be thrown.
     *
     * @param  exception  the exception thrown by {@code MethodHandler.invokeExact(…)}.
     * @return the exception to be thrown by the caller.
     */
    public static RuntimeException propagate(final Throwable exception) {
        switch (exception) {
            case Error e:              throw e;
            case RuntimeException e:   return e;
            case IOException e:        return new UncheckedIOException(e);
            case DataStoreException e: return new BackingStoreException(e);
            default: return new UndeclaredThrowableException(exception);
        }
    }

    /**
     * Disposes the native library when this {@code NativeFunctions} has been garbage-collected or during
     * the <abbr>JVM</abbr> shutdown. This method is public as an implementation side-effect and should
     * not be invoked directly in application code.
     *
     * <p>This method is invoked in contexts where we cannot propagate exceptions to the caller.
     * Instead, exceptions are logged.</p>
     *
     * @return {@code null}.
     */
    @Override
    public final Object call() {
        // Note: `libraryArena` may be the global arena, which cannot be closed.
        try {
            destroy();
        } catch (Throwable e) {
            Logging.unexpectedException(getLogger(), getClass(), "destroy", e);
        }
        return null;
    }

    /**
     * Releases any resources used by the native library. This method is invoked automatically when
     * the last {@code NativeFunctions} instance using the native library has been garbage-collected,
     * or during the <abbr>JVM</abbr> shutdown.
     */
    protected void destroy() {
    }

    /**
     * Returns the logger where to report warnings.
     * This is used if an exception occurred during the execution of {@link #destroy()}.
     *
     * @return the logger for warnings.
     */
    protected abstract Logger getLogger();
}
