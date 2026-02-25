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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A 3D model is attached to a scene node with geometric and rendering definitions.
 *
 * GLTF name this a Mesh.
 * ANARI has no equivalent for it.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Model {

    private CoordinateReferenceSystem crs;
    private String name;
    private final List<Double> morphWeights = new ArrayList<>();
    private final List<Surface> components = new ArrayList<>();
    //user properties
    private Map<String,Object> properties;

    /**
     * Create a geometry with right hand 3D CRS.
     */
    public Model() {
        this(Geometries.RIGHT_HAND_3D);
    }

    public Model(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    /**
     * @param geometries must contain at least one geometry
     */
    public Model(Collection<? extends Geometry> geometries) {
        this(geometries.toArray(MeshPrimitive[]::new));
    }

    /**
     * @param geometries must contain at least one geometry
     */
    public Model(Geometry ... geometries) {
        this(geometries[0].getCoordinateReferenceSystem());
        for (Geometry p : geometries) {
            this.components.add(new Surface(p));
        }
    }

    /**
     * @param geometries must contain at least one surface
     */
    public Model(Surface ... surfaces) {
        this(surfaces[0].getGeometry().getCoordinateReferenceSystem());
        for (Surface p : surfaces) {
            this.components.add(p);
        }
    }

    /**
     * @return model name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name model name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return scene node coordinate system, never null.
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * This method will modify :
     * - model surface crs if defined
     * @param crs
     */
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("crs", crs);
        for (Surface surface : components) {
            surface.getGeometry().setCoordinateReferenceSystem(crs);
        }
        this.crs = crs;
    }

    /**
     *
     * @return Model envelope, never null but can be set to NaN
     */
    public Envelope getEnvelope() {
        GeneralEnvelope e = null;
        for (Surface ms : components) {
            GeneralEnvelope me = new GeneralEnvelope(ms.getGeometry().getEnvelope());
            //we ignore the model crs, often undefined
            me.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
            if (!me.isAllNaN()) {
                if (e == null) {
                    e = me;
                } else {
                    e.add(me);
                }
            }
        }
        if (e == null) {
            e = new GeneralEnvelope(getCoordinateReferenceSystem());
            e.setToNaN();
        }
        return e;
    }

    /**
     * @return Model morph target weights.
     */
    public List<Double> getMorphWeights() {
        return morphWeights;
    }

    public List<Surface> getComponents() {
        return components;
    }

    /**
     * Set given material on all primitives.
     * @param material
     */
    public void setMaterial(Material material) {
        for (Surface p : getComponents()) {
            p.setMaterial(material);
        }
    }

    /**
     * Map of properties for user needs.
     * Those informations may be lost in model processes.
     *
     * @return Map, never null.
     */
    public synchronized Map<String, Object> userProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.name);
        hash = 59 * hash + Objects.hashCode(this.morphWeights);
        return hash + super.hashCode();
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
        final Model other = (Model) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.morphWeights, other.morphWeights)) {
            return false;
        }
        if (!Objects.equals(this.components, other.components)) {
            return false;
        }
        if (!Objects.equals(this.crs, other.crs)) {
            return false;
        }
        return true;
    }

}
