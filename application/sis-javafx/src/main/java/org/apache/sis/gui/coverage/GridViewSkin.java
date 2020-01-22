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

import java.awt.image.RenderedImage;
import javafx.scene.control.skin.VirtualContainerBase;
import javafx.scene.control.skin.VirtualFlow;


/**
 * The {@link GridView} renderer as a virtualized and scrollable content.
 * The primary direction of virtualization is vertical (rows will stack vertically on top of each other).
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>This is created by {@link GridView#createDefaultSkin()}.</li>
 *   <li>The {@link GridView} owner is given by {@link #getSkinnable()}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GridViewSkin extends VirtualContainerBase<GridView, GridRow> {
    /**
     * Creates a new skin for the specified view.
     */
    GridViewSkin(final GridView view) {
        super(view);
        final VirtualFlow<GridRow> flow = getVirtualFlow();
        flow.setCellFactory(GridRow::new);
        flow.setFocusTraversable(true);
        flow.setPannable(false);
        /*
         * The list of children is initially empty. We need to
         * add the virtual flow, otherwise nothing will appear.
         */
        getChildren().add(flow);
    }

    /*
     * TODO:
     * VirtualFlow.setFixedCellSize​(double): For optimisation purposes, some use cases can trade
     * dynamic cell length for speed. If fixedCellSize is greater than zero JavaFX uses that rather
     * than determining it by querying the cell itself.
     */

    /**
     * Returns the total number of image rows, including those that are currently hidden because
     * they are out of view. The returned value is (indirectly) {@link RenderedImage#getHeight()}.
     */
    @Override
    public int getItemCount() {
        return getSkinnable().getImageHeight();
    }

    /**
     * Invoked when it is possible that the item count has changed. JavaFX may invoke this method
     * when scrolling has occurred, the control has resized, <i>etc.</i>, but for {@link GridView}
     * the count will change only if a new {@link RenderedImage} has been specified.
     */
    @Override
    protected void updateItemCount() {
        /*
         * VirtualFlow.setCellCount(int) indicates the number of cells that should be in the flow.
         * When the cell count changes, VirtualFlow responds by updating the visuals. If the items
         * backing the cells change but the count has not changed, then reconfigureCells() should
         * be invoked instead.
         */
        final VirtualFlow<GridRow> flow = getVirtualFlow();
        flow.setCellCount(getItemCount());                  // Fires event only if count changed.
    }

    // TODO: to update content, invoke getSkinnable().requestLayout().

    /**
     * Called during the layout pass of the scene graph. Current implementation sets the virtual
     * flow size to the given size.
     */
    @Override
    protected void layoutChildren(final double x, final double y, final double width, final double height) {
        /*
         * Super-class only invokes `updateItemCount()` if needed.
         * It does not perform any layout by itself in this method.
         */
        super.layoutChildren(x, y, width, height);
        getVirtualFlow().resizeRelocate(x, y, width, height);
    }
}
