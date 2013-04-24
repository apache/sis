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
package org.apache.sis.metadata;

import java.util.Map;
import java.util.Collection;
import java.lang.reflect.Array;


/**
 * Whatever {@link MetadataStandard#asValueMap MetadataStandard.asValueMap(…)} shall contain
 * entries for null or empty values. By default the map does not provide
 * {@linkplain java.util.Map.Entry entries} for {@code null} metadata properties or
 * {@linkplain java.util.Collection#isEmpty() empty collections}.
 * This enumeration allows control on this behavior.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 *
 * @see MetadataStandard#asValueMap(Object, KeyNamePolicy, ValueExistencePolicy)
 */
public enum ValueExistencePolicy {
    /**
     * Includes all entries in the map, including those having a null value or an
     * empty collection.
     */
    ALL() {
        @Override boolean isSkipped(final Object value) {
            return false;
        }
    },

    /**
     * Includes only the non-null properties.
     * Collections are included no matter if they are empty or not.
     */
    NON_NULL() {
        @Override boolean isSkipped(final Object value) {
            return (value == null);
        }
    },

    /**
     * Includes only the properties that are non-null and non empty.
     * A non-null property is considered empty if:
     *
     * <ul>
     *   <li>It is a character sequence containing only {@linkplain Character#isWhitespace(int) whitespaces}.</li>
     *   <li>It is an {@linkplain Collection#isEmpty() empty collection}.</li>
     *   <li>It is an {@linkplain Map#isEmpty() empty map}.</li>
     *   <li>It is an empty array (of length 0).</li>
     * </ul>
     *
     * This is the default behavior of {@link AbstractMetadata#asMap()}.
     */
    NON_EMPTY() {
        @Override boolean isSkipped(final Object value) {
            return isNullOrEmpty(value);
        }
    };

    /**
     * Returns {@code true} if the given value shall be skipped for this policy.
     */
    abstract boolean isSkipped(Object value);

    /**
     * Returns {@code true} if the specified object is null or an empty collection, array or string.
     *
     * <p>This method intentionally does not inspect array or collection elements, since this method
     * is invoked from methods doing shallow copy or comparison. If we were inspecting elements,
     * we would need to add a check against infinite recursivity.</p>
     */
    static boolean isNullOrEmpty(final Object value) {
        return value == null
                || ((value instanceof CharSequence)  && isEmpty((CharSequence) value))
                || ((value instanceof Collection<?>) && ((Collection<?>) value).isEmpty())
                || ((value instanceof Map<?,?>)      && ((Map<?,?>) value).isEmpty())
                || (value.getClass().isArray()       && Array.getLength(value) == 0);
    }

    /**
     * Returns {@code true} if the given character sequence shall be considered empty.
     * The current implementation returns {@code true} if the sequence contains only
     * whitespaces in the sense of Java (i.e. ignoring line feeds, but not ignoring
     * non-breaking spaces). The exact criterion is not a committed part of the API
     * and may change in future SIS versions according experiences.
     */
    private static boolean isEmpty(final CharSequence value) {
        final int length = value.length();
        for (int i=0; i<length;) {
            final int c = Character.codePointAt(value, i);
            if (!Character.isWhitespace(c)) {
                return false;
            }
            i += Character.charCount(c);
        }
        return true;
    }
}
