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
package org.apache.sis.storage.base;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.sql.MetadataSource;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.system.Modules;


/**
 * Base class of data store providers having an entry in the metadata SQL database.
 * The primary key in the {@code MD_Format} table must be the name given at construction time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class DocumentedStoreProvider extends URIDataStoreProvider {
    /**
     * The primary key to use for searching in the {@code MD_Format} table, or {@code null} if none.
     * This primary name is also the value returned by {@link #getShortName()} default implementation.
     */
    private final String name;

    /**
     * The format, created when first requested.
     *
     * @see #getFormat(StoreListeners)
     */
    private transient Format format;

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
    public final synchronized Format getFormat(final StoreListeners listeners) {
        if (format == null) {
            if (name != null) try {
                return format = MetadataSource.getProvided().lookup(Format.class, name);
            } catch (MetadataStoreException e) {
                final LogRecord record = Resources.forLocale(null).createLogRecord(Level.WARNING,
                        Resources.Keys.CanNotGetCommonMetadata_2, getShortName(), e.getLocalizedMessage());
                record.setSourceClassName(getClass().getCanonicalName());
                record.setSourceMethodName("getFormat");
                record.setLoggerName(Modules.STORAGE);
                if (listeners != null) {
                    listeners.warning(record);
                } else {
                    getLogger().log(record);
                }
            }
            format = super.getFormat();
        }
        return format;
    }
}
