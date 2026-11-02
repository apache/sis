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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.internal.shared.DefaultGeometryCollection;


/**
 * MultiPrimitive is a group of primitive defining a more complex geometry.
 * A primitive mimics the natural structures used by GPUs.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MultiMeshPrimitive<T extends MeshPrimitive> extends DefaultGeometryCollection<T> {

    /**
     * Create a geometry with right hand 3D CRS.
     */
    public MultiMeshPrimitive() {
        this(Geometries.RIGHT_HAND_3D);
    }

    public MultiMeshPrimitive(CoordinateReferenceSystem crs) {
        super(GeometryType.GEOMETRY, crs, null);
    }

    public MultiMeshPrimitive(MeshPrimitive ... primitives) {
        this(primitives[0].getCoordinateReferenceSystem());
        append(Arrays.asList(primitives));
    }

    public boolean isEmpty() {
        for (MeshPrimitive p : geometries) {
            if (!p.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public List<T> getComponents() {
        return Collections.unmodifiableList(geometries);
    }

    /**
     * Check geometry definition.
     */
    public void validate() {
        for (MeshPrimitive p : geometries) {
            p.validate();
        }
    }

    /**
     * Set each vertex normal by computing the normal of the triangle
     * where it is used.
     * This method should not be used if vertices are shared by multiple
     * triangles or it will cause visual glitches.
     */
    public void computeFaceNormals() {
        for (MeshPrimitive p : geometries) {
            p.computeFaceNormals();
        }
    }

    /**
     * Set each vertex normal by computing the normal of the triangle
     * where it is used.
     */
    public void computeSmoothNormals() {
        for (MeshPrimitive p : geometries) {
            p.computeSmoothNormals();
        }
    }

}
