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
import java.util.Arrays;
import java.util.Objects;

/**
 * A CoverageJSON object with the type "TiledNdArray" is a TiledNdArray object.
 * It represents a multidimensional (>= 1D) array with named axes that is split
 * up into sets of linked NdArray documents. Each tileset typically covers a
 * specific data access scenario, for example, loading a single time slice of a
 * grid vs. loading a time series of a spatial subset of a grid.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"dataType","shape","axisNames","tileSets"})
public final class TiledNdArray extends Dictionary<Object> {

    /**
     * A TiledNdArray object MUST have a member with the name "dataType" where
     * the value is either "float", "integer", or "string".
     */
    public String dataType;
    /**
     * A TiledNdArray object MUST have a member with the name "shape" where the
     * value is a non-empty array of integers.
     */
    public int[] shape;
    /**
     * A TiledNdArray object MUST have a member with the name "axisNames" where
     * the value is a string array of the same length as "shape".
     */
    public String[] axisNames;
    /**
     * A TiledNdArray object MUST have a member with the name "tileSets" where
     * the value is a non-empty array of TileSet objects.
     */
    public TileSet[] tileSets;

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof TiledNdArray)) return false;

        final TiledNdArray cdt = ((TiledNdArray) other);
        return super.equals(other)
            && Objects.equals(dataType, cdt.dataType)
            && Arrays.equals(shape, cdt.shape)
            && Arrays.equals(axisNames, cdt.axisNames)
            && Arrays.equals(tileSets, cdt.tileSets);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                dataType,
                Arrays.hashCode(shape),
                Arrays.hashCode(axisNames),
                Arrays.hashCode(tileSets));
    }
}
