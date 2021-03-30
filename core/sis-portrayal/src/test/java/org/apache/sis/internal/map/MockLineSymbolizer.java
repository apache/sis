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

import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.Description;
import org.opengis.style.LineSymbolizer;
import org.opengis.style.Stroke;
import org.opengis.style.StyleVisitor;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MockLineSymbolizer implements LineSymbolizer {

    String name;
    Description description;

    Stroke stroke;
    Expression<Feature,?> perpendicularOffset;
    Unit<Length> unitOfMeasure;
    Expression<Feature,?> geometry;

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
    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    @Override
    public Expression getPerpendicularOffset() {
        return perpendicularOffset;
    }

    public void setPerpendicularOffset(Expression perpendicularOffset) {
        this.perpendicularOffset = perpendicularOffset;
    }

    @Override
    public Unit<Length> getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(Unit<Length> unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    @Override
    public Expression<Feature,?> getGeometry() {
        return geometry;
    }

    public void setGeometry(Expression geometry) {
        this.geometry = geometry;
    }

    /**
     * Will likely be removed from geoapi.
     */
    @Deprecated
    @Override
    public String getGeometryPropertyName() {
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
