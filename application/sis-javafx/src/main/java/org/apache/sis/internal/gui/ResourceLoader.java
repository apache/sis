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
package org.apache.sis.internal.gui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.storage.folder.FolderStoreProvider;
import org.apache.sis.internal.storage.io.IOUtilities;


/**
 * Task in charge of loading a resource. No action is registered by default for successful completion;
 * caller should invoke {@link #setOnSucceeded(EventHandler)} for defining such action. However if the
 * operation fails, the default action is to report the error in a dialog box.
 *
 * <p>After the task has been configured, it can be executed by invoking
 * {@link BackgroundThreads#execute(Runnable)}.</p>
 *
 * @todo Set title. Add progress listener and cancellation capability.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ResourceLoader extends Task<Resource> {
    /**
     * The {@link Resource} input.
     * This is usually a {@link File} or {@link Path}.
     */
    private final Object source;

    /**
     * Creates a new task for opening the given input.
     *
     * @param  source  the source of the resource to load.
     *         This is usually a {@link File} or {@link Path}.
     */
    public ResourceLoader(final Object source) {
        this.source = source;
        setOnFailed(ExceptionReporter::show);
    }

    /**
     * Invoked for loading the resource in a background thread.
     * If the source is a directory, the directory content will
     * be parsed as a set of sub-resources.
     *
     * @return the resource.
     * @throws DataStoreException if an exception occurred while loading the resource.
     */
    @Override
    protected Resource call() throws DataStoreException {
        Object input = source;
        if (input instanceof StorageConnector) {
            input = ((StorageConnector) input).getStorage();
        }
        if ((input instanceof File && ((File) input).isDirectory()) ||
            (input instanceof Path && Files.isDirectory((Path) input)))
        {
            return FolderStoreProvider.INSTANCE.open((source instanceof StorageConnector)
                              ? (StorageConnector) source : new StorageConnector(input));
        }
        return DataStores.open(source);
    }

    /**
     * Returns the input filename, or "unknown" if we can not infer the filename.
     * This is used for reporting errors.
     */
    final String getFileName() {
        if (source instanceof StorageConnector) {
            return ((StorageConnector) source).getStorageName();
        }
        String name = IOUtilities.filename(source);
        if (name == null) {
            name = Vocabulary.format(Vocabulary.Keys.Unknown);
        }
        return name;
    }
}
