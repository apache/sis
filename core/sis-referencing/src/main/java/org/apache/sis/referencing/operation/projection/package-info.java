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
 * This package is mostly for documentation purpose (each projection documents its name and parameters)
 * and for implementors who want to extend a map projection. This package should usually not be used directly.
 *
 * <p>The best way to get a projection is to use the
 * {@linkplain org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory coordinate operation factory}
 * with the source and target CRS. That factory can bundle the projections defined in this package, together with any
 * affine transform required for handling unit conversions and axis swapping, in a single (potentially concatenated)
 * operation.</p>
 *
 * <p>Users wanting to build their transforms directly should also avoid instantiating objects directly from this
 * package and use a {@linkplain org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory math
 * transform factory} instead.
 * The {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createParameterizedTransform
 * createParameterizedTransform(…)} method of that factory is subjects to the same rules than this package,
 * namely input coordinates must be (<var>longitude</var>, <var>latitude</var>) in decimal degrees
 * and output coordinates must be (<var>easting</var>, <var>northing</var>) in metres.
 * More on this convention is explained below.</p>
 *
 * <p>Users wanting to know more about the available projections and their parameters should look at the
 * <a href="http://sis.apache.org/book/tables/CoordinateOperationMethods.html">list of coordinate operation methods</a>.
 * Only users interested in the <em>implementation</em> of those projections should look at this package.</p>
 *
 *
 * <div class="section">Definition of terms</div>
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
 * <div class="section">Axis units and orientation</div>
 * Many {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic coordinate reference systems}
 * use axis in (<var>latitude</var>, <var>longitude</var>) order, but not all. Axis order, orientation and units
 * are CRS-dependent. For example some CRS use longitude values increasing toward
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
 * where the other steps change units and swap ordinates.
 * </blockquote>
 *
 * The Apache SIS implementation extends this rule to axis directions as well, i.e. (<var>x</var>, <var>y</var>) coordinates
 * must be ({@linkplain org.opengis.referencing.cs.AxisDirection#EAST East},
 * {@linkplain org.opengis.referencing.cs.AxisDirection#NORTH North}) oriented.
 *
 * <div class="note"><b>Implications on South oriented projections</b><br>
 * The above rule implies a non-intuitive behavior for the <cite>Transverse Mercator (South Orientated)</cite>
 * projection, which still projects coordinates with <var>y</var> values increasing toward North.
 * The real axis flip is performed outside this projection package, upon
 * {@linkplain org.opengis.referencing.cs.CoordinateSystemAxis coordinate system axis} inspection,
 * as a concatenation of the North oriented cartographic projection with an affine transform.
 * Such axis analysis and transforms concatenation can be performed automatically by the
 * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createBaseToDerived
 * createBaseToDerived(…)} method defined in the {@code MathTransformFactory} interface.
 * The same rule applies to the <cite>Krovak</cite> projection as well (at the opposite of what ESRI does).
 * </div>
 *
 * In order to reduce the risk of confusion, this package never defines south oriented map projection.
 * This rule removes ambiguity when reading a transform in <cite>Well Known Text</cite> (WKT) format,
 * since only the north-oriented variant is used and the affine transform coefficients tell exactly
 * which axis flips are applied.
 *
 *
 * <div class="section">Projection on unit ellipse</div>
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
 * The first step (<cite>"normalization"</cite>) converts longitude and latitude values from degrees to radians
 * and removes the <cite>central meridian</cite> from the longitude.
 * The last step (<cite>"denormalization"</cite>) multiplies the result of the middle step by the global scale factor
 * (typically the product of the <cite>scale factor</cite> with the <cite>semi-major</cite> axis length),
 * then adds the <cite>false easting</cite> and <cite>false northing</cite>.
 * This means that the middle step (<cite>"normalized projection"</cite>) is performed on an ellipse (or sphere)
 * having a semi-major axis of 1.
 *
 * <p>In other words, the
 * {@linkplain org.apache.sis.referencing.operation.projection.NormalizedProjection#transform(double[],int,double[],int,boolean)
 * transform} method of the middle step works typically on longitude and latitude values in <strong>radians</strong>
 * relative to the central meridian (not necessarily Greenwich). Its results are typically (<var>x</var>, <var>y</var>)
 * coordinates having ({@linkplain org.opengis.referencing.cs.AxisDirection#EAST East},
 * {@linkplain org.opengis.referencing.cs.AxisDirection#NORTH North}) axis orientation.
 * However in some cases the actual input and output coordinates may be different than the above by some scale factor,
 * translation or rotation, if the projection implementation choose to combine some linear coefficients with the
 * above-cited normalization and denormalization affine transforms.</p>
 *
 * <div class="note"><b>Note:</b>
 * In <a href="http://www.remotesensing.org/proj/">Proj.4</a>, the same standardization is handled by {@code pj_fwd.c}
 * and {@code pj_inv.c}. This normalization makes the equations closer to the ones published in Snyder's book, where the
 * <cite>false easting</cite>, <cite>false northing</cite> and <cite>scale factor</cite> are usually not given.</div>
 *
 * <div class="section">References</div>
 * <ul>
 *   <li>"Coordinate Conversions and Transformations including Formulas",<br>
 *       Geomatics Guidance Note Number 7, part 2, Version 49.</li>
 *   <li>John P. Snyder (Map Projections - A Working Manual,<br>
 *       U.S. Geological Survey Professional Paper 1395, 1987)</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Adrian Custer (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list">Projections list on RemoteSensing.org</a>
 * @see <a href="http://mathworld.wolfram.com/MapProjection.html">Map projections on MathWorld</a>
 */
package org.apache.sis.referencing.operation.projection;
