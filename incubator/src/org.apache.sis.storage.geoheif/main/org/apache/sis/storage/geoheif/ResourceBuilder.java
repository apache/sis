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
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.awt.image.RasterFormatException;
import java.io.IOException;
import javax.imageio.spi.ImageReaderSpi;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.ByteReader;
import org.apache.sis.storage.isobmff.Root;
import org.apache.sis.storage.isobmff.base.EntityToGroup;
import org.apache.sis.storage.isobmff.base.GroupList;
import org.apache.sis.storage.isobmff.base.Meta;
import org.apache.sis.storage.isobmff.base.ItemInfo;
import org.apache.sis.storage.isobmff.base.ItemInfoEntry;
import org.apache.sis.storage.isobmff.base.ItemLocation;
import org.apache.sis.storage.isobmff.base.ItemProperties;
import org.apache.sis.storage.isobmff.base.ItemPropertyAssociation;
import org.apache.sis.storage.isobmff.base.SingleItemTypeReference;
import org.apache.sis.storage.isobmff.base.ItemReference;
import org.apache.sis.storage.isobmff.base.MediaData;
import org.apache.sis.storage.isobmff.base.PrimaryItem;
import org.apache.sis.storage.isobmff.image.DerivedImageReference;
import org.apache.sis.storage.isobmff.image.ImagePyramid;
import org.apache.sis.util.resources.Errors;


/**
 * Helper class for building the resources of a GeoHEIF file.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class ResourceBuilder {
    /**
     * The data store for which the resources are built.
     */
    private final GeoHeifStore store;

    /**
     * Identifier of the primary item. Should contain at most one element, but we are paranoiac.
     */
    private final List<PrimaryItem> primaryItem;

    /**
     * The location of items in the stream of bytes.
     * Keys are the {@link ItemLocation.Item#itemID} value of the associated map value.
     */
    private final Map<Integer, ItemLocation.Item> itemLocations;

    /**
     * Various properties (image size, coordinate reference system, <i>etc.</i>).
     * They should be inferred from exactly one {@link ItemProperties} instance.
     * If nevertheless many instances are found, their content will be merged.
     * Keys are {@link ItemPropertyAssociation.Entry#itemID} values.
     */
    private final Map<Integer, ItemProperties.ForID> properties;

    /**
     * Information about all resources.
     * Keys are resource identifiers, or 0 for the {@linkplain #primaryItem}.
     * Keys are {@link ItemInfoEntry#itemID} values of the associated map value.
     * Each list should contain only one item, but this class nevertheless accept many.
     * Note that some items may be members of a group such as a pyramid.
     */
    private final Map<Integer, List<ItemInfoEntry>> itemInfos;

    /**
     * References from the resources to other resources.
     * The main usage is for a tiled image referencing each tile as an individual image.
     * May be {@code null} if no such information was found.
     */
    private SingleItemTypeReference[] references;

    /**
     * Helper objects for building coverages.
     * The internal state of each builder depends only on the properties in the associated key.
     */
    private final Map<ItemProperties.ForID, CoverageBuilder> builders;

    /**
     * Names of boxes that were duplicated.
     * Used for logging a warning only once per type of box.
     */
    private final Set<String> duplicatedBoxes;

    /**
     * How to group the resources. If there is pyramids, they are declared here.
     */
    private final List<GroupList> groups;

    /**
     * Resources built by this class.
     */
    private final List<Resource> resources;

    /**
     * All data when no specific data where found in {@link #locations}.
     */
    private MediaData data;

    /**
     * Provider of Image I/O readers for the JPEG format, or {@code null} if not yet fetched.
     *
     * @see #readerJPEG()
     */
    private ImageReaderSpi readerJPEG;

    /**
     * Prepares a new builder from the information found in the boxes.
     *
     * @param  store  the data store for which the resources are built.
     * @param  root   the root node which contains all boxes of the file.
     */
    ResourceBuilder(final GeoHeifStore store, final Root root) {
        this.store      = store;
        primaryItem     = new ArrayList<>(3);
        itemLocations   = new HashMap<>();
        properties      = new HashMap<>();
        itemInfos       = new LinkedHashMap<>();
        builders        = new HashMap<>();
        duplicatedBoxes = new HashSet<>();
        groups          = new ArrayList<>();
        resources       = new ArrayList<>();
        for (final Box box : root.children) {
            switch (box.type()) {
                case MediaData.BOXTYPE: {
                    data = (MediaData) box;
                    break;
                }
                case Meta.BOXTYPE: {
                    for (Box meta : ((Meta) box).children) {
                        acceptMetaBox(meta);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Takes the information from a box contained inside the {@link Meta} box.
     * Each type of box should appear only once. If, nevertheless, a type appears twice or more,
     * then this method concatenates the information if possible, or logs a warning otherwise.
     */
    private void acceptMetaBox(final Box box) {
        switch (box.type()) {
            // Name, content type, etc.
            case ItemInfo.BOXTYPE: {
                for (final ItemInfoEntry entry : ((ItemInfo) box).entries) {
                    itemInfos.computeIfAbsent(entry.itemID, (itemID) -> new ArrayList<ItemInfoEntry>(3)).add(entry);
                }
                break;
            }
            // Where to find the sequence of bytes for an item
            case ItemLocation.BOXTYPE: {
                for (final ItemLocation.Item item : ((ItemLocation) box).items) {
                    if (itemLocations.putIfAbsent(item.itemID, item) != null) {
                        warning("Many locations found for the \"{0}\" resource.", getResourceName(item.itemID));
                    }
                }
                break;
            }
            case ItemProperties.BOXTYPE: {
                ((ItemProperties) box).collect(properties);
                break;
            }
            case ItemReference.BOXTYPE: {
                references = ArraysExt.concatenate(references, ((ItemReference) box).references);
                break;
            }
            case GroupList.BOXTYPE: {
                groups.add((GroupList) box);
                break;
            }
            case PrimaryItem.BOXTYPE: {
                primaryItem.add((PrimaryItem) box);
                break;
            }
        }
    }

    /**
     * Logs a warning as if it was emitted by {@link GeoHeifStore#components()}.
     *
     * @param  message  the message with a "{0}" pattern to be replaced by the resource name.
     * @param  name     the resource name.
     */
    private void warning(String message, final String name) {
        message = message.replace("{0}", name);
        store.warning(new LogRecord(Level.WARNING, message));
    }

    /**
     * Returns the provider of JPEG readers.
     */
    private ImageReaderSpi readerJPEG() throws UnsupportedEncodingException {
        if (readerJPEG == null) {
            readerJPEG = FromImageIO.byFormatName("JPEG");
        }
        return readerJPEG;
    }

    /**
     * Returns the name for the item specified by the given identifier.
     * If no name is found, then the give {@code itemID} is formatted.
     *
     * @param  itemID  identifier of the item for which to get name.
     * @return a non-null item name.
     */
    private String getResourceName(final int itemID) {
        final List<ItemInfoEntry> info = itemInfos.get(itemID);
        if (info != null) {
            for (ItemInfoEntry entry : info) {
                if (entry.itemName != null) {
                    return entry.itemName;
                }
            }
        }
        return Integer.toUnsignedString(itemID);
    }

    /**
     * Returns the location of data for the item having the given identifier, or {@code null} if none.
     *
     * @param  itemID  identifier of the item to read.
     * @return the item for locating the identified data, or {@code null} if none.
     * @throws DataStoreContentException if there is two ore more items for the same identifier.
     */
    private ByteReader getLocationByIdentifier(final int itemID) throws DataStoreContentException {
        final Object item = itemLocations.get(itemID);
        if (item == null) {
            return data;    // May be null.
        }
        if (item instanceof ItemLocation.Item) {
            return (ItemLocation.Item) item;
        }
        throw new DataStoreContentException(Errors.format(Errors.Keys.DuplicatedIdentifier_1, itemID));
    }

    /**
     * Creates all tiles referenced by the given box.
     *
     * @param  coverage  the builder of the coverage that contains the tiles.
     * @param  items     references to the tiles.
     * @param  addTo     a list where to add the tiles.
     * @throws DataStoreException if an error occurred while building a tile.
     * @return the builder used for building the first tile.
     */
    private void createTiles(final CoverageBuilder coverage, final SingleItemTypeReference items, final List<Image> addTo)
            throws DataStoreException, IOException
    {
        switch (items.type()) {
            // Only one case for now, but more cases may be added in the future.
            case DerivedImageReference.BOXTYPE: {
                for (final Integer toItem : items.toItemID) {
                    final List<ItemInfoEntry> info = itemInfos.remove(toItem);
                    if (info != null) {
                        coverage.setTileBuilder(createImage(toItem, info, addTo));
                    }
                }
            }
            break;
        }
    }

    /**
     * Creates the image for the item specified by the given identifier.
     * The resource may be an untiled image (item type {@code unci}) or a tiled image (item type {@code grid}).
     * If the {@code addTo} list is {@code null}, then the image is added to the {@linkplain #resources} list.
     *
     * <p>The given {@code info} list should contain exactly one entry.
     * This method nevertheless tries to be robust to cases where two or more entries exist,
     * but it shouldn't happen. The first entry that this method recognizes has precedence.</p>
     *
     * @param  itemID  identifier of the item to build as a coverage.
     * @param  info    information about the item, normally as a singleton but other size are nevertheless accepted.
     * @param  addTo   a list where to add the image, or {@code null} for adding to {@link #resources} instead.
     * @return the builder used for building the image. If many images were created, returns the first builder.
     * @throws RasterFormatException if the sample dimensions or sample model cannot be created.
     * @throws DataStoreException if another error occurred while building the image or resource.
     */
    private CoverageBuilder createImage(final Integer itemID, final List<ItemInfoEntry> info, final List<Image> addTo)
            throws DataStoreException, IOException
    {
        CoverageBuilder firstBuilder = null;
        for (final ItemInfoEntry entry : info) {
            final String name = entry.itemName();
            if (entry.itemProtectionIndex != 0) {
                warning("The \"{0}\" resource is protected.", name);
                continue;
            }
            final ItemProperties.ForID itemProperties = properties.remove(itemID);
            final CoverageBuilder coverage = builders.computeIfAbsent(itemProperties,
                    (p) -> new CoverageBuilder(store, p, duplicatedBoxes));
            if (coverage.reportUnknownBoxes(name)) {
                // Warning already logged by `reportUnknownBoxes(…)`.
                continue;
            }
            if (coverage.isEmpty()) {
                warning("The \"{0}\" resource is empty.", name);
                continue;
            }
            if (firstBuilder == null) {
                firstBuilder = coverage;
            }
            final ByteReader locator;
            final Supplier<Image> image;
            switch (entry.itemType) {
                default: {
                    warning("Unsupported type " + Box.formatFourCC(entry.itemType) + " for the \"{0}\" resource.", name);
                    continue;
                }
                /*
                 * Tiled image: the tiles are defined as an array of images of type `ItemInfoEntry.UNCI`.
                 * This case invokes this method recursively for each tile. The tiles are collected into
                 * the `addTo` list. In the current implementation, if some tiles are themselves gridded
                 * image, the list of tiles is flattened. However, we don't know if such nested grids
                 * happen in practice and if this is the way to handle them.
                 */
                case ItemInfoEntry.GRID: {
                    if (references != null) {
                        List<Image> tiles = null;
                        for (final SingleItemTypeReference items : references) {
                            if (items.fromItemID == entry.itemID) {
                                if (tiles == null && (tiles = addTo) == null) {
                                    tiles = new ArrayList<>(items.toItemID.length);
                                }
                                createTiles(coverage, items, tiles);
                            }
                        }
                        if (addTo == null && tiles != null && !tiles.isEmpty()) {
                            builders.remove(itemProperties);    // Builder cannot be reused after resource creation.
                            resources.add(coverage.build(name, tiles));
                        }
                    }
                    continue;
                }
                /*
                 * JPEG compression, handled like `UNCI` except that the payload is decoded by Image I/O.
                 * Contrarily to the `UNCI` case, it is okay to build the `Image` instance now because
                 * the constructor will not ask for the sample model.
                 */
                case ItemInfoEntry.JPEG: {
                    final ImageReaderSpi reader = readerJPEG();
                    locator = getLocationByIdentifier(itemID);
                    final var prepared = new FromImageIO(coverage, locator, reader, name);
                    if (locator != null) coverage.setImageLayout(prepared);
                    image = () -> prepared;
                    break;
                }
                /*
                 * Untiled image, or a single tile of a tiled image. This case gets the box that contains
                 * all pixel values (the "reader", as the byte values are behind an input stream), but the
                 * actual reading is deferred.
                 */
                case ItemInfoEntry.UNCI: {
                    locator = getLocationByIdentifier(itemID);
                    image = () -> new UncompressedImage(coverage, locator, name);
                    break;
                }
            }
            if (locator == null) {
                warning("No data found for the \"{0}\" resource.", name);
            } else {
                if (addTo != null) {
                    addTo.add(image.get());
                } else {
                    builders.remove(itemProperties);    // Builder cannot be reused after resource creation.
                    resources.add(coverage.build(name, image));
                }
            }
        }
        return firstBuilder;
    }

    /**
     * Creates the resources. This method only prepares the information needed for reading pixel values.
     * The actual reading does not happen here.
     *
     * @return the resource.
     * @throws DataStoreException if another error occurred while building the image or resource.
     */
    final Resource[] build() throws DataStoreException, IOException {
        for (final PrimaryItem primary : primaryItem) {
            List<ItemInfoEntry> info = itemInfos.remove(primary.itemID);
            if (info == null) {
                info = itemInfos.remove(0);     // `itemInfoEntry.itemID` = 0 means the primary item.
                if (info == null) continue;
            }
            try {
                createImage(primary.itemID, info, null);
            } catch (RasterFormatException e) {
                // Considered fatal because it was the primary resource.
                throw new DataStoreContentException("Unsupported image layout.", e);
            }
        }
        // Iterate over a snapshot because elements may be removed by `createImage(…)`.
        for (final Integer itemID : itemInfos.keySet().toArray(Integer[]::new)) {
            final List<ItemInfoEntry> info = itemInfos.remove(itemID);
            if (info != null) try {
                createImage(itemID, info, null);
            } catch (RasterFormatException e) {
                store.listeners().warning("A resource uses an unsupported sample model.", e);
            }
        }
        /*
         * At this point, all resources have either been created or discarded.
         * Some components may be members of groups, with pyramid as a special kind of group.
         */
        for (final GroupList box : groups) {
            for (Box child : box.children) {
                if (child instanceof EntityToGroup group) {     // Should be the type of all children.
                    final GenericName name = store.createComponentName(getResourceName(group.groupID));
                    var grids = new GridCoverageResource[group.entityID.length];
                    int count = 0;
                    for (int entityID : group.entityID) {
                        // TODO: wrong class
                        var grid = (GridCoverageResource) itemInfos.remove(entityID);
                        if (grid != null) grids[count++] = grid;
                    }
                    grids = ArraysExt.resize(grids, count);
                    final Resource resource;
                    if (child instanceof ImagePyramid pyramid) {
                        new Pyramid(store, name, pyramid, grids); // TODO
                        continue;
                    } else {
                        resource = new Group(store, name, grids);
                    }
                    resources.add(resource);
                }
            }
        }
        return resources.toArray(Resource[]::new);
    }
}
