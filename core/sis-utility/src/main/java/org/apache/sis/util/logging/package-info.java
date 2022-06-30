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
 * Extensions to the {@linkplain java.util.logging JDK logging} framework.
 * This package provides:
 *
 * <ul>
 *   <li>A {@link org.apache.sis.util.logging.Logging} utility class with static utility methods.</li>
 *   <li>A {@link org.apache.sis.util.logging.PerformanceLevel} with configurable levels for
 *       logging the duration of lengthly processes.</li>
 *   <li>A {@link org.apache.sis.util.logging.MonolineFormatter} for formatting the log
 *       records on single line with colors, for easier reading on the console output.</li>
 * </ul>
 *
 * <h2>Choosing a logging framework</h2>
 * The SIS project uses the standard {@link java.util.logging.Logger} API for its logging.
 * It does not mean that users of the SIS library are forced to use that logging framework.
 * Java logging can be used as an API more powerful than {@link java.lang.System.Logger}
 * and other frameworks can redirect Java logging to themselves. For example adding the
 * {@code jul-to-slf4j.jar} dependency to a project is sufficient for redirecting Java logging to SLF4J.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/18/core/java-logging-overview.html">Java Logging Overview</a>
 *
 * @since 0.3
 * @module
 */
package org.apache.sis.util.logging;
