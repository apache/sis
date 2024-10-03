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

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
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
import org.apache.sis.storage.gimi.isobmff.iso23008_12.PixelInformationProperty;
import org.apache.sis.util.iso.Names;
import org.opengis.referencing.operation.MathTransform;


/**
 * A single uncompressed image as a GridCoverageResource.
 *
 * @author Johann Sorel (Geomatys)
 */
class ResourceImageUncompressed extends AbstractGridCoverageResource implements StoreResource {

    public static final String TYPE = "unci";
    protected final GimiStore store;
    protected final Item item;
    private final GenericName identifier;
    protected ImageSpatialExtents imageExt;
    /**
     * Can be null
     */
    protected PixelInformationProperty pixelDef;
    /**
    /**
     * Can be null
     */
    protected ComponentDefinition compDef;
    /**
     * Can be null
     */
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


    //computed values for decoding
    private final int tileWidth;
    private final int tileHeight;
    private final int tileByteArrayLength;

    public ResourceImageUncompressed(GimiStore store, Item item) throws DataStoreException {
        super(store);
        this.store = store;
        this.item = item;
        for (Box box : item.getProperties()) {
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
            } else if (box instanceof PixelInformationProperty) {
                pixelDef = (PixelInformationProperty) box;
            }
        }
        if (item.entry.itemName == null || item.entry.itemName.isBlank()) {
            this.identifier = Names.createLocalName(null, null, Integer.toString(item.entry.itemId));
        } else {
            this.identifier = Names.createLocalName(null, null, item.entry.itemName);
        }

        //pre-computed values
        if (frameConf == null) {
            //use a dumy one
            frameConf = new UncompressedFrameConfig();
            frameConf.numTileColsMinusOne = 0;
            frameConf.numTileRowsMinusOne = 0;
        }
        tileWidth = imageExt.imageWidth / (frameConf.numTileColsMinusOne+1);
        tileHeight = imageExt.imageHeight / (frameConf.numTileRowsMinusOne+1);

        //TODO handle all kind of component length and subsampling
        if (pixelDef != null) {
            tileByteArrayLength = tileWidth * tileHeight * pixelDef.bitsPerChannel.length;
        } else if (compDef != null) {
            tileByteArrayLength = tileWidth * tileHeight * compDef.componentType.length;
        } else {
            throw new DataStoreException("Failed to compute tile sizein bytes");
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
            final MathTransform gridToCrs;
            if (modelTrs == null) {
                gridToCrs = new AffineTransform2D(1, 0, 0, 1, 0, 0);
            } else {
                gridToCrs = modelTrs.toMathTransform();
            }
            final CoordinateReferenceSystem crs;
            if (modelWkt == null) {
                //TODO we should have an Image CRS
                crs = CommonCRS.Engineering.GRID.crs();
            } else {
                crs = modelWkt.toCRS();
            }
            return new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCrs, crs);
        } catch (FactoryException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        final List<SampleDimension> sd = new ArrayList<>();
        if (compDef != null) {
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
        } else if (pixelDef != null) {
            for (int i = 0; i < pixelDef.bitsPerChannel.length; i++) {
                final SampleDimension.Builder sdb = new SampleDimension.Builder();
                sdb.setName(""+i);
                sd.add(sdb.build());
            }
        }
        return sd;
    }

    @Override
    public GridCoverage read(GridGeometry gg, int... ints) throws DataStoreException {

        final BufferedImage img;
        if ( (compDef != null && Arrays.equals(compDef.componentType, new int[]{4,5,6}))
           || (pixelDef != null && pixelDef.bitsPerChannel.length == 3)
           ) {
            // RGB case
            int width = imageExt.imageWidth;
            int height = imageExt.imageHeight;
            img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            final WritableRaster raster = img.getRaster();
            for (int y = 0; y <= frameConf.numTileRowsMinusOne; y++) {
                for (int x = 0; x <= frameConf.numTileColsMinusOne; x++) {
                    readTile(x, y, raster, x*tileWidth, y*tileHeight);
                }
            }
        } else {
            throw new DataStoreException("Unsupported component model");
        }
        final GridGeometry gridGeometry = getGridGeometry();
        GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setDomain(gridGeometry);
        gcb.setRanges(getSampleDimensions());
        gcb.setValues(img);
        return gcb.build();
    }

    /**
     *
     * @param data
     * @param tileX starting from image left
     * @param tileY starting from image top
     */
    private void readTile(int tileX, int tileY, WritableRaster raster, int offsetX, int offsetY) throws DataStoreException {
        final int tileOffset = (tileX + tileY * (frameConf.numTileColsMinusOne+1)) * tileByteArrayLength;
        final byte[] data = item.getData(tileOffset, tileByteArrayLength);

        final DataBuffer buffer = new DataBufferByte(data, tileByteArrayLength, 0);

        final Raster tile = WritableRaster.createInterleavedRaster(buffer, tileWidth, tileHeight, tileWidth*3, 3, new int[]{0,1,2}, new Point(0,0));
        raster.setDataElements(offsetX, offsetY, tile);
    }

}
