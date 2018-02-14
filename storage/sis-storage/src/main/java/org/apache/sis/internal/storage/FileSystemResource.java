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
 * Files-related {@linkplain org.apache.sis.storage.Resource resource}.
 * This interface allows a resource to indicate it's used files, it
 * can then be used to improve data management.
 *
 * @author Johann Sorel (Geomatys)
 * @since 1.0
 * @module
 */
public interface FileSystemResource extends Resource {

    /**
     * Get all files used by this {@linkplain org.apache.sis.storage.DataStore data store}.
     *
     * @return Files used by this store. Should never be {@code null}.
     * @throws org.apache.sis.storage.DataStoreException if an error on the file system prevent
     *          the creation of the list.
     */
    Path[] getResourcePaths() throws DataStoreException;

}
