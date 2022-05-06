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
 * list the files that it uses. It may be used for copying or deleting resources if the caller
 * is certain that those files are not in use.
 *
 * <h2>Alternatives</h2>
 * <p>For copying data from one location to another, consider using
 * {@link org.apache.sis.storage.WritableAggregate#add(Resource)} instead.
 * The data store implementations may detect that some {@code add(…)} operations
 * can be performed by verbatim copy of files.</p>
 *
 * <p>For deleting data, consider using
 * {@link org.apache.sis.storage.WritableAggregate#remove(Resource)} instead.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface ResourceOnFileSystem extends Resource {
    /**
     * Gets the paths to files potentially used by this resource.
     * This is typically the files opened by a {@linkplain org.apache.sis.storage.DataStore data store}.
     * There is no guarantee that all files are in the same directory or that each file is used exclusively
     * by this data source (e.g. no guarantee that modifying or deleting a file will not impact other resources).
     *
     * <div class="note"><b>Example:</b>
     * a resources created for a GRIB file may use the following component files:
     * <ul>
     *   <li>The main GRIB file.</li>
     *   <li>If managed by the UCAR library, two auxiliary files next to the main GRIB file:
     *       the index file ({@code ".gbx9"}) and the collection file ({@code ".ncx"}).
     *       Those two files are owned exclusively by the resource.</li>
     *   <li>Eventually a GRIB table file. This table may be located in a path unrelated to
     *       to the path of the main file and may be shared by many resources.</li>
     * </ul>
     * </div>
     *
     * This method should return paths to files only. It should not return paths to directories.
     * The caller should verify that all paths are regular files;
     * non-existent paths should be omitted.
     *
     * @return files used by this resource. Should never be {@code null}.
     * @throws DataStoreException if an error on the file system prevent the creation of the list.
     *
     * @todo Should be renamed to something else, because current name creates a confusion with
     *       {@link org.apache.sis.storage.Aggregate#components()}.
     */
    Path[] getComponentFiles() throws DataStoreException;
}
