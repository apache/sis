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
package org.apache.sis.storage.hdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.URIDataStoreOption;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.iso.Names;
import org.apache.sis.storage.hdf.internal.Constants;
import org.apache.sis.storage.hdf.internal.DatasetNode;
import org.apache.sis.storage.hdf.internal.GroupNode;
import org.apache.sis.storage.hdf.internal.NativeMetadata;
import org.apache.sis.storage.hdf.internal.Nodes;
import org.apache.sis.storage.hdf.convention.CFCoverageResource;
import org.apache.sis.storage.hdf.convention.DatasetAsFeatureSet;
import org.apache.sis.storage.hdf.convention.GroupAsAggregate;
import org.apache.sis.storage.hdf.io.Connector;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 * Data store for <abbr>HDF5</abbr> files. The content is available in three ways:
 *
 * <ul>
 *   <li>{@link #components()} gives the content interpreted as standard resources: groups following the
 *       <abbr>CF</abbr> conventions are exposed as grid coverages, other groups as aggregates and datasets
 *       as feature sets.</li>
 *   <li>{@link #getRootGroup()} gives the raw tree of {@linkplain Group groups} and {@linkplain Dataset datasets},
 *       with their attributes and values.</li>
 *   <li>{@link #getNativeMetadata()} gives all structures of the file (datatypes, layouts, filters, <i>etc.</i>)
 *       as a tree table, for information or debugging purposes.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class HDF5Store extends DataStore implements Aggregate {

    private final Connector cnx;
    private final GroupNode root;
    private final Resource decorate;
    private final String name;

    /**
     * Opens the HDF-5 file at the given path and reads its root group.
     *
     * @param provider provider creating this store.
     * @param path path to the HDF-5 file.
     * @throws IllegalArgumentException if the store can not be created for the given path.
     * @throws DataStoreException if the file is invalid or unsupported, or its root object is not a group.
     * @throws IOException if an I/O error occurred.
     */
    public HDF5Store(HDF5Provider provider, Path path) throws IllegalArgumentException, DataStoreException, IOException {
        super(provider, new StorageConnector(path));
        cnx = new Connector(path);
        /*
        Superblock versions 0 and 1 point at the root group through a symbol table
        entry, versions 2 and 3 through an object header address. Both resolve to
        an object header address.
        */
        name = IOUtilities.filenameWithoutExtension(path.toUri().toString());
        final long rootAddress = cnx.getSuperblock().resolveRootObjectHeaderAddress();
        final Node rootNode = Nodes.create(null, cnx, rootAddress, name);
        if (!(rootNode instanceof GroupNode g)) {
            throw new DataStoreException("Root object of " + path + " is not a group.");
        }
        root = g;
        decorate = decorate(root);
    }

    /**
     * Returns the parameters used for opening this data store.
     *
     * @return parameters used for opening this data store.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.ofNullable(URIDataStoreOption.createWithLocationOnly(provider, cnx.getPath().toUri()));
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return new DefaultMetadata();
    }

    @Override
    public void close() throws DataStoreException {
        cnx.close();
    }

    /**
     * Returns the root group of the <abbr>HDF5</abbr> file.
     * This is the raw tree of groups and datasets, without interpretation of conventions.
     *
     * @return the root group, never {@code null}.
     */
    public Group getRootGroup() {
        return root;
    }

    /**
     * Returns all the structures of the <abbr>HDF5</abbr> file as a tree: superblock, then the tree of groups
     * and datasets with their attributes, datatypes, dataspaces, layouts, filters and fill values.
     * The {@linkplain #getRootGroup() root group} and the {@linkplain #components() components}
     * should be preferred for accessing the data; native metadata are for information or debugging purposes.
     *
     * @return the structures of the <abbr>HDF5</abbr> file as a tree.
     * @throws DataStoreException if an error occurred while building the tree.
     */
    @Override
    public Optional<TreeTable> getNativeMetadata() throws DataStoreException {
        return Optional.of(NativeMetadata.create(cnx.getSuperblock(), root));
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(Names.createLocalName(null, null, name));
    }

    @Override
    public Collection<? extends Resource> components() throws DataStoreException {
        return Collections.singletonList(decorate);
    }

    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        return Optional.of(new FileSet(cnx.getPath()));
    }

    @Override
    public String toString() {
        return getRootGroup().toString();
    }

    /**
     * Decorate HDF nodes to Resources.
     *
     * @param node group or dataset.
     * @return resource exposing the node.
     */
    private Resource decorate(Node node) {
        if (node instanceof GroupNode group) {
            final String conventions = String.valueOf(group.getAttributes().get("Conventions"));
            if (conventions.startsWith("CF")) {
                try {
                    return new CFCoverageResource(this, group);
                } catch (DataStoreException | FactoryException ex) {
                    Constants.LOGGER.log(Level.WARNING, "Failed to intepret " + group.getName() + " as a CF grid coverage.\n" + ex.getMessage(), ex);
                }
            }
            final GroupAsAggregate gaa = new GroupAsAggregate(this, group);

            //search for siblings
            final List<DatasetAsFeatureSet> siblings = findSiblings(group);
            final List<Node> remaining = new ArrayList<>(group.components());
            for (DatasetAsFeatureSet set : siblings) {
                gaa.resources().add(set);
                remaining.removeAll(List.of(set.getDatasets()));
            }

            //create single dataset for the remaining
            for (Node n : remaining) {
                gaa.resources().add(decorate(n));
            }
            return gaa;
        } else if (node instanceof DatasetNode ds) {
            final String baseName = createName(ds);
            return new DatasetAsFeatureSet(this, Names.createLocalName(null, null, baseName), ds);
        } else {
            throw new IllegalArgumentException("Unexpected node type " + node);
        }
    }

    private List<DatasetAsFeatureSet> findSiblings(GroupNode group) {

        final String baseName = createName(group);

        //group by coordinates
        final Map<String,List<DatasetNode>> candidates = new LinkedHashMap();
        for (Node n : group.components()) {
            if (n instanceof DatasetNode dataset) {
                final String coordinates = (String) dataset.getAttributes().getOrDefault("coordinates", null);
                if (coordinates != null) {
                    List<DatasetNode> lst = candidates.get(coordinates);
                    if (lst == null) {
                        lst = new ArrayList<>();
                        candidates.put(coordinates, lst);
                    }
                    lst.add(dataset);
                }
            }
        }

        //remove lists with a single value
        Iterator<Entry<String, List<DatasetNode>>> ite = candidates.entrySet().iterator();
        while (ite.hasNext()) {
            Entry<String, List<DatasetNode>> entry = ite.next();
            if (entry.getValue().size() == 1) {
                ite.remove();
            }
        }

        //build resource
        int inc = 0;
        final List<DatasetAsFeatureSet> result = new ArrayList<>();
        for (Entry<String,List<DatasetNode>> candidate : candidates.entrySet()) {
            final GenericName name = Names.createLocalName(null, null, baseName+"_"+inc++);
            final List<DatasetNode> lst = candidate.getValue();
            final DatasetAsFeatureSet ds = new DatasetAsFeatureSet(this, name, lst.toArray(DatasetNode[]::new));
            result.add(ds);
        }

        return result;
    }

    private String createName(Node node) {
        final StringBuilder sb = new StringBuilder();
        sb.append(node.getName());
        Node p = node;
        while (p.getParent() != null) {
            p = p.getParent();
            sb.insert(0, p.getName()+".");
        }

        return sb.toString();
    }
}
