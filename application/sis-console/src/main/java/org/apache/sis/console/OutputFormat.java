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
package org.apache.sis.console;

import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreProvider;


/**
 * The output format specified by the user as an option.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see FormattedOutputCommand
 *
 * @since 0.8
 * @module
 */
enum OutputFormat {
    /**
     * Arbitrary text, if possible similar to Well Known Text (WKT) but not necessarily.
     */
    TEXT(null),

    /**
     * Well Known Text format, used for geometric objects and Coordinate Reference Systems.
     */
    WKT(null),

    /**
     * Standardized XML format as defined by ISO 19115-3 or by GML.
     */
    XML(null),

    /**
     * XML format used for GPS data exchange.
     */
    GPX(org.apache.sis.internal.storage.gpx.StoreProvider.class);

    /**
     * The provider class for this format.
     */
    private final Class<? extends DataStoreProvider> providerClass;

    /**
     * Creates a new enumeration value.
     *
     * @param  provider The provider class for this format.
     */
    private OutputFormat(final Class<? extends DataStoreProvider> provider) {
        providerClass = provider;
    }

    /**
     * Returns the data store provider, or {@code null} if none.
     *
     * @todo on the JDK9 branch, use the {@code ServiceLoader} API that allows us
     *       to filter by class without instantiation.
     */
    final DataStoreProvider provider() throws InvalidOptionException {
        if (providerClass != null) {
            for (final DataStoreProvider provider : DataStores.providers()) {
                if (providerClass.isInstance(provider)) {
                    return provider;
                }
            }
        }
        return null;
    }
}
