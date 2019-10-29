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

import java.nio.file.Path;
import javafx.concurrent.Task;


/**
 * A task executed on a file.
 * This is used for reporting more information in case of error.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param <V> the kind of value computed by the task.
 *
 * @since 1.1
 * @module
 */
public abstract class TaskOnFile<V> extends Task<V> {
    /**
     * The file or URI on which an operation was performed.
     */
    protected final Path file;

    /**
     * Creates a new task operating on the given file.
     *
     * @param  file  the file on which the task is operating.
     */
    protected TaskOnFile(final Path file) {
        this.file = file;
        setOnFailed(ExceptionReporter::show);
    }
}
