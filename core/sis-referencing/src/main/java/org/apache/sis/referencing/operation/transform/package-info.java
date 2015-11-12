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
 * Conversions or transformations of multi-dimensional coordinate points.
 * {@link org.opengis.referencing.operation.MathTransform} provides a single API for
 * coordinate conversions or transformations, including map projections.
 * Each {@code MathTransform} instances can:
 *
 * <ul>
 *   <li>transform a single point,</li>
 *   <li>transform efficiently an array of coordinates,</li>
 *   <li>transform a Java2D {@link java.awt.Shape} ({@link org.opengis.referencing.operation.MathTransform2D} only),</li>
 *   <li>compute the transform derivative at a location (for advanced users),</li>
 *   <li>be concatenated in a conversion or transformation chain.</li>
 * </ul>
 *
 * {@code MathTransform} are truly <var>n</var>-dimensional, but specialized implementations
 * for 1D and 2D cases are provided for performance reasons or for inter-operability with Java2D.
 * In the 2D case, Apache SIS provides instances of the standard {@link java.awt.geom.AffineTransform}
 * class when possible.
 *
 * <p>This package does not include map projections, which are a special kind of transforms defined
 * in their own {@linkplain org.apache.sis.referencing.operation.projection projection} package.</p>
 *
 *
 * <div class="section">Creating math transforms</div>
 * {@code MathTransform} instances can be created either directly or indirectly.
 * The recommended way is the indirect one: first
 * {@linkplain org.apache.sis.referencing.CRS#findOperation find the coordinate operation}
 * (generally from a pair of <var>source</var> and <var>target</var> CRS), then invoke
 * {@link org.opengis.referencing.operation.CoordinateOperation#getMathTransform()}.
 * However sophisticated users can also create math transforms explicitely from a group of parameter values
 * using the {@linkplain org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory math
 * transform factory}.
 *
 *
 * <div class="section">Non-spatial coordinates</div>
 * {@code MathTransform} usually performs conversions or transformations from points given in a
 * {@linkplain org.apache.sis.referencing.operation.DefaultCoordinateOperation#getSourceCRS()
 * source coordinate reference system} to coordinate values for the same points in the
 * {@linkplain org.apache.sis.referencing.operation.DefaultCoordinateOperation#getTargetCRS()
 * target coordinate reference system}. However the conversions are not necessarily between CRS;
 * a {@code MathTransform} can also be used for converting the sample values in a raster for example.
 * Such kind of transforms are named {@linkplain org.apache.sis.referencing.operation.transform.TransferFunction
 * transfer functions}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Adrian Custer (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
package org.apache.sis.referencing.operation.transform;
