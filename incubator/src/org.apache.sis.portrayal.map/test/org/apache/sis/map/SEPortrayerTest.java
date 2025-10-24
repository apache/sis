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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.MatchAction;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.MemoryFeatureSet;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Symbology;
import org.apache.sis.style.se1.StyleFactory;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.SemanticType;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.iso.Names;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SEPortrayerTest {
    /**
     * The factory to use for creating style elements.
     */
    private final StyleFactory<Feature> factory = FeatureTypeStyle.FACTORY;

    private final FilterFactory<Feature,Object,Object> filterFactory;
    private final FeatureSet fishes;
    private final FeatureSet boats;

    /**
     * Creates a new test case.
     */
    public SEPortrayerTest() {
        filterFactory = DefaultFilterFactory.forFeatures();

        final GeometryFactory gf = org.apache.sis.geometry.wrapper.jts.Factory.INSTANCE.factory(false);
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();

        final FeatureTypeBuilder fishbuilder = new FeatureTypeBuilder();
        fishbuilder.setName("fish");
        fishbuilder.addAttribute(String.class).setName("id").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        fishbuilder.addAttribute(Point.class).setCRS(crs).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        fishbuilder.addAttribute(String.class).setName("description");
        final FeatureType fishType = fishbuilder.build();

        final Point point1 = gf.createPoint(new CoordinateXY(0, 0));
        point1.setUserData(crs);
        final Feature fish1 = fishType.newInstance();
        fish1.setPropertyValue("id", "1");
        fish1.setPropertyValue("geom", point1);
        fish1.setPropertyValue("description", "A red fish");

        final Point point2 = gf.createPoint(new CoordinateXY(10, 20));
        point2.setUserData(crs);
        final Feature fish2 = fishType.newInstance();
        fish2.setPropertyValue("id", "2");
        fish2.setPropertyValue("geom", point2);
        fish2.setPropertyValue("description", "A small blue fish");

        //a special fish with a sub-type
        final FeatureTypeBuilder sharkbuilder = new FeatureTypeBuilder();
        sharkbuilder.setName("shark");
        sharkbuilder.setSuperTypes(fishType);
        sharkbuilder.addAttribute(String.class).setName("specie");
        sharkbuilder.addAttribute(Double.class).setName("length");
        final FeatureType sharkType = sharkbuilder.build();

        final Point point3 = gf.createPoint(new CoordinateXY(30, 40));
        point3.setUserData(crs);
        final Feature shark1 = sharkType.newInstance();
        shark1.setPropertyValue("id", "100");
        shark1.setPropertyValue("geom", point3);
        shark1.setPropertyValue("description", "dangerous fish");
        shark1.setPropertyValue("specie", "White Shark");
        shark1.setPropertyValue("length", 12.0);

        fishes = new MemoryFeatureSet(null, sharkType, List.of(fish1, fish2, shark1));

        final FeatureTypeBuilder boatbuilder = new FeatureTypeBuilder();
        boatbuilder.setName("boat");
        boatbuilder.addAttribute(String.class).setName("id").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        boatbuilder.addAttribute(Polygon.class).setCRS(crs).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        boatbuilder.addAttribute(String.class).setName("description");
        final FeatureType boatType = boatbuilder.build();

        final Polygon poly1 = gf.createPolygon(gf.createLinearRing(new CoordinateXY[] {
            new CoordinateXY(0, 0), new CoordinateXY(0, 1), new CoordinateXY(1, 1), new CoordinateXY(0, 0)}));
        poly1.setUserData(crs);
        final Feature boat1 = boatType.newInstance();
        boat1.setPropertyValue("id", "10");
        boat1.setPropertyValue("geom", poly1);
        boat1.setPropertyValue("description", "A fishing boat");

        final Polygon poly2 = gf.createPolygon(gf.createLinearRing(new CoordinateXY[] {
            new CoordinateXY(0, 0), new CoordinateXY(0, 1), new CoordinateXY(1, 1), new CoordinateXY(0, 0)}));
        poly2.setUserData(crs);
        final Feature boat2 = boatType.newInstance();
        boat2.setPropertyValue("id", "20");
        boat2.setPropertyValue("geom", poly2);
        boat2.setPropertyValue("description", "A submarine");

        boats = new MemoryFeatureSet(null, boatType, List.of(boat1, boat2));
    }

    private Set<Match> present(MapItem item) {
        return present(item, CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()));
    }

    private Set<Match> present(MapItem item, Envelope env) {
        final GridGeometry grid = new GridGeometry(new GridExtent(360, 180), env, GridOrientation.REFLECTION_Y);
        final SEPortrayer portrayer = new SEPortrayer();
        final Stream<Presentation> stream = portrayer.present(grid, item);
        final List<Presentation> presentations = stream.collect(Collectors.toList());

        final Set<Match> ids = new HashSet<>();
        presentations.stream().forEach(new Consumer<Presentation>() {
            @Override
            public void accept(Presentation t) {
                if (t instanceof SEPresentation se) {
                    Feature Feature = se.getCandidate();
                    ids.add(new Match(String.valueOf(Feature.getPropertyValue(AttributeConvention.IDENTIFIER)),
                            se.getLayer(),
                            se.getResource(),
                            se.getSymbolizer()));
                } else if (t instanceof ExceptionPresentation ep) {
                    ids.add(new Match(ep.getException()));
                }
            }
        });
        return ids;
    }

    /**
     * Portray using no filtering operations
     */
    @Test
    public void testSanity() {
        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        final var rule = factory.createRule();
        final var symbolizer = factory.createLineSymbolizer();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);
        rule.symbolizers().add(symbolizer);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers);
        assertEquals(5, presentations.size());
        assertTrue(presentations.contains(new Match("1", fishLayer, fishes, symbolizer)));
        assertTrue(presentations.contains(new Match("2", fishLayer, fishes, symbolizer)));
        assertTrue(presentations.contains(new Match("100", fishLayer, fishes, symbolizer)));
        assertTrue(presentations.contains(new Match("10", boatLayer, boats, symbolizer)));
        assertTrue(presentations.contains(new Match("20", boatLayer, boats, symbolizer)));
    }

    /**
     * Test portrayer includes the bounding box of the canvas while querying features.
     * Only fish feature with identifier "2" matches in this test.
     */
    @Test
    public void testCanvasBboxfilter() {
        final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, 9, 11);
        env.setRange(1, 19, 21);

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        final var rule = factory.createRule();
        final var symbolizer = factory.createLineSymbolizer();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);
        rule.symbolizers().add(symbolizer);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers, env);
        assertEquals(1, presentations.size());
        assertTrue(presentations.contains(new Match("2", fishLayer, fishes, symbolizer)));
    }

    /**
     * Test portrayer uses the user defined query when portraying.
     * Only fish feature with identifier "1" and boat feature with identifier "20" matches in this test.
     */
    @Test
    public void testUserQuery() {
        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        final var rule = factory.createRule();
        final var symbolizer = factory.createLineSymbolizer();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);
        rule.symbolizers().add(symbolizer);

        final Filter<Feature> filter = filterFactory.or(
                filterFactory.resourceId("1"),
                filterFactory.resourceId("20"));
        final FeatureQuery query = new FeatureQuery();
        query.setSelection(filter);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        fishLayer.setQuery(query);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        boatLayer.setQuery(query);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers);
        assertEquals(2, presentations.size());
        assertTrue(presentations.contains(new Match("1", fishLayer, fishes, symbolizer)));
        assertTrue(presentations.contains(new Match("20", boatLayer, boats, symbolizer)));
    }

    /**
     * Portray using defined type names.
     * Test expect only boat type features to be rendered.
     */
    @Test
    public void testFeatureTypeStyleTypeNames() {
        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        fts.setFeatureTypeName(Names.createLocalName(null, null, "boat"));
        final var rule = factory.createRule();
        final var symbolizer = factory.createLineSymbolizer();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);
        rule.symbolizers().add(symbolizer);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers);
        assertEquals(2, presentations.size());
        assertTrue(presentations.contains(new Match("10", boatLayer, boats, symbolizer)));
        assertTrue(presentations.contains(new Match("20", boatLayer, boats, symbolizer)));
    }

    /**
     * Portray using defined type names.
     * Test expect only point geometric type to be rendered.
     */
    @Test
    public void testFeatureTypeStyleSemanticType() {
        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        fts.semanticTypeIdentifiers().add(SemanticType.POINT);
        final var rule = factory.createRule();
        final var symbolizer = factory.createLineSymbolizer();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);
        rule.symbolizers().add(symbolizer);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers);
        assertEquals(3, presentations.size());
        assertTrue(presentations.contains(new Match("1", fishLayer, fishes, symbolizer)));
        assertTrue(presentations.contains(new Match("2", fishLayer, fishes, symbolizer)));
        assertTrue(presentations.contains(new Match("100", fishLayer, fishes, symbolizer)));
    }

    /**
     * Portray using defined rule filter
     * Test expect only features with identifier equals "2" to match.
     */
    @Test
    public void testRuleFilter() {
        final Filter<Feature> filter = filterFactory.resourceId("2");

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        final var rule = factory.createRule();
        rule.setFilter(filter);
        final var symbolizer = factory.createLineSymbolizer();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);
        rule.symbolizers().add(symbolizer);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers);
        assertEquals(1, presentations.size());
        assertTrue(presentations.contains(new Match("2", fishLayer, fishes, symbolizer)));
    }

    /**
     * Portray using defined rule scale filter.
     * Test expect only matching scale rule symbolizer to be portrayed.
     */
    @Test
    public void testRuleScale() {
        final var symbolizerAbove = factory.createLineSymbolizer();
        final var symbolizerUnder = factory.createLineSymbolizer();
        final var symbolizerMatch = factory.createLineSymbolizer();

        //Symbology rendering scale here is 3.944391406060875E8
        final var ruleAbove = factory.createRule();
        ruleAbove.symbolizers().add(symbolizerAbove);
        ruleAbove.setMinScaleDenominator(4e8);
        ruleAbove.setMaxScaleDenominator(Double.MAX_VALUE);
        final var ruleUnder = factory.createRule();
        ruleUnder.symbolizers().add(symbolizerUnder);
        ruleUnder.setMinScaleDenominator(0.0);
        ruleUnder.setMaxScaleDenominator(3e8);
        final var ruleMatch = factory.createRule();
        ruleMatch.symbolizers().add(symbolizerMatch);
        ruleMatch.setMinScaleDenominator(3e8);
        ruleMatch.setMaxScaleDenominator(4e8);

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);
        fts.rules().add(ruleAbove);
        fts.rules().add(ruleUnder);
        fts.rules().add(ruleMatch);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers);
        assertEquals(5, presentations.size());
        assertTrue(presentations.contains(new Match(  "1", fishLayer, fishes, symbolizerMatch)));
        assertTrue(presentations.contains(new Match(  "2", fishLayer, fishes, symbolizerMatch)));
        assertTrue(presentations.contains(new Match("100", fishLayer, fishes, symbolizerMatch)));
        assertTrue(presentations.contains(new Match( "10", boatLayer, boats,  symbolizerMatch)));
        assertTrue(presentations.contains(new Match( "20", boatLayer, boats,  symbolizerMatch)));
    }

    /**
     * Portray using defined rule filter.
     * The rule uses a property only available on the shark sub type.
     * Test expect only features with specy equals "White Shark" to match.
     */
    @Test
    public void testRuleFilterOnSubType() {
        final BinaryComparisonOperator<Feature> filter = filterFactory.equal(
                filterFactory.property("specie", String.class),
                filterFactory.literal("White Shark"),
                true, MatchAction.ANY);

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        final var rule = factory.createRule();
        rule.setFilter(filter);
        final var symbolizer = factory.createLineSymbolizer();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);
        rule.symbolizers().add(symbolizer);


        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers);
        assertEquals(1, presentations.size());
        assertTrue(presentations.contains(new Match("100", fishLayer, fishes, symbolizer)));
    }

    /**
     * Portray using defined rule 'is else' property.
     * Test expect only feature with identifier "10" to be rendered with the base rule
     * and other features to rendered with the fallback rule.
     */
    @Test
    public void testRuleElseCondition() {
        final Filter<Feature> filter = filterFactory.resourceId("10");

        final var symbolizerBase = factory.createLineSymbolizer();
        final var symbolizerElse = factory.createLineSymbolizer();

        final var ruleBase = factory.createRule();
        ruleBase.symbolizers().add(symbolizerBase);
        ruleBase.setFilter(filter);
        final var ruleOther = factory.createRule();
        ruleOther.setElseFilter(true);
        ruleOther.symbolizers().add(symbolizerElse);

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);
        fts.rules().add(ruleBase);
        fts.rules().add(ruleOther);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers);
        assertEquals(5, presentations.size());
        assertTrue(presentations.contains(new Match(  "1", fishLayer, fishes, symbolizerElse)));
        assertTrue(presentations.contains(new Match(  "2", fishLayer, fishes, symbolizerElse)));
        assertTrue(presentations.contains(new Match("100", fishLayer, fishes, symbolizerElse)));
        assertTrue(presentations.contains(new Match( "10", boatLayer, boats,  symbolizerBase)));
        assertTrue(presentations.contains(new Match( "20", boatLayer, boats,  symbolizerElse)));
    }

    /**
     * Portray using and aggregated resource.
     * Test expect presentations to be correctly associated to each resource but on the same layer.
     */
    @Test
    public void testAggregateResource() {
        final var symbolizerBase = factory.createLineSymbolizer();

        final var ruleBase = factory.createRule();
        ruleBase.symbolizers().add(symbolizerBase);

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);
        fts.rules().add(ruleBase);

        final List<Resource> list = List.of(fishes, boats);
        final Aggregate agg = new Aggregate() {
            @Override
            public Collection<? extends Resource> components() throws DataStoreException {
                return list;
            }

            @Override
            public Metadata getMetadata() throws DataStoreException {
                return null;
            }
        };

        final MapLayer aggLayer = new MapLayer();
        aggLayer.setData(agg);
        aggLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(aggLayer);

        final Set<Match> presentations = present(layers);
        assertEquals(5, presentations.size());
        assertTrue(presentations.contains(new Match(  "1", aggLayer, fishes, symbolizerBase)));
        assertTrue(presentations.contains(new Match(  "2", aggLayer, fishes, symbolizerBase)));
        assertTrue(presentations.contains(new Match("100", aggLayer, fishes, symbolizerBase)));
        assertTrue(presentations.contains(new Match( "10", aggLayer, boats,  symbolizerBase)));
        assertTrue(presentations.contains(new Match( "20", aggLayer, boats,  symbolizerBase)));
    }

    /**
     * Portray preserving all feature attributes test.
     */
    @Test
    public void testPreserveProperties() {
        final Filter<Feature> filter = filterFactory.resourceId("2");
        final var symbolizer = factory.createLineSymbolizer();

        final var rule = factory.createRule();
        rule.symbolizers().add(symbolizer);
        rule.setFilter(filter);

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);

        final GridGeometry grid = new GridGeometry(new GridExtent(360, 180), CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()), GridOrientation.REFLECTION_Y);
        {
            // test without preserve properties
            // we expect only identifier and geometry to be available.
            final SEPortrayer portrayer = new SEPortrayer();
            portrayer.setPreserveProperties(false);
            final Stream<Presentation> stream = portrayer.present(grid, layers);
            final List<Presentation> presentations = stream.collect(Collectors.toList());
            assertEquals(1, presentations.size());
            final SEPresentation presentation = (SEPresentation) presentations.get(0);
            final Feature feature = presentation.getCandidate();
            final FeatureType type = feature.getType();
            assertEquals(2, type.getProperties(true).size());
            assertNotNull(type.getProperty(AttributeConvention.IDENTIFIER));
            assertNotNull(type.getProperty(AttributeConvention.GEOMETRY));
        }
        {
            // test with preserve properties
            // we expect only identifier and geometry to be available.
            final SEPortrayer portrayer = new SEPortrayer();
            portrayer.setPreserveProperties(true);
            final Stream<Presentation> stream = portrayer.present(grid, layers);
            final List<Presentation> presentations = stream.collect(Collectors.toList());
            assertEquals(1, presentations.size());
            final SEPresentation presentation = (SEPresentation) presentations.get(0);
            final Feature feature = presentation.getCandidate();
            final FeatureType type = feature.getType();
            assertEquals(6, type.getProperties(true).size());
            assertNotNull(type.getProperty(AttributeConvention.IDENTIFIER));
            assertNotNull(type.getProperty(AttributeConvention.GEOMETRY));
            assertNotNull(type.getProperty(AttributeConvention.ENVELOPE));
            assertNotNull(type.getProperty("id"));
            assertNotNull(type.getProperty("geom"));
            assertNotNull(type.getProperty("description"));
        }
    }

    /**
     * Test all properties used in the style are returned
     * in the presentation features.
     */
    @Test
    public void testStylePropertiesReturned() {
        final BinaryComparisonOperator<Feature> filter = filterFactory.equal(
                filterFactory.property("id", String.class),
                filterFactory.literal("2"),
                true, MatchAction.ANY);

        final var symbolizer = factory.createLineSymbolizer();
        symbolizer.setPerpendicularOffset(filterFactory.property("offset", Integer.class));

        final var rule = factory.createRule();
        rule.symbolizers().add(symbolizer);
        rule.setFilter(filter);

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);

        final GridGeometry grid = new GridGeometry(new GridExtent(360, 180), CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()), GridOrientation.REFLECTION_Y);

        // test without preserve properties
        // we expect identifier, geometry, id(used by rule filter), description (used in symbolizer)
        final SEPortrayer portrayer = new SEPortrayer();
        portrayer.setPreserveProperties(false);
        final Stream<Presentation> stream = portrayer.present(grid, layers);
        final List<Presentation> presentations = stream.collect(Collectors.toList());
        assertEquals(1, presentations.size());
        final SEPresentation presentation = (SEPresentation) presentations.get(0);
        final Feature feature = presentation.getCandidate();
        final FeatureType type = feature.getType();
        assertEquals(6, type.getProperties(true).size());
        assertNotNull(type.getProperty(AttributeConvention.IDENTIFIER));
        assertNotNull(type.getProperty(AttributeConvention.GEOMETRY));
        assertNotNull(type.getProperty("id"));
        assertNotNull(type.getProperty("description"));
    }

    /**
     * Test a geometry expression do not affect portraying bbox filtering.
     */
    @Test
    public void testGeometryExpression() {
        final var symbolizer = factory.createLineSymbolizer();
        symbolizer.setGeometry(filterFactory.function("ST_Centroid", filterFactory.property("geom")));

        final var rule = factory.createRule();
        rule.symbolizers().add(symbolizer);

        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);

        final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, 9, 11);
        env.setRange(1, 19, 21);

        final MapLayer fishLayer = new MapLayer();
        fishLayer.setData(fishes);
        fishLayer.setStyle(style);
        final MapLayer boatLayer = new MapLayer();
        boatLayer.setData(boats);
        boatLayer.setStyle(style);
        final MapLayers layers = new MapLayers();
        layers.getComponents().add(fishLayer);
        layers.getComponents().add(boatLayer);

        final Set<Match> presentations = present(layers, env);
        assertEquals(1, presentations.size());
        assertTrue(presentations.contains(new Match("2", fishLayer, fishes, symbolizer)));
    }

    private static class Match {
        private final String identifier;
        private final MapLayer layer;
        private final Resource resource;
        private final Symbolizer<?> symbolizer;
        private final Exception exception;

        public Match(String identifier, MapLayer layer, Resource resource, Symbolizer<?> symbolizer) {
            this.identifier = identifier;
            this.layer = layer;
            this.resource = resource;
            this.symbolizer = symbolizer;
            this.exception = null;
        }

        public Match(Exception e) {
            this.identifier = null;
            this.layer = null;
            this.resource = null;
            this.symbolizer = null;
            this.exception = e;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.identifier);
            hash = 29 * hash + Objects.hashCode(this.layer);
            hash = 29 * hash + Objects.hashCode(this.resource);
            hash = 29 * hash + Objects.hashCode(this.symbolizer);
            hash = 29 * hash + Objects.hashCode(this.exception);
            return hash;
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
            final Match other = (Match) obj;
            if (!Objects.equals(this.identifier, other.identifier)) {
                return false;
            }
            if (!Objects.equals(this.layer, other.layer)) {
                return false;
            }
            if (!Objects.equals(this.resource, other.resource)) {
                return false;
            }
            if (!Objects.equals(this.symbolizer, other.symbolizer)) {
                return false;
            }
            if (!Objects.equals(this.exception, other.exception)) {
                return false;
            }
            return true;
        }
    }
}
