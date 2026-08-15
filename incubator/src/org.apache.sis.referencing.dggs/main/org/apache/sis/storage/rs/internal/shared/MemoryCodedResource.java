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
package org.apache.sis.storage.rs.internal.shared;

import java.util.List;
import java.util.Optional;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;


/**
 * Decorate a referenced coverage as a referenced resource.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MemoryCodedResource extends AbstractResource implements CodedResource {

    private final GenericName name;
    private final CodedCoverage coverage;

    public MemoryCodedResource(GenericName name, CodedCoverage coverage) {
        super(null);
        this.name = name;
        this.coverage = coverage;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.ofNullable(name);
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return coverage.getSampleDimensions();
    }

    @Override
    public CodedGeometry getGridGeometry() {
        return coverage.getGeometry();
    }

    @Override
    public CodedCoverage read(CodedGeometry geometry, int... range) throws DataStoreException {
        return coverage;
    }

}
