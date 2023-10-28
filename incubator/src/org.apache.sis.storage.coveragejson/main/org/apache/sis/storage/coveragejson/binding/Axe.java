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
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * An axis object MUST have either a "values" member or, as a compact notation
 * for a regularly spaced numeric axis, have all the members "start", "stop",
 * and "num".
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"start","stop","num","dataType","coordinates","values","bounds"})
public final class Axe extends Dictionary<Object> {

    public static final String DATATYPE_PRIMITIVE = "primitive";
    public static final String DATATYPE_TUPLE = "tuple";
    public static final String DATATYPE_POLYGON = "polygon";

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * The values of "start" and "stop" MUST be numbers
     */
    public Double start;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * The values of "start" and "stop" MUST be numbers
     */
    public Double stop;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * the value of "num" an integer greater than zero.
     *
     * If the value of "num" is 1, then "start" and "stop" MUST have identical
     * values. For num > 1, the array elements of "values" MAY be reconstructed
     * with the formula start + i * step where i is the ith element and in the
     * interval [0, num-1] and step = (stop - start) / (num - 1).
     *
     * If num = 1 then "values" is [start]. Note that "start" can be greater
     * than "stop" in which case the axis values are descending.
     */
    public Integer num;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * The value of "dataType" determines the structure of an axis value and its
     * coordinates that are made available for referencing. The values of
     * "dataType" defined in this Community Standard are "primitive", "tuple",
     * and "polygon". Custom values MAY be used as detailed in the Extensions
     * section. For "primitive", there is a single coordinate identifier and
     * each axis value MUST be a number or string. For "tuple", each axis value
     * MUST be an array of fixed size of primitive values in a defined order,
     * where the tuple size corresponds to the number of coordinate identifiers.
     * For "polygon", each axis value MUST be a GeoJSON Polygon coordinate
     * array, where the order of coordinates is given by the "coordinates"
     * array.
     *
     * If missing, the member "dataType" defaults to "primitive" and MUST not be
     * included for that default case.
     *
     * If "dataType" is "primitive" and the associated reference system (see
     * 6.1.2) defines a natural ordering of values then the array values in
     * "values", if existing, MUST be ordered monotonically, that is, increasing
     * or decreasing.
     */
    public String dataType;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * The value of "coordinates" is a non-empty array of coordinate identifiers
     * corresponding to the order of the coordinates defined by "dataType".
     *
     * If missing, the member "coordinates" defaults to a one-element array of
     * the axis identifier and MUST NOT be included for that default case.
     *
     * A coordinate identifier SHALL NOT be defined more than once in all axis
     * objects of a domain object.
     */
    public List<String> coordinates;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * The value of "values" is a non-empty array of axis values.
     */
    public List<Object> values;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * An axis object MAY have axis value bounds defined in the member "bounds"
     * where the value is an array of values of length len*2 with len being the
     * length of the "values" array. For each axis value at array index i in the
     * "values" array, a lower and upper bounding value at positions 2*i and
     * 2*i+1, respectively, are given in the bounds array.
     *
     * If a domain axis object has no "bounds" member then a bounds array MAY be
     * derived automatically.
     */
    public List<Object> bounds;

    public Axe() {
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Axe)) return false;

        final Axe cdt = ((Axe) other);
        return super.equals(other)
            && Objects.equals(start, cdt.start)
            && Objects.equals(stop, cdt.stop)
            && Objects.equals(num, cdt.num)
            && Objects.equals(dataType, cdt.dataType)
            && Objects.equals(coordinates, cdt.coordinates)
            && Objects.equals(values, cdt.values)
            && Objects.equals(bounds, cdt.bounds);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                start,
                stop,
                num,
                dataType,
                coordinates,
                values,
                bounds);
    }
}
