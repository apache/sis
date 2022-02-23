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
import org.opengis.filter.Filter;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.style.StyleVisitor;
import org.opengis.style.Symbolizer;

/**
 * Mutable implementation of {@link org.opengis.style.Rule}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Rule implements org.opengis.style.Rule {

    private String name;
    private Description description;
    private GraphicLegend legend;
    private Filter filter;
    private boolean elseFilter;
    private double minScale;
    private double maxScale = Double.MAX_VALUE;
    private final List<Symbolizer> symbolizers = new ArrayList<>();
    private OnlineResource onlineResource;

    public Rule() {
    }

    public Rule(String name, Description description, GraphicLegend legend, Filter filter, boolean elseFilter, double minScale, double maxScale, List<Symbolizer> symbolizers, OnlineResource onlineResource) {
        this.name = name;
        this.description = description;
        this.legend = legend;
        this.filter = filter;
        this.elseFilter = elseFilter;
        this.minScale = minScale;
        this.maxScale = maxScale;
        if (symbolizers != null) this.symbolizers.addAll(symbolizers);
        this.onlineResource = onlineResource;
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
    public GraphicLegend getLegend() {
        return legend;
    }

    public void setLegend(GraphicLegend legend) {
        this.legend = legend;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public boolean isElseFilter() {
        return elseFilter;
    }

    public void setElseFilter(boolean elseFilter) {
        this.elseFilter = elseFilter;
    }

    @Override
    public double getMinScaleDenominator() {
        return minScale;
    }

    public void setMinScaleDenominator(double minScale) {
        this.minScale = minScale;
    }

    @Override
    public double getMaxScaleDenominator() {
        return maxScale;
    }

    public void setMaxScaleDenominator(double maxScale) {
        this.maxScale = maxScale;
    }

    @Override
    public List<Symbolizer> symbolizers() {
        return symbolizers;
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
        return Objects.hash(name, description, legend, filter, elseFilter, minScale, maxScale, symbolizers, onlineResource);
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
        final Rule other = (Rule) obj;
        return this.elseFilter == other.elseFilter
            && this.minScale == other.minScale
            && this.maxScale == other.maxScale
            && Objects.equals(this.name, other.name)
            && Objects.equals(this.description, other.description)
            && Objects.equals(this.legend, other.legend)
            && Objects.equals(this.filter, other.filter)
            && Objects.equals(this.symbolizers, other.symbolizers)
            && Objects.equals(this.onlineResource, other.onlineResource);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Rule castOrCopy(org.opengis.style.Rule candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Rule) {
            return (Rule) candidate;
        }

        final List<Symbolizer> symbols = new ArrayList<>();
        for (org.opengis.style.Symbolizer s : candidate.symbolizers()) {
            symbols.add(org.apache.sis.internal.style.Symbolizer.tryCastOrCopy(s));
        }

        return new Rule(
                candidate.getName(),
                Description.castOrCopy(candidate.getDescription()),
                GraphicLegend.castOrCopy(candidate.getLegend()),
                candidate.getFilter(),
                candidate.isElseFilter(),
                candidate.getMinScaleDenominator(),
                candidate.getMaxScaleDenominator(),
                symbols,
                candidate.getOnlineResource());
    }
}
