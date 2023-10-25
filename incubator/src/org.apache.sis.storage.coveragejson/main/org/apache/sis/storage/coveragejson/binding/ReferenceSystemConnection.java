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
package org.apache.sis.storage.coveragejson.binding;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.List;
import java.util.Objects;

/**
 * A reference system connection object creates a link between values within
 * domain axes and a reference system to be able to interpret those values, e.g.
 * as coordinates in a certain coordinate reference system.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"coordinates","system"})
public final class ReferenceSystemConnection extends Dictionary<Object> {

    /**
     * A reference system connection object MUST have a member "coordinates"
     * which has as value an array of coordinate identifiers that are referenced
     * in this object. Depending on the type of referencing, the ordering of the
     * identifiers MAY be relevant, e.g. for 2D/3D coordinate reference systems.
     * In this case, the order of the identifiers MUST match the order of axes
     * in the coordinate reference system.
     */
    public List<String> coordinates;
    /**
     * A reference system connection object MUST have a member "system" whose
     * value MUST be a Reference System Object.
     */
    public CoverageJsonObject system;

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof ReferenceSystemConnection)) return false;

        final ReferenceSystemConnection cdt = ((ReferenceSystemConnection) other);
        return super.equals(other)
            && Objects.equals(system, cdt.system)
            && Objects.equals(coordinates, cdt.coordinates);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                system,
                coordinates);
    }
}
