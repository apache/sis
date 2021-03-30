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
package org.apache.sis.internal.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opengis.filter.ResourceId;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.style.Description;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.Rule;
import org.opengis.style.SemanticType;
import org.opengis.style.StyleVisitor;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MockFeatureTypeStyle implements FeatureTypeStyle {

    private String name;
    private Description description;
    private final Set<GenericName> featureTypeNames = new HashSet<>();
    private final Set<SemanticType> semanticTypes = new HashSet<>();
    private final List<Rule> rules = new ArrayList<>();
    private OnlineResource onlineResource;

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
    public Set<GenericName> featureTypeNames() {
        return featureTypeNames;
    }

    @Override
    public Set<SemanticType> semanticTypeIdentifiers() {
        return semanticTypes;
    }

    @Override
    public List<Rule> rules() {
        return rules;
    }

    @Override
    public OnlineResource getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(OnlineResource onlineResource) {
        this.onlineResource = onlineResource;
    }

    /**
     * May be removed from GeoAPI.
     */
    @Override
    @Deprecated
    public ResourceId getFeatureInstanceIDs() {
        throw new UnsupportedOperationException();
    }

    /**
     * May be removed from GeoAPI.
     */
    @Override
    @Deprecated
    public Object accept(StyleVisitor visitor, Object extraData) {
        throw new UnsupportedOperationException();
    }
}
