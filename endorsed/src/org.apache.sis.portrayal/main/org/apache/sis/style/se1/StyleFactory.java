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

import java.net.URI;
import java.awt.Color;
import java.util.Objects;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;

// Specific to the main branch:
import org.apache.sis.filter.Expression;
import org.apache.sis.filter.DefaultFilterFactory;


/**
 * Factory of style elements.
 * Style factory uses a {@link FilterFactory} instance that depends on the type of data to be styled.
 * That type of data is specified by the parameterized type {@code <R>}.
 * The two main types are {@link org.apache.sis.feature.AbstractFeature}
 * and {@link org.apache.sis.coverage.BandedCoverage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 */
public class StyleFactory<R> {
    /**
     * The factory to use for creating expressions.
     */
    final DefaultFilterFactory<R,?,?> filterFactory;

    /**
     * Literal commonly used as a default value.
     *
     * @see StyleElement#defaultToTrue(Expression)
     * @see StyleElement#defaultToFalse(Expression)
     */
    final Expression<R,Boolean> enabled, disabled;

    /**
     * Literal commonly used as a default value.
     */
    final Expression<R,Integer> zeroAsInt;

    /**
     * Literal commonly used as a default value.
     *
     * @see StyleElement#defaultToZero(Expression)
     * @see StyleElement#defaultToHalf(Expression)
     * @see StyleElement#defaultToOne(Expression)
     */
    final Expression<R,Double> zero, half, one, six, ten;

    /**
     * Default factor for shaded relief.
     * This is an arbitrary suggested but not standardized by OGC 05-077r4.
     */
    final Expression<R,Double> relief;

    /**
     * Literal commonly used as a default value.
     */
    final Expression<R,String> normal, square, bevel;

    /**
     * Literal for a predefined color which can be used as fill color.
     */
    final Expression<R,Color> black, gray, white;

    /**
     * An expression for fetching the default geometry.
     *
     * @todo According SE specification, the default expression in the context of some symbolizers should
     *       fetch all geometries, not only a default one. The default seems to depend on the symbolizer type.
     */
    final Expression<R,?> defaultGeometry;

    /**
     * Creates a new style factory.
     *
     * @param  filterFactory  the factory to use for creating expressions.
     */
    public StyleFactory(final DefaultFilterFactory<R,?,?> filterFactory) {
        this.filterFactory = Objects.requireNonNull(filterFactory);
        enabled   = filterFactory.literal(Boolean.TRUE);
        disabled  = filterFactory.literal(Boolean.FALSE);
        zeroAsInt = filterFactory.literal(0);
        zero      = filterFactory.literal(0.0);
        half      = filterFactory.literal(0.5);
        one       = filterFactory.literal(1.0);
        six       = filterFactory.literal(6.0);
        ten       = filterFactory.literal(10.0);
        relief    = filterFactory.literal(55.0);
        normal    = filterFactory.literal("normal");
        square    = filterFactory.literal("square");
        bevel     = filterFactory.literal("bevel");
        black     = filterFactory.literal(Color.BLACK);
        gray      = filterFactory.literal(Color.GRAY);
        white     = filterFactory.literal(Color.WHITE);

        defaultGeometry = filterFactory.property(AttributeConvention.GEOMETRY);
    }

    /**
     * Creates a new style factory with the same literals as the given factory.
     * This constructor shall not be public because it assumes that all literals
     * are implementations that ignore the type {@code <R>} of data to style,
     * in which case the unchecked cast is safe.
     *
     * @param  source  the style factory to copy.
     */
    @SuppressWarnings("unchecked")
    StyleFactory(final StyleFactory<?> source) {
        enabled   = (Expression<R,Boolean>) source.enabled;
        disabled  = (Expression<R,Boolean>) source.disabled;
        zeroAsInt = (Expression<R,Integer>) source.zeroAsInt;
        zero      = (Expression<R,Double>)  source.zero;
        half      = (Expression<R,Double>)  source.half;
        one       = (Expression<R,Double>)  source.one;
        six       = (Expression<R,Double>)  source.six;
        ten       = (Expression<R,Double>)  source.ten;
        relief    = (Expression<R,Double>)  source.relief;
        normal    = (Expression<R,String>)  source.normal;
        square    = (Expression<R,String>)  source.square;
        bevel     = (Expression<R,String>)  source.bevel;
        black     = (Expression<R,Color>)   source.black;
        gray      = (Expression<R,Color>)   source.gray;
        white     = (Expression<R,Color>)   source.white;

        filterFactory   = null;   // TODO: FilterFactory for coverage is not yet available.
        defaultGeometry = null;   // Idem.
    }

    /**
     * Creates an initially empty rule.
     * A rule is a set of tendering instructions grouped by feature-property conditions and map scales.
     *
     * @return new initially empty rule.
     */
    public Rule<R> createRule() {
        return new Rule<>(this);
    }

    /**
     * Creates a point symbolizer initialized to a default graphic.
     * A point symbolizer is a set of instructions about how to draw a graphic at a point.
     *
     * @return new point symbolizer initialized to a default graphic.
     */
    public PointSymbolizer<R> createPointSymbolizer() {
        return new PointSymbolizer<>(this);
    }

    /**
     * Creates a line symbolizer with the default stroke and no perpendicular offset.
     * A line symbolizer is a set of instructions about how to draw on a map the lines of a geometry.
     *
     * @return new ine symbolizer with the default stroke and no perpendicular offset.
     */
    public LineSymbolizer<R> createLineSymbolizer() {
        return new LineSymbolizer<>(this);
    }

    /**
     * Creates a polygon symbolizer initialized to the default fill and default stroke.
     * A polygon symbolizer is a set of instructions about how to draw on a map the lines and the interior of polygons.
     *
     * @return new polygon symbolizer initialized to the default fill and default stroke.
     */
    public PolygonSymbolizer<R> createPolygonSymbolizer() {
        return new PolygonSymbolizer<>(this);
    }

    /**
     * Creates a text symbolizer with default placement and default font.
     * A text symbolizer is a set of instructions about how to drawn text on a map.
     * The new symbolizer has no initial label.
     *
     * @return new text symbolizer with default placement and default font.
     *
     * @see #createTextSymbolizer(String)
     */
    public TextSymbolizer<R> createTextSymbolizer() {
        return new TextSymbolizer<>(this);
    }

    /**
     * Creates a text symbolizer initialized with the specified label literal.
     *
     * @param  label  initial label literal, or {@code null} if none.
     * @return new text symbolizer with default placement and default font.
     */
    public TextSymbolizer<R> createTextSymbolizer(final String label) {
        final var s = createTextSymbolizer();
        s.label = s.literal(label);
        return s;
    }

    /**
     * Creates an initially opaque raster symbolizer with no contrast enhancement, shaded relief or outline.
     * A raster symbolizer is a set of instructions about how to render raster, matrix or coverage data.
     *
     * @return new initially opaque raster symbolizer.
     */
    public RasterSymbolizer<R> createRasterSymbolizer() {
        return new RasterSymbolizer<>(this);
    }

    /**
     * Creates an initially empty description.
     * A description is a set of human-readable information about a style object being defined.
     *
     * @return new initially empty description.
     */
    public Description<R> createDescription() {
        return new Description<>(this);
    }

    /**
     * Creates a point placement initialized to anchor at the middle and no displacement.
     * A point placement is a set of instructions about how a text label is positioned relative to a point.
     *
     * @return new point placement initialized to anchor at the middle and no displacement.
     */
    public PointPlacement<R> createPointPlacement() {
        return new PointPlacement<>(this);
    }

    /**
     * Creates a line placement initialized to no offset, no repetition and no gap.
     * A line placement is a set of instructions about where and how a text label should be rendered relative to a line.
     *
     * @return new line placement initialized to no offset, no repetition and no gap.
     */
    public LinePlacement<R> createLinePlacement() {
        return new LinePlacement<>(this);
    }

    /**
     * Creates an anchor point initialized to <var>x</var> = 0.5 and <var>y</var> = 0.5.
     * An anchor point is the location inside a graphic or label to use as an "anchor"
     * for positioning it relative to a point.
     *
     * @return new anchor point initialized to center.
     *
     * @see #createAnchorPoint(double, double)
     */
    public AnchorPoint<R> createAnchorPoint() {
        return new AnchorPoint<>(this);
    }

    /**
     * Creates an anchor point initialized to the given position.
     * This is a convenience method for a frequently used operation.
     *
     * @param  x  the initial <var>x</var> position.
     * @param  y  the initial <var>y</var> position.
     * @return new anchor point initialized to the given position.
     */
    public AnchorPoint<R> createAnchorPoint(final double x, final double y) {
        final var s = createAnchorPoint();
        s.anchorPointX = filterFactory.literal(x);
        s.anchorPointY = filterFactory.literal(y);
        return s;
    }

    /**
     * Creates a displacement initialized to zero offsets.
     * A displacement is the two-dimensional offsets from the original geometry.
     *
     * @return new displacement initialized to zero offsets.
     *
     * @see #createDisplacement(double, double)
     */
    public Displacement<R> createDisplacement() {
        return new Displacement<>(this);
    }

    /**
     * Creates a displacement initialized to the given offsets.
     * This is a convenience method for a frequently used operation.
     *
     * @param  x  the <var>x</var> displacement.
     * @param  y  the <var>y</var> displacement.
     * @return new displacement initialized to the given offsets.
     */
    public Displacement<R> createDisplacement(final double x, final double y) {
        final var s = createDisplacement();
        s.displacementX = filterFactory.literal(x);
        s.displacementY = filterFactory.literal(y);
        return s;
    }

    /**
     * Creates a mark initialized to a gray square with black outline.
     * A mark is a predefined shape that can be drawn at the points of the geometry.
     *
     * @return new mark initialized to a gray square with black outline.
     */
    public Mark<R> createMark() {
        return new Mark<>(this);
    }

    /**
     * Creates an initially empty external graphic.
     * An external graphic is a reference to an external file that contains an image of some kind,
     * such as a PNG or SVG.
     *
     * @return new initially empty external graphic.
     *
     * @see #createExternalGraphic(URI, String)
     */
    public ExternalGraphic<R> createExternalGraphic() {
        return new ExternalGraphic<>(this);
    }

    /**
     * Creates an external graphic initialized to the given URI.
     *
     * @param  linkage  URI to the external graphic, or {@code null} if none.
     * @param  format   MIME type of the external graphic, or {@code null} if unspecified.
     * @return new external graphic initialized to the given URI.
     */
    public ExternalGraphic<R> createExternalGraphic(final URI linkage, final String format) {
        final var s = createExternalGraphic();
        s.format = format;
        if (linkage != null) {
            s.onlineResource = new DefaultOnlineResource(linkage);
        }
        return s;
    }

    /**
     * Creates a stroke initialized to solid line of black opaque color, 1 pixel width.
     * A stroke is a set of instructions about how to draw styled lines.
     *
     * @return new stroke initialized to solid line of black opaque color, 1 pixel width.
     *
     * @see #createStroke(Color)
     */
    public Stroke<R> createStroke() {
        return new Stroke<>(this);
    }

    /**
     * Creates a stroke initialized to the given color and opacity.
     * The alpha channel of the given color is used for determining the opacity.
     *
     * @param  color  the initial color, or {@code null} if none.
     * @return new stroke initialized to the given color and opacity.
     */
    public Stroke<R> createStroke(final Color color) {
        final var s = createStroke();
        s.setColorAndOpacity(color);
        return s;
    }

    /**
     * Creates an opaque fill initialized to the gray color.
     * A fill is a set of instructions about how to fill the interior of polygons.
     *
     * @return new opaque fill initialized to the gray color.
     *
     * @see #createFill(Color)
     */
    public Fill<R> createFill() {
        return new Fill<>(this);
    }

    /**
     * Creates a fill initialized to the given color and opacity.
     * The alpha channel of the given color is used for determining the opacity.
     *
     * @param  color  the initial color, or {@code null} if none.
     * @return new fill initialized to the given color and opacity.
     */
    public Fill<R> createFill(final Color color) {
        final var s = createFill();
        s.setColorAndOpacity(color);
        return s;
    }

    /**
     * Creates an halo initialized to a white color and a radius of 1 pixel.
     * A halo is a fill that are applied to the backgrounds of font glyphs.
     *
     * @return new halo initialized to a white color and a radius of 1 pixel.
     */
    public Halo<R> createHalo() {
        return new Halo<>(this);
    }

    /**
     * Creates a font initialized to normal style, normal weight and a size of 10 pixels.
     * A font is the identification of a font of a certain family, style, and size.
     *
     * @return new font initialized to normal style, normal weight and a size of 10 pixels.
     */
    public Font<R> createFont() {
        return new Font<>(this);
    }

    /**
     * Creates a graphic initialized to opaque default mark, default size and no rotation.
     * A graphic is a symbol with an inherent shape, color(s), and possibly size.
     *
     * @return new graphic initialized to opaque default mark, default size and no rotation.
     */
    public Graphic<R> createGraphic() {
        return new Graphic<>(this);
    }

    /**
     * Creates a graphic fill initialized to a default graphic.
     * A graphic fill is a stipple-fill repeated graphic.
     *
     * @return new graphic fill initialized to a default graphic.
     */
    public GraphicFill<R> createGraphicFill() {
        return new GraphicFill<>(this);
    }

    /**
     * Creates a graphic stroke initialized to a default graphic and no gap.
     * A graphic stroke is a repeated-linear-graphic stroke.
     *
     * @return new graphic stroke initialized to a default graphic and no gap.
     */
    public GraphicStroke<R> createGraphicStroke() {
        return new GraphicStroke<>(this);
    }

    /**
     * Creates a legend initialized to the default graphic.
     * A legend graphic is a graphic to do displayed in a legend for a rule.
     *
     * @return new legend initialized to the default graphic.
     */
    public LegendGraphic<R> createLegendGraphic() {
        return new LegendGraphic<>(this);
    }

    /**
     * Creates an initially empty color replacement.
     * A color replacement defines the replacement of a color in an external graphic.
     *
     * @return new initially empty color replacement.
     */
    public ColorReplacement<R> createColorReplacement() {
        return new ColorReplacement<>(this);
    }

    /**
     * Creates an initially empty color map.
     * A color map is the mapping of fixed-numeric pixel values to colors.
     *
     * @return new initially empty color map.
     */
    public ColorMap<R> createColorMap() {
        return new ColorMap<>(this);
    }

    /**
     * Creates an initially empty channel selection.
     * A channel selection specifies the false-color channel selection for a multi-spectral raster source.
     *
     * @return new initially empty channel selection.
     */
    public ChannelSelection<R> createChannelSelection() {
        return new ChannelSelection<>(this);
    }

    /**
     * Creates an initially empty selected channel.
     * A selected channel is information about a channel to use in a multi-spectral source.
     *
     * @return new initially empty selected channel.
     *
     * @see #createSelectedChannel(String)
     */
    public SelectedChannel<R> createSelectedChannel() {
        return new SelectedChannel<>(this);
    }

    /**
     * Creates a selected channel initialized to the given channel name.
     *
     * @param  sourceChannelName  the channel's name, or {@code null} if unspecified.
     * @return new selected channel for the given name.
     */
    public SelectedChannel<R> createSelectedChannel(final String sourceChannelName) {
        final var s = createSelectedChannel();
        s.sourceChannelName = s.literal(sourceChannelName);
        return s;
    }

    /**
     * Creates a contrast enhancement initialized to no operation.
     *
     * @return new contrast enhancement initialized to no operation.
     */
    public ContrastEnhancement<R> createContrastEnhancement() {
        return new ContrastEnhancement<> (this);
    }

    /**
     * Creates a shaded relief initialized to implementation-specific default values.
     * A shaded relief is a “hill shading” applied to an image for a three-dimensional visual effect.
     *
     * @return new shaded relief initialized to implementation-specific default values.
     */
    public ShadedRelief<R> createShadedRelief() {
        return new ShadedRelief<>(this);
    }
}
