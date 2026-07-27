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
import java.util.List;
import java.util.Objects;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * A CoverageJSON object with the type "NdArray" is an NdArray object.
 * It represents a multidimensional (>= 0D) array with named axes, encoded as a
 * flat, one-dimensional JSON array in row-major order.
 *
 * Note that common JSON implementations use IEEE 754-2008 64-bit
 * (double precision) floating point numbers as the data type for "values". Users
 * SHOULD be aware of the limitations in precision when encoding numbers in this way.
 * For example, when encoding integers, users SHOULD be aware that only values
 * within the range [-253+1, 253-1] can be represented in a way that will ensure
 * exact interoperability among such implementations [IETF RFC 7159].
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"type","dataType","axisNames","shape","values"})
public final class NdArray extends CoverageJsonObject {

    public static final String DATATYPE_FLOAT = "float";
    public static final String DATATYPE_INTEGER = "integer";
    public static final String DATATYPE_STRING = "string";

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * An NdArray object MUST have a member with the name "dataType" where the
     * value is either "float", "integer", or "string" and MUST correspond to
     * the data type of the non-null values in the "values" array.
     */
    public String dataType;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * An NdArray object MAY have a member with the name "axisNames" where the
     * value is an array of strings of the same length as "shape", such that
     * each string assigns a name to the corresponding dimension. For 0D arrays,
     * "axisNames" MAY be omitted (defaulting to []). For >= 1D arrays it MUST
     * be included.
     */
    public String[] axisNames;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * An NdArray object MAY have a member with the name "shape" where the value
     * is an array of integers. For 0D arrays, "shape" MAY be omitted
     * (defaulting to []). For >= 1D arrays it MUST be included.
     *
     * Where "shape" is present and non-empty, the product of its values MUST
     * equal the number of elements in the "values" array.
     */
    public int[] shape;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * An NdArray object MUST have a member with the name "values" where the
     * value is a non-empty array of numbers and nulls, or strings and nulls,
     * where nulls represent missing data.
     *
     * Zero-dimensional NdArrays MUST have exactly one item in the "values" array.
     *
     * Within the "values" array, the elements MUST be ordered such that the
     * last dimension in "axisNames" varies fastest, i.e. row-major order.
     * (This mimics the approach taken in NetCDF; see the example below.)
     */
    public List<Object> values; //because of null and string values

    public NdArray() {
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof NdArray)) return false;

        final NdArray cdt = ((NdArray) other);
        return super.equals(other)
            && Objects.equals(dataType, cdt.dataType)
            && Arrays.equals(axisNames, cdt.axisNames)
            && Arrays.equals(shape, cdt.shape)
            && Objects.equals(values, cdt.values);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                dataType,
                Arrays.hashCode(axisNames),
                Arrays.hashCode(shape),
                values);
    }
}
