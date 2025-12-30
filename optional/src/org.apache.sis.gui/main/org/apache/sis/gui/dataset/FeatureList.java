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
package org.apache.sis.gui.dataset;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Spliterator;
import javafx.application.Platform;
import javafx.collections.ObservableListBase;
import javafx.concurrent.Worker;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.Containers;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;


/**
 * An observable list of features containing only a subset of {@link FeatureSet} content.
 * When an element is requested, if that element has not yet been read, the reading is done
 * in a background thread.
 *
 * <p>This list is unmodifiable through public API except for the {@link #clear()} method.
 * This list should be modified only through package-private API.
 * The intent is to prevents uncontrolled modifications to introduce inconsistencies
 * with the modifications to be applied by the loader running in background thread.</p>
 *
 * <p>This list does not accept null elements; any attempt to add a null feature is silently ignored.
 * The null value is reserved for meaning that the element is in process of being loaded.</p>
 *
 * <p>All methods in this class shall be invoked from JavaFX thread only.</p>
 *
 * @todo Current implementation does not release previously loaded features.
 *       We could do that in a future version if memory usage is a problem,
 *       provided that {@link Spliterator#ORDERED} is set.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FeatureList extends ObservableListBase<AbstractFeature> {
    /**
     * Number of empty rows to show in the bottom of the table when we don't know how many rows still
     * need to be read. Those rows do not stay empty for long since they will become valid as soon as
     * the background loader finished to load a page ({@value FeatureLoader#PAGE_SIZE} rows).
     */
    private static final long NUM_PENDING_ROWS = 10;

    /**
     * Maximum number of rows that this list will allow. Must be smaller than {@link Integer#MAX_VALUE}.
     */
    private static final int MAXIMUM_ROWS = Integer.MAX_VALUE - 1;

    /**
     * The {@link #elements} value when this list is empty.
     */
    private static final AbstractFeature[] EMPTY = new AbstractFeature[0];

    /**
     * The elements in this list, never {@code null}.
     */
    private AbstractFeature[] elements;

    /**
     * Number of valid elements in {@link #elements}.
     */
    private int validCount;

    /**
     * Expected number of elements. Cannot be smaller than {@link #validCount}.
     * May be greater than {@link #elements} length if some elements are not yet loaded.
     */
    private int estimatedSize;

    /**
     * Whether {@link #estimatedSize} is exact.
     */
    private boolean isSizeExact;

    /**
     * If not all features have been read, the task for loading the next batch
     * of {@value FeatureLoader#PAGE_SIZE} features in a background thread.
     * This task will be executed only if there is a need to see new features.
     *
     * <p>If a loading is in progress, then this field is the loader doing the work.
     * But this field will be updated with next loader as soon as the loading is completed.</p>
     *
     * @see #setNextPage(FeatureLoader)
     */
    private FeatureLoader nextPageLoader;

    /**
     * Creates a new list of features.
     */
    FeatureList() {
        elements = EMPTY;
    }

    /**
     * Returns the currently valid elements.
     */
    private List<AbstractFeature> validElements() {
        return Containers.viewAsUnmodifiableList(elements, 0, validCount);
    }

    /**
     * Clears the content of this list. While this method can be invoked from public API,
     * it should be reserved to {@link FeatureList} and {@link FeatureTable} internal usage.
     * This method should be invoked only when no loader is running in a background thread,
     * or when the loaded decided itself to invoke this method.
     */
    @Override
    public void clear() {
        final List<AbstractFeature> removed = validElements();
        elements = EMPTY;
        estimatedSize = 0;
        validCount    = 0;
        beginChange();
        nextReplace(0, 0, removed);
        endChange();
    }

    /**
     * Schedules a background thread which will set the features in this list.
     * If the loading of another {@code FeatureSet} was in progress at the
     * time this method is invoked, that previous loading is cancelled.
     *
     * @param  table     the table which own this list.
     * @param  features  the features to show in the table, or {@code null} if none.
     * @return whether a background process has been scheduled.
     */
    final boolean startFeaturesLoading(final FeatureTable table, final FeatureSet features) {
        assert Platform.isFxApplicationThread();
        final FeatureLoader previous = nextPageLoader;
        if (previous != null) {
            nextPageLoader = null;
            previous.cancel(BackgroundThreads.NO_INTERRUPT_DURING_IO);
        }
        if (features != null) {
            nextPageLoader = new FeatureLoader(table, features);
            BackgroundThreads.execute(nextPageLoader);
            return true;
        } else {
            clear();
            return false;
        }
    }

    /**
     * Invoked by {@link FeatureLoader} for replacing the current content by a new list of features.
     * The list size after this method invocation will be {@code expectedSize}, not {@code count}.
     * The missing elements will be implicitly null until {@link #addFeatures(AbstractFeature[], int, boolean)}
     * is invoked. If the expected size is unknown (i.e. its value is {@link Long#MAX_VALUE}),
     * then an arbitrary size is computed from {@code count}.
     *
     * @param  remainingCount   value of {@link Spliterator#estimateSize()} after partial traversal.
     * @param  characteristics  value of {@link Spliterator#characteristics()}.
     * @param  features         new features. This array is not cloned and may be modified in-place.
     * @param  count            number of valid elements in the given array.
     * @param  hasMore          if the stream may have more features.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    final void setFeatures(long remainingCount, int characteristics,
                           final AbstractFeature[] features, final int count, final boolean hasMore)
    {
        assert Platform.isFxApplicationThread();
        int newValidCount = 0;
        for (int i=0; i<count; i++) {
            final AbstractFeature f = features[i];
            if (f != null) features[newValidCount++] = f;       // Exclude null elements.
        }
        final List<AbstractFeature> removed = validElements();  // Want this call outside {beginChange … endChange}.
        if (remainingCount == Long.MAX_VALUE) {
            remainingCount  = count + NUM_PENDING_ROWS;         // Arbitrary additional amount.
            characteristics = 0;
        }
        estimatedSize = (int) Math.min(MAXIMUM_ROWS, Math.addExact(remainingCount, newValidCount));
        isSizeExact   = (characteristics & Spliterator.SIZED) != 0;
        elements      = features;
        validCount    = newValidCount;
        if (hasMore && estimatedSize <= newValidCount) {
            // Estimated size seems incorrect. Add some empty rows for triggering a new page load.
            estimatedSize = (int) Math.min(MAXIMUM_ROWS, newValidCount + NUM_PENDING_ROWS);
        }
        beginChange();
        nextReplace(0, estimatedSize, removed);
        endChange();
        checkOverflow();
    }

    /**
     * Invoked when more features have been loaded. This method does not actually changes the size of
     * this list, unless the number of elements after this method call exceeds {@link #estimatedSize}.
     *
     * @param  features  the features to add. Null elements are ignored.
     * @param  count     number of valid elements in the given array.
     * @param  hasMore   if the stream may have more features.
     * @throws ArithmeticException if the number of elements exceeds this list capacity.
     */
    final void addFeatures(final AbstractFeature[] features, final int count, final boolean hasMore) {
        assert Platform.isFxApplicationThread();
        if (count > 0) {
            int newValidCount = Math.addExact(validCount, count);
            if (newValidCount > elements.length) {
                // Note: if `length << 1` overflows, it will be negative and max(…) = newValidCount.
                elements = Arrays.copyOf(elements, Math.max(newValidCount, elements.length << 1));
            }
            newValidCount = validCount;         // Recompute `validCount + count` but excluding null elements.
            for (int i=0; i<count; i++) {
                final AbstractFeature f = features[i];
                if (f != null) elements[newValidCount++] = f;
            }
            /*
             * This method is not really adding new elements, but replacing null elements by non-null elements.
             * Only if the new size exceeds the previously expected size, we send a notification about addition.
             */
            final int replaceTo = Math.min(newValidCount, estimatedSize);
            final List<AbstractFeature> removed = Collections.nCopies(replaceTo - validCount, null);
            if (newValidCount >= estimatedSize) {
                estimatedSize = newValidCount;              // Update before we send events.
                if (hasMore) {
                    // Estimated size seems incorrect. Add some empty rows for triggering a new page load.
                    estimatedSize = (int) Math.min(MAXIMUM_ROWS, newValidCount + NUM_PENDING_ROWS);
                }
            }
            beginChange();
            nextReplace(validCount, replaceTo, removed);
            nextAdd(replaceTo, replaceTo + (validCount = newValidCount));
            endChange();
            checkOverflow();
        }
    }

    /**
     * If we cannot load more features stop the reading process.
     *
     * @todo Add some message in the widget for warning the user.
     *       Proposal: set MAXIMUM_ROWS to MAX_INTEGER - 2 and reserve the last table row for a message.
     *       That row would span all columns. That row could also be used for exception message when the
     *       exception did not happened at the file beginning.
     */
    private void checkOverflow() {
        if (validCount >= MAXIMUM_ROWS) {
            interrupt();
        }
    }

    /**
     * Sets the task to be used for next features to load. A {@code null} values notifies
     * this list that the loading process is finished and no more elements will be added.
     *
     * @param  next  the loader for next {@value FeatureLoader#PAGE_SIZE} features,
     *               or {@code null} if there are no more features to load.
     */
    final void setNextPage(final FeatureLoader next) {
        assert Platform.isFxApplicationThread();
        assert nextPageLoader.isDone();
        nextPageLoader = next;
        if (next == null) {
            final int n = estimatedSize - validCount;
            if (n != 0) {
                final List<AbstractFeature> removed = Collections.nCopies(n, null);
                estimatedSize = validCount;
                beginChange();
                nextRemove(validCount, removed);
                endChange();
            }
            isSizeExact = true;
            elements = ArraysExt.resize(elements, validCount);
        }
    }

    /**
     * Returns whether the specified loader is the one scheduled for loading next page of features.
     * We use this check in case a loader has been cancelled and another one started its work immediately.
     */
    final boolean isCurrentLoader(final FeatureLoader loader) {
        return loader == nextPageLoader;
    }

    /**
     * Returns the estimated number of elements.
     * Note that this value may be greater than the number of elements actually loaded.
     */
    @Override
    public int size() {
        return estimatedSize;
    }

    /**
     * Returns the element at the given index. If the element is expected to exist
     * but has not yet been loaded, returns {@code null}.
     */
    @Override
    public AbstractFeature get(final int index) {
        assert Platform.isFxApplicationThread();
        if (index < validCount) {
            return elements[index];
        }
        if (isSizeExact && index >= estimatedSize) {
            throw new IndexOutOfBoundsException(index);
        }
        final FeatureLoader loader = nextPageLoader;
        if (loader != null && loader.getState() == Worker.State.READY) {
            BackgroundThreads.execute(loader);
        }
        return null;
    }

    /**
     * If a loading process was under way, interrupts it and closes the feature stream.
     * This method returns immediately; the release of resources happens in a background thread.
     *
     * @see FeatureTable#interrupt()
     */
    final void interrupt() {
        assert Platform.isFxApplicationThread();
        final FeatureLoader loader = nextPageLoader;
        nextPageLoader = null;
        if (loader != null) {
            loader.cancel(BackgroundThreads.NO_INTERRUPT_DURING_IO);
            BackgroundThreads.execute(loader::waitAndClose);
        }
    }
}
