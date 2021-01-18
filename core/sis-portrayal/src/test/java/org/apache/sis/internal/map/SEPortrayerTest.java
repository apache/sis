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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.storage.MemoryFeatureSet;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.Names;
import static org.junit.Assert.*;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.Identifier;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.style.SemanticType;
import org.opengis.style.Symbolizer;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class SEPortrayerTest extends TestCase {

    private final FilterFactory2 filterFactory;
    private final FeatureSet fishes;
    private final FeatureSet boats;

    public SEPortrayerTest() {

        FilterFactory filterFactory = DefaultFactories.forClass(FilterFactory.class);
        if (!(filterFactory instanceof FilterFactory2)) {
            filterFactory = new DefaultFilterFactory();
        }
        this.filterFactory = (FilterFactory2) filterFactory;

        final GeometryFactory gf = new GeometryFactory();
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();

        final FeatureTypeBuilder fishbuilder = new FeatureTypeBuilder();
        fishbuilder.setName("fish");
        fishbuilder.addAttribute(String.class).setName("id").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        fishbuilder.addAttribute(Point.class).setCRS(crs).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        final FeatureType fishType = fishbuilder.build();

        final Point point1 = gf.createPoint(new Coordinate(0, 0));
        point1.setUserData(crs);
        final Feature fish1 = fishType.newInstance();
        fish1.setPropertyValue("id", "1");
        fish1.setPropertyValue("geom", point1);

        final Point point2 = gf.createPoint(new Coordinate(10, 20));
        point2.setUserData(crs);
        final Feature fish2 = fishType.newInstance();
        fish2.setPropertyValue("id", "2");
        fish2.setPropertyValue("geom", point2);

        fishes = new MemoryFeatureSet(null, fishType, Arrays.asList(fish1, fish2));


        final FeatureTypeBuilder boatbuilder = new FeatureTypeBuilder();
        boatbuilder.setName("boat");
        boatbuilder.addAttribute(String.class).setName("id").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        boatbuilder.addAttribute(Polygon.class).setCRS(crs).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        final FeatureType boatType = boatbuilder.build();

        final Polygon poly1 = gf.createPolygon(gf.createLinearRing(new Coordinate[]{new Coordinate(0, 0),new Coordinate(0, 1),new Coordinate(1, 1),new Coordinate(0, 0)}));
        poly1.setUserData(crs);
        final Feature boat1 = boatType.newInstance();
        boat1.setPropertyValue("id", "10");
        boat1.setPropertyValue("geom", poly1);

        final Polygon poly2 = gf.createPolygon(gf.createLinearRing(new Coordinate[]{new Coordinate(0, 0),new Coordinate(0, 1),new Coordinate(1, 1),new Coordinate(0, 0)}));
        poly2.setUserData(crs);
        final Feature boat2 = boatType.newInstance();
        boat2.setPropertyValue("id", "20");
        boat2.setPropertyValue("geom", poly2);

        boats = new MemoryFeatureSet(null, boatType, Arrays.asList(boat1, boat2));
    }

    private Set<Entry<String, Symbolizer>> present(MapItem item) {
        return present(item, CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()));
    }

    private Set<Entry<String, Symbolizer>> present(MapItem item, Envelope env) {
        final GridGeometry grid = new GridGeometry(new GridExtent(360, 180), env, GridOrientation.REFLECTION_Y);
        final SEPortrayer portrayer = new SEPortrayer();
        final Stream<Presentation> stream = portrayer.present(grid, item);
        final List<Presentation> presentations = stream.collect(Collectors.toList());

        final Set<Entry<String,Symbolizer>> ids = new HashSet<>();

        presentations.stream().forEach(new Consumer<Presentation>() {
            @Override
            public void accept(Presentation t) {
                if (t instanceof SEPresentation) {
                    Symbolizer symbolizer = ((SEPresentation) t).getSymbolizer();
                    Feature Feature = (Feature) ((SEPresentation) t).getCandidate();
                    ids.add(new AbstractMap.SimpleEntry<>(String.valueOf(Feature.getPropertyValue(AttributeConvention.IDENTIFIER)), symbolizer));
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

        final MockStyle style = new MockStyle();
        final MockFeatureTypeStyle fts = new MockFeatureTypeStyle();
        final MockRule rule = new MockRule();
        final MockLineSymbolizer symbolizer = new MockLineSymbolizer();
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

        final Set<Entry<String, Symbolizer>> presentations = present(layers);
        assertEquals(4, presentations.size());
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("1", symbolizer)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("2", symbolizer)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("10", symbolizer)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("20", symbolizer)));
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

        final MockStyle style = new MockStyle();
        final MockFeatureTypeStyle fts = new MockFeatureTypeStyle();
        final MockRule rule = new MockRule();
        final MockLineSymbolizer symbolizer = new MockLineSymbolizer();
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

        final Set<Entry<String, Symbolizer>> presentations = present(layers, env);
        assertEquals(1, presentations.size());
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("2", symbolizer)));
    }

    /**
     * Test portrayer uses the user defined query when portraying.
     * Only fish feature with identifier "1" and boat feature with identifier "20" matches in this test.
     */
    @Test
    public void testUserQuery() {

        final MockStyle style = new MockStyle();
        final MockFeatureTypeStyle fts = new MockFeatureTypeStyle();
        final MockRule rule = new MockRule();
        final MockLineSymbolizer symbolizer = new MockLineSymbolizer();
        style.featureTypeStyles().add(fts);
        fts.rules().add(rule);
        rule.symbolizers().add(symbolizer);

        final Set<Identifier> ids = new HashSet<>();
        ids.add(filterFactory.featureId("1"));
        ids.add(filterFactory.featureId("20"));
        final Filter filter = filterFactory.id(ids);
        final SimpleQuery query = new SimpleQuery();
        query.setFilter(filter);

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

        final Set<Entry<String, Symbolizer>> presentations = present(layers);
        assertEquals(2, presentations.size());
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("1", symbolizer)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("20", symbolizer)));
    }

    /**
     * Portray using defined type names.
     * Test expect only boat type features to be rendered.
     */
    @Test
    public void testFeatureTypeStyleTypeNames() {

        final MockStyle style = new MockStyle();
        final MockFeatureTypeStyle fts = new MockFeatureTypeStyle();
        fts.featureTypeNames().add(Names.createLocalName(null, null, "boat"));
        final MockRule rule = new MockRule();
        final MockLineSymbolizer symbolizer = new MockLineSymbolizer();
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

        final Set<Entry<String, Symbolizer>> presentations = present(layers);
        assertEquals(2, presentations.size());
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("10", symbolizer)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("20", symbolizer)));
    }

    /**
     * Portray using defined type names.
     * Test expect only point geometric type to be rendered.
     */
    @Test
    public void testFeatureTypeStyleSemanticType() {

        final MockStyle style = new MockStyle();
        final MockFeatureTypeStyle fts = new MockFeatureTypeStyle();
        fts.semanticTypeIdentifiers().add(SemanticType.POINT);
        final MockRule rule = new MockRule();
        final MockLineSymbolizer symbolizer = new MockLineSymbolizer();
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

        final Set<Entry<String, Symbolizer>> presentations = present(layers);
        assertEquals(2, presentations.size());
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("1", symbolizer)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("2", symbolizer)));
    }

    /**
     * Portray using defined rule filter
     * Test expect only features with identifier equals "2" to match.
     */
    @Test
    public void testRuleFilter() {

        final Set<Identifier> ids = new HashSet<>();
        ids.add(filterFactory.featureId("2"));
        final Filter filter = filterFactory.id(ids);

        final MockStyle style = new MockStyle();
        final MockFeatureTypeStyle fts = new MockFeatureTypeStyle();
        final MockRule rule = new MockRule();
        rule.setFilter(filter);
        final MockLineSymbolizer symbolizer = new MockLineSymbolizer();
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

        final Set<Entry<String, Symbolizer>> presentations = present(layers);
        assertEquals(1, presentations.size());
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("2", symbolizer)));
    }

    /**
     * Portray using defined rule scale filter.
     * Test expect only matching scale rule symbolizer to be portrayed.
     */
    @Test
    public void testRuleScale() {

        final MockLineSymbolizer symbolizerAbove = new MockLineSymbolizer();
        final MockLineSymbolizer symbolizerUnder = new MockLineSymbolizer();
        final MockLineSymbolizer symbolizerMatch = new MockLineSymbolizer();

        //Symbology rendering scale here is 3.944391406060875E8
        final MockRule ruleAbove = new MockRule();
        ruleAbove.symbolizers().add(symbolizerAbove);
        ruleAbove.setMinScaleDenominator(4e8);
        ruleAbove.setMaxScaleDenominator(Double.MAX_VALUE);
        final MockRule ruleUnder = new MockRule();
        ruleUnder.symbolizers().add(symbolizerUnder);
        ruleUnder.setMinScaleDenominator(0.0);
        ruleUnder.setMaxScaleDenominator(3e8);
        final MockRule ruleMatch = new MockRule();
        ruleMatch.symbolizers().add(symbolizerMatch);
        ruleMatch.setMinScaleDenominator(3e8);
        ruleMatch.setMaxScaleDenominator(4e8);

        final MockStyle style = new MockStyle();
        final MockFeatureTypeStyle fts = new MockFeatureTypeStyle();
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

        final Set<Entry<String, Symbolizer>> presentations = present(layers);
        assertEquals(4, presentations.size());
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("1", symbolizerMatch)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("2", symbolizerMatch)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("10", symbolizerMatch)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("20", symbolizerMatch)));
    }

    /**
     * Portray using defined rule 'is else' property.
     * Test expect only feature with identifier "10" to be rendered with the base rule
     * and other features to rendered with the fallback rule.
     */
    @Test
    public void testRuleElseCondition() {

        final Set<Identifier> ids = new HashSet<>();
        ids.add(filterFactory.featureId("10"));
        final Filter filter = filterFactory.id(ids);

        final MockLineSymbolizer symbolizerBase = new MockLineSymbolizer();
        final MockLineSymbolizer symbolizerElse = new MockLineSymbolizer();

        final MockRule ruleBase = new MockRule();
        ruleBase.symbolizers().add(symbolizerBase);
        ruleBase.setFilter(filter);
        final MockRule ruleOther = new MockRule();
        ruleOther.setIsElseFilter(true);
        ruleOther.symbolizers().add(symbolizerElse);

        final MockStyle style = new MockStyle();
        final MockFeatureTypeStyle fts = new MockFeatureTypeStyle();
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

        final Set<Entry<String, Symbolizer>> presentations = present(layers);
        assertEquals(4, presentations.size());
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("1", symbolizerElse)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("2", symbolizerElse)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("10", symbolizerBase)));
        assertTrue(presentations.contains(new AbstractMap.SimpleEntry<>("20", symbolizerElse)));
    }


}
