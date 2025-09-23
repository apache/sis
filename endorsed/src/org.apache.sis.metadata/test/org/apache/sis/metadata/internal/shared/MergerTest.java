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
package org.apache.sis.metadata.internal.shared;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.opengis.metadata.content.ImagingCondition;
import org.opengis.metadata.content.ImageDescription;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription;
import org.apache.sis.metadata.iso.content.DefaultImageDescription;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSetEquals;


/**
 * Tests the {@link Merger} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MergerTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MergerTest() {
    }

    /**
     * Creates a metadata sample with 3 content information of different kind.
     */
    private static DefaultMetadata createSample1() {
        final DefaultFeatureCatalogueDescription features = new DefaultFeatureCatalogueDescription();
        final DefaultCoverageDescription         coverage = new DefaultCoverageDescription();
        final DefaultImageDescription            image    = new DefaultImageDescription();
        final DefaultMetadata                    metadata = new DefaultMetadata();
        features.setFeatureCatalogueCitations(Set.of(new DefaultCitation("Shapefile")));
        features.setIncludedWithDataset(Boolean.TRUE);
        metadata.getContentInfo().add(features);

        coverage.setProcessingLevelCode(new DefaultIdentifier("Level 1"));
        metadata.getContentInfo().add(coverage);

        image.setImagingCondition(ImagingCondition.CLOUD);
        image.setCloudCoverPercentage(0.8);
        metadata.getContentInfo().add(image);

        metadata.setLocalesAndCharsets(Map.of(Locale.JAPANESE, StandardCharsets.UTF_16));
        return metadata;
    }

    /**
     * Creates a metadata sample with content information of different kind in a different order
     * than the one created by {@link #createSample1()}.
     */
    private static DefaultMetadata createSample2() {
        final DefaultFeatureCatalogueDescription features = new DefaultFeatureCatalogueDescription();
        final DefaultImageDescription            image    = new DefaultImageDescription();
        final DefaultMetadata                    metadata = new DefaultMetadata();
        image.setProcessingLevelCode(new DefaultIdentifier("Level 2"));
        metadata.getContentInfo().add(image);

        features.setFeatureCatalogueCitations(Set.of(new DefaultCitation("GPX file")));
        features.setIncludedWithDataset(Boolean.TRUE);
        metadata.getContentInfo().add(features);

        metadata.setLocalesAndCharsets(Map.of(Locale.FRENCH, StandardCharsets.UTF_8));
        return metadata;
    }

    /**
     * Tests a merge operation that merge also the collection elements. Such deep merge is a
     * little bit aggressive; it may be desired in some occasions, but may also be dangerous.
     */
    @Test
    public void testDeepMerge() {
        final DefaultMetadata source = createSample1();
        final DefaultMetadata target = createSample2();
        final Merger merger = new Merger(null);
        merger.copy(source, target);

        assertSetEquals(List.of(Locale.JAPANESE, Locale.FRENCH),
                        target.getLocalesAndCharsets().keySet());
        assertSetEquals(List.of(StandardCharsets.UTF_16, StandardCharsets.UTF_8),
                        target.getLocalesAndCharsets().values());

        final Iterator<ContentInformation> it       = target.getContentInfo().iterator();
        final ImageDescription             image    = (ImageDescription)            it.next();
        final FeatureCatalogueDescription  features = (FeatureCatalogueDescription) it.next();
        final DefaultCoverageDescription   coverage = (DefaultCoverageDescription)  it.next();
        assertFalse(it.hasNext());

        assertEquals(ImagingCondition.CLOUD, image   .getImagingCondition());
        assertEquals(Double.valueOf(0.8),    image   .getCloudCoverPercentage());
        assertEquals("Level 2",              image   .getProcessingLevelCode().getCode());
        assertEquals("Level 1",              coverage.getProcessingLevelCode().getCode());
        assertEquals(Boolean.TRUE,           features.isIncludedWithDataset());

        final Iterator<? extends Citation> ci = features.getFeatureCatalogueCitations().iterator();
        assertEquals("GPX file",  ci.next().getTitle().toString());
        assertEquals("Shapefile", ci.next().getTitle().toString());
        assertFalse(ci.hasNext());
    }
}
