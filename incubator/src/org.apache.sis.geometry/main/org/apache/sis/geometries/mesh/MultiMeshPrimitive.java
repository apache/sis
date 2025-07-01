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
package org.apache.sis.geometries.mesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.sis.geometries.privy.AbstractGeometry;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * MultiPrimitive is a group of primitive defining a more complex geometry.
 * A primitive mimics the natural structures used by GPUs.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MultiMeshPrimitive<T extends MeshPrimitive> extends AbstractGeometry implements GeometryCollection<T> {

    protected final List<T> primitives = new ArrayList<>();

    private CoordinateReferenceSystem crs;
    private Map<String, Object> userProperties;

    /**
     * Create a geometry with right hand 3D CRS.
     */
    public MultiMeshPrimitive() {
        this(Geometries.RIGHT_HAND_3D);
    }

    public MultiMeshPrimitive(CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("crs", crs);
        this.crs = crs;
    }

    public MultiMeshPrimitive(MeshPrimitive ... primitives) {
        this(primitives[0].getCoordinateReferenceSystem());
        append(Arrays.asList(primitives));
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        for (MeshPrimitive p : primitives) {
            return p.getCoordinateReferenceSystem();
        }
        return crs;
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
        for (MeshPrimitive p : primitives) {
            p.setCoordinateReferenceSystem(crs);
        }
        this.crs = crs;
    }

    public boolean isEmpty() {
        for (MeshPrimitive p : primitives) {
            if (!p.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public List<T> getComponents() {
        return Collections.unmodifiableList(primitives);
    }

    @Override
    public int getNumGeometries() {
        return primitives.size();
    }

    @Override
    public T getGeometryN(int n) {
        return primitives.get(n);
    }

    /**
     * Check geometry definition.
     */
    public void validate() {
        for (MeshPrimitive p : primitives) {
            p.validate();
        }
    }

    /**
     * Add primitives.
     *
     * @param other Primitives to merge in.
     */
    public void append(Collection<? extends MeshPrimitive> other) {
        for (MeshPrimitive p : other) {
            if (!Utilities.equalsIgnoreMetadata(crs, p.getCoordinateReferenceSystem())) {
                throw new IllegalArgumentException("Primitives must have the same CRS as the MultiPrimitive.");
            }
        }
        primitives.addAll((Collection<? extends T>) other);
    }

    /**
     * Make a deep copy of this multi-primitive.
     *
     * Note : this method will clone all attributes.
     *
     * @return copied MultiPrimitive
     */
    public MultiMeshPrimitive deepCopy() throws TransformException {
        final MultiMeshPrimitive copy = new MultiMeshPrimitive(getCoordinateReferenceSystem());
        for (MeshPrimitive p : primitives) {
            copy.primitives.add(p.deepCopy());
        }
        return copy;
    }

    /**
     * Set each vertex normal by computing the normal of the triangle
     * where it is used.
     * This method should not be used if vertices are shared by multiple
     * triangles or it will cause visual glitches.
     */
    public void computeFaceNormals() {
        for (MeshPrimitive p : primitives) {
            p.computeFaceNormals();
        }
    }

    /**
     * Set each vertex normal by computing the normal of the triangle
     * where it is used.
     */
    public void computeSmoothNormals() {
        for (MeshPrimitive p : primitives) {
            p.computeSmoothNormals();
        }
    }

    @Override
    public synchronized Map<String, Object> userProperties() {
        if (userProperties == null) {
            userProperties = new HashMap<>();
        }
        return userProperties;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.primitives);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MultiMeshPrimitive other = (MultiMeshPrimitive) obj;
        if (!Objects.equals(this.primitives, other.primitives)) {
            return false;
        }
        return Objects.equals(this.getCoordinateReferenceSystem(), other.getCoordinateReferenceSystem());
    }

}
