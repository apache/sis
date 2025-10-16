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
 * A coverage backed by a regular grid.
 * In the two-dimensional case, the grid coverage is an image and the cells are pixels.
 * In the three-dimensional case, the cells are voxels.
 *
 * <p>{@link org.apache.sis.coverage.grid.GridCoverage2D}
 * is a two-dimensional slice in a <var>n</var>-dimensional cube of data.
 * Despite its name, {@code GridCoverage2D} instances can be associated to <var>n</var>-dimensional
 * {@linkplain org.opengis.geometry.Envelope envelopes} providing that only two dimensions have a
 * {@link org.apache.sis.coverage.grid.GridExtent#getSize(int) grid span} greater than 1.</p>
 *
 * <p>{@link org.apache.sis.coverage.grid.GridCoverageBuilder} is a convenience class
 * making easier to create a grid coverage for some common cases.</p>
 *
 * <h2>Accurate definition of georeferencing information</h2>
 * While it is possible to create a grid coverage from a geodetic
 * {@linkplain org.opengis.geometry.Envelope envelope}, this approach should be used <em>in last resort</em> only.
 * Instead, always specify the <i>grid to CRS</i> affine transform.
 * This is preferable because envelopes have ambiguities
 * (do we need to swap the longitude and latitude axes? Do we need to flip the <var>y</var> axis?).
 * On the other hand, the <i>grid to CRS</i> affine transform is fully determinist.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.6
 * @since   1.0
 */
package org.apache.sis.coverage.grid;
