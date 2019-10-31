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

import javafx.scene.paint.Color;


/**
 * A central place where to store all colors used by SIS application.
 * This provides a single place to revisit if we learn more about how
 * to make those color more dynamic with JavaFX styling.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class Styles {
    /**
     * Usual color of text.
     */
    public static final Color NORMAL_TEXT = Color.BLACK;

    /**
     * Color of text in a selection.
     */
    public static final Color SELECTED_TEXT = Color.WHITE;

    /**
     * Color of the text saying that data are in process of being loaded.
     */
    public static final Color LOADING_TEXT = Color.BLUE;

    /**
     * Color of text shown in place of data that we failed to load.
     */
    public static final Color ERROR_TEXT = Color.RED;

    /**
     * Do not allow instantiation of this class.
     */
    private Styles() {
    }
}
