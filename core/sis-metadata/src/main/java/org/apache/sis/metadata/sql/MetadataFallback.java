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
package org.apache.sis.metadata.sql;

import org.opengis.util.ControlledVocabulary;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.metadata.ServicesForUtility;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.xml.NilReason;


/**
 * A fallback providing hard-coded values of metadata entities.
 * Used when connection to the spatial metadata can not be established.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class MetadataFallback extends MetadataSource {
    /**
     * The unique instance of this fallback.
     */
    static final MetadataFallback INSTANCE = new MetadataFallback();

    /**
     * Creates the singleton.
     */
    private MetadataFallback() {
    }

    /**
     * Searches for the given metadata in the hard-coded list.
     *
     * @param  metadata  the metadata to search for.
     * @return the identifier of the given metadata, or {@code null} if none.
     */
    @Override
    public String search(final Object metadata) {
        ArgumentChecks.ensureNonNull("metadata", metadata);
        return null;
    }

    /**
     * Returns a hard-coded metadata filled with the data referenced by the specified identifier.
     * Alternatively, this method can also return a {@code CodeList} or {@code Enum} element.
     *
     * @param  <T>         the parameterized type of the {@code type} argument.
     * @param  type        the interface to implement, or {@code CodeList} or some {@code Enum} types.
     * @param  identifier  the identifier of hard-coded values for the metadata entity to be returned.
     * @return an implementation of the required interface, or the code list element.
     */
    @Override
    public <T> T lookup(final Class<T> type, final String identifier) {
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        Object value;
        if (ControlledVocabulary.class.isAssignableFrom(type)) {
            value = getCodeList(type, identifier);
        } else {
            value = null;
            if (type == Citation.class) {
                // TODO: move ServicesForUtility code here.
                value = ServicesForUtility.createCitation(identifier);
            }
            if (value == null) {
                return NilReason.MISSING.createNilObject(type);
            }
        }
        return type.cast(value);
    }

    /**
     * Ignored.
     */
    @Override
    public void addWarningListener(WarningListener<? super MetadataSource> listener) {
    }

    /**
     * Ignored.
     */
    @Override
    public void removeWarningListener(WarningListener<? super MetadataSource> listener) {
    }

    /**
     * Ignored.
     */
    @Override
    public void close() {
    }
}
