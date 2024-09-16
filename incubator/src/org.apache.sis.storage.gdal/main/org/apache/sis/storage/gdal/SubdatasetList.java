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
package org.apache.sis.storage.gdal;

import java.util.List;
import java.util.AbstractList;
import java.util.LinkedHashMap;
import java.util.Locale;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.storage.DataStoreException;


/**
 * A list of sub-datasets where each element is opened when first requested.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class SubdatasetList extends AbstractList<Subdataset> {
    /**
     * Index uses in temporary arrays at construction time.
     */
    private static final int URL = 0, DESCRIPTION = 1;

    /**
     * The data store which owns this list.
     */
    private final GDALStore parent;

    /**
     * The <abbr>URL</abbr>s of all elements.
     */
    private final String[] urls;

    /**
     * The description associated to each <abbr>URL</abbr>.
     * May contain null values.
     */
    private final String[] descriptions;

    /**
     * The elements of this list. Elements are initially null and created when first requested.
     */
    private final Subdataset[] components;

    /**
     * Name of the <abbr>GDAL</abbr> driver to use for opening the sub-datasets.
     */
    private final String driver;

    /**
     * Creates a list of sub-datasets.
     *
     * @param  gdal      set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  parent    the data store which owns this list.
     * @param  driver    name of the <abbr>GDAL</abbr> driver to use for opening the sub-datasets.
     * @param  metadata  the metadata items for the {@code "SUBDATASETS"} domain.
     */
    SubdatasetList(final GDAL gdal, final GDALStore parent, final String driver, final List<String> metadata)
            throws DataStoreException
    {
        this.parent = parent;
        this.driver = driver;
        /*
         * URLs of all sub-dataset, optionally associated to their descriptions.
         * Keys are metadata keys. Values at index 0 are the URLs. Values at index 1 are descriptions.
         */
        final var subdatasets = new LinkedHashMap<String, String[]>(metadata.size());
        for (final String item : metadata) {
            final int splitAt = item.indexOf('=');
            if (splitAt > 0) {
                int index = URL;
                String key = separateKey(item, splitAt, "_NAME");
                if (key == null) {
                    key = separateKey(item, splitAt, "_DESC");
                    if (key == null) continue;
                    index = DESCRIPTION;
                }
                final String value = item.substring(splitAt+1).trim();
                subdatasets.computeIfAbsent(key, (_) -> new String[2])[index] = value;
            }
        }
        int i = subdatasets.size();
        urls = new String[i];
        descriptions = new String[i];
        components = new Subdataset[i];
        i = 0;
        for (final String[] entry : subdatasets.values()) {
            urls[i] = entry[0];
            descriptions[i++] = entry[1];
        }
    }

    /**
     * Separates the key from the values if the metadata item key has the given suffix.
     *
     * @param  item     the <abbr>GDAL</abbr> metadata item to parse.
     * @param  splitAt  position item the {@code '='} sign.
     * @param  suffix   the expected suffix in the metadata key.
     * @return the item key without its suffix, or {@code null} if not recognized.
     */
    private static String separateKey(final String item, int splitAt, final String suffix) {
        final int length = suffix.length();
        if (item.regionMatches(true, splitAt -= length, suffix, 0, length)) {
            return item.substring(0, splitAt).trim().toUpperCase(Locale.US);
        }
        return null;
    }

    /**
     * Returns the number of elements in this list.
     */
    @Override
    public int size() {
        return components.length;
    }

    /**
     * Returns the element at the given index.
     * The data set is opened when the element is requested for the first time.
     */
    @Override
    public Subdataset get(final int index) {
        synchronized (parent) {
            Subdataset component = components[index];
            if (component == null) try {
                final String url = urls[index];
                component = new Subdataset(parent, url, driver);
                component.description = descriptions[index];
                final int splitAt = url.lastIndexOf(':');
                if (splitAt >= 0) {
                    final String identifier = url.substring(splitAt + 1).trim();
                    if (!identifier.isBlank()) {
                        final var factory = parent.factory;
                        component.namespace = factory.createNameSpace(factory.createLocalName(parent.namespace, identifier), null);
                    }
                }
                components[index] = component;
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
            return component;
        }
    }
}
