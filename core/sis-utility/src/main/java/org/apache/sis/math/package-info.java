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
 * A set of mathematical objects and algebraic utilities.
 * This package provides:
 *
 * <ul>
 *   <li>Static functions in {@link org.apache.sis.math.MathFunctions} and {@link org.apache.sis.math.DecimalFunctions}.</li>
 *   <li>{@link org.apache.sis.math.Statistics} accumulator, optionally with statistics on derivatives
 *       and {@linkplain org.apache.sis.math.StatisticsFormat tabular formatting}.</li>
 *   <li>Simple equations for {@link org.apache.sis.math.Line} and {@link org.apache.sis.math.Plane}
 *       with capability to determine the coefficients from a set of coordinates.</li>
 * </ul>
 *
 * <div class="note"><b>Note:</b>
 * {@code Line} and {@code Plane} classes are not geometric objects since they are not bounded in space.
 * For example the {@link java.awt.geom.Line2D} geometry have starting and ending points, while the
 * {@code Line} class in this package extends to infinite.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
package org.apache.sis.math;
