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

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.feature.Features;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArgumentChecks;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.Rule;
import org.opengis.style.SemanticType;
import org.opengis.style.Symbolizer;
import org.opengis.util.GenericName;

/**
 * Generation a Stream of Presentation for a map.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
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
    public static final Predicate<IdentifiedType> IS_NOT_CONVENTION = p -> !AttributeConvention.contains(p.getName());

    /**
     * Hint to avoid decimating feature properties because they may be used
     * later for other purposes.
     */
    private boolean preserveProperties;

    private BiFunction<GridGeometry, Symbolizer, Double> marginSolver = new BiFunction<GridGeometry, Symbolizer, Double>() {
        @Override
        public Double apply(GridGeometry t, Symbolizer u) {
            return 30.0;
        }
    };

    public SEPortrayer() {}

    /**
     * Replace default margin solver.
     * The margin solver try to guess the expected symbolizer size to expand the query bounding box.
     * @param marginSolver
     */
    public void setMarginSolver(BiFunction<GridGeometry, Symbolizer, Double> marginSolver) {
        ArgumentChecks.ensureNonNull("marginSolver", marginSolver);
        this.marginSolver = marginSolver;
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

        Stream<Presentation> stream = Stream.empty();

        FeatureType type;
        if (resource instanceof FeatureSet) {
            try {
                type = ((FeatureSet) resource).getType();
            } catch (DataStoreException ex) {
                stream = Stream.concat(stream, Stream.of(new ExceptionPresentation(ex)));
                return stream;
            }
        } else if (resource instanceof GridCoverageResource) {
            type = null;
        } else if (resource instanceof Aggregate) {
            try {
                //combine each component resource in the stream
                for (Resource r : ((Aggregate) resource).components()) {
                    stream = Stream.concat(stream, present(canvas, layer, r));
                }
            } catch (DataStoreException ex) {
                stream = Stream.concat(stream, Stream.of(new ExceptionPresentation(ex)));
            }
            return stream;
        } else {
            //unknown type
            return Stream.empty();
        }


        final MathTransform gridToCRS = canvas.getGridToCRS(PixelInCell.CELL_CENTER);
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

        for (FeatureTypeStyle fts : layer.getStyle().featureTypeStyles()) {
            final List<Rule> rules = getValidRules(fts, seScale, type);
            if (rules.isEmpty()) continue;

            //prepare the renderers
            final int elseRuleIndex = sortByElseRule(rules);

            {   //special case for resource symbolizers
                //resource symbolizers must be alone in a FTS
                ResourceSymbolizer resourceSymbolizer = null;
                int count = 0;
                for (Rule r : rules) {
                    for (Symbolizer s : r.symbolizers()) {
                        count++;
                        if (s instanceof ResourceSymbolizer) {
                            resourceSymbolizer = (ResourceSymbolizer) s;
                        }
                    }
                }
                if (resourceSymbolizer != null) {
                    if (count > 1) {
                        Exception ex = new IllegalArgumentException("A resource symbolizer must be alone in a FeatureTypeStyle element." );
                        ex.fillInStackTrace();
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

            //extract the used names
            Set<String> names;
            if (preserveProperties) {
                names = null;
            } else {
                names = propertiesNames(rules);
                if (names.contains("*")) {
                    //we need all properties
                    names = null;
                }
            }

            if (resource instanceof GridCoverageResource) {

                boolean painted = false;
                for (int i = 0; i < elseRuleIndex; i++) {
                    final Stream<Presentation> subStream = present(rules.get(i), layer, resource, null);
                    if (subStream != null) {
                        painted = true;
                        stream = Stream.concat(stream, subStream);
                    }
                }

                //the data hasn't been painted, paint it with the 'else' rules
                if (!painted) {
                    for (int i = elseRuleIndex, n = rules.size(); i < n; i++) {
                        final Stream<Presentation> subStream = present(rules.get(i), layer, resource, null);
                        if (subStream != null) {
                            stream = Stream.concat(stream, subStream);
                        }
                    }
                }

            } else if (resource instanceof FeatureSet) {
                final FeatureSet fs = (FeatureSet) resource;

                //calculate max symbol size, to expand search envelope.
                double symbolsMargin = 0.0;
                for (Rule rule : rules) {
                    for (Symbolizer symbolizer : rule.symbolizers()) {
                        symbolsMargin = Math.max(symbolsMargin, marginSolver.apply(canvas, symbolizer));
                    }
                }
                if (Double.isNaN(symbolsMargin) || Double.isInfinite(symbolsMargin)) {
                    //symbol margin can not be pre calculated, expect a max of 300pixels
                    symbolsMargin = 300f;
                }
                if (symbolsMargin > 0) {
                    symbolsMargin *= AffineTransforms2D.getScale(dispToObj);
                }

                //optimize
                final Query query = prepareQuery(canvas, fs, layer, names, rules, symbolsMargin);

                try {
                    final Stream<Presentation> s = fs.subset(query)
                            .features(false)
                            .flatMap(new Function<Feature, Stream<Presentation>>() {
                        @Override
                        public Stream<Presentation> apply(Feature feature) {

                            Stream<Presentation> stream = Stream.empty();
                            boolean painted = false;
                            for (int i = 0; i < elseRuleIndex; i++) {
                                final Stream<Presentation> subStream = present(rules.get(i), layer, fs, feature);
                                if (subStream != null) {
                                    painted = true;
                                    stream = Stream.concat(stream, subStream);
                                }
                            }

                            //the feature hasn't been painted, paint it with the 'else' rules
                            if (!painted) {
                                for (int i = elseRuleIndex, n = rules.size(); i < n; i++) {
                                    final Stream<Presentation> subStream = present(rules.get(i), layer, fs, feature);
                                    if (subStream != null) {
                                        stream = Stream.concat(stream, subStream);
                                    }
                                }
                            }

                            return stream;
                        }
                    });
                    stream = Stream.concat(stream, s);
                } catch (DataStoreException ex) {
                    stream = Stream.concat(stream, Stream.of(new ExceptionPresentation(ex)));
                }
            }
        }

        return stream;
    }

    private static Stream<Presentation> present(Rule rule, MapLayer layer, Resource resource, Feature feature) {
        final Filter ruleFilter = rule.getFilter();
        //test if the rule is valid for this resource/feature
        if (ruleFilter == null || ruleFilter.evaluate(feature == null ? resource : feature)) {
            Stream<Presentation> stream = Stream.empty();
            for (final Symbolizer symbolizer : rule.symbolizers()) {
                final SEPresentation presentation = new SEPresentation(layer, resource, feature, symbolizer);
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
    private SimpleQuery prepareQuery(GridGeometry canvas, FeatureSet fs, MapLayer layer, Set<String> names, List<Rule> rules, double symbolsMargin) {
        //TODO
        throw new UnsupportedOperationException("todo");
    }

    /**
     * Geographic scale calculated using OGC Symbology Encoding specification.
     * This is not the scale Objective to Display.
     * This is not an accurate geographic scale.
     * This is a fake average scale unproper for correct rendering.
     * It is used only to filter SE rules.
     */
    private static double getSEScale(GridGeometry canvas, AffineTransform objToDisp) {
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
    private static List<Rule> getValidRules(final FeatureTypeStyle fts, final double scale, final FeatureType type) {

        final Id ids = fts.getFeatureInstanceIDs();
        final Set<GenericName> names = fts.featureTypeNames();

        //check semantic, only if we have a feature type
        if (type != null) {
            final Collection<SemanticType> semantics = fts.semanticTypeIdentifiers();
            if (!semantics.isEmpty()) {
                Class ctype;
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
                        if (ctype == LineString.class || ctype == MultiLineString.class || ctype == Geometry.class ) {
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
                        // can not test this on feature datas
                    } else if (semantic == SemanticType.TEXT) {
                        // can not define a 'text' type with current API.
                    }
                }

                if (!valid) return Collections.emptyList();
            }
        }

        //TODO filter correctly possibilities
        //test if the featutetype is valid
        //we move to next feature  type if not valid
        //if (typeName != null && !(typeName.equalsIgnoreCase(fts.getFeatureTypeName())) ) continue;

        final List<? extends Rule> rules = fts.rules();
        final List<Rule> validRules = new ArrayList<>();
        for (final Rule rule : rules) {
            //test if the scale is valid for this rule
            if (rule.getMinScaleDenominator() - SE_EPSILON <= scale && rule.getMaxScaleDenominator() + SE_EPSILON > scale) {
                validRules.add(rule);
            }
        }

        return validRules;
    }

    /**
     * List all properties used in given rules.
     */
    private static Set<String> propertiesNames(final Collection<? extends Rule> rules) {
        final PropertyNameCollector collector = new PropertyNameCollector();
        rules.forEach(collector::visit);
        return collector.getPropertyNames()
                .stream()
                .map(PropertyName::getPropertyName).collect(Collectors.toSet());
    }

    /**
     * Sort the rules, isolate the else rules, they must be handle differently
     * @return index of starting else rules.
     */
    private static int sortByElseRule(final List<Rule> sortedRules){
        int elseRuleIndex = sortedRules.size();
        for (int i = 0; i < elseRuleIndex; i++) {
            final Rule r = sortedRules.get(i);
            if (r.isElseFilter()) {
                elseRuleIndex--;
                //move the rule at the end
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
     * @param type The data type to search into.
     * @return The main geometric property we've found.
     * @throws PropertyNotFoundException If no geometric property is available
     * in the given type.
     * @throws IllegalStateException If no convention is set (see
     * {@link AttributeConvention#GEOMETRY_PROPERTY}), and we've found more than
     * one geometry.
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
     * Search for a geometric attribute outside SIS conventions. More accurately,
     * we expect the given type to have a single geometry attribute. If many are
     * found, an exception is thrown.
     *
     * @param type The data type to search into.
     * @return The only geometric property we've found.
     * @throws PropertyNotFoundException If no geometric property is available in
     * the given type.
     * @throws IllegalStateException If we've found more than one geometry.
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

}
