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
package org.apache.sis.gui.map.style;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBoxTreeItem;
import org.apache.sis.gui.internal.Resources;


/**
 * Base class of controller for configuring a map item (data and style).
 * An {@code ItemController} contains indirectly the following properties:
 *
 * <ul>
 *   <li>A human-readable {@linkplain MapItem#title title} to show in the <abbr>GUI</abbr>.</li>
 *   <li>A narrative {@linkplain MapItem#description description} providing more details.</li>
 *   <li>Whether the map item {@linkplain #selectedProperty() should be shown} on the map.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ItemController extends CheckBoxTreeItem<MapItem> {
    /**
     * A default configuration panel which said that there is nothing to configure.
     * Created when first needed.
     */
    private Region configurationPanel;

    /**
     * Creates an initially empty controller.
     * The controller is unselected by default.
     */
    public ItemController() {
    }

    /**
     * Creates a controller for the given map item.
     * The controller is unselected by default.
     *
     * @param  item  the map item, or {@code null} if none.
     */
    public ItemController(final MapItem item) {
        super(item);
    }

    /**
     * Returns a panel of JavaFX controls for configuring the map item.
     * This method should create the panel when first requested, then cache it.
     * This method will be invoked from the JavaFX thread.
     *
     * @return the configuration panel for the map item.
     */
    protected Region getConfigurationPanel() {
        if (configurationPanel == null) {
            configurationPanel = createLabel(Resources.Keys.NothingToConfigure);
        }
        return configurationPanel;
    }

    /**
     * Creates a panel showing a label such a "No selected item".
     *
     * @param  label  one of the {@link Resources} constants.
     * @return the label to show.
     */
    static Region createLabel(final short label) {
        final var box = new VBox(new Label(Resources.format(label)));
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
