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
 * Tools for <abbr>SIS</abbr> tests. This package defines {@link org.apache.sis.test.TestCase} as
 * the base class extended directly or indirectly by most (but not all) <abbr>SIS</abbr> tests.
 * This package defines also an {@link org.apache.sis.test.Assertions} class
 * with assertion methods commonly used in <abbr>SIS</abbr> tests.
 *
 * <h2>Test configuration</h2>
 * Some test behavior can be controlled from the command line by defining
 * {@linkplain java.lang.System#getProperties() system properties} values like below:
 *
 * <ul class="verbose">
 *   <li><b>{@code -Dorg.apache.sis.test.extensive=true}</b><br>
 *     For enabling more extensive tests.</li>
 *
 *   <li><b>{@code -Dorg.apache.sis.test.epsg=true}</b><br>
 *     For mandating the use of the <abbr>EPSG</abbr> geodetic dataset.
 *     If {@code false} (the default), an absence of <abbr>EPSG</abbr> database cause the test to be skipped.
 *     If {@code true}, an absence of <abbr>EPSG</abbr> database causes the test to fail.
 *     This is useful when the developer wants to be sure that no <abbr>EPSG</abbr> test
 *     has been accidentally skipped.</li>
 *
 *   <li><b>{@code -Dorg.apache.sis.test.postgresql=true}</b><br>
 *     For allowing the use of the PostgreSQL database on the local host.
 *     If {@code true}, then the {@code "SpatialMetadataTest"} database will be used if present.</li>
 *
 *   <li><b>{@code -Dorg.apache.sis.test.widget=true}</b><br>
 *     For allowing the tests to popup widgets with Swing.</li>
 *
 *   <li><b>{@code -Dorg.apache.sis.test.verbose=true}</b><br>
 *     For enabling verbose outputs to the {@linkplain java.lang.System#console() console} if any,
 *     or to the {@linkplain java.lang.System#out standard output stream} otherwise.
 *     If this property is not set, by default successful tests should not print anything.
 *     This flag is sometime useful for analyzing test outputs even when successful.
 * </li>
 *
 *   <li><b>{@code -Dorg.apache.sis.test.encoding=UTF-8}</b> (or any other valid encoding name)<br>
 *     For the encoding of the above-cited verbose output, and the encoding of logging messages
 *     sent to the {@linkplain java.util.logging.ConsoleHandler console handler}.
 *     This is useful on Windows platforms having a console encoding different than the
 *     platform encoding. If omitted, then the platform encoding will be used.</li>
 * </ul>
 *
 * Alternatively, the behavior can also be controlled by specifying a comma-separated list of values in
 * the {@code SIS_TEST_OPTIONS} environment variable. Values can be the tips of above-cited properties,
 * i.e. {@code extensive}, {@code epsg}, {@code postgresql}, {@code widget} or {@code verbose}.
 * If both system properties and the environment variable are defined, the system properties have precedence.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
package org.apache.sis.test;
