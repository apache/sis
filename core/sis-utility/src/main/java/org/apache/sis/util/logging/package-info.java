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
 *   <li>A {@link org.apache.sis.util.logging.Logging} utility class with static methods for
 *       fetching a logger (see <cite>Choosing a logging framework</cite> below) or logging an
 *       exception.</li>
 *   <li>A {@link org.apache.sis.util.logging.PerformanceLevel} with configurable levels for
 *       logging the duration of lengthly processes.</li>
 *   <li>A {@link org.apache.sis.util.logging.MonolineFormatter} for formatting the log
 *       records on single line with colors, for easier reading on the console output.</li>
 * </ul>
 *
 * <div class="section">Choosing a logging framework</div>
 * The SIS project uses the standard {@link java.util.logging.Logger} API for its logging,
 * but this package allows redirection of logging messages to some other frameworks like
 * <a href="http://logging.apache.org/log4j/">Log4J</a>.
 * We recommend to stick to standard JDK logging when possible. However if inter-operability
 * with an other logging framework is required, then the only action needed is to include
 * <strong>one</strong> of the following JAR on the classpath:
 *
 * <ul>
 *   <li>{@code sis-logging-commons.jar} for Apache logging</li>
 *   <li>{@code sis-logging-log4j.jar} for Log4J logging</li>
 *   <li>Any other JAR registering a {@link org.apache.sis.util.logging.LoggerFactory} implementation.</li>
 * </ul>
 *
 * <div class="section">Note for SIS developers</div>
 * All SIS code should fetch their logger through a call to our custom
 * {@link org.apache.sis.util.logging.Logging#getLogger(String)} method instead than
 * the standard {@link java.util.logging.Logger#getLogger(String)} method. This is necessary in
 * order to give SIS a chance to redirect log events to an other logging framework.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see <a href="http://download.oracle.com/javase/6/docs/technotes/guides/logging/overview.html">Java Logging Overview</a>
 */
package org.apache.sis.util.logging;
