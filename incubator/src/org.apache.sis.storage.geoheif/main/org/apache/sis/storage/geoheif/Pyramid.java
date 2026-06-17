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
package org.apache.sis.storage.geoheif;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.image.ImagePyramid;
import org.apache.sis.storage.tiling.TiledGridCoverage;
import org.apache.sis.storage.tiling.TiledGridCoverageResource;


/**
 * A pyramid of images.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class Pyramid extends TiledGridCoverageResource implements TiledGridCoverageResource.Pyramid {
    /**
     * Name of this pyramid, or {@code null} if none.
     */
    private final GenericName name;

    /**
     * Tile width in pixels.
     */
    private final int tileSizeX;

    /**
     * Tile height in pixels.
     */
    private final int tileSizeY;

    /**
     * The layers in the order they were declared in the {@code pymd} box.
     * This order should be from finest resolution (at index 0) to coarsest resolution.
     */
    private final ImageResource[] levels;

    /**
     * Creates a new pyramid.
     *
     * @param  store    the parent of this pyramid.
     * @param  name     the name of this pyramid, or {@code null} if none.
     * @param  pyramid  information about the pyramid.
     * @param  levels   the child resources from finest resolution to coarsest resolution.
     * @throws TransformException if an error occurred while deriving a "grid to <abbr>CRS</abbr>" transform.
     */
    Pyramid(final GeoHeifStore store, final GenericName name, final ImagePyramid pyramid, final ImageResource[] levels)
            throws TransformException
    {
        super(store);
        this.name = name;
        tileSizeX = pyramid.tileSizeX;
        tileSizeY = pyramid.tileSizeY;
        this.levels = levels;
        GridGeometry base = null;
        for (ImageResource level : levels) {
            base = level.setPyramidLevelOf(base);
        }
    }

    /**
     * Returns the name factory to use for creating identifiers of tiles and tile matrices.
     */
    @Override
    public NameFactory nameFactory() {
        return levels[0].store.nameFactory;
    }

    /**
     * Returns the name of this pyramid.
     */
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the size of tiles in this resource.
     * The length of the returned array is the number of dimensions,
     */
    @Override
    protected int[] getTileSize() {
        return new int[] {tileSizeX, tileSizeY};
    }

    /**
     * Returns the grid geometry of the level with the finest resolution.
     *
     * @return grid geometry at finest resolution.
     * @throws DataStoreException if an error occurred while fetching the grid geometry.
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return representative().getGridGeometry();
    }

    /**
     * Returns the sample dimensions of this grid coverage.
     * All levels should have the same sample dimensions.
     * This method uses the finest level as representative.
     *
     * @return sample dimensions of this grid coverage.
     * @throws DataStoreException if an error occurred while fetching the sample dimensions.
     */
    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return representative().getSampleDimensions();
    }

    /**
     * Returns a resource which is representative of all pyramid levels except for the resolution.
     * This method is invoked for fetching metadata such as the Coordinate Reference System
     * when the resolution does not matter. For a <abbr>HEIF</abbr> file, this is the image
     * with the finest resolution.
     *
     * @return a resource representative of all levels (ignoring resolution).
     */
    @Override
    public TiledGridCoverageResource representative() {
        return levels[0];
    }

    /**
     * Returns information about the overviews which form the pyramid.
     */
    @Override
    protected List<Pyramid> getPyramids() {
        return List.of(this);
    }

    /**
     * Returns the number of pyramid levels.
     */
    @Override
    public OptionalInt numberOfLevels() {
        return OptionalInt.of(levels.length);
    }

    /**
     * Returns the image at the given pyramid level.
     * Indices are in the reverse order of the images in the <abbr>HEIF</abbr> file,
     * with 0 for the image at the coarsest resolution (the overview).
     *
     * @param  level  image index (level) in the pyramid, with 0 for coarsest resolution (the overview).
     * @return image at the given pyramid level, or {@code null} if the given level is out of bounds.
     */
    @Override
    public TiledGridCoverageResource forPyramidLevel(int level) {
        if (level >= 0) {
            level = (levels.length - 1) - level;    // Reverse order.
            if (level >= 0) {
                return levels[level];
            }
        }
        return null;
    }

    /**
     * Delegates to the pyramid level for the resolution of the given subset.
     * This method should never be invoked because {@link #read(GridGeometry, int...)}
     * should select itself the pyramid level on which to delegate the read operation.
     * We nevertheless implement this method for safety.
     *
     * @param  subset  desired grid extent, resolution and sample dimensions to read.
     * @return the grid coverage for the specified domain, resolution and ranges.
     * @throws DataStoreException if the coverage cannot be created.
     */
    @Override
    protected TiledGridCoverage read(final Subset subset) throws DataStoreException {
        final double[] request = subset.domain.getResolution(false);
        final int x = subset.xDimension();
        final int y = subset.yDimension();
        int level = levels.length;
        while (--level >= 1) {
            final double[] actual = levels[level].getGridGeometry().getResolution(false);
            if (request[x] >= actual[x] && request[y] >= actual[y]) break;
        }
        return levels[level].read(subset);
    }
}
