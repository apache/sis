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
package org.apache.sis.geometries.index;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.Tuple;


/**
 * Adapted version of KdTree from :
 * https://stackoverflow.com/questions/253767/kdtree-implementation-in-java
 *
 * @author Johann Sorel (Geomatys)
 * @param <T>
 */
public final class KdTree<T> {

    private Node<T> root = null;

    /**
     * @return true if tree is empty
     */
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * @return number of objects in the tree
     */
    public int size() {
        return rechnenSize(root);
    }

    private static int rechnenSize(Node<?> node) {
        if (node == null) {
            return 0;
        } else {
            return node.getSize();
        }
    }

    /**
     * @param position only X/Y ordinates will be used by the tree, not null
     * @param value to insert in the tree, not null
     */
    public void insert(Tuple position, T value) {
        insertInternal(new TuplePoint2D<>(position), value);
    }

    /**
     * @param x X ordinate
     * @param y Y ordinate
     * @param value to insert in the tree, not null
     */
    public void insert(double x, double y, T value) {
        insertInternal(new TuplePoint2D<>(x, y), value);
    }

    /**
     * Insert the given tuple in the tree, but do not copy it's values.
     * This allows to reduce memory usage but will reduce performances.
     * The tuple values MUST remain unchanged while the tree is being used
     * otherwise the tree will be broken.
     *
     * @param position only X/Y ordinates will be used by the tree, not null
     * @param value to insert in the tree, not null
     */
    public void insertNoCopy(Tuple position, T value) {
        insertInternal(position, value);
    }

    private void insertInternal(Tuple p, T payload) {
        if (isEmpty()) {
            root = insertInternal(p, payload, root, 0);
        } else {
            root = insertInternal(p, payload, root, 1);
        }
    }

    /**
     * At odd level we will compare x coordinate, and at even level we will
     * compare y coordinate
     */
    private Node<T> insertInternal(Tuple pointToInsert, T payload, Node<T> node, int level) {
        if (node == null) {
            final Node<T> newNode;
            if (payload == null) {
                newNode = new Node<>(pointToInsert, null, null);
            } else {
                newNode = new NodeWithPayload<>(pointToInsert, payload, null, null);
            }
            return newNode;
        }
        if (level % 2 == 0) {
            //Horizontal partition line
            if (pointToInsert.get(1) < node.getY()) {
                //Traverse in bottom area of partition
                node.lb = insertInternal(pointToInsert, payload, node.lb, level + 1);
            } else {
                //Traverse in top area of partition
                if (!node.point.equals(pointToInsert)) {
                    node.rt = insertInternal(pointToInsert, payload, node.rt, level + 1);
                }
            }

        } else {
            //Vertical partition line
            if (pointToInsert.get(0) < node.getX()) {
                //Traverse in left area of partition
                node.lb = insertInternal(pointToInsert, payload, node.lb, level + 1);
            } else {
                //Traverse in right area of partition
                if (!node.point.equals(pointToInsert)) {
                    node.rt = insertInternal(pointToInsert, payload, node.rt, level + 1);
                }
            }
        }
        return node;
    }

    /**
     * @param p not null
     * @return true if point in in the tree
     */
    public boolean contains(Tuple p) {
        return containsInternal(new Point2D.Double(p.get(0), p.get(1)), root, 1);
    }

    /**
     * @param p not null
     * @return true if point in in the tree
     */
    public boolean contains(Point2D.Double p) {
        return containsInternal(p, root, 1);
    }

    private boolean containsInternal(Point2D.Double pointToSearch, Node<T> node, int level) {
        if (node == null) {
            return false;
        }
        if (level % 2 == 0) {
            //Horizontal partition line
            if (pointToSearch.y < node.getY()) {
                return containsInternal(pointToSearch, node.lb, level + 1);
            } else {
                if (node.point.equals(pointToSearch)) {
                    return true;
                }
                return containsInternal(pointToSearch, node.rt, level + 1);
            }
        } else {
            //Vertical partition line
            if (pointToSearch.x < node.getX()) {
                return containsInternal(pointToSearch, node.lb, level + 1);
            } else {
                if (node.point.equals(pointToSearch)) {
                    return true;
                }
                return containsInternal(pointToSearch, node.rt, level + 1);
            }
        }
    }

    /**
     * @param p searched position, not null
     * @return nearest entry in the tree
     */
    public Entry<Tuple,T> nearest(Tuple p) {
        if (root == null) {
            return null;
        }
        final BestMatch champion = new BestMatch(root, Double.MAX_VALUE);
        nearestInternal(p, champion, root, 1, false);
        return champion.champion;
    }

    /**
     * @param p searched position, not null
     * @return nearest entry in the tree
     */
    public Entry<Tuple,T> nearest(Point2D.Double p) {
        if (root == null) {
            return null;
        }
        return nearest((Tuple)new TuplePoint2D(p.x, p.y));
    }

    /**
     * Search nearest point but must be distinct from given point.
     *
     * @param p searched position, not null
     * @return nearest entry in the tree
     */
    public Entry<Tuple,T> nearestDistinct(Tuple p) {
        if (root == null) {
            return null;
        }
        final BestMatch champion = new BestMatch(root, Double.MAX_VALUE);
        nearestInternal(p, champion, root, 1, true);
        return champion.champion;
    }

    /**
     * Search nearest point but must be distinct from given point.
     *
     * @param p searched position, not null
     * @return nearest entry in the tree
     */
    public Entry<Tuple,T> nearestDistinct(Point2D.Double p) {
        if (root == null) {
            return null;
        }
        return nearestDistinct((Tuple)new TuplePoint2D(p.x, p.y));
    }

    /**
     * @return true if perfect match (distance == 0)
     */
    private boolean nearestInternal(final Tuple targetPoint, final BestMatch champion,
            Node node, int level, boolean distinct) {


        final double dist = distanceSq(targetPoint, node.point);
        if (dist < champion.championDist) {
            if (distinct && dist == 0){
                //same point, ignore it
            } else {
                champion.champion = node;
                champion.championDist = dist;
                if (dist == 0) return true;
            }
        }

        //We will decide which part to be visited first, based upon in which part point lies.
        //If point is towards left or bottom part, we traverse in that area first, and later on decide
        //if we need to search in other part too.
        final boolean goLeftOrBottom;
        if (level % 2 == 0) {
            goLeftOrBottom = (targetPoint.get(1) < node.getY());
        } else {
            goLeftOrBottom = (targetPoint.get(0) < node.getX());
        }
        if (goLeftOrBottom) {
            if (node.lb != null && nearestInternal(targetPoint, champion, node.lb, level+1, distinct)) {
                return true;
            }
            if (node.rt != null) {
                final Point2D orientationPoint = createOrientationPoint(node.getX(), node.getY(), targetPoint, level);
                final double orientationDist = orientationPoint.distanceSq(targetPoint.get(0), targetPoint.get(1));
                //We will search on the other part only, if the point is very near to partitioned line
                //and champion point found so far is far away from the partitioned line.
                if (orientationDist < champion.championDist && nearestInternal(targetPoint, champion, node.rt, level+1, distinct)) {
                    return true;
                }
            }

        } else {
            if (node.rt != null && nearestInternal(targetPoint, champion, node.rt, level+1, distinct)) {
                return true;
            }
            if (node.lb != null) {
                final Point2D orientationPoint = createOrientationPoint(node.getX(), node.getY(), targetPoint, level);
                //We will search on the other part only, if the point is very near to partitioned line
                //and champion point found so far is far away from the partitioned line.
                final double orientationDist = orientationPoint.distanceSq(targetPoint.get(0), targetPoint.get(1));
                if (orientationDist < champion.championDist && nearestInternal(targetPoint, champion, node.lb, level+1, distinct)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the point from a partitioned line, which can be directly used to
     * calculate distance between partitioned line and the target point for
     * which neighbours are to be searched.
     *
     * @param linePointX
     * @param linePointY
     * @param targetPoint
     * @param level
     * @return
     */
    private Point2D.Double createOrientationPoint(double linePointX, double linePointY, Tuple targetPoint, int level) {
        if (level % 2 == 0) {
            return new Point2D.Double(targetPoint.get(0), linePointY);
        } else {
            return new Point2D.Double(linePointX, targetPoint.get(1));
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KdTree");
        if (root != null) sb.append("\n").append(root.toString(1));
        return sb.toString();
    }

    private static double distanceSq(Tuple p0, Tuple p1) {
        double px = p1.get(0) - p0.get(0);
        double py = p1.get(1) - p0.get(1);
        return (px * px + py * py);
    }

    private static class BestMatch {

        public Node champion;
        public double championDist;

        public BestMatch(Node c, double d) {
            champion = c;
            championDist = d;
        }
    }

    private static class TuplePoint2D<T> extends Point2D.Double implements Tuple{

        public TuplePoint2D(Tuple position) {
            this.x = position.get(0);
            this.y = position.get(1);
        }

        public TuplePoint2D(double x, double y) {
            super(x, y);
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public String toString() {
            return x + "/" + y;
        }

        @Override
        public SampleSystem getSampleSystem() {
            return SampleSystem.ofSize(2);
        }

        @Override
        public DataType getDataType() {
            return DataType.DOUBLE;
        }

        @Override
        public double get(int indice) throws IndexOutOfBoundsException {
            return indice == 0 ? x : y;
        }

        @Override
        public void set(int indice, double value) throws IndexOutOfBoundsException {
            if (indice == 0) x = value;
            else y = value;
        }
    }

    private static class Node<T> implements Entry<Tuple, T>{
        // the point
        private final Tuple point;
        // the left/bottom subtree
        private Node<T> lb;
        // the right/top subtree
        private Node<T> rt;

        public Node(Tuple point, Node<T> lb, Node<T> rt) {
            this.point = point;
            this.lb = lb;
            this.rt = rt;
        }

        public double getX() {
            return point.get(0);
        }

        public double getY() {
            return point.get(1);
        }

        @Override
        public Tuple getKey() {
            return point;
        }

        @Override
        public T getValue() {
            return null;
        }

        public int getSize() {
            return 1 + rechnenSize(lb) + rechnenSize(rt);
        }

        @Override
        public T setValue(T value) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public String toString() {
            return toStringTree(point.toString() + " size :" + getSize(),
                    Arrays.asList("left/bot : " + lb, "right/top : " + rt));
        }

        public String toString(int level) {

            if (level % 2 == 0) {
                //Horizontal partition
                final List<String> children = new ArrayList<>();
                children.add("top: " + ((rt == null) ? "null" : rt.toString(level+1)));
                children.add("bot: " + ((lb == null) ? "null" : lb.toString(level+1)));
                return toStringTree("X:" + getX() + " Y:" + getY() + " size:" + getSize(), children);
            } else {
                //Vertical partition
                final List<String> children = new ArrayList<>();
                children.add("left: " + ((lb == null) ? "null" : lb.toString(level+1)));
                children.add("right: " + ((rt == null) ? "null" : rt.toString(level+1)));
                return toStringTree("X:" + getX() + " Y:" + getY() + " size:" + getSize(), children);
            }
        }
    }

    private static class NodeWithPayload<T> extends Node<T>{
        private final T payload;

        public NodeWithPayload(Tuple point, T payload, Node<T> lb, Node<T> rt) {
            super(point,lb,rt);
            this.payload = payload;
        }

        @Override
        public T getValue() {
            return payload;
        }

    }

    /**
     * Returns a graphical representation of the specified objects. This representation can be
     * printed to the {@linkplain System#out standard output stream} (for example) if it uses
     * a monospaced font and supports unicode.
     *
     * @param  root  The root name of the tree to format.
     * @param  objects The objects to format as root children.
     * @return A string representation of the tree.
     */
    private static String toStringTree(String root, final Iterable<?> objects) {
        final StringBuilder sb = new StringBuilder();
        if (root != null) {
            sb.append(root);
        }
        if (objects != null) {
            final Iterator<?> ite = objects.iterator();
            while (ite.hasNext()) {
                sb.append('\n');
                final Object next = ite.next();
                final boolean last = !ite.hasNext();
                sb.append(last ? "\u2514\u2500 " : "\u251C\u2500 ");

                final String[] parts = String.valueOf(next).split("\n");
                sb.append(parts[0]);
                for (int k=1;k<parts.length;k++) {
                    sb.append('\n');
                    sb.append(last ? ' ' : '\u2502');
                    sb.append("  ");
                    sb.append(parts[k]);
                }
            }
        }
        return sb.toString();
    }
}
