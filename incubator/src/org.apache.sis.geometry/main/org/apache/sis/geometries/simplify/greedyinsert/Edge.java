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

import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Edge {

    private boolean obsolete = false;
    public final Tuple p0;
    public final Tuple p1;
    public WTriangle t0;
    public WTriangle t1;
    /**
     * Set to true if this edge is a constraint.
     * If true this edge should not be removed in the TIN creation process.
     * Still this edge can be split, each resulting edge will inherit the constraint.
     */
    private boolean constraint = false;

    public Edge(Tuple p0, Tuple p1) {
        if (p0.get(0) == p1.get(0) && p0.get(1) == p1.get(1)) {
            throw new IllegalArgumentException("Edge points XY are identical");
        }
        this.p0 = p0;
        this.p1 = p1;
    }

    void makeObsolete(){
        this.obsolete = true;
    }

    boolean isObsolete() {
        return obsolete;
    }

    /**
     * @return true if point in on the edge
     */
    public boolean isOnEdge(Tuple pt) {
        assert (!obsolete);
        return Maths.isOnLine(p0, p1, pt);
    }

    void change(WTriangle before, WTriangle after) {
        assert (!obsolete);
        if (t0 == before) {
            t0 = after;
        } else if (t1 == before) {
            t1 = after;
        } else{
            throw new IllegalStateException("Triangle is not defined for this edge");
        }
    }

    /**
     * @return true if one of the two edge point is the same as given point.
     */
    public boolean hasPoint(Tuple pt) {
        assert (!obsolete);
        return p0 == pt || p1 == pt;
    }

    /**
     * Set to true if this edge is a constraint.
     * If true this edge should not be removed in the TIN creation process.
     * Still this edge can be split, each resulting edge will inherit the constraint.
     *
     * @param constraint true for a constraint edge
     */
    public void setConstraint(boolean constraint) {
        this.constraint = constraint;
    }

    /**
     * @return true if edge is a constraint
     */
    public boolean isConstraint() {
        return constraint;
    }

    @Override
    public String toString() {
        return "E "+p0+" "+p1;
    }

}
