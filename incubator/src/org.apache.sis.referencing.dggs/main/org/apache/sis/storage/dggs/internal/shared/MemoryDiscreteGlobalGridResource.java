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
package org.apache.sis.storage.dggs.internal.shared;

import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.dggs.DiscreteGlobalGridResource;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;

/**
 * Decorate a DGGS coverage as a DGGS resource.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MemoryDiscreteGlobalGridResource extends AbstractResource implements DiscreteGlobalGridResource{

    private final CodedCoverage coverage;

    public MemoryDiscreteGlobalGridResource(CodedCoverage coverage) {
        super(null);
        this.coverage = coverage;
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return coverage.getSampleDimensions();
    }

    @Override
    public DiscreteGlobalGridGeometry getGridGeometry() {
        return (DiscreteGlobalGridGeometry) coverage.getGeometry();
    }

    @Override
    public NumberRange<Integer> getAvailableDepths() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getDefaultDepth() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaxRelativeDepth() {
        return 0;
    }

    @Override
    public CodedCoverage read(CodedGeometry geometry, int... range) throws DataStoreException {
        return coverage;
    }

}
