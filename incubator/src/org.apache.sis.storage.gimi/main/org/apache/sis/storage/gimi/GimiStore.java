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
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.ISOBMFFReader;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.EntityToGroup;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.GroupList;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemInfo;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemInfoEntry;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.Meta;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.SingleItemTypeReference;
import org.apache.sis.storage.gimi.isobmff.iso23008_12.ImagePyramidEntityGroup;
import org.apache.sis.util.iso.Names;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GimiStore extends DataStore implements Aggregate {

    private final URI gimiUri;
    private final Path gimiPath;

    private List<Resource> components;
    private Map<Integer,Item> itemIndex;
    private Map<Integer,Resource> componentIndex;
    private Metadata metadata;

    //cache the reader
    private ISOBMFFReader reader;
    private Box root;

    public GimiStore(StorageConnector connector) throws DataStoreException {
        this.gimiUri = connector.getStorageAs(URI.class);
        this.gimiPath = connector.getStorageAs(Path.class);
        connector.closeAllExcept(null);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final Parameters parameters = Parameters.castOrWrap(GimiProvider.PARAMETERS_DESCRIPTOR.createValue());
        parameters.parameter(GimiProvider.LOCATION).setValue(gimiUri);
        return Optional.of(parameters);
    }

    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final MetadataBuilder builder = new MetadataBuilder();
            final String path = gimiPath == null ? gimiUri.toString() : gimiPath.getFileName().toString();
            builder.addIdentifier(Names.createLocalName(null, null, IOUtilities.filenameWithoutExtension(path)), MetadataBuilder.Scope.ALL);
            metadata = builder.buildAndFreeze();
        }
        return metadata;
    }

    synchronized ISOBMFFReader getReader() throws DataStoreException {
        if (reader == null) {
            final StorageConnector cnx = new StorageConnector(gimiPath == null ? gimiUri : gimiPath);
            final ChannelDataInput cdi = cnx.getStorageAs(ChannelDataInput.class);
            reader = new ISOBMFFReader(cdi);
        }
        return reader;
    }

    /**
     * Get virtual root box.
     * This box do not really exist in the file, use getChildren to acces file boxes.
     */
    synchronized Box getRootBox() throws DataStoreException {
        if (root == null) {
            root = new Box() {
                @Override
                public boolean isContainer() {
                    return true;
                }
            };
            root.setLoader(getReader());
        }
        return root;
    }

    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (components != null) return components;

        components = new ArrayList<>();
        itemIndex = new HashMap<>();
        componentIndex = new HashMap<>();

        /*
         * Collect elements which should not be displayed since they are part of a larger image (grid or pyramid)
         */
        final Set<Integer> includedInParents = new HashSet<>();

        try {
            final Box root = getRootBox();
            final Meta meta = (Meta) root.getChild(Meta.FCC, null);
            final Box groups = meta.getChild(GroupList.FCC, null);
            final ItemInfo iinf = (ItemInfo) meta.getChild(ItemInfo.FCC, null);

            /*
             * Build item index.
             * Create GridImages and collect used items.
             */
            for (ItemInfoEntry iie : iinf.entries) {
                final Item item = new Item(this, iie);
                itemIndex.put(iie.itemId, item);

                if (ResourceGrid.TYPE.equals(iie.itemType)) {
                    for (SingleItemTypeReference refs : item.getReferences()) {
                        final ResourceGrid resource = new ResourceGrid(item);
                        componentIndex.put(iie.itemId, resource);
                        for (int i :refs.toItemId) {
                            includedInParents.add(i);
                        }
                    }
                }
            }

            /*
             * Read groups.
             * Create pyramids and groups and collect used items.
             */
            if (groups != null) {
                for (Box b : groups.getChildren()) {
                    if (b instanceof ImagePyramidEntityGroup) {
                        final ImagePyramidEntityGroup img = (ImagePyramidEntityGroup) b;
                        final ResourcePyramid pyramid = new ResourcePyramid(this, img);
                        components.add(pyramid);

                        for (int i :img.entitiesId) {
                            includedInParents.add(i);
                        }
                    } else if (b instanceof EntityToGroup) {
                        final EntityToGroup grp = (EntityToGroup) b;
                        final Group group = new Group(this, grp);
                        components.add(group);

                        for (int i :grp.entitiesId) {
                            includedInParents.add(i);
                        }
                    }
                }
            }

            /*
             * Add all items not used by groups and grids.
             */
            final Set<Integer> keys = new HashSet<>(itemIndex.keySet());
            keys.removeAll(includedInParents);
            for (Integer k : keys) {
                components.add(getComponent(k));
            }

        } catch (IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }

        return components;
    }

    /**
     * Get file item as Resource.
     *
     * @param itemId item identifier
     * @return resource current or created resource
     */
    synchronized Resource getComponent(int itemId) throws DataStoreException {
        components();
        Resource resource = componentIndex.get(itemId);
        if (resource == null) {
            final Item item = itemIndex.get(itemId);
            if (item == null) {
                throw new IllegalNameException("No item for id : " + itemId);
            }

            if (ResourceImageUncompressed.TYPE.equals(item.entry.itemType)) {
                //uncompressed image
                resource = new ResourceImageUncompressed(this, item);
            } else if (ResourceImageJpeg.TYPE.equals(item.entry.itemType)) {
                //jpeg image
                resource = new ResourceImageJpeg(this, item);
            }  else if (ResourceGrid.TYPE.equals(item.entry.itemType)) {
                //tiled image
                resource = new ResourceGrid(item);
            } else {
                //TODO
                resource = new ResourceUnknown(this, item);
            }
        }
        componentIndex.put(itemId, resource);
        return resource;
    }

    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        if (gimiPath != null) {
            return Optional.of(new FileSet(gimiPath));
        }
        return Optional.empty();
    }

    /**
     * Release internal reader.
     * @throws DataStoreException if closing operation fails
     */
    @Override
    public synchronized void close() throws DataStoreException {
        try {
            reader.channel.channel.close();
            reader = null;
        } catch (IOException ex) {
            throw new DataStoreException("Fail to closed channel", ex);
        }
    }
}
