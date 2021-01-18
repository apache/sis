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
import org.opengis.filter.Filter;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.style.Description;
import org.opengis.style.GraphicLegend;
import org.opengis.style.Rule;
import org.opengis.style.StyleVisitor;
import org.opengis.style.Symbolizer;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MockRule implements Rule {

    private String name;
    private Description description;
    private GraphicLegend legend;
    private Filter filter;
    private boolean iselseFilter;
    private double minScaleDenominator = 0.0;
    private double maxScaleDenominator = Double.MAX_VALUE;
    private final List<Symbolizer> symbolizers = new ArrayList<>();
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
        return iselseFilter;
    }

    public void setIsElseFilter(boolean iselseFilter) {
        this.iselseFilter = iselseFilter;
    }

    @Override
    public double getMinScaleDenominator() {
        return minScaleDenominator;
    }

    public void setMinScaleDenominator(double minScaleDenominator) {
        this.minScaleDenominator = minScaleDenominator;
    }

    @Override
    public double getMaxScaleDenominator() {
        return maxScaleDenominator;
    }

    public void setMaxScaleDenominator(double maxScaleDenominator) {
        this.maxScaleDenominator = maxScaleDenominator;
    }

    @Override
    public List<Symbolizer> symbolizers() {
        return symbolizers;
    }

    @Override
    public OnlineResource getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(OnlineResource onlineResource) {
        this.onlineResource = onlineResource;
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
