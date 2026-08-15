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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.dggs.DiscreteGlobalGridCoverageProcessor;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.dggs.DiscreteGlobalGridResource;
import org.apache.sis.storage.rs.CodedCoverage;


/**
 *
 * @author Johnan Sorel (Geomatys)
 */
public class CachingTiledDiscreteGlobalGridCoverageResource extends TiledDiscreteGlobalGridCoverageResource {

    private final DiscreteGlobalGridResource source;
    private final DiscreteGlobalGridCoverageProcessor processor;
    private final WritableTiledDiscreteGlobalGridCoverageResource caching;

    public CachingTiledDiscreteGlobalGridCoverageResource(DiscreteGlobalGridResource resource, DiscreteGlobalGridCoverageProcessor processor, WritableTiledDiscreteGlobalGridCoverageResource caching) {
        this.source = resource;
        this.processor = processor;
        this.caching = caching;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return source.getIdentifier();
    }

    @Override
    protected Metadata createMetadata() throws DataStoreException {
        return source.getMetadata();
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return source.getEnvelope();
    }

    @Override
    public NumberRange<Integer> getTileAvailableDepths() {
        return caching.getTileAvailableDepths();
    }

    @Override
    public int getTileRelativeDepth() {
        return caching.getTileRelativeDepth();
    }

    @Override
    public DiscreteGlobalGridGeometry getGridGeometry() {
        return caching.getGridGeometry();
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return caching.getSampleDimensions();
    }

    @Override
    public CodedCoverage getZoneTile(Object identifierOrZone) throws DataStoreException {

        final String zid;
        if (identifierOrZone instanceof Zone z) {
            zid = z.getTextIdentifier().toString();
        } else if (!(identifierOrZone instanceof String)) {
            zid = getGridGeometry().getReferenceSystem().getGridSystem().getHierarchy().toTextIdentifier(identifierOrZone);
        } else {
            zid = (String) identifierOrZone;
        }

        CodedCoverage coverage;
        try {
            lock(zid);

            coverage = caching.getZoneTile(identifierOrZone);
            if (coverage == null) {
                final DiscreteGlobalGridGeometry tileGrid = DiscreteGlobalGridGeometry.subZone(getGridGeometry().getReferenceSystem(), identifierOrZone, getTileRelativeDepth());
                coverage = source.read(tileGrid);
                if (!coverage.getGeometry().equals(tileGrid)) {
                    try {
                        coverage = processor.resample(coverage, tileGrid);
                    } catch (FactoryException | TransformException ex) {
                        throw new DataStoreException(ex.getMessage(), ex);
                    }
                }
                //store it in the cache
                caching.setZoneTile(identifierOrZone, coverage);
            }

        } finally {
            unlock(zid);
        }

        return coverage;
    }


    private final ConcurrentHashMap<String, LockWrapper> locks = new ConcurrentHashMap<>();

    private void lock(String key) {
        LockWrapper lockWrapper = locks.compute(key, (k, v) -> v == null ? new LockWrapper() : v.addThreadInQueue());
        lockWrapper.lock.lock();
    }

    private void unlock(String key) {
        LockWrapper lockWrapper = locks.get(key);
        lockWrapper.lock.unlock();
        if (lockWrapper.removeThreadFromQueue() == 0) {
            // NB : We pass in the specific value to remove to handle the case where another thread would queue right before the removal
            locks.remove(key, lockWrapper);
        }
    }

    private static class LockWrapper {
        private final Lock lock = new ReentrantLock();
        private final AtomicInteger numberOfThreadsInQueue = new AtomicInteger(1);

        private LockWrapper addThreadInQueue() {
            numberOfThreadsInQueue.incrementAndGet();
            return this;
        }

        private int removeThreadFromQueue() {
            return numberOfThreadsInQueue.decrementAndGet();
        }

    }

}
