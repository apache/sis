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
     * Whether to show a visual indication of which tiles are read.
     */
    private final ItemController showTileReads;

    /**
     * Creates a controller for a map layer.
     * The controller is initially selected.
     *
     * @param  view       where the coverage will be rendered.
     * @param  resources  resources for localized <abbr>GUI</abbr> elements.
     */
    StyleController(final CoverageCanvas view, final Resources resources) {
        setSelected(true);
        setIndependent(true);
        selectedProperty().addListener((p,o,n) -> view.setCoverageHidden(!n));
        showTileReads = new ItemController(new MapItem(resources.getString(Resources.Keys.ShowTileReadEvents)));
        showTileReads.selectedProperty().addListener((p,o,n) -> view.showTileReads(n));
        showTileReads.setIndependent(true);
        getChildren().add(showTileReads);
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
        } else {
            if (isTiled) {
                children.add(showTileReads);
            }
        }
    }
}
