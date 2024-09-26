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

import org.apache.sis.storage.DataStoreClosedException;


/**
 * Status of the native library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum LibraryStatus {
    /**
     * The native library is ready for use.
     */
    LOADED((short) 0),

    /**
     * The native library has been unloaded.
     */
    UNLOADED(Resources.Keys.LibraryUnloaded_1),

    /**
     * The native library has not been found.
     */
    LIBRARY_NOT_FOUND(Resources.Keys.LibraryNotFound_1),

    /**
     * The native library was found, but not symbol that we searched.
     */
    FUNCTION_NOT_FOUND(Resources.Keys.FunctionNotFound_1),

    /**
     * <abbr>SIS</abbr> is not authorized to perform native function calls.
     */
    UNAUTHORIZED(Resources.Keys.NativeAccessNotAllowed),

    /**
     * A fatal error occurred in the native library and that library should not be used anymore.
     */
    FATAL_ERROR(Resources.Keys.FatalLibraryError_1);

    /**
     * Resource key of an explanatory message, or 0 if none.
     */
    private final short message;

    /**
     * Creates a new enumeration value.
     */
    private LibraryStatus(final short message) {
        this.message = message;
    }

    /**
     * Throws an exception if the native library is not available.
     *
     * @param  library  the library name, of formatting the error message.
     * @param  cause    the cause of the error, or {@code null} if none.
     * @throws DataStoreClosedException if this enumeration value is not {@link #LOADED}
     *         or if the given cause is not null.
     */
    public void report(String library, Exception cause) throws DataStoreClosedException {
        if (message != 0 || cause != null) {
            // Note: `NativeAccessNotAllowed` will ignore the `library` argument.
            throw new DataStoreClosedException(Resources.format(message, library), cause);
        }
    }
}
