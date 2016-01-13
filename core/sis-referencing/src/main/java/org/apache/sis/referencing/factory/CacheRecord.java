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
package org.apache.sis.referencing.factory;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.io.Console;
import java.io.PrintWriter;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;


/**
 * Implementation of {@link ConcurrentAuthorityFactory#printCacheContent(PrintWriter)}.
 * Instance of this class represent a single record in the cache content to be listed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see ConcurrentAuthorityFactory#printCacheContent(PrintWriter)
 */
@Debug
final class CacheRecord implements Comparable<CacheRecord> {
    /**
     * The key-value pair, and the identity string representation of the value.
     */
    private final String key, value, identity;

    /**
     * The key numeric value, using for sorting purpose only.
     */
    private final int code;

    /**
     * Creates a new record for the given key-value pair.
     */
    private CacheRecord(final Object key, Object value) {
        identity = Classes.getShortClassName(value) + '@' + Integer.toHexString(System.identityHashCode(value));
        String text;
        if (value instanceof Collection<?>) {
            final Iterator<?> it = ((Collection<?>) value).iterator();
            value = it.hasNext() ? it.next() : null;
        }
        if (value instanceof IdentifiedObject) {
            text = String.valueOf(((IdentifiedObject) value).getName());
        } else {
            text = null;
        }
        this.value = text;
        this.key = text = String.valueOf(key);
        text = text.substring(text.indexOf('[') + 1);
        final int i = text.indexOf(' ');
        if (i >= 1) {
            text = text.substring(0, i);
        }
        int code;
        try {
            code = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            code = Integer.MAX_VALUE;
        }
        this.code = code;
    }

    /**
     * Compares with the given record for ordering.
     */
    @Override
    public int compareTo(final CacheRecord other) {
        if (code < other.code) return -1;
        if (code > other.code) return +1;
        return key.compareTo(other.key);
    }

    /**
     * Implementation of the public {@link ConcurrentAuthorityFactory#printCacheContent()} method.
     *
     * @param cache The cache.
     * @param out The output writer, or {@code null} for the standard output stream.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static void printCacheContent(final Map<?,?> cache, PrintWriter out) {
        final List<CacheRecord> list = new ArrayList<CacheRecord>(cache.size() + 10);
        int codeLength = 0;
        int identityLength = 0;
        for (final Map.Entry<?,?> entry : cache.entrySet()) {
            final CacheRecord record = new CacheRecord(entry.getKey(), entry.getValue());
            int length = record.key.length();
            if (length > codeLength) {
                codeLength = length;
            }
            length = record.identity.length();
            if (length > identityLength) {
                identityLength = length;
            }
            list.add(record);
        }
        codeLength += 2;
        identityLength += 2;
        final CacheRecord[] records = list.toArray(new CacheRecord[list.size()]);
        Arrays.sort(records);
        if (out == null) {
            final Console c = System.console();
            out = (c != null) ? c.writer() : new PrintWriter(System.out);
        }
        for (final CacheRecord record : records) {
            out.print(record.key);
            out.print(CharSequences.spaces(codeLength - record.key.length()));
            out.print(record.identity);
            if (record.value != null) {
                out.print(CharSequences.spaces(identityLength - record.identity.length()));
                out.println(record.value);
            } else {
                out.println();
            }
        }
        out.flush();
    }
}
