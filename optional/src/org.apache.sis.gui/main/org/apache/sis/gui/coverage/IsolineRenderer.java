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

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.awt.Shape;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import javafx.application.Platform;
import javafx.scene.control.TableView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.gui.controls.ColorRamp;
import org.apache.sis.gui.controls.ValueColorMapper.Step;
import org.apache.sis.image.processing.isoline.Isolines;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.geometry.wrapper.j2d.EmptyShape;
import org.apache.sis.geometry.wrapper.j2d.FlatShape;
import org.apache.sis.util.ArraysExt;


/**
 * Caches and draws isoline shapes in a {@link CoverageCanvas}. This class is designed for interactive use
 * in JavaFX widget; this is not a class for doing symbology e.g. in a web service. Most of the work done
 * by {@code IsolineRenderer} is about listening to changes in {@link TableView}, managing data exchanges
 * between JavaFX thread and background thread, and computes only isolines that are new compared to previous
 * rendering.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class IsolineRenderer {
    /**
     * The canvas where isolines are drawn.
     */
    private final CoverageCanvas canvas;

    /**
     * The list of isoline values and associated shapes for each band, or {@code null} if none.
     */
    private Band[] bands;

    /**
     * Creates an initially empty set of isolines.
     *
     * @param  canvas  the canvas where isolines are drawn.
     */
    public IsolineRenderer(final CoverageCanvas canvas) {
        if (canvas.isolines != null) {
            throw new IllegalArgumentException();
        }
        this.canvas = canvas;
        canvas.isolines = this;
    }

    /**
     * Returns {@code true} if there are no isolines to show.
     * This method shall be invoked in JavaFX thread.
     */
    private boolean isEmpty() {
        if (bands != null) {
            for (final Band band : bands) {
                for (final Step level : band.steps) {
                    if (level.visible.get()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Clears the cache. This method shall be invoked when the image used for computing isolines has changed,
     * or when the {@code gridToCRS} transform has changed. This method shall be invoked in JavaFX thread.
     */
    final void clear() {
        assert Platform.isFxApplicationThread();
        if (bands != null) {
            for (final Band band : bands) {
                band.clear();
            }
        }
    }

    /**
     * Sets the isoline values for all bands from the content of tables edited by user.
     * This method registers listener on the given lists for repainting the isolines
     * when table content changed.
     */
    public void setIsolineTables(final List<ObservableList<Step>> tables) {
        if (bands != null) {
            for (final Band band : bands) {
                band.dispose();
            }
        }
        bands = null;
        if (tables != null && !tables.isEmpty()) {
            bands = new Band[tables.size()];
            for (int i=0; i<bands.length; i++) {
                bands[i] = new Band(tables.get(i));
            }
        }
    }

    /**
     * List of isoline values and associated shapes for a single band.
     */
    private final class Band implements ListChangeListener<Step>, ChangeListener<Object> {
        /**
         * The isoline levels to draw, together with their color.
         * Considered as read-only (in JavaFX thread) by this class.
         */
        final ObservableList<Step> steps;

        /**
         * Cache of Java2D shapes for each isoline level. Shall be read in written in JavaFX thread.
         * New isolines are computed in background thread and results stored later in JavaFX thread.
         *
         * @see Snapshot#isolines
         */
        private Map<Double,Shape> isolines;

        /**
         * Creates an initially empty set of isolines.
         *
         * @param  steps  the list of isoline levels to render.
         */
        Band(final ObservableList<Step> steps) {
            this.steps = steps;
            addListeners(steps);
            steps.addListener(this);
        }

        /**
         * Clears the cache. This method shall be invoked when the image used for computing isolines has changed,
         * or when the {@code gridToCRS} transform has changed. This method shall be invoked in JavaFX thread.
         *
         * @see IsolineRenderer#clear()
         */
        final void clear() {
            // Force new instance instead of `Map.clear()` because previous instance may be used by `Snapshot`.
            isolines = null;
        }

        /**
         * Unregisters all listeners. This method should be invoked before this {@code Band} instance is discarded.
         */
        final void dispose() {
            clear();
            removeListeners(steps);
        }

        /**
         * Invoked when steps are added or removed from the observable list.
         * This method is public as an implementation side-effect and should not be invoked directly.
         *
         * @param  change  set of steps which have been added or removed.
         */
        @Override
        public void onChanged(final Change<? extends Step> change) {
            while (change.next()) {
                if (!change.wasPermutated() && !change.wasUpdated()) {
                    removeListeners(change.getRemoved());
                    addListeners(change.getAddedSubList());
                }
            }
            canvas.requestRepaint();
        }

        /**
         * Unregisters listeners form all properties in the given list.
         */
        private void removeListeners(final List<? extends Step> list) {
            for (final Step level : list) {
                level.value  .removeListener(this);
                level.color  .removeListener(this);
                level.visible.removeListener(this);
            }
        }

        /**
         * Registers listeners on all properties in the given list.
         */
        private void addListeners(final List<? extends Step> list) {
            for (final Step level : list) {
                level.value  .addListener(this);
                level.color  .addListener(this);
                level.visible.addListener(this);
            }
        }

        /**
         * Invoked when an isoline value, color or visibility has changed.
         * This method is public as an implementation side-effect and should not be invoked directly.
         *
         * @param  property  one of the properties defined in the {@link Step} class.
         * @param  oldValue  the old property value, or {@code null} if none.
         * @param  newValue  the new property value, or {@code null} if none.
         */
        @Override
        public void changed(final ObservableValue<?> property, final Object oldValue, final Object newValue) {
            canvas.requestRepaint();
        }

        /**
         * Creates a snapshot of the list of isolines to draw, together with lists of cached shapes
         * and shapes that need to be computed. This snapshot is created in JavaFX thread and used
         * in a background thread.
         *
         * @param  keep  an empty set used by this method for listing the levels to keep in the cache.
         * @return a snapshot of current {@code Band} state for use by a background thread.
         *
         * @see IsolineRenderer#prepare()
         */
        final Snapshot prepare(final Set<Double> keep) {
            if (isolines == null) {
                isolines = new HashMap<>();
            }
            final Snapshot s = new Snapshot(isolines, steps.size());
            for (final Step level : steps) {
                final Double value = level.value.get();
                if (!value.isNaN()) {
                    keep.add(value);
                    if (level.visible.get()) {
                        final ColorRamp cr = level.color.get();
                        if (cr != null && !cr.isTransparent()) {
                            s.add(value, cr.colors[0]);
                        }
                    }
                }
            }
            isolines.keySet().retainAll(keep);          // Discard shapes that are no longer in use.
            keep.clear();
            return s;
        }
    }

    /**
     * Prepares a list of isolines to draw for each bands, initially populated with shapes that are already available.
     * This method shall be invoked in JavaFX thread for having consistent information. The snapshots returned by this
     * method will be completed and used in a background thread.
     *
     * @return snapshots of information about isolines in each bands, or {@code null} if none.
     */
    final Snapshot[] prepare() {
        assert Platform.isFxApplicationThread();
        if (isEmpty()) {
            return null;
        }
        final Snapshot[] snapshots = new Snapshot[bands.length];
        final Set<Double> keep = new HashSet<>();
        for (int i=0; i < snapshots.length; i++) {
            snapshots[i] = bands[i].prepare(keep);
        }
        return snapshots;
    }

    /**
     * Continues isoline preparation by computing the missing Java2D shapes.
     * This method shall be invoked in a background thread. After this call,
     * {@link #complete(Snapshot[], Future)} needs to be invoked before
     * isolines can be painted with {@link Snapshot#paint(Graphics2D, Rectangle2D)}.
     *
     * @param  snapshots  value of {@link #prepare()}. Shall not be {@code null}.
     * @param  data       the source of data. Used only if there is new isolines to compute.
     * @param  gridToCRS  transform from pixel coordinates to geometry coordinates, or {@code null} if none.
     *                    Integer source coordinates are located at pixel centers.
     * @return result of isolines generation, or {@code null} if there are no isolines to compute.
     * @throws TransformException if an interpolated point cannot be transformed using the given transform.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")      // Used only for debugging.
    static Future<Isolines[]> generate(final Snapshot[] snapshots, final RenderedImage data, final MathTransform gridToCRS)
            throws TransformException
    {
        assert !Platform.isFxApplicationThread();
        double[][] levels = null;
        final int numBands = ImageUtilities.getNumBands(data);
        final int numViews = Math.min(numBands, snapshots.length);
        for (int i=0; i<numViews; i++) {
            final Snapshot s = snapshots[i];
            int n = s.missingLevels.size();
            if (n != 0) {
                if (levels == null) {
                    levels = new double[numBands][];
                    Arrays.fill(levels, ArraysExt.EMPTY_DOUBLE);
                }
                final double[] values = new double[n];
                for (final Double value : s.missingLevels.keySet()) {
                    values[--n] = value;
                }
                assert n == 0 : n;
                levels[i] = values;
            }
        }
        /*
         * Compute only the isolines that are not already available. For performance reasons, we need to process
         * all bands in one single call to `Isolines.generate(…)`. Results are written in empty slots of `shapes`.
         */
        if (levels != null) {
            if (CoverageCanvas.TRACE) {
                System.out.println("IsolineRenderer.complete(…):");
                for (int i=0; i<levels.length; i++) {
                    System.out.printf("\tFor band %d: %s%n", i, Arrays.toString(levels[i]));
                }
            }
            return Isolines.parallelGenerate(data, levels, gridToCRS);
        }
        return null;
    }

    /**
     * Waits for completion of isolines generation if not already finished, then stores the result.
     * The {@code isolines} argument is the {@link #generate(Snapshot[], RenderedImage, MathTransform)}
     * return value. Caller shall verify that {@code snapshots} is non-null.
     *
     * @param  snapshots    where to store the result of isoline computations.
     * @param  newIsolines  the result of isolines generation, or {@code null} if none.
     */
    static void complete(final Snapshot[] snapshots, final Future<Isolines[]> newIsolines)
            throws ExecutionException, InterruptedException
    {
        final Isolines[] isolines = newIsolines.get();
        final int n = Math.min(snapshots.length, isolines.length);
        int i;
        for (i=0; i<n; i++) {
            snapshots[i].complete(isolines[i]);
        }
        while (i < snapshots.length) {              // Clear remaining snapshots if any.
            snapshots[i++].clear();
        }
    }

    /**
     * Snapshot of {@link Band} information before rendering. Life cycle is:
     *
     * <ol>
     *   <li>Created in JavaFX thread with shapes already available.</li>
     *   <li>Missing Java2D shapes completed in a background thread.</li>
     *   <li>Painting done in background thread.</li>
     *   <li>New shapes cached in JavaFX thread.</li>
     * </ol>
     */
    static final class Snapshot {
        /**
         * Isolines available before snapshot, and where to store new isolines after completion.
         * This map shall be read and written in JavaFX thread only. This is initially the same
         * reference than {@link Band#isolines}, but may become different if {@link Band#clear()}
         * is invoked while isolines computation or painting is in progress.
         *
         * @see Band#isolines
         */
        private final Map<Double,Shape> isolines;

        /**
         * The isoline levels that are missing in the {@link #isolines} map. This map is populated
         * in JavaFX thread and consumed in the background thread.
         */
        private final Map<Double,Integer> missingLevels;

        /**
         * Isolines that have been created, or {@code null} if none. They are the shapes to store in
         * the {@link #isolines} map after the isoline painting is completed. Stored in a separated map
         * because we must wait to be back to JavaFX thread before we can write in {@link #isolines}.
         */
        private Map<Double,Shape> newIsolines;

        /**
         * The isoline shapes to draw. May contain {@code null} elements if some shapes are missing.
         * Those missing shapes will be computed in a background thread.
         */
        private final Shape[] shapes;

        /**
         * Shape colors, obtained in JavaFX thread.
         */
        private final int[] colors;

        /**
         * Number of valid elements in {@link #shapes} and {@link #colors} array.
         */
        private int count;

        /**
         * Creates a new snapshot of {@link Band} information.
         *
         * @param  isolines  value of {@link Band#isolines} reference.
         * @param  capacity  maximal number of isoline levels that can be {@linkplain #add added}.
         */
        private Snapshot(final Map<Double,Shape> isolines, final int capacity) {
            this.isolines = isolines;
            missingLevels = new HashMap<>();
            shapes = new Shape[capacity];
            colors = new int[capacity];
        }

        /**
         * Removes all isolines.
         */
        private void clear() {
            isolines.clear();
            missingLevels.clear();
            newIsolines = null;
            Arrays.fill(shapes, null);
            count = 0;
        }

        /**
         * Adds an isoline level. This method shall be invoked in JavaFX thread.
         *
         * @param  value  the level value.
         * @param  color  color of the isolines to draw for the specified value.
         */
        private void add(final Double value, final int color) {
            final Shape shape = isolines.putIfAbsent(value, EmptyShape.INSTANCE);
            if (shape == null) {
                missingLevels.put(value, count);
            }
            shapes[count] = shape;
            colors[count] = color;
            count++;
        }

        /**
         * Completes the {@link #shapes} array by assigning a shapes to null elements.
         *
         * @param  isolines  missing isolines computed for the band of this snapshot.
         */
        private void complete(final Isolines isolines) {
            newIsolines = isolines.polylines();
            for (final Map.Entry<Double,Shape> entry : newIsolines.entrySet()) {
                final Integer j = missingLevels.get(entry.getKey());
                if (j != null) shapes[j] = entry.getValue();
            }
        }

        /**
         * Paints all isolines in the given graphics.
         * This method should be invoked in a background thread.
         *
         * @param  target          where to draw isolines.
         * @param  areaOfInterest  the area where isolines will be drawn, or {@code null} if unknown.
         */
        final void paint(final Graphics2D target, final Rectangle2D areaOfInterest) {
            for (int i=0; i<count; i++) {
                Shape shape = shapes[i];
                if (shape != null) {
                    if (areaOfInterest != null && shape instanceof FlatShape) {
                        shape = ((FlatShape) shape).fastClip(areaOfInterest);
                        if (shape == null) continue;
                    }
                    target.setColor(new Color(colors[i], true));
                    target.draw(shape);
                }
            }
        }

        /**
         * Invoked in JavaFX thread after successful rendering for caching the new isolines.
         */
        final void commit() {
            assert Platform.isFxApplicationThread();
            if (newIsolines != null) {
                isolines.putAll(newIsolines);
            }
        }
    }
}
