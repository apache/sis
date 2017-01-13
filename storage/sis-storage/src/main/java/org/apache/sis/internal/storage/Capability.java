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
import org.apache.sis.util.resources.Vocabulary;
import org.opengis.metadata.citation.Citation;
import org.opengis.util.InternationalString;


/**
 * One aspect in the set of capabilities of a class annotated by {@link Capabilities}.
 *
 * <p>This is not a committed API since the way to represent data store capabilities is likely to change.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
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
    WRITE(Vocabulary.Keys.Write);

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
     * @param  locale      the locale of the strings to return. Can not be null.
     * @param  resources   the {@code Vocabulary.getResources(locale)} value.
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
            final Capabilities annotation = provider.getClass().getAnnotation(Capabilities.class);
            String capabilities = null;
            if (annotation != null) {
                for (final Capability c : annotation.value()) {
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
            final Citation spec = provider.getFormat().getFormatSpecificationCitation();
            String title = title(spec, true).toString(locale);
            final String abbreviation = title(spec, false).toString(locale);
            if (!abbreviation.equals(title)) {
                title = resources.getString(Vocabulary.Keys.Parenthesis_2, title, abbreviation);
            }
            list[i++] = capabilities;
            list[i++] = title;
        }
        return list;
    }

    /**
     * Returns the title or alternate title of the given citation, or "untitled" if none.
     *
     * @param  preferTitle  {@code true} for preferring the title over alternate titles, or {@code false} for the opposite.
     */
    private static InternationalString title(final Citation spec, final boolean preferTitle) {
        final InternationalString title = spec.getTitle();
        if (preferTitle && title != null) return title;
        for (final InternationalString t : spec.getAlternateTitles()) {
            if (t != null) return t;
        }
        if (title != null) return title;
        return Vocabulary.formatInternational(Vocabulary.Keys.Untitled);
    }
}
