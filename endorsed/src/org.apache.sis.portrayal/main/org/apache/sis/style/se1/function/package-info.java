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
 * Functions defined by Symbology Encoding for transforming and formatting values.
 * All functions are {@linkplain org.opengis.filter.Expression expressions} which can be applied
 * on any resource, for example features, and can be used for computing any symbolizer property.
 *
 * <p>The transformation functions convert raw values to "symbolizable" quantities such as colors:</p>
 * <ul>
 *   <li>{@link org.apache.sis.style.se1.function.Categorize}: continuous values to distinct values,</li>
 *   <li>{@link org.apache.sis.style.se1.function.Interpolate}: continuous values to continuous values,</li>
 *   <li>{@link org.apache.sis.style.se1.function.Recode}: discrete values to other values.</li>
 * </ul>
 * Categorize and Interpolate are used by {@link org.apache.sis.style.se1.ColorMap} with the raster data as their
 * lookup value, and Recode is used by {@link org.apache.sis.style.se1.ColorReplacement} with the colors of external
 * graphics as lookup value.
 *
 * <p>The formatting functions are especially useful for the labels of text symbolizers:</p>
 * <ul>
 *   <li>{@link org.apache.sis.style.se1.function.FormatNumber} and
 *       {@link org.apache.sis.style.se1.function.FormatDate} for formatting numbers and dates,</li>
 *   <li>{@link org.apache.sis.style.se1.function.Substring},
 *       {@link org.apache.sis.style.se1.function.Concatenate},
 *       {@link org.apache.sis.style.se1.function.ChangeCase},
 *       {@link org.apache.sis.style.se1.function.Trim},
 *       {@link org.apache.sis.style.se1.function.StringPosition} and
 *       {@link org.apache.sis.style.se1.function.StringLength} for manipulating strings.</li>
 * </ul>
 *
 * <p>All those functions are also available by their names in Apache SIS filter factories,
 * see {@link org.apache.sis.style.se1.function.SymbologyFunctions} for the parameters.</p>
 *
 * @author  Johann Sorel (Geomatys)
 */
package org.apache.sis.style.se1.function;
