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
 * can be obtained with {@link org.apache.sis.converter.ConverterRegistry}.
 * A system-wide instance of {@code ConverterRegistry} with a default set of conversions
 * is available as {@link org.apache.sis.converter.SystemRegistry#INSTANCE}.
 *
 * <h2>Adding system-wide converters</h2>
 * Applications can add system-wide custom converters either by explicit calls to the
 * {@code SystemRegistry.INSTANCE.register(ObjectConverter)} method, or by listing the class names
 * of their {@link org.apache.sis.util.ObjectConverter} implementations in {@code module-info.java}
 * as provider of the {@code org.apache.sis.util.ObjectConverter} service.
 *
 * <p>Applications deployed in a container framework like OSGi shall use only the service loader mechanism,
 * because system converters are discarded every time that the module-path changes. Having the converters
 * declared in {@code module-info.java} ensures that they will be reloaded when needed.</p>
 *
 * <p>Alternatively, applications can also use their own {@code ConverterRegistry} instance.
 * Non-system instances do not scan for {@code module-info.class} and do not discard their content
 * on module-path changes.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.3
 */
package org.apache.sis.converter;
