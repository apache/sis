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
package org.apache.sis.referencing.dggs.smc;

import javax.measure.IncommensurableException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.GridConstraints;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.referencing.dggs.AbstractDggrsTest;


/**
 * Tests {@link SmcDggrs} on a global lattice whose north pole is at 135°E 10°N,
 * the rotated pole of the Arctic SMC grid.
 *
 * @author Johann Sorel (Geomatys)
 * @see "Li 2023, SMC grid guide, section 7.5"
 */
public class SmcRotatedTest extends AbstractDggrsTest {

    private static final SmcDggrs SMC;
    static {
        try {
            SMC = new SmcDggrs("SMC rotated", null, 0, -90, 45, 30, 8, 6, 10, 135);
        } catch (FactoryException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public SmcRotatedTest() {
        super(SMC);
    }

    private SmcZone zoneAt(final int level, final double longitude, final double latitude) throws TransformException {
        final DirectPosition position = new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), longitude, latitude);
        return SMC.dggs.dggh.get(level).getZone(position);
    }

    /**
     * The geographic position of the rotated pole is in the lattice north polar cell.
     */
    @Test
    public void testLatticePole() throws TransformException {
        final SmcZone zone = zoneAt(4, 135, 10);
        assertTrue(zone.isNorthPolar());
        final DirectPosition position = zone.getPosition();
        assertEquals(135, position.getCoordinate(0), 1e-9);
        assertEquals(10,  position.getCoordinate(1), 1e-9);
    }

    /**
     * The geographic north pole is at lattice latitude 10°, and the envelope of its cell reaches it.
     */
    @Test
    public void testGeographicPole() throws TransformException {
        final SmcZone zone = zoneAt(3, 0, 90);
        assertFalse(zone.isPolar());
        assertTrue(zone.south() <= 10 && zone.north() >= 10, zone::toString);
        final Envelope envelope = zone.getEnvelope();
        assertEquals(90,   envelope.getMaximum(1));
        assertEquals(-180, envelope.getMinimum(0));
        assertEquals(180,  envelope.getMaximum(0));
    }

    /**
     * Enabled for SMC, which passes the spatial search test disabled for other reference systems.
     */
    @Test
    @Override
    public void testSearchEnvelope() throws TransformException, IncommensurableException {
        super.testSearchEnvelope();
    }

    /**
     * Enabled for SMC, which passes the cell geometry test disabled for other reference systems.
     */
    @Test
    @Override
    public void testPickingInCellGeometry() throws TransformException, IncommensurableException {
        super.testPickingInCellGeometry();
    }

    /**
     * Rotated cells are not aligned with the base CRS axes.
     */
    @Test
    public void testGridConstraints() {
        assertFalse(SMC.getGridSystem().getGridConstraints().contains(GridConstraints.cellAxisAligned));
        assertEquals(10,  SMC.getGridSystem().getParameters().getOrientation().getLatitude());
        assertEquals(135, SMC.getGridSystem().getParameters().getOrientation().getLongitude());
    }
}
