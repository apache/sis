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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class AddDimCodedResource extends AbstractResource implements CodedResource {

    private final CodedResource source;
    private final GridGeometry slice;
    private final CodedGeometry compound;
    private final List<CodedGeometry> alternates = new ArrayList<>();

    public AddDimCodedResource(CodedResource source, GridGeometry slice) throws DataStoreException {
        super(null);
        this.source = source;
        this.slice = slice;
        compound = CodedGeometry.compound(source.getGridGeometry(), new CodedGeometry(slice));

        for (CodedGeometry g : source.getAlternateGridGeometry()) {
            alternates.add(CodedGeometry.compound(g, new CodedGeometry(slice)));
        }
    }

    public CodedResource getSource() {
        return source;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return source.getIdentifier();
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        Optional<Envelope> opt = source.getEnvelope();
        if (opt.isPresent() && slice.isDefined(GridGeometry.ENVELOPE)) {
            Envelope e;
            try {
                e = Envelopes.compound(opt.get(), slice.getEnvelope());
                return Optional.of(e);
            } catch (FactoryException ex) {
                //we have try
            }
        }
        return opt;
    }

    @Override
    protected Metadata createMetadata() throws DataStoreException {
        return source.getMetadata();
    }

    @Override
    public CodedGeometry getGridGeometry() throws DataStoreException {
        return compound;
    }

    @Override
    public List<CodedGeometry> getAlternateGridGeometry() throws DataStoreException {
        return Collections.unmodifiableList(alternates);
    }

    @Override
    public CodedCoverage read(CodedGeometry geometry, int... range) throws DataStoreException {
        final CodedCoverage coverage = source.read(geometry, range);
        return new AddDimCodedCoverage(coverage, slice);
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return source.getSampleDimensions();
    }

}
