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
package org.apache.sis.geometries;

import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.DataType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 3D Engine decompose draw calls by primitive types.
 * Each indexed primitives are forwarded with a set of Attributes.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface AttributesType {

    public static final String ATT_POSITION = "POSITION";
    public static final String ATT_NORMAL = "NORMAL";
    public static final String ATT_TANGENT = "TANGENT";
    public static final String ATT_TEXCOORD_0 = "TEXCOORD_0";

    //indexed attributes
    public static final String ATT_TEXCOORD = "TEXCOORD";
    public static final String ATT_COLOR = "COLOR";
    public static final String ATT_JOINTS = "JOINTS";
    public static final String ATT_WEIGHTS = "WEIGHTS";

    /**
     * Attribute from OGC 3D Tiles.
     * To link primitives to features/batch tables.
     */
    public static final String ATT_BATCH_ID = "_BATCHID";

    /**
     * Returns attribute system for given name.
     *
     * @param name seached attribute name
     * @return system or null.
     */
    SampleSystem getAttributeSystem(String name);

    /**
     * Returns attribute type for given name.
     *
     * @param name seached attribute name
     * @return type or null.
     */
    DataType getAttributeType(String name);

    /**
     * Returns attribute names.
     *
     * @return names, never null, can be empty
     */
    List<String> getAttributeNames();


    /**
     * Empty attributes type.
     */
    public static AttributesType EMPTY = new AttributesType() {
        @Override
        public SampleSystem getAttributeSystem(String name) {
            return null;
        }

        @Override
        public DataType getAttributeType(String name) {
            return null;
        }

        @Override
        public List<String> getAttributeNames() {
            return Collections.EMPTY_LIST;
        }
    };

    /**
     * Modifiable AttributesType implementation.
     */
    public static final class Template implements AttributesType {

        private final Map<String,DataType> datatypes = new HashMap<>();
        private final Map<String,SampleSystem> sampleSystems = new HashMap<>();

        public Template() {}

        public void addOrReplaceAttribute(String name, SampleSystem system, DataType type) {
            datatypes.put(name, type);
            sampleSystems.put(name, system);
        }

        @Override
        public SampleSystem getAttributeSystem(String name) {
            return sampleSystems.get(name);
        }

        @Override
        public DataType getAttributeType(String name) {
            return datatypes.get(name);
        }

        @Override
        public List<String> getAttributeNames() {
            return new ArrayList(datatypes.keySet());
        }
    }

    /**
     * Retains only the elements in this AttributesType that are contained in the
     * specified AttributesType. In other words, removes from
     * this AttributesType all of its elements that are not contained in the
     * specified AttributesType.
     *
     * @param other not null
     * @return new AttributesType or this instance if unchanged.
     */
    default AttributesType retainAll(AttributesType other) throws IllegalArgumentException {
        List<String> attributeNames = new ArrayList<>(getAttributeNames());
        attributeNames.retainAll(other.getAttributeNames());

        final Template template = new Template();
        for (String name : getAttributeNames()) {
            SampleSystem system = other.getAttributeSystem(name);
            if (system == null) continue;
            if (!system.equals(getAttributeSystem(name))) {
                throw new IllegalArgumentException("Both attribute types contain " + name + " but sample system differ");
            }
            DataType type = DataType.largest(getAttributeType(name), other.getAttributeType(name));
            template.addOrReplaceAttribute(name, system, type);
        }

        return template;
    }
}
