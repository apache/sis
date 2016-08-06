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
package org.apache.sis.feature.builder;

import java.util.Set;
import java.util.EnumSet;
import org.apache.sis.feature.FeatureOperations;


/**
 * Roles that can be associated to some attributes for instructing {@code FeatureTypeBuilder}
 * how to generate pre-defined operations. Those pre-defined operations are:
 *
 * <ul>
 *   <li>A {@linkplain FeatureOperations#compound compound operation} for generating a unique identifier
 *       from an arbitrary amount of attribute values.</li>
 *   <li>A {@linkplain FeatureOperations#link link operation} for referencing a geometry to be used as the
 *       <em>default</em> geometry.</li>
 *   <li>An {@linkplain FeatureOperations#envelope operation} for computing the bounding box of all geometries
 *       found in the feature. This operation is automatically added if the feature contains a default geometry.</li>
 * </ul>
 *
 * This enumeration allows user code to specify which feature attribute to use for creating those operations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see Attribute#addRole(AttributeRole)
 */
public enum AttributeRole {
    /**
     * Attribute value will be part of a unique identifier for the feature instance.
     * An arbitrary amount of attributes can be flagged as identifier components:
     *
     * <ul>
     *   <li>If no attribute has this role, then no attribute is marked as feature identifier.</li>
     *   <li>If exactly one attribute has this role, then a synthetic attribute named {@code "@identifier"}
     *       will be created as a {@linkplain FeatureOperations#link link} to the flagged attribute.</li>
     *   <li>If more than one attribute have this role, then a synthetic attribute named {@code "@identifier"}
     *       will be created as a {@linkplain FeatureOperations#compound compound key} made of all flagged
     *       attributes. The separator character can be modified by a call to
     *       {@link FeatureTypeBuilder#setIdentifierDelimiters(String, String, String)}</li>
     * </ul>
     *
     * @see FeatureTypeBuilder#setIdentifierDelimiters(String, String, String)
     */
    IDENTIFIER_COMPONENT,

    /**
     * Attribute value will be flagged as the <em>default</em> geometry.
     * Feature can have an arbitrary amount of geometry attributes,
     * but only one can be flagged as the default geometry.
     */
    DEFAULT_GEOMETRY;

    /**
     * Returns the union of the given set of attribute roles.
     */
    static Set<AttributeRole> merge(final Set<AttributeRole> oldValue,
                                    final Set<AttributeRole> newValue)
    {
        final EnumSet<AttributeRole> union = EnumSet.copyOf(oldValue);
        return union.addAll(newValue) ? union : oldValue;
    }
}
