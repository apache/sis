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
 * Basic geometric objects (envelopes and direct positions). Every geometry objects are associated
 * with a {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System},
 * which may have an arbitrary number of dimensions. However a few specialized classes restrict
 * the CRS to a fixed number of dimensions only. The table below summarizes the most common
 * objects, and list the Java2D classes that are conceptually equivalent.
 *
 * <table class="sis">
 *   <caption>Java2D and geometry equivalences</caption>
 *   <tr>
 *     <th>Purpose</th>
 *     <th>Any dimension</th>
 *     <th>One dimension</th>
 *     <th>Two dimensions</th>
 *     <th>Java2D equivalence</th>
 *   </tr><tr>
 *     <td>A point in a multi-dimensional space</td>
 *     <td>{@link org.apache.sis.geometry.GeneralDirectPosition}</td>
 *     <td>{@link org.apache.sis.geometry.DirectPosition1D}</td>
 *     <td>{@link org.apache.sis.geometry.DirectPosition2D}</td>
 *     <td>{@link java.awt.geom.Point2D}</td>
 *   </tr><tr>
 *     <td>A box in a multi-dimensional space</td>
 *     <td>{@link org.apache.sis.geometry.GeneralEnvelope}</td>
 *     <td></td>
 *     <td>{@link org.apache.sis.geometry.Envelope2D}</td>
 *     <td>{@link java.awt.geom.Rectangle2D}</td>
 *   </tr>
 * </table>
 *
 * <div class="section">Envelopes spanning the anti-meridian of a Geographic CRS</div>
 * The Web Coverage Service (WCS) 1.1 specification uses an extended interpretation
 * of the bounding box definition. In a WCS 1.1 data structure, the
 * {@linkplain org.apache.sis.geometry.GeneralEnvelope#getLowerCorner() lower corner}
 * defines the edges region in the directions of <em>decreasing</em> coordinate values in the
 * {@linkplain org.apache.sis.geometry.GeneralEnvelope#getCoordinateReferenceSystem() envelope CRS},
 * while the {@linkplain org.apache.sis.geometry.GeneralEnvelope#getUpperCorner() upper corner}
 * defines the edges region in the directions of <em>increasing</em> coordinate values.
 * Those lower and upper corners are usually the algebraic
 * {@linkplain org.apache.sis.geometry.GeneralEnvelope#getMinimum(int) minimum} and
 * {@linkplain org.apache.sis.geometry.GeneralEnvelope#getMaximum(int) maximum} coordinates respectively,
 * but not always. For example, an envelope crossing the anti-meridian could have a lower corner
 * longitude greater than the upper corner longitude, like the red box below (the green box is the
 * usual case):
 *
 * <center><img src="doc-files/AntiMeridian.png" alt="Envelope spannning the anti-meridian"></center>
 *
 * In SIS, every envelopes defined in this package support the extended bounding box interpretation:
 * for any dimension, ordinate values such that <var>upper</var> &lt; <var>lower</var> are handled
 * in a special way. This handling is slightly different for two groups of methods:
 *
 * <ul>
 *   <li>In calculation of envelopes spans and median positions (centers) — handled specially only
 *       on axes having the {@link org.opengis.referencing.cs.RangeMeaning#WRAPAROUND WRAPAROUND}
 *       range meaning.</li>
 *   <li>When checking for containment, intersections or unions — can be handled specially for
 *       any axis, in which case the envelope represents an <em>exclusion</em> area instead
 *       than an inclusion area.</li>
 * </ul>
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
package org.apache.sis.geometry;
