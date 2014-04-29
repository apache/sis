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
package org.apache.sis.feature;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import com.esri.core.geometry.Geometry;
import org.apache.sis.util.ArgumentChecks;


/**
 * An instance of {@linkplain DefaultFeatureType feature type} containing values for a real-world phenomena.
 *
 * @author  Travis L. Pinney
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class DefaultFeature {
    /**
     * The properties (attributes, operations, feature associations) of this feature.
     */
    private final Map<String, DefaultAttribute<?>> properties;

    private Geometry geom;

    /**
     * Creates a new features.
     */
    public DefaultFeature() {
        properties = new HashMap<>();
    }

    /**
     * Returns the value of the attribute of the given name.
     *
     * @param  name The attribute name.
     * @return The value for the given attribute, or {@code null} if none.
     */
    public Object getAttributeValue(final String name) {
        final DefaultAttribute<?> attribute = properties.get(name);
        return (attribute != null) ? attribute.getValue() : null;
    }

    /**
     * Sets the value of the attribute of the given name.
     *
     * @param name The attribute name.
     * @param value The new value for the given attribute.
     */
    @SuppressWarnings("unchecked")
    public void setAttributeValue(final String name, final Object value) {
        DefaultAttribute<?> attribute = properties.get(name);
        if (attribute == null) {
            if (value == null) {
                return;
            }
            // TEMPORARY HACK: FeatureType should be defined at construction time.
            final DefaultAttributeType<Object> type = new DefaultAttributeType<>(
                    Collections.singletonMap("name", name), Object.class, null, null);
            attribute = new DefaultAttribute<>(type);
        }
        ArgumentChecks.ensureCanCast(name, attribute.getType().getValueClass(), value);
        ((DefaultAttribute) attribute).setValue(value);
    }

    public Geometry getGeom() {
        return geom;
    }

    public void setGeom(Geometry geom) {
        this.geom = geom;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String lineSeparator = System.lineSeparator();
        for (final DefaultAttribute<?> attribute : properties.values()) {
            sb.append(attribute.getType().getName()).append(": ").append(attribute.getValue()).append(lineSeparator);
        }
        return sb.toString();
    }
}
