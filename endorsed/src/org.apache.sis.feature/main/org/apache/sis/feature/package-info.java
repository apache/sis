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
 * The phenomenon to represent (or a fundamental unit of information) is called <dfn>a feature</dfn>.
 * The term “feature” may be used in different contexts:
 *
 * <ul class="verbose">
 *   <li><b>{@linkplain org.apache.sis.feature.DefaultFeatureType Feature types}</b><br>
 *       Define the <em>structure</em> of real-world representations. A feature type lists the
 *       {@linkplain org.apache.sis.feature.DefaultAttributeType attributes},
 *       {@linkplain org.apache.sis.feature.AbstractOperation operations} or
 *       {@linkplain org.apache.sis.feature.DefaultAssociationRole associations to other features}
 *       (collectively called “{@linkplain org.apache.sis.feature.DefaultFeatureType#getProperties(boolean) properties}”
 *       or “characteristics”) that a feature can have.
 *
 *       <div class="note"><b>Analogy:</b> a {@code FeatureType} in a Spatial Information System is equivalent to a
 *       {@link java.lang.Class} in the Java language. By extension, {@code AttributeType} and {@code Operation} are
 *       equivalent to {@link java.lang.reflect.Field} and {@link java.lang.reflect.Method} respectively.</div></li>
 *
 *   <li><b>{@linkplain org.apache.sis.feature.AbstractFeature Feature instances}</b> (often called only Features)<br>
 *       Hold the <em>content</em> (or values) that describe one specific real-world object.
 *
 *       <div class="note"><b>Example:</b> the “Eiffel tower” is a <em>feature instance</em> belonging
 *       to the “Tower” <em>feature type</em>.</div></li>
 *
 *   <li><b>{@linkplain org.apache.sis.feature.DefaultFeatureType#isSimple() Simple features}</b><br>
 *       Are instances of a feature type with no association to other features, and where all attributes
 *       have [1 … 1] multiplicity. Such simple features are very common.</li>
 * </ul>
 *
 * In addition, a feature type can inherit the properties of one or more other feature types.
 * Properties defined in the sub-type can override properties of the same name defined in the
 * {@linkplain org.apache.sis.feature.DefaultFeatureType#getSuperTypes() super-types}, provided
 * that values of the sub-type property are assignable to the super-type property.
 *
 * <h2>Naming</h2>
 * Each feature type has a {@linkplain org.apache.sis.feature.DefaultFeatureType#getName() name},
 * which should be unique. Those names are the main criterion used for checking if a feature type
 * {@linkplain org.apache.sis.feature.DefaultFeatureType#isAssignableFrom is assignable from} another type.
 * Names can be {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped} for avoiding name collision.
 *
 * <h2>Class hierarchy</h2>
 * The class hierarchy for feature <em>types</em> is derived from ISO 19109 specification.
 * The class hierarchy for feature <em>instances</em> is closely related:
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
 * {@code      └─} {@linkplain org.apache.sis.feature.AbstractOperation       Operation}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *                 Object<br>
 * {@code  ├─}     {@linkplain org.apache.sis.feature.AbstractFeature     Feature}             (<i>sparse</i> or <i>dense</i>)<br>
 * {@code  └─}                                                            Property<br>
 * {@code      ├─} {@linkplain org.apache.sis.feature.AbstractAttribute   Attribute}           (<i>singleton</i> or <i>multi-valued</i>)<br>
 * {@code      └─} {@linkplain org.apache.sis.feature.AbstractAssociation Feature association} (<i>singleton</i> or <i>multi-valued</i>)<br>
 * </td></tr></table>
 *
 * <h2>Instantiation</h2>
 * Classes defined in this package are rarely instantiated directly (by a {@code new} statement).
 * Instead, those classes are instantiated indirectly by invoking a method on a parent container,
 * or by using a builder. The starting point is {@code FeatureType}, which may be created by a
 * {@link org.apache.sis.feature.builder.FeatureTypeBuilder} or may be provided by a
 * {@link org.apache.sis.storage.DataStore} reading a data file.
 * Once a {@code FeatureType} has been obtained, {@code Feature}s can be instantiated by calls to the
 * {@link org.apache.sis.feature.DefaultFeatureType#newInstance() FeatureType.newInstance()} method.
 * Once a {@code Feature} instance has been obtained, {@code Attribute}s can be instantiated indirectly
 * by calls to the {@link org.apache.sis.feature.AbstractFeature#setPropertyValue Feature.setPropertyValue(…)} method.
 *
 * @author  Travis L. Pinney
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   0.5
 */
package org.apache.sis.feature;
