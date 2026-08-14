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
package org.apache.sis.referencing.dggs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import org.apache.sis.referencing.gazetteer.ReferencingByIdentifiers;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.citation.Party;
import org.opengis.referencing.gazetteer.LocationType;
import org.opengis.referencing.operation.TransformException;

/**
 * Integrated system comprised of a specific discrete global grid hierarchy, spatiotemporal referencing by
 * zone identifiers and deterministic sub-zone ordering.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#term-dggrs
 */
public abstract class DiscreteGlobalGridReferenceSystem extends ReferencingByIdentifiers {

    private Party party;
    private boolean partyCreated = false;

    public DiscreteGlobalGridReferenceSystem(Map<String, ?> properties, LocationType[] types) {
        super(properties, types);
    }

    @Override
    public Party getOverallOwner() {
        if (!partyCreated) {
            partyCreated = true;
            try {
                party = DiscreteGlobalGridReferenceSystems.createParty(getName().getCode(), getGridSystem());
                if (party == null) partyCreated = false;
            } catch (TransformException | IOException | URISyntaxException ex) {
                //do nothing
            }
        }
        return party;
    }

    /**
     * A set of key words defining this DGGS.
     *
     * @return list of key words, never null, can be empty.
     */
    public abstract List<String> getKeywords();

    /**
     * Link to the DGGS definition.
     *
     * @return DGGS specification site.
     */
    public abstract URI getUri();

    /**
     * Returns the global grid system.
     *
     * @return global grid system, never null
     */
    public abstract DiscreteGlobalGridSystem getGridSystem();

    /**
     * Returns the zone reference system.
     *
     * @return zonal reference system, never null
     */
    public abstract ZonalReferenceSystem getZonalSystem();

    /**
     * Returns a description of the child zone ordering.
     *
     * @return sub zone order, never null
     */
    public abstract SubZoneOrder getSubZoneOrder();

    /**
     * {@inheritDoc }
     */
    @Override
    public abstract Coder createCoder();

    public abstract static class Coder extends ReferencingByIdentifiers.Coder {

        /**
         * @return base DGGRS
         */
        @Override
        public abstract DiscreteGlobalGridReferenceSystem getReferenceSystem();

        /**
         * @return coder hierarchy level
         */
        public abstract int getPrecisionLevel();

        /**
         * @param level set coder hierarchy level
         * @throws IncommensurableException
         */
        public abstract void setPrecisionLevel(int level) throws IncommensurableException;

        @Override
        public final void setPrecision(Quantity<?> qnt, DirectPosition dp) throws IncommensurableException {
            final DiscreteGlobalGridHierarchy hierarchy = getReferenceSystem().getGridSystem().getHierarchy();
            setPrecisionLevel(hierarchy.getGrid(qnt).getRefinementLevel());
        }

        @Override
        public final Quantity<?> getPrecision(DirectPosition dp) {
            return getReferenceSystem().getGridSystem().getHierarchy()
                    .getGrids().get(getPrecisionLevel()).getPrecision();
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public abstract String encode(DirectPosition dp) throws TransformException;

        /**
         * Convert a location to a zone identifier.
         */
        public abstract Object encodeIdentifier(DirectPosition dp) throws TransformException;

        /**
         * Compute the zone object for given identifier
         */
        public Zone decode(Object zid) throws TransformException {
            return getReferenceSystem().getGridSystem().getHierarchy().getZone(zid);
        }

        @Override
        public Zone decode(CharSequence cs) throws TransformException {
            return getReferenceSystem().getGridSystem().getHierarchy().getZone(cs);
        }
    }
}
