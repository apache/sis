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
package org.apache.sis.internal.coverage.j2d;

import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;


/**
 * Information about the color model. This enumeration provides an easier way of determining
 * whether the color ramp <em>can</em> be replaced, and whether it <em>should</em> be replaced
 * for performance reasons.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public enum ColorModelType {
    /**
     * Color model uses directly RGB colors.
     * This model is efficient and should not be changed.
     * Color palette can not be changed.
     */
    DIRECT(false, false),

    /**
     * Color model uses indexed colors.
     * This model is efficient and does not need to be changed.
     * Color palette can be changed.
     */
    INDEXED(true, false),

    /**
     * Color model use colors computed on the fly from floating point values.
     * This model is inefficient and should be changed if possible.
     */
    SCALED(true, true),

    /**
     * Unrecognized color model.
     */
    OTHER(false, false);

    /**
     * Whether the color model uses a color palette.
     * A {@code true} value implies that the color ramp is replaceable.
     */
    public final boolean useColorRamp;

    /**
     * Whether rendering with this color model is slow.
     * In such case, the color model may need to be changed using {@link Colorizer}.
     */
    public final boolean isSlow;

    /**
     * Creates a new enumeration value.
     */
    private ColorModelType(final boolean useColorRamp, final boolean isSlow) {
        this.useColorRamp = useColorRamp;
        this.isSlow = isSlow;
    }

    /**
     * Gets the type of given color model.
     *
     * @param  model  the color model (may be {@code null}).
     * @return type of given color model (never {@code null}).
     */
    public static ColorModelType find(final ColorModel model) {
        if (model != null) {
            if (model instanceof DirectColorModel) {
                return DIRECT;
            }
            if (model instanceof IndexColorModel) {
                return INDEXED;
            }
            if (model.getColorSpace() instanceof ScaledColorSpace) {
                return SCALED;
            }
        }
        return OTHER;
    }
}
