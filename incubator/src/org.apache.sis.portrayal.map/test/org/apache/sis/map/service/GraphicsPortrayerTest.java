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
 * See the License for the specific language governing permissions andz
 * limitations under the License.
 */
package org.apache.sis.map.service;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.base.MemoryFeatureSet;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.LineSymbolizer;
import org.apache.sis.style.se1.Rule;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.Symbology;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GraphicsPortrayerTest {

    private final GridGeometry WORLD = new GridGeometry(
            new GridExtent(360, 180),
            CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()),
            GridOrientation.REFLECTION_Y);

    private final GeometryFactory GF = new GeometryFactory();

    /**
     * Creates a new test case.
     */
    public GraphicsPortrayerTest() {
    }

    /**
     * Sanity test for rendering.
     */
    @Test
    public void testPortray() throws RenderingException {

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Geometry.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        final FeatureType ft = ftb.build();

        final Feature feature = ft.newInstance();
        LineString geom = GF.createLineString(new Coordinate[]{new Coordinate(0,0), new Coordinate(0,90)});
        geom.setUserData(WORLD.getCoordinateReferenceSystem());
        feature.setPropertyValue("geom", geom);

        final FeatureSet featureSet = new MemoryFeatureSet(null, ft, Arrays.asList(feature));

        final LineSymbolizer<Feature> symbolizer = new LineSymbolizer<>(FeatureTypeStyle.FACTORY);
        final Symbology style = createStyle(symbolizer);

        final MapLayer item = new MapLayer();
        item.setData(featureSet);
        item.setStyle(style);

        BufferedImage image = new GraphicsPortrayer()
                .setDomain(WORLD)
                .portray(item)
                .getImage();

        int color1 = image.getRGB(180, 45);
        int color2 = image.getRGB(179, 45);
        assertEquals(color1, Color.BLACK.getRGB());
        assertEquals(color2, new Color(0,0,0,0).getRGB());
    }

    /**
     * Sanity test for presentation.
     */
    @Test
    public void testPresent() throws RenderingException {

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(String.class).setName(AttributeConvention.IDENTIFIER);
        ftb.addAttribute(Geometry.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        final FeatureType ft = ftb.build();

        final Feature feature = ft.newInstance();
        LineString geom = GF.createLineString(new Coordinate[]{new Coordinate(0,0), new Coordinate(0,90)});
        geom.setUserData(WORLD.getCoordinateReferenceSystem());
        feature.setPropertyValue(AttributeConvention.IDENTIFIER, "test-1");
        feature.setPropertyValue("geom", geom);

        final FeatureSet featureSet = new MemoryFeatureSet(null, ft, Arrays.asList(feature));

        final LineSymbolizer<Feature> symbolizer = new LineSymbolizer<>(FeatureTypeStyle.FACTORY);
        final Symbology style = createStyle(symbolizer);

        final MapLayer item = new MapLayer();
        item.setData(featureSet);
        item.setStyle(style);

        try (Stream<Presentation> stream = new GraphicsPortrayer()
                .setDomain(WORLD)
                .present(item)) {
            Object[] presentations = stream.toArray();
            assertEquals(1, presentations.length);
            assertTrue(presentations[0] instanceof SEPresentation);
            final SEPresentation pe = (SEPresentation) presentations[0];
            assertEquals(item, pe.getLayer());
            assertEquals(featureSet, pe.getResource());
            assertEquals("test-1", pe.getCandidate().getPropertyValue(AttributeConvention.IDENTIFIER));
            assertEquals(symbolizer, pe.getSymbolizer());
        }
    }

    /**
     * Sanity test for intersection.
     */
    @Test
    public void testIntersects() throws RenderingException {

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Geometry.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        final FeatureType ft = ftb.build();

        final Feature feature = ft.newInstance();
        LineString geom = GF.createLineString(new Coordinate[]{new Coordinate(0,0), new Coordinate(0,90)});
        geom.setUserData(WORLD.getCoordinateReferenceSystem());
        feature.setPropertyValue("geom", geom);

        final FeatureSet featureSet = new MemoryFeatureSet(null, ft, Arrays.asList(feature));

        final LineSymbolizer<Feature> symbolizer = new LineSymbolizer<>(FeatureTypeStyle.FACTORY);
        final Symbology style = createStyle(symbolizer);

        final MapLayer item = new MapLayer();
        item.setData(featureSet);
        item.setStyle(style);

        //rectangle outside, no intersection
        try (Stream<Presentation> result = new GraphicsPortrayer()
                .setDomain(WORLD)
                .intersects(item, new Rectangle(7,50,4,4))) {
            assertEquals(0, result.count());
        }

        //rectangle overlaps, intersects
        try (Stream<Presentation> result = new GraphicsPortrayer()
                .setDomain(WORLD)
                .intersects(item, new Rectangle(178,50,4,4))) {
            assertEquals(1, result.count());
        }
    }

    private static Symbology createStyle(Symbolizer symbolizer) {
        final Symbology style = new Symbology();
        final FeatureTypeStyle fts = new FeatureTypeStyle();
        style.featureTypeStyles().add(fts);

        final Rule<Feature> rule = new Rule<>(FeatureTypeStyle.FACTORY);
        rule.symbolizers().add(symbolizer);
        fts.rules().add(rule);

        return style;
    }

}
