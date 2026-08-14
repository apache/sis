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
package org.apache.sis.scene.light;

import java.awt.Color;
import org.apache.sis.util.ArgumentChecks;

/**
 * A light emits illumination into the scene. Position and orientation are
 * not carried by the light itself but by the {@link SceneNode} it is
 * attached to (translation is the light position, local -Z is the emission
 * direction, local +Y is up), the same way a {@link Camera} is placed.
 *
 * {@link Directional}, {@link Point} and {@link Spot} follow the Khronos
 * glTF {@code KHR_lights_punctual} extension naming and defaults.
 * {@link Quad}, {@link Ring} and {@link HDRI} have no glTF punctual-light
 * equivalent; they exist to reach feature parity with the Khronos ANARI
 * light model (area and environment lighting).
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract sealed class Light permits Directional, Point, Spot, Quad, Ring, HDRI {

    private Color color = Color.WHITE;
    private boolean visible = true;

    Light() {
    }

    /**
     * Unitless color factor which filters the emitted light.
     * Default is WHITE (1,1,1).
     * @return light color, never null.
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color light color, not null
     */
    public void setColor(Color color) {
        ArgumentChecks.ensureNonNull("color", color);
        this.color = color;
    }

    /**
     * Whether the light itself can be directly seen (only meaningful for
     * area lights such as {@link Quad} and {@link Ring}).
     * Default value is true.
     * @return visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible whether the light can be directly seen
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
