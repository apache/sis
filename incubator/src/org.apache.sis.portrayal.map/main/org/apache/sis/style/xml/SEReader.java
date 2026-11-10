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

import java.awt.Color;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.measure.Unit;
import javax.swing.Icon;
import javax.xml.stream.XMLStreamException;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.util.InternationalString;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.style.se1.AbstractStyle;
import org.apache.sis.style.se1.AnchorPoint;
import org.apache.sis.style.se1.ChannelSelection;
import org.apache.sis.style.se1.ColorMap;
import org.apache.sis.style.se1.ColorReplacement;
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
import org.apache.sis.style.se1.Halo;
import org.apache.sis.style.se1.LabelPlacement;
import org.apache.sis.style.se1.LegendGraphic;
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
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.TextSymbolizer;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.function.Interpolate;
import org.apache.sis.style.se1.function.Recode;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.iso.Names;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;


/**
 * Base class of the readers of the symbology elements: feature type styles, coverage styles, rules and symbolizers.
 * The filters and expressions are read by the {@linkplain #filterReader() filter reader} of the same version.
 * This class contains the reading code shared by {@link SEReader100} and {@link SEReader110}.
 * That code is lenient: it accepts the encodings of both Styled Layer Descriptor 1.0
 * and Symbology Encoding 1.1 in the same document.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class SEReader extends AbstractStaxReader {
    /**
     * Creates a new reader sharing the cursor, the factories and the warnings of the given reader.
     *
     * @param  parent  the style reader which contains this symbology reader.
     */
    protected SEReader(final AbstractStaxReader parent) {
        super(parent);
    }

    /**
     * Returns the reader of filters and expressions of the same version as this reader.
     *
     * @return the filter reader used by this symbology reader.
     */
    abstract FilterReader filterReader();

    /**
     * Reads a parameter value, which is a mixed content of texts and expressions.
     * This method delegates to the {@linkplain #filterReader() filter reader}.
     *
     * @param  <R>  type of resources on which the expression applies.
     * @param  f    the factories to use.
     * @return the expression, or {@code null} if the parameter is empty.
     *
     * @see FilterReader#readParameterValue(Factories)
     */
    private <R> Expression<R,?> readParameterValue(final Factories<R> f) throws XMLStreamException, DataStoreException {
        return filterReader().readParameterValue(f);
    }

    // ---------------------------------------------------------------------------------------------
    //  Elements shared by many style elements
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the given text as an international string, or {@code null} if empty.
     */
    private static InternationalString international(final String text) {
        return text.isEmpty() ? null : new SimpleInternationalString(text);
    }

    /**
     * Reads the {@code <Description>} element at the current cursor position (Symbology Encoding 1.1).
     *
     * @param  <R>  type of resources to style.
     * @param  f    the factories to use.
     * @return the description.
     */
    final <R> Description<R> readDescription(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final Description<R> description = f.style().createDescription();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.TITLE:
                case SETags.ABSTRACT: readTitleOrAbstract(description); break;
                default: unexpected(SETags.DESCRIPTION); break;
            }
        }
        return description;
    }

    /**
     * Reads the {@code <Title>} or {@code <Abstract>} element at the current cursor position and stores
     * its value in the given description. In SLD 1.0, those elements are directly in the described element.
     *
     * @param  description  the description where to store the title or abstract.
     */
    final void readTitleOrAbstract(final Description<?> description) throws XMLStreamException, DataStoreException {
        final boolean isTitle = SETags.TITLE.equals(reader.getLocalName());
        final InternationalString text = international(readText());
        if (isTitle) {
            description.setTitle(text);
        } else {
            description.setAbstract(text);
        }
    }

    /**
     * Reads the {@code <OnlineResource>} element at the current cursor position.
     *
     * @return the online resource, or {@code null} if the link is missing or invalid.
     */
    final OnlineResource readOnlineResource() throws XMLStreamException {
        String href = reader.getAttributeValue(FilterTags.XLINK_NAMESPACE, FilterTags.HREF);
        if (href == null) {
            href = reader.getAttributeValue(null, FilterTags.HREF);
        }
        skipElement();
        if (href != null && !href.isBlank()) try {
            return new DefaultOnlineResource(new URI(href.trim()));
        } catch (URISyntaxException e) {
            warning("Invalid link in <" + SETags.ONLINE_RESOURCE + ">: " + e.getMessage());
            return null;
        }
        warning("Missing link in <" + SETags.ONLINE_RESOURCE + ">.");
        return null;
    }

    /**
     * Reads the {@code <InlineContent>} element at the current cursor position.
     * Only the base64 encoding of images is supported.
     *
     * @return the image, or {@code null} if it cannot be decoded.
     */
    private Icon readInlineContent() throws XMLStreamException, DataStoreException {
        final String encoding = attribute(SETags.ENCODING);
        if (encoding != null && !SETags.BASE64.equalsIgnoreCase(encoding)) {
            warning("Unsupported encoding of <" + SETags.INLINE_CONTENT + ">: \"" + encoding + "\".");
            skipElement();
            return null;
        }
        try {
            return new InlineImage(Base64.getMimeDecoder().decode(readText()));
        } catch (IllegalArgumentException e) {
            warning("Invalid base64 content in <" + SETags.INLINE_CONTENT + ">: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses the unit of measurement in the {@code uom} attribute of a symbolizer.
     *
     * @param  uom  the unit of measurement, as a URI or as a unit symbol.
     * @return the unit, or {@code null} if unrecognized.
     */
    private Unit<?> parseUnit(final String uom) {
        String key = uom;
        if (key.startsWith(SETags.UOM_PREFIX)) {
            key = key.substring(SETags.UOM_PREFIX.length());
        }
        switch (key) {
            case SETags.METRE: case "meter": return Units.METRE;
            case SETags.FOOT:                return Units.FOOT;
            case SETags.PIXEL:               return Units.PIXEL;
        }
        try {
            return Units.valueOf(key);
        } catch (RuntimeException e) {
            warning("Unsupported unit of measurement: \"" + uom + "\".");
            return null;
        }
    }

    /**
     * Parses a semantic type identifier such as {@code "generic:point"}.
     *
     * @param  text  the semantic type identifier.
     * @return the semantic type, or {@code null} if unrecognized.
     */
    private SemanticType parseSemanticType(String text) {
        if (text.regionMatches(true, 0, SETags.GENERIC, 0, SETags.GENERIC.length())) {
            text = text.substring(SETags.GENERIC.length());
        }
        return keyword(text, SemanticType.class);
    }

    // ---------------------------------------------------------------------------------------------
    //  Styles and rules
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the {@code <FeatureTypeStyle>} element at the current cursor position.
     *
     * @return the feature type style.
     */
    final FeatureTypeStyle readFeatureTypeStyle() throws XMLStreamException, DataStoreException {
        final var style = new FeatureTypeStyle();
        readStyle(style, features, SETags.FEATURE_TYPE_STYLE);
        return style;
    }

    /**
     * Reads the {@code <CoverageStyle>} element at the current cursor position.
     *
     * @return the coverage style.
     */
    final CoverageStyle readCoverageStyle() throws XMLStreamException, DataStoreException {
        final var style = new CoverageStyle();
        readStyle(style, coverages, SETags.COVERAGE_STYLE);
        return style;
    }

    /**
     * Reads the content of a feature type style or coverage style.
     *
     * @param  <R>      type of resources to style.
     * @param  style    the style where to store the values.
     * @param  f        the factories to use.
     * @param  element  local name of the style element, for warning messages.
     */
    private <R> void readStyle(final AbstractStyle<R> style, final Factories<R> f, final String element)
            throws XMLStreamException, DataStoreException
    {
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.NAME: {
                    style.setName(readTextOrNull());
                    break;
                }
                case SETags.DESCRIPTION: {
                    style.setDescription(readDescription(f));
                    break;
                }
                case SETags.TITLE:
                case SETags.ABSTRACT: {
                    Description<R> description = style.getDescription().orElse(null);
                    if (description == null) {
                        style.setDescription(description = f.style().createDescription());
                    }
                    readTitleOrAbstract(description);
                    break;
                }
                case SETags.FEATURE_TYPE_NAME:
                case SETags.COVERAGE_NAME: {
                    final String name = readTextOrNull();
                    style.setFeatureTypeName((name != null) ? Names.createLocalName(null, null, name) : null);
                    break;
                }
                case SETags.SEMANTIC_TYPE_IDENTIFIER: {
                    final SemanticType type = parseSemanticType(readText());
                    if (type != null) {
                        style.semanticTypeIdentifiers().add(type);
                    }
                    break;
                }
                case SETags.RULE: {
                    style.rules().add(readRule(f));
                    break;
                }
                case SETags.ONLINE_RESOURCE: {
                    // A link to a rule defined in another document. The link is not resolved.
                    final Rule<R> rule = f.style().createRule();
                    rule.setOnlineSource(readOnlineResource());
                    style.rules().add(rule);
                    break;
                }
                default: {
                    unexpected(element);
                    break;
                }
            }
        }
    }

    /**
     * Reads the {@code <Rule>} element at the current cursor position.
     *
     * @param  <R>  type of resources to style.
     * @param  f    the factories to use.
     * @return the rule.
     */
    final <R> Rule<R> readRule(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final Rule<R> rule = f.style().createRule();
        while (nextChild()) {
            final String name = reader.getLocalName();
            switch (name) {
                case SETags.NAME: {
                    rule.setName(readTextOrNull());
                    break;
                }
                case SETags.DESCRIPTION: {
                    rule.setDescription(readDescription(f));
                    break;
                }
                case SETags.TITLE:
                case SETags.ABSTRACT: {
                    Description<R> description = rule.getDescription().orElse(null);
                    if (description == null) {
                        rule.setDescription(description = f.style().createDescription());
                    }
                    readTitleOrAbstract(description);
                    break;
                }
                case SETags.LEGEND_GRAPHIC: {
                    final LegendGraphic<R> legend = f.style().createLegendGraphic();
                    while (nextChild()) {
                        if (SETags.GRAPHIC.equals(reader.getLocalName())) {
                            legend.setGraphic(readGraphic(f));
                        } else {
                            unexpected(name);
                        }
                    }
                    rule.setLegend(legend);
                    break;
                }
                case FilterTags.FILTER: {
                    rule.setFilter(filterReader().readFilter(f));
                    break;
                }
                case SETags.ELSE_FILTER: {
                    skipElement();
                    rule.setElseFilter(true);
                    break;
                }
                case SETags.MIN_SCALE_DENOMINATOR: {
                    rule.setMinScaleDenominator(parseDouble(readText(), name));
                    break;
                }
                case SETags.MAX_SCALE_DENOMINATOR: {
                    rule.setMaxScaleDenominator(parseDouble(readText(), name));
                    break;
                }
                default: {
                    final Symbolizer<R> symbolizer = readSymbolizer(f);
                    if (symbolizer != null) {
                        rule.symbolizers().add(symbolizer);
                    } else {
                        unexpected(SETags.RULE);
                    }
                    break;
                }
            }
        }
        return rule;
    }

    // ---------------------------------------------------------------------------------------------
    //  Symbolizers
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the symbolizer at the current cursor position. If the current element is not a symbolizer,
     * then this method returns {@code null} without moving the cursor.
     *
     * @param  <R>  type of resources to style.
     * @param  f    the factories to use.
     * @return the symbolizer, or {@code null} if the current element is not a symbolizer.
     */
    final <R> Symbolizer<R> readSymbolizer(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final String name = reader.getLocalName();
        final Symbolizer<R> symbolizer;
        switch (name) {
            case SETags.POINT_SYMBOLIZER:   symbolizer = f.style().createPointSymbolizer();   break;
            case SETags.LINE_SYMBOLIZER:    symbolizer = f.style().createLineSymbolizer();    break;
            case SETags.POLYGON_SYMBOLIZER: symbolizer = f.style().createPolygonSymbolizer(); break;
            case SETags.TEXT_SYMBOLIZER:    symbolizer = f.style().createTextSymbolizer();    break;
            case SETags.RASTER_SYMBOLIZER:  symbolizer = f.style().createRasterSymbolizer();  break;
            default: return null;
        }
        /*
         * OGC 05-077r4 said that if the graphic, fill or stroke is not specified, then none should be applied.
         * This is different than the default values of symbolizers created by the API.
         */
        if (symbolizer instanceof PointSymbolizer<R> s) {
            s.setGraphic(null);
        } else if (symbolizer instanceof PolygonSymbolizer<R> s) {
            s.setFill(null);
            s.setStroke(null);
        }
        final String uom = attribute(SETags.UOM);
        if (uom != null) try {
            symbolizer.setUnitOfMeasure(parseUnit(uom));
        } catch (IllegalArgumentException e) {
            warning(e.getMessage());
        }
        while (nextChild()) {
            final String child = reader.getLocalName();
            switch (child) {
                case SETags.NAME: {
                    symbolizer.setName(readTextOrNull());
                    continue;
                }
                case SETags.DESCRIPTION: {
                    symbolizer.setDescription(readDescription(f));
                    continue;
                }
                case SETags.TITLE:
                case SETags.ABSTRACT: {
                    Description<R> description = symbolizer.getDescription().orElse(null);
                    if (description == null) {
                        symbolizer.setDescription(description = f.style().createDescription());
                    }
                    readTitleOrAbstract(description);
                    continue;
                }
                case SETags.GEOMETRY: {
                    final Expression<R,?> geometry = readParameterValue(f);
                    if (geometry != null) {
                        symbolizer.setGeometry(geometry);
                    }
                    continue;
                }
                case SETags.BASE_SYMBOLIZER: {
                    warning("The <" + child + "> element is not supported.");
                    skipElement();
                    continue;
                }
            }
            if (!readSymbolizerProperty(symbolizer, f, child)) {
                unexpected(name);
            }
        }
        return symbolizer;
    }

    /**
     * Reads a property specific to the given type of symbolizer.
     *
     * @param  <R>         type of resources to style.
     * @param  symbolizer  the symbolizer where to store the property.
     * @param  f           the factories to use.
     * @param  child       local name of the element at the current cursor position.
     * @return whether the element has been recognized and read.
     */
    private <R> boolean readSymbolizerProperty(final Symbolizer<R> symbolizer, final Factories<R> f, final String child)
            throws XMLStreamException, DataStoreException
    {
        if (symbolizer instanceof PointSymbolizer<R> s) {
            if (SETags.GRAPHIC.equals(child)) {
                s.setGraphic(readGraphic(f));
                return true;
            }
        } else if (symbolizer instanceof LineSymbolizer<R> s) {
            switch (child) {
                case SETags.STROKE:               s.setStroke(readStroke(f)); return true;
                case SETags.PERPENDICULAR_OFFSET: s.setPerpendicularOffset(asNumber(f, readParameterValue(f))); return true;
            }
        } else if (symbolizer instanceof PolygonSymbolizer<R> s) {
            switch (child) {
                case SETags.FILL:                 s.setFill(readFill(f)); return true;
                case SETags.STROKE:               s.setStroke(readStroke(f)); return true;
                case SETags.DISPLACEMENT:         s.setDisplacement(readDisplacement(f)); return true;
                case SETags.PERPENDICULAR_OFFSET: s.setPerpendicularOffset(asNumber(f, readParameterValue(f))); return true;
            }
        } else if (symbolizer instanceof TextSymbolizer<R> s) {
            switch (child) {
                case SETags.LABEL:  s.setLabel(asString(f, readParameterValue(f))); return true;
                case SETags.FONT:   s.setFont(readFont(f)); return true;
                case SETags.HALO:   s.setHalo(readHalo(f)); return true;
                case SETags.FILL:   s.setFill(readFill(f)); return true;
                case SETags.LABEL_PLACEMENT: {
                    final LabelPlacement<R> placement = readLabelPlacement(f);
                    if (placement != null) {
                        s.setLabelPlacement(placement);
                    }
                    return true;
                }
            }
        } else if (symbolizer instanceof RasterSymbolizer<R> s) {
            switch (child) {
                case SETags.OPACITY:              s.setOpacity(asNumber(f, readParameterValue(f))); return true;
                case SETags.CHANNEL_SELECTION:    s.setChannelSelection(readChannelSelection(f)); return true;
                case SETags.COLOR_MAP:            s.setColorMap(readColorMap(f)); return true;
                case SETags.CONTRAST_ENHANCEMENT: s.setContrastEnhancement(readContrastEnhancement(f)); return true;
                case SETags.SHADED_RELIEF:        s.setShadedRelief(readShadedRelief(f)); return true;
                case SETags.OVERLAP_BEHAVIOR: {
                    final OverlapBehavior value = keyword(readTextOrChildName(), OverlapBehavior.class);
                    if (value != null) {
                        s.setOverlapBehavior(value);
                    }
                    return true;
                }
                case SETags.IMAGE_OUTLINE: {
                    while (nextChild()) {
                        final Symbolizer<R> outline = readSymbolizer(f);
                        if (outline != null) {
                            s.setImageOutline(outline);
                        } else {
                            unexpected(child);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    //  Strokes, fills and fonts
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the {@code <Stroke>} element at the current cursor position.
     */
    private <R> Stroke<R> readStroke(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final Stroke<R> stroke = f.style().createStroke();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.GRAPHIC_FILL:   stroke.setGraphicFill(readGraphicFill(f)); break;
                case SETags.GRAPHIC_STROKE: stroke.setGraphicStroke(readGraphicStroke(f)); break;
                case SETags.SVG_PARAMETER:
                case SLDTags.CSS_PARAMETER: {
                    final String name = attribute(SETags.PARAMETER_NAME);
                    final Expression<R,?> value = readParameterValue(f);
                    switch (String.valueOf(name)) {
                        case SETags.STROKE_COLOR:      stroke.setColor     (asColor  (f, value)); break;
                        case SETags.STROKE_OPACITY:    stroke.setOpacity   (asNumber (f, value)); break;
                        case SETags.STROKE_WIDTH:      stroke.setWidth     (asNumber (f, value)); break;
                        case SETags.STROKE_LINEJOIN:   stroke.setLineJoin  (asString (f, value)); break;
                        case SETags.STROKE_LINECAP:    stroke.setLineCap   (asString (f, value)); break;
                        case SETags.STROKE_DASHARRAY:  stroke.setDashArray (asFloats (f, value)); break;
                        case SETags.STROKE_DASHOFFSET: stroke.setDashOffset(asInteger(f, value)); break;
                        default: unknownParameter(SETags.STROKE, name); break;
                    }
                    break;
                }
                default: unexpected(SETags.STROKE); break;
            }
        }
        return stroke;
    }

    /**
     * Reads the {@code <Fill>} element at the current cursor position.
     */
    private <R> Fill<R> readFill(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final Fill<R> fill = f.style().createFill();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.GRAPHIC_FILL: fill.setGraphicFill(readGraphicFill(f)); break;
                case SETags.SVG_PARAMETER:
                case SLDTags.CSS_PARAMETER: {
                    final String name = attribute(SETags.PARAMETER_NAME);
                    final Expression<R,?> value = readParameterValue(f);
                    switch (String.valueOf(name)) {
                        case SETags.FILL_COLOR:   fill.setColor  (asColor (f, value)); break;
                        case SETags.FILL_OPACITY: fill.setOpacity(asNumber(f, value)); break;
                        default: unknownParameter(SETags.FILL, name); break;
                    }
                    break;
                }
                default: unexpected(SETags.FILL); break;
            }
        }
        return fill;
    }

    /**
     * Reads the {@code <Font>} element at the current cursor position.
     */
    private <R> Font<R> readFont(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final Font<R> font = f.style().createFont();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.SVG_PARAMETER:
                case SLDTags.CSS_PARAMETER: {
                    final String name = attribute(SETags.PARAMETER_NAME);
                    final Expression<R,?> value = readParameterValue(f);
                    switch (String.valueOf(name)) {
                        case SETags.FONT_FAMILY: if (value != null) font.family().add(asString(f, value)); break;
                        case SETags.FONT_STYLE:  font.setStyle (asString(f, value)); break;
                        case SETags.FONT_WEIGHT: font.setWeight(asString(f, value)); break;
                        case SETags.FONT_SIZE:   font.setSize  (asNumber(f, value)); break;
                        default: unknownParameter(SETags.FONT, name); break;
                    }
                    break;
                }
                default: unexpected(SETags.FONT); break;
            }
        }
        return font;
    }

    /**
     * Emits a warning for an unknown SVG or CSS parameter.
     */
    private void unknownParameter(final String element, final String name) {
        warning("Unknown parameter \"" + name + "\" in <" + element + ">, ignored.");
    }

    /**
     * Reads the {@code <GraphicFill>} element at the current cursor position.
     */
    private <R> GraphicFill<R> readGraphicFill(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final GraphicFill<R> fill = f.style().createGraphicFill();
        while (nextChild()) {
            if (SETags.GRAPHIC.equals(reader.getLocalName())) {
                fill.setGraphic(readGraphic(f));
            } else {
                unexpected(SETags.GRAPHIC_FILL);
            }
        }
        return fill;
    }

    /**
     * Reads the {@code <GraphicStroke>} element at the current cursor position.
     */
    private <R> GraphicStroke<R> readGraphicStroke(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final GraphicStroke<R> stroke = f.style().createGraphicStroke();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.GRAPHIC:     stroke.setGraphic(readGraphic(f)); break;
                case SETags.INITIAL_GAP: stroke.setInitialGap(asNumber(f, readParameterValue(f))); break;
                case SETags.GAP:         stroke.setGap(asNumber(f, readParameterValue(f))); break;
                default: unexpected(SETags.GRAPHIC_STROKE); break;
            }
        }
        return stroke;
    }

    // ---------------------------------------------------------------------------------------------
    //  Graphics
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the {@code <Graphic>} element at the current cursor position.
     */
    private <R> Graphic<R> readGraphic(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final Graphic<R> graphic = f.style().createGraphic();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.EXTERNAL_GRAPHIC: graphic.graphicalSymbols().add(readExternalGraphic(f)); break;
                case SETags.MARK:             graphic.graphicalSymbols().add(readMark(f)); break;
                case SETags.OPACITY:          graphic.setOpacity (asNumber(f, readParameterValue(f))); break;
                case SETags.SIZE:             graphic.setSize    (asNumber(f, readParameterValue(f))); break;
                case SETags.ROTATION:         graphic.setRotation(asNumber(f, readParameterValue(f))); break;
                case SETags.ANCHOR_POINT:     graphic.setAnchorPoint(readAnchorPoint(f)); break;
                case SETags.DISPLACEMENT:     graphic.setDisplacement(readDisplacement(f)); break;
                default: unexpected(SETags.GRAPHIC); break;
            }
        }
        return graphic;
    }

    /**
     * Reads the {@code <Mark>} element at the current cursor position.
     */
    private <R> Mark<R> readMark(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final Mark<R> mark = f.style().createMark();
        mark.setFill(null);         // If not specified, then no fill or stroke shall be applied.
        mark.setStroke(null);
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.WELL_KNOWN_NAME: mark.setWellKnownName(asString(f, readParameterValue(f))); break;
                case SETags.ONLINE_RESOURCE: mark.setOnlineResource(readOnlineResource()); break;
                case SETags.INLINE_CONTENT:  mark.setInlineContent(readInlineContent()); break;
                case SETags.FORMAT:          mark.setFormat(readTextOrNull()); break;
                case SETags.MARK_INDEX:      mark.setMarkIndex(asInteger(f, readParameterValue(f))); break;
                case SETags.FILL:            mark.setFill(readFill(f)); break;
                case SETags.STROKE:          mark.setStroke(readStroke(f)); break;
                default: unexpected(SETags.MARK); break;
            }
        }
        return mark;
    }

    /**
     * Reads the {@code <ExternalGraphic>} element at the current cursor position.
     */
    private <R> ExternalGraphic<R> readExternalGraphic(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final ExternalGraphic<R> graphic = f.style().createExternalGraphic();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.ONLINE_RESOURCE: graphic.setOnlineResource(readOnlineResource()); break;
                case SETags.INLINE_CONTENT:  graphic.setInlineContent(readInlineContent()); break;
                case SETags.FORMAT:          graphic.setFormat(readTextOrNull()); break;
                case SETags.COLOR_REPLACEMENT: {
                    final ColorReplacement<R> replacement = f.style().createColorReplacement();
                    while (nextChild()) {
                        final Expression<R,?> function = filterReader().readExpression(f);
                        if (function != null) {
                            replacement.setFunction(function);
                        }
                    }
                    graphic.colorReplacements().add(replacement);
                    break;
                }
                default: unexpected(SETags.EXTERNAL_GRAPHIC); break;
            }
        }
        return graphic;
    }

    /**
     * Reads the {@code <AnchorPoint>} element at the current cursor position.
     */
    private <R> AnchorPoint<R> readAnchorPoint(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final AnchorPoint<R> anchor = f.style().createAnchorPoint();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.ANCHOR_POINT_X: anchor.setAnchorPointX(asNumber(f, readParameterValue(f))); break;
                case SETags.ANCHOR_POINT_Y: anchor.setAnchorPointY(asNumber(f, readParameterValue(f))); break;
                default: unexpected(SETags.ANCHOR_POINT); break;
            }
        }
        return anchor;
    }

    /**
     * Reads the {@code <Displacement>} element at the current cursor position.
     */
    private <R> Displacement<R> readDisplacement(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final Displacement<R> displacement = f.style().createDisplacement();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.DISPLACEMENT_X: displacement.setDisplacementX(asNumber(f, readParameterValue(f))); break;
                case SETags.DISPLACEMENT_Y: displacement.setDisplacementY(asNumber(f, readParameterValue(f))); break;
                default: unexpected(SETags.DISPLACEMENT); break;
            }
        }
        return displacement;
    }

    // ---------------------------------------------------------------------------------------------
    //  Texts
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the {@code <LabelPlacement>} element at the current cursor position.
     *
     * @return the label placement, or {@code null} if the element is empty.
     */
    private <R> LabelPlacement<R> readLabelPlacement(final Factories<R> f) throws XMLStreamException, DataStoreException {
        LabelPlacement<R> placement = null;
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.POINT_PLACEMENT: {
                    final PointPlacement<R> p = f.style().createPointPlacement();
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.ANCHOR_POINT: p.setAnchorPoint(readAnchorPoint(f)); break;
                            case SETags.DISPLACEMENT: p.setDisplacement(readDisplacement(f)); break;
                            case SETags.ROTATION:     p.setRotation(asNumber(f, readParameterValue(f))); break;
                            default: unexpected(SETags.POINT_PLACEMENT); break;
                        }
                    }
                    placement = p;
                    break;
                }
                case SETags.LINE_PLACEMENT: {
                    final LinePlacement<R> p = f.style().createLinePlacement();
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.PERPENDICULAR_OFFSET: p.setPerpendicularOffset(asNumber (f, readParameterValue(f))); break;
                            case SETags.IS_REPEATED:          p.setRepeated           (asBoolean(f, readParameterValue(f))); break;
                            case SETags.INITIAL_GAP:          p.setInitialGap         (asNumber (f, readParameterValue(f))); break;
                            case SETags.GAP:                  p.setGap                (asNumber (f, readParameterValue(f))); break;
                            case SETags.IS_ALIGNED:           p.setAligned            (asBoolean(f, readParameterValue(f))); break;
                            case SETags.GENERALIZE_LINE:      p.setGeneralizeLine     (asBoolean(f, readParameterValue(f))); break;
                            default: unexpected(SETags.LINE_PLACEMENT); break;
                        }
                    }
                    placement = p;
                    break;
                }
                default: unexpected(SETags.LABEL_PLACEMENT); break;
            }
        }
        return placement;
    }

    /**
     * Reads the {@code <Halo>} element at the current cursor position.
     */
    private <R> Halo<R> readHalo(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final Halo<R> halo = f.style().createHalo();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.RADIUS: halo.setRadius(asNumber(f, readParameterValue(f))); break;
                case SETags.FILL:   halo.setFill(readFill(f)); break;
                default: unexpected(SETags.HALO); break;
            }
        }
        return halo;
    }

    // ---------------------------------------------------------------------------------------------
    //  Rasters (channels, color maps, contrast and relief)
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the {@code <ChannelSelection>} element at the current cursor position.
     */
    private <R> ChannelSelection<R> readChannelSelection(final Factories<R> f) throws XMLStreamException, DataStoreException {
        SelectedChannel<R> red = null, green = null, blue = null, gray = null;
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.RED_CHANNEL:   red   = readSelectedChannel(f); break;
                case SETags.GREEN_CHANNEL: green = readSelectedChannel(f); break;
                case SETags.BLUE_CHANNEL:  blue  = readSelectedChannel(f); break;
                case SETags.GRAY_CHANNEL:  gray  = readSelectedChannel(f); break;
                default: unexpected(SETags.CHANNEL_SELECTION); break;
            }
        }
        final ChannelSelection<R> selection = f.style().createChannelSelection();
        if (red != null || green != null || blue != null) {
            if (gray != null) {
                warning("Both RGB and gray channels are specified. The gray channel is ignored.");
            }
            selection.setChannels(red, green, blue);
        } else if (gray != null) {
            selection.setChannels(gray);
        }
        return selection;
    }

    /**
     * Reads a {@code <RedChannel>}, {@code <GreenChannel>}, {@code <BlueChannel>} or {@code <GrayChannel>} element.
     */
    private <R> SelectedChannel<R> readSelectedChannel(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final String name = reader.getLocalName();
        final SelectedChannel<R> channel = f.style().createSelectedChannel();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.SOURCE_CHANNEL_NAME:  channel.setSourceChannelName(asString(f, readParameterValue(f))); break;
                case SETags.CONTRAST_ENHANCEMENT: channel.setContrastEnhancement(readContrastEnhancement(f)); break;
                default: unexpected(name); break;
            }
        }
        return channel;
    }

    /**
     * Reads the {@code <ContrastEnhancement>} element at the current cursor position.
     */
    private <R> ContrastEnhancement<R> readContrastEnhancement(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final ContrastEnhancement<R> contrast = f.style().createContrastEnhancement();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.NORMALIZE:   contrast.setMethod(ContrastMethod.NORMALIZE); skipElement(); break;
                case SETags.HISTOGRAM:   contrast.setMethod(ContrastMethod.HISTOGRAM); skipElement(); break;
                case SETags.GAMMA_VALUE: contrast.setGammaValue(asNumber(f, readParameterValue(f))); break;
                default: unexpected(SETags.CONTRAST_ENHANCEMENT); break;
            }
        }
        return contrast;
    }

    /**
     * Reads the {@code <ShadedRelief>} element at the current cursor position.
     */
    private <R> ShadedRelief<R> readShadedRelief(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final ShadedRelief<R> relief = f.style().createShadedRelief();
        while (nextChild()) {
            switch (reader.getLocalName()) {
                case SETags.BRIGHTNESS_ONLY: relief.setBrightnessOnly(asBoolean(f, readParameterValue(f))); break;
                case SETags.RELIEF_FACTOR:   relief.setReliefFactor(asNumber(f, readParameterValue(f))); break;
                default: unexpected(SETags.SHADED_RELIEF); break;
            }
        }
        return relief;
    }

    /**
     * An entry of a SLD 1.0 color map.
     *
     * @param quantity  the value in the raster data.
     * @param color     the color associated to the value, including the opacity.
     */
    private record ColorMapEntry(double quantity, Color color) {
    }

    /**
     * Reads the {@code <ColorMap>} element at the current cursor position.
     * In Symbology Encoding 1.1, the color map contains a {@code <Categorize>} or {@code <Interpolate>} function.
     * In Styled Layer Descriptor 1.0, the color map contains a list of {@code <ColorMapEntry>} elements, which
     * are converted to a function. The conversion depends on the {@code type} attribute (an extension commonly
     * used with SLD 1.0): {@code "ramp"} (the default) for interpolations, {@code "intervals"} for categories
     * and {@code "values"} for recoding.
     */
    private <R> ColorMap<R> readColorMap(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final String type = attribute(SLDTags.TYPE);
        final ColorMap<R> colorMap = f.style().createColorMap();
        final var entries = new ArrayList<ColorMapEntry>();
        while (nextChild()) {
            if (SLDTags.COLOR_MAP_ENTRY.equals(reader.getLocalName())) {
                final ColorMapEntry entry = readColorMapEntry();
                if (entry != null) {
                    entries.add(entry);
                }
            } else {
                final Expression<R,?> function = filterReader().readExpression(f);
                if (function != null) {
                    colorMap.setFunction(function);
                }
            }
        }
        if (!entries.isEmpty()) {
            colorMap.setFunction(toFunction(f, type, entries));
        }
        return colorMap;
    }

    /**
     * Reads the {@code <ColorMapEntry>} element at the current cursor position.
     *
     * @return the entry, or {@code null} if the color or the quantity is missing.
     */
    private ColorMapEntry readColorMapEntry() throws XMLStreamException, DataStoreException {
        final String colorText    = attribute(SLDTags.COLOR);
        final String opacityText  = attribute(SLDTags.OPACITY);
        final String quantityText = attribute(SLDTags.QUANTITY);
        skipElement();
        if (colorText == null || quantityText == null) {
            warning("Incomplete <" + SLDTags.COLOR_MAP_ENTRY + ">, ignored.");
            return null;
        }
        Color color;
        try {
            color = parseColor(colorText);
        } catch (NumberFormatException e) {
            throw new DataStoreContentException("Invalid color in <" + SLDTags.COLOR_MAP_ENTRY + ">: \"" + colorText + "\".", e);
        }
        if (opacityText != null) {
            final double opacity = parseDouble(opacityText, SLDTags.OPACITY);
            final int alpha = (int) Math.round(Math.max(0, Math.min(1, opacity)) * 255);
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        }
        return new ColorMapEntry(parseDouble(quantityText, SLDTags.QUANTITY), color);
    }

    /**
     * Converts the entries of a SLD 1.0 color map to a Symbology Encoding function.
     *
     * @param  type     the {@code type} attribute of the color map, or {@code null} if none.
     * @param  entries  the color map entries.
     * @return the function.
     */
    private <R> Expression<R,?> toFunction(final Factories<R> f, final String type, final List<ColorMapEntry> entries) {
        final FilterFactory<R,Object,Object> ff = f.filter();
        if (SLDTags.INTERVALS.equalsIgnoreCase(type)) {
            /*
             * Each entry gives the color of values below its quantity (and above the quantity of the previous entry).
             * Values greater than or equal to the last quantity are transparent.
             */
            final var values     = new ArrayList<Expression<R,?>>();
            final var thresholds = new ArrayList<Expression<R,?>>();
            for (final ColorMapEntry entry : entries) {
                values.add(ff.literal(entry.color()));
                thresholds.add(ff.literal(entry.quantity()));
            }
            values.add(ff.literal(new Color(0, true)));
            return new Categorize<R,Object>(null, values, thresholds, Categorize.ThresholdsBelongTo.SUCCEEDING, null);
        }
        if (SLDTags.VALUES.equalsIgnoreCase(type)) {
            final var items = new ArrayList<Recode.MapItem<R>>();
            for (final ColorMapEntry entry : entries) {
                items.add(new Recode.MapItem<>(ff.literal(entry.quantity()), ff.literal(entry.color())));
            }
            return new Recode<>(null, items, null);
        }
        if (type != null && !SLDTags.RAMP.equalsIgnoreCase(type)) {
            warning("Unknown type of color map \"" + type + "\", interpolation assumed.");
        }
        final var points = new ArrayList<Interpolate.InterpolationPoint<R>>();
        for (final ColorMapEntry entry : entries) {
            points.add(new Interpolate.InterpolationPoint<>(ff.literal(entry.quantity()), ff.literal(entry.color())));
        }
        return new Interpolate<>(null, points, Interpolate.Mode.LINEAR, Interpolate.Method.COLOR, null);
    }
}
