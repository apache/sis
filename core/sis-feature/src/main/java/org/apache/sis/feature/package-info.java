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

/**
 * Defines the structure and content of views of real-world phenomenon.
 * The phenomenon to represent (or a fundamental unit of information) is called <cite>a feature</cite>.
 * The term “feature” may be used in different contexts:
 *
 * <ul>
 *   <li>{@linkplain org.apache.sis.feature.DefaultFeatureType Feature types} define the <em>structure</em> of
 *       real-world representations. A feature type lists the attributes, operations, or associations to other
 *       features (collectively called “characteristics”) that a feature can have.
 *
 *       <p style="font-size:small"><b>Analogy:</b> a {@code FeatureType} in a Spatial Information System is equivalent
 *       to a {@link java.lang.Class} in the Java language. By extension, {@code AttributeType} and {@code Operation}
 *       are equivalent to {@link java.lang.reflect.Field} and {@link java.lang.reflect.Method} respectively.</p></li>
 *
 *   <li>{@linkplain org.apache.sis.feature.DefaultFeature Feature instances} (often called only {@code Feature}s)
 *       hold the <em>content</em> (or values) that describe one specific real-world object.
 *
 *       <p style="font-size:small"><b>Example:</b> the “Eiffel tower” is a <em>feature instance</em> belonging
 *       to the “Tower” <em>feature type</em>.</p></li>
 * </ul>
 *
 * {@section Class hierarchy}
 * The class hierarchy for feature <cite>types</cite> is derived from ISO 19109 specification.
 * The class hierarchy for feature <cite>instances</cite> is closely related:
 *
 * <table class="sis">
 * <caption>Feature class hierarchy</caption>
 * <tr>
 *   <th>Types</th>
 *   <th class="sep">Instances</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 *                 {@linkplain org.apache.sis.feature.AbstractIdentifiedType  Identified type}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.feature.DefaultFeatureType      Feature type}<br>
 * {@code  └─}                                                                Property type<br>
 * {@code      ├─} {@linkplain org.apache.sis.feature.DefaultAttributeType    Attribute type}<br>
 * {@code      ├─} {@linkplain org.apache.sis.feature.DefaultAssociationRole  Feature association role}<br>
 * {@code      └─} {@linkplain org.apache.sis.feature.DefaultFeatureOperation Operation}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *             {@linkplain org.apache.sis.feature.DefaultFeature     Feature}<br>
 *                                                                   Property<br>
 * {@code  ├─} {@linkplain org.apache.sis.feature.DefaultAttribute   Attribute}<br>
 * {@code  └─} {@linkplain org.apache.sis.feature.DefaultAssociation Feature association}<br>
 * </td></tr></table>
 *
 * @author  Travis L. Pinney
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
package org.apache.sis.feature;
