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
 * {@code MathTransform} implementations for conversions or transformations of multi-dimensional coordinate points.
 * A {@code MathTransform} usually performs conversions or transformations from points given in a
 * {@linkplain org.apache.sis.referencing.operation.DefaultCoordinateOperation#getSourceCRS()
 * source coordinate reference system} to coordinate values for the same points in the
 * {@linkplain org.apache.sis.referencing.operation.DefaultCoordinateOperation#getTargetCRS()
 * target coordinate reference system}. However the conversions are not necessarily between CRS;
 * a {@code MathTransform} can also be used for converting the sample values in a raster for example.
 *
 * <p>This package does not include map projections, which are a special kind of transforms defined
 * in their own {@linkplain org.apache.sis.referencing.operation.projection projection} package.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5 (derived from geotk-1.2)
 * @version 0.5
 * @module
 */
package org.apache.sis.referencing.operation.transform;
