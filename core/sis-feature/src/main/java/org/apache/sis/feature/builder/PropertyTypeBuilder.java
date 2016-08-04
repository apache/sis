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
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.FeatureAssociationRole;


/**
 * Describes one property of the {@code FeatureType} to be built by an {@code FeatureTypeBuilder}.
 * A different instance of {@code PropertyTypeBuilder} exists for each property to describe.
 * Those instances can be created by:
 *
 * <ul>
 *   <li>{@link FeatureTypeBuilder#addAttribute(Class)}</li>
 *   <li>{@link FeatureTypeBuilder#addAttribute(AttributeType)} for using an existing attribute as a template</li>
 *   <li>{@link FeatureTypeBuilder#addAssociation(FeatureType)}</li>
 *   <li>{@link FeatureTypeBuilder#addAssociation(GenericName)}</li>
 *   <li>{@link FeatureTypeBuilder#addAssociation(FeatureAssociationRole)} for using an existing association as a template</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
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
     * The default value is 1, unless otherwise specified by {@link #setDefaultCardinality(int, int)}.
     *
     * @see #setCardinality(int, int)
     */
    int minimumOccurs;

    /**
     * The maximum number of property values.
     * The default value is 1, unless otherwise specified by {@link #setDefaultCardinality(int, int)}.
     *
     * @see #setCardinality(int, int)
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
     * Creates a new {@code PropertyType} builder initialized to the values of an existing property.
     *
     * @param owner     the builder of the {@code FeatureType} for which to add this property.
     * @param template  an existing property to use as a template, or {@code null} if none.
     */
    PropertyTypeBuilder(final FeatureTypeBuilder owner, final PropertyType template) {
        super(template, owner.getLocale());
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
     * Sets the minimum and maximum number of property values. Those numbers must be equal or greater than zero.
     *
     * <p>If this method is not invoked, then the default values are the cardinality specified by the last call
     * to {@link FeatureTypeBuilder#setDefaultCardinality(int, int)} at the time this instance has been created.
     * If the later method has not been invoked, then the default cardinality is [1 … 1].</p>
     *
     * @param  minimumOccurs  new minimum number of property values.
     * @param  maximumOccurs  new maximum number of property values.
     * @return {@code this} for allowing method calls chaining.
     */
    @SuppressWarnings("unchecked")
    public PropertyTypeBuilder setCardinality(final int minimumOccurs, final int maximumOccurs) {
        if (this.minimumOccurs != minimumOccurs || this.maximumOccurs != maximumOccurs) {
            if (minimumOccurs < 0 || maximumOccurs < minimumOccurs) {
                throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
            }
            this.minimumOccurs = minimumOccurs;
            this.maximumOccurs = maximumOccurs;
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
     * Delegates the creation of a new name to the enclosing builder.
     */
    @Override
    final GenericName name(final String scope, final String localPart) {
        ensureAlive(owner);
        return owner.name(scope, localPart);
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
     * @return the property type.
     * @throws IllegalStateException if the builder contains inconsistent information.
     */
    @Override
    public abstract PropertyType build() throws IllegalStateException;

    /**
     * Flags this builder as a disposed one. The builder should not be used anymore after this method call.
     */
    @Override
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
    public void remove() {
        if (owner != null) {
            owner.replace(this, null);
            dispose();
        }
    }
}
