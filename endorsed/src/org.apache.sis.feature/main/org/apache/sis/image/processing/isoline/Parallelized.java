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
package org.apache.sis.image.processing.isoline;

import java.awt.image.RenderedImage;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.processing.TiledProcess;


/**
 * Wraps {@code Isolines.generate(…)} calculation in a process for parallel execution.
 * The source image is divided in sub-region and the isolines in each sub-region will
 * be computed in a different thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Parallelized extends TiledProcess<Isolines[]> {
    /**
     * Values for which to compute isolines. An array should be provided for each band.
     * If there is more bands than {@code levels.length}, the last array is reused for
     * all remaining bands.
     *
     * @see #cloneAndSort(double[][])
     */
    private final double[][] levels;

    /**
     * Transform from image upper left corner (in pixel coordinates) to geometry coordinates.
     */
    private final MathTransform gridToCRS;

    /**
     * Creates a process for parallel isoline computation.
     *
     * @param  data       image providing source values.
     * @param  levels     values for which to compute isolines.
     * @param  gridToCRS  transform from pixel coordinates to geometry coordinates, or {@code null} if none.
     */
    Parallelized(final RenderedImage data, final double[][] levels, final MathTransform gridToCRS) {
        super(data, 1, 1, Isolines.iterators());
        this.levels = levels;
        this.gridToCRS = gridToCRS;
    }

    /**
     * Invoked by {@link TiledProcess} for creating a sub-task
     * doing isoline computation on a sub-region of the image.
     */
    @Override
    protected Task createSubTask() {
        return new Tile();
    }

    /**
     * A sub-task doing isoline computation on a sub-region of the image.
     * The region is determined by the {@link #iterator}.
     */
    private final class Tile extends Task {
        /**
         * Isolines computed in the sub-region of this sub-task.
         */
        private Isolines[] isolines;

        /**
         * Creates a new sub-task.
         */
        Tile() {
        }

        /**
         * Invoked in a background thread for performing isoline computation.
         */
        @Override
        protected void execute() throws TransformException {
            isolines = Isolines.generate(iterator, levels, gridToCRS);
        }

        /**
         * Invoked in a background thread for merging results of two sub-tasks.
         */
        @Override
        protected void merge(final Task neighbor) throws TransformException {
            Isolines.merge(isolines, ((Tile) neighbor).isolines);
        }

        /**
         * Invoked on the last sub-task (after all merges) for getting final result.
         */
        @Override
        protected Isolines[] result() throws TransformException {
            return Isolines.flush(isolines);
        }
    }
}
