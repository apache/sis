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
 * {@section Standard parameters}
 * Some {@code MathTransform} implementations declare a single {@link org.opengis.parameter.ParameterDescriptorGroup}
 * constant named {@code PARAMETERS}. Each group describes all the parameters expected by the
 * {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod operation method}
 * associated to the transform implementation.
 * The set of parameters varies for each operation or projection, but the following can be considered typical:
 *
 * <ul>
 *   <li>A <cite>semi-major</cite> and <cite>semi-minor</cite> axis length in metres.</li>
 *   <li>A <cite>central meridian</cite> and <cite>latitude of origin</cite> in decimal degrees.</li>
 *   <li>A <cite>scale factor</cite>, which default to 1.</li>
 *   <li>A <cite>false easting</cite> and <cite>false northing</cite> in metres, which default to 0.</li>
 * </ul>
 *
 * <p>Each descriptor has many aliases, and those aliases may vary between different projections.
 * For example the <cite>false easting</cite> parameter is usually called {@code "false_easting"}
 * by OGC, while EPSG uses various names like "<cite>False easting</cite>" or "<cite>Easting at
 * false origin</cite>".</p>
 *
 * {@section Dynamic parameters}
 * A few non-standard parameters are defined for compatibility reasons,
 * but delegates their work to standard parameters. Those dynamic parameters are not listed in the
 * {@linkplain org.apache.sis.parameter.DefaultParameterValueGroup#values() parameter values}.
 * Dynamic parameters are:
 *
 * <ul>
 *   <li>{@code "earth_radius"}, which copy its value to the {@code "semi_major"} and
 *       {@code "semi_minor"} parameter values.</li>
 *   <li>{@code "inverse_flattening"}, which compute the {@code "semi_minor"} value from
 *       the {@code "semi_major"} parameter value.</li>
 *   <li>{@code "standard_parallel"} expecting an array of type {@code double[]}, which copy
 *       its elements to the {@code "standard_parallel_1"} and {@code "standard_parallel_2"}
 *       parameter scalar values.</li>
 * </ul>
 *
 * <p>The main purpose of those dynamic parameters is to support some less commonly used conventions
 * without duplicating the most commonly used conventions. The alternative ways are used in NetCDF
 * files for example, which often use spherical models instead than ellipsoidal ones.</p>
 *
 *
 * {@section Mandatory and optional parameters}
 * <a name="Obligation">Parameters are flagged as either <cite>mandatory</cite> or <cite>optional</cite></a>.
 * A parameter may be mandatory and still have a default value. In the context of this package, "mandatory"
 * means that the parameter is an essential part of the projection defined by standards.
 * Such mandatory parameters will always appears in any <cite>Well Known Text</cite> (WKT) formatting,
 * even if not explicitly set by the user. For example the central meridian is typically a mandatory
 * parameter with a default value of 0° (the Greenwich meridian).
 *
 * <p>Optional parameters, on the other hand, are often non-standard extensions.
 * They will appear in WKT formatting only if the user defined explicitly a value which is different than the
 * default value.</p>
 *
 *
 * {@section Non-spatial coordinates}
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
 * @version 0.5
 * @module
 */
package org.apache.sis.referencing.operation.transform;
