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
package org.apache.sis.style.sld1;

import java.net.URI;
import java.util.Optional;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.style.se1.Description;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.Symbology;
import org.apache.sis.util.SimpleInternationalString;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;


/**
 * Base class of all tests of Styled Layer Descriptor elements.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class SLDTestCase extends TestCase {
    /**
     * Creates a new test case.
     */
    SLDTestCase() {
    }

    /**
     * Creates a dummy description with arbitrary title and abstract.
     *
     * @return a new description.
     */
    static Description<Feature> anyDescription() {
        final var value = FeatureTypeStyle.FACTORY.createDescription();
        value.setTitle(new SimpleInternationalString("A random title"));
        value.setAbstract(new SimpleInternationalString("A random abstract"));
        return value;
    }

    /**
     * Creates a dummy online resource.
     *
     * @return a new online resource.
     */
    static OnlineResource anyOnlineResource() {
        return new DefaultOnlineResource(URI.create("http://example.org/resource.xml"));
    }

    /**
     * Creates a dummy user style containing one feature type style.
     *
     * @param  name  name of the user style.
     * @return a new user style.
     */
    static Symbology anyUserStyle(final String name) {
        final var style = new Symbology();
        style.setName(name);
        style.featureTypeStyles().add(new FeatureTypeStyle());
        return style;
    }

    /**
     * Asserts that the given optional is empty.
     *
     * @param  opt  the optional to check.
     */
    static void assertEmpty(final Optional<?> opt) {
        assertTrue(opt.isEmpty());
    }

    /**
     * Asserts that the value of the given optional is equal to the expected value.
     *
     * @param <E>       type of object to compare.
     * @param expected  the expected value.
     * @param opt       the actual value in an optional.
     */
    static <E> void assertOptionalEquals(final E expected, final Optional<E> opt) {
        assertEquals(expected, opt.orElseThrow());
    }

    /**
     * Asserts that the given clone is equal to the original but is a distinct instance.
     *
     * @param  original  the original object.
     * @param  clone     the clone of the original object.
     */
    static void assertCloneEquals(final Object original, final Object clone) {
        assertNotSame(original, clone);
        assertEquals(original, clone);
        assertEquals(original.hashCode(), clone.hashCode());
    }
}
