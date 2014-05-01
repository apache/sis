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
 *   <li>{@linkplain org.apache.sis.feature.DefaultFeatureType Feature types} define the <em>structure</em> of a
 *       real-world representation. A feature type lists the attributes, operations, or associations to other
 *       features (collectively called “properties”) that a feature can have.
 *
 *       <div class="note"><b>Note:</b> a {@code FeatureType} in a Spatial Information System is equivalent to a
 *       {@link java.lang.Class} in the Java language. By extension, {@code AttributeType} and {@code OperationType}
 *       are equivalent to {@link java.lang.reflect.Field} and {@link java.lang.reflect.Method} respectively.</div></li>
 *
 *   <li>{@linkplain org.apache.sis.feature.DefaultFeature Feature instances} holds the <em>content</em> (or values)
 *       that describe one specific real-world object. For example the “Eiffel tower” is a feature <em>instance</em>
 *       belonging to the “Tower” feature <em>type</em>.
 *
 *       <div class="note"><b>Note:</b> feature instances are often called only {@code Feature}s.</div></li>
 * </ul>
 *
 * @author  Travis L. Pinney
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
package org.apache.sis.feature;
