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
import org.opengis.style.OverlapBehavior;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.RasterSymbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class RasterSymbolizer extends Symbolizer implements org.opengis.style.RasterSymbolizer {

    private Expression<Feature,? extends Number> opacity;
    private ChannelSelection channelSelection;
    private OverlapBehavior overlapBehavior;
    private ColorMap colorMap;
    private ContrastEnhancement contrastEnhancement;
    private ShadedRelief shadedRelief;
    private org.opengis.style.Symbolizer imageOutline;

    public static RasterSymbolizer createDefault() {
        return new RasterSymbolizer();
    }

    public RasterSymbolizer() {
    }

    public RasterSymbolizer(String name, Expression geometry, Description description, Unit<Length> unit,
            Expression opacity, ChannelSelection channelSelection, OverlapBehavior overlapsBehaviour,
            ColorMap colorMap, ContrastEnhancement contrast, ShadedRelief shaded, org.opengis.style.Symbolizer outline) {
        super(name, geometry, description, unit);
        this.opacity = opacity;
        this.channelSelection = channelSelection;
        this.overlapBehavior = overlapsBehaviour;
        this.colorMap = colorMap;
        this.contrastEnhancement = contrast;
        this.shadedRelief = shaded;
        this.imageOutline = outline;
    }

    @Override
    public Expression<Feature,? extends Number> getOpacity() {
        return opacity;
    }

    public void setOpacity(Expression<Feature, ? extends Number> opacity) {
        this.opacity = opacity;
    }

    @Override
    public ChannelSelection getChannelSelection() {
        return channelSelection;
    }

    public void setChannelSelection(ChannelSelection channelSelection) {
        this.channelSelection = channelSelection;
    }

    @Override
    public OverlapBehavior getOverlapBehavior() {
        return overlapBehavior;
    }

    public void setOverlapBehavior(OverlapBehavior overlapBehavior) {
        this.overlapBehavior = overlapBehavior;
    }

    @Override
    public ColorMap getColorMap() {
        return colorMap;
    }

    public void setColorMap(ColorMap colorMap) {
        this.colorMap = colorMap;
    }

    @Override
    public ContrastEnhancement getContrastEnhancement() {
        return contrastEnhancement;
    }

    public void setContrastEnhancement(ContrastEnhancement contrastEnhancement) {
        this.contrastEnhancement = contrastEnhancement;
    }

    @Override
    public ShadedRelief getShadedRelief() {
        return shadedRelief;
    }

    public void setShadedRelief(ShadedRelief shadedRelief) {
        this.shadedRelief = shadedRelief;
    }

    @Override
    public org.opengis.style.Symbolizer getImageOutline() {
        return imageOutline;
    }

    public void setImageOutline(org.opengis.style.Symbolizer imageOutline) {
        this.imageOutline = imageOutline;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(opacity, channelSelection, overlapBehavior,
                colorMap, contrastEnhancement, shadedRelief, imageOutline);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RasterSymbolizer other = (RasterSymbolizer) obj;
        return Objects.equals(this.opacity, other.opacity)
            && Objects.equals(this.channelSelection, other.channelSelection)
            && this.overlapBehavior == other.overlapBehavior
            && Objects.equals(this.colorMap, other.colorMap)
            && Objects.equals(this.contrastEnhancement, other.contrastEnhancement)
            && Objects.equals(this.shadedRelief, other.shadedRelief)
            && Objects.equals(this.imageOutline, other.imageOutline);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static RasterSymbolizer castOrCopy(org.opengis.style.RasterSymbolizer candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof RasterSymbolizer) {
            return (RasterSymbolizer) candidate;
        }
        return new RasterSymbolizer(
                candidate.getName(),
                candidate.getGeometry(),
                Description.castOrCopy(candidate.getDescription()),
                candidate.getUnitOfMeasure(),
                candidate.getOpacity(),
                ChannelSelection.castOrCopy(candidate.getChannelSelection()),
                candidate.getOverlapBehavior(),
                ColorMap.castOrCopy(candidate.getColorMap()),
                ContrastEnhancement.castOrCopy(candidate.getContrastEnhancement()),
                ShadedRelief.castOrCopy(candidate.getShadedRelief()),
                candidate.getImageOutline()
            );
    }
}
