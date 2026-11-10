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
package org.apache.sis.style.xml;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.measure.Unit;
import javax.swing.Icon;
import javax.xml.stream.XMLStreamException;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.style.se1.AbstractStyle;
import org.apache.sis.style.se1.AnchorPoint;
import org.apache.sis.style.se1.ChannelSelection;
import org.apache.sis.style.se1.ColorMap;
import org.apache.sis.style.se1.ContrastEnhancement;
import org.apache.sis.style.se1.ContrastMethod;
import org.apache.sis.style.se1.CoverageStyle;
import org.apache.sis.style.se1.Description;
import org.apache.sis.style.se1.Displacement;
import org.apache.sis.style.se1.ExternalGraphic;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.Font;
import org.apache.sis.style.se1.Graphic;
import org.apache.sis.style.se1.GraphicFill;
import org.apache.sis.style.se1.GraphicStroke;
import org.apache.sis.style.se1.GraphicalSymbol;
import org.apache.sis.style.se1.Halo;
import org.apache.sis.style.se1.LabelPlacement;
import org.apache.sis.style.se1.LinePlacement;
import org.apache.sis.style.se1.LineSymbolizer;
import org.apache.sis.style.se1.Mark;
import org.apache.sis.style.se1.OverlapBehavior;
import org.apache.sis.style.se1.PointPlacement;
import org.apache.sis.style.se1.PointSymbolizer;
import org.apache.sis.style.se1.PolygonSymbolizer;
import org.apache.sis.style.se1.RasterSymbolizer;
import org.apache.sis.style.se1.Rule;
import org.apache.sis.style.se1.SelectedChannel;
import org.apache.sis.style.se1.SemanticType;
import org.apache.sis.style.se1.ShadedRelief;
import org.apache.sis.style.se1.Stroke;
import org.apache.sis.style.se1.StyleFactory;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.TextSymbolizer;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.ValueReference;


/**
 * Base class of the writers of the symbology elements: feature type styles, coverage styles, rules and symbolizers.
 * The values which are equal to the default values are not written.
 * The filters and expressions are written by the {@linkplain #filterWriter() filter writer} of the same version.
 * This class contains the writing code shared by {@link SEWriter100} and {@link SEWriter110}.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class SEWriter extends AbstractStaxWriter {
    /**
     * The factory used for creating style elements with default values.
     * Default values are the same for all types of resources.
     */
    private static final StyleFactory<Feature> DEFAULTS = FeatureTypeStyle.FACTORY;

    /**
     * Creates a new writer sharing the cursor and the state of the given writer.
     *
     * @param  parent  the style writer which contains this symbology writer.
     */
    protected SEWriter(final AbstractStaxWriter parent) {
        super(parent);
    }

    /**
     * Returns the writer of filters and expressions of the same version as this writer.
     *
     * @return the filter writer used by this symbology writer.
     */
    abstract FilterWriter filterWriter();

    /**
     * Returns the namespace of the symbology elements.
     * This is the SLD namespace in version 1.0 and the SE namespace in version 1.1.
     * This namespace is also used for the {@code <Name>} and {@code <Description>} elements of SLD.
     *
     * @return namespace of the symbology elements.
     */
    abstract String ns();

    /**
     * Returns the name of the elements containing SVG or CSS parameters.
     *
     * @return {@code "CssParameter"} in version 1.0 or {@code "SvgParameter"} in version 1.1.
     */
    abstract String parameterElement();

    /**
     * Writes the description of an element. In version 1.1, this is a {@code <Description>} element.
     * In version 1.0, this is a {@code <Title>} and an {@code <Abstract>} element, which are allowed
     * only in some elements.
     *
     * @param  description    the description to write, or {@code null} if none.
     * @param  allowedIn10    whether a description is allowed in the version 1.0 of the enclosing element.
     */
    abstract void writeDescription(Description<?> description, boolean allowedIn10) throws XMLStreamException;

    /**
     * Writes a {@code <ColorMap>} element.
     *
     * @param  <R>       type of resources to style.
     * @param  colorMap  the color map to write.
     */
    abstract <R> void writeColorMap(ColorMap<R> colorMap) throws XMLStreamException, DataStoreException;

    /**
     * Writes an {@code <OverlapBehavior>} element.
     *
     * @param  behavior  the overlap behavior to write.
     */
    abstract void writeOverlapBehavior(OverlapBehavior behavior) throws XMLStreamException;

    // ---------------------------------------------------------------------------------------------
    //  Elements shared by many style elements
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the given international string as a string, or {@code null} if absent.
     */
    static String asText(final Optional<InternationalString> text) {
        return text.map(InternationalString::toString).orElse(null);
    }

    /**
     * Writes the {@code <Name>} element if the given name is present.
     *
     * @param  name  the name to write.
     */
    final void writeName(final Optional<String> name) throws XMLStreamException {
        text(ns(), SETags.NAME, name.orElse(null));
    }

    /**
     * Writes an element containing the given expression as a parameter value.
     * Does nothing if the given expression is null.
     * This method delegates to the {@linkplain #filterWriter() filter writer}.
     *
     * @param  ns          namespace of the element to write.
     * @param  local       local name of the element to write.
     * @param  expression  the expression to write, or {@code null}.
     *
     * @see FilterWriter#writeParameter(String, String, Expression)
     */
    private <R> void writeParameter(final String ns, final String local, final Expression<R,?> expression)
            throws XMLStreamException, DataStoreException
    {
        filterWriter().writeParameter(ns, local, expression);
    }

    /**
     * Writes an element containing the given expression as a parameter value, in the symbology namespace.
     * Does nothing if the value is null or equal to the default value.
     *
     * @param  local         local name of the element to write.
     * @param  value         the value to write, or {@code null} if none.
     * @param  defaultValue  the default value, or {@code null} if none.
     */
    private <R> void writeParameter(final String local, final Expression<R,?> value, final Expression<?,?> defaultValue)
            throws XMLStreamException, DataStoreException
    {
        if (value != null && !value.equals(defaultValue)) {
            writeParameter(ns(), local, value);
        }
    }

    /**
     * Writes a {@code <SvgParameter>} or {@code <CssParameter>} element.
     * Does nothing if the value is null or equal to the default value.
     *
     * @param  name          name of the SVG parameter.
     * @param  value         the value to write, or {@code null} if none.
     * @param  defaultValue  the default value, or {@code null} if none.
     */
    private <R> void writeSvgParameter(final String name, final Expression<R,?> value, final Expression<?,?> defaultValue)
            throws XMLStreamException, DataStoreException
    {
        if (value != null && !value.equals(defaultValue)) {
            start(ns(), parameterElement());
            attribute(SETags.PARAMETER_NAME, name);
            filterWriter().writeParameterContent(value);
            end();
        }
    }

    /**
     * Writes an {@code <OnlineResource>} element.
     *
     * @param  ns        namespace of the element.
     * @param  resource  the online resource to write.
     */
    final void writeOnlineResource(final String ns, final OnlineResource resource) throws XMLStreamException {
        final URI linkage = resource.getLinkage();
        if (linkage == null) {
            warning("Online resource without linkage, ignored.");
            return;
        }
        empty(ns, SETags.ONLINE_RESOURCE);
        xlink(FilterTags.TYPE, FilterTags.SIMPLE);
        xlink(FilterTags.HREF, linkage.toString());
    }

    /**
     * Writes an {@code <InlineContent>} element containing the given image encoded in base64.
     * Images read by {@link StyleReader} are written with their original encoding.
     * Other images are encoded in PNG format.
     *
     * @param  icon  the image to write.
     * @return whether the image has been written.
     */
    private boolean writeInlineContent(final Icon icon) throws XMLStreamException, DataStoreException {
        final byte[] data;
        if (icon instanceof InlineImage image) {
            data = image.data;
        } else {
            final int width  = icon.getIconWidth();
            final int height = icon.getIconHeight();
            if (width <= 0 || height <= 0) {
                warning("Empty inline image, ignored.");
                return false;
            }
            final var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = image.createGraphics();
            try {
                icon.paintIcon(null, g, 0, 0);
            } finally {
                g.dispose();
            }
            final var buffer = new ByteArrayOutputStream();
            try {
                ImageIO.write(image, "png", buffer);
            } catch (IOException e) {
                throw new DataStoreException("Cannot encode the inline image.", e);
            }
            data = buffer.toByteArray();
        }
        start(ns(), SETags.INLINE_CONTENT);
        attribute(SETags.ENCODING, SETags.BASE64);
        writer.writeCharacters(Base64.getEncoder().encodeToString(data));
        end();
        return true;
    }

    /**
     * Returns the URI of the given unit of measurement, or {@code null} for pixels (the default).
     */
    private String formatUnit(final Unit<?> unit) {
        if (unit == null || unit.equals(Units.PIXEL)) return null;
        if (unit.equals(Units.METRE)) return SETags.UOM_PREFIX + SETags.METRE;
        if (unit.equals(Units.FOOT))  return SETags.UOM_PREFIX + SETags.FOOT;
        warning("The unit of measurement \"" + unit + "\" is not standard in Symbology Encoding.");
        return unit.toString();
    }

    // ---------------------------------------------------------------------------------------------
    //  Styles and rules
    // ---------------------------------------------------------------------------------------------

    /**
     * Writes a {@code <FeatureTypeStyle>} element.
     *
     * @param  style  the style to write.
     */
    final void writeFeatureTypeStyle(final FeatureTypeStyle style) throws XMLStreamException, DataStoreException {
        writeStyle(style, SETags.FEATURE_TYPE_STYLE, SETags.FEATURE_TYPE_NAME);
    }

    /**
     * Writes a {@code <CoverageStyle>} element. This element exists only in version 1.1.
     *
     * @param  style  the style to write.
     */
    final void writeCoverageStyle(final CoverageStyle style) throws XMLStreamException, DataStoreException {
        if (isVersion11()) {
            writeStyle(style, SETags.COVERAGE_STYLE, SETags.COVERAGE_NAME);
        } else {
            unsupported(SETags.COVERAGE_STYLE);
        }
    }

    /**
     * Writes a feature type style or a coverage style.
     *
     * @param  style      the style to write.
     * @param  element    local name of the style element.
     * @param  typeName   local name of the element for the feature type name or coverage name.
     */
    private <R> void writeStyle(final AbstractStyle<R> style, final String element, final String typeName)
            throws XMLStreamException, DataStoreException
    {
        final String ns = ns();
        start(ns, element);
        writeName(style.getName());
        writeDescription(style.getDescription().orElse(null), true);
        final GenericName name = style.getFeatureTypeName().orElse(null);
        if (name != null) {
            text(ns, typeName, name.toString());
        }
        for (final SemanticType type : style.semanticTypeIdentifiers()) {
            text(ns, SETags.SEMANTIC_TYPE_IDENTIFIER, SETags.GENERIC + type.name().toLowerCase(Locale.ROOT));
        }
        for (final Rule<R> rule : style.rules()) {
            writeRule(rule);
        }
        end();
    }

    /**
     * Writes a {@code <Rule>} element. In version 1.1, a rule having an online source is written as
     * an {@code <OnlineResource>} element instead.
     *
     * @param  rule  the rule to write.
     */
    final <R> void writeRule(final Rule<R> rule) throws XMLStreamException, DataStoreException {
        final String ns = ns();
        final OnlineResource source = rule.getOnlineSource().orElse(null);
        if (source != null && isVersion11()) {
            writeOnlineResource(ns, source);
            return;
        }
        start(ns, SETags.RULE);
        writeName(rule.getName());
        writeDescription(rule.getDescription().orElse(null), true);
        if (rule.getLegend().isPresent()) {
            start(ns, SETags.LEGEND_GRAPHIC);
            writeGraphic(rule.getLegend().get().getGraphic());
            end();
        }
        if (rule.isElseFilter()) {
            empty(ns, SETags.ELSE_FILTER);
        } else {
            final Filter<R> filter = rule.getFilter();
            if (filter != null && !filter.equals(Filter.include())) {
                filterWriter().writeFilter(filter);
            }
        }
        final double min = rule.getMinScaleDenominator();
        final double max = rule.getMaxScaleDenominator();
        if (min > 0) {
            text(ns, SETags.MIN_SCALE_DENOMINATOR, format(min));
        }
        if (max < Double.POSITIVE_INFINITY) {
            text(ns, SETags.MAX_SCALE_DENOMINATOR, format(max));
        }
        for (final Symbolizer<R> symbolizer : rule.symbolizers()) {
            writeSymbolizer(symbolizer);
        }
        end();
    }

    // ---------------------------------------------------------------------------------------------
    //  Symbolizers
    // ---------------------------------------------------------------------------------------------

    /**
     * Writes a symbolizer.
     *
     * @param  symbolizer  the symbolizer to write.
     */
    final <R> void writeSymbolizer(final Symbolizer<R> symbolizer) throws XMLStreamException, DataStoreException {
        final String element;
        if      (symbolizer instanceof PointSymbolizer)   element = SETags.POINT_SYMBOLIZER;
        else if (symbolizer instanceof LineSymbolizer)    element = SETags.LINE_SYMBOLIZER;
        else if (symbolizer instanceof PolygonSymbolizer) element = SETags.POLYGON_SYMBOLIZER;
        else if (symbolizer instanceof TextSymbolizer)    element = SETags.TEXT_SYMBOLIZER;
        else if (symbolizer instanceof RasterSymbolizer)  element = SETags.RASTER_SYMBOLIZER;
        else {
            warning("Unsupported symbolizer: " + symbolizer.getClass().getSimpleName() + ", ignored.");
            return;
        }
        final String ns = ns();
        start(ns, element);
        if (isVersion11()) {
            attribute(SETags.UOM, formatUnit(symbolizer.getUnitOfMeasure()));
            writeName(symbolizer.getName());
            writeDescription(symbolizer.getDescription().orElse(null), false);
        } else if (symbolizer.getName().isPresent() || symbolizer.getDescription().isPresent()) {
            warning("Symbolizer names and descriptions are not supported in version 1.0, ignored.");
        }
        final Expression<R,?> geometry = symbolizer.getGeometry();
        if (geometry != null && !(geometry instanceof ValueReference<R,?> ref && FilterReader.DEFAULT_GEOMETRY.equals(ref.getXPath()))) {
            writeParameter(ns, SETags.GEOMETRY, geometry);
        }
        /*
         * For point and polygon symbolizers, an absent graphic, fill or stroke means that none shall be applied.
         * Therefore, those elements are written even when they have the default values.
         */
        if (symbolizer instanceof PointSymbolizer<R> s) {
            if (s.isVisible()) {
                writeGraphic(s.getGraphic());
            }
        } else if (symbolizer instanceof LineSymbolizer<R> s) {
            final LineSymbolizer<Feature> d = DEFAULTS.createLineSymbolizer();
            final Stroke<R> stroke = s.getStroke();
            if (!stroke.equals(DEFAULTS.createStroke())) {
                writeStroke(stroke);
            }
            writeVersion11Parameter(SETags.PERPENDICULAR_OFFSET, s.getPerpendicularOffset(), d.getPerpendicularOffset());
        } else if (symbolizer instanceof PolygonSymbolizer<R> s) {
            final PolygonSymbolizer<Feature> d = DEFAULTS.createPolygonSymbolizer();
            if (s.getFill().isPresent())   writeFill(s.getFill().get());
            if (s.getStroke().isPresent()) writeStroke(s.getStroke().get());
            final Displacement<R> displacement = s.getDisplacement();
            if (!displacement.equals(d.getDisplacement())) {
                if (isVersion11()) {
                    writeDisplacement(displacement);
                } else {
                    unsupported(SETags.DISPLACEMENT);
                }
            }
            writeVersion11Parameter(SETags.PERPENDICULAR_OFFSET, s.getPerpendicularOffset(), d.getPerpendicularOffset());
        } else if (symbolizer instanceof TextSymbolizer<R> s) {
            writeParameter(ns, SETags.LABEL, s.getLabel());
            final Font<R> font = s.getFont();
            if (!font.equals(DEFAULTS.createFont())) {
                writeFont(font);
            }
            final LabelPlacement<R> placement = s.getLabelPlacement();
            if (!isDefault(placement)) {
                writeLabelPlacement(placement);
            }
            if (s.getHalo().isPresent()) {
                writeHalo(s.getHalo().get());
            }
            final Fill<R> fill = s.getFill();
            if (!fill.equals(DEFAULTS.createTextSymbolizer().getFill())) {
                writeFill(fill);
            }
        } else if (symbolizer instanceof RasterSymbolizer<R> s) {
            final RasterSymbolizer<Feature> d = DEFAULTS.createRasterSymbolizer();
            writeParameter(SETags.OPACITY, s.getOpacity(), d.getOpacity());
            if (s.getChannelSelection().isPresent()) {
                writeChannelSelection(s.getChannelSelection().get());
            }
            final OverlapBehavior overlap = s.getOverlapBehavior();
            if (overlap != d.getOverlapBehavior()) {
                writeOverlapBehavior(overlap);
            }
            if (s.getColorMap().isPresent()) {
                writeColorMap(s.getColorMap().get());
            }
            if (s.getContrastEnhancement().isPresent()) {
                writeContrastEnhancement(s.getContrastEnhancement().get());
            }
            if (s.getShadedRelief().isPresent()) {
                writeShadedRelief(s.getShadedRelief().get());
            }
            if (s.getImageOutline().isPresent()) {
                start(ns, SETags.IMAGE_OUTLINE);
                writeSymbolizer(s.getImageOutline().get());
                end();
            }
        }
        end();
    }

    /**
     * Writes a parameter which exists only in version 1.1.
     * In version 1.0, a warning is emitted if the value is not the default value.
     */
    private <R> void writeVersion11Parameter(final String local, final Expression<R,?> value, final Expression<?,?> defaultValue)
            throws XMLStreamException, DataStoreException
    {
        if (value != null && !value.equals(defaultValue)) {
            if (isVersion11()) {
                writeParameter(ns(), local, value);
            } else {
                unsupported(local);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    //  Strokes, fills and fonts
    // ---------------------------------------------------------------------------------------------

    /**
     * Writes a {@code <Stroke>} element.
     */
    private <R> void writeStroke(final Stroke<R> stroke) throws XMLStreamException, DataStoreException {
        final Stroke<Feature> d = DEFAULTS.createStroke();
        start(ns(), SETags.STROKE);
        if (stroke.getGraphicFill().isPresent()) {
            writeGraphicFill(stroke.getGraphicFill().get());
        } else if (stroke.getGraphicStroke().isPresent()) {
            writeGraphicStroke(stroke.getGraphicStroke().get());
        }
        writeSvgParameter(SETags.STROKE_COLOR,      stroke.getColor(),      d.getColor());
        writeSvgParameter(SETags.STROKE_OPACITY,    stroke.getOpacity(),    d.getOpacity());
        writeSvgParameter(SETags.STROKE_WIDTH,      stroke.getWidth(),      d.getWidth());
        writeSvgParameter(SETags.STROKE_LINEJOIN,   stroke.getLineJoin(),   d.getLineJoin());
        writeSvgParameter(SETags.STROKE_LINECAP,    stroke.getLineCap(),    d.getLineCap());
        writeSvgParameter(SETags.STROKE_DASHARRAY,  stroke.getDashArray().orElse(null), null);
        writeSvgParameter(SETags.STROKE_DASHOFFSET, stroke.getDashOffset(), d.getDashOffset());
        end();
    }

    /**
     * Writes a {@code <Fill>} element.
     */
    private <R> void writeFill(final Fill<R> fill) throws XMLStreamException, DataStoreException {
        final Fill<Feature> d = DEFAULTS.createFill();
        start(ns(), SETags.FILL);
        if (fill.getGraphicFill().isPresent()) {
            writeGraphicFill(fill.getGraphicFill().get());
        }
        writeSvgParameter(SETags.FILL_COLOR,   fill.getColor(),   d.getColor());
        writeSvgParameter(SETags.FILL_OPACITY, fill.getOpacity(), d.getOpacity());
        end();
    }

    /**
     * Writes a {@code <Font>} element.
     */
    private <R> void writeFont(final Font<R> font) throws XMLStreamException, DataStoreException {
        final Font<Feature> d = DEFAULTS.createFont();
        start(ns(), SETags.FONT);
        for (final Expression<R,String> family : font.family()) {
            writeSvgParameter(SETags.FONT_FAMILY, family, null);
        }
        writeSvgParameter(SETags.FONT_STYLE,  font.getStyle(),  d.getStyle());
        writeSvgParameter(SETags.FONT_WEIGHT, font.getWeight(), d.getWeight());
        writeSvgParameter(SETags.FONT_SIZE,   font.getSize(),   d.getSize());
        end();
    }

    /**
     * Writes a {@code <GraphicFill>} element.
     */
    private <R> void writeGraphicFill(final GraphicFill<R> fill) throws XMLStreamException, DataStoreException {
        start(ns(), SETags.GRAPHIC_FILL);
        writeGraphic(fill.getGraphic());
        end();
    }

    /**
     * Writes a {@code <GraphicStroke>} element. The gaps exist only in version 1.1.
     */
    private <R> void writeGraphicStroke(final GraphicStroke<R> stroke) throws XMLStreamException, DataStoreException {
        final GraphicStroke<Feature> d = DEFAULTS.createGraphicStroke();
        start(ns(), SETags.GRAPHIC_STROKE);
        writeGraphic(stroke.getGraphic());
        writeVersion11Parameter(SETags.INITIAL_GAP, stroke.getInitialGap(), d.getInitialGap());
        writeVersion11Parameter(SETags.GAP,         stroke.getGap(),        d.getGap());
        end();
    }

    // ---------------------------------------------------------------------------------------------
    //  Graphics
    // ---------------------------------------------------------------------------------------------

    /**
     * Writes a {@code <Graphic>} element.
     */
    private <R> void writeGraphic(final Graphic<R> graphic) throws XMLStreamException, DataStoreException {
        final Graphic<Feature> d = DEFAULTS.createGraphic();
        start(ns(), SETags.GRAPHIC);
        for (final GraphicalSymbol<R> symbol : graphic.graphicalSymbols()) {
            if (symbol instanceof Mark<R> mark) {
                writeMark(mark);
            } else if (symbol instanceof ExternalGraphic<R> external) {
                writeExternalGraphic(external);
            }
        }
        writeParameter(SETags.OPACITY,  graphic.getOpacity(),  d.getOpacity());
        writeParameter(SETags.SIZE,     graphic.getSize(),     d.getSize());
        writeParameter(SETags.ROTATION, graphic.getRotation(), d.getRotation());
        final AnchorPoint<R> anchor = graphic.getAnchorPoint();
        final Displacement<R> displacement = graphic.getDisplacement();
        if (isVersion11()) {
            if (!anchor.equals(d.getAnchorPoint())) {
                writeAnchorPoint(anchor);
            }
            if (!displacement.equals(d.getDisplacement())) {
                writeDisplacement(displacement);
            }
        } else if (!anchor.equals(d.getAnchorPoint()) || !displacement.equals(d.getDisplacement())) {
            warning("Anchor point and displacement of graphics are not supported in version 1.0, ignored.");
        }
        end();
    }

    /**
     * Writes a {@code <Mark>} element.
     */
    private <R> void writeMark(final Mark<R> mark) throws XMLStreamException, DataStoreException {
        final String ns = ns();
        start(ns, SETags.MARK);
        final OnlineResource online = mark.getOnlineResource().orElse(null);
        final Icon inline = mark.getInlineContent().orElse(null);
        if (isVersion11() && (online != null || inline != null)) {
            if (online != null) {
                writeOnlineResource(ns, online);
            } else {
                writeInlineContent(inline);
            }
            text(ns, SETags.FORMAT, mark.getFormat().orElse(null));
            writeParameter(ns, SETags.MARK_INDEX, mark.getMarkIndex().orElse(null));
        } else {
            if (online != null || inline != null) {
                warning("Marks from external resources are not supported in version 1.0, ignored.");
            }
            writeParameter(ns, SETags.WELL_KNOWN_NAME, mark.getWellKnownName());
        }
        if (mark.getFill().isPresent())   writeFill(mark.getFill().get());
        if (mark.getStroke().isPresent()) writeStroke(mark.getStroke().get());
        end();
    }

    /**
     * Writes an {@code <ExternalGraphic>} element.
     */
    private <R> void writeExternalGraphic(final ExternalGraphic<R> graphic) throws XMLStreamException, DataStoreException {
        final String ns = ns();
        start(ns, SETags.EXTERNAL_GRAPHIC);
        final OnlineResource online = graphic.getOnlineResource().orElse(null);
        final Icon inline = graphic.getInlineContent().orElse(null);
        String format = graphic.getFormat().orElse(null);
        if (online != null) {
            writeOnlineResource(ns, online);
        } else if (inline != null) {
            if (!isVersion11()) {
                unsupported(SETags.INLINE_CONTENT);
            } else if (writeInlineContent(inline) && format == null && !(inline instanceof InlineImage)) {
                format = "image/png";
            }
        }
        text(ns, SETags.FORMAT, format);
        if (!graphic.colorReplacements().isEmpty()) {
            if (isVersion11()) {
                for (final var replacement : graphic.colorReplacements()) {
                    if (replacement.getFunction().isPresent()) {
                        start(ns, SETags.COLOR_REPLACEMENT);
                        filterWriter().writeExpression(replacement.getFunction().get());
                        end();
                    }
                }
            } else {
                unsupported(SETags.COLOR_REPLACEMENT);
            }
        }
        end();
    }

    /**
     * Writes an {@code <AnchorPoint>} element.
     */
    private <R> void writeAnchorPoint(final AnchorPoint<R> anchor) throws XMLStreamException, DataStoreException {
        final String ns = ns();
        start(ns, SETags.ANCHOR_POINT);
        writeParameter(ns, SETags.ANCHOR_POINT_X, anchor.getAnchorPointX());
        writeParameter(ns, SETags.ANCHOR_POINT_Y, anchor.getAnchorPointY());
        end();
    }

    /**
     * Writes a {@code <Displacement>} element.
     */
    private <R> void writeDisplacement(final Displacement<R> displacement) throws XMLStreamException, DataStoreException {
        final String ns = ns();
        start(ns, SETags.DISPLACEMENT);
        writeParameter(ns, SETags.DISPLACEMENT_X, displacement.getDisplacementX());
        writeParameter(ns, SETags.DISPLACEMENT_Y, displacement.getDisplacementY());
        end();
    }

    // ---------------------------------------------------------------------------------------------
    //  Texts
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns whether the given label placement has only default values, in which case it does not need to be written.
     * The comparison uses the values returned by the getter methods, consistently with {@link #writeLabelPlacement},
     * because a property explicitly set to its default value cannot be distinguished from an unset property once
     * written in a XML document. A line placement is never the default, because the default is a point placement.
     *
     * @param  placement  the label placement to test.
     * @return whether writing the given placement can be omitted.
     */
    private static boolean isDefault(final LabelPlacement<?> placement) {
        if (placement instanceof PointPlacement<?> p) {
            final PointPlacement<Feature> d = DEFAULTS.createPointPlacement();
            return p.getAnchorPoint().equals(d.getAnchorPoint())
                && p.getDisplacement().equals(d.getDisplacement())
                && p.getRotation().equals(d.getRotation());
        }
        return false;
    }

    /**
     * Writes a {@code <LabelPlacement>} element.
     */
    private <R> void writeLabelPlacement(final LabelPlacement<R> placement) throws XMLStreamException, DataStoreException {
        final String ns = ns();
        start(ns, SETags.LABEL_PLACEMENT);
        if (placement instanceof PointPlacement<R> p) {
            final PointPlacement<Feature> d = DEFAULTS.createPointPlacement();
            start(ns, SETags.POINT_PLACEMENT);
            if (!p.getAnchorPoint().equals(d.getAnchorPoint())) {
                writeAnchorPoint(p.getAnchorPoint());
            }
            if (!p.getDisplacement().equals(d.getDisplacement())) {
                writeDisplacement(p.getDisplacement());
            }
            writeParameter(SETags.ROTATION, p.getRotation(), d.getRotation());
            end();
        } else if (placement instanceof LinePlacement<R> p) {
            final LinePlacement<Feature> d = DEFAULTS.createLinePlacement();
            start(ns, SETags.LINE_PLACEMENT);
            writeParameter(SETags.PERPENDICULAR_OFFSET, p.getPerpendicularOffset(), d.getPerpendicularOffset());
            writeVersion11Parameter(SETags.IS_REPEATED,     p.isRepeated(),        d.isRepeated());
            writeVersion11Parameter(SETags.INITIAL_GAP,     p.getInitialGap(),     d.getInitialGap());
            writeVersion11Parameter(SETags.GAP,             p.getGap(),            d.getGap());
            writeVersion11Parameter(SETags.IS_ALIGNED,      p.isAligned(),         d.isAligned());
            writeVersion11Parameter(SETags.GENERALIZE_LINE, p.getGeneralizeLine(), d.getGeneralizeLine());
            end();
        }
        end();
    }

    /**
     * Writes a {@code <Halo>} element.
     */
    private <R> void writeHalo(final Halo<R> halo) throws XMLStreamException, DataStoreException {
        final Halo<Feature> d = DEFAULTS.createHalo();
        start(ns(), SETags.HALO);
        writeParameter(SETags.RADIUS, halo.getRadius(), d.getRadius());
        final Fill<R> fill = halo.getFill();
        if (!fill.equals(d.getFill())) {
            writeFill(fill);
        }
        end();
    }

    // ---------------------------------------------------------------------------------------------
    //  Rasters
    // ---------------------------------------------------------------------------------------------

    /**
     * Writes a {@code <ChannelSelection>} element.
     */
    private <R> void writeChannelSelection(final ChannelSelection<R> selection) throws XMLStreamException, DataStoreException {
        final SelectedChannel<R>[] channels = selection.getChannels();
        if (channels == null) {
            return;
        }
        start(ns(), SETags.CHANNEL_SELECTION);
        if (channels.length == 1) {
            writeSelectedChannel(SETags.GRAY_CHANNEL, channels[0]);
        } else {
            writeSelectedChannel(SETags.RED_CHANNEL,   channels[0]);
            writeSelectedChannel(SETags.GREEN_CHANNEL, channels[1]);
            writeSelectedChannel(SETags.BLUE_CHANNEL,  channels[2]);
        }
        end();
    }

    /**
     * Writes a {@code <RedChannel>}, {@code <GreenChannel>}, {@code <BlueChannel>} or {@code <GrayChannel>} element.
     * Does nothing if the given channel is null.
     */
    private <R> void writeSelectedChannel(final String element, final SelectedChannel<R> channel)
            throws XMLStreamException, DataStoreException
    {
        if (channel != null) {
            final String ns = ns();
            start(ns, element);
            writeParameter(ns, SETags.SOURCE_CHANNEL_NAME, channel.getSourceChannelName());
            if (channel.getContrastEnhancement().isPresent()) {
                writeContrastEnhancement(channel.getContrastEnhancement().get());
            }
            end();
        }
    }

    /**
     * Writes a {@code <ContrastEnhancement>} element.
     */
    private <R> void writeContrastEnhancement(final ContrastEnhancement<R> contrast) throws XMLStreamException, DataStoreException {
        final String ns = ns();
        start(ns, SETags.CONTRAST_ENHANCEMENT);
        final ContrastMethod method = contrast.getMethod();
        if (method == ContrastMethod.NORMALIZE) {
            empty(ns, SETags.NORMALIZE);
        } else if (method == ContrastMethod.HISTOGRAM) {
            empty(ns, SETags.HISTOGRAM);
        }
        writeParameter(SETags.GAMMA_VALUE, contrast.getGammaValue(), DEFAULTS.createContrastEnhancement().getGammaValue());
        end();
    }

    /**
     * Writes a {@code <ShadedRelief>} element.
     */
    private <R> void writeShadedRelief(final ShadedRelief<R> relief) throws XMLStreamException, DataStoreException {
        final ShadedRelief<Feature> d = DEFAULTS.createShadedRelief();
        start(ns(), SETags.SHADED_RELIEF);
        writeParameter(SETags.BRIGHTNESS_ONLY, relief.isBrightnessOnly(), d.isBrightnessOnly());
        writeParameter(SETags.RELIEF_FACTOR,   relief.getReliefFactor(),  d.getReliefFactor());
        end();
    }

}
