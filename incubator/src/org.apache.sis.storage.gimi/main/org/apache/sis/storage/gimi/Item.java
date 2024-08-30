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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.ISOBMFFReader;
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

/**
 * Regroup properties of a single item in the file.
 *
 * @author Johann Sorel (Geomatys)
 */
final class Item {

    public final GimiStore store;
    public final ItemInfoEntry entry;
    public final boolean isPrimary;
    public final List<Box> properties = new ArrayList<>();
    public final List<SingleItemTypeReference> references = new ArrayList<>();
    public final ItemLocation.Item location;

    public Item(GimiStore store, ItemInfoEntry entry) throws IllegalArgumentException, DataStoreException, IOException {
        this.store = store;
        this.entry = entry;
        final Box meta = store.getRootBox().getChild(Meta.FCC, null);
        //is item primary
        final PrimaryItem primaryItem = (PrimaryItem) meta.getChild(PrimaryItem.FCC, null);
        if (primaryItem != null) {
            isPrimary = primaryItem.itemId == entry.itemId;
        } else {
            isPrimary = true;
        }
        //extract properties
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
        //extract outter references
        final ItemReference itemReferences = (ItemReference) meta.getChild(ItemReference.FCC, null);
        if (itemReferences != null) {
            for (SingleItemTypeReference sitr : itemReferences.references) {
                if (sitr.fromItemId == entry.itemId) {
                    references.add(sitr);
                }
            }
        }
        //extract location
        ItemLocation.Item loc = null;
        final ItemLocation itemLocation = (ItemLocation) meta.getChild(ItemLocation.FCC, null);
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
            if (location == null) {
                //read data from the default mediadata box
                MediaData mediaData = (MediaData) store.getRootBox().getChild(MediaData.FCC, null);
                return mediaData.getData();
            } else if (location.constructionMethod == 0) {
                //absolute location
                if (location.dataReferenceIndex == 0) {
                    //compute total size
                    final int length = IntStream.of(location.extentLength).sum();
                    //read datas
                    final byte[] data = new byte[length];
                    final ISOBMFFReader reader = store.getReader();
                    synchronized (reader) {
                        for (int i = 0, offset = 0; i < location.extentLength.length; i++) {
                            reader.channel.seek(location.baseOffset + location.extentOffset[i]);
                            reader.channel.readFully(data, offset, location.extentLength[i]);
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
