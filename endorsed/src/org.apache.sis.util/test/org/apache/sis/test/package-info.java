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
 * Tools for SIS tests. This package defines a base class, {@link org.apache.sis.test.TestCase},
 * which is extended directly or indirectly by most (but not all) SIS tests.
 * This package defines also an {@link org.apache.sis.test.Assertions} with assertion methods
 * commonly used in SIS tests.
 *
 * <h2>Outputs configuration</h2>
 * By default, successful tests should not print anything. However, it is sometime useful to get
 * test outputs even when successful. This behavior can be controlled from the command line by
 * defining {@linkplain java.lang.System#getProperties() system properties} values like below:
 *
 * <ul class="verbose">
 *   <li><b>{@code -Dorg.apache.sis.test.extensive=true}</b><br>
 *     For enabling more extensive tests.</li>
 *
 *   <li><b>{@code -Dorg.apache.sis.test.verbose=true}</b><br>
 *     For enabling verbose outputs to the {@linkplain java.lang.System#console() console} if any,
 *     or to the {@linkplain java.lang.System#out standard output stream} otherwise.</li>
 *
 *   <li><b>{@code -Dorg.apache.sis.test.encoding=UTF-8}</b> (or any other valid encoding name)<br>
 *     For the encoding of the above-cited verbose output, and the encoding of logging messages
 *     sent to the {@linkplain java.util.logging.ConsoleHandler console handler}.
 *     This is useful on Windows platforms having a console encoding different than the
 *     platform encoding. If omitted, then the platform encoding will be used.</li>
 * </ul>
 *
 * Alternatively, the behavior can also be controlled by specifying a comma-separated
 * list of values in the {@code SIS_TEST_OPTIONS} environment variable. Values can be:
 *
 * <ul>
 *   <li><b>{@code extensive}:</b>
 *     For running more tests.</li>
 *   <li><b>{@code verbose}:</b>
 *     For enabling verbose outputs to the {@linkplain java.lang.System#console() console} if any,
 *     or to the {@linkplain java.lang.System#out standard output stream} otherwise.</li>
 * </ul>
 *
 * If both system properties and environment variable are defined, the system property has precedence.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
package org.apache.sis.test;
