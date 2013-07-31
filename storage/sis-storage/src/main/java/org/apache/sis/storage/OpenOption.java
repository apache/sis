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
package org.apache.sis.storage;

import java.util.List;
import java.util.ArrayList;
import org.opengis.util.CodeList;


/**
 * Specifies how to open a {@link DataStore}.
 * This code list serves a similar purpose than the {@code java.nio.file} {@link java.nio.file.OpenOption},
 * except that it applies to data stores instead than files. The options provided by this code list are very
 * similar to the {@code java.nio.file} ones except for {@link #UNKNOWN}.
 *
 * <p>This code list is extensible: some {@code DataStore} subclasses may provide their own implementation
 * specific open options.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see java.nio.file.OpenOption
 */
public final class OpenOption extends CodeList<OpenOption> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -892034640198004572L;

    /**
     * List of all enumerations of this type.
     * Shall be declared before any enum declaration.
     */
    private static final List<OpenOption> VALUES = new ArrayList<>(5);

    /**
     * Open for reading data from a {@link DataStore}.
     *
     * @see java.nio.file.StandardOpenOption#READ
     */
    public static final OpenOption READ = new OpenOption("READ");

    /**
     * Open for overwriting existing data in a {@link DataStore}.
     *
     * @see java.nio.file.StandardOpenOption#WRITE
     */
    public static final OpenOption WRITE = new OpenOption("WRITE");

    /**
     * Open for appending new data in a {@link DataStore}.
     *
     * @see java.nio.file.StandardOpenOption#APPEND
     */
    public static final OpenOption APPEND = new OpenOption("APPEND");

    /**
     * Creates a new file or database if it does not exist.
     *
     * @see java.nio.file.StandardOpenOption#CREATE
     */
    public static final OpenOption CREATE = new OpenOption("CREATE");

    /**
     * Indicates that the open capabilities can not be determined.
     * This value may be returned by {@linkplain DataStoreProvider#getOpenCapabilities(StorageConnector)}
     * in two kind of situations:
     *
     * <ul>
     *   <li>The method can not look ahead far enough in the file header,
     *       for example because the buffer has not fetched enough bytes.</li>
     *   <li>The {@code DataStore} could potentially open anything.
     *       This is the case for example of the RAW image format.</li>
     * </ul>
     *
     * This option is exclusive with all other options.
     */
    public static final OpenOption UNKNOWN = new OpenOption("UNKNOWN");

    /**
     * Creates a new code list element of the given name.
     * The new element is automatically added to the {@link #VALUES} list.
     *
     * @param name The name of the new element. Must be unique in this code list.
     */
    private OpenOption(final String name) {
        super(name, VALUES);
    }

    /**
     * Returns the list of {@code OpenOption}s.
     *
     * @return The list of open options declared in the current JVM.
     */
    public static OpenOption[] values() {
        synchronized (VALUES) {
            return VALUES.toArray(new OpenOption[VALUES.size()]);
        }
    }

    /**
     * Returns the list of codes of the same kind than this code list element.
     * Invoking this method is equivalent to invoking {@link #values()}, except that
     * this method can be invoked on an instance of the parent {@code CodeList} class.
     *
     * @return The list of open options declared in the current JVM.
     */
    @Override
    public OpenOption[] family() {
        return values();
    }

    /**
     * Returns the open option that matches the given string, or returns a new one if none match it.
     * More specifically, this methods returns the first instance for which
     * <code>{@linkplain #name() name()}.{@linkplain String#equals equals}(code)</code> returns {@code true}.
     * If no existing instance is found, then a new one is created for the given name.
     *
     * @param code The name of the code to fetch or to create.
     * @return A code matching the given name.
     */
    public static OpenOption valueOf(String code) {
        return valueOf(OpenOption.class, code);
    }
}
