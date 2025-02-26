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

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.base.TiledGridCoverage;
import org.apache.sis.storage.base.TiledGridResource;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.gimi.ModelTiePoint;
import org.apache.sis.storage.isobmff.gimi.ModelTransformation;
import org.apache.sis.storage.isobmff.gimi.ModelCRS;
import org.apache.sis.storage.isobmff.mpeg.ComponentDefinition;
import org.apache.sis.storage.isobmff.mpeg.UncompressedFrameConfig;
import org.apache.sis.storage.isobmff.image.ImageSpatialExtents;
import org.apache.sis.storage.isobmff.image.PixelInformation;
import org.apache.sis.util.iso.Names;


/**
 * A single uncompressed image as a GridCoverageResource.
 *
 * @author Johann Sorel (Geomatys)
 */
class UncompressedImage extends TiledGridResource implements StoreResource {

    public static final String TYPE = "unci";
    protected final GeoHeifStore store;
    protected final Image item;
    private final GenericName identifier;
    protected ImageSpatialExtents imageExt;
    /**
     * Can be null
     */
    protected PixelInformation pixelDef;
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
    protected ModelTransformation modelTrs;
    /**
     * Can be null
     */
    protected ModelTiePoint modelTp;
    /**
     * Can be null
     */
    protected ModelCRS modelWkt;


    //computed values for decoding
    private final int tileWidth;
    private final int tileHeight;
    private final int tileByteArrayLength;
    private final SampleModel sampleModel;
    private final ColorModel colorModel;

    public UncompressedImage(GeoHeifStore store, Image item) throws DataStoreException {
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
            } else if (box instanceof ModelTransformation) {
                modelTrs = (ModelTransformation) box;
            } else if (box instanceof ModelTiePoint) {
                modelTp = (ModelTiePoint) box;
            } else if (box instanceof ModelCRS) {
                modelWkt = (ModelCRS) box;
            } else if (box instanceof PixelInformation) {
                pixelDef = (PixelInformation) box;
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

        if ( (compDef != null && Arrays.equals(compDef.componentType, new int[]{4,5,6}))
           || (pixelDef != null && pixelDef.bitsPerChannel.length == 3)
           ) {
            // RGB case
            final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            final int[] nBits = {8, 8, 8};
            final int[] bOffs = {0, 1, 2};
            colorModel = new ComponentColorModel(
                    cs,
                    nBits,
                    false,
                    false,
                    Transparency.OPAQUE,
                    DataBuffer.TYPE_BYTE);
            sampleModel = new PixelInterleavedSampleModel(
                    DataBuffer.TYPE_BYTE,
                    tileWidth,
                    tileHeight,
                    3,
                    tileWidth * 3,
                    bOffs);
        } else {
            throw new DataStoreException("Unsupported component model");
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
            return new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCrs, crs);
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
    protected int[] getTileSize() throws DataStoreException {
        return new int[]{tileWidth, tileHeight};
    }

    @Override
    protected SampleModel getSampleModel(int[] bands) throws DataStoreException {
        if (bands != null) {
            return null;
        }
        return sampleModel;
    }

    @Override
    protected ColorModel getColorModel(int[] bands) throws DataStoreException {
        if (bands != null) {
            return null;
        }
        return colorModel;
    }

    /**
     *
     * @param tileX starting from image left
     * @param tileY starting from image top
     */
    private void readTile(long tileX, long tileY, WritableRaster raster, int offsetX, int offsetY) throws DataStoreException {
        final long tileOffset = (tileX + tileY * (frameConf.numTileColsMinusOne+1)) * tileByteArrayLength;

        final DataBuffer targetBuffer = raster.getDataBuffer();
        if (targetBuffer instanceof DataBufferByte) {
            final DataBufferByte dbb = (DataBufferByte) targetBuffer;
            item.getData(tileOffset, tileByteArrayLength, dbb.getData(), 0);
        } else {
            final byte[] data = item.getData(tileOffset, tileByteArrayLength, null, 0);
            final DataBuffer buffer = new DataBufferByte(data, tileByteArrayLength, 0);
            final Raster tile = WritableRaster.createInterleavedRaster(buffer, tileWidth, tileHeight, tileWidth*3, 3, new int[]{0,1,2}, new Point(0,0));
            raster.setDataElements(offsetX, offsetY, tile);
        }
    }

    @Override
    public GridCoverage read(GridGeometry domain, int ... range) throws DataStoreException {
        return new InternalTiledCoverage(new Subset(domain, range));
    }

    private class InternalTiledCoverage extends TiledGridCoverage {

        public InternalTiledCoverage(TiledGridResource.Subset subset) {
            super(subset);
        }

        @Override
        protected GenericName getIdentifier() {
            return UncompressedImage.this.identifier;
        }

        @Override
        protected Raster[] readTiles(final TileIterator iterator) throws IOException, DataStoreException {
            final Raster[] result = new Raster[iterator.tileCountInQuery];
            synchronized (UncompressedImage.this.getSynchronizationLock()) {
                do {
                    final Raster tile = iterator.getCachedTile();
                    if (tile != null) {
                        result[iterator.getTileIndexInResultArray()] = tile;
                    } else {
                        long[] tileCoord = iterator.getTileCoordinatesInResource();
                        final WritableRaster raster = iterator.createRaster();
                        readTile(tileCoord[0], tileCoord[1], raster, Math.toIntExact(tileCoord[0]* tileWidth), Math.toIntExact(tileCoord[1]* tileHeight));
                        result[iterator.getTileIndexInResultArray()] = iterator.cache(raster);
                    }
                } while (iterator.next());
            }
            return result;
        }

    }

}
