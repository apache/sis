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
package org.apache.sis.style.se1;

import java.util.Optional;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.OverlapBehavior;


/**
 * Instructions about how to render raster, matrix or coverage data.
 * It may be satellite photos or DEMs for example.
 *
 * <p>In the particular case of raster symbolizer, {@link #getGeometry()}
 * should return a {@link org.apache.sis.coverage.BandedCoverage} instead
 * of a geometry.</p>
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Ian Turton (CCG)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlType(name = "RasterSymbolizerType", propOrder = {
    "opacity",
    "channelSelection",
    "overlapBehavior",
    "colorMap",
    "contrastEnhancement",
    "shadedRelief",
    "imageOutline"
})
@XmlRootElement(name = "RasterSymbolizer")
public class RasterSymbolizer extends Symbolizer implements Translucent {
    /**
     * Level of translucency as a floating point number between 0 and 1 (inclusive), or {@code null} the default value.
     * The default value specified by OGC 05-077r4 standard is 1.
     *
     * @see #getOpacity()
     * @see #setOpacity(Expression)
     */
    @XmlElement(name = "Opacity")
    protected Expression<Feature, ? extends Number> opacity;

    /**
     * Selection of false-color channels for a multi-spectral raster source, or {@code null} if none.
     *
     * @see #getChannelSelection()
     * @see #setChannelSelection(ChannelSelection)
     */
    @XmlElement(name = "ChannelSelection")
    protected ChannelSelection channelSelection;

    /**
     * Behavior when multiple raster images in a layer overlap each other, or {@code null} if unspecified.
     * The default value is implementation-dependent.
     *
     * @see #getOverlapBehavior()
     * @see #setOverlapBehavior(OverlapBehavior)
     */
    @XmlElement(name = "OverlapBehavior")
    protected OverlapBehavior overlapBehavior;

    /**
     * Mapping of fixed-numeric pixel values to colors, or {@code null} if none.
     *
     * @see #getColorMap()
     * @see #setColorMap(ColorMap)
     */
    @XmlElement(name = "ColorMap")
    protected ColorMap colorMap;

    /**
     * Contrast enhancement for the whole image, or {@code null} if none.
     *
     * @see #getContrastEnhancement()
     * @see #setContrastEnhancement(ContrastEnhancement)
     */
    @XmlElement(name = "ContrastEnhancement")
    protected ContrastEnhancement contrastEnhancement;

    /**
     * Relief shading (or “hill shading”) to apply to the image for a three-dimensional visual effect.
     *
     * @see #getShadedRelief()
     * @see #setShadedRelief(ShadedRelief)
     */
    @XmlElement(name = "ShadedRelief")
    protected ShadedRelief shadedRelief;

    /**
     * Line or polygon symbolizer to use for outlining source rasters, or {@code null} if none.
     *
     * @see #getImageOutline()
     * @see #setImageOutline(Symbolizer)
     */
    @XmlElement(name = "ImageOutline")
    protected Symbolizer imageOutline;

    /**
     * Creates an initially opaque raster symbolizer with no contrast enhancement, shaded relief or outline.
     */
    public RasterSymbolizer() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public RasterSymbolizer(final RasterSymbolizer source) {
        super(source);
        opacity             = source.opacity;
        channelSelection    = source.channelSelection;
        overlapBehavior     = source.overlapBehavior;
        colorMap            = source.colorMap;
        contrastEnhancement = source.contrastEnhancement;
        shadedRelief        = source.shadedRelief;
        imageOutline        = source.imageOutline;
    }

    /**
     * Indicates the level of translucency as a floating point number between 0 and 1 (inclusive).
     * A value of zero means completely transparent. A value of 1.0 means completely opaque.
     *
     * @return the level of translucency as a floating point number between 0 and 1 (inclusive).
     *
     * @see Fill#getOpacity()
     * @see Stroke#getOpacity()
     * @see Graphic#getOpacity()
     */
    @Override
    public Expression<Feature, ? extends Number> getOpacity() {
        return defaultToOne(opacity);
    }

    /**
     * Sets the level of translucency as a floating point number between 0 and 1 (inclusive).
     * If this method is never invoked, then the default value is literal 1 (totally opaque).
     * That default value is standardized by OGC 05-077r4.
     *
     * @param  value  new level of translucency, or {@code null} for resetting the default value.
     */
    @Override
    public void setOpacity(final Expression<Feature, ? extends Number> value) {
        opacity = value;
    }

    /**
     * Returns the selection of false-color channels for a multi-spectral raster source.
     * Either red, green, and blue channels are selected, or a single grayscale channel is selected.
     * Contrast enhancement may be applied to each channel in isolation.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this fill, and conversely.</p>
     *
     * @return the selection of channels.
     */
    public Optional<ChannelSelection> getChannelSelection() {
        return Optional.ofNullable(channelSelection);
    }

    /**
     * Sets the selection of false-color channels for a multi-spectral raster source.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new selection of channels, or {@code null} for none.
     */
    public void setChannelSelection(final ChannelSelection value) {
        channelSelection = value;
    }

    /**
     * Returns the behavior when multiple raster images in a layer overlap each other.
     *
     * @return behavior when multiple raster images in a layer overlap each other.
     */
    public OverlapBehavior getOverlapBehavior() {
        final var value = overlapBehavior;
        return (value != null) ? value : OverlapBehavior.LATEST_ON_TOP;
        // Default value is unspecified, we use LATEST_ON_TOP for now.
    }

    /**
     * Set the behavior when multiple raster images in a layer overlap each other.
     *
     * @param  value  new behavior, or {@code null} for resetting the default value.
     */
    public void setOverlapBehavior(final OverlapBehavior value) {
        overlapBehavior = value;
    }

    /**
     * Returns the mapping of fixed-numeric pixel values to colors.
     * It can be used for defining the olors of a palette-type raster source.
     * For example, a DEM raster giving elevations in meters above sea level
     * can be translated to a colored image.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this fill, and conversely.</p>
     *
     * @return color map for the raster.
     */
    public Optional<ColorMap> getColorMap() {
        return Optional.ofNullable(colorMap);
    }

    /**
     * Sets the mapping of fixed-numeric pixel values to colors.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new color map for the raster, or {@code null} if none.
     */
    public void setColorMap(final ColorMap value) {
        colorMap = value;
    }

    /**
     * Returns the contrast enhancement for the whole image.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.
     *
     * @return contrast enhancement for the whole image.
     *
     * @see SelectedChannel#getContrastEnhancement()
     */
    public Optional<ContrastEnhancement> getContrastEnhancement() {
        return Optional.ofNullable(contrastEnhancement);
    }

    /**
     * Sets the contrast enhancement applied to the whole image.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new contrast enhancement, or {@code null} if none.
     *
     * @see SelectedChannel#setContrastEnhancement(ContrastEnhancement)
     */
    public void setContrastEnhancement(final ContrastEnhancement value) {
        contrastEnhancement = value;
    }

    /**
     * Returns the relief shading to apply to the image for a three-dimensional visual effect.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.
     *
     * @return the relief shading to apply.
     */
    public Optional<ShadedRelief> getShadedRelief() {
        return Optional.ofNullable(shadedRelief);
    }

    /**
     * Sets the relief shading to apply to the image for a three-dimensional visual effect.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new relief shading to apply, or {@code null} if none.
     */
    public void setShadedRelief(final ShadedRelief value) {
        shadedRelief = value;
    }

    /**
     * How to outline individual source rasters in a multi-raster set.
     * The value should be either a {@link LineSymbolizer} or {@link PolygonSymbolizer}.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.</p>
     *
     * @return Line or polygon symbolizer to use for outlining source rasters.
     */
    public Optional<Symbolizer> getImageOutline() {
        return Optional.ofNullable(imageOutline);
    }

    /**
     * Sets how to outline individual source rasters in a multi-raster set.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new line or polygon symbolizer to use, or {@code null} if none.
     */
    public void setImageOutline(final Symbolizer value) {
        imageOutline = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {opacity, channelSelection, overlapBehavior,
                colorMap, contrastEnhancement, shadedRelief, imageOutline};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public RasterSymbolizer clone() {
        final var clone = (RasterSymbolizer) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (channelSelection    != null) channelSelection    = channelSelection.clone();
        if (colorMap            != null) colorMap            = colorMap.clone();
        if (contrastEnhancement != null) contrastEnhancement = contrastEnhancement.clone();
        if (shadedRelief        != null) shadedRelief        = shadedRelief.clone();
        if (imageOutline        != null) imageOutline        = imageOutline.clone();
    }
}
