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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.IOException;
import javax.imageio.spi.ImageReaderSpi;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.ByteRanges;
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
    final GeoHeifStore store;

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
     * Information about all resources in the order they were found in the file.
     * Keys are {@link ItemInfoEntry#itemID} values of the associated map values.
     * Key 0 is a special value meaning that the value is the {@link #primaryItem}.
     *
     * <p>Values shall be instances of {@link ItemInfoEntry} or {@code ItemInfoEntry[]}.
     * Each entry should contain only one item, but this class nevertheless accept many.
     * Note that some items may be members of a group such as a pyramid.</p>
     *
     * @see #info(Object)
     */
    private final Map<Integer, Object> itemInfos;

    /**
     * Returns the given {@link #itemInfos} value as a list of {@link ItemInfoEntry} instances.
     * The list should contain exactly one element, but may also be empty in case of missing value.
     * List of more than one element are supported for avoiding to choose an arbitrary element.
     */
    private static List<ItemInfoEntry> info(final Object value) {
        if (value == null) {
            return Collections.emptyList();
        } else if (value instanceof ItemInfoEntry) {
            return Collections.singletonList((ItemInfoEntry) value);
        } else {
            return Arrays.asList((ItemInfoEntry[]) value);
        }
    }

    /**
     * References from the resources to other resources.
     * The main usage is for a tiled image referencing each tile as an individual image.
     * May be {@code null} if no such information was found.
     *
     * <p><b>Implementation note:</b>
     * We perform linear search in this array for a specific value of {@link SingleItemTypeReference#fromItemID}.
     * This is less efficient than using an hash map, but we assume that this array is short.
     * Note that we also need to iterate over the array elements anyways.</p>
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
     * How to group the resources. If this resource contains pyramids, these pyramids are declared here.
     */
    private final List<GroupList> groups;

    /**
     * Resources built by this class, in the order they were found in the file.
     * Keys are {@link ItemInfoEntry#itemID} values.
     */
    private final Map<Integer, List<Resource>> itemResources;

    /**
     * All data when no specific data where found in {@link #itemLocations}.
     */
    private MediaData data;

    /**
     * Provider of Image I/O readers for the <abbr>JPEG</abbr> format,
     * or {@code null} if not yet fetched.
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
        itemResources   = new LinkedHashMap<>();
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
                    itemInfos.merge(entry.itemID, entry, (current, value) -> {
                        final var more = (ItemInfoEntry) value;
                        return (current instanceof ItemInfoEntry)
                                ? new ItemInfoEntry[] {(ItemInfoEntry) current, more}
                                : ArraysExt.append((ItemInfoEntry[]) current, more);
                    });
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
     * Returns the provider of <abbr>JPEG</abbr> readers.
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
        for (ItemInfoEntry entry : info(itemInfos.get(itemID))) {
            if (entry.itemName != null) {
                return entry.itemName;
            }
        }
        return Integer.toUnsignedString(itemID);
    }

    /**
     * Returns the location of data for the item having the given identifier, or {@code null} if none.
     *
     * @param  itemID  identifier of the item to read.
     * @return the item for locating the identified data, or {@code null} if none.
     * @throws DataStoreContentException if there are two ore more items for the same identifier.
     */
    private ByteRanges.Reader getLocationByIdentifier(final int itemID) throws DataStoreContentException {
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
                    coverage.setTileBuilder(createImage(toItem, info(itemInfos.remove(toItem)), addTo));
                }
            }
            break;
        }
    }

    /**
     * Creates the image for the item specified by the given identifier if the image sample model is supported.
     * The resource may be an untiled image (item type {@code unci}) or a tiled image (item type {@code grid}).
     *
     * @param  itemID  identifier of the item to build as a coverage.
     * @throws IOException if an error occurred while reading bytes from the input stream.
     * @throws DataStoreException if another error occurred while building the image or resource.
     */
    private void createOptionalImage(final Integer itemID) throws DataStoreException, IOException {
        try {
            createImage(itemID, info(itemInfos.remove(itemID)), null);
        } catch (UnsupportedEncodingException e) {
            store.listeners().warning("A resource uses an unsupported sample model.", e);
        }
    }

    /**
     * Creates the image for the item specified by the given identifier.
     * The resource may be an untiled image (item type {@code unci}) or a tiled image (item type {@code grid}).
     * If the {@code addTo} list is {@code null}, then the image is added to the {@link #itemResources} map.
     *
     * <p>The given {@code info} list should contain exactly one entry.
     * This method nevertheless tries to be robust to cases where two or more entries exist,
     * but it shouldn't happen. The first entry that this method recognizes has precedence.</p>
     *
     * @param  itemID  identifier of the item to build as a coverage.
     * @param  info    information about the item, normally as a singleton but other size are nevertheless accepted.
     * @param  addTo   a list where to add the image, or {@code null} for adding to {@link #itemResources} instead.
     * @return the builder used for building the first image, or {@code null} if none.
     * @throws IOException if an error occurred while reading bytes from the input stream.
     * @throws DataStoreContentException if the "grid to <abbr>CRS</abbr>" transform or the sample dimensions cannot be created.
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
            final int imageIndex;
            if (addTo != null) {
                imageIndex = addTo.size();
            } else {
                final List<Resource> resources = itemResources.get(entry.itemID);
                imageIndex = (resources != null) ? resources.size() : 0;
            }
            final ItemProperties.ForID itemProperties = properties.remove(itemID);
            final CoverageBuilder coverage = builders.computeIfAbsent(itemProperties,
                    (p) -> new CoverageBuilder(this, imageIndex, p, duplicatedBoxes));
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
            Image image = null;
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
                        List<Image> tiles = null;   // Wait to know the number of items before to create.
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
                            resources(entry.itemID).add(coverage.build(name, tiles));
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
                    final ImageReaderSpi provider = readerJPEG();
                    final ByteRanges.Reader locator = getLocationByIdentifier(itemID);
                    if (locator != null) {
                        image = new FromImageIO(coverage, locator, provider, name);
                        coverage.setImageLayout(image);
                    }
                    break;
                }
                /*
                 * Untiled image, or a single tile of a tiled image. This case gets the box that contains
                 * all pixel values (the "reader", as the byte values are behind an input stream), but the
                 * actual reading is deferred.
                 */
                case ItemInfoEntry.UNCI: {
                    final ByteRanges.Reader locator = getLocationByIdentifier(itemID);
                    if (locator != null) {
                        image = new UncompressedImage(coverage, locator, name);
                    }
                    break;
                }
            }
            if (image == null) {
                warning("No data found for the \"{0}\" resource.", name);
            } else {
                if (addTo != null) {
                    addTo.add(image);
                } else {
                    builders.remove(itemProperties);    // Builder cannot be reused after resource creation.
                    resources(entry.itemID).add(coverage.build(name, image));
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
            List<ItemInfoEntry> info = info(itemInfos.remove(primary.itemID));
            if (info.isEmpty()) {
                info = info(itemInfos.remove(0));     // `itemInfoEntry.itemID` = 0 means the primary item.
                if (info.isEmpty()) continue;
            }
            createImage(primary.itemID, info, null);
        }
        /*
         * Create an image for all remaining items (items other than the primary item).
         * We need to process first the items which contain references to other items,
         * because they may be tiled images referencing their tiles.
         */
        if (references != null) {
            for (final SingleItemTypeReference items : references) {
                if (items.type() == DerivedImageReference.BOXTYPE) {
                    createOptionalImage(items.fromItemID);
                }
            }
        }
        // Iterate over a snapshot because elements may be removed by `createImage(…)`.
        for (final Integer itemID : itemInfos.keySet().toArray(Integer[]::new)) {
            createOptionalImage(itemID);
        }
        /*
         * At this point, all resources have either been created or discarded.
         * Some components may be members of groups, with pyramid as a special kind of group.
         */
        for (final GroupList box : groups) {
            for (Box child : box.children) {
                if (child instanceof EntityToGroup group) {     // Should be the type of all children.
                    final GenericName name = store.createComponentName(getResourceName(group.groupID));
                    final var components = new ArrayList<ImageResource>(group.entityID.length);
                    for (int entityID : group.entityID) {
                        final Iterator<Resource> it = itemResources.getOrDefault(entityID, List.of()).iterator();
                        while (it.hasNext()) {
                            final Resource grid = it.next();
                            if (grid instanceof ImageResource) {
                                components.add((ImageResource) grid);
                                it.remove();
                            }
                        }
                    }
                    final Resource resource;
                    switch (components.size()) {
                        case 0: continue;
                        case 1: resource = components.get(0); break;
                        default: {
                            final var grids = components.toArray(ImageResource[]::new);
                            if (child instanceof ImagePyramid pyramid) {
                                resource = new Pyramid(store, name, pyramid, grids);
                            } else {
                                resource = new Group(store, name, grids);
                            }
                        }
                    }
                    resources(group.groupID).add(resource);
                }
            }
        }
        // Concatenate the collection of lists to a single list.
        return itemResources.values().stream()
                .map(List::stream)
                .reduce(Stream::concat)
                .orElse(Stream.empty())
                .toArray(Resource[]::new);
    }

    /**
     * Returns the resource for the given item identifier.
     *
     * @param  itemID  item identifier for which to get the resources.
     * @return modifiable list of resources for the given identifier.
     */
    private List<Resource> resources(final int itemID) {
        return itemResources.computeIfAbsent(itemID, (key) -> new ArrayList<>());
    }
}
