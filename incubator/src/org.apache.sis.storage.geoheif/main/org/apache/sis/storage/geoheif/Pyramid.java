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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.isobmff.base.ItemInfoEntry;
import org.apache.sis.storage.isobmff.base.SingleItemTypeReference;
import org.apache.sis.storage.isobmff.image.ImagePyramid;
import org.apache.sis.storage.isobmff.image.ImageSpatialExtents;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.util.iso.Names;


/**
 * Map an ImagePyramid as a GridCoverageResource and as a TiledResource.
 *
 * @author Johann Sorel (Geomatys)
 */
final class Pyramid extends AbstractGridCoverageResource implements TiledResource, StoreResource {

    private final GeoHeifStore store;
    final ImagePyramid group;
    private GimiTileMatrixSet tileMatrixSet;
    private final Map<GenericName, ImageResource> grids = new HashMap<>();

    public Pyramid(GeoHeifStore store, ImagePyramid group) {
        super(store);
        this.store = store;
        this.group = group;
    }

    @Override
    public DataStore getOriginator() {
        return store;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(Names.createLocalName(null, null, "ImagePyramid " + group.groupId));
    }

    private synchronized void initialize() throws DataStoreException {
        if (tileMatrixSet != null) {
            return;
        }

        /*
         * Find all resources of the pyramid
         */
        final List<ImageResource> candidates = new ArrayList<>();
        for (int i = 0; i < group.matrices.length; i++) {
            Resource res = store.getComponent(group.entitiesId[i]);
            if (res instanceof GridCoverageResource && !(res instanceof TiledResource)) {
                final GridCoverageResource gcr = (GridCoverageResource) res;
                //create a fake grid
                try {
                    final int tileItemId = Integer.parseInt(res.getIdentifier().get().tip().toString());
                    final ItemInfoEntry entry = new ItemInfoEntry();
                    entry.itemId = -tileItemId; //fake id, should not be used
                    final Image item = new Image(store, entry);
                    item.setReferences(List.of(new SingleItemTypeReference(entry.itemId, new int[]{tileItemId})));
                    final GridExtent tileExtent = gcr.getGridGeometry().getExtent();
                    final ImageSpatialExtents ext = new ImageSpatialExtents(Math.toIntExact(tileExtent.getSize(0)), Math.toIntExact(tileExtent.getSize(1)));
                    item.setProperties(List.of(ext));
                    res = new ImageResource(item);
                } catch (IOException ex) {
                    throw new DataStoreException(ex);
                }
            }
            if (res instanceof ImageResource) {
                final ImageResource tr = (ImageResource) res;
                candidates.add(tr);
            } else {
                throw new DataStoreException("A resource in the pyramid in not a coverage, itemId : " + group.entitiesId[i]);
            }
        }

        /*
         * Sort by scale, lowest resolution at index 0.
         * Only the most accurate matrix seems to contains georeferencing informations
         */
        Collections.sort(candidates, (ImageResource o1, ImageResource o2) -> {
            return Long.compare(
                    o2.getTileMatrix().getTilingScheme().getExtent().getSize(0),
                    o1.getTileMatrix().getTilingScheme().getExtent().getSize(0));
        });

        /*
         * Define each matrix CRS and transform based on the lowest one.
         * each level is 2x resolution with the same tope left corner.
         */
        final TileMatrix referenceMatrix = candidates.get(0).getTileMatrix();
        final GridGeometry reference = referenceMatrix.getTilingScheme();
        final MathTransform referenceTransform = reference.isDefined(GridGeometry.GRID_TO_CRS) ? reference.getGridToCRS(PixelInCell.CELL_CORNER) : new AffineTransform2D(1, 0, 0, 1, 0, 0);
        final CoordinateReferenceSystem crs = reference.isDefined(GridGeometry.CRS) ? referenceMatrix.getTilingScheme().getCoordinateReferenceSystem() : CommonCRS.Engineering.GRID.crs();

        tileMatrixSet = new GimiTileMatrixSet(Names.createLocalName(null, null, getIdentifier().get().tip().toString() + "_tms"), crs);
        grids.put(referenceMatrix.getIdentifier(), candidates.get(0));
        tileMatrixSet.matrices.insertByScale(referenceMatrix);
        final long[] tileSize = new long[]{group.tileSizeX, group.tileSizeY};

        for (int i = 1, n = candidates.size(); i < n; i++) {
            final ImageResource resource = candidates.get(i);

            final double scale = Math.pow(2, i);
            final MathTransform scaleTrs = new AffineTransform2D(scale, 0, 0, scale, 0, 0);

            final GridGeometry fixed = new GridGeometry(
                    resource.getTileMatrix().getTilingScheme().getExtent(),
                    PixelInCell.CELL_CORNER,
                    MathTransforms.concatenate(scaleTrs, referenceTransform),
                    reference.getCoordinateReferenceSystem());

            resource.amendTilingScheme(fixed, tileSize);
            final TileMatrix matrix = resource.getTileMatrix();

            grids.put(matrix.getIdentifier(), resource);
            tileMatrixSet.matrices.insertByScale(matrix);
        }
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        initialize();
        return grids.get(tileMatrixSet.getTileMatrices().lastKey()).getGridGeometry();
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        initialize();
        return grids.get(tileMatrixSet.getTileMatrices().lastKey()).getSampleDimensions();
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        initialize();

        ImageResource bestMatch = grids.get(tileMatrixSet.getTileMatrices().lastKey());
        if (domain != null && domain.isDefined(GridGeometry.RESOLUTION)) {
            //transpose it to this pyramid
            domain = getGridGeometry().derive().rounding(GridRoundingMode.ENCLOSING).subgrid(domain).build();
            final double[] resolution = domain.getResolution(true);
            for (Map.Entry<GenericName, ? extends TileMatrix> entry : tileMatrixSet.getTileMatrices().entrySet()) {
                final ImageResource grid = grids.get(entry.getKey());
                final double[] cdt = grid.getGridGeometry().getResolution(true);
                bestMatch = grid;
                if (cdt[0] <= resolution[0]) {
                    //best match found
                    break;
                }
            }
        }
        return bestMatch.read(domain, ranges);
    }

    @Override
    public Collection<? extends TileMatrixSet> getTileMatrixSets() throws DataStoreException {
        initialize();
        return List.of(tileMatrixSet);
    }

}
