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
package org.apache.sis.internal.system;

import java.net.URL;
import java.net.URISyntaxException;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;


/**
 * The operation system on which SIS is running.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
public enum OS {
    /**
     * Unknown system.
     */
    UNKNOWN(null, false),

    /**
     * Windows.
     */
    WINDOWS("windows", false),

    /**
     * Mac OS.
     */
    MAC_OS("darwin", true),

    /**
     * Linux.
     */
    LINUX("linux", true);

    /**
     * The sub-directory where to look for native files ({@code .so} or {@code .dll}).
     * Those subdirectories are not standard (as far as we know) and could change in
     * any future Apache SIS version. The directory is {@code null} for unknown OS.
     */
    private final String libdir;

    /**
     * {@code true} if this OS is a kind of Unix.
     */
    public final boolean unix;

    /**
     * Creates a new enumeration.
     */
    private OS(final String libdir, final boolean unix) {
        this.libdir = libdir;
        this.unix   = unix;
    }

    /**
     * Returns the name value of {@code "os.name"} property, or {@code null} if the security manager
     * does not allow us to access this information.
     *
     * <div class="note"><b>Note:</b> {@code uname} is an Unix command providing the same information.</div>
     *
     * @return the operation system name, or {@code null} if this information is not available.
     */
    public static String uname() {
        try {
            return System.getProperty("os.name");
        } catch (SecurityException e) {
            Logging.recoverableException(Logging.getLogger(Loggers.SYSTEM), OS.class, "uname", e);
            return null;
        }
    }

    /**
     * Returns the operating system SIS is currently on.
     *
     * @return the operation system.
     */
    public static OS current() {
        final String name = uname();
        if (name != null) {
            if (name.contains("Windows")) return WINDOWS;
            if (name.contains("Mac OS"))  return MAC_OS;
            if (name.contains("Linux"))   return LINUX;
        }
        return UNKNOWN;
    }

    /**
     * Loads the native library of the given name from the JAR file of the given class.
     * This method searches for a resource in the {@code /native/<os>} directory where
     * {@code <os>} is {@code windows}, {@code darwin} or {@code linux}.
     *
     * @param  caller  a class in the JAR file where to look for native resources.
     * @param  name    the native library name without {@code ".so"} or {@code ".dll"} extension.
     * @throws UnsatisfiedLinkError if the native library can not be loaded for the current OS.
     *
     * @see System#load(String)
     */
    public static void load(final Class<?> caller, final String name) {
        try {
            System.load(current().nativeLibrary(caller.getClassLoader(), name));
        } catch (IOException | SecurityException e) {
            throw (UnsatisfiedLinkError) new UnsatisfiedLinkError(e.getMessage()).initCause(e);
        }
    }

    /**
     * Returns an absolute path to the library of the given name in the JAR file.
     * If the resources can not be accessed by an absolute path, then this method
     * copies the resource in a temporary file.
     *
     * @param  loader  the loader of the JAR file where to look for native resources.
     * @param  name    the native library name without {@code ".so"} or {@code ".dll"} extension.
     * @return absolute path to the library (may be a temporary file).
     * @throws IOException if an error occurred while copying the library to a temporary file.
     * @throws SecurityException if the security manager denies loading resource, creating absolute path, <i>etc</i>.
     * @throws UnsatisfiedLinkError if no native resource has been found for the current OS.
     *
     * @see System#load(String)
     */
    private String nativeLibrary(final ClassLoader loader, String name) throws IOException {
        if (libdir != null) {
            final String ext = unix ? ".so" : ".dll";
            name = "native/" + libdir + '/' + name + ext;
            final URL res = loader.getResource(name);
            if (res != null) {
                try {
                    return new File(res.toURI()).getAbsolutePath();
                } catch (IllegalArgumentException | URISyntaxException e) {
                    Logging.recoverableException(Logging.getLogger(Loggers.SYSTEM), OS.class, "nativeLibrary", e);
                }
                final Path tmp = Files.createTempFile(name, ext).toAbsolutePath();
                tmp.toFile().deleteOnExit();
                try (InputStream in = res.openStream()) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                return tmp.toString();
            }
        }
        throw new UnsatisfiedLinkError(Errors.format(Errors.Keys.NativeInterfacesNotFound_2, uname(), name));
    }
}
