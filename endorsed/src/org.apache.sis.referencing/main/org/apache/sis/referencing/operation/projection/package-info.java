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
 * Map projection implementations.
 * This package should usually not be used directly. The best way to get a projection is to use the
 * {@linkplain org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory coordinate operation factory}
 * with the source and target <abbr>CRS</abbr>. That factory can bundle the projections defined in this package,
 * together with any affine transform required for handling unit conversions and axis swapping,
 * in a single (potentially concatenated) operation.
 *
 * <p>Users wanting to know more about the available projections and their parameters should look at the
 * <a href="https://sis.apache.org/tables/CoordinateOperationMethods.html">list of coordinate operation methods</a>.
 * Only users interested in the <em>implementation</em> of those projections should look at this package.</p>
 *
 *
 * <h2>Definition of terms</h2>
 * <ul class="verbose">
 *   <li><b>Coordinate operation</b><br>
 *       In the particular case of this package, the conversion of geographic coordinates in any
 *       axis order, geodesic orientation and angular units to projected coordinates in any axis
 *       order, horizontal orientation and linear units.</li>
 *   <li><b>Map projection</b> (a.k.a. cartographic projection)<br>
 *       The conversion of geographic coordinates from (<var>longitude</var>, <var>latitude</var>)
 *       in decimal degrees to projected coordinates (<var>x</var>, <var>y</var>) in metres.</li>
 *   <li><b>Normalized projection</b><br>
 *       The conversion of geographic coordinates from (<var>longitude</var>, <var>latitude</var>)
 *       in radians to projected coordinates (<var>x</var>, <var>y</var>) on a sphere or ellipse
 *       having a semi-major axis length of 1. This definition may be slightly relaxed if some
 *       projection-specifics coefficients are concatenated with the conversions that take place
 *       between the above map projection and this normalized projection.</li>
 * </ul>
 *
 *
 * <h2>Axis units and orientation</h2>
 * Many {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic coordinate reference systems}
 * use axis in (<var>latitude</var>, <var>longitude</var>) order, but not all. Axis order, orientation and units
 * are CRS-dependent. For example, some CRS use longitude values increasing toward
 * {@linkplain org.opengis.referencing.cs.AxisDirection#EAST East}, while some others use longitude values
 * increasing toward {@linkplain org.opengis.referencing.cs.AxisDirection#WEST West}.
 * The axis order must be specified in all CRS, and any method working with them should take their
 * axis order and units in account.
 *
 * <p>However, map projections defined in this package are <strong>transform steps</strong>, not the full transform
 * to the final CRS. All projections defined in this package must comply with the OGC 01-009 specification.
 * This specification says (quoting section 10.6 at page 34):</p>
 *
 * <blockquote>
 * Cartographic projection transforms are used by projected coordinate reference systems to map
 * geographic coordinates (e.g. <var>longitude</var> and <var>latitude</var>) into (<var>x</var>,
 * <var>y</var>) coordinates. These (<var>x</var>, <var>y</var>) coordinates can be imagined to
 * lie on a plane, such as a paper map or a screen. All cartographic projection transforms will
 * have the following properties:
 *
 * <ul>
 *   <li>Converts from (<var>longitude</var>, <var>latitude</var>) coordinates to (<var>x</var>,<var>y</var>).</li>
 *   <li>All angles are assumed to be decimal degrees, and all distances are assumed to be metres.</li>
 *   <li>The domain should be a subset of {[-180 … 180)×[-90 … 90]}°.</li>
 * </ul>
 *
 * Although all cartographic projection transforms must have the properties listed above, many projected coordinate
 * reference systems have different properties. For example, in Europe some projected coordinate reference systems
 * use grads instead of decimal degrees, and often the base geographic coordinate reference system is
 * (<var>latitude</var>, <var>longitude</var>) instead of (<var>longitude</var>, <var>latitude</var>).
 * This means that the cartographic projected transform is often used as a single step in a series of transforms,
 * where the other steps change units and swap coordinates.
 * </blockquote>
 *
 * The Apache SIS implementation extends this rule to axis directions as well, i.e. (<var>x</var>, <var>y</var>) coordinates
 * must be ({@linkplain org.opengis.referencing.cs.AxisDirection#EAST East},
 * {@linkplain org.opengis.referencing.cs.AxisDirection#NORTH North}) oriented.
 *
 * <h3>Implications on South oriented projections</h3>
 * The above rule implies a non-intuitive behavior for the <cite>Transverse Mercator (South Orientated)</cite>
 * projection, which still projects coordinates with <var>y</var> values increasing toward North.
 * The real axis flip is performed outside this projection package, upon
 * {@linkplain org.opengis.referencing.cs.CoordinateSystemAxis coordinate system axis} inspection,
 * as a concatenation of the North oriented cartographic projection with an affine transform.
 * Such axis analysis and transforms concatenation can be performed automatically by
 * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory.Context}.
 * The same rule applies to the <cite>Krovak</cite> projection as well (at the opposite of what ESRI does).
 *
 * <p>In order to reduce the risk of confusion, this package never defines south oriented map projection.
 * This rule removes ambiguity when reading a transform in <i>Well Known Text</i> (WKT) format,
 * since only the north-oriented variant is used and the affine transform coefficients tell exactly
 * which axis flips are applied.</p>
 *
 *
 * <h2>Projection on unit ellipse</h2>
 * A map projection in this package is actually the concatenation of the following transforms, in that order
 * (ignoring {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis order changes}
 * and conversions from/to units other then degrees and metres, which are not the purpose of this package):
 *
 * <ul>
 *   <li>A {@linkplain org.apache.sis.referencing.operation.transform.ContextualParameters#normalizeGeographicInputs normalization} affine transform</li>
 *   <li>A {@link      org.apache.sis.referencing.operation.projection.NormalizedProjection} subclass</li>
 *   <li>A {@linkplain org.apache.sis.referencing.operation.transform.ContextualParameters#getMatrix denormalization} affine transform</li>
 * </ul>
 *
 * The first step (<i>normalization</i>) converts longitude and latitude values from degrees to radians
 * and removes the <i>central meridian</i> from the longitude.
 * The last step (<i>denormalization</i>) multiplies the result of the middle step by the global scale factor
 * (typically the product of the <i>scale factor</i> with the <i>semi-major</i> axis length),
 * then adds the <i>false easting</i> and <i>false northing</i>.
 * This means that the middle step (<i>normalized projection</i>) is performed on an ellipse (or sphere)
 * having a semi-major axis of 1.
 *
 * <p>In other words, the
 * {@linkplain org.apache.sis.referencing.operation.projection.NormalizedProjection#transform(double[],int,double[],int,boolean)
 * transform} method of the middle step works typically on longitude and latitude values in <strong>radians</strong>
 * relative to the central meridian (not necessarily Greenwich). Its results are typically (<var>x</var>, <var>y</var>)
 * coordinates having ({@linkplain org.opengis.referencing.cs.AxisDirection#EAST East},
 * {@linkplain org.opengis.referencing.cs.AxisDirection#NORTH North}) axis orientation.
 * However, in some cases the actual input and output coordinates may be different than the above by some scale factor,
 * translation or rotation, if the projection implementation choose to combine some linear coefficients with the
 * above-cited normalization and denormalization affine transforms.</p>
 *
 * <div class="note"><b>Note:</b>
 * In the <a href="https://proj.org/">PROJ</a> library,
 * the same standardization is handled by {@code pj_fwd.c} and {@code pj_inv.c}.
 * This normalization makes the equations closer to the ones published in Snyder's book, where the
 * <i>false easting</i>, <i>false northing</i> and <i>scale factor</i> are usually not given.</div>
 *
 * <h2>References</h2>
 * <ul>
 *   <li>IOGP. <u>Coordinate Conversions and Transformations including Formulas.</u><br>
 *       Geomatics Guidance Note Number 7, part 2, Version 49.</li>
 *   <li>Snyder, John P. <u>Map Projections - A Working Manual.</u><br>
 *       U.S. Geological Survey Professional Paper 1395, 1987.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Adrian Custer (Geomatys)
 * @author  Matthieu Bastianelli (Geomatys)
 *
 * @see <a href="https://mathworld.wolfram.com/MapProjection.html">Map projections on MathWorld</a>
 */
package org.apache.sis.referencing.operation.projection;
