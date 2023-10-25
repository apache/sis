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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.annotation.JsonbTransient;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Johann Sorel (Geomatys)
 */
public class Dictionary<T> {

    /**
     * could be anything.
     * TODO find how to cath any other property as in Jackson @JsonAnyGetter and Johnzon @JohnzonAny
     */
    @JsonbTransient
    public final LinkedHashMap<String, T> any = new LinkedHashMap<>();

    public final Map<String, T> getAny() {
        return this.any;
    }

    public final void setAnyProperty(String name, T value) {
        this.any.put(name, value);
    }

    @Override
    public String toString() {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.toJson(this);
        } catch (Exception ex) {
            ex.printStackTrace();
            return getClass().getName() + " : to_string_exception" + ex.getMessage();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Dictionary)) return false;

        final Dictionary cdt = ((Dictionary) other);
        return Objects.equals(any, cdt.any);
    }

    @Override
    public int hashCode() {
        return Objects.hash(any);
    }
}
