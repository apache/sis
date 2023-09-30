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
package org.apache.sis.image;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.awt.Image;
import java.awt.image.RenderedImage;
import org.apache.sis.util.ArgumentChecks;


/**
 * An image with some properties overwritten by user-specified properties.
 * The property calculations may be deferred, and {@code null} is a valid property value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class UserProperties extends ImageAdapter {
    /**
     * The user-specified properties which may overwrite source image properties.
     * This is a reference to the map specified at construction time, not a copy.
     * No copy is done for allowing the use of instances doing deferred computation.
     * It is legal to have {@code null} value associated to keys: the meaning is not
     * the same as "undefined properties".
     *
     * <p>This {@code UserProperties} class shall not modify the content of this map.</p>
     */
    private final Map<String,Object> properties;

    /**
     * Creates a new wrapper for the given image.
     *
     * @param  source  the image to wrap. The map is retained directly (not cloned).
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    UserProperties(final RenderedImage source, final Map<String,Object> properties) {
        super(source);
        ArgumentChecks.ensureNonNull("properties", properties);
        this.properties = properties;
    }

    /**
     * Returns the names of all supported properties, including in wrapped image.
     * The property names are computed on each invocation for allowing dynamic changes
     * in source image properties or in {@link #properties} map.
     *
     * @return all recognized property names, or {@code null} if none.
     */
    @Override
    public String[] getPropertyNames() {
        String[] names = super.getPropertyNames();
        if (!properties.isEmpty()) {
            final Set<String> union;
            if (names != null) {
                union = new HashSet<>(Arrays.asList(names));
                union.addAll(properties.keySet());
            } else {
                union = properties.keySet();
            }
            names = union.toArray(String[]::new);
        }
        return (names.length != 0) ? names : null;
    }

    /**
     * Gets a property from this image or from its source.
     *
     * @param  name  name of the property to get.
     * @return the property for the given name ({@code null} is a valid result),
     *         or {@link Image#UndefinedProperty} if the given name is not a recognized property name.
     */
    @Override
    public Object getProperty(final String name) {
        Object value = properties.getOrDefault(name, Image.UndefinedProperty);
        if (value == Image.UndefinedProperty) {
            value = super.getProperty(name);
        }
        return value;
    }

    /**
     * Compares the given object with this image for equality. This method should be quick and compare
     * how images compute their values from their sources; it should not compare the actual pixel values.
     */
    @Override
    public boolean equals(final Object object) {
        return super.equals(object) && properties.equals(((UserProperties) object).properties);
    }

    /**
     * Returns a hash code value for this image. This method should be quick.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 71 * properties.hashCode();
    }

    /**
     * Appends a content to show in the {@link #toString()} representation.
     */
    @Override
    Class<? extends ImageAdapter> appendStringContent(final StringBuilder buffer) {
        buffer.append(properties.keySet());
        return UserProperties.class;
    }
}
