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
 * Relationship between any two {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems}.
 * An explanation for this package is provided in the {@linkplain org.opengis.referencing.operation OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the Apache SIS implementation.
 *
 * <p>This package provides an ISO 19111 {@linkplain org.apache.sis.referencing.operation.AbstractCoordinateOperation
 * Coordinate Operation implementation} and support classes. The actual transform work is performed by the following
 * sub-packages, but most users will not need to deal with them directly:</p>
 *
 * <ul>
 *   <li>{@link org.apache.sis.referencing.operation.projection} for map projections</li>
 *   <li>{@link org.apache.sis.referencing.operation.transform} for any transform other than map projections</li>
 * </ul>
 *
 * {@section Apache SIS specific behavior}
 * The following operations have a behavior in Apache SIS which may be different
 * than the behavior found in other softwares. Those particularities apply only when the math transform is
 * {@linkplain org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createParameterizedTransform
 * created directly}. Users do not need to care about them when the coordinate operation is
 * {@link org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#createOperation
 * inferred by Apache SIS for a given pair of CRS}.
 *
 * <ul>
 *   <li><b>Longitude rotation</b> (EPSG:9601) — the longitude offset may be specified in any units,
 *     but SIS unconditionally converts the value to degrees. Consequently the user is responsible
 *     for converting the longitude axis of source and target CRS to degrees before this operation is applied.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.6
 * @since   0.6
 * @module
 */
package org.apache.sis.referencing.operation;
