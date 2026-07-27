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

import java.util.Arrays;
import java.util.Objects;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"tileShape","urlTemplate"})
public final class TileSet extends Dictionary<Object> {
    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A TileSet object MUST have a member with the name "tileShape" where
     * the value is an array of the same length as "shape" and where each
     * array element is either null or an integer lower or equal than the
     * corresponding element in "shape". A null value denotes that the axis
     * is not tiled.
     */
    public Integer[] tileShape;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A TileSet object MUST have a member with the name "urlTemplate" where
     * the value is a Level 1 URI template as defined in RFC 6570 .
     * The URI template MUST contain a variable for each axis name whose
     * corresponding element in "tileShape" is not null. A variable for an
     * axis of total size totalSize (from "shape") and tile size tileSize
     * (from "tileShape") has as value one of the integers 0, 1, …, q + r - 1
     * where q and r are the quotient and remainder obtained by dividing
     * totalSize by tileSize. Each URI that can be generated from the URI
     * template MUST resolve to an NdArray CoverageJSON document where the
     * members "dataType" and "axisNames`" are identical to the ones of the
     * TiledNdArray object, and where each value of `"shape" is an integer
     * equal, or lower if an edge tile, to the corresponding element in
     * "tileShape" while replacing null with the corresponding element of
     * "shape" of the TiledNdArray.
     */
    public String urlTemplate;

    public TileSet() {
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof TileSet)) return false;

        final TileSet cdt = ((TileSet) other);
        return super.equals(other)
            && Objects.equals(urlTemplate, cdt.urlTemplate)
            && Arrays.equals(tileShape, cdt.tileShape);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                urlTemplate,
                Arrays.hashCode(tileShape));
    }
}
