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

import java.util.function.Function;
import java.awt.image.RenderedImage;


/**
 * An image property for which the computation is differed.
 * This special kind of properties is recognized by the following methods:
 *
 * <ul>
 *   <li>{@link TiledImage#getProperty(String)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class DeferredProperty {
    /**
     * The value, or {@code null} if not yet computed.
     */
    private Object value;

    /**
     * The function computing the {@linkplain #value}. This function is reset to {@code null}
     * after the value has been computed for allowing the garbage collector to do its work.
     */
    private Function<RenderedImage, ?> provider;

    /**
     * Creates a new deferred property.
     *
     * @param  provider  function computing the {@linkplain #value}.
     */
    public DeferredProperty(final Function<RenderedImage, ?> provider) {
        this.provider = provider;
    }

    /**
     * Returns the property value, which is computed when this method is first invoked.
     *
     * @param  image  the image for which to compute the property value.
     * @return the property value, or {@code null} if it can not be computed.
     */
    final synchronized Object compute(final RenderedImage image) {
        if (value == null) {
            final Function<RenderedImage, ?> p = provider;
            if (p != null) {
                provider = null;            // Clear first in case an exception is thrown below.
                value = p.apply(image);
            }
        }
        return value;
    }
}
