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
package org.apache.sis.geometries.scene;

import java.util.Objects;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.util.ArgumentChecks;

/**
 * GLTF name this a Primitive.
 * ANARI name this a Surface.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Surface {

    private Geometry geometry;
    private Material material;

    public Surface(Geometry geometry) {
        ArgumentChecks.ensureNonNull("geometry", geometry);
        this.geometry = geometry;
        this.material = new Material();
    }

    public Surface(Geometry geometry, Material material) {
        ArgumentChecks.ensureNonNull("geometry", geometry);
        ArgumentChecks.ensureNonNull("material", material);
        this.geometry = geometry;
        this.material = material;
    }

    /**
     * Get geometry definition.
     *
     * @return Geometry never null
     */
    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        ArgumentChecks.ensureNonNull("geometry", geometry);
        this.geometry = geometry;
    }

    /**
     * Get material definition.
     *
     * @return Material never null
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Set material definition.
     * @param material not null;
     */
    public void setMaterial(Material material) {
        ArgumentChecks.ensureNonNull("material", material);
        this.material = material;
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
        final Surface other = (Surface) obj;
        if (!Objects.equals(this.geometry, other.geometry)) {
            return false;
        }
        if (!Objects.equals(this.material, other.material)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.geometry);
        hash = 71 * hash + Objects.hashCode(this.material);
        return hash;
    }
}
