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
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.TextSymbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TextSymbolizer extends Symbolizer implements org.opengis.style.TextSymbolizer {

    private Expression<Feature,String> label;
    private Font font;
    private LabelPlacement labelPlacement;
    private Halo halo;
    private Fill fill;

    public static TextSymbolizer createDefault() {
        return new TextSymbolizer(null, null, new Description(), StyleFactory.DEFAULT_UOM,
            StyleFactory.DEFAULT_TEXT_LABEL, new Font(),
                new PointPlacement(new AnchorPoint(), new Displacement(), StyleFactory.LITERAL_ZERO),
                null, new Fill());
    }

    public TextSymbolizer() {
    }

    public TextSymbolizer(String name, Expression geometry,
            Description description,
            Unit<Length> unit,
            Expression label,
            Font font,
            LabelPlacement placement,
            Halo halo,
            Fill fill) {
        super(name, geometry, description, unit);
        this.label = label;
        this.font = font;
        this.labelPlacement = placement;
        this.halo = halo;
        this.fill = fill;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public Expression<Feature,String> getLabel() {
        return label;
    }

    public void setLabel(Expression<Feature, String> label) {
        this.label = label;
    }

    @Override
    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    @Override
    public LabelPlacement getLabelPlacement() {
        return labelPlacement;
    }

    public void setLabelPlacement(LabelPlacement labelPlacement) {
        this.labelPlacement = labelPlacement;
    }

    @Override
    public Halo getHalo() {
        return halo;
    }

    public void setHalo(Halo halo) {
        this.halo = halo;
    }

    @Override
    public Fill getFill() {
        return fill;
    }

    public void setFill(Fill fill) {
        this.fill = fill;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(label, font, labelPlacement, halo, fill);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof TextSymbolizer)) {
            return false;
        }
        final TextSymbolizer other = (TextSymbolizer) obj;
        return Objects.equals(this.label, other.label)
            && Objects.equals(this.font, other.font)
            && Objects.equals(this.labelPlacement, other.labelPlacement)
            && Objects.equals(this.halo, other.halo)
            && Objects.equals(this.fill, other.fill);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static TextSymbolizer castOrCopy(org.opengis.style.TextSymbolizer candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof TextSymbolizer) {
            return (TextSymbolizer) candidate;
        }
        return new TextSymbolizer(candidate.getName(),
                candidate.getGeometry(),
                org.apache.sis.internal.style.Description.castOrCopy(candidate.getDescription()),
                candidate.getUnitOfMeasure(),
                candidate.getLabel(),
                org.apache.sis.internal.style.Font.castOrCopy(candidate.getFont()),
                LabelPlacement.castOrCopy(candidate.getLabelPlacement()),
                org.apache.sis.internal.style.Halo.castOrCopy(candidate.getHalo()),
                org.apache.sis.internal.style.Fill.castOrCopy(candidate.getFill()));
    }
}
