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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import javafx.util.Callback;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.TextAlignment;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.TransformationList;
import javafx.scene.layout.Background;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.gui.internal.Styles;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.FeatureType;
import org.opengis.feature.Feature;


/**
 * Wraps a {@link FeatureList} with the capability to expand the multi-valued properties of
 * a selected {@link Feature}. The expansion appears as additional rows below the feature.
 * This view is used only if the feature type contains at least one property type with a
 * maximum number of occurrence greater than 1.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ExpandableList extends TransformationList<Feature,Feature>
        implements Callback<TableColumn<Feature,Feature>, TableCell<Feature,Feature>>,
                   EventHandler<MouseEvent>
{
    /**
     * The background of {@linkplain #expansion} rows header.
     */
    private static final Background EXPANSION_HEADER = Background.fill(Styles.EXPANDED_ROW);

    /**
     * The icon for rows that can be expanded.
     * Set to the Unicode supplementary character U+1F5C7 "Empty Note Pad".
     */
    private static final String EXPANDABLE = "\uD83D\uDDC7";

    /**
     * The icon for rows that are expanded.
     * Set to the Unicode supplementary character U+1F5CA "Note Pad".
     */
    private static final String EXPANDED = "\uD83D\uDDCA";

    /**
     * Mapping from property names to index in the {@link ExpandedFeature#values} array.
     * This map is shared by all {@link ExpandedFeature} instances contained in this list.
     * It shall be modified only if this list has been cleared first.
     */
    private final Map<String,Integer> nameToIndex;

    /**
     * View index of the first row of the currently expanded feature, {@link Integer#MAX_VALUE} if none.
     * Expansion rows will be added or removed <em>below</em> this row; this row itself will not be removed.
     */
    private int indexOfExpanded;

    /**
     * The rows of the expanded feature, or {@code null} if none. If non-null, this array shall never be empty.
     * The reason why we do not allow empty arrays is because we will insert {@code expansion.length - 1} rows
     * below the expanded feature. Consequently, empty arrays cause negative indices that are more difficult to
     * debug than {@link NullPointerException}, because they happen later.
     *
     * <p>If non-null, they array should have at least 2 elements. An array of only 1 element is not wrong,
     * but is useless since it has no additional rows to show below the first row.</p>
     */
    private ExpandedFeature[] expansion;

    /**
     * Creates a new expandable list for the given features.
     *
     * @param  features  the {@link FeatureList} list to wrap.
     */
    ExpandableList(final FeatureList features) {
        super(features);
        nameToIndex     = new LinkedHashMap<>();
        indexOfExpanded = Integer.MAX_VALUE;
    }

    /**
     * Specifies the names of properties that may be multi-valued. This method needs to be invoked
     * only if the {@link FeatureType} changed. This method shall not be invoked if there is any
     * {@link #expansion} rows. Normally this list will be empty at invocation time.
     *
     * @param  columnNames  names of properties that may contain multi-values.
     */
    final void setMultivaluedColumns(final List<String> columnNames) {
        assert expansion == null : indexOfExpanded;
        nameToIndex.clear();
        final int size = columnNames.size();
        for (int i=0; i<size; i++) {
            nameToIndex.putIfAbsent(columnNames.get(i), i);
        }
    }

    /**
     * Removes the expanded rows. This method does not fire change event;
     * it is caller's responsibility to perform those tasks.
     *
     * <h4>Design note</h4>
     * We return {@code null} instead of an empty list if
     * there are no removed elements because we want to force callers to perform a null check.
     * The reason is that if there was no expansion rows, then {@link #indexOfExpanded} has an
     * invalid value and using that value in {@link #nextRemove(int, List)} may be dangerous.
     * A {@link NullPointerException} would intercept that error sooner.
     *
     * @return the removed rows, or {@code null} if none.
     */
    private List<Feature> shrink() {
        final List<Feature> removed = (expansion == null) ? null
                                    : UnmodifiableArrayList.wrap(expansion, 1, expansion.length);
        expansion       = null;
        indexOfExpanded = Integer.MAX_VALUE;
        return removed;
    }

    /**
     * Clears all elements from this list. This method removes the expanded rows before to
     * remove the rest of the list because otherwise, the {@code sourceChanged(…)} method in
     * this class would have to expand the whole feature list for inserting removed elements.
     */
    @Override
    public void clear() {
        final int removeAfter = indexOfExpanded;
        final List<Feature> removed = shrink();
        if (removed != null) {
            beginChange();
            nextUpdate(removeAfter);
            nextRemove(removeAfter + 1, removed);
            endChange();
        }
        getSource().clear();
    }

    /**
     * Invoked when user clicked on the icon on the left of a row.
     * The method sets the expanded rows to the ones containing the clicked cell.
     * If that row is the currently expanded one, then it will be reduced to a single row.
     */
    @Override
    public void handle(final MouseEvent event) {
        /*
         * Remove the additional rows from this list. Before doing so, we need to remember
         * what we are removing from this list view in order to send notification later.
         */
        final IconCell cell = (IconCell) event.getSource();
        final int index = getSourceIndex(cell.getIndex());      // Must be invoked before `shrink()`.
        final int removeAfter = indexOfExpanded;
        final List<Feature> removed = shrink();
//      index = getViewIndex(index);                // Not needed for current single-selection model.
        /*
         * If a new row is selected, extract now all properties. We need at least the number
         * of properties anyway for determining the number of additional rows.  But we store
         * also the property values in arrays for convenience because we cannot use indices
         * on arbitrary collections (they may not be lists).  This is okay on the assumption
         * that the number of elements is not large.
         */
        if (index != indexOfExpanded) {
            expansion = ExpandedFeature.create(cell.getItem(), nameToIndex);
            if (expansion != null) {
                indexOfExpanded = index;
                final int limit = Integer.MAX_VALUE - getSource().size();
                if (expansion.length > limit) {
                    if (limit > 1) {
                        expansion = Arrays.copyOf(expansion, limit);    // Drop last rows for avoiding integer overflow.
                    } else {
                        expansion = null;                               // Drop completely for avoiding integer overflow.
                        indexOfExpanded = Integer.MAX_VALUE;
                    }
                }
            }
        }
        /*
         * Send change notifications only after all states have been updated.
         */
        beginChange();
        if (removed != null) {
            nextUpdate(removeAfter);
            nextRemove(removeAfter + 1, removed);
        }
        if (expansion != null) {
            // An ArithmeticException below would be a bug in above limit adjustment.
            nextAdd(indexOfExpanded + 1, Math.addExact(indexOfExpanded, expansion.length));
        }
        endChange();
    }

    /**
     * Returns {@code true} if the given feature contains more than one row.
     */
    private boolean isExpandable(final Feature feature) {
        if (feature != null) {
            for (final String name : nameToIndex.keySet()) {
                final Object value = feature.getPropertyValue(name);
                if (value instanceof Collection<?>) {
                    final int size = ((Collection<?>) value).size();
                    if (size >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the number of elements in this list.
     */
    @Override
    public int size() {
        int size = getSource().size();
        if (size != 0 && expansion != null) {
            size += expansion.length - 1;
        }
        return size;
    }

    /**
     * Returns the feature at the given index. This method forwards the request to the source,
     * except if the given index is for an expanded row.
     */
    @Override
    public Feature get(int index) {
        final int i = index - indexOfExpanded;
        if (i >= 0) {
            final int n = expansion.length;     // A NullPointerException here would be an ExpandableList bug.
            if (i < n) return expansion[i];
            index -= n;
        }
        return getSource().get(index);
    }

    /**
     * Given an index in this expanded list, returns the index of corresponding element in the feature list.
     * All indices from {@link #indexOfExpanded} inclusive to <code>{@linkplain #indexOfExpanded} +
     * {@linkplain #expansion}.length</code> exclusive map to the same {@link Feature} instance.
     *
     * @param  index  index in this expandable list.
     * @return index of the corresponding element in {@link FeatureList}.
     */
    @Override
    public int getSourceIndex(int index) {
        if (index > indexOfExpanded) {
            index = Math.max(indexOfExpanded, index - (expansion.length - 1));
            // A NullPointerException above would be an ExpandableList bug.
        }
        return index;
    }

    /**
     * Given an index in the feature list, returns the index in this expandable list.
     * If the given index maps the expanded feature, then the returned index will be
     * for the first row. This is okay if the lower index inclusive or for upper index
     * <em>exclusive</em> (it would not be okay for upper index inclusive).
     *
     * @param  index  index in the wrapped {@link FeatureList}.
     * @return index of the corresponding element in this list.
     */
    @Override
    public int getViewIndex(int index) {
        if (index > indexOfExpanded) {
            index += expansion.length - 1;
            // A NullPointerException above would be an ExpandableList bug.
        }
        return index;
    }

    /**
     * Notifies all listeners that the list changed. This method expects an event from the wrapped
     * {@link FeatureList} and converts source indices to indices of this expandable list.
     */
    @Override
    protected void sourceChanged(final ListChangeListener.Change<? extends Feature> c) {
        fireChange(new ListChangeListener.Change<Feature>(this) {
            @Override public void     reset()               {c.reset();}
            @Override public boolean  next()                {return c.next();}
            @Override public boolean  wasAdded()            {return c.wasAdded();}
            @Override public boolean  wasRemoved()          {return c.wasRemoved();}
            @Override public boolean  wasReplaced()         {return c.wasReplaced();}
            @Override public boolean  wasUpdated()          {return c.wasUpdated();}
            @Override public boolean  wasPermutated()       {return c.wasPermutated();}
            @Override protected int[] getPermutation()      {return null;}  // Not invoked since we override the method below.
            @Override public    int   getPermutation(int i) {return getViewIndex(c.getPermutation(getSourceIndex(i)));}
            @Override public    int   getFrom()             {return getViewIndex(c.getFrom());}
            @Override public    int   getTo() {
                // If remove only, must be where removed elements were positioned in the list.
                return (wasAdded() || !wasRemoved()) ? getViewIndex(c.getTo()) : getFrom();
            }

            @Override
            public int getRemovedSize() {
                int removedSize = c.getRemovedSize();
                if (overlapExpanded(c.getFrom(), removedSize)) {
                    removedSize += expansion.length - 1;
                }
                return removedSize;
            }

            @Override
            @SuppressWarnings("unchecked")
            public List<Feature> getRemoved() {
                return (List<Feature>) expandRemoved(c.getFrom(), c.getRemoved());
            }
        });
    }

    /**
     * Returns {@code true} if the given range of removed rows overlaps the expanded rows.
     */
    private boolean overlapExpanded(final int sourceFrom, final int removedSize) {
        return (sourceFrom <= indexOfExpanded && sourceFrom > indexOfExpanded - removedSize);   // Use - for avoiding overflow.
    }

    /**
     * If the range of removed elements overlaps the range of expanded rows, inserts values in the
     * {@code removed} list for the expanded rows. Actually this insertion should never happens in
     * the way we use {@link ExpandableList}, but we check as a safety.
     *
     * @param  sourceFrom  index of the first removed element in the source list.
     * @param  removed     the removed elements provided by the {@link FeatureList}.
     * @return the removed elements as seen by this {@code ExpandableList}.
     */
    private List<? extends Feature> expandRemoved(final int sourceFrom, final List<? extends Feature> removed) {
        if (!overlapExpanded(sourceFrom, removed.size())) {
            return removed;
        }
        final int s = indexOfExpanded;
        final int n = expansion.length;         // A NullPointerException here would be an ExpandableList bug.
        final Feature[] features = removed.toArray(new Feature[removed.size() + (n - 1)]);
        System.arraycopy(features,  s+1, features, s + n, features.length - (s+1));
        System.arraycopy(expansion, 0,   features, s,  n);
        return Arrays.asList(features);
    }

    /**
     * Creates a new cell for an icon to show at the beginning of a row.
     * This method is provided for allowing {@code ExpandableList} to be
     * given to {@link TableColumn#setCellFactory(Callback)}.
     *
     * @param  column  the column where the cell will be shown.
     */
    @Override
    public TableCell<Feature,Feature> call(final TableColumn<Feature,Feature> column) {
        return new IconCell();
    }

    /**
     * The cell which represents whether a row is expandable or expanded.
     * If visible, this is the first column in the table.
     */
    private final class IconCell extends TableCell<Feature,Feature>  {
        /**
         * Whether this cell is listening to mouse click events.
         */
        private boolean isListening;

        /**
         * Creates a new cell for feature property value.
         */
        IconCell() {
            setTextAlignment(TextAlignment.CENTER);
        }

        /**
         * Invoked when a new feature needs to be show. This method sets an icon depending on
         * whether there is multi-valued properties, and whether the current row is expanded.
         * The call will have a listener only if it has an icon.
         */
        @Override
        protected void updateItem(final Feature value, final boolean empty) {
            super.updateItem(value, empty);
            Background b = null;
            String  text = null;
            if (value instanceof ExpandedFeature) {
                /*
                 * If this is the selected row, put an icon only on the first row,
                 * not on additional rows showing the other collection elements.
                 */
                if (((ExpandedFeature) value).index == 0) {
                    text = EXPANDED;
                } else {
                    b = EXPANSION_HEADER;
                }
            } else if (isExpandable(value)) {
                text = EXPANDABLE;
            }
            setBackground(b);
            setText(text);
            if (isListening != (isListening = (text != null))) {        // Check whether `isListening` changed.
                if (isListening) {
                    addEventFilter(MouseEvent.MOUSE_CLICKED, ExpandableList.this);
                } else {
                    removeEventFilter(MouseEvent.MOUSE_CLICKED, ExpandableList.this);
                }
            }
        }
    }
}
