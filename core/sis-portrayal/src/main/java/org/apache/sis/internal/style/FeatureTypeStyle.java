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
package org.apache.sis.internal.style;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opengis.filter.ResourceId;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.style.SemanticType;
import org.opengis.style.StyleVisitor;
import org.opengis.util.GenericName;

/**
 * Mutable implementation of {@link org.opengis.style.FeatureTypeStyle}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class FeatureTypeStyle implements org.opengis.style.FeatureTypeStyle {

    private String name;
    private Description description;
    private ResourceId featureInstanceIDs;
    private final Set<GenericName> featureTypeNames = new HashSet<>();
    private final Set<SemanticType> semanticTypeIdentifiers = new HashSet<>();
    private final List<Rule> rules = new ArrayList<>();
    private OnlineResource onlineResource;

    public FeatureTypeStyle() {
    }

    public FeatureTypeStyle(String name, Description description, ResourceId resourceId, Set<GenericName> featureTypeNames, Set<SemanticType> semanticTypeIdentifiers, List<Rule> rules, OnlineResource OnlineResource) {
        this.name = name;
        this.description = description;
        this.featureInstanceIDs = resourceId;
        if (featureTypeNames != null) this.featureTypeNames.addAll(featureTypeNames);
        if (semanticTypeIdentifiers != null) this.semanticTypeIdentifiers.addAll(semanticTypeIdentifiers);
        if (rules != null) this.rules.addAll(rules);
        this.onlineResource = OnlineResource;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Description getDescription() {
        return description;
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    @Override
    public ResourceId getFeatureInstanceIDs() {
        return featureInstanceIDs;
    }

    public void setFeatureInstanceIDs(ResourceId featureInstanceIDs) {
        this.featureInstanceIDs = featureInstanceIDs;
    }

    @Override
    public Set<GenericName> featureTypeNames() {
        return featureTypeNames;
    }

    @Override
    public Set<SemanticType> semanticTypeIdentifiers() {
        return semanticTypeIdentifiers;
    }

    @Override
    public List<Rule> rules() {
        return rules;
    }

    @Override
    public OnlineResource getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(OnlineResource OnlineResource) {
        this.onlineResource = OnlineResource;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, featureInstanceIDs, featureTypeNames, semanticTypeIdentifiers, rules, onlineResource);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FeatureTypeStyle other = (FeatureTypeStyle) obj;
        return Objects.equals(this.name, other.name)
            && Objects.equals(this.description, other.description)
            && Objects.equals(this.featureInstanceIDs, other.featureInstanceIDs)
            && Objects.equals(this.featureTypeNames, other.featureTypeNames)
            && Objects.equals(this.semanticTypeIdentifiers, other.semanticTypeIdentifiers)
            && Objects.equals(this.rules, other.rules)
            && Objects.equals(this.onlineResource, other.onlineResource);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static FeatureTypeStyle castOrCopy(org.opengis.style.FeatureTypeStyle candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof FeatureTypeStyle) {
            return (FeatureTypeStyle) candidate;
        }

        final List<Rule> rules = new ArrayList<>();
        for (org.opengis.style.Rule cr : candidate.rules()) {
            rules.add(Rule.castOrCopy(cr));
        }

        return new FeatureTypeStyle(
                candidate.getName(),
                Description.castOrCopy(candidate.getDescription()),
                candidate.getFeatureInstanceIDs(),
                candidate.featureTypeNames(),
                candidate.semanticTypeIdentifiers(),
                rules,
                candidate.getOnlineResource()
        );
    }
}
