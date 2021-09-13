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
package org.apache.sis.gui.metadata;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.function.IntFunction;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import org.opengis.metadata.Metadata;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.Styles;


/**
 * Base class of pane with data organized as a form. For all comments in this class,
 * "line" means a (label, value) pair. This is closely related to a grid pane row but
 * not identical lines may not start at grid row zero; the grid may have some header
 * controls before the lines start.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param <T> the type of information object (e.g. {@link org.opengis.metadata.identification.Identification}).
 *
 * @since 1.1
 * @module
 */
abstract class Section<T> extends GridPane implements EventHandler<ActionEvent> {
    /**
     * The pane which contains this section. Used for fetching information like number format
     * or localization resources.
     */
    final MetadataSummary owner;

    /**
     * Number of children per line. This is not necessarily the number of columns in the grid pane
     * since we reserve the last column for {@linkplain #pagination}, which span all grid rows.
     */
    static final int NUM_CHILD_PER_LINE = 2;

    /**
     * For selecting which section to show when there is many. Children are {@link ToggleButton}.
     * The number of buttons should be the length of {@link #information} array.
     */
    private final TilePane pagination;

    /**
     * The group of {@linkplain #pagination} buttons, for keeping only one of the selected at a given time.
     */
    private final ToggleGroup pageGroup;

    /**
     * The information to show in this section, or {@code null} if not yet determined.
     * This array shall not contain any null element.
     */
    private T[] information;

    /**
     * Index of the grid pane row containing the first (label, value) pair of this section.
     * This is usually 0, unless there is some heading rows managed by the subclass.
     */
    private int rowOfFirstLine;

    /**
     * Index of the first child containing the lines added by calls to {@link #addLine(short, String)} method.
     * All children from this index until the end of the children list shall be instances of {@link Label}.
     */
    private int linesStartIndex;

    /**
     * Index after the last valid child. Should be equal to {@code getChildren().size()} but may temporarily
     * differ while we are updating the pane content with new information. The difference may happen because
     * we try to recycle existing {@link Label} instances before to discard them and create new ones.
     */
    private int linesEndIndex;

    /**
     * Creates a new section.
     */
    Section(final MetadataSummary owner) {
        this.owner = owner;
        pageGroup  = new ToggleGroup();
        pagination = new TilePane(Orientation.VERTICAL);
        pagination.setAlignment(Pos.TOP_RIGHT);
        pagination.setVgap(9);
        pagination.setHgap(9);
        setPadding(Styles.FORM_INSETS);
        setVgap(9);
        setHgap(9);
        add(pagination, NUM_CHILD_PER_LINE, 0);
        setVgrow(pagination, Priority.ALWAYS);
        setHgrow(pagination, Priority.NEVER);
        getColumnConstraints().setAll(
            new ColumnConstraints(),     // Let GridPane computes the width of this column.
            new ColumnConstraints(100, 300, Double.MAX_VALUE, Priority.ALWAYS, null, true)
        );
    }

    /**
     * Must be invoked by sub-class constructors after they finished construction.
     */
    final void finished() {
        rowOfFirstLine  = getRowCount();
        linesStartIndex = getChildren().size();
    }

    /**
     * Returns the number of non-null items specified in last calls to
     * {@link #setInformation(Collection, IntFunction)}.
     */
    final int numPages() {
        return (information != null) ? information.length : 0;
    }

    /**
     * Sets the information from the given metadata. Subclasses extract the collection of interest
     * and delegate to the {@link #setInformation(Collection, IntFunction)} method.
     *
     * @param  metadata  the metadata to show, or {@code null}.
     */
    abstract void setInformation(Metadata metadata);

    /**
     * Specifies a new set of information objects to show. This method takes a snapshot of
     * collection content, selects an element to show and invoke {@link #buildContent(Object)}.
     *
     * @param  info       the information objects to be shown, or an empty collection if none.
     * @param  generator  {@code T[]::new}, to be provided by subclasses.
     */
    final void setInformation(final Collection<? extends T> info, final IntFunction<T[]> generator) {
        information = info.toArray(generator);
        /*
         * The array of information should not contain any null element.
         * However we are better to check. Null elements are removed below.
         */
        int n = 0;
        for (final T e : information) {
            if (e != null) {
                information[n++] = e;
            }
        }
        information = ArraysExt.resize(information, n);
        /*
         * Adjusts the number of children in the `pagination` control so that the number of
         * buttons is the length of `information` array. We try to recycle existing objects.
         */
        final ObservableList<Node> pages = pagination.getChildren();
        int i = pages.size();
        if (i < n) {
            final NumberFormat format = owner.getNumberFormat();
            do {
                final ToggleButton b = new ToggleButton(format.format(++i));
                b.setToggleGroup(pageGroup);
                b.setOnAction​(this);
                pages.add(b);
            } while (i < n);
        } else if (i > n) {
            pages.subList(n, i).clear();
        }
        /*
         * Update the pane content with the first information.
         */
        linesEndIndex = linesStartIndex;
        if (n != 0) {
            pageGroup.selectToggle((ToggleButton) pagination.getChildren().get(0));
            update(0);
        }
    }

    /**
     * Invoked when the user selects a page. This method locates which button has been pressed,
     * then invokes {@link #update(int)} for the corresponding page of information.
     */
    @Override
    public final void handle(final ActionEvent event) {
        final ToggleButton source = (ToggleButton) event.getSource();
        if (source.isSelected()) {
            final ObservableList<Node> children = pagination.getChildren();
            final int n = children.size();
            for (int i=0; i<n; i++) {
                if (children.get(i) == source) {
                    update(i);
                    break;
                }
            }
        }
    }

    /**
     * Invoked when the pane needs to update its content. This method resets this pane except for the
     * pagination control, invokes {@link #buildContent(Object)} then discards any extraneous labels.
     * The pagination control will span as many rows as the grid pane has.
     */
    private void update(final int index) {
        linesEndIndex = linesStartIndex;
        buildContent(information[index]);
        setRowSpan(pagination, nextRowIndex());               // For allowing getRowCount() to compute a correct value.
        final ObservableList<Node> children = getChildren();
        children.subList(linesEndIndex, children.size()).clear();
        setRowSpan(pagination, getRowCount());
    }

    /**
     * Invoked when a new information object should be shown in this pane.
     * Implementer should invoke {@link #addLine(short, String)} method.
     *
     * @param  info   the information object to show (never {@code null}).
     */
    abstract void buildContent(T info);

    /**
     * Adds a (label, value) pair to this section.
     * This method does nothing if the given {@code value} is null.
     *
     * @param  label  a {@link Vocabulary.Keys} for the label of the line to add.
     * @param  value  the value associated to the label, or {@code null} if none.
     */
    final void addLine(final short label, final String value) {
        if (value == null) {
            return;
        }
        final String labelText = owner.vocabulary.getLabel(label);
        final Label labelCtrl, valueCtrl;
        final ObservableList<Node> children = getChildren();
        if (linesEndIndex < children.size()) {
            labelCtrl = (Label) children.get(linesEndIndex);
            valueCtrl = (Label) children.get(linesEndIndex + 1);
            labelCtrl.setText(labelText);
        } else {
            final int row = nextRowIndex();
            labelCtrl = new Label(labelText);
            valueCtrl = new Label();
            labelCtrl.setLabelFor(valueCtrl);
            valueCtrl.setWrapText(true);
            add(labelCtrl, 0, row);
            add(valueCtrl, 1, row);
            setValignment(labelCtrl, VPos.TOP);
            setValignment(valueCtrl, VPos.TOP);
        }
        valueCtrl.setText(value);
        linesEndIndex += NUM_CHILD_PER_LINE;
    }

    /**
     * Returns the row index in the grid pane of the next line.
     */
    final int nextRowIndex() {
        return (linesEndIndex - linesStartIndex) / NUM_CHILD_PER_LINE + rowOfFirstLine;
    }

    /**
     * Returns the number of children before the first line added by {@link #addLine(short, String)}.
     */
    final int linesStartIndex() {
        return linesStartIndex;
    }

    /**
     * Returns {@code true} if this section contains no data.
     */
    boolean isEmpty() {
        return linesStartIndex >= linesEndIndex;
    }
}
