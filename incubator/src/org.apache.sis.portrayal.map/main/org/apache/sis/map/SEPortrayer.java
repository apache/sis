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
package org.apache.sis.map;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Expression;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.feature.Features;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.filter.base.XPath;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Rule;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.SemanticType;
import org.apache.sis.style.se1.Symbology;


/**
 * Generation a Stream of Presentation for a map.
 *
 * <p>
 * NOTE: this class is experimental and subject to modifications.
 * </p>
 *
 * <p>
 * Style properties ignored :
 * </p>
 * <ul>
 *   <li>Style : isDefault : behavior from ISO 19117 which can be replaced by OGC SE Rule.isElseRule</li>
 *   <li>Style : defaultSpecification : behavior from ISO 19117 which can be replaced by OGC SE Rule.isElseRule</li>
 *   <li>FeatureTypeStyle : instance ids : behavior from ISO 19117 which can be replaced by OGC SE Rule.filter</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class SEPortrayer {
    /**
     * Rule scale tolerance.
     */
    private static final double SE_EPSILON = 1e-6;

    /**
     * Used in SLD/SE to calculate scale for degree CRSs.
     */
    private static final double SE_DEGREE_TO_METERS = 6378137.0 * 2.0 * Math.PI / 360.0;
    private static final double DEFAULT_DPI = 90.0; // ~ 0.28 * 0.28mm
    private static final double PIXEL_SIZE = 0.0254;

    /**
     * A test to know if a given property is an SIS convention or not. Return true if
     * the property is NOT marked as an SIS convention, false otherwise.
     */
    private static final Predicate<IdentifiedType> IS_NOT_CONVENTION = p -> !AttributeConvention.contains(p.getName());

    private final FilterFactory<Feature,Object,Object> filterFactory;

    /**
     * Hint to avoid decimating feature properties because they may be used
     * later for other purposes.
     */
    private boolean preserveProperties;

    private BiFunction<GridGeometry, Symbolizer<?>, Double> marginSolver;

    public SEPortrayer() {
        filterFactory = DefaultFilterFactory.forFeatures();
        marginSolver  = (GridGeometry t, Symbolizer<?> u) -> 30.0;
    }

    /**
     * Hint to avoid decimating feature properties because they may be used
     * later for other purposes.
     * Default value is false.
     *
     * @return true if all feature properties are preserved in Presentation instances.
     */
    public boolean isPreserveProperties() {
        return preserveProperties;
    }

    /**
     * Hint to avoid decimating feature properties because they may be used
     * later for other purposes.
     * Default value is false.
     *
     * @param preserveProperties set to true to preserve all feature properties in Presentation instances.
     */
    public void setPreserveProperties(boolean preserveProperties) {
        this.preserveProperties = preserveProperties;
    }

    /**
     * Replace default margin solver.
     * The margin solver try to guess the expected symbolizer size to expand the query bounding box.
     * @param marginSolver
     */
    public void setMarginSolver(BiFunction<GridGeometry, Symbolizer<?>, Double> marginSolver) {
        this.marginSolver = Objects.requireNonNull(marginSolver);
    }

    /**
     * Generate presentations for the given map item.
     */
    public Stream<Presentation> present(GridGeometry canvas, final MapItem mapitem) {
        Stream<Presentation> stream = Stream.empty();
        if (mapitem.isVisible()) {
            if (mapitem instanceof MapLayer) {
                final MapLayer layer = (MapLayer) mapitem;
                stream = Stream.concat(stream, present(canvas, layer, layer.getData()));
            } else if (mapitem instanceof MapLayers) {
                final MapLayers layers = (MapLayers) mapitem;
                for (MapItem item : layers.getComponents()) {
                    stream = Stream.concat(stream, present(canvas, item));
                }
            }
        }
        return stream;
    }

    private Stream<Presentation> present(GridGeometry canvas, MapLayer layer, Resource resource) {
        final Resource refResource = resource;
        Stream<Presentation> stream = Stream.empty();
        final FeatureType type;
        if (resource instanceof FeatureSet) {
            // Apply user query if defined.
            final Query basequery = layer.getQuery();
            if (basequery != null) try {
                resource = ((FeatureSet) resource).subset(basequery);
            } catch (DataStoreException ex) {
                stream = Stream.concat(stream, Stream.of(new ExceptionPresentation(ex)));
                return stream;
            }
            try {
                type = ((FeatureSet) resource).getType();
            } catch (DataStoreException ex) {
                stream = Stream.concat(stream, Stream.of(new ExceptionPresentation(ex)));
                return stream;
            }
        } else if (resource instanceof GridCoverageResource) {

            // Apply user query if defined.
            final Query basequery = layer.getQuery();
            if (basequery != null) try {
                resource = ((GridCoverageResource) resource).subset(basequery);
            } catch (DataStoreException ex) {
                stream = Stream.concat(stream, Stream.of(new ExceptionPresentation(ex)));
                return stream;
            }

            type = null;
        } else if (resource instanceof Aggregate) {
            try {
                // Combine each component resource in the stream.
                for (final Resource r : ((Aggregate) resource).components()) {
                    stream = Stream.concat(stream, present(canvas, layer, r));
                }
            } catch (DataStoreException ex) {
                stream = Stream.concat(stream, Stream.of(new ExceptionPresentation(ex)));
            }
            return stream;
        } else {
            // Unknown type.
            return Stream.empty();
        }
        final MathTransform   gridToCRS = canvas.getGridToCRS(PixelInCell.CELL_CENTER);
        final AffineTransform dispToObj;
        final AffineTransform objToDisp;
        try {
            dispToObj = AffineTransforms2D.castOrCopy(gridToCRS);
            objToDisp = dispToObj.createInverse();
        } catch (IllegalArgumentException | NoninvertibleTransformException ex) {
            stream = Stream.concat(stream, Stream.of(new ExceptionPresentation(ex)));
            return stream;
        }
        final double seScale = getSEScale(canvas, objToDisp);
        final Symbology style = (Symbology) layer.getStyle();     // TODO: we do not yet support other implementations.
        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            final List<Rule<Feature>> rules = getValidRules(fts, seScale, type);
            if (rules.isEmpty()) continue;

            // Prepare the renderers.
            final int elseRuleIndex = sortByElseRule(rules);
            {   //special case for resource symbolizers
                //resource symbolizers must be alone in a FTS
                ResourceSymbolizer<?> resourceSymbolizer = null;
                int count = 0;
                for (final Rule<Feature> r : rules) {
                    for (final Symbolizer<?> s : r.symbolizers()) {
                        count++;
                        if (s instanceof ResourceSymbolizer) {
                            resourceSymbolizer = (ResourceSymbolizer) s;
                        }
                    }
                }
                if (resourceSymbolizer != null) {
                    if (count > 1) {
                        Exception ex = new IllegalArgumentException("A resource symbolizer must be alone in a FeatureTypeStyle element." );
                        final ExceptionPresentation presentation = new ExceptionPresentation(ex);
                        presentation.setLayer(layer);
                        presentation.setResource(resource);
                        stream = Stream.concat(stream, Stream.of(presentation));
                    } else {
                        final SEPresentation presentation = new SEPresentation(layer, resource, null, resourceSymbolizer);
                        stream = Stream.concat(stream, Stream.of(presentation));
                    }
                    continue;
                }
            }
            // Extract the used names.
            Set<String> names;
            if (preserveProperties) {
                names = null;
            } else {
                names = propertiesNames(rules);
                if (names.contains("*")) {
                    // We need all properties.
                    names = null;
                }
            }
            if (resource instanceof GridCoverageResource) {
                boolean painted = false;
                for (int i = 0; i < elseRuleIndex; i++) {
                    final Stream<Presentation> subStream = present(rules.get(i), layer, resource, resource, null);
                    if (subStream != null) {
                        painted = true;
                        stream = Stream.concat(stream, subStream);
                    }
                }
                // The data hasn't been painted, paint it with the 'else' rules.
                if (!painted) {
                    for (int i = elseRuleIndex, n = rules.size(); i < n; i++) {
                        final Stream<Presentation> subStream = present(rules.get(i), layer, resource, resource, null);
                        if (subStream != null) {
                            stream = Stream.concat(stream, subStream);
                        }
                    }
                }
            } else if (resource instanceof FeatureSet) {
                final FeatureSet fs = (FeatureSet) resource;
                // Calculate max symbol size, to expand search envelope.
                double symbolsMargin = 0.0;
                for (Rule<Feature> rule : rules) {
                    for (Symbolizer<?> symbolizer : rule.symbolizers()) {
                        symbolsMargin = Math.max(symbolsMargin, marginSolver.apply(canvas, symbolizer));
                    }
                }
                if (Double.isNaN(symbolsMargin) || Double.isInfinite(symbolsMargin)) {
                    // Symbol margin cannot be pre calculated, expect a max of 300pixels.
                    symbolsMargin = 300f;
                }
                if (symbolsMargin > 0) {
                    symbolsMargin *= AffineTransforms2D.getScale(dispToObj);
                }
                try {
                    // Optimize query.
                    final Query query = prepareQuery(canvas, fs, names, rules, symbolsMargin);
                    final Stream<Presentation> s = fs.subset(query)
                            .features(false)
                            .flatMap((Feature feature) ->
                    {
                        Stream<Presentation> stream1 = Stream.empty();
                        boolean painted = false;
                        for (int i = 0; i < elseRuleIndex; i++) {
                            final Stream<Presentation> subStream = present(rules.get(i), layer, fs, refResource, feature);
                            if (subStream != null) {
                                painted = true;
                                stream1 = Stream.concat(stream1, subStream);
                            }
                        }
                        // The feature hasn't been painted, paint it with the 'else' rules.
                        if (!painted) {
                            for (int i = elseRuleIndex, n = rules.size(); i < n; i++) {
                                final Stream<Presentation> subStream = present(rules.get(i), layer, fs, refResource, feature);
                                if (subStream != null) {
                                    stream1 = Stream.concat(stream1, subStream);
                                }
                            }
                        }
                        return stream1;
                    });
                    stream = Stream.concat(stream, s);
                } catch (DataStoreException | TransformException ex) {
                    stream = Stream.concat(stream, Stream.of(new ExceptionPresentation(ex)));
                }
            }
        }
        return stream;
    }

    private static Stream<Presentation> present(Rule<Feature> rule, MapLayer layer,
            Resource resource, Resource refResource, Feature feature)
    {
        final Filter<Feature> ruleFilter = rule.getFilter();
        //test if the rule is valid for this resource/feature
        if (rule.isElseFilter() || ((Filter) ruleFilter).test(feature == null ? resource : feature)) {       // TODO: unsafe cast.
            Stream<Presentation> stream = Stream.empty();
            for (final Symbolizer<?> symbolizer : rule.symbolizers()) {
                final SEPresentation presentation = new SEPresentation(layer, refResource, feature, symbolizer);
                stream = Stream.concat(stream, Stream.of(presentation));
            }
            return stream;
        }
        return null;
    }

    /**
     * Creates an optimal query to send to the Featureset, knowing which properties are knowned and
     * the appropriate bounding box to filter.
     */
    private FeatureQuery prepareQuery(GridGeometry canvas, FeatureSet fs, Set<String> requiredProperties,
            List<Rule<Feature>> rules, double symbolsMargin) throws DataStoreException, TransformException
    {
        final FeatureQuery query = new FeatureQuery();
        final FeatureType schema = fs.getType();
        /*
         * Check if some used properties are not part of the type.
         * This means the FeatureSet may contain sub types.
         * We cannot optimize the query.
         */
        if (requiredProperties != null) {
            for (String pn : requiredProperties) {
                try {
                    schema.getProperty(pn);
                } catch (PropertyNotFoundException e) {
                    return query;
                }
            }
        }
        // Search all geometry expression used in the symbols.
        boolean allDefined = true;
        final Set<Expression<Feature,?>> geomProperties = new HashSet<>();
        if (rules != null) {
            for (final Rule<Feature> r : rules) {
                for (final Symbolizer<Feature> s : r.symbolizers()) {
                    final Expression<Feature,?> expGeom = s.getGeometry();
                    if (expGeom != null) {
                        geomProperties.add(expGeom );
                    } else {
                        allDefined = false;
                    }
                }
            }
        } else {
            allDefined = false;
        }
        if (!allDefined) {
            // Add the default geometry property.
            try {
                PropertyType geomDesc = getDefaultGeometry(schema);
                geomProperties.add(filterFactory.property(geomDesc.getName().toString()));
            } catch (PropertyNotFoundException | IllegalStateException ex) {
                // Do nothing.
            }
        }
        if (geomProperties.isEmpty()) {
            // No geometry selected for rendering.
            query.setSelection(Filter.exclude());
            return query;
        }
        final Envelope bbox = optimizeBBox(canvas, symbolsMargin);
        Filter<Feature> filter;
        // Make a bbox filter.
        if (geomProperties.size() == 1) {
            final Expression<Feature,?> geomExp = geomProperties.iterator().next();
            filter = filterFactory.bbox(geomExp, bbox);
        } else {
            // Make an OR filter with all geometries.
            final List<Filter<?>> geomFilters = new ArrayList<>();
            for (final Expression<Feature,?> geomExp : geomProperties) {
                geomFilters.add(filterFactory.bbox(geomExp, bbox));
            }
            filter = filterFactory.or((List) geomFilters);      // TODO
        }
        /*
         * Combine the filter with rule filters.
         */
        ruleOpti:
        if (rules != null) {
            final List<Filter<Feature>> rulefilters = new ArrayList<>();
            for (final Rule<Feature> rule : rules) {
                if (rule.isElseFilter()) {
                    // We cannot append styling filters, an else rule match all features.
                    break ruleOpti;
                } else {
                    final Filter<Feature> rf = rule.getFilter();
                    if (rf == Filter.<Feature>include()) {
                        // We cannot append styling filters, this rule matchs all features.
                        break ruleOpti;
                    }
                    rulefilters.add(rf);
                }
            }
            final Filter<Feature> combined;
            if (rulefilters.size() == 1) {
//                //TODO need a stylefactory in SIS
//                //special case, only one rule and we passed the filter to the query
//                //we can remove it from the rule
//                final Rule original = rules.get(0);
//                Rule rule = styleFactory.rule(null, null, null,
//                        original.getMinScaleDenominator(),
//                        original.getMaxScaleDenominator(),
//                        new ArrayList(original.symbolizers()),
//                        Filter.include());
//                rules.set(0, rule);
                combined = rulefilters.get(0);
            } else {
                combined = filterFactory.or(rulefilters);
            }
            if (filter != Filter.<Feature>include()) {
                filter = filterFactory.and(filter, combined);
            } else {
                filter = combined;
            }
        }
        query.setSelection(filter);
        /*
         * Reduce requiered attributes.
         */
        if (requiredProperties == null) {
            // All properties are required.
        } else {
            final Set<String> copy = new HashSet<>();
            // Add used properties.
            for (final String str : requiredProperties) {
                copy.add(stripXpath(str));
            }
            // Add properties used as geometry.
            for (Expression<?,?> exp : geomProperties) {
                final PropertyNameCollector collector = new PropertyNameCollector();
                collector.visit(exp);
                collector.references.stream()
                        .map(SEPortrayer::stripXpath)
                        .forEach(copy::add);
            }
            try {
                // Always include the identifier if it exist.
                schema.getProperty(AttributeConvention.IDENTIFIER);
                copy.add(AttributeConvention.IDENTIFIER);
            } catch (PropertyNotFoundException ex) {
                // No id, ignore it.
            }
            final List<FeatureQuery.NamedExpression> columns = new ArrayList<>();
            for (String propName : copy) {
                columns.add(new FeatureQuery.NamedExpression(filterFactory.property(propName), propName));
            }
            query.setProjection(columns.toArray(FeatureQuery.NamedExpression[]::new));
        }
        //TODO optimize filter
        //TODO add linear resolution
        return query;
    }

    /**
     * Geographic scale calculated using OGC Symbology Encoding specification.
     * This is not the scale Objective to Display.
     * This is not an accurate geographic scale.
     * This is a fake average scale unproper for correct rendering.
     * It is used only to filter SE rules.
     */
    private static double getSEScale(final GridGeometry canvas, final AffineTransform objToDisp) {
        final Envelope envelope = canvas.getEnvelope();
        final CoordinateReferenceSystem objCRS = envelope.getCoordinateReferenceSystem();
        final long width = canvas.getExtent().getSize(0);
        if (AffineTransforms2D.getRotation(objToDisp) != 0.0) {
            final double scale = AffineTransforms2D.getScale(objToDisp);
            if (objCRS instanceof GeographicCRS) {
                return (SE_DEGREE_TO_METERS * DEFAULT_DPI) / (scale*PIXEL_SIZE);
            } else {
                return DEFAULT_DPI / (scale *PIXEL_SIZE);
            }
        } else {
            if (objCRS instanceof GeographicCRS) {
                return (envelope.getSpan(0) * SE_DEGREE_TO_METERS) / (width / DEFAULT_DPI * PIXEL_SIZE);
            } else {
                return envelope.getSpan(0) / (width / DEFAULT_DPI * PIXEL_SIZE);
            }
        }
    }

    /**
     * List the valid rules for current scale and type.
     */
    private static List<Rule<Feature>> getValidRules(final FeatureTypeStyle fts, final double scale, final FeatureType type) {
        final Optional<GenericName> name = fts.getFeatureTypeName();
        if (name.isPresent()) {
            // TODO: should we check parent types?
            if (!name.get().equals(type.getName())) {
                return List.of();
            }
        }
        // Check semantic, only if we have a feature type.
        if (type != null) {
            final Collection<SemanticType> semantics = fts.semanticTypeIdentifiers();
            if (!semantics.isEmpty()) {
                Class<?> ctype;
                try {
                    ctype = Features.toAttribute(getDefaultGeometry(type))
                            .map(AttributeType::getValueClass)
                            .orElse(null);
                } catch (PropertyNotFoundException e) {
                      ctype = null;
                }
                boolean valid = false;
                for (SemanticType semantic : semantics) {
                    if (semantic == SemanticType.ANY) {
                        valid = true;
                        break;
                    } else if (semantic == SemanticType.LINE) {
                        if (ctype == LineString.class || ctype == MultiLineString.class || ctype == Geometry.class) {
                            valid = true;
                            break;
                        }
                    } else if (semantic == SemanticType.POINT) {
                        if (ctype == Point.class || ctype == MultiPoint.class || ctype == Geometry.class) {
                            valid = true;
                            break;
                        }
                    } else if (semantic == SemanticType.POLYGON) {
                        if (ctype == Polygon.class || ctype == MultiPolygon.class || ctype == Geometry.class) {
                            valid = true;
                            break;
                        }
                    } else if (semantic == SemanticType.RASTER) {
                        // Cannot test this on feature datas.
                    } else if (semantic == SemanticType.TEXT) {
                        // Cannot define a `text` type with current API.
                    }
                }
                if (!valid) return List.of();
            }
        }

        //TODO filter correctly possibilities
        //test if the featutetype is valid
        //we move to next feature  type if not valid
        //if (typeName != null && !(typeName.equalsIgnoreCase(fts.getFeatureTypeName())) ) continue;

        final List<? extends Rule<Feature>> rules = fts.rules();
        final List<Rule<Feature>> validRules = new ArrayList<>();
        for (final Rule<Feature> rule : rules) {
            //test if the scale is valid for this rule
            if (rule.getMinScaleDenominator() - SE_EPSILON <= scale && rule.getMaxScaleDenominator() + SE_EPSILON > scale) {
                validRules.add(rule);
            }
        }
        return validRules;
    }

    /**
     * Lists all properties used in given rules.
     */
    private static Set<String> propertiesNames(final Collection<? extends Rule<Feature>> rules) {
        final PropertyNameCollector collector = new PropertyNameCollector();
        for (final Rule<Feature> r : rules) {
            collector.visit(r);
            collector.visit(r.getFilter());
        }
        return collector.references;
    }

    /**
     * Sorts the rules, isolate the else rules, they must be handle differently
     *
     * @return index of starting else rules.
     */
    private static int sortByElseRule(final List<Rule<Feature>> sortedRules){
        int elseRuleIndex = sortedRules.size();
        for (int i = 0; i < elseRuleIndex; i++) {
            final Rule<Feature> r = sortedRules.get(i);
            if (r.isElseFilter()) {
                elseRuleIndex--;
                // Move the rule at the end
                sortedRules.remove(i);
                sortedRules.add(r);
            }
        }
        return elseRuleIndex;
    }

    /**
     * Search for the main geometric property in the given type. We'll search
     * for an SIS convention first (see
     * {@link AttributeConvention#GEOMETRY_PROPERTY}. If no convention is set on
     * the input type, we'll check if it contains a single geometric property.
     * If it's the case, we return it. Otherwise (no or multiple geometries), we
     * throw an exception.
     *
     * @param  type  the data type to search into.
     * @return the main geometric property we've found.
     * @throws PropertyNotFoundException if no geometric property is available in the given type.
     * @throws IllegalStateException if no convention is set (see {@link AttributeConvention#GEOMETRY_PROPERTY}),
     *         and we have found more than one geometry.
     */
    private static PropertyType getDefaultGeometry(final FeatureType type) throws PropertyNotFoundException, IllegalStateException {
        PropertyType geometry;
        try {
            geometry = type.getProperty(AttributeConvention.GEOMETRY_PROPERTY.toString());
        } catch (PropertyNotFoundException e) {
            try {
                geometry = searchForGeometry(type);
            } catch (RuntimeException e2) {
                e2.addSuppressed(e);
                throw e2;
            }
        }
        return geometry;
    }

    /**
     * Searches for a geometric attribute outside SIS conventions. More accurately,
     * we expect the given type to have a single geometry attribute. If many are
     * found, an exception is thrown.
     *
     * @param  type  the data type to search into.
     * @return the only geometric property we've found.
     * @throws PropertyNotFoundException if no geometric property is available in the given type.
     * @throws IllegalStateException if we have found more than one geometry.
     */
    private static PropertyType searchForGeometry(final FeatureType type) throws PropertyNotFoundException, IllegalStateException {
        final List<? extends PropertyType> geometries = type.getProperties(true).stream()
                .filter(IS_NOT_CONVENTION)
                .filter(AttributeConvention::isGeometryAttribute)
                .collect(Collectors.toList());

        if (geometries.size() < 1) {
            throw new PropertyNotFoundException("No geometric property can be found outside of sis convention.");
        } else if (geometries.size() > 1) {
            throw new IllegalStateException("Multiple geometries found. We don't know which one to select.");
        } else {
            return geometries.get(0);
        }
    }

    /**
     * Removes any xpath elements, keep only the root property name.
     */
    private static String stripXpath(String attName) {
        int index = attName.indexOf(XPath.SEPARATOR);
        if (index == 0) {
            attName = attName.substring(1);             // Remove first slash
            final Pattern pattern = Pattern.compile("(\\{[^\\{\\}]*\\})|(\\[[^\\[\\]]*\\])|/{1}");
            final Matcher matcher = pattern.matcher(attName);
            final StringBuilder sb = new StringBuilder();
            int position = 0;
matches:    while (matcher.find()) {
                final String match = matcher.group();
                sb.append(attName.substring(position, matcher.start()));
                position = matcher.end();
                switch (match.charAt(0)) {
                    case XPath.SEPARATOR: {
                        // We do not query precisely sub elements.
                        position = attName.length();
                        break matches;
                    }
                    case '{': {
                        sb.append(match);
                        break;
                    }
                    case '[': {
                        // Strip indexes or xpath searches.
                        break;
                    }
                }
            }
            sb.append(attName.substring(position));
            attName = sb.toString();
        }
        return attName;
    }

    /**
     * Extracts envelope and expand it's horizontal component by given margin.
     */
    private static Envelope optimizeBBox(GridGeometry canvas, double symbolsMargin) throws TransformException {
        Envelope env = canvas.getEnvelope();
        //keep only horizontal component
        env = Envelopes.transform(env, CRS.getHorizontalComponent(env.getCoordinateReferenceSystem()));

        //expand the search area by given margin
        if (symbolsMargin > 0) {
            final GeneralEnvelope e = new GeneralEnvelope(env);
            e.setRange(0, e.getMinimum(0) - symbolsMargin, e.getMaximum(0) + symbolsMargin);
            e.setRange(1, e.getMinimum(1) - symbolsMargin, e.getMaximum(1) + symbolsMargin);
            env = e;
        }
        return env;
    }
}
