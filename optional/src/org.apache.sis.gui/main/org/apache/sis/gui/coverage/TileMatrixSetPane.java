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

import javafx.scene.control.TextArea;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.gui.Widget;


/**
 * A visual representation of the internal tile matrices defined in a {@link TiledResource}.
 *
 * @todo change the text area to a split pane with a tree view on the left and a description pane on the right
 * @todo if the resource is writable, add tiling modification controls
 *
 * @author Johann Sorel (Geomatys)
 *
 * @sinec 1.7
 */
public class TileMatrixSetPane extends Widget {
    /**
     * The data shown in this widget.
     *
     * @see #getContent()
     * @see #setContent(TiledResource)
     */
    public final ObjectProperty<TiledResource> contentProperty;

    private final TextArea area = new TextArea();

    /**
     * Creates an initially empty pane showing the content of a tile matrix.
     */
    public TileMatrixSetPane() {
        area.setMaxSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        area.setEditable(false);
        contentProperty = new SimpleObjectProperty<>(this, "content");
        contentProperty.addListener(TileMatrixSetPane::applyChange);
    }

    /**
     * Sets the data for which to show the tiling.
     * This is a convenience method for setting {@link #contentProperty} value.
     *
     * @param  data  the data for which to show the tiling, or {@code null} if none.
     */
    public final void setContent(final TiledResource data) {
        contentProperty.setValue(data);
    }

    /**
     * Returns the data for which the tiling is currently shown, or {@code null} if none.
     * This is a convenience method for fetching {@link #contentProperty} value.
     *
     * @return the table for which the tiling is currently shown, or {@code null} if none.
     *
     * @see #contentProperty
     * @see #setContent(TiledResource)
     */
    public final TiledResource getContent() {
        return contentProperty.getValue();
    }

    /**
     * Returns the region containing the visual components managed by this {@code TileMatrixSetPane}.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the JavaFX component to insert in a scene graph.
     */
    @Override
    public Region getView() {
        return area;
    }

    /**
     * Invoked when {@link #contentProperty} value changed.
     *
     * @param  property  the property which has been modified.
     * @param  oldValue  the old tree table.
     * @param  newValue  the tree table to use for building new content.
     */
    private static void applyChange(final ObservableValue<? extends TiledResource> property,
                                    final TiledResource oldValue, final TiledResource newValue)
    {
        final var s = (TileMatrixSetPane) ((SimpleObjectProperty) property).getBean();
        if (newValue == null) {
            s.area.setText(null);
            return;
        }
        final var sb = new StringBuilder();
        try {
            for (TileMatrixSet tms : newValue.getTileMatrixSets()) {
                sb.append(tms);
            }
        } catch (DataStoreException ex) {
            sb.append(ex.getMessage());
        }
        s.area.setText(sb.toString());
    }
}
