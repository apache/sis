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
package org.apache.sis.geometries.simplify.greedyinsert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.operation.OperationException;


/**
 * Triangle.
 *
 * @author Johann Sorel (Geomatys)
 */
final class WTriangle extends AbstractGeometry implements Triangle {

    private final LinearRing ring = GeometryFactory.createLinearRing(new PointSequence() {
        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return p0.getCoordinateReferenceSystem();
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public AttributesType getAttributesType() {
            return AttributesType.EMPTY;
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public Point getPoint(final int index) {
            final PointSequence ps = this;
            switch (index) {
                case 0 :
                case 1 :
                case 2 :
                case 3 :
                    return new Point() {
                        @Override
                        public Tuple getPosition() {
                            return ps.getPosition(index);
                        }

                        @Override
                        public Tuple getAttribute(String name) {
                            return ps.getAttribute(index, name);
                        }

                        @Override
                        public void setAttribute(String name, Tuple tuple) {
                            ps.setAttribute(index, name, tuple);
                        }

                        @Override
                        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
                            return p0.getCoordinateReferenceSystem();
                        }

                        @Override
                        public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
                            throw new UnsupportedOperationException("Not supported.");
                        }

                        @Override
                        public AttributesType getAttributesType() {
                            return ps.getAttributesType();
                        }

                        @Override
                        public boolean isEmpty() {
                            return false;
                        }

                    };
                default : throw new ArrayIndexOutOfBoundsException();
            }
        }

        @Override
        public Tuple getPosition(int index) {
            switch (index) {
                case 0 : return p0.copy();
                case 1 : return p1.copy();
                case 2 : return p2.copy();
                case 3 : return p0.copy();
                default : throw new ArrayIndexOutOfBoundsException();
            }
        }

        @Override
        public void setPosition(int index, Tuple value) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Tuple getAttribute(int index, String name) {
            if (AttributesType.ATT_POSITION.equals(name)) {
                return getPosition(index);
            }
            return null;
        }

        @Override
        public void setAttribute(int index, String name, Tuple value) {
            if (AttributesType.ATT_POSITION.equals(name)) {
                setPosition(index, value);
                return;
            }
            throw new UnsupportedOperationException("Not supported.");
        }
    });

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public LinearRing getExteriorRing() {
        return ring;
    }

    private static class Candidate implements Comparable<Candidate>{

        private final Tuple tuple;
        private final double error;

        public Candidate(Tuple tuple, double error) {
            this.tuple = tuple;
            this.error = error;
        }

        @Override
        public int compareTo(Candidate other) {
            return Double.compare(other.error, error);
        }
    }

    private boolean obsolete = false;
    public final Edge e0;
    public final Edge e1;
    public final Edge e2;
    /**
     * triangle points in counter clockwise order
     */
    public final Tuple p0;
    public final Tuple p1;
    public final Tuple p2;

    private final List<Candidate> candidates;
    private Candidate max;
    private final BiFunction<Tuple,Triangle,Double> errorCalculator;

    public WTriangle(Edge e0, Edge e1, Edge e2, BiFunction<Tuple,Triangle,Double> errorCalculator) {
        this.errorCalculator = errorCalculator;

        candidates = new ArrayList<>();

        this.e0 = e0;
        this.e1 = e1;
        this.e2 = e2;
        this.p0 = e0.p0;

        Tuple p1 = e0.p1;
        Tuple p2;

        if (e1.p0.equals(e0.p0) || e1.p0.equals(e0.p1)) {
            p2 = e1.p1;
        } else {
            p2 = e1.p0;
        }
        if (!Maths.isCounterClockwise(p0, p1, p2)) {
            Tuple temp = p1;
            p1 = p2;
            p2 = temp;
        }
        this.p1 = p1;
        this.p2 = p2;
    }

    public void addCandidate(Tuple t) {
        final double error = errorCalculator.apply(t, this);
        final Candidate cdt = new Candidate(t, error);

        if (max == null || error > max.error) {
            max = cdt;
        }
        candidates.add(cdt);
    }

    public void removeCandidate(Tuple t) {
        if (max.tuple == t) {
            max = null;
        }

        for (Candidate cdt : candidates) {
            if (cdt.tuple == t) {
                candidates.remove(cdt);
                return;
            }
        }
    }

    public void clearCandidates() {
        candidates.clear();
        max = null;
    }

    public void reassign(WTriangle triangle1, WTriangle triangle2) throws OperationException{
        for (Candidate cdt : candidates) {
            if (triangle1.contains(cdt.tuple)) {
                triangle1.addCandidate(cdt.tuple);
            } else {
                triangle2.addCandidate(cdt.tuple);
            }
        }
    }

    public void reassign(WTriangle triangle1, WTriangle triangle2, WTriangle triangle3) throws OperationException{
        for (Candidate cdt : candidates) {
            if (triangle1.contains(cdt.tuple)) {
                triangle1.addCandidate(cdt.tuple);
            } else if (triangle2.contains(cdt.tuple)) {
                triangle2.addCandidate(cdt.tuple);
            } else {
                triangle3.addCandidate(cdt.tuple);
            }
        }
    }

    public Tuple getFirstCoord() {
        return p0;
    }

    public Tuple getSecondCoord() {
        return p1;
    }

    public Tuple getThirdCoord() {
        return p2;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void obsolete(){
        this.obsolete = true;
    }

    public boolean contains(Tuple pt) {
        assert (!obsolete);
        return Maths.isPointInTriangle_BaryAlgo(p0, p1, p2, pt)
            || e0.isOnEdge(pt)
            || e1.isOnEdge(pt)
            || e2.isOnEdge(pt);
    }

    public Tuple oppositePoint(Edge edge) throws OperationException {
        assert (!obsolete);
        if (!edge.hasPoint(p0)) {
            return p0;
        }
        if (!edge.hasPoint(p1)) {
            return p1;
        }
        if (!edge.hasPoint(p2)) {
            return p2;
        } else {
            throw new OperationException("Should not happen, TIN simplification algorithm flow.");
        }
    }

    public Edge getEdge(Tuple p0, Tuple p1) throws OperationException {
        assert (!obsolete);
        if ((e0.p0 == p0 && e0.p1 == p1) || (e0.p1 == p0 && e0.p0 == p1)) {
            return e0;
        }
        if ((e1.p0 == p0 && e1.p1 == p1) || (e1.p1 == p0 && e1.p0 == p1)) {
            return e1;
        }
        if ((e2.p0 == p0 && e2.p1 == p1) || (e2.p1 == p0 && e2.p0 == p1)) {
            return e2;
        } else {
            throw new OperationException("Should not happen, TIN simplification algorithm flow.");
        }
    }

    public Tuple findMaxDistancePoint(){
        if (max == null && !candidates.isEmpty()) {
            max = candidates.get(0);
            for (int i=1,n=candidates.size();i<n;i++) {
                Candidate c = candidates.get(i);
                if(c.error > max.error) {
                    max = c;
                }
            }
        }

        if (max != null) {
            return max.tuple;
        } else {
            return null;
        }
    }

    public boolean validate() throws OperationException{
        assert (!obsolete);

        if(e0.p0 == e0.p1 || e1.p0 == e1.p1 || e2.p0 == e2.p1) throw new OperationException("Unvalid edges");

        if(e0.p0 != p0 && e0.p0 != p1 && e0.p0 != p2) throw new OperationException("Unvalid edge "+e0);
        if(e0.p1 != p0 && e0.p1 != p1 && e0.p1 != p2) throw new OperationException("Unvalid edge "+e0);

        if(e1.p0 != p0 && e1.p0 != p1 && e1.p0 != p2) throw new OperationException("Unvalid edge "+e1);
        if(e1.p1 != p0 && e1.p1 != p1 && e1.p1 != p2) throw new OperationException("Unvalid edge "+e1);

        if(e2.p0 != p0 && e2.p0 != p1 && e2.p0 != p2) throw new OperationException("Unvalid edge "+e2);
        if(e2.p1 != p0 && e2.p1 != p1 && e2.p1 != p2) throw new OperationException("Unvalid edge "+e2);

        if(e0.t0 != this && e0.t1 != this) throw new OperationException("Unvalid edge "+e0);
        if(e1.t0 != this && e1.t1 != this) throw new OperationException("Unvalid edge "+e1);
        if(e2.t0 != this && e2.t1 != this) throw new OperationException("Unvalid edge "+e2);

        if(e0.isObsolete()) throw new OperationException("Obsolete edge "+e0);
        if(e1.isObsolete()) throw new OperationException("Obsolete edge "+e1);
        if(e2.isObsolete()) throw new OperationException("Obsolete edge "+e2);
        return true;
    }

    @Override
    public String toString() {
        return "T "+p0+" "+p1+" "+p2;
    }

}
