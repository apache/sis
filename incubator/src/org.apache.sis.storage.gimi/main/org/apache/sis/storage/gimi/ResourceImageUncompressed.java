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

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.gimi.ModelTiePointProperty;
import org.apache.sis.storage.gimi.isobmff.gimi.ModelTransformationProperty;
import org.apache.sis.storage.gimi.isobmff.gimi.WellKnownText2Property;
import org.apache.sis.storage.gimi.isobmff.iso23001_17.ComponentDefinition;
import org.apache.sis.storage.gimi.isobmff.iso23001_17.UncompressedFrameConfig;
import org.apache.sis.storage.gimi.isobmff.iso23008_12.ImageSpatialExtents;
import org.apache.sis.util.iso.Names;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 * A single uncompressed image.
 *
 * @author Johann Sorel (Geomatys)
 */
class ResourceImageUncompressed extends AbstractGridCoverageResource implements StoreResource {

    public static final String TYPE = "unci";
    protected final GimiStore store;
    protected final Item item;
    private final GenericName identifier;
    protected ComponentDefinition compDef;
    protected ImageSpatialExtents imageExt;
    protected UncompressedFrameConfig frameConf;
    /**
     * Can be null
     */
    protected ModelTransformationProperty modelTrs;
    /**
     * Can be null
     */
    protected ModelTiePointProperty modelTp;
    /**
     * Can be null
     */
    protected WellKnownText2Property modelWkt;

    public ResourceImageUncompressed(GimiStore store, Item item) throws DataStoreException {
        super(store);
        this.store = store;
        this.item = item;
        for (Box box : item.properties) {
            if (box instanceof ComponentDefinition) {
                compDef = (ComponentDefinition) box;
            } else if (box instanceof ImageSpatialExtents) {
                imageExt = (ImageSpatialExtents) box;
            } else if (box instanceof UncompressedFrameConfig) {
                frameConf = (UncompressedFrameConfig) box;
            } else if (box instanceof ModelTransformationProperty) {
                modelTrs = (ModelTransformationProperty) box;
            } else if (box instanceof ModelTiePointProperty) {
                modelTp = (ModelTiePointProperty) box;
            } else if (box instanceof WellKnownText2Property) {
                modelWkt = (WellKnownText2Property) box;
            }
        }
        if (item.entry.itemName == null || item.entry.itemName.isBlank()) {
            this.identifier = Names.createLocalName(null, null, Integer.toString(item.entry.itemId));
        } else {
            this.identifier = Names.createLocalName(null, null, item.entry.itemName);
        }
    }

    @Override
    public DataStore getOriginator() {
        return store;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(identifier);
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        try {
            final GridExtent extent = new GridExtent(imageExt.imageWidth, imageExt.imageHeight);
            final AffineTransform2D gridToCrs;
            if (modelTrs == null) {
                gridToCrs = new AffineTransform2D(1, 0, 0, 1, 0, 0);
            } else {
                gridToCrs = new AffineTransform2D(modelTrs.transform[0], modelTrs.transform[3], modelTrs.transform[1], modelTrs.transform[4], modelTrs.transform[2], modelTrs.transform[5]);
            }
            final CoordinateReferenceSystem crs;
            if (modelWkt == null) {
                //TODO we should have an Image CRS
                crs = CommonCRS.defaultGeographic();
            } else {
                String wkt = modelWkt.wkt2;
                //TODO remove this hack when SIS support BASEGEOGCRS
                wkt = wkt.replace("BASEGEOGCRS", "BASEGEODCRS");
                crs = CRS.fromWKT(wkt);
            }
            return new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCrs, crs);
        } catch (FactoryException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        final List<SampleDimension> sd = new ArrayList<>();
        for (int i = 0; i < compDef.componentType.length; i++) {
            final SampleDimension.Builder sdb = new SampleDimension.Builder();
            switch (compDef.componentType[i]) {
                case 4:
                    sdb.setName("Red");
                    break;
                case 5:
                    sdb.setName("Green");
                    break;
                case 6:
                    sdb.setName("Blue");
                    break;
            }
            sd.add(sdb.build());
        }
        return sd;
    }

    @Override
    public GridCoverage read(GridGeometry gg, int... ints) throws DataStoreException {
        final byte[] data = item.getData();
        final BufferedImage img = new BufferedImage(2048, 1024, BufferedImage.TYPE_3BYTE_BGR);
        final WritableRaster raster = img.getRaster();
        for (int y = 0; y < 1024; y++) {
            for (int x = 0; x < 2048; x++) {
                int offset = y * 2048 + x;
                raster.setSample(x, y, 0, data[offset * 3] & 0xFF);
                raster.setSample(x, y, 1, data[offset * 3 + 1] & 0xFF);
                raster.setSample(x, y, 2, data[offset * 3 + 2] & 0xFF);
            }
        }
        final GridGeometry gridGeometry = getGridGeometry();
        GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setDomain(gridGeometry);
        //gcb.setRanges(getSampleDimensions());
        //gcb.setValues(db, new Dimension((int)gridGeometry.getExtent().getSize(0), (int)gridGeometry.getExtent().getSize(1)));
        gcb.setValues(img);
        return gcb.build();
    }

}
