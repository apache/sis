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
package org.apache.sis.map.service;

import java.awt.Shape;
import java.util.List;
import java.util.stream.Stream;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.Presentation;
import org.apache.sis.style.Style;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link PainterRegistry}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class PainterRegistryTest {

    /**
     * Creates a new test case.
     */
    public PainterRegistryTest() {
    }

    /**
     * A style class with a registered painter.
     */
    private static class BaseStyle implements Style {
    }

    /**
     * A style class without registered painter, inheriting the painter of {@link BaseStyle}.
     */
    private static class SubStyle extends BaseStyle {
    }

    /**
     * A style interface with a registered painter.
     */
    private interface MarkerStyle extends Style {
    }

    /**
     * A style class inheriting the painter of {@link MarkerStyle}.
     */
    private static final class MarkedStyle implements MarkerStyle {
    }

    /**
     * A style class unrelated to any registered painter.
     */
    private static final class OtherStyle implements Style {
    }

    /**
     * A painter which only declares the style class it supports.
     */
    private static final class TestPainter implements StylePainter {
        private final Class<? extends Style> styleClass;

        TestPainter(final Class<? extends Style> styleClass) {
            this.styleClass = styleClass;
        }

        @Override
        public Class<? extends Style> getStyleClass() {
            return styleClass;
        }

        @Override
        public void paint(final Scene2D scene, final MapLayer layer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<Presentation> present(final Scene2D scene, final MapLayer layer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<Presentation> intersects(final Scene2D scene, final MapLayer layer, final Shape mask) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Tests that the painter of a parent class or interface is found.
     */
    @Test
    public void testInheritedPainter() {
        final TestPainter base   = new TestPainter(BaseStyle.class);
        final TestPainter marker = new TestPainter(MarkerStyle.class);
        final PainterRegistry registry = new PainterRegistry(List.of(base, marker));
        assertSame(base,   registry.get(BaseStyle.class));
        assertSame(base,   registry.get(SubStyle.class));
        assertSame(marker, registry.get(MarkedStyle.class));
        assertNull(registry.get(OtherStyle.class));
        assertNull(registry.get(OtherStyle.class), "Cached negative result.");
    }

    /**
     * Tests that a painter registered for the exact class has precedence over the parent painter.
     */
    @Test
    public void testExactPainterFirst() {
        final TestPainter base = new TestPainter(BaseStyle.class);
        final TestPainter sub  = new TestPainter(SubStyle.class);
        final PainterRegistry registry = new PainterRegistry(List.of(base, sub));
        assertSame(sub,  registry.get(SubStyle.class));
        assertSame(base, registry.get(BaseStyle.class));
    }
}
