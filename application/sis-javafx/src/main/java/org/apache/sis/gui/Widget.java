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
package org.apache.sis.gui;

import java.util.Locale;
import javafx.scene.layout.Region;
import org.apache.sis.util.Localized;


/**
 * Base class of user interfaces provided by Apache SIS.
 * This base class is used for components that encapsulate JavaFX controls instead of extending them.
 * We use this indirection level for hiding implementation details such as the exact JavaFX classes used
 * for implementing the widget.
 *
 * <h2>Other controls</h2>
 * Not all Apache SIS widgets extent this class.
 * Other widgets extending directly a JavaFX control or other classes are
 * {@link org.apache.sis.gui.metadata.MetadataTree},
 * {@link org.apache.sis.gui.dataset.ResourceTree},
 * {@link org.apache.sis.gui.dataset.FeatureTable},
 * {@link org.apache.sis.gui.coverage.GridView},
 * {@link org.apache.sis.gui.referencing.CRSChooser} and
 * {@link org.apache.sis.gui.map.MapCanvas}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public abstract class Widget implements Localized {
    /**
     * Creates a new widget.
     */
    protected Widget() {
    }

    /**
     * Returns the encapsulated JavaFX component to add in a scene graph for making the widget visible.
     * The {@code Region} subclass is implementation dependent and may change in any future SIS version.
     *
     * @return the JavaFX component to insert in a scene graph.
     */
    public abstract Region getView();

    /**
     * Returns the locale for controls and messages. This is usually the
     * {@linkplain Locale#getDefault() default locale} but some widgets allow alternative locale.
     * This is indicative; there is no guarantee that this locale will be honored by all controls.
     *
     * @return the locale for controls in this widget.
     *
     * @since 1.2
     */
    @Override
    public Locale getLocale() {
        return Locale.getDefault(Locale.Category.DISPLAY);
    }
}
