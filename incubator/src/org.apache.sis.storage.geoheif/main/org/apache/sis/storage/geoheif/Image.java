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
import java.util.List;
import java.util.stream.IntStream;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.base.ItemInfoEntry;
import org.apache.sis.storage.isobmff.base.ItemLocation;
import org.apache.sis.storage.isobmff.base.ItemProperties;
import org.apache.sis.storage.isobmff.base.ItemPropertyAssociation;
import org.apache.sis.storage.isobmff.base.ItemPropertyContainer;
import org.apache.sis.storage.isobmff.base.ItemReference;
import org.apache.sis.storage.isobmff.base.MediaData;
import org.apache.sis.storage.isobmff.base.Meta;
import org.apache.sis.storage.isobmff.base.PrimaryItem;
import org.apache.sis.storage.isobmff.base.SingleItemTypeReference;


/**
 * Regroup properties of a single item of the file.
 *
 * @author Johann Sorel (Geomatys)
 */
final class Image {

    private static final ItemLocation.Item NO_LOCATION = new ItemLocation.Item();

    public final GeoHeifStore store;
    public final ItemInfoEntry entry;

    // caches
    private List<Box> properties;
    private List<SingleItemTypeReference> references;
    private ItemLocation.Item location;
    private Boolean isPrimary;

    public Image(GeoHeifStore store, ItemInfoEntry entry) throws IllegalArgumentException, DataStoreException, IOException {
        this.store = store;
        this.entry = entry;
    }

    /**
     * @return true if this item is the primary item defined in the file
     */
    public synchronized boolean isPrimary() throws DataStoreException {
        if (isPrimary != null) return isPrimary;
        try {
            final Box meta = store.getRootBox().getChild(Meta.FCC, null);
            final PrimaryItem primaryItem = (PrimaryItem) meta.getChild(PrimaryItem.FCC, null);
            if (primaryItem != null) {
                isPrimary = primaryItem.itemId == entry.itemId;
            } else {
                isPrimary = true;
            }
        } catch (IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }

        return isPrimary;
    }

    /**
     * List all property boxes.
     *
     * @return list of property boxes, can be empty.
     */
    public synchronized List<Box> getProperties() throws DataStoreException {
        if (properties != null) return properties;

        //extract properties
        properties = new ArrayList<>();
        try {
            final Box meta = store.getRootBox().getChild(Meta.FCC, null);
            final Box itemProperties = meta.getChild(ItemProperties.FCC, null);
            if (itemProperties != null) {
                final ItemPropertyContainer itemPropertiesContainer = (ItemPropertyContainer) itemProperties.getChild(ItemPropertyContainer.FCC, null);
                final List<Box> allProperties = itemPropertiesContainer.getChildren();
                final ItemPropertyAssociation itemPropertiesAssociations = (ItemPropertyAssociation) itemProperties.getChild(ItemPropertyAssociation.FCC, null);
                for (ItemPropertyAssociation.Entry en : itemPropertiesAssociations.entries) {
                    if (en.itemId == entry.itemId) {
                        for (int i : en.propertyIndex) {
                            properties.add(allProperties.get(i - 1)); //starts at 1
                        }
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
        return properties;
    }

    /**
     * Override property boxes.
     *
     * @param boxes not null
     */
    synchronized void setProperties(List<Box> boxes) {
        this.properties = boxes;
    }

    public synchronized List<SingleItemTypeReference> getReferences() throws DataStoreException {
        if (references != null) return references;

        //extract outter references
        references = new ArrayList<>();
        try {
            final Box meta = store.getRootBox().getChild(Meta.FCC, null);
            final ItemReference itemReferences = (ItemReference) meta.getChild(ItemReference.FCC, null);
            if (itemReferences != null) {
                for (SingleItemTypeReference sitr : itemReferences.references) {
                    if (sitr.fromItemId == entry.itemId) {
                        references.add(sitr);
                    }
                }
            }
        } catch (IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }

        return references;
    }

    synchronized void setReferences(List<SingleItemTypeReference> refs) {
        this.references = refs;
    }

    public synchronized ItemLocation.Item getLocation() throws DataStoreException {
        if (location != null) return location == NO_LOCATION ? null : location;

        //extract location
        location = NO_LOCATION;
        try {
            final Box meta = store.getRootBox().getChild(Meta.FCC, null);
            final ItemLocation itemLocation = (ItemLocation) meta.getChild(ItemLocation.FCC, null);
            if (itemLocation != null) {
                for (ItemLocation.Item en : itemLocation.items) {
                    if (en.itemId == entry.itemId) {
                        location = en;
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
        return location == NO_LOCATION ? null : location;
    }

    /**
     * @param offset start position to read from
     * @param count number of bytes to read, -1 for all
     * @param target Optional array to write into
     * @param targetOffset if target is provided, offset to start writing at
     * @return new or provided array
     * @throws DataStoreException
     */
    public byte[] getData(long dataOffset, int count, byte[] target, int targetOffset) throws DataStoreException {
        try {
            final ItemLocation.Item location = getLocation();
            if (location == null) {
                //read data from the default mediadata box
                MediaData mediaData = (MediaData) store.getRootBox().getChild(MediaData.FCC, null);
                return mediaData.getData(dataOffset, count, target, targetOffset);
            } else if (location.constructionMethod == 0) {
                //absolute location
                if (location.dataReferenceIndex == 0) {
                    //compute total size
                    final int length = IntStream.of(location.extentLength).sum();
                    //read datas
                    if (count == -1) count = length;
                    if (target == null) targetOffset = 0; //ignore user value if array is null
                    final byte[] data = target == null ? new byte[count] : target;
                    final Reader reader = store.getReader();
                    synchronized (reader) {
                        long remaining = count;
                        int bufferOffset = 0;
                        for (int i = 0, currentOffset = 0; i < location.extentLength.length && remaining > 0; i++) {
                            if (dataOffset <= currentOffset) {
                                reader.channel.seek(location.baseOffset + location.extentOffset[i]);
                                final long toRead = Math.min(remaining, location.extentLength[i]);
                                reader.channel.readFully(data, bufferOffset + targetOffset, Math.toIntExact(toRead));
                                bufferOffset += toRead;
                                remaining -= toRead;
                            } else if (dataOffset >= (currentOffset + location.extentLength[i])) {
                                //skip the full block
                            } else if (dataOffset > currentOffset) {
                                long toSkip = dataOffset - currentOffset;
                                reader.channel.seek(location.baseOffset + location.extentOffset[i] + toSkip);
                                final long toRead = Math.min(remaining, location.extentLength[i] - toSkip);
                                reader.channel.readFully(data, bufferOffset + targetOffset, Math.toIntExact(toRead));
                                bufferOffset += toRead;
                                remaining -= toRead;
                            }
                            currentOffset += location.extentLength[i];
                        }
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
