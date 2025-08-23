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
package org.apache.sis.geometries;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vector2D;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;


/**
 * Decorate a TIN, adding optimized search capabilities.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface PreparedTIN extends TIN {

    /**
     * Triangles intersecting given area.
     * @param env searched area, null for all triangles.
     * @throws org.opengis.referencing.operation.TransformException if envelope transform to TIN crs fails
     */
    Stream<Triangle> getPatches(org.opengis.geometry.Envelope env) throws TransformException;

    public static PreparedTIN create(Collection<TIN> tin) {
        return create(tin.toArray(TIN[]::new));
    }

    public static PreparedTIN create(TIN... tin) {
        if (tin.length == 0) throw new IllegalArgumentException("At least one TIN must be provided");
        if (tin.length == 1) return new Single(tin[0]);
        final Single[] singles = new Single[tin.length];
        for (int i = 0; i < tin.length; i++) singles[i] = new Single(tin[i]);
        return new Multi(singles);
    }

    final class Single implements PreparedTIN {

        private final TIN base;
        private final CoordinateReferenceSystem crs;
        private final CoordinateReferenceSystem crs2d;
        private Quadtree quadTree = new Quadtree();

        public Single(TIN base) {
            ArgumentChecks.ensureNonNull("tin", base);
            this.base = base;
            this.crs = base.getCoordinateReferenceSystem();
            this.crs2d = CRS.getHorizontalComponent(this.crs);
            ArgumentChecks.ensureNonNull("Horizontal CRS", crs2d);
            buildQuadTree();
        }

        private void buildQuadTree() {
            this.quadTree = new Quadtree();
            for (int i = 0, n = base.getNumPatches(); i < n; i++) {
                final Triangle triangle = base.getPatchN(i);
                final Envelope env = triangle.getEnvelope();
                final org.locationtech.jts.geom.Envelope e = new org.locationtech.jts.geom.Envelope(
                    env.getMinimum(0),
                    env.getMaximum(0),
                    env.getMinimum(1),
                    env.getMaximum(1));
                quadTree.insert(e, triangle);
            }
        }

        @Override
        public Stream<Triangle> getPatches(org.opengis.geometry.Envelope env) throws TransformException {
            final Stream<Triangle> stream;
            if (env == null) {
                stream = ((List<Triangle>) quadTree.queryAll()).stream();
            } else {
                final CoordinateReferenceSystem envCrs = env.getCoordinateReferenceSystem();
                final org.locationtech.jts.geom.Envelope e;
                if (CRS.equivalent(envCrs, crs)
                 || CRS.equivalent(envCrs, crs2d)) {
                    //compatible crs
                } else {
                    env = Envelopes.transform(env, crs2d);
                }
                e = new org.locationtech.jts.geom.Envelope(
                    env.getMinimum(0),
                    env.getMaximum(0),
                    env.getMinimum(1),
                    env.getMaximum(1));
                stream = ((List<Triangle>) quadTree.query(e)).stream();
            }
            return stream;
        }

        @Override
        public int getNumPatches() {
            return base.getNumPatches();
        }

        @Override
        public Triangle getPatchN(int n) {
            return base.getPatchN(n);
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return base.getCoordinateReferenceSystem();
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
            throw new IllegalArgumentException("CoordinateReferenceSystem can not be changed on PreparedTIN");
        }

        @Override
        public Envelope getEnvelope() {
            return base.getEnvelope();
        }

        @Override
        public boolean isEmpty() {
            return base.isEmpty();
        }

        @Override
        public AttributesType getAttributesType() {
            return base.getAttributesType();
        }
    }

    final class Multi implements PreparedTIN {

        private final Single[] base;
        private Quadtree quadTree = new Quadtree();
        private GeneralEnvelope all;
        private boolean empty;
        private int nbPatches;
        private int[] offsets;

        public Multi(Single... base) {
            this.base = base;
            buildQuadTree();
        }

        private void buildQuadTree() {
            this.quadTree = new Quadtree();
            this.offsets = new int[base.length];
            for (int i = 0; i < base.length; i++) {
                final Envelope env = base[i].getEnvelope();
                final org.locationtech.jts.geom.Envelope e = new org.locationtech.jts.geom.Envelope(
                    env.getMinimum(0),
                    env.getMaximum(0),
                    env.getMinimum(1),
                    env.getMaximum(1));
                quadTree.insert(e, base[i]);

                if (all == null) all = new GeneralEnvelope(env);
                else all.add(env);
                empty &= base[i].isEmpty();
                offsets[i] = nbPatches;
                nbPatches += base[i].getNumPatches();
            }
        }

        @Override
        public Stream<Triangle> getPatches(org.opengis.geometry.Envelope env) throws TransformException {
            final Stream<Single> stream;
            if (env == null) {
                stream = ((List<Single>) quadTree.queryAll()).stream();
            } else {
                final CoordinateReferenceSystem envCrs = env.getCoordinateReferenceSystem();
                final org.locationtech.jts.geom.Envelope e;
                if (CRS.equivalent(envCrs, base[0].crs)
                 || CRS.equivalent(envCrs, base[0].crs2d)) {
                    //compatible crs
                } else {
                    env = Envelopes.transform(env, base[0].crs2d);
                }
                e = new org.locationtech.jts.geom.Envelope(
                    env.getMinimum(0),
                    env.getMaximum(0),
                    env.getMinimum(1),
                    env.getMaximum(1));
                stream = ((List<Single>) quadTree.query(e)).stream();
            }
            final org.opengis.geometry.Envelope fenv = env;
            return stream.flatMap((Single t) -> {
                try {
                    return t.getPatches(fenv);
                } catch (TransformException ex) {
                    //won't happen
                    throw new RuntimeException(ex);
                }
            });
        }

        @Override
        public int getNumPatches() {
            return nbPatches;
        }

        @Override
        public Triangle getPatchN(int n) {
            int idx = Arrays.binarySearch(offsets, n);
            if (idx < 0) idx = (-(idx) - 1) -1;
            return base[idx].getPatchN(n - offsets[idx]);
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return base[0].getCoordinateReferenceSystem();
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
            throw new IllegalArgumentException("CoordinateReferenceSystem can not be changed on PreparedTIN");
        }

        @Override
        public Envelope getEnvelope() {
            return all;
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }

        @Override
        public AttributesType getAttributesType() {
            return base[0].getAttributesType();
        }
    }

    default Evaluator evaluator() {
        return new Evaluator(this);
    }

    public static final class Evaluator {

        private final PreparedTIN tin;
        private double epsilon = Double.NaN;
        private final CoordinateReferenceSystem tincrs;
        //cache last position crs
        private CoordinateReferenceSystem lastCrs;
        private MathTransform lastTransform;
        private final Vector<?> target;
        private final Vector2D.Double p = new Vector2D.Double();
        private final GeneralEnvelope env;

        private Evaluator(PreparedTIN tin) {
            this.tin = tin;
            this.tincrs = tin.getCoordinateReferenceSystem();
            target = Vectors.createDouble(tincrs);
            env = new GeneralEnvelope(tincrs);
        }

        public double getEpsilon() {
            return epsilon;
        }

        public void setEpsilon(double epsilon) {
            this.epsilon = epsilon;
        }

        public Optional<Point> evaluate(Tuple<?> dp) throws CannotEvaluateException {
            final CoordinateReferenceSystem dpCrs = dp.getCoordinateReferenceSystem();
            final MathTransform transform;
            if (dpCrs == null) {
                //do nothing, we expect the same crs as the surface
                transform = null;
            } else if (dpCrs == lastCrs) {
                transform = lastTransform;
            } else {
                lastCrs = dpCrs;
                if (Geometries.isUndefined(dpCrs)) {
                    //expecting the same crs as the surface, no transform
                    lastTransform = null;
                } else if (CRS.equivalent(tincrs, dpCrs)) {
                    //equivalent crs, no transform
                    lastTransform = null;
                } else {
                    try {
                        lastTransform = CRS.findOperation(dpCrs, tincrs, null).getMathTransform();
                    } catch (FactoryException ex) {
                        throw new CannotEvaluateException(ex.getMessage(), ex);
                    }
                }
                transform = lastTransform;
            }

            if (transform != null) {
                try {
                    dp.transformTo(transform, target);
                    dp = target;
                } catch (TransformException ex) {
                    throw new CannotEvaluateException(ex.getMessage(), ex);
                }
            }
            p.x = dp.get(0);
            p.y = dp.get(1);

            //use a local coordinate epsilon if undefined
            double epsilon = this.epsilon;
            if (!Double.isFinite(epsilon)) epsilon = Math.max(Math.ulp(p.x), Math.ulp(p.y)) * 10;

            double margin = epsilon;
            env.setRange(0, p.x - margin, p.x + margin);
            env.setRange(1, p.y - margin, p.y + margin);

            try (final Stream<Triangle> triangles = tin.getPatches(env)) {
                final Iterator<Triangle> iterator = triangles.iterator();
                while (iterator.hasNext()) {
                    final Triangle triangle = iterator.next();
                    final PointSequence points = triangle.getExteriorRing().getPoints();
                    final Tuple a = points.getPosition(0);
                    final Tuple b = points.getPosition(1);
                    final Tuple c = points.getPosition(2);
                    final double[] bary = Triangle.getBarycentricValue2D(
                            a.get(0), a.get(1),
                            b.get(0), b.get(1),
                            c.get(0), c.get(1),
                            p.get(0), p.get(1),
                            epsilon, true);
                    if (bary != null) {
                        return Optional.of(triangle.interpolate(bary));
                    }
                }
            } catch (TransformException e) {
                throw new CannotEvaluateException(e.getMessage(), e);
            }
            return Optional.empty();
        }
    }
}
