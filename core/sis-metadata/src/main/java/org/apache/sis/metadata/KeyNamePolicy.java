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

import org.opengis.annotation.UML;


/**
 * The name of the keys included in a {@link java.util.Map} of metadata. Those maps are created
 * by the {@link AbstractMetadata#asMap()} method. The keys in those map are {@link String}s which
 * can be inferred from the {@linkplain UML#identifier() UML identifier}, the name of the Javabeans
 * property, or the {@linkplain java.lang.reflect.Method#getName() method name}.
 *
 * <p>In GeoAPI implementation of ISO 19115, {@code UML_IDENTIFIER} and {@code JAVA_PROPERTY}
 * names are usually identical except for {@linkplain java.util.Collection collections}:
 * {@code JAVA_PROPERTY} names are plural when the property is a collection while
 * {@code UML_IDENTIFIER} usually stay singular no matter the property cardinality.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see MetadataStandard#asValueMap(Object, KeyNamePolicy, ValueExistencePolicy)
 */
public enum KeyNamePolicy {
    /**
     * The keys in the map are the {@linkplain UML#identifier() UML identifier} of the metadata
     * properties. If a property has no UML annotation, then the Javabeans property name is used
     * as a fallback.
     */
    UML_IDENTIFIER,

    /**
     * The keys in the map are the Javabeans property names. This is the method name with
     * the {@code get} or {@code is} prefix removed, and the first letter made lower-case.
     *
     * <p>This is the default type of names returned by {@link AbstractMetadata#asMap()}.</p>
     */
    JAVABEANS_PROPERTY,

    /**
     * The keys in the map are the plain {@linkplain java.lang.reflect.Method#getName() method names}.
     */
    METHOD_NAME,

    /**
     * The keys in the map are sentences inferred from the UML identifiers. This policy starts
     * with the same names than {@link #UML_IDENTIFIER}, searches for word boundaries (defined
     * as a lower case letter followed by a upper case letter) and inserts a space between the
     * words found. The first letter in the sentence is made upper-case. The first letters of
     * following words are made lower-case.
     */
    SENTENCE
}
