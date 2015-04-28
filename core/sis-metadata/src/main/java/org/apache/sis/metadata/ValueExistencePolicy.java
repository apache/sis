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
import org.apache.sis.xml.NilObject;
import org.apache.sis.xml.NilReason;


/**
 * Whatever {@link MetadataStandard#asValueMap MetadataStandard.asValueMap(…)} shall contain entries for null,
 * {@linkplain org.apache.sis.xml.NilObject nil} or empty values. By default the value map does not provide
 * {@linkplain java.util.Map.Entry entries} for {@code null} metadata properties, nil objects or
 * {@linkplain java.util.Collection#isEmpty() empty collections}.
 * This enumeration allows to control this behavior.
 *
 * <div class="section">Difference between null and nil</div>
 * A null property is a reference which is {@code null} in the Java sense.
 * Null references can be used for missing properties when no information is provided about why the property is missing.
 * On the other hand, a nil object is a placeholder for a missing property similar in purpose to {@code null} references,
 * except that an explanation about why the property is missing can be attached to those objects.
 * Those explanations can be obtained by calls to the {@link org.apache.sis.xml.NilReason#forObject(Object)} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see MetadataStandard#asValueMap(Object, KeyNamePolicy, ValueExistencePolicy)
 */
public enum ValueExistencePolicy {
    /**
     * Includes all entries in the map, including those having a null value or an empty collection.
     */
    ALL() {
        /** Never skip values. */
        @Override boolean isSkipped(final Object value) {
            return false;
        }

        /** Substitutes null or empty collections by a null singleton element
            in order to make the property visible in {@code TreeNode}. */
        @Override boolean substituteByNullElement(final Collection<?> values) {
            return (values == null) || values.isEmpty();
        }
    },

    /**
     * Includes only the non-null properties.
     * {@link org.apache.sis.xml.NilObject}s are included.
     * Collections are included no matter if they are empty or not.
     *
     * <p>The set of {@code NON_NULL} properties is a subset of {@link #ALL} properties.</p>
     */
    NON_NULL() {
        /** Skips all null values. */
        @Override boolean isSkipped(final Object value) {
            return (value == null);
        }

        /** Substitutes empty collections by a null singleton element, but not
            null references since they are supposed to be skipped by this policy. */
        @Override boolean substituteByNullElement(final Collection<?> values) {
            return (values != null) && values.isEmpty();
        }
    },

    /**
     * Includes only the non-null and non-nil properties.
     * Collections are included no matter if they are empty or not.
     *
     * <p>The set of {@code NON_NIL} properties is a subset of {@link #NON_NULL} properties.</p>
     *
     * @since 0.4
     */
    NON_NIL() {
        /** Skips all null or nil values. */
        @Override boolean isSkipped(final Object value) {
            return (value == null) || (value instanceof NilObject) || NilReason.forObject(value) != null;
        }

        /** Substitutes empty collections by a null singleton element, but not
            null references since they are supposed to be skipped by this policy. */
        @Override boolean substituteByNullElement(final Collection<?> values) {
            return (values != null) && values.isEmpty();
        }
    },

    /**
     * Includes only the properties that are non-null, non-nil and non empty.
     * A non-null and non-nil property is considered empty in any of the following cases:
     *
     * <ul>
     *   <li>It is a character sequence containing only {@linkplain Character#isWhitespace(int) whitespaces}.</li>
     *   <li>It is an {@linkplain Collection#isEmpty() empty collection}.</li>
     *   <li>It is an {@linkplain Map#isEmpty() empty map}.</li>
     *   <li>It is an empty array (of length 0).</li>
     * </ul>
     *
     * This is the default behavior of {@link AbstractMetadata#asMap()}.
     *
     * <p>The set of {@code NON_EMPTY} properties is a subset of {@link #NON_NIL} properties.</p>
     */
    NON_EMPTY() {
        /** Skips all null or empty values. */
        @Override boolean isSkipped(final Object value) {
            return isNullOrEmpty(value);
        }

        /** Never substitute null or empty collections since they should be skipped. */
        @Override boolean substituteByNullElement(final Collection<?> values) {
            return false;
        }
    };

    /**
     * Returns {@code true} if the given value shall be skipped for this policy.
     */
    abstract boolean isSkipped(Object value);

    /**
     * Returns {@code true} if {@link TreeNode} shall substitute the given collection by
     * a singleton containing only a null element.
     *
     * <p><b>Purpose:</b>
     * When a collection is null or empty, while not excluded according this {@code ValueExistencePolicy},
     * we need an empty space for making the metadata property visible in {@code TreeNode}.</p>
     */
    abstract boolean substituteByNullElement(Collection<?> values);

    /**
     * Returns {@code true} if the specified object is null or an empty collection, array or string.
     *
     * <p>This method intentionally does not inspect array or collection elements, since this method
     * is invoked from methods doing shallow copy or comparison. If we were inspecting elements,
     * we would need to add a check against infinite recursivity.</p>
     *
     * <p>This method does not check for the {@link org.apache.sis.util.Emptiable} interface because
     * the {@code isEmpty()} method may be costly (for example {@link AbstractMetadata#isEmpty()}
     * iterates over all the metadata tree). Instead, the check for {@code Emptiable} will be done
     * explicitely by the caller when appropriate.</p>
     */
    static boolean isNullOrEmpty(final Object value) {
        if (value == null)                  return true;
        if (value instanceof NilObject)     return true;
        if (value instanceof CharSequence)  return isEmpty((CharSequence) value);
        if (value instanceof Collection<?>) return ((Collection<?>) value).isEmpty();
        if (value instanceof Map<?,?>)      return ((Map<?,?>) value).isEmpty();
        if (value.getClass().isArray())     return Array.getLength(value) == 0;
        return NilReason.forObject(value) != null;
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
