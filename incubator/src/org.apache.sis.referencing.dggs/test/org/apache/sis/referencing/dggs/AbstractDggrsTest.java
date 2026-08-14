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

import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import javax.measure.IncommensurableException;
import org.apache.sis.coverage.BandedCoverage;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vector2D;
import org.apache.sis.referencing.GeodeticCalculator;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.iso.Names;
import org.apache.sis.storage.dggs.internal.shared.ArrayDiscreteGlobalGridCoverage;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;
import org.apache.sis.storage.rs.CodeIterator;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractDggrsTest {

    private final DiscreteGlobalGridReferenceSystem dggrs;

    public AbstractDggrsTest(DiscreteGlobalGridReferenceSystem dggrs) {
        this.dggrs = dggrs;
    }

    @Test
    public void testRoots() throws TransformException {

        //test there is at least one root zone
        final List<Zone> rootZoneIds = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().toList();
        assertTrue(!rootZoneIds.isEmpty());

    }

    @Test
    public void testChildrenParentRelations() throws TransformException {

        final List<Zone> rootZoneIds = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().limit(10).toList();

        for (Zone zone : rootZoneIds) {
            testParentChildrenRelations(zone, 0, 4);
            testParentChildrenAtDepth(zone, 3);
        }
    }

    private void testParentChildrenRelations(Zone zone, int currentDepth, int testDepth) {

        //check zone refinement level matched the current depth
        assertEquals(currentDepth, zone.getLocationType().getRefinementLevel());

        //check at depth of zero we have this zone
        try (Stream<Zone> rd0 = zone.getChildrenAtRelativeDepth(0)) {
            List<Zone> lst = rd0.toList();
            assertEquals(1, lst.size());
            assertEquals(zone, lst.get(0));
        }

        //check at depth of 1 we have the same zones as relative depth of 1
        final Collection<? extends Zone> children1 = zone.getChildren();
        try (Stream<Zone> children2 = zone.getChildrenAtRelativeDepth(1)) {
            List<Zone> lst = children2.toList();
            assertEquals(lst.size(), children1.size());
            assertTrue(lst.containsAll(children1));
        }

        //check the children have this zone as parent
        for (Zone z : children1) {
            final Collection<? extends Zone> parents = z.getParents();
            assertTrue(parents.contains(zone));
        }

        if (testDepth > 0) {
            for (Zone z : children1) {
                testParentChildrenRelations(z, currentDepth+1, testDepth-1);
            }
        }
    }

    private void testParentChildrenAtDepth(Zone zone, int testDepth) {

        final List<Zone> children = zone.getChildrenAtRelativeDepth(testDepth).toList();

        for (Zone z : children) {
            List<? extends Zone> backToParent = z.getParents().stream()
                    .flatMap((p) -> p.getParents().stream())
                    .flatMap((pr) -> pr.getParents().stream()).toList();
            assertTrue(backToParent.contains(zone), z +" does not have expected parent " + zone + " in list : " + Arrays.toString(backToParent.toArray()));
        }
    }

    @Test
    public void testCoder() throws TransformException, IncommensurableException {

        final List<Zone> rootZoneIds = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().toList();
        final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();

        for (Zone zone : rootZoneIds) {
            final List<Zone> candidates = zone.getChildrenAtRelativeDepth(3).toList();

            //check the coder can find the zone by location
            for (Zone z : candidates) {
                coder.setPrecisionLevel(z.getLocationType().getRefinementLevel());
                String candidate = coder.encode(z.getPosition());
                assertEquals(z.getGeographicIdentifier().toString(), candidate);
                assertEquals(z, coder.decode(candidate));
            }
        }

    }

    @Disabled //TODO does not pass for all H3 and Healpix cells yet
    @Test
    public void testSearchEnvelope() throws TransformException, IncommensurableException {

        final List<Zone> rootZoneIds = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().toList();
        final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();
        coder.setPrecisionLevel(3);

        for (Zone zone : rootZoneIds) {
            final List<Zone> candidates = dggrs.getGridSystem().getHierarchy().getGrids().get(3).getZones(zone.getEnvelope()).toList();
            final List<Zone> children3 = zone.getChildrenAtRelativeDepth(3).toList();
            //must contain at least all direct children
            assertTrue(candidates.containsAll(children3));
        }

    }

    @Disabled //TODO does not pass for all A5 cells yet
    @Test
    public void testPickingInCellGeometry() throws TransformException, IncommensurableException {

        final List<Zone> rootZoneIds = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().toList();
        final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();

        final GeodeticCalculator calculator = GeodeticCalculator.create(dggrs.getGridSystem().getCrs());

        for (Zone zone : rootZoneIds) {
            final List<Zone> candidates = zone.getChildrenAtRelativeDepth(3).toList();

            //check the coder can find the zone using points inside the cell polygon
            for (Zone z : candidates) {
                System.out.println(z.getGeographicIdentifier());
                coder.setPrecisionLevel(z.getLocationType().getRefinementLevel());

                final Polygon geometry = (Polygon) DiscreteGlobalGridSystems.toSISPolygon(z.getGeographicExtent());
                final DirectPosition center = z.getPosition();
                final LinearRing exterior = (LinearRing) geometry.getExteriorRing();
                final PointSequence ps = exterior.getPoints();
                for (int i = 0; i < ps.size(); i++) {
                    final Tuple corner = ps.getPosition(i);

                    calculator.setStartPoint(center);
                    calculator.setEndPoint(Vectors.asDirectPostion(corner));
                    double distance = calculator.getGeodesicDistance();
                    calculator.setGeodesicDistance(distance * 0.99);
                    final Vector2D.Double candidateInside = new Vector2D.Double(calculator.getEndPoint().getCoordinates());
                    calculator.setGeodesicDistance(distance * 1.01);
                    final Vector2D.Double candidateOutside = new Vector2D.Double(calculator.getEndPoint().getCoordinates());

                    final String candidateInsideHash = coder.encode(Vectors.asDirectPostion(candidateInside));
                    final String candidateOutsideHash = coder.encode(Vectors.asDirectPostion(candidateOutside));
                    System.out.println(z.getGeographicExtent().toString());
                    System.out.println(candidateInside + " " +candidateOutside);
                    System.out.println(candidateInsideHash + " " +candidateOutsideHash);
                    assertEquals(z.getGeographicIdentifier().toString(), candidateInsideHash);
                    assertNotEquals(z.getGeographicIdentifier().toString(), candidateOutsideHash);
                }
            }
        }
    }

    @Test
    public void testSampling() throws TransformException, FactoryException {

        final List<Zone> rootZoneIds = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().limit(3).toList();

        for (Zone zone : rootZoneIds) {
            final List<Zone> candidates = zone.getChildrenAtRelativeDepth(2).limit(3).toList();

            for (Zone z : candidates) {
                final DirectPosition position = z.getPosition();
                final ArrayDiscreteGlobalGridCoverage dggsCoverage = new ArrayDiscreteGlobalGridCoverage(
                        Names.createLocalName(null, null, "test"), DiscreteGlobalGridGeometry.unstructured(dggrs, List.of(z.getIdentifier()), null),
                        List.of(NDArrays.of(1, 123456)));

                //test iterator
                final CodeIterator iterator = dggsCoverage.createIterator();
                assertTrue(iterator.next());
                assertEquals(123456.0, iterator.getSampleDouble(0), 0.0);
                assertFalse(iterator.next());

                //test evaluator at cell center
                BandedCoverage.Evaluator evaluator = dggsCoverage.evaluator();
                double[] values = evaluator.apply(position);
                assertEquals(1, values.length);
                assertEquals(123456.0, values[0], 0.0);

                //test image sampling
                final CoordinateReferenceSystem orthocrs = DiscreteGlobalGridSystems.createOrthographicCRS(CommonCRS.WGS84.normalizedGeographic(), position.getCoordinate(1), position.getCoordinate(0));

                Envelope env = dggsCoverage.getGeometry().getEnvelope(orthocrs);
                final GridGeometry geom = new GridGeometry(new GridExtent(256, 256), env, GridOrientation.REFLECTION_Y);
                final GridCoverage gridCoverage = dggsCoverage.sample(geom, geom);
                final RenderedImage image = gridCoverage.render(geom.getExtent());
                values = image.getData().getPixel(128, 128, (double[])null);
                assertEquals(1, values.length);
                assertEquals(123456.0, values[0], 0.0);

            }
        }
    }

}
