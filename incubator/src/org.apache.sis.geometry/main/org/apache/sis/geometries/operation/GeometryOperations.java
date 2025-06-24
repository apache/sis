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
package org.apache.sis.geometries.operation;

import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MeshPrimitive;
import org.apache.sis.geometries.MeshPrimitiveVisitor;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.operation.spatialanalysis2d.Buffer;
import org.apache.sis.geometries.operation.spatialanalysis2d.ConvexHull;
import org.apache.sis.geometries.operation.spatialanalysis2d.Difference;
import org.apache.sis.geometries.operation.spatialanalysis2d.Distance;
import org.apache.sis.geometries.operation.spatialanalysis2d.Intersection;
import org.apache.sis.geometries.operation.spatialanalysis2d.SymDifference;
import org.apache.sis.geometries.operation.spatialanalysis2d.Union;
import org.apache.sis.geometries.operation.spatialedition.ComputeAttribute;
import org.apache.sis.geometries.operation.spatialedition.To3D;
import org.apache.sis.geometries.operation.spatialedition.ToPrimitive;
import org.apache.sis.geometries.operation.spatialedition.Transform;
import org.apache.sis.geometries.operation.spatialrelations2d.Contains;
import org.apache.sis.geometries.operation.spatialrelations2d.Crosses;
import org.apache.sis.geometries.operation.spatialrelations2d.Disjoint;
import org.apache.sis.geometries.operation.spatialrelations2d.Equals;
import org.apache.sis.geometries.operation.spatialrelations2d.Intersects;
import org.apache.sis.geometries.operation.spatialrelations2d.LocateAlong;
import org.apache.sis.geometries.operation.spatialrelations2d.LocateBetween;
import org.apache.sis.geometries.operation.spatialrelations2d.Overlaps;
import org.apache.sis.geometries.operation.spatialrelations2d.Relate;
import org.apache.sis.geometries.operation.spatialrelations2d.Touches;
import org.apache.sis.geometries.operation.spatialrelations2d.Within;
import org.apache.sis.geometries.processor.Processor;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.measure.quantity.Length;
import org.apache.sis.util.Static;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GeometryOperations extends Static {

    private static final Processor NONE = new Processor() {
        @Override
        public Class<Operation> getOperationClass() {
            return Operation.class;
        }

        @Override
        public Class<Geometry> getGeometryClass() {
            return Geometry.class;
        }

        @Override
        public void process(Operation operand) throws OperationException {
            throw new OperationException("Not supported.");
        }
    };

    private static final Map<UnaryKey,Processor> UNARY_PROCESSORS = new HashMap<>();
    private static final Map<BinaryKey,Processor> BINARY_PROCESSORS = new HashMap<>();
    static {
        final ServiceLoader<Processor> serviceLoader = ServiceLoader.load(Processor.class, Processor.class.getClassLoader());
        final Iterator<Processor> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            final Processor p = iterator.next();
            if (Operation.Binary.class.isAssignableFrom(p.getOperationClass())) {
                final Processor.Binary bp = (Processor.Binary) p;
                final BinaryKey key = new BinaryKey(p.getOperationClass(), p.getGeometryClass(), bp.getRelatedClass(), true);
                BINARY_PROCESSORS.put(key, p);
                if (Operation.ReversableBinary.class.isAssignableFrom(p.getOperationClass())) {
                    final BinaryKey key2 = new BinaryKey(p.getOperationClass(), bp.getRelatedClass(), p.getGeometryClass(), true);
                    BINARY_PROCESSORS.put(key2, p);
                }
            } else {
                final UnaryKey key = new UnaryKey(p.getOperationClass(), p.getGeometryClass(), true);
                UNARY_PROCESSORS.put(key, p);
            }
        }
    }

    private GeometryOperations() {}

    /**
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.4 Methods that support spatial analysis
     */
    @UML(identifier="Query2D", specification=ISO_19107) // section 6.4.8
    public static final class SpatialAnalysis2D extends Static {

        private SpatialAnalysis2D(){}

        /**
         * Returns a geometric object that represents all Points whose distance from this geometric object is less than
         * or equal to distance. Calculations are in the spatial reference system of this geometric object. Because of the
         * limitations of linear interpolation, there will often be some relatively small error in this distance,
         * but it should be near the resolution of the coordinates used.
         */
        public static Geometry buffer(Geometry geom, double distance) {
            return evaluate(new Buffer(geom, distance)).result;
        }

        @UML(identifier="buffer", specification=ISO_19107) // section 6.4.4.24 and 6.4.8.3
        public static Geometry buffer(Geometry geom, Length radius){
            throw new OperationException("Not supported yet");
        }

        /**
         * Returns a geometric object that represents the convex hull of this geometric object.
         * Convex hulls, being dependent on straight lines, can be accurately represented in linear interpolations
         * for any geometry restricted to linear interpolations.
         */
        public static Geometry convexHull(Geometry geom) {
            return evaluate(new ConvexHull(geom)).result;
        }

        /**
         * Returns a geometric object that represents the Point set difference of this geometric object with anotherGeometry.
         */
        @UML(identifier="difference", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.5
        public static Geometry difference(Geometry geom1, Geometry geom2) {
            return evaluate(new Difference(geom1, geom2)).result;
        }

        /**
         * Returns the shortest distance between any two Points in the two geometric objects as calculated in the
         * spatial reference system of this geometric object.
         * Because the geometries are closed, it is possible to find a point on each geometric object involved, such that
         * the distance between these 2 points is the returned distance between their geometric objects.
         */
        public static double distance(Geometry geom1, Geometry geom2) {
            return evaluate(new Distance(geom1, geom2)).result;
        }

        @UML(identifier="distance", specification=ISO_19107) // section 6.4.4.26 and 6.4.8.2
        public static Length distance2(Geometry geometry) {
            throw new OperationException("Not supported yet");
        }

        /**
         * Returns a geometric object that represents the Point set intersection of this geometric object with anotherGeometry.
         */
        @UML(identifier="intersection", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.4
        public static Geometry intersection(Geometry geom1, Geometry geom2) {
            return evaluate(new Intersection(geom1, geom2)).result;
        }

        /**
         * Returns a geometric object that represents the Point set symmetric difference of this geometric
         * object with anotherGeometry.
         */
        @UML(identifier="symDifference", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.6
        public static Geometry symDifference(Geometry geom1, Geometry geom2) {
            return evaluate(new SymDifference(geom1, geom2)).result;
        }

        /**
         * Returns a geometric object that represents the Point set union of this geometric object with anotherGeometry.
         */
        @UML(identifier="union", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.7
        public static Geometry union(Geometry geom1, Geometry geom2) {
            return evaluate(new Union(geom1, geom2)).result;
        }
    }

    @UML(identifier="Query3D", specification=ISO_19107) // section 6.4.9
    public static final class SpatialAnalysis3D extends Static {

        private SpatialAnalysis3D(){}

        @UML(identifier="3Dintersection", specification=ISO_19107) // section 6.4.9
        public static Geometry intersection(Geometry geometry) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Ddifference", specification=ISO_19107) // section 6.4.9
        public static Geometry difference(Geometry geometry) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3DsymDifference", specification=ISO_19107) // section 6.4.9
        public static Geometry symDifference(Geometry geometry) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Dunion", specification=ISO_19107) // section 6.4.9
        public static Geometry union(Geometry geometry) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Dbuffer", specification=ISO_19107) // section 6.4.9
        public static Geometry buffer(Length radius) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3DconvexHull", specification=ISO_19107) // section 6.4.9
        public static Geometry getConvexHull() {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Ddistance", specification=ISO_19107) // section 6.4.9
        public static Length distance(Geometry geometry) {
            throw new OperationException("Not supported yet");
        }
    }

    /**
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.3 Methods for testing spatial relations between geometric objects
     */
    @UML(identifier="Query2D", specification=ISO_19107) // section 6.4.8
    public static final class SpatialRelations2D extends Static {

        private SpatialRelations2D(){}

        @UML(identifier="contains", specification=ISO_19107) // section 6.4.4.30 ?
        public static boolean contains(Geometry geom1, DirectPosition element) {
            throw new OperationException("Not supported yet");
        }

        /**
         * Returns TRUE if this geometric object “spatially contains” anotherGeometry.
         */
        @UML(identifier="contains", specification=ISO_19107) // section 6.4.8.8, 6.4.4.2
        public static boolean contains(Geometry geom1, Geometry geom2) {
            return evaluate(new Contains(geom1, geom2)).result;
        }

        /**
         * Returns TRUE if this geometric object “spatially crosses” anotherGeometry.
         */
        @UML(identifier="crosses", specification=ISO_19107) // section 6.4.8.8
        public static boolean crosses(Geometry geom1, Geometry geom2) {
            return evaluate(new Crosses(geom1, geom2)).result;
        }

        /**
         * Returns TRUE if this geometric object “spatially disjoint” anotherGeometry.
         */
        @UML(identifier="disjoint", specification=ISO_19107) // section 6.4.8.8
        public static boolean disjoint(Geometry geom1, Geometry geom2) {
            return evaluate(new Disjoint(geom1, geom2)).result;
        }

        /**
         * Returns TRUE if this geometric object “spatially equal” anotherGeometry.
         */
        @UML(identifier="equals", specification=ISO_19107) // section 6.4.8.8, 6.4.4.30
        public static boolean Equals(Geometry geom1, Geometry geom2) {
            return evaluate(new Equals(geom1, geom2)).result;
        }

        /**
         * Returns TRUE if this geometric object “spatially intersects” anotherGeometry.
         */
        @UML(identifier="intersects", specification=ISO_19107) // section 6.4.8.8, 6.4.4.30
        public static boolean intersects(Geometry geom1, Geometry geom2) {
            return evaluate(new Intersects(geom1, geom2)).result;
        }

        /**
         * Returns a derived geometry collection value that matches the specified m coordinate value.
         * See Subclause 6.1.2.6 “Measures on Geometry” for more details.
         */
        public static Geometry locateAlong(Geometry geom1, double mValue) {
            return evaluate(new LocateAlong(geom1, mValue)).result;
        }

        /**
         * Returns a derived geometry collection value that matches the specified range of m coordinate values inclusively.
         * See Subclause 6.1.2.6 “Measures on Geometry” for more details.
         */
        public static Geometry contains(Geometry geom1, double mStart, double mEnd) {
            return evaluate(new LocateBetween(geom1, mStart, mEnd)).result;
        }

        /**
         * Returns TRUE if this geometric object “spatially overlaps” anotherGeometry.
         */
        @UML(identifier="overlaps", specification=ISO_19107) // section 6.4.8.8
        public static boolean overlaps(Geometry geom1, Geometry geom2) {
            return evaluate(new Overlaps(geom1, geom2)).result;
        }

        /**
         * Returns TRUE if this geometric object is spatially related to anotherGeometry by testing for intersections between
         * the interior, boundary and exterior of the two geometric objects as specified by the values in the
         * intersectionPatternMatrix.
         * This returns FALSE if all the tested intersections are empty except exterior (this) intersect exterior (another).
         */
        public static boolean Relate(Geometry geom1, Geometry geom2, int matrix) {
            return evaluate(new Relate(geom1, geom2, matrix)).result;
        }

        @UML(identifier="relate", specification=ISO_19107) // section 6.4.8.8
        public static boolean relate(Geometry another, String matrix) {
            throw new OperationException("Not supported yet");
        }

        /**
         * Returns TRUE if this geometric object “spatially touches” anotherGeometry.
         */
        @UML(identifier="touches", specification=ISO_19107) // section 6.4.8.8
        public static boolean touches(Geometry geom1, Geometry geom2) {
            return evaluate(new Touches(geom1, geom2)).result;
        }

        /**
         * Returns TRUE if this geometric object “spatially within” anotherGeometry.
         */
        @UML(identifier="within", specification=ISO_19107) // section 6.4.8.8
        public static boolean within(Geometry geom1, Geometry geom2) {
            return evaluate(new Within(geom1, geom2)).result;
        }

        @UML(identifier="withinDistance", specification=ISO_19107) // section 6.4.8.8
        public static boolean withinDistance(Geometry another, Length distance) {
            throw new OperationException("Not supported yet");
        }

    }

    @UML(identifier="Query3D", specification=ISO_19107) // section 6.4.9
    public static final class SpatialRelations3D extends Static {

        private SpatialRelations3D(){}

        @UML(identifier="3Dcontains", specification=ISO_19107) // section 6.4.9
        public static boolean contains(Geometry another) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Dcrosses", specification=ISO_19107) // section 6.4.9
        public static boolean crosses(Geometry another) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Ddisjoint", specification=ISO_19107) // section 6.4.9
        public static boolean disjoint(Geometry another) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Dequals", specification=ISO_19107) // section 6.4.9
        public static boolean equals(Geometry another) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Dintersects", specification=ISO_19107) // section 6.4.9
        public static boolean intersects(Geometry another) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Doverlaps", specification=ISO_19107) // section 6.4.9
        public static boolean overlaps(Geometry another) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Dtouches", specification=ISO_19107) // section 6.4.9
        public static boolean touches(Geometry another) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Dwithin", specification=ISO_19107) // section 6.4.9
        public static boolean within(Geometry another) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3DwithinDistance", specification=ISO_19107) // section 6.4.9
        public static boolean withinDistance3D(Geometry another, Length distance) {
            throw new OperationException("Not supported yet");
        }

        @UML(identifier="3Drelate", specification=ISO_19107) // section 6.4.9
        public static boolean relate3D(Geometry another, String matrix) {
            throw new OperationException("Not supported yet");
        }
    }

    public static final class SpatialEdition extends Static {

        private SpatialEdition(){}

        /**
         * Returns a geometric object that represents a transformed version of the geometry.
         *
         * @param geom geometry to transform
         * @param crs target CRS, if null geometry crs is unchanged but transform will still be applied
         * @param transform transform to apply, if null, geometry crs to target crs will be used
         * @return geometry of same type when possible
         */
        @UML(identifier="transform", specification=ISO_19107) // section 6.4.4.28
        public static Geometry transform(Geometry geom, CoordinateReferenceSystem crs, MathTransform transform) {
            return evaluate(new Transform(geom, crs, transform)).result;
        }

        /**
         * Convert this geometry to a Primitive geometry type.
         * This method is provided as a conversion to GPU geometric model.
         *
         * @return equivalent primitive, all attributes are copied.
         *  can be a Primitive or MultiPrimitive
         */
        public static Geometry toPrimitive(Geometry geom) {
            return evaluate(new ToPrimitive(geom)).result;
        }

        /**
         * Add a Z axis on the geometry and configure it's ordinates.
         *
         * @param geom geometry to transform, if it already has a 3D crs and given crs is null, geometry crs will be preserved.
         * @param crs3d the result crs in 3d, if null an ellipsoid height is assumed
         * @param zEdit called to configure the Z value on each position, if null, value 0.0 will be used
         */
        public static Geometry to3D(Geometry geom, CoordinateReferenceSystem crs3d, Consumer<Tuple> zEdit) {
            return evaluate(new To3D(geom, crs3d, zEdit)).result;
        }

        /**
         * Create a new attribute or update an existing one.
         * @param geom geometry to modify
         * @param attributeName new attribute name
         * @param attributeSystem new attribute system
         * @param attributeType new attribute type
         * @param valueGenerator function to generate attribute value
         * @return new or modified geometry
         */
        public static Geometry computeAttribute(Geometry geom, String attributeName, SampleSystem attributeSystem, DataType attributeType, Function<Point,Tuple> valueGenerator) {
            return evaluate(new ComputeAttribute(geom, attributeName, attributeSystem, attributeType, valueGenerator)).result;
        }

        /**
         * Separate the points/lines/triangles in the given primitive.
         * This ensure each point is used only once.
         *
         * @return equivalent primitive, all attributes are copied.
         *  can be a Primitive or MultiPrimitive
         */
        public static Geometry separateFaces(Geometry geom) {
            final MeshPrimitive p = (MeshPrimitive) geom;

            final AttributesType attributesType = p.getAttributesType();
            final Map<String,List<Tuple>> atts = new HashMap<>();

            for (String name : attributesType.getAttributeNames()) {
                atts.put(name, new ArrayList<>());
            }

            MeshPrimitiveVisitor pv = new MeshPrimitiveVisitor(geom) {
                @Override
                protected void visit(Point candidate) {
                    for (Entry<String,List<Tuple>> entry : atts.entrySet()) {
                        entry.getValue().add(candidate.getAttribute(entry.getKey()));
                    }
                }

                @Override
                protected void visit(LineString candidate) {
                    final Point p0 = candidate.getPointN(0);
                    final Point p1 = candidate.getPointN(1);
                    for (Entry<String,List<Tuple>> entry : atts.entrySet()) {
                        entry.getValue().add(p0.getAttribute(entry.getKey()));
                        entry.getValue().add(p1.getAttribute(entry.getKey()));
                    }
                }

                @Override
                protected void visit(Triangle candidate) {
                    final LinearRing ring = candidate.getExteriorRing();
                    final Point p0 = ring.getPointN(0);
                    final Point p1 = ring.getPointN(1);
                    final Point p2 = ring.getPointN(2);
                    for (Entry<String,List<Tuple>> entry : atts.entrySet()) {
                        entry.getValue().add(p0.getAttribute(entry.getKey()));
                        entry.getValue().add(p1.getAttribute(entry.getKey()));
                        entry.getValue().add(p2.getAttribute(entry.getKey()));
                    }
                }

                @Override
                protected void visit(MeshPrimitive.Vertex vertex) {}
            };
            pv.visit();

            //do not create an index, result elements
            final MeshPrimitive.Type type;
            switch (p.getType()) {
                case POINTS : type = MeshPrimitive.Type.POINTS; break;
                case LINES :
                case LINE_LOOP :
                case LINE_STRIP : type = MeshPrimitive.Type.LINES; break;
                case TRIANGLES :
                case TRIANGLE_FAN :
                case TRIANGLE_STRIP :
                default : type = MeshPrimitive.Type.TRIANGLES; break;
            }
            final MeshPrimitive sep = MeshPrimitive.create(type);
            for (Entry<String,List<Tuple>> entry : atts.entrySet()) {
                final String name = entry.getKey();
                final TupleArray array = TupleArrays.of(entry.getValue(), attributesType.getAttributeSystem(name), attributesType.getAttributeType(name));
                sep.setAttribute(name, array);
            }
            return sep;
        }
    }

    /**
     * Find a processor capable to execute the operand.
     *
     * @param <T>
     * @param op searched operand, not null
     * @return processor or empty optional
     */
    public static <T extends Operation> Optional<Processor<T, ?>> findProcessor(T op) {
        if (op instanceof Operation.Binary cdt) {
            final BinaryKey key = new BinaryKey(op.getClass(), cdt.getGeometry().getClass(), cdt.getOtherGeometry().getClass(), false);
            Processor p;
            synchronized (BINARY_PROCESSORS) {
                p = BINARY_PROCESSORS.get(key);
                if (p == null) {
                    p = deepSearch(key);
                    BINARY_PROCESSORS.put(key, p);
                }
            }
            if (p == NONE) return Optional.empty();
            return Optional.of(p);

        } else {
            final UnaryKey key = new UnaryKey(op.getClass(), op.getGeometry().getClass(), false);
            Processor p;
            synchronized (UNARY_PROCESSORS) {
                p = UNARY_PROCESSORS.get(key);
                if (p == null) {
                    p = deepSearch(key);
                    UNARY_PROCESSORS.put(key, p);
                }
            }
            if (p == NONE) return Optional.empty();
            return Optional.of(p);
        }
    }

    /**
     * Find a processor and execute the operand.
     *
     * @param <T>
     * @param op operand to compute
     * @return input operand with filled results
     * @throws OperationException if no processor could be found or computing failed
     */
    public static <T extends Operation> T evaluate(T op) throws OperationException {
        final Optional<Processor<T, ?>> proc = findProcessor(op);
        if (proc.isEmpty()) throw new OperationException("No processor can handle requested operation.");
        proc.get().process(op);
        return op;
    }

    /**
     * Make a deep search for another processor with wider scope that can handle the operand.
     * @param key searched key
     * @return
     */
    private static Processor deepSearch(UnaryKey key) {

        //find keys that can support the combine parameters
        List<UnaryKey> candidates = UNARY_PROCESSORS.keySet().stream()
                .filter(Key::mainKey)
                .filter((t) -> t.canSupport(key))
                .collect(Collectors.toCollection(ArrayList::new));

        //try to reduce list
        reduce:
        while (candidates.size() > 1) {
            final UnaryKey cdt1 = candidates.get(0);
            for (int i = candidates.size() - 1; i > 0; i--) {
                final UnaryKey cdt2 = candidates.get(i);
                if (cdt2.canSupport(cdt1)) {
                    //means cdt2 has a larger scope, it is less specialized so less efficient
                    candidates.remove(i);
                    continue reduce;
                } else if (cdt1.canSupport(cdt2)) {
                    //means cdt2 has a larger scope, it is less specialized so less efficient
                    candidates.remove(0);
                    continue reduce;
                }
            }
            //no more simplification possible, or we encounter a case were we can not identify the most efficient one
            break;
        }

        if (candidates.size() > 1) {
            throw new OperationException("Could not identify a single processor, several matches");
        } else if (candidates.size() == 1) {
            return UNARY_PROCESSORS.get(candidates.get(0));
        } else {
            return NONE;
        }
    }

    /**
     * Make a deep search for another processor with wider scope that can handle the operand.
     * @param key searched key
     * @return
     */
    private static Processor deepSearch(BinaryKey key) {

        //find keys that can support the combine parameters
        List<BinaryKey> candidates = BINARY_PROCESSORS.keySet().stream()
                .filter(Key::mainKey)
                .filter((t) -> t.canSupport(key))
                .collect(Collectors.toCollection(ArrayList::new));

        //try to reduce list
        reduce:
        while (candidates.size() > 1) {
            final BinaryKey cdt1 = candidates.get(0);
            for (int i = candidates.size() - 1; i > 0; i--) {
                final BinaryKey cdt2 = candidates.get(i);
                if (cdt2.canSupport(cdt1)) {
                    //means cdt2 has a larger scope, it is less specialized so less efficient
                    candidates.remove(i);
                    continue reduce;
                } else if (cdt1.canSupport(cdt2)) {
                    //means cdt2 has a larger scope, it is less specialized so less efficient
                    candidates.remove(0);
                    continue reduce;
                }
            }
            //no more simplification possible, or we encounter a case were we can not identify the most efficient one
            break;
        }

        if (candidates.size() > 1) {
            throw new OperationException("Could not identify a single processor, several matches");
        } else if (candidates.size() == 1) {
            return BINARY_PROCESSORS.get(candidates.get(0));
        } else {
            return NONE;
        }
    }

    private static abstract class Key<T extends Operation> {

        protected final Class opClass;
        protected final boolean mainKey;

        public Key(Class opClass, boolean mainKey) {
            this.opClass = opClass;
            this.mainKey = mainKey;
        }

        public boolean mainKey() {
            return mainKey;
        }
    }

    private static final class UnaryKey extends Key {
        private final Class geomClass;

        public UnaryKey(Class opClass, Class geomClass, boolean mainKey) {
            super(opClass, mainKey);
            this.geomClass = geomClass;
        }

        @Override
        public int hashCode() {
            return opClass.hashCode() + geomClass.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final UnaryKey other = (UnaryKey) obj;
            return this.opClass.equals(other.opClass)
                && this.geomClass.equals(other.geomClass);
        }

        boolean canSupport(UnaryKey other) {
            return this.opClass == other.opClass
                && this.geomClass.isAssignableFrom(other.geomClass);
        }
    }

    private static final class BinaryKey extends Key {
        private final Class geom1Class;
        private final Class geom2Class;

        public BinaryKey(Class opClass, Class geom1Class, Class geom2Class, boolean mainKey) {
            super(opClass, mainKey);
            this.geom1Class = geom1Class;
            this.geom2Class = geom2Class;
        }

        @Override
        public int hashCode() {
            return opClass.hashCode() + geom1Class.hashCode() + geom2Class.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final BinaryKey other = (BinaryKey) obj;
            return this.opClass.equals(other.opClass)
                && this.geom1Class.equals(other.geom1Class)
                && this.geom2Class.equals(other.geom2Class);
        }

        boolean canSupport(BinaryKey other) {
            return this.opClass == other.opClass
                && this.geom1Class.isAssignableFrom(other.geom1Class)
                && this.geom2Class.isAssignableFrom(other.geom2Class);
        }
    }

}
