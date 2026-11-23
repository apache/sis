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
package org.apache.sis.storage.rs;

import java.awt.image.BufferedImage;
import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.internal.shared.AbstractCodedCoverage;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link CodedCoverage}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CodedCoverageTest {

    /**
     * Tests {@link CodedCoverage.CodeEvaluator#getCoverage()} when the evaluator of the coverage
     * fetches its values from another coverage, which is not a coded coverage.
     */
    @Test
    public void testCodeEvaluatorCoverage() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final List<Object> zones = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().map(Zone::getIdentifier).toList();
        final GridCoverage values = new GridCoverageBuilder().setValues(new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY)).build();
        final CodedCoverage coverage = new AbstractCodedCoverage(Names.createLocalName(null, null, "test"),
                DiscreteGlobalGridGeometry.unstructured(dggrs, zones, null),
                List.of(new SampleDimension.Builder().setName("value").build()))
        {
            @Override public CodeIterator         createIterator()         {throw new UnsupportedOperationException();}
            @Override public WritableCodeIterator createWritableIterator() {throw new UnsupportedOperationException();}
            @Override public Evaluator            evaluator()              {return values.evaluator();}
        };
        assertSame(coverage, coverage.codeEvaluator().getCoverage());
    }
}
