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
package org.apache.sis.storage.netcdf;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Path;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Grid;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.storage.AbstractGridResource;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;


/**
 * A grid coverage in a netCDF file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class GridResource extends AbstractGridResource implements ResourceOnFileSystem {
    /**
     * The identifier of this grid resource.
     * This is the variable name.
     */
    private final GenericName identifier;

    /**
     * The grid geometry (size, CRS…) of the {@linkplain #data} cube.
     */
    private final GridGeometry gridGeometry;

    /**
     * The netCDF variable wrapped by this resource.
     */
    private final Variable data;

    /**
     * Path to the netCDF file for information purpose, or {@code null} if unknown.
     */
    private final Path location;

    /**
     * Creates a new resource.
     *
     * @param  decoder  the implementation used for decoding the netCDF file.
     * @param  grid     the grid geometry (size, CRS…) of the {@linkplain #data} cube.
     * @param  data     the variable providing actual data.
     */
    private GridResource(final Decoder decoder, final Grid grid, final Variable data) throws IOException, DataStoreException {
        super(decoder.listeners);
        this.data    = data;
        gridGeometry = grid.getGridGeometry(decoder);
        identifier   = decoder.nameFactory.createLocalName(decoder.namespace, data.getName());
        location     = decoder.location;
    }

    /**
     * Returns a list of all grid resources found in the netCDF file opened by the given decoder.
     * This method should be invoked only once and the result cached. The returned list is modifiable;
     * caller is free to add other elements.
     */
    static List<Resource> list(final Decoder decoder) throws IOException, DataStoreException {
        final List<Resource> resources = new ArrayList<>();
        for (final Variable variable : decoder.getVariables()) {
            final Grid grid = variable.getGridGeometry(decoder);
            if (grid != null) {
                resources.add(new GridResource(decoder, grid, variable));
            }
        }
        return resources;
    }

    /**
     * Returns the variable name as an identifier of this resource.
     */
    @Override
    public GenericName getIdentifier() {
        return identifier;
    }

    /**
     * Returns an object containing the grid size, the CRS and the conversion from grid indices to CRS coordinates.
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return gridGeometry;
    }

    /**
     * Gets the paths to files used by this resource, or an empty array if unknown.
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        return (location != null) ? new Path[] {location} : new Path[0];
    }
}
