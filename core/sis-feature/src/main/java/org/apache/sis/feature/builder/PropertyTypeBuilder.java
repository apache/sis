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
package org.apache.sis.feature.builder;

import org.opengis.util.GenericName;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.feature.AbstractIdentifiedType;


/**
 * Describes one property of the {@code FeatureType} to be built by an {@code FeatureTypeBuilder}.
 * A different instance of {@code PropertyTypeBuilder} exists for each property to describe.
 * Those instances can be created by:
 *
 * <ul>
 *   <li>{@link FeatureTypeBuilder#addAttribute(Class)}</li>
 *   <li>{@link FeatureTypeBuilder#addAttribute(DefaultAttributeType)} for using an existing attribute as a template</li>
 *   <li>{@link FeatureTypeBuilder#addAssociation(DefaultFeatureType)}</li>
 *   <li>{@link FeatureTypeBuilder#addAssociation(GenericName)}</li>
 *   <li>{@link FeatureTypeBuilder#addAssociation(DefaultAssociationRole)} for using an existing association as a template</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class PropertyTypeBuilder extends TypeBuilder {
    /**
     * The feature type builder instance that created this {@code PropertyTypeBuilder}.
     * This is set at construction time and considered as immutable until it is set to {@code null}.
     *
     * @see #owner()
     */
    private FeatureTypeBuilder owner;

    /**
     * The minimum number of property values.
     * The default value is 1, unless otherwise specified by {@link #setMinimumOccurs(int)}.
     *
     * @see #getMinimumOccurs()
     */
    int minimumOccurs;

    /**
     * The maximum number of property values.
     * The default value is 1, unless otherwise specified by {@link #setMaximumOccurs(int)}.
     *
     * @see #getMaximumOccurs()
     */
    int maximumOccurs;

    /**
     * Creates a new builder initialized to the values of the given builder.
     * This constructor is for {@link AttributeTypeBuilder#setValueClass(Class)} implementation.
     *
     * @param builder  the builder from which to copy values.
     */
    PropertyTypeBuilder(final PropertyTypeBuilder builder) {
        super(builder);
        owner         = builder.owner;
        minimumOccurs = builder.minimumOccurs;
        maximumOccurs = builder.maximumOccurs;
        // Do not copy the 'property' reference since the 'valueClass' is different.
    }

    /**
     * Creates a new {@code PropertyType} builder.
     *
     * @param owner  the builder of the {@code FeatureType} for which to add this property.
     */
    PropertyTypeBuilder(final FeatureTypeBuilder owner) {
        super(owner.getLocale());
        this.owner    = owner;
        minimumOccurs = owner.defaultMinimumOccurs;
        maximumOccurs = owner.defaultMaximumOccurs;
    }

    /**
     * Returns the feature type builder instance that created this {@code PropertyTypeBuilder}.
     */
    final FeatureTypeBuilder owner() {
        ensureAlive(owner);
        return owner;
    }

    /**
     * Returns the minimum number of property values.
     * The returned value is greater than or equal to zero.
     *
     * @return the minimum number of property values.
     *
     * @see org.apache.sis.feature.DefaultAttributeType#getMinimumOccurs()
     */
    public int getMinimumOccurs() {
        return minimumOccurs;
    }

    /**
     * Sets the minimum number of property values. If the given number is greater than the
     * {@linkplain #getMaximumOccurs() maximal number} of property values, than the maximum
     * is also set to that value.
     *
     * @param  occurs  the new minimum number of property values.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getMinimumOccurs()
     */
    public PropertyTypeBuilder setMinimumOccurs(final int occurs) {
        if (occurs != minimumOccurs) {
            if (occurs < 0) {
                throw new IllegalArgumentException(errors().getString(Errors.Keys.NegativeArgument_2, "occurs", occurs));
            }
            minimumOccurs = occurs;
            if (occurs > maximumOccurs) {
                maximumOccurs = occurs;
            }
            clearCache();
        }
        return this;
    }

    /**
     * Returns the maximum number of property values.
     * The returned value is greater than or equal to the {@link #getMinimumOccurs()} value.
     * If there is no maximum, then this method returns {@link Integer#MAX_VALUE}.
     *
     * @return the maximum number of property values, or {@link Integer#MAX_VALUE} if none.
     *
     * @see org.apache.sis.feature.DefaultAttributeType#getMaximumOccurs()
     */
    public final int getMaximumOccurs() {
        return maximumOccurs;
    }

    /**
     * Sets the maximum number of property values. If the given number is less than the
     * {@linkplain #getMinimumOccurs() minimal number} of property values, than the minimum
     * is also set to that value.
     *
     * @param  occurs  the new maximum number of property values.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getMaximumOccurs()
     */
    public PropertyTypeBuilder setMaximumOccurs(final int occurs) {
        if (occurs != maximumOccurs) {
            if (occurs < 0) {
                throw new IllegalArgumentException(errors().getString(Errors.Keys.NegativeArgument_2, "occurs", occurs));
            }
            maximumOccurs = occurs;
            if (occurs < minimumOccurs) {
                minimumOccurs = occurs;
            }
            clearCache();
        }
        return this;
    }

    /**
     * Returns {@code true} if {@link AttributeRole#IDENTIFIER_COMPONENT} has been associated to this property.
     */
    boolean isIdentifier() {
        return false;
    }

    /**
     * Creates a local name in the {@linkplain FeatureTypeBuilder#setNameSpace feature namespace}.
     */
    @Override
    final GenericName createLocalName(final CharSequence name) {
        ensureAlive(owner);
        return owner.createLocalName(name);
    }

    /**
     * Creates a generic name in the {@linkplain FeatureTypeBuilder#setNameSpace feature namespace}.
     */
    @Override
    final GenericName createGenericName(final CharSequence... names) {
        ensureAlive(owner);
        return owner.createGenericName(names);
    }

    /**
     * If the {@code PropertyType} created by the last call to {@link #build()} has been cached,
     * clears that cache. This method must be invoked every time that a setter method is invoked.
     */
    @Override
    void clearCache() {
        ensureAlive(owner);
        owner.clearCache();
    }

    /**
     * Builds the property type from the information specified to this builder.
     * If a type has already been built and this builder state has not changed since the type creation,
     * then the previously created {@code PropertyType} instance is returned
     * (see {@link AttributeTypeBuilder#build()} for more information).
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.PropertyType}. This change is pending GeoAPI revision.</div>
     *
     * @return the property type.
     * @throws IllegalStateException if the builder contains inconsistent information.
     */
    @Override
    public abstract AbstractIdentifiedType build() throws IllegalStateException;

    /**
     * Flags this builder as a disposed one. The builder should not be used anymore after this method call.
     */
    final void dispose() {
        owner = null;
    }

    /**
     * Removes this property from the {@code FeatureTypeBuilder}.
     * After this method has been invoked, this {@code PropertyTypeBuilder} instance
     * is no longer in the list returned by {@link FeatureTypeBuilder#properties()}
     * and attempts to invoke any setter method on {@code this} will cause an
     * {@link IllegalStateException} to be thrown.
     */
    @Override
    public void remove() {
        if (owner != null) {
            owner.replace(this, null);
            dispose();
        }
    }
}
