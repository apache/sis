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

import java.util.Locale;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Workaround;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.io.stream.IOUtilities;


/**
 * Thrown when no {@link DataStoreProvider} is found for a given storage object.
 * May also be thrown if a {@code DataStore} is instantiated directly but the data store
 * cannot handle the given input or output object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.4
 */
public class UnsupportedStorageException extends IllegalOpenParameterException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8754573140979570187L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public UnsupportedStorageException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public UnsupportedStorageException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause and no details message.
     *
     * @param cause  the cause for this exception.
     */
    public UnsupportedStorageException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public UnsupportedStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception which will format a localized message in the given locale.
     *
     * @param locale      the locale for the message to be returned by {@link #getLocalizedMessage()}.
     * @param key         one of {@link Resources.Keys} constants.
     * @param parameters  parameters to use for formatting the messages.
     */
    UnsupportedStorageException(final Locale locale, final short key, final Object... parameters) {
        super(locale, key, parameters);
    }

    /**
     * Creates a localized exception for an invalid input or output object given to a data store.
     * Arguments given to this constructor are hints for building an error message.
     *
     * @param locale   the locale of the message to be returned by {@link #getLocalizedMessage()}, or {@code null}.
     * @param format   short name or abbreviation of the data format (e.g. "CSV", "GML", "WKT", <i>etc</i>).
     * @param storage  the invalid input or output object. This is typically {@link StorageConnector#getStorage()}.
     * @param options  the option used for opening the file, or {@code null} or empty if unknown.
     *                 This method looks in particular for {@link java.nio.file.StandardOpenOption#READ} and
     *                 {@code WRITE} options for inferring if the data store was to be used as a reader or as a writer.
     *                 Those options can be obtained by {@code StorageConnector.getOption(OptionKey.OPEN_OPTIONS)}.
     *
     * @since 0.8
     */
    public UnsupportedStorageException(final Locale locale, final String format, final Object storage, final OpenOption... options) {
        super(locale, IOUtilities.isWrite(options) ? Resources.Keys.IllegalOutputTypeForWriter_2
                                                   : Resources.Keys.IllegalInputTypeForReader_2, format, type(storage));
    }

    /**
     * Returns the type of the given storage, with a special case for files or paths to directories.
     * This is a work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="10", fixed="25")
    private static Object type(final Object storage) {
        if ((storage instanceof File && ((File) storage).isDirectory()) ||
            (storage instanceof Path && Files.isDirectory((Path) storage)))
        {
            return "Directory";
        } else {
            return Classes.getClass(storage);
        }
    }
}
