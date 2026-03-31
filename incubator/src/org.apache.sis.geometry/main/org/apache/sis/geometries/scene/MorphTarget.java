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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.SampleSystem;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MorphTarget implements AttributesType {

    private final LinkedHashMap<String,Array> attributes = new LinkedHashMap<>();

    /**
     * Get geometry attributes type.
     * @return attributes type, never null
     */
    public AttributesType getAttributesType() {
        return this;
    }

    @Override
    public List<String> getAttributeNames() {
        return List.of(attributes.keySet().toArray(new String[0]));
    }

    @Override
    public SampleSystem getAttributeSystem(String name){
        Array att = getAttribute(name);
        if (att == null) return null;
        return att.getSampleSystem();
    }

    @Override
    public DataType getAttributeType(String name){
        Array att = getAttribute(name);
        if (att == null) return null;
        return att.getDataType();
    }

    /**
     * Returns tuple array for given name.
     *
     * @param name seached attribute name
     * @return attribute array or null.
     */
    public Array getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * @param name attribute name
     * @param array if null, will remove the attribute
     */
    public void setAttribute(String name, Array array) {
        if (array == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, array);
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.attributes);
        return hash;
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
        final MorphTarget other = (MorphTarget) obj;
        if (!Objects.equals(this.attributes, other.attributes)) {
            return false;
        }
        return true;
    }


}
