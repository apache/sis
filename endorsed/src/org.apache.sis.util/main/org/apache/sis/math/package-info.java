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
 * <ul class="verbose">
 *   <li>Mathematical functions as static methods in {@link org.apache.sis.math.MathFunctions} and
 *       {@link org.apache.sis.math.DecimalFunctions}.</li>
 *
 *   <li>{@link org.apache.sis.math.Statistics} accumulator, optionally with statistics on derivatives
 *       and {@linkplain org.apache.sis.math.StatisticsFormat tabular formatting}.</li>
 *
 *   <li>Simple equations for {@link org.apache.sis.math.Line} and {@link org.apache.sis.math.Plane}
 *       with capability to determine the coefficients from a set of coordinates.
 *
 *       <div class="note"><b>Note:</b>
 *       {@code Line} and {@code Plane} classes are not geometric objects since they are not bounded in space.
 *       For example, the {@link java.awt.geom.Line2D} geometry have starting and ending points, while the
 *       {@code Line} class in this package extends to infinite.</div></li>
 *
 *   <li>{@link org.apache.sis.math.Vector} of real numbers, typically as views over arrays of primitive types.
 *       Those views make abstraction of the data type (e.g. {@code float}, {@code double}, {@code int},
 *       unsigned {@code int}, <i>etc.</i>).</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   0.3
 */
package org.apache.sis.math;
