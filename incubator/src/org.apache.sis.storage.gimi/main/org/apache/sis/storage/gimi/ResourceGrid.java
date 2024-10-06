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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.gimi.internal.MatrixGridRessource;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.gimi.ModelTransformationProperty;
import org.apache.sis.storage.gimi.isobmff.gimi.WellKnownText2Property;
import org.apache.sis.storage.gimi.isobmff.iso23008_12.ImageSpatialExtents;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.util.iso.Names;


/**
 * A Grid as a GridCoverageResource and as a TiledResource.
 *
 * @author Johann Sorel (Geomatys)
 */
final class ResourceGrid extends MatrixGridRessource implements TiledResource, StoreResource {

    public static final String TYPE = "grid";

    private final Item item;
    private final GenericName identifier;
    //filled after initialize
    private GridCoverageResource first;
    private CoordinateReferenceSystem crs;
    private TileMatrix tileMatrix;
    private GimiTileMatrixSet tileMatrixSet;

    public ResourceGrid(Item item) throws DataStoreException {
        this.item = item;
        if (item.entry.itemName == null || item.entry.itemName.isBlank()) {
            this.identifier = Names.createLocalName(null, null, Integer.toString(item.entry.itemId));
        } else {
            this.identifier = Names.createLocalName(null, null, item.entry.itemName);
        }
    }

    /**
     * Get ISOBMFF item.
     *
     * @return item, never null.
     */
    Item getItem() {
        return item;
    }

    @Override
    public TileMatrix getTileMatrix() {
        try {
            initialize();
        } catch (DataStoreException ex) {
            throw new IllegalStateException();
        }
        return tileMatrix;
    }

    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.of(identifier);
    }

    @Override
    public DataStore getOriginator() {
        return item.store;
    }

    private synchronized void initialize() throws DataStoreException {
        if (tileMatrix != null) return;

        final Resource first = item.store.getComponent(item.getReferences().get(0).toItemId[0]);
        if (first instanceof GridCoverageResource) {
            this.first = (GridCoverageResource) first;
        } else {
            throw new DataStoreException("Expecting a GridCoverageResource tile but was a " + first.getClass().getName());
        }
        final GridGeometry firstTileGridGeom = this.first.getGridGeometry();

        final GridExtent tileExtent = firstTileGridGeom.getExtent();
        final long[] tileSize = new long[]{tileExtent.getSize(0), tileExtent.getSize(1)};

        ImageSpatialExtents imageExts = null;
        ModelTransformationProperty modelTrs = null;
        WellKnownText2Property modelWkt = null;

        for (Box box : item.getProperties()) {
            if (box instanceof ImageSpatialExtents) {
                imageExts = (ImageSpatialExtents) box;
            } else if (box instanceof ModelTransformationProperty) {
                modelTrs = (ModelTransformationProperty) box;
            } else if (box instanceof WellKnownText2Property) {
                modelWkt = (WellKnownText2Property) box;
            }
        }

        if (modelWkt != null) {
            try {
                this.crs = modelWkt.toCRS();
            } catch (FactoryException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }
        } else {
            this.crs = null;
        }

        MathTransform matrixGridToCrs;
        if (modelTrs != null) {
            matrixGridToCrs = modelTrs.toMathTransform();
        } else {
            matrixGridToCrs = null;
        }

        //create tile matrix
        GridGeometry tilingScheme = new GridGeometry(new GridExtent(imageExts.imageWidth, imageExts.imageHeight), PixelInCell.CELL_CENTER, matrixGridToCrs, crs);
        tilingScheme = tilingScheme.derive().subgrid(null, tileSize).build(); //remove tile size from scheme
        tileMatrix = new GimiTileMatrix(this, tilingScheme, tileSize);
        //create tile matrix set
        tileMatrixSet = new GimiTileMatrixSet(Names.createLocalName(null, null, identifier.tip().toString() + "_tms"), crs);
        tileMatrixSet.matrices.insertByScale(tileMatrix);
    }

    void amendTilingScheme(GridGeometry tilingScheme, long[] tileSize) {
        tileMatrix = new GimiTileMatrix(this, tilingScheme, tileSize);
        tileMatrixSet = new GimiTileMatrixSet(Names.createLocalName(null, null, identifier.tip().toString() + "_tms"), crs);
        tileMatrixSet.matrices.insertByScale(tileMatrix);
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        initialize();
        return first.getSampleDimensions();
    }

    @Override
    public Collection<? extends TileMatrixSet> getTileMatrixSets() throws DataStoreException {
        initialize();
        return List.of(tileMatrixSet);
    }

}
