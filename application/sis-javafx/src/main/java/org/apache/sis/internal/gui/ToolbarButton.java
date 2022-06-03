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
package org.apache.sis.internal.gui;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import org.apache.sis.util.ArraysExt;


/**
 * Builder for a button to add in a the toolbar of a {@link org.apache.sis.gui.dataset} window.
 * This class is used only for content-specific buttons; it is not used for all buttons created
 * by the {@code dataset} package. A {@code ToolbarButton} can create and configure a button with its icon,
 * tooltip text and action to execute when the button is pushed.
 *
 * <p>This class is defined in this internal package for allowing interactions between classes
 * in different packages without making toolbar API public.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public abstract class ToolbarButton implements EventHandler<ActionEvent> {
    /**
     * The property to use in {@link Node#getProperties()} for storing instances of this class.
     * Values associated to this key shall be arrays of {@code Control[]} type.
     */
    private static final String PROPERTY_KEY = "org.apache.sis.gui.ToolbarButtons";

    /**
     * Gets and removes the toolbar buttons associated to the given content pane. Those buttons
     * should have been specified by a previous call to {@link #insert(Node, Control...)}.
     * They will be requested by {@link org.apache.sis.gui.dataset.WindowHandler} only once,
     * which is why we remove them afterward.
     *
     * @param  content  the pane for which to get the toolbar buttons.
     * @return the toolbar buttons (never null, but may be empty).
     */
    public static Control[] remove(final Node content) {
        final Control[] buttons = (Control[]) content.getProperties().remove(PROPERTY_KEY);
        return (buttons != null) ? buttons : new Control[0];
    }

    /**
     * Sets the toolbar buttons that the given pane wants to have in the data window.
     * If the pane already has buttons, the new ones will be inserted before existing ones.
     *
     * @param  content  the pane for which to set the toolbar buttons.
     * @param  buttons  the toolbar buttons to add.
     */
    public static void insert(final Node content, final Control... buttons) {
        content.getProperties().merge(PROPERTY_KEY, buttons, ToolbarButton::prepend);
    }

    /**
     * Invoked if toolbar buttons already exist for a pane, in which case the new ones
     * are inserted before the existing ones.
     */
    private static Object prepend(final Object oldValue, final Object newValue) {
        return ArraysExt.append((ToolbarButton[]) newValue, (ToolbarButton[]) oldValue);
    }

    /**
     * For subclass constructors.
     */
    protected ToolbarButton() {
    }

    /**
     * Convenience method for creating a button.
     * The action handler will be {@code this}.
     *
     * @param  group      the group of the toggle button.
     * @param  icon       the text to put in the button, as a Unicode emoji.
     * @param  localized  an instance of {@link Resources} for current locale.
     * @param  tooltip    the {@link Resources.Keys} value for the tooltip.
     * @return the button configured with text or icon, tooltip and action.
     */
    public final ToggleButton createButton(final ToggleGroup group, final String icon, final Resources localized, final short tooltip) {
        final ToggleButton tb = new ToggleButton(icon);
        tb.setToggleGroup(group);
        tb.setTooltip(new Tooltip(localized.getString(tooltip)));
        tb.setOnAction(this);
        return tb;
    }

    /**
     * Invoked when the user pushed the button.
     *
     * @param  event  the pushed button event.
     */
    @Override
    public abstract void handle(ActionEvent event);
}
