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
import java.util.List;
import java.util.Objects;
import org.opengis.style.StyleVisitor;
import org.opengis.style.Symbolizer;

/**
 * Mutable implementation of {@link org.opengis.style.Style}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Style implements org.opengis.style.Style {

    private String name;
    private Description description;
    private boolean isDefault;
    private final List<FeatureTypeStyle> fts = new ArrayList<>();
    private Symbolizer defaultSymbolizer;

    public Style() {
    }

    public Style(String name, Description description, boolean isDefault, List<FeatureTypeStyle> fts, Symbolizer defaultSymbolizer) {
        this.name = name;
        this.description = description;
        this.isDefault = isDefault;
        if (fts!=null) this.fts.addAll(fts);
        this.defaultSymbolizer = defaultSymbolizer;
    }

    @Override
    public List<FeatureTypeStyle> featureTypeStyles() {
        return fts;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public Symbolizer getDefaultSpecification() {
        return defaultSymbolizer;
    }

    public void setDefaultSpecification(Symbolizer defaultSymbolizer) {
        this.defaultSymbolizer = defaultSymbolizer;
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
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, fts);
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
        final Style other = (Style) obj;
        return Objects.equals(this.name, other.name)
            && Objects.equals(this.description, other.description)
            && Objects.equals(this.fts, other.fts);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Style castOrCopy(org.opengis.style.Style candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Style) {
            return (Style) candidate;
        }

        final List<FeatureTypeStyle> cs = new ArrayList<>();
        for (org.opengis.style.FeatureTypeStyle cr : candidate.featureTypeStyles()) {
            cs.add(FeatureTypeStyle.castOrCopy(cr));
        }
        return new Style(
                candidate.getName(),
                Description.castOrCopy(candidate.getDescription()),
                candidate.isDefault(),
                cs,
                org.apache.sis.internal.style.Symbolizer.tryCastOrCopy(candidate.getDefaultSpecification())
                );
    }
}
