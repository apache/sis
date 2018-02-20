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
package org.apache.sis.internal.storage;

import java.nio.file.Path;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;


/**
 * A resource which is loaded from one or many files on an arbitrary file system. This interface
 * allows a resource (typically a {@linkplain org.apache.sis.storage.DataStore data store}) to
 * list the files that it uses. This information can be used for improving data management,
 * for example copy operations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 *
 * @todo Current name suggests resources provided by the file system (i.e. files and directories).
 *       Rename as {@code FileBackedResource}, {@code ResourceBackedByFiles} or {@code ResourceOnFileSystem}?
 */
public interface FileSystemResource extends Resource {
    /**
     * Gets the paths to all files used by this resource. This is typically the
     * files opened by a {@linkplain org.apache.sis.storage.DataStore data store}.
     *
     * @return files used by this resource. Should never be {@code null}.
     * @throws DataStoreException if an error on the file system prevent the creation of the list.
     */
    Path[] getResourcePaths() throws DataStoreException;
}
