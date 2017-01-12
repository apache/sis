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

import java.util.logging.Logger;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.sql.MetadataSource;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.internal.system.Modules;


/**
 * Base class of data store provider having an entry in the metadata SQL database.
 * The primary key in the {@code MD_Format} table must be the name given at construction time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class DocumentedStoreProvider extends DataStoreProvider {
    /**
     * The primary key to use for searching in the {@code MD_Format} table, or {@code null} if none.
     * This primary name is also the value returned by {@link #getShortName()} default implementation.
     */
    private final String name;

    /**
     * {@code true} if the call to {@link #getFormat()} caught an exception. In such case,
     * we log a warning only the first time and use a finer logging level the other times.
     * The intend is to avoid polluting the logs with too many warnings.
     */
    private volatile boolean logged;

    /**
     * Creates a new provider.
     * The primary key given in argument is also the value returned by {@link #getShortName()} default implementation.
     * If this is not the desired value for the format short name, then subclass should override {@code getShortName()}.
     *
     * @param  name  the primary key to use for searching in the {@code MD_Format} table, or {@code null} if none.
     */
    protected DocumentedStoreProvider(final String name) {
        this.name = name;
    }

    /**
     * Returns a short name or abbreviation for the data format.
     * The default implementation returns the primary key given at construction time.
     * If that primary key is not an appropriate format short name, then subclass should override this method.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return name;
    }

    /**
     * Returns a more complete description of the format.
     *
     * @return a description of the data format.
     */
    @Override
    public final Format getFormat() {
        return getFormat(null);
    }

    /**
     * Returns a more complete description of the format, sending warnings to the given listeners if non-null.
     *
     * @param  listeners  where to report the warning in case of error, or {@code null} if none.
     * @return a description of the data format.
     */
    public final Format getFormat(final WarningListeners<DataStore> listeners) {
        /*
         * Note: this method does not cache the format because such caching is already done by MetadataSource.
         */
        if (name != null) try {
            return MetadataSource.getProvided().lookup(Format.class, name);
        } catch (MetadataStoreException e) {
            if (listeners != null) {
                listeners.warning(null, e);
            } else {
                final Logger logger = Logging.getLogger(Modules.STORAGE);
                if (!logged) {
                    logged = true;      // Not atomic - not a big deal if we use warning level twice.
                    Logging.unexpectedException(logger, getClass(), "getFormat", e);
                } else {
                    Logging.recoverableException(logger, getClass(), "getFormat", e);
                }
            }
        }
        return super.getFormat();
    }
}
