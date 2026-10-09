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
package org.apache.sis.storage.geopose.binding;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * Orientation of a GeoPose inner frame expressed as a unit quaternion.
 *
 * <p>This class is the JSON image of the quaternion only; it carries no arithmetic.
 * Quaternion operations are obtained by converting to {@code org.apache.sis.maths.Quaternion}
 * with {@code org.apache.sis.storage.geopose.GeoPoseMapper}.</p>
 *
 * <ul>
 *   <li>The four members are mandatory.</li>
 *   <li>The quaternion is expected to be of unit length; this class does not verify it.</li>
 *   <li>Member order in the JSON encoding is x, y, z, w.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition Quaternion"
 */
@JsonPropertyOrder({
    Quaternion.JSON_PROPERTY_X,
    Quaternion.JSON_PROPERTY_Y,
    Quaternion.JSON_PROPERTY_Z,
    Quaternion.JSON_PROPERTY_W
})
public class Quaternion extends DataTransferObject {

    public static final String JSON_PROPERTY_X = "x";
    private double x;

    public static final String JSON_PROPERTY_Y = "y";
    private double y;

    public static final String JSON_PROPERTY_Z = "z";
    private double z;

    public static final String JSON_PROPERTY_W = "w";
    private double w;

    public Quaternion() {
    }

    public Quaternion(final double x, final double y, final double z, final double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Returns the first vector component.
     *
     * @return the x component
     */
    @JsonProperty(JSON_PROPERTY_X)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getX() {
        return x;
    }

    @JsonProperty(JSON_PROPERTY_X)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setX(final double x) {
        this.x = x;
    }

    /**
     * Returns the second vector component.
     *
     * @return the y component
     */
    @JsonProperty(JSON_PROPERTY_Y)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getY() {
        return y;
    }

    @JsonProperty(JSON_PROPERTY_Y)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setY(final double y) {
        this.y = y;
    }

    /**
     * Returns the third vector component.
     *
     * @return the z component
     */
    @JsonProperty(JSON_PROPERTY_Z)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getZ() {
        return z;
    }

    @JsonProperty(JSON_PROPERTY_Z)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setZ(final double z) {
        this.z = z;
    }

    /**
     * Returns the scalar component.
     *
     * @return the w component
     */
    @JsonProperty(JSON_PROPERTY_W)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getW() {
        return w;
    }

    @JsonProperty(JSON_PROPERTY_W)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setW(final double w) {
        this.w = w;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Quaternion other = (Quaternion) o;
        return Double.compare(this.x, other.x) == 0
            && Double.compare(this.y, other.y) == 0
            && Double.compare(this.z, other.z) == 0
            && Double.compare(this.w, other.w) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }
}
