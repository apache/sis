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

import java.util.concurrent.Callable;
import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;


/**
 * Base class for sets of method handles to native functions.
 *
 * @see org.apache.sis.storage.panama.NativeFunctions
 */
public abstract class NativeFunctions implements Runnable, Callable<Object> {
    /**
     * The arena used for loading the library, or {@code null} for the global arena.
     */
    private final Arena arena;

    /**
     * The lookup for retrieving the address of a symbol in the native library.
     */
    public final SymbolLookup symbols;

    /**
     * The linker to use for fetching method handles from the {@linkplain #symbols}.
     */
    public final Linker linker;

    /**
     * Creates a new set of handles to native functions.
     */
    protected NativeFunctions(final LibraryLoader loader) {
        arena   = loader.arena;
        symbols = loader.symbols;
        linker  = Linker.nativeLinker();
    }

    /**
     * Returns the method handler for the <abbr>GSF</abbr> function of given name and signature.
     */
    @SuppressWarnings("restricted")
    protected final MethodHandle lookup(final String function, final FunctionDescriptor signature) {
        return symbols.find(function).map((method) -> linker.downcallHandle(method, signature)).orElseThrow();
    }

    /**
     * Unloads the native library. If the arena is global,
     * then this method should not be invoked before <abbr>JVM</abbr> shutdown.
     */
    @Override
    public void run() {
        if (arena != null) {
            arena.close();
        }
    }

    /**
     * Synonymous of {@link #run()}, used in shutdown hook.
     */
    @Override
    public final Object call() {
        run();
        return null;
    }
}
