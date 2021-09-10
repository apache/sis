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

import java.util.Locale;
import java.util.Collection;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.BackingStoreException;

// Branch-dependent imports
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;


/**
 * Capabilities of a class annotated by {@link StoreMetadata}.
 *
 * <p>This is not a committed API since the way to represent data store capabilities is likely to change.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
public enum Capability {
    /**
     * The annotated implementation can read data.
     */
    READ(Vocabulary.Keys.Read),

    /**
     * The annotated implementation can write data.
     */
    WRITE(Vocabulary.Keys.Write),

    /**
     * The annotated implementation can create new data.
     */
    CREATE(Vocabulary.Keys.Create);

    /**
     * The {@link Vocabulary} key to use for fetching a localized name.
     */
    private final short resourceKey;

    /**
     * Creates a new capability item.
     */
    private Capability(final short key) {
        resourceKey = key;
    }

    /**
     * Lists the capabilities of all data stores together with their name.
     * The returned array is twice the amount of providers, with the capabilities
     * at even indices and the format name at odd indices.
     *
     * <p>This method is for internal usage by {@link org.apache.sis.setup.About}
     * only and may change without notice in any future Apache SIS version.</p>
     *
     * @param  locale     the locale of the strings to return. Can not be null.
     * @param  resources  the {@code Vocabulary.getResources(locale)} value.
     * @return localized string representations of the capabilities of all data store providers.
     */
    public static String[] providers(final Locale locale, final Vocabulary resources) {
        final Collection<DataStoreProvider> providers = DataStores.providers();
        final int count = providers.size();
        final String[] list = new String[count * 2];
        int i = 0;
        for (final DataStoreProvider provider : providers) {
            /*
             * Build a slash-separated list of capabilities. Example: "Read / write".
             */
            final StoreMetadata metadata = provider.getClass().getAnnotation(StoreMetadata.class);
            String capabilities = null;
            if (metadata != null) {
                for (final Capability c : metadata.capabilities()) {
                    final String e = resources.getString(c.resourceKey);
                    capabilities = (capabilities == null) ? e : resources.getString(
                            Vocabulary.Keys.SlashSeparatedList_2, capabilities, e.toLowerCase(locale));
                }
            }
            if (capabilities == null) {
                capabilities = resources.getString(Vocabulary.Keys.Unknown);
            }
            /*
             * Get a title for the format, followed by the short name between parenthesis
             * if it does not repeat the main title.
             */
            String title, complement;
            try {
                title = title(provider.getFormat().getFormatSpecificationCitation()).toString(locale);
                complement = provider.getShortName();
            } catch (BackingStoreException e) {
                title = provider.getShortName();
                complement = Exceptions.getLocalizedMessage(Exceptions.unwrap(e), locale);
            }
            if (complement != null && !complement.equals(title)) {
                title = resources.getString(Vocabulary.Keys.Parenthesis_2, title, complement);
            }
            list[i++] = capabilities;
            list[i++] = title;
        }
        return list;
    }

    /**
     * Returns the title or alternate title of the given citation, or "untitled" if none.
     */
    private static InternationalString title(final Citation specification) {
        final InternationalString title = specification.getTitle();
        if (title != null) return title;
        for (final InternationalString t : specification.getAlternateTitles()) {
            if (t != null) return t;
        }
        return Vocabulary.formatInternational(Vocabulary.Keys.Untitled);
    }
}
