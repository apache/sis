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
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.ISOBMFFReader;
import org.apache.sis.storage.gimi.isobmff.gimi.ModelTiePointProperty;
import org.apache.sis.storage.gimi.isobmff.gimi.ModelTransformationProperty;
import org.apache.sis.storage.gimi.isobmff.gimi.WellKnownText2Property;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemInfo;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemInfoEntry;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemLocation;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemProperties;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemPropertyAssociation;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemPropertyContainer;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemReference;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.MediaData;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.Meta;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.PrimaryItem;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.SingleItemTypeReference;
import org.apache.sis.storage.gimi.isobmff.iso23001_17.ComponentDefinition;
import org.apache.sis.storage.gimi.isobmff.iso23001_17.UncompressedFrameConfig;
import org.apache.sis.storage.gimi.isobmff.iso23008_12.ImageSpatialExtents;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TiledResource;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GimiStore extends DataStore implements Aggregate {

    private final Path gimiPath;

    private List<Resource> components;

    //cache the reader
    private ISOBMFFReader reader;
    private Box root;

    public GimiStore(Path path) {
        this.gimiPath = path;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final Parameters parameters = Parameters.castOrWrap(GimiProvider.PARAMETERS_DESCRIPTOR.createValue());
        parameters.parameter(GimiProvider.LOCATION).setValue(gimiPath.toUri());
        return Optional.of(parameters);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public synchronized void close() throws DataStoreException {
        try {
            reader.channel.channel.close();
            reader = null;
        } catch (IOException ex) {
            throw new DataStoreException("Fail to closed channel", ex);
        }
    }

    private synchronized ISOBMFFReader getReader() throws DataStoreException {
        if (reader == null) {
            final StorageConnector cnx = new StorageConnector(gimiPath);
            final ChannelDataInput cdi = cnx.getStorageAs(ChannelDataInput.class);
            reader = new ISOBMFFReader(cdi);
        }
        return reader;
    }

    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (components != null) return components;

        components = new ArrayList<>();
        try {
            final ISOBMFFReader reader = getReader();

            root = new Box() {
                @Override
                public boolean isContainer() {
                    return true;
                }
            };
            root.readPayload(reader.channel);

            Meta meta = (Meta) root.getChild(Meta.FCC, null, reader.channel);
            ItemInfo iinf = (ItemInfo) meta.getChild(ItemInfo.FCC, null, reader.channel);
            iinf.readPayload(reader.channel);
            for (ItemInfoEntry iie : iinf.entries) {
                final Item item = new Item(iie);
                if (UncompressedImage.TYPE.equals(iie.itemType)) {
                    //uncompressed image
                    components.add(new UncompressedImage(item));
                } else if (Grid.TYPE.equals(iie.itemType)) {
                    //tiled image
                    components.add(new Grid(item));
                } else {
                    //TODO
                    //ignore all others for now
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return components;
    }

    /**
     * A single uncompressed image.
     */
    private final class UncompressedImage extends AbstractGridCoverageResource implements StoreResource {

        public static final String TYPE = "unci";

        private final Item item;
        private ComponentDefinition compDef;
        private ImageSpatialExtents imageExt;
        private UncompressedFrameConfig frameConf;
        private ModelTransformationProperty modelTrs;
        private ModelTiePointProperty modelTp;
        private WellKnownText2Property modelWkt;
        private MediaData mediaData;

        public UncompressedImage(Item item) throws DataStoreException {
            super(GimiStore.this);
            this.item = item;

            for (Box box : item.properties) {
                if (box instanceof ComponentDefinition) compDef = (ComponentDefinition) box;
                else if (box instanceof ImageSpatialExtents) imageExt = (ImageSpatialExtents) box;
                else if (box instanceof UncompressedFrameConfig) frameConf = (UncompressedFrameConfig) box;
                else if (box instanceof ModelTransformationProperty) modelTrs = (ModelTransformationProperty) box;
                else if (box instanceof ModelTiePointProperty) modelTp = (ModelTiePointProperty) box;
                else if (box instanceof WellKnownText2Property) modelWkt = (WellKnownText2Property) box;
            }
        }

        @Override
        public DataStore getOriginator() {
            return GimiStore.this;
        }

        @Override
        public GridGeometry getGridGeometry() throws DataStoreException {

            try {
                final GridExtent extent = new GridExtent(imageExt.imageWidth, imageExt.imageHeight);
                final AffineTransform2D gridToCrs = new AffineTransform2D(
                        modelTrs.transform[0], modelTrs.transform[3], modelTrs.transform[1], modelTrs.transform[4], modelTrs.transform[2], modelTrs.transform[5]);
                String wkt = modelWkt.wkt2;
                //TODO remove this hack when SIS support BASEGEOGCRS
                wkt = wkt.replace("BASEGEOGCRS", "BASEGEODCRS");
                final CoordinateReferenceSystem crs = CRS.fromWKT(wkt);
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
                    case 4 : sdb.setName("Red"); break;
                    case 5 : sdb.setName("Green"); break;
                    case 6 : sdb.setName("Blue"); break;
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

            for (int y =  0; y < 1024; y++) {
                for (int x = 0; x < 2048; x++) {
                    int offset = y*2048 + x;
                    raster.setSample(x, y, 0, mediaData.data[offset*3] & 0xFF);
                    raster.setSample(x, y, 1, mediaData.data[offset*3+1] & 0xFF);
                    raster.setSample(x, y, 2, mediaData.data[offset*3+2] & 0xFF);
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

    private final class Grid extends AbstractGridCoverageResource implements TiledResource, StoreResource {

        public static final String TYPE = "grid";

        private final Item item;

        public Grid(Item item) throws DataStoreException {
            super(GimiStore.this);
            this.item = item;
        }

        @Override
        public GridGeometry getGridGeometry() throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public List<SampleDimension> getSampleDimensions() throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Collection<? extends TileMatrixSet> getTileMatrixSets() throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public DataStore getOriginator() {
            return GimiStore.this;
        }

    }

    /**
     * Regroup properties of a single item in the file.
     */
    private final class Item {

        public final ItemInfoEntry entry;
        public final boolean isPrimary;
        public final List<Box> properties = new ArrayList<>();
        public final List<SingleItemTypeReference> references = new ArrayList<>();
        public final ItemLocation.Item location;

        public Item(ItemInfoEntry entry) throws IllegalArgumentException, DataStoreException, IOException {
            this.entry = entry;

            final ISOBMFFReader reader = getReader();
            final Box meta = root.getChild(Meta.FCC, null, reader.channel);

            //is item primary
            final PrimaryItem primaryItem = (PrimaryItem) meta.getChild(PrimaryItem.FCC, null, reader.channel);
            if (primaryItem != null) {
                primaryItem.readPayload(reader.channel);
                isPrimary = primaryItem.itemId == entry.itemId;
            } else {
                isPrimary = true;
            }

            //extract properties
            final Box itemProperties = meta.getChild(ItemProperties.FCC, null, reader.channel);
            if (itemProperties != null) {
                itemProperties.readPayload(reader.channel);
                final ItemPropertyContainer itemPropertiesContainer = (ItemPropertyContainer) itemProperties.getChild(ItemPropertyContainer.FCC, null, reader.channel);
                itemPropertiesContainer.readPayload(reader.channel);
                final List<Box> allProperties = itemPropertiesContainer.getChildren(reader.channel);
                final ItemPropertyAssociation itemPropertiesAssociations = (ItemPropertyAssociation) itemProperties.getChild(ItemPropertyAssociation.FCC, null, reader.channel);
                itemPropertiesAssociations.readPayload(reader.channel);

                for (ItemPropertyAssociation.Entry en : itemPropertiesAssociations.entries) {
                    if (en.itemId == entry.itemId) {
                        for(int i : en.propertyIndex) {
                            properties.add(allProperties.get(i-1)); //starts at 1
                        }
                        break;
                    }
                }
            }

            //extract outter references
            final ItemReference itemReferences = (ItemReference) meta.getChild(ItemReference.FCC, null, reader.channel);
            if (itemReferences != null) {

            }


            //extract location
            ItemLocation.Item loc = null;
            final ItemLocation itemLocation = (ItemLocation) meta.getChild(ItemLocation.FCC, null, reader.channel);
            if (itemLocation != null) {
                for (ItemLocation.Item en : itemLocation.items) {
                    if (en.itemId == entry.itemId) {
                        loc = en;
                        break;
                    }
                }
            }
            this.location = loc;
        }

        public byte[] getData() throws DataStoreException {
            try {
                final ISOBMFFReader reader = getReader();

                if (location == null) {
                    //read data from the default mediadata box
                    MediaData mediaData = (MediaData) root.getChild(MediaData.FCC, null, getReader().channel);
                    mediaData.readPayload(getReader().channel);
                    return mediaData.data;
                } else if (location.constructionMethod == 0) {
                    //absolute location
                    if (location.dataReferenceIndex == 0) {
                        //compute total size
                        final int length = IntStream.of(location.extentLength).sum();
                        //read datas
                        final byte[] data = new byte[length];
                        for (int i = 0, offset = 0; i < location.extentLength.length; i++) {
                            reader.channel.seek(location.baseOffset + location.extentOffset[i]);
                            reader.channel.readFully(data, offset, location.extentLength[i]);
                        }
                        return data;
                    } else {
                        throw new IOException("Not supported yet");
                    }

                } else if (location.constructionMethod == 1) {
                    throw new IOException("Not supported yet");
                } else if (location.constructionMethod == 2) {
                    throw new IOException("Not supported yet");
                } else {
                    throw new DataStoreException("Unexpected construction method " + location.constructionMethod);
                }

            } catch (IOException ex) {
                throw new DataStoreException("Failed reading data", ex);
            }
        }

    }
}
