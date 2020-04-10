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
package org.apache.sis.gui.coverage;

import java.awt.image.RenderedImage;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.internal.gui.ExceptionReporter;


/**
 * A task for loading {@link GridCoverage} from a resource in a background thread, then fetching an image from it.
 * Callers needs to define a task to execute on success with {@link #setOnSucceeded(EventHandler)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ImageLoader extends Task<RenderedImage> {
    /**
     * The {@value} value, for identifying code that assume two-dimensional objects.
     */
    public static final int BIDIMENSIONAL = 2;

    /**
     * The image source together with optional parameters for reading only a subset.
     */
    final ImageRequest request;

    /**
     * The coverage explorer to inform when {@link ImageRequest#coverage} become available, or {@code null} if none.
     * We do not provide a more generic listeners API for now, but it could be done in the future if there is a need.
     */
    private CoverageExplorer listener;

    /**
     * Whether the caller wants a grid coverage that contains real values or sample values.
     */
    private final boolean converted;

    /**
     * Creates a new task for loading an image from the specified resource.
     *
     * @param  request    source of the image to load.
     * @param  converted  {@code true} for a coverage containing converted values,
     *                    or {@code false} for a coverage containing packed values.
     */
    ImageLoader(final ImageRequest request, final boolean converted) {
        this.request     = request;
        this.converted   = converted;
        this.listener    = request.listener;
        request.listener = null;
    }

    /**
     * Computes a two dimension slice of the given grid geometry.
     * This method select the two first dimension having a size greater than 1 cell.
     *
     * @param  domain  the grid geometry in which to choose a two-dimensional slice.
     * @return a builder configured for returning the desired two-dimensional slice.
     */
    private static GridDerivation slice(final GridGeometry domain) {
        final GridExtent extent = domain.getExtent();
        final int dimension = extent.getDimension();
        final int[] sliceDimensions = new int[BIDIMENSIONAL];
        int k = 0;
        for (int i=0; i<dimension; i++) {
            if (extent.getLow(i) != extent.getHigh(i)) {
                sliceDimensions[k] = i;
                if (++k >= BIDIMENSIONAL) break;
            }
        }
        return domain.derive().sliceByRatio(ImageRequest.SLICE_RATIO, sliceDimensions);
    }

    /**
     * Loads the image. Current implementation reads the full image. If the coverage has more than 2 dimensions,
     * only two of them are taken for the image; for all other dimensions, only the values at lowest index will
     * be read.
     *
     * @return the image loaded from the source given at construction time.
     * @throws DataStoreException if an error occurred while loading the grid coverage.
     */
    @Override
    protected RenderedImage call() throws DataStoreException {
        GridCoverage cv = request.coverage;
        if (cv == null) {
            GridGeometry domain = request.getDomain().orElse(null);
            final int[]  range  = request.getRange() .orElse(null);
            if (request.getOverviewSize().isPresent()) {
                if (domain == null) {
                    domain = request.resource.getGridGeometry();
                }
                if (domain != null && domain.getDimension() > BIDIMENSIONAL) {
                    domain = slice(domain).build();
                }
                /*
                 * TODO: we should apply a subsampling here. GridDerivation has the API for that,
                 * what is missing is to transmit this information to GridView column and row headers.
                 * See ImageRequest.setOverviewSize(int).
                 */
            }
            cv = request.resource.read(domain, range);                      // May be long to execute.
            cv = cv.forConvertedValues(converted);
            request.coverage = cv;
            Platform.runLater(this::fireCoverageLoaded);
        }
        if (isCancelled()) {
            return null;
        }
        GridExtent sliceExtent = request.getSliceExtent().orElse(null);
        if (sliceExtent == null) {
            final GridGeometry domain = cv.getGridGeometry();
            if (domain != null && domain.getDimension() > BIDIMENSIONAL) {  // Should never be null but we are paranoiac.
                sliceExtent = slice(domain).getIntersection();
            }
        }
        return cv.render(sliceExtent);
    }

    /**
     * Invoked in JavaFX thread on failure.
     * Current implementation popups a dialog box for reporting the error.
     */
    @Override
    protected void failed() {
        fireFinished(null);
        final GridCoverageResource resource = request.resource;
        if (resource instanceof StoreListeners) {
            ExceptionReporter.canNotReadFile(((StoreListeners) resource).getSourceName(), getException());
        } else {
            ExceptionReporter.canNotUseResource(getException());
        }
    }

    /**
     * Invoked in JavaFX thread in case of cancellation.
     */
    @Override
    protected void cancelled() {
        fireFinished(null);
    }

    /**
     * Notifies listener that the given coverage has been read or failed to be read,
     * then discards the listener. This method shall be invoked in JavaFX thread.
     *
     * <p>This method is also invoked with a null argument for notifying listener
     * that the read operation failed (or has been cancelled).</p>
     *
     * @param  result  the result, or {@code null} on failure.
     */
    private void fireFinished(final GridCoverage result) {
        final CoverageExplorer snapshot = listener;
        if (snapshot != null) {
            listener = null;                               // Clear now in case an error happen.
            snapshot.onCoverageLoaded(result);
        }
    }

    /**
     * Notifies all listeners that the coverage has been read, then discards the listeners.
     * This method shall be invoked in JavaFX thread.
     */
    private void fireCoverageLoaded() {
        fireFinished(request.coverage);
    }
}
