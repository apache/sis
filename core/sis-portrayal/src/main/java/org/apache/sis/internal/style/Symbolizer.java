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

import java.util.Objects;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.Description;

/**
 * Mutable implementation of {@link org.opengis.style.Symbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class Symbolizer implements org.opengis.style.Symbolizer {

    private String name;
    private Description description;
    private Expression<Feature,?> geometry;
    private Unit<Length> unit;

    public Symbolizer() {
    }

    public Symbolizer(String name, Expression geometry, Description description, Unit<Length> unit) {
        this.name = name;
        this.geometry = geometry;
        this.description = description;
        this.unit = unit;
    }

    @Override
    public Unit<Length> getUnitOfMeasure() {
        return unit;
    }

    public void setUnitOfMeasure(Unit<Length> unit) {
        this.unit = unit;
    }

    @Override
    public String getGeometryPropertyName() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Expression<Feature, ?> getGeometry() {
        return geometry;
    }

    public void setGeometry(Expression<Feature, ?> geometry) {
        this.geometry = geometry;
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
    public int hashCode() {
        return Objects.hash(name, description, geometry, unit);
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
        final Symbolizer other = (Symbolizer) obj;
        return Objects.equals(this.name, other.name)
            && Objects.equals(this.description, other.description)
            && Objects.equals(this.geometry, other.geometry)
            && Objects.equals(this.unit, other.unit);
    }

    static org.opengis.style.Symbolizer tryCastOrCopy(org.opengis.style.Symbolizer s) {
        if (s instanceof org.opengis.style.PointSymbolizer) {
            return PointSymbolizer.castOrCopy((org.opengis.style.PointSymbolizer) s);
        } else if (s instanceof org.opengis.style.LineSymbolizer) {
            return LineSymbolizer.castOrCopy((org.opengis.style.LineSymbolizer) s);
        } else if (s instanceof org.opengis.style.PolygonSymbolizer) {
            return PolygonSymbolizer.castOrCopy((org.opengis.style.PolygonSymbolizer) s);
        } else if (s instanceof org.opengis.style.TextSymbolizer) {
            return TextSymbolizer.castOrCopy((org.opengis.style.TextSymbolizer) s);
        } else if (s instanceof org.opengis.style.RasterSymbolizer) {
            return RasterSymbolizer.castOrCopy((org.opengis.style.RasterSymbolizer) s);
        } else {
            //leave it unchanged
            return s;
        }
    }
}
