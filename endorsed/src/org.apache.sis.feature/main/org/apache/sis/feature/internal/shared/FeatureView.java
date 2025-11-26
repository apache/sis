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
package org.apache.sis.feature.internal.shared;

import org.apache.sis.feature.AbstractFeature;

// Specific to the main branch:
import org.apache.sis.feature.DefaultFeatureType;


/**
 * A feature containing a subset of the properties of another feature.
 * The feature type of the view must be specified in argument together with the source feature instance.
 * All properties that are present in this feature view shall have the same name as in the source feature.
 * This class does not verify that requirement.
 *
 * <h2>Limitations</h2>
 * For performance and simplicity reasons, the current implementation does not prevent users
 * from requesting a property that exists in the full feature instance but not in this subset.
 *
 * <h2>Possible evolution</h2>
 * It would be possible for the view to contain operations that are not present in the source features.
 * This extension has not yet been implemented because not yet needed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FeatureView extends AbstractFeature {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4299168913599597991L;

    /**
     * The instance with all properties.
     */
    @SuppressWarnings("serial")     // Apache SIS implementations are serializable.
    private final AbstractFeature source;

    /**
     * Creates a new feature instance which is a subset of the given {@code source} feature.
     * This constructor does not verify the consistency of the two arguments.
     *
     * @param  subset  the feature type that describes the feature subset.
     * @param  source  the complete feature instance to view as a subset.
     */
    FeatureView(final DefaultFeatureType subset, final AbstractFeature source) {
        super(subset);
        this.source = source;
    }

    /**
     * Returns the property (attribute, feature association or operation result) of the given name.
     * This method delegates to the wrapped source without checking whether the given name exists in this subset.
     *
     * @param  name  the property name.
     * @return the property of the given name (never {@code null}).
     */
    @Override
    public Object getProperty(final String name) {
        return source.getProperty(name);
    }

    /**
     * Sets the property (attribute or feature association).
     * This method delegates to the wrapped source without checking whether the given name exists in this subset.
     *
     * @param  property  the property to set.
     */
    @Override
    public void setProperty(final Object property) {
        source.setProperty(property);
    }

    /**
     * Returns the value for the property of the given name.
     * This method delegates to the wrapped source without checking whether the given name exists in this subset.
     *
     * @param  name  the property name.
     * @return value of the specified property, or the default value (which may be {@code null}} if none.
     */
    @Override
    public Object getPropertyValue(final String name) {
        return source.getPropertyValue(name);
    }

    /**
     * Sets the value for the property of the given name.
     * This method delegates to the wrapped source without checking whether the given name exists in this subset.
     *
     * @param  name   the property name.
     * @param  value  the new value for the specified property (may be {@code null}).
     */
    @Override
    public void setPropertyValue(final String name, final Object value) {
        source.setPropertyValue(name, value);
    }

    /**
     * Returns the value for the property of the given name if that property exists, or a fallback value otherwise.
     * This method delegates to the wrapped source without checking whether the given name exists in this subset.
     *
     * <h4>Design note</h4>
     * We could add a verification of whether the property exists in the feature type given by {@link #getType()}.
     * We don't do that for now because the current usages of this method in the Apache SIS code base do not need
     * this method to be strict, and for consistency with the behavior of other methods in this class.
     *
     * @param  name  the property name.
     * @param  missingPropertyFallback  the value to return if no attribute or association of the given name exists.
     * @return value or default value of the specified property, or {@code missingPropertyFallback}.
     *
     * @deprecated Experience suggests that this method encourage bugs in user's code that stay unnoticed.
     */
    @Override
    @Deprecated(since = "1.5", forRemoval = true)
    public Object getValueOrFallback(final String name, final Object missingPropertyFallback) {
        return source.getValueOrFallback(name, missingPropertyFallback);
    }
}
