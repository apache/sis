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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.ISOBMFFReader;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.GroupList;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemInfo;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemInfoEntry;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.Meta;
import org.apache.sis.storage.gimi.isobmff.iso23008_12.ImagePyramidEntityGroup;
import org.apache.sis.util.iso.Names;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GimiStore extends DataStore implements Aggregate {

    private final Path gimiPath;

    private List<Resource> components;
    private Map<Integer,Resource> componentIndex;
    private Metadata metadata;

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
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final MetadataBuilder builder = new MetadataBuilder();
            builder.addIdentifier(Names.createLocalName(null, null, IOUtilities.filenameWithoutExtension(gimiPath.getFileName().toString())), MetadataBuilder.Scope.ALL);
            metadata = builder.buildAndFreeze();
        }
        return metadata;
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

    synchronized ISOBMFFReader getReader() throws DataStoreException {
        if (reader == null) {
            final StorageConnector cnx = new StorageConnector(gimiPath);
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
        componentIndex = new HashMap<>();
        try {

            final Box root = getRootBox();
            final Meta meta = (Meta) root.getChild(Meta.FCC, null);

            //single items
            final ItemInfo iinf = (ItemInfo) meta.getChild(ItemInfo.FCC, null);
            for (ItemInfoEntry iie : iinf.entries) {
                final Item item = new Item(this, iie);
                Resource resource;
                if (ResourceImageUncompressed.TYPE.equals(iie.itemType)) {
                    //uncompressed image
                    resource = new ResourceImageUncompressed(this, item);
                } else if (ResourceImageJpeg.TYPE.equals(iie.itemType)) {
                    //jpeg image
                    resource = new ResourceImageJpeg(this, item);
                }  else if (ResourceGrid.TYPE.equals(iie.itemType)) {
                    //tiled image
                    resource = new ResourceGrid(item);
                } else {
                    //TODO
                    resource = new ResourceUnknown(this, item);
                }
                components.add(resource);
                componentIndex.put(iie.itemId, resource);
            }

            //groups
            final Box groups = meta.getChild(GroupList.FCC, null);
            if (groups != null) {
                for (Box b : groups.getChildren()) {
                    if (b instanceof ImagePyramidEntityGroup) {
                        final ImagePyramidEntityGroup img = (ImagePyramidEntityGroup) b;
                        final ResourcePyramid pyramid = new ResourcePyramid(this, img);
                        components.add(pyramid);
                        componentIndex.put(pyramid.group.groupId, pyramid);

                        //force initialize now, pyramids may amend existing grids
                        pyramid.getGridGeometry();

                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return components;
    }

    /**
     * Get file item as Resource.
     * @param itemId item identifier
     * @return resource or null if not found
     */
    Resource getComponent(int itemId) throws DataStoreException {
        components();
        return componentIndex.get(itemId);
    }

}
