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

import javafx.scene.control.TreeItem;
import javafx.collections.ObservableList;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.map.style.MapItem;
import org.apache.sis.gui.map.style.ItemController;
import org.apache.sis.gui.map.style.MapLayer;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.tiling.TiledGridCoverageResource;


/**
 * A node shown in a tree of map items for controlling the rendering of a single grid coverage.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class StyleController extends ItemController {
    /**
     * The canvas for which to control the rendering.
     */
    private final CoverageCanvas canvas;

    /**
     * Whether to show a visual indication of which tiles are read.
     * Creates when first needed.
     */
    private ItemController showTileReads;

    /**
     * Creates a controller for a map layer.
     * The controller is initially selected.
     *
     * @param  canvas  where the coverage will be rendered.
     */
    StyleController(final CoverageCanvas canvas) {
        this.canvas = canvas;
        setSelected(true);
        setIndependent(true);
        selectedProperty().addListener((p,o,n) -> canvas.setCoverageHidden(!n));
    }

    /**
     * Sets the item to show.
     *
     * @param  item  the new item, or {@code null} if none.
     */
    final void setData(final MapLayer<GridCoverageResource> item) {
        setValue(item);
        final boolean isTiled = (item.resource instanceof TiledGridCoverageResource);
        final ObservableList<TreeItem<MapItem>> children = getChildren();
        final int last = children.size() - 1;
        if (last >= 0 && children.get(last) == showTileReads) {
            if (!isTiled) {
                children.remove(last);
            }
        } else if (isTiled) {
            if (showTileReads == null) {
                final Resources resources = Resources.forLocale(canvas.getLocale());
                showTileReads = new ItemController(new MapItem(resources.getString(Resources.Keys.ShowTileReadEvents)));
                showTileReads.selectedProperty().addListener((p,o,n) -> canvas.showTileReads(n));
                showTileReads.setIndependent(true);
            }
            children.add(showTileReads);
        }
    }
}
