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
import org.apache.sis.feature.Features;
import org.apache.sis.feature.DefaultAssociationRole;

// Specific to the main branch:
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Describes one association from the {@code FeatureType} to be built by an {@code FeatureTypeBuilder} to another
 * {@code FeatureType}. A different instance of {@code AssociationRoleBuilder} exists for each feature association
 * to describe. Those instances are created preferably by {@code FeatureTypeBuilder.addAssociation(FeatureType)},
 * or in case of cyclic reference by {@code FeatureTypeBuilder.addAssociation(GenericName)}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see org.apache.sis.feature.DefaultAssociationRole
 * @see FeatureTypeBuilder#addAssociation(DefaultFeatureType)
 * @see FeatureTypeBuilder#addAssociation(GenericName)
 *
 * @since 0.8
 */
public final class AssociationRoleBuilder extends PropertyTypeBuilder {
    /**
     * The target feature type, or {@code null} if unknown.
     */
    private final DefaultFeatureType type;

    /**
     * Name of the target feature type (never null).
     */
    private final GenericName typeName;

    /**
     * The association created by this builder, or {@code null} if not yet created.
     * This field must be cleared every time that a setter method is invoked on this builder.
     */
    private transient DefaultAssociationRole property;

    /**
     * Creates a new {@code AssociationRole} builder for values of the given type.
     * The {@code type} argument can be null if unknown, but {@code typeName} is mandatory.
     *
     * @param owner  the builder of the {@code FeatureType} for which to add this property.
     */
    AssociationRoleBuilder(final FeatureTypeBuilder owner, final DefaultFeatureType type, final GenericName typeName) {
        super(owner);
        this.type     = type;
        this.typeName = typeName;
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder initialized to the values of an existing association.
     *
     * @param owner  the builder of the {@code FeatureType} for which to add this property.
     */
    AssociationRoleBuilder(final FeatureTypeBuilder owner, final DefaultAssociationRole template) {
        super(owner);
        property      = template;
        minimumOccurs = template.getMinimumOccurs();
        maximumOccurs = template.getMaximumOccurs();
        if (!template.isResolved()) {
            type     = null;
            typeName = Features.getValueTypeName(template);
        } else {
            type     = template.getValueType();
            typeName = type.getName();
        }
        initialize(template);
    }

    /**
     * If the {@code FeatureAssociationRole} created by the last call to {@link #build()} has been cached,
     * clears that cache. This method must be invoked every time that a setter method is invoked.
     */
    @Override
    final void clearCache() {
        property = null;
        super.clearCache();
    }

    /**
     * Appends a text inside the value returned by {@link #toString()}, before the closing bracket.
     */
    @Override
    final void toStringInternal(final StringBuilder buffer) {
        buffer.append(" → ").append(typeName);
    }

    /**
     * Returns a default name to use if the user did not specify a name. The first letter will be changed to
     * lower case (unless the name looks like an acronym) for compliance with Java convention on property names.
     */
    @Override
    final String getDefaultName() {
        return typeName.tip().toString();
    }

    /**
     * Sets the {@code FeatureAssociationRole} name as a generic name.
     * If another name was defined before this method call, that previous value will be discarded.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AssociationRoleBuilder setName(final GenericName name) {
        super.setName(name);
        return this;
    }

    /**
     * Sets the {@code FeatureAssociationRole} name as a simple string (local name).
     * The namespace will be the value specified by the last call to {@link FeatureTypeBuilder#setNameSpace(CharSequence)},
     * but that namespace will not be visible in the {@linkplain org.apache.sis.util.iso.DefaultLocalName#toString()
     * string representation} unless the {@linkplain org.apache.sis.util.iso.DefaultLocalName#toFullyQualifiedName()
     * fully qualified name} is requested.
     *
     * <p>This convenience method creates a {@link org.opengis.util.LocalName} instance from
     * the given {@code CharSequence}, then delegates to {@link #setName(GenericName)}.</p>
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AssociationRoleBuilder setName(final CharSequence localPart) {
        super.setName(localPart);
        return this;
    }

    /**
     * Sets the {@code FeatureAssociationRole} name as a string in the given scope.
     * The {@code components} array must contain at least one element.
     * The last component (the {@linkplain org.apache.sis.util.iso.DefaultScopedName#tip() tip}) will be sufficient
     * in many cases for calls to the {@link org.apache.sis.feature.AbstractFeature#getProperty(String)} method.
     * The other elements before the last one are optional and can be used for resolving ambiguity.
     * They will be visible as the name {@linkplain org.apache.sis.util.iso.DefaultScopedName#path() path}.
     *
     * <div class="note"><b>Example:</b>
     * a call to {@code setName("A", "B", "C")} will create a "A:B:C" name.
     * An association built with this name can be obtained from a feature by a call to {@code feature.getProperty("C")}
     * if there is no ambiguity, or otherwise by a call to {@code feature.getProperty("B:C")} (if non-ambiguous) or
     * {@code feature.getProperty("A:B:C")}.</div>
     *
     * In addition to the path specified by the {@code components} array, the name may also contain
     * a namespace specified by the last call to {@link FeatureTypeBuilder#setNameSpace(CharSequence)}.
     * But contrarily to the specified components, the namespace will not be visible in the name
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#toString() string representation} unless the
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#toFullyQualifiedName() fully qualified name} is requested.
     *
     * <p>This convenience method creates a {@link org.opengis.util.LocalName} or {@link org.opengis.util.ScopedName}
     * instance depending on whether the {@code names} array contains exactly 1 element or more than 1 element, then
     * delegates to {@link #setName(GenericName)}.</p>
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AssociationRoleBuilder setName(final CharSequence... components) {
        super.setName(components);
        return this;
    }

    /**
     * Sets the minimum number of associations. If the given number is greater than the
     * {@linkplain #getMaximumOccurs() maximal number} of associations, than the maximum
     * is also set to that value.
     *
     * @param  occurs  the new minimum number of associations.
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AssociationRoleBuilder setMinimumOccurs(final int occurs) {
        super.setMinimumOccurs(occurs);
        return this;
    }

    /**
     * Sets the maximum number of associations. If the given number is less than the
     * {@linkplain #getMinimumOccurs() minimal number} of associations, than the minimum
     * is also set to that value. {@link Integer#MAX_VALUE} means that there is no maximum.
     *
     * @param  occurs  the new maximum number of associations.
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AssociationRoleBuilder setMaximumOccurs(final int occurs) {
        super.setMaximumOccurs(occurs);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AssociationRoleBuilder setDefinition(final CharSequence definition) {
        super.setDefinition(definition);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AssociationRoleBuilder setDesignation(final CharSequence designation) {
        super.setDesignation(designation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AssociationRoleBuilder setDescription(final CharSequence description) {
        super.setDescription(description);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AssociationRoleBuilder setDeprecated(final boolean deprecated) {
        super.setDeprecated(deprecated);
        return this;
    }

    /**
     * Builds the association role from the information specified to this builder.
     * If a role has already been built and this builder state has not changed since the role creation,
     * then the previously created {@code FeatureAssociationRole} instance is returned.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed to the
     * {@code org.opengis.feature.FeatureAssociationRole} interface. This change is pending GeoAPI revision.</div>
     *
     * @return the association role.
     */
    @Override
    public DefaultAssociationRole build() {
        if (property == null) {
            if (type != null) {
                property = new DefaultAssociationRole(identification(), type, minimumOccurs, maximumOccurs);
            } else {
                property = new DefaultAssociationRole(identification(), typeName, minimumOccurs, maximumOccurs);
            }
        }
        return property;
    }
}
