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
package org.apache.sis.storage.gimi;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemInfoEntry;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.SingleItemTypeReference;
import org.apache.sis.storage.gimi.isobmff.iso23008_12.ImagePyramidEntityGroup;
import org.apache.sis.storage.gimi.isobmff.iso23008_12.ImageSpatialExtents;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.util.iso.Names;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class ResourcePyramid extends AbstractGridCoverageResource implements TiledResource, StoreResource {

    private final GimiStore store;
    final ImagePyramidEntityGroup group;
    private GimiTileMatrixSet tileMatrixSet;
    private final Map<GenericName, ResourceGrid> grids = new HashMap<>();

    public ResourcePyramid(GimiStore store, ImagePyramidEntityGroup group) {
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
        return Optional.of(Names.createLocalName(null, null, "" + group.groupId));
    }

    private synchronized void initialize() throws DataStoreException {
        if (tileMatrixSet != null) {
            return;
        }
        final GridCoverageResource first = (GridCoverageResource) store.getComponent(group.entitiesId[0]);
        tileMatrixSet = new GimiTileMatrixSet(Names.createLocalName(null, null, getIdentifier().get().tip().toString() + "_tms"), first.getGridGeometry().getCoordinateReferenceSystem());
        for (int i = 0; i < group.matrices.length; i++) {
            Resource res = store.getComponent(group.entitiesId[i]);
            if (res instanceof GridCoverageResource && !(res instanceof TiledResource)) {
                final GridCoverageResource gcr = (GridCoverageResource) res;
                //create a fake grid
                try {
                    final int tileItemId = Integer.parseInt(res.getIdentifier().get().tip().toString());
                    final ItemInfoEntry entry = new ItemInfoEntry();
                    entry.itemId = -tileItemId; //fake id, should not be used
                    final Item item = new Item(store, entry);
                    item.references.add(new SingleItemTypeReference(entry.itemId, new int[]{tileItemId}));
                    final GridExtent tileExtent = gcr.getGridGeometry().getExtent();
                    final ImageSpatialExtents ext = new ImageSpatialExtents(Math.toIntExact(tileExtent.getSize(0)), Math.toIntExact(tileExtent.getSize(1)));
                    item.properties.add(ext);
                    res = new ResourceGrid(item);
                } catch (IOException ex) {
                    throw new DataStoreException(ex);
                }
            }
            if (res instanceof ResourceGrid) {
                final ResourceGrid tr = (ResourceGrid) res;
                final TileMatrix tm = tr.getTileMatrix();
                grids.put(tm.getIdentifier(), tr);
                tileMatrixSet.matrices.insertByScale(tm);
            } else {
                throw new DataStoreException("A resource in the pyramid in not a coverage, itemId : " + group.entitiesId[i]);
            }
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
        ResourceGrid bestMatch = grids.get(tileMatrixSet.getTileMatrices().lastKey());
        if (domain != null && domain.isDefined(GridGeometry.RESOLUTION)) {
            final double[] resolution = domain.getResolution(true);
            for (Map.Entry<GenericName, ? extends TileMatrix> entry : tileMatrixSet.getTileMatrices().entrySet()) {
                final ResourceGrid grid = grids.get(entry.getKey());
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
