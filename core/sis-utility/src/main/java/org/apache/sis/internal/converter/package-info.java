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
 * Default set of {@link org.apache.sis.util.ObjectConverter} implementations.
 * Converter for a given pair of <cite>source</cite> and <cite>target</cite> classes
 * can be obtained with {@link org.apache.sis.internal.converter.ConverterRegistry}.
 * A system-wide instance of {@code ConverterRegistry} with a default set of conversions
 * is available as {@link org.apache.sis.internal.converter.SystemRegistry#INSTANCE}.
 *
 * <div class="section">Adding system-wide converters</div>
 * Applications can add system-wide custom converters either by explicit calls to the
 * {@code SystemRegistry.INSTANCE.register(ObjectConverter)} method, or by listing the
 * fully qualified classnames of their {@link ObjectConverter} instances in a file having
 * exactly the following name:
 *
 * {@preformat text
 *     META-INF/services/org.apache.sis.util.converter.ObjectConverter
 * }
 *
 * Applications deployed in a modularization framework like OSGi shall use only the
 * {@code META-INF} approach, because system converters are discarded every time the
 * classpath changes. Having the converters declared in {@code META-INF} ensure that
 * they will be reloaded when needed.
 *
 * <p>Alternatively, applications can also use their own {@code ConverterRegistry} instance.
 * Non-system instances do not scan for {@code META-INF} and do not discard their content on
 * classpath changes.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
package org.apache.sis.internal.converter;
