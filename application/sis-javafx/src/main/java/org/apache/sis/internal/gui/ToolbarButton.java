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
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.apache.sis.util.ArraysExt;


/**
 * Description of a button to add in a the {@link org.apache.sis.gui.dataset.DataWindow} toolbar.
 * This class is used only for content-specific buttons; it is not used for buttons managed directly by
 * {@code DataWindow} itself. A {@code ToolbarButton} can create and configure a button with its icon,
 * tooltip text and action to execute when the button is pushed. {@code ToolbarButton} instances exist
 * only temporarily and are discarded after the button has been created and placed in the toolbar.
 *
 * <p>This class is defined in this internal package for allowing interactions between classes
 * in different packages without making toolbar API public.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class ToolbarButton {
    /**
     * The property to use in {@link Node#getProperties()} for storing instances of this class.
     * Values associated to this key shall be arrays of {@code ToolbarButton[]} type.
     */
    private static final String PROPERTY_KEY = "org.apache.sis.gui.ToolbarButton";

    /**
     * Gets and removes the toolbar buttons associated to the given content pane. Those buttons
     * should have been specified by a previous call to {@link #insert(Node, ToolbarButton...)}.
     * They will be requested by {@link org.apache.sis.gui.dataset.DataWindow} only once,
     * which is why we remove them afterward.
     *
     * @param  content  the pane for which to get the toolbar buttons.
     * @return the toolbar buttons (never null, but may be empty).
     */
    public static ToolbarButton[] remove(final Node content) {
        final ToolbarButton[] buttons = (ToolbarButton[]) content.getProperties().remove(PROPERTY_KEY);
        return (buttons != null) ? buttons : new ToolbarButton[0];
    }

    /**
     * Sets the toolbar buttons that the given pane which to have in the data window.
     * If the pane already has buttons, the new one will be inserted before existing ones.
     *
     * @param  content  the pane for which to set the toolbar buttons.
     * @param  buttons  the toolbar buttons to add.
     */
    public static void insert(final Node content, final ToolbarButton... buttons) {
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
     * Creates a button configured with its icon, tooltip and action.
     * The button will be added to the toolbar by the caller.
     *
     * <p>If this {@code ToolbarButton} is an instance of {@link RelatedWindow},
     * then this method does not need to set an action on the button because it
     * will be done by the caller.</p>
     *
     * @param  localized  an instance of {@link Resources} for current locale.
     * @return the button configured with text or icon, tooltip and action.
     */
    public abstract Node createButton(Resources localized);

    /**
     * Convenience method for creating a button.
     *
     * @param  icon       the text to put in the button, as a Unicode emoji.
     * @param  localized  an instance of {@link Resources} for current locale.
     * @param  tooltip    the {@link Resources.Keys} value for the tooltip.
     * @return the button configured with text or icon, tooltip and action.
     */
    protected static Button createButton(final String icon, final Resources localized, final short tooltip) {
        final Button b = new Button(icon);
        b.setTooltip(new Tooltip(localized.getString(tooltip)));
        return b;
    }

    /**
     * A toolbar button for creating and showing a new window related to the window in which the button has been pushed.
     * The button action will create a new instance of {@link org.apache.sis.gui.dataset.DataWindow} which will itself
     * contain a button for going back to the original window.
     */
    public abstract static class RelatedWindow extends ToolbarButton {
        /**
         * For subclass constructors.
         */
        protected RelatedWindow() {
        }

        /**
         * Creates a button configured with its icon, tooltip and action.
         * This button does not need to contain an action; it will be set by the caller.
         *
         * @param  localized  an instance of {@link Resources} for current locale.
         * @return the button configured with text or icon and tooltip.
         */
        @Override
        public abstract Button createButton(Resources localized);

        /**
         * Creates the button for navigation back to the original window.
         * This button does not need to contain an action; it will be set by the caller.
         *
         * @param  localized  an instance of {@link Resources} for current locale.
         * @return the button configured with text or icon and tooltip.
         */
        public abstract Button createBackButton(Resources localized);

        /**
         * Creates the content of the window to show when the user click on the button.
         * This method is invoked only on the first click. For all subsequent clicks,
         * the existing window will be shown again.
         *
         * @return content of the window to show.
         */
        public abstract Region createView();
    }
}
