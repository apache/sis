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
 *       are constrained to the [1 … 1] cardinality. Such simple features are very common.</li>
 * </ul>
 *
 * In addition, a feature type can inherit the properties of one or more other feature types.
 * Properties defined in the sub-type can override properties of the same name defined in the
 * {@linkplain org.apache.sis.feature.DefaultFeatureType#getSuperTypes() super-types}, provided
 * that values of the sub-type property are assignable to the super-type property.
 *
 * <div class="section">Naming</div>
 * Each feature type has a {@linkplain org.apache.sis.feature.DefaultFeatureType#getName() name},
 * which should be unique. Those names are the main criterion used for checking if a feature type
 * {@linkplain org.apache.sis.feature.DefaultFeatureType#isAssignableFrom is assignable from} another type.
 * Names can be {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped} for avoiding name collision.
 *
 * <div class="section">Class hierarchy</div>
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
 * {@code      └─} {@linkplain org.apache.sis.feature.AbstractOperation       Operation}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *             {@linkplain org.apache.sis.feature.AbstractFeature     Feature}             (<cite>sparse</cite> or <cite>dense</cite>)<br>
 *                                                                    Property<br>
 * {@code  ├─} {@linkplain org.apache.sis.feature.AbstractAttribute   Attribute}           (<cite>singleton</cite> or <cite>multi-valued</cite>)<br>
 * {@code  └─} {@linkplain org.apache.sis.feature.AbstractAssociation Feature association} (<cite>singleton</cite> or <cite>multi-valued</cite>)<br>
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
