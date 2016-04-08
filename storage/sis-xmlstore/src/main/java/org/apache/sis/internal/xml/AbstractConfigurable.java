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
package org.apache.sis.internal.xml;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class AbstractConfigurable {

    /**
     * Map of optional properties to configure the underlying object.
     *
     */
    protected final Map<String, Object> properties = new HashMap<>();

    /**
     * 
     * @param key property key
     * @return value, may be null
     */
    public Object getProperty(final String key) {
        return properties.get(key);
    }
    
    /**
     * @return the properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

}
