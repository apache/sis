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
import javafx.scene.layout.Region;


/**
 * A button in a the toolbar of a {@link org.apache.sis.gui.dataset.DataWindow},
 * other than the common buttons provided by {@code DataWindow} itself.
 * Those button depends on the window content.
 *
 * <p>Current API is for creating a new window of related data. A future version
 * may move that API in a subclass if we need to support other kinds of service.</p>
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
    public static final String PROPERTY_KEY = "org.apache.sis.gui.ToolbarButton";

    /**
     * For subclass constructors.
     */
    protected ToolbarButton() {
    }

    /**
     * Returns the text to show in the button.
     *
     * @return the button text.
     */
    public abstract String getText();

    /**
     * Creates the content of the window to show when the user click on the button.
     * This method is invoked only on the first click. For all subsequent clicks,
     * the existing window will be shown again.
     *
     * @return content of the window to show.
     */
    public abstract Region createView();
}
