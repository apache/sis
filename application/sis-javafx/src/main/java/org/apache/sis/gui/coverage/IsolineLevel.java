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

import javafx.scene.paint.Color;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.sis.internal.gui.control.ColorRamp;


/**
 * Colors to apply on isoline for a given level.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class IsolineLevel {
    /**
     * Isolines level.
     */
    public final DoubleProperty value;

    /**
     * Color associated to isoline at this level.
     *
     * The value type is {@link ColorRamp} for now, but if this property become public in a future version
     * then the type should be changed to {@link Color} and bidirectionally binded to another property
     * (package-private) of type {@link ColorRamp}.
     */
    final ObjectProperty<ColorRamp> color;

    /**
     * Whether the isoline should be drawn on the map.
     */
    public final BooleanProperty visible;

    /**
     * Creates an empty isoline level.
     */
    IsolineLevel() {
        value   = new SimpleDoubleProperty  (this, "value", Double.NaN);
        color   = new SimpleObjectProperty<>(this, "color");
        visible = new SimpleBooleanProperty (this, "visible");
    }

    /**
     * Creates an isoline level for the given value.
     */
    IsolineLevel(final double value, final Color color) {
        this();
        this.value.set(value);
        this.color.set(new ColorRamp(color));
        visible.set(true);
    }
}
