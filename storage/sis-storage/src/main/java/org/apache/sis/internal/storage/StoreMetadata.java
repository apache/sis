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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource;


/**
 * Metadata about of {@link DataStoreProvider}.
 * Some data stores can only read data while other can read and write.
 *
 * <p>This is not a committed API since the way to represent data store capabilities is likely to change.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StoreMetadata {
    /**
     * Returns a name for the data store format.
     * <b>This is not guaranteed to be a unique identifier!</b>
     * Should not be used as a way to uniquely identify a provider.
     * In many cases, this is the same than {@link DataStoreProvider#getShortName()}.
     *
     * @return a name for the data store format.
     *
     * @see StoreUtilities#getFormatName(DataStoreProvider)
     * @see DataStoreProvider#getShortName()
     */
    String formatName();

    /**
     * Indicates whether the data store created by the {@code open(…)} method can read and/or write data.
     *
     * @return information about whether the data store implementation can read and/or write data.
     */
    Capability[] capabilities();

    /**
     * Returns the suffixes that may be used with the name of the "main" file.
     * The "main" file is the file that users specify when opening the dataset.
     * The returned array should <em>not</em> include the suffixes of auxiliary files.
     *
     * <div class="note"><b>Example:</b>
     * GeoTIFF data are contained in files with the {@code ".tif"} or {@code ".tiff"} suffix,
     * sometime accompanied by auxiliary files with {@code ".prj"} and {@code ".tfw"} suffixes.
     * This method should return an array containing only {@code "tif"} or {@code "tiff"} strings,
     * without the leading dot.</div>
     *
     * The suffixes are case-insensitive (no need to declare both lower-case and upper-case variants)
     * and shall not contain the leading dot. The first element in the list is the preferred suffix
     * to use for new files.
     *
     * <p>The same suffixes may be used by many different formats. For example the {@code ".xml"} suffix
     * is used for files in many mutually incompatible formats. Consequently the file suffixes shall not
     * be used as format identifiers.</p>
     *
     * @return the filename suffixes, case insensitive. Never null but can be empty.
     */
    String[] fileSuffixes() default {};

    /**
     * Returns the types of resource that the {@link DataStoreProvider} may be able to produce.
     * Values in this array may be
     * {@link org.apache.sis.storage.Aggregate},
     * {@link org.apache.sis.storage.FeatureSet} or
     * {@link org.apache.sis.storage.GridCoverageResource}.
     *
     * @return information about the expected resource types which might be encounter with this format.
     */
    Class<? extends Resource>[] resourceTypes() default {};

    /**
     * Returns {@code true} if the data store should be tested last when searching for a data store capable
     * to open a given file. This method should return {@code true} if the data store claims to be able to
     * open a wide variety of files, in order to allow specialized data stores to be tested before generic
     * data stores.
     *
     * <p>If many data stores yield priority, the ordering between them is unspecified.</p>
     *
     * @return {@code true} if this data store should be tested after all "normal priority" data stores.
     */
    boolean yieldPriority() default false;
}
