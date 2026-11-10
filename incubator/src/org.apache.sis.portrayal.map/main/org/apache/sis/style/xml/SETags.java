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

import org.apache.sis.xml.Namespaces;


/**
 * XML tags of the symbology elements. In Symbology Encoding 1.1, those elements are in the
 * {@value #NAMESPACE} namespace. In Styled Layer Descriptor 1.0, the same elements (with a few
 * differences documented in {@link SLDTags}) are in the {@value SLDTags#NAMESPACE} namespace.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class SETags {
    /**
     * Namespace of Symbology Encoding 1.1.
     */
    static final String NAMESPACE = Namespaces.SE;

    /**
     * Usual prefix for the {@link #NAMESPACE}.
     */
    static final String PREFIX = "se";

    // Common elements and attributes
    static final String VERSION           = "version";
    static final String NAME              = "Name";
    static final String DESCRIPTION       = "Description";
    static final String TITLE             = "Title";
    static final String ABSTRACT          = "Abstract";
    static final String FEATURE_TYPE_NAME = "FeatureTypeName";
    static final String COVERAGE_NAME     = "CoverageName";
    static final String ONLINE_RESOURCE   = "OnlineResource";
    static final String INLINE_CONTENT    = "InlineContent";
    static final String ENCODING          = "encoding";
    static final String BASE64            = "base64";

    // Styles and rules
    static final String FEATURE_TYPE_STYLE       = "FeatureTypeStyle";
    static final String COVERAGE_STYLE           = "CoverageStyle";
    static final String SEMANTIC_TYPE_IDENTIFIER = "SemanticTypeIdentifier";
    static final String GENERIC                  = "generic:";
    static final String RULE                     = "Rule";
    static final String LEGEND_GRAPHIC           = "LegendGraphic";
    static final String ELSE_FILTER              = "ElseFilter";
    static final String MIN_SCALE_DENOMINATOR    = "MinScaleDenominator";
    static final String MAX_SCALE_DENOMINATOR    = "MaxScaleDenominator";

    // Symbolizers
    static final String UOM                = "uom";
    static final String BASE_SYMBOLIZER    = "BaseSymbolizer";
    static final String POINT_SYMBOLIZER   = "PointSymbolizer";
    static final String LINE_SYMBOLIZER    = "LineSymbolizer";
    static final String POLYGON_SYMBOLIZER = "PolygonSymbolizer";
    static final String TEXT_SYMBOLIZER    = "TextSymbolizer";
    static final String RASTER_SYMBOLIZER  = "RasterSymbolizer";
    static final String GEOMETRY           = "Geometry";

    // Units of measurement
    static final String UOM_PREFIX = "http://www.opengeospatial.org/se/units/";
    static final String METRE      = "metre";
    static final String FOOT       = "foot";
    static final String PIXEL      = "pixel";

    // Strokes and fills
    static final String STROKE                = "Stroke";
    static final String FILL                  = "Fill";
    static final String SVG_PARAMETER         = "SvgParameter";
    static final String PARAMETER_NAME        = "name";
    static final String GRAPHIC_FILL          = "GraphicFill";
    static final String GRAPHIC_STROKE        = "GraphicStroke";
    static final String INITIAL_GAP           = "InitialGap";
    static final String GAP                   = "Gap";
    static final String PERPENDICULAR_OFFSET  = "PerpendicularOffset";

    // Names of SVG (or CSS) parameters
    static final String STROKE_COLOR      = "stroke";
    static final String STROKE_OPACITY    = "stroke-opacity";
    static final String STROKE_WIDTH      = "stroke-width";
    static final String STROKE_LINEJOIN   = "stroke-linejoin";
    static final String STROKE_LINECAP    = "stroke-linecap";
    static final String STROKE_DASHARRAY  = "stroke-dasharray";
    static final String STROKE_DASHOFFSET = "stroke-dashoffset";
    static final String FILL_COLOR        = "fill";
    static final String FILL_OPACITY      = "fill-opacity";
    static final String FONT_FAMILY       = "font-family";
    static final String FONT_STYLE        = "font-style";
    static final String FONT_WEIGHT       = "font-weight";
    static final String FONT_SIZE         = "font-size";

    // Graphics
    static final String GRAPHIC           = "Graphic";
    static final String OPACITY           = "Opacity";
    static final String SIZE              = "Size";
    static final String ROTATION          = "Rotation";
    static final String ANCHOR_POINT      = "AnchorPoint";
    static final String ANCHOR_POINT_X    = "AnchorPointX";
    static final String ANCHOR_POINT_Y    = "AnchorPointY";
    static final String DISPLACEMENT      = "Displacement";
    static final String DISPLACEMENT_X    = "DisplacementX";
    static final String DISPLACEMENT_Y    = "DisplacementY";
    static final String EXTERNAL_GRAPHIC  = "ExternalGraphic";
    static final String FORMAT            = "Format";
    static final String COLOR_REPLACEMENT = "ColorReplacement";
    static final String MARK              = "Mark";
    static final String WELL_KNOWN_NAME   = "WellKnownName";
    static final String MARK_INDEX        = "MarkIndex";

    // Texts
    static final String LABEL           = "Label";
    static final String FONT            = "Font";
    static final String LABEL_PLACEMENT = "LabelPlacement";
    static final String POINT_PLACEMENT = "PointPlacement";
    static final String LINE_PLACEMENT  = "LinePlacement";
    static final String IS_REPEATED     = "IsRepeated";
    static final String IS_ALIGNED      = "IsAligned";
    static final String GENERALIZE_LINE = "GeneralizeLine";
    static final String HALO            = "Halo";
    static final String RADIUS          = "Radius";

    // Rasters
    static final String CHANNEL_SELECTION    = "ChannelSelection";
    static final String RED_CHANNEL          = "RedChannel";
    static final String GREEN_CHANNEL        = "GreenChannel";
    static final String BLUE_CHANNEL         = "BlueChannel";
    static final String GRAY_CHANNEL         = "GrayChannel";
    static final String SOURCE_CHANNEL_NAME  = "SourceChannelName";
    static final String OVERLAP_BEHAVIOR     = "OverlapBehavior";
    static final String COLOR_MAP            = "ColorMap";
    static final String CONTRAST_ENHANCEMENT = "ContrastEnhancement";
    static final String NORMALIZE            = "Normalize";
    static final String HISTOGRAM            = "Histogram";
    static final String GAMMA_VALUE          = "GammaValue";
    static final String SHADED_RELIEF        = "ShadedRelief";
    static final String BRIGHTNESS_ONLY      = "BrightnessOnly";
    static final String RELIEF_FACTOR        = "ReliefFactor";
    static final String IMAGE_OUTLINE        = "ImageOutline";

    // Functions
    static final String FALLBACK_VALUE         = "fallbackValue";
    static final String LOOKUP_VALUE           = "LookupValue";
    static final String RASTER_DATA            = "Rasterdata";
    static final String VALUE                  = "Value";
    static final String DATA                   = "Data";
    static final String CATEGORIZE             = "Categorize";
    static final String THRESHOLD              = "Threshold";
    static final String THRESHOLDS_BELONG_TO   = "threshholdsBelongTo";    // Misspelled in the standard.
    static final String INTERPOLATE            = "Interpolate";
    static final String INTERPOLATION_POINT    = "InterpolationPoint";
    static final String MODE                   = "mode";
    static final String METHOD                 = "method";
    static final String RECODE                 = "Recode";
    static final String MAP_ITEM               = "MapItem";
    static final String FORMAT_NUMBER          = "FormatNumber";
    static final String NUMERIC_VALUE          = "NumericValue";
    static final String PATTERN                = "Pattern";
    static final String NEGATIVE_PATTERN       = "NegativePattern";
    static final String DECIMAL_POINT          = "decimalPoint";
    static final String GROUPING_SEPARATOR     = "groupingSeparator";
    static final String FORMAT_DATE            = "FormatDate";
    static final String DATE_VALUE             = "DateValue";
    static final String SUBSTRING              = "Substring";
    static final String STRING_VALUE           = "StringValue";
    static final String POSITION               = "Position";
    static final String LENGTH                 = "Length";
    static final String CONCATENATE            = "Concatenate";
    static final String CHANGE_CASE            = "ChangeCase";
    static final String DIRECTION              = "direction";
    static final String TRIM                   = "Trim";
    static final String STRIP_OFF_POSITION     = "stripOffPosition";
    static final String STRIP_OFF_CHAR         = "stripOffChar";
    static final String STRING_POSITION        = "StringPosition";
    static final String LOOKUP_STRING          = "LookupString";
    static final String SEARCH_DIRECTION       = "searchDirection";
    static final String STRING_LENGTH          = "StringLength";

    /**
     * Do not allow instantiation of this class.
     */
    private SETags() {
    }
}
