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
import java.util.List;
import org.opengis.style.Description;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.StyleVisitor;
import org.opengis.style.Symbolizer;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MockStyle implements org.opengis.style.Style {

    private String name;
    private Description description;
    private final List<FeatureTypeStyle> fts = new ArrayList<>();

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
    public List<FeatureTypeStyle> featureTypeStyles() {
        return fts;
    }

    /**
     * Will likely be removed from geoapi.
     */
    @Deprecated
    @Override
    public boolean isDefault() {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Will likely be removed from geoapi.
     */
    @Deprecated
    @Override
    public Symbolizer getDefaultSpecification() {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Will likely be removed from geoapi.
     */
    @Deprecated
    @Override
    public Object accept(StyleVisitor sv, Object o) {
        throw new UnsupportedOperationException("Not supported.");
    }

}
