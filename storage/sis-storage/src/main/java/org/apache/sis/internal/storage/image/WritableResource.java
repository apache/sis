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
package org.apache.sis.internal.storage.image;

import java.io.IOException;
import java.awt.image.RenderedImage;
import javax.imageio.ImageWriter;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.WritableGridCoverageResource;
import org.apache.sis.internal.storage.WritableResourceSupport;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.storage.event.StoreListeners;


/**
 * An image which can be replaced or updated.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class WritableResource extends WorldFileResource implements WritableGridCoverageResource {
    /**
     * Creates a new resource.
     */
    WritableResource(final WritableStore store, final StoreListeners parent, final int imageIndex,
                     final GridGeometry gridGeometry) throws DataStoreException
    {
        super(store, parent, imageIndex, gridGeometry);
    }

    /**
     * Writes a new coverage in the data store for this resource. If a coverage already exists for this resource,
     * then it will be overwritten only if the {@code TRUNCATE} or {@code UPDATE} option is specified.
     *
     * @param  coverage  new data to write in the data store for this resource.
     * @param  options   configuration of the write operation.
     * @throws DataStoreException if an error occurred while writing data in the underlying data store.
     */
    @Override
    public void write(GridCoverage coverage, final Option... options) throws DataStoreException {
        final WritableResourceSupport h = new WritableResourceSupport(this, options);   // Does argument validation.
        final WritableStore store = (WritableStore) store();
        try {
            synchronized (store) {
                if (getImageIndex() != WorldFileStore.MAIN_IMAGE || (store.isMultiImages() != 0 && !h.replace(null))) {
                    // TODO: we should use `ImageWriter.replacePixels(…)` methods instead.
                    coverage = h.update(coverage);
                }
                final RenderedImage data = coverage.render(null);                       // Fail if not two-dimensional.
                store.setGridGeometry(getImageIndex(), coverage.getGridGeometry());     // May use the image reader.
                setGridCoverage(coverage);
                final ImageWriter writer = store.writer();                      // Should be after `setGridGeometry(…)`.
                writer.write(data);
            }
        } catch (IOException | RuntimeException e) {
            throw new DataStoreException(store.resources().getString(Resources.Keys.CanNotWriteResource_1, store.getDisplayName()), e);
        }
    }
}
