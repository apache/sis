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
package org.apache.sis.gui.map;

import java.util.concurrent.atomic.AtomicReference;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.Menu;
import javafx.application.Platform;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.gui.coverage.CoverageCanvas;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.gui.internal.BackgroundThreads;


/**
 * Provider of textual content to show in a {@link StatusBar} for values under cursor position.
 * When the mouse cursor moves, {@link #evaluateLater(DirectPosition)} is invoked with the same
 * "real world" coordinates than the ones shown in the status bar.
 *
 * <h2>Multi-threading</h2>
 * Instances of {@code ValueUnderCursor} do not need to be thread-safe, because
 * all {@code ValuesUnderCursor} methods will be invoked from JavaFX thread.
 * However, the actual fetching and formatting of values will be done in a background
 * thread using the {@link Formatter} inner class, which needs to be thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 */
public abstract class ValuesUnderCursor {
    /**
     * The status bar for which this object is providing values.
     * Each {@link ValuesUnderCursor} instance is used by at most one {@link StatusBar} instance.
     * This field shall be read and written from JavaFX thread only.
     *
     * @see #update(StatusBar, ValuesUnderCursor, ValuesUnderCursor)
     */
    private StatusBar owner;

    /**
     * Menu offering choices among the values that this {@code ValuesUnderCursor} can show.
     * This menu will be available as a contextual menu in the {@link StatusBar}.
     * It is subclass responsibility to listen to menu selections and adapt their
     * {@link #evaluateLater(DirectPosition)} output accordingly.
     */
    protected final Menu valueChoices;

    /**
     * The task to execute in JavaFX thread for showing the result of formatting values at cursor position.
     * This is given in a call to {@link Platform#runLater(Runnable)} after the values have been formatted
     * as text in a background thread.
     */
    private final Consumer consumer;

    /**
     * Task to execute in JavaFX thread for showing the result of formatting values at cursor position.
     * The {@link AtomicReference} value is the text to show in {@linkplain #owner owner} status bar.
     * The value is atomically set to {@code null} as it is given to the control.
     */
    @SuppressWarnings("serial")         // Not intended to be serialized.
    private final class Consumer extends AtomicReference<String> implements Runnable {
        /**
         * Creates a new task to execute in JavaFX thread for showing sample values.
         */
        Consumer() {
        }

        /**
         * Sets the result to the given value, then submits a task in JavaFX thread if no task is already waiting.
         * If a task is already waiting to be executed, then that task will use the specified value instead of the
         * value which was specified when the previous task was submitted.
         */
        final void setLater(final String result) {
            if (getAndSet(result) == null) {
                Platform.runLater(this);
            }
        }

        /**
         * Invoked in JavaFX thread for showing the sample values. The value is reset to {@code null}
         * for letting {@link #setLater(String)} know that the value has been consumed.
         */
        @Override
        public void run() {
            final String text = getAndSet(null);        // Must be invoked even if `owner` is null.
            final StatusBar c = owner;
            if (c != null) c.setSampleValues(text);
        }
    }

    /**
     * Creates a new evaluator instance. The {@link #valueChoices} list of items is initially empty;
     * subclass constructor should set a text and add items.
     */
    protected ValuesUnderCursor() {
        valueChoices = new Menu();
        consumer = new Consumer();
    }

    /**
     * Returns the task for fetching and formatting values in a background thread.
     * {@code ValuesUnderCursor} subclasses should keep a single {@link Formatter} instance,
     * eventually replaced when the data shown in {@link MapCanvas} changed.
     * That instance will be reused every time that the cursor position changed.
     *
     * @return task for fetching and formatting values in a background thread, or {@code null} if none.
     *
     * @since 1.3
     */
    protected abstract Formatter formatter();

    /**
     * Formats a string representation of data under given "real world" position.
     * This method shall be invoked in JavaFX thread, but values will be fetched
     * and formatted in a background thread managed automatically by this
     * {@code ValuesUnderCursor} class.
     *
     * <p>The {@linkplain DirectPosition#getCoordinateReferenceSystem() position CRS}
     * should be non-null for avoiding ambiguity about what is the default CRS.
     * The position CRS can be anything; it will be transformed if needed.</p>
     *
     * @param  point  the cursor location in arbitrary CRS (usually the CRS shown in the status bar).
     *                May be {@code null} for declaring that the point is outside canvas region.
     *
     * @since 1.3
     */
    public void evaluateLater(final DirectPosition point) {
        final Formatter formatter = formatter();
        if (formatter != null) {
            formatter.evaluateLater(point);
        }
    }

    /**
     * Task for fetching and formatting values in a background thread.
     * The background thread and the interaction with JavaFX thread are managed by the enclosing class.
     * The same {@code Formatter} instance can be reused as long as the source of data does not change.
     *
     * <p>As a rule of thumbs, all properties in {@link ValuesUnderCursor} class shall be read and written
     * from the JavaFX thread, while all properties in this {@code Formatter} class may be read and written
     * from any thread.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.3
     * @since   1.3
     */
    protected abstract static class Formatter implements Runnable {
        /**
         * Coordinates and CRS of the position where to evaluate values.
         * This position shall not be modified; new coordinates shall be specified in a new instance.
         * A {@code null} value means that there is no more sample values to format.
         *
         * <p>Instances are created by {@link #copy(DirectPosition)}. The same instance shall be given
         * to {@link #evaluate(DirectPosition)} because subclasses may rely on a specific type.</p>
         */
        private DirectPosition position;

        /**
         * Whether there is a new point for which to format sample values.
         * The new point may be {@code null}.
         */
        private boolean hasNewPoint;

        /**
         * Whether a background thread is already running. This information is used for looping
         * in the running thread instead of launching many threads when coordinates are updated.
         */
        private boolean isRunning;

        /**
         * A copy of {@link ValuesUnderCursor#consumer} field.
         */
        private final Consumer consumer;

        /**
         * Creates a new formatter instance.
         *
         * @param  owner  instance of the enclosing class which will evaluate values under cursor position.
         */
        protected Formatter(final ValuesUnderCursor owner) {
            consumer = owner.consumer;
        }

        /**
         * Invoked in JavaFX thread for creating a copy of the given position.
         * A copy is needed because the position will be read in a background thread,
         * and the {@code point} instance may change concurrently.
         *
         * <p>Subclasses can override this method for opportunistically fetching
         * in JavaFX thread other information related to the current cursor position.
         * Those information can be stored in a custom {@link DirectPosition} implementation class.
         * The {@link DirectPosition} instance given to the {@link #evaluate(DirectPosition)} method
         * will be the instance returned by this method.</p>
         *
         * @param  point  position to copy (never {@code null}).
         * @return a copy of the given position, or {@code null} if the position should be considered outside.
         */
        DirectPosition copy(final DirectPosition point) {
            return new GeneralDirectPosition(point);
        }

        /**
         * Sets the position of next point to evaluate, then launches background thread if not already running.
         * Even if technically this method can be invoked from any thread, it should be the JavaFX thread.
         * The given position will be copied in order to protect it from concurrent changes.
         *
         * @param  point  coordinates of the point for which to evaluate the grid coverage value.
         *                May be {@code null} for declaring that the point is outside canvas region.
         *
         * @see ValuesUnderCursor#evaluateLater(DirectPosition)
         */
        final synchronized void evaluateLater(final DirectPosition point) {
            position = (point != null) ? copy(point) : null;
            hasNewPoint = true;
            if (!isRunning) {
                BackgroundThreads.execute(this);
                isRunning = true;                   // Set only after success.
            }
        }

        /**
         * Invoked in a background thread for formatting values at the most recent position.
         * If the cursor moves while this method is formatting values, then this method will
         * continue its execution for formatting also the values at new positions until the
         * cursor stop moving.
         *
         * <p>This method does not need to be invoked explicitly; it is invoked automatically
         * by {@link ValuesUnderCursor}. But it may be overridden for adding pretreatment or
         * post-treatment.</p>
         */
        @Override
        public void run() {
            for (;;) {                              // `while(hasNewPoint)` but synchronized.
                final DirectPosition point;
                synchronized (this) {
                    if (!hasNewPoint) {
                        isRunning = false;          // Must be inside the synchronized block.
                        break;
                    }
                    point       = position;
                    position    = null;
                    hasNewPoint = false;
                }
                consumer.setLater(evaluate(point));
            }
        }

        /**
         * Returns a string representation of data under the given "real world" position.
         * The {@linkplain DirectPosition#getCoordinateReferenceSystem() position CRS}
         * should be non-null for avoiding ambiguity about what is the default CRS.
         * The position CRS may be anything; this method shall transform coordinates itself if needed.
         *
         * <p>This method is invoked by {@link #run()} in a background thread.
         * Implementations are responsible for fetching data in a thread-safe manner.</p>
         *
         * @param  point  the cursor location in arbitrary CRS (usually the CRS shown in the status bar).
         *                May be {@code null} for declaring that the point is outside canvas region.
         * @return string representation of data under given position, or {@code null} if none.
         */
        public abstract String evaluate(final DirectPosition point);
    }

    /**
     * Returns whether a status bar is associated to this instance.
     * If {@code false}, then it is useless to compute values for {@link #prototype(String, Iterable)}.
     */
    final boolean usePrototype() {
        return owner != null;
    }

    /**
     * Invoked when a new source of values is known for computing the expected size.
     * The given {@code main} text should be an example of the longest expected text,
     * ignoring "special" labels like "no data" values (those special cases are listed
     * in the {@code others} argument).
     *
     * <p>If {@code main} is an empty string, then no values are expected and {@link MapCanvas}
     * may hide the space normally used for showing values.</p>
     *
     * @param  main    a prototype of longest normal text that we expect.
     * @param  others  some other texts that may appear, such as labels for missing data.
     * @return {@code true} on success, or {@code false} if this method should be invoked again.
     */
    final boolean prototype(final String main, final Iterable<String> others) {
        return (owner == null) || owner.computeSizeOfSampleValues(main, others);
    }

    /**
     * Invoked when {@link StatusBar#sampleValuesProvider} changed. Each {@link ValuesUnderCursor} instance
     * can be used by at most one {@link StatusBar} instance. Current implementation silently does nothing
     * if this is not the case.
     */
    static void update(final StatusBar owner, final ValuesUnderCursor oldValue, final ValuesUnderCursor newValue) {
        if (oldValue != null && oldValue.owner == owner) {
            oldValue.owner = null;
        }
        if (newValue != null && newValue.owner != owner) {
            if (newValue.owner != null) {
                newValue.owner.sampleValuesProvider.set(null);
            }
            newValue.owner = owner;
        }
    }

    /**
     * Creates a new instance for the given canvas and registers as a listener by weak reference.
     * Caller must retain the returned reference somewhere, e.g. in {@link StatusBar#sampleValuesProvider}.
     *
     * @param  canvas  the canvas for which to create a {@link ValuesUnderCursor}, or {@code null}.
     * @return the sample values provider, or {@code null} if none.
     */
    static ValuesUnderCursor create(final MapCanvas canvas) {
        if (canvas instanceof CoverageCanvas) {
            final CoverageCanvas cc = (CoverageCanvas) canvas;
            final ValuesFromCoverage listener = new ValuesFromCoverage();
            cc.coverageProperty.addListener(new WeakChangeListener<>(listener));
            cc.sliceExtentProperty.addListener((p,o,n) -> listener.setSlice(n));
            final GridCoverage coverage = cc.coverageProperty.get();
            if (coverage != null) {
                listener.changed(null, null, coverage);
            }
            return listener;
        } else {
            // More cases may be added in the future.
        }
        return null;
    }

    /**
     * Invoked when an exception occurred while computing values.
     */
    final void setError(final Throwable e) {
        final StatusBar owner = this.owner;
        if (owner != null) {
            owner.setSampleValues(owner.cause(e));
        }
    }
}
