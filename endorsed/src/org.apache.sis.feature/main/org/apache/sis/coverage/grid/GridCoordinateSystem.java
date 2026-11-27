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
package org.apache.sis.coverage.grid;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;


/**
 * Identification of the coordinate system used for cell indices or tile indices in a grid.
 * The coordinates can be relative to a {@link org.apache.sis.storage.GridCoverageResource}
 * such as a big <abbr>TIFF</abbr> file,
 * or relative to a {@link GridCoverage} read from a subset of that file,
 * or relative to a {@link RenderedImage} produced for a sub-region of that coverage,
 * or relative to a {@link Raster} for one tile of that image (non-exhaustive list).
 * The values are enumerated from bigger object to smaller object, except the generic <abbr>AOI</abbr>.
 * The conversions between different grid coordinate systems are translations,
 * and sometime scales when a subsampling may be applied.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.8
 *
 * @since SIS 1.8
 */
public enum GridCoordinateSystem {

    // Implementation note: the order is significant. See usages of `GridCoordinateSystem.ordinal()`.

    /**
     * Cell or tile indices are in the grid coordinate system of a resource.
     * A resource is, for example, a data store opened on a big <abbr>TIFF</abbr> file
     * and is represented by a {@link org.apache.sis.storage.GridCoverageResource}.
     * The origin with (0, 0, …) coordinates is often (but not necessarily) the upper-left corner of the whole file.
     *
     * @see org.apache.sis.storage.GridCoverageResource#getGridGeometry()
     */
    RESOURCE,

    /**
     * Cell or tile indices are in the grid coordinate system of a coverage.
     * A coverage can be {@linkplain org.apache.sis.storage.GridCoverageResource#read read from a resource subspace}.
     * Compared to {@link #RESOURCE}, this coordinate system may be translated (although a {@link GridCoverage}
     * can choose to keep the same origin as the the parent {@link org.apache.sis.storage.GridCoverageResource}
     * even if that origin is not inside the coverage) and may have a scale factor if subsampling was applied
     * at reading time.
     *
     * @see GridCoverage#getGridGeometry()
     * @see org.apache.sis.storage.tiling.TiledGridCoverage#coverageToResourceCoordinate(long, int)
     */
    COVERAGE,

    /**
     * Pixel or tile coordinates are in the grid coordinate system of a rendered image.
     * An image can be {@linkplain GridCoverage#render rendered from a coverage sub-region},
     * in which case the image origin with (0, 0) pixel coordinates is the lower corner
     * of the {@link GridExtent} argument given to the {@code render(…)} method.
     * Compared to {@link #COVERAGE}, this coordinate system is only translated
     * (i.e. it has no additional subsampling).
     *
     * @see RenderedImage#getMinX()
     * @see RenderedImage#getMinY()
     */
    RENDERED_IMAGE,

    /**
     * Pixel coordinates are relative to the location of an image tile.
     * The origin with (0, 0) coordinates is the upper-left corner of the tile.
     * This is not a coordinate system formally used by any Java2D object, because tiles represented by
     * {@link Raster} objects use the same coordinate system as their enclosing {@link RenderedImage}.
     * However, it is sometime convenient to use a tile-relative space for calculation purposes.
     * Compared to {@link #RENDERED_IMAGE}, this coordinate system is only translated.
     *
     * @see RenderedImage#getTile(int, int)
     */
    TILE,

    /**
     * Pixel coordinates are relative to the location of a raster.
     * In Java2D, each image tile is represented by a {@link Raster}.
     * Therefore, {@code RASTER} is often synonymous of {@link #TILE}.
     * However, differences may exist when only a sub-region of a tile is read.
     * which may happen in the last column or last row of a tile matrix, or if the tile is too large.
     * Compared to {@link #TILE}, this coordinate system is only translated.
     *
     * @see Raster#getMinX()
     * @see Raster#getMinY()
     */
    RASTER,

    /**
     * Pixel or tile coordinates are in the grid coordinate system of an Area Of Interest (<abbr>AOI</abbr>).
     * This is often a {@link #RENDERED_IMAGE} or {@link #RASTER} sub-region represented by a {@link Rectangle},
     * although <abbr>AOI</abbr> may have different meanings in different contexts.
     */
    AREA_OF_INTEREST
}
