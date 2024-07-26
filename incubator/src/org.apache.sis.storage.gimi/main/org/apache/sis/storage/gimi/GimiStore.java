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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
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
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.gimi.internal.ScaleSortedMap;
import org.apache.sis.storage.gimi.internal.TileMatrices;
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
import org.apache.sis.storage.tiling.Tile;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TileStatus;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.util.iso.Names;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GimiStore extends DataStore implements Aggregate {

    private final Path gimiPath;

    private List<Resource> components;
    private Map<Integer,Resource> componentIndex;

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
        componentIndex = new HashMap<>();
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
                Resource resource;
                if (UncompressedImage.TYPE_UNCOMPRESSED.equals(iie.itemType)) {
                    //uncompressed image
                    resource = new UncompressedImage(item);
                } else if (Jpeg.TYPE_JPEG.equals(iie.itemType)) {
                    //tiled image
                    resource = new Jpeg(item);
                }  else if (Grid.TYPE.equals(iie.itemType)) {
                    //tiled image
                    resource = new Grid(item);
                } else {
                    //TODO
                    resource = new UnknownResource(item);
                }
                components.add(resource);
                componentIndex.put(iie.itemId, resource);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return components;
    }

    private final class UnknownResource extends AbstractResource implements StoreResource{

        private final Item item;

        public UnknownResource(Item item) throws DataStoreException {
            super(GimiStore.this);
            this.item = item;

        }

        @Override
        public DataStore getOriginator() {
            return GimiStore.this;
        }
    }

    /**
     * A single uncompressed image.
     */
    private class UncompressedImage extends AbstractGridCoverageResource implements StoreResource {

        public static final String TYPE_UNCOMPRESSED = "unci";

        protected final Item item;
        protected ComponentDefinition compDef;
        protected ImageSpatialExtents imageExt;
        protected UncompressedFrameConfig frameConf;
        protected ModelTransformationProperty modelTrs;
        protected ModelTiePointProperty modelTp;
        protected WellKnownText2Property modelWkt;

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
                    raster.setSample(x, y, 0, data[offset*3] & 0xFF);
                    raster.setSample(x, y, 1, data[offset*3+1] & 0xFF);
                    raster.setSample(x, y, 2, data[offset*3+2] & 0xFF);
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

    private final class Jpeg extends UncompressedImage {

        public static final String TYPE_JPEG = "jpeg";

        public Jpeg(Item item) throws DataStoreException {
            super(item);
        }

        @Override
        public GridCoverage read(GridGeometry gg, int... ints) throws DataStoreException {
            final byte[] data = item.getData();

            ImageInputStream iis;
            BufferedImage img;
            try {
                iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data));
                img = ImageIO.read(iis);
            } catch (IOException ex) {
                throw new DataStoreException(ex);
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
        private final GenericName identifier;

        //filled after initialize
        private GridCoverageResource first;
        private CoordinateReferenceSystem crs;
        private TileMatrix tileMatrix;
        private GimiTileMatrixSet tileMatrixSet;

        public Grid(Item item) throws DataStoreException {
            super(GimiStore.this);
            this.item = item;
            if (item.entry.itemName == null || item.entry.itemName.isBlank()) {
                this.identifier = Names.createLocalName(null, null, Integer.toString(item.entry.itemId));
            } else {
                this.identifier = Names.createLocalName(null, null, item.entry.itemName);
            }
        }

        @Override
        public Optional<GenericName> getIdentifier() {
            return Optional.of(identifier);
        }

        @Override
        public DataStore getOriginator() {
            return GimiStore.this;
        }

        private synchronized void initialize() throws DataStoreException {
            final Resource first = componentIndex.get(item.references.get(0).toItemId[0]);
            if (first instanceof GridCoverageResource) {
                this.first = (GridCoverageResource) first;
            } else {
                throw new DataStoreException("Expecting a GridCoverageResource tile but was a " + first.getClass().getName());
            }

            final GridGeometry firstTileGridGeom = this.first.getGridGeometry();
            this.crs = firstTileGridGeom.getCoordinateReferenceSystem();
            final GridExtent tileExtent = firstTileGridGeom.getExtent();
            final int[] tileSize = new int[]{Math.toIntExact(tileExtent.getSize(0)), Math.toIntExact(tileExtent.getSize(1))};
            final MathTransform matrixGridToCrs = firstTileGridGeom.derive().subgrid(null, tileSize).build().getGridToCRS(PixelInCell.CELL_CENTER);

            for (Box b : item.properties) {
                if (b instanceof ImageSpatialExtents) {
                    final ImageSpatialExtents ext = (ImageSpatialExtents) b;
                    final long matrixWidth = ext.imageWidth / tileExtent.getSize(0);
                    final long matrixHeight = ext.imageHeight / tileExtent.getSize(1);

                    //create tile matrix
                    final GridGeometry tilingScheme = new GridGeometry(new GridExtent(matrixWidth, matrixHeight), PixelInCell.CELL_CENTER, matrixGridToCrs, crs);
                    tileMatrix = new GimiTileMatrix(this, tilingScheme, tileSize);

                    //create tile matrix set
                    tileMatrixSet = new GimiTileMatrixSet(Names.createLocalName(null, null, identifier.tip().toString() + "_tms"), crs);
                    tileMatrixSet.matrices.insertByScale(tileMatrix);
                }
            }
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

        @Override
        public GridGeometry getGridGeometry() throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet.");
        }


        @Override
        public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    private final class GimiTile implements Tile {

        private final long[] indices;
        private final int itemId;

        public GimiTile(long[] indices, int itemId) {
            this.indices = indices;
            this.itemId = itemId;
        }

        @Override
        public long[] getIndices() {
            return indices.clone();
        }

        @Override
        public TileStatus getStatus() {
            return componentIndex.containsKey(itemId) ? TileStatus.EXISTS : TileStatus.MISSING;
        }

        @Override
        public Resource getResource() throws DataStoreException {
            final Resource res = componentIndex.get(itemId);
            if (res == null) throw new DataStoreContentException("Missing tile at " + Arrays.toString(indices) + ", it should not be possible with GIMI model");
            return res;
        }
    }

    private final class GimiTileMatrix implements TileMatrix {

        private final Grid grid;
        private final GenericName identifier;
        private final GridGeometry tilingScheme;
        private final int[] tileSize;

        public GimiTileMatrix(Grid grid, GridGeometry tilingScheme, int[] tileSize) {
            this.grid = grid;
            this.identifier = Names.createLocalName(null, null, grid.getIdentifier().get().tip().toString()+"_tm");
            this.tilingScheme = tilingScheme;
            this.tileSize = tileSize;
        }

        @Override
        public GenericName getIdentifier() {
            return identifier;
        }

        @Override
        public double[] getResolution() {
            double[] resolution = tilingScheme.getResolution(true);
            resolution[0] /= tileSize[0];
            resolution[1] /= tileSize[1];
            return resolution;
        }

        @Override
        public GridGeometry getTilingScheme() {
            return tilingScheme;
        }

        @Override
        public TileStatus getTileStatus(long... indices) throws DataStoreException {
            return tilingScheme.getExtent().contains(indices) ? TileStatus.EXISTS : TileStatus.OUTSIDE_EXTENT;
        }

        @Override
        public Optional<Tile> getTile(long... indices) throws DataStoreException {
            final int itemIdx = Math.toIntExact(indices[0] + indices[1] * tilingScheme.getExtent().getSize(0));
            return Optional.of(new GimiTile(indices, grid.item.references.get(0).toItemId[itemIdx]));
        }

        @Override
        public Stream<Tile> getTiles(GridExtent indicesRanges, boolean parallel) throws DataStoreException {
            Stream<long[]> stream = TileMatrices.pointStream(indicesRanges);
            stream = parallel ? stream.parallel() : stream.sequential();
            return stream.map(new Function<long[], Tile>(){
                @Override
                public Tile apply(final long[] indices) {
                    try {
                        return getTile(indices).orElse(null);
                    } catch (DataStoreException ex) {
                        return new Tile() {
                            @Override
                            public long[] getIndices() {
                                return indices;
                            }

                            @Override
                            public TileStatus getStatus() {
                                return TileStatus.IN_ERROR;
                            }

                            @Override
                            public Resource getResource() throws DataStoreException {
                                throw ex;
                            }
                        };
                    }
                }
            });
        }
    }

    private final class GimiTileMatrixSet implements TileMatrixSet {

        private final GenericName identifier;
        private final CoordinateReferenceSystem crs;
        private final ScaleSortedMap<TileMatrix> matrices = new ScaleSortedMap<>();

        public GimiTileMatrixSet(GenericName identifier, CoordinateReferenceSystem crs) {
            this.identifier = identifier;
            this.crs = crs;
        }

        @Override
        public GenericName getIdentifier() {
            return identifier;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return crs;
        }

        @Override
        public Optional<Envelope> getEnvelope() {
            if (matrices.isEmpty()) return Optional.empty();
            return Optional.of(matrices.lastEntry().getValue().getTilingScheme().getEnvelope());
        }

        @Override
        public SortedMap<GenericName, ? extends TileMatrix> getTileMatrices() {
            return matrices;
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
            ISOBMFFReader.load(meta, reader.channel);

            //is item primary
            final PrimaryItem primaryItem = (PrimaryItem) meta.getChild(PrimaryItem.FCC, null, reader.channel);
            if (primaryItem != null) {
                isPrimary = primaryItem.itemId == entry.itemId;
            } else {
                isPrimary = true;
            }

            //extract properties
            final Box itemProperties = meta.getChild(ItemProperties.FCC, null, reader.channel);
            if (itemProperties != null) {
                final ItemPropertyContainer itemPropertiesContainer = (ItemPropertyContainer) itemProperties.getChild(ItemPropertyContainer.FCC, null, reader.channel);
                final List<Box> allProperties = itemPropertiesContainer.getChildren(reader.channel);
                final ItemPropertyAssociation itemPropertiesAssociations = (ItemPropertyAssociation) itemProperties.getChild(ItemPropertyAssociation.FCC, null, reader.channel);

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
                for (SingleItemTypeReference sitr : itemReferences.references) {
                    if (sitr.fromItemId == entry.itemId) {
                        references.add(sitr);
                    }
                }
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
