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
package org.apache.sis.storage.hdf.internal;

import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.base.ObjectHeader;
import org.apache.sis.storage.hdf.io.Connector;

/**
 * A dataset associated with a dimension of another dataset.
 * Not yet fully implemented: it currently behaves as a plain dataset.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_h5_d_s__u_g.html#subsec_dim_scales_spec_store">HDF5 Dimension Scale Specification - Storage</a>
 * @author Johann Sorel (Geomatys)
 */
public class DimensionScaleNode extends DatasetNode {

    /**
     * Create a dimension scale from the address of its object header.
     *
     * @param parent parent group, not {@code null}
     * @param connector file connector, not {@code null}
     * @param objectHeaderAddress address of this dataset object header
     * @param name name of this dataset within its parent
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the object header is invalid or unsupported.
     */
    public DimensionScaleNode(GroupNode parent, Connector connector, long objectHeaderAddress, String name) throws IOException, DataStoreException {
        super(parent, connector, objectHeaderAddress, name);
    }

    DimensionScaleNode(GroupNode parent, Connector connector, long objectHeaderAddress, String name, ObjectHeader header) throws IOException, DataStoreException {
        super(parent, connector, objectHeaderAddress, name);
    }
}
