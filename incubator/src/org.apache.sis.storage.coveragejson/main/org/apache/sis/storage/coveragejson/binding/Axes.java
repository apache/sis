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
import java.util.Objects;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * The "axes" member MUST NOT be empty.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"x","y","z","t"})
public final class Axes extends Dictionary<Object> {

    public Axe x;
    public Axe y;
    public Axe z;
    public Axe t;

    public Axes() {
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Axes)) return false;

        final Axes cdt = ((Axes) other);
        return super.equals(other)
            && Objects.equals(x, cdt.x)
            && Objects.equals(y, cdt.y)
            && Objects.equals(y, cdt.y)
            && Objects.equals(t, cdt.t);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                x,
                y,
                z,
                t);
    }
}
