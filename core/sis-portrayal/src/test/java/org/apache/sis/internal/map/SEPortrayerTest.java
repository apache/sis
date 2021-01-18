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
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.storage.MemoryFeatureSet;
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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.style.SemanticType;
import org.opengis.style.Symbolizer;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class SEPortrayerTest extends TestCase {

    private final FeatureSet fishes;
    private final FeatureSet boats;

    public SEPortrayerTest() {

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

        final Point point2 = gf.createPoint(new Coordinate(1, 1));
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
        final GridGeometry grid = new GridGeometry(new GridExtent(360, 180), CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()), GridOrientation.REFLECTION_Y);
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
}
