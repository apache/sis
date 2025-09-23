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
package org.apache.sis.image;

import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import org.apache.sis.util.Disposable;
import org.apache.sis.system.CommonExecutor;
import org.apache.sis.image.internal.shared.ImageUtilities;


/**
 * A helper class for forwarding a {@code prefetch(…)} operation to multiple sources.
 * This implementation assumes that all sources share the same pixel coordinates space.
 * However the tile matrix does not need to be the same.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MultiSourcePrefetch implements Disposable {
    /**
     * A filtered list of images on which to prefetch at least one tile.
     * This array contains only {@link PlanarImage} instances which intersect the area of interest.
     */
    private final PlanarImage[] sources;

    /**
     * Indices of tiles to prefetch for each image in the {@link #sources} array.
     */
    private final Rectangle[] tileIndices;

    /**
     * Number of valid elements in {@link #sources} and {@link #tileIndices} arrays.
     * Note that it will not be worth to use parallelism if the count is less than two.
     */
    private int count;

    /**
     * Handlers to invoke for releasing resources after the prefetch operation is completed.
     */
    private Disposable[] cleaners;

    /**
     * Number of valid elements in the {@link #cleaners} array.
     */
    private int cleanerCount;

    /**
     * If an error occurred while invoking {@code prefetch(…)} or {@code dispose()} on a source, that error.
     * An error during disposal will not prevent other handlers to be disposed as well. If errors also occur
     * during the disposal of other handlers, the other exceptions are added as suppressed exceptions.
     */
    private RuntimeException error;

    /**
     * Prepares (without launching) a prefetch operation using the given source images.
     *
     * @param  images  sources on which to apply a prefetch operation.
     * @param  aoi     pixel coordinates of the region to prefetch.
     */
    MultiSourcePrefetch(final RenderedImage[] images, final Rectangle aoi) {
        sources = new PlanarImage[images.length];
        tileIndices = new Rectangle[images.length];
        for (final RenderedImage source : images) {
            if (source instanceof PlanarImage) {
                Rectangle r = new Rectangle(aoi);
                ImageUtilities.clipBounds(source, r);
                r = ImageUtilities.pixelsToTiles(source, r);
                if (!r.isEmpty()) {
                    tileIndices[count] = r;
                    sources[count++] = (PlanarImage) source;
                }
            }
        }
    }

    /**
     * Forwards the prefetchs calls to source images.
     *
     * <h4>Implementation note</h4>
     * In many cases the background threads are not really necessary because {@code prefetch(…)} will
     * only forward to another {@code prefetch(…)} until we reach a final {@code prefetch(…)} which
     * happen to be a no-op. But it some cases, that final {@code prefetch(…)} will read tiles from
     * a TIFF or netCDF file (for example).
     *
     * @param  parallel  whether parallelism is allowed.
     * @return a handler for disposing resources after prefetch, or {@code null} if none.
     */
    final Disposable run(boolean parallel) {
        switch (count) {
            case 0: return null;
            case 1: parallel = false;
        }
        @SuppressWarnings({"unchecked","rawtypes"})
        final var workers = (Future<Disposable>[]) (parallel ? new Future[count] : null);
        cleaners = new Disposable[count];
        for (int i=0; i<count; i++) {
            final PlanarImage source = sources[i];
            final Rectangle r = tileIndices[i];
            if (parallel) {
                Callable<Disposable> worker = () -> source.prefetch(r);
                workers[i] = CommonExecutor.instance().submit(worker);
            } else {
                final Disposable cleaner = source.prefetch(r);
                if (cleaner != null) {
                    cleaners[cleanerCount++] = cleaner;
                }
            }
        }
        /*
         * Block until all background threads finished their work. This is needed because `PrefetchedImage`
         * will start to query tiles after this method returned, so the source images need to be ready.
         */
        if (parallel) {
            for (final Future<Disposable> worker : workers) try {
                final Disposable cleaner = worker.get();
                if (cleaner != null) {
                    cleaners[cleanerCount++] = cleaner;
                }
            } catch (Exception e) {
                addError(e);
                dispose();      // Will rethrow the exception after disposal.
            }
        }
        switch (cleanerCount) {
            case 0:  return null;
            case 1:  return cleaners[0];
            default: return this;
        }
    }

    /**
     * Disposes the handlers of all sources.
     */
    @Override
    public void dispose() {
        for (int i=0; i<cleanerCount; i++) try {
            cleaners[i].dispose();
        } catch (Exception e) {
            addError(e);
        }
        if (error != null) {
            throw error;
        }
    }

    /**
     * Declares that an exception occurred. The exception will be thrown after all handlers have been disposed.
     * If more than one exception occurs, all additional errors are added as suppressed exceptions.
     */
    private void addError(final Exception e) {
        if (error != null) {
            error.addSuppressed(e);
        } else if (e instanceof RuntimeException) {
            error = (RuntimeException) e;
        } else {
            error = (ImagingOpException) new ImagingOpException(e.getMessage()).initCause(e);
        }
    }
}
