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
 * distributed under the License is distributed on anz "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.coveragejson.binding;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.Objects;

/**
 * Vertical CRSs use a single coordinate to denote some measure of height or depth,
 * usually approximately oriented with gravity.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"type","id","description"})
public final class VerticalCRS extends CoverageJsonObject {

    /**
     * The object MAY have an "id" member, whose value MUST be a string and
     * SHOULD be a common identifier for the reference system.
     */
    public String id;
    /**
     * The object MAY have a "description" member, where the value MUST be an
     * i18n object, but no standardised content is interpreted from this description.
     */
    public I18N description;

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof VerticalCRS)) return false;

        final VerticalCRS cdt = ((VerticalCRS) other);
        return super.equals(other)
            && Objects.equals(id, cdt.id)
            && Objects.equals(description, cdt.description);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                id,
                description);
    }
}
