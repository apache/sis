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
package org.apache.sis.internal.processing.image;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.sis.internal.system.CommonExecutor;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.jdk9.JDK9;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.util.ArgumentChecks;


/**
 * Calculation in a two-dimensional space that can be subdivided in smaller calculations in sub-regions.
 * This class manages a kind of fork-join task but differs from {@link java.util.concurrent.ForkJoinPool}
 * in the following aspects:
 *
 * <ul class="verbose">
 *   <li>The fork and join processes are presumed relatively costly (i.e. they may need to reconstruct
 *       geometries splitted in two consecutive tiles). So instead of having many medium tasks waiting
 *       for a thread to take them, it may be more efficient to have fewer tasks processing larger areas.
 *       {@code TiledProcess} tries to create a number of sub-tasks close to the number of processors.
 *       This is a different approach than "work stealing" algorithm applied by JDK {@code ForkJoinPool},
 *       which is designed for smaller (and more easily separable) non-blocking tasks.</li>
 *   <li>The main task is splitted in sub-tasks with a single fork step, with two division factors along
 *       <var>x</var> and <var>y</var> axes which can be any integer (not necessarily powers of 2). This
 *       is a different approach than JDK {@code ForkJoinPool} where tasks are forked recursively in two
 *       sub-tasks at each step.</li>
 *   <li>The join operation tries to operate on tiles that are neighbors in the two dimensional space.
 *       It allows the join operation to merge geometries that are splited between two tiles.</li>
 *   <li>Tasks may block on I/O operations. We want to avoid blocking the JDK common fork/join pool,
 *       so we use a separated pool.</li>
 * </ul>
 *
 * The tiling applied by {@code TiledProcess} is independent of {@link RenderedImage} tiling.
 * This class assumes that the objects to be calculated are geometries or other non-raster data.
 * Consequently tile size will be determined by other considerations such as the number of processors.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <R>  the type of value computed as a result of this process.
 *
 * @since 1.1
 * @module
 */
abstract class TiledProcess<R> {
    /**
     * Minimal "tile" size for sub-tasks computation. That size should not be too small because the
     * fork/join processes have some extra cost compared to processing the whole image as one single tile.
     */
    private static final int MIN_TILE_SIZE = 1000;

    /**
     * Number of threads that are running. This is used for knowing when a thread is the last one.
     */
    private final AtomicInteger runningThreads;

    /**
     * All tasks executed in parallel threads. The array length should be less than the number of processors.
     * All read and write accesses to array elements must be done inside a {@code synchronized(tasks)} block.
     *
     * <p>This array initially contains only {@code null} elements. Non-null elements are assigned after
     * {@link Task#execute()} completion but before {@link Task#merge(Task)}.</p>
     */
    private final Task[] tasks;

    /**
     * Number of tiles (or tasks) on the <var>x</var> axis. Used for computing (x,y) coordinates of elements
     * in the {@link #tasks} array, considering that the array is a matrix encoded in row-major fashion.
     * This is the increment to apply on array index for incrementing the <var>y</var> coordinate by one.
     */
    private final int yStride;

    /**
     * Index of this {@code TiledProcess} instance in the {@link #tasks} array.
     * This is a temporary variable for {@link Task#index} initialization only.
     */
    private int taskIndex;

    /**
     * Iterator over the pixel for each element in the {@link #tasks} array.
     * This is a temporary variable for {@link Task#iterator} initialization only.
     */
    private PixelIterator[] iterators;

    /**
     * Prepares {@link TiledProcess} for execution of a task splitted in different regions.
     * This constructor splits the given image in sub-regions ("tiles" but not in the sense
     * of {@link RenderedImage} tiles), then creates a pixel iterator for each sub-region.
     * Iterators are created with the given {@link org.apache.sis.image.PixelIterator.Builder},
     * which should be configured by the caller in all desired aspects (e.g. iteration order) except the
     * {@linkplain org.apache.sis.image.PixelIterator.Builder#setRegionOfInterest(Rectangle) region of interest},
     * which will be overwritten by this method.
     *
     * <p>Usage example:</p>
     * {@preformat java
     *     TiledProcess process = new TiledProcess<MyResultType>(image, 0, 0,
     *             new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR))
     *     {
     *         &#64;Override
     *         protected Task createSubTask() {
     *             return new SubTask();
     *         }
     *
     *         private final class SubTask extends Task {
     *             private MyResultType result;
     *
     *             &#64;Override protected void execute() {result = ...}    // Do calculation in background thread.
     *             &#64;Override protected void merge(Task neighbor) {...}  // Merge this.result with neighbor.result().
     *             &#64;Override protected MyResultType result() {return result;}
     *         }
     *     };
     *     process.execute();
     * }
     *
     * @param  data             the image on which to iterate over the pixels.
     * @param  overlapX         the number of overlapping pixels between tiles on the <var>x</var> axis.
     * @param  overlapY         the number of overlapping pixels between tiles on the <var>y</var> axis.
     * @param  iteratorFactory  a pre-configured (except for sub-region aspect) supplier of pixel iterators.
     * @throws ArithmeticException if the image size is too large.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})                // Generic array creation.
    protected TiledProcess(final RenderedImage data, final int overlapX, final int overlapY,
                           final PixelIterator.Builder iteratorFactory)
    {
        ArgumentChecks.ensurePositive("overlapX", overlapX);
        ArgumentChecks.ensurePositive("overlapY", overlapY);
        /*
         * Get a first estimation of the number of tiles. If the total number is greater than `PARALLELISM`
         * (number of processors - 1), scale the number of tiles for having a total less than `PARALLELISM`.
         */
        final int width  = data.getWidth();
        final int height = data.getHeight();
        int  numTileX = Math.max(width  / MIN_TILE_SIZE, 1);
        int  numTileY = Math.max(height / MIN_TILE_SIZE, 1);
        long numTiles = JDK9.multiplyFull(numTileX, numTileY);
        if (numTiles > CommonExecutor.PARALLELISM) {
            final double r = Math.sqrt(CommonExecutor.PARALLELISM / (double) numTiles);     // Always < 1.
            if (numTileX >= numTileY) {
                numTileX = Math.max((int) Math.round(numTileX * r), 1);
                numTileY = Math.max(Math.min(CommonExecutor.PARALLELISM / numTileX, numTileY), 1);
            } else {
                numTileY = Math.max((int) Math.round(numTileY * r), 1);
                numTileX = Math.max(Math.min(CommonExecutor.PARALLELISM / numTileY, numTileX), 1);
            }
        }
        yStride        = numTileX;
        tasks          = new TiledProcess.Task[numTileX * numTileY];    // length ≤ CommonExecutor.PARALLELISM.
        runningThreads = new AtomicInteger(tasks.length);
        /*
         * Prepare the pixel iterators for all sub-tasks, without starting those sub-tasks yet.
         * The sub-tasks will be created and started by `execute()` after everything is ready.
         */
        final int xmin = data.getMinX();
        final int ymin = data.getMinY();
        final int xmax = Math.addExact(xmin, width);
        final int ymax = Math.addExact(ymin, height);
        final int xinc = Numerics.ceilDiv(width,  numTileX);
        final int yinc = Numerics.ceilDiv(height, numTileY);
        final Rectangle subArea = new Rectangle(Math.addExact(xinc, overlapX),
                                                Math.addExact(yinc, overlapY));
        int count = 0;
        iterators = new PixelIterator[tasks.length];
        for (subArea.y = ymin; subArea.y < ymax; subArea.y += yinc) {
            for (subArea.x = xmin; subArea.x < xmax; subArea.x += xinc) {
                iterators[count++] = iteratorFactory.setRegionOfInterest(subArea).create(data);
            }
        }
        assert count == tasks.length;
    }

    /**
     * Starts execution of each sub-task in its own thread.
     *
     * @return a {@code Future} representing pending completion of the task.
     */
    public final Future<R> execute() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        final Future<R>[] components = new Future[tasks.length];
        while (taskIndex < components.length) {
            final Task task = createSubTask();
            components[taskIndex++] = CommonExecutor.instance().submit(task);
        }
        iterators = null;       // Clear only after all tasks have been created.
        return CompoundFuture.create(components);
    }

    /**
     * Creates a sub-task doing the computation on a sub-region of the image.
     * This method is invoked by {@link #execute()} for each "tile" where the
     * sub-task will be executed. Each sub-tasks will have its
     * {@linkplain Task#iterator own pixel iterator}.
     *
     * @return a sub-task over a sub-region of the image to process.
     */
    protected abstract Task createSubTask();

    /**
     * A task to be executed in {@link TiledProcess} for a sub-region of the image to process.
     *
     * <p>This class implements {@link Callable} for {@code TiledProcess} convenience.
     * This implementation details should be ignored; it may change in any future version.</p>
     */
    abstract class Task implements Callable<R> {
        /**
         * Index of this {@code Task} instance in the {@link TiledProcess#tasks} array.
         */
        private final int index;

        /**
         * Iterator over the pixels in the sub-region explored by this task.
         */
        protected final PixelIterator iterator;

        /**
         * Synchronization lock during the merge phase. Created only before merge attempts.
         * This reference shall be considered final after initialization.
         */
        private ReentrantLock merging;

        /**
         * Creates a new sub-task to be executed in a sub-area of a two-dimensional plane.
         */
        protected Task() {
            index = taskIndex;
            iterator = iterators[index];
        }

        /**
         * Executes this sub-task. This method is invoked by {@link TiledProcess#execute()}
         * in a background thread. Implementation should store the result in this {@code Task}
         * instance for future {@linkplain #merge(Task) merge}.
         *
         * @throws Exception if an error occurred during sub-task execution.
         */
        protected abstract void execute() throws Exception;

        /**
         * Merges the result of given sub-task into this one. The given {@code Task} will be a neighbor tile
         * (located on top, left, right or bottom of this tile) on a <em>best-effort</em> basis (no guarantee).
         * After this method call, all data should be in {@code this} and the {@code neighbor} sub-task will be
         * discarded.
         *
         * @param  neighbor  the other sub-task to merge with this one.
         * @throws Exception if an error occurred during merge operation.
         */
        protected abstract void merge(Task neighbor) throws Exception;

        /**
         * Returns the computation result. This method is invoked by {@link TiledProcess} only once,
         * on the last {@code Task} instance after all other instances have been {@linkplain #merge
         * merged} on {@code this}.
         *
         * @return the computation result.
         * @throws Exception if final result can not be computed.
         */
        protected abstract R result() throws Exception;

        /**
         * Executes this sub-task, then tries to merge it with all neighbor sub-tasks that are completed.
         * This method is public as an implementation side-effect and should not be invoked directly;
         * it will typically be invoked by {@link java.util.concurrent.Executor} instead.
         *
         * @return value computed by this sub-task, or {@code null} if this is not the last task.
         * @throws Exception if an error occurred during sub-task execution or merge operation.
         *
         * @see java.util.concurrent.Executor#execute(Runnable)
         */
        @Override
        @SuppressWarnings("fallthrough")
        public final R call() throws Exception {
            execute();
            final Task[] tasks = TiledProcess.this.tasks;
            final int yStride  = TiledProcess.this.yStride;
            final int rowStart = index - (index % yStride);
            /*
             * No lock can exist for this `Task` before this line because they were no reference to
             * `this` in the `tasks` array. This reference will be added (potentially many times)
             * after the lock for making sure that no other thread uses it before we finished.
             */
            assert merging == null;
            merging = new ReentrantLock();
            merging.lock();
            try {
                synchronized (tasks) {
                    assert tasks[index] == null;
                    tasks[index] = this;
                }
                /*
                 * We will check the 4 neighbors (top, left, right, bottom) for another `Task` instance
                 * that we can merge with `this`. We may recheck those 4 neighbors many times depending
                 * how many we found in previous iteration:
                 *
                 *   - If `count == 4` we are done and the loop is stopped.
                 *   - If `count == 0` we found no neighbor and abandon. Note that neighbors may appear later
                 *     if they were still under computation at the time this method is invoked. A final check
                 *     for late additions will be done sequentially in the last thread.
                 *   - For all other counts, we merged some but not all neighbors. Maybe new neighbors became
                 *     available during the time we did the merge, so the 4 neighbors will be checked again.
                 */
                int count = 0;
retry:          for (int side=0;; side++) {         // Break condition is inside the loop.
                    final boolean valid;            // Whether `i` index is inside bounds.
                    final int i;                    // Neighbor index in the `tasks` array.
                    switch (side) {
                        default: {
                            if ((count & 3) == 0) break retry;       // `count` == 0 or 4.
                            count = 0;
                            side  = 0;              // Continue (fallthrough) with case 0.
                        }
                        case 0: i = index - yStride; valid = (i >= 0);                  break;    // Neighbor on top.
                        case 1: i = index - 1;       valid = (i >= rowStart);           break;    // Neighbor on left.
                        case 2: i = index + 1;       valid = (i <  rowStart + yStride); break;    // Neighbor on right.
                        case 3: i = index + yStride; valid = (i <  tasks.length);       break;    // Neighbor on bottom.
                    }
                    if (valid) {
                        /*
                         * Index was computed without synchronization, but access to `tasks` elements
                         * need to be synchronized. If we find a neighbor that we can merge, replace
                         * immediately all instances by `this` so that no other thread will merge it.
                         * The `this` instance will be ignored by other threads until this method exits
                         * because we hold the `this.merging` lock.
                         */
                        final Task neighbor;
                        synchronized (tasks) {
                            neighbor = tasks[i];
                            if (neighbor == null || neighbor == this || !neighbor.merging.tryLock()) {
                                continue;       // Ignore (for now) this neighbor, check next neighbor.
                            }
                            for (int j=0; j<tasks.length; j++) {
                                if (tasks[j] == neighbor) {
                                    tasks[j] = this;
                                }
                            }
                        }
                        /*
                         * Do the actual merge between sub-tasks while we hold the lock on the two
                         * `Task` instances. The `neighbor` reference is discarded after this block.
                         */
                        try {
                            merge(neighbor);
                        } finally {
                            neighbor.merging.unlock();
                        }
                        count++;
                    }
                }
            } finally {
                merging.unlock();
            }
            /*
             * If this thread is the last one, check if there is any unmerged `Task` instances.
             * There is no more synchronization at this point because no other thread is using
             * those objects; this final check is monothread and sequential.
             */
            if (runningThreads.decrementAndGet() != 0) {
                return null;
            }
            for (int i=0; i<tasks.length; i++) {
                final Task other = tasks[i];
                if (other != null && other != this) {
                    assert !other.merging.isLocked();
                    for (int j=i; j<tasks.length; j++) {
                        if (tasks[j] == other) {
                            tasks[j] = this;
                        }
                    }
                    merge(other);
                }
            }
            return result();
        }
    }
}
